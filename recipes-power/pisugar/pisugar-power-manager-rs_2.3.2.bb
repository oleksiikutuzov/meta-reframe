SUMMARY = "PiSugar battery and RTC management services"
DESCRIPTION = "Pinned PiSugar 2/3 power manager server and shutdown helper"
HOMEPAGE = "https://github.com/PiSugar/pisugar-power-manager-rs"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://LICENSE;md5=1ebbd3e34237af26da5dc08a4e440464"

SRC_URI = " \
    git://github.com/PiSugar/pisugar-power-manager-rs.git;protocol=https;branch=master \
    file://0001-Cargo.lock-sync-workspace-version.patch \
    file://pisugar-server.service \
    file://pisugar-poweroff.service \
    file://config.json \
"
SRCREV = "f8c5eb343a29fd708cee519a5a7ad93858c7d7ea"

inherit cargo cargo-update-recipe-crates pkgconfig systemd

require pisugar-power-manager-rs-crates.inc

DEPENDS += "zstd"

# Use Yocto's zstd instead of compiling zstd-sys' bundled C sources. Besides
# avoiding a duplicate library, this keeps Cargo's vendor path out of DWARF.
export ZSTD_SYS_USE_PKG_CONFIG = "1"

CARGO_BUILD_FLAGS:append = " --package pisugar-server --package pisugar-poweroff"

SYSTEMD_SERVICE:${PN} = "pisugar-server.service pisugar-poweroff.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${B}/target/${CARGO_TARGET_SUBDIR}/pisugar-server ${D}${bindir}/pisugar-server
    install -m 0755 ${B}/target/${CARGO_TARGET_SUBDIR}/pisugar-poweroff ${D}${bindir}/pisugar-poweroff

    install -d ${D}${sysconfdir}/pisugar-server
    install -m 0600 ${UNPACKDIR}/config.json ${D}${sysconfdir}/pisugar-server/config.json

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/pisugar-server.service ${D}${systemd_system_unitdir}/pisugar-server.service
    install -m 0644 ${UNPACKDIR}/pisugar-poweroff.service ${D}${systemd_system_unitdir}/pisugar-poweroff.service
}

CONFFILES:${PN} = "${sysconfdir}/pisugar-server/config.json"
