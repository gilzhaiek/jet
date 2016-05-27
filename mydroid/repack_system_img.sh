mkuserimg.sh out/target/product/jet/system out/target/product/jet/obj/PACKAGING/systemimage_intermediates/system.img ext4 system 576716800 out/target/product/jet/root/file_contexts
make_ext4fs  -S out/target/product/jet/root/file_contexts -l 576716800 -a system out/target/product/jet/obj/PACKAGING/systemimage_intermediates/system.img out/target/product/jet/system
cp out/target/product/jet/obj/PACKAGING/systemimage_intermediates/system.img out/target/product/jet/system.img

