for i in `bash print_release_apps.sh -b `; do cd $i; cat AndroidManifest.xml | awk 'BEGIN{FS="="}/package=/ {print "adb -d uninstall "$2}'; cd ..; done | bash
