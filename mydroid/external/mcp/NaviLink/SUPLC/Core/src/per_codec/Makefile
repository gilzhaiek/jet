

CFLAGS += -I../../include/ -I../ -D_LINUX_ -D_ANDROID_DEVICE_

SRCS = $(wildcard *.c)
OBJS = $(patsubst %.c,%.o,$(SRCS))
HHDR = $(wildcard ../../include/algorithm/*.h)

all: $(OBJS)

%.o: %.c
	@echo "CC     $<"
	@$(CC) -c $(CFLAGS) $< -o $@
#	@echo "$(CC) -c  $(CFLAGS)    $<"


clean:
	rm -f $(OBJS)

