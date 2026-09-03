SUMMARY = "reFrame cross-slot system state"
DESCRIPTION = "Persist the root home and SSH server identity across A/B updates"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = " \
    file://reframe-persistent-state \
    file://reframe-persistent-state.service \
"
S = "${UNPACKDIR}"

inherit allarch systemd

SYSTEMD_SERVICE:${PN} = "reframe-persistent-state.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

RDEPENDS:${PN} = " \
    openssh-keygen \
    util-linux-mount \
    util-linux-umount \
"

do_install() {
    install -d ${D}${libexecdir} ${D}${systemd_system_unitdir}
    install -m 0755 ${UNPACKDIR}/reframe-persistent-state \
        ${D}${libexecdir}/reframe-persistent-state
    install -m 0644 ${UNPACKDIR}/reframe-persistent-state.service \
        ${D}${systemd_system_unitdir}/reframe-persistent-state.service
}
