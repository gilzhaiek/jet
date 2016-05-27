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
## Control List (“CCL”). The assurances provided for herein are furnished in  *
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
*   FILE NAME:      mcp_hal_fs.c
*
*   DESCRIPTION:    This file contain implementation of file system in LINUX
*
*   AUTHOR:         Udi Ron
*
\*******************************************************************************/


/********************************************************************************
 *
 * Include files
 *
 *******************************************************************************/
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <errno.h>
#include <fcntl.h>
#include <time.h>
#include <sys/types.h>
#include <dirent.h>
#include <string.h>
#include <unistd.h>
#include <sys/time.h>

#include "mcp_hal_defs.h"
#include "mcp_hal_fs.h"
#include "mcp_hal_log.h"
#include "mcp_unicode.h"
#include "mcp_linux_line_parser.h"
#include "mcp_hal_memory.h"
#include "mcp_hal_string.h"
#include "mcp_hal_types.h"
#include "mcpf_defs.h"

/********************************************************************************
 *
 * Constants 
 *
 *******************************************************************************/
#define MCP_HAL_FS_MONTH_LIST_MAX         12


/********************************************************************************
 *
 * Data Structures
 *
 *******************************************************************************/

 typedef struct _tagMcpHalFsDirHandle
 {
	McpBool				busy;
	McpU8				path[MCP_HAL_CONFIG_FS_MAX_PATH_LEN_CHARS + 1];
	DIR*			searchHandle;
} _McpHalFsDirHandle;


/********************************************************************************
 *
 * Globals
 *
 *******************************************************************************/


static _McpHalFsDirHandle	_mcpHalFs_DirStructureArray[MCP_HAL_CONFIG_FS_MAX_NUM_OF_OPEN_DIRS];

/* Full name (path+name), null terminated */
static McpU8                _mcpHalFs_lfilename[MCP_HAL_CONFIG_FS_MAX_PATH_LEN_CHARS+ MCP_HAL_CONFIG_FS_MAX_FILE_NAME_LEN_CHARS+ 1];


/********************************************************************************
 *
 * Internal function prototypes
 *
 *******************************************************************************/

static McpU32	 _McpHalFs_GetFreeScpaceInDirStructurArray(void);
static McpHalFsStatus	_McpHalFs_ConvertLinuxErrorToFsError();
static McpHalFsStatus	_McpHalFs_CheckIfDirHandleIsInValidAndRange(const McpHalFsDirDesc dirDesc);
static void _McpHalFs_ExtractPermissions(mode_t file_mode, McpHalFsStat* fileStat);


/********************************************************************************
 *
 * Function definitions
 *
 *******************************************************************************/
/*-------------------------------------------------------------------------------
 * MCP_HAL_FS_Init()
 *
 *	Synopsis:  int FS set zero to all FileStructureArray and _mcpHalFs_DirStructureArray
 * 
 * 	Returns:
 *		MCP_HAL_FS_STATUS_SUCCESS - Operation is successful.
 *
 */
McpHalFsStatus MCP_HAL_FS_Init(void)
{
	memset((void*)_mcpHalFs_DirStructureArray, 0, (MCP_HAL_CONFIG_FS_MAX_NUM_OF_OPEN_DIRS * sizeof(_McpHalFsDirHandle)));

	return MCP_HAL_FS_STATUS_SUCCESS;
}

/*-------------------------------------------------------------------------------
 * MCP_HAL_FS_DeInit()
 *
 *	Synopsis:  
 * 
 * 	Returns:
 *		MCP_HAL_FS_STATUS_SUCCESS - Operation is successful.
 *
 */
McpHalFsStatus MCP_HAL_FS_DeInit(void)
{
	return MCP_HAL_FS_STATUS_SUCCESS;
}

/*---------------------------------------------------------------------------
 *            _McpHalFs_GetFreeScpaceInDirStructurArray
 *---------------------------------------------------------------------------
 *
 * Synopsis:  get the first free space in Dir structure array
 *
 * Return:    int - cell number 
 *
 */ 
static McpU32 _McpHalFs_GetFreeScpaceInDirStructurArray(void)
{
	McpU32 index;

    for (index = 0;index < MCP_HAL_CONFIG_FS_MAX_NUM_OF_OPEN_DIRS;index++)
    {
        if (_mcpHalFs_DirStructureArray[index].busy == 0)
        {
            _mcpHalFs_DirStructureArray[index].busy = 1;
            return index;
        }
    }
    return (McpU32)MCP_HAL_FS_INVALID_DIRECTORY_DESC;
}

