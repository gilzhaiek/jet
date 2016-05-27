LOCAL_PATH:= $(call my-dir)

#NOTE: libtfa98xx, for now we use static libs

############################### libtfa98xx 
include $(CLEAR_VARS)
LOCAL_C_INCLUDES:= external/climax/inc external/climax/src external/climax/Tfa98xxAPI/inc external/climax/src/lxScribo
LOCAL_SRC_FILES:=src/nxpTfa98xx.c src/NXP_I2C_linux.c src/tfa98xxDiagnostics.c src/tfa98xxLiveData.c\
				Tfa98xxAPI/src/Tfa98xx.c Tfa98xxAPI/src/Tfa98xx_TextSupport.c\
				Tfa98xxAPI/src/initTfa9887.c
LOCAL_MODULE := libtfa98xx
LOCAL_SHARED_LIBRARIES:= libcutils libutils
LOCAL_MODULE_TAGS := optional
LOCAL_PRELINK_MODULE := false

include $(BUILD_STATIC_LIBRARY)


############################## linux scribo
include $(CLEAR_VARS)
LOCAL_C_INCLUDES:= external/climax/inc external/climax/src/lxScribo external/climax/src/lxScribo/scribosrv external/climax/Tfa98xxAPI/inc
LOCAL_SRC_FILES:= src/lxScribo/lxScribo.c  src/lxScribo/lxDummy.c  src/lxScribo/lxScriboSerial.c  src/lxScribo/lxScriboSocket.c\
	              src/lxScribo/lxI2c.c src/lxScribo/scribosrv/i2cserver.c src/lxScribo/scribosrv/cmd.c
LOCAL_MODULE := liblxScribo
LOCAL_SHARED_LIBRARIES:= libcutils libutils
LOCAL_MODULE_TAGS := optional
LOCAL_PRELINK_MODULE := false

include $(BUILD_STATIC_LIBRARY)

############################## cli app
include $(CLEAR_VARS)
LOCAL_C_INCLUDES:= external/climax/inc external/climax/src/cli external/climax/src/lxScribo external/climax/Tfa98xxAPI/inc
LOCAL_SRC_FILES:= src/climax.c src/cliCommands.c src/cli/cmdline.c
LOCAL_MODULE := climax
LOCAL_SHARED_LIBRARIES:= libcutils libutils
LOCAL_STATIC_LIBRARIES:= libtfa98xx liblxScribo
LOCAL_MODULE_TAGS := optional
LOCAL_PRELINK_MODULE := false

include $(BUILD_EXECUTABLE)
