MY_PATH=`pwd`
IMAGES_PATH=${MY_PATH}/images
SUN_IMAGES_PATH=${MY_PATH}/images_sun
CLEAN_PATH=/home/upgrader/jet_upgrade
INC_PATH=/home/upgrader/jet_inc_upgrade
CLEAN_IMAGES_PATH=${CLEAN_PATH}/images
INC_IMAGES_PATH=${INC_PATH}/images
UPDATES_PATH=${MY_PATH}/updates
NEW_IMAGE_FILE_NAME=signed-target-files.zip
UPDATE_FILE_NAME=update.bin
ADB=${MY_PATH}/adb
FASTBOOT=${MY_PATH}/fastboot
LOG_FILE_NAME=make_patch.log
SNOW2_DEVICES_FILE_NAME=snow2_devices.csv
SNOW2_FLASH_HISTORY_FILE_NAME=snow2_flash_history.csv
VERSION_3_0_OMAP4_PATH=${CLEAN_PATH}/omap4_emmc_files_jet_version_3_0
VERSION_3_1_OMAP4_PATH=${CLEAN_PATH}/omap4_emmc_files_jet_version_3_1
VERSION_3_2_OMAP4_PATH=${CLEAN_PATH}/omap4_emmc_files_jet_version_3_2
VERSION_3_2_1_OMAP4_PATH=${CLEAN_PATH}/omap4_emmc_files_jet_version_3_2_1
VERSION_BIDWELL_OMAP4_PATH=${CLEAN_PATH}/omap4_emmc_files_jet_version_bidwell
CLEAN_OMAP4_PATH=${CLEAN_PATH}/omap4_emmc_files_jet
INC_OMAP4_PATH=${INC_PATH}/omap4_emmc_files_jet
SUN_OMAP4_PATH=${CLEAN_PATH}/omap4_emmc_files_jet_sun
X_LOADER=/home/upgrader/dev/x-loader

CLEAN_UBOOT_FILE=${CLEAN_OMAP4_PATH}/u-boot.bin
INC_UBOOT_FILE=${INC_OMAP4_PATH}/u-boot.bin
SUN_UBOOT_FILE=${SUN_OMAP4_PATH}/u-boot.bin
UPDATE_FILE=${MY_PATH}/${UPDATE_FILE_NAME}
TARGET_CACHE_FOLDER=/mnt/sdcard/ReconApps/cache
TARGET_UPDATE_FILE=${TARGET_CACHE_FOLDER}/${UPDATE_FILE_NAME}
CLEAN_NEW_IMAGE=${CLEAN_IMAGES_PATH}/${NEW_IMAGE_FILE_NAME}
INC_NEW_IMAGE=${INC_IMAGES_PATH}/${NEW_IMAGE_FILE_NAME}
SUN_NEW_IMAGE=${SUN_IMAGES_PATH}/${NEW_IMAGE_FILE_NAME}
LOG_FILE=${MY_PATH}/logs/${LOG_FILE_NAME}
SNOW2_DEVICES_FILE=${MY_PATH}/logs/${SNOW2_DEVICES_FILE_NAME}
SNOW2_FLASH_HISTORY_FILE=${MY_PATH}/logs/${SNOW2_FLASH_HISTORY_FILE_NAME}
SERIAL_JET='29'
SERIAL_SNOW='27'
CALIBRATION_DATA_FLAG=0

function print_header() {
  echo -e '\E[34m'"\033[1m$1\033[0m"
}

function update_device_serial() {
  export device_serial=`${ADB} devices | grep '012\|015D\|26\|27\|28\|29' | sed -e 's|device||g;s| ||g'`
}

function update_product_model() {
  export ro_product_model=`${ADB} shell getprop ro.product.model`
  if [[ $ro_product_model == Snow2* ]]; then
    export PRODUCT_MODEL=Snow2
    export KEYS="security/testkey"
  elif [[ $ro_product_model == JET* ]]; then
    export PRODUCT_MODEL=JET
    export KEYS="security/releasekey"
  else
    echo -e "\033[1;31m FAILURE: Unknown ro_product_model=$ro_product_model\033[m"
  fi
}

