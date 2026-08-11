SUMMARY = "Minimal reFrame hardware bring-up image"
DESCRIPTION = "Development image for validating Raspberry Pi Zero 2 W interfaces and Camera Module 3 discovery."

inherit core-image

# Keep SSH and diagnostic tools available until hardware bring-up is stable.
IMAGE_FEATURES = "ssh-server-openssh"

IMAGE_INSTALL += " \
    packagegroup-core-boot \
    i2c-tools \
    v4l-utils \
"
