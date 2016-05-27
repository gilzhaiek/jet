/*
******************************************************************
* COPYRIGHT © Teleca AB                                          *
*----------------------------------------------------------------*
* MODULE     : SUPLController.h                                  *
*                                                                *
* PROGRAMMER : Ildar Abdullin                                    *
* DATE       : 28 Feb 2009                                       *
* VERSION    : 1.0                                               *
*                                                                *
*----------------------------------------------------------------*
* MODULE SUMMARY : CSUPLController class is a main SUPL module   *
* which contains the state machine and controls the program      *
* life cycle. It performs the SUPL initialization,               *
* processes the message queue and manages the Network connection.*
*----------------------------------------------------------------*
* MODIFICATION RECORDS                                           *
* Ildar Abdullin   - 23 Nov 2007 - Initial version               *
* Dmitriy Kardakov - 28 Feb 2009 - Porting to Android            *
******************************************************************
*/
#include "android/ags_androidlog.h"
#include "suplcontroller/SUPLController.h"
#include <string.h>
//#include "suplc_core_wrapper.h"
#include "ti_client_wrapper/suplc_hal_os.h"

#include <netdb.h>
#include <arpa/inet.h>
#include "jni.h"
#include <utils/Log.h>
#include "nav_log_msg.h"
#include "nav_log_cust_codes.h"
#define SUPL_CONFIG_FILE_PATH "/system/etc/gps/config/SuplConfig.spl"
#define RDONLY "r"
#define MAX_HSLP_LENGTH 2
#define MAX_BUF_LENGTH 1024
#define TRUE 1
#define FALSE 0
#define slphost_port "slphost_port"
#define ULTS_SLP "192.168.0.35:7275"
#define TCS_SLP "208.8.164.7:7275"



using namespace std;
using namespace Platform;
using namespace Codec;




extern SEM_HANDLE gp_semHandle;
extern "C" void SetSLPAddress_c(char* SLPAddress)
{
   LOGD(" Entered SetSLPAddress_c \n ");
   Engine::CSUPLController::GetInstance().SetSLPAddress(SLPAddress);

}


