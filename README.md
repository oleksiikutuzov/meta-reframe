# meta-reframe

`meta-reframe` is an independent Yocto/OpenEmbedded layer for building a
purpose-built Linux image for the open-source reFrame camera hardware. The
first target is a Raspberry Pi Zero 2 W (64-bit) with Camera Module 3 (IMX708),
I2C, and SPI enabled.

The current milestone provides only a hardware bring-up image. It deliberately
does **not** package the reFrame application, dashboard, or PiSugar software.

## Build environment

Yocto Project 6.0 (Wrynose) and `kas` 5.3 are required. The canonical
configuration is `kas/reframe.yml`; it pins BitBake, OpenEmbedded-Core,
meta-openembedded, and meta-raspberrypi to the revisions used for the successful
build on 2026-08-11.

```sh
mkdir reframe-yocto
cd reframe-yocto
git clone https://github.com/oleksiikutuzov/meta-reframe.git
cd meta-reframe

pipx install kas==5.3
kas checkout kas/reframe.yml
```

`kas checkout` resolves the pinned repositories and writes `build/conf`. To
inspect or debug the resulting BitBake environment directly, use:

```sh
kas shell kas/reframe.yml
```

## Raspberry Pi Zero 2 W configuration

The kas configuration selects `raspberrypi0-2w-64`, systemd, I2C, SPI, and the
Camera Module 3/IMX708 overlay. Its effective `local.conf` settings are:

```sh
INIT_MANAGER = "systemd"
ENABLE_I2C = "1"
ENABLE_SPI_BUS = "1"
ENABLE_UART = "1"
VIDEO_CAMERA = "1"
RASPBERRYPI_CAMERA_V3 = "1"
LICENSE_FLAGS_ACCEPTED += "synaptics-killswitch"
```

`RASPBERRYPI_CAMERA_V3` selects the Camera Module 3/IMX708 firmware overlay in
`meta-raspberrypi`. Review the restricted Wi-Fi firmware license before using
the accepted `synaptics-killswitch` flag.

## Serial bring-up console

The development image enables the UART at 115200 baud and automatically logs
in as root on the physical serial console. Connect a 3.3 V USB-to-UART adapter
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
kas build kas/reframe.yml
```

For quicker metadata checks, run:

```sh
kas shell kas/reframe.yml -c 'bitbake-layers show-layers'
kas shell kas/reframe.yml -c 'bitbake-layers show-recipes reframe-image-minimal'
kas shell kas/reframe.yml -c 'bitbake -p reframe-image-minimal'
```

Artifacts are written below
`tmp/deploy/images/raspberrypi0-2w-64/`. The tested configuration produces the
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
    tmp/deploy/images/raspberrypi0-2w-64/reframe-image-minimal-raspberrypi0-2w-64.rootfs.wic.bz2 \
    /dev/sdX
```

The 2026-08-11 build completed all 5,724 tasks successfully. This confirms the
metadata and image build; physical hardware boot and interface tests remain
required.

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

Camera capture through libcamera and Picamera2 belongs to the next milestone.

## Contributing

Send patches through GitHub pull requests. Keep each patch focused on one
milestone and state the validation performed. The layer maintainer is Oleksii
Kutuzov <oleksii.kutuzov@icloud.com>.
