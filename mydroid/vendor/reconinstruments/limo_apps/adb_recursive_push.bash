#code that recursively pushes to teh mass storage
# usage: adb_recursive <source_folder> <path_on_mass_storage>
source_folder=$1
dest_folder=$2
cd $source_folder;
for i in `find . -type d`; do
    thedirname=`dirname $i`;
    adb -d shell "mkdir -p ${dest_folder}/$thedirname";
    echo basename is `basename $i`;
    if [ `basename $i` = "." ]; then
	echo skipping .
    elif [ `basename $i` = "" ]; then
	echo skipping EMPTY...
	
    else
 	adb -d push $i ${dest_folder}/$i;
    fi
done
