YOUR_PATH=`pwd`
MYDROID=${YOUR_PATH}../../../
LOG_FOLDER=${YOUR_PATH}/logs
SIGNING_FOLDER=${YOUR_PATH}/signing/jet
APKS_OUT_FOLDER=${YOUR_PATH}/apks

sign_apps=0

function rebuild_required() {
        in_folder=$1
        out_file=$2
        out_date=$( date -u -r $out_file +%s)
        in_date=$( find $YOUR_PATH/$in_folder/src -type f -exec date -u -r {} +%s \; | sort -r | head -1 )
        if [ "$in_date" -gt "$out_date" ]; then
            echo "true"
        else
            echo "false"
        fi
        }
function setup_activity_name() {
        if [[ "$1" == "ServiceWithCallBack" ]]; then
                name="HeadingService"
        elif [[ "$1" == "OffsetKeyboard" ]]; then
                name="SoftKeyboard"
        elif [[ "$1" == "Dashlauncher_redux" ]]; then
                name="Dashlauncher_Redux"
        elif [[ "$1" == "CompassSensor" ]]; then
                name="CompassCalibration"
        elif [[ "$1" == "ReconApplauncher" ]]; then
                name="ReconAppLauncher"
        elif [[ "$1" == "Stats" ]]; then
                name="ReconStats"
        else
                name=$1
        fi
}

function setup_activity() {
        setup_activity_name $1

        if [[ "$1" == "ReconAppInstaller" ]]; then
                android update project -p . -n $name -t android-10
        elif [[ "$1" == "ReconApplauncher" ]]; then
                android update project -p . -n $name -t android-16
        elif [[ "$1" == "ReconCamera" ]]; then
                android update project -p . -n $name -t android-19
        else
                android update project -p . -n $name -t android-16
        fi
}

function pre_reqs() {
        do_exit=0
        # ANT Version 1.8.x
        if [ ! -e `which ant` ]; then
                echo -e  "\033[1;31mERROR: ant doesnt't exists\033[m"
                echo -e "Please run: \033[1;32msudo apt-get install ant1.8\033[m"
                do_exit=1
        else
                ant_version=`ant -version`
                if [ `echo ${ant_version} | grep -c 1.8` -eq 0 ]; then
                        echo -e  "\033[1;31mERROR: ${ant_version} is not 1.8.x\033[m"
                        echo -e "Please run: \033[1;32msudo apt-get install ant1.8\033[m"
                        do_exit=1
                fi
        fi

        # Android Version and SDK
        if [ ! -e `which android` ]; then
                echo -e "\033[1;31mERROR: Android SDK is missing!\033[m"
                do_exit=1
        else
                android_sdk_root=`which android |  sed -e 's|tools/android||g'`
                android_10_jar=${android_sdk_root}/platforms/android-10/android.jar
                if [ ! -e ${android_10_jar} ]; then
                        echo -e "\033[1;31mERROR:  ${android_10_jar} is missing, Run Android and add API-10 \033[m"
                        do_exit=1
                else
                        if [[ `md5sum ${android_10_jar} | sed -e 's| .*||g'` != `md5sum ${YOUR_PATH}/jars/android.jar | sed -e 's| .*||g'` ]]; then
                                echo -e "\033[1;32m...Updating your Android-10 SDK to Recon Instruments' Custom API...\033[m"
                                cp jars/android.jar ${android_10_jar}
                        else
                                echo -e "\033[1;32m-[OK]- Your Android-10 API is checked and valid ------\033[m"
                        fi
                fi
                if [ ! -e ${android_sdk_root}/build-tools ]; then
                        echo -e "\033[1;31mERROR: Android BUILD Tools are missing, please run android!\033[m"
                        do_exit=1
                else
                        echo -e "\033[1;32m-[OK]- Found Android Build Tools ------\033[m"
                fi
        fi


        # Git is updated
        if [ ! -e Android_MobileSDK/HUDConnectivity ];
        then
                echo -e "\033[1;31mERROR: Missing Submodules! Please run the following\033[m"
                echo "cd ${YOUR_PATH}"
                echo "git submodule update"
                echo "git submodule update --init"
                do_exit=1
        fi

        # Various Folder
        if [ ! -e ${LOG_FOLDER} ]; then
                mkdir -p ${LOG_FOLDER}
        fi
        if [ ! -e ${APKS_OUT_FOLDER} ]; then
                mkdir -p ${APKS_OUT_FOLDER}
        fi

        if [ ${do_exit} -eq 1 ]; then
                exit 1
        fi
}

