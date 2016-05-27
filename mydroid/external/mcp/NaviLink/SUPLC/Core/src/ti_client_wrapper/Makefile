
#CPPFLAGS += -I../../include/ -D_LINUX_ -D_LINUX_ -D_ANDROID_IMSI_

CPPFLAGS += -I../../include/per_codec -I../../include/android -I../../include/ -D_LINUX_ -D_ANDROID_IMSI_
#CPPFLAGS += -DENABLE_WRAPPER_DEBUG -DDEBUG_SUPLC_HAL_OS

SRCS = $(wildcard *.cpp)
OBJS = $(patsubst %.cpp,%.o,$(SRCS))
HHDR = $(wildcard ../../include/ti_client_wrapper/*.h)


all: $(OBJS)
%.o: %.cpp
#	@echo "$(CPP) -c  $(CPPFLAGS)    $<"
	@echo "CC     $<"
	@$(CPP) -c $(CPPFLAGS) $< -o $@

clean:
	rm -f $(OBJS)
