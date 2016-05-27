#!/bin/bash
echo 'push osm tiles into jet devices.'
sudo adb push map_recon_2014-09-25_LowMainLand_compressed/ /sdcard/ReconApps/GeodataService/PreloadedOSMTiles/

echo 'done.'
