MY_PATH=`pwd`
IMAGES_PATH=${MY_PATH}/images
UPDATES_PATH=${MY_PATH}/updates
RECON_APPS_PATH=${MY_PATH}/recon_apps
NEW_IMAGE_FILE_NAME=signed-target-files.zip
ADB=${MY_PATH}/adb
FASTBOOT=${MY_PATH}/fastboot
LOG_FILE_NAME=make_ota.log
DROPBOX_FOLDER=/home/upgrader/Dropbox/Public/ota
OMAP4_PATH=${MY_PATH}/omap4_emmc_files_jet

UBOOT_FILE=${OMAP4_PATH}/u-boot.bin
NEW_IMAGE=${IMAGES_PATH}/${NEW_IMAGE_FILE_NAME}
LOG_FILE=${MY_PATH}/logs/${LOG_FILE_NAME}
XML_FILE=${DROPBOX_FOLDER}/snow2_update.xml

function print_header() {
  echo -e '\E[34m'"\033[1m$1\033[0m"
}

trap ctrl_c INT
function ctrl_c() {
  echo "Ctrl-C Pressed Exiting!"
  exit 1;
}

if [[ ! -e ${NEW_IMAGE} ]]; then
  echo -e "\033[1;31mERROR: ${NEW_IMAGE} is MISSING\033[m"
  exit 1;
fi

# Full Update
target_update_file=update.bin
c_cmd="./releasetools/ota_from_target_files -n -v -k security/testkey ${NEW_IMAGE} ${target_update_file}"; echo ${c_cmd}; eval ${c_cmd}
sleep 1
md5sum_target=`md5sum ${target_update_file} | sed -e 's| .*||g'`
target_size=`ls -l ${target_update_file}  | awk '{print $5}'`


