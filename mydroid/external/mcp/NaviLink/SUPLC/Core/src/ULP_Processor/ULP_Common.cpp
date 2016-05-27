/**
*  @file  ULP_Common.cpp
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
Ildar Abdullin				 01.11.2007	                 initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#include <new>
#include "ulp_processor/ULP_Common.h"
#include "utils/Log.h"
#include "device/ags_LinDevice.h"

using namespace std;
using namespace Codec;


SUCCESS_ERROR_T ULP_Processor::EncodeMessage(Engine::MSG* msg)
{
	SUCCESS_ERROR_T result;

	result = EncodeMessage(msg->m_pOutgoingMessage);
	if(result != SUCCESS)
	{return result;}

	msg->m_pOutgoingRawData = out_buffer;
	msg->m_OutgoingRawDataSize=*((uint16_t*)out_buffer);
	msg->m_OutgoingRawDataSize = RevShortFromShort(msg->m_OutgoingRawDataSize);

	return result;
}

SUCCESS_ERROR_T ULP_Processor::DecodeMessage(Engine::MSG* msg)
{
	SUCCESS_ERROR_T result;

	result = DecodeMessage(msg->m_pIncomingRawData);
	if(result!=SUCCESS)
	{
		return result;
	}

	msg->m_pIncomingMessage = GetMessageType();
	if (!msg->m_pIncomingMessage)
	{
		return NO_MEMORY;
	}

	result = ParseMessage(msg->m_pIncomingMessage);
	if(result!=SUCCESS)
	{
		return result;
	}

	return result;
}

int ULP_Processor::IntegerDiv(int32_t a, int32_t b, INTEGER_DIV_ROUNDING_T divType)
{
	uint32_t  resultNegative = 0;
	uint32_t  remainderNonZero;
	int32_t  result;

	if (a < 0)
	{
		a = -a;
		resultNegative = 1;
	}

	if (b < 0)
	{
		b = -b;
		resultNegative = (resultNegative == 1) ? 0 : 1;
	}

	result = (int)(a/b);

	remainderNonZero = ((a-(b*result)) > 0) ? 1 : 0;

	if (resultNegative == 1)
	{
		result = -result;

		if ( (divType == INTEGER_DIV_ROUNDING_FLOORING)
			&& (remainderNonZero == 1) )
		{
			result--;
		}
	}
	else
	{
		if ( (divType == INTEGER_DIV_ROUNDING_CEILING)
			&& (remainderNonZero == 1) )
		{
			result++;
		}
	}
	return result;
}


SUCCESS_ERROR_T ULP_Processor::EncodeMessage(Engine::CSUPLMessage* message)
{
	asn_enc_rval_t ret_val;
	static uint32_t bytes_encoded=0;
	SUCCESS_ERROR_T result;
	
	if(!message)
	{
		return INVALID_PARAMETER;
	}
	
	result=PrepareHeader(message);

	if(result!=SUCCESS)
	{
		ClearCodecMemory();
		return result;
	}
	
	switch(message->GetType())
	{
		case Engine::SUPL_START:	result=Encode_SUPLSTART((Engine::CSUPLStart*)(message)); break;
		//case /*SUPLUTHREQ*/:	result=Encode_SUPLUTHREQ(/*Message structure as parameter*/); break;
		case Engine::SUPL_POS_INIT:	result=Encode_SUPLPOSINIT((Engine::CSUPLPosInit*)(message)); break;
		case Engine::SUPL_POS:		result=Encode_SUPLPOS((Engine::CSUPLPos*)(message));break;
		case Engine::SUPL_END:		result=Encode_SUPLEND((Engine::CSUPLEnd*)(message));break;
		default: return UNEXPECTED_MESSAGE;
	}
	
	if(result!=SUCCESS)
	{
		ClearCodecMemory();
		return result;
	}
	
	if(ulp.message.present == UlpMessage_PR_msSUPLSTART)
	{
	LOGD("SUPL:Before Encode in ULP_Controller \n");
	LOGD("ULP:ULP packet address: %p \n",&ulp);
	LOGD("ULP:SLP SessionID: %p \n",ulp.sessionID.slpSessionID);
	LOGD("ULP:SessionId: %d \n",ulp.sessionID.setSessionID->sessionId);
	if((getSubscriberIdType()) == 1)
	{
	   LOGD("ULP:SessionId: %d \n",*ulp.sessionID.setSessionID->setId.choice.msisdn.buf);
	}
	else
	{
	   LOGD("ULP:SessionId: %d \n",*ulp.sessionID.setSessionID->setId.choice.imsi.buf);
	}
	LOGD("ULP:QoP pointer: %p \n",ulp.message.choice.msSUPLSTART.qoP);
	LOGD("ULP:Cell Info Status: %d \n",*ulp.message.choice.msSUPLSTART.locationId.status.buf);
	if(ulp.message.choice.msSUPLSTART.locationId.cellInfo.present==CellInfo_PR_wcdmaCell)
	{
		LOGD("ULP:Cell MNC: %d \n",ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.wcdmaCell.refMNC);
		LOGD("ULP:Cell MCC: %d \n",ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.wcdmaCell.refMCC);
		LOGD("ULP:Cell LAC: %d \n",ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.wcdmaCell.refUC);
	}
else if(ulp.message.choice.msSUPLSTART.locationId.cellInfo.present==CellInfo_PR_gsmCell)
{
	LOGD("ULP:Cell MNC: %d \n",ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.gsmCell.refMNC);
	LOGD("ULP:Cell MCC: %d \n",ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.gsmCell.refMCC);
	LOGD("ULP:Cell LAC: %d \n",ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.gsmCell.refLAC);
	LOGD("ULP:Cell CI: %d \n",ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.gsmCell.refCI);
	LOGD("ULP:Cell NMR pointer: %p \n",ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.gsmCell.nMR);
	LOGD("ULP:Cell TA pointer: %p \n",ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.gsmCell.tA);
}

	LOGD("ULP:RRLP protocol: %d \n",ulp.message.choice.msSUPLSTART.sETCapabilities.posProtocol.rrlp);
	LOGD("ULP:AGPS Set Assisted: %d \n",ulp.message.choice.msSUPLSTART.sETCapabilities.posTechnology.agpsSETassisted);
	LOGD("ULP:Pref method: %d \n",*ulp.message.choice.msSUPLSTART.sETCapabilities.prefMethod.buf);
	}

	if(ulp.message.present == UlpMessage_PR_msSUPLPOSINIT)
	{
		LOGD("\n ULP: SUPLPOSINIT message encoding \n");
		LOGD("SUPL:Before Encode in ULP_Controller \n");
		LOGD("ULP:ULP packet address: %p \n",&ulp);
		LOGD("ulp.message.choice.msSUPLPOSINIT.requestedAssistData = 0x%x", ulp.message.choice.msSUPLPOSINIT.requestedAssistData);
		/*if(ulp.message.choice.msSUPLPOSINIT.requestedAssistData->ver2_RequestedAssistData_extension)
		{
			if(ulp.message.choice.msSUPLPOSINIT.requestedAssistData->ver2_RequestedAssistData_extension->extendedEphemeris)
				LOGD("ULP:EE validity: %d \n",ulp.message.choice.msSUPLPOSINIT.requestedAssistData->ver2_RequestedAssistData_extension->extendedEphemeris->validity);
			if(ulp.message.choice.msSUPLPOSINIT.requestedAssistData->ver2_RequestedAssistData_extension->extendedEphemerisCheck)
			{
				LOGD("ULP:EE check begin week: %d \n",ulp.message.choice.msSUPLPOSINIT.requestedAssistData->ver2_RequestedAssistData_extension->extendedEphemerisCheck->beginTime.gPSWeek);
				LOGD("ULP:EE check begin hour: %d \n",ulp.message.choice.msSUPLPOSINIT.requestedAssistData->ver2_RequestedAssistData_extension->extendedEphemerisCheck->beginTime.gPSTOWhour);
				LOGD("ULP:EE check begin week: %d \n",ulp.message.choice.msSUPLPOSINIT.requestedAssistData->ver2_RequestedAssistData_extension->extendedEphemerisCheck->endTime.gPSWeek);
				LOGD("ULP:EE check begin hour: %d \n",ulp.message.choice.msSUPLPOSINIT.requestedAssistData->ver2_RequestedAssistData_extension->extendedEphemerisCheck->endTime.gPSTOWhour);
			}
		}*/

	}
	/*Encode whole message*/
	//memset(out_buffer,0,BUFFER_SIZE);
	ret_val=uper_encode_to_buffer((asn_TYPE_descriptor_s*)&asn_DEF_ULP_PDU,&ulp,out_buffer,sizeof(out_buffer));	

	
	if(ret_val.encoded==-1)
	{
		ClearCodecMemory();
		return ENCODE_ERROR;
	}
	bytes_encoded=IntegerDiv(ret_val.encoded,8,INTEGER_DIV_ROUNDING_CEILING);

	printf("\n");
      for(int i=0;i<bytes_encoded;i++)
      {
            printf("0x%x,",out_buffer[i]);
      }
    printf("\n");

	int size = bytes_encoded;
	bytes_encoded=RevShortFromShort(bytes_encoded);
	*((unsigned short*)out_buffer)=bytes_encoded;
	

	ClearCodecMemory();
	LOGD("SUPL:Encoding finished successfuly \n");
	return SUCCESS;
}

SUCCESS_ERROR_T ULP_Processor::DecodeMessage(void* buffer)
{
	asn_dec_rval_t ret_val_dec;
	asn_codec_ctx_t codec_ctx;
	ULP_PDU_t* pULPMessage = 0;
	codec_ctx.max_stack_size=0;
	unsigned short length;
	length=*((unsigned short*)(buffer));
	length=RevShortFromShort(length);
	// reset union memory from trash data.
	memset(&ulp.message, 0, sizeof (ulp.message));
	pULPMessage = &ulp;
	ret_val_dec=uper_decode(&codec_ctx,(asn_TYPE_descriptor_s*)&asn_DEF_ULP_PDU,(void**)&pULPMessage,buffer,length,0,0);
	LOGD("ULP_Processor::DecodeMessage: ret_val_dec.code = %d", ret_val_dec.code);
	if(ret_val_dec.code != RC_OK)
	{
		if(ret_val_dec.code == RC_EINVAL)
		{
			ClearCodecMemory();
			return INVALID_PARAMETER;
		}
		else
		{
			ClearCodecMemory();
			return DECODE_ERROR;
		}
	}

	return SUCCESS;
}

Engine::CSUPLMessage* ULP_Processor::GetMessageType()
{
	Engine::CSUPLMessage* message = NULL;
	switch(ulp.message.present)
	{
		case UlpMessage_PR_msSUPLINIT:			
				message=new(nothrow) Engine::CSUPLInit;		break;
		case UlpMessage_PR_msSUPLPOS:	
				message=new(nothrow) Engine::CSUPLPos;		break;
		case UlpMessage_PR_msSUPLRESPONSE:	
				message=new(nothrow) Engine::CSUPLResponse; break;
		case UlpMessage_PR_msSUPLEND:			
				message=new(nothrow) Engine::CSUPLEnd;		break;
		//case UlpMessage_PR_msSUPLAUTHRESP:	result=Parse_SUPLAUTHRESP(message);break;
		default: message=NULL;
	}
	if(!message)
	{
	ClearCodecMemory();
	message = NULL;
	return 0;
	}
	return message;
}

SUCCESS_ERROR_T ULP_Processor::ParseMessage(Engine::CSUPLMessage* message)
{
	SUCCESS_ERROR_T result;

	result=ParseHeader(message);
	if(result!=SUCCESS)
	{ClearCodecMemory();return result;}

	switch(ulp.message.present)
	{
		case UlpMessage_PR_msSUPLINIT:			
				result=Parse_SUPLINIT(message); break;

		case UlpMessage_PR_msSUPLPOS:	
				result=Parse_SUPLPOS(message); break;

		case UlpMessage_PR_msSUPLRESPONSE:	
				result=Parse_SUPLRESPONSE(message); break;

		case UlpMessage_PR_msSUPLEND:			
				result=Parse_SUPLEND(message); break;
		//case UlpMessage_PR_msSUPLAUTHRESP:	result=Parse_SUPLAUTHRESP(message);break;

		default: result=PARSE_ERROR;
	}
	if(result!=SUCCESS)
	{ClearCodecMemory();return result;}
	
	ClearCodecMemory();
	
	return SUCCESS;
}

void ULP_Processor::ClearCodecMemory()
{
	ClearHeader();
	switch(ulp.message.present)
	{
		case UlpMessage_PR_msSUPLSTART:		Clear_SUPLSTART(); break;
		//case UlpMessage_PR_msSUPLAUTHREQ:	Clear_SUPLUTHREQ(); break;
		case UlpMessage_PR_msSUPLINIT:		Clear_SUPLINIT(); break;
		case UlpMessage_PR_msSUPLPOSINIT:	Clear_SUPLPOSINIT(); break;
		case UlpMessage_PR_msSUPLPOS:		Clear_SUPLPOS();break;
		case UlpMessage_PR_msSUPLRESPONSE:  Clear_SUPLRESPONSE();break;
		case UlpMessage_PR_msSUPLEND:		Clear_SUPLEND();break;
	}

}

SUCCESS_ERROR_T ULP_Processor::PrepareHeader(Engine::CSUPLMessage* message)
{
	/* Prepare hard coded SUPL version*/
	ulp.version.maj=message->m_Version.m_Major;
	ulp.version.min=message->m_Version.m_Minor;
	ulp.version.servind=message->m_Version.m_Servind;

	/*Prepare Message Header: SessionID, Version and put message length at the end of function*/
	if(!message->m_SessionID.iSETSessionID)
		ulp.sessionID.setSessionID=NULL;
	else
	{
		ulp.sessionID.setSessionID=(SetSessionID*)MALLOC(sizeof(SetSessionID));
		if(!ulp.sessionID.setSessionID)
		{return NO_MEMORY;}
		//Parse Message structure and fill the fields
		//1. Parse SetSessionID
		//1.1. Parse SessionId
		ulp.sessionID.setSessionID->sessionId=message->m_SessionID.iSETSessionID->sessionId;
		//1.2. Parse setId
		ulp.sessionID.setSessionID->setId.present=(SETId_PR)message->m_SessionID.iSETSessionID->iSetID->GetType();
		if(ulp.sessionID.setSessionID->setId.present==SETId_PR_msisdn)
		{
			LOGD("\n----In MSISDN choice----\n");
			Engine::MSDNChoice* msdn;
			msdn=(Engine::MSDNChoice*)message->m_SessionID.iSETSessionID->iSetID;
			ulp.sessionID.setSessionID->setId.choice.msisdn.buf=(Engine::uint8_t*)MALLOC(msdn->iMSISDN.m_Size);
			if(!ulp.sessionID.setSessionID->setId.choice.msisdn.buf)
			{return NO_MEMORY;}
			ulp.sessionID.setSessionID->setId.choice.msisdn.size=msdn->iMSISDN.m_Size;
			memcpy(ulp.sessionID.setSessionID->setId.choice.msisdn.buf,msdn->iMSISDN.m_Data,ulp.sessionID.setSessionID->setId.choice.msisdn.size);		
		}
		else if(ulp.sessionID.setSessionID->setId.present==SETId_PR_mdn)
		{
			Engine::MDNChoice* mdn;
			mdn=(Engine::MDNChoice*)message->m_SessionID.iSETSessionID->iSetID;
			ulp.sessionID.setSessionID->setId.choice.mdn.buf=(Engine::uint8_t*)MALLOC(mdn->iMDN.m_Size);
			if(!ulp.sessionID.setSessionID->setId.choice.mdn.buf)
			{return NO_MEMORY;}
			ulp.sessionID.setSessionID->setId.choice.mdn.size=mdn->iMDN.m_Size;
			memcpy(ulp.sessionID.setSessionID->setId.choice.mdn.buf,mdn->iMDN.m_Data,ulp.sessionID.setSessionID->setId.choice.mdn.size);		
		}
		else if(ulp.sessionID.setSessionID->setId.present==SETId_PR_min)
		{
			Engine::MINChoice* min;
			min=(Engine::MINChoice*)message->m_SessionID.iSETSessionID->iSetID;
			ulp.sessionID.setSessionID->setId.choice.min.buf=(Engine::uint8_t*)MALLOC(min->iMin.m_Size);
			if(!ulp.sessionID.setSessionID->setId.choice.min.buf)
			{return NO_MEMORY;}
			ulp.sessionID.setSessionID->setId.choice.min.size=min->iMin.m_Size;
			ulp.sessionID.setSessionID->setId.choice.min.bits_unused=6;
			memcpy(ulp.sessionID.setSessionID->setId.choice.min.buf,min->iMin.m_Data,ulp.sessionID.setSessionID->setId.choice.min.size);		
		}
		else if(ulp.sessionID.setSessionID->setId.present==SETId_PR_imsi)
		{
			Engine::IMSIChoice* imsi;
			imsi=(Engine::IMSIChoice*)message->m_SessionID.iSETSessionID->iSetID;
			ulp.sessionID.setSessionID->setId.choice.imsi.buf=(Engine::uint8_t*)MALLOC(imsi->iIMSI.m_Size);
			if(!ulp.sessionID.setSessionID->setId.choice.imsi.buf)
			{return NO_MEMORY;}
			ulp.sessionID.setSessionID->setId.choice.imsi.size=imsi->iIMSI.m_Size;
			memcpy(ulp.sessionID.setSessionID->setId.choice.imsi.buf,imsi->iIMSI.m_Data,ulp.sessionID.setSessionID->setId.choice.imsi.size);
		}
		else if(ulp.sessionID.setSessionID->setId.present==SETId_PR_nai)
		{
			Engine::NAIChoice* nai;
			nai=(Engine::NAIChoice*)message->m_SessionID.iSETSessionID->iSetID;
			ulp.sessionID.setSessionID->setId.choice.nai.buf=(Engine::uint8_t*)MALLOC(nai->iNai.m_Size);
			if(!ulp.sessionID.setSessionID->setId.choice.nai.buf)
			{return NO_MEMORY;}
			ulp.sessionID.setSessionID->setId.choice.nai.size=nai->iNai.m_Size;
			memcpy(ulp.sessionID.setSessionID->setId.choice.nai.buf,nai->iNai.m_Data,ulp.sessionID.setSessionID->setId.choice.nai.size);
		}
		else if(ulp.sessionID.setSessionID->setId.present==SETId_PR_iPAddress)
		{
			Engine::CIPAddress* addr = (Engine::CIPAddress*)message->m_SessionID.iSETSessionID->iSetID;
			ulp.sessionID.setSessionID->setId.choice.iPAddress.present=(IPAddress_PR)addr->iIP_Addr->GetType();
			if(ulp.sessionID.setSessionID->setId.choice.iPAddress.present==IPAddress_PR_ipv4Address)
			{
				Engine::CIPv4* ip_v4;
				ip_v4=(Engine::CIPv4*)addr->iIP_Addr;
				ulp.sessionID.setSessionID->setId.choice.iPAddress.choice.ipv4Address.buf=(Engine::uint8_t*)MALLOC(ip_v4->m_Addr.m_Size);
				if(!ulp.sessionID.setSessionID->setId.choice.iPAddress.choice.ipv4Address.buf)
				{return NO_MEMORY;}
				ulp.sessionID.setSessionID->setId.choice.iPAddress.choice.ipv4Address.size=ip_v4->m_Addr.m_Size;
				memcpy(ulp.sessionID.setSessionID->setId.choice.iPAddress.choice.ipv4Address.buf,ip_v4->m_Addr.m_Data,ulp.sessionID.setSessionID->setId.choice.iPAddress.choice.ipv4Address.size);
			}
			else if(ulp.sessionID.setSessionID->setId.choice.iPAddress.present==IPAddress_PR_ipv6Address)
			{
				Engine::CIPv6* ip_v6;
				ip_v6=(Engine::CIPv6*)addr->iIP_Addr;
				ulp.sessionID.setSessionID->setId.choice.iPAddress.choice.ipv6Address.buf=(Engine::uint8_t*)MALLOC(ip_v6->m_Addr.m_Size);
				if(!ulp.sessionID.setSessionID->setId.choice.iPAddress.choice.ipv6Address.buf)
				{return NO_MEMORY;}
				ulp.sessionID.setSessionID->setId.choice.iPAddress.choice.ipv6Address.size=ip_v6->m_Addr.m_Size;
				memcpy(ulp.sessionID.setSessionID->setId.choice.iPAddress.choice.ipv6Address.buf,ip_v6->m_Addr.m_Data,ulp.sessionID.setSessionID->setId.choice.iPAddress.choice.ipv6Address.size);
			}
		else 
			return MAN_PARAM_IS_ABSENT;		
		}
		else
			return MAN_PARAM_IS_ABSENT;
	}
	//2. Prepare SlpSessionID
	if(!message->m_SessionID.iSLPSessionID)
		ulp.sessionID.slpSessionID=NULL;
	else
	{
		ulp.sessionID.slpSessionID=(SlpSessionID*)MALLOC(sizeof(SlpSessionID));
		if(!ulp.sessionID.slpSessionID)
		{return NO_MEMORY;}
		ulp.sessionID.slpSessionID->sessionID.buf=(Engine::uint8_t*)MALLOC(4);
		ulp.sessionID.slpSessionID->sessionID.size=4;
		if(!ulp.sessionID.slpSessionID->sessionID.buf)
		{return NO_MEMORY;}
		memcpy(ulp.sessionID.slpSessionID->sessionID.buf,message->m_SessionID.iSLPSessionID->iSessionId.m_Data,4);
		
		ulp.sessionID.slpSessionID->slpId.present=(SLPAddress_PR)message->m_SessionID.iSLPSessionID->iSLPaddr->GetType();
		if(ulp.sessionID.slpSessionID->slpId.present==SLPAddress_PR_iPAddress)
		{
			Engine::CIPAddress* ip_address;
			ip_address=(Engine::CIPAddress*)message->m_SessionID.iSLPSessionID->iSLPaddr;
			ulp.sessionID.slpSessionID->slpId.choice.iPAddress.present=(IPAddress_PR)ip_address->iIP_Addr->GetType();
			if(ulp.sessionID.slpSessionID->slpId.choice.iPAddress.present==IPAddress_PR_ipv4Address)
			{
				Engine::CIPv4* ip_v4;
				ip_v4=(Engine::CIPv4*)ip_address->iIP_Addr;
				ulp.sessionID.slpSessionID->slpId.choice.iPAddress.choice.ipv4Address.buf=(Engine::uint8_t*)MALLOC(4);
				if(!ulp.sessionID.slpSessionID->slpId.choice.iPAddress.choice.ipv4Address.buf)
				{return NO_MEMORY;}
				ulp.sessionID.slpSessionID->slpId.choice.iPAddress.choice.ipv4Address.size=4;
				memcpy(ulp.sessionID.slpSessionID->slpId.choice.iPAddress.choice.ipv4Address.buf,ip_v4->m_Addr.m_Data,4);
			}
			else if(ulp.sessionID.slpSessionID->slpId.choice.iPAddress.present==IPAddress_PR_ipv6Address)
			{
				Engine::CIPv6* ip_v6;
				ip_v6=(Engine::CIPv6*)ip_address->iIP_Addr;
				ulp.sessionID.slpSessionID->slpId.choice.iPAddress.choice.ipv6Address.buf=(Engine::uint8_t*)MALLOC(16);
				if(!ulp.sessionID.slpSessionID->slpId.choice.iPAddress.choice.ipv6Address.buf)
				{return NO_MEMORY;}
				ulp.sessionID.slpSessionID->slpId.choice.iPAddress.choice.ipv6Address.size=16;
				memcpy(ulp.sessionID.slpSessionID->slpId.choice.iPAddress.choice.ipv6Address.buf,ip_v6->m_Addr.m_Data,16);
			}
			else
				return MAN_PARAM_IS_ABSENT;
		}
		else if (ulp.sessionID.slpSessionID->slpId.present==SLPAddress_PR_fQDN)
		{
			Engine::CFQDN* fqdn;
			fqdn=(Engine::CFQDN*)message->m_SessionID.iSLPSessionID->iSLPaddr;
			ulp.sessionID.slpSessionID->slpId.choice.fQDN.buf=(Engine::uint8_t*)MALLOC(fqdn->m_FQDN.m_Size);
			if(!ulp.sessionID.slpSessionID->slpId.choice.fQDN.buf)
			{return NO_MEMORY;}
			ulp.sessionID.slpSessionID->slpId.choice.fQDN.size=fqdn->m_FQDN.m_Size;
			memcpy(ulp.sessionID.slpSessionID->slpId.choice.fQDN.buf,fqdn->m_FQDN.m_Data,ulp.sessionID.slpSessionID->slpId.choice.fQDN.size);
		}
		else 
			return MAN_PARAM_IS_ABSENT;	
	}

	return SUCCESS;
}

