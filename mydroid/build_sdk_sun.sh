export YOUR_PATH=`pwd`
export MYDROID=${YOUR_PATH}
export PATH=$PATH:${MYDROID}/prebuilt/linux-x86/toolchain/arm-eabi-4.4.3/bin/
export CROSS_COMPILE=${MYDROID}/prebuilt/linux-x86/toolchain/arm-eabi-4.4.3/bin/arm-eabi-
export PATH=${YOUR_PATH}/../u-boot/tools:$PATH
export JET_PATH=${YOUR_PATH}/..
export YOUR_NAME=`whoami`
export BOARD_TYPE=jet
export JET_PRODUCT=sun
export JET_BOARD_VERSION=sunb3

TMP_FOLDER=${YOUR_PATH}/tmp_sdk

LOG_FILE=sdk.out

EXTRA_PARAMS=PRODUCT-jet_sdk_addon-sdk_addon

cp system/core/rootdir/init_${JET_PRODUCT}.rc system/core/rootdir/init.rc
cp device/ti/${BOARD_TYPE}/init_${JET_PRODUCT}.omap4${BOARD_TYPE}board.rc device/ti/${BOARD_TYPE}/init.omap4${BOARD_TYPE}board.rc

source ./build/envsetup.sh
lunch lean_jet_sun-user

rm -r out/host/linux-x86/obj/SDK_ADDON
rm -r out/target/common/obj/JAVA_LIBRARIES/com.reconinstruments.os_doc_intermediates
rm -r out/target/common/obj/JAVA_LIBRARIES/com.reconinstruments.os_intermediates
rm -r out/target/common/docs/com.reconinstruments.os_doc
rm out/target/common/docs/com.reconinstruments.os_doc-timestamp
rm out/host/linux-x86/sdk_addon/jet_sdk_addon-eng*

make -j$(egrep '^processor' /proc/cpuinfo | wc -l) ${EXTRA_PARAMS} 2>&1 | tee ${JET_PATH}/logs/${LOG_FILE}
#make -j2 ${EXTRA_PARAMS} 2>&1 | tee ${JET_PATH}/logs/${LOG_FILE}

if [ ! $? -eq 0 ] ;then
    echo -e "\033[1;31mERROR: make  ${EXTRA_PARAMS} failed\033[m"
    exit $?
fi

exit 0
