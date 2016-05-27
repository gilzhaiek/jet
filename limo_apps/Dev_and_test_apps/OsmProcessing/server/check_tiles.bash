address=$1
padding=$2
cd ..
python ./ReconTileFetcherByName.py -m "City2TileIds" -a "$address" -p $padding | bash is_on_s3.bash | awk '{sum=sum+1.0}/not/{nots=nots+1.0;print $0}END{print "-------------"; print "Total tiles: "sum; print (1-(nots/sum))*100"% mapped"}'






