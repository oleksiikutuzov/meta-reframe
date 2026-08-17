SUMMARY = "Python ctypes bindings for the Video4Linux2 API"
HOMEPAGE = "https://github.com/raspberrypi/py-videodev2"
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://README.md;beginline=54;endline=54;md5=8ec6c3b23b71fcefcb488c55bbbdec63"

PYPI_PACKAGE = "videodev2"

inherit pypi setuptools3

SRC_URI[sha256sum] = "c34ba70491d148c23a08cbacd8efabeb413cff5baa943a7548ac4abd1eb19e2a"

RDEPENDS:${PN} += "python3-ctypes"
