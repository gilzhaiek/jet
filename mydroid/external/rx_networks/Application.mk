# The ARMv7 is significantly faster due to the use of the hardware FPU
#APP_ABI := armeabi armeabi-v7a
ifeq ($(APP_ABI),)
  APP_ABI := armeabi-v7a
endif

APP_OPTIM := release

ifeq ($(APP_CFLAGS),)
  APP_CFLAGS := -O2
endif

APP_BUILD_SCRIPT := Android.mk