/*---------------------------------------------------------------------------
 *            FsCheckIfDirHandleIsInRange
 *---------------------------------------------------------------------------
 *
 * Synopsis:  get the first free space in Dir structure array
 *
 * 	Returns:	MCP_HAL_FS_STATUS_SUCCESS - if successful,
 *				MCP_HAL_FS_STATUS_ERROR_DIRECTORY_HANDLE_OUT_OF_RANGE - out of range.   
 *				MCP_HAL_FS_STATUS_ERROR_INVALID_DIRECTORY_HANDLE_VALUE - Invalid handle. 
 */ 

static McpHalFsStatus	_McpHalFs_CheckIfDirHandleIsInValidAndRange(const McpHalFsDirDesc dirDesc)
{
    if (dirDesc >= MCP_HAL_CONFIG_FS_MAX_NUM_OF_OPEN_DIRS)
        return MCP_HAL_FS_STATUS_ERROR_DIRECTORY_HANDLE_OUT_OF_RANGE;

	if (_mcpHalFs_DirStructureArray[dirDesc].busy == 0)
		return MCP_HAL_FS_STATUS_ERROR_INVALID_DIRECTORY_HANDLE_VALUE;

	return MCP_HAL_FS_STATUS_SUCCESS;
}

/*---------------------------------------------------------------------------
 *            _McpHalFs_ConvertLinuxErrorToFsError
 *---------------------------------------------------------------------------
 *
 * Synopsis:  convert Linux error to fs error
 *
 * Return:    McpHalFsStatus
 *
 */ 
static McpHalFsStatus	_McpHalFs_ConvertLinuxErrorToFsError()
{
	switch(errno)
	{
	
	case EACCES:
		return MCP_HAL_FS_STATUS_ERROR_ACCESS_DENIED;        /* Permission denied */
	case ENOTEMPTY:
		return MCP_HAL_FS_STATUS_ERROR_DIRECTORY_NOT_EMPTY;  /* Permission denied */
	case EAGAIN:
		return MCP_HAL_FS_STATUS_ERROR_GENERAL;              /* No more processes or not enough memory or maximum nesting level reached */
	case EBADF:
		return MCP_HAL_FS_STATUS_ERROR_FILE_HANDLE;          /* Bad file number/ handle */
	case ECHILD:
		return MCP_HAL_FS_STATUS_ERROR_GENERAL;              /* No spawned processes */
	case EDEADLOCK:
		return MCP_HAL_FS_STATUS_ERROR_GENERAL;              /* Resource deadlock would occur */
	case EDOM:
		return MCP_HAL_FS_STATUS_ERROR_GENERAL;              /* Math argument */
	case EEXIST:
		return MCP_HAL_FS_STATUS_ERROR_EXISTS;               /* File exists */
	case EINVAL:
		return MCP_HAL_FS_STATUS_ERROR_INVALID;              /* Invalid argument */
	case EMFILE:
		return MCP_HAL_FS_STATUS_ERROR_GENERAL;              /* Too many open files */
	case ENOENT:
		return MCP_HAL_FS_STATUS_ERROR_NOTFOUND;             /* No such file or directory */
	case ENOEXEC:
		return MCP_HAL_FS_STATUS_ERROR_GENERAL;              /* Exec format error */
	case ENOSPC:
		return MCP_HAL_FS_STATUS_ERROR_OUT_OF_SPACE;         /* No space left on device */
	case ERANGE:
		return MCP_HAL_FS_STATUS_ERROR_GENERAL;              /* Result too large */
	case EXDEV:
		return MCP_HAL_FS_STATUS_ERROR_GENERAL;              /* Cross-device link */
	case ENOMEM:
		return MCP_HAL_FS_STATUS_ERROR_GENERAL;              /* Not enough memory */
	case -1:
		return MCP_HAL_FS_STATUS_ERROR_FILE_NOT_CLOSE;       /* file open */
	case 0:
		return MCP_HAL_FS_STATUS_SUCCESS;
	default:
		return MCP_HAL_FS_STATUS_ERROR_GENERAL;
	}
}

