
#! /bin/bash
# mkcard.sh v0.5
# (c) Copyright 2009 Graeme Gregory <dp@xora.org.uk>
# Licensed under terms of GPLv2
#
# Parts of the procudure base on the work of Denys Dmytriyenko
# http://wiki.omap.com/index.php/MMC_Boot_Format
#
# Extended for 3 partitions by Prasad Golla <golla@ti.com> in April 2011.
# The 'media' partition can be used for storing media clips which can 
# be preserved even if the binaries in the other partitions are erased.
#
#example:
#1. Use gpart to creacet loop partition;
#2. reinsert sd card
#3. sudo bash mkcard.sh /dev/sdb
#
export LC_ALL=C

if [ $# -ne 1 ]; then
        echo "Usage: $0 <drive>"
        exit 1;
fi

DRIVE=$1

SKIPMEDIA=0

function format_boot_drive (){
echo "Formatting boot drive"
if [ -b ${DRIVE}1 ]; then
        umount ${DRIVE}1
        mkfs.vfat -F 32 -n "boot" ${DRIVE}1
else
        if [ -b ${DRIVE}p1 ]; then
                umount ${DRIVE}p1
                mkfs.vfat -F 32 -n "boot" ${DRIVE}p1
        else
                echo "Can't find boot partition in /dev"
        fi
fi
}

function format_rootfs_drive (){
echo "Formatting rootfs drive"
if [ -b ${DRIVE}2 ]; then
        umount ${DRIVE}2
        mke2fs -j -L "rootfs" ${DRIVE}2
else
        if [ -b ${DRIVE}p2 ]; then
                umount ${DRIVE}p2
                mke2fs -j -L "rootfs" ${DRIVE}p2
        else
                echo "Can't find rootfs partition in /dev"
        fi
fi
}

function format_media_drive (){
	echo "Formatting media mediadrive"
	if [ -b ${DRIVE}3 ]; then
        	umount ${DRIVE}3
	        mkfs.vfat -F 32 -n "media" ${DRIVE}3
	else
        	if [ -b ${DRIVE}p3 ]; then
	                umount ${DRIVE}p3
	                mkfs.vfat -F 32 -n "media" ${DRIVE}p3
        	else
	                echo "Can't find media partition in /dev"
	        fi
	fi
}

function get_media_drive_size(){
	echo "How big of a media drive do you want?"
	echo "Enter 0 to NOT create the media drive."
	echo "Enter 1 for 1GB."
	echo "Enter 64 for 64MB."
	echo "Enter 128 for 128MB."
	echo "Enter 512 for 512MB."
	echo "Enter 256 for 256MB."
	echo -n "Enter size of media drive you want: "

	read disksize

	case "$disksize" in
		0)   SKIPMEDIA=1; return ;;
		1)   MEDIASIZEREQ=`echo "1024 * 1024 * 1024" | bc` ;;
		512) MEDIASIZEREQ=`echo "1024 * 1024 * 1024 / 2" | bc` ;;
		256) MEDIASIZEREQ=`echo "1024 * 1024 * 1024 / 4" | bc` ;;
		128) MEDIASIZEREQ=`echo "1024 * 1024 * 1024 / 8" | bc` ;;
		64)  MEDIASIZEREQ=`echo "1024 * 1024 * 1024 / 16" | bc` ;;
		*)   echo "Not a valid option. Try again."; get_media_drive_size;;
	esac

	echo Media size in bytes - $MEDIASIZEREQ
	MEDIACYLINDERS=`echo $MEDIASIZEREQ/255/63/512 | bc`
	echo Media CYLINDERS - $MEDIACYLINDERS
}

function create_drives(){
	dd if=/dev/zero of=$DRIVE bs=1024 count=1024

	SIZE=`fdisk -l $DRIVE | grep Disk | grep bytes | awk '{print $5}'`

	echo DISK SIZE - $SIZE bytes

	CYLINDERS=`echo $SIZE/255/63/512 | bc`
	echo CYLINDERS - $CYLINDERS

	get_media_drive_size;
	echo  $SIZE - $CYLINDERS - $DRIVE -  $SKIPMEDIA

	if [ $SKIPMEDIA == 1 ] ; then
		{
			echo ,9,0x0C,*
			echo ,,,-
		} | sfdisk -D -H 255 -S 63 -C $CYLINDERS $DRIVE
		sleep 1
		return;
	fi

	LASTCYLINDERLINUX=`echo $CYLINDERS - 9 - $MEDIACYLINDERS | bc`

	{
		echo ,9,0x0C,*
		echo ,$LASTCYLINDERLINUX,,-
		echo ,,0x0C,-
	} | sfdisk -D -H 255 -S 63 -C $CYLINDERS $DRIVE

	sleep 1
}

echo "To format the boot and rootfs only, enter 'f'."
echo "To create the boot, media and rootfs and format them, enter 'c'."
echo -n "Enter c or f:"

read answer

case "$answer" in
f) format_boot_drive; format_rootfs_drive; exit ;;
c) create_drives; format_boot_drive; format_rootfs_drive; format_media_drive; exit;;
*) echo "Not a valid option. Exiting"; exit ;;
esac
