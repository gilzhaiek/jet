/*
* Copyright (c) 2011 Rx Networks, Inc. All rights reserved.
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
* $LastChangedDate: 2009-03-02 15:53:19 -0800 (Mon, 02 Mar 2009) $
* $Revision: 9396 $
*************************************************************************
*
*/

#include <arpa/inet.h>
#include <sys/time.h>
#include <fcntl.h>
#include <errno.h>            /* Required for error handling */
#include <pthread.h>
#include <sys/stat.h>
#include <signal.h>
#include <assert.h>
#include <sys/resource.h>
#include <stdlib.h>


/* need for rm command */
#include <dirent.h>

#ifdef USE_NATIVE_SOCKET
#include <sys/ioctl.h>
#include <linux/if.h>
#endif

#include "RXN_MSL_Common.h"   /* Includes declarations for fcns within and common declarations. */
#include "RXN_MSL_Platform.h"

/* See global definitions within RXN_MSL_Common.c. */
extern MSL_Config_t gConfig;

#ifdef ANDROID
extern int MSL_SetFdSet(fd_set* readSet);
extern void MSL_PreSelect();
extern void MSL_PostSelect(const fd_set* readSet);
extern int64_t elapsedRealtime();
#else
static int MSL_SetFdSet(fd_set* readSet);
#endif

/********************************************************
* Static vars used to support resources such as files, *
* sockets, etc.                                        *
********************************************************/

#define MSL_WAKE_LOCK_BUFFER_SIZE 100

/* pipe used to wake up select timeout */
static int mWakePipe[2] = {-1,-1};

static int mCmdPipe[2];   /* We will use this pipe to send commands to MsgSocketThread */

/* Critical sections used to protect access to MSL and PGPS libs
* on a global and on a per-PRN basis. Array indices will be setup as follows:
* 0 - Critical Section protecting SA/PG access to all PRNs (typically used by PGPS)
* 1-32 - Critical Sections protecting SA/PG access to individual PRNs (typically used by SAGPS)
* 33-35 - Critical sections protecting ephemeris (33), seed (34) and poly (35) files. */
static pthread_mutex_t mPRNCritSec[RXN_MSL_MAX_CS];

static int unlink_recursive(const char* name);


/******************
* File Functions *
******************/

U16 MSL_CreateDir(char dir[RXN_MSL_MAX_PATH])
{
    /* Create the directory using default security attibutes. */
    if(mkdir(dir, 0771) != 0)
    {
        return MSL_FILE_OPENCLOSE_ERROR;
    }
    return RXN_SUCCESS;
}

void MSL_CleanDir(char folderpath[RXN_MSL_MAX_PATH])
{
    unlink_recursive(folderpath);
}

/* return -1 on failure, with errno set to the first error */
static int unlink_recursive(const char* name)
{
    struct stat st;
    DIR *dir;
    struct dirent *de;
    int fail = 0;

    /* is it a file or directory? */
    if (lstat(name, &st) < 0)
        return -1;

    /* a file, so unlink it */
    if (!S_ISDIR(st.st_mode))
        return unlink(name);

    /* a directory, so open handle */
    dir = opendir(name);
    if (dir == NULL)
        return -1;

    /* recurse over components */
    errno = 0;
    while ((de = readdir(dir)) != NULL) {
        char dn[PATH_MAX];
        if (!strcmp(de->d_name, "..") || !strcmp(de->d_name, "."))
            continue;
        snprintf(dn, PATH_MAX, "%s/%s", name, de->d_name);
        if (unlink_recursive(dn) < 0) {
            fail = 1;
            break;
        }
        errno = 0;
    }
    /* in case readdir or unlink_recursive failed */
    if (fail || errno < 0) {
        int save = errno;
        closedir(dir);
        errno = save;
        return -1;
    }

    /* close directory handle */
    if (closedir(dir) < 0)
        return -1;

    return 0;
    /* delete target directory */
    //return rmdir(name);
}


/********************
* Socket Functions *
********************/

