export YOUR_PATH=`pwd`
export MYDROID=${YOUR_PATH}/../../mydroid
export PATH=$PATH:${MYDROID}/prebuilt/linux-x86/toolchain/arm-eabi-4.4.3/bin/
#blaze

make CROSS_COMPILE=${MYDROID}/prebuilt/linux-x86/toolchain/arm-eabi-4.4.3/bin/arm-eabi- \
ARCH=arm MACH=omap4 BOARD=jet \
clean

make CROSS_COMPILE=${MYDROID}/prebuilt/linux-x86/toolchain/arm-eabi-4.4.3/bin/arm-eabi- \
ARCH=arm MACH=omap4 BOARD=jet