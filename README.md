# meta-reframe

`meta-reframe` is an independent Yocto/OpenEmbedded layer for building a
purpose-built Linux image for the open-source reFrame camera hardware. The
first target is a Raspberry Pi Zero 2 W (64-bit) with Camera Module 3 (IMX708),
I2C, and SPI enabled.

The current image integrates the camera application, Wi-Fi provisioning,
loopback-backed dashboard, and PiSugar 3 power and RTC support. A pinned,
source-built PiSugar service exposes only local control sockets, preserves time
across offline boots, and completes board shutdown after systemd powers off
Linux. Spectra display integration remains a separate milestone.

## Software update policy

There are currently **no in-system software updates**. The dashboard cannot
install updates, and the image provides no OTA client, package-feed upgrade, or
supported Git-based self-update path. Its update control is intentionally
disabled and reports that updates are managed by the reFrame system image.

To update a device today, build a new Yocto image and write it to the SD card.
Back up anything needed from `/var/lib/reframe` first because reflashing the
whole card can erase photos and settings. A future updater must be designed as
an image-level, authenticated and signed mechanism with failure recovery; the
upstream application's Git updater must not be enabled on this appliance.

## Build environment

Yocto Project 6.0 (Wrynose) and `kas` 5.3 are required. The canonical
configuration is `kas/reframe.yml`; it pins BitBake, OpenEmbedded-Core,
meta-openembedded, and meta-raspberrypi to the revisions used for the successful
build on 2026-08-11.

```sh
mkdir reframe-yocto
cd reframe-yocto
git clone https://github.com/oleksiikutuzov/meta-reframe.git

pipx install kas==5.3
KAS_WORK_DIR="$PWD" kas checkout meta-reframe/kas/reframe.yml
```

Run kas from the `reframe-yocto` project directory, not from inside the
`meta-reframe` repository. `KAS_WORK_DIR` keeps all checked-out layers at the
same level:

```text
reframe-yocto/
├── bitbake/
├── meta-openembedded/
├── meta-raspberrypi/
├── meta-reframe/
├── openembedded-core/
└── build/
```

`kas checkout` resolves the pinned repositories and writes `build/conf`. From
the same project directory, inspect or debug the BitBake environment with:

```sh
KAS_WORK_DIR="$PWD" kas shell meta-reframe/kas/reframe.yml
```

## Raspberry Pi Zero 2 W configuration

The kas configuration selects `raspberrypi0-2w-64`, systemd, I2C, SPI, and the
Camera Module 3/IMX708 overlay. Its effective `local.conf` settings are:

```sh
DEBUG_BUILD = "1"
INIT_MANAGER = "systemd"
ENABLE_I2C = "1"
ENABLE_SPI_BUS = "1"
ENABLE_UART = "${@oe.utils.vartrue('DEBUG_BUILD', '1', '0', d)}"
VIDEO_CAMERA = "1"
RASPBERRYPI_CAMERA_V3 = "1"
hostname:pn-base-files = "reframe"
DISABLE_SPLASH = "1"
DISABLE_RPI_BOOT_LOGO = "1"
CMDLINE_DEBUG = "${@oe.utils.vartrue('DEBUG_BUILD', '', 'quiet loglevel=3', d)}"
RPI_EXTRA_CONFIG = "dtoverlay=disable-bt\nhdmi_blanking=2\nboot_delay=0\ndisplay_auto_detect=0\ndisable_poe_fan=1\nforce_eeprom_read=0\nenable_tvout=0"
LICENSE_FLAGS_ACCEPTED += "synaptics-killswitch"
```

`RASPBERRYPI_CAMERA_V3` selects the Camera Module 3/IMX708 firmware overlay in
`meta-raspberrypi`. Review the restricted Wi-Fi firmware license before using
the accepted `synaptics-killswitch` flag.

`DEBUG_BUILD = "1"` enables SSH, the UART console with root autologin, an empty
root password for that console, and the `i2c-tools`, `v4l-utils`, and
`systemd-analyze` packages. Without that setting these development additions
are omitted and UART is disabled. This standard OpenEmbedded variable also
selects debug compiler optimization, so unset it or set it to `0` for release
builds.

