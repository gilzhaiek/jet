#ifndef __NAVC_LOG_MSG__
#define __NAVC_LOG_MSG__

#ifdef __cplusplus
extern "C" {
#endif

#include "mcp_hal_types.h"
#include "mcpf_defs.h"
#include "mcp_hal_types.h"

#define SESSLOGMSG nav_sess_log_msg
#define NAV_SESS_LOG_FILE_NAME "NavSessLog.txt"
#define MAX_NAVLOGMSG_LENGTH 220

typedef struct 
{
	McpS32 navLogFd;
	handle_t hMcpf;
	McpU8 fixMode;
	McpS32 sessNo;
	
}navLog;

extern navLog *pSuplNavLog;

void nav_sess_log_init(handle_t hMcpf, char * a_AidingPath);
void nav_sess_log_deinit(handle_t hMcpf);
void nav_sess_log_msg(char *p_sDiagMsg, ...);
void nav_sess_setPtr(void * pPtr);
McpS32 nav_sess_getCount(McpU8 incFlag);
char * get_utc_time();

#ifdef __cplusplus
}
#endif

#endif



