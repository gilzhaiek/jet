#:wexport BUILD_TYPE=emulator
PWD = $(shell pwd)

ARM_TOOLCHAIN_PATH = /home/srinivas/GB/27-GB-ENG-1/arm-2010q1/bin
#export JAVA_DIR    = /usr/lib/jvm/java-1.5.0-sun/include
export JAVA_DIR    = /usr/lib/jvm/java-6-sun-1.6.0.24/include
TI_BSP_PATH      = /home/srinivas/GB/27-GB-ENG-1/mydroid
export ANDROID_SDK_PATH = /home/srinivas/GB/27-GB-ENG-1/android-sdk-linux_x86-1.6_r1

export DEX = $(ANDROID_SDK_PATH)/tools/dx
export JAVAC = javac
export AAPT = $(ANDROID_SDK_PATH)/tools/aapt
export APK = $(ANDROID_SDK_PATH)/tools/apkbuilder
export BOOTCLASSPATH = $(PWD)/../Binaries/jars/classes.jar:$(ANDROID_SDK_PATH)/android.jar

export MYDROID_PATH = $(TI_BSP_PATH)
ifeq ($(BUILD_TYPE), emulator)
# Emulator
export CC = $(MYDROID_PATH)/mydroid/prebuilt/linux-x86/toolchain/arm-eabi-4.2.1/bin/arm-eabi-gcc
export CPP =$(MYDROID_PATH)/mydroid/prebuilt/linux-x86/toolchain/arm-eabi-4.2.1/bin/arm-eabi-g++
export LD = $(MYDROID_PATH)/mydroid/prebuilt/linux-x86/toolchain/arm-eabi-4.2.1/bin/arm-eabi-ld
export ANDROID_EMULATOR_LIBS = $(PWD)/Binaries/emulator_libs
export LDFLAGS =  -lm -lstdc++ -llog -lc -lcutils -ldl -lz -lutils -shared -static -soname,libsupllocationprovider.so -L$(ANDROID_EMULATOR_LIBS) -soname,libgpsservices -L$(ANDROID_EMULATOR_LIBS)
#LDFLAGS =  -lm -lstdc++ -llog -lc -lcutils -ldl -lz -lutils -static  -L$(ANDROID_EMULATOR_LIBS)
else
# Target
export CC = $(ARM_TOOLCHAIN_PATH)/arm-none-linux-gnueabi-gcc
export CPP = $(ARM_TOOLCHAIN_PATH)/arm-none-linux-gnueabi-g++
export CPP = $(ARM_TOOLCHAIN_PATH)/arm-none-linux-gnueabi-gcc
export LD = $(ARM_TOOLCHAIN_PATH)/arm-none-linux-gnueabi-ld
export ANDROID_EMULATOR_LIBS = /home/dk/GOOGLEMAPS
export LDFLAGS =  -L$(MYDROID_PATH)/out/target/product/blaze/system/lib -lm -lc -lstdc++ -lutils -lcutils -ldl -lz -llog -lgpsservices -shared -static -soname,libsupllocationprovider.so -L$(MYDROID_PATH)/out/target/product/blaze/system/lib

endif
