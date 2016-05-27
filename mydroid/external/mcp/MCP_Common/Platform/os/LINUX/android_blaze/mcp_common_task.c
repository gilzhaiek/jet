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
## Control List (CCL). The assurances provided for herein are furnished in    *
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
*   FILE NAME:      mcp_common_task.c
*
*   DESCRIPTION:    
*
*   AUTHOR:         Chen Ganir, Bella Schor, Vladimir Abram
*
\*******************************************************************************/


/* Header files includes */
#include <string.h>
#include <stdlib.h>
#include <errno.h>
#include <stddef.h>
#include <unistd.h>
#include <pthread.h>
#include <stdio.h>      /* for printf() and fprintf() */
#include <sys/socket.h> /* for socket(), bind(), and connect() */
#include <arpa/inet.h>  /* for sockaddr_in and inet_ntoa() */
#include <sys/time.h>
#include <sys/resource.h>
#include <sys/stat.h>
#ifndef ANDROID
#include <signal.h>
#endif /* ANDROID */
#include "mcp_main.h"


/* tbd leave like this? move?*/
handle_t	 hMcpf;

void init_signal_behavior(void);
void generate_cmd_line(int argc, char** argv, char *buf);
void run_as_daemon(void);


/* Macros */

//for now, define REPORT as printf
#ifndef Report
#define Report(x) printf x
#endif


int main(int argc, char** argv)
{
    char        cmd_line[256] = "";

    generate_cmd_line(argc, argv, cmd_line);
     
    Report (("cmd line %s\n", cmd_line));

    init_signal_behavior();

    if (0 != strstr((const char *)cmd_line, "--run_as_daemon"))
    {
        run_as_daemon();
    }
  
    Report(("MCP | main | starting...\n"));
    Report(("Note: this task requires root priviledges\n"));

    /* MCP_Init invokes all the other tasks */
    hMcpf = MCP_Init (cmd_line);

    MCP_Deinit(hMcpf);

    Report(("MCP | main | De-initialization completed\n"));
    
    return 0;
}

void generate_cmd_line(int argc, char** argv, char *buf)
{
    int         i;
    McpU32  len;
    
    /* Parse command line parameters and combine them into one command line string */
    for (i=0; i<argc; i++)
    {
        strcat(buf, (const char *)argv[i]);
        strcat(buf, " ");
    }

    /* Remove the last space */
    len = strlen((const char *)buf);
    buf[len-1] = '\0';
}

void sighandler_logger(int signum, siginfo_t* info, void* context)
{
    void *addr = NULL;

    MCPF_UNUSED_PARAMETER(context);

    switch (signum) {
    case SIGILL:
    case SIGFPE:
    case SIGSEGV:
    case SIGBUS:
        addr = info->si_addr;
        break;
    }

    Report(("signal %d sent to our program from address %p and code %d", 
            signum, 
            addr,info->si_code));

    // do the default action upon receiving the signal
    signal(signum, SIG_DFL);
}

void init_signal_behavior()
{
    struct sigaction sa;    
#ifndef EBTIPS_RELEASE
    struct rlimit corelim;
#endif /* EBTIPS_RELEASE */

    Report(("MCP | initializing sighandler\n"));

    // initialize signal behavior
    memset(&sa, 0, sizeof(sa));
    sigaction(SIGPIPE, &sa, NULL);

    memset(&sa, 0, sizeof(sa));
    sa.sa_flags = SA_SIGINFO;
    sa.sa_sigaction = sighandler_logger;
    sigaction(SIGTERM, &sa, NULL);
    sigaction(SIGINT,  &sa, NULL);
    sigaction(SIGILL,  &sa, NULL);
    sigaction(SIGSEGV,  &sa, NULL);
    sigaction(SIGBUS,  &sa, NULL);
    sigaction(SIGFPE,  &sa, NULL);
    sigaction(SIGKILL,  &sa, NULL);
    sigaction(SIGSTOP,  &sa, NULL);
    sigaction(SIGABRT,  &sa, NULL);
    sigaction(SIGQUIT,  &sa,NULL);
    sigaction(SIGSTKFLT, &sa,NULL);
    
#ifndef EBTIPS_RELEASE
    if (0 != chdir(CORE_DUMP_LOCATION))
    {
        Report(("could not set new working dir: %s", strerror(errno)));
    }
    
    corelim.rlim_cur = RLIM_INFINITY;
    corelim.rlim_max =  RLIM_INFINITY;
    
    if (0 != setrlimit(RLIMIT_CORE,&corelim))
    {
        Report(("could not set rlimit for core dump: %s", strerror(errno))); 
    }
#endif /* EBTIPS_RELEASE */
}

void run_as_daemon(void)
{
    char        cmd[256];
    
    /* Our process ID and Session ID */
    pid_t       pid, sid;
    
    /* Fork off the parent process */
    pid = fork();
    if (pid < 0) {
        Report(("BTBUS | Error Forking daemon. Aborting\n"));
        printf("PID ERROR\n");
        exit(EXIT_FAILURE);
    }
    /* If we got a good PID, then
       we can exit the parent process. */
    if (pid > 0) {
        sprintf(cmd,"echo %d >/tmp/btipsd.pid",pid);
        system(cmd);
                
        Report(("BTBUS | Forking daemon. \n"));
        exit(EXIT_SUCCESS);
    }

    /* Change the file mode mask */
    umask(0);
                  
    /* Create a new SID for the child process */
    sid = setsid();
    if (sid < 0) {
        Report(("BTBUS | Error creating new SID. Aborting\n"));
        printf("SID ERROR\n");
        /* Log the failure */
        exit(EXIT_FAILURE);
    }
        

        
    /* Change the current working directory */
    if ((chdir("/tmp")) < 0) {
        Report(("BTBUS | Error changing to /tmp. Aborting\n"));
        printf("CHDIR /TMP ERROR\n");            
        exit(EXIT_FAILURE);
    }
        
    /* Close out the standard file descriptors */
    close(STDIN_FILENO);
    close(STDERR_FILENO);
    close(STDOUT_FILENO);
}

