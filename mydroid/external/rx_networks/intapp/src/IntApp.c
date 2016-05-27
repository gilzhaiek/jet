/* IntApp.cpp : Defines the entry point for the console application. */

/* General Includes. */
#include <string.h>   /* Used for memset */
#include <stdlib.h>

#ifdef LINUX
#include <signal.h>
#endif

#ifdef _MSC_VER
#ifndef _WIN32_WINNT            /* Allow use of features specific to Windows XP or later.*/
#define _WIN32_WINNT 0x0501     /* Change this to the appropriate value to target other versions of Windows.*/
#endif

#ifndef CUSTOM_BUILD_SETTINGS
  #define USE_CL
#endif

#include <stdio.h>    /* Used for file I/O within the int app. */
#include <tchar.h>    /* Required for _tmain support. */
#include <windows.h>  /* Required for Win32 API support. */
#endif

/* RxN Library Includes. */
#include "RXN_CL.h"     /* Required to support a CL instance for chipset I/O. */
#if defined USE_RINEX
#include "RXN_CL_RX.h"
#endif /* USE_RINEX */

#include "RXN_MSL.h"    /* Required to support the MSL for generation of EE (PGPS or SAGPS). */
#include "RXN_MSL_Common.h"
#include "RXN_MSL_Platform.h"

#ifdef U8500
#define RXN_CONFIG_FILE       "/system/etc/MSLConfig.txt"
#elif ANDROID
#define RXN_CONFIG_FILE       "/data/RXN/MSLConfig.txt"
#elif defined WINCE
#define RXN_CONFIG_FILE       "\\RXN\\MSLConfig.txt"
#else
#define RXN_CONFIG_FILE       "MSLConfig.txt"
#endif

/* RxN Library Dependencies. */

#ifdef WIN32
#ifdef USE_CL
#ifdef USE_RINEX
#pragma comment(lib, "RXN_CL_XP_RINEX.lib")
#elif defined USE_GNL_3_1_5
#pragma comment(lib, "RXN_CL_XP_GNL_3_1_5.lib")
#pragma comment(lib, "GN_GPS_Lib_VStudio2005.lib")
#elif defined USE_WL1283
#pragma comment(lib, "RXN_CL_XP_WL1283_SOCKET.lib")
#else
#undef USE_CL
#endif
#endif
#endif

#define EXIT_FAILURE          1
#define EXIT_SUCCESS          0

#define RINEX_PATH                  "/data/RXN/rinex"

#ifdef USE_CL
static void locationCallback(RXN_RefLocation_t* location);
static void bceCallback(RXN_MSL_NavDataList_t* ephemeris, RXN_constel_t constel);
static void finishedAssistanceCallback();
#endif

/* Utility Fcns. */
static void shutDown();
static void readseed(const char* filename);

#if defined LINUX
void sighandler(int signal);
#endif

/* Global vars below. */
#if defined USE_RINEX
	static U16 gCL_Handle[2];        /* Handle to a CL instance.*/
#else
	static U16 gCL_Handle = 0;        /* Handle to a CL instance.*/
#endif
static BOOL  gRun = TRUE;         /* Flag to control exit. */
static U32 gStartTime = 0;

static char gCL_DefaultLogFilePath[RXN_MSL_MAX_PATH];
static char gCL_ConfigStr[RXN_CL_CONFIG_MAX_STR_LEN];
static char gMSL_ConfigFile[RXN_MSL_MAX_PATH];

/* Handle and thread proc to support calling the CL work function. */
void CLib_WorkThread();

void parseRinex(const char* filename)
{
    char rinexFile[RXN_MSL_MAX_PATH];
    sprintf(rinexFile,"%s/%s", RINEX_PATH, filename);
    printf("Loading RINEX file %s\n", rinexFile);
    RXN_MSL_ProcessRinexFile(rinexFile, RXN_GPS_CONSTEL);
}

void parseConfig(const char* filename)
{
    printf("Loading config file %s\n", filename);
    strncpy(gMSL_ConfigFile, filename, RXN_MSL_MAX_PATH);
}

void handleSingleCommand(BOOL beforeStartup, const char* command)
{
    if (command[1] == 'h')
    {
        printf("\nRXN_IntApp\n\n");
        printf("Usage:\n");
        printf("-h\tPrints this help message\n");
        printf("-r <file>\tLoad the RINEX file <file>\n");
        printf("-c <file>\tLoad the config file <file>\n");
        printf("-b <file>\tLoad the binary seed file <file>\n");
        printf("\n");
    }
    else
    {
        printf("Unknown command %s\n", command);
    }
}