void ULP_Processor::ClearHeader()
{
	if(ulp.sessionID.setSessionID)
	{
		if(ulp.sessionID.setSessionID->setId.present==SETId_PR_msisdn)
		{
			if(ulp.sessionID.setSessionID->setId.choice.msisdn.buf)
			{
				FREEMEM(ulp.sessionID.setSessionID->setId.choice.msisdn.buf);
			}
		}
		else if(ulp.sessionID.setSessionID->setId.present==SETId_PR_mdn)
		{
			if(ulp.sessionID.setSessionID->setId.choice.mdn.buf)
			{
				FREEMEM(ulp.sessionID.setSessionID->setId.choice.mdn.buf);
			}
		}
		else if(ulp.sessionID.setSessionID->setId.present==SETId_PR_min)
		{
			if(ulp.sessionID.setSessionID->setId.choice.min.buf)
			{
				FREEMEM(ulp.sessionID.setSessionID->setId.choice.min.buf);
			}
		}
		else if(ulp.sessionID.setSessionID->setId.present==SETId_PR_imsi)
		{
			if(ulp.sessionID.setSessionID->setId.choice.imsi.buf)
			{
				FREEMEM(ulp.sessionID.setSessionID->setId.choice.imsi.buf);
			}
		}
		else if(ulp.sessionID.setSessionID->setId.present==SETId_PR_nai)
		{
			if(ulp.sessionID.setSessionID->setId.choice.nai.buf)
			{
				FREEMEM(ulp.sessionID.setSessionID->setId.choice.nai.buf);
			}
		}
		else if(ulp.sessionID.setSessionID->setId.present==SETId_PR_iPAddress)
		{
			if(ulp.sessionID.setSessionID->setId.choice.iPAddress.present==IPAddress_PR_ipv4Address)
			{
				if(ulp.sessionID.setSessionID->setId.choice.iPAddress.choice.ipv4Address.buf)
				{
					FREEMEM(ulp.sessionID.setSessionID->setId.choice.iPAddress.choice.ipv4Address.buf);
				}
			}
			else if(ulp.sessionID.setSessionID->setId.choice.iPAddress.present==IPAddress_PR_ipv6Address)
			{
				if(ulp.sessionID.setSessionID->setId.choice.iPAddress.choice.ipv6Address.buf)
				{
					FREEMEM(ulp.sessionID.setSessionID->setId.choice.iPAddress.choice.ipv6Address.buf);
				}
			}
		}
		if(ulp.sessionID.setSessionID)
		{
			FREEMEM(ulp.sessionID.setSessionID);
		}
	}
	if(ulp.sessionID.slpSessionID)
	{
		if(ulp.sessionID.slpSessionID->sessionID.buf)
		{
			FREEMEM(ulp.sessionID.slpSessionID->sessionID.buf);
		}
		if(ulp.sessionID.slpSessionID->slpId.present==SLPAddress_PR_iPAddress)
		{
			if(ulp.sessionID.slpSessionID->slpId.choice.iPAddress.present==IPAddress_PR_ipv4Address)
			{
				if(ulp.sessionID.slpSessionID->slpId.choice.iPAddress.choice.ipv4Address.buf)
				{
					FREEMEM(ulp.sessionID.slpSessionID->slpId.choice.iPAddress.choice.ipv4Address.buf);
				}
			}
			else if(ulp.sessionID.slpSessionID->slpId.choice.iPAddress.present==IPAddress_PR_ipv6Address)
			{
				if(ulp.sessionID.slpSessionID->slpId.choice.iPAddress.choice.ipv6Address.buf)
				{
					FREEMEM(ulp.sessionID.slpSessionID->slpId.choice.iPAddress.choice.ipv6Address.buf);
				}
			}
		}
		else if(ulp.sessionID.slpSessionID->slpId.present==SLPAddress_PR_fQDN)
		{
			if(ulp.sessionID.slpSessionID->slpId.choice.fQDN.buf)
			{
				FREEMEM(ulp.sessionID.slpSessionID->slpId.choice.fQDN.buf);
			}
		}
		if(ulp.sessionID.slpSessionID)
		{
			FREEMEM(ulp.sessionID.slpSessionID);
		}
	}
	ulp.version.maj=0;
	ulp.version.min=0;
	ulp.version.servind=0;
}

SUCCESS_ERROR_T ULP_Processor::Encode_SUPLSTART(Engine::CSUPLStart* message)
{
	LOGD("SUPL:In Encode SUPL_START \n");
	long value; //temp variable
	ulp.message.present=UlpMessage_PR_msSUPLSTART;
	//2. Parse Message
	//2.1 Parse Set Cababilities
	//POS_PROTOCOL
	ulp.message.choice.msSUPLSTART.sETCapabilities.posProtocol.rrc=message->m_SETCaps.m_PosProtocol.m_RRC;
	ulp.message.choice.msSUPLSTART.sETCapabilities.posProtocol.rrlp=message->m_SETCaps.m_PosProtocol.m_RRLP;
	ulp.message.choice.msSUPLSTART.sETCapabilities.posProtocol.tia801=message->m_SETCaps.m_PosProtocol.m_TIA801;
	//POS_Technology
	ulp.message.choice.msSUPLSTART.sETCapabilities.posTechnology.aFLT=message->m_SETCaps.m_PosTechnology.m_aFLT;
	ulp.message.choice.msSUPLSTART.sETCapabilities.posTechnology.agpsSETassisted=message->m_SETCaps.m_PosTechnology.m_AGPSSETAssisted;
	ulp.message.choice.msSUPLSTART.sETCapabilities.posTechnology.agpsSETBased=message->m_SETCaps.m_PosTechnology.m_AGPSSETBased;
	ulp.message.choice.msSUPLSTART.sETCapabilities.posTechnology.autonomousGPS=message->m_SETCaps.m_PosTechnology.m_AutonomusGPS;
	ulp.message.choice.msSUPLSTART.sETCapabilities.posTechnology.eCID=message->m_SETCaps.m_PosTechnology.m_eCID;
	ulp.message.choice.msSUPLSTART.sETCapabilities.posTechnology.eOTD=message->m_SETCaps.m_PosTechnology.m_eOTD;
	ulp.message.choice.msSUPLSTART.sETCapabilities.posTechnology.oTDOA=message->m_SETCaps.m_PosTechnology.m_oTDOA;
	//
	ulp.message.choice.msSUPLSTART.sETCapabilities.prefMethod.buf=(Engine::uint8_t*)MALLOC(1);
	if(!ulp.message.choice.msSUPLSTART.sETCapabilities.prefMethod.buf)
	{return NO_MEMORY;}
	ulp.message.choice.msSUPLSTART.sETCapabilities.prefMethod.size=1;
	*(ulp.message.choice.msSUPLSTART.sETCapabilities.prefMethod.buf)=message->m_SETCaps.m_PrefMethod;
	//2.2 Parse LOCATION_ID
	ulp.message.choice.msSUPLSTART.locationId.cellInfo.present=(CellInfo_PR)message->m_LocationID.m_pCellInformation->GetType();
	if(ulp.message.choice.msSUPLSTART.locationId.cellInfo.present==CellInfo_PR_gsmCell)
	{
		Engine::GSMCellInformation* gsm_cell_info;
		gsm_cell_info=(Engine::GSMCellInformation*)message->m_LocationID.m_pCellInformation;
		if(gsm_cell_info->m_aNMR)
		{
			//if(gsm_cell_info->m_aNMR->empty())
			if(gsm_cell_info->m_aNMR->isEmpty()) //Android STL
			{
				ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.gsmCell.nMR=NULL; //OPTIONAL PARAMETER
			}
			else
			{
				Engine::NMR loc_nmr;
				NMRelement* nmr_elem;
				ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.gsmCell.nMR=(NMR*)MALLOC(sizeof(NMR));
				if(!ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.gsmCell.nMR)
				{return NO_MEMORY;}
				ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.gsmCell.nMR->list.size=gsm_cell_info->m_aNMR->size();
				ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.gsmCell.nMR->list.array=(NMRelement**)MALLOC(gsm_cell_info->m_aNMR->size() * sizeof(NMRelement**));
				if(!ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.gsmCell.nMR->list.array)
				{return NO_MEMORY;}
				ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.gsmCell.nMR->list.count=0;
				for(unsigned int i=0;i<gsm_cell_info->m_aNMR->size();i++)
				{

					//loc_nmr=gsm_cell_info->m_aNMR->at(i);
					loc_nmr=gsm_cell_info->m_aNMR->itemAt(i); //Android STL
					nmr_elem=(NMRelement*)MALLOC(sizeof(NMRelement));
					if(!nmr_elem)
					{return NO_MEMORY;}
					nmr_elem->aRFCN=loc_nmr.m_RFCN;
					nmr_elem->bSIC=loc_nmr.m_SIC;
					nmr_elem->rxLev=loc_nmr.m_Lev;
					if(asn_set_add(ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.gsmCell.nMR, nmr_elem) != 0)
					{
// 						ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.gsmCell.nMR=NULL;
// 						break;
						return NO_MEMORY;
					}
				}
			}
		}
		else
		{
			ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.gsmCell.nMR=NULL;
		}
		ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.gsmCell.refCI=gsm_cell_info->m_CI;
		ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.gsmCell.refLAC=gsm_cell_info->m_LAC;
		ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.gsmCell.refMCC=gsm_cell_info->m_MCC;
		ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.gsmCell.refMNC=gsm_cell_info->m_MNC;
		LOGD("\ngsm_cell_info->m_CI = %d",gsm_cell_info->m_CI);
		if(!gsm_cell_info->m_TA)
			ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.gsmCell.tA=NULL;
		else
		{
			value=*(gsm_cell_info->m_TA);
			if((value > 255) || (value < 0)) 
			{return INVALID_PARAMETER;}
			ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.gsmCell.tA=(long*)MALLOC(sizeof(long));
			if(!ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.gsmCell.tA)
			{return NO_MEMORY;}
			*(ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.gsmCell.tA)=value;
		}
	}

	LOGD("\n\n----SUPL_START:Encode Start: WCDMA INFO----\n\n");
	if(ulp.message.choice.msSUPLSTART.locationId.cellInfo.present==CellInfo_PR_wcdmaCell)
	{
		Engine::WCDMACellInformation* wcdma_cell_info;
		wcdma_cell_info=(Engine::WCDMACellInformation*)message->m_LocationID.m_pCellInformation;
		ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.wcdmaCell.refUC=wcdma_cell_info->m_CI;
		ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.wcdmaCell.refMCC=wcdma_cell_info->m_MCC;
		ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.wcdmaCell.refMNC=wcdma_cell_info->m_MNC;
		ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.wcdmaCell.frequencyInfo=NULL;
		ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.wcdmaCell.primaryScramblingCode=NULL;
		ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.wcdmaCell.measuredResultsList=NULL; 
	}
	LOGD("\n\n----SUPL_START:Encode Finish: WCDMA INFO----\n\n");

#ifndef CDMA_NOT_SUPPORTED
	if(ulp.message.choice.msSUPLSTART.locationId.cellInfo.present==CellInfo_PR_cdmaCell)
	{
		CDMACellInformation* gsm_cell_info;
		//CDMA location id data
	}
#endif
/*	else
	{
		return MAN_PARAM_IS_ABSENT;
	}*/
	//2.3 PARSE STATUS 
	ulp.message.choice.msSUPLSTART.locationId.status.buf=(Engine::uint8_t*)MALLOC(1);
	if(!ulp.message.choice.msSUPLSTART.locationId.status.buf)
	{return NO_MEMORY;}
	ulp.message.choice.msSUPLSTART.locationId.status.size=1;
	*(ulp.message.choice.msSUPLSTART.locationId.status.buf)= message->m_LocationID.m_Status;
	//3. QOP
	if(!message->m_pQoP)
	{
		LOGD("QOP is null\n");
		ulp.message.choice.msSUPLSTART.qoP=NULL;
	}
	else 
	{
		ulp.message.choice.msSUPLSTART.qoP=(QoP_t*)MALLOC(sizeof(QoP_t));
		if(!ulp.message.choice.msSUPLSTART.qoP)
		{return NO_MEMORY;}
		//Following code added to avoid crash further
		ulp.message.choice.msSUPLSTART.qoP->delay=NULL;
		ulp.message.choice.msSUPLSTART.qoP->maxLocAge=NULL;
		ulp.message.choice.msSUPLSTART.qoP->veracc=NULL;

		value = message->m_pQoP->m_Horacc;
		if(value < 0 || value > 127)
			value = 127; //If you don't get good QOP hor accuracy, RESet to defult value as QOP is optional

		ulp.message.choice.msSUPLSTART.qoP->horacc= value;

		
		if(!message->m_pQoP->m_pDelay)
			ulp.message.choice.msSUPLSTART.qoP->delay=NULL;
		else
		{
			value=*(message->m_pQoP->m_pDelay);

			if((value < 0) || (value > 7))
			{
				value = 7;
			}
			
			ulp.message.choice.msSUPLSTART.qoP->delay=(long*)MALLOC(sizeof(long));

			if(!ulp.message.choice.msSUPLSTART.qoP->delay)
			{   
				return NO_MEMORY;
			}
			*(ulp.message.choice.msSUPLSTART.qoP->delay)=value;
			
		}
		if(!message->m_pQoP->m_pMaxLocAge)
			ulp.message.choice.msSUPLSTART.qoP->maxLocAge=NULL;
		else
		{
			value=*(message->m_pQoP->m_pMaxLocAge);
			if((value < 0) || (value > 65535))
			{ 
				value =65535;
			}
			ulp.message.choice.msSUPLSTART.qoP->maxLocAge=(long*)MALLOC(sizeof(long));
			if(!ulp.message.choice.msSUPLSTART.qoP->maxLocAge)
			{
				return NO_MEMORY;
			}
			*(ulp.message.choice.msSUPLSTART.qoP->maxLocAge)=value;
			
		}
		if(!message->m_pQoP->m_pVeracc)
			ulp.message.choice.msSUPLSTART.qoP->veracc=NULL;
		else
		{
			value=*(message->m_pQoP->m_pVeracc);
			if((value < 0) || (value > 127)) 
			{
				value=127;
			}
			ulp.message.choice.msSUPLSTART.qoP->veracc=(long*)MALLOC(sizeof(long));
			if(!ulp.message.choice.msSUPLSTART.qoP->veracc)
			{
				return NO_MEMORY;
			}
			*(ulp.message.choice.msSUPLSTART.qoP->veracc)=value;
			
		}
	}
	
	return SUCCESS;
}

void ULP_Processor::Clear_SUPLSTART()
{
	ulp.message.present=UlpMessage_PR_NOTHING;
	if(ulp.message.choice.msSUPLSTART.sETCapabilities.prefMethod.buf)
	{
		FREEMEM(ulp.message.choice.msSUPLSTART.sETCapabilities.prefMethod.buf);
	}
	if(ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.gsmCell.tA)
	{
		FREEMEM(ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.gsmCell.tA);
	}
	if(ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.gsmCell.nMR)
	{
		for(int i=0;i<ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.gsmCell.nMR->list.size;i++)
		{
			FREEMEM(ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.gsmCell.nMR->list.array[i]);
		}
		FREEMEM(ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.gsmCell.nMR->list.array);
		FREEMEM(ulp.message.choice.msSUPLSTART.locationId.cellInfo.choice.gsmCell.nMR);
	}
	if(ulp.message.choice.msSUPLSTART.locationId.status.buf)
	{
		FREEMEM(ulp.message.choice.msSUPLSTART.locationId.status.buf);
	}
	if(ulp.message.choice.msSUPLSTART.qoP)
	{
		if(ulp.message.choice.msSUPLSTART.qoP->delay)
		{
			FREEMEM(ulp.message.choice.msSUPLSTART.qoP->delay);
		}
		if(ulp.message.choice.msSUPLSTART.qoP->maxLocAge)	
		{
			FREEMEM(ulp.message.choice.msSUPLSTART.qoP->maxLocAge);
		}
		if(ulp.message.choice.msSUPLSTART.qoP->veracc)
		{

			FREEMEM(ulp.message.choice.msSUPLSTART.qoP->veracc);

		}
		FREEMEM(ulp.message.choice.msSUPLSTART.qoP);
		ulp.message.choice.msSUPLSTART.qoP=NULL;
	}

}

