# ./create_sd.sh /dev/sdb
export YOUR_PATH=`pwd`/..
export MYDROID=${YOUR_PATH}

SD_NAME=sdb
sudo bash mkcard.sh /dev/$SD_NAME
./simg2img system.img system.img.raw


