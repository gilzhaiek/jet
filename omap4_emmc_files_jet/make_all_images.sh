export MY_PATH=`pwd`
export JET_PATH=${MY_PATH}/..

./copy_boot.sh
./make_ramdisk.sh

mkdir -p tmp/
sudo ./make_ext4fs -s -l 256M -a cache cache.img tmp/
rm -r tmp



