update the inputGeoRegions.txt file to include all possible georegions that have had tiles produced

run following line to get list of current S3 map tiles into the file “existingtiles.txt”

aws s3 ls geotilebucket/1/base/ > existingtiles.txt


then run s3TileAnalyser from command line (see instructions below)

this produces several csv files in /output/analysis/
- regionalTileAnalysis_<date>.csv    - a list of %tile completion in each region
- extraTiles_<date>.csv        - a list of tiles found on S3 but not in regions listed in inputGeoRegions.txt
- oldFormatS3Tiles_<date>.csv  - a list of S3 tiles that use the pre-nov-2014, 8-digit naming format - these tiles should be removed from S3

for every region in inputGeoRegions.txt, a separate cdv is generated under the /output/regionDetails/<region name>/<region name>_completedTiles_<date>.csv

the program also produces a plotting file /plotting/existingtles.out for use with the matlab visualization function plottiles.m in /plotting.  To plot the existing S3 tiles in a world map png, cd to /plotting and run plottiles.m in Octave or Matlab.



running from command line
=========================
cd ../s3TileAnalyser
java -cp bin s3TileAnalyser

which uses inputGeoRegions.txt as input regions and puts output as described above


OR    for custom region, output put to condole

cd ../s3TileAnalyser
java -cp bin s3TileAnalyser "[<regionName>;<leftLong>;<bottomLat>;<rightLong>;<topLat>]"

(with no spaces in arg) for example

java -cp bin s3TileAnalyser "[customRegion;-123.4;49.2;-123.1;49.5]"