U16 MSL_SetSocketNonBlocking(SOCKET sckt)
{
    /* Set the socket for non-blocking I/O. */
    if(fcntl(sckt, F_SETFL, O_NONBLOCK) == -1)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
            "MSL_SetSocketNonBlocking: Error setting a socket to non-blocking.");
        MSL_ScktClose(sckt);
        return RXN_FAIL;
    }
    return RXN_SUCCESS;
}


/* Not currently used */
#if 0
U16 MSL_ProcessSeedFile(const CHAR file[RXN_MSL_MAX_PATH])
{
    U08 buffer[MSL_SKT_RESP_MSG_MAX_LEN];
    U16 seedSize = 0;
    U16 result;

    if(MSL_OpenFile(MSL_PGPS_SEED_FILE_RSRC_ID, file, "r") != RXN_SUCCESS)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
            "MSL_ProcessSeedFile: Unable to read file.");
        return RXN_FAIL;
    }
    /* Determine file size */
    fseek(mFile[MSL_PGPS_SEED_FILE_RSRC_ID], 0, SEEK_END);
    seedSize = ftell(mFile[MSL_PGPS_SEED_FILE_RSRC_ID]);
    rewind(mFile[MSL_PGPS_SEED_FILE_RSRC_ID]);

    MSL_ReadFileBytes(MSL_PGPS_SEED_FILE_RSRC_ID, &buffer[0], seedSize);
    MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
        "MSL_ProcessSeedFile: Read in seed file of size %1.0f.", seedSize);
    result = MSL_ProcessPGPSServerRespMsg((CHAR *)buffer, seedSize);
    return RXN_SUCCESS;
}
#endif

/******************
* Time Functions *
******************/

void MSL_GetLogTime(MSL_time_t* pTime, const BOOL GMT)
{
    time_t tTime;
    struct tm* pSysTime;              /* Will store the current system time (UTC). */
    struct timezone curTimeZone;      /* Un-used, but req'd for fcn. */
    struct timeval curTimeSinceEpoch; /* Used to get sub-sec values. */

    /* Get the system time. */
    time(&tTime);

    /* Get the local or GMT time. */
    if(GMT == TRUE)
    {
        pSysTime = gmtime(&tTime);
    }
    else
    {
        pSysTime = localtime(&tTime);
    }

    /* Map the SysTime to pTime. */
    pTime->MSL_Year = (U16) (pSysTime->tm_year) + 1900;
    pTime->MSL_Month = (U08) (pSysTime->tm_mon + 1);
    pTime->MSL_Day = (U08) pSysTime->tm_mday;
    pTime->MSL_Hour = (U08) pSysTime->tm_hour;
    pTime->MSL_Min = (U08) pSysTime->tm_min;
    pTime->MSL_Sec = (U08) pSysTime->tm_sec;

    /* Get a timeval for access to mSec. */
    gettimeofday(&curTimeSinceEpoch, &curTimeZone);
    pTime->MSL_mSec = (U16) (curTimeSinceEpoch.tv_usec / 1000);

    return;
}

U32 MSL_GetSystemTime(void)
{
    time_t tTime;
    time(&tTime);
    return MSL_ConvertUTCToGPSTime(tTime);
}