void handleCommand(BOOL beforeStartup, const char* command, const char* arg)
{
    if (command)
    {
        switch(command[1])
        {
            case 'r':
                {
                    if (!beforeStartup)
                        parseRinex(arg);
                    break;
                }
            case 'c':
                {
                    if (beforeStartup)
                        parseConfig(arg);
                    break;
                }
            case 'b':
                {
                    if (!beforeStartup)
                        readseed(arg);
                    break;
                }
            default:
                {
                    printf("Unknown command: %s %s\n", command, arg);
                    break;
                }
        }
    }
}

void parseArguments(BOOL beforeStartup, int argc, const char* argv[])
{
    while (--argc)
    {
        const char* token = argv[argc]; 
        if (token)
        {
            if (token[0] == '-')
            {
                handleSingleCommand(beforeStartup, token);
            }
            else if (--argc >= 0)
            {
                handleCommand(beforeStartup, argv[argc], token);
            }
        }
    }
}

#if defined USE_CL
void initializeCL()
{
    U32 error = 0;
#if defined USE_RINEX
	char * nextParam = 0;
	char lconfig[RXN_CL_CONFIG_MAX_STR_LEN];
#endif

    /* Get CL ConfigStr from MSLConfig.txt */
    if(RXN_MSL_Get_CL_Config_Str(gCL_ConfigStr) != RXN_SUCCESS)
    {
        printf("RXN_MSL_Get_CL_Config_Str failed.\n"); 
        exit(EXIT_FAILURE);
    }
    /* Initialize the CL for all chipset I/O. */
#if defined USE_RINEX

	nextParam = strstr(gCL_ConfigStr, " ");

	if(nextParam == NULL)
	{
		memcpy(lconfig, gCL_ConfigStr, RXN_CL_CONFIG_MAX_STR_LEN);
	}
	else
	{
		/* file path for GPS RINEX file */
		memset(lconfig, 0, RXN_CL_CONFIG_MAX_STR_LEN);
		memcpy(lconfig, gCL_ConfigStr, strlen(gCL_ConfigStr)-strlen(nextParam));

		/* remove leading space character for GLO RINEX file */
		nextParam = nextParam+1;

		if(RXN_CL_RINEX_Initialize(nextParam, &gCL_Handle[RXN_GLO_CONSTEL]) != RXN_SUCCESS)
		{
			printf("RXN_CL_Initialize failed. Please refer to cl_Log.txt for the details.\n");
			/* Shutdown the MSL logger. */
			RXN_MSL_Log_UnInit();    
			exit(EXIT_FAILURE); 
		}
	}

    if(RXN_CL_RINEX_Initialize(lconfig, &gCL_Handle[RXN_GPS_CONSTEL]) != RXN_SUCCESS)
    {
        printf("RXN_CL_Initialize failed. Please refer to cl_Log.txt for the details.\n");
        /* Shutdown the MSL logger. */
        RXN_MSL_Log_UnInit();    
        exit(EXIT_FAILURE); 
    }
#else
    /* ERROR LOG NOTE
    * No error logging is handled within the integration application itself.
    * If logging (in addition to that supported within the MSL and CL) is
    * required - log such errors based on RXN_CL_* and RXN_MSL_* returns.*/

    /* Initalize the CL logger. */
    if((error = RXN_CL_Log_Init(gCL_DefaultLogFilePath)) != RXN_SUCCESS)
    {
        printf("RXN_CL_Log_Init failed due to err %u.\n", error);
        exit(EXIT_FAILURE);
    }

    if(RXN_CL_Initialize(100, gCL_ConfigStr, &gCL_Handle) != RXN_SUCCESS)
    {
        printf("RXN_CL_Initialize failed. Please refer to cl_Log.txt for the details.\n");
        /* Shutdown the CL logger. */
        RXN_CL_Log_UnInit();

        /* Shutdown the MSL logger. */
        RXN_MSL_Log_UnInit();    
        exit(EXIT_FAILURE); 
    }

#ifdef USE_CL_THREAD
    if (MSL_StartThread(&CLib_WorkThread) == 0)
    {
        printf("Error creating CL thread\n");
        shutDown();
        exit(EXIT_FAILURE);
    }
#endif
#endif /* USE_RINEX */
}
#endif /* USE_CL */


