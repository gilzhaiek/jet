MY_PATH=`pwd`
IMAGES_PATH=${MY_PATH}/images
UPDATES_PATH=${MY_PATH}/updates
RECON_APPS_PATH=${MY_PATH}/recon_apps
NEW_IMAGE_FILE_NAME=signed-target-files.zip
UPDATE_FILE_NAME=update.bin
COMMAND_FILE_NAME=command
ADB=${MY_PATH}/adb
FASTBOOT=${MY_PATH}/fastboot
LOG_FILE_NAME=make_patch.log
SNOW2_DEVICES_FILE_NAME=snow2_devices.csv
SNOW2_FLASH_HISTORY_FILE_NAME=snow2_flash_history.csv
OMAP4_PATH=${MY_PATH}/omap4_emmc_files_jet_sun

UBOOT_FILE=${OMAP4_PATH}/u-boot.bin
UPDATE_FILE=${MY_PATH}/${UPDATE_FILE_NAME}
COMMAND_FILE=${MY_PATH}/${COMMAND_FILE_NAME}
NEW_IMAGE=${IMAGES_PATH}/${NEW_IMAGE_FILE_NAME}
LOG_FILE=${MY_PATH}/logs/${LOG_FILE_NAME}
SNOW2_DEVICES_FILE=${MY_PATH}/logs/${SNOW2_DEVICES_FILE_NAME}
SNOW2_FLASH_HISTORY_FILE=${MY_PATH}/logs/${SNOW2_FLASH_HISTORY_FILE_NAME}

function print_header() {
  echo -e '\E[34m'"\033[1m$1\033[0m"
}

function update_device_serial() {
  export device_serial=`${ADB} devices | grep '1\|2\|3\|0' | sed -e 's|device||g;s| ||g'`
}

function update_params() {
  prefix_build=`${ADB} shell getprop | grep ro.build.description | sed -e 's|.*eng.||g;s| test-keys.*||g;s| release-keys.*||g'`
  reference_image_file_name=${prefix_build}_${NEW_IMAGE_FILE_NAME}
  reference_image_file=${IMAGES_PATH}/${reference_image_file_name}

  serialno=`${ADB} shell getprop | grep ro.serialno | sed -e 's|.*: \[||g;s|].*||g'`
  bootloader=`${ADB} shell getprop | grep ro.bootloader | sed -e 's|.*: \[||g;s|].*||g'`
  device_info="$serialno, $prefix_build, $bootloader, `date`"

  echo $device_info >> $SNOW2_FLASH_HISTORY_FILE
  if [ `grep -c $serialno $SNOW2_DEVICES_FILE` -eq 1 ]; then
    cat ${SNOW2_DEVICES_FILE} | grep -v $serialno > ${SNOW2_DEVICES_FILE}_tmp;
    mv ${SNOW2_DEVICES_FILE}_tmp $SNOW2_DEVICES_FILE
  fi
  echo $device_info >> $SNOW2_DEVICES_FILE
}

trap ctrl_c INT
function ctrl_c() {
    echo "Ctrl-C Pressed Exiting!"
    exit 1;
}

function wait_for_full_boot() {
  sleep 1;
  echo -ne "Waiting for device to boot to active partition"
  while true; do
    sleep 1;
    echo -ne "."
    adb_devices=`${ADB} devices`
    if [ `echo $adb_devices | grep -c '1\|2\|3\|0'` -eq 1 ]; then
      update_device_serial;
      if [[ `${ADB} shell getprop | grep -c 'sys.boot_completed]: \[1\]'` -eq 1 ]]; then
        echo " "
        echo -e " Device Serial\t= \033[1;32m$device_serial\033[m"
        break;
      fi
    fi
  done

  update_params;
}

function wait_for_finish_fastboot() {
  in_fastboot=0;
  sleep 1;
  echo -ne "Waiting for device to Finish Update"
  while true; do
    sleep 1;
    echo -ne "."
    adb_devices=`${ADB} devices`
    if [ `echo $adb_devices | grep -c recovery` -eq 1 ]; then
      if [ $in_fastboot -eq 0 ]; then
        echo " "
        echo "Entered Fastboot..."
        in_fastboot=1;
      fi
    else
      if [ $in_fastboot -eq 1 ]; then
        echo " "
        echo "Exited Fastboot... "
        echo "NOTE: Turn OFF the device and Turn ON if it get stuck on a black screen"
        break;
      fi
    fi
  done
}