The headless settings skip unused firmware probes. A layer append also forces
HDMI audio off after the Raspberry Pi machine recipe enables it. Release builds
add `quiet loglevel=3`; debug builds intentionally retain the serial kernel
console for recovery. See `docs/boot-analysis.md` for the service-side latency
changes and measurements to collect on hardware.

## Serial bring-up console

With `DEBUG_BUILD = "1"`, the image enables the UART at 115200 baud and
automatically logs in as root on the physical serial console. Connect a 3.3 V
USB-to-UART adapter
with adapter RX to GPIO14/TX (pin 8), adapter TX to GPIO15/RX (pin 10), and
ground to a Pi ground pin. Do not connect a 5 V UART signal.

Open the console from the build host, replacing the device path as needed:

```sh
picocom --baud 115200 /dev/ttyUSB0
```

The empty root password is limited to this local development console; SSH does
not allow empty-password root login. Remove serial autologin from the eventual
production image.

## Build and deploy

Build the bring-up image with the pinned configuration:

```sh
KAS_WORK_DIR="$PWD" kas build meta-reframe/kas/reframe.yml
```

For quicker metadata checks, run:

```sh
KAS_WORK_DIR="$PWD" kas shell meta-reframe/kas/reframe.yml -c 'bitbake-layers show-layers'
KAS_WORK_DIR="$PWD" kas shell meta-reframe/kas/reframe.yml -c 'bitbake-layers show-recipes reframe-image-minimal'
KAS_WORK_DIR="$PWD" kas shell meta-reframe/kas/reframe.yml -c 'bitbake -p reframe-image-minimal'
```

Artifacts are written below
`build/tmp/deploy/images/raspberrypi0-2w-64/`. The tested configuration produces the
stable symlink
`reframe-image-minimal-raspberrypi0-2w-64.rootfs.wic`, a compressed `.wic.bz2`
variant, and the matching `.wic.bmap` file.

To flash with Balena Etcher, select the uncompressed `.wic` file as the image,
select the correct SD card, and start the flash. Etcher writes the complete disk
layout, so do not extract or copy individual partitions.

For command-line deployment, confirm the destination device and flash the
compressed image with:

```sh
sudo bmaptool copy \
    build/tmp/deploy/images/raspberrypi0-2w-64/reframe-image-minimal-raspberrypi0-2w-64.rootfs.wic.bz2 \
    /dev/sdX
```

The 2026-08-19 build completed all 7,269 tasks successfully. This confirms the
metadata, PiSugar package QA, root filesystem, SPDX/SBOM generation, and WIC
image build. The libcamera capture path and PiSugar I2C addresses have been
validated on physical hardware; repeat the service-level tests below after
deploying this milestone.

The `Yocto sanity` GitHub Actions workflow performs fast kas and BitBake metadata
checks for pull requests and pushes to `main`. Run the separate `Yocto full layer
check` workflow manually when the complete `yocto-check-layer` signature suite
is needed. Neither workflow compiles or boots the image.
Full builds require about 66 GB of build-directory storage and remain a local
developer responsibility.

On the target, verify boot, interfaces, and camera discovery:

```sh
systemctl is-system-running
ls -l /dev/i2c* /dev/spidev* /dev/video* /dev/v4l-subdev*
dmesg | grep -Ei 'imx708|camera|i2c|spi'
systemd-analyze critical-chain
```

List cameras and perform a headless still capture with:

```sh
rpicam-hello --list-cameras
rpicam-still -n --timeout 2000 -o /tmp/camera-test.jpg
ls -lh /tmp/camera-test.jpg
```

Verify the Python API with a second headless still capture:

```sh
python3 - <<'PY'
from picamera2 import Picamera2

camera = Picamera2()
try:
    camera.start_and_capture_file(
        "/tmp/picamera2-test.jpg", show_preview=False
    )
finally:
    camera.close()
PY
ls -lh /tmp/picamera2-test.jpg
```

Verify the packaged application service and its persistent outputs with:

```sh
systemctl status reframe.service
journalctl -u reframe.service -b --no-pager
find /var/lib/reframe/photos -maxdepth 1 -type f -name '*.jpg' -print
find /var/lib/reframe/dithered_photos -maxdepth 1 -type f -name '*.png' -print
stat /var/lib/reframe/settings.json
test ! -w /usr/lib/reframe/reframe.py
```

