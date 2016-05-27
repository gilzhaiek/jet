include ./Config.mk
PWD = $(shell pwd)
#export CFLAGS = -Wall -c -g -Wno-format -Wno-missing-braces -Wno-variadic-macros -D_BYTE_ORDER=_LITTLE_ENDIAN

export CFLAGS = -Wno-multichar -W -Wall -Wno-unused -Wstrict-aliasing=2 -fno-exceptions -fpic -ffunction-sections -funwind-tables -fmessage-length=0 -finline-functions -fno-inline-functions-called-once -fgcse-after-reload -frerun-cse-after-loop -frename-registers -fomit-frame-pointer -fno-strict-aliasing -finline-limit=64 -D_BYTE_ORDER=_LITTLE_ENDIAN -DLINUX

#export CPPFLAGS = -Wall -c -g -Wno-format -Wno-missing-braces -Wno-variadic-macros -D_BYTE_ORDER=_LITTLE_ENDIAN

export CPPFLAGS = -Wno-multichar -W -Wall -Wno-unused -Wstrict-aliasing=2 -fno-exceptions -fpic -ffunction-sections -funwind-tables -fmessage-length=0 -finline-functions -fno-inline-functions-called-once -fgcse-after-reload -frerun-cse-after-loop -frename-registers -fvisibility-inlines-hidden -fomit-frame-pointer -fno-strict-aliasing -finline-limit=64 -fno-rtti -D_BYTE_ORDER=_LITTLE_ENDIAN -DLINUX
#-fstack-protector
#export CFLAGS
#export CPPFLAGS
ifeq ($(BUILD_TYPE), emulator)
	CFLAGS += -DANDROID_EMULATOR
	CPPFLAGS += -DANDROID_EMULATOR
endif

AND_BIONIC_DIR = $(MYDROID_PATH)/bionic
#CFLAGS += -I${JAVA_DIR}
#CFLAGS += -I${JAVA_DIR}/linux 
CFLAGS += -I${AND_BIONIC_DIR}/libc/include
CFLAGS += -I${AND_BIONIC_DIR}/libc/arch-arm/include
CFLAGS += -I${AND_BIONIC_DIR}/libc/kernel/common 
CFLAGS += -I${AND_BIONIC_DIR}/libc/kernel/arch-arm 
CFLAGS += -I${AND_BIONIC_DIR}/libm/include
#CFLAGS += -I${MYDROID_PATH}/base/include
CFLAGS += -I${MYDROID_PATH}/system/core/include



#CPPFLAGS += -I${JAVA_DIR}
CPPFLAGS += -DHAVE_SYS_UIO_H
#CPPFLAGS += -I${JAVA_DIR}/linux
CPPFLAGS += -I${AND_BIONIC_DIR}/libc/include
CPPFLAGS += -I${AND_BIONIC_DIR}/libc/arch-arm/include
CPPFLAGS += -I${AND_BIONIC_DIR}/libc/kernel/common
CPPFLAGS += -I${AND_BIONIC_DIR}/libc/kernel/arch-arm
CPPFLAGS += -I${AND_BIONIC_DIR}/libm/include
CPPFLAGS += -I${MYDROID_PATH}/bionic/libstdc++/include
CPPFLAGS += -I${MYDROID_PATH}/frameworks/base/include/
CPPFLAGS += -I${MYDROID_PATH}/system/core/include/
CPPFLAGS += -I${MYDROID_PATH}/dalvik/libnativehelper/include
CPPFLAGS += -I${MYDROID_PATH}/dalvik/libnativehelper/include/nativehelper
CPPFLAGS += -I${MYDROID_PATH}/out/target/product/generic/obj/include
CPPFLAGS += -I${AND_BIONIC_DIR}/libstdc++/include
CPPFLAGS += -I${AND_BIONIC_DIR}/libm/include/arch/arm
CPPFLAGS += -I${AND_BIONIC_DIR}/libthread_db/include
CPPFLAGS += -I${AND_BIONIC_DIR}/libm/arm
CPPFLAGS += -I${AND_BIONIC_DIR}/libm
CPPFLAGS += -I${MYDROID_PATH}/out/target/product/generic/obj/SHARED_LIBRARIES/libm_intermediates
CPPFLAGS += -I${MYDROID_PATH}/external/mcp/NaviLink/SUPLC/Core/include/ti_client_wrapper
CPPFLAGS += -I${MYDROID_PATH}/external/mcp/MCP_Common/global_inc
CPPFLAGS += -I${MYDROID_PATH}/external/mcp/MCP_Common/inc
CPPFLAGS += -I${MYDROID_PATH}/external/mcp/MCP_Common/Platform/inc
CPPFLAGS += -I${MYDROID_PATH}/external/mcp/MCP_Common/Platform/os/LINUX/inc
CPPFLAGS += -I${MYDROID_PATH}/external/mcp/NaviLink/inc
CPPFLAGS += -I${MYDROID_PATH}/external/mcp/NaviLink/platforms/modem
CPPFLAGS += -I${MYDROID_PATH}/external/mcp/NaviLink/NAVC
CPPFLAGS += -I${MYDROID_PATH}/external/mcp/NaviLink/NAVC/Core
CPPFLAGS += -I${MYDROID_PATH}/external/mcp/NaviLink/NAVC/Lib

SOURCES = $(wildcard ./src/*.cpp)

SUBDIRS = ./src

OBJECTS = $(patsubst %.cpp,%.o,$(SOURCES))
EXECUTABLE=libsuplhelperservicejni.so

all: $(EXECUTABLE)
$(EXECUTABLE): subdirs
	$(LD)  $(OBJECTS) $(LDFLAGS) -o $@	
	@echo "LD    $(EXECUTABLE)"

subdirs:
	@set -e; for i in $(SUBDIRS); do $(MAKE) -C $$i ; done

clean:
	rm -rf *.so $(EXECUTABLE) 
	@set -e; for i in $(SUBDIRS); do $(MAKE) -C $$i clean ; done
