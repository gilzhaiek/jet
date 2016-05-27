/******************************************************************************\
##                                                                            *
## Unpublished Proprietary and Confidential Information of Texas Instruments  *
## Israel Ltd. Do Not Disclose.                                               *
## Copyright 2008 Texas Instruments Israel Ltd.                               *
## All rights reserved. All unpublished rights reserved.                      *
##                                                                            *
## No part of this work may be used or reproduced in any form or by any       *
## means, or stored in a database or retrieval system, without prior written  *
## permission of Texas Instruments Israel Ltd. or its parent company Texas    *
## Instruments Incorporated.                                                  *
## Use of this work is subject to a license from Texas Instruments Israel     *
## Ltd. or its parent company Texas Instruments Incorporated.                 *
##                                                                            *
## This work contains Texas Instruments Israel Ltd. confidential and          *
## proprietary information which is protected by copyright, trade secret,     *
## trademark and other intellectual property rights.                          *
##                                                                            *
## The United States, Israel  and other countries maintain controls on the    *
## export and/or import of cryptographic items and technology. Unless prior   *
## authorization is obtained from the U.S. Department of Commerce and the     *
## Israeli Government, you shall not export, reexport, or release, directly   *
## or indirectly, any technology, software, or software source code received  *
## from Texas Instruments Incorporated (TI) or Texas Instruments Israel,      *
## or export, directly or indirectly, any direct product of such technology,  *
## software, or software source code to any destination or country to which   *
## the export, reexport or release of the technology, software, software      *
## source code, or direct product is prohibited by the EAR. The subject items *
## are classified as encryption items under Part 740.17 of the Commerce       *
## Control List (“CCL”). The assurances provided for herein are furnished in  *
## compliance with the specific encryption controls set forth in Part 740.17  *
## of the EAR -Encryption Commodities and Software (ENC).                     *
##                                                                            *
## NOTE: THE TRANSFER OF THE TECHNICAL INFORMATION IS BEING MADE UNDER AN     *
## EXPORT LICENSE ISSUED BY THE ISRAELI GOVERNMENT AND THE APPLICABLE EXPORT  *
## LICENSE DOES NOT ALLOW THE TECHNICAL INFORMATION TO BE USED FOR THE        *
## MODIFICATION OF THE BT ENCRYPTION OR THE DEVELOPMENT OF ANY NEW ENCRYPTION.*
## UNDER THE ISRAELI GOVERNMENT'S EXPORT LICENSE, THE INFORMATION CAN BE USED *
## FOR THE INTERNAL DESIGN AND MANUFACTURE OF TI PRODUCTS THAT WILL CONTAIN   *
## THE BT IC.                                                                 *
##                                                                            *
\******************************************************************************/
/*******************************************************************************\
*
*   FILE NAME:      mcp_hal_log.c
*
*   DESCRIPTION:    This file implements the API of the MCP HAL log utilities.
*
*   AUTHOR:         Chen Ganir
*
\*******************************************************************************/


/********************************************************************************
 *
 * Include files
 *
 *******************************************************************************/
#define _GNU_SOURCE
#include <stdio.h>
#include <stdarg.h>
#include <time.h>
#include <sys/time.h>
#include <pthread.h>
#include <unistd.h>
#include <sys/syscall.h>

#include <stdio.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <stdlib.h>
#include <string.h>
#include <netinet/in.h>


#include "mcp_hal_types.h"
#include "mcp_hal_log.h"
#include "mcp_hal_log_udp.h"
#include "mcp_hal_config.h"
#include "mcp_hal_string.h"
#include "mcp_hal_memory.h"
#include "mcp_hal_fs.h"
#include "mcp_defs.h"
#include "mcpf_defs.h"


#ifdef ANDROID
#include "cutils/log.h"
#undef LOG_TAG
#define LOG_TAG gMcpLogAppName
#endif

void MCP_HAL_InitUdpSockets(void);
static McpU8 readConfFile(void);
static void MCP_HAL_LOG_UpdatePlaceHolderFile(McpS8 *fileName);
static void MCP_HAL_LOG_LogToFile(const char*       fileName, McpU32  line, McpHalLogModuleId_e moduleId,                          McpHalLogSeverity severity,
                           const char*      msg);
static void MCP_HAL_LOG_ChunkLogFile(void);


/****************************************************************************
 *
 * Constants
 *
 ****************************************************************************/

#define MCP_HAL_MAX_FORMATTED_MSG_LEN           (200)
#define MCP_HAL_MAX_USER_MSG_LEN                (100)
#define BUFFSIZE 255
#define APP_NAME_MAX_LEN			(50)
#define MCP_HAL_MAX_TAGS			(5)
#define MCP_HAL_MAX_TAG_LENGTH	(4)

#define GPS_CONFIG_FILE_PATH "/system/bin/GpsConfigFile.txt"
#define RDONLY  "r"
#define MAX_BUF_LENGTH 128
#define LOGGER_IP  "logger_ip"
#define LOGGER_PORT "logger_port"

static char _mcpLog_FormattedMsg[MCP_HAL_MAX_FORMATTED_MSG_LEN + 1];