U64 MSL_GetTickMilliSecond(void)
{
    MSL_EnterBlock(RXN_MSL_CS_GetTick);
    static U64 curTC = 0;

#ifdef ANDROID
    U64 ret = elapsedRealtime();

    if (ret > 0)
    {
        curTC = ret;
    }
    else
#endif
    {
        /* Consider using clock()/CLOCKS_PER_SEC if code below is
        * problematic for multiple targets. */
        struct timezone curTimeZone;      /* Un-used, but req'd for fcn. */
        static BOOL firstCall = TRUE;
        static struct timeval storedTimeSinceEpoch;
        struct timeval curTimeSinceEpoch;
        U32 elapsedTC = 0;

        /* If this is the first call, init unixTimeAtStart. */
        if(firstCall == TRUE)
        {
            /* Clear firstCall to avoid subsequent initialization. */
            firstCall = FALSE;

            /* Init curTimeSinceEpoch. */
            gettimeofday(&storedTimeSinceEpoch, &curTimeZone);

            MSL_LeaveBlock(RXN_MSL_CS_GetTick);
            return curTC;
        }

        /* Get current timeSinceEpoch. */
        gettimeofday(&curTimeSinceEpoch, &curTimeZone);

        /* Calc the elapsedTC - seconds component (if any). */
        elapsedTC = ((curTimeSinceEpoch.tv_sec - storedTimeSinceEpoch.tv_sec) * 1000);

        /* Calc the elapsedTC - mSec component.
        * Use the S32 data type cast to support calculating a negative
        * change to elapsedTC when just rolling past the start of a second value
        * (e.g. sec component increased by 1, but mSec component lower than prev) */
        elapsedTC = (U32) ((S32) elapsedTC +
            ((S32) (curTimeSinceEpoch.tv_usec / 1000) - (S32) (storedTimeSinceEpoch.tv_usec / 1000)));

        /* Update storedTimeSinceEpoch. */
        gettimeofday(&storedTimeSinceEpoch, &curTimeZone);

        /* Check for rollover and handle accordingly. */
        if((0xFFFFFFFF - curTC) < elapsedTC)
        {
            curTC += elapsedTC - (0xFFFFFFFF - curTC);
        }
        else /* No rollover */
        {
            curTC += elapsedTC;
        }
    }

    U64 retTime = curTC;
    MSL_LeaveBlock(RXN_MSL_CS_GetTick);
    return retTime;
}

void MSL_Sleep(U32 sleepMSec)
{
    usleep(sleepMSec*1000);

    return;
}

void MSL_CreateSNTPRequest(U08* ReqPkt)
{
    time_t sTime;
    U32 ReqTransTimeInteger;

    /* Get the system time since the Epoch (00:00:00 UTC, January 1, 1970) in seconds. */
    time(&sTime);

    /* Get the current system time since 1900. Setup a U32 to contain the TransmitTimestamp (integer) value. */
    ReqTransTimeInteger = sTime + MSL_OFFSET_1900_TO_1970;

    /* Setup a BYTE array to contain request packet data. */
    memset((void*) ReqPkt, 0, 48);

    /* Set the LI to 0 (00), VN to 3 (011) and Mode to 3 (011) or (00011011 = 0x1b). */
    ReqPkt[0] = 0x1b;

    /* Set the integer value within the request packet. */
    ReqPkt[40] = (ReqTransTimeInteger & 0xFF000000) >> 24; // MSByte
    ReqPkt[41] = (ReqTransTimeInteger & 0x00FF0000) >> 16;
    ReqPkt[42] = (ReqTransTimeInteger & 0x0000FF00) >> 8;
    ReqPkt[43] = ReqTransTimeInteger & 0x000000FF; // LSByte
}

U32 MSL_DecodeSNTPResponse(U08* RespPkt)
{
    U32 SvrTimeInteger;

    /* Get the integer part of the response (transmit time from svr). */
    SvrTimeInteger = 0;
    SvrTimeInteger |= ((U32) RespPkt[40]) << 24;
    SvrTimeInteger |= ((U32) RespPkt[41]) << 16;
    SvrTimeInteger |= ((U32) RespPkt[42]) << 8;
    SvrTimeInteger |= ((U32) RespPkt[43]);

    return SvrTimeInteger - MSL_OFFSET_1900_TO_1970;
}

U16 MSL_CmdRequest(EVENT event)
{
    const char cmd[2] = {event, '\0'};
    if (write(mCmdPipe[1], cmd, 1) == 1)
    {
        return RXN_SUCCESS;
    } 
    else
    {
        return RXN_FAIL;
    }
}

void MSL_StopThread(THREAD thread)
{
    /* Block the SIGTERM signal so we don't kill the whole process */
    sigset_t newMask;
    sigemptyset(&newMask);
    sigaddset(&newMask, SIGTERM);
    pthread_sigmask(SIG_BLOCK, &newMask, NULL);

    pthread_kill(thread, SIGTERM);

    close(mCmdPipe[0]);
    close(mCmdPipe[1]);
}

