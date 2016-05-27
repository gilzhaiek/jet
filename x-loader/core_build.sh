export X_LOADER_PATH=`pwd`
export JET_PATH=${X_LOADER_PATH}/..
export UBOOT_PATH=${JET_PATH}/u-boot
export MYDROID=${JET_PATH}/mydroid
export JET_BOARD=JET
export PATH=$PATH:${UBOOT_PATH}/tools:${MYDROID}/prebuilt/linux-x86/toolchain/arm-eabi-4.4.3/bin/
export CROSS_COMPILE=${MYDROID}/prebuilt/linux-x86/toolchain/arm-eabi-4.4.3/bin/arm-eabi-


def_config=omap4430jet_snow_config

if [ $# -eq 1 ]
then
    if [[ "$1" == -* ]]
    then
        if [[ "$1" == *c* ]]
        then
            echo -e "\033[1;32m------ Cleaning X-Loader ------\033[m"
			make distclean
            echo -e "\033[1;32m------ DONE Cleaning X-Loader ------\033[m"
        fi
        if [[ "$1" == *x* ]]
        then
            def_config=omap4430jet_snow_config
        fi
        if [[ "$1" == *y1* ]]
        then
            def_config=omap4430jet_sun_b1_config
        elif [[ "$1" == *y* ]]
        then
            def_config=omap4430jet_sun_config
        fi

    fi
fi

echo -e "\033[1;32m------ Building X-Loader (${def_config}) ------\033[m"
make ARCH=arm $def_config
make ift |tee ${JET_PATH}/logs/x-loader_make.out
echo -e "\033[1;32m------ DONE Building X-Loader (${def_config}) ------\033[m"

exit 0

