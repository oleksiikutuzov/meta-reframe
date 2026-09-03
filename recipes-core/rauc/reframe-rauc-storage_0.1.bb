SUMMARY = "reFrame persistent RAUC storage initialization"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://reframe-grow-data-partition.service"

S = "${UNPACKDIR}"

inherit systemd

RDEPENDS:${PN} = "parted"

SYSTEMD_SERVICE:${PN} = "reframe-grow-data-partition.service"

do_install() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/reframe-grow-data-partition.service \
        ${D}${systemd_system_unitdir}/
}