/*---------------------------------------------------------------------------
 *            MCP_HAL_FS_Open
 *---------------------------------------------------------------------------
 *
 * Synopsis:  the file name should include all file-system and drive specification as
 *			   used by convention in the target platform.
 *
 * Return:    MCP_HAL_FS_STATUS_SUCCESS if success, MCP_HAL_FS_STATUS_ERROR_OPENING_FILE otherwise
 *
 */
McpHalFsStatus MCP_HAL_FS_Open(const McpUtf8* fullPathFileName, McpHalFsOpenFlags flags, McpHalFsFileDesc *fd)
{
        /* TODO (a0798989): support UTF-8 */
        int filehandle;
	int openFlags;

	openFlags = 0;
	*fd = MCP_HAL_FS_INVALID_FILE_DESC;
	
	if(flags & MCP_HAL_FS_O_APPEND)
		openFlags = openFlags | O_APPEND;
	if(flags & MCP_HAL_FS_O_CREATE)
		openFlags = openFlags | O_CREAT;
	if(flags & MCP_HAL_FS_O_EXCL)
		openFlags = openFlags | O_EXCL;
	if(flags & MCP_HAL_FS_O_RDONLY)
		openFlags = openFlags | O_RDONLY;
	if(flags & MCP_HAL_FS_O_RDWR)
		openFlags = openFlags | O_RDWR;
	if(flags & MCP_HAL_FS_O_TRUNC)
		openFlags = openFlags | O_TRUNC;
	if(flags & MCP_HAL_FS_O_WRONLY)
		openFlags = openFlags | O_WRONLY;

	filehandle = open( (char *)fullPathFileName, openFlags, S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP);

	if ( -1 == filehandle ) {
        MCP_HAL_LOG_INFO(__FILE__, __LINE__, MCP_HAL_LOG_MODULE_TYPE_HAL_FS, ("MCP_HAL_FS_Open: failed to open %s", fullPathFileName));
		return _McpHalFs_ConvertLinuxErrorToFsError();
	}

    MCP_HAL_LOG_INFO(__FILE__, __LINE__, MCP_HAL_LOG_MODULE_TYPE_HAL_FS, ("MCP_HAL_FS_Open: opened %s", fullPathFileName));

	*fd = filehandle;
	return MCP_HAL_FS_STATUS_SUCCESS;
}


/*---------------------------------------------------------------------------
 *            MCP_HAL_FS_Close
 *---------------------------------------------------------------------------
 *
 * Synopsis:  close file
 *
 * Return:    MCP_HAL_FS_STATUS_SUCCESS if success,
 *			  other - if failed.
 *
 */
McpHalFsStatus MCP_HAL_FS_Close( const McpHalFsFileDesc fd )
{
	if(close(fd) != 0)
		return _McpHalFs_ConvertLinuxErrorToFsError();

	return MCP_HAL_FS_STATUS_SUCCESS;
}

/*---------------------------------------------------------------------------
 *            MCP_HAL_FS_Read
 *---------------------------------------------------------------------------
 *
 * Synopsis:  read file
 *
 * Return:    MCP_HAL_FS_STATUS_SUCCESS if success,
 *			  other - if failed.
 *
 */
McpHalFsStatus MCP_HAL_FS_Read ( const McpHalFsFileDesc fd, void* buf, McpU32 nSize, McpU32 *numRead )
{
    int sNumRead;

    sNumRead = read(fd,buf,nSize);

    if (sNumRead < 0) {
            MCP_HAL_LOG_ERROR(__FILE__, __LINE__, MCP_HAL_LOG_MODULE_TYPE_HAL_FS, ("MCP_HAL_FS_Read: read failed: %s\n", strerror(errno)));
            *numRead = 0;
            return _McpHalFs_ConvertLinuxErrorToFsError();
    }

    *numRead = (McpU32)sNumRead;

	return MCP_HAL_FS_STATUS_SUCCESS;
}

/*---------------------------------------------------------------------------
 *            MCP_HAL_FS_Write
 *---------------------------------------------------------------------------
 *
 * Synopsis:  write file
 *
 * 	Returns:	MCP_HAL_FS_STATUS_SUCCESS - if successful,
 *				other - if failed.
 */
