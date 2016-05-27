/*
* Copyright (c) 2012 Rx Networks, Inc. All rights reserved.
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
* $LastChangedDate: 2009-04-22 17:33:16 -0700 (Wed, 22 Apr 2009) $
* $Revision: 10292 $
*************************************************************************
*
*/

#include <pthread.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <linux/socket.h>
#include <linux/un.h>
#include <fcntl.h>

#include "RXN_MSL_Common.h"
#include "RXN_MSL_Platform.h"

/* needed for elapseRealtime function*/
#include <linux/ioctl.h>
#include <linux/rtc.h>
#include <linux/android_alarm.h>

/* prototypes */
void MSL_ManageConnection();

/* module variables */
#define MSL_WAKE_LOCK_NAME_LENGTH 30
#define MSL_COMMAND_TIMEOUT 5
static const char mListenerWakeLock[MSL_WAKE_LOCK_NAME_LENGTH] = "RXNET_MSL_LISTENER_WAKELOCK\0";
static const char mainWakeLock[MSL_WAKE_LOCK_NAME_LENGTH] = "RXNET_DRV_MSL_WAKELOCK\0";
static const char* mDomainPath = "RXN_MSL_PGPS";
static BOOL mMainWakeLockAcquired = FALSE;
static BOOL mListenerWakeLockAcquired = FALSE;
static uid_t mRXNServicesUid = 0;

static int mMsgSocket;
static int mRXNServicesSocket;
static CHAR mReadBuffer[MSL_SKT_RESP_MSG_MAX_LEN];

static MSL_SYNC mDeviceIdSync;

static BOOL mConnected;

/* See global definitions within RXN_MSL_Common.c. */
extern MSL_Config_t gConfig;

/* local prototypes */
static void AcquireListenerLock();
static void ReleaseListenerLock();
static BOOL AcquireWakeLock(const char* wl);
static BOOL ReleaseWakeLock(const char* wl);
static int authenticated_read(int socket, CHAR* buffer, int len);
static BOOL SetupRXNServicesSocket();

void MSL_DisconnectRXNServices();
void MSL_SyncCreate(MSL_SYNC* sync);
void MSL_SyncDestroy(MSL_SYNC* sync);
U08 MSL_SyncStart(MSL_SYNC* sync, U32 seconds);
void MSL_SyncEnd(MSL_SYNC* sync);

extern U08 MSL_GetConstelConfig();

void MSL_ThreadInit()
{
    MSL_CreatePipe();
    MSL_SyncCreate(&mDeviceIdSync);

    /* Start listening for connections from RXN Services */
    SetupRXNServicesSocket();
}

void MSL_ThreadUninit()
{
    MSL_SyncDestroy(&mDeviceIdSync);

    if (mConnected)
    {
        MSL_DisconnectRXNServices();

        int rc = shutdown(mMsgSocket, SHUT_RDWR);
        if (rc)
        {
            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_WARNING, RXN_LOG_ZONE04,"Native Socket-MSL_DisconnectRXNServices: Socket shutdown failed.");
        }
        close(mMsgSocket);
    }
}

static BOOL SetupRXNServicesSocket()
{
    struct sockaddr_un local;
    int len;

    mMsgSocket = socket(AF_UNIX, SOCK_STREAM, 0);
    if (mMsgSocket < 0)
    {
        MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
                "Could not create listening socket: %1.0f", errno);
        return FALSE;
    }

    local.sun_family = AF_UNIX;
    local.sun_path[0] = 0;
    strcpy(&local.sun_path[1], mDomainPath);
    len = strlen(mDomainPath) + sizeof(local.sun_family) + 1;

    int err = bind(mMsgSocket, (struct sockaddr*)&local, len);

    if (err < 0)
    {
        MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
                "Error binding RXN Services socket: %1.0f", errno);
        close(mMsgSocket);
        return FALSE;
    }

    err = listen(mMsgSocket, 1);

    if (err < 0)
    {
        MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
                "Error listening on RXN Services socket: %1.0f", errno);
        close(mMsgSocket);
        return FALSE;
    }

    return TRUE;
}

