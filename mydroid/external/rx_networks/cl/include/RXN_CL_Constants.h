/*
 * Copyright (c) 2007-2011 Rx Networks, Inc. All rights reserved.
 *
 * Property of Rx Networks
 * Proprietary and Confidential
 * Do NOT distribute
 * 
 * Any use, distribution, or copying of this document requires a 
 * license agreement with Rx Networks. 
 * Any product development based on the contents of this document 
 * requires a license agreement with Rx Networks. 
 * If you have received this document in error, please notify the 
 * sender immediately by telephone and email, delete the original 
 * document from your electronic files, and destroy any printed 
 * versions.
 *
 * This file contains sample code only and the code carries no 
 * warranty whatsoever.
 * Sample code is used at your own risk and may not be functional. 
 * It is not intended for commercial use.   
 *
 * Example code to illustrate how to integrate Rx Networks PGPS 
 * System into a client application.
 *
 * The emphasis is in trying to explain what goes on in the software,
 * and how to the various API functions relate to each other, 
 * rather than providing a fully optimized implementation.
 *
 *************************************************************************
 * $LastChangedDate: 2009-06-23 11:25:43 -0700 (Tue, 23 Jun 2009) $
 * $Revision: 11559 $
 *************************************************************************
 *
 */

/**
 * \file
 * \brief
 * Chipset Library (CL) constants.
 * 
 * \details
 * This file contains constants used by the CL.
 * 
 */

#ifndef RXN_CL_CONSTANTS_H
#define RXN_CL_CONSTANTS_H

/** 
 * \brief
 * Indicates the current version for the CL. 
 */
#define RXN_CL_VER                          "3.0.0.9"

/****************************
 * String constants follow. *
 ****************************/

/** 
 * \brief
 * Defines a max length for a string describing the port or file path to open
 * or other config params.
 */ 
#define RXN_CL_CONFIG_MAX_STR_LEN           256

/** 
 * \brief
 * Defines a max length for a string describing the log file path.
 */ 
#define RXN_CL_MAX_LOG_PATH                 256

/**
 * \brief
 * Defines a max length for a chipset string. Long length required by RxN Eng for DLL support. 
 */ 
#define RXN_CL_CHIP_MAX_STR_LEN             1024

/**
 * \brief
 * Default offset value indicating that the clock offset has not been set.
 */
#define RXN_CL_INVALID_CLK_OFFSET           0x7fffffff

/*************************
 ** Error codes follow. **
 *************************/

/**
 * \brief
 * Defines an error that results when no appropriate chipset support exists within a compile of the CL.
 */
#define RXN_CL_NO_CHIPSET_SUPPORT_ERR       1

/**
 * \brief
 * Defines an error that results when an invalid chipset is specified.
 */
#define RXN_CL_INVALID_CHIPSET_VERSION_ERR  2

/**
 * \brief
 * Defines an error that results when another instance of the CL can't be instantiated
 * because there are already too many instances in use.
 */
#define RXN_CL_TOO_MANY_INSTANCES_ERR       3

/**
 * \brief
 * Defines an error that results when an invalid instance handle is specified.
 */
#define RXN_CL_INSTANCE_NOT_FOUND_ERR       4

/**
 * \brief
 * Defines an error that results when a config string is poorly formatted.
 */
#define RXN_CL_CONFIG_FORMAT_ERR            5

/**
 * \brief
 * Defines a file or port open or close error.
 */
#define RXN_CL_OPEN_OR_CLOSE_ERR            6

/**
 * \brief
 * Defines a file or port setup error.
 */
#define RXN_CL_PORT_SETUP_ERR               7

/**
 * \brief
 * Defines a chipset read error.
 */
#define RXN_CL_CHIPSET_READ_ERR             8

/**
 * \brief
 * Defines a chipset write error.
 */
#define RXN_CL_CHIPSET_WRITE_ERR            9

/**
 * \brief
 * Defines a chipset read error when a timeout occurs.
 */
#define RXN_CL_CHIPSET_READ_TIMEOUT         10

/**
 * \brief
 * Defines a chipset read error when a timeout occurs.
 */
#define RXN_CL_CHIPSET_WRITE_TIMEOUT        11

/**
 * \brief
 * Defines a response indicating a file end was reached during a read.
 */
#define RXN_CL_CHIPSET_AT_EOF               12

/**
 * \brief
 * Defines an error that results when a chipset initialization fails.
 */
#define RXN_CL_CHIPSET_INIT_ERR             13

/** 
 * \brief
 * Defines an error that results when an invalid restart type is specified.
 */ 
#define RXN_CL_INVALID_RESTART_TYPE_ERR     14

/** 
 * \brief
 * Defines an error when the RXN_RefLocation_t format is not set to a RXN_LocationType enum value of RXN_LOC_LLA or RXN_LOC_ECEF.
 */ 
#define RXN_CL_INVALID_REF_LOC_FORMAT_ERR   15

/** 
 * \brief
 * Defines an error when the ref location or time is not accepted by the chipset.
 */ 
#define RXN_CL_REF_LOC_TIME_ACCEPT_ERR      16

/** 
 * \brief
 * Defines an error that occurs when parsing msg contents.
 */ 
#define RXN_CL_MSG_PARSE_ERROR              17

/** 
 * \brief
 * Defines an error that occurs when a reciever nacks a request.
 */ 
#define RXN_CL_MSG_NACK_ERROR               18

/** 
 * \brief
 * Defines an error that occurs when a thread is created.
 */ 
#define RXN_CL_SCKT_SETUP_ERROR             19

/** 
 * \brief
 * Defines an error that occurs when there is a memory alloation error.
 */ 
#define RXN_CL_ALLOCATION_ERROR             20

/** 
 * \brief
 * Defines an error that occurs when a request is made and the chip library is busy.
 * handling another request.
 */ 
#define RXN_CL_BUSY_ERROR                   21

/** 
 * \brief
 * Defines an error that occurs when a request is made for data that is unavailable
 * at present within a chipset (i.e. in general, the chipset supports access to the
 * data, but such data is not presently available due to chipset state).
 */ 
#define RXN_CL_DATA_UNAVAILABLE             22

#endif /* RXN_CL_CONSTANTS_H */
