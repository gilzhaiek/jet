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

rm -f ${XML_FILE}
rm -f ${DROPBOX_FOLDER}/*.bin

md5sum_new_image=`md5sum ${NEW_IMAGE}`
md5_short_new_image=`echo ${md5sum_new_image} | cut -c 1-10`
for base_image in `ls ${IMAGES_PATH}/*linux*`
do
  md5sum_base_image=`md5sum ${base_image}`
  md5sum_short_base_image=`echo ${md5sum_base_image} | cut -c 1-10`
  if [[ "${md5_short_new_image}" == "${md5sum_short_base_image}" ]]; then
    new_version=`ls ${base_image} | sed -e "s|${IMAGES_PATH}/||g;s|_.*||g"`
  fi
done

echo '<?xml version="1.0" encoding="ISO-8859-1"?>' > ${XML_FILE}
echo "<updates to_version='${new_version}' >" >> ${XML_FILE}

for base_image in `ls ${IMAGES_PATH}/*linux*`
do
  md5sum_base_image=`md5sum ${base_image}`
  md5sum_short_base_image=`echo ${md5sum_base_image} | cut -c 1-10`
  if [[ "${md5_short_new_image}" == "${md5sum_short_base_image}" ]]; then
    echo -e "Skipping \033[1;32m${base_image}\033[m - this is the new image"
  else
    base_version=`echo ${base_image} | sed -e 's|_signed-target-files.zip||g;s|/home/upgrader/jet_upgrade/images/||g'`
    echo ${base_version}
    target_update_file=${DROPBOX_FOLDER}/${md5sum_short_base_image}_${md5_short_new_image}.bin
    c_cmd="./releasetools/ota_from_target_files -v -k security/testkey -u ${UBOOT_FILE} -i ${base_image} ${NEW_IMAGE} ${target_update_file}"; echo ${c_cmd}; eval ${c_cmd}
    sleep 1
    target_size=`ls -l ${target_update_file}  | awk '{print $5}'`
    md5sum_target=`md5sum ${target_update_file} | sed -e 's| .*||g'`
    dropbox_link=`dropbox puburl ${target_update_file}`
    echo " <update from_version='${base_version}' size='${target_size}' md5sum='${md5sum_target}' link='${dropbox_link}' />" >> ${XML_FILE}
  fi;
done

# Full Update
target_update_file=${DROPBOX_FOLDER}/${md5_short_new_image}.bin
c_cmd="./releasetools/ota_from_target_files -n -v -k security/testkey ${NEW_IMAGE} ${target_update_file}"; echo ${c_cmd}; eval ${c_cmd}
sleep 1
md5sum_target=`md5sum ${target_update_file} | sed -e 's| .*||g'`
target_size=`ls -l ${target_update_file}  | awk '{print $5}'`
dropbox_link=`dropbox puburl ${target_update_file}`
echo " <update from_version='_' size='${target_size}' md5sum='${md5sum_target}' link='${dropbox_link}' />" >> ${XML_FILE}

echo '</updates>' >> ${XML_FILE}


