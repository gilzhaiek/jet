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
 * $LastChangedDate: 2009-03-02 15:53:19 -0800 (Mon, 02 Mar 2009) $
 * $Revision: 9396 $
 *************************************************************************
 *
 * This file contains implementation of APIs exposed within the RXN_CL_Serial_Windows.h file. *
 */

#include "RXN_CL_Common.h"  /* Includes declarations for fcns within and common declarations. */
#include "RXN_CL_Linux.h"
#include <termios.h>
#include <fcntl.h>
#include <errno.h>
#include <stdarg.h>   /* Req'd for va_start and va_end. */
#include <sys/time.h>
#include <sys/types.h>        /* Required for socket support */
#include <sys/socket.h>       /* Required for socket support */
#include <netdb.h>            /* Required for socket support */
#include <netinet/in.h>       /* Required for socket support */
#include <signal.h>

/* Define the max log size in kB*/
#define CL_MAX_LOG_SIZE     1 /* 1kB */

#ifndef FNDELAY
#define FNDELAY O_NDELAY
#endif

/********************
 * Log Functions    *
 ********************/

U32 CL_GetMaxLogSize(void)
{
  return CL_MAX_LOG_SIZE;
}

/********************
 * Thread Functions *
 ********************/

void CL_Sleep(U32 mSecToSleep)
{
    usleep(mSecToSleep * 1000);  
}

/******************
 * Time Functions *
 ******************/

void CL_GetTime(CL_time_t* pTime, BOOL GMT)
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
  pTime->CL_Year = (U16) (pSysTime->tm_year) + 1900;
  pTime->CL_Month = (U08) (pSysTime->tm_mon + 1);
  pTime->CL_Day = (U08) pSysTime->tm_mday;
  pTime->CL_Hour = (U08) pSysTime->tm_hour;
  pTime->CL_Min = (U08) pSysTime->tm_min;
  pTime->CL_Sec = (U08) pSysTime->tm_sec;

  /* Get a timeval for access to mSec. */
  gettimeofday(&curTimeSinceEpoch, &curTimeZone);
  pTime->CL_mSec = (U16) (curTimeSinceEpoch.tv_usec / 1000);

  return;
}

U32 CL_GetGPSTime(void)
{
  time_t tTime;
  struct tm gpsTime;
  U32 gpsOffset = 15;
  /* Seconds between Jan 1 1970 & GPS Jan 7 1980 */
  U32 gpsTimeSystemOffset = 315964800;
  U32 result = 0;

  /* gps started 00:00 on 6 January 1980*/
  gpsTime.tm_sec = 0;
  gpsTime.tm_min = 0;
  gpsTime.tm_hour = 0;
  gpsTime.tm_mday = 6;
  gpsTime.tm_mon = 1;
  gpsTime.tm_year = 1980;
  gpsTime.tm_isdst = 0;

  time(&tTime);
  result = tTime - mktime(&gpsTime) - gpsTimeSystemOffset + gpsOffset;
  return result;
}

U32 CL_GetTickCount(void)
{
  /* Consider using clock()/CLOCKS_PER_SEC if code below is
   * problematic for multiple targets. */
    
  struct timezone curTimeZone;      /* Un-used, but req'd for fcn. */
  static BOOL firstCall = TRUE;
  static U32 curTC = 0;
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

  return curTC;
}

/********************
 * Serial Functions *
 ********************/

U16 CL_OpenPort(char config[RXN_CL_CONFIG_MAX_STR_LEN], CL_Chipset_Attrib_t* pAttrib)
{
  pAttrib->CL_FD = open( config,O_RDWR|O_NOCTTY|O_NDELAY);
  /* Check that the port could be opened. */
  if(pAttrib->CL_FD == -1)
  {
    /* Handle the error and return an error code. */
    CL_LogStr(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,"Error opening a port within CL_OpenPort. config: %s.", config);
    return RXN_CL_OPEN_OR_CLOSE_ERR;
  }
  fcntl(pAttrib->CL_FD,F_SETFL,FNDELAY);
  return RXN_SUCCESS;
}