SUCCESS_ERROR_T ULP_Processor::Encode_SUPLPOSINIT(Engine::CSUPLPosInit* message)
{
	long value; //temp variable
	ulp.message.present=UlpMessage_PR_msSUPLPOSINIT;
	//SET CAPABILITIES
	ulp.message.choice.msSUPLPOSINIT.sETCapabilities.posProtocol.rrc=message->m_SETCapabilities.m_PosProtocol.m_RRC;
	ulp.message.choice.msSUPLPOSINIT.sETCapabilities.posProtocol.rrlp=message->m_SETCapabilities.m_PosProtocol.m_RRLP;
	ulp.message.choice.msSUPLPOSINIT.sETCapabilities.posProtocol.tia801=message->m_SETCapabilities.m_PosProtocol.m_TIA801;
	//POS_Technology
	ulp.message.choice.msSUPLPOSINIT.sETCapabilities.posTechnology.aFLT=message->m_SETCapabilities.m_PosTechnology.m_aFLT;
	ulp.message.choice.msSUPLPOSINIT.sETCapabilities.posTechnology.agpsSETassisted=message->m_SETCapabilities.m_PosTechnology.m_AGPSSETAssisted;
	ulp.message.choice.msSUPLPOSINIT.sETCapabilities.posTechnology.agpsSETBased=message->m_SETCapabilities.m_PosTechnology.m_AGPSSETBased;
	ulp.message.choice.msSUPLPOSINIT.sETCapabilities.posTechnology.autonomousGPS=message->m_SETCapabilities.m_PosTechnology.m_AutonomusGPS;
	ulp.message.choice.msSUPLPOSINIT.sETCapabilities.posTechnology.eCID=message->m_SETCapabilities.m_PosTechnology.m_eCID;
	ulp.message.choice.msSUPLPOSINIT.sETCapabilities.posTechnology.eOTD=message->m_SETCapabilities.m_PosTechnology.m_eOTD;
	ulp.message.choice.msSUPLPOSINIT.sETCapabilities.posTechnology.oTDOA=message->m_SETCapabilities.m_PosTechnology.m_oTDOA;
	//
	ulp.message.choice.msSUPLPOSINIT.sETCapabilities.prefMethod.buf=(Engine::uint8_t*)MALLOC(1);
	if(!ulp.message.choice.msSUPLPOSINIT.sETCapabilities.prefMethod.buf)
	{return NO_MEMORY;}
	ulp.message.choice.msSUPLPOSINIT.sETCapabilities.prefMethod.size=1;
	*(ulp.message.choice.msSUPLPOSINIT.sETCapabilities.prefMethod.buf)=message->m_SETCapabilities.m_PrefMethod;
	//LOCATION ID
	ulp.message.choice.msSUPLPOSINIT.locationId.cellInfo.present=(CellInfo_PR)message->m_LocationID.m_pCellInformation->GetType();
	if(ulp.message.choice.msSUPLPOSINIT.locationId.cellInfo.present==CellInfo_PR_gsmCell)
	{
		Engine::GSMCellInformation* gsm_cell_info;
		gsm_cell_info=(Engine::GSMCellInformation*)message->m_LocationID.m_pCellInformation;
		if(gsm_cell_info->m_aNMR)
		{
			//if(gsm_cell_info->m_aNMR->empty())
			if(gsm_cell_info->m_aNMR->isEmpty()) //Android STL
			{
				ulp.message.choice.msSUPLPOSINIT.locationId.cellInfo.choice.gsmCell.nMR=NULL; //OPTIONAL PARAMETER
			}
			else
			{
				Engine::NMR loc_nmr;
				NMRelement* nmr_elem;
				ulp.message.choice.msSUPLPOSINIT.locationId.cellInfo.choice.gsmCell.nMR=(NMR*)MALLOC(sizeof(NMR));
				if(!ulp.message.choice.msSUPLPOSINIT.locationId.cellInfo.choice.gsmCell.nMR)
				{return NO_MEMORY;}
				ulp.message.choice.msSUPLPOSINIT.locationId.cellInfo.choice.gsmCell.nMR->list.size=gsm_cell_info->m_aNMR->size();
				ulp.message.choice.msSUPLPOSINIT.locationId.cellInfo.choice.gsmCell.nMR->list.array=(NMRelement**)MALLOC(gsm_cell_info->m_aNMR->size() * sizeof(NMRelement**));
				if(!ulp.message.choice.msSUPLPOSINIT.locationId.cellInfo.choice.gsmCell.nMR->list.array)
				{return NO_MEMORY;}
				ulp.message.choice.msSUPLPOSINIT.locationId.cellInfo.choice.gsmCell.nMR->list.count=0;
				for(unsigned int i=0;i<gsm_cell_info->m_aNMR->size();i++)
				{

					//loc_nmr=gsm_cell_info->m_aNMR->at(i);
					loc_nmr=gsm_cell_info->m_aNMR->itemAt(i); //Android STL
					nmr_elem=(NMRelement*)MALLOC(sizeof(NMRelement));
					if(!nmr_elem)
					{return NO_MEMORY;}
					nmr_elem->aRFCN=loc_nmr.m_RFCN;
					nmr_elem->bSIC=loc_nmr.m_SIC;
					nmr_elem->rxLev=loc_nmr.m_Lev;
					if(asn_set_add(ulp.message.choice.msSUPLPOSINIT.locationId.cellInfo.choice.gsmCell.nMR, nmr_elem) != 0)
					{
// 						ulp.message.choice.msSUPLPOSINIT.locationId.cellInfo.choice.gsmCell.nMR=NULL;
// 						break;
						return NO_MEMORY;
					}
				}
			}
		}
		else
		{
			ulp.message.choice.msSUPLPOSINIT.locationId.cellInfo.choice.gsmCell.nMR=NULL;
		}
		ulp.message.choice.msSUPLPOSINIT.locationId.cellInfo.choice.gsmCell.refCI=gsm_cell_info->m_CI;
		ulp.message.choice.msSUPLPOSINIT.locationId.cellInfo.choice.gsmCell.refLAC=gsm_cell_info->m_LAC;
		ulp.message.choice.msSUPLPOSINIT.locationId.cellInfo.choice.gsmCell.refMCC=gsm_cell_info->m_MCC;
		ulp.message.choice.msSUPLPOSINIT.locationId.cellInfo.choice.gsmCell.refMNC=gsm_cell_info->m_MNC;
		if(!gsm_cell_info->m_TA)
			ulp.message.choice.msSUPLPOSINIT.locationId.cellInfo.choice.gsmCell.tA=NULL;
		else
		{
			value=*(gsm_cell_info->m_TA);
			if((value > 255) || (value < 0)) 
			{return INVALID_PARAMETER;}
			ulp.message.choice.msSUPLPOSINIT.locationId.cellInfo.choice.gsmCell.tA=(long*)MALLOC(sizeof(long));
			if(!ulp.message.choice.msSUPLPOSINIT.locationId.cellInfo.choice.gsmCell.tA)
			{return NO_MEMORY;}
			*(ulp.message.choice.msSUPLPOSINIT.locationId.cellInfo.choice.gsmCell.tA)=value;
		}
	}
	LOGD("\n\n----SUPL_POSINIT:Encode Start: WCDMA info----\n\n");
	if(ulp.message.choice.msSUPLPOSINIT.locationId.cellInfo.present==CellInfo_PR_wcdmaCell)
	{
		Engine::WCDMACellInformation* wcdma_cell_info;
		wcdma_cell_info=(Engine::WCDMACellInformation*)message->m_LocationID.m_pCellInformation; 
		ulp.message.choice.msSUPLPOSINIT.locationId.cellInfo.choice.wcdmaCell.refUC=wcdma_cell_info->m_CI;
		ulp.message.choice.msSUPLPOSINIT.locationId.cellInfo.choice.wcdmaCell.refMCC=wcdma_cell_info->m_MCC;
		ulp.message.choice.msSUPLPOSINIT.locationId.cellInfo.choice.wcdmaCell.refMNC=wcdma_cell_info->m_MNC;
		ulp.message.choice.msSUPLPOSINIT.locationId.cellInfo.choice.wcdmaCell.frequencyInfo=NULL;
		ulp.message.choice.msSUPLPOSINIT.locationId.cellInfo.choice.wcdmaCell.primaryScramblingCode=NULL;
		ulp.message.choice.msSUPLPOSINIT.locationId.cellInfo.choice.wcdmaCell.measuredResultsList=NULL;  
	}
	LOGD("\n\n----SUPL_POSINIT:Encode Finish: WCDMA INFO----\n\n");

#ifndef CDMA_NOT_SUPPORTED
	if(ulp.message.choice.msSUPLPOSINIT.locationId.cellInfo.present==CellInfo_PR_cdmaCell)
	{
		CDMACellInformation* cdma_cell_info;
		//CDMA location id data
	}
#endif
/*	else
	{
		return MAN_PARAM_IS_ABSENT;
	}*/
	//STATUS
	ulp.message.choice.msSUPLPOSINIT.locationId.status.buf=(Engine::uint8_t*)MALLOC(1);
	if(!ulp.message.choice.msSUPLPOSINIT.locationId.status.buf)
	{return NO_MEMORY;}
	ulp.message.choice.msSUPLPOSINIT.locationId.status.size=1;
	*(ulp.message.choice.msSUPLPOSINIT.locationId.status.buf)=message->m_LocationID.m_Status;
	//REQUEST ASSIST DATA OPTIONAL
	if(!message->m_pRequestedAssistData)
		ulp.message.choice.msSUPLPOSINIT.requestedAssistData=NULL;
	else
	{
		ulp.message.choice.msSUPLPOSINIT.requestedAssistData=(RequestedAssistData_t*)MALLOC(sizeof(RequestedAssistData_t));
		if(!ulp.message.choice.msSUPLPOSINIT.requestedAssistData)
		{return NO_MEMORY;}
		ulp.message.choice.msSUPLPOSINIT.requestedAssistData->acquisitionAssistanceRequested=message->m_pRequestedAssistData->m_AcquisitionAssistanceRequested;
		ulp.message.choice.msSUPLPOSINIT.requestedAssistData->almanacRequested=message->m_pRequestedAssistData->m_AlmanacRequested;
		ulp.message.choice.msSUPLPOSINIT.requestedAssistData->dgpsCorrectionsRequested=message->m_pRequestedAssistData->m_DGPSCorrectionsRequested;
		ulp.message.choice.msSUPLPOSINIT.requestedAssistData->ionosphericModelRequested=message->m_pRequestedAssistData->m_IonosphericModeRequested;
		ulp.message.choice.msSUPLPOSINIT.requestedAssistData->realTimeIntegrityRequested=message->m_pRequestedAssistData->m_RealTimeIntegrityRequested;
		ulp.message.choice.msSUPLPOSINIT.requestedAssistData->referenceLocationRequested=message->m_pRequestedAssistData->m_ReferenceLocationRequested;
		ulp.message.choice.msSUPLPOSINIT.requestedAssistData->referenceTimeRequested=message->m_pRequestedAssistData->m_ReferenceTimeRequested;
		ulp.message.choice.msSUPLPOSINIT.requestedAssistData->utcModelRequested=message->m_pRequestedAssistData->m_UTCModeRequested;
		ulp.message.choice.msSUPLPOSINIT.requestedAssistData->navigationModelRequested=message->m_pRequestedAssistData->m_NavigationModelRequested;
		if(message->m_pRequestedAssistData->m_pNavigationModelData)
		{
			ulp.message.choice.msSUPLPOSINIT.requestedAssistData->navigationModelData=(NavigationModel_t*)MALLOC(sizeof(NavigationModel_t));
			if(!ulp.message.choice.msSUPLPOSINIT.requestedAssistData->navigationModelData)
			{return NO_MEMORY;}
			ulp.message.choice.msSUPLPOSINIT.requestedAssistData->navigationModelData->gpsToe=message->m_pRequestedAssistData->m_pNavigationModelData->m_GPSToe;
			ulp.message.choice.msSUPLPOSINIT.requestedAssistData->navigationModelData->gpsWeek=message->m_pRequestedAssistData->m_pNavigationModelData->m_GPSWeek;
			ulp.message.choice.msSUPLPOSINIT.requestedAssistData->navigationModelData->nSAT=message->m_pRequestedAssistData->m_pNavigationModelData->m_NSAT;
			ulp.message.choice.msSUPLPOSINIT.requestedAssistData->navigationModelData->toeLimit=message->m_pRequestedAssistData->m_pNavigationModelData->m_ToeLimit;
			if(message->m_pRequestedAssistData->m_pNavigationModelData->m_SatInfo)
			{
				//if(message->m_pRequestedAssistData->m_pNavigationModelData->m_SatInfo->empty())
				if(message->m_pRequestedAssistData->m_pNavigationModelData->m_SatInfo->isEmpty()) //Android STL
				{
					ulp.message.choice.msSUPLPOSINIT.requestedAssistData->navigationModelData->satInfo=NULL;
				}
				else
				{
					Engine::SatelliteInfoElement sat_info;
					SatelliteInfoElement* sat_info_elem;
					ulp.message.choice.msSUPLPOSINIT.requestedAssistData->navigationModelData->satInfo=(SatelliteInfo*)MALLOC(sizeof(SatelliteInfo));
					if(!ulp.message.choice.msSUPLPOSINIT.requestedAssistData->navigationModelData->satInfo)
					{return NO_MEMORY;}
					ulp.message.choice.msSUPLPOSINIT.requestedAssistData->navigationModelData->satInfo->list.size=message->m_pRequestedAssistData->m_pNavigationModelData->m_SatInfo->size();
					ulp.message.choice.msSUPLPOSINIT.requestedAssistData->navigationModelData->satInfo->list.array=(SatelliteInfoElement**)MALLOC(message->m_pRequestedAssistData->m_pNavigationModelData->m_SatInfo->size() * sizeof(SatelliteInfoElement**));
					if(!ulp.message.choice.msSUPLPOSINIT.requestedAssistData->navigationModelData->satInfo->list.array)
					{return NO_MEMORY;}
					ulp.message.choice.msSUPLPOSINIT.requestedAssistData->navigationModelData->satInfo->list.count=0;
					for(unsigned int i=0;i<(message->m_pRequestedAssistData->m_pNavigationModelData->m_SatInfo->size());i++)
					{

						//sat_info=message->m_pRequestedAssistData->m_pNavigationModelData->m_SatInfo->at(i);
						sat_info=message->m_pRequestedAssistData->m_pNavigationModelData->m_SatInfo->itemAt(i); //Android STL
						sat_info_elem=(SatelliteInfoElement*)MALLOC(sizeof(SatelliteInfoElement));
						if(!sat_info_elem)
						{return NO_MEMORY;}
						sat_info_elem->iODE=sat_info.m_IODE;
						sat_info_elem->satId=sat_info.m_SatID;
						if(asn_set_add(ulp.message.choice.msSUPLPOSINIT.requestedAssistData->navigationModelData->satInfo, sat_info_elem) != 0)
						{
// 							ulp.message.choice.msSUPLPOSINIT.requestedAssistData->navigationModelData->satInfo=NULL;
// 							break;
							return NO_MEMORY;
						}
					}
				}
			}
			else
			{
				ulp.message.choice.msSUPLPOSINIT.requestedAssistData->navigationModelData->satInfo=NULL;
			}
		}
		else
		{
			ulp.message.choice.msSUPLPOSINIT.requestedAssistData->navigationModelData = NULL;
		}
		//rsuvorov extended ephemeris improvement
		if(message->m_pRequestedAssistData->m_pVer2RequestedAssistDataExtension)
		{
			ulp.message.choice.msSUPLPOSINIT.requestedAssistData->ver2_RequestedAssistData_extension=(Ver2_RequestedAssistData_extension_t *)MALLOC(sizeof(Ver2_RequestedAssistData_extension_t));
			if(!ulp.message.choice.msSUPLPOSINIT.requestedAssistData->ver2_RequestedAssistData_extension)
			{return NO_MEMORY;}
			ulp.message.choice.msSUPLPOSINIT.requestedAssistData->ver2_RequestedAssistData_extension->ganssRequestedCommonAssistanceDataList=0; //not supported by XSS
			ulp.message.choice.msSUPLPOSINIT.requestedAssistData->ver2_RequestedAssistData_extension->ganssRequestedGenericAssistanceDataList=0; //not supported by XSS

			if(message->m_pRequestedAssistData->m_pVer2RequestedAssistDataExtension->m_pExtendedEphemeris)
			{
				ulp.message.choice.msSUPLPOSINIT.requestedAssistData->ver2_RequestedAssistData_extension->extendedEphemeris=(ExtendedEphemeris_t *)MALLOC(sizeof(ExtendedEphemeris_t));
				if(!ulp.message.choice.msSUPLPOSINIT.requestedAssistData->ver2_RequestedAssistData_extension->extendedEphemeris)
				{return NO_MEMORY;}
				ulp.message.choice.msSUPLPOSINIT.requestedAssistData->ver2_RequestedAssistData_extension->extendedEphemeris->validity=message->m_pRequestedAssistData->m_pVer2RequestedAssistDataExtension->m_pExtendedEphemeris->validity;
			}
			else
			{
				ulp.message.choice.msSUPLPOSINIT.requestedAssistData->ver2_RequestedAssistData_extension->extendedEphemeris=NULL;
			}

			if(message->m_pRequestedAssistData->m_pVer2RequestedAssistDataExtension->m_pExtendedEphemerisCheck)
			{
				ulp.message.choice.msSUPLPOSINIT.requestedAssistData->ver2_RequestedAssistData_extension->extendedEphemerisCheck=(ExtendedEphCheck_t *)MALLOC(sizeof(ExtendedEphCheck_t));
				if(!ulp.message.choice.msSUPLPOSINIT.requestedAssistData->ver2_RequestedAssistData_extension->extendedEphemerisCheck)
				{return NO_MEMORY;}
				ulp.message.choice.msSUPLPOSINIT.requestedAssistData->ver2_RequestedAssistData_extension->extendedEphemerisCheck->beginTime.gPSWeek=message->m_pRequestedAssistData->m_pVer2RequestedAssistDataExtension->m_pExtendedEphemerisCheck->m_pBeginTime.m_GpsWeek;
				ulp.message.choice.msSUPLPOSINIT.requestedAssistData->ver2_RequestedAssistData_extension->extendedEphemerisCheck->beginTime.gPSTOWhour=message->m_pRequestedAssistData->m_pVer2RequestedAssistDataExtension->m_pExtendedEphemerisCheck->m_pBeginTime.m_GpsTOWHour;

				ulp.message.choice.msSUPLPOSINIT.requestedAssistData->ver2_RequestedAssistData_extension->extendedEphemerisCheck->endTime.gPSWeek=message->m_pRequestedAssistData->m_pVer2RequestedAssistDataExtension->m_pExtendedEphemerisCheck->m_pEndTime.m_GpsWeek;
				ulp.message.choice.msSUPLPOSINIT.requestedAssistData->ver2_RequestedAssistData_extension->extendedEphemerisCheck->endTime.gPSTOWhour=message->m_pRequestedAssistData->m_pVer2RequestedAssistDataExtension->m_pExtendedEphemerisCheck->m_pEndTime.m_GpsTOWHour;
			}
			else
			{
				ulp.message.choice.msSUPLPOSINIT.requestedAssistData->ver2_RequestedAssistData_extension->extendedEphemerisCheck=NULL;
			}
		}
		else
		{
			ulp.message.choice.msSUPLPOSINIT.requestedAssistData->ver2_RequestedAssistData_extension=NULL;
		}
	}
	ulp.message.choice.msSUPLPOSINIT.sUPLPOS=NULL; //Set off sending SUPLPOS packet in SUPLPOSINIT packet
	if(!message->m_pPosition)
		ulp.message.choice.msSUPLPOSINIT.position=NULL;
	else
	{
		ulp.message.choice.msSUPLPOSINIT.position=(Position*)MALLOC(sizeof(Position));
		if(!ulp.message.choice.msSUPLPOSINIT.position)
		{return NO_MEMORY;}
		if(!message->m_pPosition->m_UTCTime.m_Data)
		{return MAN_PARAM_IS_ABSENT;}
		ulp.message.choice.msSUPLPOSINIT.position->timestamp.buf=(Engine::uint8_t*)MALLOC(message->m_pPosition->m_UTCTime.m_Size);
		if(!ulp.message.choice.msSUPLPOSINIT.position->timestamp.buf)
		{return NO_MEMORY;}
		ulp.message.choice.msSUPLPOSINIT.position->timestamp.size=message->m_pPosition->m_UTCTime.m_Size;
		memcpy(ulp.message.choice.msSUPLPOSINIT.position->timestamp.buf,message->m_pPosition->m_UTCTime.m_Data,ulp.message.choice.msSUPLPOSINIT.position->timestamp.size);
		ulp.message.choice.msSUPLPOSINIT.position->positionEstimate.latitude=message->m_pPosition->m_PositionEstimate.m_Latitude;
		ulp.message.choice.msSUPLPOSINIT.position->positionEstimate.latitudeSign.buf=(Engine::uint8_t*)MALLOC(1);
		if(!ulp.message.choice.msSUPLPOSINIT.position->positionEstimate.latitudeSign.buf)
		{return NO_MEMORY;}
		ulp.message.choice.msSUPLPOSINIT.position->positionEstimate.latitudeSign.size=1;
		*(ulp.message.choice.msSUPLPOSINIT.position->positionEstimate.latitudeSign.buf)=(Engine::uint8_t)message->m_pPosition->m_PositionEstimate.m_LatitudeSign;
		ulp.message.choice.msSUPLPOSINIT.position->positionEstimate.longitude=message->m_pPosition->m_PositionEstimate.m_Longitude;
		if(!message->m_pPosition->m_PositionEstimate.m_pAltitudeInfo)
			ulp.message.choice.msSUPLPOSINIT.position->positionEstimate.altitudeInfo=NULL;
		else
		{
			ulp.message.choice.msSUPLPOSINIT.position->positionEstimate.altitudeInfo=(AltitudeInfo*)MALLOC(sizeof(AltitudeInfo));
			if(!ulp.message.choice.msSUPLPOSINIT.position->positionEstimate.altitudeInfo)
			{return NO_MEMORY;}
			ulp.message.choice.msSUPLPOSINIT.position->positionEstimate.altitudeInfo->altitude=message->m_pPosition->m_PositionEstimate.m_pAltitudeInfo->m_Altitude;
			ulp.message.choice.msSUPLPOSINIT.position->positionEstimate.altitudeInfo->altitudeDirection.buf=(Engine::uint8_t*)MALLOC(1);
			if(!ulp.message.choice.msSUPLPOSINIT.position->positionEstimate.altitudeInfo->altitudeDirection.buf) //rsuvorov !!!!
			{return NO_MEMORY;}
			ulp.message.choice.msSUPLPOSINIT.position->positionEstimate.altitudeInfo->altitudeDirection.size=1;
			*(ulp.message.choice.msSUPLPOSINIT.position->positionEstimate.altitudeInfo->altitudeDirection.buf)=(Engine::uint8_t)message->m_pPosition->m_PositionEstimate.m_pAltitudeInfo->m_AltitudeDirection;
			ulp.message.choice.msSUPLPOSINIT.position->positionEstimate.altitudeInfo->altUncertainty=message->m_pPosition->m_PositionEstimate.m_pAltitudeInfo->m_AltUncertainty;
		}
		if(!message->m_pPosition->m_PositionEstimate.m_pConfidence)
			ulp.message.choice.msSUPLPOSINIT.position->positionEstimate.confidence=NULL;
		else
		{	
			ulp.message.choice.msSUPLPOSINIT.position->positionEstimate.confidence=(long*)MALLOC(sizeof(long));
			if(!ulp.message.choice.msSUPLPOSINIT.position->positionEstimate.confidence)
			{return NO_MEMORY;}
			*(ulp.message.choice.msSUPLPOSINIT.position->positionEstimate.confidence)=*(message->m_pPosition->m_PositionEstimate.m_pConfidence);
		}
		if(!message->m_pPosition->m_PositionEstimate.m_pUncertainty)
			ulp.message.choice.msSUPLPOSINIT.position->positionEstimate.uncertainty=NULL;
		else
		{
			ulp.message.choice.msSUPLPOSINIT.position->positionEstimate.uncertainty=(struct PositionEstimate::uncertainty*)MALLOC(sizeof(struct PositionEstimate::uncertainty));
			if(!ulp.message.choice.msSUPLPOSINIT.position->positionEstimate.uncertainty)
			{return NO_MEMORY;}
			ulp.message.choice.msSUPLPOSINIT.position->positionEstimate.uncertainty->orientationMajorAxis=message->m_pPosition->m_PositionEstimate.m_pUncertainty->m_OrientationMajorAxis;
			ulp.message.choice.msSUPLPOSINIT.position->positionEstimate.uncertainty->uncertaintySemiMajor=message->m_pPosition->m_PositionEstimate.m_pUncertainty->m_UncertaintySemiMajor;
			ulp.message.choice.msSUPLPOSINIT.position->positionEstimate.uncertainty->uncertaintySemiMinor=message->m_pPosition->m_PositionEstimate.m_pUncertainty->m_UncertaintySeminMinor;
		}
		if(!message->m_pPosition->m_pVelocity)
			ulp.message.choice.msSUPLPOSINIT.position->velocity=NULL;
		else
		{
			ulp.message.choice.msSUPLPOSINIT.position->velocity=(Velocity_t*)MALLOC(sizeof(Velocity_t));
			if(!ulp.message.choice.msSUPLPOSINIT.position->velocity)
			{return NO_MEMORY;}
			ulp.message.choice.msSUPLPOSINIT.position->velocity->present=(Velocity_PR)message->m_pPosition->m_pVelocity->GetType();
			if(ulp.message.choice.msSUPLPOSINIT.position->velocity->present==Velocity_PR_horandveruncert)
			{
				Engine::CHorandveruncert* horandveruncert;
				horandveruncert=(Engine::CHorandveruncert*)message->m_pPosition->m_pVelocity;
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandveruncert.bearing.buf=(Engine::uint8_t*)MALLOC(2);
				if(!ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandveruncert.bearing.buf)
				{return NO_MEMORY;}
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandveruncert.bearing.size=2;
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandveruncert.bearing.bits_unused=7;
				memcpy(ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandveruncert.bearing.buf,horandveruncert->m_Bearing.m_Data,2);
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandveruncert.horspeed.buf=(Engine::uint8_t*)MALLOC(2);
				if(!ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandveruncert.horspeed.buf)
				{return NO_MEMORY;}
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandveruncert.horspeed.size=2;
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandveruncert.horspeed.bits_unused=0;
				memcpy(ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandveruncert.horspeed.buf,horandveruncert->m_HorSpeed.m_Data,2);
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandveruncert.horuncertspeed.buf=(Engine::uint8_t*)MALLOC(1);
				if(!ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandveruncert.horuncertspeed.buf)
				{return NO_MEMORY;}
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandveruncert.horuncertspeed.size=1;
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandveruncert.horuncertspeed.bits_unused=0;
				memcpy(ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandveruncert.horuncertspeed.buf,horandveruncert->m_HorUncertSpeed.m_Data,1);
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandveruncert.verdirect.buf=(Engine::uint8_t*)MALLOC(1);
				if(!ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandveruncert.verdirect.buf)
				{return NO_MEMORY;}
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandveruncert.verdirect.size=1;
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandveruncert.verdirect.bits_unused=7;
				memcpy(ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandveruncert.verdirect.buf,horandveruncert->m_VerDirect.m_Data,1);
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandveruncert.verspeed.buf=(Engine::uint8_t*)MALLOC(1);
				if(!ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandveruncert.verspeed.buf)
				{return NO_MEMORY;}
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandveruncert.verspeed.size=1;
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandveruncert.verspeed.bits_unused=0;
				memcpy(ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandveruncert.verspeed.buf,horandveruncert->m_VerSpeed.m_Data,1);
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandveruncert.veruncertspeed.buf=(Engine::uint8_t*)MALLOC(1);
				if(!ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandveruncert.veruncertspeed.buf)
				{return NO_MEMORY;}
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandveruncert.veruncertspeed.size=1;
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandveruncert.veruncertspeed.bits_unused=0;
				memcpy(ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandveruncert.veruncertspeed.buf,horandveruncert->m_VerUncertSpeed.m_Data,1);
			}
			else if(ulp.message.choice.msSUPLPOSINIT.position->velocity->present==Velocity_PR_horandvervel)
			{
				Engine::CHorandvervel* horandvervel;
				horandvervel=(Engine::CHorandvervel*)message->m_pPosition->m_pVelocity;
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandvervel.bearing.buf=(Engine::uint8_t*)MALLOC(2);
				if(!ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandvervel.bearing.buf)
				{return NO_MEMORY;}
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandvervel.bearing.size=2;
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandvervel.bearing.bits_unused=7;
				memcpy(ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandvervel.bearing.buf,horandvervel->m_Bearing.m_Data,2);
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandvervel.horspeed.buf=(Engine::uint8_t*)MALLOC(2);
				if(!ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandvervel.horspeed.buf)
				{return NO_MEMORY;}
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandvervel.horspeed.size=2;
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandvervel.horspeed.bits_unused=0;
				memcpy(ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandvervel.horspeed.buf,horandvervel->m_HorSpeed.m_Data,2);
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandvervel.verdirect.buf=(Engine::uint8_t*)MALLOC(1);
				if(!ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandvervel.verdirect.buf)
				{return NO_MEMORY;}
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandvervel.verdirect.size=1;
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandvervel.verdirect.bits_unused=7;
				memcpy(ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandvervel.verdirect.buf,horandvervel->m_VerDirect.m_Data,1);
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandvervel.verspeed.buf=(Engine::uint8_t*)MALLOC(1);
				if(!ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandvervel.verspeed.buf)
				{return NO_MEMORY;}
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandvervel.verspeed.size=1;
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandvervel.verspeed.bits_unused=0;
				memcpy(ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandvervel.verspeed.buf,horandvervel->m_VerSpeed.m_Data,1);
			}
			else if(ulp.message.choice.msSUPLPOSINIT.position->velocity->present==Velocity_PR_horvel)
			{
				Engine::CHorvel* horvel;
				horvel=(Engine::CHorvel*)message->m_pPosition->m_pVelocity;
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horvel.bearing.buf=(Engine::uint8_t*)MALLOC(2);
				if(!ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horvel.bearing.buf)
				{return NO_MEMORY;}
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horvel.bearing.size=2;
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horvel.bearing.bits_unused=7;
				memcpy(ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horvel.bearing.buf,horvel->m_Bearing.m_Data,2);
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horvel.horspeed.buf=(Engine::uint8_t*)MALLOC(2);
				if(!ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horvel.horspeed.buf)
				{return NO_MEMORY;}
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horvel.horspeed.size=2;
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horvel.horspeed.bits_unused=0;
				memcpy(ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horvel.horspeed.buf,horvel->m_HorSpeed.m_Data,2);

			}
			else if(ulp.message.choice.msSUPLPOSINIT.position->velocity->present==Velocity_PR_horveluncert)
			{
				Engine::CHorveluncert* horveluncert;
				horveluncert=(Engine::CHorveluncert*)message->m_pPosition->m_pVelocity;
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horveluncert.bearing.buf=(Engine::uint8_t*)MALLOC(2);
				if(!ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horveluncert.bearing.buf)
				{return NO_MEMORY;}
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horveluncert.bearing.size=2;
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horveluncert.bearing.bits_unused=7;
				memcpy(ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horveluncert.bearing.buf,horveluncert->m_Bearing.m_Data,2);	
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horveluncert.horspeed.buf=(Engine::uint8_t*)MALLOC(2);
				if(!ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horveluncert.horspeed.buf)
				{return NO_MEMORY;}
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horveluncert.horspeed.size=2;
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horveluncert.horspeed.bits_unused=0;
				memcpy(ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horveluncert.horspeed.buf,horveluncert->m_HorSpeed.m_Data,2);	
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horveluncert.uncertspeed.buf=(Engine::uint8_t*)MALLOC(1);
				if(!ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horveluncert.uncertspeed.buf)
				{return NO_MEMORY;}
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horveluncert.uncertspeed.size=1;
				ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horveluncert.uncertspeed.bits_unused=0;
				memcpy(ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horveluncert.uncertspeed.buf,horveluncert->m_UncertSpeed.m_Data,1);	
			}
			else
			{return INVALID_PARAMETER;}
		}
	}
	if(!message->m_pVer)
		ulp.message.choice.msSUPLPOSINIT.ver=NULL;
	else
	{
		ulp.message.choice.msSUPLPOSINIT.ver=(Ver_t*)MALLOC(sizeof(Ver_t));
		if(!ulp.message.choice.msSUPLPOSINIT.ver)
		{return NO_MEMORY;}
		ulp.message.choice.msSUPLPOSINIT.ver->buf=(Engine::uint8_t*)MALLOC(8);
		if(!ulp.message.choice.msSUPLPOSINIT.ver->buf)
		{return NO_MEMORY;}
		ulp.message.choice.msSUPLPOSINIT.ver->size=8;
		ulp.message.choice.msSUPLPOSINIT.ver->bits_unused=0;
		memcpy(ulp.message.choice.msSUPLPOSINIT.ver->buf,message->m_pVer->m_Data,8);
	}
	return SUCCESS;
}

