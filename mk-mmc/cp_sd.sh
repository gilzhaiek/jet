export YOUR_PATH=`pwd`/..
export MYDROID=${YOUR_PATH}

[ $# -eq 0 ] && { echo "Usage: $0 </dev/sdx>"; exit 1; }

SD_NAME=$1

echo "copy xloader,uboot and kernel image---"
sudo mkdir /mnt/mmc1
sudo mount ${SD_NAME}1 /mnt/mmc1


sudo cp MLO /mnt/mmc1/
sudo cp u-boot.bin /mnt/mmc1/
sudo cp uImage /mnt/mmc1/

sudo umount /mnt/mmc1
sudo rm -r /mnt/mmc1
if [ -a ./system.img.raw ];  then
    echo "copy rootfs---"
    sudo mkdir /mnt/mmc2
    sudo mount ${SD_NAME}2 /mnt/mmc2

    mkdir -p tmp
    sudo mount -t ext4 -o loop system.img.raw tmp/

    cd /mnt/mmc2
    sudo cp -r ${YOUR_PATH}/mk-mmc/tmp/* .
    cd ${YOUR_PATH}/mk-mmc/


    sudo umount ${YOUR_PATH}/mk-mmc/tmp
    sudo umount /mnt/mmc2
    sudo rm -r /mnt/mmc2
    rm -r ./temp
fi
echo "finish"   
