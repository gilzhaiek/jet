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

/* ==================================================================
* Abstraction Layer functions.
*
* These functions need to be implemented by the system integrator
* and are platform-specific.
*
* ==================================================================
*/

/* Include support for threading sync and other O/S - specific elements. */
#ifdef WINCE
#include <winbase.h>        /* Required for Win32 functions. */
#include <Winsock2.h>       /* Required for winsock fcns such as gethostname(). */
#endif /* defined (WINCE) */

#if defined (WIN32) && ! defined(WINCE)
#include <windows.h>        /* Required for Win32 functions. */
#endif /* defined (WIN32) || (WINCE) */

#ifdef LINUX
#include <pthread.h>
#include <unistd.h> /* gethostname() */
#endif

#if defined (ANDROID) || defined (LINUX)
#include <sys/types.h>
#include <sys/stat.h>
#endif

#include <stdio.h>      /* Required for ANSI-C file I/O. */

/* Include RXN constant values. */
#include "RXN_constants.h"

/* Include MSL support elements. */
#include "RXN_MSL_Common.h"

/* Include abstraction initialisation definitions. */
#include "RXN_API_calls_extra.h"

/* Utility function that can init file contents with a specific value. */
U16 MSL_InitStoreFile(U08 ID, U16 totalBytes, U08 value);

/* Define the max length for file system paths. */
#define MAX_FILE_PATH_LEN	128

/* Store a format for poly data files. */
static char mPolyFormat[MSL_MAX_FULL_FILE_PATH];

/* Store a format for eph data files. */
static char mEphFormat[MSL_MAX_FULL_FILE_PATH];

/* Store a format for frequency channel data files. */
static char mFreqChanFormat[MSL_MAX_FULL_FILE_PATH];

/* Store a format for block type data files. */
static char mBlockTypeFormat[MSL_MAX_FULL_FILE_PATH];

/* Store a full path for rxn_config_data file */
static char mConfigFullPath[MSL_MAX_PATH];

/* Define a file name format for poly data files. */
static const char* mPolyFileFormat = "pred_%d";

/* Define a file name format for eph data files. */
static const char* mEphFileFormat = "eph_%d";

/* Define a file name format for frequency channel data files. */
static const char* mFreqChanFileFormat = "freqchan_%d";

/* Define a file name format for block type data files. */
static const char* mBlockTypeFileFormat = "blocktype_%d";

/* Define  the folder name for poly data files. */
static const char* mPolyFolder = "pred";

/* Define a file name format for eph data files. */
static const char* mEphFolder = "eph";

/* Define a file name format for meta data files. */
static const char* mMetaFolder = "meta";

/* Define a file path separator. */
static const char* mPathSeparator = "/";

static const char* mConfigFile = "rxn_config_data";

/* Define a version that is 8 bytes long. Use 4 U16's to denote version n.n.n.n. */
static U16 RXN_Version[] = {0x0000, 0x0000, 0x0000, 0x0001};

// Utility function that can init a file to a given pattern. THIS PATTERN IS IMPORTANT see DISK_OPS
void InitFile(FILE* fileStream, size_t totalBytes, char value);

typedef enum ABS_OP
{
  PRED = 0,
  EPH,
  CONF,
  DBG,
  FREQCHAN,
  BLOCKTYPE
} abs_op_t;

typedef struct ABS_DATA
{
  unsigned prn;         // satellite number
  size_t offset;        // byte offset in file
  size_t size;          // size of p_data in bytes
  unsigned char read;   // 1 to read, 0 to write
  abs_op_t type;        // file type
  void* p_data;         // data to read/write
} abs_data_t;

size_t fileSize(abs_data_t data)  //total file size
{
  size_t size = (data.prn  < RXN_CONSTANT_NUM_GPS_PRNS) ?  RXN_NAVSTAR_FILE_SIZE 
    : RXN_GLONASS_FILE_SIZE;
  switch(data.type)
  {
  case PRED:
    return RXN_PRED_FILE_SIZE;
  case EPH:
    return size;
  case CONF:
    return RXN_CONSTANT_CONFIG_RECORD_SIZE;
  case FREQCHAN:
    return RXN_CONSTANT_FREQCHAN_RECORD_SIZE;
  case BLOCKTYPE:
    return RXN_CONSTANT_BLOCKTYPE_RECORD_SIZE;
  default :
    return 0;
  }
}

