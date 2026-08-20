# Boot and Capture Latency

This file records boot-critical design decisions and reproducible measurements.
Build completion alone does not establish a hardware speed-up; compare target
measurements before and after each change.

## Upstream baseline

Upstream commit `50aef477375a8c40da5e0230748a1d2b89595f3b` reduces boot and capture
latency. It is already an ancestor of the layer's pinned reFrame revision
`5b88b443a9225b7954b57bbb784854c081c6991b`. The packaged application therefore
already contains:

- Python import and camera discovery overlap before HDR and Picamera2 open;
- systemd `Type=notify` readiness after startup capture dispatch;
- early asynchronous display preparation;
- no autofocus settle delay after the first capture in continuous-AF mode;
- Pillow's fast exact-2x reduction path;
- monotonic timeout and performance measurements; and
- 25 ms physical-button polling.

## Yocto integration

The layer ports the applicable host configuration from that commit:

- `reframe.service` waits for udev trigger, not global udev settle;
- the CPU governor is set to `performance` for startup imports and first
  capture, then restored to `ondemand` or `schedutil` after `READY=1` and on
  service stop;
- PiSugar RTC restoration starts alongside reFrame instead of blocking camera
  initialization; and
- generic-image networkd, Ofono, rpcbind, and Bluetooth units are masked because
  NetworkManager owns networking and the appliance has no modem, RPC, or
  Bluetooth role;
- unused Bluetooth, audio, splash, HDMI/TV, display, fan, EEPROM, and boot-delay
  firmware paths are disabled; and
- release cmdlines use `quiet loglevel=3` while debug images retain UART console
  output as a recovery requirement.

The upstream `Before=basic.target` ordering is intentionally not copied. The
camera still starts after local filesystems and udev trigger so its state paths
and media devices exist. RTC restoration is a soft dependency and runs in
parallel; consequently, the first photo can be captured before a valid PiSugar
RTC adjusts the system clock, but capture numbering and image contents are not
affected. The oneshot remains active after successful completion so a later
camera-service restart cannot restore the RTC repeatedly and move system time
backwards.

## Build validation

On 2026-08-20, `reframe`, `rpi-config`, and `rpi-cmdline` built successfully and
the complete `reframe-image-minimal` build completed all 10,405 tasks. The WIC
copied to `/mnt/share/reframe-image-minimal-raspberrypi0-2w-64.rootfs.wic` had:

```text
SHA-256  074addcf7a2a1ca7a5c3a532a7c67b4d3988b62f033cf7348fb9723936363e46
size     885 MB
```

Generated `config.txt` contained the intended effective values, including
`dtparam=audio=off`. The debug `cmdline.txt` retained
`console=serial0,115200` and omitted release-only quiet settings.

## Target measurements

An initial live-device A/B test on 2026-08-20 produced:

| Event | Ordered after RTC | Parallel RTC | Improvement |
| --- | ---: | ---: | ---: |
| reFrame service start | 12.99 s | 9.12 s | 3.87 s |
| Startup capture begins | 18.69 s | 16.00 s | 2.69 s |
| Startup capture completes | 20.10 s | 17.41 s | 2.69 s |

The parallel boot saved 2.69 seconds to the first completed photo. The image
was saved successfully, Wi-Fi reconnected, RTC restore completed concurrently
at 12.08 seconds, all masked units remained inactive, and systemd reported no
failed units. This is a warm A/B validation on one device; retain the cold-boot
repeatability test below.

After flashing, collect three cold boots and report median values rather than a
single best run:

```sh
systemd-analyze
systemd-analyze critical-chain reframe.service reframe-dashboard.service
journalctl -b -o short-monotonic -u reframe -u reframe-dashboard --no-pager
cat /sys/devices/system/cpu/cpufreq/policy0/scaling_governor
systemctl is-active reframe reframe-dashboard reframe-network
```

Verify that startup capture and subsequent button capture still succeed, the
post-start governor is `ondemand` or `schedutil`, dashboard startup follows the
camera readiness notification, UART recovery still works in a debug image, and
no camera/I2C race appears across at least ten cold boots. Record boot-to-startup
capture and button-to-display times here once the display is available.