McpHalFsStatus MCP_HAL_FS_Write( const McpHalFsFileDesc fd, void* buf, McpU32 nSize, McpU32 *numWritten )
{
    int sNumWritten;

    sNumWritten = write(fd,buf,nSize);

    if (sNumWritten < 0) {
        MCP_HAL_LOG_ERROR(__FILE__, __LINE__, MCP_HAL_LOG_MODULE_TYPE_HAL_FS, ("MCP_HAL_FS_Write: write failed: %s\n", strerror(errno)));
        *numWritten = 0;
        return _McpHalFs_ConvertLinuxErrorToFsError();
    }

    *numWritten = (McpU32)sNumWritten;

	return MCP_HAL_FS_STATUS_SUCCESS;
}

/*---------------------------------------------------------------------------
 *            MCP_HAL_FS_Tell
 *---------------------------------------------------------------------------
 *
 * Synopsis:  gets the current position of a file pointer.
 *
 * 	Returns:	MCP_HAL_FS_STATUS_SUCCESS - if successful,
 *				other -  Operation failed.
 */
McpHalFsStatus MCP_HAL_FS_Tell( const McpHalFsFileDesc fd, McpU32 *curPosition )
{
	off_t offset;

	offset = lseek(fd, 0, SEEK_CUR);
	if (offset != (off_t)-1){
		*curPosition = offset;
		return MCP_HAL_FS_STATUS_SUCCESS;
	}

	return _McpHalFs_ConvertLinuxErrorToFsError();
}

/*---------------------------------------------------------------------------
 *            MCP_HAL_FS_Seek
 *---------------------------------------------------------------------------
 *
 * Synopsis:  moves the file pointer to a specified location.
 *
 * 	Returns:	MCP_HAL_FS_STATUS_SUCCESS - if successful,
 *				other -  Operation failed.
 */
McpHalFsStatus MCP_HAL_FS_Seek( const McpHalFsFileDesc fd, McpS32 offset, McpHalFsSeekOrigin from )
{
	McpS32 origin;

	switch(from)
	{
	case MCP_HAL_FS_CUR:
		origin = SEEK_CUR;
		break;
	case MCP_HAL_FS_END:
		origin = SEEK_END;
	    break;
	case MCP_HAL_FS_START:
		origin = SEEK_SET;
	    break;
	default:
        MCP_HAL_LOG_ERROR(__FILE__, __LINE__, MCP_HAL_LOG_MODULE_TYPE_HAL_FS, ("MCP_HAL_FS_Seek: bad MCP_HAL_FsSeekOrigin: %d\n", from));
		return MCP_HAL_FS_STATUS_ERROR_GENERAL;
	    break;
	}

	if(lseek(fd,offset,origin) == -1 )
		return _McpHalFs_ConvertLinuxErrorToFsError();

	return MCP_HAL_FS_STATUS_SUCCESS;
}


/*---------------------------------------------------------------------------
 *            MCP_HAL_FS_Flush
 *---------------------------------------------------------------------------
 *
 * Synopsis:  flush write buffers from memory to file
 *
 * 	Returns:	MCP_HAL_FS_STATUS_SUCCESS - if successful,
 *				MCP_HAL_FS_STATUS_ERROR_GENERAL - if failed.
 */
McpHalFsStatus MCP_HAL_FS_Flush( const McpHalFsFileDesc fd )
{
	if(fsync(fd) !=0)
		return _McpHalFs_ConvertLinuxErrorToFsError();		

	return MCP_HAL_FS_STATUS_SUCCESS;
}


/*---------------------------------------------------------------------------
 *            MCP_HAL_FS_Stat
 *---------------------------------------------------------------------------
 *
 * Synopsis:  get information of a file or folder - name, size, type, 
 *            created/modified/accessed time, and Read/Write/Delete         . 
 *            access permissions.
 *
 * Returns:	MCP_HAL_FS_STATUS_SUCCESS - if successful,
 *				other -  Operation failed.
 */