namespace Engine
{


CSUPLController& CSUPLController::GetInstance()
{
    static CSUPLController m_pInstance;
    return m_pInstance;
}

CSUPLController::CSUPLController():

#if defined(WIN32) || defined(WINCE)
        hThread(NULL),
#endif /* WIN32 || WINCE */
#if defined(_LINUX_)
        hThread(0),
#endif /* _LINUX_ */
        m_pGPSDevice(NULL),
        m_pNetwork(NULL),
        m_pDevice(NULL),
        m_ULP_Processor(NULL),
        m_SignalToKill(FALSE),
        m_ControllerStopped(TRUE),
	m_IsSessionInProgress(FALSE)
{
#if defined(WIN32) || defined(WINCE)
    InitializeCriticalSection(&cs);
#endif /* WIN32 || WINCE */
#if defined(_LINUX_)
    pthread_mutex_init(&cs, NULL);
#endif /* _LINUX_ */
}

CSUPLController::~CSUPLController()
{
    SUPLCleanup();
    if (m_pGPSDevice)
    {
        delete m_pGPSDevice;
        m_pGPSDevice = NULL;
    }
    if (m_pNetwork)
    {
        delete m_pNetwork;
        m_pNetwork = NULL;
    }
    if (m_pDevice)
    {
        delete m_pDevice;
        m_pDevice = NULL;
    }
    if (m_ULP_Processor)
    {
        delete m_ULP_Processor;
        m_ULP_Processor = NULL;
    }
#if defined(WIN32) || defined(WINCE)
    DeleteCriticalSection(&cs);
#endif /* WIN32 || WINCE */
#if defined(_LINUX_)
    pthread_mutex_destroy(&cs);
#endif /* _LINUX_ */
}

#if defined(_LINUX_)
int CSUPLController::Sleep(int ms)
{
    struct timespec ts;

    /* Delay for a bit */
    ts.tv_sec = 0;
    ts.tv_nsec = ms * 1000 * 1000;
    nanosleep (&ts, NULL);
    return 0;
}
#endif /* _LINUX_ */

void CSUPLController::Lock()
{
#if defined(WIN32) || defined(WINCE)
    EnterCriticalSection(&cs);
#endif /* WIN32 || WINCE */
#if defined(_LINUX_)
    pthread_mutex_lock(&cs);
#endif /* _LINUX_ */
}

void CSUPLController::UnLock()
{
#if defined(WIN32) || defined(WINCE)
    LeaveCriticalSection(&cs);
#endif /* WIN32 || WINCE */
#if defined(_LINUX_)
    pthread_mutex_unlock(&cs);
#endif /* _LINUX_ */
}

bool_t CSUPLController::AppendNetwork(Platform::INetwork* net)
{

    if (!net)
        return FALSE;

    if (m_pNetwork != NULL)
    {
        delete m_pNetwork;
    }
    m_pNetwork = net;
#if defined(_LINUX_)
    if (m_pNetwork->LSR_StartListen() != 0)
    {
   		SESSLOGMSG("[%s]%s:%d, %s %s \n",get_utc_time(),SESS_WARN_SET_INIT,nav_sess_getCount(0)," ","#SET-Initiated session, no network connection, fallback to standalone");
        return FALSE;

    }
#endif /* _LINUX_ */
    if (m_pNetwork->TLS_Init() != 0)
    {
		SESSLOGMSG("[%s]%s:%d, %s %s \n",get_utc_time(),SESS_WARN_SET_INIT,nav_sess_getCount(0)," ","#SET-Initiated session, no network connection, fallback to standalone");
        return FALSE;
    }

    SESSLOGMSG("[%s]%s:%d, %s \n",get_utc_time(),SESS_NW_CONN_SUCCESS,nav_sess_getCount(0),"#Network Connection Success");
    return TRUE;
}

bool_t CSUPLController::AppendGPS(Platform::IGPS* gps)
{
    if (!gps)
        return FALSE;

    if (m_pGPSDevice != NULL)
    {
        delete m_pGPSDevice;
    }
    m_pGPSDevice = gps;

    return TRUE;
}

bool_t CSUPLController::AppendDevice(Platform::IDevice* device)
{
    if (!device)
        return FALSE;

    if (m_pDevice != NULL)
    {
        delete m_pDevice;
    }
    m_pDevice = device;

    return TRUE;
}

bool_t CSUPLController::AppendULPProccessor(Codec::ULP_Processor* ulp_proc)
{
    if (!ulp_proc)
        return FALSE;

    if (m_ULP_Processor != NULL)
    {
        delete m_ULP_Processor;
    }
    m_ULP_Processor = ulp_proc;

    return TRUE;
}

static int stringCompare(const char *const p_string_1,
                                   const char *const p_string_2)
{
    if ( strcmp(p_string_1, p_string_2) == 0)
    {
        return TRUE;
    }
    return FALSE;
}
char*   DUMMY_SLP_HOST =  "0.0.0.0:0";
#define BUFF_SIZE 256

void CSUPLController:: SetSLPAddress(char * SLPAddress)
{
     /*KW Changes by Naresh */
	 strncpy(m_ServerIPAdrress,SLPAddress, sizeof(m_ServerIPAdrress)-1);

    LOGD(" CSUPLController:: SetSLPAddress set m_ServerIPAdrress %s \n ", m_ServerIPAdrress);
}

char* CSUPLController::GetSlpAddress(void)
{
return  m_ServerIPAdrress;
}

JNIEXPORT jstring JNICALL Java_android_supl_config_SuplConfig_read_slp_c(JNIEnv* env, jobject jobj)
{

  LOGD("ben received call from java to read slp address \n");

  return (env->NewStringUTF(CSUPLController::GetInstance().GetSlpAddress()));

}

int read_config_file(char *tag, char *value)
{
	FILE *fp = NULL;
	char buff[BUFF_SIZE], *pos;
	int i=0;
	char c;

	if ((value==NULL) || (tag==NULL) ){
		LOGD ("INVALID PARAMETERS RECEIVED !!!!! \n");
        return -1;
	}
     fp = fopen(SUPL_CONFIG_FILE_PATH,"r");
	if (fp == NULL)
	{
		LOGD ("file open failed : %d returning dummy slp\n ", fp);
		value=DUMMY_SLP_HOST;
		return -1;
	}
	else
     #if DEBUG
		LOGD ("file open sucess \n");
      #endif

    strncpy(buff,"0",BUFF_SIZE);
	do{
		c = getc(fp);
		if (c!='\n')
		{
			buff[i++] = c;
		}
		else{  //I have read a complete line by now
		//	LOGD ("\n %d \t %s\n",strlen(buff), buff);
			pos=strstr(buff,tag);
			if (pos!=NULL)
			{
				strcpy(value, (buff+strlen(tag)) );
				fclose(fp);
				return 1;
			}
			//I haven't got my tag yet, continue search
			i=0;
			strncpy(buff,"0",BUFF_SIZE);
		}
		//LOGD ("%c",c);
	}while(c!=EOF);

	fclose(fp);
 return 0;
}

//#ifdef FQDN_ENABLED
char *fqdnToIP(char *fqdn )
{

    int i;
    struct hostent *myhostent;
    struct in_addr **addr_list;

    myhostent = gethostbyname(fqdn);
        LOGD("errno  %d", h_errno);


    if (myhostent == NULL)
	    {
         LOGD("ERROR fqdnToIP C:  cannot resolve fqdn : %s \n ", fqdn);

		 return fqdn;
		}
    else
       {
       LOGD("Canonical Name : %s \n", myhostent->h_name);
       addr_list = (struct in_addr **)myhostent->h_addr_list;

      //If more than one IP's print here
      for (i=0; addr_list[i]!=0 ; i++)
           LOGD("in loop : %s \n", (char *)inet_ntoa(*addr_list[i]));
      }

      //I expect only one IP address, so return first element in array

       return (char *)inet_ntoa(*addr_list[0]);

}
//#endif

char slp_address[BUFF_SIZE];
char slp_IP[BUFF_SIZE];
#define PORT_SIZE 10
char value[BUFF_SIZE];
char port [PORT_SIZE];
#define ULTS_IP "192.168.0.35"




void genAutoHSLP(int mcc,int mnc, char_t* fqdn)
{
	int n=-1;
	LOGD("genAutoHSLP with mcc =  %d, mnc = %d \n", mcc,mnc);
	memset(fqdn,'\0',BUFF_SIZE);
	n=sprintf(fqdn,"h-slp.mnc%03d.mcc%03d.pub.3gppnetwork.org",mnc,mcc);
	LOGD("genAutoHSLP n =  %d \n", n);
}


SLP_Type_t CSUPLController::readSlpType()
{
	char slpType[BUFF_SIZE];
	m_SlpType = IP_ADDR;
	if( read_config_file("<slp_type> ",slpType)==1)
	{
		if(strcmp(slpType,SLP_FQDN_PHONE)==0)
			m_SlpType=FQDN_PHONE;
		else if(strcmp(slpType,SLP_FQDN_SIM)==0)
			m_SlpType=FQDN_SIM;
		else if(strcmp(slpType,SLP_FQDN_AUTO)==0)
			m_SlpType=FQDN_AUTO;
		else
			m_SlpType=IP_ADDR;

	}

	return m_SlpType;
}

bool_t CSUPLController::genSLPIpAddress(SLP_Type_t slpType)
{

    int info[MAX_HSLP_LENGTH];

    LOGD("slpType %d  \n", slpType);
    if (slpType==FQDN_AUTO)
    {
        LOGD("slpType=FQDN_AUTO \n");

        if (getHSlp(info) < 0)
        {
            debugMessage("%s, error get HSLP info data", __FUNCTION__);
            return FALSE;
        }

        genAutoHSLP(info[0],info[1],value);

        LOGD("fqdn %s  len : %d \n", value, strlen(value));
        strcpy(slp_address, value);
        strcpy(slp_IP, value);
        // If FQDN Conversion returns FQDN, default to ULTS ip
        if (strcmp (slp_IP, ULTS_IP) == 0)
        {
            m_SlpType=FQDN_AUTO;
        }

        if (read_config_file("<port> ", port) == 1 )
        {
            LOGD("fqdn port is %s   \n", port);
        }
        else
        {
            LOGD("Failed getting fqdn port, using default 7275 \n"); // use 7275
            strcpy(port,"7275");
        }
        strcat (slp_IP,":");
        strcat (slp_IP,port);
        m_ServerIPAdrress=slp_IP;
        m_SlpType=FQDN_AUTO;
        LOGD("After combining fqdn and port, %s \n", slp_IP);

    }

    else
    {
        if ( read_config_file("<slphost_fqdn> ",value)==1)
        {
            LOGD("fqdn %s  len : %d \n", value, strlen(value));
            strcpy(slp_address, value);
            strcpy(slp_IP, value);

            // If FQDN Conversion returns FQDN, default to ULTS ip
            if (strcmp (slp_IP, ULTS_IP) == 0)
            {
                m_SlpType=IP_ADDR;
            }
        }
        else
        {
            LOGD("ERROR !!!! Failed getting fqdn \n"); //return some default value
            SESSLOGMSG("[%s]%s:%d, %s \n",get_utc_time(),SESS_ERR_SET_INIT,nav_sess_getCount(0),"#SET-Initiated Session: wrong input configuration");
            /*KW Changes by Naresh */
			strncpy(m_ServerIPAdrress,DUMMY_SLP_HOST,sizeof(m_ServerIPAdrress)-1);

        }
        if (read_config_file("<port> ", port) == 1 )
        {
            LOGD("fqdn port is %s  \n", port);
        }
        else
        {
            LOGD("Failed getting fqdn port, using default 7275 \n"); // use 7275
            strcpy(port,"7275");
        }
        strcat (slp_IP,":");
        strcat (slp_IP,port);
        m_ServerIPAdrress=slp_IP;
        LOGD("after combining fqdn and port, %s \n", slp_IP);
    }

    return TRUE;

}



bool_t CSUPLController::SUPLInit(void * pNavPtr)
{
FILE *fp = NULL;
char a_inputBuffer[MAX_BUF_LENGTH] = {'\0'};
char *p_token = NULL;
char a_logMsg[200];


    nav_sess_setPtr(pNavPtr);
    LOGD("cmcm:: CSUPLController::SUPLInit %s, %d", get_utc_time(), nav_sess_getCount(0));

    if (AppendNetwork(INetwork::NewNetworkIF()) != TRUE)
    {
        return FALSE;
    }
    if (AppendGPS(IGPS::NewGpsIF()) != TRUE)
    {
        return FALSE;
    }
    if (AppendDevice(IDevice::NewDeviceIF()) != TRUE)
    {
        return FALSE;
    }
    if (AppendULPProccessor(new(std::nothrow)Codec::ULP_Processor) != TRUE)
    {
        return FALSE;
    }

    //genSLPIpAddress(readSlpType());

    // Get SlpType
    readSlpType();

    // Update slp_address
    if( read_config_file("<slphost_fqdn> ",value)==1)
    {
        strcpy(slp_address, value);
        LOGD("slp_address %s    len : %d \n", slp_address, strlen(value));
    }
    else
    {
        LOGD("ERROR !!!! Failed getting fqdn \n"); //return some default value
    }
    m_SignalToKill = FALSE;
    m_ControllerStopped = FALSE;

    return TRUE;
}

#if defined(WIN32) || defined(WINCE)
DWORD CSUPLController::SUPL_ThreadProc(void* ptr)
#endif /* WIN32 || WINCE */
#if defined(_LINUX_)
void* CSUPLController::SUPL_ThreadProc(void* ptr)
#endif /* _LINUX_ */
{
    CSUPLController::GetInstance().SUPLRun();
    return 0;
}

uint8_t CSUPLController::HandleServerConnection(uint8_t conRes)
{
	LOGD("HandleServerConnection: Connection Response = %d",conRes);
	static bool_t bConRetry = FALSE;

	Engine::MSG* msg = 0;
    msg = m_MessageQueue.front();
	if(NULL!=msg)
	{
	if(conRes==0) //CON_SUCCESS
	{
		if(msg->m_Status==MSG_WAIT_FOR_SERVER_CONNECTION)
		{
                readSlpType();
			//SESSLOGMSG("[%s]%s:%d,%s \n",get_utc_time(),SESS_SERVER_CONN_SUCCESS,nav_sess_getCount(0),"SC#Server Connection Success");
			LOGD("	 : Chnage the status to Ready to Send");
			msg->m_Status=MSG_READY_TO_SEND;
		}
	}
	else if(conRes==2) //CON_SSL_ERR_RETRY
	{
            m_pNetwork->TLS_CloseConnection();
		if(m_SlpType!=Engine::FQDN_AUTO)
		{
			m_SlpType=FQDN_AUTO;
			LOGD("HandleServerConnection: Failure : Need to Retry connection with Auto HSLP");
                if(FALSE == genSLPIpAddress(m_SlpType))
                {
                    msg->m_Status = Engine::MSG_FATAL_ERROR;
                    m_MessageQueue.push(msg);
                }
                else
                {
                    if ((NULL != msg->m_pIncomingMessage) &&
                        (SUPL_INIT == msg->m_pIncomingMessage->GetType()))
                    {
                        /* Re-encode the message and send..>!! */
                        msg->m_Status=MSG_READY_TO_PROCESS;
                    }
                    else
                    {
                        /* Re-encode not required in SI case...!! Just resend the message... */
                        msg->m_Status=MSG_RETRY_SERVER_CONNECTION;
                    }
                }
            }
		else
		{
			LOGD("HandleServerConnection: Failure : Chnage the status to FATAL ERROR.");
			SESSLOGMSG("[%s]%s:%d, %s \n",get_utc_time(),SESS_ERR_SERVER_CONN_FAIL,nav_sess_getCount(0),"#Server connection Failure");
			msg->m_Status=MSG_FATAL_ERROR;
		}
	}
	else // CON_FAILURE
	{
		LOGD("HandleServerConnection: Failure : Chnage the status to FATAL ERROR");
		SESSLOGMSG("[%s]%s:%d, %s \n",get_utc_time(),SESS_ERR_SERVER_CONN_FAIL,nav_sess_getCount(0),"#Server connection Failure");
		msg->m_Status=MSG_FATAL_ERROR;
	}

	}
	return 0;

}


bool_t CSUPLController::SUPLStart()
{

    Lock();
#if defined(WIN32) || defined(WINCE)
    if (hThread == NULL)
    {
        DWORD ID = 0;
        DWORD pCode = 0;
        hThread = CreateThread(NULL,0,( LPTHREAD_START_ROUTINE ) SUPL_ThreadProc, 0, 0, &ID);
        if (hThread == NULL )
        {
            return FALSE;
        }

        return TRUE;
    }
    else
    {
        return TRUE;
    }
#endif /* WIN32 || WINCE */
#if defined(_LINUX_)

    if (hThread == 0)
    {
        int ret = 0;
        size_t defStackSize = 0;
        pthread_attr_init(&pAttr);
        pthread_attr_getstacksize(&pAttr, &defStackSize);
        LOGD("Default stack size = %d", defStackSize);
        //pthread_attr_setstacksize(&pAttr, SUPL_THREAD_STACK_SIZE);
        //ret = pthread_create(&hThread, NULL, SUPL_ThreadProc, NULL);
        ret = pthread_create(&hThread, &pAttr, SUPL_ThreadProc, NULL);
        pthread_attr_destroy(&pAttr);
        if (ret < 0 )
        {
            UnLock();
            return FALSE;
        }
        UnLock();
        return TRUE;

    }
    else
    {
        UnLock();
        return TRUE;
    }
#endif /* _LINUX_ */

    UnLock();
}

void CSUPLController::SUPLRun()
{
    LOGD("%s BEGIN", __FUNCTION__);

    gp_semHandle = (SEM_HANDLE) malloc(sizeof(sem_t));
    if (NULL == gp_semHandle)
    {
        LOGD("%s: Memory allocation for gp_semHandle FAILED !!! ", __FUNCTION__);
        return;
    }


    if ( SUPLC_createSemaphore(gp_semHandle) < 0 )
    {
        LOGD("%s: Semaphore creation FAILED !!! ", __FUNCTION__);
        return;
    }

    // works while signal kill will not received.
    while (m_SignalToKill == FALSE)
    {
        //zz_Aby_c LOGD("%s: Semaphore waiting ... \n",  __FUNCTION__);
        if ( SUPLC_waitForSemaphore(gp_semHandle) < 0 )
        {
            //LOGD("%s: Semaphore waiting FAILED !!! ", __FUNCTION__);
            return;
        }

        // check data arrived
        CheckForData();

        // if message exists, process it.
        if (!m_MessageQueue.empty())
        {
            // process message;
            ProcessMessage();
        }

        if (!m_SessionQueue.isEmpty())
        {
            CheckSessionsForTimeout();
        }

    }
    m_ControllerStopped = TRUE;

    if ( SUPLC_destroySemaphore(gp_semHandle) < 0 )
    {
        LOGD("%s: Semaphore destroy FAILED !!! ", __FUNCTION__);
        return;
    }
    LOGD("%s END", __FUNCTION__);
}

/*
*******************************************************************
* Function: ProcessMessage
*
* Description : Process message. Get message from queue. Find
* assign session(or create new one). Process message through
* session. Verify message status. Encode message and send it.
* Process session.
* Parameters : None
* Returns : None
*
*******************************************************************
*/
void CSUPLController::ProcessMessage()
{
    Engine::MSG* message = NULL;
    CSession* session = NULL;
    int code=0;
	//LOGD("Entering ProcessMessage");
    // get active message;
    message = m_MessageQueue.front();
    m_MessageQueue.pop();

  //  LOGD("message->m_Status = %d ->sessionId = %d ", message->m_Status, message->m_SETSessionID);
  //  LOGD("m_PendingMessage = %d", GetSession(message)->m_PendingMessage);

    switch (message->m_Status)
    {
    case MSG_READY_TO_DECODE:
    {
        // decode message.
        Codec::SUCCESS_ERROR_T error;
        LOGD("SUPLC:MSG_READY_TO_DECODE");
        error = m_ULP_Processor->DecodeMessage(message);
        if (error != Codec::SUCCESS)
        {
            if (error == Codec::NO_MEMORY)
            {
                LOGD("Codec::NO_MEMORY");
                message->m_Status = MSG_FATAL_ERROR;
            }
            else
            {
                    LOGD("Error %d: Before HandleProtocolErrors! mStatus = %d", error, message->m_Status);
					session = GetSession(message);
        			if (session == NULL /*&& message->m_pIncomingMessage->GetType() == Engine::SUPL_INIT*/)
        			{
                        LOGD("MSG_READY_TO_DECODE: SUPL_INIT");

            			// New SUPL session
            			session = new(std::nothrow) CSession();
            			if (!session)
            			{
                			message->m_Status = Engine::MSG_FATAL_ERROR;
							LOGD("MSG_READY_TO_DECODE: MSG_FATAL_ERROR");
                			m_MessageQueue.push(message);
                			break;
            			}

            			uint16_t app_id=CSession::GenerateSessionId();
                        app_id=1001;
                        AddSession(session,app_id);
                        //AddSession(session,CSession::GenerateSessionId());

			            message->m_SETSessionID = session->m_AppID;
						LOGD("MSG_READY_TO_DECODE: message->m_SETSessionID = %d", message->m_SETSessionID);
        			}

#if defined(_DEBUG_IMSI)
						uint8_t  buffer[8];
						uint32_t size=8;
						CSUPLController::GetInstance().GetDevice().GetIMSIFromSIM(buffer,&size);
#elif defined(_ANDROID_IMSI_)
			CMSISDN   msisdn;
			CIMSI	  imsi;
			uint32_t  size = 0;
			uint8_t   tmp[16] ;
			uint8_t   *buffer = tmp;
			if((getSubscriberIdType()) == 1)
			{
			   /* Use MSISDN */
			   CSUPLController::GetInstance().GetDevice().GetMSISDNFromSIM(&msisdn);
			   msisdn.getCodedMSISDN(&buffer, &size);
			}
			else /* Default (0 or other values)  use IMSI */
			{
			   CSUPLController::GetInstance().GetDevice().GetIMSIFromSIM(&imsi);
			   imsi.getCodedIMSI(&buffer, &size);
			}
#endif
			
			// create session id	warning this place may contains BUG in SetSETSessionID
			COctetString CoC(buffer, size);
			if((getSubscriberIdType()) == 1)
			{
    			   if(session->SetSETSessionID(MSISDN, CoC, message->m_SETSessionID) != RETURN_OK)
			   {
			      return ;
			   }	
			}
			else
			{
			   if(session->SetSETSessionID(IMSI, CoC, message->m_SETSessionID) != RETURN_OK)
			   {
			      return ;
			   }	
			}

						uint8_t  bufferSLPuid[4] = {0x00, 0x00, 0x00, 0x01};
						uint32_t sizeSLPuid=4;

						uint8_t  bufferSLPAdd[4] = {0x00, 0x00, 0x00, 0x00};
						uint32_t sizeSLPAdd=4;

						COctetString CoCSLPuid(bufferSLPuid, sizeSLPuid);
						COctetString CoCSLPAdd(bufferSLPAdd, sizeSLPAdd);

						CIPAddress CIPSLPAdd(IPv4, &CoCSLPAdd);
						if(session->SetSLPSessionID(CoCSLPuid, CIPSLPAdd) != RETURN_OK)
						{
							return ;
						}

                    HandleProtocolErrors(message, error, session->iSessionID);
                    LOGD("Error %d: After HandleProtocolErrors! mStatus = %d", error, message->m_Status);
            }
            m_MessageQueue.push(message);
            break;
        }
        if(session)
        {
            MessageType type = message->m_pIncomingMessage->GetType();
            if(type != Engine::SUPL_END)
            {
                debugMessage("Encode success!!!: Calling SetTimestamp\n\r");
                session->SetTimestamp();
            }
        }


        message->m_SETSessionID = message->m_pIncomingMessage->m_SessionID.GetSessionUID();
        session = GetSession(message);

		LOGD("Message type is : %d", message->m_pIncomingMessage->GetType());

		if(message->m_pIncomingMessage->GetType() == Engine::SUPL_INIT)
		{
			if(m_IsSessionInProgress == TRUE)
			{
			    if(m_NewSessionMsgQueue.empty())
			    {
			        LOGD("SUPL: New NI session request when a session is ongoing");
			    }
			    else
			    {
			        LOGD("SUPL: Another NI session request when a session is ongoing");
			    }

	            // mark as ready to process
	            message->m_Status = Engine::MSG_READY_TO_PROCESS;
			    LOGD("SUPL: Push the message into NewSessionQ for future handling");
			    m_NewSessionMsgQueue.push(message);
	            break;
			}
			else
			{
			    /* An NI session begin is identified here */
			    m_IsSessionInProgress = TRUE;
			    LOGD("SUPL: New NI session start is identified");
			}
		}


        if (session == NULL && (message->m_pIncomingMessage->GetType() != Engine::SUPL_INIT))
        {
            if (m_SessionQueue.isEmpty())
            {
                LOGD("Codec::NO_MEMORY");
                message->m_Status = MSG_READY_TO_DIE;
                m_MessageQueue.push(message);
                break;
            }
            message->m_SETSessionID = 1001;
            CreateFailureResponse(message,Engine::INVALID_SESSION_ID);
            LOGD("SUPL: Engine::INVALID_SESSION_ID = %d", Engine::INVALID_SESSION_ID);
            m_MessageQueue.push(message);
            break;
        }
        if (message->m_pIncomingMessage->GetType() != Engine::SUPL_INIT)
        {
            session->ResetTimestamp();
            if (session->GetSessionStatus() == SESSION_INACTIVE)
            {
                delete message;
                break;
            }
        }

        if (message->m_pIncomingMessage->GetType() == Engine::SUPL_END)
        {
            FindRelMessageAndDelete(message->m_SETSessionID);
        }
        // mark as ready to process
        message->m_Status = Engine::MSG_READY_TO_PROCESS;
        // push back to message queue
        m_MessageQueue.push(message);
        LOGD("SUPL:After Decode Message \n");
        break;
    }
    case MSG_READY_TO_PROCESS:
    {
        LOGD("SUPLC:MSG_READY_TO_PROCESS");
        LOGD("Get session");
        session = GetSession(message);
        LOGD("After get session");
        if (session == NULL)
        {
            LOGD("session === NULL");
        }

        if (session == NULL && message->m_pIncomingMessage->GetType() == Engine::SUPL_INIT)
        {
            LOGD("MSG_READY_TO_PROCESS: SUPL_INIT");

            // New SUPL session
            session = new(std::nothrow) CSession();
            if (!session)
            {
                message->m_Status = Engine::MSG_FATAL_ERROR;
                m_MessageQueue.push(message);
                break;
            }
            if (!session->IsObjectReady())
            {
                message->m_Status = Engine::MSG_FATAL_ERROR;
                m_MessageQueue.push(message);
                break;
            }
            /* Read KEY before calling HashMessage. Else, "authSuplInitFailure"... */
            if(FALSE==genSLPIpAddress(m_SlpType))
            {
                LOGD("SLP Address Could not be resolved:");
                message->m_Status = Engine::MSG_FATAL_ERROR;
                m_MessageQueue.push(message);
                break;
            }
			uint16_t app_id=CSession::GenerateSessionId();
			LOGD("SUPLC:MSG_READY_TO_PROCESS Application Id = %d", app_id);
			app_id=1001;
            AddSession(session,app_id);
            message->m_SETSessionID = session->m_AppID;


#ifdef HASH_ENABLED
            session->m_pVer = new(nothrow) CBitString(64);
            if (!session->m_pVer)
            {
                message->m_Status = Engine::MSG_FATAL_ERROR;
                m_MessageQueue.push(message);
                break;
            }
            if (!session->m_pVer->IsObjectReady())
            {
                message->m_Status = Engine::MSG_FATAL_ERROR;
                m_MessageQueue.push(message);
                break;
            }
            HashMessage(message,&session->m_pVer);
#endif
        }
        if ( (NULL != session) &&
             (NULL != message->m_pIncomingMessage) &&
             (SUPL_INIT == message->m_pIncomingMessage->GetType()) &&
             (NULL != message->m_pOutgoingMessage) &&
             (SUPL_POS_INIT == message->m_pOutgoingMessage->GetType()) )
        {
            /* Memory for "session->m_pVer" already allocated during 1st ENCODE cycle of SUPL_POS_INIT */
            if (!session->m_pVer)
            {
                message->m_Status = Engine::MSG_FATAL_ERROR;
                m_MessageQueue.push(message);
                break;
            }

            if (!session->m_pVer->IsObjectReady())
            {
                message->m_Status = Engine::MSG_FATAL_ERROR;
                m_MessageQueue.push(message);
                break;
            }

            HashMessage(message,&session->m_pVer);
            /* Reset session->CurrentState
               Check "CSUPLPosInitAlgorithm::Response" for details..>!! */
            session->CurrentState = (MessageType)-1;
        }
        if (session == NULL)
        {
        	LOGD("Error: Session = NULL");
            if (message) delete message;
            break;
        }

        if (session->GetSessionStatus() == SESSION_INACTIVE)
        {
        	LOGD("Error: Session = SESSION_INACTIVE");
            delete message;
            break;
        }
        /* verify have message process successfully.
        (change status according to this om Engine::MSG_WAIT_FOR_DATA
        NOTE if we need data ALGORITM must set message status to Engine::MSG_WAIT_FOR_DATA. */
        LOGD("before ExecuteSession!");
        if (!session->ExecuteSession(message))
        {
            LOGD("SUPLC:MSG_READY_TO_PROCESS xxx MSG_FATAL_ERROR");
			message->m_Status = Engine::MSG_FATAL_ERROR;
            m_MessageQueue.push(message);
            break;
        }
        LOGD("After ExecuteSession!");
        // set new status.
        LOGD("m_pIncomingMessage = 0x%x", (Engine::uint32_t) message->m_pIncomingMessage);
        if (message->m_pIncomingMessage != NULL)
        {
            if (message->m_pIncomingMessage->GetType() == Engine::SUPL_END)
            {
                LOGD("MSG_READY_TO_PROCESS: SUPL_END");

                message->m_Status = Engine::MSG_READY_TO_DIE;
                session->SetSessionStatus(Engine::SESSION_INACTIVE);
                m_MessageQueue.push(message);
                break;
            }
            if (message->m_pIncomingMessage->GetType() == Engine::SUPL_INIT && !message->m_pOutgoingMessage)
            {
                //zz_Aby
                LOGD("MSG_READY_TO_PROCESS: SUPL_INIT.");

                if (session->m_Notification)
                {
#ifdef NOTIFICATIONS_ENABLED
                    LOGD("MSG_READY_TO_PROCESS: notificationType = %d", session->m_Notification->notificationType);
                    if (session->m_Notification->notificationType != noNotificationNoVerification &&
                            session->m_Notification->notificationType != privacyOverride)
                    {
                        if (session->m_Notification->notificationType == notificationOnly)
                        {
                        	//session->m_PendingMessage = message;
                        	HandleNotifications(session);
							message->m_Status = Engine::MSG_POSITIONING_GRANTED;
                    	}
						else
						{
							session->m_PendingMessage = message;
                        	HandleNotifications(session);
                        	message->m_Status = Engine::MSG_WAIT_REPLY_FROM_USER;
						}
                    }
                    else
                    {
                        if (session->m_Notification->notificationType == privacyOverride)
                        {
                            LOGD("MSG_READY_TO_PROCESS: privacyOverride");
                        }
                        else if (session->m_Notification->notificationType == noNotificationNoVerification)
                        {
                            LOGD("MSG_READY_TO_PROCESS: noNotificationNoVerification");
                        }
                        message->m_Status = Engine::MSG_SEND_TO_GPS_NI_REQUEST;
                    }
#else
					// Notifications Disabled - just send NI Request
                    LOGD("MSG_READY_TO_PROCESS: NOTIFICATIONS NOT ENABLED");
                    message->m_Status = Engine::MSG_SEND_TO_GPS_NI_REQUEST;
#endif
                }
                else
                {
                    LOGD("MSG_READY_TO_PROCESS: MSG_SEND_TO_GPS_NI_REQUEST");
                    message->m_Status = Engine::MSG_SEND_TO_GPS_NI_REQUEST;
                }
                m_MessageQueue.push(message);

                break;
            }
        }

        if (message->m_GPSData.m_pdu)
        {
            // we have data for gps.
            message->m_Status = Engine::MSG_SEND_TO_GPS_PAYLOAD;
        }
        else
        {
            LOGD("this is not supl pos or we already process supl pos(gps gave to us new rrlp");
            // this is not supl pos or we already process supl pos(gps gave to us new rrlp)
            message->m_Status = Engine::MSG_READY_TO_ENCODE;
        }

		// push back to message queue
        LOGD("Before push back to message queue");
        m_MessageQueue.push(message);
        //LOGD("SUPL:After Process Message | Status = %d\n", message->m_Status);
        break;

    }
    case MSG_READY_TO_ENCODE:
    {
        LOGD("SUPLC:MSG_READY_TO_ENCODE");
        Codec::SUCCESS_ERROR_T error;
        error = m_ULP_Processor->EncodeMessage(message);
        if (error!=Codec::SUCCESS)
            LOGD("SUPL: !!!!FATAL ERROR IN ENCODE FUNCTION!!!");
		else
            LOGD("Encode success!!!");
        session = GetSession(message);
        if(session)
        {
            LOGD(" ===>>> message = %x, message->m_pOutgoingMessage = %x \n", message, message->m_pOutgoingMessage);
            if (!message->m_pOutgoingMessage)
            {

                CreateFailureResponse(message);
                m_MessageQueue.push(message);
                break;
            }
        }
        if (error != Codec::SUCCESS)
        {
            if (message->m_pOutgoingMessage)
            {
                if (message->m_pOutgoingMessage->GetType() == SUPL_START || error == Codec::NO_MEMORY)
                {
                    message->m_Status = Engine::MSG_FATAL_ERROR;
                    m_MessageQueue.push(message);
                    break;
                }
                else
                {
                    CreateFailureResponse(message);
                    m_MessageQueue.push(message);
                    break;
                }
            }
            else
            {
                CreateFailureResponse(message);
                m_MessageQueue.push(message);
                break;
            }
        }

		McpInt ret= m_pNetwork->TLS_IsActive();
		if(ret==1)
        {
            message->m_Status = Engine::MSG_READY_TO_SEND;
            m_MessageQueue.push(message);
            LOGD("ProcessMessage:MSG_READY_TO_SEND");
        }
      	else  if (ret==0)
        {

	        LOGD(" ===>>> CheckForData: TLS Connection not active \n");
			//if(FALSE==genSLPIpAddress(m_SlpType))
			if(FALSE==genSLPIpAddress( readSlpType()))
			{
				LOGD("SLP Address Could not be resolved:");
	            message->m_Status = Engine::MSG_FATAL_ERROR;
	            m_MessageQueue.push(message);
				break;
			}


            LOGD(" ===>>> TLS_CreateConnection 1 \n");

            int8_t iRet = m_pNetwork->TLS_CreateConnection(m_ServerIPAdrress);
			LOGD("ProcessMessage:Connection iRet = %d",iRet);
            if (iRet != 0)
            {
                SESSLOGMSG("[%s]%s:%d, %s \n",get_utc_time(),SESS_ERR_SERVER_CONN_FAIL,nav_sess_getCount(0),"#Server connection Failure");
                LOGD("Connection problem:");
                message->m_Status = Engine::MSG_FATAL_ERROR;
                m_MessageQueue.push(message);
                break;
            }


			LOGD("ProcessMessage:Connection success");
				message->m_Status = Engine::MSG_WAIT_FOR_SERVER_CONNECTION;
            	m_MessageQueue.push(message);

        }
		else
		{
			message->m_Status = Engine::MSG_FATAL_ERROR;
            m_MessageQueue.push(message);
			LOGD("ProcessMessage:MSG_FATAL_ERROR");
		}

    }

	break;

    case MSG_READY_TO_SEND:
    {
		 LOGD("Try to send data");
		 session = GetSession(message);
        if(session)
        {
			 MessageType type = message->m_pOutgoingMessage->GetType();
	         if(type != Engine::SUPL_END)
	        {
	            LOGD("Encode success!!!: Calling SetTimestamp\n\r");
	            session->SetTimestamp();
		    }


			if(message->m_pOutgoingMessage->GetType() == SUPL_START)
			{
				SESSLOGMSG("[%s]%s:%d, %d,%d,%d,%d,%d %s \n",get_utc_time(),SESS_QOP,nav_sess_getCount(0),session->iQoP->m_Horacc,session->iQoP->m_pVeracc, 128,0,1,"#QOP hor_acc,ver_acc,max_res_time,num_fixes,time_btw_fixes ");
				 if(session->m_PrefMethod == AGPS_SET_ASSISTED_PREF)
			    {
			        SESSLOGMSG("[%s]%s:%d, %s %s\n",get_utc_time(),SESS_OTA_MSG_TYPE,nav_sess_getCount(0),"SUPL START","#SI MSA");
			    }
			    else if(session->m_PrefMethod == AGPS_SET_BASED_PREF)
			    {
			        SESSLOGMSG("[%s]%s:%d, %s %s\n",get_utc_time(),SESS_OTA_MSG_TYPE,nav_sess_getCount(0),"SUPL START","#SI MSB");
			    }else if(session->m_PrefMethod == NO_PREF)
			    {
			        SESSLOGMSG("[%s]%s:%d, %s %s\n",get_utc_time(),SESS_OTA_MSG_TYPE,nav_sess_getCount(0),"SUPL START","#SI AUT/Cell-Id");
			    }
			}
		}

        int iRet = m_pNetwork->TLS_Send(message->m_pOutgoingRawData, message->m_OutgoingRawDataSize);
        if (iRet != 0)
        {
        	SESSLOGMSG("[%s]%s:%d, %s %s \n",get_utc_time(),SESS_ERR_NW_CONN_FAIL,nav_sess_getCount(0)," ","#Network Connection Failure");
            LOGD(" TLS_Send returned ERROR !!! \n ");
            message->m_Status = Engine::MSG_FATAL_ERROR;
            m_MessageQueue.push(message);
            break;
        }

        if ( (session == NULL) &&
             (NULL != message->m_pIncomingMessage) &&
             (message->m_pIncomingMessage->GetType() != Engine::SUPL_INIT) )
        {
            debugMessage(" ===>>> session is NULL. m_pIncomingMessage != SUPL_INIT");
            message->m_SETSessionID = 1001;
            message->m_pIncomingMessage->m_SessionID.iSETSessionID->sessionId = 1001;
        }
        message->m_Status = Engine::MSG_READY_TO_DIE;
        // push back to message queue
        m_MessageQueue.push(message);

    }
	break;


    case MSG_READY_TO_DIE:
    {
        LOGD("+SUPLC:MSG_READY_TO_DIE");

		session = GetSession(message);
        LOGD("SUPLC:MSG_READY_TO_DIE GetSession... session = 0x%x", (Engine::uint32_t) session);
        if (session == NULL)
        {
            LOGD("SUPLC:MSG_READY_TO_DIE session == NULL");
            if (m_SessionQueue.isEmpty()  && m_MessageQueue.empty())
            {
                //LOGD("m_pNetwork->TLS_CloseConnection(): Try to close connection....");
                if( m_pNetwork->TLS_IsActive())
                	m_pNetwork->TLS_CloseConnection();
                LOGD("SUPLC:MSG_READY_TO_DIE: Close connection OK....");

				m_IsSessionInProgress = FALSE;
            }
            delete message;

			/* Check if any new New Session/Message has been queued */
	    	if(!m_NewSessionMsgQueue.empty())
	   	 	{
			    LOGD("DIE: New Session Message Queue Size: %d\n", m_NewSessionMsgQueue.size());
				LOGD("DIE: Get the pending message and push it to message queue\n");

				/* Get the pending message from the queue */
				message = m_NewSessionMsgQueue.front();
				m_NewSessionMsgQueue.pop();

				/* Push it to the message queue to start with the processing */
				m_MessageQueue.push(message);

	    	}

            /* send UPL_STOP_IND for Stop Request */
            CGPSCommand* cmd = 0;
            cmd = CGPSCommand::CreateCommand(UPL_STOP_IND, 1001, /*TBD: get app id*/
                                             NULL, NULL, NULL);
            if (cmd)
            {
                cmd->Execute();
                delete cmd;
            }

            break;
        }


        if (session->GetSessionStatus() == SESSION_INACTIVE)
        {
            LOGD("SUPLC:MSG_READY_TO_DIE - SESSION_INACTIVE");
            CGPSCommand* cmd = 0;

            if (message->m_pIncomingMessage == NULL)
            {
                LOGD("SUPLC:MSG_READY_TO_DIE message->m_pIncomingMessage == NULL");
				if(message->m_pOutgoingMessage)
				{
					if(message->m_pOutgoingMessage->GetType() == Engine::SUPL_END)
					{
						CSUPLEnd* end = (CSUPLEnd*) message->m_pOutgoingMessage;
						if (end->m_pStatusCode == NULL)
						{
							/* Normal end */
							LOGD("SUPLC:Normal SUPL END in Client");
							cmd = CGPSCommand::CreateCommand(UPL_STOP_IND, session->GetSessionUID(),
															 session, NULL, &code);
						}
						else
						{
							if(*end->m_pStatusCode == 0)
							{
								/* Normal end */
								LOGD("SUPLC:Normal SUPL END in Client");
	                    		cmd = CGPSCommand::CreateCommand(UPL_STOP_IND, session->GetSessionUID(),
	                                                     session, NULL, &code);
							}
							else
							{
								/* Failure case */
								LOGD("SUPLC: SUPL END on failure in Client");
	                    		cmd = CGPSCommand::CreateCommand(UPL_STOP_IND, session->GetSessionUID(),
	                                                     session, NULL, NULL);
							}
						}
					}
				}
				else
				{
                	cmd = CGPSCommand::CreateCommand(UPL_STOP_IND, session->GetSessionUID(),
                                                 session, NULL, NULL);
				}
            }
			//zz_Aby
			/* NI Changes */
            else if (message->m_pIncomingMessage->GetType() == Engine::SUPL_END)
            {
                LOGD("SUPLC:MSG_READY_TO_DIE m_pIncomingMessage Type = SUPL_END");

                CSUPLEnd* end = (CSUPLEnd*) message->m_pIncomingMessage;

				if (end->m_pPosition != NULL)
                {
                    LOGD("SUPLC:MSG_READY_TO_DIE Incoming m_pPosition != NULL");
                    LOGD("SUPLC:MSG_READY_TO_DIE CreateCommand UPL_POS_IND");
                    cmd = CGPSCommand::CreateCommand(UPL_POS_IND, session->GetSessionUID(),
                                                     session, message, end->m_pPosition);
                    end->m_pPosition = NULL;
                }
                else if (end->m_pStatusCode == NULL)
                {
                    LOGD("SUPLC:MSG_READY_TO_DIE Incoming m_pStatusCode = NULL");
                    LOGD("SUPLC:MSG_READY_TO_DIE CreateCommand UPL_STOP_IND");
                    cmd = CGPSCommand::CreateCommand(UPL_STOP_IND, session->GetSessionUID(),
                                                     session, NULL, &code);
                }
                else
                {
                	if(*end->m_pStatusCode == 0)
                	{
                		LOGD("SUPLC: Normal SUPL END from network");
						cmd = CGPSCommand::CreateCommand(UPL_STOP_IND, session->GetSessionUID(),
														 session, NULL, &code);
                	}
                	else
                	{
                		LOGD("SUPLC: SUPL END on failure from network");
                    LOGD("SUPLC:MSG_READY_TO_DIE - CreateCommand UPL_STOP_IND");
                    cmd = CGPSCommand::CreateCommand(UPL_STOP_IND, session->GetSessionUID(),
                                                     session, NULL, NULL);
                }
                }

            }
            else
            {
                LOGD("SUPLC:MSG_READY_TO_DIE m_pIncomingMessage Type != SUPL_END");
                cmd = CGPSCommand::CreateCommand(UPL_STOP_IND, session->GetSessionUID(),
                                                 session, NULL, NULL);
            }

            if (cmd)
            {
                LOGD("SUPLC:MSG_READY_TO_DIE cmd Execute");
                cmd->Execute();
                delete cmd;
            }
            else
            {
                LOGD("SUPLC:MSG_READY_TO_DIE Status = MSG_FATAL_ERROR");
                message->m_Status = Engine::MSG_FATAL_ERROR;
                m_MessageQueue.push(message);
                break;
            }

            FindRelMessageAndDelete(session->GetSessionUID());

            LOGD("SUPLC:MSG_READY_TO_DIE try to delete session....");
            // delete session from map
            //m_SessionQueue.erase(message->m_SETSessionID);
            m_SessionQueue.removeItem(message->m_SETSessionID); // Android STL

			m_IsSessionInProgress = FALSE;

			// erase session
            delete session;
			delete message;
			message=NULL;

            if (m_SessionQueue.isEmpty() && m_MessageQueue.empty()) // Android STL
            {
                LOGD("SUPLC:MSG_READY_TO_DIE -> TLS_CloseConnection()");
				if( m_pNetwork->TLS_IsActive())
               		 m_pNetwork->TLS_CloseConnection();
                LOGD("SUPLC:MSG_READY_TO_DIE <- TLS_CloseConnection()");
            }

			//LOGD("DIE: TLS Connection activity: %d\n", m_pNetwork->TLS_IsActive());
            LOGD("DIE: Session Queue Size: %d\n", m_SessionQueue.size());
            LOGD("DIE: Message Queue Size: %d\n", m_MessageQueue.size());
			LOGD("SUPLC:MSG_READY_TO_DIE - delete message");

			/* Check if any new New Session/Message has been queued */
	    	if(!m_NewSessionMsgQueue.empty())
	   	 	{
			    LOGD("DIE: New Session Message Queue Size: %d\n", m_NewSessionMsgQueue.size());
				LOGD("DIE: Get the pending message and push it to message queue\n");

				/* Get the pending message from the queue */
				message = m_NewSessionMsgQueue.front();
				m_NewSessionMsgQueue.pop();

				/* Push it to the message queue to start with the processing */
				m_MessageQueue.push(message);

	    	}


	    	break;
        }



        LOGD("DIE: Session Queue Size: %d\n", m_SessionQueue.size());
        LOGD("DIE: Message Queue Size: %d\n", m_MessageQueue.size());
        delete message;
        message=NULL;
        LOGD("SUPLC:MSG_READY_TO_DIE - delete message");
        break;
    }
    case MSG_WAIT_REPLY_FROM_GPS:
    {
        m_MessageQueue.push(message);
        break;
    }
    case MSG_WAIT_REPLY_FROM_USER:
    {
        m_MessageQueue.push(message);
        //LOGD("ProcessMessage: Queue.push MSG_WAIT_REPLY_FROM_USER");
        break;
    }
    case MSG_POSITIONING_GRANTED:
    {
        LOGD("SUPLC:MSG_POSITIONING_GRANTED");
        session = GetSession(message);
        if (session == NULL)
        {
            message->m_Status = MSG_FATAL_ERROR;
            break;
        }
        LOGD("MSG_POSITIONING_GRANTED: session != NULL");
        if (session->m_UsedPosTechnology == Engine::POS_METHOD_NO_POSITION)
        {
            LOGD("MSG_POSITIONING_GRANTED: m_UsedPosTechnology == Engine::POS_METHOD_NO_POSITION");
            if (session->m_Notification->notificationType == Engine::notificationOnly)
            {
                LOGD("MSG_POSITIONING_GRANTED: notificationType == Engine::notificationOnly");
                StopSession(session->GetSessionUID(),(Engine::StatusCode)-1);
            }
            else
            {
                StopSession(session->GetSessionUID(),Engine::CONSENT_GRANTED_BY_USER);
            }
        }
        else
        {
            LOGD("MSG_POSITIONING_GRANTED: m_Status = Engine::MSG_SEND_TO_GPS_NI_REQUEST");
            message->m_Status = Engine::MSG_SEND_TO_GPS_NI_REQUEST;
			LOGD("MSG_POSITIONING_GRANTED: put message to Queue...");
	        m_MessageQueue.push(message);
        }
        
        break;
    }


    case MSG_POSITIONING_DENIED:
    {
        LOGD("SUPLC:MSG_POSITIONING_DENIED");
        session = GetSession(message);
        if (session == NULL)
        {
            message->m_Status = MSG_FATAL_ERROR;
            break;
        }
        if (session->m_UsedPosTechnology == Engine::POS_METHOD_NO_POSITION)
        {
            StopSession(session->GetSessionUID(),Engine::CONSENT_DENIED_BY_USER);
        }
        else
        {
            //StopSession(session->GetSessionUID(),Engine::POSITIONING_NOT_PERMITTED);
            LOGD("SUPLC:CONSENT_DENIED_BY_USER");
            StopSession(session->GetSessionUID(),Engine::CONSENT_DENIED_BY_USER);
        }
        //m_MessageQueue.push(message);
        break;
    }
    case MSG_SEND_NOTIFY_CMD:
    {
        LOGD("SUPLC:MSG_SEND_NOTIFY_CMD");
        Platform::CGPSCommand * cmd;

        session = GetSession(message);
        if (session == NULL)
        {
            message->m_Status = MSG_FATAL_ERROR;
            break;
        }

        cmd = Platform::CGPSCommand::CreateCommand(UPL_NOTIFY_IND, message->m_SETSessionID, session,
                message,0);
        if (cmd)
        {
            cmd->Execute();
            delete cmd;
        }
        else
        {
            message->m_Status = Engine::MSG_FATAL_ERROR;
            m_MessageQueue.push(message);
            break;
        }
        message->m_Status = MSG_WAIT_REPLY_FROM_GPS;
        m_MessageQueue.push(message);
        break;
    }
    case MSG_SEND_TO_GPS_NI_REQUEST:
    {
        LOGD("SUPLC:MSG_SEND_TO_GPS_NI_REQUEST");
        Platform::CGPSCommand * cmd;

        session = GetSession(message);
        if (session == NULL)
        {
            message->m_Status = MSG_FATAL_ERROR;
            m_MessageQueue.push(message);
            break;
        }



        cmd = Platform::CGPSCommand::CreateCommand(UPL_START_LOC_IND, message->m_SETSessionID, session,
                message,0);
        if (cmd)
        {
            cmd->Execute();
            delete cmd;
        }
        else
        {
            message->m_Status = Engine::MSG_FATAL_ERROR;
            m_MessageQueue.push(message);
            break;
        }


        message->m_Status = MSG_WAIT_REPLY_FROM_GPS;
        m_MessageQueue.push(message);
        break;
    }
    case MSG_SEND_TO_GPS_PAYLOAD:
    {
        LOGD("SUPLC:MSG_SEND_TO_GPS_PAYLOAD");
        Platform::CGPSCommand * cmd;

        session = GetSession(message);

        if (session == NULL)
        {
            delete message;
            break;
        }

        if (session->m_GPSResult == CSession::FAILURE)
        {
            message->m_Status = Engine::MSG_READY_TO_DIE;
            break;
        }

        cmd = Platform::CGPSCommand::CreateCommand(UPL_DATA_IND, message->m_SETSessionID, session,
                message, &message->m_GPSData);
        if (cmd)
        {
            cmd->Execute();
            delete cmd;
        }
        else
        {
            message->m_Status = Engine::MSG_FATAL_ERROR;
            m_MessageQueue.push(message);
            break;
        }

        message->m_Status = Engine::MSG_WAIT_REPLY_FROM_GPS;
        session->m_PendingMessage = message;
        m_MessageQueue.push(message);
        LOGD("SUPL: After MSG_SEND_TO_GPS_PAYLOAD state");
        break;
    }
    case MSG_FATAL_ERROR:
    {
        LOGD("SUPLC:MSG_FATAL_ERROR");
        CGPSCommand* cmd = 0;
        uint32_t AppId = message->m_SETSessionID;
        delete message;

        session = GetSession(AppId);
        if (session)
        {
            if (session->m_GpsSessionCreated == TRUE)
            {
                cmd = CGPSCommand::CreateCommand(UPL_STOP_IND, AppId, 0, 0, 0);
            }

            m_SessionQueue.removeItem(AppId);
            delete session;
        }
        FindRelMessageAndDelete(AppId);

        if (cmd)
        {
            cmd->Execute();
            delete cmd;
        }

	    if(!m_NewSessionMsgQueue.empty())
	   	{
		    LOGD("MSG_FATAL_ERROR: New Session Message Queue Size: %d\n", m_NewSessionMsgQueue.size());
			LOGD("MSG_FATAL_ERROR: Get the pending message and push it to message queue\n");

			/* Get the pending message from the queue */
			message = m_NewSessionMsgQueue.front();
			m_NewSessionMsgQueue.pop();

			/* Push it to the message queue to start with the processing */
			m_MessageQueue.push(message);
	    }

        if (m_SessionQueue.isEmpty()  && m_MessageQueue.empty())
        {
            if (m_pNetwork->TLS_IsActive())
            {
                m_pNetwork->TLS_CloseConnection();
            }
        }

		/* Reset the m_IsSessionInProgress flag */
		m_IsSessionInProgress = FALSE;

#ifdef NDEBUG
        LOGD("FATAL: TLS Connection activity: %d\n", m_pNetwork->TLS_IsActive());
        LOGD("FATAL: Session Queue Size: %d\n", m_SessionQueue.size());
        LOGD("FATAL: Message Queue Size: %d\n", m_MessageQueue.size());
#endif
        LOGD("SUPL: After MSG_FATAL_ERROR state");
        break;
    }
	case MSG_WAIT_FOR_SERVER_CONNECTION:
    {
		//LOGD("processmessage:MSG_WAIT_FOR_SERVER_CONNECTION");
		m_MessageQueue.push(message);
		break;
	}

	case MSG_RETRY_SERVER_CONNECTION:
    {
		LOGD("processmessage:MSG_RETRY_SERVER_CONNECTION");

		if(FALSE==genSLPIpAddress(m_SlpType))
		{
			LOGD("SLP Address Could not be resolved:");
            message->m_Status = Engine::MSG_FATAL_ERROR;
            m_MessageQueue.push(message);
			break;
		}

        LOGD(" ===>>> TLS_CreateConnection 1 \n");

        int8_t iRet = m_pNetwork->TLS_CreateConnection(m_ServerIPAdrress);
		LOGD("ProcessMessage:Connection iRet = %d",iRet);
        if (iRet != 0)
        {
            SESSLOGMSG("[%s]%s:%d, %s \n",get_utc_time(),SESS_ERR_SERVER_CONN_FAIL,nav_sess_getCount(0),"#Server connection Failure");
            LOGD("Connection problem:");
            message->m_Status = Engine::MSG_FATAL_ERROR;
            m_MessageQueue.push(message);
            break;
        }

		message->m_Status = Engine::MSG_WAIT_FOR_SERVER_CONNECTION;
		m_MessageQueue.push(message);
	}
    break;

    default:
    {
        LOGD("SUPLC:UNKNOWN_OPCODE");
        // this is stub for error handlers.
        //throw;
    }
    }

    /* Release semaphore for SUPL_Core to process this message. */
    //zz_Aby_c LOGD(" %s: Calling SUPLC_releaseSemaphore \n", __FUNCTION__);
    if( (message) &&
        (message->m_Status!=MSG_WAIT_FOR_SERVER_CONNECTION)&&
        (message->m_Status!=MSG_RETRY_SERVER_CONNECTION) /* &&
        (message->m_Status!=MSG_WAIT_REPLY_FROM_GPS) &&
        (message->m_Status!=MSG_WAIT_REPLY_FROM_USER)*/ )
 		{
		    if ( SUPLC_releaseSemaphore(gp_semHandle) < 0 )
		    {
		        LOGD(" %s: sem_post FAILED !!! \n", __FUNCTION__);
		    }
 		}

}

void CSUPLController::HandleProtocolErrors(MSG* msg, Codec::SUCCESS_ERROR_T error , CSessionID& aSessID)
{
    CSession* session = 0;
    Engine::StatusCode code;

    switch (error)
    {
    case Codec::MAN_PARAM_IS_ABSENT:
    {
        code = DATA_MISSING;
        break;
    }
    case Codec::INVALID_PARAMETER:
    {
        code = UNEXPECTED_DATA_VALUE;
	SESSLOGMSG("[%s]%s:%d, %s %s \n",get_utc_time(),SESS_WARN_UNKWN_OTA_MSG,nav_sess_getCount(0)," ","#Unknown OTA message received");
        break;
    }
    case Codec::DECODE_ERROR:
    {
        code = PROTOCOL_ERROR;
	SESSLOGMSG("[%s]%s:%d, %s %s \n",get_utc_time(),SESS_ERR_DECODE_OTA_MSG,nav_sess_getCount(0)," ","#OTA message decoding error");
        break;
    }
	case Codec::UNEXPECTED_MESSAGE:
    {
        code = UNEXPECTED_MESSAGE;
		SESSLOGMSG("[%s]%s:%d, %s \n",get_utc_time(),SESS_ERR_UNEXP_OTA_MSG,nav_sess_getCount(0),"Unexpected OTA message received. FSM out-of-sync");
        break;
    }
    case Codec::PARSE_ERROR:
    {
        code = SYSTEM_FAILURE;
        break;
    }
    case Codec::VER_NOT_SUPPORTED:
    {
        code = VERSION_NOT_SUPPORTED;
        break;
    }
    case Codec::INVALID_SESSION_ID:
    {
        code = INVALID_SESSION_ID;
        break;
    }
    default:
    {
        code = SYSTEM_FAILURE;
        break;
    }
    }

    if (msg)
    {
        CBitString* Ver = NULL;
        if (msg->m_pIncomingMessage)
        {
        	LOGD("HandleProtocolErrors: m_pIncomingMessage");
            if (msg->m_pIncomingMessage->GetType() == SUPL_INIT)
            {
            	LOGD("HandleProtocolErrors: SUPL_INIT");
                if (!HashMessage(msg,&Ver))
                {
                	LOGD("HandleProtocolErrors: HashMessage Err");
                    return;
                }
            }
            Engine::bool_t ret = CSUPLEndAlgorithm::PrepareErrorMessage(msg,msg->m_pIncomingMessage->m_SessionID,code,&Ver);
            if (!ret)
            {
                LOGD("CSUPLController::HandleProtocolErrors code = %d: ret = %d", code, ret);
                msg->m_Status = MSG_FATAL_ERROR;
                return;
            }
        }
        else //if(code == PROTOCOL_ERROR)
        {
        	LOGD("HandleProtocolErrors: PROTOCOL_ERROR");
            if(!HashMessage(msg,&Ver))
            {
               	LOGD("HandleProtocolErrors: HashMessage Err");
                return;
            }

			Engine::bool_t ret = CSUPLEndAlgorithm::PrepareErrorMessage(msg,aSessID,code,&Ver);
            if (!ret)
            {
                LOGD("code == PROTOCOL_ERROR: ret = %d", ret);
                msg->m_Status = MSG_FATAL_ERROR;
                return;
            }
        }


    }
}

void CSUPLController::CreateFailureResponse(MSG* msg, Engine::StatusCode code)
{
    CSession* session=GetSession(msg);
    if (session)
    {
//      m_SessionQueue.erase(msg->m_SETSessionID);
//      delete session;
        session->SetSessionStatus(SESSION_INACTIVE);
    }

    delete msg->m_pOutgoingMessage;
    msg->m_pOutgoingMessage = NULL;

    Engine::CSUPLEndAlgorithm::PrepareErrorMessage(msg,msg->m_pIncomingMessage->m_SessionID,code);
}

void CSUPLController::StopSession(uint16_t appId,Engine::StatusCode status_code)
{
    CSession* session=GetSession(appId);
    MSG* msg = 0;
	CGPSCommand* cmd = 0;

    FindRelMessageAndDelete(appId);
	LOGD(" SUPLController.cpp: StopSession appid: %d,status_code=%d entering \n",appId,status_code);

    if (session)
    {
		LOGD(" SUPLController.cpp: StopSession first if is true\n");
       // m_SessionQueue.removeItem(appId);
		LOGD(" SUPLController.cpp: StopSession appid: %d after m_SessionQueue.removeItem(appId) \n",appId);
        if (session->iSessionID.iSLPSessionID)
        {
            Engine::MSG*  end_msg = new(std::nothrow)  Engine::MSG;
            if (!end_msg)
            {
            	LOGD(" SUPLController.cpp: StopSession: !end_msg true\n");
                delete session;
                return;
            }

			if(status_code!=Engine::UNSPECIFIED)
			{

	            end_msg->m_SETSessionID = appId;
				LOGD(" SUPLController.cpp: StopSession end_msg->m_SETSessionID: %d  \n",end_msg->m_SETSessionID);
	            Engine::CSUPLEndAlgorithm::PrepareErrorMessage(end_msg,session->iSessionID,status_code,&session->m_pVer);
				LOGD(" SUPLController.cpp: StopSession after PrepareErrorMessage ");
				m_MessageQueue.push(end_msg);

        	}
			else // status code = unspecified
			{
				if(m_pNetwork->TLS_IsActive())
				{
					end_msg->m_SETSessionID = appId;
					LOGD(" SUPLController.cpp: TLS is Active So sending End message: %d  \n",end_msg->m_SETSessionID);
					Engine::CSUPLEndAlgorithm::PrepareErrorMessage(end_msg,session->iSessionID,status_code,&session->m_pVer);
					LOGD(" SUPLController.cpp: StopSession after PrepareErrorMessage ");
					m_MessageQueue.push(end_msg);
				}
				else
				{
					LOGD(" SUPLController.cpp: Connection Not active..SUPL End is not sent..deleting session ");
					m_SessionQueue.removeItem(appId);
					delete session;
					delete end_msg;
					end_msg = NULL;
					/* send UPL_STOP_IND for Stop Request */

					cmd = CGPSCommand::CreateCommand(UPL_STOP_IND,
					 appId,
					 NULL,
					 NULL,
					 NULL);
					if (cmd)
					{
						cmd->Execute();
						delete cmd;
					}

				}
			}
        }
		else
		{

			LOGD(" SUPLController.cpp: Session not started..Stopping wait connection ");
			m_SessionQueue.removeItem(appId);
			if (m_SessionQueue.isEmpty()  && m_MessageQueue.empty())
            {
                m_pNetwork->TLS_CloseConnection();
                LOGD("SUPLController.cpp: StopSession :: Close connection OK....");
            }

            /* send UPL_STOP_IND for Stop Request */

            cmd = CGPSCommand::CreateCommand(UPL_STOP_IND,
             appId,
             NULL,
             NULL,
             NULL);
            if (cmd)
            {
                cmd->Execute();
                delete cmd;
            }
        /* Release semaphore for SUPL_Core to process this message. */
        LOGD(" %s: Calling SUPLC_releaseSemaphore \n", __FUNCTION__);
        if ( SUPLC_releaseSemaphore(gp_semHandle) < 0 )
        {
            LOGD(" %s: sem_post FAILED !!! \n", __FUNCTION__);
        }
        delete session;
    	}
	}
}
void CSUPLController::CheckSessionsForTimeout()
{
    SessionIter iter ;
	int32_t tDiff;

    /*Check that there is no timeout for session*/
    for (unsigned int i = 0; i < m_SessionQueue.size(); ++i)
    {
        iter = m_SessionQueue.valueAt(i);
        //Waiting message from slp server
        if (iter->GetTimestamp() != -1)
        {
        	tDiff = iter->GetTimeDiff();
            if(tDiff >= UT1234_TIME_OUT_VALUE)
            {
            	LOGD("[TIME]: Timout at %d", tDiff);
                uint16_t AppId = iter->GetSessionUID();
                CBitString* Ver = NULL;

                int n = iter->GetTimeDiff();
                iter->SetSessionStatus(SESSION_INACTIVE);
                iter->ResetTimestamp();

                FindRelMessageAndDelete(AppId);

                MSG*  end_msg = new(std::nothrow)  MSG;
                if (!end_msg)
                {
                    m_SessionQueue.removeItem(m_SessionQueue.keyAt(i));
                    i--;
                    delete iter;

                    CGPSCommand* cmd = 0;
                    cmd = CGPSCommand::CreateCommand(UPL_STOP_IND, AppId,
                                                     NULL, NULL, NULL);
                    if (cmd)
                    {
                        cmd->Execute();
                        delete cmd;
                    }

                    return;
                }
                end_msg->m_SETSessionID = AppId;
				SESSLOGMSG("[%s]%s:%d, %s \n",get_utc_time(),SESS_ERR_OTA_MSG_TIME_OUT,nav_sess_getCount(0),"#Time-out for OTA message (SUPL message)");
                Engine::CSUPLEndAlgorithm::PrepareErrorMessage(end_msg,iter->iSessionID,Engine::SYSTEM_FAILURE);
                m_MessageQueue.push(end_msg);

                /* Release semaphore for SUPL_Core to process this message. */
                LOGD(" %s: Calling SUPLC_releaseSemaphore \n", __FUNCTION__);
                if ( SUPLC_releaseSemaphore(gp_semHandle) < 0 )
                {
                    LOGD(" %s: sem_post FAILED !!! \n", __FUNCTION__);
                }

            }
        }
    }
}

/*
    @breif Wait for data from GPS or net.

 */

void CSUPLController::CheckForData()
{
    Engine::MSG*    message = NULL;
    CNetworkComponentBase::RawData_t net_data;
	uint32_t i;

    /*
        poll network data
    */
    if (m_pNetwork->CMN_IsDataExist(&net_data))
    {
        LOGD(" ===>>> CheckForData: Got network message. \n");
        message = new(std::nothrow) Engine::MSG();
        if (!message)
        {
            // session will be deleted in timeout handler
            return;
        }
        // copy data to internal buffer
        message->m_pIncomingRawData = new(std::nothrow) uint8_t[net_data.size];
        if (!message->m_pIncomingRawData)
        {
			delete message;
			message = NULL;
            return;
        }
        memcpy(message->m_pIncomingRawData,net_data.RawData,net_data.size);
        message->m_IncomingRawDataSize = net_data.size;
        message->m_Status = Engine::MSG_READY_TO_DECODE;


        m_MessageQueue.push(message);

        /* Release semaphore for SUPL_Core to process this message. */
        LOGD(" %s: Calling SUPLC_releaseSemaphore \n", __FUNCTION__);
        if ( SUPLC_releaseSemaphore(gp_semHandle) < 0 )
        {
            LOGD(" %s: sem_post FAILED !!! \n", __FUNCTION__);
        }

    }

    /*
        Poll GPS device
    */

    if (m_pGPSDevice->DataReady())
    {
        CSession* session = 0;
        uint16_t app_id = 0;
        Platform::CGPSCommand* cmd = m_pGPSDevice->GetCommand();
        LOGD(" ===>>> CheckForData: Got message from GPS_HSW. \n");
        // get session uid
        app_id = cmd->GetAppID();
        session = GetSession(app_id);

        // Added for NI
        //message = m_MessageQueue.front();
        //session = GetSession(message);


        if (cmd->GetCmdCode() == UPL_START_REQ)
        {
            LOGD(" ===>>> CheckForData: UPL_START_REQ Received \n");
            if (!session)
            {
            	if(m_IsSessionInProgress == TRUE)
				{
					/* Handling of corner case when SI start immediately follows
					  * NI SUPL_INIT message decoding
					  */
					if(!m_MessageQueue.empty())
					{
						/* Get the pending message from the queue */
						message = m_MessageQueue.front();

						if(message->m_Status == Engine::MSG_READY_TO_PROCESS)
						{
							if(message->m_pIncomingMessage->GetType() == Engine::SUPL_INIT)
							{
								LOGD(" ===>>> CheckForData: NI Session Start was Received \n");
								m_MessageQueue.pop();
								/* Push it to the message queue to start with the processing */
								m_NewSessionMsgQueue.push(message);
							}
						}
					}
            	}

			    /* Session start added here. */
				LOGD(" ===>>> CheckForData: Start of new SI session identified here. \n");
				m_IsSessionInProgress = TRUE;

                // new session & new message is SUPL_START
                session = new(std::nothrow) CSession();
                message = new(std::nothrow) Engine::MSG();
                if (!session || !message)
                {
                    delete message;
                    delete session;
                    delete cmd;
                    cmd = NULL;

                    cmd = CGPSCommand::CreateCommand(UPL_STOP_IND, app_id, NULL, NULL, NULL);
                    cmd->Execute();
                    delete cmd;
                    cmd = NULL;

                    return;
                }
                if (!session->IsObjectReady())
                {
                    delete message;
                    delete session;
                    delete cmd;
                    cmd = NULL;

                    cmd = CGPSCommand::CreateCommand(UPL_STOP_IND, app_id,NULL, NULL, NULL);
                    cmd->Execute();
                    delete cmd;
                    cmd = NULL;

                    return;
                }
                session->m_AppID = app_id;
                LOGD(" ===>>> CheckForData: New Session & Message created. \n");
            }
            else
            {
                message = session->m_PendingMessage;
                LOGD(" ===>>> CheckForData: Session was already created. \n");
            }


            message->m_SETSessionID = app_id;

            cmd->SetSession(session);
            cmd->SetMessage(message);
            //Execute command
            if (cmd->Execute() != TRUE)
            {
                LOGD(" ===>>> CheckForData: UPL_START_REQ cmd execution FAILED !!! \n");
                if (session->m_PendingMessage)
                {
                    m_SessionQueue.removeItem(session->m_AppID);
                    FindRelMessageAndDelete(message->m_SETSessionID);
                }
                else
                {
                    delete message;
                }
                delete session;
                delete cmd;
                cmd = NULL;
                cmd = CGPSCommand::CreateCommand(UPL_STOP_IND, app_id,NULL, NULL, NULL);
                cmd->Execute();
                delete cmd;
                cmd = NULL;

                return;
            }


			message->m_Status = Engine::MSG_READY_TO_PROCESS;

            LOGD("SUPL:Before SUPL_START execution");
	   		SESSLOGMSG("[%s]%s:%d, %s \n",get_utc_time(),SESS_SERVER_CONN_SUCCESS,nav_sess_getCount(0),"#Server Connection Success");
            if (!session->m_PendingMessage)
            {
                // store message in message queue and session in session map
                m_MessageQueue.push(message);
                int ind = 0;
                if ((ind = m_SessionQueue.indexOfKey(app_id)) >= 0)
                    m_SessionQueue.editValueAt(app_id) = session;
                else
                    m_SessionQueue.add(app_id, session);

            }

            session->m_GpsSessionCreated = TRUE;

            /* Release semaphore for SUPL_Core to process this message. */
            LOGD(" %s: Calling SUPLC_releaseSemaphore \n", __FUNCTION__);
            if ( SUPLC_releaseSemaphore(gp_semHandle) < 0 )
            {
                LOGD(" %s: sem_post FAILED !!! \n", __FUNCTION__);
            }
        }
        else if (cmd->GetCmdCode() == UPL_DATA_REQ)
        {
            LOGD("cmd->GetCmdCode() == UPL_DATA_REQ");
            if (session)
            {
                if (cmd->Execute() != TRUE)
                {
                    Engine::CSUPLController::GetInstance().CreateFailureResponse(session->m_PendingMessage);
                    delete cmd;
                    cmd = NULL;
                    cmd = CGPSCommand::CreateCommand(UPL_STOP_IND, app_id,NULL, NULL, NULL);
                    cmd->Execute();
                    delete cmd;
                    cmd = NULL;
                }
            }
        }
        else if (cmd->GetCmdCode() == UPL_STOP_REQ)
        {
            if (session)
            {
                cmd->Execute();
				LOGD("cmd->GetCmdCode() == UPL_STOP_REQ after cmd->Execute()");
            }
			else
			{
				LOGD("UPL_STOP_REQ Session Didnt Start, Send Stop Ind");
				/* send UPL_STOP_IND for Stop Request */
	            delete cmd;
				cmd = NULL;
	            cmd = CGPSCommand::CreateCommand(UPL_STOP_IND,
	             app_id,
	             NULL,
	             NULL,
	             NULL);
	            if (cmd)
	            {
	                cmd->Execute();
	                delete cmd;
					cmd = NULL;
	            }
			}
        }
        else if (cmd->GetCmdCode() == UPL_NOTIFY_RESP)
        {
            if (session)
            {
                cmd->Execute();
            }
        }
		else
		{
			delete cmd;
			cmd = NULL;
		}

		if(cmd)
		{
        	LOGD(" ===>>> CheckForData: Delete command and return \n");
			delete cmd;
			cmd = NULL;
		}
    return;
    }
}

/*
*******************************************************************
* Function: GetSession
*
* Description : Function that obtain session from session list or
* creates new one. If the session is new one, add it to message
* queue.
*
* Parameters :
* sessionid r session id.
* Returns : Pointer to CSession class object
*
*******************************************************************
*/

CSession* CSUPLController::GetSession(Engine::uint32_t sessionid)
{
    LOGD("CSUPLController::GetSession(sessionid) begin");
    CSession* session = NULL;
    int index = -1;

    SessionIter iter;
    index = m_SessionQueue.indexOfKey(sessionid);
    LOGD("CSUPLController::GetSession index = %d", index);
    if (index < 0)
    {
        session = NULL;
    }
    else
    {
        session = m_SessionQueue.valueFor(sessionid);
    }

    LOGD("CSUPLController::GetSession session = 0x%x, sessionid = %d", (Engine::uint32_t) session, sessionid);
    return session;
}

CSession* CSUPLController::GetSession(Engine::MSG* message)
{
    LOGD("CSUPLController::GetSession(message) begin");
    CSession* session = NULL;

	if (message == NULL)
	{
        LOGD("CSUPLController::GetSession: message NULL");
		return session;
	}

    int index = -1;


    SessionIter iter;
    index = m_SessionQueue.indexOfKey(message->m_SETSessionID);
    LOGD("CSUPLController::GetSession index = %d", index);
    if (index < 0)
    {
        session = NULL;
    }
    else
    {
        session = m_SessionQueue.valueFor(m_SessionQueue.keyAt(index));
    }

    LOGD("CSUPLController::GetSession session = 0x%x, sessionid = %d", (Engine::uint32_t)session, message->m_SETSessionID);
    return session;
}

CSession*   CSUPLController::GetSession(Engine::CSLPSessionID* SlpSessionId)
{
    SessionIter iter;
    /*Check that there is no timeout for session*/
    for (unsigned int i = 0; i < m_SessionQueue.size(); ++i)
    {
        iter = m_SessionQueue.valueAt(i);
        if (iter->iSessionID.iSLPSessionID)
        {
            Engine::uint8_t* SessionId1 = iter->iSessionID.iSLPSessionID->iSessionId.m_Data;
            Engine::uint8_t* SessionId2 = SlpSessionId->iSessionId.m_Data;
            Engine::uint8_t  size = SlpSessionId->iSessionId.m_Size;
            if (memcmp(SessionId1,SessionId2,size) == 0)
            {
                return iter;
            }
        }
    }

    return NULL;
}

/*
*******************************************************************
* Function: AddSession
*
* Description : Adds session to the list if this session doesn't
* presents in the list yet.
*
* Parameters :
* session rw Pointer to CSession class object .
* AppId    r application ID
* Returns : None
*******************************************************************
*/

void CSUPLController::AddSession(CSession* session,uint16_t AppId)
{

    LOGD("CSUPLController::AddSession: session = 0x%x, sessionId = %d", (Engine::uint32_t) session, session->GetSessionUID());
    if (AppId)
    {
        session->m_AppID = AppId;
        int index = m_SessionQueue.indexOfKey(AppId);
        LOGD("CSUPLController::AddSession: AppId = %d, index = %d", AppId, index);
        if (index < 0)
            m_SessionQueue.add(AppId, session);
        else
            m_SessionQueue.editValueAt(index) = session;

        return;
    }

    uint32_t id = session->GetSessionUID();
    if (id > 0)
    {
        int index = m_SessionQueue.indexOfKey(id);
        if (index < 0)
            m_SessionQueue.add(id, session);

    }
    else
    {
        m_SessionQueue.add(id, session);
    }
}


void CSUPLController::SUPLCleanup()
{
    Lock();
    if (m_SignalToKill == FALSE)
    {
        /*******************STOP WORKER THREAD*****************/
        unsigned long pCode=0;
        Engine::MSG* msg = 0;
        Engine::CSession* session = 0;
        LOGD("SUPL: In SUPL Deinit function");
        m_SignalToKill = TRUE;  //Send STOP signal to SUPL Controller
        LOGD("SUPL: Deinit Supl Controller");
        if ( SUPLC_releaseSemaphore(gp_semHandle) < 0 )
        {
            LOGD(" %s: sem_post FAILED !!! \n", __FUNCTION__);
        }
#if defined(WIN32) || defined(WINCE)
        WaitForSingleObject(hThread,INFINITE);
        pCode = CloseHandle(hThread);
        hThread = NULL;
#endif /* WIN32 || WINCE */
#if defined(_LINUX_)
        pthread_join(hThread, NULL);
        hThread = 0;
#endif /* _LINUX_ */

        LOGD("SUPL: Deinit TLS");

        /*******************CLOSE CONNECTION*****************/
		if( m_pNetwork->TLS_IsActive())
        {
            m_pNetwork->TLS_CloseConnection();
        }

        /*******************CLEAR ACTIVE NOTIFICATIONS*****************/
        m_pDevice->ClearAllNotifications();

        /*******************CLEAR INCOMING MESSAGE QUEUE*****************/
        CNetworkComponentBase::Lock();
        CNetworkComponentBase::ClearIncomingQueue();
        CNetworkComponentBase::UnLock();

        for (uint16_t i=0; i < m_MessageQueue.size(); i++)
        {
            msg = m_MessageQueue.front();
            delete msg;
            m_MessageQueue.pop();
        }
        if (m_SessionQueue.size() != 0)
        {
            SessionIter iter;
            for (unsigned int i = 0; i < m_SessionQueue.size(); ++i)
            {
                session = m_SessionQueue.valueAt(i);
                delete session;
            }
            m_SessionQueue.clear();
        }

        LOGD("SUPL: Deinit performed successfully");
    }
    UnLock();
}


#if defined(WIN32) || defined(WINCE)
string CSUPLController::GenerateIPAddress()
{
#if defined(WINXP)
    char mnc[] = "00";
    char mcc[] = "00";
    string mnc_part, mcc_part;
    string ip;

    /*
      at first, we should find address in SIM. if we don't find address we should try search in
      address in secure memory in the phone(Managment object(xml) in WM) else we should generate this address
      from imsi.
     */

    // add SIM code here.

    // add MO search here.

    m_pDevice->GetMNC(mnc);

    mnc_part = string("mnc0") + string(mnc);

    m_pDevice->GetMCC(mcc);

    mcc_part = string("mcc") + string(mcc);
#elif defined(WINCE)
    CIMSI ismi;
    int mnc;
    string mnc_part, mcc_part;
    string ip;
    char   buf[3];

    /*
    at first, we should find address in SIM. if we don't find address we should try search in
    address in secure memory in the phone(Managment object(xml) in WM) else we should generate this address
    from imsi.
    */

    // add SIM code here.

    // add MO search here.

    m_pDevice->GetIMSIFromSIM(&ismi);
    mnc = ismi.getMNC();
    if (((int) (mnc / 100)) == 0)
    {
        mnc_part = string("mnc0") + string(_itoa(mnc, buf, 10));
    }
    else
    {
        mnc_part = string("mnc") + string(_itoa(mnc, buf, 10));
    }

    mcc_part = string("mcc") + string(_itoa(ismi.getMCC(), buf, 10));
#endif

    ip = string("h-slp.");
    ip += mnc_part;
    ip += string(".");
    ip += mcc_part;
    ip += string(".pub.3gppnetwork.org");
    return ip;
}
#endif /* WIN32 || WINCE */
#if defined(_LINUX_)
String8 CSUPLController::GenerateIPAddress()
{
    String8 ip;
    ip = String8("h-slp.");
//  ip += mnc_part;
//  ip += string(".");
//  ip += mcc_part;
//  ip += string(".pub.3gppnetwork.org");
    return ip;
}
#endif /* _LINUX_ */

void CSUPLController::FindRelMessageAndDelete(uint16_t AppId)
{
    MSG* msg = 0;

	LOGD(" %s: Entering \n", __FUNCTION__);
    for (uint16_t i=0;i < m_MessageQueue.size();i++) // Find msg relative to session and delete it
    {
        msg = m_MessageQueue.front();
        m_MessageQueue.pop();
        if (msg->m_SETSessionID == AppId)
        {
        	LOGD(" %s: Deleting the message for the current session \n", __FUNCTION__);
			delete msg;
        }
        else
        {
            m_MessageQueue.push(msg);

            /* Release semaphore for SUPL_Core to process this message. */
            LOGD(" %s: Calling SUPLC_releaseSemaphore \n", __FUNCTION__);
            if ( SUPLC_releaseSemaphore(gp_semHandle) < 0 )
            {
                LOGD(" %s: sem_post FAILED !!! \n", __FUNCTION__);
            }
        }
    }
}

Platform::IGPS&  CSUPLController::GetGPS()
{
    return (*m_pGPSDevice);
}

Platform::IDevice&   CSUPLController::GetDevice()
{
    return (*m_pDevice);
}
#if defined(_LINUX_)
Platform::INetwork&  CSUPLController::GetNetwork()
{
    return (*m_pNetwork);
}
#endif /* _LINUX_ */
bool_t  CSUPLController::HashMessage(Engine::MSG* msg,CBitString** Ver)
{
	int i;
	const char *key= slp_address;
    LOGD("CSUPLController::+HashMessage");
	LOGD("CSUPLController::HashMessage KEY = [%s]", key);
    *Ver = new(nothrow) CBitString(64);
    if (!(*Ver))
    {
        LOGD("CSUPLController::HashMessage: !(*Ver)");
        msg->m_Status = Engine::MSG_FATAL_ERROR;
        return FALSE;
    }
    if (!(*Ver)->IsObjectReady())
    {
        LOGD("CSUPLController::HashMessage: !(*Ver)->IsObjectReady()");
        msg->m_Status = Engine::MSG_FATAL_ERROR;
        return FALSE;
    }
    uint8_t* hash = m_pNetwork->CMN_HMAC(key,strlen(key),msg->m_pIncomingRawData,msg->m_IncomingRawDataSize);
    if (!hash)
    {
        LOGD("CSUPLController::HashMessage: !hash");
        msg->m_Status = Engine::MSG_FATAL_ERROR;
        return FALSE;
    }
    memcpy((*Ver)->m_Data,hash,8);
    free(hash);
	hash = NULL;

	LOGD("CSUPLController::-HashMessage");
    return TRUE;
}

bool_t CSUPLController::HandleNotifications(CSession* session)
{
    return m_pDevice->DisplayNotifications(session);
}
}

