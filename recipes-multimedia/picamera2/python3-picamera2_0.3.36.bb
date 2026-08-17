SUMMARY = "Python interface to Raspberry Pi cameras using libcamera"
HOMEPAGE = "https://github.com/raspberrypi/picamera2"
LICENSE = "BSD-2-Clause"
LIC_FILES_CHKSUM = "file://LICENSE;md5=6541a38108b5accb25bd55a14e76086d"

SRC_URI = "git://github.com/raspberrypi/picamera2.git;protocol=https;branch=main"
SRCREV = "bd448421165283c0512b599b7edd961b77dc9d53"
inherit python_setuptools_build_meta

RDEPENDS:${PN} += " \
    kmsxx-python \
    libcamera-pycamera \
    python3-core \
    python3-numpy \
    python3-pidng \
    python3-piexif \
    python3-pillow \
    python3-prctl \
    python3-simplejpeg \
    python3-videodev2 \
"