void MSL_DisconnectRXNServices()
{
    /* Indicate to the MSL data isn't available since RXN Services is no longer present, this will prevent MSL from falsely incrementing the download count */
    RXN_MSL_SetDataAccess(FALSE);

    mConnected = FALSE;
    if (mRXNServicesSocket > 0)
    {
        shutdown(mRXNServicesSocket, SHUT_RDWR);
        close(mRXNServicesSocket);
    }
    mRXNServicesSocket = 0;
}


void MSL_HandleSendFailure()
{
    /* We had a send failure. Tear down the socket.
    * Go into retry mode. Set the timeout value.
    */
    mConnected = FALSE;
    MSL_Log(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE09,
        "MSL_HandleSendFailure: Domain socket not connected, disconnect and retry.");
    MSL_DisconnectRXNServices();
}

BOOL MSL_CommandReceived(EVENT event)
{
    /* Only allow commands to be processed if we are connected. */
    return mConnected;
}


void MSL_PreSelect()
{
    /* Allows processor to go to sleep */
    ReleaseListenerLock();
}

static void ReadBytes(int sock)
{
    enum MSL_Message_Response_Type messageType;

    int bytesRead = 0;
    if (mRXNServicesUid == 0)
        bytesRead = read(sock, mReadBuffer, sizeof(mReadBuffer));
    else
        bytesRead = authenticated_read(sock, mReadBuffer, sizeof(mReadBuffer));


    if (bytesRead > 0)
    {
        MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE09,
                "RXN_SocketMsgThread: Received %1.0f bytes.", bytesRead);

        messageType = MSL_ParseXML(&mReadBuffer[0], bytesRead);

        /* If a DeviceId command was received, signal CmdRequestDeviceId() 
           that it can return. */
        if (messageType == MSL_DEVICE_ID)
        {

            MSL_SyncEnd(&mDeviceIdSync);
        }
    }
    else if (bytesRead < 0)
    {
        MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE09,
                "RXN_SocketMsgThread: recv() failed, errno: %1.0f", errno);
    }
    else // if (bytesRead == 0)
    {
        /* Server has performed an orderly shutdown. */
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE09,
                "RXN_SocketMsgThread: Server has performed an orderly shutdown.");

        /* Tear down the socket.
         * Go into retry mode. Set timeout value.
         */
        MSL_DisconnectRXNServices();
    }
}

void MSL_PostSelect(const fd_set* readSet)
{
    /* Guarantees the process is running */
    AcquireListenerLock();

    /* If we've received something from RXN Services, process it. */
    if (FD_ISSET(mMsgSocket, readSet))
    {
        if (!mConnected)
        {
            struct sockaddr remote;
            int len;
            mRXNServicesSocket = accept(mMsgSocket, &remote, &len);

            if (mRXNServicesSocket < 0)
            {
                MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE09,
                        "Error accepting connection from RXN Services: %1.0f", errno);
                return;
            }

            MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE09,
                    "Accepted connection from RXN Services");

            mConnected = TRUE;
        }
    }

    if (mConnected && FD_ISSET(mRXNServicesSocket, readSet))
    {
        ReadBytes(mRXNServicesSocket);
    }
}

int MSL_SetFdSet(fd_set* readSet)
{
    extern int MSL_GetCmdPipe();
    int maxFds;
    int cmdPipe = MSL_GetCmdPipe();

    FD_ZERO(readSet);
    FD_SET(cmdPipe, readSet);      /* Add mCmdPipe to readSet. */
    maxFds = cmdPipe + 1;

    if (!mConnected && mMsgSocket > 0)
    {
        FD_SET(mMsgSocket, readSet);       /* Add socket to readSet. */
        if (mMsgSocket > maxFds)
        {
            maxFds = mMsgSocket + 1;
        }
    }

    if (mConnected && mRXNServicesSocket > 0)
    {
        FD_SET(mRXNServicesSocket, readSet);
        if (mRXNServicesSocket > maxFds)
        {
            maxFds = mRXNServicesSocket + 1;
        }
    }

    return maxFds;
}

