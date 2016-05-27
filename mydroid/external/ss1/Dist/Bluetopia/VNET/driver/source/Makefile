
# Makefile for 2.6 kernels, validated on Ubuntu 7.10 system
#
# To build modules outside of the kernel tree, we run "make"
# in the kernel source tree; the Makefile these then includes this
# Makefile once again.
# This conditional selects whether we are being included from the
# kernel Makefile or not.
ifeq ($(KERNELRELEASE),)

ifeq ($(KERNELDIR),)
    # Assume the source tree is where the running kernel was built
    # You should set KERNELDIR in the environment if it's elsewhere
    KERNELDIR ?= /lib/modules/$(shell uname -r)/build
endif

# The current directory is passed to sub-makes as argument
MODULE_PATH := $(shell pwd)

# 'all' isn't really all targets, since it only builds the module. It is
# present to match the behavior of the other makefiles in the tree.
.PHONY: all
all: modules

modules:
	$(MAKE) -C $(KERNELDIR) M=$(MODULE_PATH) modules

modules_install:
	$(MAKE) -C $(KERNELDIR) M=$(MODULE_PATH) modules_install

clean:
	rm -rf *.o *~ core .depend .*.cmd *.ko *.mod.c .tmp_versions

.PHONY: modules modules_install clean

else
    # called from kernel build system: just declare what our modules are
    obj-m := SS1VNETM.o
endif


