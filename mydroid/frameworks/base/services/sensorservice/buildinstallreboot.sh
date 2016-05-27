cd ../../../..
./build_module.sh libsensorservice

if [[ $? -eq "0" ]]; then
    echo Compiled, pushing
    adb remount
    adb push out/target/product/jet/system/lib/libsensorservice.so system/lib
    if [[ $? -eq "0" ]]; then
        echo Restarting device..
        adb reboot
    fi
    echo Done!
else
    echo "Failed to compile"
    exit 1
fi