U16 CL_SetupPort(CL_Chipset_Attrib_t* pAttrib, U32 uReadIntervalTimeout, U32 uReadTimeoutMult,
		U32 m_uReadTimeoutConstant, U32 uWriteTimeoutMult, U32 uWriteTimeoutConstant,
		U32 uBaudRate, U08 uByteSize, U08 uParity, U08 uStopBits)
{

  struct termios port_settings; /* struct used to store settings */
    /* Don't do anything if the port has not been properly opened. */
    if((pAttrib == 0) || (pAttrib->CL_FD == -1))
    {
        /* Handle the error and return error code. */
        CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04, "CL_SetupPort called before a port was properly opened. Handle: %1.0f",
                (double) pAttrib->CL_FD);
        return RXN_CL_OPEN_OR_CLOSE_ERR;
    }
  /* Get the current options for the port: */
  if (tcgetattr(pAttrib->CL_FD,&port_settings)!= 0) {
        return RXN_CL_OPEN_OR_CLOSE_ERR;
  }
  cfmakeraw(&port_settings);
  port_settings.c_cc[VMIN] = 1;
  port_settings.c_cc[VTIME] = 0;

  /* Set Byte Size */
  port_settings.c_cflag &= ~CSIZE;
  switch(uByteSize)
  {
    case 5:
    {
      port_settings.c_cflag |= CS5;
      break;
    }
    case 6:
    {
      port_settings.c_cflag |= CS6;
      break;
    }
    case 7:
    {
      port_settings.c_cflag |= CS7;
      break;
    }
    case 8:
    {
      port_settings.c_cflag |= CS8;
      break;
    }
    /* set parity */
    
    default:
      CL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,"CL_SetupPort: Byte size specified not supported.");
  }
  /* Set Parity */
  switch(uParity)
  {
    case 0:
    {
      port_settings.c_cflag &=~PARENB;
      port_settings.c_cflag &=~PARODD;
      break;
    }
    case 1:
    {
      port_settings.c_cflag &=PARENB;
      port_settings.c_cflag &=PARODD;
      break;
    }
    case 2:
    {
      port_settings.c_cflag &=PARENB;
      break;
    }
    default:
      CL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04, "CL_SetupPort: Parity specified not supported.");
  }

  /* Set Stop Bit */
  switch(uStopBits)
  {
    case 0:
    case 1:
    {
      port_settings.c_cflag &=~CSTOPB;
      break;
    }
    case 2:
    {
      port_settings.c_cflag &=CSTOPB;
      break;
    }

    default:
      CL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04, "CL_SetupPort: Stop bit specified not supported.");
  }
  port_settings.c_cflag |=  CLOCAL;
  port_settings.c_cflag &= ~CRTSCTS;

  /* set baudrate */
  switch(uBaudRate)
  {
    case 4800:
    {
      cfsetspeed(&port_settings, B4800);
      break;
    }
    case 9600:
    {
      cfsetspeed(&port_settings, B9600);
      break;
    }
    case 19200:
    {
      cfsetspeed(&port_settings, B19200);
      break;
    }
    case 38400:
    {
      cfsetspeed(&port_settings, B38400);
      break;
    }
    case 57600:
    {
      cfsetspeed(&port_settings, B57600);
      break;
    }
    case 115200:
    {
      cfsetspeed(&port_settings, B115200);
      break;
    }
    case 230400:
    {
      cfsetspeed(&port_settings, B230400);
      break;
    }
    default:
      CL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04, "CL_SetupPort: Baud Rate specified not supported.");
  }
  
  /* Apply settings */
  tcsetattr(pAttrib->CL_FD, TCSANOW, &port_settings);
  usleep(10000);
  tcflush(pAttrib->CL_FD, TCIFLUSH);

  return RXN_SUCCESS;
}


