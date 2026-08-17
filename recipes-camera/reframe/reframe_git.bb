SUMMARY = "reFrame camera appliance application"
DESCRIPTION = "Pinned reFrame camera capture and image-processing service"
HOMEPAGE = "https://github.com/kaloyaan/reframe"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=329917d587673b2f419eb6dbaa94f14a"

SRC_URI = " \
    git://github.com/kaloyaan/reframe.git;protocol=https;branch=main \
    file://0001-paths-Separate-immutable-code-from-writable-state.patch \
    file://reframe.service \
    file://reframe-settings.json \
    file://99-reframe-i2c.rules \
"
SRCREV = "5b88b443a9225b7954b57bbb784854c081c6991b"
PV = "0.3+git${SRCPV}"

inherit systemd useradd

USERADD_PACKAGES = "${PN}"
GROUPADD_PARAM:${PN} = "--system i2c"
USERADD_PARAM:${PN} = "--system --no-create-home --home-dir ${localstatedir}/lib/reframe --shell /sbin/nologin --groups video,i2c --user-group reframe"

SYSTEMD_SERVICE:${PN} = "reframe.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

RDEPENDS:${PN} = " \
    bash \
    python3-core \
    python3-numpy \
    python3-picamera2 \
    python3-pillow \
    python3-smbus2 \
    v4l-utils \
"

do_install() {
    install -d ${D}${libdir}/reframe/scripts
    install -m 0755 ${S}/reframe.py ${D}${libdir}/reframe/reframe.py
    install -m 0755 ${S}/scripts/enable_hdr.sh ${D}${libdir}/reframe/scripts/enable_hdr.sh

    install -d ${D}${localstatedir}/lib/reframe/photos
    install -d ${D}${localstatedir}/lib/reframe/dithered_photos
    install -d ${D}${localstatedir}/lib/reframe/.runtime
    install -m 0644 ${UNPACKDIR}/reframe-settings.json ${D}${localstatedir}/lib/reframe/settings.json
    chown -R reframe:reframe ${D}${localstatedir}/lib/reframe

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/reframe.service ${D}${systemd_system_unitdir}/reframe.service

    install -d ${D}${nonarch_base_libdir}/udev/rules.d
    install -m 0644 ${UNPACKDIR}/99-reframe-i2c.rules ${D}${nonarch_base_libdir}/udev/rules.d/99-reframe-i2c.rules
}

FILES:${PN} += " \
    ${libdir}/reframe \
    ${localstatedir}/lib/reframe \
    ${nonarch_base_libdir}/udev/rules.d/99-reframe-i2c.rules \
"

CONFFILES:${PN} = "${localstatedir}/lib/reframe/settings.json"
