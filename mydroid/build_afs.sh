export YOUR_PATH=`pwd`
export MYDROID=${YOUR_PATH}
export PATH=$PATH:${MYDROID}/prebuilt/linux-x86/toolchain/arm-eabi-4.4.3/bin/
export CROSS_COMPILE=${MYDROID}/prebuilt/linux-x86/toolchain/arm-eabi-4.4.3/bin/arm-eabi-
export PATH=${YOUR_PATH}/../u-boot/tools:$PATH
export JET_PATH=${YOUR_PATH}/..
export YOUR_NAME=`whoami`
export BOARD_TYPE=jet

export JET_PRODUCT=snow
version_number=2

LOG_FILE=afs.out

EXTRA_PARAMS=

if [[ -z ${BUILD_NUMBER} ]]
then
  FILE_NAME_TAG=eng.${YOUR_NAME}
else
  FILE_NAME_TAG=${BUILD_NUMBER}
fi

echo -e 'Command: \E[34m'"\033[1m$0 $1 [$#]\033[0m"

if [[ "$1" == -* ]]
then
  if [[ "$1" == *t* ]]
  then
    echo -e "\033[1;32m------ Tests Apps in the Build ------\033[m"
    export TARGET_BUILD_VARIANT=tests
  fi
  if [[ "$1" == *x* ]]
  then
    echo -e "\033[1;32m------ snow PRODUCT ------\033[m"
    export JET_PRODUCT=snow
  fi
  if [[ "$1" == *y1* ]]
  then
    echo -e "\033[1;32m------ sun V1 PRODUCT ------\033[m"
    export JET_PRODUCT=sun
    version_number=1
  elif [[ "$1" == *y2* ]]
  then
      echo -e "\033[1;32m------ sun V2 PRODUCT ------\033[m"
      export JET_PRODUCT=sun
      version_number=2
  elif [[ "$1" == *y* ]]
  then
    echo -e "\033[1;32m------ sun V3 PRODUCT ------\033[m"
    export JET_PRODUCT=sun
    export JET_BOARD_VERSION=sunb3
    version_number=3
  fi
fi


echo -e "\033[1;32m------ Building AFS ------\033[m"

if [[ $version_number -eq 1 ]]; then
  cp system/core/rootdir/init_${JET_PRODUCT}_b1.rc system/core/rootdir/init.rc
  cp device/ti/${BOARD_TYPE}/init_${JET_PRODUCT}_b1.omap4${BOARD_TYPE}board.rc device/ti/${BOARD_TYPE}/init.omap4${BOARD_TYPE}board.rc
else
  cp system/core/rootdir/init_${JET_PRODUCT}.rc system/core/rootdir/init.rc
  cp device/ti/${BOARD_TYPE}/init_${JET_PRODUCT}.omap4${BOARD_TYPE}board.rc device/ti/${BOARD_TYPE}/init.omap4${BOARD_TYPE}board.rc
fi

source ./build/envsetup.sh
lunch lean_${BOARD_TYPE}_${JET_PRODUCT}-user

if [[ "$1" == -* ]]
then
  if [[ "$1" == *c* ]]
  then
    echo -e "\033[1;32mmake clean Build\033[m"
    make clean
  fi

  if [[ "$1" == *r* ]]
  then
    echo -e "\033[1;32mRemoving Images\033[m"
    find out/target/product/${BOARD_TYPE} -name *.img -exec rm -f {} \;
    LOG_FILE=afs_r.out
  fi

  if [[ "$1" == *d* ]]
  then
    echo -e "\033[1;32mBuilding Dist\033[m"
	EXTRA_PARAMS="${EXTRA_PARAMS} dist"
    LOG_FILE=afs_d.out
  fi
fi

make -j$(egrep '^processor' /proc/cpuinfo | wc -l) ${EXTRA_PARAMS} 2>&1 | tee ${JET_PATH}/logs/${LOG_FILE}
#make -j2 ${EXTRA_PARAMS} 2>&1 | tee ${JET_PATH}/logs/${LOG_FILE}

if [ ! $? -eq 0 ] ;then
    echo -e "\033[1;31mERROR: make -j1 ${EXTRA_PARAMS} failed\033[m"
    exit $?
fi

if [[ "$1" == -* ]]
then
  if [[ "$1" == *d* ]]
  then
    echo "Signing the system apps...";
    extra_sign=
    if [ -e vendor/reconinstruments/limo_apps/apks ]; then
        cd vendor/reconinstruments/limo_apps/apks/
        apk_lists=`ls *.apk`
        apk_list=`echo -n $apk_lists | sed -e 's| |,|g'`
        apk_list="${apk_list},HUDServer.apk"
        extra_sign=`echo -n " -e ${apk_list}=vendor/recon/security/${BOARD_TYPE}/platform"`
        cd ${YOUR_PATH}
    fi
    ./build/tools/releasetools/sign_target_files_apks ${extra_sign} -d vendor/recon/security/${BOARD_TYPE} out/dist/lean_${BOARD_TYPE}_${JET_PRODUCT}-target_files-${FILE_NAME_TAG}.zip signed-target-files.zip

    if [ ! -e signed-target-files.zip ]; then
        echo -e "\033[4m\033[1;31mError: signed-target-files.zip is missing!!\033[m"
        exit 1;
    fi

    ./build/tools/releasetools/img_from_target_files signed-target-files.zip signed-img.zip

    echo "Overwriting non-signed system.img over out/target/product/${BOARD_TYPE}/system.img"
    mkdir -p /tmp/jet_system/
    unzip -o signed-img.zip -d /tmp/jet_system/
    mv /tmp/jet_system/system.img ${MYDROID}/out/target/product/${BOARD_TYPE}/system.img
    rm -rf /tmp/jet_system

    echo "Moving signed packages to ../omap4_emmc_files_jet/"
    mv signed-img.zip ../omap4_emmc_files_jet/
    mv signed-target-files.zip ../omap4_emmc_files_jet/

    echo "To Update/Fastboot with your signed image:"
    echo "cd mydroid"
    echo "cd ../omap4_emmc_files_jet/"
    echo "sudo ./fastboot update signed-img.zip"
  fi
fi

exit 0