function verify_update() {
  wait_for_full_boot;

  if [[ ! -e ${reference_image_file} ]]; then
    echo -e "\033[1;31mERROR: ${reference_image_file} is MISSING\033[m"
  fi

  diff_result=`diff ${reference_image_file} ${NEW_IMAGE}`
  if [[ "$diff_result" == "" ]]; then
    echo -e "\033[1;32m SUCCESS: Device Updated to ${prefix_build}\033[m"
  else
    echo -e "\033[1;31m FAILURE: Device Updated to ${prefix_build}, which is not latest\033[m"
    while true; do
      echo -e "Would you like to [\033[1;32mE\033[m]xit / [\033[1;31mC\033[m]ontinue to Mass Storage?"
      read -n1 ans
      echo " ";
      case $ans in
        [Cc]* ) break;;
        [Ee]* ) echo "Goodbye and have a gorgeous day!!!"; exit 0; break;;
        * ) echo "Please answer C/E";;
      esac
    done
  fi
}

function wait_for_fastboot() {
  while true; do
    sleep 1;
    echo -ne "."
    fastboot_serial=`${FASTBOOT} devices | sed -e 's|fastboot||g;s| ||g;'`
    if [[ "$fastboot_serial" == *1* ]] || [[ "$fastboot_serial" == *2* ]] ||[[ "$fastboot_serial" == *0* ]] || [[ "$fastboot_serial" == *3* ]]; then break; fi;
  done
  echo " "
}

