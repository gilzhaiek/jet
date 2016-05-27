export YOUR_PATH=`pwd`
export MYDROID=${YOUR_PATH}
export JET_PATH=`pwd`/..
export PATH=$PATH:${MYDROID}/prebuilt/linux-x86/toolchain/arm-eabi-4.4.3/bin/
export CROSS_COMPILE=${MYDROID}/prebuilt/linux-x86/toolchain/arm-eabi-4.4.3/bin/arm-eabi-
export PATH=${YOUR_PATH}/../u-boot/tools:$PATH

if [ -n "$1" ]; then
    echo "Build modules--------------------------"
    source ./build/envsetup.sh
    lunch lean_jet_snow-user
    make -j$(egrep '^processor' /proc/cpuinfo | wc -l) $1 2>&1 |tee ${JET_PATH}/logs/afs_modules.out
else
    echo "err: need to provide module name--------------------------"
fi
