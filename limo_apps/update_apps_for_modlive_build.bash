#usage: bash update_apps_for_modlive_build.bash <address_to_limo_droid_folder>

#The difference wiht release_apps.bash is that it doesnt add any of
# the changes to git. It only makes the working copy dirty
LIMODROID=$1
GIT_APP_FOLDER=$LIMODROID"/out/target/product/limo/root/data/app/"
PATCHER_PLACE=$LIMODROID"/patcher/patcher_script_for_apps.txt"
echo "" > $PATCHER_PLACE
### The main thing
applist="ReconAppInstaller ReconApplauncher ReconChrono ReconDashboard ReconJump ReconMusic ReconNavigation ReconPhone ReconPowerMenu ReconSettings ReconStats SoftKeyboard";

for i in $applist
do
    echo ++++++++++++building $i"+++++++++++++++"
    bash update_app_for_modlive_build.bash $i/ $GIT_APP_FOLDER
    bash generate_update_script.bash $i/ >> $PATCHER_PLACE
done
