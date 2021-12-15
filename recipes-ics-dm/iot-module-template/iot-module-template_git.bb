LICENSE = "MIT | Apache-2.0"

LIC_FILES_CHKSUM="\
  file://LICENSE-MIT;md5=afb814368d9110052a22e0da67f027d3 \
  file://LICENSE-APACHE;md5=650e893673eb59696f3d4ee64f6d2357 \
"

FILESEXTRAPATHS:prepend := "${LAYERDIR_ics_dm}/files:"

# version 0.2.2
GITREV = "d08ead8831ffdeec8687eb5235148cbb20f93924"
# TODO change to https uri when public
REPO_URI = "git://git@github.com/ICS-DeviceManagement/iot-module-template.git;protocol=ssh;branch=main;rev=${GITREV}"
SRC_URI = " \
  ${REPO_URI} \
  file://iot-identity-service-keyd.template.toml \
  file://iot-identity-service-identityd.template.toml \
"

S = "${WORKDIR}/git"

DEPENDS = "azure-iot-sdk-c libeis-utils"
RDEPENDS_${PN} = "ca-certificates iot-identity-service"

inherit aziot cmake overwrite_src_uri systemd

EXTRA_OECMAKE += "-DSERVICE_INSTALL_DIR=${systemd_system_unitdir}"

do_install_append() {
  # allow iot-module-template access to device_id secret created by manual provisioning
  install -m 0600 -o aziotks -g aziotks ${WORKDIR}/iot-identity-service-keyd.template.toml ${D}${sysconfdir}/aziot/keyd/config.d/iot-module-template.toml

  # allow iot-module-template provisioning via module identity
  install -m 0600 -o aziotid -g aziotid ${WORKDIR}/iot-identity-service-identityd.template.toml ${D}${sysconfdir}/aziot/identityd/config.d/iot-module-template.toml

  install -d ${D}${systemd_system_unitdir}
  install -m 0644 ${S}/systemd/iot-module-template.timer ${D}${systemd_system_unitdir}/
}

pkg_postinst_${PN}() {
  sed -i "s/@@UID@@/$(id -u iotmodule-c)/" $D${sysconfdir}/aziot/keyd/config.d/iot-module-template.toml
  sed -i -e "s/@@UID@@/$(id -u iotmodule-c)/" -e "s/@@NAME@@/iot-module-template/" $D${sysconfdir}/aziot/identityd/config.d/iot-module-template.toml
}

SYSTEMD_SERVICE_${PN} = "iot-module-template.service iot-module-template.timer"

GROUPADD_PARAM_${PN} += "-r iotmodule-c;"
USERADD_PARAM_${PN} += "--no-create-home -r -s /bin/false -G aziotcs,aziotid,aziotks -g iotmodule-c iotmodule-c;"
