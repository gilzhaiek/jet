/*****< iacptran.c >***********************************************************/
/*      Copyright 2001 - 2012 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  IACPTRAN - Stonestreet One Apple Authentication Coprocessor Transport     */
/*             Layer for TI Hardware Platform.                                */
/*                                                                            */
/*  Author:  Tim Thomas                                                       */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   --------  -----------    ------------------------------------------------*/
/*   04/23/13  Li Chen                                     */
/******************************************************************************/
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <sys/select.h>
#include <sys/time.h>
#include <errno.h>
#include <time.h>
/*Disable DEBUG_IAP for the final release*/
//#define DEBUG_IAP

#ifdef DEBUG_IAP
#include <string.h>

#define BTPSAPI
#define BTPSAPI_DECLARATION

#define IACP_STATUS_SUCCESS 0
#define IACP_STATUS_READ_FAILURE -1
#define IACP_STATUS_WRITE_FAILURE -2
#define IACP_RESPONSE_TIMEOUT -3
#define IACP_MODULE_NOT_INITIALIZED -4
#define IACP_INVALID_PARAMETER -5

typedef enum { FALSE, TRUE } Boolean_t;
typedef unsigned char Byte_t;
#define NO_COMMAND_ERROR                           (-1) 
#define OUT_OF_RANGE                               (-2) 
#define EXIT_CODE                                  (-3) 
#define INVALID_PARAMETERS_ERROR                   (-4) 

#define MAX_COMMAND_LENGTH                        (128) 
#define MAX_BYTE_LENGTH                           (128) 
#else

#include "BTPSKRNL.h"            /* Bluetooth Kernel Prototypes/Constants.     */
#include "IACPTRAN.h"            /* ACP Transport Prototypes/Constants.       */
#endif
	/* The following constant represents the maximum number retry        */
	/* attempts that will be used when reading/writing to the device.    */
#define I2C_MAXIMUM_NUMBER_OF_RETRIES            10
#define MFI_IOCTL_BASE 'g'
#define MFI_SET_READREG_ADDRESS  _IOW(MFI_IOCTL_BASE, 0,int)
#define MFI_SET_WRITEREG_ADDRESS  _IOW(MFI_IOCTL_BASE, 1,int)


	/* The following constants represent the GPIO interface for control  */
	/* of the Reset pin on the ACP.                                      */
#define GPIO_RST                                 44
#define GPIO_RST_LOW                              0
#define GPIO_RST_HIGH                             1


	/* Internal Variables to this Module (Remember that all variables */
	/* declared static are initialized to 0 automatically by the      */
	/* compiler as part of standard C/C++).                           */

	/* Variable which holds the File Descriptor of the open I2C device.  */
	/* This device used to talk to actual Apple Authentication           */
	/* Co-Processor.                                                     */
static int FileDescriptor = -1;

	/* Variable which holds the delay we should wait after an unsuccessul*/
	/* register selection in the Co-Processor. This is used with the     */
	/* nanosleep() API to delay for 50 ms.                              */
static struct timespec DelayAfterNACK = {0, 50000000};


static Boolean_t GPIO_Set(unsigned int GPIO, unsigned int State)
{
	Boolean_t Result;
	char      FilePath[64];
	FILE     *GPIOControlFile;

	sprintf(FilePath, "/sys/class/gpio/gpio%u/value", GPIO);
	GPIOControlFile = fopen(FilePath, "w");

	if(GPIOControlFile)
	{
	  fprintf(GPIOControlFile, "%u", State);
	  fclose(GPIOControlFile);

	  Result = TRUE;
	}
	else
	  Result = FALSE;

	return(Result);
}

	/* The following function is responsible for Initializing the ACP    */
	/* hardware.  The function is passed a pointer to an opaque object.  */
	/* Since the hardware platform is not know, it is intended that the  */
	/* parameter be used to pass hardware specific information to the    */
	/* module that would be needed for proper operation.  Each           */
	/* implementation will define what gets passed to this function.     */
	/* This function returns zero if successful or a negative value if   */
	/* the initialization fails.                                         */
