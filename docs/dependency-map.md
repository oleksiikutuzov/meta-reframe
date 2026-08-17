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
|   +-- HTTPX
|   +-- Pillow
|   +-- writable reFrame state
|
+-- dashboard_proxy.py
|   +-- Python networking
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
|   +-- Avahi (`reframe.local`)
|
+-- PiSugar 3
    +-- I2C
    +-- pisugar-server
    +-- RTC
    +-- power-button configuration
```

## Integration rules

- Prefer recipes already available in Poky, meta-openembedded, or
  meta-raspberrypi; never install Python packages with `pip` on the target.
- Validate libcamera and Picamera2 capture independently before packaging
  reFrame.
- Keep application code immutable under `/usr/lib/reframe` and persistent state
  under `/var/lib/reframe`.
- Treat Waveshare, networking/dashboard, and PiSugar support as separate,
  testable integration stages.
- Keep Wi-Fi credentials in NetworkManager profiles created on the target; do
  not put them in recipes or application settings.
- Keep the system-wide dnsmasq service disabled. NetworkManager owns a private
  dnsmasq process for each IPv4 shared connection, and a second daemon will
  collide on the hotspot DNS socket.
