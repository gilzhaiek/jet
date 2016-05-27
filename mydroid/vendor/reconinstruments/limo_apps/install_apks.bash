cd $1
for i in `ls *.apk`; do adb -d install -r $i; done
if [ "$2" = "jet" ]; then
    cd -;
    bash adb_recursive_push.bash ExternalStorage/  /sdcard/;
fi;
if [ "$2" = "limo" ]; then
    cd -;
    bash adb_recursive_push.bash ExternalStorage/  /mnt/storage/;
fi;
    