U16 CL_ReadBytes(CL_Chipset_Attrib_t * pAttrib, U08 * pBuf, U16 uBufSize,
		 U32 * puNumBytesRead)
{
  U08 temp;
	S32 result = 0;
  U16 count = 0;

    /* Don't do anything if the port has not been properly opened. */
	if ((pAttrib == 0) || (pAttrib->CL_FD == -1)) {
		/* Handle the error and return error code. */
		CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
			  "CL_ReadBytes called before a port was properly opened. Handle: %1.0f",
			  (double) pAttrib->CL_FD);
		return RXN_CL_OPEN_OR_CLOSE_ERR;
	}

  while(count < CL_MAX_MSG_BUF_LEN)
  {
    result = read(pAttrib->CL_FD, &temp, 1);
    if(result == 1)
    {
      pBuf[count] = temp;
      count++;
    }
    else
    {
      *puNumBytesRead = count;
      break;
    }
  }
	/* Return RXN_SUCCESS regardless of the number of bytes read. */
	return RXN_SUCCESS;
}

U16 CL_WriteBytes(CL_Chipset_Attrib_t * pAttrib, U08 * pBuf,
		  U32 uNumBytesToWrite)
{
	U32 uNumBytesWritten = 0;	/* Number of bytes actually written. */


	/* Don't do anything if the port has not been properly opened. */
	if ((pAttrib == 0) || (pAttrib->CL_FD == -1)) {
		/* Handle the error and return error code. */
		CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
			  "CL_WriteBytes called before a port was properly opened. "
			  "Handle: %1.0f", (double) pAttrib->CL_FD);
		return RXN_CL_OPEN_OR_CLOSE_ERR;
	}

  unsigned int i; 
  char ch; 
  volatile int k;
  for(i=0; i<uNumBytesToWrite; ++i)
  {
    ch = *(pBuf+i); 
    if(write(pAttrib->CL_FD, &ch , 1))
    {
      uNumBytesWritten++;
    }
    for (k=0; k<1000000; ++k) 
    {
    }
  }

	/* Validate that all bytes were written. */
	if (uNumBytesWritten < uNumBytesToWrite) {
		/* Handle the error and return an error code. */
		CL_LogFltFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
			     "WriteFile failed to write all bytes within CL_WriteBytes. "
			     "Wrote %1.0f of %1.0f.",
			     (double) uNumBytesWritten, (double) uNumBytesToWrite);
		return RXN_CL_CHIPSET_WRITE_ERR;
	}
	/* Return RXN_SUCCESS. */
	return RXN_SUCCESS;
}
U16 CL_ClosePort(CL_Chipset_Attrib_t* pAttrib)
{
	/* Don't do anything if the port has not been properly opened. */
	if((pAttrib == 0) || (pAttrib->CL_FD == -1))
	{
		/* Handle the error and return error code. */
    CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04, "CL_ClosePort called before a port was properly opened. Handle: %1.0f",
              (double) pAttrib->CL_FD);
		return RXN_CL_OPEN_OR_CLOSE_ERR;
	}

	/* Close the port. */
	if(close((int)pAttrib->CL_Port_Hdl) != 0)
  {
    CL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04, "Error calling close within CL_ClosePort.");
  }

	/* Return RXN_SUCCESS. */
	return RXN_SUCCESS;
}

U16 CL_PurgeTx(CL_Chipset_Attrib_t* pAttrib)
{
	/* Don't do anything if the port has not been properly opened. */
	if((pAttrib == 0) || (pAttrib->CL_FD == -1))
	{
		/* Handle the error and return error code. */
    CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04, "CL_PurgeTx called before a port was properly opened. Handle: %1.0f",
            (double) pAttrib->CL_FD);
		return RXN_CL_OPEN_OR_CLOSE_ERR;
	}

	/* Purge the transmit buf. */
  if(tcflush(pAttrib->CL_FD, TCOFLUSH) != 0)
  {
		/* Handle the error and return error code. */
    CL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04, "Error calling tcflush within CL_PurgeTx.");

		return RXN_CL_OPEN_OR_CLOSE_ERR;
  }

	/* Return RXN_SUCCESS. */
	return RXN_SUCCESS;
}