McpHalFsStatus MCP_HAL_FS_Stat( const McpUtf8* fullPathName, McpHalFsStat* fileStat )
{
	/* TODO (a0798989): support UTF-8 */

        struct stat buf;
	
	if (stat((char *)fullPathName, &buf) == 0)
	{
		/* Get file/dir size */ 
	        fileStat->size = buf.st_size;
		
	        /* Set either file or directory field */
	        fileStat->type = S_ISDIR(buf.st_mode) ? MCP_HAL_FS_DIR : MCP_HAL_FS_FILE;
		
	        /* if true read only */
	        fileStat->isReadOnly = (buf.st_mode & (S_IRUSR|S_IWUSR)) == S_IRUSR ? MCP_TRUE: MCP_FALSE;

	        /* Extract user/group/other file or directory access permissions */
	        _McpHalFs_ExtractPermissions(buf.st_mode, fileStat);
	        
		/* Extract file creation fields (in UTC) */
	        MCP_HAL_FS_ExtractDateAndTime(buf.st_ctime, &fileStat->cTime);
	        
	        /* Extract file modification fields (in UTC) */
	        MCP_HAL_FS_ExtractDateAndTime(buf.st_mtime, &fileStat->mTime);
	        
	        /* Extract file access fields (in UTC) */
	        MCP_HAL_FS_ExtractDateAndTime(buf.st_atime, &fileStat->aTime);

            /* set device id */
            fileStat->deviceId = buf.st_dev;

        return MCP_HAL_FS_STATUS_SUCCESS;
    }

    MCP_HAL_LOG_INFO(__FILE__, __LINE__, MCP_HAL_LOG_MODULE_TYPE_HAL_FS, ("Can't find file %s", fullPathName));
	
	return _McpHalFs_ConvertLinuxErrorToFsError();
}

/*---------------------------------------------------------------------------
 *            MCP_HAL_FS_Mkdir
 *---------------------------------------------------------------------------
 *
 * Synopsis:  make dir
 *
 * 	Returns:	MCP_HAL_FS_STATUS_SUCCESS - if successful,
 *				other - if failed 
 */
McpHalFsStatus MCP_HAL_FS_Mkdir( const McpUtf8 *dirFullPathName ) 
{
        /* TODO (a0798989): support UTF-8 */

	/* Allow everyone to read dir, but only owner to change its contents */
	if(mkdir((char *)dirFullPathName, 0755) == 0)
		return MCP_HAL_FS_STATUS_SUCCESS;

	return _McpHalFs_ConvertLinuxErrorToFsError();
}

/*---------------------------------------------------------------------------
 *            MCP_HAL_FS_Rmdir
 *---------------------------------------------------------------------------
 *
 * Synopsis:  remove dir
 *
 * 	Returns:	MCP_HAL_FS_STATUS_SUCCESS - if successful,
 *				other - if failed 
 */
McpHalFsStatus MCP_HAL_FS_Rmdir( const McpUtf8 *dirFullPathName )
{
	/* TODO (a0798989): support UTF-8 */

        if(rmdir((char *)dirFullPathName) == 0)
		return MCP_HAL_FS_STATUS_SUCCESS;

	return _McpHalFs_ConvertLinuxErrorToFsError();
}
/*---------------------------------------------------------------------------
 *            MCP_HAL_FS_OpenDir
 *---------------------------------------------------------------------------
 *
 * Synopsis:  open a directory for reading,
 *
 * 	Returns:	MCP_HAL_FS_STATUS_SUCCESS - if successful,
 *				MCP_HAL_FS_STATUS_ERROR_INVALID_DIRECTORY_HANDLE_VALUE - if failed 
 */
McpHalFsStatus MCP_HAL_FS_OpenDir( const McpUtf8 *dirFullPathName, McpHalFsDirDesc *dirDesc )
{
	McpU32	directoryHandle;

	*dirDesc = (McpHalFsDirDesc)MCP_HAL_FS_INVALID_DIRECTORY_DESC;

	if((directoryHandle = _McpHalFs_GetFreeScpaceInDirStructurArray()) == MCP_HAL_FS_INVALID_DIRECTORY_DESC)
		return MCP_HAL_FS_STATUS_ERROR_MAX_DIRECTORY_HANDLE;

	strncpy((char*)_mcpHalFs_DirStructureArray[directoryHandle].path,
								(char*)dirFullPathName,
								MCP_HAL_CONFIG_FS_MAX_PATH_LEN_CHARS);

	_mcpHalFs_DirStructureArray[directoryHandle].searchHandle = opendir((char *)dirFullPathName); 	
 	if(_mcpHalFs_DirStructureArray[directoryHandle].searchHandle == NULL)
	{
        MCP_HAL_LOG_INFO(__FILE__, __LINE__, MCP_HAL_LOG_MODULE_TYPE_HAL_FS, ("MCP_HAL_FS_OpenDir: opendir failed: %s\n", strerror(errno)));
 		return MCP_HAL_FS_STATUS_ERROR_INVALID_DIRECTORY_HANDLE_VALUE;
	}

	*dirDesc = directoryHandle;
	return 	MCP_HAL_FS_STATUS_SUCCESS;
}

