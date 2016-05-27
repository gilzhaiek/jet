#This script is based on mydroid/build/core/Makefile
out/host/linux-x86/bin/mkbootfs out/target/product/jet/root | out/host/linux-x86/bin/minigzip > out/target/product/jet/ramdisk.img
out/host/linux-x86/bin/mkbootimg  --kernel out/target/product/jet/kernel  --ramdisk out/target/product/jet/ramdisk.img --base 0x80000000 --output out/target/product/jet/boot.img
