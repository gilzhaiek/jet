#!/bin/bash

#sudo su postgres
#export JAVACMD_OPTIONS="-Xmx3G -Djava.io.tmpdir=/media/passport/tmp/"
#export JAVACMD_OPTIONS="-Xmx3G -Djava.io.tmpdir=/media/db119/tmp/"
export JAVACMD_OPETIONS="Xmx3G -Djava.io.tmpdir=tmp/"
echo $JAVACMD_OPTIONS
#python ReconMain.py;

#bash get_next_item.bash
for ((i=0; i< 1000; i++)) 
    do
    cat NextItem.txt
    python ReconMain.py;
    echo "ended. next item."
    sleep 3; 
    bash get_next_item.bash
done

#bash ReconTileFetcherByCentralLocation.sh

echo "done."






