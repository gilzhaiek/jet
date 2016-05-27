senstest wrapper for magnetometer and accelerometer testing over uart
alex.bell@reconinstruments.com
03/18/2014

========= Intro  ===========
There are 2 stages of using these tools. Since having USB connected will engage the charger and skew magnetometer results, the script is designed to be run via UART. However it must be installed over adb.

========= Install  ===========
Unzip the package on the machine with adb set up. This readme does not cover installation of adb. The script checks if the device is accessible by running 'adb devices'. If there are no devices listed when you run this command, or if there is more than one device listed, the script will fail.

unzip -zvxf senstool.tar.gz

You should have the following files:
alexbell@RECON-072-L7$ find 
./readme.txt
./senstool.sh
./scripts
./scripts/busybox_install.sh
./scripts/all.sh
./scripts/runtest.sh
./apps
./apps/i2cget
./apps/busybox-armv7l
./apps/i2cset
./apps/senstest
./apps/i2cdump

Connect your device and run senstool.sh. You should see this:

1) Install (disable zygote, reboot, then copy utils)
2) Install (copy utils only)
3) Uninstall (re-enable zygote and reboot)
4) Copy results back
5) Quit

========= Options ===========

senstest cannot poll the 9-axis sensor correctly when android/zygote is running. We disable it by renaming a critical init process and then rebooting. After we are finished we restore it, and android will start again on next reboot. When android is not running, the screen will show Recon Instruments and nothing else.

If android is already disabled but you need to copy the tools again, select option 2. Tools can be deleted if the system is rebooted and will need to be re-copied.

You will be prompted before rebooting.

========= Gathering data ===========

Once you have rebooted with android disabled, the tools will be copied to the device. You must then disconnect the usb cable and switch to communicating over UART.

From the UART shell, run the script by typing the following:
 ash runtest.sh

It will ask for the serial number of the device to use as the folder for saving results.

You can then run accelerometer and magnetometer tests for 10 seconds. Once the test is complete, you can reconnect the usb cable.

========= Copying results  ===========

If you have already collected results, you can use option 4 from the senstool.sh script on your local machine. This will copy the results files back to a folder called results.
