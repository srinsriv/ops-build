SUMMARY = "OpenSwitch Configuration Daemon"
LICENSE = "Apache-2.0"

LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

DEPENDS = "ops-ovsdb ops-cli ops-supportability ops-utils"

RDEPENDS_${PN} = "python-argparse python-json python-ops-ovsdb python-distribute python-pam pam-plugin-radius-auth pam-plugin-radius-chap-auth"

BRANCH ?= "${OPS_REPO_BRANCH}"

SRC_URI = "https://github.com/srinsriv/ops-aaa-utils; \
           file://aaautils.service \
           file://server \
           file://useradd \
         "

SRCREV= ${AUTOREV}

# When using AUTOREV, we need to force the package version to the revision of git
# in order to avoid stale shared states.
PV = "git${SRCPV}"

S = "${WORKDIR}/git"

# Mixing of two classes, the build happens on the source directory.
inherit openswitch cmake setuptools systemd pkgconfig

# Doing some magic here. We are inheriting cmake and setuptools classes, so we
# need to override the exported functions and call both by ourselves.
do_compile() {
     cd ${S}
     distutils_do_compile
     # Cmake compile changes to the B directory
     cmake_do_compile
}

do_install() {
     cd ${S}
     distutils_do_install
     # Cmake compile changes to the B directory
     cmake_do_install
     install -d ${D}${systemd_unitdir}/system
     install -d ${D}/lib
     install -m 0644 ${WORKDIR}/aaautils.service ${D}${systemd_unitdir}/system/
     install -d ${D}${sysconfdir}/raddb
     install -m 0644 ${WORKDIR}/server ${D}${sysconfdir}/raddb/server
     install -d ${D}${sysconfdir}/sudoers.d
     install -m 0644 ${WORKDIR}/useradd ${D}${sysconfdir}/sudoers.d/useradd

     for plugin in $(find ${S}/pam_libraries/tacacs/libtac -name "*.so"); do \
        install -m 777 ${plugin} ${D}/lib
     done

     # Uninstall old libraries
     rm -f ${D}/usr/include/rbac.h
     rm -f ${D}/usr/lib/librbac.so*
     rm -f ${D}/usr/lib/python2.7/site-packages/rbac.py*
}
FILES_${PN} += "/usr/lib/cli/plugins/ /lib"
FILES_${PN}   += "${sysconfdir}/raddb/ ${sysconfdir}/sudoers.d/"
SYSTEMD_PACKAGES = "${PN}"
SYSTEMD_SERVICE_${PN} = "aaautils.service"
