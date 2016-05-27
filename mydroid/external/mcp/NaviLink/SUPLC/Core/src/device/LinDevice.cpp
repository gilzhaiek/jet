/*
******************************************************************
* COPYRIGHT © Teleca AB                                          *
*----------------------------------------------------------------*
* MODULE     : LinDevice.cpp                                     *
*                                                                *
* PROGRAMMER : Dmitriy Kardakov <Dmitriy.Kardakov@teleca.com>    *
* DATE       : 28 Feb 2009                                       *
* VERSION    : 1.0                                               *
*                                                                *
*----------------------------------------------------------------*
*                                                                *
* MODULE SUMMARY : Implementation of CLinDevice class.           *
* CLinDevice class provides methods for getting the GSM phone    *
* data (mcc, mnc, lac, ci) and processing the user notifications *
* about Network Initiated session                                *
*                                                                *
* This module works with SuplLocationProvider module accessing   *
* the OS-dependent Java methods                                  *
*----------------------------------------------------------------*
*                                                                *
* MODIFICATION RECORDS                                           *
* Dmitriy Kardakov - 28 Feb 2009 - Initial version               *
******************************************************************
*/

#include "android/ags_androidlog.h"
#include "device/ags_LinDevice.h"
#include <stdlib.h>
#include <stdio.h>

#include "utils/Log.h"