The service attempts Camera Module 3 HDR setup before opening Picamera2 and
continues gracefully when that V4L2 control is unavailable. It takes one
startup capture, writes the original JPEG and processed PNG as `reframe`, then
waits for the PiSugar button on I2C. Reboot the board and confirm that settings
and existing numbered captures persist and that a new capture uses the next
number.

The packaged application polls the PiSugar power-button state directly over
I2C, matching the original reFrame hardware design. Without PiSugar hardware,
the startup capture still succeeds; button read failures are logged and retried.

## PiSugar power and RTC

The image builds `pisugar-server` and `pisugar-poweroff` from pinned Rust
sources. `pisugar-server` is configured for PiSugar 3 and exposes a Unix socket
at `/run/pisugar/pisugar-server.sock` plus a loopback-only compatibility port at
`127.0.0.1:8423`. It is not reachable from Wi-Fi. The layer disables shell
actions from programmable-button tap events. reFrame independently measures
the power-button press duration and captures only on a short press; long presses
remain reserved for PiSugar shutdown behavior.

At boot, `pisugar-rtc-restore.service` restores a plausible UTC value from the
PiSugar RTC before reFrame starts. After network time synchronization,
`pisugar-rtc-update.service` writes UTC back to the RTC. The server requests an
orderly shutdown after 30 seconds below 5 percent charge, and
`pisugar-poweroff.service` tells the board to cut power after Linux shuts down.

Inspect the integration on the target with:

```sh
systemctl status pisugar-server pisugar-rtc-restore pisugar-rtc-update
systemctl status pisugar-poweroff
printf 'get model\nget battery\nget rtc_time\nget anti_mistouch\n' \
    | nc -U -w 2 -q 0 /run/pisugar/pisugar-server.sock
ss -lntp | grep 8423
journalctl -u pisugar-server -u pisugar-rtc-restore -b --no-pager
```

The socket query should report PiSugar 3 data, `anti_mistouch: false`, and a
plausible RTC time. Port 8423 must listen only on `127.0.0.1`. Test low-battery
and final power-cut behavior with the device attended; do not deliberately
deep-discharge the battery.

## Wi-Fi provisioning

Wi-Fi can be prepared before the first boot. After writing the image, place a
file named `reframe-wifi.json` in the top level of the computer-visible boot
partition:

```json
{
  "ssid": "My Wi-Fi",
  "password": "correct horse battery staple",
  "hidden": false
}
```

`password` may be omitted or empty for an open network, and `hidden` defaults
to `false`. SSIDs and passwords may contain spaces and punctuation because the
file is parsed as JSON, not as shell code. On boot, reFrame creates a persistent
NetworkManager profile and erases the plaintext JSON file. Remove the card
safely after copying it and provision it in a trusted environment: until first
boot consumes the file, anyone who can read the FAT boot partition can see the
password. If parsing fails, the credentials are erased and a non-secret
`reframe-wifi.error.txt` explanation is written beside them.

On first boot, or whenever no saved Wi-Fi connection can be activated, join the
open `reFrame-Setup` access point. Phones and laptops should open the Wi-Fi
setup page as a captive portal; `http://10.42.0.1` remains the manual fallback.
Select a network and enter its password. The access point disappears while the single
Wi-Fi radio changes to client mode; after it connects, open
`http://reframe.local`. NetworkManager stores the connection on the device and
reconnects it on later boots. No Wi-Fi credentials are part of the image.

The setup access point is deliberately open because this milestone cannot show
a per-device secret without a working display. Provision in a trusted physical
environment. The setup service keeps listening after provisioning so network
settings can be changed at `http://reframe.local`; authentication and HTTPS are
required before treating that page as a production management interface.

Inspect or recover networking over UART with:

```sh
systemctl status NetworkManager reframe-wifi-import reframe-network avahi-daemon
nmcli device status
nmcli connection show
journalctl -u reframe-wifi-import -u reframe-network -u NetworkManager -b --no-pager
```

## Contributing

Send patches through GitHub pull requests. Keep each patch focused on one
milestone and state the validation performed. The layer maintainer is Oleksii
Kutuzov <oleksii.kutuzov@icloud.com>.
