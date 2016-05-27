#used to flash all image to mmc
if [ "$1" = "1" ]
then
echo "1st time flash to init sd card"
sudo ./fastboot oem format #first time
sudo ./fastboot.sh
else 
	if [ "$1" = "a" ]
	then
		echo "flash sd card without bootloader"
		sudo ./fastboot flash recovery    recovery.img
		sudo ./fastboot flash system      system.img
		sudo ./fastboot flash cache       cache.img
		sudo ./fastboot flash userdata    userdata.img
		sudo ./fastboot flash boot        boot.img
		sudo ./fastboot reboot
	else
		echo "flash sd card begin"
		sudo ./fastboot flash xloader     MLO
		sudo ./fastboot flash bootloader  u-boot.bin
		#sudo ./fastboot reboot-bootloader
		#sleep 5
		sudo ./fastboot flash recovery    recovery.img
		sudo ./fastboot flash system      system.img
		sudo ./fastboot flash cache       cache.img
		sudo ./fastboot flash userdata    userdata.img
		sudo ./fastboot flash boot        boot.img
		sudo ./fastboot reboot
	fi
fi
