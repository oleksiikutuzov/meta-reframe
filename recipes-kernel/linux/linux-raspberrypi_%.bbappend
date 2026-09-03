FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append = " file://rauc.cfg"

# The selected slot is appended by U-Boot at runtime.
CMDLINE:remove = "root=/dev/mmcblk0p2"
