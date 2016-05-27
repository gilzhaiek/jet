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
*                                                                *
* MODULE SUMMARY : Declares CSUPLController class.               *
*----------------------------------------------------------------*
*                                                                *
* MODIFICATION RECORDS                                           *
* Ildar Abdullin   - 23 Nov 2007 - Initial version               *
* Dmitriy Kardakov - 28 Feb 2009 - Porting to Android            *
******************************************************************
*/

#ifndef __SUPL_CONTROLLER_H__
#define __SUPL_CONTROLLER_H__

#if defined(_LINUX_)
#include "android/_linux_specific.h"
#endif /* _LINUX_ */
#include <stdio.h>
#include "android/ags_queue.h" 
#include "utils/KeyedVector.h" 
#include "utils/String8.h" 
#include "common/types.h"
#include "common/SUPL_defines.h"
#include "common/MSG.h"
#include "session/Session.h"
#include "gps/GPS.h"
#include "network/Network.h"
#include "device/ags_Device.h"
#include "gps/GPSCommand.h"
#include "algorithm/SUPLEndAlgorithm.h"
#include "ulp_processor/ULP_Common.h"
#include "ti_client_wrapper/mcpf_nal_common.h"
#include "jni.h"
#define SUPL_THREAD_STACK_SIZE       (64*1024)
int getHSlp(int *buffer);

#define SLP_FQDN_PHONE "FQDN_PHONE"
#define SLP_FQDN_SIM "FQDN_SIM"
#define SLP_FQDN_AUTO "FQDN_AUTO"
#define SLP_IP_ADDR "IP_ADDR"



#define MAX_SLP_ADDR_LEN 256

namespace Engine 
{

typedef android::Queue<Engine::MSG*>    MSGQueue; 
typedef android::KeyedVector<uint32_t, CSession*> SessionMap; 
typedef CSession*        SessionIter;

typedef enum SlpType
{
    FQDN_PHONE = 0,
    FQDN_SIM = 1,
    FQDN_AUTO = 2,
    IP_ADDR = 3
}SLP_Type_t;


class CSUPLController
{

public:
#if defined(WIN32) || defined(WINCE)
    HANDLE                      hThread;
#endif /* WIN32 || WINCE */
#if defined(_LINUX_)
    pthread_t                   hThread;
#endif /* _LINUX_ */
    GPSCallBack                 gps_callback;
    NALCallback                 p_nalExecuteCommand;
public:
    char*                 m_ServerIPAdrress;
    //char                 m_SLPAdrress[MAX_SLP_ADDR_LEN];
    static CSUPLController& GetInstance();
    bool_t AppendNetwork(Platform::INetwork*);
    bool_t AppendGPS(Platform::IGPS*);
    bool_t AppendDevice(Platform::IDevice*);
    bool_t AppendULPProccessor(Codec::ULP_Processor* ulp_proc);
#if defined(WIN32) || defined(WINCE)
    static DWORD CSUPLController::SUPL_ThreadProc(void* ptr);
#endif /* WIN32 || WINCE */
#if defined(_LINUX_)
    static void* SUPL_ThreadProc(void* ptr);
    Platform::INetwork& GetNetwork();
#endif /* _LINUX_ */
    bool_t SUPLInit(void *);
    bool_t SUPLStart();
	uint8_t HandleServerConnection(uint8_t conRes);

    void SUPLCleanup();
    /* controlling signals */
    void SUPLRun();
    Platform::IGPS& GetGPS();
    Platform::IDevice& GetDevice();
    CSession* GetSession(Engine::uint32_t sessionid);
    void CheckSessionsForTimeout();
    void HandleProtocolErrors(MSG* msg,Codec::SUCCESS_ERROR_T error, CSessionID& aSessID);
    void StopSession(uint16_t appId,Engine::StatusCode status_code = Engine::SYSTEM_FAILURE);
    void CreateFailureResponse(MSG* msg, Engine::StatusCode code = Engine::SYSTEM_FAILURE);
    void SetSLPAddress(char * SLPAddress) ;
    //String8 GetSlpAddress(void);
    char* GetSlpAddress(void);
//    jstring java_android_supl_config_SuplConfig_read_slp_c(JNIEnv* env, jobject jobj);
#ifdef WIN32
    void PushMessage(MSG* message);
#endif

private:
    MSGQueue                    m_MessageQueue;
    MSGQueue                    m_NewSessionMsgQueue; /* To store messages for new sessions when a session is in progress */
    SessionMap                  m_SessionQueue;
    Engine::SLP_Type_t 			m_SlpType;

    /* platform specific classes. */
    Platform::IGPS*             m_pGPSDevice;
    Platform::INetwork*         m_pNetwork;
    Platform::IDevice*          m_pDevice;
    Codec::ULP_Processor*       m_ULP_Processor;
    // uses for terminate SUPLRun main loop from outside.
    bool_t                      m_SignalToKill;
    bool_t                      m_ControllerStopped;
    
    /* variable used to store session progress */
    bool_t			m_IsSessionInProgress;
    //uint32_t                    m_CurrSessionID;
#if defined(WIN32) || defined(WINCE)
    CRITICAL_SECTION            cs;
#endif /* WIN32 || WINCE */
#if defined(_LINUX_)
    pthread_mutex_t             cs;
    pthread_attr_t              pAttr;
#endif /* _LINUX_ */

private:
    CSUPLController();
    ~CSUPLController();
    CSUPLController(const CSUPLController&);
    CSUPLController& operator = (const CSUPLController&);
    void ProcessMessage();
    void CheckForData();
    CSession* GetSession(Engine::MSG*);
    CSession* GetSession(Engine::CSLPSessionID*);
    void AddSession(CSession*,uint16_t AppId=0);
    android::String8 GenerateIPAddress(); 
    void FindRelMessageAndDelete(uint16_t AppId);
    bool_t HashMessage(Engine::MSG*,CBitString**);
    bool_t HandleNotifications(CSession*);
    bool_t genSLPIpAddress(Engine::SLP_Type_t slpType);
	SLP_Type_t readSlpType();
#if defined(_LINUX_)
    int Sleep(int ms);
#endif /* _LINUX_ */
    void Lock();
    void UnLock();
};

} /* end of Engine */

#endif /* __SUPL_CONTROLLER_H__ */