static int authenticated_read(int socket, CHAR* buffer, int len)
{
    char control[1024];
    struct msghdr msg;
    struct cmsghdr* cmsg;
    struct iovec iov;
    struct ucred cred;

    memset(&msg, 0, sizeof(msg));
    iov.iov_base = buffer;
    iov.iov_len = len;
    msg.msg_iov = &iov;
    msg.msg_iovlen = 1;
    msg.msg_control = control;
    msg.msg_controllen = sizeof(control);

    if (recvmsg(socket, &msg, 0) < 0)
    {
        MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE03, 
                "Error receiving message from RXN Services: %s", strerror(errno)); 
        return 0;
    }

    /* Loop over all control messages */
    cmsg = CMSG_FIRSTHDR(&msg);
    while (cmsg != NULL) 
    {
        if (cmsg->cmsg_level == SOL_SOCKET
                && cmsg->cmsg_type  == SCM_CREDENTIALS)
        {
            memcpy(&cred, CMSG_DATA(cmsg), sizeof(cred));
            MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE03, "pid: %1.0f", cred.pid);
            MSL_LogFltFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE03, 
                    "uid: %1.0f, gid: %1.0f", cred.uid, cred.gid);

            if (cred.uid != mRXNServicesUid)
            {
                MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE03,
                        "Received message from unauthorized uid: %1.0f.  Closing connection.", cred.uid);
                MSL_DisconnectRXNServices();
                return 0;
            }

            return iov.iov_len;
        }
        cmsg = CMSG_NXTHDR(&msg, cmsg);
    }
    return 0;
}

BOOL MSL_SendBytes(U08* pBytes, int bytesToSend)
{
    int bytesSent = 0;

    while (bytesToSend)
    {
        bytesSent = send(mRXNServicesSocket, pBytes + bytesSent, bytesToSend, 0);
        if (bytesSent < 0)
        {
            /* Probably EPIPE. */
            return FALSE;
        }
        else if (bytesSent == 0)
        {
            /* Peer shutdown has occurred. */
            return FALSE;
        }
        else
        {
            bytesToSend -= bytesSent;
        }
    }

    return TRUE;
}

BOOL MSL_SendAndroidSNTPTimeRequest()
{
    char xmlReqMsg[MSL_SKT_REQ_MSG_MAX_LEN];
    char sPort[5];

    memset(xmlReqMsg, 0, sizeof(xmlReqMsg));

    strcpy(xmlReqMsg, "<RXN_Command>\n");
    strcat(xmlReqMsg, "<name>Get_SNTP_Time</name>\n");
    strcat(xmlReqMsg, "<data>\n");

    /*<host> element */
    strcat(xmlReqMsg, "<host>");
    strcat(xmlReqMsg, gConfig.SNTP.host);
    strcat(xmlReqMsg, "</host>\n");

    /*<port> element */
    strcat(xmlReqMsg, "<port>");
    sprintf(sPort, "%d",gConfig.SNTP.port);
    strcat(xmlReqMsg, sPort);
    strcat(xmlReqMsg, "</port>\n");

    strcat(xmlReqMsg, "</data>\n");
    strcat(xmlReqMsg, "</RXN_Command>");
    /* Add numerical values to string */

    printf("SendSNTPTimeRequest: XML request: %s\n", xmlReqMsg);
    MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE04,
        "SendSNTPTimeRequest: XML request: %s", (char*)xmlReqMsg);

    /* Send command to RXN message socket */
    return MSL_SendBytes((U08*)xmlReqMsg, sizeof(xmlReqMsg));
}