int main(int argc, const char *argv[])
{
    U16 result = 0;

    /* Drop the thread priority. */
    RXN_MSL_SetPriority(RXN_MSL_PRIORITY_LOW);

#ifdef LINUX
    /* Setup the handler for all possible termination signals. */
    signal(SIGTERM, sighandler);
    signal(SIGABRT, sighandler);
    signal(SIGQUIT, sighandler);
#endif

    memset((void*) gCL_DefaultLogFilePath, 0, sizeof(char) * RXN_MSL_MAX_PATH);
    memset((void*) gCL_ConfigStr, 0, sizeof(char) * RXN_CL_CONFIG_MAX_STR_LEN);
    memset((void*) gMSL_ConfigFile, 0, sizeof(char) * RXN_MSL_MAX_PATH);

    sprintf(gCL_DefaultLogFilePath, "/data/RXN/CL_Log.txt");	

    /* Parse command line arguments before MSL startup */
    parseArguments(TRUE, argc, argv);

    /* Load config file from the command line and fallback to the default
       if necessary */
    if (gMSL_ConfigFile[0] == '\0'
        || (result = RXN_MSL_Load_Config(gMSL_ConfigFile)) != RXN_SUCCESS)
    {
        printf("Loading default configuration file %s\n", RXN_CONFIG_FILE);
        result = RXN_MSL_Load_Config(RXN_CONFIG_FILE);	
    }

    if(result != RXN_SUCCESS)
    {
        if(result == RXN_MSL_CONFIG_FORMAT_ERR)
        {
            printf("RXN_MSL_Load_Config: Error opening configuration file.\n");
        }
        else
        {
            printf("RXN_MSL_Load_Config failed due to err %u.\n", result);
        }		
        exit(EXIT_FAILURE);
    }

#if defined USE_CL
    initializeCL();
#endif

    /* Initialize the MSL.  */
    if((result = RXN_MSL_Initialize()) != RXN_SUCCESS)
    {
        shutDown();

        if(result == RXN_MSL_EOL_ERR)
        {
            printf("EOL detected.\n");
            /* IntApp usually is registered as a service / daemon,
            * on most OSs if a service exits or crashes it will automatically restart it.
            * On integrations where the intapp is a service and daemon and an EOL is detected,
            * instead of exiting which would cause an infinite restart loop, we simple sleep.
            * For integrations that do not register us as a service or daemon or have built in
            * logic to automatically restart the intapp on exit this sleep can be removed. */
            while(1)
            {
                MSL_Sleep(1000);
            }
        }
        else
        {
            printf("RXN_MSL_Initialize failed due to err %u.\n", result);
            exit(EXIT_FAILURE);
        }
    }

    /* Parse command line arguments after MSL startup */
    parseArguments(FALSE, argc, argv);

#ifdef USE_CL
    RXN_MSL_AddLocationObserver(locationCallback);
    RXN_MSL_AddBCEObserver(bceCallback);
    RXN_MSL_AddEEObserver(bceCallback);
    RXN_MSL_AddFinishedAssistanceObserver(finishedAssistanceCallback);
#endif


    /* Continuously loop - supporting BCE updates (through the CL work function)
    * and extended ephemeris generation or acquisition (through the MSL work() function).*/
    while(gRun)
    {
        /* Give other threads some cycles.*/
        MSL_Sleep(1);

        /* Give the MSL a chance to work (perform a single step towards
        * any outstanding tasks required to support generation of EE).*/

        result = RXN_MSL_Work();
        if(result != RXN_SUCCESS)
        {
            /* Check if error is critical. */
            if(result != RXN_MSL_SKT_COMMS_ERR)  /* Can occur if net connect unavail */
            {                                    /* or time offset inaccurate. */
                printf("RXN_MSL_Work net connect error.\n");
                break;
            }
            printf("RXN_MSL_Work failed due to err %u.\n", result);
        }
#ifdef USE_RINEX
		result = RXN_CL_RINEX_Work(gCL_Handle[RXN_GPS_CONSTEL]);
		if(result != RXN_SUCCESS)
		{
			printf("RXN_CL_RINEX_Work for GPS failed.\n");
		}

		if(gCL_Handle[RXN_GLO_CONSTEL] != 0)
		{
			result = RXN_CL_RINEX_Work(gCL_Handle[RXN_GLO_CONSTEL]);
			if(result != RXN_SUCCESS)
			{
				printf("RXN_CL_RINEX_Work for GLO failed.\n");
			}
		}
#endif
    } /* while(gRun) */

    /* Give the CLib_WorkThread time to shut down (1 sec). */
    MSL_Sleep(1000);

    /* Shutdown libs and logging. */
    shutDown();
    exit(EXIT_SUCCESS);
}
#if defined USE_CL
static void bceCallback(RXN_MSL_NavDataList_t* ephemeris, RXN_constel_t constel)
{
    U16 result = 0;
    int i = 0;

    if (!gCL_Handle)
        return;

    printf("EE/BCE callback %u\n", RXN_MSL_GetBestGPSTime() - gStartTime);

#if defined USE_RINEX
	if(gCL_Handle[constel] != NULL)
	{
		result = RXN_CL_RINEX_WriteEphemeris(gCL_Handle[constel], ephemeris, constel);
	}
	else
	{
		printf("Please update the MSL_Config.txt param: CL_Config_Str to include a output file for constel: %d.\n", constel);
		return;
	}
#else
    result = RXN_CL_WriteEphemeris(gCL_Handle, ephemeris, constel);
#endif /* USE_RINEX */

    if (result != RXN_SUCCESS)
    {
        printf("Error writing ephemeris");
    }

    for (i = 0; i < ephemeris->numEntries; i++)
    {
        if(constel == RXN_GPS_CONSTEL)
        {
            printf("%u, ", ephemeris->ephList.gps[i].prn);
        }
        else
        {
            printf("%u, ", ephemeris->ephList.glo[i].slot);
        }
    }
    printf("\n");
}

