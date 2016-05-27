Please read:
http://omappedia.org/wiki/Android_eMMC_Booting

File list:
MLO
u-boot.bin
recovery.img
boot.img
system.img
cache.img
userdata.img

eMMC binaries

This is the efi partition table as exists on the emmc
Sector#    Size Name
    256     128K xloader
    512     256K bootloader
   2048       8M recovery                                                      
  18432       8M boot                                                          
  34816     512M system                                                        
1083392     256M cache                                                         
1607680     512M userdata                                                      
2656256    2183M media
[edit]Creating the GPT table

On Target
Connect a USB cable to the OTG port on your platform
Boot your platform up with a stock u-boot and MLO
Once you platform is booted you will see the following:
Fastboot entered...
On Host Machine
locate fastboot in you android filesystem
cd $mydroid/out/host/linux-x86/bin/fastboot
Search for fastboot devices
fastboot devices
Create GPT table on eMMC/SD card
fastboot oem format
From the android build these are the binaries that go into each partition:
 Sector#    Size Name            Binary
    256     128K xloader         MLO
    512     256K bootloader      u-boot.bin
   2048       8M recovery        recovery.img                                                    
  18432       8M boot            boot.img                                  
  34816     512M system          system.img                                      
1083392     256M cache           cache.img                                    
1607680     512M userdata        userdata.img                                     
2656256    2183M media           none

File locations
MLO --> x-loader/MLO
u-boot --> u-boot/u-boot.bin
boot.img --> need to create using zImage + ramdisk.img
recovery.img ---> need to create using zImage + ramdisk-recovery.img
system.img --> $mydroid/out/target/product/<platform>/system.img
cache.img -->
userdata.img --> $mydroid/out/target/product/<platform>/userdata.img

All these partitions can be flashed with the given binary using fastboot.
 fastboot flash <name> <binary>
Example flashing of all partitions
sudo ./fastboot flash xloader     MLO
sudo ./fastboot flash bootloader  u-boot.bin
sudo ./fastboot reboot-bootloader
sleep 5
sudo ./fastboot flash recovery    recovery.img
sudo ./fastboot flash boot        boot.img
sudo ./fastboot flash system      system.img
sudo ./fastboot flash cache       cache.img
sudo ./fastboot flash userdata    userdata.img
[edit]Modifying .IMG Files

Typically when you want to modify any of the partitions, you would need to unzip-modify-rezip and then fastboot flash.
Following section talks about how to do that for each partition
BOOT.IMG
 boot.img = zImage + ramdisk.img
 zImage = kernel image
 ramdisk.img = out/target/product/blaze/root/
 %./out/host/linux-x86/bin/mkbootimg 
 --kernel zImage 
 --ramdisk ramdisk.img 
 --base 0x80000000 
 --cmdline "console=ttyO2,115200n8 mem=456M@0x80000000 mem=512M@0xA0000000 init=/init vram=10M omapfb.vram=0:4M androidboot.console=ttyO2" 
 --board omap4 
 -o boot.img.new
 Output: boot.img.new
 **Note: bootarg is passed to kernel via --cmdline option above
 **Note：For Pandaboard ES,--cmdline should be mmodified as "console=ttyO2,115200n8 mem=456M@0x80000000 mem=512M@0xA0000000 init=/init vram=32M omapfb.vram=0:16M androidboot.console=ttyO2"
To "just" boot boot.img (before flashing) you can use:
%fastboot boot boot.img
RAMDISK.IMG
 %mkdir root
 %cd root
 %gunzip -c ../ramdisk.img | cpio -i
 <make changes to root/ contents...>
 %./out/host/linux-x86/bin/mkbootfs root/ | ./out/host/linux-x86/bin/minigzip >ramdisk.img.new
 #output: ramdisk.img.new
 ** Note: any init.rc changes will need to use this method
RECOVERY.IMG
Is just like boot.img. 
recovery.img = zImage + ramdisk-recovery.img
*Follow the same steps as boot.img for packing/unpacking
SYSTEM.IMG
 #uncompress
 %./out/host/linux-x86/bin/simg2img system.img system.img.raw
 #mount to directory mnt-point/
 %mkdir mnt-point
 %sudo mount -t ext4 -o loop system.img.raw mnt-point/
 #modify any .so or apk in the mnt-point/ directory
 #rezip
 %sudo out/host/linux-x86/bin/make_ext4fs -s -l 512M -a system system.img.new mnt-point/
 %sudo umount mnt-point/
 Output: system.img.new
Instead of having to reflash the whole big system.img, one can selective update any binary in /system folder on running target
%adb remount
%adb push <local> <remote>
Eg: 
%adb remount
%adb push out/target/product/blaze/obj/lib/overlay.omap4.so /system/lib/hw/overlay.omap4.so
%adb sync
USERDATA.IMG
 #uncompress
 %./out/host/linux-x86/bin/simg2img userdata.img userdata.img.raw
 #mount to directory mnt-point/
 %mkdir mnt-point
 %sudo mount -t ext4 -o loop userdata.img.raw mnt-point/
 #modify any .so or apk in the mnt-point/ directory
 #rezip
 #%sudo ./out/host/linux-x86/bin/make_ext4fs -s -l 512M -a userdata userdata.img.new mnt-point/
 # Above command won't work on GB/HC. For GB/HC, please use the following updated command
 %sudo ./out/host/linux-x86/bin/make_ext4fs -s -l 512M -a data userdata.img.new mnt-point/
 %sudo umount mnt-point/
 Output: userdata.img.new
CACHE.IMG
 #This is empty ext4 fs image
 %mkdir mnt-point/
 %sudo ./make_ext4fs -s -l 256M -a cache cache.img mnt-point/
 Output: cache.img
[edit] TI Android build setup

Copy kernel zImage, u-boot.bin and MLO for your board in folder device/ti/blaze/boot/.
Rename as:
 %mv MLO MLO_es2.2_emu 
 or 
 %mv MLO MLO_es2.2_gp 
 (based on your board being GP or EMU)
Next start standard android build and all img files are generated in:
out/target/product/blaze/*.img
A script is introduced in TI Android release to make this flashing process easier: device/ti/blaze/boot/fastboot.sh
Usage:
cd device/ti/blaze/boot/
%fastboot.sh --emu
or
%fastboot.sh --gp
Running this script will flash whole android system on your board.

########Extract boot.img###
perl split_bootimg.pl boot.img
# Then you got two images: boot.img-kernel for zImage in our case; And boot.img-ramdisk.gz for zipped ramdisk

# Then extract ramdisk by
gzip -dc boot.img-ramdisk.gz > ramdisk