THREAD MSL_StartThread(void* threadStartFunc)
{
    THREAD ret = 0;
    /* We block SIGPIPE while we spawn the thread. The new thread will
    * will inherit the current sigmask. Therefore all write() calls in the thread
    * which could fail will return EPIPE instead of throwing a SIGPIPE.
    */
    sigset_t newMask, oldMask;

    sigemptyset(&newMask);
    sigaddset(&newMask, SIGPIPE);
    if (sigprocmask(SIG_BLOCK, &newMask, &oldMask) != 0)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE03,
            "Failed to block SIGPIPE");
    }

    if(pthread_create(&ret, NULL, threadStartFunc, NULL) == 0)
    {
        /* TODO: We should wait for thread to open GCmdPipe before proceeding.
        */

    }
    else
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE03,
            "Failed to start thread");
        return 0;
    }
    /* In any case we can now restore the original sigmask.
    */
    if (sigprocmask(SIG_SETMASK, &oldMask, NULL) != 0)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE03,
            "Failed to restore sigmask");
    }

    return ret;
}


/******************************
* Critical Section Functions *
******************************/

U16 MSL_InitBlocks(void)
{
    U08 x;

    for(x = 0; x < RXN_MSL_MAX_CS; x++)
    {
        pthread_mutex_init(&(mPRNCritSec[x]), NULL);
    }

    return RXN_SUCCESS;
}

void MSL_UninitBlocks(void)
{
    U08 x;

    for(x = 0; x < RXN_MSL_MAX_CS; x++)
    {
        pthread_mutex_destroy(&(mPRNCritSec[x]));
    }

    return;
}

BOOL MSL_TryEnterBlock(U08 CSIdx)
{
    int result;

    result = pthread_mutex_trylock(&(mPRNCritSec[CSIdx]));

    /* Only fail if we know for certain that the block is initialized and blocked. */
    if(result == EBUSY)
    {
        return FALSE;
    }
    else
    {
        return TRUE;
    }
}

void MSL_EnterBlock(U08 CSIdx)
{
    pthread_mutex_lock(&(mPRNCritSec[CSIdx]));
}

U16 MSL_LeaveBlock(U08 CSIdx)
{
    pthread_mutex_unlock(&(mPRNCritSec[CSIdx]));

    return RXN_SUCCESS;
}


S32 MSL_GetOSError()
{
    return errno;
}

const char* MSL_GetOSErrorString(S32 error)
{
    return strerror(error);
}

void MSL_SetSockaddr(struct sockaddr_in* addr, const char* ip, U16 port)
{
    if (addr)
    {
        memset(addr, 0, sizeof(addr));
        addr->sin_family = AF_INET;
        addr->sin_addr.s_addr = inet_addr(ip);
        addr->sin_port = htons(port);
    }
}

int MSL_GetCmdPipe()
{
    return mCmdPipe[0];
}

BOOL MSL_WaitForEvent(EVENT* event)
{
    int rc;
    fd_set readSet;
    int maxFds;
    struct timeval* timeout = NULL;

    assert(event);

    maxFds = MSL_SetFdSet(&readSet);

#ifdef ANDROID
    MSL_PreSelect();
#endif

    MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE09,
        "RXN_SocketMsgThread: Entering select maxFds: %1.0f.", maxFds);
    rc = select(maxFds, &readSet, NULL, NULL, timeout);

#ifdef ANDROID
    MSL_PostSelect(&readSet);
#endif

    if (rc < 0)
    {
        /* Major error. Bail. */
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE09,
            "RXN_SocketMsgThread: Select function failed.");
        return FALSE;
    }

    if (FD_ISSET(mCmdPipe[0], &readSet))
    {
        /* We received a command from the main thread. */            
        CHAR cmd;

        /* Read a char.*/
        if (read(mCmdPipe[0], &cmd, 1) == 1)
        {
            MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE09,
                "RXN_SocketMsgThread: Command: %1.0f received from main thread.", cmd);
            *event = cmd;
            return TRUE;
        }
    }

    return FALSE;
}