function root_device() {
  print_header " - Rooting Device -"
  c_cmd="${ADB} reboot bootloader"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${LOG_FILE};

  echo -ne "Waiting for device to enter fastboot"
  wait_for_fastboot;

  cp root_files/* ${OMAP4_PATH}/

  cd ${OMAP4_PATH}

  cp boot.img boot_orig.img

  ./ramdisk_create_and_flash.sh -yr

  cp boot_orig.img boot.img

  cd -
}

function unroot_image() {
  cp root_files/* ${OMAP4_PATH}/

  cd ${OMAP4_PATH}

  sed -e 's|secure=0|secure=1|g' default_nonsecure.prop > default.prop

  ./make_ramdisk.sh snow_b1

  cd -
}

function reflash_device() {
  user_name_ok=0;
  print_header " - Reflashing Device -"
  c_cmd="${ADB} reboot bootloader"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${LOG_FILE};

  echo -ne "Waiting for device to enter fastboot"
  wait_for_fastboot;

  cd ${OMAP4_PATH}
  c_cmd="sudo ./fastboot flash bootloader u-boot.bin"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${LOG_FILE};
  c_cmd="sudo ./fastboot reboot-bootloader"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${LOG_FILE};

  echo -ne "Waiting for device to enter the new u-boot"
  wait_for_fastboot;

  c_cmd="sudo ./fastboot oem format"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${LOG_FILE};

  if [[ "$device_serial" == *1* ]] || [[ "$device_serial" == *2* ]] ||[[ "$device_serial" == *3* ]] || [[ "$device_serial" == *0* ]]; then
    user_name=`echo ${device_serial} | sed -e "s|_1.*||g;s|_2.*||g;s|_3.*||g;s|_0.*||g;"`
    device_serial=`echo ${device_serial} | sed -e "s|.*_||g"`
    while true; do
      echo -e "Excuse me, is your real name in English ${user_name}? [\033[1;32mY\033[m]es / [\033[1;31mN\033[m]o?"
      read -n1 ans
      echo " ";
      case $ans in
        [Yy]* ) user_name_ok=1; break;;
        [Nn]* ) break;;
        * ) echo "Please answer Y/N";;
      esac
    done
  fi

  if [ ${user_name_ok} -eq 0 ]; then
    echo -e "Please enter the unit's owner name without spaces:"
    read user_name
  fi
  new_device_serial=${user_name}_${device_serial}
  echo -e "Thank you \033[1;32m${user_name}\033[m";
  echo -e "Your device serial will be \033[1;32m${new_device_serial}\033[m";

  c_cmd="sudo ./fastboot oem set_rid ${new_device_serial} ${new_device_serial}"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${LOG_FILE};
  c_cmd="sudo ./fastboot_update_all.sh"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${LOG_FILE};

  cd ${MY_PATH}

  verify_update;
  update_params;
}

function wipe_data_cache() {
  print_header " - Performing a SOFT Reset, Wiping Data/Cache -"

  echo " "
  echo "--wipe_data" > ${COMMAND_FILE}
  echo "--wipe_cache" >> ${COMMAND_FILE}

  c_cmd="${ADB} shell mkdir -p /cache/recovery"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${LOG_FILE};
  c_cmd="${ADB} push ${COMMAND_FILE} /cache/recovery/${COMMAND_FILE_NAME}"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${LOG_FILE};
  c_cmd="${ADB} reboot recovery"; eval ${c_cmd} 2>&1 | tee ${LOG_FILE};

  cd ${MY_PATH}

  wait_for_finish_fastboot;
  wait_for_full_boot;
}

function make_patch_and_update() {
  diff_result=`diff ${reference_image_file} ${NEW_IMAGE}`
  if [[ "$diff_result" == "" ]]; then
    while true; do
      echo -e "Core Image on device is \033[1;32mUp-To-Date\033[m, would you like to [\033[1;32mE\033[m]xit / [\033[1;32mR\033[m]eflash / [\033[1;31mC\033[m]ontinue to Mass Storage?"
      read -n1 ans
      echo " ";
      case $ans in
        [Rr]* ) reflash_device; return;;
        [Ee]* ) exit 0; return;;
        [Cc]* ) return;;
        * ) echo "Please answer R/C";;
      esac
    done
  fi

  print_header " - Updating Device -"

  #push_home_apk;

  cd ${MY_PATH}

  md5sum_ref=`md5sum ${reference_image_file} | sed -e "s|${reference_image_file}||g;s| ||g"`
  md5sum_new=`md5sum ${NEW_IMAGE} | sed -e "s|${NEW_IMAGE}||g;s| ||g"`

  new_update_name=${md5sum_ref}_2_${md5sum_new}.bin

  if [ ! -e ${UPDATES_PATH}/${new_update_name} ]; then
    echo ${MAKE_PATCH_CMD}
    eval ${MAKE_PATCH_CMD} 2>&1 | tee ${LOG_FILE}
    mv ${UPDATE_FILE} ${UPDATES_PATH}/${new_update_name}
  fi
  cd ${UPDATES_PATH}
  echo -e "---------------------------------------------------------------"
  echo " ...Updating Jet..."
  unzip -l ${new_update_name}

  echo " "

  echo "--update_package=/cache/${UPDATE_FILE_NAME}" > ${COMMAND_FILE}
  c_cmd="${ADB} push ${new_update_name} /cache/${UPDATE_FILE_NAME}"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${LOG_FILE};
  c_cmd="${ADB} shell mkdir -p /cache/recovery"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${LOG_FILE};
  c_cmd="${ADB} push ${COMMAND_FILE} /cache/recovery/${COMMAND_FILE_NAME}"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${LOG_FILE};
  c_cmd="${ADB} reboot recovery"; eval ${c_cmd} 2>&1 | tee ${LOG_FILE};

  cd ${MY_PATH}

  wait_for_finish_fastboot;

  verify_update;
}

function push_recon_mass_storage() {
  print_header " - Pushing Recon Mass Storage -"
  cd ${RECON_APPS_PATH}/ReconApps
  ${ADB} shell mkdir /mnt/sdcard/ReconApps
  eval `find -type d | sed -e 's|./||g;s|\.||g;/./!d;s|.*|&;cd &;${ADB} push . /mnt/sdcard/ReconApps/&; cd ../|g;s|^|${ADB} shell mkdir /mnt/sdcard/ReconApps/|g;'`
}

if [[ `whoami` != *root* ]]; then
  echo -e "\033[1;31mERROR: Please login as root 'sudo -s'\033[m"
  exit 1;
fi

update_device_serial;
fastboot_serial=$device_serial
if [[ "$device_serial" == *1* ]] || [[ "$device_serial" == *2* ]] ||[[ "$device_serial" == *3* ]] || [[ "$device_serial" == *0* ]]; then
  echo -e " Device Serial\t= \033[1;32m$device_serial\033[m"
else
  echo -e "\033[1;31mERROR: Device not detected by ADB\033[m"
  ${ADB} devices
  exit 1;
fi

while true; do
  echo " "
  echo -e "Would you like to [\033[1;31mR\033[m]OOT your device or [\033[1;32mC\033[m]ONTINUE?"
  read -n1 ans
  echo " ";
  case $ans in
    [Rr]* ) root_device; exit 0; break;;
    [Cc]* ) unroot_image; break;;
    * ) echo "Please answer R/C";;
  esac
done

exit 0

if [[ `./adb shell cat default.prop | grep -c ro.secure=1` -eq 1 ]]; then
  echo -e " \033[1;32mNON-ROOTED\033[m device - start new script..."
  ./update_jet_new.sh
  exit 1;
fi
update_params;

echo -e " Build ID\t= \033[1;32m$prefix_build\033[m"

MAKE_PATCH_CMD="./releasetools/ota_from_target_files -v -k security/testkey -u ${UBOOT_FILE} -i ${reference_image_file} ${NEW_IMAGE} ${UPDATE_FILE}"

missing_images=0
automatic_build=0
jenkins_build=0
skip_to_mass_storage=0;

if [[ "$1" == -* ]]
then
  if [[ "$1" == *a* ]]
  then
    echo -e " ...\033[1;32mAutomatic build and push\033[m"
    automatic_build=1
  fi
  if [[ "$1" == *j* ]]
  then
    echo -e " ...\033[1;32mFor Jenkins - Exit after creation\033[m"
    jenkins_build=1
  fi
fi

echo -e " "
echo -e " Welcome to \033[1;31mRecon Instruments\033[m Patch generator"
echo -e " This script will take the original image and diff it against"
echo -e " a new image. The result of the script will be an update.bin"
echo -e "---------------------------------------------------------------"

if [ `${ADB} shell ls /data/app | grep -c offsetkeyboard` -eq 1 ]; then
  while true; do
    echo " "
    echo -e "\033[1;31mYour Recon Application is in the data partition, we have moved to system\033[m"
    echo -e "It is required to [\033[1;32mR\033[m]eflash / [\033[1;32mW\033[m]ipe your device to continue? Or [\033[1;31mE\033[m]xit]"
    read -n1 ans
    echo " ";
    case $ans in
      [Rr]* ) reflash_device; skip_to_mass_storage=1; break;;
      [Ww]* ) wipe_data_cache; break;;
      [Ee]* ) echo "Goodbye and have a gorgeous day!!!"; exit 0; break;;
      * ) echo "Please answer R/W/E";;
    esac
  done
fi

if [[ ! -e ${reference_image_file} ]]; then
  echo -e "Reference Image: ${reference_image_file} : \033[1;31mMISSING\033[m"
  missing_images=1
  skip_to_mass_storage=0;
else
  echo -e "Reference Image: ${reference_image_file} : \033[1;32mFOUND\033[m"
fi;
if [[ ! -e ${NEW_IMAGE} ]]; then
  echo -e "New Image: ${NEW_IMAGE} : \033[1;31mMISSING\033[m"
  missing_images=1
  skip_to_mass_storage=0;
else
  echo -e "New Image: ${NEW_IMAGE} : \033[1;32mFOUND\033[m"
fi;
if [[ ! -e ${UBOOT_FILE} ]]; then
  echo -e "uBoot File: ${UBOOT_FILE} : \033[1;31mMISSING\033[m"
  missing_images=1
  skip_to_mass_storage=0;
else
  echo -e "uBoot File: ${UBOOT_FILE} : \033[1;32mFOUND\033[m"
fi;

echo -e "---------------------------------------------------------------"

if [ ${skip_to_mass_storage} -eq 0 ]; then
  if [ ${missing_images} -eq 1 ]; then
    while true; do
      echo -e "Reference Images is missing - would you like to [\033[1;32mR\033[m]eflash / [\033[1;32mS\033[m]kip to Mass Storage / [\033[1;31mE\033[m]xit?"
      read -n1 ans
      echo " ";
      case $ans in
        [Rr]* ) reflash_device; break;;
        [Ss]* ) skip_to_mass_storage=1; break;;
        [Ee]* ) echo "Goodbye and have a gorgeous day!!!"; exit 0; break;;
        * ) echo "Please answer R/S/E";;
      esac
    done
  else
    if [ `diff ${reference_image_file} ${NEW_IMAGE} | grep -c differ` -eq 0 ]; then
      while true; do
        echo -e "Reference Images is the same as New Image, Do you want to [\033[1;32mC\033[m]ontinue / [\033[1;32mS\033[m]kip to Mass Storage ?"
        read -n1 ans
        echo " ";
        case $ans in
          [Cc]* ) break;;
          [Ss]* ) skip_to_mass_storage=1; break;;
          * ) echo "Please answer C/S";;
        esac
      done
    fi

    if [ ${skip_to_mass_storage} -eq 0 ]; then
      while true; do
        echo -e "Would you like to [\033[1;32mU\033[m]pdate / [\033[1;32mR\033[m]eflash / [\033[1;32mW\033[m]ipe your device ?"
        read -n1 ans
        echo " ";
        case $ans in
          [Uu]* ) make_patch_and_update; break;;
          [Rr]* ) reflash_device; break;;
          [Ww]* ) wipe_data_cache; break;;
          * ) echo "Please answer U/R/W";;
        esac
      done
    fi
  fi
fi

echo " "
echo -e "---------------------------------------------------------------"

#pull_and_erase_home_apk
#push_recon_mass_storage

cd ${MY_PATH}

