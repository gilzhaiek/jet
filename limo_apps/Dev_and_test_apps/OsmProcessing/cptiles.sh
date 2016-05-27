#!/bin/bash
src_foldername="map_recon/"
today1=$(date +"%Y-%m-%d")
time1=$(date +"%H%M%S")
#echo "it is $today"
#echo "time is $time1"
folder_name="map_recon_"$today1"_"$time1"/"
va=$1
if [ -n "$va" ]; then
    folder_name=$va
fi

if [ -e "$folder_name" ]
  then
    #echo "file $folder_name exist."
    rm -r $folder_name
fi

echo "destination_folder=$folder_name"
mkdir $folder_name

#copy all xml file into destination folder $folder_name
find $src_foldername -name "*.xml" -exec cp \{\} $folder_name \;
echo "copy done"


    