int BTPSAPI IACPTR_Initialize(void *Parameters)
{
	int    ret_val;
	Byte_t Status;
	Byte_t IACPCommand;

	/* Verify that the device is not already opened.                     */
	if(FileDescriptor < 0)
	{
		/* Device is not already opened.                                  */
		if((FileDescriptor = open("/dev/mfi", O_RDWR)) >= 0)
		{
			ret_val = 0;
		}
		else
			ret_val = -100;
	}
	else
	  ret_val = 0;

	/* Finally return the result to the caller.                          */
	return(ret_val);
}

	/* The following function is responsible for performing any cleanup  */
	/* that may be required by the hardware or module.  This function    */
	/* returns no status.                                                */
void BTPSAPI IACPTR_Cleanup(void)
{
	Byte_t Status;
	Byte_t IACPCommand;

	/* If the device has been opened, we simply need to close it.        */
	if(FileDescriptor >= 0)
	{
	  close(FileDescriptor);

	  FileDescriptor = -1;
	}
}

	/* The following function is responsible for Reading Data from a     */
	/* device via the ACP interface.  The first parameter to this        */
	/* function is the Register/Address that is to be read.  The second  */
	/* parameter indicates the number of bytes that are to be read from  */
	/* the device.  The third parameter is a pointer to a buffer where   */
	/* the data read is placed.  The size of the Read Buffer must be     */
	/* large enough to hold the number of bytes being read.  The last    */
	/* parameter is a pointer to a variable that will receive status     */
	/* information about the result of the read operation.  This function*/
	/* should return a non-negative return value if successful (and      */
	/* return IACP_STATUS_SUCCESS in the status parameter).  This        */
	/* function should return a negative return error code if there is an*/
	/* error with the parameters and/or module initialization.  Finally, */
	/* this function can also return success (non-negative return value) */
	/* as well as specifying a non-successful Status value (to denote    */
	/* that there was an actual error during the write process, but the  */
	/* module is initialized and configured correctly).                  */
	/* * NOTE * If this function returns a non-negative return value     */
	/*          then the caller will ALSO examine the value that was     */
	/*          placed in the Status parameter to determine the actual   */
	/*          status of the operation (success or failure).  This      */
	/*          means that a successful return value will be a           */
	/*          non-negative return value and the status member will     */
	/*          contain the value:                                       */
	/*             IACP_STATUS_SUCCESS                                   */
int BTPSAPI IACPTR_Read(unsigned char Register, unsigned char BytesToRead, unsigned char *ReadBuffer, unsigned char *Status)
{
	int          ret_val;
	unsigned int RetryCount;

	/* Verify that the parameters passed in appear valid.                */
	if((BytesToRead) && (ReadBuffer) && (Status))
	{
		/* Initialize failure status.                                     */
		*Status = IACP_STATUS_READ_FAILURE;

		/* Check to see if the device has been opened.                    */
		if(FileDescriptor >= 0)
		{
			/* Device has been opened, attempt to write the address.       */
			RetryCount = 0;
			do
			{
				if( ioctl(FileDescriptor, MFI_SET_READREG_ADDRESS, &Register) == 0)
					break;
				else{
					RetryCount++;
					nanosleep(&DelayAfterNACK, NULL);
				}
			}while(RetryCount < I2C_MAXIMUM_NUMBER_OF_RETRIES);

			/* Check to see if the address was written successfully.    */
			if(RetryCount != I2C_MAXIMUM_NUMBER_OF_RETRIES)
			{
				if(read(FileDescriptor, ReadBuffer, BytesToRead) == BytesToRead)
				{
					/* Flag success to the caller.                           */
					ret_val = 0;
					*Status = IACP_STATUS_SUCCESS;
				}
				else
					ret_val = IACP_RESPONSE_TIMEOUT;
			}
			else
				ret_val = IACP_RESPONSE_TIMEOUT;
		}
		else
			ret_val = IACP_MODULE_NOT_INITIALIZED;
	}
	else
		ret_val = IACP_INVALID_PARAMETER;

	/* Finally return the result to the caller.                          */
	return(ret_val);
}

	/* The following function is responsible for Writing Data to a device*/
	/* via the ACP interface.  The first parameter to this function is   */
	/* the Register/Address where the write operation is to start.  The  */
	/* second parameter indicates the number of bytes that are to be     */
	/* written to the device.  The third parameter is a pointer to a     */
	/* buffer where the data to be written is placed.  The last parameter*/
	/* is a pointer to a variable that will receive status information   */
	/* about the result of the write operation.  This function should    */
	/* return a non-negative return value if successful (and return      */
	/* IACP_STATUS_SUCCESS in the status parameter).  This function      */
	/* should return a negative return error code if there is an error   */
	/* with the parameters and/or module initialization.  Finally, this  */
	/* function can also return success (non-negative return value) as   */
	/* well as specifying a non-successful Status value (to denote that  */
	/* there was an actual error during the write process, but the       */
	/* module is initialized and configured correctly).                  */
	/* * NOTE * If this function returns a non-negative return value     */
	/*          then the caller will ALSO examine the value that was     */
	/*          placed in the Status parameter to deterimine the actual  */
	/*          status of the operation (success or failure).  This      */
	/*          means that a successful return value will be a           */
	/*          non-negative return value and the status member will     */
	/*          contain the value:                                       */
	/*             IACP_STATUS_SUCCESS                                   */