const char* filePath(abs_op_t type)
{
  static char strRxnPath[MAX_FILE_PATH_LEN];

    switch(type)
    {
      case PRED:
        strcpy(strRxnPath,&mPolyFormat[0]);
        break;
      case EPH:
        strcpy(strRxnPath,&mEphFormat[0]);
        break;
      case CONF:
        strcpy(strRxnPath,&mConfigFullPath[0]);
        break;
      case FREQCHAN:
        strcpy(strRxnPath,&mFreqChanFormat[0]);
        break;
      case BLOCKTYPE:
        strcpy(strRxnPath,&mBlockTypeFormat[0]);
        break;
      default :
        break;
    }
    return strRxnPath;
}


/* Customer declared method that will be used to initialize folders, critical sections,
* etc as required. 
*
* ** Note that this function is not declared within RXN_API_calls.h as are other
* functions within this file. 
*
* ** Further note that this function will NOT be called by RXN libraries. Integrators
* should call this function during thier startup code.*/
U16 RXN_Init_Abstraction(char pantryPath[RXN_MSL_MAX_PATH])
{
#if defined (ANDROID) || defined (LINUX)
    /* Set default file permissions to the equivalent to chmod 771 */
    umask(006);
#endif
    /* Try to create required folders. 
    * Ignore resulting error if they already exist. */
    //clean up the file path store
    memset((void*) mPolyFormat, 0, sizeof(char) * MSL_MAX_PATH);
    memset((void*) mEphFormat, 0, sizeof(char) * MSL_MAX_PATH);
	memset((void*) mFreqChanFormat, 0, sizeof(char) * MSL_MAX_PATH);
	memset((void*) mBlockTypeFormat, 0, sizeof(char) * MSL_MAX_PATH);
    memset((void*) mConfigFullPath, 0, sizeof(char) * MSL_MAX_PATH);

    //create the root folder for pantry
    MSL_CreateDir(pantryPath);

    /* prepare the folder pathes */
    strncpy(mPolyFormat, pantryPath, MSL_MAX_PATH);
    strncpy(mEphFormat, pantryPath, MSL_MAX_PATH);
	strncpy(mFreqChanFormat, pantryPath, MSL_MAX_PATH);

    /* MSL_MAX_FULL_FILE_PATH factors in 5 extra chars for path separators*/
    strcat(mPolyFormat, mPathSeparator);
    strcat(mEphFormat, mPathSeparator);
	strcat(mFreqChanFormat, mPathSeparator);

    strncat(mPolyFormat, mPolyFolder, MSL_MAX_FOLDER_NAME);
    strncat(mEphFormat, mEphFolder, MSL_MAX_FOLDER_NAME);
	strncat(mFreqChanFormat, mMetaFolder, MSL_MAX_FOLDER_NAME);


    /*create the pantry folders */
    MSL_CreateDir(mPolyFormat);
    MSL_CreateDir(mEphFormat);
	MSL_CreateDir(mFreqChanFormat);
	
    /* prepare the pantry file format */
    strcat(mPolyFormat, mPathSeparator);
    strcat(mEphFormat, mPathSeparator);
	strcat(mFreqChanFormat, mPathSeparator);
	
	/* Separate the frequency slot format from block type */
	strncpy(mBlockTypeFormat, mFreqChanFormat, MSL_MAX_PATH);

    strncat(mPolyFormat, mPolyFileFormat, MSL_MAX_FILE_NAME);
    strncat(mEphFormat, mEphFileFormat, MSL_MAX_FILE_NAME);
	strncat(mFreqChanFormat, mFreqChanFileFormat, MSL_MAX_FILE_NAME);
	strncat(mBlockTypeFormat, mBlockTypeFileFormat, MSL_MAX_FILE_NAME);

    /*prepare the rxn_config_data file path */
    strncpy(mConfigFullPath, pantryPath, MSL_MAX_PATH);

    /* MSL_MAX_FULL_FILE_PATH factors in 5 extra chars for path separators*/
    strcat(mConfigFullPath, mPathSeparator);
    strncat(mConfigFullPath, mConfigFile, MSL_MAX_FILE_NAME);

    return RXN_SUCCESS;
}

