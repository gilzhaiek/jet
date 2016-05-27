/*
******************************************************************
* COPYRIGHT © Teleca AB                                          *
*----------------------------------------------------------------*
* MODULE     : Network.h                                         *
*                                                                *
* PROGRAMMER : Dmitriy Kardakov                                  *
* DATE       : 28 Feb 2009                                       *
* VERSION    : 1.0                                               *
*                                                                *
*----------------------------------------------------------------*
* MODULE SUMMARY : INetwork class and its subclasses declaration.*
*                  Provides functions for working with SSL       *
*                  connection                                    *
*----------------------------------------------------------------*
* MODIFICATION RECORDS                                           *
* Alexander V. Morozov                                           *
* Ildar Abdullin   - 23 Nov 2007 - Initial version               *
* Dmitriy Kardakov - 28 Feb 2009 - Porting to Android            *
******************************************************************
*/
#ifndef __NETWORK_H__
#define __NETWORK_H__

#include "NetListener.h"
#include "NetworkComponent.h"
#include "ags_TLSConnection.h"
#include "NetListener.h"
#include "common/types.h"

namespace Platform
{

class INetwork 
{
public:
    /* Fabric Function */
    static INetwork* NewNetworkIF();
    /* TLS part */
    virtual int TLS_Init() = 0;
    virtual int TLS_Send(void* buffer, uint32_t size) = 0;
    virtual int TLS_CreateConnection(char* host_port) = 0;
    virtual void TLS_CloseConnection() = 0;
    virtual int TLS_IsActive() = 0;
    /* SMS/WAP-push message listen */
    virtual int LSR_StartListen() = 0;
    /* Common Part */
    virtual int CMN_IsDataExist(CNetworkComponentBase::RawData_t* raw_data) = 0;
    virtual unsigned char* CMN_HMAC(const void* key, int key_len, 
                                    unsigned char* data, int data_len) = 0;
#if defined(_LINUX_)
    virtual int HandleMessage(const char * body, int size) = 0;
#endif
    INetwork()
    {
    }
    virtual ~INetwork()
    {
    }
};

#if defined(_LINUX_)
class CLinNetwork : public INetwork
{
public:
    int TLS_Init();

    int TLS_CreateConnection(char* host_port);

    int TLS_Send(void* buffer, uint32_t size);

    int TLS_IsActive();

    void TLS_CloseConnection();

    int LSR_StartListen();

    int CMN_IsDataExist(CNetworkComponentBase::RawData_t* raw_data);

    unsigned char* CMN_HMAC(const void* key, int key_len, unsigned char* data,
                            int data_len);

	static void generating_hash(const char* key,int key_len,unsigned char *output, const char* fqdn_id);
    int HandleMessage(const char * body, int size);

    CLinNetwork()
    {
	}

    ~CLinNetwork()
    {
	}

private:
    CLinTLSCon tls;
    CLinNetListener listener; 
};
#endif
}
#endif // __NETWORK_H__