int BTPSAPI IACPTR_Write(unsigned char Register, unsigned char BytesToWrite, unsigned char *WriteBuffer, unsigned char *Status)
{
	int          ret_val;
	unsigned int RetryCount;

	/* Verify that the parameters passed in appear valid.                */
	if((BytesToWrite) && (WriteBuffer) && (Status))
	{
		/* Initialize failure status.                                     */
		*Status = IACP_STATUS_WRITE_FAILURE;

		/* Check to see if the device has been opened.                    */
		if(FileDescriptor >= 0)
		{
			RetryCount = 0;
			if( ioctl(FileDescriptor, MFI_SET_WRITEREG_ADDRESS, &Register))
			//this ioctl command didn't involve i2c transfer, so just simple return
				return (IACP_RESPONSE_TIMEOUT);

			do
			{
				if(write(FileDescriptor, WriteBuffer, BytesToWrite) == BytesToWrite)
				  break;
				else
				{
				  RetryCount++;
				  nanosleep(&DelayAfterNACK, NULL);
				}
			} while(RetryCount < I2C_MAXIMUM_NUMBER_OF_RETRIES);

			/* Check to see if the data was written successfully.       */
			if(RetryCount != I2C_MAXIMUM_NUMBER_OF_RETRIES)
			{
				/* Flag success to the caller.                           */
				ret_val = 0;
				*Status = IACP_STATUS_SUCCESS;
			}
			else
				ret_val = IACP_RESPONSE_TIMEOUT;
		}
		else
		 ret_val = IACP_MODULE_NOT_INITIALIZED;
	}
	else
	  ret_val = IACP_INVALID_PARAMETER;

	/* Finally return the result to the caller.                          */
	return(ret_val);
}

	/* The following function is responsible for resetting the ACP       */
	/* hardware. The implementation should block until the reset is      */
	/* complete.                                                         */
	/* * NOTE * This function only applies to chips version 2.0B and     */
	/*          earlier. For chip version 2.0C and newer, this function  */
	/*          should be implemented as a NO-OP.                        */
BTPSAPI_DECLARATION void BTPSAPI IACPTR_Reset(void)
{
	struct timespec ResetDelay;
	struct timespec RemainingDelay;

	/* Toggle the Reset GPIO                                             */
	if(GPIO_Set(GPIO_RST, GPIO_RST_LOW))
	{
		/* Short sleep to let the ACP fully discharge.                    */
		ResetDelay.tv_sec  = 0;
		ResetDelay.tv_nsec = 30000000;

		while(nanosleep(&ResetDelay, &RemainingDelay) == EINTR)
		 ResetDelay = RemainingDelay;

		GPIO_Set(GPIO_RST, GPIO_RST_HIGH);

		/* Long sleep to let the ACP intialize its I2C interface.         */
		ResetDelay.tv_sec  = 0;
		ResetDelay.tv_nsec = 60000000;

		while(nanosleep(&ResetDelay, &RemainingDelay) == EINTR)
		 ResetDelay = RemainingDelay;
	}
}

