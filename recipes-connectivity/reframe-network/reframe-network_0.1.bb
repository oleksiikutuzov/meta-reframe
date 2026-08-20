SUMMARY = "reFrame Wi-Fi provisioning hotspot and web UI"
DESCRIPTION = "NetworkManager-based client provisioning with an access-point fallback"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = " \
    file://captive-portal.conf \
    file://reframe-network.py \
    file://reframe-network.service \
    file://reframe-wifi-import.py \
    file://reframe-wifi-import.service \
"
S = "${UNPACKDIR}"

inherit allarch systemd

SYSTEMD_SERVICE:${PN} = "reframe-wifi-import.service reframe-network.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

RDEPENDS:${PN} = " \
    avahi-daemon \
    dnsmasq \
    networkmanager-daemon \
    networkmanager-nmcli \
    networkmanager-wifi \
    python3-core \
    python3-crypt \
    python3-html \
    python3-json \
    python3-logging \
    python3-netclient \
    util-linux-mount \
    util-linux-umount \
"

do_install() {
    install -d ${D}${libexecdir}
    install -m 0755 ${UNPACKDIR}/reframe-network.py ${D}${libexecdir}/reframe-network
    install -m 0755 ${UNPACKDIR}/reframe-wifi-import.py ${D}${libexecdir}/reframe-wifi-import

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/reframe-network.service ${D}${systemd_system_unitdir}/reframe-network.service
    install -m 0644 ${UNPACKDIR}/reframe-wifi-import.service ${D}${systemd_system_unitdir}/reframe-wifi-import.service

    install -d ${D}${sysconfdir}/NetworkManager/dnsmasq-shared.d
    install -m 0644 ${UNPACKDIR}/captive-portal.conf ${D}${sysconfdir}/NetworkManager/dnsmasq-shared.d/50-reframe-captive-portal.conf

    # NetworkManager owns all interfaces in this appliance. The networkd wait
    # helper otherwise waits two minutes for an interface networkd never owns
    # and permanently leaves an otherwise healthy boot in degraded state.
    install -d ${D}${sysconfdir}/systemd/system
    ln -s /dev/null ${D}${sysconfdir}/systemd/system/systemd-networkd-wait-online.service
}

FILES:${PN} += " \
    ${sysconfdir}/NetworkManager/dnsmasq-shared.d/50-reframe-captive-portal.conf \
    ${sysconfdir}/systemd/system/systemd-networkd-wait-online.service \
"
