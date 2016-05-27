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

#include <assert.h>

#ifdef LINUX
#include <resolv.h>
#endif

#include "RXN_MSL_Common.h"
#include "RXN_MSL_Platform.h"

/********************************************************
* Static vars used to support resources such as files, *
* sockets, etc.                                        *
********************************************************/

#define CONNECT_TIMEOUT 10	/* value in seconds */

/* Files supporting RxN storage, logging.
* gFile[] elements will be accessed using IDs
* specified within MSL_FileResourceIDs as indices
* (see RXN_MSL_Common.h). */
#define MSL_MAX_FILES MSL_FILE_RSRC_MAX + 2 * (RXN_CONSTANT_NUM_GPS_PRNS + RXN_CONSTANT_NUM_GLONASS_PRNS)
static FILE* mFile[MSL_MAX_FILES];

/* Flag indicating that the file is at capacity. */
static BOOL mFileAtCap = FALSE;

/* Open a file for subsequent I/O.
* For windows - use standard lib support and
* fileConfig values of "r", "w", "a" "r+", "w+" and "a+" supported. Directly used where
* standard file I/O support is available (e.g. Win32). Must be translated on platforms
* where no standard file I/O support is available. */
U16 MSL_OpenFile(U08 rsrcID, const CHAR file[RXN_MSL_MAX_PATH], CHAR fileConfig[MSL_MAX_FILE_CONFIG_STR_LEN])
{
    char fullConfig[MSL_MAX_FILE_CONFIG_STR_LEN]; /* Full config string. */

    memset((void *)fullConfig, 0, sizeof(char) * MSL_MAX_FILE_CONFIG_STR_LEN);

    if (rsrcID >= MSL_MAX_FILES)
    {
        return MSL_FILE_OPENCLOSE_ERROR;
    }

    /* Append 'c' to the end of fileConfig to support flushing
    * (Microsoft requirement only). */
    snprintf(fullConfig, MSL_MAX_FILE_CONFIG_STR_LEN, "%sc", fileConfig);

    /* Open the file. */
    mFile[rsrcID] = fopen(file, fullConfig);
    if(mFile[rsrcID] == NULL)
    {
        return MSL_FILE_OPENCLOSE_ERROR;
    }

    /* If opening a log file - reset mFileAtCap. */
    if(rsrcID == MSL_LOG_FILE_RSRC_ID)
    {
        mFileAtCap = FALSE;
    }

    return RXN_SUCCESS;
}

U16 MSL_CloseFile(U08 rsrcID)
{
    U16 result = RXN_SUCCESS; /* Result store. */

    if (rsrcID >= MSL_MAX_FILES)
    {
        return MSL_FILE_OPENCLOSE_ERROR;
    }

    /* Don't bother doing anything if mFile is NULL. */
    if(mFile[rsrcID] == NULL)
    {
        /* Already closed. */
        return result;
    }

    /* Close the file and handle an error.*/
    if(fclose(mFile[rsrcID]) != 0)
    {
        result = MSL_FILE_OPENCLOSE_ERROR;
    }

    /* Reset mFile. */
    mFile[rsrcID] = NULL;

    return result;
}

U16 MSL_ReadFileLine(U08 rsrcID, U08* readBuf, const U16 readBufSize)
{
    /* Don't do anything if the file has not been opened. */
    if(mFile[rsrcID] == NULL)
    {
        return MSL_FILE_OPENCLOSE_ERROR;
    }

    /* Read the string. */
    if(fgets((char*)readBuf, readBufSize, mFile[rsrcID]) == NULL)
    {
        /* Check for EOF. */
        if(feof(mFile[rsrcID]) != 0)
        {
            /* Clear the EOF flag */
            clearerr(mFile[rsrcID]);

            /* Got to the end of file. */
            return MSL_FILE_EOF_ERROR;
        }
        else
        {
            /* Some other error. */
            return MSL_FILE_IO_ERROR;
        }
    }

    return RXN_SUCCESS;
}