McpU8 gMcpLogEnabled = 0;
McpU8 gMcpLogToStdout = 0;
McpU8 gMcpLogToFile = 0;
McpU8 gMcpLogToUdpSocket = 0;
McpU32 McpMaxLogLines = 0;
McpU32 McpMaxNvmFiles = 0;
McpU8 McpLogFileName[250];
McpU32 McpLogFileCurrentChunckNum=0;
McpU32 McpLogFileLineCount=0;

mcp_log_socket_t  gMcpSocket;

#ifdef ANDROID
McpU8 gMcpLogToAndroid = 0;
char gMcpLogAppName[APP_NAME_MAX_LEN] = {0};
#endif

char gMcpTagString[MCP_HAL_MAX_TAGS][MCP_HAL_MAX_TAG_LENGTH] = {MCP_HAL_LOG_INTERNAL_TAG, MCP_HAL_LOG_TOSENSOR_TAG, MCP_HAL_LOG_FROMSENSOR_TAG,
																    MCP_HAL_LOG_NULL_TAG, MCP_HAL_LOG_INTERNAL_TAG} ;


char gMcpLogUdpTargetAddress[30] = "";
unsigned long gMcpLogUdpTargetPort = MCP_LOG_PORT_DEFAULT;
int g_udp_sock = -1;

time_t g_start_time_seconds = 0;

struct sockaddr_in g_udp_logserver;

static pthread_key_t thread_id;

static McpU8 gInitialized = 0;

McpS32 McpLogFileFD = NULL;

/* this key will hold the thread-specific thread handle that will be used
 * for self identification of threads. the goal is that every thread will know its name
 * to ease having a consistent self identifying logs */
static pthread_key_t thread_name;



/*-------------------------------------------------------------------------------
 * MCP_HAL_LOG_EnableLogging()
 *
 *      Enable full MCP logging
 *
 * Type:
 *      Synchronous, non-reentrant
 *
 * Parameters:
 *	void
 *
 * Returns:
 *     void
 *
 */
void MCP_HAL_LOG_EnableLogging(void)
{
	    gMcpLogEnabled = MCP_TRUE;
}

/*-------------------------------------------------------------------------------
 * MCP_HAL_LOG_DisableLogging()
 *
 *      Disable full MCP logging
 *
 * Type:
 *      Synchronous, non-reentrant
 *
 * Parameters:
 *	void
 *
 * Returns:
 *     void
 *
 */
void MCP_HAL_LOG_DisableLogging(void)
{
	    gMcpLogEnabled = MCP_FALSE;
}

#ifdef ANDROID
/*-------------------------------------------------------------------------------
 * MCP_HAL_LOG_EnableLogToAndroid()
 *
 *      Enable logging to the Android framework
 *
 * Type:
 *      Synchronous, non-reentrant
 *
 * Parameters:
 *	const char *app_name -- name of the app (will be included in the log string)
 *
 * Returns:
 *     void
 *
 */
void MCP_HAL_LOG_EnableLogToAndroid(const char *app_name)
{
	MCP_HAL_LOG_EnableLogging();
	gMcpLogToAndroid = MCP_TRUE;
	strncpy(gMcpLogAppName,app_name,APP_NAME_MAX_LEN-1);
}

/*-------------------------------------------------------------------------------
 * MCP_HAL_LOG_DisableLogToAndroid()
 *
 *      Disable logging to the Android framework
 *
 * Type:
 *      Synchronous, non-reentrant
 *
 * Parameters:
 *	void
 *
 * Returns:
 *     void
 *
 */
void MCP_HAL_LOG_DisableLogToAndroid(void)
{
	gMcpLogToAndroid = MCP_FALSE;
}

#endif


/*-------------------------------------------------------------------------------
 * MCP_HAL_LOG_EnableUdpLogging()
 *
 *      Enable loggin to a UDP Socket
 *
 * Type:
 *      Synchronous, non-reentrant
 *
 * Parameters:
 *	const char* ip -- IP address of UDP socket
 *    unsigned long port -- Port address at UDP socket
 *
 * Returns:
 *     void
 */
void MCP_HAL_LOG_EnableUdpLogging(const char* ip, unsigned long port)
{
	MCP_HAL_LOG_EnableLogging();
	gMcpLogToUdpSocket = MCP_TRUE;

	/* copy ip and port *//*Klocwork Changes  */
	MCP_HAL_STRING_StrnCpy(gMcpSocket.mcpLogUdpTargetAddress,ip,MCPHAL_LOG_MAX_IPADDR_LENGTH-1);
	gMcpSocket.mcpLogUdpTargetPort = port;

	MCP_HAL_InitUdpSockets();
}

/*-------------------------------------------------------------------------------
 * MCP_HAL_LOG_DisableUdpLogging()
 *
 *      Disable loggin to a UDP Socket
 *
 * Type:
 *      Synchronous, non-reentrant
 *
 * Parameters:
 *	void
 *
 * Returns:
 *     void
 */
void MCP_HAL_LOG_DisableUdpLogging(void)
{
	gMcpLogToUdpSocket = MCP_FALSE;

	MCP_HAL_DeInitUdpSockets();

	/*uninit the struct */
   	gMcpSocket.udp_sock = -1;
	strcpy(gMcpSocket.mcpLogUdpTargetAddress, " ");
   	gMcpSocket.mcpLogUdpTargetPort = MCP_LOG_PORT_DEFAULT;

}


