# meta-reframe

`meta-reframe` is an independent Yocto/OpenEmbedded layer for building a
purpose-built Linux image for the open-source reFrame camera hardware. The
first target is a Raspberry Pi Zero 2 W (64-bit) with Camera Module 3 (IMX708),
I2C, and SPI enabled.

The current milestone provides only a hardware bring-up image. It deliberately
does **not** package the reFrame application, dashboard, or PiSugar software.

## Build environment

Yocto Project 6.0 (Wrynose) and matching layer branches are required. Start
with the manual workflow below so that every layer and configuration change is
visible:

```sh
mkdir reframe-yocto
cd reframe-yocto

git clone -b wrynose https://git.yoctoproject.org/poky
git clone -b wrynose https://git.openembedded.org/meta-openembedded
git clone -b wrynose https://github.com/agherzan/meta-raspberrypi.git
git clone https://github.com/oleksiikutuzov/meta-reframe.git

source poky/oe-init-build-env build

bitbake-layers add-layer ../meta-openembedded/meta-oe
bitbake-layers add-layer ../meta-openembedded/meta-python
bitbake-layers add-layer ../meta-openembedded/meta-networking
bitbake-layers add-layer ../meta-openembedded/meta-multimedia
bitbake-layers add-layer ../meta-raspberrypi
bitbake-layers add-layer ../meta-reframe
```

The branch names above move over time. Record the exact commits used for the
first successful hardware build; these will become the tested revision set
until a pinned `kas` manifest is added.

## Raspberry Pi Zero 2 W configuration

Append the machine, init system, and required hardware interfaces to
`build/conf/local.conf`:

```sh
cat >> conf/local.conf <<'EOF'

# reFrame hardware bring-up
MACHINE = "raspberrypi0-2w-64"
INIT_MANAGER = "systemd"
ENABLE_I2C = "1"
ENABLE_SPI_BUS = "1"
VIDEO_CAMERA = "1"
RASPBERRYPI_CAMERA_V3 = "1"
EOF
```

`RASPBERRYPI_CAMERA_V3` selects the Camera Module 3/IMX708 firmware overlay in
`meta-raspberrypi`. Keep board configuration in `local.conf`; the layer remains
usable with different build configurations.

## Build and deploy

Confirm that metadata parses and build the bring-up image:

```sh
bitbake-layers show-layers
bitbake-layers show-recipes reframe-image-minimal
bitbake reframe-image-minimal
```

Artifacts are written below
`tmp/deploy/images/raspberrypi0-2w-64/`. Inspect the generated files before
flashing; the exact Wic filename and compression depend on the selected layer
revisions and image configuration.

On the target, verify boot, interfaces, and camera discovery:

```sh
systemctl is-system-running
ls -l /dev/i2c* /dev/spidev* /dev/video* /dev/v4l-subdev*
dmesg | grep -Ei 'imx708|camera|i2c|spi'
systemd-analyze critical-chain
```

Camera capture through libcamera and Picamera2 belongs to the next milestone.
