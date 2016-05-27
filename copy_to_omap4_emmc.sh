export JET_PATH=`pwd`
export MYDROID=${JET_PATH}/mydroid
export BOARD_TYPE=jet
SVN_NUM=`cat ${MYDROID}/vendor/reconinstruments/limo_apps/svn_info | sed -e 's|.*: ||'`
FLASH_FOLDER=${JET_PATH}/omap4_emmc_files_${BOARD_TYPE}
LOCAL_EFS_FOLDER=${FLASH_FOLDER}/efs
LOCAL_EFS_SDCARD=${LOCAL_EFS_FOLDER}/sdcard
LOCAL_EFS_IMAGES=${LOCAL_EFS_FOLDER}/images
LOCAL_EFS_DATA=${LOCAL_EFS_FOLDER}/data
LOCAL_EFS_ID_FILE=${LOCAL_EFS_FOLDER}/recon

LIMO_APPS=${MYDROID}/vendor/reconinstruments/limo_apps
EXTERNAL_FOLDER=${LIMO_APPS}/ExternalStorage/sdcard
STORAGE_DEP_FOLDER=${LIMO_APPS}/StorageDependencies
RECON_APPS=ReconApps
GEODATA_SERVICE=GeodataService
RECON_APPS_FOLDER=${EXTERNAL_FOLDER}/${RECON_APPS}
GEODATA_FILES_FOLDER=${RECON_APPS_FOLDER}/${GEODATA_SERVICE}
DEVICE_SDCARD=/data/media
RAPPS_ZIP=${FLASH_FOLDER}/rapps.zip
GEODATA_ZIP=${FLASH_FOLDER}/geodata.zip
SVN_NUM_FILE=${FLASH_FOLDER}/factory_svn_num.txt
RAPPS_FACTORY_FILE=${LOCAL_EFS_SDCARD}/rapps_${SVN_NUM}.zip
GEODATA_FACTORY_FILE=${LOCAL_EFS_SDCARD}/geodata_${SVN_NUM}.zip

FACTORY_FOLDER="/factory"
FACTORY_EFS_SDCARD=${FACTORY_FOLDER}/sdcard
FACTORY_EFS_IMAGES=${FACTORY_FOLDER}/images
FACTORY_EFS_DATA=${FACTORY_FOLDER}/data

FACTORY_FILE_NAME=factory.bin
LOCAL_FACTORY_FILE=${LOCAL_EFS_IMAGES}/${FACTORY_FILE_NAME}
LOCAL_COMMAND_FILE=${LOCAL_EFS_IMAGES}/command
FACTORY_FILE=${FACTORY_EFS_IMAGES}/${FACTORY_FILE_NAME}

IMAGES_PATH=${JET_PATH}/omap4_emmc_files_jet
SIGNED_TARGET_FILE_NAME=signed-target-files.zip
SIGNED_TARGET_FILE=${IMAGES_PATH}/${SIGNED_TARGET_FILE_NAME}

