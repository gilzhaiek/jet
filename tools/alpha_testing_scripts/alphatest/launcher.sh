#!/bin/bash
#
# This is just a handy shortcut to launch various android apps since the commands are somewhat cryptic
options=("GPS Alpha test" "Calibrate Compass" "Camera App" "Compass linearity test" "Battery test (voltagelogger)")
select opt in "${options[@]}" "Quit"; do 

    case "$REPLY" in

    1 ) adb shell am start -n com.reconinstruments.alphatester/.GPS;;
    2 ) adb shell am start -a com.reconinstruments.compass.CALIBRATE;;
    3 ) adb shell am start -n com.reconinstruments.camera/.app.CameraActivity;;
    4 ) adb shell am start -n com.reconinstruments.alphatester/.Compass;;
    5 ) adb shell am start -n com.reconinstruments.voltagelogger/.MainActivity

    $(( ${#options[@]}+1 )) ) echo "Goodbye!"; break;;
    *) echo "Invalid option. Try another one.";continue;;

    esac

done
