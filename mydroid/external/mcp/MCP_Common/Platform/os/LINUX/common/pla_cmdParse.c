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

/** Include Files **/
#include <stdarg.h>
#include <stdio.h>
#include "string.h"
#include "pla_cmdParse.h"


/************************************************************************/
/*                           Definitions                                */
/************************************************************************/

//for now, define REPORT as printf
#ifndef Report
#define Report(x) printf x
#endif

#define PATHCONFIGFILE "-c/system/bin/pathconfigfile.txt"


/************************************************************************/
/*                          Global Variables                            */
/************************************************************************/


/************************************************************************/
/*                  Internal Function Declaration                       */
/************************************************************************/
static void getParamString(char *paramName, char *buf);


/************************************************************************/
/*                  External Function Declaration                       */
/************************************************************************/
extern void MCP_HAL_LOG_EnableUdpLogging(const char* ip, unsigned long port);
extern void MCP_HAL_LOG_EnableFileLogging(const char* fileName);
extern void MCP_HAL_LOG_EnableStdoutLogging(void);
extern void MCP_HAL_LOG_EnableLogToAndroid(const char *app_name);



/************************************************************************/
/*                              APIs                                    */
/************************************************************************/
/**
 * \fn     pla_cmdParse_GeneralCmdLine
 * \brief  Parses the initial command line
 *
 * This function parses the initial command line,
 * and returns the BT & NAVC strings,
 * along with the common parameter.
 *
 * \note
 * \param   CmdLine     - Received command Line.
 * \param   **bt_str - returned BT cmd line.
 * \param   **nav_str - returned NAVC cmd line.
 * \return  Port Number
 * \sa      pla_cmdParse_GeneralCmdLine
 */
McpU32 pla_cmdParse_GeneralCmdLine(LPSTR CmdLine, char **bt_str, char **nav_str)
{
    /* Currently, in Linux the serial port name is hardcoded in bthal_config.h.
     * So, put some number in order to prevent assert in a caller function */
    McpU32      uPortNum = -1;
    
    char        *pParam;
    char        *paramParse;
    char        pathfile[256] = "";
    char        logName[30] = "";
    char        logFile[256] = "";
    char        logIp[30] = "";
    char        portStr[30] = "";
    unsigned long port = 0;
    char        *pPort;
    int         i=0;

    MCPF_UNUSED_PARAMETER(bt_str);
    
	pPort = strstr(CmdLine, "-p");
	
	if(pPort != NULL)
	{
		uPortNum = atoi(pPort+2);
		sprintf((*nav_str), "%s%d", "-p", uPortNum); 
	}

    
    /*Issue Fix -nav*/
    if (0 != (pParam = strstr((const char *)CmdLine, "-nav")))
    {
	
	paramParse = pParam;   
	paramParse += 5;	 
	
	strcat (*nav_str, " ");

	while(*paramParse != '"')
	{
		
              pathfile[i] = *paramParse;
		paramParse++;
		i++;
       }
       strcat (*nav_str, pathfile);

	   		MCP_HAL_LOG_INFO(__FILE__,
                                      __LINE__,
                                      MCP_HAL_LOG_MODULE_TYPE_MCP_MAIN,
                                      ("pla_cmdParse_GeneralCmdLine111: NAV Cmd line \"%s\"", *nav_str));
    }
    else
    {
	strcat (*nav_str, " ");
	strcat (*nav_str, PATHCONFIGFILE);

    }

#ifdef ANDROID
    if (0 != (pParam = strstr((const char *)CmdLine, "--android_log")))
    {
        getParamString(pParam, logName);
        MCP_HAL_LOG_EnableLogToAndroid((const char *)logName);
    }
#endif
    if (0 != (pParam = strstr((const char *)CmdLine, "-logfile")))
    {
        getParamString(pParam, logFile);
        MCP_HAL_LOG_EnableFileLogging((const char *)logFile);
    }
    else if (0 != strstr((const char *)CmdLine, "--log_to_stdout"))
    {
        MCP_HAL_LOG_EnableStdoutLogging();
    }
    else if (0 != (pParam = strstr((const char *)CmdLine, "-log_ip")))
    {
        getParamString(pParam, logIp);
        
        if (0 != (pParam = strstr((const char *)CmdLine, "-log_port")))
        {
            getParamString(pParam, portStr);
            port = atoi((const char *)portStr);
        }
        if (strlen(logIp)>0 && port>0)
        {
            MCP_HAL_LOG_EnableUdpLogging(logIp, port);
            MCP_HAL_LOG_INFO(__FILE__,
                             __LINE__,
                             MCP_HAL_LOG_MODULE_TYPE_MCP_MAIN,
                             ("pla_cmdParse_GeneralCmdLine: UDP logging, IP %s, port %d",
                              logIp, port));
        }
    }
    else if (0 != strstr((const char *)CmdLine, "--help"))
    {
        printf ("btipsd usage :\nbtipsd [options]\n-h, --help : Show this screen\n-r, --run_as_daemon : Run the mcpd as daemon in the background\n-l, --logfile [LOGFILENAME] : Log to specified file\n-d, --log_to_stdout : Log to STDOUT\n-log_ip [IP_ADDRESS] : Target UDP Listener IP for logging\n-log_port [PORT] : Target UDP Listener PORT for logging\n");
#ifdef ANDROID
        printf ("-no_android_log : Disable Android logging\n");
#endif
        return 0;
    }
    
    MCP_HAL_LOG_INFO(__FILE__,
                                      __LINE__,
                                      MCP_HAL_LOG_MODULE_TYPE_MCP_MAIN,
                                      ("pla_cmdParse_GeneralCmdLine: cmd line \"%s\"", CmdLine));
    
    return uPortNum;
  
}