/*-------------------------------------------------------------------------------
 * MCP_HAL_LOG_EnableFileLogging()
 *
 *      Enable logging to NVM file system
 *
 * Type:
 *      Synchronous, non-reentrant
 *
 * Parameters:
 *	const char* path  -- path to the directory where the log file needs to be created.
 *
 * Returns:
 *     void
 */
void MCP_HAL_LOG_EnableFileLogging(const char* pathName, McpU32 numFiles, McpU32 numLines)
{
	McpU8 fileName[256];
	McpU8 TimeTemp[5];
	McpU8 ChunkCount[5];

	McpU32 u_CurrentTime;
	McpU32 u_TimeTemp;
	McpHalDateAndTime dateAndTimeStruct;
	EMcpfRes res;

	if (gMcpLogToFile == MCP_FALSE &&
		numFiles > 0 &&
		numLines > 0)
	{
		MCP_HAL_LOG_EnableLogging();

		MCP_HAL_STRING_StrCpy (fileName,(McpU8*)pathName); /*Klocwork Changes  */

		MCP_HAL_STRING_StrnCat(fileName, "LOG_",sizeof(fileName)-1-strlen(fileName));



		u_CurrentTime = time(NULL);
		mcpf_ExtractDateAndTime(u_CurrentTime,&dateAndTimeStruct );

		/*year*/
		u_TimeTemp = dateAndTimeStruct.year;
		MCP_HAL_STRING_ItoA(u_TimeTemp,TimeTemp);
		MCP_HAL_STRING_StrnCat (fileName, TimeTemp,sizeof(fileName)-1-strlen(fileName));
		MCP_HAL_STRING_StrnCat (fileName, "-",sizeof(fileName)-1-strlen(fileName));

		/*month*/
		u_TimeTemp = dateAndTimeStruct.month;
		MCP_HAL_STRING_ItoA(u_TimeTemp,TimeTemp);
		MCP_HAL_STRING_StrnCat (fileName, TimeTemp,sizeof(fileName)-1-strlen(fileName));
		MCP_HAL_STRING_StrnCat (fileName, "-",sizeof(fileName)-1-strlen(fileName));

		/*day*/
		u_TimeTemp = dateAndTimeStruct.day;
		MCP_HAL_STRING_ItoA(u_TimeTemp,TimeTemp);
		MCP_HAL_STRING_StrnCat (fileName, TimeTemp,sizeof(fileName)-1-strlen(fileName));
		MCP_HAL_STRING_StrnCat (fileName, "_",sizeof(fileName)-1-strlen(fileName));

		/*hour*/
		u_TimeTemp = dateAndTimeStruct.hour;
		MCP_HAL_STRING_ItoA(u_TimeTemp,TimeTemp);
		MCP_HAL_STRING_StrnCat (fileName, TimeTemp,sizeof(fileName)-1-strlen(fileName));
		MCP_HAL_STRING_StrnCat (fileName, "-",sizeof(fileName)-1-strlen(fileName));

		/*minute*/
		u_TimeTemp = dateAndTimeStruct.minute;
		MCP_HAL_STRING_ItoA(u_TimeTemp,TimeTemp);
		MCP_HAL_STRING_StrnCat (fileName, TimeTemp,sizeof(fileName)-1-strlen(fileName));
		MCP_HAL_STRING_StrnCat (fileName, "-",sizeof(fileName)-1-strlen(fileName));

		/*second*/
		u_TimeTemp = dateAndTimeStruct.second;
		MCP_HAL_STRING_ItoA(u_TimeTemp,TimeTemp);
		MCP_HAL_STRING_StrnCat (fileName, TimeTemp,sizeof(fileName)-1-strlen(fileName));

		/* copy the file name for later use */

		MCP_HAL_STRING_StrnCpy(McpLogFileName,fileName,sizeof(McpLogFileName)-1);  /*Klocwork Changes  */

		McpMaxLogLines = numLines;
		McpMaxNvmFiles = numFiles;
		McpLogFileCurrentChunckNum = 0;
		McpLogFileLineCount = 0;

		/*Chunk count*/ /*Klocwork Changes  */
		snprintf(ChunkCount,sizeof(ChunkCount), "-%05d", McpLogFileCurrentChunckNum);
		MCP_HAL_STRING_StrnCat (fileName, ChunkCount,sizeof(fileName)-1-strlen(fileName));

		/*file extension*/
		MCP_HAL_STRING_StrnCat (fileName, ".bin",sizeof(fileName)-1-strlen(fileName));

		/* open the new file for writing */
		res = mcpf_file_open(NULL, (McpU8 *) fileName,
			MCP_HAL_FS_O_BINARY | MCP_HAL_FS_O_WRONLY | MCP_HAL_FS_O_CREATE | MCP_HAL_FS_O_TRUNC, &McpLogFileFD);

		if (McpLogFileFD == NULL || res == RES_ERROR)
		{
			printf("file loging: unable to open file\n");
			gMcpLogToFile = MCP_FALSE;
			return;
		}
		else
		{
			gMcpLogToFile = MCP_TRUE;

			MCP_HAL_LOG_UpdatePlaceHolderFile(fileName);

		}
	}
	else
	{
		printf("MCP_HAL_LOG_EnableFileLogging: not init'ed maxFiles=%d, maxLines=%d\n", numFiles, numLines);
	}
}