U16 CL_PurgeRx(CL_Chipset_Attrib_t* pAttrib)
{
	/* Don't do anything if the port has not been properly opened. */
	if((pAttrib == 0) || (pAttrib->CL_FD == -1))
	{
		/* Handle the error and return error code. */
    CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04, "CL_PurgeRx called before a port was properly opened. Handle: %1.0f",
              (double) pAttrib->CL_FD);
		return RXN_CL_OPEN_OR_CLOSE_ERR;
	}

	/* Purge the receive buf. */
  if(tcflush(pAttrib->CL_FD, TCIFLUSH) != 0)
  {
		/* Handle the error and return error code. */
    CL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04, "Error calling tcflush within CL_PurgeRx");

		return RXN_CL_OPEN_OR_CLOSE_ERR;
  }

	/* Return RXN_SUCCESS. */
	return RXN_SUCCESS;
}

/********************
 * Socket Functions *
 ********************/

U16 CL_CloseSckt(SOCKET GenSocket)
{
  /* Close the socket. */
  if(close(GenSocket) != SOCKET_ERROR)
  {
    return RXN_SUCCESS;
  }
  else
  {
    CL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE04,
      "CL_CloseSckt: Error closing a socket. Errno: %1.0f", (double) errno);
  }

  return RXN_FAIL;
}

/******************************
* Critical Section Functions *
******************************/

void CL_InitBlock(LOCK* lock)
{
    pthread_mutex_init(lock, NULL);
}

void CL_UninitBlock(LOCK* lock)
{
    pthread_mutex_destroy(lock);
}

BOOL CL_TryEnterBlock(LOCK* lock)
{
    int result;

    /* The pthread_mutex_lock() function returns an error if the mutex
     * is already locked. Otherwise, it returns with the mutex in the 
     * locked state with the calling thread as its owner.
     */
    result = pthread_mutex_trylock(lock);

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

void CL_EnterBlock(LOCK* lock)
{
    pthread_mutex_lock(lock);
}

void CL_LeaveBlock(LOCK* lock)
{
    pthread_mutex_unlock(lock);
}

void CL_InitEvent(EVENT* event)
{
    pthread_cond_init(event, NULL);
}

void CL_UninitEvent(EVENT* event)
{
    pthread_cond_destroy(event);
}

void CL_SignalEvent(EVENT* event)
{
    pthread_cond_signal(event);
}

BOOL CL_WaitForEvent(EVENT* event)
{
    int result = 0;
    pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;

    pthread_mutex_lock(&mutex);
    result = pthread_cond_wait(event, &mutex);
    pthread_mutex_unlock(&mutex);

    return (result == 0);    
}

S32 CL_GetOSError()
{
    return errno;
}

const char* CL_GetOSErrorString(S32 error)
{
    return strerror(error);
}

void CL_SetSockaddr(struct sockaddr_in* addr, const char* ip, U16 port)
{
    if (addr)
    {
        memset(addr, 0, sizeof(addr));
        addr->sin_family = AF_INET;
        addr->sin_addr.s_addr = inet_addr(ip);
        addr->sin_port = htons(port);
    }
}

void CL_StopThread(THREAD thread)
{
    /* Block the SIGTERM signal so we don't kill the whole process */
    sigset_t newMask;
    sigemptyset(&newMask);
    sigaddset(&newMask, SIGTERM);
    pthread_sigmask(SIG_BLOCK, &newMask, NULL);

    pthread_kill(thread, SIGTERM);
}

THREAD CL_StartThread(void* threadStartFunc, void* pArg)
{
    THREAD ret = 0;
    if(pthread_create(&ret, NULL, threadStartFunc, pArg) != 0)
    {
        CL_Log(TRUE, TRUE, RXN_LOG_SEV_ERROR, RXN_LOG_ZONE03,
            "Failed to start thread");
        return 0;
    }
 
    return ret;
}

