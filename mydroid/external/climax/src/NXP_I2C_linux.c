
/* implementation of the NXP_I2C API on Linux */

#include <stdio.h>
#include <assert.h>
#include <unistd.h>
#include <sys/types.h>
#include "NXP_I2C.h"
#include "lxScribo.h"
#include "nxpTfa98xx.h"	// for device driver interface special file

#ifndef __BOOL_DEFINED
  typedef unsigned char bool;
  #define true ((bool)(1==1))
  #define false ((bool)(1==0))
#endif

typedef enum { SCL, SDA } i2cPins;

typedef enum { i2cNoInterface, i2cDSM, i2cSinglemaster, i2cHardwareless, i2cDSI, i2cSSI } i2cInterfaceTypes;

typedef enum { i2cBus1, i2cBus2, i2cBus2Burst } i2cBusTypes;

typedef enum { i2cNoError, i2cNoAck, i2cSclStuckAtOne, i2cSdaStuckAtOne, i2cSclStuckAtZero, i2cSdaStuckAtZero,
	i2cTimeOut, i2cArbLost, i2cNoInit, i2cDisabled, i2cUnsupportedValue, i2cUnsupportedType, i2cNoInterfaceFound,
	i2cNoPortnumber } i2cErrorTypes;


typedef struct
{
  char msg[32];
} NXP_I2C_ErrorStr_t;

NXP_I2C_Error_t i2cExecuteRS(int NrOfWriteBytes,
		const unsigned char * WriteData, int NrOfReadBytes, unsigned char * ReadData);

/* set to true to show the actually I2C message in stdout
 * set to false otherwise
 */
static int bDebug = false;
/* for debug logging, keep track of number of I2C messages and when sent */
static long cnt = 0;
static long start;
static char debug_buffer[5000];

static int bInit = false;

static int i2cTargetFd=-1;						 /* file descriptor for target device */
#ifdef I2CVERBOSE
int NXP_I2C_verbose=1;
#else
int NXP_I2C_verbose=0;
#endif
//#define VERBOSE(format, args...) if (NXP_I2C_verbose) printf(format, ##args)
#define VERBOSE if (NXP_I2C_verbose)

static NXP_I2C_Error_t init_I2C(void)
 {
#ifdef TFA_CHECK_REV
 unsigned char wbuf[2], rbuf[3];
#endif 
	VERBOSE printf("%s\n", __FUNCTION__);

	/*	TODO properly define open device configs */
#ifdef TFA_I2CDEVICE
	if (i2cTargetFd < 0 ) {
		i2cTargetFd = lxScriboRegister(TFA_I2CDEVICE);
	}
#else
	if (i2cTargetFd < 0 ) {
		i2cTargetFd = lxScriboRegister("/dev/Scribo");
	//	i2cTargetFd = lxScriboGetFd();
	}
#endif
	if (i2cTargetFd < 0 ){
		fprintf( stderr, "!i2c device was not opened\n");
		return NXP_I2C_NoInterfaceFound;
	}

#ifdef TFA_CHECK_REV
        // check the contents of the ID regsiter
        wbuf[0] = TFA_I2CSLAVE<<1;
        wbuf[1] = 0x03; // ID register
        rbuf[0] = (TFA_I2CSLAVE<<1)+1; // read
        i2cExecuteRS( sizeof(wbuf), wbuf, sizeof(rbuf), rbuf);
        if ( rbuf[1]!=(TFA_CHECK_REV>>8) || rbuf[2] != (TFA_CHECK_REV&0xff) ) {
                fprintf( stderr, "!wrong ID expected:0x%04x received:0x%04x, register 3 of slave 0x%02x\n", 
                             TFA_CHECK_REV ,  rbuf[1]<<8 | rbuf[2], TFA_I2CSLAVE);
              return NXP_I2C_NoInterfaceFound;
              }
#endif        
	bInit = true;
	return NXP_I2C_Ok;
}

static const NXP_I2C_ErrorStr_t errorStr[NXP_I2C_ErrorMaxValue] =
{
  { "UnassignedErrorCode" },
  { "Ok" },
  { "NoAck" },
  { "SclStuckAtOne" },
  { "SdaStuckAtOne" },
  { "SclStuckAtZero" },
  { "SdaStuckAtZero" },
  { "TimeOut" },
  { "ArbLost" },
  { "NoInit" },
  { "Disabled" },
  { "UnsupportedValue" },
  { "UnsupportedType" },
  { "NoInterfaceFound" },
  { "NoPortnumber" }
};

#define DUMMIES
#ifdef DUMMIES
/*
 * dummies TODO fix dummies
 */
int GetTickCount(void)
{
	return 0;
}

void  i2cDebugStr(char * GetString)
{
	GetString = "TBD" ;
}
void  i2cDebug(int err)
{
	printf("%s %d\n", __FUNCTION__, err);
}
static void hexdump(int num_write_bytes, const unsigned char * data)
{
	int i;

	for(i=0;i<num_write_bytes;i++)
	{
		printf("0x%02x ", data[i]);
	}

}
NXP_I2C_Error_t i2cExecute(int num_write_bytes, unsigned char * data)
{

	VERBOSE printf("%s   W %3d: ",__FUNCTION__, num_write_bytes);
	VERBOSE hexdump(num_write_bytes, data);
	VERBOSE printf("\n");

	if ( 0 == (*lxWrite)(i2cTargetFd, num_write_bytes, data) ) {
		VERBOSE printf(" NoAck\n");
		return NXP_I2C_NoAck;
	}
	return NXP_I2C_Ok;
}

