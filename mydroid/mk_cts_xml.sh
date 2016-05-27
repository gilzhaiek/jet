#!/bin/bash
# declare STRING variable

echo
echo -e "Do you want to proceed and create CTS XML(y/N): \c "
read COND

if [ "$COND" = "y" ];
then
    #get cts tests source directory
    DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )/cts/tests/tests/
    echo -e "Enter source_root: $DIR\c"
    read SOURCE 
    DIR=$DIR$SOURCE
    echo "DIR: $DIR"

    #check if cts test source directory exists
    if [ -e $DIR ];
    then
        echo -e "Build all Sub Project Directories instead of current folder (y/N): \c"
        read BUILD

        #build all projects under source folder
        if [ "$BUILD" = "y" ];
        then
            for f in $DIR/*/
            do
                #variables to make cts testcase xml file
                SUBSOURCE=$SOURCE/"$(basename $f)"
                DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )/out/host/linux-x86/cts/android-cts/repository/plans
                DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )/out/host/linux-x86/cts/android-cts/repository/plans
                DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )/out/host/linux-x86/cts/android-cts/repository/plans
                PACKAGE=$(grep "LOCAL_PACKAGE_NAME" $f/Android.mk | grep -oh "Cts\w*")
                NAME='reconinstruments.'$(basename $f)

                echo
                echo "Copying apk 'out/target/product/jet/data/app/$PACKAGE.apk' to 'out/host/linux-x86/cts/android-cts/repository/testcases/'...."

                if [ -e ~/Development/jet/mydroid/out/target/product/jet/data/app/$PACKAGE.apk ];
                then
                    #copy built apk to cts folder
                    rm out/host/linux-x86/cts/android-cts/repository/testcases/$PACKAGE.apk
                    rm out/host/linux-x86/cts/android-cts/repository/testcases/$PACKAGE.xml
                    cp out/target/product/jet/data/app/$PACKAGE.apk out/host/linux-x86/cts/android-cts/repository/testcases/$PACKAGE.apk

                    echo
                    echo "Creating CTS XML file with the following parameters..."
                    echo "source_root: cts/tests/tests/$SUBSOURCE"
                    echo "test_name: \"$NAME\""
                    echo "package_name: \"$PACKAGE\""
                    echo "...."
                    echo

                    #generate cts testcase xml file
                    cts-java-scanner \
                        -s cts/tests/tests/$SUBSOURCE \
                        -d out/host/linux-x86/framework/cts-java-scanner-doclet.jar | \
                        cts-xml-generator \
                        -p $NAME \
                        -n $PACKAGE \
                        -m cts/tests/tests/$SUBSOURCE/AndroidManifest.xml \
                        -e cts/tests/expectations/knownfailures.txt \
                        -o out/host/linux-x86/cts/android-cts/repository/testcases/$PACKAGE.xml
                else
                    echo
                    echo "$PACKAGE.apk does not exist!"
                    echo "Please comple the $PACKAGE project before running this script!"
                    echo "exiting..."
                    exit
                fi
            done

            echo -e "Launch the cts_console to run CTS tests (y/N): \c"
            read COND

            #launch cts console
            if [ "$COND" = "y" ];
            then
                echo -e "Which type of test do you want to run (plan/package): \c"
                read TEST

                #run cts plan
                if [ "$TEST" = "plan" ];
                then
                    DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )/out/host/linux-x86/cts/android-cts/repository/plans
                    for f in $DIR/*.xml
                    do
                        FILE=$(basename "$f")
                        echo "${FILE%.*}"
                    done
                    #run cts testcase
                elif [ "$TEST" = "package" ];
                then
                    DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )/out/host/linux-x86/cts/android-cts/repository/testcases
                    for f in $DIR/*.xml
                    do
                        grep "appPackageName" $f | grep -oh "reconinstruments.\w*"
                    done
                else
                    echo "CTS does not support this type of testing...ABORT!"
                    exit
                fi
                echo -e "Choose from the above tests/plans to run: \c"
                read TESTCASE

                #run cts console
                (cd ~/Development/jet/mydroid/ && ./out/host/linux-x86/cts/android-cts/tools/cts-tradefed run cts --$TEST $TESTCASE)
            fi
        else
            #variables to make cts testcase xml file
            echo $SOURCE 
            PACKAGE=$(grep "LOCAL_PACKAGE_NAME" $DIR/Android.mk | grep -oh "Cts\w*")
            echo $PACKAGE
            NAME='reconinstruments.'$(basename $SOURCE)

            echo
            echo "Copying apk 'out/target/product/jet/data/app/$PACKAGE.apk' to 'out/host/linux-x86/cts/android-cts/repository/testcases/'...."

            if [ -e ~/Development/jet/mydroid/out/target/product/jet/data/app/$PACKAGE.apk ];
            then
                #copy built apk to cts folder
                rm out/host/linux-x86/cts/android-cts/repository/testcases/$PACKAGE.apk
                rm out/host/linux-x86/cts/android-cts/repository/testcases/$PACKAGE.xml
                cp out/target/product/jet/data/app/$PACKAGE.apk out/host/linux-x86/cts/android-cts/repository/testcases/$PACKAGE.apk

                echo
                echo "Creating CTS XML file with the following parameters..."
                echo "source_root: cts/tests/test/$SOURCE"
                echo "test_name: \"$NAME\""
                echo "package_name: \"$PACKAGE\""
                echo "...."
                echo

                #generate cts testcase xml file
                cts-java-scanner \
                    -s cts/tests/tests/$SOURCE \
                    -d out/host/linux-x86/framework/cts-java-scanner-doclet.jar | \
                    cts-xml-generator \
                    -p $NAME \
                    -n $PACKAGE \
                    -m cts/tests/tests/$SOURCE/AndroidManifest.xml \
                    -e cts/tests/expectations/knownfailures.txt \
                    -o out/host/linux-x86/cts/android-cts/repository/testcases/$PACKAGE.xml
            else
                echo
                echo "$PACKAGE.apk does not exist!"
                echo "Please comple the $PACKAGE project before running this script!"
                echo "exiting..."
                exit
            fi

            echo -e "Launch the cts_console to run the package just built (y/N): \c"
            read COND
            if [ "$COND" = "y" ];
            then
                #run cts consol
                (cd ~/Development/jet/mydroid/ && ./out/host/linux-x86/cts/android-cts/tools/cts-tradefed run cts --package $NAME)
            fi
        fi
    else

        echo
        echo "$DIR does not exist!"
        echo "Please make sure to enter the correct project root location!"
        echo "exiting..."
        exit
    fi

fi