/*-------------------------------------------------------------------------------
 * MCP_HAL_FS_ReadDir()
 *
 *	Synopsis:  get first/next file name in a directory. return the full path of the file
 *
 * 	Returns:
 *		MCP_HAL_FS_STATUS_SUCCESS - Operation is successful.
 *
 *		MCP_HAL_FS_STATUS_ERROR_FIND_NEXT_FILE -  Operation failed.
 */

McpHalFsStatus MCP_HAL_FS_ReadDir( const McpHalFsDirDesc dirDesc, McpUtf8 **fileName )
{
	/* TODO (a0798989): support UTF-8 */

        McpHalFsStatus	error;
	struct dirent *fileInfo	= NULL;
	
	/* check if file directory valid and in range */
	if((error = _McpHalFs_CheckIfDirHandleIsInValidAndRange(dirDesc)) != 0)
		return error;

	fileInfo = readdir(_mcpHalFs_DirStructureArray[dirDesc].searchHandle);
	if (fileInfo == NULL){
			return MCP_HAL_FS_STATUS_ERROR_FIND_NEXT_FILE;
	}

	/* clean the local file name var - this gurantees that the file is always null terminated */
	memset((void*)_mcpHalFs_lfilename, 0, MCP_HAL_CONFIG_FS_MAX_PATH_LEN_CHARS + MCP_HAL_CONFIG_FS_MAX_FILE_NAME_LEN_CHARS+1);
	
	/* build the local file name var, begin with the folder name */
	strncpy((char*)_mcpHalFs_lfilename,(char*)_mcpHalFs_DirStructureArray[dirDesc].path, MCP_HAL_CONFIG_FS_MAX_PATH_LEN_CHARS);
	
         //klocwork
         MCPF_Assert(strlen((char*)_mcpHalFs_lfilename)>0);
	//make sure folder names ends with '/'
	if(strcmp((char*)(_mcpHalFs_lfilename + strlen((char*)_mcpHalFs_lfilename)-1),"/") != 0)
	{
		strcat((char*)_mcpHalFs_lfilename,  "/");
	}

	//add file name portion
	strncat((char*)_mcpHalFs_lfilename,(char*)fileInfo->d_name, MCP_HAL_CONFIG_FS_MAX_FILE_NAME_LEN_CHARS);

	*fileName = _mcpHalFs_lfilename;

	return MCP_HAL_FS_STATUS_SUCCESS;
}

/*-------------------------------------------------------------------------------
 * MCP_HAL_FS_CloseDir()
 *
 *	Synopsis:  set the busy flag to 0 -> free the cell. 
 *
 * 	Parameters:
 *		dirDesc [in] - points to Dir handle .
 *
 * 	Returns:
 *		MCP_HAL_FS_STATUS_SUCCESS - Operation is successful.
 *
 *		MCP_HAL_FS_STATUS_ERROR_GENERAL - -  Operation failed.
 *
 */
McpHalFsStatus MCP_HAL_FS_CloseDir( const McpHalFsDirDesc dirDesc )
{
	McpHalFsStatus error;

	int retval;
	
	/* check if directory handle valid and in range */
	if((error = _McpHalFs_CheckIfDirHandleIsInValidAndRange(dirDesc)) != 0)
		return error;


	retval = closedir(_mcpHalFs_DirStructureArray[dirDesc].searchHandle);

	/* free the cell */
	_mcpHalFs_DirStructureArray[dirDesc].busy = 0;
	if (0 == retval)
	{
		return MCP_HAL_FS_STATUS_SUCCESS;
	}

	return MCP_HAL_FS_STATUS_ERROR_GENERAL;
}

/*-------------------------------------------------------------------------------
 *
 *  MCP_HAL_FS_Rename
 *
 *  renames a file or a directory
 */
McpHalFsStatus MCP_HAL_FS_Rename(const McpUtf8 *fullPathOldName, const McpUtf8 *fullPathNewName )
{
        /* TODO (a0798989): support UTF-8 */

        if (rename((char *)fullPathOldName,(char *)fullPathNewName) != 0)
	{
		return _McpHalFs_ConvertLinuxErrorToFsError();
	}
	return MCP_HAL_FS_STATUS_SUCCESS;
}