void ULP_Processor::Clear_SUPLPOSINIT()
{
	ulp.message.present=UlpMessage_PR_NOTHING;
	if(ulp.message.choice.msSUPLPOSINIT.sETCapabilities.prefMethod.buf)
	{
		FREEMEM(ulp.message.choice.msSUPLPOSINIT.sETCapabilities.prefMethod.buf);
	}
	if(ulp.message.choice.msSUPLPOSINIT.locationId.cellInfo.choice.gsmCell.nMR)
	{
		for(int i=0;i<ulp.message.choice.msSUPLPOSINIT.locationId.cellInfo.choice.gsmCell.nMR->list.size;i++)
		{
			FREEMEM(ulp.message.choice.msSUPLPOSINIT.locationId.cellInfo.choice.gsmCell.nMR->list.array[i]);
		}
		FREEMEM(ulp.message.choice.msSUPLPOSINIT.locationId.cellInfo.choice.gsmCell.nMR->list.array);
		FREEMEM(ulp.message.choice.msSUPLPOSINIT.locationId.cellInfo.choice.gsmCell.nMR);
	}
	if(ulp.message.choice.msSUPLPOSINIT.locationId.cellInfo.choice.gsmCell.tA)
		FREEMEM(ulp.message.choice.msSUPLPOSINIT.locationId.cellInfo.choice.gsmCell.tA);
	if(ulp.message.choice.msSUPLPOSINIT.locationId.status.buf)
		FREEMEM(ulp.message.choice.msSUPLPOSINIT.locationId.status.buf);
	if(ulp.message.choice.msSUPLPOSINIT.requestedAssistData)
	{//rsuvorov extended ephemeris improvement
		if(ulp.message.choice.msSUPLPOSINIT.requestedAssistData->ver2_RequestedAssistData_extension)
		{
			if(ulp.message.choice.msSUPLPOSINIT.requestedAssistData->ver2_RequestedAssistData_extension->ganssRequestedCommonAssistanceDataList)//not supported by XSS
				FREEMEM(ulp.message.choice.msSUPLPOSINIT.requestedAssistData->ver2_RequestedAssistData_extension->ganssRequestedCommonAssistanceDataList); //not supported by XSS
			if(ulp.message.choice.msSUPLPOSINIT.requestedAssistData->ver2_RequestedAssistData_extension->ganssRequestedGenericAssistanceDataList)//not supported by XSS
				FREEMEM(ulp.message.choice.msSUPLPOSINIT.requestedAssistData->ver2_RequestedAssistData_extension->ganssRequestedGenericAssistanceDataList); //not supported by XSS
				
			if(ulp.message.choice.msSUPLPOSINIT.requestedAssistData->ver2_RequestedAssistData_extension->extendedEphemeris)
				FREEMEM(ulp.message.choice.msSUPLPOSINIT.requestedAssistData->ver2_RequestedAssistData_extension->extendedEphemeris);
			
			if(ulp.message.choice.msSUPLPOSINIT.requestedAssistData->ver2_RequestedAssistData_extension->extendedEphemerisCheck)
				FREEMEM(ulp.message.choice.msSUPLPOSINIT.requestedAssistData->ver2_RequestedAssistData_extension->extendedEphemerisCheck);

			if(ulp.message.choice.msSUPLPOSINIT.requestedAssistData->ver2_RequestedAssistData_extension)
			FREEMEM(ulp.message.choice.msSUPLPOSINIT.requestedAssistData->ver2_RequestedAssistData_extension);
		}

		if(ulp.message.choice.msSUPLPOSINIT.requestedAssistData->navigationModelData)
		{
			if(ulp.message.choice.msSUPLPOSINIT.requestedAssistData->navigationModelData->satInfo)
			{
				for(int i=0;i<ulp.message.choice.msSUPLPOSINIT.requestedAssistData->navigationModelData->satInfo->list.size;i++)
				{
					FREEMEM(ulp.message.choice.msSUPLPOSINIT.requestedAssistData->navigationModelData->satInfo->list.array[i]);
				}

				FREEMEM(ulp.message.choice.msSUPLPOSINIT.requestedAssistData->navigationModelData->satInfo->list.array);
				FREEMEM(ulp.message.choice.msSUPLPOSINIT.requestedAssistData->navigationModelData->satInfo);
			}
			FREEMEM(ulp.message.choice.msSUPLPOSINIT.requestedAssistData->navigationModelData);
		}

		FREEMEM(ulp.message.choice.msSUPLPOSINIT.requestedAssistData);
	}
	if(ulp.message.choice.msSUPLPOSINIT.position)
	{
		if(ulp.message.choice.msSUPLPOSINIT.position->timestamp.buf)
			FREEMEM(ulp.message.choice.msSUPLPOSINIT.position->timestamp.buf);
		if(ulp.message.choice.msSUPLPOSINIT.position->positionEstimate.latitudeSign.buf)
			FREEMEM(ulp.message.choice.msSUPLPOSINIT.position->positionEstimate.latitudeSign.buf);
		if(ulp.message.choice.msSUPLPOSINIT.position->positionEstimate.altitudeInfo)
		{
			if(ulp.message.choice.msSUPLPOSINIT.position->positionEstimate.altitudeInfo->altitudeDirection.buf)
				FREEMEM(ulp.message.choice.msSUPLPOSINIT.position->positionEstimate.altitudeInfo->altitudeDirection.buf);
			FREEMEM(ulp.message.choice.msSUPLPOSINIT.position->positionEstimate.altitudeInfo);
		}
		if(ulp.message.choice.msSUPLPOSINIT.position->positionEstimate.confidence)
			FREEMEM(ulp.message.choice.msSUPLPOSINIT.position->positionEstimate.confidence);
		if(ulp.message.choice.msSUPLPOSINIT.position->positionEstimate.uncertainty)
			FREEMEM(ulp.message.choice.msSUPLPOSINIT.position->positionEstimate.uncertainty);
		if(ulp.message.choice.msSUPLPOSINIT.position->velocity)
		{
			if(ulp.message.choice.msSUPLPOSINIT.position->velocity->present==Velocity_PR_horandveruncert)
			{
				FREEMEM(ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandveruncert.bearing.buf);
				FREEMEM(ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandveruncert.horspeed.buf);
				FREEMEM(ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandveruncert.horuncertspeed.buf);
				FREEMEM(ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandveruncert.verdirect.buf);
				FREEMEM(ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandveruncert.verspeed.buf);
				FREEMEM(ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandveruncert.veruncertspeed.buf);
			}
			else if(ulp.message.choice.msSUPLPOSINIT.position->velocity->present==Velocity_PR_horandvervel)
			{
				FREEMEM(ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandvervel.bearing.buf);
				FREEMEM(ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandvervel.horspeed.buf);
				FREEMEM(ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandvervel.verdirect.buf);
				FREEMEM(ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horandvervel.verspeed.buf);
			}
			else if(ulp.message.choice.msSUPLPOSINIT.position->velocity->present==Velocity_PR_horvel)
			{
				FREEMEM(ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horvel.bearing.buf);
				FREEMEM(ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horvel.horspeed.buf);
			}
			else if(ulp.message.choice.msSUPLPOSINIT.position->velocity->present==Velocity_PR_horveluncert)
			{
				FREEMEM(ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horveluncert.bearing.buf);
				FREEMEM(ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horveluncert.horspeed.buf);
				FREEMEM(ulp.message.choice.msSUPLPOSINIT.position->velocity->choice.horveluncert.uncertspeed.buf)
			}
			FREEMEM(ulp.message.choice.msSUPLPOSINIT.position->velocity);
		}
		FREEMEM(ulp.message.choice.msSUPLPOSINIT.position);
	}
	if(ulp.message.choice.msSUPLPOSINIT.ver)
	{
		if(ulp.message.choice.msSUPLPOSINIT.ver->buf)
		{
			FREEMEM(ulp.message.choice.msSUPLPOSINIT.ver->buf);
		}
		FREEMEM(ulp.message.choice.msSUPLPOSINIT.ver);
	}
}

SUCCESS_ERROR_T ULP_Processor::Encode_SUPLPOS(Engine::CSUPLPos* message)
{
	ulp.message.present=UlpMessage_PR_msSUPLPOS;
	//VELOCITY
	if(!message->m_pVelocity)
			ulp.message.choice.msSUPLPOS.velocity=NULL;
	else
	{
		ulp.message.choice.msSUPLPOS.velocity=(Velocity_t*)MALLOC(sizeof(Velocity_t));
		if(!ulp.message.choice.msSUPLPOS.velocity)
		{return NO_MEMORY;}
		ulp.message.choice.msSUPLPOS.velocity->present=(Velocity_PR)message->m_pVelocity->GetType();
		if(ulp.message.choice.msSUPLPOS.velocity->present==Velocity_PR_horandveruncert)
		{
			Engine::CHorandveruncert* horandveruncert;
			horandveruncert=(Engine::CHorandveruncert*)message->m_pVelocity;
			ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.bearing.buf=(Engine::uint8_t*)MALLOC(2);
			if(!ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.bearing.buf)
			{return NO_MEMORY;}
			ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.bearing.size=2;
			ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.bearing.bits_unused=7;
			memcpy(ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.bearing.buf,horandveruncert->m_Bearing.m_Data,2);
			ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.horspeed.buf=(Engine::uint8_t*)MALLOC(2);
			if(!ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.horspeed.buf)
			{return NO_MEMORY;}
			ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.horspeed.size=2;
			ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.horspeed.bits_unused=0;
			memcpy(ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.horspeed.buf,horandveruncert->m_HorSpeed.m_Data,2);
			ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.horuncertspeed.buf=(Engine::uint8_t*)MALLOC(1);
			if(!ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.horuncertspeed.buf)
			{return NO_MEMORY;}
			ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.horuncertspeed.size=1;
			ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.horuncertspeed.bits_unused=0;
			memcpy(ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.horuncertspeed.buf,horandveruncert->m_HorUncertSpeed.m_Data,1);
			ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.verdirect.buf=(Engine::uint8_t*)MALLOC(1);
			if(!ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.verdirect.buf)
			{return NO_MEMORY;}
			ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.verdirect.size=1;
			ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.verdirect.bits_unused=7;
			memcpy(ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.verdirect.buf,horandveruncert->m_VerDirect.m_Data,1);
			ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.verspeed.buf=(Engine::uint8_t*)MALLOC(1);
			if(!ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.verspeed.buf)
			{return NO_MEMORY;}
			ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.verspeed.size=1;
			ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.verspeed.bits_unused=0;
			memcpy(ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.verspeed.buf,horandveruncert->m_VerSpeed.m_Data,1);
			ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.veruncertspeed.buf=(Engine::uint8_t*)MALLOC(1);
			if(!ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.veruncertspeed.buf)
			{return NO_MEMORY;}
			ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.veruncertspeed.size=1;
			ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.veruncertspeed.bits_unused=0;
			memcpy(ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.veruncertspeed.buf,horandveruncert->m_VerUncertSpeed.m_Data,1);
		}
		else if(ulp.message.choice.msSUPLPOS.velocity->present==Velocity_PR_horandvervel)
		{
			Engine::CHorandvervel* horandvervel;
			horandvervel=(Engine::CHorandvervel*)message->m_pVelocity;
			ulp.message.choice.msSUPLPOS.velocity->choice.horandvervel.bearing.buf=(Engine::uint8_t*)MALLOC(2);
			if(!ulp.message.choice.msSUPLPOS.velocity->choice.horandvervel.bearing.buf)
			{return NO_MEMORY;}
			ulp.message.choice.msSUPLPOS.velocity->choice.horandvervel.bearing.size=2;
			ulp.message.choice.msSUPLPOS.velocity->choice.horandvervel.bearing.bits_unused=7;
			memcpy(ulp.message.choice.msSUPLPOS.velocity->choice.horandvervel.bearing.buf,horandvervel->m_Bearing.m_Data,2);
			ulp.message.choice.msSUPLPOS.velocity->choice.horandvervel.horspeed.buf=(Engine::uint8_t*)MALLOC(2);
			if(!ulp.message.choice.msSUPLPOS.velocity->choice.horandvervel.horspeed.buf)
			{return NO_MEMORY;}
			ulp.message.choice.msSUPLPOS.velocity->choice.horandvervel.horspeed.size=2;
			ulp.message.choice.msSUPLPOS.velocity->choice.horandvervel.horspeed.bits_unused=0;
			memcpy(ulp.message.choice.msSUPLPOS.velocity->choice.horandvervel.horspeed.buf,horandvervel->m_HorSpeed.m_Data,2);
			ulp.message.choice.msSUPLPOS.velocity->choice.horandvervel.verdirect.buf=(Engine::uint8_t*)MALLOC(1);
			if(!ulp.message.choice.msSUPLPOS.velocity->choice.horandvervel.verdirect.buf)
			{return NO_MEMORY;}
			ulp.message.choice.msSUPLPOS.velocity->choice.horandvervel.verdirect.size=1;
			ulp.message.choice.msSUPLPOS.velocity->choice.horandvervel.verdirect.bits_unused=7;
			memcpy(ulp.message.choice.msSUPLPOS.velocity->choice.horandvervel.verdirect.buf,horandvervel->m_VerDirect.m_Data,1);
			ulp.message.choice.msSUPLPOS.velocity->choice.horandvervel.verspeed.buf=(Engine::uint8_t*)MALLOC(1);
			if(!ulp.message.choice.msSUPLPOS.velocity->choice.horandvervel.verspeed.buf)
			{return NO_MEMORY;}
			ulp.message.choice.msSUPLPOS.velocity->choice.horandvervel.verspeed.size=1;
			ulp.message.choice.msSUPLPOS.velocity->choice.horandvervel.verspeed.bits_unused=0;
			memcpy(ulp.message.choice.msSUPLPOS.velocity->choice.horandvervel.verspeed.buf,horandvervel->m_VerSpeed.m_Data,1);
		}
		else if(ulp.message.choice.msSUPLPOS.velocity->present==Velocity_PR_horvel)
		{
			Engine::CHorvel* horvel;
			horvel=(Engine::CHorvel*)message->m_pVelocity;
			ulp.message.choice.msSUPLPOS.velocity->choice.horvel.bearing.buf=(Engine::uint8_t*)MALLOC(2);
			if(!ulp.message.choice.msSUPLPOS.velocity->choice.horvel.bearing.buf)
			{return NO_MEMORY;}
			ulp.message.choice.msSUPLPOS.velocity->choice.horvel.bearing.size=2;
			ulp.message.choice.msSUPLPOS.velocity->choice.horvel.bearing.bits_unused=7;
			memcpy(ulp.message.choice.msSUPLPOS.velocity->choice.horvel.bearing.buf,horvel->m_Bearing.m_Data,2);
			ulp.message.choice.msSUPLPOS.velocity->choice.horvel.horspeed.buf=(Engine::uint8_t*)MALLOC(2);
			if(!ulp.message.choice.msSUPLPOS.velocity->choice.horvel.horspeed.buf)
			{return NO_MEMORY;}
			ulp.message.choice.msSUPLPOS.velocity->choice.horvel.horspeed.size=2;
			ulp.message.choice.msSUPLPOS.velocity->choice.horvel.horspeed.bits_unused=0;
			memcpy(ulp.message.choice.msSUPLPOS.velocity->choice.horvel.horspeed.buf,horvel->m_HorSpeed.m_Data,2);
			}
		else if(ulp.message.choice.msSUPLPOS.velocity->present==Velocity_PR_horveluncert)
		{
			Engine::CHorveluncert* horveluncert;
			horveluncert=(Engine::CHorveluncert*)message->m_pVelocity;
			ulp.message.choice.msSUPLPOS.velocity->choice.horveluncert.bearing.buf=(Engine::uint8_t*)MALLOC(2);
			if(!ulp.message.choice.msSUPLPOS.velocity->choice.horveluncert.bearing.buf)
			{return NO_MEMORY;}
			ulp.message.choice.msSUPLPOS.velocity->choice.horveluncert.bearing.size=2;
			ulp.message.choice.msSUPLPOS.velocity->choice.horveluncert.bearing.bits_unused=7;
			memcpy(ulp.message.choice.msSUPLPOS.velocity->choice.horveluncert.bearing.buf,horveluncert->m_Bearing.m_Data,2);	
			ulp.message.choice.msSUPLPOS.velocity->choice.horveluncert.horspeed.buf=(Engine::uint8_t*)MALLOC(2);
			if(!ulp.message.choice.msSUPLPOS.velocity->choice.horveluncert.horspeed.buf)
			{return NO_MEMORY;}
			ulp.message.choice.msSUPLPOS.velocity->choice.horveluncert.horspeed.size=2;
			ulp.message.choice.msSUPLPOS.velocity->choice.horveluncert.horspeed.bits_unused=0;
			memcpy(ulp.message.choice.msSUPLPOS.velocity->choice.horveluncert.horspeed.buf,horveluncert->m_HorSpeed.m_Data,2);	
			ulp.message.choice.msSUPLPOS.velocity->choice.horveluncert.uncertspeed.buf=(Engine::uint8_t*)MALLOC(1);
			if(!ulp.message.choice.msSUPLPOS.velocity->choice.horveluncert.uncertspeed.buf)
			{return NO_MEMORY;}
			ulp.message.choice.msSUPLPOS.velocity->choice.horveluncert.uncertspeed.size=1;
			ulp.message.choice.msSUPLPOS.velocity->choice.horveluncert.uncertspeed.bits_unused=0;
			memcpy(ulp.message.choice.msSUPLPOS.velocity->choice.horveluncert.uncertspeed.buf,horveluncert->m_UncertSpeed.m_Data,1);	
		}
		else
		{return INVALID_PARAMETER;}
	}
	if(!message->m_PosPayLoad)
	{return MAN_PARAM_IS_ABSENT;}
	else
	{
		ulp.message.choice.msSUPLPOS.posPayLoad.present=(PosPayLoad_PR)message->m_PosPayLoad->GetType();
		if(ulp.message.choice.msSUPLPOS.posPayLoad.present == PosPayLoad_PR_rrlpPayload)
		{

			Engine::CRRLPPayLoad* rrlp;
			rrlp=(Engine::CRRLPPayLoad*)message->m_PosPayLoad;
			ulp.message.choice.msSUPLPOS.posPayLoad.choice.rrlpPayload.size=rrlp->m_RRLPPayLoad.m_Size;
			ulp.message.choice.msSUPLPOS.posPayLoad.choice.rrlpPayload.buf=(Engine::uint8_t*)MALLOC(ulp.message.choice.msSUPLPOS.posPayLoad.choice.rrlpPayload.size);
			if(!ulp.message.choice.msSUPLPOS.posPayLoad.choice.rrlpPayload.buf)
			{return NO_MEMORY;}
			memcpy(ulp.message.choice.msSUPLPOS.posPayLoad.choice.rrlpPayload.buf,rrlp->m_RRLPPayLoad.m_Data,ulp.message.choice.msSUPLPOS.posPayLoad.choice.rrlpPayload.size);
		}
		else if(ulp.message.choice.msSUPLPOS.posPayLoad.present == PosPayLoad_PR_rrcPayload)
		{	
			Engine::CRRCPayLoad* rrc;
			rrc=(Engine::CRRCPayLoad*)message->m_PosPayLoad;
			ulp.message.choice.msSUPLPOS.posPayLoad.choice.rrcPayload.size=rrc->m_RRCPayLoad.m_Size;
			ulp.message.choice.msSUPLPOS.posPayLoad.choice.rrcPayload.buf=(Engine::uint8_t*)MALLOC(ulp.message.choice.msSUPLPOS.posPayLoad.choice.rrcPayload.size);
			if(!ulp.message.choice.msSUPLPOS.posPayLoad.choice.rrlpPayload.buf)
			{return NO_MEMORY;}
			memcpy(ulp.message.choice.msSUPLPOS.posPayLoad.choice.rrcPayload.buf,rrc->m_RRCPayLoad.m_Data,ulp.message.choice.msSUPLPOS.posPayLoad.choice.rrcPayload.size);
		}
		else if(ulp.message.choice.msSUPLPOS.posPayLoad.present == PosPayLoad_PR_tia801payload)
		{
			Engine::CTIA801PayLoad* tia;
			tia=(Engine::CTIA801PayLoad*)message->m_PosPayLoad;
			ulp.message.choice.msSUPLPOS.posPayLoad.choice.tia801payload.size=tia->m_Tia801PayLoad.m_Size;
			ulp.message.choice.msSUPLPOS.posPayLoad.choice.tia801payload.buf=(Engine::uint8_t*)MALLOC(ulp.message.choice.msSUPLPOS.posPayLoad.choice.tia801payload.size);
			if(!ulp.message.choice.msSUPLPOS.posPayLoad.choice.tia801payload.buf)
			{return NO_MEMORY;}
			memcpy(ulp.message.choice.msSUPLPOS.posPayLoad.choice.tia801payload.buf,tia->m_Tia801PayLoad.m_Data,ulp.message.choice.msSUPLPOS.posPayLoad.choice.tia801payload.size);
		}
		else
		{
			return INVALID_PARAMETER;
		}
	}
	return SUCCESS;
}

