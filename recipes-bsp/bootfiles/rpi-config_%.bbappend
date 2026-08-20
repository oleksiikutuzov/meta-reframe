# raspberrypi0-2w-64 inherits raspberrypi3-64, whose recipe-level deploy append
# unconditionally enables HDMI audio after RPI_EXTRA_CONFIG. reFrame is a
# headless appliance, so make the final effective setting explicit.
do_deploy:append:raspberrypi0-2w-64() {
    sed -i 's/^dtparam=audio=on$/dtparam=audio=off/' \
        ${DEPLOYDIR}/${BOOTFILES_DIR_NAME}/config.txt
}
