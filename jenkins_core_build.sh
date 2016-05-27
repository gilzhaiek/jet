#!/bin/bash
################################################################################################
#                                       JENKINS BUILD SCRIPT                                   #
################################################################################################
# Author: Nicolas
# Version: 0.1 - dev in progress
# Date: 12/03/2015

#!/bin/bash

#This script is going to build all jet related images and tools from beggining (right after you git clone the jet source code). It has several steps below:
#     1. build initial android file system and copy all the needded tools to ${MY_PATH}/omap4_emmc_files_${BOARD_TYPE}.
#     2. build xloader, uboot kernel and kernel dynamic modules. copy all realeted ko files into ${MY_PATH}/mydroid/out/target/product/jet/system/lib/modules/
#     3. rebuild andoird file system(system.img especially) in order to link all realted ko files. I know it's tricky but that's how omappedia's instruction 
#        works here.
#     4. copy all the images to ${MY_PATH}/omap4_emmc_files_${BOARD_TYPE}.
#     5. regenerate the boot.img (kernel+ramdisk).
#     6. generate cache.img.

export MY_PATH=`pwd`
export MYDROID=${MY_PATH}/mydroid


#All places and variables for building properly
VERSION="0.1 - dev in progress"
BOARD_TYPE=jet
OUT_JET_FOLDER=${MYDROID}/out/target/product/jet
OUT_SYSTEM_FOLDER=${OUT_JET_FOLDER}/system
OUT_SYSTEM_APPS=${OUT_SYSTEM_FOLDER}/app
RECON_APPS=${MYDROID}/vendor/reconinstruments/limo_apps
RECON_APKS=${RECON_APPS}/apks
BUSYBOX_BIN=${MY_PATH}/tools/busybox/busybox
PRODUCT=snow
OP=-

build_apps=0
clean_build=0
build_mydroid_only=0
copy_over=0

echo -e 'Command: \E[34m'"\033[1m$0 $1 [$#]\033[0m"

function help(){
echo -e "\033[1;32m#####################################################################\033[m"
echo -e "\033[1;32m#                         CORE BUILD HELPER                         #\033[m"
echo -e "\033[1;32m#####################################################################\033[m"
echo -e "VERSION: \033[1;34m${VERSION}\033[m"
echo -e "Author: Nicolas"
echo -e "Date: 12/03/2015"
echo -e "-m: trigger the build of mydroid alone"
echo -e "-a: force the applications build"
echo -e "-y: define the jet platform (sunglasses)"
echo -e "-c: define the clean build"
echo -e "-b: Copy over the application"
echo -e "-h: call this help"
exit 0
}


#Parameters Handling
while getopts "mabcyh" var
do
    case "$var" in
        m) echo -e "Build Only set"; build_mydroid_only=1;;
     	a) echo -e "Build app enabled"; build_apps=1;;
    	b) echo -e "Copy over limo apps"; copy_over=1;;
    	c) echo -e "\033[1;32m------ CLEAN Build ------\033[m"; clean_build=1;;
    	y) PRODUCT=sun;OP=${OP}y;;
    	h) help;;
    esac
done

#This is made for copying over the JAS application. WARNING: The link is hardcoded for Jenkins ReconBuilder 
#And zip align them when it's done.
if [ ${copy_over} -eq 0 ]; then
    `rsync -aqz ../Jet_Sun_Apps/apks ${RECON_APKS}`
    echo -e "\033[1;32m------ Running Zipalign on the apps ------\033[m"
    cd ${RECON_APKS}
    ls -l | sed -e 's\.* [0-9][0-9]:[0-9][0-9] \+\g;s\+.*\&&\g;s\k+\k tmp.apk ;mv tmp.apk \g;s\+\zipalign -f 4 \g;' | grep apk > zipalign.sh
    sh zipalign.sh
    rm zipalign.sh
    echo -e "\033[1;32m------ DONE Building Recon Apps ------\033[m"
fi

