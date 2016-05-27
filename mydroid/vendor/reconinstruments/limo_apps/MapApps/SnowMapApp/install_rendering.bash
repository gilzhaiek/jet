cp res/raw/rendering_schemes_limo.xml ../../../MapData/rendering_schemes_limo.xml
cp res/raw/rendering_schemes_jet.xml ../../../MapData/rendering_schemes_jet.xml
cp res/raw/rendering_schemes_limo.xml ../../ExternalStorage/ReconApps/MapData/rendering_schemes_limo.xml
cp res/raw/rendering_schemes_jet.xml ../../ExternalStorage/ReconApps/MapData/rendering_schemes_jet.xml
sudo adb push ../../../MapData/rendering_schemes_limo.xml /mnt/sdcard/ReconApps/MapData/rendering_schemes_limo.xml
sudo adb push ../../../MapData/rendering_schemes_jet.xml /mnt/sdcard/ReconApps/MapData/rendering_schemes_jet.xml