U16 MSL_WriteFileLine(U08 rsrcID, const U08* writeBuf, U32 maxFilesize)
{
    int result = 0;     /* Result store. */
    long fileLength = 0; /* Current file length. */

    /* Don't do anything if the file has not been opened. */
    if(mFile[rsrcID] == NULL)
    {
        return MSL_FILE_OPENCLOSE_ERROR;
    }

    /* Don't do anything if the file is a log file and has
    * already been deemed to be at capacity. */
    if( (rsrcID == MSL_LOG_FILE_RSRC_ID) && 
        (mFileAtCap == TRUE) )
    {
        return MSL_FILE_OVER_CAP_ERROR;
    }

    /* If trying to write to a log file, check if the file
    * is at capacity. */
    if(rsrcID == MSL_LOG_FILE_RSRC_ID)
    {
        /* Get the file length. On Windows it is necessary to seek to the end of the
           file because if a file is opened in append mode, ftell() returns 0 when no
           write has been performed yet. */
        fseek(mFile[rsrcID], 0, SEEK_END);
        fileLength = ftell(mFile[rsrcID]);

        /* Check if greater than maxFilesize (spec'd in kB). */
        if(fileLength > (long) (maxFilesize * 1024))
        {
            /* Provide a warning on the last line. */
            fputs("FILE AT CAPACITY - NO ADDITIONAL LOGGING AFTER THIS ENTRY!", mFile[rsrcID]);

            /* Set mFileAtCap to avoid future logging. */
            mFileAtCap = TRUE;

            return MSL_FILE_OVER_CAP_ERROR;
        }
    }

    /* Write the string. */
    result = fputs((char*)writeBuf, mFile[rsrcID]);

    /* Check for errors. 
    * If successful, the fputs() function returns a non-negative number on success. 
    * If an error occurs, it returns EOF 
    */
    if(result == EOF)
    {
        return MSL_FILE_EOF_ERROR;
    }
    else
    {
        return RXN_SUCCESS;
    }
}

U16 MSL_ReadFileBytes(U08 rsrcID, U08* readBuf, const U16 bytesToRead)
{
    /* Don't do anything if the file has not been opened. */
    if(mFile[rsrcID] == NULL)
    {
        return MSL_FILE_OPENCLOSE_ERROR;
    }

    /* Read individual bytes from the file. */
    if(fread(readBuf, 1, bytesToRead, mFile[rsrcID]) != bytesToRead)
    {
        if(feof(mFile[rsrcID]) != 0)
        {
            return MSL_FILE_EOF_ERROR;
        }
        else
        {
            return MSL_FILE_IO_ERROR;
        }
    }

    return RXN_SUCCESS;
}

U16 MSL_WriteFileBytes(U08 rsrcID, const U08* writeBuf, const U16 bytesToWrite)
{
    /* Don't do anything if the file has not been opened. */
    if(mFile[rsrcID] == NULL)
    {
        return MSL_FILE_OPENCLOSE_ERROR;
    }

    /* Write individual bytes to the file. */
    if(fwrite(writeBuf, 1, bytesToWrite, mFile[rsrcID]) != bytesToWrite)
    {
        return MSL_FILE_IO_ERROR;
    }

    return RXN_SUCCESS;
}

U16 MSL_SetFilePtrOffset(U08 rsrcID, U32 offset)
{
    /* Don't do anything if the file has not been opened. */
    if(mFile[rsrcID] == NULL)
    {
        return MSL_FILE_OPENCLOSE_ERROR;
    }

    /* Seek to a position given an offset. */
    fseek(mFile[rsrcID], offset, SEEK_SET);

    return RXN_SUCCESS;
}

U16 MSL_FileFlush(U08 rsrcID)
{
    /* Don't do anything if the file has not been opened. */
    if(mFile[rsrcID] == NULL)
    {
        return MSL_FILE_OPENCLOSE_ERROR;
    }

    /* Flush buffer contents to the file. */
    if(fflush(mFile[rsrcID]) != 0)
    {
        return RXN_FAIL;
    }

    return RXN_SUCCESS;
}

