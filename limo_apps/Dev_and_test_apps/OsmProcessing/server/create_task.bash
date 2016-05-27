address=$1
padding=$2
cd ..
python ./ReconTileFetcherByName.py -m "City2Task" -a "$address" -p $padding