void MSL_CreateTimer()
{
    if (pipe(mWakePipe) != 0)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
            "MSL_SystemInitializeAction: Failed to create mWakePipe");
    }
    else
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
            "MSL_SystemInitializeAction: Successful in creating mWakePipe");
        MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
            "MSL_SystemInitializeAction: mWakePipe[0] = (%1.0f)", mWakePipe[0]);
        MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
            "MSL_SystemInitializeAction: mWakePipe[1] = (%1.0f)", mWakePipe[1]);

        /* Set pipe to be non-blocking */
        fcntl(mWakePipe[0], F_SETFL, O_NONBLOCK);
        fcntl(mWakePipe[1], F_SETFL, O_NONBLOCK);
    }
}

U16 MSL_ExitTimer()
{
    MSL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
        "MSL_WakeUpWork: write to mWakePipe");
    if ( mWakePipe[1] >= 0 ) {
        if (write(mWakePipe[1], "w", 1) == 1)
        {
            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
                "MSL_WakeUpWork: write to mWakePipe SUCCESS");
            return RXN_SUCCESS;
        }
        else
        {
            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
                "MSL_WakeUpWork: write to mWakePipe Failure");
            return RXN_FAIL;
        }
    }
    else {
        return RXN_FAIL;
    }
}

U16 MSL_WaitTimer(U32 timeoutSeconds)
{
    struct timeval timeout;
    S16 sResult = 0;
    S16 readReturn;
    static CHAR wakePulse[MSL_WAKE_LOCK_BUFFER_SIZE];
    fd_set wakeReadSet;

    if (mWakePipe[0] == -1 || mWakePipe[1] == -1 )
    {
        if(pipe(mWakePipe) != 0)
        {
            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
                "MSL_Timeout: Failed to create mWakePipe");
        }
        else
        {
            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
                "MSL_Timeout: Created mWakePipe");
        }
    }
    else
    {
        MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
            "MSL_Timeout: mWakePipe[0] = (%d)", mWakePipe[0]);
        MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
            "MSL_Timeout: mWakePipe[1] = (%d)", mWakePipe[1]);
    }

    /* Pre-emptively read in order to clear any previous data */
    read(mWakePipe[0], wakePulse, 1);

    FD_ZERO(&wakeReadSet);
    FD_SET(mWakePipe[0], &wakeReadSet);      /* Add mWakePipe to readSet. */

    timeout.tv_sec = timeoutSeconds;
    timeout.tv_usec = 0;
    MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08, "MSL_Timeout: Enter select for %1.0f seconds.", timeout.tv_sec);
#ifdef ANDROID
    MSL_ReleaseMainWakeLock();
#endif

    sResult = select(mWakePipe[0] + 1, &wakeReadSet, NULL, NULL, &timeout);

    if(sResult  < 0)
    {
        if(errno == EINTR)
        {
            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08, "MSL_Timeout: Signal received.");
        }
        else
        {
            MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08, "MSL_Timeout: Unknown select: %s.", strerror(errno));
        }
    }
    else if(sResult == 0)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08, "MSL_Timeout: Select timeout reached.");
    }
    else
    {
        if(FD_ISSET(mWakePipe[0], &wakeReadSet))
        {
            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08, "MSL_Timeout: mWakePipe caused select release...");

            readReturn = -1;
            readReturn = read(mWakePipe[0], wakePulse, MSL_WAKE_LOCK_BUFFER_SIZE);

            if (readReturn > 0)
            {
                MSL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08, "MSL_Timeout: Wake pulse received.");
            }
            else if (readReturn == 0)
            {
                MSL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08, "MSL_Timeout: EOF detected on mWakePipe.");
                MSL_CloseTimer();
            }
            else
            {
                MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08, "MSL_Timeout: read returns:%d", readReturn);
                MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08, "MSL_Timeout: NOP State. read failure error: %s.", strerror(errno));
            }
        }
        else
        {
            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08, "MSL_Timeout: Unknown fd caused select release...");
        }
    }
    return RXN_SUCCESS;
}

void MSL_CloseTimer()
{
    close(mWakePipe[0]);
    close(mWakePipe[1]);
}

void MSL_CreatePipe() 
{
    if (pipe(mCmdPipe) != 0)
    {
       MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE03,
           "MSL_CreatePipe: Failed to create mCmdPipe");
    }
}

