/*
******************************************************************
* COPYRIGHT © Teleca AB                                          *
*----------------------------------------------------------------*
* MODULE     : TLSConnection.cpp                                 *
*                                                                *
* PROGRAMMER : Dmitriy Kardakov <Dmitriy.Kardakov@teleca.com>    *
* DATE       : 28 Feb 2009                                       *
* VERSION    : 1.0                                               *
*                                                                *
*----------------------------------------------------------------*
*                                                                *
* MODULE SUMMARY : Implementation of TLSConnection class.        *
* TLSConnection class represents a sublevel between              *
* SUPLController and Java network implementation. This module    *
* interacts with SuplLocationProvider module accessing the       *
* Java methods for network operations                            *
*----------------------------------------------------------------*
*                                                                *
* MODIFICATION RECORDS                                           *
* Dmitriy Kardakov - 28 Feb 2009 - Initial version               *
******************************************************************
*/

#include "network/ags_TLSConnection.h"
#include "common/SUPL_defines.h"
#include "android/ags_jni_interface.h"


#if defined(_LINUX_)
CLinTLSCon::CLinTLSCon()
{
}
CLinTLSCon::~CLinTLSCon()
{
    FreeConnection();
}

CNetTLSCon::CNetTLSCon() {}
CNetTLSCon::~CNetTLSCon() {}

/*
*******************************************************************
* Function: Init
*
* Description : Initializes a network component. 
*
* Parameters : None
*
* Returns : 0 if success and -1 if failure
*
*******************************************************************
*/

int CLinTLSCon::Init()
{
    if (Init_native() < 0)
    {
        return -1;
    }
    else
    {
        return 0;
    }
}

/*
*******************************************************************
* Function: CreateConnection
*
* Description : Creates a secure connection to the specified port 
* number at the specified IP address.
*
* Parameters : 
* host_port  r null terminated string with a remote host IP address 
* and port number (host:port)
* Returns : -1 if an I/0 error occurs, 0 otherwise.
*
*******************************************************************
*/
int CLinTLSCon::CreateConnection(char *host_port)
{
    if (CreateConnection_native(host_port) < 0)
    {
        return -1;
    }
    else
    {
        return 0;
    }
}
/*
*******************************************************************
* Function: FreeConnection
*
* Description : Closes a secure network connection. After a secure 
* connection is closed, any further attempt to invoke Send operation 
* upon it will return -1, if Receive() method has been already 
* invoked then receiving thread will be closed. 

* Parameters : None
* Returns : None
*
*******************************************************************
*/
void CLinTLSCon::FreeConnection()
{
    FreeConnection_native();
}

void CLinTLSCon::ResetConnection() {}

/*
*******************************************************************
* Function: Recieve
*
* Description : Starts the thread receiving messages from SUPL 
* server using secure connection.
* 
* Parameters : None
* Returns : 0 if operation success and -1 if failure
*
*******************************************************************
*/
int CLinTLSCon::Recieve()
{
    if (Receive_native() < 0)
    {
        return -1;
    }
    else 
    {
        return 0;
    }
}

/*
*******************************************************************
* Function: Send
*
* Description : Writes len bytes from the specified data buffer to 
* SUPL Server using secure connection. Method blocks until all data
* data have been sent. 
*
* Parameters : 
* data   r data buffer from which bytes are to be retrieved
* len    r the number of bytes to write
* Returns : 0 if all size bytes are sent, -1 if some I/O error 
* occurs.
*
*******************************************************************
*/
int CLinTLSCon::Send(void *data, unsigned int len)
{
  
    if (Send_native(data, len) >= 0)
    {
        return 0;
    }
    else
    {
        return -1;
    }
}

/*
*******************************************************************
* Function: IsActive
*
* Description : Checks the secure network connection current status. 
*
* Parameters : None
* Returns : 0 - connection is accessible, otherwise, -1 - network 
* connection is not reachable.
*
*******************************************************************
*/

int CLinTLSCon::IsActive()
{
    return IsActive_native();
}

#endif /* _LINUX_ */