U32 MSL_GetTickSecond(void)
{
    U32 ret = (U32)((MSL_GetTickMilliSecond() + 500 ) / 1000);
    return ret;
}


/********************
* Socket Functions *
********************/

U16 MSL_ScktGetHostIP(const char Host[MSL_MAX_HOST_STR_LEN], char IPAdd[MSL_SKT_ADDR_MAX_LEN])
{
    struct hostent *hostData = NULL;  /* gethostbyname() result store. */

#ifdef LINUX
    /* Restart the resolver system to flush any old DNS entries */
    res_init();
#endif

    /* Get hostent using the Host string. */
    hostData = gethostbyname(Host);

    /* Handle errors. */
    if(hostData == NULL)
    {
        /* Clear the returned IP address */
        IPAdd[0] = 0;

        MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
            "MSL_ScktGetHostIP: Error resolving an IP address from host name: %s", Host);

        return MSL_SKT_HOST_RESOLVE_ERROR;
    }

    /* Setup the IPAdd string. */
    snprintf(IPAdd, MSL_SKT_ADDR_MAX_LEN, "%d.%d.%d.%d",
        (U08) (hostData->h_addr_list[0][0]),  /* 1st IP addr element. */
        (U08) (hostData->h_addr_list[0][1]),  /* 2nd IP addr element. */
        (U08) (hostData->h_addr_list[0][2]),  /* 3rd IP addr element. */
        (U08) (hostData->h_addr_list[0][3])   /* 4th IP addr element. */
        );

    return RXN_SUCCESS;
}

U16 MSL_ScktOpenUDP(const char* host, U16 port, SOCKET* pUDPSocket) 
{
    CHAR IP[MSL_SKT_ADDR_MAX_LEN];
    struct sockaddr_in Addr;

    assert(pUDPSocket);

    if (*pUDPSocket != 0)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04, "Socket already open");
        return MSL_SKT_CREATE_ERROR; 
    }

    if (MSL_ScktGetHostIP(host, IP) != RXN_SUCCESS)
    {
        return MSL_SKT_HOST_RESOLVE_ERROR;
    }

    /* Create a socket with UDP support. */
    *pUDPSocket = socket(AF_INET, SOCK_DGRAM, 0);
    if (*pUDPSocket == INVALID_SOCKET)
    {
        MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
            "MSL_ScktOpenUDP: Error creating a socket. Error: %s.", MSL_GetOSErrorString(MSL_GetOSError()));
        return MSL_SKT_CREATE_ERROR;
    }

    /* Setup server socket address data. SNTP uses port 123 */
    MSL_SetSockaddr(&Addr, IP, port);

    /* Connect the socket. */
    if (connect(*pUDPSocket, (struct sockaddr*) & Addr, sizeof (struct sockaddr_in)) == SOCKET_ERROR)
    {
        MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
            "MSL_OpenScktTCP: Error connecting a socket. Errno: %s.", MSL_GetOSErrorString(MSL_GetOSError())); 
        MSL_ScktClose(*pUDPSocket);
        *pUDPSocket = 0;
        return MSL_SKT_CONNECT_ERROR;
    }

    return RXN_SUCCESS;
}

