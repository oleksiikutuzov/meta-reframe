do_install:append:reframe() {
    cat >> ${D}${sysconfdir}/fstab <<'EOF'
/dev/disk/by-label/boot /boot vfat defaults 0 2
/dev/disk/by-label/reframe-data /var/lib/reframe ext4 defaults,x-systemd.growfs 0 2
EOF
}
