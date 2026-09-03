FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append = " file://reframe-rauc-dbus.conf"

do_install:append() {
    install -d ${D}${datadir}/dbus-1/system.d
    install -m 0644 ${UNPACKDIR}/reframe-rauc-dbus.conf \
        ${D}${datadir}/dbus-1/system.d/
}

FILES:${PN}-service += "${datadir}/dbus-1/system.d/reframe-rauc-dbus.conf"
