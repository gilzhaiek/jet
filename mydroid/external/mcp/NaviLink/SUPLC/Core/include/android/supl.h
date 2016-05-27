#ifndef _SUPL_H_
#define _SUPL_H_

namespace Engine {

/***************************************************************************************************
*   Define section
**************************************************************************************************/
#define NULL    0
#define TRUE    1
#define FALSE   0
/**************************************************************************************************
* Types section
*************************************************************************************************/
typedef unsigned char   uint8_t;
typedef unsigned short  uint16_t;
typedef unsigned int    uint32_t;
typedef unsigned long   uint64_t;

typedef signed char     int8_t;
typedef signed short    int16_t;
typedef signed int      int32_t;
typedef signed long     int64_t;
typedef uint8_t         bool_t;

typedef char            char_t; // ascii char

typedef bool_t (*GPSCallBack) (uint16_t,uint32_t,void*);

};

Engine::uint8_t  SUPL_Init(Engine::GPSCallBack);
Engine::uint8_t  SUPL_Deinit();
Engine::uint8_t  SUPL_Control(Engine::uint16_t, Engine::uint32_t, Engine::uint8_t*);
//#if defined(WINXP)
//__declspec(dllimport) void SUPL_StartNWSession(Engine::uint8_t* buf,Engine::uint32_t size);
//__declspec(dllexport) void SUPL_ProvideIMSICode(Engine::uint8_t *pnImsi, Engine::uint32_t nImsiSize);
//#endif

//SUPL command definition
// direction GPSM->SUPL
#define UPL_START_REQ       0x00030000
#define UPL_DATA_REQ        0x00030001
#define UPL_STOP_REQ        0x00030002
#define UPL_NOTIFY_RESP     0x00030003
// direction SUPL->GPSM
#define UPL_DATA_IND        0x00038000
#define UPL_POS_IND         0x00038001
#define UPL_STOP_IND        0x00038002
#define UPL_START_LOC_IND   0x00038003
#define UPL_NOTIFY_IND      0x00038004

#include "common/platformHeader.h"

#endif //#ifndef _SUPL_H_