/**
 *  @file  SUPLPosInitAlgorithm.h
 *  @brief CSUPLPosInitAlgorithm  declaration.
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
Alexander V. Morozov         20.11.2007	                 initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#include "algorithm/SUPLPosAlgorithm.h"
#include <new>

#include "nav_log_msg.h"
#include "nav_log_cust_codes.h"
namespace Engine {

CSUPLPosAlgorithm::CSUPLPosAlgorithm()
{

}

CSUPLPosAlgorithm::~CSUPLPosAlgorithm()
{

}

bool_t CSUPLPosAlgorithm::Response(MSG* msg, CSession* session)
{

	// we can enter here only 
	
	if (msg->m_pIncomingMessage->GetType() != SUPL_POS)
	{
		if(!CreateNewResponse(this, msg)->Response(msg, session))
			return FALSE;
		
		return TRUE;
	}
	SESSLOGMSG("[%s]%s:%d, %s \n",get_utc_time(),SESS_OTA_MSG_TYPE,nav_sess_getCount(0),"SUPL POS");
		
	CSUPLPos*		pos = 0;
	CSUPLPos*		new_pos = 0;
	
	if(msg->m_GPSData.m_pdu == NULL)
	{
		CommonError ret = CheckPacket(msg,session);
		if(ret != RETURN_OK)
		{
			if(ret == RETURN_ERROR)
			{
				if(!CreateNewResponse(this, msg)->Response(msg, session))
					return FALSE;
			
				return TRUE;
			}
			else
			{
				return FALSE;
			}
		}
	}

	pos = (CSUPLPos*) msg->m_pIncomingMessage;

	// get RRLP packet
	switch (pos->m_PosPayLoad->GetType())
	{
		
		case RRLP_PAY_LOAD:
		{
			CRRLPPayLoad* rrlp = (CRRLPPayLoad*) (pos->m_PosPayLoad);			
			
			// if this is a new message with rrpl data
			if (msg->m_GPSData.m_pdu == NULL)			
			{
				//send to gps rrlp packet															
				msg->m_GPSData.m_pdu = rrlp->m_RRLPPayLoad.m_Data;
				msg->m_GPSData.m_pdu_size = rrlp->m_RRLPPayLoad.m_Size;
				rrlp->m_RRLPPayLoad.m_Data = NULL; // Receiver of packet is responsible for deleting it
				rrlp->m_RRLPPayLoad.m_Size = 0;
				if(pos->m_pVelocity) //Check for optional parameter
				{
					session->m_pVelocity=pos->m_pVelocity;
					pos->m_pVelocity = NULL;
				}
		
			}
			// else we need create SUPL_POS to server
			else
			{
				// check the data has arrived to message from gps
				new_pos = new(std::nothrow)  CSUPLPos();
				if(!new_pos)
					return FALSE;
				msg->m_pOutgoingMessage = new_pos;	
				// create new rrlp packet
				rrlp = new(std::nothrow)  CRRLPPayLoad(msg->m_GPSData.m_pdu, msg->m_GPSData.m_pdu_size);
				delete[] msg->m_GPSData.m_pdu;
				msg->m_GPSData.m_pdu = NULL;
				msg->m_GPSData.m_pdu_size = 0;
				if(!rrlp)
					return FALSE;
				if(!rrlp->IsObjectReady())
					return FALSE;

				new_pos->m_PosPayLoad = rrlp;
				rrlp = NULL;
					
				//create common part of message
				new_pos->m_Lenght = 0;
				if(new_pos->m_SessionID.Copy(session->iSessionID) != RETURN_OK)
					return FALSE;
				new_pos->m_Version.m_Major = MAJ_VERSION;
				new_pos->m_Version.m_Minor = MIN_VERSION;
				new_pos->m_Version.m_Servind = SER_VERSION;
				// fill message specific part
				new_pos->m_pVelocity = session->m_pVelocity;
				session->m_pVelocity = NULL;
			}
			break;
		}
	case RRC_PAY_LOAD:
	{
			CRRCPayLoad* rrc = (CRRCPayLoad*) (pos->m_PosPayLoad);		
			
			// if this is a new message with rrpl data
			if (msg->m_GPSData.m_pdu == NULL)			
			{
				msg->m_GPSData.m_pdu = rrc->m_RRCPayLoad.m_Data;
				msg->m_GPSData.m_pdu_size = rrc->m_RRCPayLoad.m_Size;
				rrc->m_RRCPayLoad.m_Data = NULL; // Receiver of packet is responsible for deleting it
				rrc->m_RRCPayLoad.m_Size = 0;
				if(pos->m_pVelocity) //Check for optional parameter
				{
					session->m_pVelocity=pos->m_pVelocity;
					pos->m_pVelocity = NULL;
				}
		
			}
			// else we need create SUPL_POS to server
			else
			{
				new_pos = new(std::nothrow)  CSUPLPos();
				if(!new_pos)
					return FALSE;
				msg->m_pOutgoingMessage = new_pos;	
				rrc = new(std::nothrow)  CRRCPayLoad(msg->m_GPSData.m_pdu, msg->m_GPSData.m_pdu_size);
				delete[] msg->m_GPSData.m_pdu;
				msg->m_GPSData.m_pdu = NULL;
				msg->m_GPSData.m_pdu_size = 0;
				if(!rrc)
					return FALSE;
				if(!rrc->IsObjectReady())
					return FALSE;

				new_pos->m_PosPayLoad = rrc;
				rrc = NULL;
					
				//create common part of message
				new_pos->m_Lenght = 0;
				if(new_pos->m_SessionID.Copy(session->iSessionID) != RETURN_OK)
					return FALSE;
				new_pos->m_Version.m_Major = MAJ_VERSION;
				new_pos->m_Version.m_Minor = MIN_VERSION;
				new_pos->m_Version.m_Servind = SER_VERSION;
				// fill message specific part
				new_pos->m_pVelocity = session->m_pVelocity;
				session->m_pVelocity = NULL;
			}
			break;
		}
	}
	return TRUE;
}

CommonError CSUPLPosAlgorithm::CheckPacket(MSG* msg,CSession* session)
{
	bool_t		   ret;
	CSUPLPos*	   supl_pos = (CSUPLPos*) msg->m_pIncomingMessage;
	CSUPLEnd*	   supl_end = NULL;	
	StatusCode	   status_code = (StatusCode)0;

	switch(supl_pos->m_PosPayLoad->GetType())
	{
		case RRLP_PAY_LOAD:{
							session->m_PosProtocol.m_RRLP = 1;		
							ret =  session->m_PosProtocol.m_RRLP; 
							session->m_PosProtocol.m_RRC = 0; 
							session->m_PosProtocol.m_TIA801 = 0; 
							} 
							break;
		case RRC_PAY_LOAD:{	
							session->m_PosProtocol.m_RRC = 1;	
							ret = session->m_PosProtocol.m_RRC; 
							session->m_PosProtocol.m_RRLP= 0; 
							session->m_PosProtocol.m_TIA801 = 0;
							}
							break;
		case TI801_PAY_LOAD:{
							 session->m_PosProtocol.m_TIA801 = 1;
							 ret = session->m_PosProtocol.m_TIA801; 
							 session->m_PosProtocol.m_RRLP= 0;
							 session->m_PosProtocol.m_RRC = 0;
							}
							 break;
		default: ret = FALSE; 
			     break;
	}
	
	if(!ret)
	{
		status_code = Engine::POS_PROTOCOL_MISMATCH;
	}
	if(!(session->iSessionID.IsEqual(supl_pos->m_SessionID)))
	{
		status_code = Engine::INVALID_SESSION_ID;
	}

	if(status_code != 0)
	{
		// create SUPL_END
		supl_end = new(std::nothrow) CSUPLEnd();
		if(!supl_end)
			return RETURN_FATAL;
		msg->m_pOutgoingMessage = supl_end;
		// set session id
		if(supl_end->m_SessionID.Copy(supl_pos->m_SessionID) != RETURN_OK)
			return RETURN_FATAL;
		// set protocol version.
		supl_end->m_Version.m_Major = MAJ_VERSION;
		supl_end->m_Version.m_Minor = MIN_VERSION;
		supl_end->m_Version.m_Servind = SER_VERSION;
		supl_end->m_pStatusCode = new(std::nothrow) StatusCode;
		if(!supl_end->m_pStatusCode)
			return RETURN_FATAL;
		if(session->m_pVer)
		{
			supl_end->m_pVer = new(std::nothrow)  Engine::CBitString(64);
			if(!supl_end->m_pVer)
				return RETURN_FATAL;
			if(!supl_end->m_pVer->IsObjectReady())
				return RETURN_FATAL;
			if(supl_end->m_pVer->Copy(*(session->m_pVer)) != RETURN_OK)
				return RETURN_FATAL;
		}
		*supl_end->m_pStatusCode = status_code;
		return RETURN_ERROR;
	}

	return RETURN_OK;
}

CAlgorithmBase* CSUPLPosAlgorithm::CreateNewResponse(CAlgorithmBase* old, MSG* msg)
{
	return new (old) CSUPLEndAlgorithm();
}

} // end of namespace Engine
