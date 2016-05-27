#usage: bash release_apps.bash <address_to_limo_droid_folder>
if [ ! -e logs ]; then
	mkdir logs
fi
LOG_FOLDER=`pwd`/logs

LIMODROID=$1

trap ctrl_c INT

function ctrl_c() {
	echo "Ctrl-C Pressed Exiting!"
	exit;
}

function validate_build {
    if [ ! -e $GIT_APP_FOLDER/${1}* ]; then
        echo -e  "\033[1;31mERROR ERROR ERROR\033[m"
        echo -e  "\033[1;31m${1}\033[m is \033[4mmissing\033[m"
    else
        ls_ret=`ls -l $GIT_APP_FOLDER/${1}*`
        echo -e "\033[1;32mOK OK OK\033[m"
        echo -e "\033[1;32m${ls_ret}\033[m"
    fi
}

if [ "$2" = "" ]; then
    GIT_APP_FOLDER=$LIMODROID"/out/target/product/limo/root/data/app/"
    PATCHER_PLACE=$LIMODROID"/patcher/patcher_script_for_apps.txt"
    PATCHER_REMOVE_LEGACY=$LIMODROID"/patcher/patcher_remove_legacy_apps.txt"
    cat  $PATCHER_REMOVE_LEGACY > $PATCHER_PLACE
else
    GIT_APP_FOLDER=$LIMODROID
fi

if [ ! -e Android_MobileSDK/HUDConnectivity ];
then
    echo -e "\033[1;31mMissing Submodules! Please run the following\033[m"
    echo "git submodule update"
    echo "git submodule update --init"
    exit;
fi

place=`pwd`;
cd libraries/ReconOSCommonWidgets;
android update project -p . -t android-17
ant debug
cd $place
cd deprecated/ReconCommonWidgets;
android update project -p . -t android-17
ant debug
cd $place
cd libraries/isOakleyDecider;
android update project -p . -t android-17
ant debug;
cd $place

cd libraries/ReconUtils;
android update project -p . -t android-17
ant debug;
cd $place

cd libraries/ReconMessageAPI/;
android update project -p . -t android-17
ant debug
cd $place

cd libraries/ReconPhoneProvider/;
android update project -p . -t android-17
ant debug
cd $place

cd libraries/Breadcrumb/
android update project -p . -t android-17
ant debug
cd $place

cd libraries/Dashlauncher_element
bash make_app.bash;
cd $place
cd Android_MobileSDK/HUDConnectivity/
bash make_app.bash
cd $place
cd Android_MobileSDK/MobileConnectLibrary/
bash make_app.bash
cd $place
cd Android_MobileSDK/HUDPhoneStatusExchange/
bash make_app.bash
cd $place




### The main thing
applist=`./print_release_apps.sh`
app_names=" "

for i in $applist
do
    echo -e "\033[1;32mBuilding ${i}\033[m"
    if [ "$2" = "" ]; then
		bash new_release_app.bash $i/ $GIT_APP_FOLDER limo
		bash generate_update_script.bash $i/ >>  $PATCHER_PLACE 
    else
		bash new_release_app.bash $i $GIT_APP_FOLDER $2 2>&1 | tee ${LOG_FOLDER}/${i}.log
		file_name=`cat ${LOG_FOLDER}/${i}.log | grep filename | sed -s 's|filename ||'`
		if [ -n "$file_name" ]; then 
			validate_build $file_name
			app_name="${app_names} ${file_name}"
			echo -e "\033[1;31m${app_names}\033[m"
		else 
	        echo -e  "\033[1;31m${i}\033[m has not been \033[4mcompiled\033[m"
		fi
    fi
done

# This line breaks Jenkins build. Comment out for now.
#grep -ni error ${LOG_FOLDER}/* | grep -v 'translation' | grep -i error
