op=-n

if [ $# -eq 1 ]
then
    if [[ "$1" == -* ]]
    then
        if [[ "$1" == *b* ]] # Break Lines
        then
            op=
        fi
    fi
fi

#Format is echo ${op} "<app_name> "  // Must have a space in the end of the app name
echo ${op} "BLEServiceSS1 "
echo ${op} "CompassSensor "
echo ${op} "JetConnectDevice "
echo ${op} "JetMusic "
echo ${op} "JetDashSettings "
echo ${op} "DashWarning "
echo ${op} "Dashlauncher_redux "
echo ${op} "DashLivestats "
echo ${op} "HUDService "
echo ${op} "MfiTester "
echo ${op} "OffsetKeyboard "
echo ${op} "Phone "
echo ${op} "ReconAppInstaller "
echo ${op} "ReconApplauncher "
echo ${op} "ReconBLE_java "
echo ${op} "ReconCamera "
echo ${op} "ReconCompass "
echo ${op} "ReconItemHost "
echo ${op} "ReconMyActivities "
echo ${op} "ReconSunMessageCenter "
echo ${op} "ReconPowerMenu "
echo ${op} "ReconQuickActions "
echo ${op} "SocialSharing "
echo ${op} "ServiceWithCallBack "
echo ${op} "mapApp "
echo ${op} "GeodataService_soft_link "
echo ${op} "JetUnpairDevice "
echo ${op} "Welcome "
echo ${op} "AssistedGps_RX "
echo ${op} "rxn_services_xybrid "
echo ${op} "JetSensorConnect "
echo ${op} "LispXml "
echo ${op} "SymptomChecker "
echo ${op} "LockDown "
echo ${op} "PasscodeLock "
echo ${op} "ReconSystemUI "
