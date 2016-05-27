/*
 * lxI2c.c
 *
 *  Created on: Apr 21, 2012
 *      Author: wim
 */
#include <stdio.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <errno.h>
#include <inttypes.h>	//TODO fix/converge types
//#include <linux/stat.h>
#include <fcntl.h>
#include <i2c-dev.h>

#include "lxScribo.h"
#include "nxpTfa98xx.h"	// for i2c slave address

int i2c_trace=0;

static void hexdump(int num_write_bytes, const unsigned char * data)
{
	int i;

	for(i=0;i<num_write_bytes;i++)
	{
		printf("0x%02x ", data[i]);
	}

}

void  lxI2cSlave(int fd, int slave)
{
    // open the slave
    int res = ioctl(fd, I2C_SLAVE, slave);
    if ( res < 0 ) {
        /* TODO ERROR HANDLING; you can check errno to see what went wrong */
        fprintf(stderr, "Can't open i2c slave:0x%02x\n", slave);
        _exit(res);
    }

    if (i2c_trace) printf("I2C slave=0x%02x\n", tfa98xxI2cSlave);

}

int lxI2cWriteRead(int fd, int NrOfWriteBytes, const uint8_t * WriteData,
		int NrOfReadBytes, uint8_t * ReadData) {
	struct i2c_smbus_ioctl_data args;
	uint8_t buf[256];
	int ln;

	// TODO redo checking
	if (WriteData[0] !=  (tfa98xxI2cSlave<<1)) {
		printf ("wrong slave 0x%02x iso0x%02x\n", WriteData[0]>>1, tfa98xxI2cSlave);
		return 0;
	}
	//lxI2cSlave(fd, tfa98xxI2cSlave);

	if (NrOfWriteBytes & i2c_trace) {
		printf("W %d:", NrOfWriteBytes);
		hexdump (NrOfWriteBytes, WriteData);
		printf("\n");
	}

	if (NrOfWriteBytes > 2)

		ln = write(fd, &WriteData[1],  NrOfWriteBytes - 1);

//	if (NrOfReadBytes==3) {// TODO simplify read register
//		args.read_write = 1;
//		args.command = WriteData[1]; //register address
//		args.size = NrOfReadBytes;
//		args.data = &ReadData[1];
//
//		ln = ioctl(fd, I2C_SMBUS, &args);
//	} else
	if (NrOfReadBytes) { // bigger
		//if ( (ReadData[0]>1) != (WriteData[0]>1) ) // if block read is different
		//		write(fd, &ReadData[0],  1);
		write(fd, &WriteData[1],1); //write sub address
		ln = read(fd,  &ReadData[1], NrOfReadBytes-1);
	}

	if (NrOfReadBytes & i2c_trace) {
		printf("R %d:", NrOfReadBytes);
		hexdump (NrOfReadBytes, ReadData);
		printf("\n");
	}
	if ( ln < 0 )
	        perror("i2c slave error");

	return ln+1;
}

int lxI2cWrite(int fd, int size, uint8_t *buffer)
{
	uint8_t cmd[5], *ptr, slave,term;
	int length,status, total=0;

	return lxI2cWriteRead( fd, size, buffer, 0, NULL);

}

int lxI2cInit(char *devname)
{
	int fd, res;
	fprintf(stdout, "%s:%s\n", __FUNCTION__,devname);
	fd = open(devname, O_RDWR | O_NONBLOCK, 0);
	if (fd < 0) {
		fprintf(stderr, "Can't open i2c bus:%s\n", devname);
		_exit(1);
	}

	//lxI2cSlave(fd, tfa98xxI2cSlave);

	return fd;
}


