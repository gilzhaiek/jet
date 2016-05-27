#$1=system or userdat
if [ "$2" = "-m" ]
then
    ./simg2img $1.img $1.img.raw
    mkdir -p tmp 
    sudo mount -t ext4 -o loop $1.img.raw tmp/
fi

if [ "$2" = "-u" ]
then
    sudo ./make_ext4fs -s -l 512M -a $1 $1.img.new tmp/
    sudo umount tmp
    rm -rf tmp
fi

