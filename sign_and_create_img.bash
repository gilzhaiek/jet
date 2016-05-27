cd mydroid
./build/tools/releasetools/sign_target_files_apks -d ./vendor/recon/security/jet out/dist/full_jet-target_files-eng.gil.zip ../omap4_emmc_files_jet/signed-target-files.zip
./build/tools/releasetools/img_from_target_files ../omap4_emmc_files_jet/signed-target-files.zip ../omap4_emmc_files_jet/signed-img.zip
cd ..