BOOL MSL_SendAndroidUpdateSeedOrClkRequest(U08 type)
{
    CHAR fullHost[MSL_MAX_HOST_STR_LEN];      /* Full host string - including index. */
    CHAR IP[MSL_SKT_ADDR_MAX_LEN];            /* IP Address string. */
    CHAR reqMsg[MSL_SKT_REQ_MSG_MAX_LEN];      /* Request msg store. */
    U16 reqMsgLen;                            /* Len of the request msg. */
    CHAR xmlReqMsg[MSL_SKT_REQ_MSG_MAX_LEN * 2];
    CHAR sPort[5];

    MSL_CreateHostName(fullHost, gConfig.net.host, gConfig.net.hostMaxIdx);

    /* Generate a request msg. Note: IP is ignored. */
    MSL_GenReqMsg(type, IP, reqMsg, &reqMsgLen, MSL_GetConstelConfig());
    memset(xmlReqMsg, 0, sizeof(xmlReqMsg));

    //strcpy(xmlReqMsg, "<?xml version=\"1.0\"?>\n");
    strcpy(xmlReqMsg, "<RXN_Command>\n");
    strcat(xmlReqMsg, "<name>Get_PGPS_Seed</name>\n");
    strcat(xmlReqMsg, "<data>\n");

    /*<host> element */
    strcat(xmlReqMsg, "<host>");
    strcat(xmlReqMsg, fullHost);
    strcat(xmlReqMsg, "</host>\n");

    /*<port> element */
    strcat(xmlReqMsg, "<port>");
    sprintf(sPort, "%d",MSL_GetHostPort());
    strcat(xmlReqMsg, sPort);
    strcat(xmlReqMsg, "</port>\n");


    /*<port> element */
    strcat(xmlReqMsg, "<request><![CDATA[");
    strcat(xmlReqMsg, (char*)reqMsg);
    strcat(xmlReqMsg, "]]></request>\n");

    strcat(xmlReqMsg, "</data>\n");
    strcat(xmlReqMsg, "</RXN_Command>");
    /* Add numerical values to string */

    MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE04,
        "SendUpdateSeedOrClkRequest: http request: %s", xmlReqMsg);
    /* Send command to RXN message socket */
    return MSL_SendBytes((U08*)xmlReqMsg, sizeof(xmlReqMsg));
}

BOOL MSL_SendDeviceIdRequest()
{
    char xmlReqMsg[MSL_SKT_REQ_MSG_MAX_LEN];

    memset(xmlReqMsg, 0, sizeof(xmlReqMsg));

    strcpy(xmlReqMsg, "<RXN_Command>\n");
    strcat(xmlReqMsg, "<name>Get_Device_ID</name>\n");
    strcat(xmlReqMsg, "</RXN_Command>\n");
    /* Add numerical values to string */

    MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE04,
        "SendDeviceIdRequest: XML request: %s", (char*)xmlReqMsg);

    /* Send command to RXN message socket */
    return MSL_SendBytes((U08*)xmlReqMsg, sizeof(xmlReqMsg));
}

BOOL MSL_SendTerminationRequest()
{
    char xmlReqMsg[MSL_SKT_REQ_MSG_MAX_LEN];

    memset(xmlReqMsg, 0, sizeof(xmlReqMsg));

    strcpy(xmlReqMsg, "<RXN_Command>\n");
    strcat(xmlReqMsg, "<name>Terminate_RXNServices</name>\n");
    strcat(xmlReqMsg, "</RXN_Command>\n");
    /* Add numerical values to string */

    MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE04,
        "MSL_SendTerminationRequest: XML request: %s", (char*)xmlReqMsg);

    /* Send command to RXN message socket */
    return MSL_SendBytes((U08*)xmlReqMsg, sizeof(xmlReqMsg));
}

U16 CmdRequestTermination()
{
#ifdef XYBRID
    return RXN_SUCCESS;
#else
    /* We don't want the RXN Service to shutdown since our file watcher
     * is running in it */
    return RXN_SUCCESS;
#endif
}

/* Called by the main thread to tell MsgSocketThread to fetch device id.
*/
U16 CmdRequestDeviceId(char* deviceId)
{
    if (MSL_CmdRequest(MSL_SEND_DEVICE_ID_REQUEST_EVENT) == RXN_SUCCESS)
    {
        return MSL_SyncStart(&mDeviceIdSync, MSL_COMMAND_TIMEOUT);
    }
    else
    {
        return RXN_FAIL;
    }
}

static BOOL AcquireWakeLock(const char* wl)
{
    S16 error;
    S16 wakelockfd = open("/sys/power/wake_lock", O_RDWR);

    if (wakelockfd < 0)
        return FALSE;

    error = write(wakelockfd, wl, (S16)strlen(wl));
    close(wakelockfd);

    if (error > 0)
    {
        MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE03,
            "MSL_AcquireWakeLock: WakeLock acquired for %s", (CHAR *)wl);
        return TRUE;
    }
    else
    {
        MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_WARNING, RXN_LOG_ZONE03,
            "MSL_AcquireWakeLock: WakeLock cannot be acquired for %s", (CHAR *)wl);
    }
    return FALSE;
}

