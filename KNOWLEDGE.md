# Project knowledge

This file preserves the engineering context needed to maintain `meta-reframe`.
The image is treated as a complete, functioning appliance; historical plans,
bring-up logs, and verification checklists are intentionally omitted.

## Scope and source layout

`meta-reframe` is an independent Yocto/OpenEmbedded layer for the open-source
reFrame camera. Its canonical build configuration is `kas/reframe.yml`, which
pins BitBake, OpenEmbedded-Core, meta-openembedded, meta-raspberrypi, and
meta-rauc. Run `kas` from the parent `reframe-yocto/` workspace so every layer
is checked out beside `meta-reframe` and the build directory is shared.

The main target is the layer-owned `reframe` machine, based on
meta-raspberrypi's 64-bit Raspberry Pi Zero 2 W definition. The image recipe is
`reframe-image-minimal`. Application, service, patch, and configuration files
belong beside their owning recipes under `recipes-*`.

The application recipe is named `reframe-app`; `reframe` cannot be its package
name because the machine name is an active BitBake override. It retains
`reframe` as a compatibility provider, while service and user-facing names are
unchanged.

The application stack depends on Python 3, Picamera2/libcamera, Pillow, NumPy,
qrcode, the Waveshare driver, spidev/GPIO support, and V4L2 tooling. The
dashboard uses FastAPI/Starlette, Uvicorn on loopback port 8000, HTTPX, and
writable reFrame state. Prefer recipes from the pinned upstream layers before
adding local recipes.

## Appliance architecture

The image integrates:

- the reFrame camera application and loopback-backed dashboard;
- Raspberry Pi Camera Module 3 (IMX708) through libcamera and Picamera2;
- a Waveshare 4-inch Spectra 6 e-paper display;
- NetworkManager Wi-Fi provisioning and an Avahi `reframe.local` name;
- PiSugar 3 battery, power-button, shutdown, and RTC support;
- signed A/B system updates using RAUC and U-Boot attempt counters.

The WIC disk layout contains a shared firmware partition, `rootfs_A`,
`rootfs_B`, and a `reframe-data` partition mounted at `/var/lib/reframe`. The
data partition and filesystem grow to fill the SD card on first boot. The two
root filesystems are immutable deployment slots; user and device state belongs
on the shared data partition.

Persistent state includes photos, processed images, settings, staged updates,
NetworkManager connections, `/root`, and SSH host identity. Early boot services
bind persistent directories into the paths expected by their consumers. Keep
new user-owned or device-specific state under `/var/lib/reframe`, never in a
root slot.

The RAUC layout is incompatible with the former single-rootfs disk layout and
requires a full-card flash when migrating from that format.

## System updates

RAUC installs signed verity bundles into the inactive ext4 root slot. U-Boot
tracks boot attempts and can fall back to the previous slot. The shared boot
partition must be mounted at `/boot` from both root filesystems so RAUC and the
U-Boot environment tools operate consistently.

The dashboard streams uploads to `/var/lib/reframe/.updates`, validates bundle
signature and machine compatibility, reports upload and installation progress,
and requests an automatic reboot after a successful installation. The
upstream application's Git updater is disabled.

The repository contains a public development certificate and its private key
to make development bundles self-contained. They are not production
credentials. Production builds must keep signing material outside source
control, provision the matching trust anchor, and override `RAUC_KEY_FILE` and
`RAUC_CERT_FILE`.

The dashboard and setup interface have no user authentication and are intended
for a trusted LAN. Do not expose them directly to an untrusted network.

## Hardware configuration

The machine enables I2C, SPI, and Camera Module 3 support and accepts the
restricted Raspberry Pi Wi-Fi firmware licence. HDMI, unused firmware probes,
and other headless-device overhead are disabled where practical.

The Waveshare display uses `/dev/spidev0.0` plus GPIO17 for reset, GPIO25 for
data/command, GPIO24 for busy, and GPIO18 for panel power. The application owns
normal display access and releases the GPIO/SPI resources when stopped.

The application configures Camera Module 3 HDR when the control is available,
captures through Picamera2, stores an original JPEG and processed PNG, refreshes
the display, and monitors the PiSugar power button over I2C. Missing optional
HDR control does not prevent capture.

PiSugar support is built from pinned Rust sources. `pisugar-server` exposes a
Unix socket at `/run/pisugar/pisugar-server.sock` and a loopback-only
compatibility endpoint at `127.0.0.1:8423`. Programmable-button shell actions
are disabled. reFrame handles short presses for capture; long presses remain
reserved for shutdown behavior. The RTC is restored before reFrame starts and
updated after network time synchronization. Low battery triggers orderly
shutdown, followed by board power cut.

## Boot behavior

The pinned upstream application already overlaps Python imports and camera
discovery, uses systemd readiness notification, prepares the display
asynchronously, avoids an unnecessary autofocus delay, uses Pillow's fast 2x
reduction path, and polls the physical button every 25 ms.

The Yocto integration waits for udev trigger rather than global udev settle,
temporarily selects the `performance` CPU governor through the first capture,
and then restores `ondemand` or `schedutil`. PiSugar RTC restoration runs in
parallel with camera startup and remains active after completion so restarting
the camera service cannot move system time backwards. Networkd, Ofono,
rpcbind, Bluetooth, and unused firmware functions are disabled because they
have no appliance role. NetworkManager exclusively owns networking.

## Networking and provisioning

When no saved connection can be activated, the device starts the open
`reFrame-Setup` access point and captive setup page at `http://10.42.0.1`.
After client Wi-Fi connects, the dashboard is available at
`http://reframe.local`. NetworkManager profiles persist across root-slot
updates.

Wi-Fi may also be provisioned before first boot by placing
`reframe-wifi.json` in the top level of the FAT boot partition:

```json
{
  "ssid": "My Wi-Fi",
  "password": "correct horse battery staple",
  "hidden": false
}
```

The password may be empty for an open network and `hidden` defaults to
`false`. On boot, the importer creates a persistent NetworkManager profile and
erases the plaintext JSON. A parse failure also erases credentials and writes
a non-secret `reframe-wifi.error.txt` explanation to the boot partition.

NetworkManager owns a private dnsmasq instance for access-point sharing. Keep
the system-wide dnsmasq service disabled to avoid a collision on the hotspot
DNS socket. The setup service implements captive-portal DNS/HTTP behavior and
proxies the dashboard after client Wi-Fi is active.

## Development and release policy

`DEBUG_BUILD = "1"` enables developer conveniences including root SSH with an
empty password, UART at 115200 baud with root autologin, diagnostic packages,
and debug compiler optimization. Use it only on a trusted network. Disable it
for release images.

The physical serial console uses Raspberry Pi GPIO14/TX (pin 8), GPIO15/RX
(pin 10), and ground with a 3.3 V UART adapter. Never connect a 5 V UART signal.

Do not fork upstream reFrame or install target software with `pip`. Carry small
integration patches in this layer, pin source revisions, keep
`/usr/lib/reframe` immutable, and keep persistent data under
`/var/lib/reframe`. Separate camera, display, networking, update, and PiSugar
concerns by recipe and service ownership.

Full image builds require substantial local storage (approximately 66 GB).
GitHub metadata checks are useful for quick feedback, but full image builds are
a local developer responsibility.
