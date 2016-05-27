/*****< ARBTJBBTSO.h >*********************************************************/
/*      Copyright 2010 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  ARBTJBBTSO - Stonestreet One Android Runtime Bluetooth JNI Bridge Type    */
/*               Definitions, Constants, and Prototypes for the               */
/*               BluetoothSocket and BluetoothServerSocket modules.           */
/*                                                                            */
/*  Author:  Greg Hensley                                                     */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   07/29/10  G. Hensley     Initial creation.                               */
/******************************************************************************/

#ifndef __ARBTJBBTSOH__
#define __ARBTJBBTSOH__

#include "JNIHelp.h"
#include "jni.h"

#ifdef HAVE_BLUETOOTH

extern "C"
{
#include "SS1BTPM.h"

#include "client/SPPMAPI.h"
}

#include "ARBTJBCOM.h"


namespace android
{

   /* Keep TYPE_RFCOMM etc in sync with BluetoothSocket.java            */
#define SOCKET_TYPE_RFCOMM 1
#define SOCKET_TYPE_SCO    2
#define SOCKET_TYPE_L2CAP  3

#define BTSO_CLOSE_PORT_TIMEOUT 5000

   /* Limit how much data we can send at a time. */
#define BTSO_MAXIMUM_CHUNK_SIZE (BTPS_CONFIGURATION_SPP_DEFAULT_TRANSMIT_BUFFER_SIZE)

typedef enum _tagBTSO_ServerStatus_t
{
   btsoServerUninitialized,
   btsoServerListening,
   btsoServerAborted,
   btsoServerClosed,
   btsoServerNotAvailable
} BTSO_ServerStatus_t;

typedef enum _tagBTSO_ClientStatus_t
{
   btsoClientConnecting,
   btsoClientAccepting,
   btsoClientAccepted,
   btsoClientConnected,
   btsoClientDisconnected
} BTSO_ClientStatus_t;

typedef struct _tagBTSO_RefCount_t
{
   unsigned int    Count;
   pthread_mutex_t Mutex;
} BTSO_RefCount_t;

#define BTSO_REFCOUNT_INITIALIZER {0, PTHREAD_MUTEX_INITIALIZER}

typedef enum _tagBTSO_PortType_t
{
   btsoTypeUnknown,
   btsoTypeServer,
   btsoTypeClient
} BTSO_PortType_t;

   /* The following declared type represents the Prototype Function     */
   /* for an Authorizaiton Request Handler. It is called back when a    */
   /* setServerPortOpenRequest event is received. The function should   */
   /* return true if the connection should be accepted, or false if the */
   /* connection should be rejected.                                    */
typedef bool (BTPSAPI *BTSO_Authorization_Handler_t)(BD_ADDR_t RemoteDeviceAddress);

typedef struct _tagBTSO_PortData_t
{
   unsigned int                 Handle;
   BTSO_RefCount_t              RefCount;
   BTSO_ServerStatus_t          ServerStatus;
   BTSO_ClientStatus_t          ClientStatus;
   BD_ADDR_t                    RemoteAddress;
   pthread_cond_t               ClientAcceptCond;
   pthread_cond_t               ClientConnectCond;
   pthread_mutex_t              Mutex;
   BTSO_Authorization_Handler_t AuthorizationHandler;
   unsigned int                 InternalHandle;
   struct _tagBTSO_PortData_t  *NextEntry;
} BTSO_PortData_t;

typedef struct _tagBTSO_NativeData_t
{
   BTSO_PortType_t Type;
   unsigned int    PortDataInternalHandle;
} BTSO_NativeData_t;

int BTSO_CreatePortData();
BTSO_PortData_t *BTSO_AcquirePortData(unsigned int InternalHandle);
void BTSO_ReleasePortData(BTSO_PortData_t *PortData);
bool BTSO_DestroyPortData(BTSO_PortData_t *PortData);
int BTSO_WaitOnPortCondition(BTSO_PortData_t *PortData, pthread_cond_t *Condition, struct timespec *Timeout);
int BTSO_WaitOnPortConditionMS(BTSO_PortData_t *PortData, pthread_cond_t *Condition, unsigned int TimeoutMS);

int SPPMErrorToErrno(int SPPMError);

void BTSO_SPPM_EventCallback(SPPM_Event_Data_t *EventData, void *CallbackParameter);
void BTSO_SCOM_EventCallback(SCOM_Event_Data_t *EventData, void *CallbackParameter);

void BTSO_InitRefCount(BTSO_RefCount_t *RefCount);
bool BTSO_DestroyRefCount(BTSO_RefCount_t *RefCount);
int BTSO_IncRefCount(BTSO_RefCount_t *RefCount);
int BTSO_DecRefCount(BTSO_RefCount_t *RefCount);

}

#endif /* HAVE_BLUETOOTH */

#endif /* __ARBTJBBTSOH__ */

