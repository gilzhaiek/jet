MY_PATH=`pwd`
IMAGES_PATH=${MY_PATH}/images
CLEAN_IMAGES_PATH=/home/upgrader/jet_upgrade/images
INC_IMAGES_PATH=/home/upgrader/jet_inc_upgrade/images
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
OMAP4_PATH=${MY_PATH}/omap4_emmc_files_jet
X_LOADER=/home/upgrader/dev/x-loader

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
  export device_serial=`${ADB} devices | grep '0\|26\|27\|28' | sed -e 's|device||g;s| ||g'`
}

function update_params() {
  prefix_build=`${ADB} shell getprop | grep ro.build.description | sed -e 's|.*eng.||g;s| test-keys.*||g;s| release-keys.*||g'`
  reference_image_file_name=${prefix_build}_${NEW_IMAGE_FILE_NAME}
  reference_image_file=${CLEAN_IMAGES_PATH}/${reference_image_file_name}

  if [[ ! -e ${reference_image_file} ]]; then
    if [[ -e ${INC_IMAGES_PATH}/${reference_image_file_name} ]]; then
      reference_image_file=${INC_IMAGES_PATH}/${reference_image_file_name}
    fi
  fi

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

function wait_for_adb() {
  sleep 1;
  echo -ne "Waiting for finding device in ADB"
  while true; do
    sleep 1;
    echo -ne "."
    adb_devices=`${ADB} devices`
    if [ `echo $adb_devices | grep -c '015\|26\|27\|28'` -eq 1 ]; then
      break;
    fi
  done
}

function wait_for_full_boot() {
  sleep 1;
  echo -ne "Waiting for device to boot to active partition"
  while true; do
    sleep 1;
    echo -ne "."
    adb_devices=`${ADB} devices`
    if [ `echo $adb_devices | grep -c '015\|26\|27\|28'` -eq 1 ]; then
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
    echo -e "\033[1;31mERROR: ${reference_image_file_name} is MISSING\033[m"
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
    if [[ "$fastboot_serial" == *0* ]] || [[ "$fastboot_serial" == *26* ]] ||[[ "$fastboot_serial" == *27* ]] || [[ "$fastboot_serial" == *28* ]]; then break; fi;
  done
  echo " "
}

function root_device() {
  print_header " - Rooting Device -"
  c_cmd="${ADB} reboot bootloader"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${LOG_FILE};

  echo -ne "Waiting for device to enter fastboot"
  wait_for_fastboot;

  cp root_files/* omap4_emmc_files_jet/

  cd ${OMAP4_PATH}

  cp default_nonsecure.prop default.prop

  ./ramdisk_create_and_flash.sh snow_b1

  cd -
}

function unroot_image() {
  cp root_files/* omap4_emmc_files_jet/

  cd ${OMAP4_PATH}

  sed -e 's|secure=0|secure=1|g' default_nonsecure.prop > default.prop

  ./make_ramdisk.sh snow_b1

  cd -
}

function update_xloader() {
  echo -e "\033[1;31mPlease enter the last 2 numbers on the back of your Jet\033[m"
  echo -e "For example, For JET B2-05, please enter 05:"
  read_user_serial;

  while true; do
    echo -e "New Serial will be \033[1;32m${new_serial}\033[m, is this Ok [\033[1;32mY\033[m]es/[\033[1;32mN\033[m]o?"
    read -n1 ans
    echo " ";
    case $ans in
      [Yy]* ) break;;
      [Nn]* ) echo "Pleae try again:";read_user_serial;;
      * ) echo "Please answer Y/N";;
    esac
  done

  cd ${X_LOADER}

  rm MLO
  echo "#define RECON_DEVICE_ID $new_serial" > include/recon_device_id.h
  ./build_snow.sh
  if [ ! -e MLO ]; then
    echo -e "\033[1;31mERROR: Building X-Loader failed, did you enter a correct serial number?\033[m"
    exit 1;
  fi

  cp MLO ${OMAP4_PATH}/MLO

  cd ${MY_PATH}
}

function reflash_device() {
  update_xloader;
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
if [[ "$device_serial" == *015* ]] || [[ "$device_serial" == *26* ]] ||[[ "$device_serial" == *27* ]] || [[ "$device_serial" == *28* ]]; then
  echo -e " Device Serial\t= \033[1;32m$device_serial\033[m"
else
  echo -e "\033[1;31mERROR: Device not detected by ADB\033[m"
  ${ADB} devices
  exit 1;
fi

function read_user_serial {
  read jet_serial;
  if [[ ! ${#jet_serial} -eq 2 ]]; then
    echo "Serial must be excatlly 2 chars, Please try again:";
    read_user_serial;
    return;
  fi

  while true; do
    echo -e "Is this a [\033[1;32mR\033[m]econ or an [\033[1;31mO\033[m]akley device?"
    read -n1 ans
    echo " ";
    case $ans in
      [Rr]* ) new_serial=2700300${jet_serial}; break;;
      [Oo]* ) new_serial=2800300${jet_serial}; break;;
      * ) echo "Please answer R/O";;
    esac
  done
}

if [[ "$device_serial" != 27* ]] && [[ "$device_serial" != 28* ]]; then
  update_xloader;
  cd ${OMAP4_PATH}

  print_header " - Reflashing X-Loader -"
  c_cmd="${ADB} reboot bootloader"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${LOG_FILE};

  echo -ne "Waiting for device to enter fastboot"
  wait_for_fastboot;

  c_cmd="sudo ./fastboot flash xloader MLO"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${LOG_FILE};
  c_cmd="sudo ./fastboot reboot"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${LOG_FILE};

  wait_for_adb;
fi


#while true; do
#  echo " "
#  echo -e "Would you like to [\033[1;31mR\033[m]OOT your device or [\033[1;32mC\033[m]ONTINUE?"
#  read -n1 ans
#  echo " ";
#  case $ans in
#    [Rr]* ) root_device; exit 0; break;;
#    [Cc]* ) unroot_image; break;;
#    * ) echo "Please answer R/C";;
#  esac
#done

if [[ `./adb shell cat default.prop | grep -c ro.secure=1` -eq 1 ]]; then
  echo -e " \033[1;32mNON-ROOTED\033[m device - start new script..."
  cd ${MY_PATH}
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
  echo -e "Reference Image: ${reference_image_file_name} : \033[1;31mMISSING\033[m"
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

