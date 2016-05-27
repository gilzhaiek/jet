#Variables
WORKDIR='images/official_zip'
startfw=0
endfw=0

#=========================================
listfile(){
ca=1
 for file in *
  do
    firmware[$ca]=$file
    echo "$ca". "$file"
    ca=$((ca+ 1))
  done
echo -en "\n"
}

enterendfw(){
echo "Please select the firmware wanted on the unit"
echo -en "\n"
echo "0. Get the latest Version"
listfile
}

make_update(){
cd ../..
if [ ${endfw} == 0 ]; then
 ./releasetools/ota_from_target_files -v -k security/testkey -i images/official_zip/${firmware[${startfw}]} images/signed-target-files.zip update.bin
else
 ./releasetools/ota_from_target_files -v -k security/testkey -i images/official_zip/${firmware[${startfw}]} images/official_zip/${firmware[${endfw}]} update.bin
fi
ls -l update.bin
}

#=========================================
cd $WORKDIR
clear
echo "Please select the start firmware"
echo -en "\n"
listfile
echo "Enter a number and hit Enter"
read startfw
endfw=$startfw
clear
while [ $startfw == $endfw ]
do
enterendfw
echo "Enter a number and hit Enter"
read endfw
done
clear
echo "============================================================"
echo "			START Building				  "
echo "============================================================"
make_update

echo "To Push the file - run the following command:"
echo "./adb push update.bin /mnt/sdcard/ReconApps/cache/update.bin"

