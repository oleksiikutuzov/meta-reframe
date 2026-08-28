SUMMARY = "Minimal reFrame hardware bring-up image"
DESCRIPTION = "Development image for validating Raspberry Pi Zero 2 W interfaces and Camera Module 3 discovery."

inherit core-image

# Deploy an uncompressed disk image for graphical flashers such as Balena Etcher
# in addition to the compressed Raspberry Pi machine artifacts.
IMAGE_FSTYPES:append = " wic"

# DEBUG_BUILD enables unrestricted root access for hardware bring-up. Never use
# these empty-password SSH settings in a production or untrusted-network image.
IMAGE_FEATURES = "${@oe.utils.vartrue('DEBUG_BUILD', \
    'ssh-server-openssh allow-empty-password empty-root-password allow-root-login serial-autologin-root', '', d)}"

REFRAME_DEBUG_PACKAGES = "${@oe.utils.vartrue('DEBUG_BUILD', \
    'i2c-tools systemd-analyze v4l-utils', '', d)}"

IMAGE_INSTALL += " \
    packagegroup-core-boot \
    ${REFRAME_DEBUG_PACKAGES} \
    libcamera-apps \
    python3-picamera2 \
    pisugar-power-manager-rs \
    reframe-app \
    reframe-network \
    reframe-pisugar \
"

# These generic-image services have no role on the appliance. Apply masks only
# after package postinst scripts have installed/enabled their units; shipping
# the masks from an ordinary package makes systemd's enable postinst fail.
mask_reframe_unused_services() {
    install -d ${IMAGE_ROOTFS}${sysconfdir}/systemd/system
    for unit in \
        systemd-networkd.service \
        systemd-networkd.socket \
        systemd-networkd-varlink.socket \
        systemd-networkd-resolve-hook.socket \
        ofono.service \
        rpcbind.service \
        rpcbind.socket \
        bluetooth.service \
        hciuart.service \
    ; do
        ln -snf /dev/null ${IMAGE_ROOTFS}${sysconfdir}/systemd/system/$unit
    done
}
ROOTFS_POSTPROCESS_COMMAND += "mask_reframe_unused_services; "
