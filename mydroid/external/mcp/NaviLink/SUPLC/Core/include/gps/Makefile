###################################################################################################
#
#    @file   Makefile
#
#    @brief  configuration file to compile the v4l_output test.
#
###################################################################################################
#
#   Copyright (C) 2004, Freescale Semiconductor, Inc. All Rights Reserved
#   THIS SOURCE CODE IS CONFIDENTIAL AND PROPRIETARY AND MAY NOT
#   BE USED OR DISTRIBUTED WITHOUT THE WRITTEN PERMISSION OF
#   Freescale Semiconductor, Inc.
#     
###################################################################################################
#Revision History:
#                            Modification     Tracking
#Author                          Date          Number    Description of Changes
#-------------------------   ------------    ----------  ------------------------------------------
#KHOROSHEV.D                 09/29/2005      TLSbo55077  Initial version 
#
###################################################################################################
#Portability: ARM GCC
###################################################################################################

#CFLAGS       += -Wall -I /usr/local/vb_sw_linux_ap_vte.arm/include -I$(KLINUX_BLTDIR)/include 
CPPFLAGS += -I../../include/ -I../ -D_LINUX_
#LOADLIBES    += -L/usr/local/vb_sw_linux_ap_vte.arm/lib -lltp -lm 
#PROGRAM = 
#LIBRARY =
#vpath = %.h ../../include/
SRCS = $(wildcard *.cpp)
OBJS = $(patsubst %.cpp,%.o,$(SRCS))
HHDR = $(wildcard ../../include/algorithm/*.h)
#TARGET = v4l_output_testapp


all: $(OBJS)
	#CC = CPP
	@echo "***********************************************"
	@echo "** ALL : $(OBJS)                  **"
	@echo "***********************************************"
	@echo

clean:
	rm -f $(OBJS)

$(OBJS) : $(SRCS)

$(SRCS) : $(HHDR)