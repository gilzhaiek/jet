This is GPS Char Driver for Texas Instrument's Connectivity Chip.

--- Building GPS driver---

1. Set kernel path,
 export KERNEL_PATH=<path to Linux kernel>

2. Set cross compiling toolchain path,
 export CROSS_COMPILE=<path to the cross compiling toolchain>

NOTE: Both Linux kernel and GPS driver should be built using the 
same cross compiling toolchain.

3. Set the cpu architecture,
 export ARCH=<cpu architecture>
 ex. export ARCH=arm

4. Build GPS driver,
 make

5. Clean,
 make clean
