export YOUR_PATH=`pwd`
export KERNELDIR=${YOUR_PATH}/../../../../../../../../../../../kernel/android-3.0
export MYDROID=${YOUR_PATH}/../../../../../../../../../../../mydroid
export PATH=$PATH:${MYDROID}/prebuilt/linux-x86/toolchain/arm-eabi-4.4.3/bin/
export CROSS_COMPILE=${MYDROID}/prebuilt/linux-x86/toolchain/arm-eabi-4.4.3/bin/arm-eabi-
export PATH=${MYDROID}/../u-boot/tools:$PATH
export TARGET_PRODUCT="jet"

make -j$(egrep '^processor' /proc/cpuinfo | wc -l) ARCH=arm BUILD=release TARGET_SGX=540 PLATFORM_VERSION=4.0