/*-------------------------------------------------------------------------------
 * MCP_HAL_LOG_DisableFileLogging()
 *
 *      Disable logging to NVM file system
 *
 * Type:
 *      Synchronous, non-reentrant
 *
 * Parameters:
 *	void
 *
 * Returns:
 *     void
 */
void MCP_HAL_LOG_DisableFileLogging(void)
{
	if (McpLogFileFD != NULL)
	{
		mcpf_file_close(NULL,McpLogFileFD);
		McpLogFileFD = NULL;
	}

	gMcpLogToFile = MCP_FALSE;
	McpMaxLogLines = 0;
	McpMaxNvmFiles = 0;
	McpLogFileCurrentChunckNum = 0;
	McpLogFileLineCount = 0;

return;
}


/*-------------------------------------------------------------------------------
 * MCP_HAL_LOG_EnableStdoutLogging()
 *
 *      Enable logging to Stdout
 *
 * Type:
 *      Synchronous, non-reentrant
 *
 * Parameters:
 *	void
 *
 * Returns:
 *     void
 */
void MCP_HAL_LOG_EnableStdoutLogging(void)
{
	MCP_HAL_LOG_EnableLogging();
	gMcpLogToStdout = MCP_TRUE;
}

/*-------------------------------------------------------------------------------
 * MCP_HAL_LOG_DisableStdoutLogging()
 *
 *      Disable logging to Stdout
 *
 * Type:
 *      Synchronous, non-reentrant
 *
 * Parameters:
 *	void
 *
 * Returns:
 *     void
 */
void MCP_HAL_LOG_DisableStdoutLogging(void)
{
	gMcpLogToStdout = MCP_FALSE;
}



/*-------------------------------------------------------------------------------
 * MCP_HAL_LOG_UpdatePlaceHolderFile()
 *
 *      updates the place holder file with the last chunked file
 *
 * Type:
 *      Synchronous, non-reentrant
 *
 * Parameters:
 *	Filename - the newly chunked file name
 *
 * Returns:
 *     void
 */
static void MCP_HAL_LOG_UpdatePlaceHolderFile(McpS8 *fileName)
{
	McpS32 McpPlaceHolderFD = NULL;
	McpU8 ChunkCount[7];
	McpU8 placeHolder_fileName[256];

	EMcpfRes res;

	/* store the filename of the newly chunked file in the place holder file */
	/* create place holder file name */
	MCP_HAL_STRING_StrCpy(placeHolder_fileName, McpLogFileName);
	MCP_HAL_STRING_StrnCat (placeHolder_fileName, ".txt",sizeof(placeHolder_fileName)-1-strlen(placeHolder_fileName));

	/*open the file*/
	res = mcpf_file_open(NULL, (McpU8 *) placeHolder_fileName,
		MCP_HAL_FS_O_BINARY | MCP_HAL_FS_O_WRONLY | MCP_HAL_FS_O_CREATE | MCP_HAL_FS_O_TRUNC, &McpPlaceHolderFD);


	if (res != RES_OK)
	{
		printf("MCP_HAL_LOG_UpdatePlaceHolderFile: error opening place holder file\n");
		return;
	}
	else
	{
		#define TEMP_LEN 12
		McpS8 temp[TEMP_LEN];
		McpU16 templen;
		/*write the number of chunked files */ /* Klocwork Changes */
		snprintf(temp, sizeof(temp),"%05d\n", McpLogFileCurrentChunckNum);

		templen = (McpU16)MCP_StrLenUtf8(temp);

		if(templen > TEMP_LEN)
			templen = TEMP_LEN;

		mcpf_file_write(NULL, McpPlaceHolderFD, &temp[0], templen);

		/*write the last log file created*/
		mcpf_file_write(NULL, McpPlaceHolderFD, (void *)fileName, (McpU16)MCP_StrLenUtf8(fileName));

		/*close the place holder file*/
		mcpf_file_close(NULL, McpPlaceHolderFD);
	}

}



/*-------------------------------------------------------------------------------
 * MCP_HAL_LOG_ChunkLogFile()
 *
 *      Chunks the log file when called
 *
 * Type:
 *      Synchronous, non-reentrant
 *
 * Parameters:
 *	void
 *
 * Returns:
 *     void
 */