function build_app() {
        if [ ! -e ${YOUR_PATH}/$1 ]; then
                echo -e  "\033[1;31mERROR: $1 doesn't exists\033[m"
                exit 1
        fi
        setup_activity_name $1
        out_of_date=$(rebuild_required $1 $APKS_OUT_FOLDER/$name.apk)
        if [ "$out_of_date" == "false" ]; then
                echo -e  "\033[1;32m$1 up to date\033[m"
                return
        else
                echo -e "\033[1;32m$1 out of date... Building ${i}\033[m"
        fi

        cd ${YOUR_PATH}/$1
        ../update_android_manifest_with_version.bash

        setup_activity $1
        released_unsigned_apk=bin/${name}-release-unsigned.apk

        ant clean

        if [[ "$1" == "ReconApplauncher" ]]; then
                cat ../svn_info | awk '/Revision/ {print "package com.reconinstruments.applauncher.transcend;\nclass VersionInfo {\n static final public int SVN_VERSION_NUMBER = "$2";\n}"}' > src/com/reconinstruments/applauncher/transcend/VersionInfo.java;
        fi
        ant release
        if [ ! -e ${released_unsigned_apk} ]; then
                echo -e  "\033[1;31mERROR: ${released_unsigned_apk} doesnt't exists\033[m"
                exit 1
        fi

        if [[ ${sign_apps} == 1 ]]; then
                java -jar ${SIGNING_FOLDER}/signapk.jar ${SIGNING_FOLDER}/platform.x509.pem ${SIGNING_FOLDER}/platform.pk8 ${released_unsigned_apk} ${APKS_OUT_FOLDER}/${name}.apk
        else
                cp ${released_unsigned_apk} ${APKS_OUT_FOLDER}/${name}.apk
        fi

        cd ${YOUR_PATH}
}

pre_reqs

trap ctrl_c INT
function ctrl_c() {
        echo "Ctrl-C Pressed Exiting!"
        exit;
}


cd ${YOUR_PATH}
applist=`./print_release_apps_sun.sh`
model="jet"
# parse options
while getopts s:m:x:y flag; do
    case $flag in
        s)
            echo "Signing the Apps---------------------"
            sign_apps=1
            ;;
        m)
            echo "-m used: $OPTARG";
            if [[ ${OPTARG} == "snow" ]]; then
                applist=`./print_release_apps.sh`
                echo "Buliding snow apps-------------------"
		model="snow"
		
            else
                applist=`./print_release_apps_sun.sh`
                echo "Building sun apps--------------------"
		model="jet"
            fi
            ;;
        x)
            applist=`./print_release_apps.sh`
            echo "Buliding snow apps-------------------"
	    model="snow"
            ;;
        y)
            applist=`./print_release_apps_sun.sh`
            echo "Buliding snow apps-------------------"
	    model="jet"
            ;;
        ?)
            exit 1;
            ;;
    esac
done

ordered_applist=
for i in $applist
do
        setup_activity_name $i
        if [ -e ${APKS_OUT_FOLDER}/${name}.apk ]; then
                ordered_applist="${ordered_applist} $i"
        else
                ordered_applist="$i ${ordered_applist}"
        fi
done
echo -e "\033[1;32mBuilding Recon Apps in this order:\033[m"
echo -e "\033[1;32m=======================================\033[m"
echo -e "- jas_backbone/Dashlauncher_element"
echo -e "- Android_MobileSDK/HUDConnectivity"
echo -e "- Android_MobileSDK/MobileConnectLibrary"
for i in $ordered_applist
do
        setup_activity_name $i
        echo -e "- $name in $i"
done
echo -e "\033[1;32m=======================================\033[m"


# We prepare the mass storage:
echo -e "Prepare external storage"
cd ${YOUR_PATH}/StorageDependencies/
bash install_dependencies.bash $model build
cd ${YOUR_PATH}
# End of mass storage

cd ${YOUR_PATH}/deprecated/ReconCommonWidgets;
android update project -p . -t android-16
ant debug

cd ${YOUR_PATH}/Android_MobileSDK/HUDConnectivity/
bash make_app.bash

cd ${YOUR_PATH}/Android_MobileSDK/ReconHUDServiceHelper/
bash make_app.bash

cd ${YOUR_PATH}/Android_MobileSDK/MobileConnectLibrary/
bash make_app.bash

cd ${YOUR_PATH}/Android_MobileSDK/HUDPhoneStatusExchange/
bash make_app.bash

cd ${YOUR_PATH}/Android_MobileSDK/SocialMessagesLibrary/
bash make_app.bash

cd ${YOUR_PATH}/Android_MobileSDK/TimeSync/
android update project -p . -t android-16

cd ${YOUR_PATH}/Android_MobileSDK/AssistedGps/
android update project -p . -t android-16

cd ${YOUR_PATH}/libraries/Dashlauncher_element
bash make_app.bash;

cd ${YOUR_PATH}/libraries/isOakleyDecider;
android update project -p . -t android-16
ant debug

cd ${YOUR_PATH}/libraries/ReconUtils/;
android update project -p . -t android-16
ant debug

cd ${YOUR_PATH}/MapSDK;
android update project -p . -t android-16
ant debug

cd ${YOUR_PATH}/libraries/ReconOSCommonWidgets;
android update project -p . -t android-16
ant debug

cd ${YOUR_PATH}/libraries/ReconMessageAPI/;
android update project -p . -t android-16
ant debug

cd ${YOUR_PATH}/libraries/ReconPhoneProvider/;
android update project -p . -t android-16
ant debug

cd ${YOUR_PATH}/libraries/Breadcrumb/;
android update project -p . -t android-16
ant debug

cd ${YOUR_PATH}/libraries/ExternalSensorsService/;
android update project -p . -t android-16
ant debug

cd ${YOUR_PATH}/ReconMessageCenter;
android update project -p . -t android-16
ant debug



for i in $ordered_applist
do
        build_app $i
done

exit 0