void ULP_Processor::Clear_SUPLPOS()
{
	ulp.message.present=UlpMessage_PR_NOTHING;
	if(ulp.message.choice.msSUPLPOS.velocity)
	{
			if(ulp.message.choice.msSUPLPOS.velocity->present==Velocity_PR_horandveruncert)
			{
				FREEMEM(ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.bearing.buf);
				FREEMEM(ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.horspeed.buf);
				FREEMEM(ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.horuncertspeed.buf);
				FREEMEM(ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.verdirect.buf);
				FREEMEM(ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.verspeed.buf);
				FREEMEM(ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.veruncertspeed.buf);
			}
			else if(ulp.message.choice.msSUPLPOS.velocity->present==Velocity_PR_horandvervel)
			{
				FREEMEM(ulp.message.choice.msSUPLPOS.velocity->choice.horandvervel.bearing.buf);
				FREEMEM(ulp.message.choice.msSUPLPOS.velocity->choice.horandvervel.horspeed.buf);
				FREEMEM(ulp.message.choice.msSUPLPOS.velocity->choice.horandvervel.verdirect.buf);
				FREEMEM(ulp.message.choice.msSUPLPOS.velocity->choice.horandvervel.verspeed.buf);
			}
			else if(ulp.message.choice.msSUPLPOS.velocity->present==Velocity_PR_horvel)
			{
				FREEMEM(ulp.message.choice.msSUPLPOS.velocity->choice.horvel.bearing.buf);
				FREEMEM(ulp.message.choice.msSUPLPOS.velocity->choice.horvel.horspeed.buf);
			}
			else if(ulp.message.choice.msSUPLPOS.velocity->present==Velocity_PR_horveluncert)
			{
				FREEMEM(ulp.message.choice.msSUPLPOS.velocity->choice.horveluncert.bearing.buf);
				FREEMEM(ulp.message.choice.msSUPLPOS.velocity->choice.horveluncert.horspeed.buf);
				FREEMEM(ulp.message.choice.msSUPLPOS.velocity->choice.horveluncert.uncertspeed.buf)
			}
			FREEMEM(ulp.message.choice.msSUPLPOS.velocity);
	}
	if(ulp.message.choice.msSUPLPOS.posPayLoad.present == PosPayLoad_PR_rrlpPayload)
	{
		FREEMEM(ulp.message.choice.msSUPLPOS.posPayLoad.choice.rrlpPayload.buf);
	}
	else if(ulp.message.choice.msSUPLPOS.posPayLoad.present == PosPayLoad_PR_rrcPayload)
	{	
		FREEMEM(ulp.message.choice.msSUPLPOS.posPayLoad.choice.rrcPayload.buf);
	}
	else if(ulp.message.choice.msSUPLPOS.posPayLoad.present == PosPayLoad_PR_tia801payload)
	{
		FREEMEM(ulp.message.choice.msSUPLPOS.posPayLoad.choice.tia801payload.buf);
	}
}

SUCCESS_ERROR_T ULP_Processor::Encode_SUPLEND(Engine::CSUPLEnd* message)
{
	ulp.message.present=UlpMessage_PR_msSUPLEND;
	ulp.message.choice.msSUPLEND.position=NULL; //NO POSITION IN UPLINK!!!
	if(!message->m_pStatusCode)
	{
		ulp.message.choice.msSUPLEND.statusCode=NULL;
	}
	else
	{
		ulp.message.choice.msSUPLEND.statusCode=(StatusCode_t*)MALLOC(sizeof(StatusCode_t));
		if(!ulp.message.choice.msSUPLEND.statusCode)
		{return NO_MEMORY;}
		ulp.message.choice.msSUPLEND.statusCode->buf=(Engine::uint8_t*)MALLOC(1);
		if(!ulp.message.choice.msSUPLEND.statusCode->buf)
		{return NO_MEMORY;}
		ulp.message.choice.msSUPLEND.statusCode->size=1;
		*(ulp.message.choice.msSUPLEND.statusCode->buf)=*(message->m_pStatusCode);
#ifdef NDEBUG
		LOGD("SUPL_END: Status Code: %d\n", *(message->m_pStatusCode));
#endif
	}
	if(!message->m_pVer)
		ulp.message.choice.msSUPLEND.ver=NULL;
	else
	{
		ulp.message.choice.msSUPLEND.ver=(Ver_t*)MALLOC(sizeof(Ver_t));
		if(!ulp.message.choice.msSUPLEND.ver)
		{return NO_MEMORY;}
		ulp.message.choice.msSUPLEND.ver->buf=(Engine::uint8_t*)MALLOC(8);
		if(!ulp.message.choice.msSUPLEND.ver->buf)
		{return NO_MEMORY;}
		ulp.message.choice.msSUPLEND.ver->size=8;
		ulp.message.choice.msSUPLEND.ver->bits_unused=0;
		memcpy(ulp.message.choice.msSUPLEND.ver->buf,message->m_pVer->m_Data,8);
	}
	return SUCCESS;
}

void ULP_Processor::Clear_SUPLEND()
{
	ulp.message.present=UlpMessage_PR_NOTHING;
	if(ulp.message.choice.msSUPLEND.position)
	{
		if(ulp.message.choice.msSUPLEND.position->positionEstimate.altitudeInfo)
		{
			FREEMEM(ulp.message.choice.msSUPLEND.position->positionEstimate.altitudeInfo);
		}
		if(ulp.message.choice.msSUPLEND.position->positionEstimate.uncertainty)
		{
			FREEMEM(ulp.message.choice.msSUPLEND.position->positionEstimate.uncertainty);
		}
		if(ulp.message.choice.msSUPLEND.position->positionEstimate.confidence)
		{
			FREEMEM(ulp.message.choice.msSUPLEND.position->positionEstimate.confidence);
		}
		if(ulp.message.choice.msSUPLEND.position->timestamp.buf)
		{
			FREEMEM(ulp.message.choice.msSUPLEND.position->timestamp.buf);
		}
		if(ulp.message.choice.msSUPLEND.position->velocity)
		{
			if(ulp.message.choice.msSUPLEND.position->velocity->present==Velocity_PR_horandveruncert)
			{
				if(ulp.message.choice.msSUPLEND.position->velocity->choice.horandveruncert.bearing.buf)
				{
					FREEMEM(ulp.message.choice.msSUPLEND.position->velocity->choice.horandveruncert.bearing.buf);
				}
				if(ulp.message.choice.msSUPLEND.position->velocity->choice.horandveruncert.horspeed.buf)
				{
					FREEMEM(ulp.message.choice.msSUPLEND.position->velocity->choice.horandveruncert.horspeed.buf);
				}
				if(ulp.message.choice.msSUPLEND.position->velocity->choice.horandveruncert.horuncertspeed.buf)
				{
					FREEMEM(ulp.message.choice.msSUPLEND.position->velocity->choice.horandveruncert.horuncertspeed.buf);
				}
				if(ulp.message.choice.msSUPLEND.position->velocity->choice.horandveruncert.verdirect.buf)
				{
					FREEMEM(ulp.message.choice.msSUPLEND.position->velocity->choice.horandveruncert.verdirect.buf);
				}		
				if(ulp.message.choice.msSUPLEND.position->velocity->choice.horandveruncert.verspeed.buf)
				{
					FREEMEM(ulp.message.choice.msSUPLEND.position->velocity->choice.horandveruncert.verspeed.buf);
				}
				if(ulp.message.choice.msSUPLEND.position->velocity->choice.horandveruncert.veruncertspeed.buf)
				{
					FREEMEM(ulp.message.choice.msSUPLEND.position->velocity->choice.horandveruncert.veruncertspeed.buf);
				}
			}
			else if(ulp.message.choice.msSUPLEND.position->velocity->present==Velocity_PR_horandvervel)
			{
				if(ulp.message.choice.msSUPLEND.position->velocity->choice.horandvervel.bearing.buf)
				{
					FREEMEM(ulp.message.choice.msSUPLEND.position->velocity->choice.horandvervel.bearing.buf);
				}
				if(ulp.message.choice.msSUPLEND.position->velocity->choice.horandvervel.horspeed.buf)
				{
					FREEMEM(ulp.message.choice.msSUPLEND.position->velocity->choice.horandvervel.horspeed.buf)
				}
				if(ulp.message.choice.msSUPLEND.position->velocity->choice.horandvervel.verdirect.buf)
				{
					FREEMEM(ulp.message.choice.msSUPLEND.position->velocity->choice.horandvervel.verdirect.buf);
				}
				if(ulp.message.choice.msSUPLEND.position->velocity->choice.horandvervel.verspeed.buf)
				{
					FREEMEM(ulp.message.choice.msSUPLEND.position->velocity->choice.horandvervel.verspeed.buf);
				}
			}
			else if(ulp.message.choice.msSUPLEND.position->velocity->present==Velocity_PR_horvel)
			{
				if(ulp.message.choice.msSUPLEND.position->velocity->choice.horvel.bearing.buf)
				{
					FREEMEM(ulp.message.choice.msSUPLEND.position->velocity->choice.horvel.bearing.buf);
				}
				if(ulp.message.choice.msSUPLEND.position->velocity->choice.horvel.horspeed.buf)
				{
					FREEMEM(ulp.message.choice.msSUPLEND.position->velocity->choice.horvel.horspeed.buf);
				}
			}
			else if(ulp.message.choice.msSUPLEND.position->velocity->present==Velocity_PR_horveluncert)
			{
				if(ulp.message.choice.msSUPLEND.position->velocity->choice.horveluncert.bearing.buf)
				{
					FREEMEM(ulp.message.choice.msSUPLEND.position->velocity->choice.horveluncert.bearing.buf);
				}
				if(ulp.message.choice.msSUPLEND.position->velocity->choice.horveluncert.horspeed.buf)
				{
					FREEMEM(ulp.message.choice.msSUPLEND.position->velocity->choice.horveluncert.horspeed.buf);
				}
				if(ulp.message.choice.msSUPLEND.position->velocity->choice.horveluncert.uncertspeed.buf)
				{
					FREEMEM(ulp.message.choice.msSUPLEND.position->velocity->choice.horveluncert.uncertspeed.buf);
				}
			}
			FREEMEM(ulp.message.choice.msSUPLEND.position->velocity);
		}
	}	
	if(ulp.message.choice.msSUPLEND.statusCode)
	{
		FREEMEM(ulp.message.choice.msSUPLEND.statusCode->buf);
		FREEMEM(ulp.message.choice.msSUPLEND.statusCode);
	}
	if(ulp.message.choice.msSUPLEND.ver)
	{
		FREEMEM(ulp.message.choice.msSUPLEND.ver->buf);
		FREEMEM(ulp.message.choice.msSUPLEND.ver);
	}
}

