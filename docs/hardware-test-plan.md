# Hardware Test Record

This file records reproducible target checks and observed results. A successful
recipe or image build is not a substitute for the hardware gates below.

## Test platform

- Board: Raspberry Pi Zero 2 W (board revision not recorded)
- Wireless device: Broadcom BCM43438, SDIO, `brcmfmac`
- Driver version observed: `7.45.96`
- Firmware version observed: `es7`
- NetworkManager version: `1.56.0`
- Test date reported by target: 2026-03-13

## Wi-Fi provisioning observations

The target detected `wlan0`, reported AP-mode support, and scanned nearby 2.4
GHz networks. NetworkManager initially failed to activate `reframe-setup` with:

```text
Connection activation failed: IP configuration could not be reserved
```

The address could be assigned manually and IPv4 forwarding could be enabled.
The relevant dnsmasq journal then showed:

```text
failed to create listening socket for 10.42.0.1: Address already in use
```

Root cause: installing dnsmasq also enabled its system-wide service. That daemon
claimed port 53 before NetworkManager could launch the private dnsmasq instance
used by `ipv4.method=shared`. Disabling the standalone service allowed the AP
to activate and accept a client. The layer now disables that service while
retaining the binary for NetworkManager.

After provisioning, the Pi joined the selected Wi-Fi network and the setup page
was reachable at `reframe.local`. The UI previously did not identify the active
SSID; it now queries NetworkManager and displays the current connection.

The Broadcom driver printed the following while changing modes:

```text
brcmf_vif_set_mgmt_ie: vndr ie set error : -52
brcmf_cfg80211_set_power_mgmt: power save enabled
```

In this test these messages were non-fatal: AP and client operation completed.
Error `-52` is the firmware rejecting an optional vendor management-information
element update during the transition. Treat it as actionable only when paired
with scan failures, firmware timeouts, or a failed connection.

## Reproducible networking checks

On a newly flashed image with no saved Wi-Fi profile:

```sh
systemctl is-active NetworkManager reframe-network avahi-daemon
systemctl is-enabled dnsmasq
nmcli device status
nmcli connection show --active
ip address show wlan0
ss -lntup | grep -E ':53|:67|:80'
```

Expected results:

- NetworkManager, reframe-network, and Avahi are active.
- The standalone dnsmasq service is disabled.
- `reframe-setup` is active and the Wi-Fi interface owns `10.42.0.1/24`.
- A NetworkManager-owned dnsmasq instance provides DHCP and DNS.
- Joining `reFrame-Setup` opens the captive setup page, with
  `http://10.42.0.1` available as a fallback.

After selecting a network:

```sh
nmcli device status
nmcli connection show --active
getent hosts reframe.local
ping -c 4 1.1.1.1
```

Reboot and repeat the checks to verify profile persistence. Then remove or make
the selected AP unavailable and confirm `reFrame-Setup` returns after the
fallback interval.

## Remaining networking validation

- Flash and boot the image containing the dnsmasq service fix and Python module
  dependencies; the individual recipes pass package QA, but the final image has
  not yet been recorded here as a complete hardware pass.
- Confirm captive-portal auto-open behavior on Android, iOS/macOS, and Windows.
- Confirm current-SSID display and switching to a second network.
- Confirm recovery after a wrong password, AP loss, and repeated cold boots.
- Record the exact Raspberry Pi board revision and final tested commit.
