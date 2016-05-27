#=============================================================================
# Copyright 2008 Texas Instruments Inc. All Rights Reserved.
#
# Makefile for MCP_Common/ccm
#
#=============================================================================

INCLUDES = -I ../apps \
		-I ../inc \
		-I ../Platform/inc \
		-I ../tran \
		-I ../Platform/os/LINUX/common/inc \
		-I ../Platform/os/LINUX/ubuntu_x86/inc \

SOURCES =	ccm/ccm.c \
		cal/ccm_vaci_cal_chip_6450_1_0.c \
		cal/ccm_vaci_cal_chip_6450_2.c \
		cal/ccm_vaci_chip_abstration.c \
		cal/ccm_vaci_cal_chip_6350.c \
		cal/ccm_vaci_cal_chip_1273.c \
		cal/ccm_vaci_cal_chip_1273_2.c \
		cal/ccm_vaci_cal_chip_1283.c \
		im/ccm_imi_bt_tran_off_sm.c \
		im/ccm_im.c \
		im/ccm_imi_bt_tran_on_sm.c \
		im/ccm_imi_bt_tran_mngr.c \
		im/ccm_imi_bt_tran_sm.c \
		vac/ccm_vaci_allocation_engine.c \
		vac/ccm_vaci_mapping_engine.c \
		vac/ccm_vaci_configuration_engine.c \
		vac/ccm_vaci_debug.c

TARGET_NAME = libccm.a

GENERIC_MAKE_PATH ?= $(PWD)

include $(GENERIC_MAKE_PATH)/../../EBTIPS/apps/LINUX/Makefile.generic
