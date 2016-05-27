#!/system/bin/sh
SDCARD_FOLDER=/mnt/sdcard
RECON_APPS_FOLDER=${SDCARD_FOLDER}/ReconApps
TRIPDATA_FOLDER=${RECON_APPS_FOLDER}/TripData
GEODATA_FOLDER=${RECON_APPS_FOLDER}/GeodataService
LOG_FILE=${SDCARD_FOLDER}/log.txt
SVN_NUM=`getprop ro.build.svn.num`
if [ -e /factory/sdcard/factory_svn_num.txt ]; then
  FACTORY_SVN=`cat /factory/sdcard/factory_svn_num.txt`
else
  FACTORY_SVN=0000
fi
RAPPS_FACTORY_FILE=/factory/sdcard/rapps_${FACTORY_SVN}.zip
RAPPS_FILE=rapps.bin
RAPPS_SRC_FILE=${RECON_APPS_FOLDER}/cache/$RAPPS_FILE
RAPPS_TMP_FILE=${SDCARD_FOLDER}/$RAPPS_FILE
GEODATA_FACTORY_FILE=/factory/sdcard/geodata_${FACTORY_SVN}.zip
GEODATA_FILE=geodata.bin
GEODATA_SRC_FILE=${RECON_APPS_FOLDER}/cache/$GEODATA_FILE
GEODATA_TMP_FILE=${SDCARD_FOLDER}/$GEODATA_FILE
RAPPS_VER_NAME=RAPPS_VERSION
GEODATA_VER_NAME=GEODATA_VERSION
RAPPS_VER_FILE=${RECON_APPS_FOLDER}/${RAPPS_VER_NAME}
GEODATA_VER_FILE=${RECON_APPS_FOLDER}/${GEODATA_VER_NAME}

rm ${LOG_FILE}
echo "recon_boot log file\nSVN_NUM=${SVN_NUM}\nFACTORY_SVN=${FACTORY_SVN}" > ${LOG_FILE}

function wr_log {
  mtm=`date +"%r"`
  echo "${mtm}: $1" >> ${LOG_FILE}
}

function finish_recon_boot {
  wr_log "finish_recon_boot"

  #Send message the recon boot script finish
  setprop recon_boot.done 1

  # Broadcast intent to indicate we are finished
  am broadcast -a RECON_BOOT_END
}

wr_log "Checking if ${RAPPS_VER_FILE} and ${GEODATA_VER_FILE} exists"

sdcard_rapps_version=0000
sdcard_geodata_version=0000
if [ -e ${RAPPS_VER_FILE} ] && [ -e ${GEODATA_VER_FILE} ]; then
  wr_log "Found RAPPS and GEODATA VERSION files"
  wr_log "Checking VERSION values"
  sdcard_rapps_version=`cat ${RAPPS_VER_FILE}`
  sdcard_geodata_version=`cat ${GEODATA_VER_FILE}`
  if [ *$sdcard_rapps_version* == *$SVN_NUM* ] && [ *$sdcard_geodata_version* == *$SVN_NUM* ]; then
    wr_log "RAPPS and GEODATA VERSION seem to be the latest version, exiting."
    finish_recon_boot
    exit 0
  else
    wr_log "CURRENT RAPPS VERSION: `cat ${RAPPS_VER_FILE}`"
    wr_log "CURRENT GEODATA VERSION: `cat ${GEODATA_VER_FILE}`"
  fi
else
  wr_log "Couldn't find both VERSION files, sdcard require update..."
fi

function mv_to_factory {
  wr_log "---------------------------------------"
  src=$1
  dst=$2
  wr_log "Moving $src to $dst"
  mount -o remount,rw /factory
  rm $dst

  src_md5=`md5 $src | busybox sed -e 's| .*||'`
  busybox cp $src $dst
  dst_md5=`md5 $dst | busybox sed -e 's| .*||'`
  mount -o remount,ro /factory

  if [ $src_md5 == $dst_md5 ]; then
    wr_log "Success moving $src to $dst"
    wr_log "Removing $src"
    rm $src
  else
    wr_log "Failed moving $src to $dst"
    wr_log "Removing $dst"
    rm $dst
    finish_recon_boot
    exit 1
  fi
  wr_log "---------------------------------------"
}
#Disable USB port, the UsbDeviceManager will change the config back to persist.sys.usb.config and force Host rescan usb port
setprop charger.usb.config none
wr_log "Updating ReconApps..."

rapps_file=$RAPPS_FACTORY_FILE
geodata_file=$GEODATA_FACTORY_FILE
if [ $SVN_NUM -eq $FACTORY_SVN ]; then
  wr_log "SVN is same as FACTORY, using $rapps_file and $geodata_file"
else
  if [ -e $RAPPS_SRC_FILE ]; then
    wr_log "Found a new $RAPPS_SRC_FILE for rapps, moving to $RAPPS_TMP_FILE"
    mv $RAPPS_SRC_FILE $RAPPS_TMP_FILE
  elif  [ -e $RAPPS_TMP_FILE ]; then
    wr_log "Found temp $RAPPS_TMP_FILE (bad reboot), using it"
  else
    wr_log "Did not find any $RAPPS_FILE to be used"
  fi
  rapps_file=$RAPPS_TMP_FILE
  if [ -e $GEODATA_SRC_FILE ]; then
    wr_log "Found a new $GEODATA_SRC_FILE for geodata, moving to $GEODATA_TMP_FILE"
    mv $GEODATA_SRC_FILE $GEODATA_TMP_FILE
  elif  [ -e $GEODATA_TMP_FILE ]; then
    wr_log "Found temp $GEODATA_TMP_FILE (bad reboot), using it"
  else
    wr_log "Did not find any $GEODATA_FILE to be used"
  fi
  geodata_file=$GEODATA_TMP_FILE
