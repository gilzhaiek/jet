#!/bin/bash

#obtain absolute path to workspace
WORKSPACE=$1
#path to store test results and log
RESULTFOLDER=/media/o-drive/Technology/Programs/Jet/Jet-Sunglasses/Embedded/Software/jenkins-backup/Jet-Cts-Results/

#working directory
cd $WORKSPACE/mydroid

#android-cts default repository for results/logs/testcases/plans
CTSRESULTS=out/host/linux-x86/cts/android-cts/repository/results
#android-cts default repository for logs
CTSLOGS=out/host/linux-x86/cts/android-cts/repository/logs
#android-cts default repository for testcases
CTSTESTCASES=out/host/linux-x86/cts/android-cts/repository/testcases
#android-cts default repository for test plans
CTSTESTPLANS=out/host/linux-x86/cts/android-cts/repository/plans
#folder with CTS Tests
DIR=cts/tests/tests/JET

echo
echo "Setting up environment..."

#setup environment
. build/envsetup.sh
lunch 15

#setup cts and all required cts tools
cp -r $WORKSPACE/tools/CtsTest/make/cts out/host/linux-x86/

#make sure all required folders in cts/android-cts/repository exists
if [ ! -e $CTSLOGS ];
then
    mkdir $CTSLOGS
fi
if [ ! -e $CTSTESTPLANS ];
then
    mkdir $CTSTESTPLANS
fi
if [ ! -e $CTSRESULTS ];
then
    mkdir $CTSRESULTS
fi

#building and setting up minimum required cts tools
make cts-tradefed
mv out/host/linux-x86/framework/cts-tradefed.jar out/host/linux-x86/cts/android-cts/tools/
make cts-xml-generator
make cts-java-scanner
make cts-java-scanner-doclet

#init ReconJetSDK test plan
echo "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<TestPlan version=\"1.0\">" > $CTSTESTPLANS/ReconJetSDK.xml.temp

echo "Building the CTS tests and corresponding XML files..."
#loop through all CTS tests in directory $DIR
for f in $DIR/*/
do
    #variables to make CTS testcase xml file
    SUBSOURCE="$(basename $f)"
    PACKAGE=$(grep "LOCAL_PACKAGE_NAME" $f/Android.mk | grep -oh "Cts\w*")
    NAME='reconinstruments.JETSDK.'$(basename $f)

    #build project
    cd $f
    mm
    cd -

    echo "Copying apk '$WORKSPACE/mydroid/out/target/product/jet/data/app/$PACKAGE.apk' to '$WORKSPACE/mydroid/out/host/linux-x86/cts/android-cts/repository/testcases/'...."

    #copy built apk to CTS folder
    rm $CTSTESTCASES/$PACKAGE.apk
    rm $CTSTESTCASES/$PACKAGE.xml
    cp out/target/product/jet/data/app/$PACKAGE.apk $CTSTESTCASES/$PACKAGE.apk

    echo "Creating CTS XML file with the following parameters..."
    echo "source_root: cts/tests/tests/JET/$SUBSOURCE"
    echo "test_name: \"$NAME\""
    echo "package_name: \"$PACKAGE\""
    echo "...."
    echo

    #generate CTS testcase xml file
    cts-java-scanner \
        -s cts/tests/tests/JET/$SUBSOURCE \
        -d out/host/linux-x86/framework/cts-java-scanner-doclet.jar | \
        cts-xml-generator \
        -p $NAME \
        -n $PACKAGE \
        -m cts/tests/tests/JET/$SUBSOURCE/AndroidManifest.xml \
        -e cts/tests/expectations/knownfailures.txt \
        -o $CTSTESTCASES/$PACKAGE.xml

    #add testcase to ReconJetSDK testplan
    #excludes test case for HUDAshmem as it is not completed yet...
    if [ "$NAME" != "reconinstruments.JETSDK.HUDAshmem" ];
    then
        echo "  <Entry uri=\"$NAME\"/>" >> $CTSTESTPLANS/ReconJetSDK.xml.temp
    fi
done

echo
echo "Creating ReconJetSDK test plan"

#finish making ReconJetSDK testplan
rm $CTSTESTPLANS/ReconJetSDK.xml
echo "</TestPlan>" >> $CTSTESTPLANS/ReconJetSDK.xml.temp
mv $CTSTESTPLANS/ReconJetSDK.xml.temp $CTSTESTPLANS/ReconJetSDK.xml

#check if JET is detected or not
#if not detected, exit test
if [ $(adb get-state) != "device" ];
then
    echo "JET not detect..."
    echo "Cannot run CTS test without a detected JET... EXIT!"
    exit 1
fi

#run ReconJetSDK testplan
echo
echo "exit" | ./out/host/linux-x86/cts/android-cts/tools/cts-tradefed run cts --plan ReconJetSDK

#remove last CTS test results and logs
rm -r $RESULTFOLDER/* 

#create new CTS test results and logs
TESTRESULT="$(ls -t $CTSRESULTS/ | head -1)"
TESTLOG="$(ls -t $CTSLOGS/ | head -1)"

echo "Moving $TESTRESULT as test results"
echo "Moving $TESTLOG as test logs"

#give result folder the correct name and attributes
mv $CTSRESULTS/$TESTRESULT $CTSRESULTS/$MASTER_NAME-CTSResults-$TESTRESULT

#give log folder the correct name and attributes
mv $CTSLOGS/$TESTLOG $CTSLOGS/$MASTER_NAME-CTSLog-$TESTLOG
cd $CTSLOGS
zip -r $MASTER_NAME-CTSLog-$TESTLOG.zip $MASTER_NAME-CTSLog-$TESTLOG
cd -

#copy results & logs to backup folder $RESULTFOLDER
cp -r $CTSRESULTS/$MASTER_NAME-CTSResults-$TESTRESULT $RESULTFOLDER 
cp -r $CTSLOGS/$MASTER_NAME-CTSLog-$TESTLOG.zip $RESULTFOLDER
