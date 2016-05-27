./releasetools/ota_from_target_files -v -k security/testkey -i images/official_zip/signed-target-files-3.1.zip images/official_zip/signed-inc-target-files.zip update.bin
ls -l update.bin
echo "To Push the file - run the following command:"
echo "./adb push update.bin /mnt/sdcard/ReconApps/cache/update.bin"
