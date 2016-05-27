/*
******************************************************************
* COPYRIGHT © Teleca AB                                          *
*----------------------------------------------------------------*
* MODULE     : ags_LinDevice.h                                   *
*                                                                *
* PROGRAMMER : Dmitriy Kardakov <Dmitriy.Kardakov@teleca.com>    *
* DATE       : 28 Feb 2009                                       *
* VERSION    : 1.0                                               *
*                                                                *
*----------------------------------------------------------------*
*                                                                *
* MODULE SUMMARY : Declares the LinDevice class derived from     *
* abstract IDevice class.                                        *
*----------------------------------------------------------------*
*                                                                *
* MODIFICATION RECORDS                                           *
* Dmitriy Kardakov - 28 Feb 2009 - Initial version               *
******************************************************************
*/

#ifndef __AGS_LIN_DEVICE_H__
#define __AGS_LIN_DEVICE_H__


#include "ags_Device.h"
#include "common/types.h"
#include "common/SUPL_defines.h"
#include "common/endianess.h"
#include "android/_linux_specific.h"
#include "android/ags_jni_interface.h"

#include "android/ags_queue.h" //Android STL
#define MAX_NOTIFICATION_QUEUE_LENGTH 5
#define GSM_INFO_SIZE                 53
#define SMSWAP_MESSAGE_STRINGS        2

namespace Platform
{

enum eThreadStat
{
    FINISHED   = 0,
    ACTIVE     = 1
};

enum eNotificationType
{
    MB_OK      = 0,
    MB_YESNO   = 1
};

enum eNotificationResult
{
    ALLOW      = 1,
    DENY       = 2,
    TIMEOUT    = 3
};


struct NotificationParams
{
    pthread_t               pThread;
    eThreadStat             state;
    Engine::CSession*       session;
};

struct MsgBox_ThreadProc_Data
{
    NotificationParams      params;
    pthread_mutex_t         Mutex;
};


typedef android::Queue<NotificationParams*>    TNotificationParamsQueue;

class CLinDevice: public    IDevice
{
public:

    CLinDevice();
    virtual ~CLinDevice();
#if defined (_ANDROID_IMSI_)
    virtual void            GetIMSIFromSIM(CIMSI* imsi);
	virtual void			GetMSISDNFromSIM(CMSISDN* msisdn);
#elif defined (_DEBUG_IMSI)
    virtual void            GetIMSIFromSIM(Engine::uint8_t*, Engine::uint32_t*);
    virtual void            SetIMSI(Engine::uint8_t*,Engine::uint32_t);
    virtual void            GetMCC(char*);
    virtual void            GetMNC(char*);
#endif
#if defined(DOCOMO_SUPPORT_WCDMA)
	virtual void			GetCellInformation(Engine::WCDMACellInformation*);
#else
    virtual void            GetCellInformation(Engine::GSMCellInformation*);
#endif
    virtual Engine::bool_t  DisplayNotifications(Engine::CSession*);
    virtual void            ClearAllNotifications();
    static  void*           MsgBox_ThreadProc(void* pThreadData);
    virtual void            NotificationResult(int res, int tid);
private:
    TNotificationParamsQueue m_QNotificParamsQueue;
    pthread_mutex_t          m_Qm;
};

}

#endif /* __AGS_LIN_DEVICE_H__ */