function update_params() {
  prefix_build=`${ADB} shell getprop | grep ro.build.description | sed -e 's|.*eng.||g;s| test-keys.*||g;s| release-keys.*||g'`
  reference_image_file_name=${prefix_build}_${NEW_IMAGE_FILE_NAME}
  if [[ $PRODUCT_MODEL == JET ]]; then
    reference_image_file=${SUN_IMAGES_PATH}/${reference_image_file_name}
  else
    reference_image_file=${CLEAN_IMAGES_PATH}/${reference_image_file_name}
    if [[ ! -e ${reference_image_file} ]]; then
      if [[ -e ${INC_IMAGES_PATH}/${reference_image_file_name} ]]; then
        reference_image_file=${INC_IMAGES_PATH}/${reference_image_file_name}
      fi
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

function wait_for_full_boot() {
  sleep 1;
  echo -e "\033[1;31m PLEASE - Pair a remote (if you did not) and enable ADB\033[m"
  echo -ne "Waiting for device to boot to active partition"
  aplay -q /home/upgrader/Music/ding.wav
  while true; do
    sleep 1;
    echo -ne "."
    adb_devices=`${ADB} devices`
    if [ `echo $adb_devices | grep -c '012\|015D\|26\|27\|28\|29'` -eq 1 ]; then
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

  diff_result=`diff ${reference_image_file} ${new_image}`
  if [[ "$diff_result" == "" ]]; then
    echo -e "\033[1;32m SUCCESS: Device Updated to ${prefix_build}\033[m"
  else
    echo -e "\033[1;31m FAILURE: Device Updated to ${prefix_build}, which is not latest\033[m"
    while true; do
      echo -e "Would you like to [\033[1;32mE\033[m]xit / [\033[1;31mC\033[m]ontinue?"
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
    if [[ "$fastboot_serial" == *0* ]] || [[ "$fastboot_serial" == *2* ]] ||[[ "$fastboot_serial" == *1* ]]; then break; fi;
  done
  ${FASTBOOT} devices
  echo " "
}

function read_serial_from_user {
  if [[ $1 -eq 9 ]]; then
    echo -e "\033[1;31mPlease enter the full SN on the back of your Battery:\033[m"
  else
    echo -e "\033[1;31mPlease enter the last $1 numbers on the back of your Jet:\033[m"
  fi
  read user_serial;
  if [[ ! ${#user_serial} -eq $1 ]]; then
    echo "Serial must be exactly $1 chars, Please try again:";
    read_serial_from_user $1;
    return;
  fi
}

function read_model() {
  echo -e "Reading the model type";
  if [[ `${ADB} shell getprop | grep -c '\[ro.product.model]: \[JET\]'` -eq 1 ]]; then
    model=${SERIAL_JET}
    name='Jet'
  else
    model=${SERIAL_SNOW}
    name='SNOW'
  fi
  while true; do
    echo -e "The model is \033[1;32m${name}\033[m, is this Ok [\033[1;32mY\033[m]es/[\033[0;31mN\033[m]o?"
    read -n1 ans
    echo " ";
    case $ans in
      [Yy]* ) break;;
      [Nn]* ) echo "Bug Call the debug guy";;
      * ) echo "Please answer Y/N";;
    esac
  done
}

function read_serial {
  read_model;
  while true; do
    echo -e "Is this a [\033[1;36mR\033[m]econ / [\033[1;31mM\033[m]anufacturing device?"
    read -n1 ans
    echo " ";
    case $ans in
      [Mm]* ) read_serial_from_user 9; new_serial=${user_serial};        break;;
      [Rr]* ) read_serial_from_user 2; new_serial=${model}00400${user_serial}; break;;
      * ) echo "Please answer R/M";;
    esac
  done
}



function update_xloader() {
  read_serial;

  while true; do
    echo -e "New Serial will be \033[1;32m${new_serial}\033[m, is this Ok [\033[1;32mY\033[m]es/[\033[1;32mN\033[m]o?"
    read -n1 ans
    echo " ";
    case $ans in
      [Yy]* ) break;;
      [Nn]* ) echo "Pleae try again:";read_serial;;
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

  cp MLO ${omap4_path}/MLO

  cd ${MY_PATH}
}

