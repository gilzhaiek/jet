if [ $# -eq 1 ] && [[ "$1" == -* ]]
    then
        if [[ "$1" == *r* ]]
        then
            echo -e "\033[1;32m------ Rooted Device for manufaturing------\033[m"
            cp default_manufature.prop ./root/default.prop
        elif [[ "$1" == *t* ]]
        then
            echo -e "\033[1;32m------ Rooted Device for testing------\033[m"
            cp default_nonsecure.prop ./root/default.prop
        else
            echo -e "\033[1;32m------ Secured Device ------\033[m"
            cp default.prop ./root/default.prop
        fi
        if [[ "$1" == *x* ]]
        then
            echo -e "\033[1;32m------ snow Device ------\033[m"
            cp init_snow.rc ./root/init.rc
            cp init_snow.omap4jetboard.rc ./root/init.omap4jetboard.rc
            cp init_snow.omap4jetboard.usb.rc ./root/init.omap4jetboard.usb.rc
      elif [[ "$1" == *y1* ]]
        then
            echo -e "\033[1;32m------ sun B1 Device ------\033[m"
            cp init_sun_b1.rc ./root/init.rc
            cp init_sun_b1.omap4jetboard.rc ./root/init.omap4jetboard.rc
            cp init.omap4jetboard.usb.rc ./root/init.omap4jetboard.usb.rc
        elif [[ "$1" == *y* ]]
        then
            echo -e "\033[1;32m------ sun Device ------\033[m"
            cp init_sun.rc ./root/init.rc
            cp init_sun.omap4jetboard.rc ./root/init.omap4jetboard.rc
            cp init_sun.omap4jetboard.usb.rc ./root/init.omap4jetboard.usb.rc
        fi
else
  echo -e "\033[1;31mERROR: $0 is used wrong:\033[m"
  echo "Options:"
  echo " -x<r>   : Snow"
  echo " -y<r>   : Sun"
  echo " -y1<r>  : Sun B1"
  echo " r       : Rooted for manufaturing"  #Manufaturing guys want to keep using this option
  echo " t       : Rooted for normal testing"
  exit 1
fi

cp init.usb.rc ./root/init.usb.rc
./mkbootfs root/ | ./minigzip >ramdisk.img
./mkbootimg  --kernel zImage  --ramdisk ramdisk.img  --base 0x80000000 --board omap4 -o boot.img

exit 0