static void MCP_HAL_LOG_ChunkLogFile(void)
{
	McpS32 McpPlaceHolderFD = NULL;

	McpU8 fileName[250];
	McpU8 ChunkCount[12];
	McpU8 placeHolder_fileName[250];

	EMcpfRes res;

	printf("MCP_HAL_LOG_ChunkLogFile\n");

	/* Close existing log file */
	if (McpLogFileFD != NULL)
	{
		mcpf_file_close(NULL,McpLogFileFD);
		McpLogFileFD = NULL;
		/* reset the line count for the new file */
		McpLogFileLineCount = 0;

	}

	/* increment the chunk count*/
	if(McpLogFileCurrentChunckNum >= McpMaxNvmFiles)
	{
		McpLogFileCurrentChunckNum = 0;
	}
	else
	{
		McpLogFileCurrentChunckNum++;
	}

	/* copy the file name */

	MCP_HAL_STRING_StrnCpy(fileName, McpLogFileName,sizeof(fileName)-1);



	/*add the Chunk count to the filename*/ /*Klocwork Changes  */
	snprintf(ChunkCount,sizeof(ChunkCount), "-%03d", McpLogFileCurrentChunckNum);

	MCP_HAL_STRING_StrnCat (fileName, ChunkCount,sizeof(fileName)-1-strlen(fileName));



	/*file extension*/ /*Klocwork Changes  */
	MCP_HAL_STRING_StrnCat (fileName, ".bin",sizeof(fileName)-1-strlen(fileName));

	/* open the new file for writing */
	res = mcpf_file_open(NULL,
						(McpU8 *) fileName,
						MCP_HAL_FS_O_BINARY | MCP_HAL_FS_O_WRONLY | MCP_HAL_FS_O_CREATE | MCP_HAL_FS_O_TRUNC,
						&McpLogFileFD);

	if (McpLogFileFD == NULL || res == RES_ERROR)
	{
		printf("MCP_HAL_LOG_ChunkLogFile: unable to open file\n");
		gMcpLogToFile = MCP_FALSE;
		return;
	}
	else
	{

		/* update the place holder file */
		gMcpLogToFile = MCP_TRUE;

		MCP_HAL_LOG_UpdatePlaceHolderFile(fileName);

	}


}

/*-------------------------------------------------------------------------------
 * MCP_HAL_InitUdpSockets()
 *
 *      Init the socket
 *
 * Type:
 *      Synchronous, non-reentrant
 *
 * Parameters:
 *	void
 *
 * Returns:
 *     void
 */
void MCP_HAL_InitUdpSockets(void)
{

	/* Initialize the socket */
	if (gMcpLogToUdpSocket == MCP_TRUE &&  gMcpSocket.udp_sock < 0 )
	{
		/* Create the UDP socket */
		if ((gMcpSocket.udp_sock = socket(PF_INET, SOCK_DGRAM, IPPROTO_UDP)) >= 0)
		{
			/* Construct the server sockaddr_in structure */
			memset(&gMcpSocket.mcpSockAddr, 0, sizeof(gMcpSocket.mcpSockAddr)); /* Clear struct */
			gMcpSocket.mcpSockAddr.sin_family = AF_INET; /* Internet/IP */
			gMcpSocket.mcpSockAddr.sin_port = htons(gMcpSocket.mcpLogUdpTargetPort); /* server port */

			if (inet_aton(gMcpSocket.mcpLogUdpTargetAddress, &gMcpSocket.mcpSockAddr.sin_addr)==0)
			{
				printf(" Looger: UDP Socket Create Fails\n");
			}

			printf(" Looger: UDP Socket Create Success\n");
		}

		printf("InitUdp: Normal IP: %s, port: %d\n",inet_ntoa(gMcpSocket.mcpSockAddr.sin_addr),gMcpSocket.mcpLogUdpTargetPort );

	}
	else
	{
		printf(" MCP_HAL_InitUdpSockets: udp socket already init: %d \n", gMcpSocket.udp_sock );
	}

}

/*-------------------------------------------------------------------------------
 * MCP_HAL_DeInitUdpSockets()
 *
 *      close the socket
 *
 * Type:
 *      Synchronous, non-reentrant
 *
 * Parameters:
 *	void
 *
 * Returns:
 *     void
 */
void MCP_HAL_DeInitUdpSockets(void)
{
	close(gMcpSocket.udp_sock);
}

/*-------------------------------------------------------------------------------
 * MCP_HAL_LOG_Init()
 *
 *      init all the logging
 *
 * Type:
 *      Synchronous, non-reentrant
 *
 * Parameters:
 *	void
 *
 * Returns:
 *     void
 */
void MCP_HAL_LOG_Init(void)
{
	McpU8 u_i;

	/*Initialize flags*/
	gMcpLogToStdout = MCP_FALSE;
	gMcpLogToFile = MCP_FALSE;
	gMcpLogToUdpSocket = MCP_FALSE;
	gMcpLogToAndroid = MCP_FALSE;

	/*Initialize udp data*/
   	gMcpSocket.udp_sock = -1;
	strcpy(gMcpSocket.mcpLogUdpTargetAddress, " ");
   	gMcpSocket.mcpLogUdpTargetPort = MCP_LOG_PORT_DEFAULT;

  	gMcpLogEnabled = MCP_FALSE;
	gInitialized = MCP_TRUE;
}

void MCP_HAL_LOG_Deinit(void)
{
	McpU8 rc;

	MCP_HAL_DeInitUdpSockets();

	gMcpLogEnabled = MCP_FALSE;
	gInitialized = MCP_FALSE;
	gMcpLogToStdout = MCP_FALSE;
	gMcpLogToFile = MCP_FALSE;
	gMcpLogToUdpSocket = MCP_FALSE;
	gMcpLogToAndroid = MCP_FALSE;


	rc = pthread_key_delete(thread_id);

	rc = pthread_key_delete(thread_name);
}


static unsigned int MCP_HAL_LOG_GetThreadId(void)
{
    #ifndef SYS_gettid      //should be normally defined on standard Linux
    #define SYS_gettid 224 //224 on android
    #endif

    return syscall(SYS_gettid);
}