SUCCESS_ERROR_T ULP_Processor::ParseHeader(Engine::CSUPLMessage* message)
{
	message->m_Version.m_Major=(Engine::uint8_t)ulp.version.maj;
	message->m_Version.m_Minor=(Engine::uint8_t)ulp.version.min;
	message->m_Version.m_Servind=(Engine::uint8_t)ulp.version.servind;
	/*Parse Message Header: SessionID, Version*/
	if(!ulp.sessionID.setSessionID)
		message->m_SessionID.iSETSessionID=NULL;
	else
	{
		/*message->m_SessionID.iSETSessionID=new(nothrow) Engine::CSETSessionID;*/
		ULP_NEW_OBJ(message->m_SessionID.iSETSessionID,Engine::CSETSessionID);
// 		if(!message->m_SessionID.iSETSessionID)
// 		{return NO_MEMORY;}
		//Parse Message structure and fill the fields
		//1. Parse SetSessionID
		//1.1. Parse SessionId
		message->m_SessionID.iSETSessionID->sessionId=ulp.sessionID.setSessionID->sessionId;
		//1.2. Parse setId
		if(ulp.sessionID.setSessionID->setId.present==SETId_PR_msisdn)
		{
			ULP_NEW_OBJ(message->m_SessionID.iSETSessionID->iSetID,Engine::MSDNChoice);
// 			if(!((Engine::MSDNChoice*)(message->m_SessionID.iSETSessionID->iSetID)))
// 			{return NO_MEMORY;}
			if(!((Engine::MSDNChoice*)(message->m_SessionID.iSETSessionID->iSetID))->iMSISDN.m_Data)
			{
				ULP_NEW_VAL(((Engine::MSDNChoice*)(message->m_SessionID.iSETSessionID->iSetID))->iMSISDN.m_Data,Engine::uint8_t[ulp.sessionID.setSessionID->setId.choice.msisdn.size]);
// 				if(!((Engine::MSDNChoice*)(message->m_SessionID.iSETSessionID->iSetID))->iMSISDN.m_Data)
// 				{return NO_MEMORY;}
			}
			((Engine::MSDNChoice*)(message->m_SessionID.iSETSessionID->iSetID))->iMSISDN.m_Size=ulp.sessionID.setSessionID->setId.choice.msisdn.size;
			memcpy(((Engine::MSDNChoice*)(message->m_SessionID.iSETSessionID->iSetID))->iMSISDN.m_Data,ulp.sessionID.setSessionID->setId.choice.msisdn.buf,ulp.sessionID.setSessionID->setId.choice.msisdn.size);		
		}
		else if(ulp.sessionID.setSessionID->setId.present==SETId_PR_mdn)
		{
			ULP_NEW_OBJ(message->m_SessionID.iSETSessionID->iSetID,Engine::MDNChoice);
// 			if(!((Engine::MDNChoice*)(message->m_SessionID.iSETSessionID->iSetID)))
// 			{return NO_MEMORY;}
			if(!((Engine::MDNChoice*)(message->m_SessionID.iSETSessionID->iSetID))->iMDN.m_Data)
			{
				ULP_NEW_VAL(((Engine::MDNChoice*)(message->m_SessionID.iSETSessionID->iSetID))->iMDN.m_Data,Engine::uint8_t[ulp.sessionID.setSessionID->setId.choice.mdn.size]);
// 				if(!((Engine::MDNChoice*)(message->m_SessionID.iSETSessionID->iSetID))->iMDN.m_Data)
// 				{return NO_MEMORY;}
			}
			((Engine::MDNChoice*)(message->m_SessionID.iSETSessionID->iSetID))->iMDN.m_Size=ulp.sessionID.setSessionID->setId.choice.mdn.size;
			memcpy(((Engine::MDNChoice*)(message->m_SessionID.iSETSessionID->iSetID))->iMDN.m_Data,ulp.sessionID.setSessionID->setId.choice.mdn.buf,ulp.sessionID.setSessionID->setId.choice.mdn.size);		
		}
		else if(ulp.sessionID.setSessionID->setId.present==SETId_PR_min)
		{
			ULP_NEW_OBJ(message->m_SessionID.iSETSessionID->iSetID,Engine::MINChoice);
// 			if(!((Engine::MINChoice*)(message->m_SessionID.iSETSessionID->iSetID)))
// 			{return NO_MEMORY;}
			if(!((Engine::MINChoice*)(message->m_SessionID.iSETSessionID->iSetID))->iMin.m_Data)
			{
				ULP_NEW_VAL(((Engine::MINChoice*)(message->m_SessionID.iSETSessionID->iSetID))->iMin.m_Data,Engine::uint8_t[ulp.sessionID.setSessionID->setId.choice.min.size]);
// 				if(!((Engine::MINChoice*)(message->m_SessionID.iSETSessionID->iSetID))->iMin.m_Data)
// 				{return NO_MEMORY;}
			}
			((Engine::MINChoice*)(message->m_SessionID.iSETSessionID->iSetID))->iMin.m_Size=ulp.sessionID.setSessionID->setId.choice.min.size;
			memcpy(((Engine::MINChoice*)(message->m_SessionID.iSETSessionID->iSetID))->iMin.m_Data,ulp.sessionID.setSessionID->setId.choice.min.buf,ulp.sessionID.setSessionID->setId.choice.min.size);		
		}
		else if(ulp.sessionID.setSessionID->setId.present==SETId_PR_imsi)
		{
			ULP_NEW_OBJ(message->m_SessionID.iSETSessionID->iSetID,Engine::IMSIChoice);
// 			if(!((Engine::IMSIChoice*)(message->m_SessionID.iSETSessionID->iSetID)))
// 			{return NO_MEMORY;}
			if(!((Engine::IMSIChoice*)(message->m_SessionID.iSETSessionID->iSetID))->iIMSI.m_Data)
			{
				ULP_NEW_VAL(((Engine::IMSIChoice*)(message->m_SessionID.iSETSessionID->iSetID))->iIMSI.m_Data,Engine::uint8_t[ulp.sessionID.setSessionID->setId.choice.imsi.size]);
// 				if(!((Engine::IMSIChoice*)(message->m_SessionID.iSETSessionID->iSetID))->iIMSI.m_Data)
// 				{return NO_MEMORY;}
			}
			((Engine::IMSIChoice*)(message->m_SessionID.iSETSessionID->iSetID))->iIMSI.m_Size=ulp.sessionID.setSessionID->setId.choice.imsi.size;
			memcpy(((Engine::IMSIChoice*)(message->m_SessionID.iSETSessionID->iSetID))->iIMSI.m_Data,ulp.sessionID.setSessionID->setId.choice.imsi.buf,ulp.sessionID.setSessionID->setId.choice.imsi.size);		
		}
		else if(ulp.sessionID.setSessionID->setId.present==SETId_PR_nai)
		{
			ULP_NEW_OBJ(message->m_SessionID.iSETSessionID->iSetID,Engine::NAIChoice);
// 			if(!((Engine::NAIChoice*)(message->m_SessionID.iSETSessionID->iSetID)))
// 			{return NO_MEMORY;}
			if(!((Engine::NAIChoice*)(message->m_SessionID.iSETSessionID->iSetID))->iNai.m_Data)
			{
				ULP_NEW_VAL(((Engine::NAIChoice*)(message->m_SessionID.iSETSessionID->iSetID))->iNai.m_Data,Engine::uint8_t[ulp.sessionID.setSessionID->setId.choice.nai.size]);
// 				if(!((Engine::NAIChoice*)(message->m_SessionID.iSETSessionID->iSetID))->iNai.m_Data)
// 				{return NO_MEMORY;}
			}
			((Engine::NAIChoice*)(message->m_SessionID.iSETSessionID->iSetID))->iNai.m_Size=ulp.sessionID.setSessionID->setId.choice.nai.size;
			memcpy(((Engine::NAIChoice*)(message->m_SessionID.iSETSessionID->iSetID))->iNai.m_Data,ulp.sessionID.setSessionID->setId.choice.nai.buf,ulp.sessionID.setSessionID->setId.choice.nai.size);		
		}
		else if(ulp.sessionID.setSessionID->setId.present==SETId_PR_iPAddress)
		{
			ULP_NEW_OBJ(message->m_SessionID.iSETSessionID->iSetID,Engine::CIPAddress);
// 			if(!((Engine::CIPAddress*)(message->m_SessionID.iSETSessionID->iSetID)))
// 			{return NO_MEMORY;}
			if(ulp.sessionID.setSessionID->setId.choice.iPAddress.present==IPAddress_PR_ipv4Address)
			{
				Engine::CIPAddress* ip_addr = (Engine::CIPAddress*)message->m_SessionID.iSETSessionID->iSetID;
				ULP_NEW_OBJ(ip_addr->iIP_Addr,Engine::CIPv4);
// 				if(!ip_addr->iIP_Addr)
// 				{return NO_MEMORY;}
				if(!((Engine::CIPv4*)ip_addr->iIP_Addr)->m_Addr.m_Data)
				{
					ULP_NEW_VAL(((Engine::CIPv4*)(ip_addr->iIP_Addr))->m_Addr.m_Data,Engine::uint8_t[ulp.sessionID.setSessionID->setId.choice.iPAddress.choice.ipv4Address.size]);
// 					if(!((Engine::CIPv4*)(ip_addr->iIP_Addr))->m_Addr.m_Data)
// 					{return NO_MEMORY;}
				}
				((Engine::CIPv4*)(ip_addr->iIP_Addr))->m_Addr.m_Size=ulp.sessionID.setSessionID->setId.choice.iPAddress.choice.ipv4Address.size;
				memcpy(((Engine::CIPv4*)(ip_addr->iIP_Addr))->m_Addr.m_Data,ulp.sessionID.setSessionID->setId.choice.iPAddress.choice.ipv4Address.buf,ulp.sessionID.setSessionID->setId.choice.iPAddress.choice.ipv4Address.size);		
			}
			else if(ulp.sessionID.setSessionID->setId.choice.iPAddress.present==IPAddress_PR_ipv6Address)
			{
				Engine::CIPAddress* ip_addr = (Engine::CIPAddress*)message->m_SessionID.iSETSessionID->iSetID;
				ULP_NEW_OBJ(ip_addr->iIP_Addr,Engine::CIPv6);
// 				if(!ip_addr->iIP_Addr)
// 				{return NO_MEMORY;}
				if(!((Engine::CIPv6*)(ip_addr->iIP_Addr))->m_Addr.m_Data)
				{
					ULP_NEW_VAL(((Engine::CIPv6*)(ip_addr->iIP_Addr))->m_Addr.m_Data,Engine::uint8_t[ulp.sessionID.setSessionID->setId.choice.iPAddress.choice.ipv6Address.size]);
// 					if(!((Engine::CIPv6*)(ip_addr->iIP_Addr))->m_Addr.m_Data)
// 					{return NO_MEMORY;}
				}
				((Engine::CIPv6*)(ip_addr->iIP_Addr))->m_Addr.m_Size=ulp.sessionID.setSessionID->setId.choice.iPAddress.choice.ipv6Address.size;
				memcpy(((Engine::CIPv6*)(ip_addr->iIP_Addr))->m_Addr.m_Data,ulp.sessionID.setSessionID->setId.choice.iPAddress.choice.ipv6Address.buf,ulp.sessionID.setSessionID->setId.choice.iPAddress.choice.ipv6Address.size);		
			}
			else 
				return INVALID_SESSION_ID;		
		}
		else
			return INVALID_SESSION_ID;
	}
	//2. Parse SlpSessionID
	if(!ulp.sessionID.slpSessionID)
	{
		message->m_SessionID.iSLPSessionID=NULL;
		return INVALID_SESSION_ID;
	}
	else
	{
		ULP_NEW_OBJ(message->m_SessionID.iSLPSessionID,Engine::CSLPSessionID);
		message->m_SessionID.iSLPSessionID->iSessionId.m_Size=4;
		memcpy(message->m_SessionID.iSLPSessionID->iSessionId.m_Data,ulp.sessionID.slpSessionID->sessionID.buf,4);
		if(ulp.sessionID.slpSessionID->slpId.present==SLPAddress_PR_iPAddress)
		{
			Engine::CIPAddress* ip_address;
			ULP_NEW_OBJ(message->m_SessionID.iSLPSessionID->iSLPaddr,Engine::CIPAddress);
// 			if(!message->m_SessionID.iSLPSessionID->iSLPaddr)
// 			{return NO_MEMORY;}
			ip_address=(Engine::CIPAddress*)message->m_SessionID.iSLPSessionID->iSLPaddr;
			if(ulp.sessionID.slpSessionID->slpId.choice.iPAddress.present==IPAddress_PR_ipv4Address)
			{
				ULP_NEW_OBJ(ip_address->iIP_Addr,Engine::CIPv4);
// 				if(!ip_address->iIP_Addr)
// 				{return NO_MEMORY;}
				if(!((Engine::CIPv4*)(ip_address->iIP_Addr))->m_Addr.m_Data)
				{
					ULP_NEW_VAL(((Engine::CIPv4*)(ip_address->iIP_Addr))->m_Addr.m_Data,Engine::uint8_t[4]);
// 					if(!((Engine::CIPv4*)(ip_address->iIP_Addr))->m_Addr.m_Data)
// 					{return NO_MEMORY;}
				}
				((Engine::CIPv4*)(ip_address->iIP_Addr))->m_Addr.m_Size=4;
				memcpy(((Engine::CIPv4*)(ip_address->iIP_Addr))->m_Addr.m_Data,ulp.sessionID.slpSessionID->slpId.choice.iPAddress.choice.ipv4Address.buf,4);
			}
			else if(ulp.sessionID.slpSessionID->slpId.choice.iPAddress.present==IPAddress_PR_ipv6Address)
			{
				ULP_NEW_OBJ(ip_address->iIP_Addr,Engine::CIPv6);
// 				if(!ip_address->iIP_Addr)
// 				{return NO_MEMORY;}
				if(!((Engine::CIPv6*)(ip_address->iIP_Addr))->m_Addr.m_Data)
				{
					ULP_NEW_VAL(((Engine::CIPv6*)(ip_address->iIP_Addr))->m_Addr.m_Data,Engine::uint8_t[ulp.sessionID.slpSessionID->slpId.choice.iPAddress.choice.ipv6Address.size]);
// 					if(!((Engine::CIPv6*)(ip_address->iIP_Addr))->m_Addr.m_Data)
// 					{return NO_MEMORY;}
				}
				((Engine::CIPv6*)(ip_address->iIP_Addr))->m_Addr.m_Size=ulp.sessionID.slpSessionID->slpId.choice.iPAddress.choice.ipv6Address.size;
				memcpy(((Engine::CIPv6*)(ip_address->iIP_Addr))->m_Addr.m_Data,ulp.sessionID.slpSessionID->slpId.choice.iPAddress.choice.ipv6Address.buf,ulp.sessionID.slpSessionID->slpId.choice.iPAddress.choice.ipv6Address.size);
			}
			else
				return INVALID_SESSION_ID;
		}
		else if (ulp.sessionID.slpSessionID->slpId.present==SLPAddress_PR_fQDN)
		{
			ULP_NEW_OBJ(message->m_SessionID.iSLPSessionID->iSLPaddr,Engine::CFQDN);
// 			if(!message->m_SessionID.iSLPSessionID->iSLPaddr)
// 			{return NO_MEMORY;}
			if(!((Engine::CFQDN*)(message->m_SessionID.iSLPSessionID->iSLPaddr))->m_FQDN.m_Data)
			{
				ULP_NEW_VAL(((Engine::CFQDN*)(message->m_SessionID.iSLPSessionID->iSLPaddr))->m_FQDN.m_Data,Engine::uint8_t[ulp.sessionID.slpSessionID->slpId.choice.fQDN.size]);
// 				if(!((Engine::CFQDN*)(message->m_SessionID.iSLPSessionID->iSLPaddr))->m_FQDN.m_Data)
// 				{return NO_MEMORY;}
			}
			((Engine::CFQDN*)(message->m_SessionID.iSLPSessionID->iSLPaddr))->m_FQDN.m_Size=ulp.sessionID.slpSessionID->slpId.choice.fQDN.size;
			memcpy(((Engine::CFQDN*)(message->m_SessionID.iSLPSessionID->iSLPaddr))->m_FQDN.m_Data,ulp.sessionID.slpSessionID->slpId.choice.fQDN.buf,ulp.sessionID.slpSessionID->slpId.choice.fQDN.size);
		}
		else 
			return INVALID_SESSION_ID;	
	}
	
	if((ulp.message.present == UlpMessage_PR_msSUPLINIT) && ulp.sessionID.setSessionID)
	{
		return INVALID_SESSION_ID;
	}
	if(ulp.version.maj!=MAJ_VERSION)// || ulp.version.min!=MIN_VERSION || ulp.version.servind!=SER_VERSION)
	{
		return VER_NOT_SUPPORTED;
	}

	return SUCCESS;
}

SUCCESS_ERROR_T ULP_Processor::Parse_SUPLEND(Engine::CSUPLMessage* message)
{
	LOGD("SUPL: In SUPL_END parser \n");
	if(!ulp.message.choice.msSUPLEND.position)
	{
		((Engine::CSUPLEnd*)(message))->m_pPosition=NULL;
	}
	else
	{
		ULP_NEW_OBJ(((Engine::CSUPLEnd*)(message))->m_pPosition,Engine::Position);
// 		if(!((Engine::CSUPLEnd*)(message))->m_pPosition)
// 		{return NO_MEMORY;}
		((Engine::CSUPLEnd*)(message))->m_pPosition->m_PositionEstimate.m_Latitude=ulp.message.choice.msSUPLEND.position->positionEstimate.latitude;
		((Engine::CSUPLEnd*)(message))->m_pPosition->m_PositionEstimate.m_LatitudeSign=(Engine::LatitudeSign)(*(ulp.message.choice.msSUPLEND.position->positionEstimate.latitudeSign.buf));
		((Engine::CSUPLEnd*)(message))->m_pPosition->m_PositionEstimate.m_Longitude=ulp.message.choice.msSUPLEND.position->positionEstimate.longitude;
		if(!ulp.message.choice.msSUPLEND.position->positionEstimate.altitudeInfo)
		{
			((Engine::CSUPLEnd*)(message))->m_pPosition->m_PositionEstimate.m_pAltitudeInfo=NULL;
		}
		else
		{
			ULP_NEW_VAL(((Engine::CSUPLEnd*)(message))->m_pPosition->m_PositionEstimate.m_pAltitudeInfo,Engine::AltitudeInfo);
// 			if(!((Engine::CSUPLEnd*)(message))->m_pPosition->m_PositionEstimate.m_pAltitudeInfo)
// 			{return NO_MEMORY;}
			((Engine::CSUPLEnd*)(message))->m_pPosition->m_PositionEstimate.m_pAltitudeInfo->m_Altitude=ulp.message.choice.msSUPLEND.position->positionEstimate.altitudeInfo->altitude;
			((Engine::CSUPLEnd*)(message))->m_pPosition->m_PositionEstimate.m_pAltitudeInfo->m_AltitudeDirection=(Engine::AltitudeDirection)(*(ulp.message.choice.msSUPLEND.position->positionEstimate.altitudeInfo->altitudeDirection.buf));
			((Engine::CSUPLEnd*)(message))->m_pPosition->m_PositionEstimate.m_pAltitudeInfo->m_AltUncertainty=ulp.message.choice.msSUPLEND.position->positionEstimate.altitudeInfo->altUncertainty;
		}
		if(!ulp.message.choice.msSUPLEND.position->positionEstimate.uncertainty)
		{
			((Engine::CSUPLEnd*)(message))->m_pPosition->m_PositionEstimate.m_pUncertainty=NULL;
		}
		else
		{
			ULP_NEW_VAL(((Engine::CSUPLEnd*)(message))->m_pPosition->m_PositionEstimate.m_pUncertainty,Engine::Uncertainty);
// 			if(!((Engine::CSUPLEnd*)(message))->m_pPosition->m_PositionEstimate.m_pUncertainty)
// 			{return NO_MEMORY;}
			((Engine::CSUPLEnd*)(message))->m_pPosition->m_PositionEstimate.m_pUncertainty->m_OrientationMajorAxis=(Engine::uint8_t)ulp.message.choice.msSUPLEND.position->positionEstimate.uncertainty->orientationMajorAxis;
			((Engine::CSUPLEnd*)(message))->m_pPosition->m_PositionEstimate.m_pUncertainty->m_UncertaintySemiMajor=(Engine::uint8_t)ulp.message.choice.msSUPLEND.position->positionEstimate.uncertainty->uncertaintySemiMajor;
			((Engine::CSUPLEnd*)(message))->m_pPosition->m_PositionEstimate.m_pUncertainty->m_UncertaintySeminMinor=(Engine::uint8_t)ulp.message.choice.msSUPLEND.position->positionEstimate.uncertainty->uncertaintySemiMinor;
		}
		if(!ulp.message.choice.msSUPLEND.position->positionEstimate.confidence)
		{
			((Engine::CSUPLEnd*)(message))->m_pPosition->m_PositionEstimate.m_pConfidence=NULL;
		}
		else
		{
			ULP_NEW_VAL(((Engine::CSUPLEnd*)(message))->m_pPosition->m_PositionEstimate.m_pConfidence,Engine::uint8_t);
// 			if(!((Engine::CSUPLEnd*)(message))->m_pPosition->m_PositionEstimate.m_pConfidence)
// 			{return NO_MEMORY;}
			*(((Engine::CSUPLEnd*)(message))->m_pPosition->m_PositionEstimate.m_pConfidence)=(Engine::uint8_t)*(ulp.message.choice.msSUPLEND.position->positionEstimate.confidence);
		}
		if(!ulp.message.choice.msSUPLEND.position->timestamp.buf)
		{return MAN_PARAM_IS_ABSENT;}
		ULP_NEW_VAL(((Engine::CSUPLEnd*)(message))->m_pPosition->m_UTCTime.m_Data,Engine::uint8_t[ulp.message.choice.msSUPLEND.position->timestamp.size]);
// 		if(!((Engine::CSUPLEnd*)(message))->m_pPosition->m_UTCTime.m_Data)
// 		{return NO_MEMORY;}
		((Engine::CSUPLEnd*)(message))->m_pPosition->m_UTCTime.m_Size=ulp.message.choice.msSUPLEND.position->timestamp.size;
		memcpy(((Engine::CSUPLEnd*)(message))->m_pPosition->m_UTCTime.m_Data,ulp.message.choice.msSUPLEND.position->timestamp.buf,ulp.message.choice.msSUPLEND.position->timestamp.size);
		if(!ulp.message.choice.msSUPLEND.position->velocity)
		{
			((Engine::CSUPLEnd*)(message))->m_pPosition->m_pVelocity=NULL;
		}
		else
		{
			if(ulp.message.choice.msSUPLEND.position->velocity->present==Velocity_PR_horandveruncert)
			{
				ULP_NEW_OBJ(((Engine::CSUPLEnd*)(message))->m_pPosition->m_pVelocity,Engine::CHorandveruncert);
// 				if(!((Engine::CSUPLEnd*)(message))->m_pPosition->m_pVelocity)
// 				{return NO_MEMORY;}
				Engine::CHorandveruncert* horandveruncert;
				horandveruncert=(Engine::CHorandveruncert*)((Engine::CSUPLEnd*)(message))->m_pPosition->m_pVelocity;
				if(!horandveruncert->m_Bearing.m_Data)
				{
					ULP_NEW_VAL(horandveruncert->m_Bearing.m_Data,Engine::uint8_t[2]);
// 					if(!horandveruncert->m_Bearing.m_Data)
// 					{return NO_MEMORY;}
				}				
				horandveruncert->m_Bearing.m_Size=2;
				memcpy(horandveruncert->m_Bearing.m_Data,ulp.message.choice.msSUPLEND.position->velocity->choice.horandveruncert.bearing.buf,2);
				if(!horandveruncert->m_HorSpeed.m_Data)
				{
					ULP_NEW_VAL(horandveruncert->m_HorSpeed.m_Data,Engine::uint8_t[2]);
// 					if(!horandveruncert->m_HorSpeed.m_Data)
// 					{return NO_MEMORY;}
				}				
				horandveruncert->m_HorSpeed.m_Size=2;
				memcpy(horandveruncert->m_HorSpeed.m_Data,ulp.message.choice.msSUPLEND.position->velocity->choice.horandveruncert.horspeed.buf,2);
				if(!horandveruncert->m_HorUncertSpeed.m_Data)
				{
					ULP_NEW_VAL(horandveruncert->m_HorUncertSpeed.m_Data,Engine::uint8_t);
// 					if(!horandveruncert->m_HorUncertSpeed.m_Data)
// 					{return NO_MEMORY;}
				}				
				horandveruncert->m_HorUncertSpeed.m_Size=1;
				memcpy(horandveruncert->m_HorUncertSpeed.m_Data,ulp.message.choice.msSUPLEND.position->velocity->choice.horandveruncert.horuncertspeed.buf,1);
				if(!horandveruncert->m_VerDirect.m_Data)
				{
					ULP_NEW_VAL(horandveruncert->m_VerDirect.m_Data,Engine::uint8_t);
// 					if(!horandveruncert->m_VerDirect.m_Data)
// 					{return NO_MEMORY;}
				}
				horandveruncert->m_VerDirect.m_Size=1;
				memcpy(horandveruncert->m_VerDirect.m_Data,ulp.message.choice.msSUPLEND.position->velocity->choice.horandveruncert.verdirect.buf,1);
				if(!horandveruncert->m_VerSpeed.m_Data)
				{
					ULP_NEW_VAL(horandveruncert->m_VerSpeed.m_Data,Engine::uint8_t);
// 					if(!horandveruncert->m_VerSpeed.m_Data)
// 					{return NO_MEMORY;}
				}
				memcpy(horandveruncert->m_VerSpeed.m_Data,ulp.message.choice.msSUPLEND.position->velocity->choice.horandveruncert.verspeed.buf,1);
				if(!horandveruncert->m_VerUncertSpeed.m_Data)
				{
					ULP_NEW_VAL(horandveruncert->m_VerUncertSpeed.m_Data,Engine::uint8_t);
// 					if(!horandveruncert->m_VerUncertSpeed.m_Data)
// 					{return NO_MEMORY;}
				}
				horandveruncert->m_VerUncertSpeed.m_Size=1;
				memcpy(horandveruncert->m_VerUncertSpeed.m_Data,ulp.message.choice.msSUPLEND.position->velocity->choice.horandveruncert.veruncertspeed.buf,1);
			}
			else if(ulp.message.choice.msSUPLEND.position->velocity->present==Velocity_PR_horandvervel)
			{
				ULP_NEW_OBJ(((Engine::CSUPLEnd*)(message))->m_pPosition->m_pVelocity,Engine::CHorandvervel);
// 				if(!((Engine::CSUPLEnd*)(message))->m_pPosition->m_pVelocity)
// 				{return NO_MEMORY;}
				Engine::CHorandvervel* horandvervel;
				horandvervel=(Engine::CHorandvervel*)((Engine::CSUPLEnd*)(message))->m_pPosition->m_pVelocity;
				if(!horandvervel->m_Bearing.m_Data)
				{
					ULP_NEW_VAL(horandvervel->m_Bearing.m_Data,Engine::uint8_t[2]);
// 					if(!horandvervel->m_Bearing.m_Data)
// 					{return NO_MEMORY;}
				}
				horandvervel->m_Bearing.m_Size=2;
				memcpy(horandvervel->m_Bearing.m_Data,ulp.message.choice.msSUPLEND.position->velocity->choice.horandvervel.bearing.buf,2);
				if(!horandvervel->m_HorSpeed.m_Data)
				{
					ULP_NEW_VAL(horandvervel->m_HorSpeed.m_Data,Engine::uint8_t[2]);
// 					if(!horandvervel->m_HorSpeed.m_Data)
// 					{return NO_MEMORY;}
				}
				horandvervel->m_HorSpeed.m_Size=2;
				memcpy(horandvervel->m_HorSpeed.m_Data,ulp.message.choice.msSUPLEND.position->velocity->choice.horandvervel.horspeed.buf,2);
				if(!horandvervel->m_VerDirect.m_Data)
				{
					ULP_NEW_VAL(horandvervel->m_VerDirect.m_Data,Engine::uint8_t);
// 					if(!horandvervel->m_VerDirect.m_Data)
// 					{return NO_MEMORY;}
				}
				horandvervel->m_VerDirect.m_Size=1;
				memcpy(horandvervel->m_VerDirect.m_Data,ulp.message.choice.msSUPLEND.position->velocity->choice.horandvervel.verdirect.buf,1);
				if(!horandvervel->m_VerSpeed.m_Data)
				{
					ULP_NEW_VAL(horandvervel->m_VerSpeed.m_Data,Engine::uint8_t);
// 					if(!horandvervel->m_VerSpeed.m_Data)
// 					{return NO_MEMORY;}
				}
				horandvervel->m_VerSpeed.m_Size=1;
				memcpy(horandvervel->m_VerSpeed.m_Data,ulp.message.choice.msSUPLEND.position->velocity->choice.horandvervel.verspeed.buf,1);
			}
			else if(ulp.message.choice.msSUPLEND.position->velocity->present==Velocity_PR_horvel)
			{
				ULP_NEW_OBJ(((Engine::CSUPLEnd*)(message))->m_pPosition->m_pVelocity,Engine::CHorvel);
// 				if(!((Engine::CSUPLEnd*)(message))->m_pPosition->m_pVelocity)
// 				{return NO_MEMORY;}
				Engine::CHorvel* horvel;
				horvel=(Engine::CHorvel*)((Engine::CSUPLEnd*)(message))->m_pPosition->m_pVelocity;
				if(!horvel->m_Bearing.m_Data)
				{
					ULP_NEW_VAL(horvel->m_Bearing.m_Data,Engine::uint8_t[2]);
// 					if(!horvel->m_Bearing.m_Data)
// 					{return NO_MEMORY;}
				}
				horvel->m_Bearing.m_Size=2;
				memcpy(horvel->m_Bearing.m_Data,ulp.message.choice.msSUPLEND.position->velocity->choice.horvel.bearing.buf,2);
				if(!horvel->m_HorSpeed.m_Data)
				{
					ULP_NEW_VAL(horvel->m_HorSpeed.m_Data,Engine::uint8_t[2]);
// 					if(!horvel->m_HorSpeed.m_Data)
// 					{return NO_MEMORY;}
				}
				horvel->m_HorSpeed.m_Size=2;
				memcpy(horvel->m_HorSpeed.m_Data,ulp.message.choice.msSUPLEND.position->velocity->choice.horvel.horspeed.buf,2);
			}
			else if(ulp.message.choice.msSUPLEND.position->velocity->present==Velocity_PR_horveluncert)
			{
				ULP_NEW_OBJ(((Engine::CSUPLEnd*)(message))->m_pPosition->m_pVelocity,Engine::CHorveluncert);
// 				if(!((Engine::CSUPLEnd*)(message))->m_pPosition->m_pVelocity)
// 				{return NO_MEMORY;}
				Engine::CHorveluncert* horveluncert;
				horveluncert=(Engine::CHorveluncert*)((Engine::CSUPLEnd*)(message))->m_pPosition->m_pVelocity;
				if(!horveluncert->m_Bearing.m_Data)
				{
					ULP_NEW_VAL(horveluncert->m_Bearing.m_Data,Engine::uint8_t[2]);
// 					if(!horveluncert->m_Bearing.m_Data)
// 					{return NO_MEMORY;}
				}
				horveluncert->m_Bearing.m_Size=2;
				memcpy(horveluncert->m_Bearing.m_Data,ulp.message.choice.msSUPLEND.position->velocity->choice.horveluncert.bearing.buf,2);	
				if(!horveluncert->m_HorSpeed.m_Data)
				{
					ULP_NEW_VAL(horveluncert->m_HorSpeed.m_Data,Engine::uint8_t[2]);
// 					if(!horveluncert->m_HorSpeed.m_Data)
// 					{return NO_MEMORY;}
				}
				horveluncert->m_HorSpeed.m_Size=2;
				memcpy(horveluncert->m_HorSpeed.m_Data,ulp.message.choice.msSUPLEND.position->velocity->choice.horveluncert.horspeed.buf,2);	
				if(!horveluncert->m_UncertSpeed.m_Data)
				{
					ULP_NEW_VAL(horveluncert->m_UncertSpeed.m_Data,Engine::uint8_t);
// 					if(!horveluncert->m_UncertSpeed.m_Data)
// 					{return NO_MEMORY;}
				}
				horveluncert->m_UncertSpeed.m_Size=1;
				memcpy(horveluncert->m_UncertSpeed.m_Data,ulp.message.choice.msSUPLEND.position->velocity->choice.horveluncert.uncertspeed.buf,1);	
			}
		}
	}	

	if(!ulp.message.choice.msSUPLEND.statusCode)
	{
		((Engine::CSUPLEnd*)(message))->m_pStatusCode=NULL;
	}
	else
	{
		ULP_NEW_VAL(((Engine::CSUPLEnd*)(message))->m_pStatusCode,Engine::StatusCode);
// 		if(!((Engine::CSUPLEnd*)(message))->m_pStatusCode)
// 		{return NO_MEMORY;}
		*(((Engine::CSUPLEnd*)(message))->m_pStatusCode)=(Engine::StatusCode)*(ulp.message.choice.msSUPLEND.statusCode->buf);
		LOGD("SUPL: SUPL_END status code: %d\n",*ulp.message.choice.msSUPLEND.statusCode->buf);
	}
	if(!ulp.message.choice.msSUPLEND.ver)
		((Engine::CSUPLEnd*)(message))->m_pVer=NULL;
	else
	{
		ULP_NEW_OBJ(((Engine::CSUPLEnd*)(message))->m_pVer,Engine::CBitString(64));
// 		if(!((Engine::CSUPLEnd*)(message))->m_pVer)
// 		{return NO_MEMORY;}
		if(!((Engine::CSUPLEnd*)(message))->m_pVer->m_Data)
		{
			ULP_NEW_VAL(((Engine::CSUPLEnd*)(message))->m_pVer->m_Data,Engine::uint8_t[8]);
// 			if(!((Engine::CSUPLEnd*)(message))->m_pVer->m_Data)
// 			{return NO_MEMORY;}
		}
		((Engine::CSUPLEnd*)(message))->m_pVer->m_Size=8;
		memcpy(((Engine::CSUPLEnd*)(message))->m_pVer->m_Data,ulp.message.choice.msSUPLEND.ver->buf,8);
	}
	
	return SUCCESS;
}