/* Customer declared method that will be used to delete critical sections,
* etc as required. 
*
* ** Note that this function is not declared within RXN_API_calls.h. 
*
* ** Further note that this function will NOT be called by RXN libraries. Integrators
* should call this function during thier shutdown code.*/
U16 RXN_Uninit_Abstraction(void)
{
    /* No cleanup required at present. */
    return RXN_SUCCESS;
}

U16 DISK_OPS(abs_data_t data)
{
  char szFilePath[MAX_FILE_PATH_LEN];   /* File path. */
  FILE* fStream;          /* File stream. */
  size_t bytesProcessed = 0; /* Track the bytes written. */
  size_t totalFileSize;      /* Total size of the file for init. */
  size_t offset;

  sprintf(szFilePath, filePath(data.type), data.prn);

  fStream = fopen(szFilePath, "r+b");       //open for read write if file exists
  if(fStream == NULL)
  {
    fStream = fopen(szFilePath, "w+b");     //create new file
    totalFileSize = sizeof(RXN_Version) + fileSize(data);
    ////// IMPORTANT //////////
    InitFile(fStream, totalFileSize,  0xff);//set RXN approved init pattern !!!!<------VITALLY IMPORTANT
    ////// IMPORTANT //////////
    fwrite(RXN_Version, 1, sizeof(RXN_Version), fStream);
    fseek(fStream, 0L, SEEK_SET);           //be kind rewind
  }

  offset = data.offset + sizeof(RXN_Version);
  fseek(fStream,  (long)offset, SEEK_SET);

  if (data.read == 1)
    bytesProcessed = fread(data.p_data, 1, data.size, fStream);
  else
    bytesProcessed = fwrite(data.p_data, 1, data.size, fStream);
  
  fclose(fStream);

  if(bytesProcessed != data.size)
    return RXN_FAIL;
  else
    return RXN_SUCCESS;
}

/** 
 * All calls below follow the same pattern. 
 *
 * RXN_Mem_Read/Write_Eph stores the following data:
 * |index|eph0|eph2|eph3|...|eph14| Note ephemerides for GPS / Glonass have different sizes
 *
 * index refers to the index into the slot for a new ephemeris
 *
 * The other calls RXN_Mem_Read/Write_Preditiction use the following storage:
 * |seed0|seed1|poly0|poly1|....|poly47| 
 * 
 * The abs_data_t struct was used to illustrate more cleanly the differences between
 * the calls.
 */
 
U16 RXN_Mem_Read(
  unsigned prn, size_t offset, size_t size, abs_op_t type, void* p_data)
{
  abs_data_t data;
  data.offset = offset;
  data.p_data = p_data;
  data.read = 1;
  data.prn = prn;
  data.size = size;
  data.type = type;
  return DISK_OPS(data);  
}

U16 RXN_Mem_Write(
  unsigned prn, size_t offset, size_t size, abs_op_t type, const void* p_data)
{
  abs_data_t data;
  data.offset = offset;
  data.p_data = (void*)p_data;    // Need cast for DISK_OPS
  data.read = 0;
  data.prn = prn;
  data.size = size;
  data.type = type;
  return DISK_OPS(data);  
}

U16 RXN_Mem_Read_Prediction(
  unsigned prn, size_t offset, size_t size, void* p_data)
{
	U08 status;
	U08 cSIdx = RXN_MSL_CS_POLY_FILE + prn - 1;
	
    /* Enter the CS protecting poly file I/O. */
    MSL_EnterBlock(cSIdx);		/* Enter Block by prn */
	
	status = RXN_Mem_Read(prn, offset, size, PRED, p_data);
	
    // Leave the block protecting poly file access.
    MSL_LeaveBlock(cSIdx);
	
	return status;
}

