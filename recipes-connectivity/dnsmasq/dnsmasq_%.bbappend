# NetworkManager launches a private dnsmasq instance for each shared/AP
# connection. Keep the binary but do not start the system-wide daemon, which
# would otherwise take port 53 on the hotspot address first.
SYSTEMD_AUTO_ENABLE:${PN} = "disable"

do_install:append() {
    # The upstream recipe installs this for the system-wide daemon. With that
    # daemon disabled, retain systemd-resolved's local DNS stub instead.
    rm -f ${D}${sysconfdir}/systemd/resolved.conf.d/dnsmasq-resolved.conf
}
