sudo ./fastboot oem format
sudo ./fastboot flash xloader     MLO
sudo ./fastboot flash bootloader  u-boot.bin
sudo ./fastboot reboot-bootloader
sleep 10
sudo ./fastboot oem format
sleep 5
sudo ./fastboot_update_all.sh