PRODUCT=jet
KEYS="vendor/recon/security/jet/releasekey"
if [ $# -eq 1 ]
then
    if [[ "$1" == -* ]]
    then
        if [[ "$1" == *x* ]]
        then
            PRODUCT=snow
            KEYS="build/target/product/security/testkey"
        fi
    fi
fi

echo -e "PRODUCT:\t\033[1;32m${PRODUCT}\033[m"
echo -e "SVN_NUM:\t\033[1;32m${SVN_NUM}\033[m"

MAKE_FACTORY_CMD="./build/tools/releasetools/ota_from_target_files -v -n -k ${KEYS} ${SIGNED_TARGET_FILE} ${LOCAL_FACTORY_FILE}"
echo -e "Using:\t\033[1;32m${KEYS}\033[m"

image_is_missing=0

function check_for_file {
    if [ ! -e $1 ]; then
        image_is_missing=1;
        echo -e "\033[4m\033[1;31mError: $1 is missing!!\033[m"
    else
        echo -e "$2 File: \t \033[1;32m`ls -l $1`\033[m"
    fi
}

function make_factory_reset_files {
    if [[ ! -e ${SIGNED_TARGET_FILE} ]]; then
        echo -e "Signed Target File: ${SIGNED_TARGET_FILE} : \033[1;31mMISSING\033[m"
        exit 1;
    fi
    cd ${MYDROID}
    echo ${MAKE_FACTORY_CMD}; eval ${MAKE_FACTORY_CMD}
    echo -e "---------------------------------------------------------------"
    unzip -l ${LOCAL_FACTORY_FILE}
    echo " "

    echo "--wipe_data" > ${LOCAL_COMMAND_FILE}
    echo "--wipe_cache" >> ${LOCAL_COMMAND_FILE}
    echo "--update_package=${FACTORY_FILE}" >> ${LOCAL_COMMAND_FILE}

    cd -
}

function create_sdcard_files {
    echo -e "\033[1;32m------ Creating $PRODUCT SDCard Files ------\033[m"
    rm -rf ${RECON_APPS_FOLDER}
    if [ -e ${RAPPS_ZIP} ]; then rm ${RAPPS_ZIP}; fi
    if [ -e ${GEODATA_ZIP} ]; then rm ${GEODATA_ZIP}; fi

    cd ${STORAGE_DEP_FOLDER}
    bash install_dependencies.bash $PRODUCT build

    cd ${EXTERNAL_FOLDER}
    rm -rf ${GEODATA_SERVICE}
    mv ${GEODATA_FILES_FOLDER} .
    zip -vr ${RAPPS_ZIP} ${RECON_APPS}
    zip -vr ${GEODATA_ZIP} ${GEODATA_SERVICE}
    echo $SVN_NUM > $SVN_NUM_FILE

    cd ${FLASH_FOLDER}
}

function create_efs_image {
    echo -e "\033[1;32m------ Creating Factory Image (efs.img) based on uboot partition size------\033[m"
    rm -rf ${LOCAL_EFS_FOLDER}
    mkdir -p ${LOCAL_EFS_SDCARD}
    mkdir -p ${LOCAL_EFS_IMAGES}
    mkdir -p ${LOCAL_EFS_DATA}

    make_factory_reset_files;

    echo "1" > ${LOCAL_EFS_ID_FILE}

    create_sdcard_files;
    cp ${RAPPS_ZIP} ${RAPPS_FACTORY_FILE}
    cp ${GEODATA_ZIP} ${GEODATA_FACTORY_FILE}
    cp ${SVN_NUM_FILE} ${LOCAL_EFS_SDCARD}/.

    ./make_ext4fs -s -l 320M -a efs efs.img efs/
    echo -e "\033[1;32m------ DONE Creating Factory Image (efs.img) ------\033[m"
}

echo -e "\033[1;32m------ Copying Tools ------\033[m"
cd ${FLASH_FOLDER}
if [ ! -f fastboot ]; then
    cp -f ${MYDROID}/out/host/linux-x86/bin/fastboot .
fi

if [ ! -f adb ]; then
    cp -f ${MYDROID}/out/host/linux-x86/bin/adb .
fi

if [ ! -f mkbootimg ]; then
    cp -f ${MYDROID}/out/host/linux-x86/bin/mkbootimg .
fi

if [ ! -f simg2img ]; then
    cp -f ${MYDROID}/out/host/linux-x86/bin/simg2img .
fi

if [ ! -f make_ext4fs ]; then
    cp -f ${MYDROID}/out/host/linux-x86/bin/make_ext4fs .
fi

# =============================================================================
#Resize Cache partition
# =============================================================================
./resize_img.sh

cd ${FLASH_FOLDER}
echo -e "\033[1;32m------ Copying Images ------\033[m"
cp -f ${JET_PATH}/mydroid/out/target/product/jet/*.img .

check_for_file MLO x-Loader
check_for_file u-boot.bin uBoot
check_for_file boot.img Boot
check_for_file recovery.img Recovery
check_for_file system.img System
check_for_file cache.img Cache
check_for_file userdata.img Userdata

if [ ${image_is_missing} -eq 1 ]; then
    echo -e "\033[4m\033[1;31mError: Copy Failed - image is missing!!\033[m"
    exit 1
fi

create_efs_image
check_for_file efs.img EFS

echo -e "\033[1;32m------ DONE Copying Tools ------\033[m"

if [ ${image_is_missing} -eq 1 ]; then
    echo -e "\033[4m\033[1;31mError: Copy Failed - efs image is missing!!\033[m"
    exit 1
else
    exit 0
fi