static void MCP_HAL_LOG_SetThreadIdName(unsigned int id, const char *name)
{
    int rc;
    rc = pthread_setspecific(thread_id, (void *)id);
    rc = pthread_setspecific(thread_name, name);
}

static char *MCP_HAL_LOG_GetThreadName(void)
{
    return pthread_getspecific(thread_name);
}

void MCP_HAL_LOG_SetThreadName(const char* name)
{
    MCP_HAL_LOG_SetThreadIdName(MCP_HAL_LOG_GetThreadId(), name);
}

/*-------------------------------------------------------------------------------
 * MCP_HAL_LOG_FormatMsg()
 *
 *      sprintf-like string formatting. the formatted string is allocated by the function.
 *
 * Type:
 *      Synchronous, non-reentrant
 *
 * Parameters:
 *
 *      format [in]- format string
 *
 *      ...     [in]- additional format arguments
 *
 * Returns:
 *     Returns pointer to the formatted string
 *
 */
char *MCP_HAL_LOG_FormatMsg(const char *format, ...)
{
    va_list     args;

    _mcpLog_FormattedMsg[MCP_HAL_MAX_FORMATTED_MSG_LEN] = '\0';

    va_start(args, format);

    vsnprintf(_mcpLog_FormattedMsg, MCP_HAL_MAX_FORMATTED_MSG_LEN, format, args);

    va_end(args);

    return _mcpLog_FormattedMsg;
}

const char *MCP_HAL_LOG_SeverityCodeToName(McpHalLogSeverity Severity)
{
   switch(Severity)
    {
        case (MCP_HAL_LOG_SEVERITY_FUNCTION):
            return ("FUNCT");
        case (MCP_HAL_LOG_SEVERITY_DEBUG):
            return ("DEBUG");
        case (MCP_HAL_LOG_SEVERITY_INFO):
            return ("INFO");
        case (MCP_HAL_LOG_SEVERITY_ERROR):
            return ("ERROR");
        case (MCP_HAL_LOG_SEVERITY_FATAL):
            return ("FATAL");
        default:
            return (" ");
    }
    return (" ");
}

/*-------------------------------------------------------------------------------
 * MCP_HAL_LOG_LogToFile()
 *
 *	writes to the log message to the NVM file
 *	The function tries to maintain the logger format on NVM
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *
 *      fileName [in] - name of file originating the message
 *
 *      line [in] - line number in the file
 *
 *      moduleType [in] - e.g. "BTL_BMG", "BTL_SPP", "BTL_OPP",
 *
 *      severity [in] - debug, error...
 *
 *      msg [in] - message in already formatted string
 *
 *  Returns:
 *      void
 *
 */
static void MCP_HAL_LOG_LogToFile(const char*       fileName,
                           McpU32           line,
                           McpHalLogModuleId_e moduleId,
                           McpHalLogSeverity severity,
                           const char*      msg)
{
	log_msg_NVM_t nvm_msg;
	char *threadName = MCP_HAL_LOG_GetThreadName();
	size_t logStrLen = 0;

	/* check if log file needs to be chuncked */
	if (McpLogFileLineCount >= McpMaxLogLines)
	{
		/* disable logging to file, while the chunk file is created */
		/* this is done, since this function can be called by multiple threads simultaneously */
		/* and if during this time, the FD is undeclared, the wirete function below can could lead to a SEGV */
		gMcpLogToFile = MCP_FALSE;

		/* create the chunk file */
		MCP_HAL_LOG_ChunkLogFile();

		/* reenable the logging */
		gMcpLogToFile = MCP_TRUE;
	}

	/* return if the FD is not set */
	if (McpLogFileFD == NULL || McpLogFileFD == -1)
		return;

	/* Reset log message */
	memset(&nvm_msg,0,sizeof(log_msg_NVM_t));

	strncpy(nvm_msg.message,msg,MCPHAL_LOG_MAX_MESSAGE_LENGTH_NVM-1);

/* KW error
	logStrLen = strlen(nvm_msg.message);

	if (nvm_msg.message[logStrLen-1] == '\n')
		nvm_msg.message[logStrLen-1] = ' ';
*/
	/* Copy file name */
	strncpy(nvm_msg.fileName,(fileName==NULL ? "MAIN":fileName),MCPHAL_LOG_MAX_FILENAME_LENGTH_NVM-1);

	/* Copy thread name */
	strncpy(nvm_msg.moduleName,(threadName!=NULL ? threadName :"UNKN"),MCPHAL_LOG_MAX_MODULENAME_LENGTH_NVM-1);

	/* Copy other relevant log information */
	nvm_msg.line = line;
	nvm_msg.severity = severity;

	if (mcpf_file_write(NULL, McpLogFileFD, (void *)&nvm_msg, (McpU16)sizeof(nvm_msg)) != (McpU16)sizeof(nvm_msg))
		printf("error writing to file");
	else
		McpLogFileLineCount++; /* increment the line count */
}



/*-------------------------------------------------------------------------------
 * MCP_HAL_LOG_LogToUdp()
 *
 *      writes to the log message to the UDP port
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *
 *      fileName [in] - name of file originating the message
 *
 *      line [in] - line number in the file
 *
 *      moduleType [in] - e.g. "BTL_BMG", "BTL_SPP", "BTL_OPP",
 *
 *      severity [in] - debug, error...
 *
 *      msg [in] - message in already formatted string
 *
 *  Returns:
 *      void
 *
 */
