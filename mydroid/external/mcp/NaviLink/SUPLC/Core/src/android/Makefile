
CPPFLAGS += -I../../include/ -D_LINUX_ -I../../EE/include/ -I../../EE/Ephemeris/ -D_LINUX_ -D_ANDROID_IMSI_

SRCS = $(wildcard *.cpp)
OBJS = $(patsubst %.cpp,%.o,$(SRCS))
HHDR = $(wildcard ../../include/algorithm/*.h)


all: $(OBJS)
%.o: %.cpp
#	@echo "$(CPP) -c  $(CPPFLAGS)    $<"
	@echo "CC     $<"
	@$(CPP) -c $(CPPFLAGS) $< -o $@

clean:
	rm -f $(OBJS)
