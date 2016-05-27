export KERNEL_DIR=`pwd`
export JET_PATH=`pwd`/../..
export MYDROID=${JET_PATH}/mydroid
export PATH=$PATH:${MYDROID}/prebuilt/linux-x86/toolchain/arm-eabi-4.4.3/bin/
export CROSS_COMPILE=${MYDROID}/prebuilt/linux-x86/toolchain/arm-eabi-4.4.3/bin/arm-eabi-
export PATH=${MYDROID}/../u-boot/tools:$PATH
export KLIB=${KERNEL_DIR}
export KLIB_BUILD=${KERNEL_DIR}

#${MYDROID}/out/target/product/blaze/system/lib/modules/

if [ -n "$1" ]; then
    mkdir -p $1
fi

echo "build wifi module---------------------------"
cd ${MYDROID}/hardware/ti/wlan/mac80211/compat_wl12xx
make -j$(egrep '^processor' /proc/cpuinfo | wc -l)  ARCH=arm 2>&1 | tee ${JET_PATH}/logs/wilink_wifi.out

if [ -n "$1" ]; then
    #cp ./compat/compat.ko $OUT
    cp ./net/wireless/cfg80211.ko $1
    cp ./net/mac80211/mac80211.ko $1
    cp ./drivers/net/wireless/wl12xx/wl12xx.ko $1
    cp ./drivers/net/wireless/wl12xx/wl12xx_sdio.ko $1
fi

echo "build bluetooth module---------------------"
cd ${MYDROID}/hardware/ti/wpan/bluetooth-compat
make -j$(egrep '^processor' /proc/cpuinfo | wc -l)  ARCH=arm 2>&1 | tee ${JET_PATH}/logs/wilink_bt.out

if [ -n "$1" ]; then
    cp ./compat/sch_fq_codel.ko $1
    cp ./compat/sch_codel.ko $1
    cp ./compat/compat.ko $1
    cp ./drivers/bluetooth/btwilink.ko $1
    cp ./net/bluetooth/bnep/bnep.ko $1
    cp ./net/bluetooth/hidp/hidp.ko $1
    cp ./net/bluetooth/rfcomm/rfcomm.ko $1
    cp ./net/bluetooth/bluetooth.ko $1
    #find . -name \*.ko |while read f; do cp $f $1; done
fi

echo "build ss1 module---------------------"
cd $KERNEL_DIR
make -j$(egrep '^processor' /proc/cpuinfo | wc -l) M=${MYDROID}/external/ss1/tty_hci_driver \
    ARCH=arm 2>&1 | tee ${JET_PATH}/logs/ss1_bt.out
 
make -j$(egrep '^processor' /proc/cpuinfo | wc -l) M=${MYDROID}/external/ss1/VNET/source \
    ARCH=arm 2>&1 | tee -a ${JET_PATH}/logs/ss1_bt.out

if [ -n "$1" ]; then
    cp ${MYDROID}/external/ss1/tty_hci_driver/tty_hci.ko $1
    cp ${MYDROID}/external/ss1/VNET/source/SS1VNETM.ko $1
fi

echo "build GPS module---------------------"
cd ${MYDROID}/hardware/ti/gnss
make -j$(egrep '^processor' /proc/cpuinfo | wc -l)  ARCH=arm 2>&1 | tee ${JET_PATH}/logs/wilink_gnss.out

if [ -n "$1" ]; then
	cp gps_drv.ko $1
fi

cd ${KERNEL_PATH}