static void MCP_HAL_LOG_LogToUdp(const char*        fileName,
                          McpU32            line,
                          McpHalLogModuleId_e moduleId,
                          McpHalLogSeverity severity,
                          const char*       msg)
{
	udp_log_msg_t udp_msg;
	char *threadName = MCP_HAL_LOG_GetThreadName();
	size_t logStrLen = 0;

	/* Reset log message */
	memset(&udp_msg,0,sizeof(udp_log_msg_t));

	/* Copy user message */
	strncpy(udp_msg.message,msg,MCPHAL_LOG_MAX_MESSAGE_LENGTH-1);
/* KW -error
	logStrLen = strlen(udp_msg.message);

	if (udp_msg.message[logStrLen-1] == '\n')
		udp_msg.message[logStrLen-1] = ' ';
*/
	/* Copy file name */
	strncpy(udp_msg.fileName,(fileName==NULL ? "MAIN":fileName),MCPHAL_LOG_MAX_FILENAME_LENGTH-1);

	/* Copy thread name */
	strncpy(udp_msg.threadName,(threadName!=NULL ? threadName :"UK"),MCPHAL_LOG_MAX_THREADNAME_LENGTH-1);

	/* Copy other relevant log information */
	udp_msg.line = line;
	udp_msg.severity = severity;

	/*send over UDP port*/
	(void)sendto(gMcpSocket.udp_sock,
		&udp_msg,
		sizeof(udp_msg),
		MSG_DONTWAIT,
		(struct sockaddr *) &gMcpSocket.mcpSockAddr,
		sizeof(gMcpSocket.mcpSockAddr));
}



#ifdef ANDROID
/*-------------------------------------------------------------------------------
 * MCP_HAL_LOG_LogToAndroid()
 *
 *      writes to the log message to Android logging framework
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *
 *      fileName [in] - name of file originating the message
 *
 *      line [in] - line number in the file
 *
 *      moduleType [in] - e.g. "BTL_BMG", "BTL_SPP", "BTL_OPP",
 *
 *      severity [in] - debug, error...
 *
 *      msg [in] - message in already formatted string
 *
 *  Returns:
 *      void
 *
 */
static void MCP_HAL_LOG_LogToAndroid(const char*        fileName,
                          McpU32            line,
                          McpHalLogModuleId_e moduleId,
                          McpHalLogSeverity severity,
                          const char*       msg)
{
    char copy_of_msg[MCP_HAL_MAX_USER_MSG_LEN+1] = "";
    size_t copy_of_msg_len = 0;
    char *threadName = MCP_HAL_LOG_GetThreadName();

    strncpy(copy_of_msg,msg,MCP_HAL_MAX_USER_MSG_LEN);
    copy_of_msg_len = strlen(copy_of_msg);
    if (copy_of_msg_len >0)
    	{
	    if (copy_of_msg[copy_of_msg_len-1] == '\n')
	        copy_of_msg[copy_of_msg_len-1] = ' ';
    	}

    switch (severity)
    {
        case MCP_HAL_LOG_SEVERITY_FUNCTION:
            LOGD("%s(%s):%s (%ld@%s)",MCP_HAL_LOG_Modules[moduleId].name,
                threadName,
                copy_of_msg,
                line,
                fileName);
            break;
        case MCP_HAL_LOG_SEVERITY_DEBUG:
            LOGD("%s(%s):%s (%ld@%s)",MCP_HAL_LOG_Modules[moduleId].name,
                threadName,
                copy_of_msg,
                line,
                fileName);
            break;
        case MCP_HAL_LOG_SEVERITY_INFO:
            LOGI("%s(%s):%s (%ld@%s)",MCP_HAL_LOG_Modules[moduleId].name,
                threadName,
                copy_of_msg,
                line,
                fileName);
            break;
        case MCP_HAL_LOG_SEVERITY_ERROR:
            LOGE("%s(%s):%s (%ld@%s)",MCP_HAL_LOG_Modules[moduleId].name,
                MCP_HAL_LOG_GetThreadName(),
                copy_of_msg,
                line,
                fileName);
            break;
        case MCP_HAL_LOG_SEVERITY_FATAL:
            LOGE("%s(%s):%s (%ld@%s)",MCP_HAL_LOG_Modules[moduleId].name,
                threadName,
                copy_of_msg,
                line,
                fileName);
            break;
        default:
            break;
    }
}
#endif

/*-------------------------------------------------------------------------------
 * MCP_HAL_LOG_LogToStdio()
 *
 *      writes to the log message to stdio
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *
 *      fileName [in] - name of file originating the message
 *
 *      line [in] - line number in the file
 *
 *      moduleType [in] - e.g. "BTL_BMG", "BTL_SPP", "BTL_OPP",
 *
 *      severity [in] - debug, error...
 *
 *      msg [in] - message in already formatted string
 *
 *  Returns:
 *      void
 *
 */
