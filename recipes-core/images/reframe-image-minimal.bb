SUMMARY = "Minimal reFrame hardware bring-up image"
DESCRIPTION = "Development image for validating Raspberry Pi Zero 2 W interfaces and Camera Module 3 discovery."

inherit core-image

# Deploy an uncompressed disk image for graphical flashers such as Balena Etcher
# in addition to the compressed Raspberry Pi machine artifacts.
IMAGE_FSTYPES:append = " wic"

# DEBUG_BUILD enables bring-up access and tools. Empty-password login remains
# limited to serial; SSH empty-password and root login are not enabled.
IMAGE_FEATURES = "${@oe.utils.vartrue('DEBUG_BUILD', \
    'ssh-server-openssh empty-root-password serial-autologin-root', '', d)}"

REFRAME_DEBUG_PACKAGES = "${@oe.utils.vartrue('DEBUG_BUILD', \
    'i2c-tools systemd-analyze v4l-utils', '', d)}"

IMAGE_INSTALL += " \
    packagegroup-core-boot \
    ${REFRAME_DEBUG_PACKAGES} \
"
