SUMMARY = "Minimal reFrame hardware bring-up image"
DESCRIPTION = "Development image for validating Raspberry Pi Zero 2 W interfaces and Camera Module 3 discovery."

inherit core-image

# Keep SSH, a physical serial console, and diagnostic tools available until
# hardware bring-up is stable. Empty-password login remains limited to serial;
# SSH empty-password and root login are not enabled.
IMAGE_FEATURES = " \
    ssh-server-openssh \
    empty-root-password \
    serial-autologin-root \
"

IMAGE_INSTALL += " \
    packagegroup-core-boot \
    i2c-tools \
    v4l-utils \
"
