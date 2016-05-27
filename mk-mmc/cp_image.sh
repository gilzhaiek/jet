export YOUR_PATH=`pwd`/..
export MYDROID=${YOUR_PATH}/mydroid
#export PATH=$PATH:${YOUR_PATH}/arm-2010q1/bin
export BOARD_TYPE=jet

cp -f ${YOUR_PATH}/u-boot/u-boot.bin .
cp -f ${YOUR_PATH}/x-loader/MLO .
cp -f ${YOUR_PATH}/kernel/android-3.0/arch/arm/boot/uImage .

cp -f ${MYDROID}/out/host/linux-x86/bin/simg2img .
cp -f ${MYDROID}/out/target/product/${BOARD_TYPE}/system.img .
