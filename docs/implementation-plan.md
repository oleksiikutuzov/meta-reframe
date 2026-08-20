# Image Implementation Plan

## Objective and boundaries

Build an independent Yocto/OpenEmbedded appliance image for the 64-bit
Raspberry Pi Zero 2 W, Camera Module 3 (IMX708), Waveshare 4-inch Spectra 6
e-paper display, and PiSugar 3. Use Yocto 6.0 (Wrynose), pin every upstream
layer and source revision, and treat `kaloyaan/reframe` as an externally
packaged application. Carry only small integration patches in this layer.

Maintain two targets:

- `reframe-image-minimal`: development and hardware bring-up.
- `reframe-image`: the eventual camera appliance.

## Release milestones and gates

Milestones are sequential. A release advances only when its exit gate passes;
later features must not be used to conceal an incomplete earlier integration.

### v0.1 — Hardware bring-up

Deliver `reframe-image-minimal` with systemd, debug SSH/UART, I2C, SPI, V4L2
tools, and the IMX708 overlay.

**Exit gate:** Metadata parses, the image builds and flashes, the Pi Zero 2 W
reaches `multi-user.target`, and UART, I2C, SPI, and camera/media device nodes
survive repeated cold boots. Record kernel logs and boot results.

### v0.2 — Independent camera capture

Inventory Wrynose camera recipes before adding local ones. Establish the full
IMX708 → media/V4L2 → libcamera → Picamera2 chain without reFrame.

**Exit gate:** Capture and persist images through both libcamera and Picamera2
after repeated cold boots, with no unresolved camera errors in the journal.

### v0.3 — Packaged reFrame camera

Pin upstream reFrame, install it under `/usr/lib/reframe`, and patch only its
path abstraction. Run it as an unprivileged `reframe` user with writable state
under `/var/lib/reframe` and layer-owned systemd units.

**Exit gate:** The service starts automatically, executes the HDR helper or
fails gracefully, captures an original JPEG, writes processed output, persists
settings across reboot, and never writes into `/usr/lib/reframe`.

### v0.4 — Networking and provisioning

Add Wi-Fi, DHCP, hostname resolution, mDNS, and an explicit provisioning path
outside the application recipe. Do not embed credentials or assume `wlan0`.

**Exit gate:** A new device can be provisioned without rebuilding the image,
reconnects after reboot and access-point loss, advertises `reframe.local`, and
retains UART recovery access when networking is unavailable.

### v0.5 — PiSugar power integration

Build `pisugar-server` and `pisugar-poweroff` from pinned source without vendor
installers. Integrate the PiSugar 3 model, battery state, power button,
anti-mistouch behavior, safe battery poweroff, and RTC. Expose management only
over a local Unix socket and loopback TCP; prefer standard Linux RTC interfaces
where supported.

**Exit gate:** Model and battery queries work, one-button power-on and shutdown
are reliable, time survives loss of network time, and repeated power cycles do
not corrupt persistent reFrame data.

### v0.6 — Dashboard

Add the required FastAPI, Uvicorn, HTTPX, Aiofiles, and QR recipes plus
layer-owned dashboard services. Serve provisioning on the setup AP and proxy
the dashboard through the same port after Wi-Fi connects. Keep upstream
self-update disabled. There is no in-system updater in this milestone; updates
require building and writing a replacement Yocto image.

**Exit gate:** Browsing, original download, display selection, settings
persistence, and QR/access views work over the network without disrupting the
camera service.

### v0.7 — Spectra 6 display

Package the bundled Waveshare driver with reFrame and validate SPI/GPIO panel
control independently before enabling application-driven refresh.

**Exit gate:** A standalone panel test succeeds, reFrame displays its processed
image, repeated refreshes complete without service crashes, and a reboot restores
normal capture-to-display operation.

### v1.0 — Production appliance

Create `reframe-image` with only required runtime content. Separate development
features, retain pinned revisions, and define image-controlled updates rather
than application self-modification.

Image-controlled updates are a future deliverable, not a description of the
current implementation. Until a signed image-level updater with recovery is
implemented, document backup and SD-card reflashing as the only supported
upgrade procedure.

**Exit gate:** A clean production build passes the full hardware test plan,
meets recorded boot/RAM/storage baselines, contains no credentials or development
autologin, survives an extended capture/display/power-cycle test, and has a
documented recovery and image-upgrade procedure.

## Validation gates

Each gate should have reproducible commands, captured results, and the tested
hardware revision in `docs/hardware-test-plan.md`. Metadata CI is necessary but
does not satisfy a build or hardware gate. A release candidate must be built
from clean pinned sources; hardware-dependent releases require real-device
validation rather than emulation alone.

Record cold-boot reliability and hardware results in
`docs/hardware-test-plan.md`. After functionality is stable, measure image size,
RAM, and boot-to-capture/display timings in `docs/boot-analysis.md` before making
boot optimizations. A read-only root filesystem and image-level OTA mechanism
are later milestones.

## Implementation rules

- Prefer existing OE recipes; never use target-side `pip`, virtual environments,
  downloads during `do_install`, or external installer scripts.
- Pin `SRCREV`; do not use `AUTOREV` for production images.
- Keep camera, display, networking, and PiSugar bring-up independently testable.
- Do not bake secrets into the layer or optimize before measuring.
- Use small, milestone-focused commits and keep the first implementation easy to
  diagnose rather than aggressively minimized.