#ifdef USE_NATIVE_SOCKET

void MSL_ThreadInit()
{
    MSL_CreatePipe();
}

void MSL_ThreadUninit()
{
}

void MSL_HandleSendFailure()
{
}

int MSL_GetReadCmdPipe()
{
    return mCmdPipe[0];
}

static int MSL_SetFdSet(fd_set* readSet)
{
    assert(readSet);
    FD_ZERO(readSet);
    FD_SET(mCmdPipe[0], readSet);      /* Add mCmdPipe to readSet. */
    return mCmdPipe[0] + 1;
}

BOOL MSL_CommandReceived(EVENT event)
{
    return TRUE;
}

/* Get MAC address of the first network adapter on the device
*/
U16 CmdRequestDeviceId(char* deviceIMEI)
{
    S32 sckt;
    struct ifconf ifc;
    struct ifreq *pIfr;
    struct ifreq ifr;
    CHAR buffer[1024];    
    S08 i;
    BOOL bFound = FALSE;

    if ((sckt = socket(AF_INET, SOCK_DGRAM, 0)) < 0)
    {
        MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
            "CmdRequestDeviceId: Error creating a socket. Errno: %s.", strerror(errno));
        return RXN_FAIL;
    }

    ifc.ifc_len = sizeof(buffer);
    ifc.ifc_buf = buffer;
    if (ioctl(sckt, SIOCGIFCONF, &ifc) < 0)
    {
        MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
            "CmdRequestDeviceId: ioctl error. Errno: %s.", strerror(errno));
        return RXN_FAIL;
    }

    pIfr = ifc.ifc_req;

    for (i = ifc.ifc_len / sizeof(struct ifreq); i >= 0; --i)
    {
        strcpy(ifr.ifr_name, pIfr->ifr_name);
        if (ioctl(sckt, SIOCGIFFLAGS, &ifr) == 0)
        {
            if (!(ifr.ifr_flags & IFF_LOOPBACK)) /* skip loopback */
            {
                if (ioctl(sckt, SIOCGIFHWADDR, &ifr) == 0)
                {
                    bFound = TRUE;
                    break;
                }
            }
        }
        pIfr++;
    }

    close(sckt);

    if (bFound)
    {
        for (i = 0; i < 5; i++)
            sprintf(deviceIMEI + i*3, "%.2hhX-", ifr.ifr_hwaddr.sa_data[i]);
        sprintf(deviceIMEI + i*3, "%.2hhX", ifr.ifr_hwaddr.sa_data[i]);
    }
    else
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE08,
            "CmdRequestDeviceId: No MAC address is found.");
        return RXN_FAIL;
    }

    return RXN_SUCCESS;
}
#endif

void RXN_MSL_SetPriority(enum RXN_MSL_Priority priority)
{
    /* Set the priority of the process */
    if (setpriority(PRIO_PROCESS, 0, priority) == -1)
    {
        /* Log, but otherwise ignore the error */
        MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_WARNING, RXN_LOG_ZONE08,
            "RXN_MSL_SetPriority: Error setting priority: %1.0f.", errno);
    }
}

time_t MSL_ConvertTimeStructToSeconds(MSL_time_t* pTime)
{
    struct tm tmTime;
    time_t tTime;
    char* tz;

    tmTime.tm_year = pTime->MSL_Year - 1900;
    tmTime.tm_mon = pTime->MSL_Month - 1;
    tmTime.tm_mday = pTime->MSL_Day;
    tmTime.tm_hour = pTime->MSL_Hour;
    tmTime.tm_min = pTime->MSL_Min;
    tmTime.tm_sec = pTime->MSL_Sec;
    tmTime.tm_isdst = 0;

    /* Change time zone to UTC */
    tz = getenv("TZ");      
    setenv("TZ", "", 1);    
    tzset();                
    
    tTime = mktime(&tmTime);

    /* Restore the value of TZ */
    if(tz)
        setenv("TZ", tz, 1);
    else
        unsetenv("TZ");    
    tzset();

    /* Will need to investigate this function as it seems to be off by 6 seconds */
    return (time_t)(tTime - 6);
}
