#!/usr/bin/env python3
"""Small, dependency-free NetworkManager provisioning UI for reFrame."""

import json
import secrets
import subprocess
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs


AP_NAME = "reframe-setup"
AP_SSID = "reFrame-Setup"
AP_ADDRESS = "10.42.0.1/24"
LOCK = threading.RLock()
CSRF_TOKEN = secrets.token_urlsafe(24)


def nmcli(*args, timeout=40, check=True):
    result = subprocess.run(
        ["nmcli", "--terse", "--colors", "no", *args],
        check=False,
        capture_output=True,
        text=True,
        timeout=timeout,
    )
    if check and result.returncode:
        raise RuntimeError((result.stderr or result.stdout).strip() or "NetworkManager command failed")
    return result.stdout.strip()


def wifi_device():
    output = nmcli("--fields", "DEVICE,TYPE", "device", "status")
    for line in output.splitlines():
        device, _, kind = line.partition(":")
        if kind == "wifi":
            return device
    raise RuntimeError("No Wi-Fi interface is available")


def active_wifi_is_client():
    output = nmcli("--fields", "TYPE,NAME", "connection", "show", "--active", check=False)
    return any(line.startswith("802-11-wireless:") and not line.endswith(":" + AP_NAME)
               for line in output.splitlines())


def saved_wifi_exists():
    output = nmcli("--fields", "TYPE", "connection", "show", check=False)
    return "802-11-wireless" in output


def connection_status():
    device = wifi_device()
    profile = nmcli(
        "--get-values", "GENERAL.CONNECTION", "device", "show", device,
        check=False,
    )
    if not profile or profile == "--":
        return {"connected": False, "ssid": ""}
    if profile == AP_NAME:
        return {"connected": False, "ssid": "", "setup_ap": True}
    ssid = nmcli(
        "--get-values", "802-11-wireless.ssid",
        "connection", "show", profile,
        check=False,
    )
    return {"connected": True, "ssid": ssid or profile}


def start_access_point():
    with LOCK:
        if active_wifi_is_client():
            return
        device = wifi_device()
        nmcli("connection", "delete", AP_NAME, check=False)
        nmcli(
            "connection", "add", "type", "wifi", "ifname", device,
            "con-name", AP_NAME, "ssid", AP_SSID,
        )
        nmcli(
            "connection", "modify", AP_NAME,
            "802-11-wireless.mode", "ap",
            "802-11-wireless.band", "bg",
            "ipv4.method", "shared",
            "ipv4.addresses", AP_ADDRESS,
            "ipv6.method", "disabled",
            "connection.autoconnect", "no",
        )
        nmcli("connection", "up", AP_NAME, timeout=30)


def networks():
    device = wifi_device()
    # NetworkManager may only return its cache while this single radio is an AP.
    output = nmcli(
        "--escape", "yes", "--fields", "SSID,SIGNAL,SECURITY",
        "device", "wifi", "list", "ifname", device, "--rescan", "auto",
        timeout=20,
    )
    found = {}
    for line in output.splitlines():
        parts = line.rsplit(":", 2)
        if len(parts) != 3 or not parts[0]:
            continue
        ssid = parts[0].replace(r"\:", ":").replace(r"\\", "\\")
        if ssid == AP_SSID:
            continue
        try:
            signal = int(parts[1])
        except ValueError:
            signal = 0
        current = found.get(ssid)
        item = {"ssid": ssid, "signal": signal, "security": parts[2] or "Open"}
        if current is None or signal > current["signal"]:
            found[ssid] = item
    return sorted(found.values(), key=lambda item: (-item["signal"], item["ssid"].lower()))


def connect(ssid, password):
    if not ssid or len(ssid.encode("utf-8")) > 32:
        raise ValueError("Select a valid Wi-Fi network")
    if len(password) > 128:
        raise ValueError("The password is too long")
    device = wifi_device()
    with LOCK:
        nmcli("connection", "down", AP_NAME, check=False)
        args = ["device", "wifi", "connect", ssid, "ifname", device]
        if password:
            args.extend(["password", password])
        try:
            nmcli(*args, timeout=45)
        except Exception:
            # The browser disconnects as the AP goes down; always restore a
            # reachable setup path if joining the selected network fails.
            start_access_point()
            raise


