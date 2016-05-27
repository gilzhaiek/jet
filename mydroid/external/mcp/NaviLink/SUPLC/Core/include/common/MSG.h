/**
*  @file  SUPLPos.h
*  @brief CSUPLPos declaration.
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
Alexander V. Morozov         06.11.2007	                 initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#ifndef _MSG_H_
#define _MSG_H_

#include "types.h"
#include "messages/suplmessage.h"

namespace Engine {

enum MSGStatus
{		
	// message is ready to encode
	MSG_READY_TO_ENCODE,
	// message is ready to decode
	MSG_READY_TO_DECODE,
	// message is ready to process
	MSG_READY_TO_PROCESS,
	// message is ready to send
	MSG_READY_TO_SEND,
	// message is ready to die
	MSG_READY_TO_DIE,
	// send NI request to GPS
	MSG_SEND_TO_GPS_NI_REQUEST,
	// wait from reply from gps in nw initiated case
	MSG_WAIT_REPLY_FROM_GPS,
	// send to gps notification rules if exist
	MSG_SEND_NOTIFY_CMD,
	// send data to GPS
	MSG_SEND_TO_GPS_PAYLOAD,
	// wait for gps data (rrlp packet or error)
	MSG_WAIT_FROM_GPS_PAYLOAD,
	// wait confirmation from user
	MSG_WAIT_REPLY_FROM_USER,
	MSG_POSITIONING_GRANTED,
	MSG_POSITIONING_DENIED,
	// message Waiting for Connection
	MSG_WAIT_FOR_SERVER_CONNECTION,
	// fatal error during processing MSG
	MSG_RETRY_SERVER_CONNECTION,
	MSG_FATAL_ERROR
};

struct MSG
{
	uint32_t			m_SETSessionID;
	// incoming raw buffer
	uint8_t*			m_pIncomingRawData;
	uint16_t			m_IncomingRawDataSize;
	// outgoing raw buffer
	uint8_t*			m_pOutgoingRawData;
	uint16_t			m_OutgoingRawDataSize;
	// incoming message FOR SUPL SYSTEM(SUPLController)
	CSUPLMessage*		m_pIncomingMessage;
	// outgoing message to SUPL server
	CSUPLMessage*		m_pOutgoingMessage;
	// message status
	MSGStatus			m_Status;

	/*Structure for storing rrlp/rrc data*/
	struct SGPSData
	{
		uint8_t			   *m_pdu;
		uint32_t			m_pdu_size;
	} m_GPSData;

	MSG();
	~MSG();
};

}

#endif	// _MSG_H_
