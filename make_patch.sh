MY_PATH=`pwd`
MYDROID_PATH=${MY_PATH}/mydroid
IMAGES_PATH=${MY_PATH}/omap4_emmc_files_jet
ORIGIN_IMAGE_FILE_NAME=origin_signed-target-files.zip
NEW_IMAGE_FILE_NAME=signed-target-files.zip
LOG_FILE=make_patch.log
UPDATE_FILE_NAME=update.bin
COMMAND_FILE_NAME=command

UPDATE_FILE=${MY_PATH}/${UPDATE_FILE_NAME}
COMMAND_FILE=${MY_PATH}/${COMMAND_FILE_NAME}
ORIGIN_IMAGE=${IMAGES_PATH}/${ORIGIN_IMAGE_FILE_NAME}
NEW_IMAGE=${IMAGES_PATH}/${NEW_IMAGE_FILE_NAME}

MAKE_PATCH_CMD="./build/tools/releasetools/ota_from_target_files -v -n -k vendor/recon/security/jet/releasekey -u out/target/product/jet/boot/u-boot.bin -i ${ORIGIN_IMAGE} ${NEW_IMAGE} ${UPDATE_FILE}"

missing_images=0
automatic_build=0
jenkins_build=0

trap ctrl_c INT
function ctrl_c() {
    echo "Ctrl-C Pressed Exiting!"
    exit 1;
}

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

if [[ ! -e ${ORIGIN_IMAGE} ]]; then
  echo -e "Reference Image: ${ORIGIN_IMAGE} : \033[1;31mMISSING\033[m"
  missing_images=1
else
  echo -e "Reference Image: ${ORIGIN_IMAGE} : \033[1;32mFOUND\033[m"
fi;
if [[ ! -e ${NEW_IMAGE} ]]; then
  echo -e "New Image: ${NEW_IMAGE} : \033[1;31mMISSING\033[m"
  missing_images=1
else
  echo -e "Reference Image: ${NEW_IMAGE} : \033[1;32mFOUND\033[m"
fi;

echo -e "---------------------------------------------------------------"

if [ ${missing_images} -eq 1 ]; then
  exit 1;
fi

cd ${MYDROID_PATH}

echo ${MAKE_PATCH_CMD}

eval ${MAKE_PATCH_CMD} 2>&1 | tee ${MY_PATH}/logs/${LOG_FILE}

echo -e "---------------------------------------------------------------"

unzip -l ${UPDATE_FILE}

echo " "
cd ${MY_PATH}

if [ ${jenkins_build} -eq 1 ]; then
    exit 1;
fi

echo "--update_package=/cache/${UPDATE_FILE_NAME}" > ${COMMAND_FILE}

while true; do
    if [ ${automatic_build} -eq 1 ]; then
        ans='u'
    else
        echo -e "Would you like to [\033[1;32mU\033[m]pdate / [\033[1;31mE\033[m]xit you JET?"
        read -n1 ans
        echo " ";
    fi
    case $ans in
        [Uu]* )
#            c_cmd="adb push ${UPDATE_FILE} /cache/${UPDATE_FILE_NAME}"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${MY_PATH}/logs/${LOG_FILE};
#            c_cmd="adb shell mkdir -p /cache/recovery"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${MY_PATH}/logs/${LOG_FILE};
#            c_cmd="adb push ${COMMAND_FILE} /cache/recovery/${COMMAND_FILE_NAME}"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${MY_PATH}/logs/${LOG_FILE};
            c_cmd="adb push ${UPDATE_FILE} /sdcard/ReconApps/cache/${UPDATE_FILE_NAME}"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${MY_PATH}/logs/${LOG_FILE};
            break;;
        [Ee]* ) echo "Goodbye and have a gorgeous day!!!"; exit 0; break;;
        * ) echo "Please answer U/E";;
    esac
done

while true; do
    if [ ${automatic_build} -eq 1 ]; then
        ans='r'
    else
        echo -e "Would you like to [\033[1;32mR\033[m]eplace / [\033[1;31mK\033[m]eep your reference image?"
        read -n1 ans
        echo " ";
    fi
    case $ans in
        [Rr]* )
            c_cmd="mv ${ORIGIN_IMAGE}_prev ${ORIGIN_IMAGE}_pprev"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${MY_PATH}/logs/${LOG_FILE};
            c_cmd="mv ${ORIGIN_IMAGE} ${ORIGIN_IMAGE}_prev"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${MY_PATH}/logs/${LOG_FILE};
            c_cmd="cp ${NEW_IMAGE} ${ORIGIN_IMAGE}"; echo ${c_cmd}; eval ${c_cmd} 2>&1 | tee ${MY_PATH}/logs/${LOG_FILE};
            break;;
        [Kk]* ) echo "Keeping reference image..."; break;;
        * ) echo "Please answer R/K";;
    esac
done

while true; do
    if [ ${automatic_build} -eq 1 ]; then
        ans='y'
    else
        echo " "
        echo -e "Would you like to reboot to initiate an update [\033[1;32mY\033[m]es / [\033[1;31mN\033[m]o ?"
        read -n1 ans
        echo " "
    fi
    case $ans in
        [Yy]* )
            c_cmd="adb reboot recovery"; eval ${c_cmd} 2>&1 | tee ${MY_PATH}/logs/${LOG_FILE};
            break;;
        [Nn]* ) echo "Goodbye and have a gorgeous day!!!"; exit 0; break;;
        * ) echo "Please answer Y/N";;
    esac
done


