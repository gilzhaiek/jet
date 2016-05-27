echo "copy xloader,uboot,kernel images--------------------------"
./copy_boot.sh

echo "copy candroid images--------------------------"
./copy_img.sh

echo "create boot images--------------------------"
sudo ./make_ramdisk.sh

echo "create cache images--------------------------"
#dd if=/dev/zero of=./cache.img bs=1048510 count=128
#mkfs.ext4 -F cache.img -L cache
mkdir -p tmp/
sudo ./make_ext4fs -s -l 256M -a cache cache.img tmp/
rm -r tmp
echo "flash mmc--------------------------"
./flash_mmc.sh
