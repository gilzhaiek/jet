/*
******************************************************************
* COPYRIGHT © Teleca AB                                          *
*----------------------------------------------------------------*
* MODULE     : ags_TLSConnection.h                               *
*                                                                *
* PROGRAMMER : Dmitriy Kardakov <Dmitriy.Kardakov@teleca.com>    *
* DATE       : 28 Feb 2009                                       *
* VERSION    : 1.0                                               *
*                                                                *
*----------------------------------------------------------------*
*                                                                *
* MODULE SUMMARY : Declares CLinTLSCon class which enables       *
* secure communications using protocols such as the Secure       *
* Sockets Layer (SSL) or "Transport Layer Security" (TLS)        *
* protocols.                                                     *
*----------------------------------------------------------------*
*                                                                *
* MODIFICATION RECORDS                                           *
* Dmitriy Kardakov - Initial version                             *
******************************************************************
*/

#ifndef __AGS_TLS_CONNECTION__
#define __AGS_TLS_CONNECTION__

#if defined(_LINUX_)
#include "android/_linux_specific.h"
#endif /* _LINUX_ */
#include "network/NetworkComponent.h"

#if defined(_LINUX_)
class CLinTLSCon;
#endif /* _LINUX_ */

class CNetTLSCon : public CNetworkComponentBase
{
public:
    CNetTLSCon();
    virtual ~CNetTLSCon();
    virtual int  Init()=0;
    virtual int  CreateConnection(char *host_port) = 0;
    virtual void FreeConnection() = 0;
    virtual void ResetConnection() = 0;
    virtual int  Recieve() = 0;
    virtual int  Send(void *data, unsigned int len) = 0;
    virtual int  IsActive() = 0;
};

#if defined(_LINUX_)
class CLinTLSCon : public CNetTLSCon
{
public:
    CLinTLSCon();
    virtual ~CLinTLSCon();
    int  Init();
    int  CreateConnection(char *host_port);
    void FreeConnection();
    void ResetConnection();
    int  Recieve();
    int  Send(void *data, unsigned int len);
    int  IsActive();
};

#endif /* _LINUX_ */


 #endif /* __AGS_TLS_CONNECTION__ */
