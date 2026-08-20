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

On the 2026-08-19 image, the setup AP completed activation 20.6 seconds after
kernel boot. A longer delay before the phone displayed its sign-in window was
therefore client captive-portal detection rather than AP startup. Manual access
to `http://10.42.0.1` remains the deterministic fallback.

The same boot initially reported `degraded` because both NetworkManager and
`systemd-networkd-wait-online` were enabled. NetworkManager had already brought
up the AP, but the networkd helper waited 120 seconds for an interface that
networkd did not own and then failed. Masking that helper immediately returned
the system to `running`; the image now applies that mask while retaining
NetworkManager's own wait-online service.

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

To validate boot-partition provisioning, flash a clean image and place this at
the top level of its FAT boot partition before first boot:

```json
{"ssid":"test network","password":"test password","hidden":false}
```

After boot, confirm that the device joined the requested network, the profile
persists, and the plaintext input was removed:

```sh
systemctl status reframe-wifi-import --no-pager
journalctl -u reframe-wifi-import -b --no-pager
nmcli connection show
mkdir -p /run/boot-check
mount -t vfat -o ro /dev/disk/by-label/boot /run/boot-check
ls -la /run/boot-check/reframe-wifi*
umount /run/boot-check
```

Repeat with malformed JSON and confirm the input is erased,
`reframe-wifi.error.txt` contains no SSID or password, and the setup hotspot
appears. Also test an open network and a hidden WPA network.

## Remaining networking validation

- Flash and boot the image containing the dnsmasq service fix and Python module
  dependencies; the individual recipes pass package QA, but the final image has
  not yet been recorded here as a complete hardware pass.
- Confirm captive-portal auto-open behavior on Android, iOS/macOS, and Windows.
- Confirm current-SSID display and switching to a second network.
- Confirm recovery after a wrong password, AP loss, and repeated cold boots.
- Record the exact Raspberry Pi board revision and final tested commit.

## Dashboard validation

The networking service remains the only process exposed on TCP port 80. While
the setup AP is active it serves Wi-Fi provisioning and captive-portal probes;
after a client connection is active it proxies requests to the dashboard on
loopback. The dashboard and camera hardware API must not be exposed directly.

After provisioning Wi-Fi, run:

```sh
systemctl is-active reframe reframe-dashboard reframe-network
ss -lntp | grep -E ':80|:8000|:8077'
curl -fsS http://127.0.0.1:8000/ >/dev/null
curl -fsS http://reframe.local/ >/dev/null
```

Expected listeners are port 80 on all addresses and ports 8000 and 8077 on
loopback only. In a browser at `http://reframe.local`, confirm that the gallery
loads, a new capture appears, originals and processed images download, settings
survive a dashboard restart and reboot, and switching back to setup mode still
shows provisioning rather than the dashboard. Display-selection and QR-to-panel
actions remain hardware-gated until the Spectra 6 milestone.

The dashboard is intentionally unauthenticated and should be used only on a
trusted LAN. There is no in-system updater: application self-update is disabled,
and no OTA or package-feed upgrade path is installed. Updating currently means
backing up `/var/lib/reframe`, building a replacement image, and reflashing the
SD card.

## PiSugar 3 validation

Initial hardware inspection found the kernel I2C adapters in sysfs but no
`/dev/i2c-*` character devices. Loading `i2c-dev` created `/dev/i2c-0`,
`/dev/i2c-1`, `/dev/i2c-10`, and `/dev/i2c-11`. A scan of bus 1 then detected
the PiSugar controller at `0x57` and its RTC at `0x68`. The image now loads
`i2c-dev` automatically during boot.

The rebuilt image subsequently confirmed automatic module loading, stable
`pisugar-server` operation, a 76 percent battery reading at 3.79 V, and
successful RTC synchronization. The initial stale 2022 RTC value was rejected;
after network time synchronized, the service wrote current UTC to the PiSugar
RTC and created `/var/lib/reframe/rtc-last-network-sync`. Camera startup also
produced both the original JPEG and dithered PNG. Three transient reFrame I2C
read errors coincided with the manual `i2cdetect` scan. More importantly, the
initial event-stream integration watched the PiSugar custom-button events, but
the reFrame external switch is wired to the distinct power-button pad and
therefore produced no event. A live trace confirmed register `0x02` bit 0
changing while that switch was held. The application now uses the original
reFrame direct power-button polling path and measures press duration itself.
Avoid scanning bus 1 while normal services are using it.

PiSugar anti-mistouch remains disabled so a single press wakes the camera.
Short power-button presses capture; presses of at least two seconds remain
reserved for shutdown and must not trigger a capture.

With a charged PiSugar 3 connected to I2C bus 1, boot the image and run:

```sh
systemctl is-active pisugar-server pisugar-rtc-restore
systemctl is-enabled pisugar-server pisugar-poweroff \
    pisugar-rtc-restore pisugar-rtc-update
test -c /dev/i2c-1
i2cdetect -y 1
printf 'get model\nget battery\nget battery_v\nget rtc_time\nget anti_mistouch\n' \
    | nc -U -w 2 -q 0 /run/pisugar/pisugar-server.sock
ss -lntp | grep 8423
journalctl -u pisugar-server -u pisugar-rtc-restore -b --no-pager
```

Expected results:

- All services are active or enabled as appropriate, with no restart loop.
- `/dev/i2c-1` exists without a manual `modprobe`; the scan shows `57` and
  `68`.
- Model, battery percentage, voltage, and RTC queries return plausible values.
- `anti_mistouch` is false so the camera wakes from a single physical press.
- TCP port 8423 is bound only to `127.0.0.1`; no PiSugar web port is open.
- A short press creates one new original and processed image; holding the button
  does not trigger an unintended shell action.

For RTC persistence, first let system time synchronize, start
`pisugar-rtc-update.service`, disconnect networking, shut down cleanly, and
boot again. The restored time must be close to current UTC and must not move
backward behind the last trusted network timestamp.

Finally, with the device attended, run `systemctl poweroff` and confirm Linux
shuts down cleanly before PiSugar removes power. Validate the 5 percent,
30-second low-battery shutdown threshold using a controlled supply or a battery
near the threshold; do not deep-discharge the cell.

## Remaining PiSugar validation

- Record PiSugar hardware and firmware revisions, idle/load battery readings,
  RTC drift over an offline interval, and the tested commit.
- Repeat short-press capture, orderly shutdown, and cold-boot RTC restore for at
  least ten cycles.
- Confirm low-battery cancellation when charge recovers during the delay.
