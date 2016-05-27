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
Alexander V. Morozov         20.11.2007                  initial version
====================================================================================================
Portability:  MSVC compiler
====================================================================================================
*/

#include <new>
#include "algorithm/SUPLPosInitAlgorithm.h"


#include "nav_log_msg.h"
#include "nav_log_cust_codes.h"

namespace Engine {

CSUPLPosInitAlgorithm::CSUPLPosInitAlgorithm()
{

}

CSUPLPosInitAlgorithm::~CSUPLPosInitAlgorithm()
{

}

/************************************************************************/
/* @breif The purpose of this algorithm is to generate response on      */
/*        init or response message                              */
/************************************************************************/

bool_t CSUPLPosInitAlgorithm::Response(MSG* msg, CSession* session)
{

    if ((msg->m_pIncomingMessage->GetType() != SUPL_RESPONSE) &&
        (msg->m_pIncomingMessage->GetType() != SUPL_INIT))
    {
        // we sent supl_pos_init and receive supl pos probably.
        if(!CreateNewResponse(this, msg)->Response(msg, session))
            return FALSE;

        return TRUE;
    }

    if(session->CurrentState == SUPL_POS_INIT)  //For UNEXPECTED MESSAGE code
    {
        if(!CreateNewResponse(this, msg)->Response(msg, session))
            return FALSE;

        return TRUE;
    }
    session->CurrentState = SUPL_POS_INIT;
    /*
        Create response on SUPL response or SUPL init
    */
    CSUPLPosInit* pos_init = NULL;

    // create SUPL POS INIT response
    pos_init = new(std::nothrow)  CSUPLPosInit();
    if(!pos_init)
        return FALSE;
    msg->m_pOutgoingMessage = pos_init;
    // fill common part
    pos_init->m_Lenght = 0;
    if(pos_init->m_SessionID.Copy(session->iSessionID) != RETURN_OK)
        return FALSE;
    SESSLOGMSG("[%s]%s:%d, %s %s \n",get_utc_time(),SESS_OTA_MSG_TYPE,nav_sess_getCount(0),"SUPL POSINIT"," ");
    pos_init->m_SessionID.iSETSessionID->sessionId = session->GetSessionUID();
    pos_init->m_Version.m_Major = MAJ_VERSION;
    pos_init->m_Version.m_Minor = MIN_VERSION;
    pos_init->m_Version.m_Servind = SER_VERSION;
    // set message specific data;
    pos_init->m_SETCapabilities.m_PosProtocol = session->m_PosProtocol;
    pos_init->m_SETCapabilities.m_PosTechnology = session->iPosTechnology;
    pos_init->m_SETCapabilities.m_PrefMethod = session->m_PrefMethod;
    if(pos_init->m_LocationID.Copy(session->m_LocationID) != RETURN_OK)
        return FALSE;
    // optional for message.
    if(session->m_RAD)
    {
        pos_init->m_pRequestedAssistData = new(std::nothrow)  Engine::RequestedAssistData;
        if(!pos_init->m_pRequestedAssistData)
            return FALSE;
        if(!pos_init->m_pRequestedAssistData->IsObjectReady())
            return FALSE;
        if(pos_init->m_pRequestedAssistData->Copy(*(session->m_RAD)) != RETURN_OK)
            return FALSE;
    }
    if(session->m_SuplPosPosition)
    {
        pos_init->m_pPosition = new(std::nothrow)  Engine::Position;
        if(!pos_init->m_pPosition)
            return FALSE;
        if(!pos_init->m_pPosition->IsObjectReady())
            return FALSE;
        if(pos_init->m_pPosition->Copy(*(session->m_SuplPosPosition)) != RETURN_OK)
            return FALSE;
    }
    if(session->m_pVer)
    {
        pos_init->m_pVer = new(std::nothrow)  Engine::CBitString(64);
        if(!pos_init->m_pVer)
            return FALSE;
        if(!pos_init->m_pVer->IsObjectReady())
            return FALSE;
        if(pos_init->m_pVer->Copy(*(session->m_pVer)) != RETURN_OK)
            return FALSE;
    }

    pos_init->m_pSUPLPos = NULL;

    return TRUE;
}

CAlgorithmBase* CSUPLPosInitAlgorithm::CreateNewResponse(CAlgorithmBase* old, MSG* msg)
{
    if (msg->m_pIncomingMessage->GetType() == SUPL_POS)
    {
        return new (old) CSUPLPosAlgorithm();
    }


    return new (old) CSUPLEndAlgorithm();
}

} // end of namespace Engine
