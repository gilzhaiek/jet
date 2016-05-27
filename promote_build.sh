#!/bin/bash
dialog --backtitle 'Recon' --msgbox 'Welcome to the promoter' 5 50
a_list=`git log --pretty=format:'%H' -n 20 | awk 'BEGIN{a=1}{a++; print $1" "$1" off"}'`
temp_file=`(tempfile)`
OUTPUT="/tmp/input.txt"
>$OUTPUT
dialog --backtitle 'Recon' --radiolist "Choose commit" 40 200 20 $a_list 2>  $temp_file
if [ $? -ne "0" ]
    then
    exit
fi
SHA=`cat $temp_file`
echo $SHA
dialog --backtitle 'Recon' --inputbox "Enter the release name:" 8 40 2> $OUTPUT
Version=`cat $OUTPUT`
echo $Version
`wget http://10.10.75.237:6951/job/Promote_JET/buildWithParameters?token=PROMOTEIT\&sha1=$SHA\&version=$Version`
