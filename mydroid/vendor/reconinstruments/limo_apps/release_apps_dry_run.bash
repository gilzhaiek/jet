#usage: bash release_apps.bash <address_to_limo_droid_folder>
LIMODROID=$1
GIT_APP_FOLDER=$LIMODROID"/out/target/product/limo/root/data/app/"
PATCHER_PLACE=$LIMODROID"/patcher/patcher_script_for_apps.txt"
echo "" > $PATCHER_PLACE
### The main thing
applist="ServiceWithCallBack IntroVideo ReconBLE_java CompassSensor DashLauncher ReconAppInstaller ReconApplauncher ConnectDevice Phone ReconChrono  ReconPowerMenu Stats OffsetKeyboard ContourApp ReconPolarHR  RibSimulationToggle";

for i in $applist
do
    echo ++++++++++++building $i"+++++++++++++++"
    cd $i;
    bash make_app.bash
    cd ..
done
