versionname="McGill_RC1_"
versionnum=`cat ../svn_info | awk '/Revision/ {print $2}'`;
echo $versionnum

sed -r -ibak s/\(android\:versionCode.*\?=.*\?\)\"\(.*?\)\"/\\1\"$versionnum\"/ AndroidManifest.xml
sed -r -ibak s/\(android\:versionName.*\?=.*\?\)\"\(.*?\)\"/\\1\"$versionname\"/ AndroidManifest.xml
