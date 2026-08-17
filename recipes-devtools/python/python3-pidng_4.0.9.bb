SUMMARY = "Python utility for creating DNG files from raw image data"
HOMEPAGE = "https://github.com/schoolpost/PiDNG"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://LICENSE;md5=092f551bd6b87930ba9ba190f3633972"

PYPI_PACKAGE = "pidng"

inherit pypi setuptools3

SRC_URI[sha256sum] = "560eb008086f8a715fd9e1ab998817a7d4c8500a7f161b9ce6af5ab27501f82c"

RDEPENDS:${PN} += "python3-numpy"
