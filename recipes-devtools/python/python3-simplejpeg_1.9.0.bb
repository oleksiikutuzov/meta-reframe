SUMMARY = "Fast JPEG encoding and decoding for Python"
HOMEPAGE = "https://github.com/jfolz/simplejpeg"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://LICENSE;md5=4e1b2459f5d0026e696701cd45973647"

PYPI_PACKAGE = "simplejpeg"

inherit pypi python_setuptools_build_meta

SRC_URI += "file://0001-build-link-against-system-libjpeg-turbo.patch"
SRC_URI[sha256sum] = "5ac7d9489eeb812c2e7ea5c283994a29d9fefdfe5ed7b86c09d485e0dd366689"

DEPENDS += "jpeg python3-cython-native python3-numpy-native"
RDEPENDS:${PN} += "python3-numpy"

do_install:append() {
    sed -i -e 's#Content of: .*/lib/#Content of: lib/#' \
        ${D}${PYTHON_SITEPACKAGES_DIR}/simplejpeg-${PV}.dist-info/licenses/LICENSE
}
