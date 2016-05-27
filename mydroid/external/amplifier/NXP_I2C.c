/******************** (C) COPYRIGHT 2013 Recon Instruments ********************
*
* File Name          : NXP_I2C.c
* Authors            : Li Chen
* Version            : V 0.1
* Date               : October 2013
* Description        : Recon NXP i2c API on Linux
*
********************************************************************************
*
******************************************************************************/

#include <stdio.h>
#include <assert.h>
#include <unistd.h>
#include <sys/types.h>
#include <i2c-dev.h>
#include <sys/ioctl.h>
#include <fcntl.h>
#include <linux/stat.h>
#include "NXP_I2C.h"

#include "amplifier_init.h"
#include "amplifier_log.h"
//#define I2CVERBOSE
//#define TFA_CHECK_REV 0x12
#define TFA_I2CDEVICE		"/dev/tfa98xx"
#define TFA_I2CSLAVEBASE		(0x34)              // tfa device slave address of 1st (=left) device
#ifndef __BOOL_DEFINED
  typedef unsigned char bool;
  #define true ((bool)(1==1))
  #define false ((bool)(1==0))
#endif
static int bInit = false;
static int i2cTargetFd=-1;/* file descriptor for target device */
/* 
 * WriteData[0] is sub address
 * ReadData[0] is the first data read from the WriteData[0] sub address
 */
int lxI2cWriteRead(int fd, int NrOfWriteBytes, const uint8_t * WriteData,
					int NrOfReadBytes, uint8_t * ReadData) {
	uint8_t buf[256];
	int ln=0;

	if (NrOfWriteBytes > 1)
		ln = write(fd, &WriteData[0],  NrOfWriteBytes);

	if (NrOfReadBytes) { // bigger
		write(fd, &WriteData[0],1); //write sub address
		ln = read(fd,  &ReadData[0], NrOfReadBytes);
	}

	if ( ln < 0 )
		perror("i2c slave error");

	return ln;
}

static NXP_I2C_Error_t init_I2C(void){
	int res=0;
#ifdef TFA_CHECK_REV
 unsigned char wbuf[1], rbuf[2];
#endif 
#ifdef I2CVERBOSE
	printf("%s\n", __FUNCTION__);
#endif
	/*	TODO properly define open device configs */
	if (i2cTargetFd < 0 ) {
		i2cTargetFd = open(TFA_I2CDEVICE, O_RDWR | O_NONBLOCK, 0);
	}

	if (i2cTargetFd < 0 ){
		print_err("!i2c device was not opened\n");
		return NXP_I2C_NoInterfaceFound;
	}

#ifdef TFA_CHECK_REV
	// check the contents of the ID regsiter
	wbuf[0] = 0x03; // ID register
	lxI2cWriteRead(i2cTargetFd, sizeof(wbuf), wbuf, sizeof(rbuf), rbuf);
	if ( rbuf[0]!=(TFA_CHECK_REV>>8) || rbuf[1] != (TFA_CHECK_REV&0xff) ) {
			print_err("!wrong ID expected:0x%04x received:0x%04x, register 3 of slave 0x%02x\n", 
						 TFA_CHECK_REV ,  rbuf[0]<<8 | rbuf[1], TFA_I2CSLAVEBASE);
	}
#endif        
	bInit = true;
	return NXP_I2C_Ok;
}

NXP_I2C_Error_t close_I2C(void)
{
	close(i2cTargetFd);
	bInit = false;
	return NXP_I2C_Ok;
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

static void hexdump(int num_write_bytes, const unsigned char * data)
{
	int i;

	for(i=0;i<num_write_bytes;i++)
	{
		printf("0x%02x ", data[i]);
	}

}

NXP_I2C_Error_t NXP_I2C_Write(	unsigned char sla, //Not used for mono speaker
								int num_write_bytes,
								unsigned char data[] )
{
	NXP_I2C_Error_t retval;
#ifdef I2CVERBOSE
	printf("%s   W %3d: ",__FUNCTION__, num_write_bytes);
	hexdump(num_write_bytes, data);
	printf("\n");
#endif
	if (num_write_bytes > NXP_I2C_MAX_SIZE)
	{
		print_err( "%s: too many bytes: %d\n", __FUNCTION__, num_write_bytes);
		return NXP_I2C_UnsupportedValue;
	}

	retval = init_if_firsttime();
	if (NXP_I2C_Ok==retval)
	{
		if ( 0 == lxI2cWriteRead( i2cTargetFd, num_write_bytes, data, 0, NULL) )
		{
			print_err( "NoAck\n");
			retval=NXP_I2C_NoAck;
		}
		else
			retval=NXP_I2C_Ok;
	}
	return retval;
}

NXP_I2C_Error_t NXP_I2C_WriteRead(	unsigned char sla,
									int num_write_bytes,
									unsigned char write_data[],
									int num_read_bytes,
									unsigned char read_data[] )
{
  NXP_I2C_Error_t retval;

	if (num_write_bytes > NXP_I2C_MAX_SIZE)
	{
		print_err( "%s: too many bytes to write: %d\n", __FUNCTION__, num_write_bytes);
		return NXP_I2C_UnsupportedValue;
	}
	if (num_read_bytes > NXP_I2C_MAX_SIZE)
	{
		print_err( "%s: too many bytes to read: %d\n", __FUNCTION__, num_read_bytes);
		return NXP_I2C_UnsupportedValue;
	}
#ifdef I2CVERBOSE
	printf("%s W %3d: ",__FUNCTION__, num_write_bytes);
	hexdump(num_write_bytes, write_data);
	printf("\n");
#endif
	retval = init_if_firsttime();
	if (NXP_I2C_Ok==retval)
	{
		retval = lxI2cWriteRead(i2cTargetFd, num_write_bytes, write_data ,
								num_read_bytes, read_data);

		if (retval>0) {
#ifdef I2CVERBOSE
			printf("%s R %3d: ",__FUNCTION__, num_read_bytes);
			hexdump(num_read_bytes, read_data);
			printf("\n");
#endif
			retval=NXP_I2C_Ok;
		}
		else {
			print_err( "NoAck\n");
			return NXP_I2C_NoAck;
		}
	}
	return retval;
}