NXP_I2C_Error_t i2cExecuteRS(int NrOfWriteBytes,
		const unsigned char * WriteData, int NrOfReadBytes, unsigned char * ReadData)
{

	VERBOSE printf("%s W %3d: ",__FUNCTION__, NrOfWriteBytes);
	VERBOSE hexdump(NrOfWriteBytes, WriteData);
	VERBOSE printf("\n");

	NrOfReadBytes = (*lxWriteRead)(i2cTargetFd,  NrOfWriteBytes, WriteData ,
			NrOfReadBytes, ReadData);
	if (NrOfReadBytes ) {
		VERBOSE printf("%s R %3d: ",__FUNCTION__, NrOfReadBytes);
		VERBOSE hexdump(NrOfReadBytes, ReadData);
		VERBOSE printf("\n");

	}
	else {
		VERBOSE printf(" NoAck\n");
		return NXP_I2C_NoAck;
	}
	return NXP_I2C_Ok;
}
#endif

i2cErrorTypes i2cError(void)
{
	return i2cNoError;
}
static NXP_I2C_Error_t translate_error(i2cErrorTypes error)
{
  NXP_I2C_Error_t retval;

  switch (error)
  {
    case i2cNoError: retval=NXP_I2C_Ok;
      break;
    case i2cNoAck: retval=NXP_I2C_NoAck;
      break;
    case i2cSclStuckAtOne: retval=NXP_I2C_SclStuckAtOne;
      break;
    case i2cSdaStuckAtOne: retval=NXP_I2C_SdaStuckAtOne;
      break;
    case i2cSclStuckAtZero: retval=NXP_I2C_SclStuckAtZero;
      break;
    case i2cSdaStuckAtZero: retval=NXP_I2C_SdaStuckAtZero;
      break;
    case i2cTimeOut: retval=NXP_I2C_TimeOut;
      break;
    case i2cArbLost: retval=NXP_I2C_ArbLost;
      break;
    case i2cNoInit: retval=NXP_I2C_NoInit;
      break;
    case i2cDisabled: retval=NXP_I2C_Disabled;
      break;
    case i2cUnsupportedValue: retval=NXP_I2C_UnsupportedValue;
      break;
    case i2cUnsupportedType: retval=NXP_I2C_UnsupportedType;
      break;
    case i2cNoInterfaceFound: retval=NXP_I2C_NoInterfaceFound;
      break;
    case i2cNoPortnumber: retval=NXP_I2C_NoPortnumber;
      break;
    default:
      /* unexpected error */
      assert(0);
		  retval=NXP_I2C_UnassignedErrorCode;
  }

  if (error != i2cNoError)
	{
    printf("I2C error %d (%s)\n", error, (char*)&errorStr[retval].msg);
	}
  return retval;
}

static NXP_I2C_Error_t init_if_firsttime(void)
{
  NXP_I2C_Error_t retval = NXP_I2C_Ok;

  if (!bInit)
  {
    retval = init_I2C();
  }
  return retval;
}

static void debugInfo(void)
{
	cnt++;
	if (bDebug)
	{
		long t = GetTickCount();
		debug_buffer[0] = '\0';
		i2cDebugStr(debug_buffer);
		printf("%d (%d) %s\n", (int)cnt, (int)(t-start), (char*)debug_buffer);
		fflush(stdout);
	}
}

NXP_I2C_Error_t NXP_I2C_Write(  int num_write_bytes,
                                unsigned char data[] )
{
  NXP_I2C_Error_t retval;

	if (num_write_bytes > NXP_I2C_MAX_SIZE)
	{
		fprintf(stderr, "%s: too many bytes: %d\n", __FUNCTION__, num_write_bytes);
		return NXP_I2C_UnsupportedValue;
	}

  retval = init_if_firsttime();
  if (NXP_I2C_Ok==retval)
  {
    i2cExecute(num_write_bytes, data);
	  retval = translate_error( i2cError() );
    debugInfo();
  }
  return retval;
}

NXP_I2C_Error_t NXP_I2C_WriteRead(  int num_write_bytes,
                                    unsigned char write_data[],
                                    int num_read_bytes,
                                    unsigned char read_data[] )
{
  NXP_I2C_Error_t retval;

	if (num_write_bytes > NXP_I2C_MAX_SIZE)
	{
		fprintf(stderr, "%s: too many bytes to write: %d\n", __FUNCTION__, num_write_bytes);
		return NXP_I2C_UnsupportedValue;
	}
	if (num_read_bytes > NXP_I2C_MAX_SIZE)
	{
		fprintf(stderr, "%s: too many bytes to read: %d\n", __FUNCTION__, num_read_bytes);
		return NXP_I2C_UnsupportedValue;
	}

  retval = init_if_firsttime();
  if (NXP_I2C_Ok==retval)
  {
	  i2cExecuteRS(num_write_bytes, write_data, num_read_bytes, read_data);
	  retval = translate_error( i2cError() );
    debugInfo();
  }
  return retval;
}

NXP_I2C_Error_t NXP_I2C_EnableLogging(int bEnable)
{
  NXP_I2C_Error_t retval;

  retval = init_if_firsttime();
  if (NXP_I2C_Ok==retval)
  {
	  bDebug = bEnable;
	  i2cDebug(bDebug);
  }
	return retval;
}
