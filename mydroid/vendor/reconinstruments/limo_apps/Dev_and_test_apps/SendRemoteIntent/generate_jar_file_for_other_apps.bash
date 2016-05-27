bash make_app.bash
cd bin/classes/com/reconinstruments/interdevice;
mv InterDeviceIntent.class InterDeviceIntent.class.tmp
rm *.class
mv InterDeviceIntent.class.tmp InterDeviceIntent.class
cd ../../../
jar -cf InterDeviceIntent.jar ./
mv InterDeviceIntent.jar ../../
