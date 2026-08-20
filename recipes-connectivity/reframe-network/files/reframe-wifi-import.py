#!/usr/bin/env python3
"""Import an optional Wi-Fi profile from the writable boot partition."""

import hashlib
import json
import logging
import os
from pathlib import Path
import subprocess
import time


BOOT_DEVICE = Path("/dev/disk/by-label/boot")
BOOT_MOUNT = Path("/run/reframe-boot")
CONFIG_NAME = "reframe-wifi.json"
ERROR_NAME = "reframe-wifi.error.txt"
MAX_CONFIG_BYTES = 4096


def run(*args, timeout=45, check=True):
    result = subprocess.run(
        args,
        check=False,
        capture_output=True,
        text=True,
        timeout=timeout,
    )
    if check and result.returncode:
        raise RuntimeError((result.stderr or result.stdout).strip() or f"{args[0]} failed")
    return result


def read_config(path):
    if path.stat().st_size > MAX_CONFIG_BYTES:
        raise ValueError("configuration file is larger than 4096 bytes")
    try:
        config = json.loads(path.read_text(encoding="utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as error:
        raise ValueError("configuration file is not valid UTF-8 JSON") from error
    if not isinstance(config, dict):
        raise ValueError("configuration must be a JSON object")

    allowed = {"ssid", "password", "hidden"}
    unknown = set(config) - allowed
    if unknown:
        raise ValueError("unknown setting: " + sorted(unknown)[0])

    ssid = config.get("ssid")
    password = config.get("password", "")
    hidden = config.get("hidden", False)
    if not isinstance(ssid, str) or not ssid or len(ssid.encode("utf-8")) > 32:
        raise ValueError("ssid must contain between 1 and 32 UTF-8 bytes")
    if not isinstance(password, str) or len(password) > 128:
        raise ValueError("password must be a string of at most 128 characters")
    if not isinstance(hidden, bool):
        raise ValueError("hidden must be true or false")
    if password and len(password) < 8:
        raise ValueError("password must be empty or at least 8 characters")
    return ssid, password, hidden


def profile_name(ssid):
    digest = hashlib.sha256(ssid.encode("utf-8")).hexdigest()[:12]
    return f"reframe-boot-{digest}"


def import_profile(ssid, password, hidden):
    name = profile_name(ssid)
    run("nmcli", "connection", "delete", name, check=False)
    try:
        run(
            "nmcli", "connection", "add",
            "type", "wifi",
            "ifname", "*",
            "con-name", name,
            "ssid", ssid,
        )
        settings = [
            "nmcli", "connection", "modify", name,
            "connection.autoconnect", "yes",
            "connection.permissions", "",
            "802-11-wireless.hidden", "yes" if hidden else "no",
            "ipv4.method", "auto",
            "ipv6.method", "auto",
        ]
        if password:
            settings.extend([
                "802-11-wireless-security.key-mgmt", "wpa-psk",
                "802-11-wireless-security.psk", password,
            ])
        run(*settings)
    except Exception:
        run("nmcli", "connection", "delete", name, check=False)
        raise
    return name


def write_error(message):
    error_path = BOOT_MOUNT / ERROR_NAME
    error_path.write_text(
        "reFrame could not import reframe-wifi.json: " + message + "\n",
        encoding="utf-8",
    )


def main():
    logging.basicConfig(level=logging.INFO, format="%(levelname)s: %(message)s")
    for _ in range(30):
        if BOOT_DEVICE.exists():
            break
        time.sleep(0.5)
    else:
        logging.warning("Boot partition was not found; skipping Wi-Fi import")
        return 0

    BOOT_MOUNT.mkdir(mode=0o700, parents=True, exist_ok=True)
    mounted = False
    try:
        run(
            "mount", "-t", "vfat", "-o", "rw,nosuid,nodev,noexec",
            str(BOOT_DEVICE), str(BOOT_MOUNT), timeout=15,
        )
        mounted = True
        config_path = BOOT_MOUNT / CONFIG_NAME
        if not config_path.is_file():
            return 0

        try:
            ssid, password, hidden = read_config(config_path)
            name = import_profile(ssid, password, hidden)
        except (OSError, ValueError, RuntimeError, subprocess.TimeoutExpired) as error:
            logging.error("Wi-Fi configuration import failed: %s", error)
            config_path.unlink(missing_ok=True)
            write_error(str(error))
            os.sync()
            return 0

        config_path.unlink()
        (BOOT_MOUNT / ERROR_NAME).unlink(missing_ok=True)
        os.sync()
        logging.info("Imported boot Wi-Fi profile for SSID %r and erased credentials", ssid)

        result = run("nmcli", "connection", "up", name, timeout=45, check=False)
        if result.returncode:
            logging.warning(
                "Saved Wi-Fi profile but could not connect yet: %s",
                (result.stderr or result.stdout).strip(),
            )
        return 0
    except (OSError, RuntimeError, subprocess.TimeoutExpired) as error:
        logging.warning("Unable to inspect boot partition for Wi-Fi settings: %s", error)
        return 0
    finally:
        if mounted:
            run("umount", str(BOOT_MOUNT), timeout=15, check=False)


if __name__ == "__main__":
    raise SystemExit(main())
