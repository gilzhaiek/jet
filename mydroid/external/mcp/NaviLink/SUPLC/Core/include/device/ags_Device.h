/*
******************************************************************
* COPYRIGHT © Teleca AB                                          *
*----------------------------------------------------------------*
* MODULE     : ags_Device.h                                      *
*                                                                *
* PROGRAMMER : Dmitriy Kardakov <Dmitriy.Kardakov@teleca.com>    *
* DATE       : 28 Feb 2009                                       *
* VERSION    : 1.0                                               *
*                                                                *
*----------------------------------------------------------------*
* MODULE SUMMARY : Declares classes and data types required for  *
* working with GSM Modem, getting the IMSI code from SIM card and*
* sending the user notifications about Network Initiated session.*
*----------------------------------------------------------------*
*                                                                *
* MODIFICATION RECORDS                                           *
* Dmitriy Kardakov - 28 Feb 2009 - Initial version               *
******************************************************************
*/
#ifndef __AGS_DEVICE_H__
#define __AGS_DEVICE_H__

#include "common/types.h"
#include "common/MSG.h"
#include "common/LocationID.h"
#include "session/Session.h"
#include "android/ags_jni_interface.h"

#define IMSI_CODE_SIZE        16
#define IMSI_PAD              0xAB
#define IMSI_END              0xFF
#define UNIQUE_OCTET          8

namespace Platform 
{

class CIMSI
{
public:
    CIMSI();
    CIMSI(Engine::uint8_t*, Engine::uint32_t size);
    ~CIMSI();
    
#if defined (_DEBUG_IMSI)
    void                SetIMSI(Engine::uint8_t*, Engine::uint32_t);
    void                getMCC(char*);
    void                getMNC(char*);
#elif defined(_ANDROID_IMSI_)
    void                assignIMSICode(Engine::uint8_t*, Engine::uint32_t);
    Engine::uint16_t    getMCC();
    Engine::uint16_t    getMNC();
#endif
    void                getDecodedIMSI(Engine::uint8_t**, Engine::uint32_t*) {}
    void                getCodedIMSI(Engine::uint8_t**, Engine::uint32_t*);

private:
#if defined(_DEBUG_IMSI)
    Engine::uint8_t     m_Imsi[IMSI_CODE_SIZE];
    Engine::uint32_t    m_Size;
#elif defined(_ANDROID_IMSI_)
    Engine::char_t *    m_szStrRep;
    Engine::uint8_t*    m_pDecodedIMSI;
    Engine::uint32_t    m_nDecodedSize;
#endif

};

class CMSISDN
{
	public:
		CMSISDN();
		CMSISDN(Engine::uint8_t*, Engine::uint32_t size);
		~CMSISDN();
	public:
		void assignMSISDNCode(Engine::uint8_t*, Engine::uint32_t size);
		void getCodedMSISDN(Engine::uint8_t** ,Engine::uint32_t*);
	private:
		Engine::uint8_t		m_Msisdn[32];
		Engine::uint32_t	m_Size;
		Engine::char_t *    m_szStrRep;
    	Engine::uint8_t*    m_pDecodedMSISDN;
    	Engine::uint32_t    m_nDecodedSize;
};
class IDevice
{
public:
    IDevice();
    virtual ~IDevice();

public:
    static IDevice*         NewDeviceIF();
#if defined(DOCOMO_SUPPORT_WCDMA)
	virtual void			GetCellInformation(Engine::WCDMACellInformation*) = 0;
#else
    virtual void            GetCellInformation(Engine::GSMCellInformation*) = 0;
#endif
#if defined (_ANDROID_IMSI_)
    virtual void            GetIMSIFromSIM(CIMSI* imsi) = 0;
	virtual void			GetMSISDNFromSIM(CMSISDN* msisdn) = 0;
#elif defined (_DEBUG_IMSI)
    virtual void            SetIMSI(Engine::uint8_t* buffer,
                                    Engine::uint32_t size) = 0;
    virtual void            GetIMSIFromSIM(Engine::uint8_t* buffer, 
                                           Engine::uint32_t* size) = 0;
    virtual void            GetMCC(char*)=0;
    virtual void            GetMNC(char*)=0;
#endif
    virtual Engine::bool_t  DisplayNotifications(Engine::CSession*) = 0;
    virtual void            ClearAllNotifications() = 0;
    virtual void            NotificationResult(int res, int tid) = 0;

protected:
    CIMSI imsi;
};

} /* end of namespace Platform */

#endif /* __AGS_DEVICE_H__ */