void MSL_AcquireMainWakeLock()
{
    if(mMainWakeLockAcquired == FALSE)
    {
        mMainWakeLockAcquired = AcquireWakeLock(mainWakeLock);
    }
}

static void AcquireListenerLock()
{
    if(mListenerWakeLockAcquired == FALSE)
    {
        mListenerWakeLockAcquired = AcquireWakeLock(mListenerWakeLock);
    }
}

static BOOL ReleaseWakeLock(const char* wl)
{
    S16 error;
    S16 wakeunlockfd = open("/sys/power/wake_unlock", O_RDWR);

    if (wakeunlockfd < 0)
        return FALSE;

    error = write(wakeunlockfd, wl, (S16)strlen(wl));
    close(wakeunlockfd);

    if (error > 0)
    {
        MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE03,
            "MSL_ReleaseWakeLock: WakeLock released for %s", (CHAR *)wl);
        return TRUE;
    }
    else
    {
        MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_WARNING, RXN_LOG_ZONE03,
            "MSL_ReleaseWakeLock: WakeLock cannot be released for %s", (CHAR *)wl);
    }
    return FALSE;
}

void MSL_ReleaseMainWakeLock()
{
    if(mMainWakeLockAcquired == TRUE)
    {
        mMainWakeLockAcquired = !ReleaseWakeLock(mainWakeLock);
    }
}

static void ReleaseListenerLock()
{
    if(mListenerWakeLockAcquired == TRUE)
    {
        mListenerWakeLockAcquired = !ReleaseWakeLock(mListenerWakeLock);
    }
}

void MSL_SyncCreate(MSL_SYNC* sync)
{
    if (sync)
    {
        pthread_mutex_init(&sync->mutex, NULL);
        pthread_cond_init(&sync->cond, NULL);
    }
}

void MSL_SyncDestroy(MSL_SYNC* sync)
{
    if (sync)
    {
        pthread_mutex_destroy(&sync->mutex);
        pthread_cond_destroy(&sync->cond);
    }
}

U08 MSL_SyncStart(MSL_SYNC* sync, U32 seconds)
{
    struct timeval now;
    struct timespec timeout;
    int retcode = 0;

    if (!sync) {
        return RXN_FAIL;
    }

    pthread_mutex_lock( &sync->mutex );
    gettimeofday(&now, NULL);
    timeout.tv_sec = now.tv_sec + seconds;
    timeout.tv_nsec = now.tv_usec * 1000;

    retcode = pthread_cond_timedwait(&sync->cond, &sync->mutex, &timeout);

    pthread_mutex_unlock( &sync->mutex );

    if (retcode == 0) 
        return RXN_SUCCESS;
    else /* Timeout or other error occured, RXN Services is probably down */ 
    {
        MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
            "MSL_SyncStart: pthread_cond_timedwait error: %1.0f.", (double)retcode);
        return RXN_FAIL;
    }
}

void MSL_SyncEnd(MSL_SYNC* sync)
{
    if (sync) {
        pthread_mutex_lock( &sync->mutex );
        pthread_cond_signal( &sync->cond );
        pthread_mutex_unlock( &sync->mutex );
    }
}

/* Based on elapsedRealtime(). Typically it is up to platform
 * designer to make this function externally available in libutils.so or
 * other system libraries. For the production device we have in house running
 *  Android OS 2.3.5, no system shared libraries export this function. */
int64_t elapsedRealtime()
{
    static int s_fd = -1;
    struct timespec ts;
    int fd, result;

    if(s_fd == -1)
    {
        fd = open("/dev/alarm", O_RDONLY);
        if (fd < 0)
        {
            s_fd = -1;
            return 0;
        }
        else
        {
            s_fd = fd;
        }
    }
    result = ioctl(s_fd, ANDROID_ALARM_GET_TIME(ANDROID_ALARM_ELAPSED_REALTIME), &ts);

    if (result == 0)
    {
        return ts.tv_sec * 1000 + ts.tv_nsec / 1000000;
    }
    return 0;
}
