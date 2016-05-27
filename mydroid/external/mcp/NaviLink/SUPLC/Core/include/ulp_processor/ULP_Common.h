/**
*  @file  ULP_Common.h
*  @brief ulp common routines - init,encode,decode.
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
Ildar Abdullin				01.11.2007	                 initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/


#ifndef	_ULP_COMMON_
#define	_ULP_COMMON_

#include "per_codec/asn_internal.h"
#include "codec/ULP-PDU.h"
#include "codec/SUPLAUTHREQ.h"
#include "codec/SUPLAUTHRESP.h"
#include "codec/Suplend.h"
#include "codec/Suplinit.h"
#include "codec/SUPLPOS.h"
#include "codec/SUPLPOSINIT.h"
#include "codec/SUPLRESPONSE.h"
#include "codec/Suplstart.h"
#include "codec/SUPLRESPONSE.h"
#include "messages/suplmessage.h"
#include "messages/SUPLStart.h"
#include "messages/SUPLPos.h"
#include "messages/SUPLEnd.h"
#include "messages/SUPLPosInit.h"
#include "messages/SUPLPos.h"
#include "messages/SUPLResponse.h"
#include "messages/SUPLInit.h"
#include "common/sessionid.h"
#include "common/MSG.h"
#include "common/endianess.h"
#include "common/SUPL_defines.h"

namespace Codec
{

#define BUFFER_SIZE			1024 //1KB for output buffer
#define WCDMA_NOT_SUPPORTED
#define CDMA_NOT_SUPPORTED


typedef enum
{
   INTEGER_DIV_ROUNDING_NONE     = 0,
   INTEGER_DIV_ROUNDING_FLOORING = 1,
   INTEGER_DIV_ROUNDING_CEILING  = 2

 } INTEGER_DIV_ROUNDING_T;

typedef enum
{
	SUCCESS				  =  0,
	NO_MEMORY			  = -1,
	MAN_PARAM_IS_ABSENT   = -2,
	INVALID_PARAMETER	  = -3,
	UNEXPECTED_MESSAGE	  = -4,
	ENCODE_ERROR		  = -5,
	DECODE_ERROR		  = -6,
	PARSE_ERROR			  = -7,
	VER_NOT_SUPPORTED	  = -8,
	INVALID_SESSION_ID	  = -9
} SUCCESS_ERROR_T;

#define ULP_NEW_OBJ(val,type) do{    \
					val = 0; \
					val = new(std::nothrow) type; \
					if(!val) \
					return NO_MEMORY; \
					if(!val->IsObjectReady()) \
					return NO_MEMORY; \
					} while(0)

#define ULP_NEW_VAL(val,type) do{    \
		val = 0; \
		val = new(nothrow) type; \
		if(!val) \
		return NO_MEMORY; \
	} while(0)


class ULP_Processor
{
public:
	uint8_t out_buffer[BUFFER_SIZE];

public:
	ULP_Processor()
	{
		ulp.length=0;
		ulp.version.maj=MAJ_VERSION;
		ulp.version.servind=SER_VERSION;
		ulp.version.min=MIN_VERSION;
		ulp.sessionID.slpSessionID=NULL;
		ulp.sessionID.setSessionID=NULL;
		ulp.message.present=UlpMessage_PR_NOTHING;
		memset(out_buffer,0,BUFFER_SIZE);
	}
	~ULP_Processor()
	{
	}
	SUCCESS_ERROR_T			EncodeMessage(Engine::MSG* msg);
	SUCCESS_ERROR_T			DecodeMessage(Engine::MSG* msg);
#ifdef _DEBUG
public:
	ULP_PDU_t ulp;
#else
private:
	ULP_PDU_t ulp;
#endif
private:
	ULP_Processor(const ULP_Processor&);
	ULP_Processor& operator = (const ULP_Processor&);
private:
	SUCCESS_ERROR_T	EncodeMessage(Engine::CSUPLMessage* message);
	SUCCESS_ERROR_T	DecodeMessage(void* buffer);
	SUCCESS_ERROR_T			ParseMessage(Engine::CSUPLMessage* message);
	Engine::CSUPLMessage*	GetMessageType();
	/*Header functions*/
	SUCCESS_ERROR_T PrepareHeader(Engine::CSUPLMessage* message);
	SUCCESS_ERROR_T ParseHeader(Engine::CSUPLMessage* message);
	/*Uplink*/
	SUCCESS_ERROR_T Encode_SUPLSTART(Engine::CSUPLStart* message);
	SUCCESS_ERROR_T Encode_SUPLPOSINIT(Engine::CSUPLPosInit* message);
	SUCCESS_ERROR_T Encode_SUPLAUTHREQ(Engine::CSUPLMessage* message);
	SUCCESS_ERROR_T Encode_SUPLPOS(Engine::CSUPLPos* message);
	SUCCESS_ERROR_T Encode_SUPLEND(Engine::CSUPLEnd* message);
	/*Downlink*/
	SUCCESS_ERROR_T Parse_SUPLINIT(Engine::CSUPLMessage* message);
	SUCCESS_ERROR_T Parse_SUPLRESPONSE(Engine::CSUPLMessage* message);
	SUCCESS_ERROR_T Parse_SUPLAUTHRESP(Engine::CSUPLMessage* message);
	SUCCESS_ERROR_T Parse_SUPLPOS(Engine::CSUPLMessage* message);
	SUCCESS_ERROR_T Parse_SUPLEND(Engine::CSUPLMessage* message);
	/*Memory Clearing functions*/
#if _DEBUG
public:
	void ClearCodecMemory();
#else
private:
	void ClearCodecMemory();
#endif
	void ClearHeader();
	void Clear_SUPLSTART();
	void Clear_SUPLPOSINIT();
	void Clear_SUPLUTHREQ();
	void Clear_SUPLPOS();
	void Clear_SUPLEND();
	void Clear_SUPLRESPONSE();
	void Clear_SUPLINIT();

private:
	int IntegerDiv(int32_t a, int32_t b, INTEGER_DIV_ROUNDING_T divType);
};

}
//ULP_Processor ULP_Processor::proc_instance;

#endif	/* _ULP_COMMON_ */