U16 RXN_Mem_Write_Prediction(
  unsigned prn, size_t offset, size_t size, const void* p_data)
{
	U08 status;
	U08 cSIdx = RXN_MSL_CS_POLY_FILE + prn - 1;
	
    /* Enter the CS protecting poly file I/O. */
    MSL_EnterBlock(cSIdx);		/* Enter Block by prn */
	
	status = RXN_Mem_Write(prn, offset, size, PRED, p_data);

    // Leave the block protecting poly file access.
    MSL_LeaveBlock(cSIdx);
	
	return status;	
}

U16 RXN_Mem_Read_Eph(
  unsigned prn, size_t offset, size_t size, void* p_data)
{
	U08 status;
	U08 cSIdx = RXN_MSL_CS_EPH_FILE + prn - 1;
	
    /* Enter the CS protecting poly file I/O. */
    MSL_EnterBlock(cSIdx);		/* Enter Block by prn */
	
	status = RXN_Mem_Read(prn, offset, size, EPH, p_data); 
  
    // Leave the block protecting poly file access.
    MSL_LeaveBlock(cSIdx);
	
	return status;
}

U16 RXN_Mem_Write_Eph(
  unsigned prn, size_t offset, size_t size, const void* p_data)
{
	U08 status;
	U08 cSIdx = RXN_MSL_CS_EPH_FILE + prn - 1;
	
    /* Enter the CS protecting poly file I/O. */
    MSL_EnterBlock(cSIdx);		/* Enter Block by prn */
	
	status = RXN_Mem_Write(prn, offset, size, EPH, p_data); 
  
    // Leave the block protecting poly file access.
    MSL_LeaveBlock(cSIdx);
	
	return status;
}

U16 RXN_Mem_Write_Config(size_t offset, size_t size,  const void* p_data)
{
  return RXN_Mem_Write(0, offset, size, CONF, p_data);
}

U16 RXN_Mem_Read_Config(size_t offset, size_t size,void* p_data)
{
  return RXN_Mem_Read(0, offset, size, CONF, p_data);
}

U16 RXN_Mem_Write_Freq_Chan(unsigned prn, size_t offset, size_t size, const void* p_data)
{
  return RXN_Mem_Write(prn, offset, size, FREQCHAN, p_data);
}

U16 RXN_Mem_Read_Freq_Chan(unsigned prn, size_t offset, size_t size, void* p_data)
{
  return RXN_Mem_Read(prn, offset, size, FREQCHAN, p_data);
}

U16 RXN_Mem_Write_Block_Type(unsigned prn, size_t offset, size_t size,  const void* p_data)
{
  return RXN_Mem_Write(prn, offset, size, BLOCKTYPE, p_data);
}

U16 RXN_Mem_Read_Block_Type(unsigned prn, size_t offset, size_t size,void* p_data)
{
  return RXN_Mem_Read(prn, offset, size, BLOCKTYPE, p_data);
}

void InitFile(FILE* fileStream, size_t totalBytes, char value)
{
  size_t x;  
  fseek(fileStream, 0L, SEEK_SET);// Always start at the file beginning. 
  for(x=0; x<totalBytes; x++)
    fwrite(&value, 1, sizeof(char), fileStream);
  fseek(fileStream, 0L, SEEK_SET);// Reset the file position pointer.
  return;
}

/***********************************
* MSL-specific abstraction calls. *
***********************************/

