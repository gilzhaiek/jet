#!/bin/bash
export JET_PATH=`pwd`
export MYDROID=`pwd`/mydroid
export BOARD_TYPE=jet

UBOOT_FILE=${JET_PATH}/u-boot/u-boot.bin
XLOADER_FILE=${JET_PATH}/x-loader/MLO
ZIMAGE_FILE=${JET_PATH}/kernel/android-3.0/arch/arm/boot/zImage

OP=$1
PRODUCT=snow

echo -e 'Command: \E[34m'"\033[1m$0 $1 [$#]\033[0m"

if [ $# -eq 1 ]
then
    if [[ "$1" == -* ]]
    then
        if [[ "$1" == *c* ]]
        then
            echo -e "\033[1;32m------ CLEAN Build Boot------\033[m"
        fi
        if [[ "$1" == *x* ]]
        then
            PRODUCT=snow
        fi
        if [[ "$1" == *y1* ]]
        then
            PRODUCT=sun_b1
        elif [[ "$1" == *y* ]]
        then
            PRODUCT=sun
        fi
    fi
fi
echo -e "\033[1;32m------ ${PRODUCT} Product ------\033[m"

echo -e "\033[1;32m------ Deleting Old images ------\033[m"
cd ${JET_PATH}/omap4_emmc_files_${BOARD_TYPE}
rm MLO
rm u-boot.bin
rm zImage
echo -e "\033[1;32m------ DONE Deleting Old images ------\033[m"

echo -e "\033[1;32m------ Building x-Loader / MLO ------\033[m"
cd ${JET_PATH}/x-loader/
rm ${XLOADER_FILE}
./build_${PRODUCT}.sh $OP
if [ ! -e ${XLOADER_FILE} ]; then
    echo -e "\033[1;31mERROR: ${XLOADER_FILE} doesn't exists\033[m"
    exit 1;
fi
echo -e "\033[1;32m------ DONE Building x-Loader / MLO ------\033[m"

echo -e "\033[1;32m------ Building uBoot ------\033[m"
cd ${JET_PATH}/u-boot/
rm ${UBOOT_FILE}
./build_${PRODUCT}.sh $OP
if [ ! -e ${UBOOT_FILE} ]; then
    echo -e "\033[1;31mERROR: ${UBOOT_FILE} doesn't exists\033[m"
    exit 1;
fi
echo -e "\033[1;32m------ DONE Building uBoot ------\033[m"

echo -e "\033[1;32m------ Building Kernel ------\033[m"
cd ${JET_PATH}/kernel/android-3.0
rm ${ZIMAGE_FILE}
if [[ "${OP}" == *c* ]]
then
    echo -e "\033[1;32m------ Cleaning Kernel ------\033[m"
    ./distclean.sh
fi

echo -e "\033[1;32m------ Defconfig Kernel ------\033[m"
./def_${PRODUCT}_config.sh

echo -e "\033[1;32m------ Building zImage ------\033[m"
./build_kernel.sh
if [ ! -e ${ZIMAGE_FILE} ]; then
    echo -e "\033[1;31mERROR: ${ZIMAGE_FILE} doesn't exists\033[m"
    exit 1;
fi

echo -e "\033[1;32m------ Building Kernel Modules ------\033[m"
./build_modules.sh

echo -e "\033[1;32m------ Building Wilink 7 ------\033[m"
./build_wilink.sh ${MYDROID}/out/target/product/jet/system/lib/modules/

cd ${JET_PATH}
echo -e "\033[1;32m------ Copying zImage to mydroid/device/ti/jet/boot/zImage ------\033[m"
cp ${JET_PATH}/kernel/android-3.0/arch/arm/boot/zImage ${MYDROID}/device/ti/jet/boot/zImage

echo -e "\033[1;32m------ Copying boot images ------\033[m"
cp ${UBOOT_FILE} ${MYDROID}/device/ti/jet/boot/u-boot.bin
cd ${JET_PATH}/omap4_emmc_files_${BOARD_TYPE}
cp ${XLOADER_FILE} .
cp ${UBOOT_FILE} .
cp ${ZIMAGE_FILE} .

cd ${JET_PATH}

exit 0