/*-------------------------------------------------------------------------------
 *
 *  MCP_HAL_FS_Remove
 *
 *  removes a file
 */ 
McpHalFsStatus MCP_HAL_FS_Remove( const McpUtf8 *fullPathFileName )
{
	/* TODO (a0798989): support UTF-8 */

        if(remove((char *)fullPathFileName) != 0)
		return _McpHalFs_ConvertLinuxErrorToFsError();

	return MCP_HAL_FS_STATUS_SUCCESS;
}

/*-------------------------------------------------------------------------------
 *
 * MCP_HAL_FS_IsAbsoluteName()
 *
 * This function checks whether a given file name has an absolute path name
 */
McpHalFsStatus MCP_HAL_FS_IsAbsoluteName( const McpUtf8 *fileName, McpBool *isAbsolute )
{
	*isAbsolute = MCP_FALSE;
	if ( fileName )
	{
		if (fileName[0] == '/')
		{
			*isAbsolute =  MCP_TRUE;
		}
		return MCP_HAL_FS_STATUS_SUCCESS;
	}
	return MCP_HAL_FS_STATUS_ERROR_NOTAFILE;
}

/*-------------------------------------------------------------------------------
 *
 * MCP_HAL_FS_ExtractDateAndTime()
 *
 * This function extracts date and time in UTC from st_time argument.
 * Note: we are not using the raw data returned by gmtime(), but instead
 *       we are using the ascii-format string returned by asctime().
 */
void	MCP_HAL_FS_ExtractDateAndTime(time_t st_time, McpHalDateAndTime *dateAndTimeStruct)
{
	static char *pMonth[] = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"}; 
    
    McpU8 str[20];
    McpU8 *pStr;
    
    struct tm *pUtcTime;

    MCP_LINUX_LINE_PARSER_STATUS 	status;
    McpU16 idx;

    McpU16 year;    /* YYYY: e.g  2007 */
    McpU16 month;   /* MM: [1..12]     */
    McpU16 day;     /* DD: [1..31]     */
    McpU16 hour;    /* HH: [0..23]     */
    McpU16 minute;  /* MM: [0..59]     */
    McpU16 second;  /* SS: [0..59]     */
		
    month = 0;


    MCP_HAL_MEMORY_MemSet(dateAndTimeStruct, 0, sizeof(McpHalDateAndTime));

    
    /* The fields of pUtcTime hold the evaluated value of the st_time 
       argument in UTC rather than in local time. */
    pUtcTime = gmtime( &st_time );

    if (NULL == pUtcTime)
    {
        /* If timer represents a date before midnight, January 1, 1970, 
           gmtime returns NULL */
        return;
    }
    
    /* Converts a pUtcTime structure to a character string.*/
    /* The format in pStr is: "Tue May 08 17:23:04 2007" */
    pStr = (McpU8*)asctime( pUtcTime );
    
    /* Now, we shall extract the dateAndTimeStruct fields from pUtcTime string */ 
    
    /* Instructs the parser to search for 'SP' (space) and ':' (column) delimiters */

    status = MCP_LINUX_LINE_PARSER_ParseLine( pStr, " :" );
	
	if (status != MCP_LINUX_LINE_PARSER_STATUS_SUCCESS)
	{
		return;
	}
    
    /* Skip the first argument */
    if (MCP_LINUX_LINE_PARSER_STATUS_SUCCESS != MCP_LINUX_LINE_PARSER_GetNextStr(str, MCP_LINUX_LINE_PARSER_MAX_STR_LEN))
	{
		return;
	}

    /* Get month */
    if (MCP_LINUX_LINE_PARSER_STATUS_SUCCESS != MCP_LINUX_LINE_PARSER_GetNextStr(str, MCP_LINUX_LINE_PARSER_MAX_STR_LEN))
	{
		return;
	}

    for (idx=0 ;idx < MCP_HAL_FS_MONTH_LIST_MAX; idx++)
    {
        if (0 == MCP_HAL_STRING_StrCmp((char*)str, pMonth[idx]))
        {
            month = (McpU16)(idx + 1);
            break;
        }
    }
    
    /* Get day */
    if (MCP_LINUX_LINE_PARSER_STATUS_SUCCESS != MCP_LINUX_LINE_PARSER_GetNextU16(&day, MCP_LINUX_LINE_PARSER_MAX_STR_LEN))
	{
		return;
	}

    /* Get hour */
    if (MCP_LINUX_LINE_PARSER_STATUS_SUCCESS != MCP_LINUX_LINE_PARSER_GetNextU16(&hour, MCP_LINUX_LINE_PARSER_MAX_STR_LEN))
	{
		return;
	}

    /* Get minutes */
    if (MCP_LINUX_LINE_PARSER_STATUS_SUCCESS != MCP_LINUX_LINE_PARSER_GetNextU16(&minute, MCP_LINUX_LINE_PARSER_MAX_STR_LEN))
	{
		return;
	}

    /* Get seconds */
    if (MCP_LINUX_LINE_PARSER_STATUS_SUCCESS != MCP_LINUX_LINE_PARSER_GetNextU16(&second, MCP_LINUX_LINE_PARSER_MAX_STR_LEN))
	{
		return;
	}

    /* Get year */
    if (MCP_LINUX_LINE_PARSER_STATUS_SUCCESS != MCP_LINUX_LINE_PARSER_GetNextU16(&year, MCP_LINUX_LINE_PARSER_MAX_STR_LEN))
	{
		return;
	}

    dateAndTimeStruct->day = day;
    dateAndTimeStruct->hour = hour;
    dateAndTimeStruct->minute = minute;
    dateAndTimeStruct->month = month;
    dateAndTimeStruct->second = second;
    dateAndTimeStruct->year = year;

    dateAndTimeStruct ->utcTime = MCP_TRUE;
}


