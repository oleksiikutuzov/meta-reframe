# meta-reframe

`meta-reframe` is an independent Yocto/OpenEmbedded layer for building a
purpose-built Linux image for the open-source reFrame camera hardware. The
first target is a Raspberry Pi Zero 2 W (64-bit) with Camera Module 3 (IMX708),
I2C, and SPI enabled.

The current milestone provides only a hardware bring-up image. It deliberately
does **not** package the reFrame application, dashboard, or PiSugar software.

## Build environment

Yocto Project 6.0 (Wrynose) and matching layer branches are required. This
project uses BitBake and OpenEmbedded-Core directly instead of Poky or a build
orchestration tool. Start with the manual workflow below so that every layer
and configuration change is visible:

```sh
mkdir reframe-yocto
cd reframe-yocto

git clone -b 2.18 https://git.openembedded.org/bitbake
git clone -b wrynose https://git.openembedded.org/openembedded-core
git clone -b wrynose https://git.openembedded.org/meta-openembedded
git clone -b wrynose https://github.com/agherzan/meta-raspberrypi.git
git clone https://github.com/oleksiikutuzov/meta-reframe.git

source openembedded-core/oe-init-build-env build

bitbake-layers add-layer ../meta-openembedded/meta-oe
bitbake-layers add-layer ../meta-openembedded/meta-python
bitbake-layers add-layer ../meta-openembedded/meta-networking
bitbake-layers add-layer ../meta-openembedded/meta-multimedia
bitbake-layers add-layer ../meta-raspberrypi
bitbake-layers add-layer ../meta-reframe
```

Pin the dependency checkouts to the revisions used for the successful build on
2026-08-11:

```sh
git -C bitbake checkout fae9db3168dbff1b8c76fe9c6726a9687ff97514
git -C openembedded-core checkout f09a0f28aeb54ddd90415dd458338e1565bb5a49
git -C meta-openembedded checkout 6bf0d8ad57b33950bab4c7f6c037e1ccb6f6e2eb
git -C meta-raspberrypi checkout f62c67921474370829d24a4fa01ef88543f3906b
```

Only standard Git and BitBake commands are required. Update these revisions
deliberately after validating a new combination; do not silently track moving
branch heads.

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

# Required to install the Pi Zero 2 W Wi-Fi firmware. Review the firmware
# license before accepting this flag.
LICENSE_FLAGS_ACCEPTED += "synaptics-killswitch"
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

On the target, verify boot, interfaces, and camera discovery:

```sh
systemctl is-system-running
ls -l /dev/i2c* /dev/spidev* /dev/video* /dev/v4l-subdev*
dmesg | grep -Ei 'imx708|camera|i2c|spi'
systemd-analyze critical-chain
```

Camera capture through libcamera and Picamera2 belongs to the next milestone.
