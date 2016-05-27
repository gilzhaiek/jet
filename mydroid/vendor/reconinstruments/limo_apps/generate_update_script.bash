# Code that manually uninstalls
# usage: bash generate_update_script.bash <AppFolder> <destination>
app_folder=$1
dest=$2
# Extract package name 
package_name=`cat $app_folder/AndroidManifest.xml | awk 'BEGIN{FS="="}/package/ {myvar=$2;gsub(/\"/,"",myvar); print myvar}'`

# generate commands
# delete data
echo "pm uninstall "$package_name
