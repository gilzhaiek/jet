#!/bin/bash
# =============================================================================
# Call "./resize_img.sh" to resize cache partition
# Call "./resize_img.sh -s" to resize system partition
# =============================================================================
IMAGE=cache

if [ $# -eq 1 ]; then
    if [[ "$1" == -* ]]; then
        if [[ "$1" == *s* ]]; then
            IMAGE=system
        fi
    fi
fi

function generate_cache_image {
    #Remove the old image first
    if [ -f cache.img ]; then
        rm cache.img
    fi
    # ------------------------------------------------------------------
    #The comment code below is the original way to generate 128MB
    #cache size and took from 
    #http://www.omappedia.com/wiki/4AJ.2.1_OMAP4_Jelly_Bean_Release_Notes
    # ------------------------------------------------------------------

    #dd if=/dev/zero of=./cache.img bs=1048510 count=128
    #mkfs.ext4 -F cache.img -L cache
    
    rm -rf ./fastboot-cache
    mkdir ./fastboot-cache
    ./make_ext4fs -s -l 256M -a cache cache.img ./fastboot-cache/
    rm -rf ./fastboot-cache
    echo -e "\033[1;32m------ DONE Creating Cache image------\033[m"
}

function regenerate_system_image {
    resizefail=0
    #Back up original image
    cp system.img system.img.orig

    rm -rf ./fastboot-system
    mkdir ./fastboot-system
    ./simg2img system.img system.img.raw
    mount -t ext4 -o loop system.img.raw ./fastboot-system ||resizefail=1
    if [ $resizefail -eq 1 ]; then
        echo -e "\033[1;31m--Mount failed--\033[m"
        return
    fi
    ./make_ext4fs -s -l 512M -a system system.img ./fastboot-system/
    sync
    umount ./fastboot-system
    sync
    rm -rf ./fastboot-system
    rm -rf system.img.raw

    echo -e "\033[1;32m------ DONE Creating System Image and Rename Original one to system.img.orig ------\033[m"
}


if [ ${IMAGE} == system ]; then
    echo -e "\033[1;32m------ Regenerate System Image (system.img) based on uboot partition size------\033[m"
    regenerate_system_image
else
    echo -e "\033[1;32m------ Creating Cache image based on uboot partition size------\033[m"
    generate_cache_image
fi