SUCCESS_ERROR_T ULP_Processor::Parse_SUPLPOS(Engine::CSUPLMessage* message)
{
	if(!ulp.message.choice.msSUPLPOS.velocity)
	{
			((Engine::CSUPLPos*)(message))->m_pVelocity=NULL;
	}
	else
	{
		if(ulp.message.choice.msSUPLPOS.velocity->present==Velocity_PR_horandveruncert)
		{
				ULP_NEW_OBJ(((Engine::CSUPLPos*)(message))->m_pVelocity,Engine::CHorandveruncert);
				Engine::CHorandveruncert* horandveruncert;
				horandveruncert=(Engine::CHorandveruncert*)((Engine::CSUPLPos*)(message))->m_pVelocity;
				if(!horandveruncert->m_Bearing.m_Data)
				{
					ULP_NEW_VAL(horandveruncert->m_Bearing.m_Data,Engine::uint8_t[2]);
// 					if(!horandveruncert->m_Bearing.m_Data)
// 					{return NO_MEMORY;}
				}
				horandveruncert->m_Bearing.m_Size=2;
				memcpy(horandveruncert->m_Bearing.m_Data,ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.bearing.buf,2);
				if(!horandveruncert->m_HorSpeed.m_Data)
				{
					ULP_NEW_VAL(horandveruncert->m_HorSpeed.m_Data,Engine::uint8_t[2]);
// 					if(!horandveruncert->m_HorSpeed.m_Data)
// 					{return NO_MEMORY;}
				}
				horandveruncert->m_HorSpeed.m_Size=2;
				memcpy(horandveruncert->m_HorSpeed.m_Data,ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.horspeed.buf,2);
				if(!horandveruncert->m_HorUncertSpeed.m_Data)
				{
					ULP_NEW_VAL(horandveruncert->m_HorUncertSpeed.m_Data,Engine::uint8_t);
// 					if(!horandveruncert->m_HorUncertSpeed.m_Data)
// 					{return NO_MEMORY;}
				}
				horandveruncert->m_HorUncertSpeed.m_Size=1;
				memcpy(horandveruncert->m_HorUncertSpeed.m_Data,ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.horuncertspeed.buf,1);
				if(!horandveruncert->m_VerDirect.m_Data)
				{
					ULP_NEW_VAL(horandveruncert->m_VerDirect.m_Data,Engine::uint8_t);
// 					if(!horandveruncert->m_VerDirect.m_Data)
// 					{return NO_MEMORY;}
				}
				horandveruncert->m_VerDirect.m_Size=1;
				memcpy(horandveruncert->m_VerDirect.m_Data,ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.verdirect.buf,1);
				if(!horandveruncert->m_VerSpeed.m_Data)
				{
					ULP_NEW_VAL(horandveruncert->m_VerSpeed.m_Data,Engine::uint8_t);
// 					if(!horandveruncert->m_VerSpeed.m_Data)
// 					{return NO_MEMORY;}
				}
				memcpy(horandveruncert->m_VerSpeed.m_Data,ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.verspeed.buf,1);
				ULP_NEW_VAL(horandveruncert->m_VerUncertSpeed.m_Data,Engine::uint8_t);
// 				if(!horandveruncert->m_VerUncertSpeed.m_Data)
// 				{return NO_MEMORY;}
				horandveruncert->m_VerUncertSpeed.m_Size=1;
				memcpy(horandveruncert->m_VerUncertSpeed.m_Data,ulp.message.choice.msSUPLPOS.velocity->choice.horandveruncert.veruncertspeed.buf,1);
			}
		else if(ulp.message.choice.msSUPLPOS.velocity->present==Velocity_PR_horandvervel)
		{
				ULP_NEW_OBJ(((Engine::CSUPLPos*)(message))->m_pVelocity,Engine::CHorandvervel);
				Engine::CHorandvervel* horandvervel;
				horandvervel=(Engine::CHorandvervel*)((Engine::CSUPLPos*)(message))->m_pVelocity;
				if(!horandvervel->m_Bearing.m_Data)
				{
					ULP_NEW_VAL(horandvervel->m_Bearing.m_Data,Engine::uint8_t[2]);
// 					if(!horandvervel->m_Bearing.m_Data)
// 					{return NO_MEMORY;}
				}
				horandvervel->m_Bearing.m_Size=2;
				memcpy(horandvervel->m_Bearing.m_Data,ulp.message.choice.msSUPLPOS.velocity->choice.horandvervel.bearing.buf,2);
				if(!horandvervel->m_HorSpeed.m_Data)
				{
					ULP_NEW_VAL(horandvervel->m_HorSpeed.m_Data,Engine::uint8_t[2]);
// 					if(!horandvervel->m_HorSpeed.m_Data)
// 					{return NO_MEMORY;}
				}
				horandvervel->m_HorSpeed.m_Size=2;
				memcpy(horandvervel->m_HorSpeed.m_Data,ulp.message.choice.msSUPLPOS.velocity->choice.horandvervel.horspeed.buf,2);
				if(!horandvervel->m_VerDirect.m_Data)
				{
					ULP_NEW_VAL(horandvervel->m_VerDirect.m_Data,Engine::uint8_t);
// 					if(!horandvervel->m_VerDirect.m_Data)
// 					{return NO_MEMORY;}
				}
				horandvervel->m_VerDirect.m_Size=1;
				memcpy(horandvervel->m_VerDirect.m_Data,ulp.message.choice.msSUPLPOS.velocity->choice.horandvervel.verdirect.buf,1);
				if(!horandvervel->m_VerSpeed.m_Data)
				{
					ULP_NEW_VAL(horandvervel->m_VerSpeed.m_Data,Engine::uint8_t);
// 					if(!horandvervel->m_VerSpeed.m_Data)
// 					{return NO_MEMORY;}
				}
				horandvervel->m_VerSpeed.m_Size=1;
				memcpy(horandvervel->m_VerSpeed.m_Data,ulp.message.choice.msSUPLPOS.velocity->choice.horandvervel.verspeed.buf,1);
		}
		else if(ulp.message.choice.msSUPLPOS.velocity->present==Velocity_PR_horvel)
		{
				ULP_NEW_OBJ(((Engine::CSUPLPos*)(message))->m_pVelocity,Engine::CHorvel);
				Engine::CHorvel* horvel;
				horvel=(Engine::CHorvel*)((Engine::CSUPLPos*)(message))->m_pVelocity;
				if(!horvel->m_Bearing.m_Data)
				{
					ULP_NEW_VAL(horvel->m_Bearing.m_Data,Engine::uint8_t[2]);
// 					if(!horvel->m_Bearing.m_Data)
// 					{return NO_MEMORY;}
				}
				horvel->m_Bearing.m_Size=2;
				memcpy(horvel->m_Bearing.m_Data,ulp.message.choice.msSUPLPOS.velocity->choice.horvel.bearing.buf,2);
				if(!horvel->m_HorSpeed.m_Data)
				{
					ULP_NEW_VAL(horvel->m_HorSpeed.m_Data,Engine::uint8_t[2]);
// 					if(!horvel->m_HorSpeed.m_Data)
// 					{return NO_MEMORY;}
				}
				horvel->m_HorSpeed.m_Size=2;
				memcpy(horvel->m_HorSpeed.m_Data,ulp.message.choice.msSUPLPOS.velocity->choice.horvel.horspeed.buf,2);
		}
		else if(ulp.message.choice.msSUPLPOS.velocity->present==Velocity_PR_horveluncert)
		{
				ULP_NEW_OBJ(((Engine::CSUPLPos*)(message))->m_pVelocity,Engine::CHorveluncert);
				Engine::CHorveluncert* horveluncert;
				horveluncert=(Engine::CHorveluncert*)((Engine::CSUPLPos*)(message))->m_pVelocity;
				if(!horveluncert->m_Bearing.m_Data)
				{
					ULP_NEW_VAL(horveluncert->m_Bearing.m_Data,Engine::uint8_t[2]);
// 					if(!horveluncert->m_Bearing.m_Data)
// 					{return NO_MEMORY;}
				}
				horveluncert->m_Bearing.m_Size=2;
				memcpy(horveluncert->m_Bearing.m_Data,ulp.message.choice.msSUPLPOS.velocity->choice.horveluncert.bearing.buf,2);	
				if(!horveluncert->m_HorSpeed.m_Data)
				{
					ULP_NEW_VAL(horveluncert->m_HorSpeed.m_Data,Engine::uint8_t[2]);
// 					if(!horveluncert->m_HorSpeed.m_Data)
// 					{return NO_MEMORY;}
				}
				horveluncert->m_HorSpeed.m_Size=2;
				memcpy(horveluncert->m_HorSpeed.m_Data,ulp.message.choice.msSUPLPOS.velocity->choice.horveluncert.horspeed.buf,2);	
				if(!horveluncert->m_UncertSpeed.m_Data)
				{
					ULP_NEW_VAL(horveluncert->m_UncertSpeed.m_Data,Engine::uint8_t);
// 					if(!horveluncert->m_UncertSpeed.m_Data)
// 					{return NO_MEMORY;}
				}
				horveluncert->m_UncertSpeed.m_Size=1;
				memcpy(horveluncert->m_UncertSpeed.m_Data,ulp.message.choice.msSUPLPOS.velocity->choice.horveluncert.uncertspeed.buf,1);	
		}
	}
	if(ulp.message.choice.msSUPLPOS.posPayLoad.present == PosPayLoad_PR_rrlpPayload)
	{
			Engine::CRRLPPayLoad* rrlp;
			ULP_NEW_OBJ(((Engine::CSUPLPos*)(message))->m_PosPayLoad,Engine::CRRLPPayLoad(ulp.message.choice.msSUPLPOS.posPayLoad.choice.rrlpPayload.buf,ulp.message.choice.msSUPLPOS.posPayLoad.choice.rrlpPayload.size));
// 			if(!((Engine::CSUPLPos*)(message))->m_PosPayLoad)
// 			{return NO_MEMORY;}
			rrlp=(Engine::CRRLPPayLoad*)(((Engine::CSUPLPos*)(message))->m_PosPayLoad);
// 			if(!rrlp->m_RRLPPayLoad.m_Data)
// 			{return NO_MEMORY;}
	}
	else if(ulp.message.choice.msSUPLPOS.posPayLoad.present == PosPayLoad_PR_rrcPayload)
	{	
			Engine::CRRCPayLoad* rrc;
			ULP_NEW_OBJ(((Engine::CSUPLPos*)(message))->m_PosPayLoad,Engine::CRRCPayLoad(ulp.message.choice.msSUPLPOS.posPayLoad.choice.rrcPayload.buf,ulp.message.choice.msSUPLPOS.posPayLoad.choice.rrcPayload.size));
			rrc=(Engine::CRRCPayLoad*)(((Engine::CSUPLPos*)(message))->m_PosPayLoad);
// 			if(!ulp.message.choice.msSUPLPOS.posPayLoad.choice.rrcPayload.buf)
// 			{return NO_MEMORY;}
 			if(rrc)
{			
			rrc->m_RRCPayLoad.m_Size=ulp.message.choice.msSUPLPOS.posPayLoad.choice.rrcPayload.size;
			memcpy(rrc->m_RRCPayLoad.m_Data,ulp.message.choice.msSUPLPOS.posPayLoad.choice.rrcPayload.buf,ulp.message.choice.msSUPLPOS.posPayLoad.choice.rrcPayload.size);
}
	}
	else if(ulp.message.choice.msSUPLPOS.posPayLoad.present == PosPayLoad_PR_tia801payload)
	{
			Engine::CTIA801PayLoad* tia;
			tia=(Engine::CTIA801PayLoad*)(((Engine::CSUPLPos*)(message))->m_PosPayLoad);
			ULP_NEW_VAL(tia->m_Tia801PayLoad.m_Data,Engine::uint8_t[ulp.message.choice.msSUPLPOS.posPayLoad.choice.tia801payload.size]);
// 			if(!tia->m_Tia801PayLoad.m_Data)
// 			{return NO_MEMORY;}
			tia->m_Tia801PayLoad.m_Size=ulp.message.choice.msSUPLPOS.posPayLoad.choice.tia801payload.size;
			memcpy(tia->m_Tia801PayLoad.m_Data,ulp.message.choice.msSUPLPOS.posPayLoad.choice.tia801payload.buf,ulp.message.choice.msSUPLPOS.posPayLoad.choice.tia801payload.size);
	}
	else
	{
		return INVALID_PARAMETER;
	}

	return SUCCESS;
}


SUCCESS_ERROR_T ULP_Processor::Parse_SUPLRESPONSE(Engine::CSUPLMessage* message)
{	
	if(ulp.message.choice.msSUPLRESPONSE.posMethod.buf)
	{
		((Engine::CSUPLResponse*)(message))->m_PosMethod=(Engine::PosMethod)(*(ulp.message.choice.msSUPLRESPONSE.posMethod.buf));
	}

	if(!ulp.message.choice.msSUPLRESPONSE.sLPAddress)
		((Engine::CSUPLResponse*)(message))->m_pSLPAddress=NULL;
	else
	{
		if(ulp.message.choice.msSUPLRESPONSE.sLPAddress->present == SLPAddress_PR_iPAddress)
		{
			Engine::CIPAddress* ip_address;
			ULP_NEW_OBJ(((Engine::CSUPLResponse*)(message))->m_pSLPAddress,Engine::CIPAddress);
// 			if(!((Engine::CSUPLResponse*)(message))->m_pSLPAddress)
// 			{return NO_MEMORY;}
			ip_address=(Engine::CIPAddress*)(((Engine::CSUPLResponse*)(message))->m_pSLPAddress);
			if(ulp.message.choice.msSUPLRESPONSE.sLPAddress->choice.iPAddress.present == IPAddress_PR_ipv4Address)
			{
				Engine::CIPv4* ip_v4;
				ULP_NEW_OBJ(ip_address->iIP_Addr,Engine::CIPv4);
// 				if(!ip_address->iIP_Addr)
// 				{return NO_MEMORY;}
				ip_v4=(Engine::CIPv4*)ip_address->iIP_Addr;
				ip_v4->m_Addr.m_Size=ulp.message.choice.msSUPLRESPONSE.sLPAddress->choice.iPAddress.choice.ipv4Address.size;
				memcpy(ip_v4->m_Addr.m_Data,ulp.message.choice.msSUPLRESPONSE.sLPAddress->choice.iPAddress.choice.ipv4Address.buf,ulp.message.choice.msSUPLRESPONSE.sLPAddress->choice.iPAddress.choice.ipv4Address.size);

			}
			else if(ulp.message.choice.msSUPLRESPONSE.sLPAddress->choice.iPAddress.present == IPAddress_PR_ipv6Address)
			{
				Engine::CIPv6* ip_v6;
				ULP_NEW_OBJ(ip_address->iIP_Addr,Engine::CIPv6);
// 				if(!ip_address->iIP_Addr)
// 				{return NO_MEMORY;}
				ip_v6=(Engine::CIPv6*)ip_address->iIP_Addr;
				ip_v6->m_Addr.m_Size=ulp.message.choice.msSUPLRESPONSE.sLPAddress->choice.iPAddress.choice.ipv6Address.size;
				memcpy(ip_v6->m_Addr.m_Data,ulp.message.choice.msSUPLRESPONSE.sLPAddress->choice.iPAddress.choice.ipv6Address.buf,ulp.message.choice.msSUPLRESPONSE.sLPAddress->choice.iPAddress.choice.ipv6Address.size);
			}
			else
			{return MAN_PARAM_IS_ABSENT;}
		}
		else if(ulp.message.choice.msSUPLRESPONSE.sLPAddress->present == SLPAddress_PR_fQDN)
		{
			Engine::CFQDN* fqdn;
			ULP_NEW_OBJ(((Engine::CSUPLResponse*)(message))->m_pSLPAddress,Engine::CFQDN);
// 			if(!((Engine::CSUPLResponse*)(message))->m_pSLPAddress)
// 			{return NO_MEMORY;}
			fqdn=(Engine::CFQDN*)(((Engine::CSUPLResponse*)(message))->m_pSLPAddress);
			fqdn->m_FQDN.m_Size=ulp.message.choice.msSUPLRESPONSE.sLPAddress->choice.fQDN.size;
			memcpy(fqdn->m_FQDN.m_Data,ulp.message.choice.msSUPLRESPONSE.sLPAddress->choice.fQDN.buf,ulp.message.choice.msSUPLRESPONSE.sLPAddress->choice.fQDN.size);
		}
		else 
		{
			return MAN_PARAM_IS_ABSENT;
		}
	}
	if(!ulp.message.choice.msSUPLRESPONSE.keyIdentity4)
		((Engine::CSUPLResponse*)(message))->m_pKeyIdentity4=NULL;
	else
	{
		ULP_NEW_OBJ(((Engine::CSUPLResponse*)(message))->m_pKeyIdentity4,Engine::CBitString(128));
// 		if(!((Engine::CSUPLResponse*)(message))->m_pKeyIdentity4)
// 		{return NO_MEMORY;}
		if(!((Engine::CSUPLResponse*)(message))->m_pKeyIdentity4->m_Data)
		{
			ULP_NEW_VAL(((Engine::CSUPLResponse*)(message))->m_pKeyIdentity4->m_Data,Engine::uint8_t[ulp.message.choice.msSUPLRESPONSE.keyIdentity4->size]);
// 			if(!((Engine::CSUPLResponse*)(message))->m_pKeyIdentity4->m_Data)
// 			{return NO_MEMORY;}
		}
		((Engine::CSUPLResponse*)(message))->m_pKeyIdentity4->m_Size=ulp.message.choice.msSUPLRESPONSE.keyIdentity4->size;
		memcpy(((Engine::CSUPLResponse*)(message))->m_pKeyIdentity4->m_Data,ulp.message.choice.msSUPLRESPONSE.keyIdentity4->buf,ulp.message.choice.msSUPLRESPONSE.keyIdentity4->size);
	}
	if(!ulp.message.choice.msSUPLRESPONSE.sETAuthKey)
		((Engine::CSUPLResponse*)(message))->m_pSETAuthKey=NULL;
	else
	{
		if(ulp.message.choice.msSUPLRESPONSE.sETAuthKey->present == SETAuthKey_PR_shortKey)
		{
			ULP_NEW_OBJ(((Engine::CSUPLResponse*)(message))->m_pSETAuthKey,Engine::CSHORTAuthKey);
			Engine::CSHORTAuthKey* short_key;
			short_key=(Engine::CSHORTAuthKey*)(((Engine::CSUPLResponse*)(message))->m_pSETAuthKey);
			if(!short_key->iKey.m_Data)
			{
				ULP_NEW_VAL(short_key->iKey.m_Data,Engine::uint8_t[ulp.message.choice.msSUPLRESPONSE.sETAuthKey->choice.shortKey.size]);
// 				if(!short_key->iKey.m_Data)
// 				{return NO_MEMORY;}
			}
			short_key->iKey.m_Size=ulp.message.choice.msSUPLRESPONSE.sETAuthKey->choice.shortKey.size;
			memcpy(short_key->iKey.m_Data,ulp.message.choice.msSUPLRESPONSE.sETAuthKey->choice.shortKey.buf,ulp.message.choice.msSUPLRESPONSE.sETAuthKey->choice.shortKey.size);
		}
		else if(ulp.message.choice.msSUPLRESPONSE.sETAuthKey->present == SETAuthKey_PR_longKey)
		{
			ULP_NEW_OBJ(((Engine::CSUPLResponse*)(message))->m_pSETAuthKey,Engine::CSHORTAuthKey);
			Engine::CSHORTAuthKey* long_key;
			long_key=(Engine::CSHORTAuthKey*)(((Engine::CSUPLResponse*)(message))->m_pSETAuthKey);
			if(!long_key->iKey.m_Data)
			{
				ULP_NEW_VAL(long_key->iKey.m_Data,Engine::uint8_t[ulp.message.choice.msSUPLRESPONSE.sETAuthKey->choice.longKey.size]);
// 				if(!long_key->iKey.m_Data)
// 				{return NO_MEMORY;}
			}
			long_key->iKey.m_Size=ulp.message.choice.msSUPLRESPONSE.sETAuthKey->choice.longKey.size;
			memcpy(long_key->iKey.m_Data,ulp.message.choice.msSUPLRESPONSE.sETAuthKey->choice.longKey.buf,ulp.message.choice.msSUPLRESPONSE.sETAuthKey->choice.longKey.size);
		}
		else
		{
			return MAN_PARAM_IS_ABSENT;
		}
	}
	return SUCCESS;
}


