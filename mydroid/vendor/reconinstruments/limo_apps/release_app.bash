# usage bash release_app.bash <app_folder_name> <limodroid/out/target/product/limo/root/data/app/>
this=`pwd`
cd $1
ant clean
bash ../update_android_manifest_with_version.bash
bash make_app.bash 
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
cd $this
cd $2
git rm -f $filebase*.apk
# just to make sure:
rm $filebase*.apk
cd $this
cd $1
cd bin
mv $finalname $2
cd $this
cd $2
git add $finalname
cd $this