U16 MSL_ScktOpenTCP(char IPAdd[MSL_SKT_ADDR_MAX_LEN], U16 port, U32 readTO, SOCKET* pTCPSocket) 
{
    SOCKET Sckt;
    struct sockaddr_in Addr;
    struct timeval timeout;
    fd_set connect_set;

    assert(pTCPSocket);

    if (*pTCPSocket != 0)
    {
        MSL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04, "Socket already open");
        return MSL_SKT_CREATE_ERROR; 
    }

    /* Initialize for TCP I/O. */
    Sckt = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    if (Sckt == INVALID_SOCKET)
    {
        MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
            "MSL_OpenScktTCP: Error creating a socket. Error: %s.", MSL_GetOSErrorString(MSL_GetOSError()));

        return MSL_SKT_CREATE_ERROR;
    }

    /* Set socket non-blocking */
    if (MSL_SetSocketNonBlocking(Sckt) == RXN_FAIL)
    {
        return MSL_SKT_SETUP_ERROR;
    }

    /* Setup Addr. */
    MSL_SetSockaddr(&Addr, IPAdd, port);

    /* Connect the socket. */
    if (connect(Sckt, (struct sockaddr*) &Addr, sizeof (struct sockaddr_in)) == SOCKET_ERROR
        && MSL_GetOSError() != MSL_OS_INPROGRESS) 	
    {
        MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
            "MSL_OpenScktTCP: Error connecting a socket. Error: %s.", MSL_GetOSErrorString(MSL_GetOSError())); 
        MSL_ScktClose(Sckt);
        return MSL_SKT_CONNECT_ERROR;
    }

    timeout.tv_sec = CONNECT_TIMEOUT;
    timeout.tv_usec = 0;

    FD_ZERO(&connect_set);
    FD_SET((int)Sckt, &connect_set);

    /* If there is a timeout waiting for connect or some other error, return error */
    if (select((int)Sckt + 1, &connect_set, &connect_set, NULL, &timeout) <= 0)
    {
        MSL_ScktClose(Sckt);
        MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_WARNING, RXN_LOG_ZONE04,
            "MSL_OpenScktTCP: Connect timed out. Errno: %s.", strerror(errno));
        return MSL_SKT_CONNECT_ERROR;
    }

#if defined(WIN32) && !defined(WINCE)
    /* Set the socket read timeout in mSec (if != 0). WinXP support only.
    * Because socket reads will be non-blocking (see code below), the default timeout
    * in WinMo/WinCE should suffice. */
    if (MSL_SetSocketReadTimeout(Sckt, &readTO) == RXN_FAIL)
    {
        return MSL_SKT_CONNECT_ERROR;
    }
#endif

    /* Set pSckt. */
    *pTCPSocket = Sckt;

    return RXN_SUCCESS;

}

U16 MSL_ScktClose(SOCKET GenSocket)
{
    /* Close the socket. */
#if defined(WIN32) || defined (WINCE)
    if(closesocket(GenSocket) != SOCKET_ERROR)
#else
    if(close(GenSocket) != SOCKET_ERROR)
#endif
    {
        return RXN_SUCCESS;
    }
    else
    {
        MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
            "MSL_ScktClose: Error closing a socket. Error %s.", MSL_GetOSErrorString(MSL_GetOSError()));
        return RXN_FAIL;
    }
}