/****     UTC Time for CMCC Log Info      ****/

char * MCP_HAL_FS_get_utc_time()
{
  struct timeval tv;
  struct tm *local;
  time_t t;
  static char timestr[100];

  t = time(NULL);
  local = gmtime(&t);
  gettimeofday(&tv, 0);
  sprintf (timestr,"%4d%02d%02d%02d%02d%02d.%03d", 1900 + local->tm_year,local->tm_mon + 1,local->tm_mday,local->tm_hour, local->tm_min,local->tm_sec,(int)(tv.tv_usec)/1000);
   return timestr;
}
















/*-------------------------------------------------------------------------------
 *
 * _McpHalFs_ExtractPermissions()
 *
 * This function extracts user (and group/other when present) access permissions.
 */
static void _McpHalFs_ExtractPermissions(mode_t file_mode, McpHalFsStat* fileStat)
{
	fileStat->userPerm = 0;
	fileStat->groupPerm = 0;
	fileStat->otherPerm = 0;

	if (file_mode & S_IRUSR)
	{
		fileStat->userPerm |= MCP_HAL_FS_PERM_READ;
	}

	if (file_mode & S_IWUSR)
	{
		fileStat->userPerm |= MCP_HAL_FS_PERM_WRITE|MCP_HAL_FS_PERM_DELETE;
	}

	if (file_mode & S_IRGRP)
	{
		fileStat->groupPerm |= MCP_HAL_FS_PERM_READ;
	}

	if (file_mode & S_IWGRP)
	{
		fileStat->groupPerm |= MCP_HAL_FS_PERM_WRITE|MCP_HAL_FS_PERM_DELETE;
	}

	if (file_mode & S_IROTH)
	{
		fileStat->otherPerm |= MCP_HAL_FS_PERM_READ;
	}

	if (file_mode & S_IWOTH)
	{
		fileStat->otherPerm |= MCP_HAL_FS_PERM_WRITE|MCP_HAL_FS_PERM_DELETE;
	}
}

/*---------------------------------------------------------------------------
 *            MCP_HAL_FS_fopen
 *---------------------------------------------------------------------------
 *
 *
 */
McpFILE MCP_HAL_FS_fopen(McpS8* filename, McpS8* param)
{
	return fopen(filename, param);
}

/*---------------------------------------------------------------------------
 *            MCP_HAL_FS_fgets
 *---------------------------------------------------------------------------
 *
 * 
 *
 */
McpBool MCP_HAL_FS_fgets(McpS8* str1, McpU16 size, McpFILE fileptr)
{
	return fgets(str1, size, fileptr);
}

/*---------------------------------------------------------------------------
 *            MCP_HAL_FS_fclose
 *---------------------------------------------------------------------------
 *
 * 
 *
 */
McpBool MCP_HAL_FS_fclose(McpFILE fileptr)
{
	return fclose(fileptr);
}

