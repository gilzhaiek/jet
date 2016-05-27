/*************************************************************************/

#include <stdio.h>
#include <stdarg.h>
#include <string.h>
#include "mcp_hal_types.h"
#include "nav_log_msg.h"
#include "nav_log_cust_codes.h"
#include "mcpf_report.h"
#include "mcp_hal_fs.h"

#include <stdlib.h>
#include <sys/time.h>
#include <utils/Log.h>

navLog navLogData = {-1,0,0,0}; //{navLogmsgFd, hMcpf,fixMode,sessNo}
navLog *pSuplNavLog = &navLogData;

/*************************************************************************/

void nav_sess_log_init(handle_t hMcpf, char * a_AidingPath)
{

  McpU8 uAidingFile[MAX_NAVLOGMSG_LENGTH+20];
  navLogData.hMcpf = hMcpf;

  LOGD("SessLog: nav_sess_log_init: entering");

  /* Klocwork Changes */
  if(strlen(a_AidingPath) >= MAX_NAVLOGMSG_LENGTH)
  {
    LOGD("SessLog: nav_sess_log_init: Filename length too long! \n");
    return;
  }

  MCP_HAL_STRING_StrCpy (uAidingFile,(McpU8*)a_AidingPath);
  MCP_HAL_STRING_StrCat (uAidingFile,NAV_SESS_LOG_FILE_NAME);
  if (mcpf_file_open(hMcpf, (McpU8 *)uAidingFile,
    MCP_HAL_FS_O_RDWR | MCP_HAL_FS_O_TEXT | MCP_HAL_FS_O_APPEND | MCP_HAL_FS_O_CREATE, &(navLogData.navLogFd)) == RES_OK)
  {
    LOGD ("SessLog: nav_sess_log_init: File created success!\n");
  }
  else
  {
    LOGD("SessLog: nav_sess_log_init: Failed to open file! \n");
  }
  if(pSuplNavLog != NULL)
    LOGD("SessLog: nav_sess_log_init, hMcpf = %d ,navLogFd = %d sessNo = %d \n",pSuplNavLog->hMcpf,pSuplNavLog->navLogFd,pSuplNavLog->sessNo); 
}

void nav_sess_log_deinit(handle_t hMcpf)
{
  if(pSuplNavLog != NULL)
  {
    if((pSuplNavLog != NULL) && (pSuplNavLog->navLogFd != -1))
    {
      if (mcpf_file_close(pSuplNavLog->hMcpf, pSuplNavLog->navLogFd) == RES_OK)
      {
        LOGD("SessLog: nav_sess_log_deinit: Closed file success!");
      }
      else
      {
        LOGD("SessLog: nav_sess_log_deinit: Failed to close file!");
      }
    }
    else
    {
      LOGD("SessLog: nav_sess_log_deinit: FD is NULL!");
    }

    pSuplNavLog->navLogFd = -1;
  }
  else
  {
    LOGD("SessLog: nav_sess_log_deinit:pSuplNavLog = NULL !!!");
  }
}


//#define SESSLOGMSG navc_sess_log_msg
void nav_sess_log_msg(char *p_sDiagMsg, ...)
{
  char local_buffer[MAX_NAVLOGMSG_LENGTH+32];
  McpU32 		len = 0;

  va_list marker;
  va_start(marker, p_sDiagMsg);
  len = vsprintf(local_buffer, p_sDiagMsg, marker);

  if((pSuplNavLog != NULL) && (pSuplNavLog->navLogFd != -1))
  {
    if (mcpf_file_write(pSuplNavLog->hMcpf, pSuplNavLog->navLogFd, (McpU8 *)local_buffer, (McpU16)len)  > 0)
    {
      LOGD ("[GPS_CMCC] %s\n", local_buffer);
    }
    else
    {
      LOGD ("SessLog: Failed to write: %s \n ",local_buffer);
    }
  }
  else
  {
    LOGD("SessLog: [GPS_CMCC] %s\n", local_buffer);
  }
}

void nav_sess_setPtr(void * pPtr)
{
  pSuplNavLog = (navLog *) pPtr;
}

McpS32 nav_sess_getCount(McpU8 incFlag)
{
  if(incFlag)
  {
    (pSuplNavLog->sessNo)++;
    return pSuplNavLog->sessNo;
  }
  else
  {
    return pSuplNavLog->sessNo;
  }
}

char * get_utc_time()
{
  struct timeval tv;
  struct tm *local;
  time_t t;
  static char timestr[71];	/* Klocwork Changes */
 
  t = time(NULL);
  local = gmtime(&t);
  gettimeofday(&tv, 0);
  /* Klocwork Changes */
  snprintf (timestr, sizeof(timestr),"%4d%02d%02d%02d%02d%02d.%03d", 1900 + local->tm_year,local->tm_mon + 1,local->tm_mday,local->tm_hour, local->tm_min,local->tm_sec,(int)(tv.tv_usec)/1000);
  return timestr;
}