U16 MSL_ScktReadBytes(SOCKET GenSocket, U08* pBuf, U16 uBufSize, U32 msTO,
                      U32* puNumBytesRead)
{
    S32 sockReturn;   /* Return value store. */
    U64 tickAtTimeout;
    U32 bytesRecvd = 0;

    /* Calc the mSec tick count corresponding to a timeout. */
    tickAtTimeout =MSL_GetTickMilliSecond() + msTO;

    /* Loop through write retries. */
    while(bytesRecvd < uBufSize)
    {
        /* Read bytes (non-block). GenSocket must be bound (connection-less)
        * or connected. */
        sockReturn = recv(GenSocket, pBuf + bytesRecvd, uBufSize - bytesRecvd, 0);

        /* Handle status data and errors. */
        if(sockReturn == SOCKET_ERROR)
        {
            sockReturn = MSL_GetOSError();

            /* Look for a legit error. */
            if (sockReturn != MSL_OS_WOULDBLOCK)
            {
                MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
                    "MSL_ScktReadBytes: Error reading from a socket. Error %s", MSL_GetOSErrorString(sockReturn));
                return MSL_SKT_READ_ERROR;
            }
            else
            {
                /* Check if we already recieved bytes. If so, we are now done.
                * (Assume that as soon as there is a break in packets, the msg
                * is completly recieved.) */
                if(bytesRecvd > 0)
                {
                    break;
                }
            }
        } /* if(sockReturn == SOCKET_ERROR) */
        else
        {
            /* Set the number of bytes recieved. */
            bytesRecvd += sockReturn;

            /* Give other packets time to come in. We don't want
            * to read before remaining packets are in otherwise we
            * will assume that we have the complete msg. */
            MSL_Sleep(500);

            MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE04,
                "MSL_ScktReadBytes: Read %1.0f bytes.", (double) sockReturn);

            /* Successful read if we get no bytes after getting bytes. */
            if((bytesRecvd > 0) && (sockReturn == 0))
            {
                break;
            }
        }

        /* Check for a timeout. */
        if(MSL_GetTickMilliSecond() > tickAtTimeout)
        {
            MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
                "MSL_ScktReadBytes: Timeout (%1.0f mSec) reached.", (double) msTO);
            return MSL_SKT_TIMEOUT_ERROR;
        }

    } /* while(TRUE) */

    /* Set puNumBytesRead. */
    *puNumBytesRead = bytesRecvd;

    return RXN_SUCCESS;
}

U16 MSL_ScktWriteBytes(SOCKET GenSocket, U08* pBuf, U32 uNumBytesToWrite, U32 msTO,
                       CHAR IPAdd[MSL_SKT_ADDR_MAX_LEN], U16 port)
{
    S32 sockReturn;       /* Return value store. */
    struct sockaddr_in RecvAddr;
    U64 tickAtTimeout;

    /* Handle connectionless sockets (IPAdd not null). */
    if(IPAdd != NULL)
    {
        MSL_SetSockaddr(&RecvAddr, IPAdd, port);
    }

    /* Calc the mSec tick count corresponding to a timeout. */
    tickAtTimeout = MSL_GetTickMilliSecond() + msTO;

    /* Loop through write retries. */
    while(TRUE)
    {
        /* Try to write (non-blocked). Use send for connected sockets and sendto for
        * connection-less sockets (using the setup RecvAddr). */
        if(IPAdd == NULL)
        {
            sockReturn = send(GenSocket, pBuf, uNumBytesToWrite, 0);
        }
        else
        {
            sockReturn = sendto(GenSocket, pBuf, uNumBytesToWrite, 0,
                (struct sockaddr*)&RecvAddr, sizeof(struct sockaddr_in));
        }

        /* Handle status data and errors. */
        if(sockReturn == SOCKET_ERROR)
        {
            sockReturn = MSL_GetOSError();

            /* Look for a legit error. */
            if (sockReturn != MSL_OS_WOULDBLOCK)
            {
                MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
                    "MSL_ScktWriteBytes: Error writing to a socket. Error %s", MSL_GetOSErrorString(sockReturn));
                return MSL_SKT_WRITE_ERROR;
            }
        } /* if(sockReturn == SOCKET_ERROR) */
        else
        {
            /* Check that the proper number of bytes were written. */
            if(sockReturn != (S32)uNumBytesToWrite)
            {
                MSL_LogFltFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
                    "MSL_ScktWriteBytes: Error writing ALL bytes to a socket. Wrote %1.0f of %1.0f bytes",
                    (double) sockReturn, (double) uNumBytesToWrite);

                return MSL_SKT_WRITE_ERROR;
            }
            else
            {
                /* Successful write. */
                break;
            }
        }

        /* Check for a timeout. */
        if(MSL_GetTickMilliSecond() > tickAtTimeout)
        {
            MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_WARNING, RXN_LOG_ZONE04,
                "MSL_ScktWriteBytes: Timeout (%1.0f mSec) reached.", (double) msTO);

            return MSL_SKT_TIMEOUT_ERROR;
        }

    } /* while(TRUE) */

    return RXN_SUCCESS;
}

