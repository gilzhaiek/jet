/**
*  @file  SUPLService.h
*  @brief Windows service wrapper
*/

/***************************************************************************************************
====================================================================================================

Copyright (c) 2007 Teleca Inc. All rights reserved.
Redistribution and modifications are permitted subject to BSD license. 

====================================================================================================
Revision History:
Modification     Tracking
Author                          Date          Number    Description of Changes
-------------------------   ------------    ----------  --------------------------------------------
Olga Smirnova				 01.12.2007	                 initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#ifndef _SUPL_SERVICE_H_
#define _SUPL_SERVICE_H_
#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <stdlib.h>
#include "common/types.h"
#include "gps/GPS.h"
#include "gps/GPSCommand.h"
#include "suplcontroller/SUPLController.h"
// #ifdef WINCE
// #include <service.h>
// #endif
using namespace Engine;
using namespace Platform;

// direction GPSM->SUPL
#define UPL_START_REQ		0x00030000
#define UPL_DATA_REQ		0x00030001
#define UPL_STOP_REQ		0x00030002
#define	UPL_NOTIFY_RESP		0x00030003
// direction SUPL->GPSM
#define UPL_DATA_IND		0x00038000
#define UPL_POS_IND			0x00038001
#define UPL_STOP_IND		0x00038002
#define UPL_START_LOC_IND	0x00038003
#define UPL_NOTIFY_IND		0x00038004


// #ifdef __cplusplus
// extern "C" {
// #endif


__declspec(dllexport) Engine::uint8_t SUPL_Init(GPSCallBack);
__declspec(dllexport) Engine::uint8_t SUPL_Deinit();
__declspec(dllexport) Engine::uint8_t SUPL_Control(Engine::uint16_t, Engine::uint32_t, Engine::uint8_t*, Engine::uint32_t);
#ifdef _WIN32
__declspec(dllexport) void SUPL_StartNWSession(Engine::uint8_t* buf,Engine::uint32_t size);
#endif

// #ifdef __cplusplus
// }
// #endif


#endif // _SUPL_SERVICE_H_