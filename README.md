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
VIDEO_CAMERA = "1"
RASPBERRYPI_CAMERA_V3 = "1"
LICENSE_FLAGS_ACCEPTED += "synaptics-killswitch"
```

`RASPBERRYPI_CAMERA_V3` selects the Camera Module 3/IMX708 firmware overlay in
`meta-raspberrypi`. Review the restricted Wi-Fi firmware license before using
the accepted `synaptics-killswitch` flag.

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
`reframe-image-minimal-raspberrypi0-2w-64.rootfs.wic.bz2` and its matching
`.wic.bmap` file. After confirming the destination device, flash it with:

```sh
sudo bmaptool copy \
    tmp/deploy/images/raspberrypi0-2w-64/reframe-image-minimal-raspberrypi0-2w-64.rootfs.wic.bz2 \
    /dev/sdX
```

The 2026-08-11 build completed all 5,724 tasks successfully. This confirms the
metadata and image build; physical hardware boot and interface tests remain
required.

GitHub Actions performs fast kas and BitBake metadata checks for pull requests
and pushes to `main`. The complete `yocto-check-layer` signature suite runs only
when the workflow is started manually. Neither mode compiles or boots the image.
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
