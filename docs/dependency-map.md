# reFrame Dependency Map

This map records the expected integration boundaries. The initial image covers
system boot, I2C/SPI tooling, kernel camera discovery, and headless libcamera
capture; application dependencies must be verified against the selected
Wrynose layers before they are added.

```text
reFrame
|
+-- reframe.py
|   +-- Python 3
|   +-- Picamera2
|   |   +-- libcamera
|   |       +-- kernel media/V4L2 + IMX708
|   +-- Pillow
|   +-- NumPy
|   +-- qrcode
|   +-- FastAPI/uvicorn (optional API path)
|   +-- Waveshare e-paper Python driver
|   |   +-- spidev
|   |   +-- GPIO support
|   +-- v4l2-ctl
|   +-- iproute2 (`ip`)
|
+-- dashboard.py
|   +-- FastAPI / Starlette
|   +-- Uvicorn on loopback TCP 8000
|   +-- HTTPX
|   +-- Pillow
|   +-- writable reFrame state
|
+-- Camera Module 3
|   +-- IMX708 kernel support
|   +-- libcamera
|   +-- Picamera2
|
+-- Waveshare Spectra 6
|   +-- SPI
|   +-- GPIO
|
+-- Wi-Fi provisioning
|   +-- NetworkManager
|   |   +-- wpa_supplicant
|   |   +-- nftables
|   |   +-- private dnsmasq instance for IPv4 shared/AP mode
|   +-- reframe-network
|   |   +-- Python standard-library HTTP server
|   |   +-- nmcli
|   |   +-- captive-portal DNS and HTTP redirects
|   |   +-- dashboard reverse proxy while client Wi-Fi is active
|   +-- reframe-wifi-import
|   |   +-- one-shot boot-partition credential import
|   |   +-- NetworkManager profile creation and plaintext-file erasure
|   +-- Avahi (`reframe.local`)
|
+-- PiSugar 3
    +-- I2C
    +-- pisugar-server (pinned Rust source build)
    |   +-- Unix socket under /run/pisugar
    |   +-- loopback-only TCP compatibility API
    |   +-- battery, RTC, shutdown, and command APIs
    +-- pisugar-poweroff
    +-- reframe-pisugar
        +-- boot-time RTC restore before reFrame
        +-- RTC update after network time synchronization
        +-- low-battery shutdown policy
    +-- reFrame direct power-button state polling
```

## Integration rules

- Prefer recipes already available in Poky, meta-openembedded, or
  meta-raspberrypi; never install Python packages with `pip` on the target.
- Validate libcamera and Picamera2 capture independently before packaging
  reFrame.
- Keep application code immutable under `/usr/lib/reframe` and persistent state
  under `/var/lib/reframe`.
- There is no in-system update mechanism yet. Do not enable the upstream Git
  updater or modify files under `/usr/lib/reframe` on a running device. Current
  upgrades require a newly built image and an explicit backup/reflash workflow;
  a future OTA design must use authenticated, signed image artifacts and
  include failure recovery.
- Treat Waveshare, networking/dashboard, and PiSugar support as separate,
  testable integration stages.
- Keep Wi-Fi credentials in NetworkManager profiles created on the target. The
  optional plaintext boot-partition JSON is input-only and must be erased after
  import; never put credentials in recipes or application settings.
- Keep the system-wide dnsmasq service disabled. NetworkManager owns a private
  dnsmasq process for each IPv4 shared connection, and a second daemon will
  collide on the hotspot DNS socket.
- Keep PiSugar control APIs local to the device. Do not enable its web server or
  bind its TCP protocol to a non-loopback address.
- Keep capture-button policy in reFrame. Its direct I2C access is limited to
  reading the physical power-button state; PiSugar tap actions must not execute
  arbitrary shell commands.