# Pre Req
echo -e "\033[1;32m Checking if the limo_apps folder \033[m"
if [ ! -e ${RECON_APPS} ]; then
    echo -e "\033[1;31mERROR: ${RECON_APPS} is missing \033[m"
    exit 1
fi

#Remove the old image in order to force the clean build.
echo -e "====================================\n\033[1;32m------ ${PRODUCT} Product ------\033[m\n\033[1;32m------ Deleting Old images ------\033[m"
cd ${MY_PATH}/omap4_emmc_files_${BOARD_TYPE}
rm system.img
if [ ${build_mydroid_only} -eq 0 ]; then
    rm boot.img
    rm recovery.img
    rm userdata.img
fi
echo -e "\033[1;32m------ DONE Deleting Old images ------\033[m"

if [ ${clean_build} -eq 1 ]; then
    echo -e "\033[1;32m------ Removing Entire ${MYDROID}/out directory ------\033[m"
    cd ${MYDROID}
    rm -rf out
    cd ${MY_PATH}
    echo -e "\033[1;32m------ DONE Removing ${MYDROID}/out directory ------\033[m"
fi

if [ ${build_mydroid_only} -eq 0 ]; then
    echo -e "\033[1;32m------ Copying Dummy zImage and u-boot ------\033[m"
    echo "dummy_file" > ${MYDROID}/device/ti/jet/boot/u-boot.bin
    echo "dummy_file" > ${MYDROID}/device/ti/jet/boot/zImage
    echo -e "\033[1;32m------ DONE Copying Dummy zImage and u-boot ------\033[m"

    cd ${MY_PATH}/mydroid
    echo -e "\033[1;32m------ Building Android ------\033[m"
    if [ ${clean_build} -eq 1 ]; then
        ./build_afs.sh ${OP}c
    else
        ./build_afs.sh
    fi

    if [ ! $? -eq 0 ] ;then
        echo -e "\033[1;31mERROR: build_afs.sh\033[m"
        exit $?
    fi

    echo -e "\033[1;32m------ Adding Busybox ------\033[m"
    install -m 777 ${BUSYBOX_BIN} ${OUT_SYSTEM_FOLDER}/bin
    echo -e "\033[1;32m------ DONE Adding Busybox ------\033[m"

    echo -e "\033[1;32m------ DONE Building Android ------\033[m"
fi

echo -e "\033[1;32m------ Copying Recon Apps ------\033[m"
echo -e "\033[1;32m------ Source Folder Apks ------\033[m"
ls -l ${RECON_APKS}/*.apk
cp ${RECON_APKS}/*.apk ${OUT_SYSTEM_APPS}/.
echo -e "\033[1;32m------ Copied Folder Apks ------\033[m"
ls -l ${OUT_SYSTEM_APPS}/*.apk
echo -e "\033[1;32m------ DONE Copying Recon Apps ------\033[m"

if [ ${build_mydroid_only} -eq 0 ]; then
    cd ${MY_PATH}
    echo -e "\033[1;32m------ Build xloader,uboot and kernel ------\033[m"
    if [ ${clean_build} -eq 1 ]; then
        ./core_build_boot.sh ${OP}c
    else
        ./core_build_boot.sh
    fi
    if [ ! $? -eq 0 ] ;then
        echo -e "\033[1;31mERROR: ./core_build_boot.sh Failed\033[m"
        exit $?
    fi
    echo -e "\033[1;32m------ DONE Build xloader,uboot and kernel ------\033[m"
fi

cd ${MY_PATH}/mydroid
echo -e "\033[1;32m------ Rebuild Android to link modules ------\033[m"
./build_afs.sh ${OP}rd

if [ ! $? -eq 0 ] ;then
    echo -e "\033[1;31mERROR: build_afs.sh\033[m"
    exit $?
fi

echo -e "\033[1;32m------ DONE Rebuild Android to link modules ------\033[m"

cd ${MY_PATH}
./copy_to_omap4_emmc.sh ${OP}

if [ ! $? -eq 0 ] ;then
    echo -e "\033[1;31mERROR: core build Failed\033[m"
    exit $?
fi
exit 0