static void locationCallback(RXN_RefLocation_t* location)
{
#ifndef USE_RINEX
    if (gCL_Handle)
    {
        printf("location callback %u\n", RXN_MSL_GetBestGPSTime() - gStartTime);
        RXN_CL_SetRefLocTime(gCL_Handle, location, NULL);
    }
#endif
}

static void finishedAssistanceCallback()
{
#ifndef USE_RINEX
    printf("Finished: %u\n", RXN_MSL_GetBestGPSTime() - gStartTime);
    RXN_CL_WriteEphemeris(gCL_Handle, NULL, 1);
    RXN_MSL_SetPriority(RXN_MSL_PRIORITY_LOW); 
#endif
}

/* Implementation of the static callback declared within RXN_CL.h. */
void RXN_CL_GetAssistance(U08 assistTypeBitMask, U32 gpsPrnBitMask, U32 gloSlotBitMask)
{
    /* Don't continue if a CL instance is not in place.*/
    if(gCL_Handle == 0)
    {
        return;
    }

#ifndef USE_RINEX
    if (assistTypeBitMask & RXN_MSL_ASSISTANCE_REF_TIME)
    {
        RXN_RefTime_t RefTime;
        if (RXN_MSL_GetSNTPTime(&RefTime) == RXN_SUCCESS)
        {
			RXN_CL_SetRefLocTime(gCL_Handle, NULL, &RefTime);
        }
    }
#endif

    /* Increase the thread priority. */
    RXN_MSL_SetPriority(RXN_MSL_PRIORITY_HIGH); 

    gStartTime = RXN_MSL_GetBestGPSTime();
    printf("Start gpsPrnBitMask: %d, gloSlotBitMask %d\n", gpsPrnBitMask, gloSlotBitMask);
    RXN_MSL_TriggerAssistance(assistTypeBitMask, gpsPrnBitMask, gloSlotBitMask);

    return;
}
#endif /* #if defined USE_CL */

static void shutDown()
{
    /* Stop the MSL. */
    RXN_MSL_Uninitialize();

#ifdef USE_CL
#ifdef USE_RINEX
    /* Shutdown the CL. */
    RXN_CL_RINEX_Uninitialize(gCL_Handle[RXN_GPS_CONSTEL]);
	if(gCL_Handle[RXN_GLO_CONSTEL] != 0)
	{
		RXN_CL_RINEX_Uninitialize(gCL_Handle[RXN_GLO_CONSTEL]);
	}
#else
    /* Shutdown the CL. */
    RXN_CL_Uninitialize(gCL_Handle);
    /* Stop CL Logging. */
    RXN_CL_Log_UnInit();
#endif /* USE_RINEX */
    gCL_Handle = 0;
#endif /* USE_CL */

    /* Stop MSL Logging. */
    RXN_MSL_Log_UnInit();
}