/**
 * \fn     pla_cmdParse_NavcCmdLine
 * \brief  Parses the NAVC command line
 *
 * This function parses the NAVC command line.
 *
 * \note
 * \param   *nav_str - NAVC cmd line.
 * \param   *tCmdLineParams - Params to fill.
 * \return  None
 * \sa      pla_cmdParse_NavcCmdLine
 */
void pla_cmdParse_NavcCmdLine(char *nav_str, TcmdLineParams *tCmdLineParams)
{
    char    *temp;
    char    *pHyphen;
    char    *pSpace;
    char *pComma;
    char *pPort;
    McpU16  i = 0;


    /* Parse params */
    temp = nav_str;
    while(temp)
    {
        pHyphen = strstr(temp, "-");
        MCPF_Assert(pHyphen);
        pSpace = strstr(temp, " ");
        if(pSpace != NULL)
        {
            temp = pSpace + 1;
        }
        else
        {
            temp = pSpace;
        }

        if( (strncmp( pHyphen, "-p", 2 ) == 0) &&   (sscanf( pHyphen+2, "%d", &tCmdLineParams->x_SensorPcPort) == 1) )
        {
            continue;
        }

        if( (strncmp( pHyphen, "-l", 2 ) == 0) &&  (sscanf( pHyphen+2, "%d", &tCmdLineParams->x_NmeaPcPort) == 1) )
        {
             continue;
        }

        if( (strncmp( pHyphen, "-b", 2 ) == 0) )
        {
            /* To Restrict the Almanac data injection during 3GPP test*/
            sscanf(pHyphen+2, "%x", (McpU32 *) &tCmdLineParams->w_AlmanacDataBlockFlag);
            continue;
        }

	   if( (strncmp( pHyphen, "-c", 2 ) == 0) )
			{
				/* Path Control File */
                               
				sscanf(pHyphen+2, "%50s", (char *)&tCmdLineParams->s_PathCtrlFile);

				continue;
			}
	}
}

static void getParamString(char *paramName, char *buf)
{
    char *param = strchr((const char *)paramName, ' ');
    
    MCPF_Assert(param);
    
    /* Skip spaces */
    while (*param == ' ')
    {
        param++;
    }

    /* Copy parameter string to output buffer */
    while (*param != ' ' && *param != '\0')
    {
        *buf++ = *param++;
    }
    *buf = '\0';
}

