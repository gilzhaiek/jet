include ./Config.mk
PWD = $(shell pwd)

export CFLAGS = -Wno-multichar -W -Wall -Wno-unused -Wstrict-aliasing=2 -fno-exceptions -fpic -ffunction-sections -funwind-tables -fmessage-length=0 -finline-functions -fno-inline-functions-called-once -fgcse-after-reload -frerun-cse-after-loop -frename-registers -fomit-frame-pointer -fno-strict-aliasing -finline-limit=64 -D_BYTE_ORDER=_LITTLE_ENDIAN -DLINUX

export CPPFLAGS = -Wno-multichar -W -Wall -Wno-unused -Wstrict-aliasing=2 -fno-exceptions -fpic -ffunction-sections -funwind-tables -fmessage-length=0 -finline-functions -fno-inline-functions-called-once -fgcse-after-reload -frerun-cse-after-loop -frename-registers -fvisibility-inlines-hidden -fomit-frame-pointer -fno-strict-aliasing -finline-limit=64 -fno-rtti -D_BYTE_ORDER=_LITTLE_ENDIAN -DLINUX

ifeq ($(BUILD_TYPE), emulator)
	CFLAGS += -DANDROID_EMULATOR
	CPPFLAGS += -DANDROID_EMULATOR
endif

AND_BIONIC_DIR = $(MYDROID_PATH)/bionic
CFLAGS += -I${JAVA_DIR}
CFLAGS += -I${JAVA_DIR}/linux 
CFLAGS += -I${AND_BIONIC_DIR}/libc/include
CFLAGS += -I${AND_BIONIC_DIR}/libc/arch-arm/include
CFLAGS += -I${AND_BIONIC_DIR}/libc/kernel/common 
CFLAGS += -I${AND_BIONIC_DIR}/libc/kernel/arch-arm 
CFLAGS += -I${AND_BIONIC_DIR}/libm/include
#CFLAGS += -I${MYDROID_PATH}/base/include
CFLAGS += -I${MYDROID_PATH}/system/core/include
CFLAGS += -I${MYDROID_PATH}/external/mcp/NaviLink/services/logm


CPPFLAGS += -I${JAVA_DIR}
CPPFLAGS += -I${JAVA_DIR}/linux
CPPFLAGS += -I${AND_BIONIC_DIR}/libc/include
CPPFLAGS += -I${AND_BIONIC_DIR}/libc/arch-arm/include
CPPFLAGS += -I${AND_BIONIC_DIR}/libc/kernel/common
CPPFLAGS += -I${AND_BIONIC_DIR}/libc/kernel/arch-arm
CPPFLAGS += -I${AND_BIONIC_DIR}/libm/include
CPPFLAGS += -I${MYDROID_PATH}/bionic/libstdc++/include
CPPFLAGS += -I${MYDROID_PATH}/frameworks/base/include/
CPPFLAGS += -I${MYDROID_PATH}/system/core/include/
CPPFLAGS += -I${MYDROID_PATH}/external/mcp/NaviLink/services/logm/
CPPFLAGS += -I${MYDROID_PATH}/external/mcp/NaviLink/NAVC
CPPFLAGS += -I${MYDROID_PATH}/external/mcp/MCP_Common/inc
CPPFLAGS += -I${MYDROID_PATH}/external/mcp/MCP_Common/Platform/os/LINUX/common/inc
CPPFLAGS += -I${MYDROID_PATH}/external/mcp/MCP_Common/Platform/os/LINUX/android_zoom3/inc
CPPFLAGS += -I${MYDROID_PATH}/external/mcp/MCP_Common/Platform/inc
CPPFLAGS += -I${MYDROID_PATH}/external/mcp/NaviLink/SUPLC/Core/include/ti_client_wrapper/

LDFLAGS  += -L$(MYDROID_PATH)/out/target/product/zoom2/system/lib 


# Begin DOCOMO
# -----------------------------------
# DOCOMO 'y'=yes, 'n'=no
DOCOMO_FEATURE = 'y'
# -----------------------------------
ifeq ($(DOCOMO_FEATURE), 'y')
# DOCOMO Specific
# ----------
DOCOMO_FEATURE_MSISDN = 'n'
DOCOMO_FEATURE_WCDMA = 'n'
#
ifeq ($(DOCOMO_FEATURE_MSISDN), 'y')
CFLAGS+=-DDOCOMO_SUPPORT_MSISDN
CPPFLAGS+= -DDOCOMO_SUPPORT_MSISDN
endif
#
ifeq ($(DOCOMO_FEATURE_WCDMA), 'y')
CFLAGS+=-DDOCOMO_SUPPORT_WCDMA
CPPFLAGS+= -DDOCOMO_SUPPORT_WCDMA
endif
#
endif # end DOCOMO_FEATURE
# -----------------------------------
# end DOCOMO
SOURCES = $(wildcard ./src/algorithm/*.cpp \
                  ./src/android/*.cpp \
                  ./src/common/*.cpp \
                  ./src/device/*.cpp  \
                  ./src/gps/*.cpp  \
                  ./src/messages/*.cpp  \
                  ./src/network/*.cpp  \
                  ./src/session/*.cpp  \
                  ./src/suplcontroller/*.cpp  \
                  ./src/ULP_Processor/*.cpp \
		  ./src/ti_client_wrapper/*.cpp)

SOURCES2 = $(wildcard ./src/suplcontroller/*.cpp)

SUBDIRS = ./src/algorithm \
          ./src/android \
          ./src/common \
          ./src/codec \
          ./src/device \
          ./src/gps \
          ./src/messages \
          ./src/network \
          ./src/session \
          ./src/per_codec \
          ./src/suplcontroller \
          ./src/ULP_Processor \
	  ./src/ti_client_wrapper

SOURCES1 = $(wildcard ./src/codec/*.c ./src/per_codec/*.c)
OBJECTS = $(patsubst %.c,%.o,$(SOURCES1)) $(patsubst %.cpp,%.o,$(SOURCES))
EXECUTABLE=libsupllocationprovider.so

all: $(EXECUTABLE)
$(EXECUTABLE): subdirs
	$(LD)  $(OBJECTS) $(LDFLAGS) -o $@	
	@echo "LD    $(EXECUTABLE)"

subdirs:
	@set -e; for i in $(SUBDIRS); do $(MAKE) -C $$i ; done

clean:
	rm -rf *.so $(EXECUTABLE) 
	@set -e; for i in $(SUBDIRS); do $(MAKE) -C $$i clean ; done

