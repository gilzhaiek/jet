#!/bin/bash

#sudo su postgres
#export JAVACMD_OPTIONS="-Xmx3G -Djava.io.tmpdir=/media/passport/tmp/"
export JAVACMD_OPTIONS="-Xmx3G -Djava.io.tmpdir=/media/mapdata/tmp/"
#export JAVACMD_OPTIONS="-Xmx3G -Djava.io.tmpdir=tmp/"

echo $JAVACMD_OPTIONS

#python ReconMain.py;


for ((i=0; i< 300; i++)) 
    do python ReconMain.py;
    echo crashed, restart......; 
    sleep 10; 
done

#bash ReconTileFetcherByCentralLocation.sh

echo "done."