void RXN_MSL_Factory_Reset(enum MSL_Reset_Type resetType)
{
    char polyDir[MSL_MAX_FULL_FILE_PATH];
    char ephDir[MSL_MAX_FULL_FILE_PATH];
    extern MSL_Config_t gConfig;

    //clean up the file path store
    memset((void*) polyDir, 0, sizeof(char) * MSL_MAX_PATH);
    memset((void*) ephDir, 0, sizeof(char) * MSL_MAX_PATH);

    if(resetType == MSL_RESET_INVALID)
    {
        return;
    }

    if((resetType == MSL_RESET_SEED) || (resetType == MSL_RESET_SEED_AND_TIME))
    {
        //prepare the folder pathes
        strncpy(polyDir, gConfig.pantryPath, MSL_MAX_PATH);
        strncpy(ephDir, gConfig.pantryPath, MSL_MAX_PATH);

        strcat(polyDir, mPathSeparator);
        strcat(ephDir, mPathSeparator);

        strncat(polyDir, mPolyFolder, MSL_MAX_FOLDER_NAME);
        strncat(ephDir, mEphFolder, MSL_MAX_FOLDER_NAME);

        MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
            "RXN_MSL_Factory_Reset: poly path= %s", polyDir);	
        MSL_LogStr(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08,
            "RXN_MSL_Factory_Reset: eph path= %s", ephDir);	

        MSL_SetAbort(TRUE);
        MSL_EnterBlock(RXN_MSL_CS_Base);
        MSL_CleanDir(polyDir);
        MSL_CleanDir(ephDir);
        MSL_LeaveBlock(RXN_MSL_CS_Base);
        MSL_SetAbort(FALSE);
    }

    if((resetType == MSL_RESET_TIME) || (resetType == MSL_RESET_SEED_AND_TIME))
    {
        MSL_Time_Reset();
    }
    MSL_SM_FactoryReset();
    MSL_Log(TRUE, TRUE, RXN_LOG_SEV_INFO, RXN_LOG_ZONE08, "RXN_MSL_Factory_Reset: Factory Reset has occured.");
    MSL_LogFlt(TRUE, TRUE, RXN_LOG_SEV_TRACE, RXN_LOG_ZONE08, "RXN_MSL_Factory_Reset: Reset type:%1.0f.", resetType);

    MSL_SetNextSystemEvent(MSL_EVENT_FACTORY_RESET);
}

void RXN_Mem_Write_Debug(const U16 size, const U08* p_data)
{
    CHAR logEntry[MSL_MAX_LOG_ENTRY];
    CHAR fullLogEntry[MSL_MAX_LOG_ENTRY];

    memset((void*) logEntry, 0, sizeof(char) * MSL_MAX_LOG_ENTRY);
    memcpy(&logEntry[0], p_data, size);
    snprintf(fullLogEntry,MSL_MAX_LOG_ENTRY, "%s %s", "RXN_API_Log:", logEntry);
    MSL_LogSimple(fullLogEntry);
}


#if defined(HEAP_MONITOR)
void Write_Heap_Size(size_t size)
{
   FILE * pFile;

   pFile = fopen(heap_size_filename, "w");
   if (pFile != NULL)
   {
   fprintf(pFile, "Maximum Heap Size: %d (bytes)", size);
     fclose(pFile);
   }
}
#endif

void* RXN_Mem_Alloc(size_t size)
{
#if defined(HEAP_MONITOR)
  // The size of the requested object.
  size_t object_size;

  // The size of the new, allocated object.
  size_t new_size;

  // The pointer to return back to the caller.
  size_t* ptr = NULL;
  
  // Keep track of the current heap size.
  heap_size += size;
  
  // Update the watermark, if the heap size is larger than the watermark.
  if (heap_size > heap_size_watermark) 
  {
    heap_size_watermark = heap_size;
  Write_Heap_Size(heap_size_watermark);
  }
  
  // Allocate memory and to allow the size to be pre-padded.
  object_size = size;
  new_size = sizeof(object_size) + size;  
  ptr = (size_t*) malloc(new_size);
  
  // Set the size of the object. Then advance the pointer to the start
  // of the next block of memory, and return the pointer location back 
  // to the caller.
  *ptr = object_size;
  ++ptr;  
  
  return ptr;
#else
  return malloc(size);
#endif
}

void RXN_Mem_Free(void* p)
{
#if defined(HEAP_MONITOR)
  // The pointer to requested object.
  size_t* ptr = (size_t*) p;

  // The size of the requested object.
  size_t object_size;

  if (p != NULL)
  {
    // Since a size (size_t) is padded in front, the actual position is equivalent to
  // the p - sizeof(size_t).
  --ptr;
  object_size = *ptr;

  // Keep track of the current heap size.
  heap_size -= object_size;

  // Release the complete memory.
  free(ptr);
  }
#else
  free(p);
#endif
}