#if defined USE_CL && !defined USE_RINEX
void CLib_WorkThread()
{
    /* Use files to provide a control mechanism for cold and warm restarts.
     * In a standard integration, this control will be coupled to power management
     * and will be quite device - specific. */
    const char* cold_start_file_path = "/data/RXN/cold.start";
    const char* warm_start_file_path = "/data/RXN/warm.start";
    FILE* coldStartFile = NULL;
    FILE* warmStartFile = NULL;

    /* Create files that will be used to control integration application restart. */
    coldStartFile = fopen(cold_start_file_path, "w");
    warmStartFile = fopen(warm_start_file_path, "w");

    /* Close the files. Only file creation is required. */
    fclose(coldStartFile);
    fclose(warmStartFile);
    coldStartFile = NULL;
    warmStartFile = NULL;

    while(TRUE)
    {
        usleep(1000 * 1000);

        /* Check for a signal to cold start. */
        coldStartFile = fopen(cold_start_file_path, "r");
        if(coldStartFile == NULL)
        {
            /* Restart */
            RXN_CL_Restart(gCL_Handle, RXN_CL_COLD_RESTART, NULL, NULL);

            /* Re-create the file for subsequent restarts. */
            coldStartFile = fopen(cold_start_file_path, "w");
            if(coldStartFile != NULL)
            {
                fclose(coldStartFile);
                coldStartFile = NULL;
            }
        }
        else
        {
            fclose(coldStartFile);
        }

        /* Check for a signal to warm start. */
        warmStartFile = fopen(warm_start_file_path, "r");
        if(warmStartFile == NULL)
        {
            /* Restart */
            RXN_CL_Restart(gCL_Handle, RXN_CL_WARM_RESTART, NULL, NULL);

            /* Re-create the file for subsequent restarts. */
            warmStartFile = fopen(warm_start_file_path, "w");
            if(warmStartFile != NULL)
            {
                fclose(warmStartFile);
                warmStartFile = NULL;
            }
        }
        else
        {
            fclose(warmStartFile);
        }
    }
}
#endif

#ifdef LINUX
void sighandler(int signal)
{
    /* Check the signal */
    if( (signal == SIGTERM) ||
        (signal == SIGABRT) ||
        (signal == SIGQUIT) )
    {
        /* Notify the loop to exit. */
        printf("sighandler is signaled.\n");
        gRun = FALSE;        
    }

    return;
}
#endif

static void readseed(const char* filename)
{
    U08 buffer[MSL_SKT_RESP_MSG_MAX_LEN];
    U16 error = RXN_SUCCESS;
    U16 bytesRead = 0;

    if (filename == NULL)
    {
        printf("Error: No binary seed file specified\n");
        return;
    }

    if (MSL_OpenFile(MSL_PGPS_SEED_FILE_RSRC_ID, filename, "rb") != RXN_SUCCESS) 
    {
        printf("Error opening file\n");
        return;
    }

    while (bytesRead < MSL_SKT_RESP_MSG_MAX_LEN && error == RXN_SUCCESS)
    {
        error = MSL_ReadFileBytes(MSL_PGPS_SEED_FILE_RSRC_ID, buffer + bytesRead++, 1);
    }

    if (error != MSL_FILE_EOF_ERROR)
    {
        printf("Error reading file data\n");
    }
    else 
    {
        if (RXN_Decode(buffer, bytesRead, 0) == RXN_SUCCESS)
        {
            printf("Decoded GPS seed\n");
            MSL_SetNextSystemEvent(MSL_EVENT_SEED_DOWNLOADED);
        }
        else if (RXN_Decode(buffer, bytesRead, 1) == RXN_SUCCESS)
        {
            printf("Decoded GLONASS seed\n");
            MSL_SetNextSystemEvent(MSL_EVENT_SEED_DOWNLOADED);
        }
        else
        {
            printf("Could not decode seed\n");
        }
    }

    MSL_CloseFile(MSL_PGPS_SEED_FILE_RSRC_ID);
}

