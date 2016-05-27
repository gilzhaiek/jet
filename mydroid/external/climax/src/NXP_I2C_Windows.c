
#include <windows.h>
#include <tchar.h>

/* implementation of the NXP_I2C API on Windows */

#include "NXP_I2C.h"
#include <stdio.h>
#include <assert.h>

/* suppress the warnings from VS regarding bit field types other than int in crdi2c.h */
#pragma warning(disable:4214)
#include "CrdI2c.h"

typedef struct
{
  char msg[32];
} NXP_I2C_ErrorStr_t;


int NXP_I2C_verbose=1;

/* set to true to show the actually I2C message in stdout
 * set to false otherwise
 */

static bool bDebug = false;
/* for debug logging, keep track of number of I2C messages and when sent */
static long cnt = 0;
static long start;
static char debug_buffer[5000];

static bool bInit = false;


/* this overrules the initialisers of CrdI2c.dll (?) */
extern i2cDebug_type *i2cDebug = NULL;
extern i2cDebugStr_type *i2cDebugStr = NULL;
extern i2cSetBus_type *i2cSetBus = NULL;
extern i2cInterfacePresent_type *i2cInterfacePresent = NULL;
extern i2cExecute_type *i2cExecute = NULL;
extern i2cExecuteRS_type *i2cExecuteRS = NULL;
extern i2cError_type *i2cError = NULL;
extern i2cConfig_type *i2cConfig = NULL;
extern i2cGetVersion_type *i2cGetVersion = NULL;
extern i2cGetBitrate_type *i2cGetBitrate = NULL;
extern i2cSetBitrate_type *i2cSetBitrate = NULL;

#define VERBOSE if (NXP_I2C_verbose)

static void hexdump(int num_write_bytes, const unsigned char * data)
{
	int i;

	for(i=0;i<num_write_bytes;i++)
	{
		printf("0x%02x ", data[i]);
	}

}

NXP_I2C_Error_t init_I2C(void)
 {
	char info[128];
	int bitrate;
   HMODULE hDll;

	/* copied the relevant code from C:\Program Files\NXP\I2C\C\CrdI2c.c
	 */
	hDll = LoadLibrary(_TEXT("CrdI2c32.dll"));
	if (hDll == 0) {
		fprintf(stderr, "Could not open crdi2c32.dll\n");
		return NXP_I2C_NoInit;
	}

	i2cConfig = (i2cConfig_type*) GetProcAddress(hDll, "i2cConfig");
	if (i2cConfig == NULL) {
		FreeLibrary(hDll);
		return NXP_I2C_NoInit; // function not found in library
	}

	i2cDebug = (i2cDebug_type*) GetProcAddress(hDll, "i2cDebug");
	if (i2cDebug == NULL) {
		FreeLibrary(hDll);
		return NXP_I2C_NoInit; // function not found in library
	}
	i2cDebugStr = (i2cDebugStr_type*) GetProcAddress(hDll, "i2cDebugStr");
	if (i2cDebugStr == NULL) {
		FreeLibrary(hDll);
		return NXP_I2C_NoInit; // function not found in library
	}
	i2cGetVersion = (i2cGetVersion_type*) GetProcAddress(hDll, "i2cGetVersion");
	if (i2cGetVersion == NULL) {
		FreeLibrary(hDll);
		return NXP_I2C_NoInit; // function not found in library
	}
	i2cError = (i2cError_type*) GetProcAddress(hDll, "i2cError");
	if (i2cError == NULL) {
		FreeLibrary(hDll);
		return NXP_I2C_NoInit; // function not found in library
	}
	i2cExecute = (i2cExecute_type*) GetProcAddress(hDll, "i2cExecute");
	if (i2cExecute == NULL) {
		FreeLibrary(hDll);
		return NXP_I2C_NoInit; // function not found in library
	}
	i2cExecuteRS = (i2cExecuteRS_type*) GetProcAddress(hDll, "i2cExecuteRS");
	if (i2cExecuteRS == NULL) {
		FreeLibrary(hDll);
		return NXP_I2C_NoInit; // function not found in library
	}
	i2cGetBitrate = (i2cGetBitrate_type*) GetProcAddress(hDll, "i2cGetBitrate");
	if (i2cGetBitrate == NULL) {
		FreeLibrary(hDll);
		return NXP_I2C_NoInit; // function not found in library
	}
	i2cInterfacePresent = (i2cInterfacePresent_type*) GetProcAddress(hDll,
			"i2cInterfacePresent");
	if (i2cInterfacePresent == NULL) {
		FreeLibrary(hDll);
		return NXP_I2C_NoInit; // function not found in library
	}

	i2cSetBitrate = (i2cSetBitrate_type*) GetProcAddress(hDll, "i2cSetBitrate");
	if (i2cSetBitrate == NULL) {
		FreeLibrary(hDll);
		return NXP_I2C_NoInit; // function not found in library
	}
	i2cSetBus = (i2cSetBus_type*) GetProcAddress(hDll, "i2cSetBus");
	if (i2cSetBus == NULL) {
		FreeLibrary(hDll);
		return NXP_I2C_NoInit; // function not found in library
	}
	i2cSetBus(i2cBus1);
	if (!i2cInterfacePresent(FALSE)) {
		i2cConfig();
	}
	i2cSetBitrate(0); // set maximum speed: 400kHz
	//i2cSetBitrate(100);
	if (!i2cInterfacePresent(FALSE)) {
		fprintf(stderr, "I2C interface could not be initialised!\n");
		return NXP_I2C_NoInit;
	}
	i2cGetVersion(info);
	printf("Using I2C DLL version %s\n", info);
	bitrate = i2cGetBitrate();
	bitrate = (0 == bitrate) ? 400 : bitrate; // 0 means maximus value, so 400
	printf("bitrate: %d\n", bitrate);

	if (bDebug) {
		i2cDebug(1);
	}
	start = GetTickCount();

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
    printf("I2C error %d (%s)\n", error, &errorStr[retval].msg);
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
		printf("%d (%d) %s\n", cnt, t-start, debug_buffer);
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
    i2cExecute(i2cBus1, num_write_bytes, data);
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
      VERBOSE printf("%s W %3d: ",__FUNCTION__, num_write_bytes);
	   VERBOSE hexdump(num_write_bytes, write_data);
	   VERBOSE printf("\n");
      i2cExecuteRS(i2cBus1, num_write_bytes, write_data, num_read_bytes, read_data);
      if (num_read_bytes ) 
      {
         VERBOSE printf("%s R %3d: ",__FUNCTION__, num_read_bytes);
         VERBOSE hexdump(num_read_bytes, read_data);
		   VERBOSE printf("\n");
      }
      else 
      {
		   VERBOSE printf(" NoAck\n");
		   return NXP_I2C_NoAck;
	   }
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
	  bDebug = (bool)bEnable;
	  i2cDebug(bDebug);
  }
	return retval;
}
