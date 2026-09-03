# meta-reframe

Yocto/OpenEmbedded layer for the reFrame camera, targeting the Raspberry Pi
Zero 2 W with Camera Module 3, Waveshare Spectra 6 display, PiSugar 3, Wi-Fi
provisioning, persistent user data, and signed RAUC updates.

## Build

Install `kas` 5.3, then run from a parent workspace:

```sh
mkdir reframe-yocto
cd reframe-yocto
git clone https://github.com/oleksiikutuzov/meta-reframe.git
pipx install kas==5.3
KAS_WORK_DIR="$PWD" kas checkout meta-reframe/kas/reframe.yml
KAS_WORK_DIR="$PWD" kas build meta-reframe/kas/reframe.yml
```

The image is written to
`build/tmp/deploy/images/reframe/reframe-image-minimal-reframe.rootfs.wic.bz2`.

Build an update bundle with:

```sh
KAS_WORK_DIR="$PWD" kas shell meta-reframe/kas/reframe.yml \
    -c 'bitbake reframe-update-bundle'
```

The bundle is written to
`build/tmp/deploy/images/reframe/reframe-update-bundle-reframe.raucb`.

## Flash

Flash the `.wic.bz2` image with Balena Etcher, or use `bmaptool`. Replace
`/dev/sdX` with the whole SD-card device; its contents will be overwritten.

```sh
sudo bmaptool copy \
    build/tmp/deploy/images/reframe/reframe-image-minimal-reframe.rootfs.wic.bz2 \
    /dev/sdX
```

Insert the card and power on the device. If needed, join `reFrame-Setup` and
open `http://10.42.0.1` to configure Wi-Fi. The dashboard is then available at
`http://reframe.local`.

## Update

Open **Settings → Software updates** in the dashboard, select the generated
`.raucb`, and upload it. Start the installation and keep the device powered.
The dashboard shows progress and reboots automatically when finished.

See [KNOWLEDGE.md](KNOWLEDGE.md) for architecture and maintenance details.
