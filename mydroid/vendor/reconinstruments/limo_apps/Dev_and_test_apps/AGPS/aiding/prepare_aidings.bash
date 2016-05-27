#!/bin/bash

RAW_DATA_FOLDER_SERVER=/data/gps/aiding/reconephemeris/
#RAW_DATA_FOLDER_INJECT=/data/data/

FILE_NAME_EPH_PRE=eph_
FILE_NAME_EPH_EXT=.raw

FILE_NAME_ALMANAC=almanac_YUMA.alm

cd /home/patrickcho/recon/jet_jet/tools/AGPS/aiding

if [ `cat server.conf | wc -l` -lt "2" ]; then
    echo ":: FIRST RUN:: RECORD DATE "
    date >> server.conf
else
    echo ":: PREVIOUS RUN WAS - `sed -n "2p" server.conf` "
    while true; do
	read -p ":: DO YOU WISH TO PULL THE DATA? [yn] " yn
	case $yn in
            [Yy]* ) sed -i "2c`date`" server.conf; break;;
            [Nn]* ) exit;;
            * ) echo "Yes or No or Y or N or y or n.";;
	esac
    done
fi

echo "ERASING ALL PREVIOUS EPHEMERIS FILES..."
rm *.raw
echo "DONE"

echo "DOWNLOADING ALMANAC ... "
curl http://www.navcen.uscg.gov/\?pageName\=currentAlmanac\&format\=yuma > almanac_YUMA.alm

# PULL EPHEMERIS FILES
echo "DONE! PULLING ALL EPHEMERIS FILES FROM SERVER UNIT DEVICE - `head -1 server.conf` ..."
adb -d -s `head -1 server.conf | xargs echo` pull $RAW_DATA_FOLDER_SERVER

if [ $? -eq 0 ]; then
    # FIND THE ONES WITHOUT EXTENSION, WHICH ARE EPHEMERIS FILE FROM SERVER UNIT
    echo "SUCCESSFULLY PULLED ALL EPHEMERIS FILES, total ephemeris `ls | grep -v '\\.' | wc -l`"
    for i in `ls | grep -v "\\."`; do mv $i $FILE_NAME_EPH_PRE$i$FILE_NAME_EPH_EXT; done
else
    echo "FAILED TO PULL DATA"
fi

# copy to dropbox for testing (patrick)
cp *.raw *.alm ~/Dropbox/Public/aiding/
cd ~/Dropbox/Public/aiding/
rm aiding.zip
zip -r aiding.zip ./*
cd -
