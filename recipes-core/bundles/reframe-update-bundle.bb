SUMMARY = "Signed reFrame operating-system update bundle"
LICENSE = "MIT"

inherit bundle

RAUC_BUNDLE_COMPATIBLE = "${MACHINE}"
RAUC_BUNDLE_FORMAT = "verity"
RAUC_BUNDLE_SLOTS = "rootfs"

RAUC_SLOT_rootfs = "reframe-image-minimal"
RAUC_SLOT_rootfs[fstype] = "ext4"

# These repository-owned credentials are deliberately development-only. A
# production build must override both variables with protected signing inputs.
RAUC_KEY_FILE ?= "${THISDIR}/files/development.key.pem"
RAUC_CERT_FILE ?= "${THISDIR}/files/development.cert.pem"