function save_factory_calibration() {
  echo "Saving factory calibration data (if any)"
  cur_dir=`pwd`
  # remove trailing spaces
  device_serial_trim=${device_serial//[[:blank:]]/}
  mkdir -p calibration_data/$device_serial_trim/sensors
  #pull the factory data
  c_cmd="${ADB} pull /factory/HardwareRevision.txt calibration_data/$device_serial_trim";echo ${c_cmd};eval ${c_cmd};
  c_cmd="${ADB} pull /factory/sensors calibration_data/$device_serial_trim/sensors";echo ${c_cmd};eval ${c_cmd};

  # Set a flag if we found calibration data on the device
  if [ -e calibration_data/$device_serial_trim/sensors/mem.csv ]
    then
      CALIBRATION_DATA_FLAG=1
  fi

  # If our flag is set, recreate the factory partition 
  if [ $CALIBRATION_DATA_FLAG -eq 1 ]
    then
      echo "Found some factory calibration data"
      # Prep a new folder to regenerate the factory image 
      mkdir -p ${omap4_path}/efs_calib/
      cp -R ${omap4_path}/efs/*  ${omap4_path}/efs_calib/
      cp -R calibration_data/$device_serial_trim/* ${omap4_path}/efs_calib/
      cd ${omap4_path}
      echo "Recreate factory partition image to include calibration data"
      # Save the origin efs.img so that we can restore it later
      cp efs.img efs_orig.img
      ./make_ext4fs -s -l 320M -a efs efs.img efs_calib/
      rm -r ${omap4_path}/efs_calib
      cd $cur_dir
    else
      echo "No factory calibration data found"
  fi
}

function reflash_device(){
  update_xloader;
  print_header " - Reflashing Device -"
  save_factory_calibration;
  c_cmd="${ADB} reboot bootloader"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${LOG_FILE};
  echo -ne "Waiting for device to enter fastboot"
  wait_for_fastboot;
  echo "TARGET IMAGES: ${omap4_path}"
  cd ${omap4_path}
  pwd
  c_cmd="sudo ./fastboot flash bootloader u-boot.bin"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${LOG_FILE};
  c_cmd="sudo ./fastboot reboot-bootloader"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${LOG_FILE};
  echo -ne "Waiting for device to enter the new u-boot"
  wait_for_fastboot;
  c_cmd="sudo ./fastboot oem format"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${LOG_FILE};
  c_cmd="sudo ./fastboot_update_all.sh"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${LOG_FILE};
  cd ${MY_PATH}
  if [ $CALIBRATION_DATA_FLAG -eq 1 ]
    then
      echo "Restore factory partition image back to original"
      mv ${omap4_path}/efs_orig.img ${omap4_path}/efs.img
      chmod 777 ${omap4_path}/efs.img
      CALIBRATION_DATA_FLAG=0
  fi
  verify_update;
  update_params;
}

function reflash_device_secured() {
  echo "Please Enter the Password"
  read -s pass;
  if [ $pass = "RECON" ]
  then
    update_xloader;
    print_header " - Reflashing Device -"

    save_factory_calibration;

    c_cmd="${ADB} reboot bootloader"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${LOG_FILE};

    echo -ne "Waiting for device to enter fastboot"
    wait_for_fastboot;

    echo "TARGET IMAGES: ${omap4_path}"
    cd ${omap4_path}
    pwd
    c_cmd="sudo ./fastboot flash bootloader u-boot.bin"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${LOG_FILE};
    c_cmd="sudo ./fastboot reboot-bootloader"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${LOG_FILE};

    echo -ne "Waiting for device to enter the new u-boot"
    wait_for_fastboot;

    c_cmd="sudo ./fastboot oem format"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${LOG_FILE};
    c_cmd="sudo ./fastboot_update_all.sh"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${LOG_FILE};

    cd ${MY_PATH}

    if [ $CALIBRATION_DATA_FLAG -eq 1 ]
      then
        echo "Restore factory partition image back to original"
        mv ${omap4_path}/efs_orig.img ${omap4_path}/efs.img
        chmod 777 ${omap4_path}/efs.img
        CALIBRATION_DATA_FLAG=0
    fi

    verify_update;
    update_params;
  else
    while true; do
      echo -e "You can't reflash Do you want to update?"
      read -n1 ans
      echo " ";
      case $ans in
        [Uu]* ) make_patch_and_update; return;;
        [Ee]* ) exit 0; return;;
        * ) echo "Please answer U/E";;
      esac
    done
  fi
}

function make_patch_and_update() {
  diff_result=`diff ${reference_image_file} ${new_image}`
  if [[ "$diff_result" == "" ]]; then
    while true; do
      echo -e "Core Image on device is \033[1;32mUp-To-Date\033[m, would you like to [\033[1;32mE\033[m]xit / [\033[1;32mR\033[m]eflash / [\033[1;31mF\033[m]orce an Update?"
      read -n1 ans
      echo " ";
      case $ans in
        [Rr]* ) reflash_device; return;;
        [Ee]* ) exit 0; return;;
        [Ff]* ) break;;
        * ) echo "Please answer R/E/F";;
      esac
    done
  fi

  print_header " - Updating Device -"

  #push_home_apk;

  cd ${MY_PATH}

  md5sum_ref=`md5sum ${reference_image_file} | sed -e "s|${reference_image_file}||g;s| ||g"`
  md5sum_new=`md5sum ${new_image} | sed -e "s|${new_image}||g;s| ||g"`

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

  c_cmd="${ADB} push  ${omap4_path}/rapps.zip ${TARGET_CACHE_FOLDER}/rapps.bin"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${LOG_FILE};
  c_cmd="${ADB} push  ${omap4_path}/geodata.zip ${TARGET_CACHE_FOLDER}/geodata.bin"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${LOG_FILE};
  c_cmd="${ADB} push  ${new_update_name} ${TARGET_UPDATE_FILE}"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${LOG_FILE};

  echo -e "\033[1;32m---------------------------------------------------------------\033[m"
  echo -e "\033[1;32m - Unplug your device\033[m"
  echo -e "\033[1;32m - Look at the screen\033[m"
  echo -e "\033[1;32m - There should be a popup which recognize a software update\033[m"
  echo -e "\033[1;32m - After update is complete - plug in your unit and run me again\033[m"
  echo -e "\033[1;32m - to make sure the update has been succesfull\033[m"
  echo -e "\033[1;32m---------------------------------------------------------------\033[m"

  cd ${MY_PATH}
}

echo -e "---------------------------------------------------------------"
update_device_serial;
fastboot_serial=$device_serial
if [[ "$device_serial" == *012* ]] || [[ "$device_serial" == *015D* ]] || [[ "$device_serial" == *26* ]] ||[[ "$device_serial" == *27* ]] || [[ "$device_serial" == *28* ]] || [[ "$device_serial" == *29* ]]; then
  echo -e " Device Serial\t= \033[1;32m$device_serial\033[m"
else
  echo -e "\033[1;31mERROR: Device not detected by ADB\033[m"
  ${ADB} devices
  exit 1;
fi

update_product_model;
echo -e " PRODUCT \t= \033[1;32m$PRODUCT_MODEL\033[m"
echo -e " KEYS    \t= \033[1;32m$KEYS\033[m"
update_params;

echo -e " Build ID\t= \033[1;32m$prefix_build\033[m"

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

while true; do
  echo -e "Would you like to revert to Factory Version to test the update:"
  echo -e "Snow2:"
  echo -e " - [\033[1;32m1\033[m] Revert to \033[1;31mSnow2\033[m Version 3.0 (July 24th 2013)"
  echo -e " - [\033[1;32m2\033[m] Revert to \033[1;31mSnow2\033[m Version 3.1 (Sept 30th 2013)"
  echo -e " - [\033[1;32m3\033[m] Revert to \033[1;31mSnow2\033[m Version 3.2 (Dec 18th 2013)"
  echo -e " - [\033[1;32m4\033[m] Revert to \033[1;31mSnow2\033[m Version 3.2.1 (Jan 30th 2014)"
  echo -e ""
  echo -e "Jet:"
  echo -e " - [\033[1;32m5\033[m] Revert to \033[1;31mJet\033[m Bidwell (Dec 4th 2014)"
  echo -e ""
  echo -e "Don't revert:"
  echo -e " - [\033[1;32mC\033[m]ontinue to update process"
  read -n1 ans
  echo " ";
  case $ans in
    [1]* ) omap4_path=${VERSION_3_0_OMAP4_PATH}; uboot_file=${CLEAN_UBOOT_FILE}; reflash_device; break;;
    [2]* ) omap4_path=${VERSION_3_1_OMAP4_PATH}; uboot_file=${CLEAN_UBOOT_FILE}; reflash_device; break;;
    [3]* ) omap4_path=${VERSION_3_2_OMAP4_PATH}; uboot_file=${CLEAN_UBOOT_FILE}; reflash_device; break;;
    [4]* ) omap4_path=${VERSION_3_2_1_OMAP4_PATH}; uboot_file=${CLEAN_UBOOT_FILE}; reflash_device; break;;
    [5]* ) omap4_path=${VERSION_BIDWELL_OMAP4_PATH}; uboot_file=${CLEAN_UBOOT_FILE}; reflash_device; break;;
    [Cc]* ) break;;
    * ) echo "Please answer 1/2/3/C";;
  esac
done

while true; do
  echo -e "Which target image would you like to update to:"
  echo -e " - [\033[1;32mS\033[m]UNGLASS IMAGE:    \033[1;34m\033[1m`ls -l ${SUN_NEW_IMAGE} | awk '{print $6,$7,$8}'`\033[0m ${SUN_NEW_IMAGE}"
  echo -e " - [\033[1;32mC\033[m]LEAN SNOW2 IMAGE: \033[1;34m\033[1m`ls -l ${CLEAN_NEW_IMAGE} | awk '{print $6,$7,$8}'`\033[0m ${CLEAN_NEW_IMAGE}"
  echo -e " - [\033[1;32mI\033[m]NCRE SNOW2 IMAGE: \033[1;34m\033[1m`ls -l ${INC_NEW_IMAGE} | awk '{print $6,$7,$8}'`\033[0m ${INC_NEW_IMAGE}"
  read -n1 ans
  echo " ";
  case $ans in
    [Ss]* ) new_image=${SUN_NEW_IMAGE}; omap4_path=${SUN_OMAP4_PATH}; uboot_file=${SUN_UBOOT_FILE}; break;;
    [Cc]* ) new_image=${CLEAN_NEW_IMAGE}; omap4_path=${CLEAN_OMAP4_PATH}; uboot_file=${CLEAN_UBOOT_FILE}; break;;
    [Ii]* ) new_image=${INC_NEW_IMAGE}; omap4_path=${INC_OMAP4_PATH}; uboot_file=${INC_UBOOT_FILE}; break;;
    * ) echo "Please answer S/C/I";;
  esac
done

pwd
if [[ ! -e ${reference_image_file} ]]; then
  echo -e "Reference Image: ${reference_image_file} : \033[1;31mMISSING\033[m"
  missing_images=1
  skip_to_mass_storage=0;
else
  echo -e "Reference Image: ${reference_image_file} : \033[1;32mFOUND\033[m"
fi;
if [[ ! -e ${new_image} ]]; then
  echo -e "New Image: ${new_image} : \033[1;31mMISSING\033[m"
  missing_images=1
  skip_to_mass_storage=0;
else
  echo -e "New Image: ${new_image} : \033[1;32mFOUND\033[m"
fi;
if [[ ! -e ${uboot_file} ]]; then
  echo -e "uBoot File: ${uboot_file} : \033[1;31mMISSING\033[m"
  missing_images=1
  skip_to_mass_storage=0;
else
  echo -e "uBoot File: ${uboot_file} : \033[1;32mFOUND\033[m"
fi;

MAKE_PATCH_CMD="./releasetools/ota_from_target_files -v -k ${KEYS} -u ${uboot_file} -i ${reference_image_file} ${new_image} ${UPDATE_FILE}"

echo -e "---------------------------------------------------------------"

if [ ${skip_to_mass_storage} -eq 0 ]; then
  if [ ${missing_images} -eq 1 ]; then
    while true; do
      echo -e "Reference Images is missing - would you like to [\033[1;32mR\033[m]eflash / [\033[1;31mE\033[m]xit?"
      read -n1 ans
      echo " ";
      case $ans in
        [Rr]* ) reflash_device_secured; break;;
        [Ee]* ) echo "Goodbye and have a gorgeous day!!!"; exit 0; break;;
        * ) echo "Please answer R/E";;
      esac
    done
  else
    if [ `diff ${reference_image_file} ${new_image} | grep -c differ` -eq 0 ]; then
      while true; do
        echo -e "Reference Images is the same as New Image, Do you want to [\033[1;32mC\033[m]ontinue / [\033[1;32mE\033[m]xit ?"
        read -n1 ans
        echo " ";
        case $ans in
          [Cc]* ) break;;
          [Ee]* ) echo "Goodbye and have a gorgeous day!!!"; exit 0; break;;
          * ) echo "Please answer C/E";;
        esac
      done
    fi

    while true; do
      echo -e "Would you like to [\033[1;32mU\033[m]pdate / [\033[1;32mR\033[m]eflash your device ?"
      read -n1 ans
      echo " ";
      case $ans in
        [Uu]* ) make_patch_and_update; break;;
        [Rr]* ) reflash_device_secured; break;;
        * ) echo "Please answer U/R";;
      esac
    done
  fi
fi

echo " "
echo -e "---------------------------------------------------------------"

#pull_and_erase_home_apk

cd ${MY_PATH}
aplay -q /home/upgrader/Music/ding.wav