#ifdef DEBUG_IAP
static void printf_help(void) 
{
   printf("******************************************************************\r\n");
   printf("* r xx yy                                                         \r\n");
   printf("* Read Mfi start from register address xx and read back yy bytes*\r\n");
   printf("* Notes: both xx and yy are hex numbers for now                  *\r\n");
   printf("*                                                                *\r\n");
   printf("* w xx yy [yy] [yy] ...                                          *\r\n");
   printf("* write Mfi start from register address xx and the first write   *\r\n");
   printf("* value is yy                                                    *\r\n");
   printf("* Notes: both xx and yy are hex numbers for now                  *\r\n");
}
static char *StringParser(char *String)
{
   int   Index;
   char *ret_val = NULL;

   /* Before proceeding make sure that the string passed in appears to  */
   /* be at least semi-valid.                                           */
   if((String) && (strlen(String)))
   {
      /* The string appears to be at least semi-valid.  Search for the  */
      /* first space character and replace it with a NULL terminating   */
      /* character.                                                     */
      for(Index=0,ret_val=String;Index < strlen(String);Index++)
      {
         /* Is this the space character.                                */
         if((String[Index] == ' ') || (String[Index] == '\r') || (String[Index] == '\n'))
         {
            /* This is the space character, replace it with a NULL      */
            /* terminating character and set the return value to the    */
            /* begining character of the string.                        */
            String[Index] = '\0';
            break;
         }
      }
   }

   return(ret_val);
}
static int CommandParser(char *String)
{
	int size = strlen(String);
	char *str;
	char cmd;
	unsigned char buffer[MAX_BYTE_LENGTH],Status;
	int  index=0;
	int ret;

	if(size && String)
	{
		//Parse command
		str=StringParser(String);
		if(str==NULL && strlen(str)!= 1)
			return NO_COMMAND_ERROR;

		cmd=str[0];
		//TODO: so far we just have write and read function
		if(cmd!='w' && cmd!='r')
			return NO_COMMAND_ERROR;

		//Parse payload
		String += strlen(str)+1;
		size = strlen(String);
		while(size>0 && (str = StringParser(String)) != NULL)
		{
			if(strlen(str)!=2)
				return INVALID_PARAMETERS_ERROR;
			buffer[index]= (unsigned char)strtol(str, NULL, 16) ;
			printf("data=0x%.2x\r\n",buffer[index]);
			index++;
			if(index>MAX_BYTE_LENGTH)
				return OUT_OF_RANGE;

			String += strlen(str)+1;
			size = strlen(String);
		}

		//Execute command
		switch (cmd)
		{
			case 'w':
				ret=IACPTR_Write(buffer[0], index-1, buffer+1, &Status);
				printf("ret=%d, Status=%d\r\n", ret, Status);
			break;
			case 'r':
				ret=IACPTR_Read(buffer[0], buffer[1],buffer+2, &Status);
				printf("ret=%d, Status=%d\r\n", ret, Status);
				if(ret==0)
				{
					for(index=0; index<buffer[1]; index++)
						printf("read data=0x%.2x,",buffer[index+2]);
				}
			break;
			default:
				return NO_COMMAND_ERROR;
		}
		return 0;
	}
	return NO_COMMAND_ERROR;
}
static void UserInterface(void)
{
	int  Result = !EXIT_CODE;
	char UserInput[MAX_COMMAND_LENGTH];
	int ret;
	while(Result != EXIT_CODE)
	{
		UserInput[0] = '\0';
		printf("\r\nMFI>");
		if(fgets(UserInput, sizeof(UserInput), stdin) != NULL)
		{
			if(strlen(UserInput))
			{
				ret=CommandParser(UserInput);
				if(ret)
					printf("err=%d\r\n",ret);
			}
		}
		else
			Result= EXIT_CODE;
	}
}
int main(int argc, char* argv[])
{
	IACPTR_Initialize(NULL);
	printf_help();
	UserInterface();
	return 0;
}
#endif