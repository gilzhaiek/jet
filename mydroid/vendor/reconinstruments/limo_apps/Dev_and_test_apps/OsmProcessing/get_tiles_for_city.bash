#!/bin/bash
# Example usage:
#./get_tiles_for_city.bash "Vancouver, BC, Canada" tmp_tiles/
destination=$2;
mkdir -p $destination;
for i in `python ReconTileFetcherByName.py -a $1`; do  wget https://s3.amazonaws.com/geotilebucket/1/base/$i.rgz -O $destination/$i.rgz; done
