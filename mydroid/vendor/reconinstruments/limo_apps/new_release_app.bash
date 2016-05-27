# usage bash release_app.bash <app_folder_name> <apk_dest> <platform>
this=`pwd`
appfolder=$1;
apkdest=$2;
platform=$3;
cd $appfolder
ant clean
bash ../update_android_manifest_with_version.bash
bash make_app.bash $platform
cd bin
filebase=`ls *-release.apk | awk 'BEGIN{FS="-"}{print $1}'`
cd ..
echo "Current dir" `pwd`
# We face svn info until we fix our build system.
versionn=`cat ../svn_info | awk 'BEGIN{FS=" "}/Revision/{print $2}'`
cd bin
finalname=`echo $filebase"_r"$versionn".apk"` 
echo "filebase "$filebase
echo "version "$versionn
echo "filename "$finalname
cp `echo $filebase"-release.apk"` $finalname
echo "going to "$this
cd $this
cp ${appfolder}/bin/${finalname} $apkdest
cd $this


