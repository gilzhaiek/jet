/*
 * lxScribo.h
 *
 *  Created on: Mar 14, 2012
 *      Author: nlv02095
 */

#ifndef LXSCRIBO_H_
#define LXSCRIBO_H_

#if !defined(uint8_t)
	#define uint8_t unsigned char
#endif

#if !defined (int8_t)
	#define int8_t signed char
#endif

#if !defined (uint16_t)
	#define uint16_t unsigned short
#endif

#if !defined (int16_t)
	#define int16_t signed short
#endif

#if !defined (uint32_t)
	#define uint32_t unsigned long
#endif

#if !defined (int32_t)
	#define int32_t signed long
#endif

#define LXSCRIBO_VERSION 	0.1
#define SERIALDEV 			"/dev/ttyUSB0" // "/dev/Scribo"
#define LXSOCKET 			"9887" 		// note: in ascii for api alignment with serial

int lxScriboRegister(char *dev);	// register target and return opened file desc.
int lxScriboGetFd(void);			// return active file desc.

int lxScriboVersion(int fd, uint8_t *buffer);
int lxScriboWrite(int fd, int size, uint8_t *buffer);
int lxScriboWriteRead(int fd, int wsize, const uint8_t *wbuffer
										   , int rsize, unsigned char *rbuffer);
int lxScriboPrintTargetRev(int fd);
int lxScriboSerialInit(char *dev);
int lxScriboSocketInit(char *dev);
void lxScriboSocketExit(void);
int lxScriboSetPin(int fd, int pin, int value);
int lxScriboGetPin(int fd, int pin);
int lxScriboGetRev(int fd, char *str, int max);

int lxI2cInit(char *dev);
int lxI2cWrite(int fd, int size, uint8_t *buffer);
int lxI2cWriteRead(int fd, int wsize, const uint8_t *wbuffer
										   , int rsize, unsigned char *rbuffer);

int lxDummyInit(char *dev);
int lxDummyWrite(int fd, int size, uint8_t *buffer);
int lxDummyWriteRead(int fd, int wsize, const uint8_t *wbuffer
										   , int rsize, unsigned char *rbuffer);

int (*lxScriboInit)(char *dev);
int (*lxWrite)(int fd, int size, uint8_t *buffer);
int (*lxWriteRead)(int fd, int wsize, const uint8_t *wbuffer
										   , int rsize, unsigned char *rbuffer);

extern int lxScribo_verbose;
extern int i2c_trace;
extern int NXP_I2C_verbose;

// for dummy
#define rprintf printf

//from gpio.h
#define I2CBUS 0
//from gui.h
/* IDs of menu items */
typedef enum
{
  ITEM_ID_VOLUME = 100,
  ITEM_ID_PRESET,
  ITEM_ID_EQUALIZER,

  ITEM_ID_STANDALONE,

  ITEM_ID_ENABLESCREENDUMP,
} menuElement_t;

#endif /* LXSCRIBO_H_ */