PAGE = """<!doctype html>
<html lang=en><meta charset=utf-8><meta name=viewport content="width=device-width,initial-scale=1">
<title>reFrame Wi-Fi setup</title>
<style>
body{font:16px system-ui,sans-serif;max-width:34rem;margin:3rem auto;padding:0 1rem;color:#202124}
button,input,select{box-sizing:border-box;width:100%;padding:.8rem;margin:.35rem 0;font:inherit}
button{background:#202124;color:white;border:0;border-radius:.3rem;cursor:pointer}.note{color:#5f6368}
#message{min-height:1.5rem}label{display:block;margin-top:1rem}
</style>
<h1>Connect reFrame</h1>
<p id=current class=note>Checking the current connection…</p>
<p class=note>Select a network below to connect or change Wi-Fi. The setup hotspot will disappear while reFrame connects.</p>
<form id=form><label>Network<select id=ssid required><option>Scanning…</option></select></label>
<label>Password<input id=password type=password autocomplete=current-password></label>
<button>Connect</button></form><p id=message></p>
<script>
const message=document.querySelector('#message'), select=document.querySelector('#ssid');
async function status(){try{let r=await fetch('/api/status'), x=await r.json();document.querySelector('#current').textContent=x.connected?'Connected to '+x.ssid:(x.setup_ap?'Setup hotspot is active':'Wi-Fi is disconnected')}catch(e){document.querySelector('#current').textContent='Connection status unavailable'}}
async function scan(){try{let r=await fetch('/api/networks'), n=await r.json();select.innerHTML='';
for(const x of n){let o=document.createElement('option');o.value=x.ssid;o.textContent=`${x.ssid} (${x.signal}%, ${x.security})`;select.append(o)}
if(!n.length)select.innerHTML='<option value="">No networks found — reload to scan again</option>'}catch(e){message.textContent='Scan failed: '+e}}
document.querySelector('#form').onsubmit=async e=>{e.preventDefault();message.textContent='Connecting…';
try{let r=await fetch('/api/connect',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded','X-reFrame-CSRF':'__CSRF_TOKEN__'},body:new URLSearchParams({ssid:select.value,password:document.querySelector('#password').value})});
let x=await r.json();message.textContent=x.message;if(r.ok)setTimeout(()=>location.href='http://reframe.local',8000)}catch(e){message.textContent='The hotspot disconnected. Join your Wi-Fi and open http://reframe.local, or reconnect here if setup failed.'}};
status();scan();</script></html>""".replace("__CSRF_TOKEN__", CSRF_TOKEN)


class Handler(BaseHTTPRequestHandler):
    def reply(self, status, body, content_type="application/json"):
        data = body.encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", content_type + "; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(data)

    def redirect_to_setup(self):
        self.send_response(302)
        self.send_header("Location", "http://10.42.0.1/")
        self.send_header("Cache-Control", "no-store")
        self.send_header("Content-Length", "0")
        self.end_headers()

    def do_GET(self):
        if self.path == "/api/status":
            try:
                self.reply(200, json.dumps(connection_status()))
            except Exception as error:
                self.reply(503, json.dumps({"message": str(error)}))
        elif self.path == "/api/networks":
            try:
                self.reply(200, json.dumps(networks()))
            except Exception as error:
                self.reply(503, json.dumps({"message": str(error)}))
        elif self.path == "/" or self.path.startswith("/index.html"):
            self.reply(200, PAGE, "text/html")
        else:
            # Captive-portal probes deliberately request vendor-specific paths.
            # Redirecting unknown HTTP requests makes Android, Apple, Windows,
            # and desktop connectivity checks open their sign-in window.
            self.redirect_to_setup()

    def do_POST(self):
        if self.path != "/api/connect":
            self.reply(404, json.dumps({"message": "Not found"}))
            return
        try:
            if self.headers.get("X-reFrame-CSRF") != CSRF_TOKEN:
                self.reply(403, json.dumps({"message": "Invalid setup session; reload the page"}))
                return
            length = int(self.headers.get("Content-Length", "0"))
            if length > 1024:
                raise ValueError("Request is too large")
            form = parse_qs(self.rfile.read(length).decode("utf-8"), keep_blank_values=True)
            ssid = form.get("ssid", [""])[0]
            password = form.get("password", [""])[0]
            connect(ssid, password)
            self.reply(200, json.dumps({"message": "Connected. Open http://reframe.local"}))
        except (ValueError, RuntimeError, subprocess.TimeoutExpired) as error:
            self.reply(400, json.dumps({"message": str(error)}))

    def log_message(self, fmt, *args):
        # Do not put submitted form contents or client details in the journal.
        return


def ensure_setup_path():
    # Give existing profiles a chance to autoconnect before taking the radio
    # for the fallback AP. First boot has no profiles and skips this delay.
    attempts = 15 if saved_wifi_exists() else 1
    for _ in range(attempts):
        if active_wifi_is_client():
            return
        time.sleep(2)
    for _ in range(15):
        try:
            if active_wifi_is_client():
                return
            start_access_point()
            return
        except Exception:
            time.sleep(2)
    raise RuntimeError("Unable to start Wi-Fi or the setup hotspot")


def monitor_connection():
    disconnected_since = None
    while True:
        time.sleep(10)
        try:
            if active_wifi_is_client():
                disconnected_since = None
                continue
            active = nmcli(
                "--fields", "NAME", "connection", "show", "--active", check=False
            ).splitlines()
            if AP_NAME in active:
                disconnected_since = None
                continue
            disconnected_since = disconnected_since or time.monotonic()
            if time.monotonic() - disconnected_since >= 30:
                start_access_point()
                disconnected_since = None
        except Exception:
            # NetworkManager may be restarting. The next pass retries without
            # taking down any connection that recovered in the meantime.
            pass


if __name__ == "__main__":
    nmcli("general", "hostname", "reframe", check=False)
    ensure_setup_path()
    threading.Thread(target=monitor_connection, daemon=True).start()
    ThreadingHTTPServer(("0.0.0.0", 80), Handler).serve_forever()
