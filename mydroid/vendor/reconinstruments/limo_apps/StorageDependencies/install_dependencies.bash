#!/bin/bash
export MY_PATH=`pwd`
export EXT_STORAGE=${MY_PATH}/../ExternalStorage
export BASE_RECON_APPS_FOLDER=${EXT_STORAGE}/ReconApps

if [ "$1" = "" ] || [ "$1" = "jet" ] || [ "$1" = "Jet" ] || [ "$1" = "JET" ]; then
  model=Jet
else
  model=Snow2
fi

if [ "$2" = "build" ]; then
  target=build
else
  target=hud
fi

echo -e "MODEL:\t\033[1;32m${model}\033[m"
echo -e "TARGET:\t\033[1;32m${target}\033[m"

if [ "$target" = "build" ]; then
  TRG_RECON_APPS=${EXT_STORAGE}/sdcard/ReconApps
  echo "Removing old $TRG_RECON_APPS"
  rm -rf $TRG_RECON_APPS
  mkdir -p $TRG_RECON_APPS
  echo "Copying base folder ${BASE_RECON_APPS_FOLDER} to ${TRG_RECON_APPS}"
  cp -r ${BASE_RECON_APPS_FOLDER}/. ${TRG_RECON_APPS}/
  echo "Copying model dep ${model}/ReconApps to ${TRG_RECON_APPS}"
  cp -r ${model}/ReconApps/. ${TRG_RECON_APPS}/
else
  TRG_RECON_APPS="/mnt/sdcard/ReconApps"
  echo "Removing all ${TRG_RECON_APPS}/MapData content"
  adb shell rm ${TRG_RECON_APPS}/MapData/*.*
  adb shell rm ${TRG_RECON_APPS}/MapData/MapTypes/*.*
  echo "Pushing model dep ${model}/ReconApps to ${TRG_RECON_APPS}"
  adb push ${model}/ReconApps/. ${TRG_RECON_APPS}/
fi