fi


if [ -e $rapps_file ]; then
  wr_log "---------------------------------------"
  if [ *$rapps_file* == *$RAPPS_FACTORY_FILE* ] && [ *$sdcard_rapps_version* == *$FACTORY_SVN* ]; then
    wr_log "Expanded RAPPS version is already the factory version, no need to expand again"
  else
    wr_log "Found $rapps_file"
    wr_log "Moving ${TRIPDATA_FOLDER} to ${SDCARD_FOLDER}/TripData"
    mv ${TRIPDATA_FOLDER} ${SDCARD_FOLDER}/TripData
    wr_log "Moving ${GEODATA_FOLDER} to ${SDCARD_FOLDER}/GeodataService"
    mv ${GEODATA_FOLDER} ${SDCARD_FOLDER}/GeodataService
    wr_log "Moving ${GEODATA_VER_FILE} to ${SDCARD_FOLDER}/${GEODATA_VER_NAME}"
    mv ${GEODATA_VER_FILE} ${SDCARD_FOLDER}/${GEODATA_VER_NAME}
    wr_log "Removing ${RECON_APPS_FOLDER}"
    busybox rm -rf ${RECON_APPS_FOLDER}
    busybox rm -rf ${RECON_APPS_FOLDER}
    wr_log "Unzipping $rapps_file to ${SDCARD_FOLDER}"
    /system/bin/busybox unzip $rapps_file -d ${SDCARD_FOLDER}
    busybox rm -rf ${TRIPDATA_FOLDER}
    wr_log "Moving back ${SDCARD_FOLDER}/TripData to ${TRIPDATA_FOLDER}"
    mv ${SDCARD_FOLDER}/TripData ${TRIPDATA_FOLDER}
    wr_log "Moving back ${SDCARD_FOLDER}/GeodataService to ${GEODATA_FOLDER}"
    mv ${SDCARD_FOLDER}/GeodataService ${GEODATA_FOLDER}
    wr_log "Moving back ${SDCARD_FOLDER}/${GEODATA_VER_NAME} to ${GEODATA_VER_FILE}"
    mv ${SDCARD_FOLDER}/${GEODATA_VER_NAME} ${GEODATA_VER_FILE}
    if [ *$rapps_file* == *$RAPPS_FACTORY_FILE* ]; then
      wr_log "Writing FACTORY_SVN (${FACTORY_SVN}) to $RAPPS_VER_FILE"
      echo $FACTORY_SVN > $RAPPS_VER_FILE
    else
      wr_log "Writing SVN_NUM (${SVN_NUM}) to $RAPPS_VER_FILE"
      echo $SVN_NUM > $RAPPS_VER_FILE
    fi
    wr_log "Done Unpacking $rapps_file"
    if [ *$rapps_file* != *$RAPPS_FACTORY_FILE* ]; then
      wr_log "Deleting $rapps_file"
      rm $rapps_file
    fi
  fi
  wr_log "---------------------------------------"
fi

if [ -e $geodata_file ]; then
  wr_log "---------------------------------------"
  if [ *$geodata_file* == *$GEODATA_FACTORY_FILE* ] && [ *$sdcard_geodata_version* == *$FACTORY_SVN* ]; then
    wr_log "Expanded GEODATA version is already the factory version, no need to expand again"
  else
    wr_log "Found $geodata_file"
    wr_log "Removing ${GEODATA_FOLDER}"
    busybox rm -rf ${GEODATA_FOLDER}
    wr_log "Unzipping $geodata_file to ${RECON_APPS_FOLDER}"
    /system/bin/busybox unzip $geodata_file -d ${RECON_APPS_FOLDER}
    if [ *$geodata_file* == *$GEODATA_FACTORY_FILE* ]; then
      wr_log "Writing FACTORY_SVN (${FACTORY_SVN}) to $GEODATA_VER_FILE"
      echo $FACTORY_SVN > $GEODATA_VER_FILE
    else
      wr_log "Writing SVN_NUM (${SVN_NUM}) to $GEODATA_VER_FILE"
      echo $SVN_NUM > $GEODATA_VER_FILE
    fi
    wr_log "Done Unpacking $geodata_file"
    if [ *$geodata_file* != *$GEODATA_FACTORY_FILE* ]; then
      wr_log "Deleting $geodata_file"
      rm $geodata_file
    fi
  fi
  wr_log "---------------------------------------"
fi

finish_recon_boot

usb_config=`getprop sys.usb.config`
if [ "$usb_config" != "none" ]; then
  #UsbDeviceManager already reset usb config but MtpService is still disabled
  wr_log "reset usb config"
  #From UsbDeviceManager init to MtpService init is around 7 second, Add delay time here for the safty
  sleep 8
  usb_config=`getprop persist.sys.usb.config`
  setprop charger.usb.config ${usb_config}
fi

wr_log "recon_boot complete"

