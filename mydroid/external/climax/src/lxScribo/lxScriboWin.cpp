//#include <Scribo.h>
#include <NXP_I2C.h>
#include <lxScribo.h>

extern "C" 
{
//int lxScriboWinSetPin(int pin, int value);
//bool _stdcall SetPin(uint8_t pinNumber, uint16_t value);
int lxScriboWinInit(char* dev);
int lxScriboWinWrite(int size, uint8_t *buffer);
int lxScriboWinWriteRead(int wsize, uint8_t *wbuffer
										   , int rsize, unsigned char *rbuffer);
int lxScriboWinSetPin(int pin, int value);
int lxScriboWinGetPin(int pin);
int lxScriboWinVersion(char *buffer);
}

int lxScriboWinInit(char* dev)
{
	return 0;
}
int lxScriboWinWrite(int size, uint8_t *buffer)
{
	return NXP_I2C_Write(size, buffer);
}
int lxScriboWinWriteRead(int wsize, uint8_t *wbuffer
										   , int rsize, unsigned char *rbuffer)
{
	return NXP_I2C_WriteRead(wsize, wbuffer, rsize, rbuffer);
}

int lxScriboWinSetPin(int pin, int value)
{
  //return SetPin(pin, value);
	return 0;
}

int lxScriboWinGetPin(int pin)
{
	uint16_t value;
	//GetPin(pin, &value);
	return value;
}
int lxScriboWinVersion(char *buffer)
{
	unsigned short size;
	//return VersionStr(buffer, &size);
	return 0;
}
