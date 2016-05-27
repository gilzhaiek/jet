#=============================================================================
# Copyright 2008 Texas Instruments Inc. All Rights Reserved.
#
# Makefile for MCP_Common/frame
#
#=============================================================================

INCLUDES = -I ../apps \
		-I ../inc \
		-I ../Platform/os/LINUX/common/inc \
		-I ../Platform/os/LINUX/ubuntu_x86/inc \
		-I ../Platform/inc \
		-I ../Platform/inc/int

SOURCES =	mcp_bts_script_processor.c\
		mcp_config_parser.c\
		mcp_config_reader.c\
		mcp_endian.c\
		mcpf_main.c\
		mcpf_queue.c\
		mcpf_report.c\
		mcp_gensm.c\
		mcp_hci_sequencer.c\
		mcp_load_manager.c\
		mcp_pool.c\
		mcp_rom_scripts_db.c\
		mcp_utils_dl_list.c\
		mcp_utils.c\
		mcp_unicode.c\
		mcp_log_common.c\
		../Platform/hw/LINUX/ubuntu_x86/ccm_hal_pwr_up_dwn.c \
		../Platform/hw/LINUX/ubuntu_x86/mcp_hal_pm.c \
		../Platform/os/LINUX/common/mcp_linux_line_parser.c \
		../Platform/os/LINUX/common/mcp_hal_memory.c \
		../Platform/os/LINUX/common/mcp_hal_string.c \
		../Platform/os/LINUX/ubuntu_x86/mcp_hal_os.c \
		../Platform/os/LINUX/common/mcp_hal_misc.c \
		../Platform/os/LINUX/common/mcp_hal_log.c \
		../Platform/os/LINUX/common/mcp_hal_fs.c \
		../Platform/init_script/HDK/mcp_rom_scripts.c

TARGET_NAME = libmcphal.a

GENERIC_MAKE_PATH ?= $(PWD)

include $(GENERIC_MAKE_PATH)/../../EBTIPS/apps/LINUX/Makefile.generic

