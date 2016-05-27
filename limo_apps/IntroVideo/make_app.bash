# setup video files
# if grep -i 'isOakley' AndroidManifest.xml |  grep -qic 'true'
# then
# 	echo 'setting up video softlinks for oakley...'
# 	cd res/raw/
# 	rm *.webm
# 	ln -s ../../videos/oakley_demo.webm .
# 	ln -s ../../videos/in_goggle_no_intro.webm .
# 	ln -s ../../videos/null_vid.webm in_goggle_after_intro.webm
# 	ln -s ../../videos/null_vid.webm in_goggle_intro_no_status.webm
# 	cd ../..
# 	echo 'done'
# else
# 	echo 'setting up video softlinks for recon...'
#         cd res/raw/
#         rm *.webm
#         ln -s ../../videos/in_goggle_after_intro.webm .
#         ln -s ../../videos/in_goggle_intro_no_status.webm .
#         ln -s ../../videos/null_vid.webm oakley_demo.webm
#         ln -s ../../videos/null_vid.webm in_goggle_no_intro.webm
#         cd ../..
#         echo 'done'
# fi

name=IntroVideo
foldername=IntroVideo
platform=$1
if [ "$platform" = "" ]; then
platform="jet"
fi

android update project -p . -n $name -t android-17
ant clean
ant release
cp bin/$name*-unsi*.apk ../signing/$platform
cd ../signing/$platform/
java -jar signapk.jar platform.x509.pem platform.pk8 $name*-unsigned*.apk $name-release.apk
mv $name-release.apk ../../$foldername/bin/
rm $name*-unsigned*.apk
cd ../../$foldername
