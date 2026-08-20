SUMMARY = "reFrame PiSugar power and RTC policy"
DESCRIPTION = "Layer-owned PiSugar 3 startup and RTC synchronization"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

SRC_URI = " \
    file://reframe-pisugar-sync \
    file://pisugar-rtc-restore.service \
    file://pisugar-rtc-update.service \
    file://reframe-pisugar.conf \
    file://reframe-i2c.conf \
"

S = "${UNPACKDIR}"

inherit allarch systemd

SYSTEMD_SERVICE:${PN} = "pisugar-rtc-restore.service pisugar-rtc-update.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

RDEPENDS:${PN} = " \
    bash \
    coreutils \
    netcat-openbsd \
    pisugar-power-manager-rs \
    systemd \
"

do_install() {
    install -d ${D}${libexecdir}
    install -m 0755 ${UNPACKDIR}/reframe-pisugar-sync ${D}${libexecdir}/reframe-pisugar-sync

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/pisugar-rtc-restore.service ${D}${systemd_system_unitdir}/pisugar-rtc-restore.service
    install -m 0644 ${UNPACKDIR}/pisugar-rtc-update.service ${D}${systemd_system_unitdir}/pisugar-rtc-update.service

    install -d ${D}${systemd_system_unitdir}/reframe.service.d
    install -m 0644 ${UNPACKDIR}/reframe-pisugar.conf ${D}${systemd_system_unitdir}/reframe.service.d/pisugar.conf

    install -d ${D}${sysconfdir}/modules-load.d
    install -m 0644 ${UNPACKDIR}/reframe-i2c.conf ${D}${sysconfdir}/modules-load.d/reframe-i2c.conf
}

FILES:${PN} += "${systemd_system_unitdir}/reframe.service.d/pisugar.conf"