static void MCP_HAL_LOG_LogToStdio(const char*       fileName,
                           McpU32           line,
                           McpHalLogModuleId_e moduleId,
                           McpHalLogSeverity severity,
                           const char*      msg)
{
    struct timeval detail_time;
    char copy_of_msg[MCP_HAL_MAX_USER_MSG_LEN+1] = "";
    char log_formatted_str[MCP_HAL_MAX_FORMATTED_MSG_LEN+1] = "";
    size_t copy_of_msg_len = 0;
    char* moduleName = NULL;
    static int file_size = 0;

    /* Remove any CR at the end of the user message */
    strncpy(copy_of_msg,msg,MCP_HAL_MAX_USER_MSG_LEN);
    copy_of_msg_len = strlen(copy_of_msg);
    if (copy_of_msg_len>0)
    	{
	    if (copy_of_msg[copy_of_msg_len-1] == '\n')
	        copy_of_msg[copy_of_msg_len-1] = ' ';
    	}

    /* Get the thread name */
    char *threadName = MCP_HAL_LOG_GetThreadName();

    /* Query for current time */
    gettimeofday(&detail_time,NULL);

    /* Get the module name */
    moduleName = MCP_HAL_LOG_Modules[moduleId].name;

    /* Format the final log message to be printed */
    snprintf(log_formatted_str,MCP_HAL_MAX_FORMATTED_MSG_LEN,
              "%06ld.%06ld|%-5s|%-15s|%s|%s {%s@%ld}\n",
              detail_time.tv_sec - g_start_time_seconds,
              detail_time.tv_usec,
              MCP_HAL_LOG_SeverityCodeToName(severity),
              moduleName,
              (threadName != NULL ? threadName: "UNKNOWN"),
              copy_of_msg,
              (fileName==NULL?"UNKOWN":fileName),
              line);

    /* send the string to the console */
   printf("%s\n",log_formatted_str);
}


/*-------------------------------------------------------------------------------
 * MCP_HAL_LOG_LogMsg()
 *
 *      Sends a log message to the local logging tool.
 *      Not all parameters are necessarily supported in all platforms.
 *
 * Type:
 *      Synchronous
 *
 * Parameters:
 *
 *      fileName [in] - name of file originating the message
 *
 *      line [in] - line number in the file
 *
 *      moduleType [in] - e.g. "BTL_BMG", "BTL_SPP", "BTL_OPP",
 *
 *      severity [in] - debug, error...
 *
 *      msg [in] - message in already formatted string
 *
 *  Returns:
 *      void
 *
 */
void MCP_HAL_LOG_LogMsg(    const char*     fileName,
                                McpU32          line,
                                McpHalLogModuleId_e moduleId,
                                McpHalLogSeverity severity,
                                const char*         msg)
{
	if((gInitialized == 1) &&( gMcpLogEnabled ==1))
	{
		if (gMcpLogToUdpSocket == MCP_LOG_MODE_NORMAL)
			MCP_HAL_LOG_LogToUdp(fileName,line,moduleId,severity,msg);

		if (gMcpLogToAndroid == MCP_TRUE)
			MCP_HAL_LOG_LogToAndroid(fileName,line,moduleId,severity,msg);

		if (gMcpLogToFile == MCP_TRUE)
			MCP_HAL_LOG_LogToFile(fileName,line,moduleId,severity,msg);

		if (gMcpLogToStdout == MCP_TRUE)
			MCP_HAL_LOG_LogToStdio(fileName,line,moduleId,severity,msg);
	}

}


/*-------------------------------------------------------------------------------
 * DumpBuf()
 *
 *		Sends a log message to the local logging tool.
 *		Not all parameters are necessarily supported in all platforms.
 *
 * Type:
 *		Synchronous
 *
 * Parameters:
 *
 *		fileName [in] - name of file originating the message
 *
 *		line [in] - line number in the file
 *
 *		severity [in] - debug, error...
 *
 *		pTitle [in] - title, already formatted string, limited to 40 bytes length
 *
 *		pBuf [in] - pointer to buffer to dump contents
 *
 *		len [in]  - buffer size to dump
 * 	Returns:
 *		void
 *
 */
void MCP_HAL_LOG_DumpBuf (const char 		*fileName,
						 McpU32				line,
						 McpHalLogSeverity 	severity,
						 const char			*pTitle,
						 const McpU8		*pBuf,
						 const McpU32		len)
{
#ifdef TI_DBG
#define D_LINE_SIZE	64

	McpU32 i,j;
	char  * pOut, outBuf[(D_LINE_SIZE * 3)+50];

	pOut = outBuf;

	for (i=0; i<len && pOut<(outBuf + sizeof(outBuf)); i += D_LINE_SIZE)
	{
		pOut += sprintf(pOut, "%s %u:", pTitle, i);
		for (j=0; j<D_LINE_SIZE && len>i+j && (pOut<(outBuf + sizeof(outBuf))); j++  )
		{
			pOut += sprintf(pOut, " %02x", *(pBuf+i+j));
		}

		//LOGD("DUMPBUF: %s", outBuf); //dont call android here,, take the normal route
		MCP_HAL_LOG_LogMsg(__FILE__, __LINE__, 0, MCP_HAL_LOG_SEVERITY_DEBUG, outBuf);
		pOut = outBuf;
	}
#else
    MCPF_UNUSED_PARAMETER(fileName);
    MCPF_UNUSED_PARAMETER(line);
    MCPF_UNUSED_PARAMETER(severity);
#endif
}

