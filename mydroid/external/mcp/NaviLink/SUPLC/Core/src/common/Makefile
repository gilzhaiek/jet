
CPPFLAGS += -I../../include/ -D_LINUX_

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