namespace Platform 
{

using namespace Engine;

void GsmDecode(char *bytes, int *size);
CLinDevice::CLinDevice()
{
    pthread_mutex_init(&m_Qm, NULL);
}

CLinDevice::~CLinDevice()
{
    ClearAllNotifications();
    pthread_mutex_destroy(&m_Qm);
}

/*
*******************************************************************
* Function: ClearAllNotifications
*
* Description : Removes all user notifications from queue
*
* Parameters : None
*
* Returns : None
*
*******************************************************************
*/

void CLinDevice::ClearAllNotifications()
{
    NotificationParams* params = NULL;

    pthread_mutex_lock(&m_Qm);

    if(!m_QNotificParamsQueue.empty())
    {
        // Find msg relative to session and delete it
        while (!m_QNotificParamsQueue.empty())
        {
            params = m_QNotificParamsQueue.front();
            m_QNotificParamsQueue.pop();
            if(params->state == FINISHED)
            {
                delete params;
            }
            else
            {
                Engine::CSession* session = (Engine::CSession *) params->session;
                session->m_PendingMessage->m_Status = MSG_POSITIONING_DENIED;
                delete params;
            }
        }
    }

    pthread_mutex_unlock(&m_Qm);
}


/*
*******************************************************************
* Function: GetIMSIFromSIM
*
* Description : gets the unique IMSI code from SIM card 
*
* Parameters :
* imsi   w Pointer to CIMSI class object
*
* Returns : None
*
*******************************************************************
*/
#if defined(_ANDROID_IMSI_)
void  CLinDevice::GetIMSIFromSIM(CIMSI* imsi)
{
    Engine::uint8_t IMSI_data [32]; 

    memset(IMSI_data, 0, sizeof(IMSI_data));
    int size = getSubscriberId((char *) IMSI_data, 32);
    imsi->assignIMSICode(IMSI_data, size);
}
void CLinDevice::GetMSISDNFromSIM(CMSISDN * msisdn)
{
		Engine::uint8_t MSISDN_data[32];
		LOGD("\n----Entering GetMSISDNFromSIM----\n");
		memset(MSISDN_data,0,sizeof(MSISDN_data));
		int size = getMSISDN((char *) MSISDN_data, 32);
		msisdn->assignMSISDNCode(MSISDN_data, size);
}
#elif defined(_DEBUG_IMSI)
void  CLinDevice::GetIMSIFromSIM(Engine::uint8_t* pData, Engine::uint32_t* size)
{
    imsi.getCodedIMSI(&buffer, size);
}
#else
#error("You MUST define type _ANDROID_IMSI_ or _DEBUG_IMSI")
#endif

#if defined(_DEBUG_IMSI)
/*
*******************************************************************
* Function: SetIMSI
*
* Description : sets the unique IMSI code from the buffer pointed 
* buffer to the private CIMSI class object field - imsi.
* 
* Parameters :
* pData   r Data buffer pointer
* size    r Size of valid data in buffer
* Returns : None
*
*******************************************************************
*/
void  CLinDevice::SetIMSI(Engine::uint8_t* pData, Engine::uint32_t size)
{
    imsi.SetIMSI(pData, size);
}
/*
*******************************************************************
* Function: GetMCC
*
* Description : Gets the MCC code
*
* Parameters :
* pchMcc    w buffer for storing the MCC code  
* Returns : None
*
*******************************************************************
*/
void  CLinDevice::GetMCC(char* pchMcc)
{
    imsi.getMCC(pchMcc);
}
/*
*******************************************************************
* Function: GetMNC
*
* Description : Gets the MNC code
*
* Parameters : None
* pchMnc     w buffer for storing the MNC code 
* Returns : None
*
*******************************************************************
*/
void  CLinDevice::GetMNC(char* pchMnc)
{
    imsi.getMNC(pchMnc);
}
#endif

/*
*******************************************************************
* Function: GetCellInformation
*
* Description : gets the Cell Information 
* 
* Parameters :
* cell    w Pointer to the GetCellInformation class object
* Returns : None
*
*******************************************************************
*/
#if defined(_ANDROID_IMSI_)
#if defined(DOCOMO_SUPPORT_WCDMA)
void CLinDevice::GetCellInformation(Engine::WCDMACellInformation* cell)
{
	int info[GSM_INFO_SIZE];
	memset(info,0,sizeof(info));
	int num = getGSMInfo(info);
	if(num != GSM_INFO_SIZE)
	{
		return;
	}
	cell->m_MCC = info[2];
    cell->m_MNC = info[3];
    cell->m_CI  = info[0];
    cell->m_frequencyInfo = NULL;
    cell->m_primaryScramblingCode = NULL;
	cell->m_measuredResultsList = NULL;
}
#else
void CLinDevice::GetCellInformation(Engine::GSMCellInformation* cell)
{
    int info[GSM_INFO_SIZE];
    memset(info, 0, sizeof(info));
    int num = getGSMInfo(info);

    if (num != GSM_INFO_SIZE)
    {
        return;
    }
    
    cell->m_MCC = info[2];
    cell->m_MNC = info[3];
    cell->m_LAC = info[1];
    cell->m_CI  = info[0];
    cell->m_aNMR = NULL;
    cell->m_TA = NULL;    
}
#endif
#elif defined(_DEBUG_IMSI)
void CLinDevice::GetCellInformation(Engine::GSMCellInformation* cell)
{
    cell->m_MCC = 234;
    cell->m_MNC = 15;
    cell->m_LAC = 179;
    cell->m_CI = 8212;
    cell->m_aNMR = NULL;
    cell->m_TA = NULL;    
}
#else
#error("You MUST define type _ANDROID_IMSI_ or _DEBUG_IMSI")
#endif
/*
*******************************************************************
* Function: MsgBox_ThreadProc
*
* Description : Thread function. This thread processes the single 
* user notification task and sends notification to user. Function 
* calls the MessageBox method from SUPLLocationProider.cpp module.
* Parameters :
* pThreadData  r Thread function argument. 
*                Pointer to MsgBox_ThreadProc_Data data structure
* Returns : None
*
*******************************************************************
*/
void* CLinDevice::MsgBox_ThreadProc(void* pThreadData)
{
    eNotificationType iMsgType = MB_OK;
    MsgBox_ThreadProc_Data* Proc_Data = (MsgBox_ThreadProc_Data*) pThreadData;
    pthread_mutex_lock(&Proc_Data->Mutex);
    Engine::CSession* session = (Engine::CSession*)Proc_Data->params.session;
    int thread_ID = (int)Proc_Data->params.pThread;
	debugMessage("MsgBox_ThreadProc: thread_ID=[%d]", thread_ID);
    pthread_mutex_unlock(&Proc_Data->Mutex);

    pthread_mutex_destroy(&Proc_Data->Mutex);
    delete Proc_Data;

    NotificationBytes_t ReqIdAndName[SMSWAP_MESSAGE_STRINGS];
    memset(ReqIdAndName, 0, SMSWAP_MESSAGE_STRINGS * 
                            sizeof(NotificationBytes_t));
    Engine::uint8_t * ReqId = NULL;
    Engine::uint8_t * ReqName = NULL;

    if(session->m_Notification->requestorId)
    {
    	debugMessage("MsgBox_ThreadProc: +requestorId");
        if(session->m_Notification->encodingType)
        {

            if(*(session->m_Notification->encodingType) == ucs2)
            {
            	debugMessage("MsgBox_ThreadProc: requestorId ucs2");

                //ReqIdAndName[0].bytes = new char[session->m_Notification->requestorId->m_Size];
                ReqIdAndName[0].size = session->m_Notification->requestorId->m_Size;
				debugMessage("MsgBox_ThreadProc: requestorId m_Size=[%d]", session->m_Notification->requestorId->m_Size);
                ReqId = (Engine::uint8_t *) ReqIdAndName[0].bytes;

                for(Engine::uint16_t i = 0; 
                    i < (session->m_Notification->requestorId->m_Size / 2) - 1;
                    i++)
                {
                    ReqId[i] = ((Engine::uint8_t*)session->m_Notification->requestorId->m_Data)[i*2 + 3];
                    //ReqId[i] = RevShortFromShort(ReqId[i]);
					debugMessage("MsgBox_ThreadProc: ucs2 ReqId=[0x%x] ReqId=[%c]",ReqId[i], ReqId[i]);
                }
                
                ReqId[(session->m_Notification->requestorId->m_Size / 2) - 1] = L'\0';
				debugMessage("MsgBox_ThreadProc: ucs2 requestorId=[%s]",ReqIdAndName[0].bytes);
				ReqIdAndName[0].size = (session->m_Notification->requestorId->m_Size / 2) - 1;
				debugMessage("MsgBox_ThreadProc: requestorId m_Size.=[%d]", ReqIdAndName[0].size);
                ReqIdAndName[0].encoding = UTF16;
            }


            if(*(session->m_Notification->encodingType) == utf8)
            {
            	debugMessage("MsgBox_ThreadProc: utf8");
                //ReqIdAndName[0].bytes = new char[session->m_Notification->requestorId->m_Size];
                ReqIdAndName[0].size = session->m_Notification->requestorId->m_Size;
                memcpy(ReqIdAndName[0].bytes, session->m_Notification->requestorId->m_Data, ReqIdAndName[0].size);
                ReqIdAndName[0].encoding = UTF8;
            }

			if(*(session->m_Notification->encodingType) == gsmDefault)
			{
				//debugMessage("MsgBox_ThreadProc: gsm encodingType NOT SUPPORTED requestorId");
            	debugMessage("MsgBox_ThreadProc: gsmDefault");
                ReqIdAndName[0].size = session->m_Notification->requestorId->m_Size;
				//GSM decode
				memset(ReqIdAndName[0].bytes, 0, sizeof(ReqIdAndName[0].bytes));
				memcpy(ReqIdAndName[0].bytes, session->m_Notification->requestorId->m_Data, ReqIdAndName[0].size);
				debugMessage("MsgBox_ThreadProc: gsmDefault requestorId=[%s]", ReqIdAndName[0].bytes);
				GsmDecode(ReqIdAndName[0].bytes, &ReqIdAndName[0].size);
				LOGD("size = %d\n", ReqIdAndName[0].size);
				debugMessage("MsgBox_ThreadProc: gsmDefault requestorId=[%s]", ReqIdAndName[0].bytes);
				debugMessage("MsgBox_ThreadProc: requestorId m_Size.=[%d]", ReqIdAndName[0].size);					
                ReqIdAndName[0].encoding = GSM_DEFAULT;
			}
        }
		debugMessage("MsgBox_ThreadProc: -requestorId");
    }
	else
	{
		debugMessage("MsgBox_ThreadProc: !requestorId");
	}
    
    if(session->m_Notification->clientName)
    {
    	debugMessage("MsgBox_ThreadProc: +clientName");
        if(session->m_Notification->encodingType)
        {
        
            if(*(session->m_Notification->encodingType) == ucs2)
            {
            	debugMessage("MsgBox_ThreadProc: clientName ucs2");
                //ReqIdAndName[1].bytes = new char[session->m_Notification->clientName->m_Size];
                ReqIdAndName[1].size = session->m_Notification->clientName->m_Size;
				debugMessage("MsgBox_ThreadProc: clientName m_Size=[%d]", session->m_Notification->clientName->m_Size);
                ReqName = (Engine::uint8_t *) ReqIdAndName[1].bytes;
                
                for(Engine::uint16_t i = 0;
                    i < (session->m_Notification->clientName->m_Size / 2) - 1; 
                    i++)
                {
                    ReqName[i] = ((Engine::uint8_t*)session->m_Notification->clientName->m_Data)[i*2 + 3];

                    //ReqName[i] = RevShortFromShort(ReqName[i]);
					debugMessage("MsgBox_ThreadProc: ucs2 ReqName=[0x%x] ReqName=[%c]",ReqName[i], ReqName[i]);
                }

                ReqName[(session->m_Notification->clientName->m_Size / 2) - 1] = L'\0';
				debugMessage("MsgBox_ThreadProc: ucs2 clientName=[%s]",ReqIdAndName[1].bytes);				
				ReqIdAndName[1].size = (session->m_Notification->clientName->m_Size / 2) - 1;
				debugMessage("MsgBox_ThreadProc: clientName m_Size.=[%d]", ReqIdAndName[1].size);
                ReqIdAndName[1].encoding = UTF16;
            }
            
            if(*(session->m_Notification->encodingType) == utf8)
            {
                //ReqIdAndName[1].bytes = new char[session->m_Notification->clientName->m_Size];
                ReqIdAndName[1].size = session->m_Notification->clientName->m_Size;
                memcpy(ReqIdAndName[1].bytes, session->m_Notification->clientName->m_Data, ReqIdAndName[0].size);
                ReqIdAndName[1].encoding = UTF8;
            }
        }

		if(*(session->m_Notification->encodingType) == gsmDefault)
		{
			//debugMessage("MsgBox_ThreadProc: gsm encodingType NOT SUPPORTED clientName");
           	debugMessage("MsgBox_ThreadProc: utf8");
            ReqIdAndName[1].size = session->m_Notification->clientName->m_Size;
    		//GSM Decode
			memset(ReqIdAndName[1].bytes, 0, sizeof(ReqIdAndName[1].bytes));
			memcpy(ReqIdAndName[1].bytes, session->m_Notification->clientName->m_Data, ReqIdAndName[1].size);
			debugMessage("MsgBox_ThreadProc: gsmDefault requestorId=[%s]", ReqIdAndName[1].bytes);
			GsmDecode(ReqIdAndName[1].bytes, &ReqIdAndName[1].size);
			LOGD("size = %d\n", ReqIdAndName[1].size);
			debugMessage("MsgBox_ThreadProc: gsmDefault clientName=[%s]", ReqIdAndName[1].bytes);
			debugMessage("MsgBox_ThreadProc: clientName m_Size.=[%d]", ReqIdAndName[1].size);					
            ReqIdAndName[1].encoding = GSM_DEFAULT;			
		}
		debugMessage("MsgBox_ThreadProc: -clientName");
    }
	else
	{
		debugMessage("MsgBox_ThreadProc: !clientName");
	}
    
    if(session->m_Notification->notificationType == notificationOnly)
    {
    	debugMessage("MsgBox_ThreadProc: MB_OK");
        iMsgType = MB_OK;
    }
    else
    {
    	debugMessage("MsgBox_ThreadProc: MB_YESNO");
        iMsgType = MB_YESNO;
    }
    
    MessageBox(iMsgType, ReqIdAndName, SMSWAP_MESSAGE_STRINGS, thread_ID, NOTIFICATION_TIME_OUT);
    
	/*
    if (ReqIdAndName[0].bytes)
    {
        delete [] ReqIdAndName[0].bytes;
    }
    
    if (ReqIdAndName[1].bytes)
    {
        delete [] ReqIdAndName[1].bytes;
    }
    */
    
    return (void *)0;
}
/*
*******************************************************************
* Function: NotificationResult
*
* Description : Function provides the user notification result.  
* User can allow or deny the connection to SUPL server. 
* Method is called by the native method of SuplLocationProvider.cpp 
* module.
* 
* Parameters :
* res     r Notification result. 
* tid     r Notification processing thread id.
* Returns : None
*
*******************************************************************
*/
void CLinDevice::NotificationResult(int res, int tid)
{
	debugMessage("+NotificationResult");
    NotificationParams* params = NULL;
    Engine::CSession* session  = NULL;
    eNotificationType iMsgType;

	debugMessage("NotificationResult: res=[%d]", res);
	debugMessage("NotificationResult: tid=[%d]", tid);	

    pthread_mutex_lock(&m_Qm);

    
    if (m_QNotificParamsQueue.empty()) {
        pthread_mutex_unlock(&m_Qm);
		debugMessage("-NotificationResult: Queue.empty");
        return;
    }
    
	debugMessage("NotificationResult: Queue.size()=[%d]", m_QNotificParamsQueue.size());
    for (unsigned int i = 0; i < m_QNotificParamsQueue.size(); i++)
    {
        params = m_QNotificParamsQueue.front();
        m_QNotificParamsQueue.pop();
        //m_QNotificParamsQueue.push(params);
        
		debugMessage("NotificationResult: params->pThread[%d]=[%d]", i, (int)params->pThread);
        if ((int)params->pThread == tid)
        {
        	debugMessage("NotificationResult: thread tid found");
            break;
        }	

        params = NULL;
		debugMessage("NotificationResult: thread tid NOT found");
    }
    
    if (params == NULL) {
        pthread_mutex_unlock(&m_Qm);
		debugMessage("-NotificationResult: params == NULL");
        return;
    }
    
    session = (Engine::CSession*)params->session;
    if(session->m_Notification->notificationType == notificationOnly)
    {
    	debugMessage("NotificationResult: notificationOnly");
        iMsgType = MB_OK;
    }
    else
    {
    	debugMessage("NotificationResult: !notificationOnly");
        iMsgType = MB_YESNO;
    }
    
    if(iMsgType == MB_OK)
    {
        debugMessage("NotificationResult: MB_OK MSG_POSITIONING_GRANTED");
    }
    else // MB_YESNO
    {
        if(res == ALLOW)
        {
        	debugMessage("NotificationResult: ALLOW");
            session->m_PendingMessage->m_Status = MSG_POSITIONING_GRANTED;
        }
        else if (res == DENY)
        {
        	debugMessage("NotificationResult: DENY");
            session->m_PendingMessage->m_Status = MSG_POSITIONING_DENIED;
        }
		else // TIMEOUT
		{
			debugMessage("NotificationResult: TIMEOUT");
			if(session->m_Notification->notificationType == notificationAndVerficationAllowedNA)
			{
				debugMessage("NotificationResult: TIMEOUT-ALLOWED");
				session->m_PendingMessage->m_Status = MSG_POSITIONING_GRANTED;
			}
			else if (session->m_Notification->notificationType == notificationAndVerficationDeniedNA)
			{
				debugMessage("NotificationResult: TIMEOUT-DENIED");
				session->m_PendingMessage->m_Status = MSG_POSITIONING_DENIED;
			}
		}
    }
    params->state = FINISHED;
	
	debugMessage("Before if()MSG_POSITIONING_GRANTED");
	
    if(session->m_PendingMessage != NULL)
    {
	    if(session->m_PendingMessage->m_Status == MSG_POSITIONING_GRANTED)
	    {
	        debugMessage("MSG_POSITIONING_GRANTED");
	    }
	    else
	    {
	        debugMessage("MSG_POSITIONING_DENIED");
	    }
    }
	else
    {

			debugMessage("session->m_PendingMessage: NULL");
    }
	
    pthread_mutex_unlock(&m_Qm);

	debugMessage("-NotificationResult");	
}

/*
*******************************************************************
* Function: DisplayNotifications
*
* Description : Adds the new user notification task to queue and 
* launches the prosessing thread. SUPL Controller calls this method 
* for sending notifications to user about network initiated session.
* 
* Parameters :
* session r Pointer to CSession class object. 

* Returns : TRUE if success
*
*******************************************************************
*/

bool_t CLinDevice::DisplayNotifications(CSession* session)
{
    int queueSize = 0;
    NotificationParams* params = NULL;
    MsgBox_ThreadProc_Data* Proc_Data = NULL;
    pthread_mutex_lock(&m_Qm);
    if(!m_QNotificParamsQueue.empty())
    {
        queueSize = m_QNotificParamsQueue.size();
        // Find msg relative to session and delete it
        for(Engine::uint16_t i = 0; i < queueSize; i++)  
        {
            params = m_QNotificParamsQueue.front();
            m_QNotificParamsQueue.pop();
            if(params->state == FINISHED)
            {
                delete params;
            }
            else
            {
                m_QNotificParamsQueue.push(params);
            }
        }

        if(m_QNotificParamsQueue.size() > MAX_NOTIFICATION_QUEUE_LENGTH) 
        {
            pthread_mutex_unlock(&m_Qm);
            return FALSE;
        }
    }

    params = NULL;
    params = new(std::nothrow) NotificationParams;
    if(!params) 
    {
        pthread_mutex_unlock(&m_Qm);
        return FALSE;
    }
    
    Proc_Data = new(std::nothrow) MsgBox_ThreadProc_Data;
    if(!Proc_Data) 
    {
        delete params;
        pthread_mutex_unlock(&m_Qm);
        return FALSE;
    }
    
    pthread_t thread_id;
    pthread_attr_t attr;
    pthread_attr_init(&attr);
    pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
    pthread_attr_setstacksize(&attr, 1024);
    params->session = session;
    
    pthread_mutex_init(&Proc_Data->Mutex, NULL);
    pthread_mutex_lock(&Proc_Data->Mutex);
    
    int r = pthread_create (&thread_id, NULL, CLinDevice::MsgBox_ThreadProc, (void *) Proc_Data);
    if(r != 0)
    {
        pthread_mutex_unlock(&Proc_Data->Mutex);
        pthread_mutex_destroy(&Proc_Data->Mutex);
        delete params;
        delete Proc_Data;
        pthread_mutex_unlock(&m_Qm);
        return FALSE;
    }
    pthread_attr_destroy(&attr);
    params->state = ACTIVE;
    params->pThread = thread_id;
	debugMessage("DisplayNotifications: thread_id=[%d]", params->pThread);
	debugMessage("DisplayNotifications: session->m_Notification.notificationType = %d",params->session->m_Notification->notificationType);
	if((params->session->m_Notification->notificationType == notificationAndVerficationAllowedNA)||(params->session->m_Notification->notificationType == notificationAndVerficationDeniedNA) )
	{
		 m_QNotificParamsQueue.push(params);
		 debugMessage("DisplayNotifications: pushed");
	}
   

    memcpy(&Proc_Data->params, params, sizeof (struct NotificationParams));
    pthread_mutex_unlock(&Proc_Data->Mutex);
    pthread_mutex_unlock(&m_Qm);
    return TRUE;
}


void GsmDecode(char *strDec, int *size_)
{
    char p7   = 0;
    char nxtBits = 0;
    char encChar = 0;
    char decChar = 0;
    int encCount = 0;
    int decCount = 0;

	int size = *size_;

    int decMask[]     = {0x7F, 0x3F, 0x1F, 0x0F, 0x07, 0x03, 0x1};
    int nextBitMask[] = {0x80, 0xC0, 0xE0, 0xF0, 0xF8, 0xFC, 0xFE};

	char strEnc[128];

	memcpy(strEnc, strDec, size);

    while (encCount < size)
    {
        p7 = (encCount % 7);

        debugMessage("encCount %d \n", encCount);
        debugMessage("p7    %d \n", p7);

        encChar = strEnc[encCount];

        if (p7 == 0)
        {
            nxtBits = 0;
        }

        debugMessage("encChar - [%x] \n",encChar);
        decChar = ((decMask[p7] & encChar) << p7) | nxtBits;
        debugMessage("decChar - [%c] \n",decChar);
        strDec[decCount] = decChar;
        decCount++;

        nxtBits = (nextBitMask[p7] & encChar) >> (7 - p7);

        if (p7 == 6)
        {
            decChar = nxtBits;
            debugMessage("decChar. - [%c] \n",decChar);
            strDec[decCount] = decChar;
            decCount++;
        }

        encCount++;
    }

    strDec[decCount] = '\0';
    debugMessage("\nDecoded  [%s], encCount = %d\n", strDec, decCount );

	if (strlen(strDec) > 0)
	{
		*size_ = strlen(strDec);
	}

	LOGD("\nDecoded  [%s], *size_ = %d\n", strDec, *size_ );
}

}
