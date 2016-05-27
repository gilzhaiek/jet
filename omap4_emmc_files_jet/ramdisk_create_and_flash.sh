rm boot.img

./make_ramdisk.sh $1

if [ ! $? -eq 0 ] ;then
    echo -e "\033[1;31mERROR: ./make_ramdisk.sh failed\033[m"
    exit $?
fi

sudo ./fastboot flash boot boot.img
sudo ./fastboot continue