void ULP_Processor::Clear_SUPLRESPONSE()
{	
	if(ulp.message.choice.msSUPLRESPONSE.posMethod.buf)
	{
		FREEMEM(ulp.message.choice.msSUPLRESPONSE.posMethod.buf);
	}
	if(ulp.message.choice.msSUPLRESPONSE.sLPAddress)
	{
		if(ulp.message.choice.msSUPLRESPONSE.sLPAddress->present == SLPAddress_PR_iPAddress)
		{
			if(ulp.message.choice.msSUPLRESPONSE.sLPAddress->choice.iPAddress.choice.ipv4Address.buf)
			{
				FREEMEM(ulp.message.choice.msSUPLRESPONSE.sLPAddress->choice.iPAddress.choice.ipv4Address.buf);
			}
			if(ulp.message.choice.msSUPLRESPONSE.sLPAddress->choice.iPAddress.choice.ipv6Address.buf)
			{
				FREEMEM(ulp.message.choice.msSUPLRESPONSE.sLPAddress->choice.iPAddress.choice.ipv6Address.buf);
			}		
		}
		else if(ulp.message.choice.msSUPLRESPONSE.sLPAddress->present == SLPAddress_PR_fQDN)
		{
			FREEMEM(ulp.message.choice.msSUPLRESPONSE.sLPAddress->choice.fQDN.buf);			
		}
		FREEMEM(ulp.message.choice.msSUPLRESPONSE.sLPAddress);
	}
	if(ulp.message.choice.msSUPLRESPONSE.keyIdentity4)
	{
		FREEMEM(ulp.message.choice.msSUPLRESPONSE.keyIdentity4);	
	}
	if(ulp.message.choice.msSUPLRESPONSE.sETAuthKey)
	{
		if(ulp.message.choice.msSUPLRESPONSE.sETAuthKey->present == SETAuthKey_PR_shortKey)
		{
			FREEMEM(ulp.message.choice.msSUPLRESPONSE.sETAuthKey->choice.shortKey.buf);
		}
		if(ulp.message.choice.msSUPLRESPONSE.sETAuthKey->present == SETAuthKey_PR_longKey)
		{
			FREEMEM(ulp.message.choice.msSUPLRESPONSE.sETAuthKey->choice.longKey.buf);			
		}
		FREEMEM(ulp.message.choice.msSUPLRESPONSE.sETAuthKey);
	}
}

SUCCESS_ERROR_T ULP_Processor::Parse_SUPLINIT(Engine::CSUPLMessage* message)
{	
	if(!ulp.message.choice.msSUPLINIT.posMethod.buf)
	{return MAN_PARAM_IS_ABSENT;}
	if(!ulp.message.choice.msSUPLINIT.sLPMode.buf)
	{return MAN_PARAM_IS_ABSENT;}
	((Engine::CSUPLInit*)(message))->m_PosMethod=(Engine::PosMethod)(*(ulp.message.choice.msSUPLINIT.posMethod.buf));
	((Engine::CSUPLInit*)(message))->m_SLPMode=(Engine::SLPMode)(*ulp.message.choice.msSUPLINIT.sLPMode.buf);
	if(ulp.message.choice.msSUPLINIT.notification)
	{
		ULP_NEW_VAL(((Engine::CSUPLInit*)(message))->m_pNotification,Engine::Notification);
// 		if(!((Engine::CSUPLInit*)(message))->m_pNotification)
// 			return NO_MEMORY;
		((Engine::CSUPLInit*)(message))->m_pNotification->notificationType = (Engine::NotificationType) (*ulp.message.choice.msSUPLINIT.notification->notificationType.buf);
		if(ulp.message.choice.msSUPLINIT.notification->clientName)
		{
			ULP_NEW_OBJ(((Engine::CSUPLInit*)(message))->m_pNotification->clientName,Engine::COctet(ulp.message.choice.msSUPLINIT.notification->clientName->size));
// 			if(!((Engine::CSUPLInit*)(message))->m_pNotification->clientName)
// 			{return NO_MEMORY;}
			memcpy(((Engine::CSUPLInit*)(message))->m_pNotification->clientName->m_Data,ulp.message.choice.msSUPLINIT.notification->clientName->buf,ulp.message.choice.msSUPLINIT.notification->clientName->size);
		}
		if(ulp.message.choice.msSUPLINIT.notification->clientNameType)
		{
			ULP_NEW_VAL(((Engine::CSUPLInit*)(message))->m_pNotification->clientNameType,Engine::FormatIndicator);
// 			if(!((Engine::CSUPLInit*)(message))->m_pNotification->clientNameType)
// 			{return NO_MEMORY;}
			*(((Engine::CSUPLInit*)(message))->m_pNotification->clientNameType) = (Engine::FormatIndicator)(*ulp.message.choice.msSUPLINIT.notification->clientNameType->buf);
		}
		if(ulp.message.choice.msSUPLINIT.notification->encodingType)
		{
			ULP_NEW_VAL(((Engine::CSUPLInit*)(message))->m_pNotification->encodingType,Engine::EncodingType);
// 			if(!((Engine::CSUPLInit*)(message))->m_pNotification->encodingType)
// 			{return NO_MEMORY;}
			*(((Engine::CSUPLInit*)(message))->m_pNotification->encodingType) = (Engine::EncodingType)(*ulp.message.choice.msSUPLINIT.notification->encodingType->buf);
		}
		if(ulp.message.choice.msSUPLINIT.notification->requestorId)
		{
			ULP_NEW_OBJ(((Engine::CSUPLInit*)(message))->m_pNotification->requestorId,Engine::COctet(ulp.message.choice.msSUPLINIT.notification->requestorId->size));
// 			if(!((Engine::CSUPLInit*)(message))->m_pNotification->requestorId)
// 			{return NO_MEMORY;}
			memcpy(((Engine::CSUPLInit*)(message))->m_pNotification->requestorId->m_Data,ulp.message.choice.msSUPLINIT.notification->requestorId->buf,ulp.message.choice.msSUPLINIT.notification->requestorId->size);
		}
		if(ulp.message.choice.msSUPLINIT.notification->requestorIdType)
		{
			ULP_NEW_VAL(((Engine::CSUPLInit*)(message))->m_pNotification->requestorIdType,Engine::FormatIndicator);
// 			if(!((Engine::CSUPLInit*)(message))->m_pNotification->requestorIdType)
// 			{return NO_MEMORY;}
			*(((Engine::CSUPLInit*)(message))->m_pNotification->requestorIdType) = (Engine::FormatIndicator)(*ulp.message.choice.msSUPLINIT.notification->requestorIdType->buf);
		}
	}
	else
	{
		((Engine::CSUPLInit*)(message))->m_pNotification=NULL;
	}
	if(!ulp.message.choice.msSUPLINIT.sLPAddress)
		((Engine::CSUPLInit*)(message))->m_pSLPAddress=NULL;
	else
	{
		if(ulp.message.choice.msSUPLINIT.sLPAddress->present == SLPAddress_PR_iPAddress)
		{
			Engine::CIPAddress* ip_address;
			ULP_NEW_OBJ(((Engine::CSUPLInit*)(message))->m_pSLPAddress,Engine::CIPAddress);
// 			if(!((Engine::CSUPLInit*)(message))->m_pSLPAddress)
// 			{return NO_MEMORY;}
			ip_address=(Engine::CIPAddress*)(((Engine::CSUPLInit*)(message))->m_pSLPAddress);
			if(ulp.message.choice.msSUPLINIT.sLPAddress->choice.iPAddress.present == IPAddress_PR_ipv4Address)
			{
				Engine::CIPv4* ip_v4;
				ULP_NEW_OBJ(ip_address->iIP_Addr,Engine::CIPv4);
// 				if(!ip_address->iIP_Addr)
// 				{return NO_MEMORY;}
				ip_v4=(Engine::CIPv4*)ip_address->iIP_Addr;
				ip_v4->m_Addr.m_Size=ulp.message.choice.msSUPLINIT.sLPAddress->choice.iPAddress.choice.ipv4Address.size;
				memcpy(ip_v4->m_Addr.m_Data,ulp.message.choice.msSUPLINIT.sLPAddress->choice.iPAddress.choice.ipv4Address.buf,ulp.message.choice.msSUPLINIT.sLPAddress->choice.iPAddress.choice.ipv4Address.size);

			}
			else if(ulp.message.choice.msSUPLINIT.sLPAddress->choice.iPAddress.present == IPAddress_PR_ipv6Address)
			{
				Engine::CIPv6* ip_v6;
				ULP_NEW_OBJ(ip_address->iIP_Addr,Engine::CIPv6);
// 				if(!ip_address->iIP_Addr)
// 				{return NO_MEMORY;}
				ip_v6=(Engine::CIPv6*)ip_address->iIP_Addr;
				ip_v6->m_Addr.m_Size=ulp.message.choice.msSUPLINIT.sLPAddress->choice.iPAddress.choice.ipv6Address.size;
				memcpy(ip_v6->m_Addr.m_Data,ulp.message.choice.msSUPLINIT.sLPAddress->choice.iPAddress.choice.ipv6Address.buf,ulp.message.choice.msSUPLINIT.sLPAddress->choice.iPAddress.choice.ipv6Address.size);
			}
			else
			{return MAN_PARAM_IS_ABSENT;}
		}
		else if(ulp.message.choice.msSUPLINIT.sLPAddress->present == SLPAddress_PR_fQDN)
		{
			Engine::CFQDN* fqdn;
			ULP_NEW_OBJ(((Engine::CSUPLInit*)(message))->m_pSLPAddress,Engine::CFQDN);
// 			if(!((Engine::CSUPLInit*)(message))->m_pSLPAddress)
// 			{return NO_MEMORY;}
			fqdn=(Engine::CFQDN*)(((Engine::CSUPLInit*)(message))->m_pSLPAddress);
			fqdn->m_FQDN.m_Size=ulp.message.choice.msSUPLINIT.sLPAddress->choice.fQDN.size;
			memcpy(fqdn->m_FQDN.m_Data,ulp.message.choice.msSUPLINIT.sLPAddress->choice.fQDN.buf,ulp.message.choice.msSUPLINIT.sLPAddress->choice.fQDN.size);
		}
		else 
		{
			return MAN_PARAM_IS_ABSENT;
		}
	}
	if(!ulp.message.choice.msSUPLINIT.keyIdentity)
		((Engine::CSUPLInit*)(message))->m_pKeyIdentity=NULL;
	else
	{
		ULP_NEW_OBJ(((Engine::CSUPLInit*)(message))->m_pKeyIdentity,Engine::CBitString(128));
// 		if(!((Engine::CSUPLInit*)(message))->m_pKeyIdentity)
// 		{return NO_MEMORY;}
		if (!((Engine::CSUPLInit*)(message))->m_pKeyIdentity->m_Data)
		{
			ULP_NEW_VAL(((Engine::CSUPLInit*)(message))->m_pKeyIdentity->m_Data,Engine::uint8_t[((Engine::CSUPLInit*)(message))->m_pKeyIdentity->m_Size]);
// 			if(!((Engine::CSUPLInit*)(message))->m_pKeyIdentity->m_Data)
// 			{return NO_MEMORY;}
		}
		memcpy(((Engine::CSUPLInit*)(message))->m_pKeyIdentity->m_Data,ulp.message.choice.msSUPLINIT.keyIdentity->buf,ulp.message.choice.msSUPLINIT.keyIdentity->size);
	}
	if(!ulp.message.choice.msSUPLINIT.mAC)
		((Engine::CSUPLInit*)(message))->m_pMAC=NULL;
	else
	{
		ULP_NEW_OBJ(((Engine::CSUPLInit*)(message))->m_pMAC,Engine::CBitString(64));
// 		if(((Engine::CSUPLInit*)(message))->m_pMAC)
// 		{return NO_MEMORY;}
		if (!((Engine::CSUPLInit*)(message))->m_pMAC->m_Data)
		{
			ULP_NEW_VAL(((Engine::CSUPLInit*)(message))->m_pMAC->m_Data,Engine::uint8_t[((Engine::CSUPLInit*)(message))->m_pMAC->m_Size]);
// 			if(((Engine::CSUPLInit*)(message))->m_pMAC->m_Data)
// 			{return NO_MEMORY;}
		}
		memcpy(((Engine::CSUPLInit*)(message))->m_pMAC->m_Data,ulp.message.choice.msSUPLINIT.mAC->buf,ulp.message.choice.msSUPLINIT.mAC->size);
	}
	if(!ulp.message.choice.msSUPLINIT.qoP)
		((Engine::CSUPLInit*)(message))->m_pQoP=NULL;
	else
	{
		ULP_NEW_OBJ(((Engine::CSUPLInit*)(message))->m_pQoP,Engine::QoP);
// 		if(!((Engine::CSUPLInit*)(message))->m_pQoP)
// 		{return NO_MEMORY;}
		((Engine::CSUPLInit*)(message))->m_pQoP->m_Horacc=(Engine::uint8_t)ulp.message.choice.msSUPLINIT.qoP->horacc;
		if(!ulp.message.choice.msSUPLINIT.qoP->veracc)
			((Engine::CSUPLInit*)(message))->m_pQoP->m_pVeracc=NULL;
		else
		{
			ULP_NEW_VAL(((Engine::CSUPLInit*)(message))->m_pQoP->m_pVeracc,Engine::uint8_t);
// 			if(!((Engine::CSUPLInit*)(message))->m_pQoP->m_pVeracc)
// 			{return NO_MEMORY;}
			*(((Engine::CSUPLInit*)(message))->m_pQoP->m_pVeracc)=(Engine::uint8_t)*(ulp.message.choice.msSUPLINIT.qoP->veracc);
		}
		if(!ulp.message.choice.msSUPLINIT.qoP->maxLocAge)
			((Engine::CSUPLInit*)(message))->m_pQoP->m_pMaxLocAge=NULL;
		else
		{
			ULP_NEW_VAL(((Engine::CSUPLInit*)(message))->m_pQoP->m_pMaxLocAge,uint32_t);
// 			if(!((Engine::CSUPLInit*)(message))->m_pQoP->m_pMaxLocAge)
// 			{return NO_MEMORY;}
			*(((Engine::CSUPLInit*)(message))->m_pQoP->m_pMaxLocAge)=*(ulp.message.choice.msSUPLINIT.qoP->maxLocAge);
		}
		if(!ulp.message.choice.msSUPLINIT.qoP->delay)
			((Engine::CSUPLInit*)(message))->m_pQoP->m_pDelay=NULL;
		else
		{
			ULP_NEW_VAL(((Engine::CSUPLInit*)(message))->m_pQoP->m_pDelay,Engine::uint8_t);
// 			if(!((Engine::CSUPLInit*)(message))->m_pQoP->m_pDelay)
// 			{return NO_MEMORY;}
			*(((Engine::CSUPLInit*)(message))->m_pQoP->m_pDelay)=(Engine::uint8_t)*(ulp.message.choice.msSUPLINIT.qoP->delay);
		}
	}

	return SUCCESS;
}


void ULP_Processor::Clear_SUPLINIT()
{
	ulp.message.choice.msSUPLINIT.notification=NULL;
	if(ulp.message.choice.msSUPLINIT.posMethod.buf)
	{
		FREEMEM(ulp.message.choice.msSUPLINIT.posMethod.buf);
	}
	if(ulp.message.choice.msSUPLINIT.notification)
	{	
		if(ulp.message.choice.msSUPLINIT.notification->clientName)
		{
			FREEMEM(ulp.message.choice.msSUPLINIT.notification->clientName->buf);
			FREEMEM(ulp.message.choice.msSUPLINIT.notification->clientName);
		}
		if(ulp.message.choice.msSUPLINIT.notification->clientNameType)
		{
			FREEMEM(ulp.message.choice.msSUPLINIT.notification->clientNameType);
		}
		if(ulp.message.choice.msSUPLINIT.notification->encodingType)
		{
			FREEMEM(ulp.message.choice.msSUPLINIT.notification->encodingType);
		}
		if(ulp.message.choice.msSUPLINIT.notification->requestorId)
		{
			FREEMEM(ulp.message.choice.msSUPLINIT.notification->requestorId->buf);
			FREEMEM(ulp.message.choice.msSUPLINIT.notification->requestorId);
		}
		if(ulp.message.choice.msSUPLINIT.notification->requestorIdType)
		{
			FREEMEM(ulp.message.choice.msSUPLINIT.notification->requestorIdType);
		}
		FREEMEM(ulp.message.choice.msSUPLINIT.notification);
	}
	if(ulp.message.choice.msSUPLINIT.sLPAddress)
	{
		if(ulp.message.choice.msSUPLINIT.sLPAddress->present == SLPAddress_PR_iPAddress)
		{
			if(ulp.message.choice.msSUPLINIT.sLPAddress->choice.iPAddress.present == IPAddress_PR_ipv4Address)
			{
				if(ulp.message.choice.msSUPLINIT.sLPAddress->choice.iPAddress.choice.ipv4Address.buf)
					FREEMEM(ulp.message.choice.msSUPLINIT.sLPAddress->choice.iPAddress.choice.ipv4Address.buf);

			}
			else if(ulp.message.choice.msSUPLINIT.sLPAddress->choice.iPAddress.present == IPAddress_PR_ipv6Address)
			{
				if(ulp.message.choice.msSUPLINIT.sLPAddress->choice.iPAddress.choice.ipv6Address.buf)
					FREEMEM(ulp.message.choice.msSUPLINIT.sLPAddress->choice.iPAddress.choice.ipv6Address.buf);				
			}
		}
		else if(ulp.message.choice.msSUPLINIT.sLPAddress->present == SLPAddress_PR_fQDN)
		{
			if(ulp.message.choice.msSUPLINIT.sLPAddress->choice.fQDN.buf)
				FREEMEM(ulp.message.choice.msSUPLINIT.sLPAddress->choice.fQDN.buf);			
		}
		FREEMEM(ulp.message.choice.msSUPLINIT.sLPAddress);
	}
	if(ulp.message.choice.msSUPLINIT.keyIdentity)
	{
		FREEMEM(ulp.message.choice.msSUPLINIT.keyIdentity);
	}
	if(ulp.message.choice.msSUPLINIT.mAC)
	{
		FREEMEM(ulp.message.choice.msSUPLINIT.mAC);		
	}
	if(ulp.message.choice.msSUPLINIT.sLPMode.buf)
	{
		FREEMEM(ulp.message.choice.msSUPLINIT.sLPMode.buf);		
	}
	if(ulp.message.choice.msSUPLINIT.qoP)
	{
		if(ulp.message.choice.msSUPLINIT.qoP->veracc)	
		{
			FREEMEM(ulp.message.choice.msSUPLINIT.qoP->veracc);
		}
		if(ulp.message.choice.msSUPLINIT.qoP->maxLocAge)
		{
			FREEMEM(ulp.message.choice.msSUPLINIT.qoP->maxLocAge);
		}
		if(ulp.message.choice.msSUPLINIT.qoP->delay)
		{
			FREEMEM(ulp.message.choice.msSUPLINIT.qoP->delay);
		}
		FREEMEM(ulp.message.choice.msSUPLINIT.qoP);
	}
}
