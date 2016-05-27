/*****< ARBTJBBTSO.cpp >*******************************************************/
/*      Copyright 2010 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  ARBTJBBTSO - android.bluetooth.BluetoothSocket module for Stonestreet One */
/*               Android Runtime Bluetooth JNI Bridge Layer                   */
/*                                                                            */
/*  Author:  Greg Hensley                                                     */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   06/30/10  G. Hensley     Initial creation.                               */
/******************************************************************************/
#define LOG_TAG "ARBTJBBTSO"

#include "android_runtime/AndroidRuntime.h"
#include "JNIHelp.h"
#include "utils/Log.h"
#include "cutils/abort_socket.h"

#include <stdlib.h>
#include <errno.h>
#include <poll.h>
#include <unistd.h>
#include <sys/socket.h>
#include <sys/ioctl.h>
#include <sched.h>

#ifdef HAVE_BLUETOOTH

extern "C"
{
#include "SS1BTPM.h"
#include "client/SPPMAPI.h"
}

#include "ARBTJBCOM.h"
#include "ARBTJBBTSO.h"

#endif /* HAVE_BLUETOOTH */

#include "SS1UTIL.h"

namespace android
{

static jfieldID  Field_mAuth;     /* read-only */
static jfieldID  Field_mEncrypt;  /* read-only */
static jfieldID  Field_mType;     /* read-only */
static jfieldID  Field_mAddress;  /* read-only */
static jfieldID  Field_mPort;     /* read-only */
static jfieldID  Field_mSocketData;
static jmethodID Method_BluetoothSocket_ctor;
static jclass    Class_BluetoothSocket;

#ifdef HAVE_BLUETOOTH

static BTSO_PortData_t *PortDataList;
static pthread_mutex_t  PortDataListMutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t   PortDataReleased  = PTHREAD_COND_INITIALIZER;

static BTSO_NativeData_t *GetNativeData(JNIEnv *Env, jobject Object)
{
   BTSO_NativeData_t *NatData;

   if(Env && Object)
   {
      if((NatData = (BTSO_NativeData_t *)(Env->GetIntField(Object, Field_mSocketData))) == NULL)
      {
         SS1_LOGE("Found native data to be uninitialized");
         jniThrowException(Env, "java/io/IOException", "BluetoothSocket not initialized (stack failure?)");
      }
   }
   else
   {
      SS1_LOGE("Params are invalid (Env = %p, Obj = %p)", Env, Object);
      NatData = NULL;
   }

   return NatData;
}

#endif /* HAVE_BLUETOOTH */


static void BTSO_InitSocketFromFdNative(JNIEnv *Env, jobject Object, jint Fd)
{
   SS1_LOGD("Enter (%d)", Fd);
#ifdef HAVE_BLUETOOTH

   int                Result;
   unsigned int       Count;
   BTSO_PortData_t   *Port;
   BTSO_NativeData_t *NatData;

   *(void **)(&NatData) = calloc(1, sizeof(BTSO_NativeData_t));

   Env->SetIntField(Object, Field_mSocketData, (jint)NatData);

   if(NatData)
   {
      if(Fd == -1)
      {
         /* We are creating a completely new BluetoothSocket.           */
         Result = BTSO_CreatePortData();

         if(Result >= 0)
         {
            NatData->Type                   = btsoTypeUnknown;
            NatData->PortDataInternalHandle = (unsigned int)Result;

            Port = BTSO_AcquirePortData(NatData->PortDataInternalHandle);

            SS1_LOGD("Created NatData @ %p with new Port @ %p (IH=%u, H=%u)", NatData, Port, Port->InternalHandle, Port->Handle);
         }
         else
         {
            free(NatData);
            jniThrowException(Env, "java/lang/OutOfMemoryError", "Out of memory while configuring BluetoothSocket");

            Port = NULL;
         }
      }
      else
      {
         /* We are creating a BluetoothSocket tied to an existing, open */
         /* connection. This can only happen when accepting an incoming */
         /* connection on a listening server channel. In this case,     */
         /* the "Fd" is really the address of the Port Data structure   */
         /* created by BluetoothServerSocket. This is guaranteed because*/
         /* this construction routine is only ever called directly      */
         /* from BluetoothServerSocket's AcceptNative() routine. This   */
         /* structure will be shared between the two objects until      */
         /* both are destroyed. This is necessary because the server    */
         /* registration must remain open while the client is connected */
         /* but the Android API is based on BSD Sockets and actually    */
         /* recommends closing the BluetoothServerSocket as soon as the */
         /* client connection is established.                           */

         if((Port = BTSO_AcquirePortData((unsigned int)Fd)) != NULL)
         {
            NatData->Type                   = btsoTypeClient;
            NatData->PortDataInternalHandle = (unsigned int)Fd;

            SS1_LOGD("Created NatData @ %p with old Port @ %p (IH=%u, H=%u)", NatData, Port, Port->InternalHandle, Port->Handle);
         }
         else
         {
            SS1_LOGD("Error: Unable to locate IH %u *** THIS SHOULD NEVER HAPPEN ***", (unsigned int)Fd);
            NatData->Type                   = btsoTypeClient;
            NatData->PortDataInternalHandle = 0;
         }
      }

      if(Port)
      {
         Count = BTSO_IncRefCount(&(Port->RefCount));
         SS1_LOGD("Incremented reference count on port data @ %p (IH=%u): new count = %u", Port, Port->InternalHandle, Count);

         BTSO_ReleasePortData(Port);
      }
   }
   else
      jniThrowException(Env, "java/lang/OutOfMemoryError", "Out of memory while constructing BluetoothSocket");
#else /* HAVE_BLUETOOTH */
   jniThrowIOException(Env, ENOSYS);
#endif
   SS1_LOGD("Exit");
}

static void BTSO_InitSocketNative(JNIEnv *Env, jobject Object)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   BTSO_InitSocketFromFdNative(Env, Object, -1);
#else /* HAVE_BLUETOOTH */
   jniThrowIOException(Env, ENOSYS);
#endif
   SS1_LOGD("Exit");
}

static void BTSO_ConnectNative(JNIEnv *Env, jobject Object)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   int                             Result;
   bool                            CanConnect;
   jint                            ProtocolType;
   jstring                         Address;
   BD_ADDR_t                       BD_ADDR;
   const char                     *AddrString;
   unsigned int                    Attempts;
   unsigned int                    Channel;
   unsigned int                    Status;
   unsigned long                   Flags;
   BTSO_PortData_t                *Port;
   BTSO_NativeData_t              *NatData;
   DEVM_Local_Device_Properties_t  LocalDevProps;

   Result     = 0;
   NatData    = NULL;
   Attempts   = 0;
   CanConnect = false;

   /* Make sure this process has a connection to the Bluetopia daemon.  */
   if(InitBTPMClientNoRetries(NULL, NULL, NULL) == 0)
   {
      if((NatData = GetNativeData(Env, Object)) != NULL)
      {
         SS1_LOGD("BTSO Port: Searching (IH=%u)", NatData->PortDataInternalHandle);
         if((Port = BTSO_AcquirePortData(NatData->PortDataInternalHandle)) != NULL)
         {
            SS1_LOGD("BTSO Port: Acquired (IH=%u)", NatData->PortDataInternalHandle);

            /* Make sure we don't already have a connection and this    */
            /* isn't a server socket.                                   */
            if(Port->Handle == 0)
            {
               if(NatData->Type == btsoTypeUnknown)
               {
                  /* Whether the connection succeeds or not, we now     */
                  /* know this is not a BluetoothServerSocket because   */
                  /* ConnectNative() is never called on such an obect.  */
                  NatData->Type      = btsoTypeClient;
                  Port->ServerStatus = btsoServerNotAvailable;
               }

               if(NatData->Type == btsoTypeClient)
               {
                  if(Port->ClientStatus == btsoClientDisconnected)
                  {
                     /* The port currently has no remote connection, so */
                     /* we are clear to initiate a connection.          */
                     Port->ClientStatus = btsoClientConnecting;

                     CanConnect = true;
                  }
                  else
                  {
                     /* If the port is not disconnected, then it's      */
                     /* either already connected or in the process of   */
                     /* connecting so we shouldn't try making another   */
                     /* connection yet.                                 */
                     if(Port->ClientStatus == btsoClientConnected)
                        Result = EISCONN;
                     else
                        Result = EINPROGRESS;
                  }
               }
            }

            BTSO_ReleasePortData(Port);
            Port = NULL;
            SS1_LOGD("BTSO Port: Released (IH=%u)", NatData->PortDataInternalHandle);
         }

         if(CanConnect)
         {
            memset(&BD_ADDR, 0, sizeof(BD_ADDR));
            if((Address = (jstring) Env->GetObjectField(Object, Field_mAddress)) != NULL)
            {
               if((AddrString = Env->GetStringUTFChars(Address, NULL)) != NULL)
               {
                  if(StrToBD_ADDR(&BD_ADDR, AddrString) == false)
                     memset(&BD_ADDR, 0, sizeof(BD_ADDR));
                  Env->ReleaseStringUTFChars(Address, AddrString);
               }
            }

            ProtocolType = Env->GetIntField(Object, Field_mType);
            switch(ProtocolType)
            {
               case SOCKET_TYPE_RFCOMM:
                  Flags = 0;
                  if(Env->GetBooleanField(Object, Field_mAuth))
                     Flags |= SPPM_OPEN_REMOTE_PORT_FLAGS_REQUIRE_AUTHENTICATION;
                  if(Env->GetBooleanField(Object, Field_mEncrypt))
                     Flags |= SPPM_OPEN_REMOTE_PORT_FLAGS_REQUIRE_ENCRYPTION;

                  Channel = (unsigned int)Env->GetIntField(Object, Field_mPort);
                  if(RFCOMM_VALID_SERVER_CHANNEL_ID(Channel))
                  {
                     SS1_LOGD("Opening remote port to channel %u, with callback @ %p and param @ 0x%08X", Channel, BTSO_SPPM_EventCallback, NatData->PortDataInternalHandle);

                     Result = -1;
                     Attempts = 0;

                     while((Attempts < 3) && (Result == -1))
                     {
                        if(Attempts > 0)
                        {
                           /* Not our first try, so introduce a delay to*/
                           /* avoid issues with stack state.            */
                           BTPS_Delay(250);
                        }
                        Status = 0;
                        Attempts++;

                        /* Stop any active Discovery process, as this   */
                        /* will prevent PM from accepting our connection*/
                        /* request.                                     */
                        if((DEVM_QueryLocalDeviceProperties(&LocalDevProps) == 0) && (LocalDevProps.LocalDeviceFlags & DEVM_LOCAL_DEVICE_FLAGS_DEVICE_DISCOVERY_IN_PROGRESS))
                           DEVM_StopDeviceDiscovery();

                        if((Result = SPPM_OpenRemotePort(BD_ADDR, Channel, Flags, BTSO_SPPM_EventCallback, (void *)(NatData->PortDataInternalHandle), &Status)) > 0)
                        {
                           SS1_LOGD("BTSO Port: Searching (IH=%u)", NatData->PortDataInternalHandle);
                           if((Port = BTSO_AcquirePortData(NatData->PortDataInternalHandle)) != NULL)
                           {
                              SS1_LOGD("BTSO Port: Acquired (IH=%u)", NatData->PortDataInternalHandle);

                              Port->Handle        = (unsigned int)Result;
                              Port->ClientStatus  = btsoClientConnected;
                              Port->RemoteAddress = BD_ADDR;

                              BTSO_ReleasePortData(Port);
                              Port = NULL;
                              SS1_LOGD("BTSO Port Mutex: Released (IH=%u)", NatData->PortDataInternalHandle);

                              /* Success                                */
                              Result = 0;
                           }
                           else
                           {
                              /* We couldn't reaquire the lock over     */
                              /* the port data, so close the port       */
                              /* immediately.                           */
                              SS1_LOGE("Failed to re-acquire port mutex after connect");
                              SPPM_ClosePort((unsigned int)Result, SPPM_CLOSE_DATA_FLUSH_TIMEOUT_IMMEDIATE);
                              Result = EAGAIN;
                           }
                        }
                        else
                        {
                           SS1_LOGD("RFCOMM Connection failed with code %d/%d", Result, Status);
                           Result = SPPMErrorToErrno(Result);
                        }
                     }
                  }
                  else
                  {
                     /* Bad RFCOMM Channel (port) number.               */
                     Result = ENETUNREACH;
                  }
                  break;

               case SOCKET_TYPE_SCO:
                  SS1_LOGD("Opening SCO connection, with callback @ %p and param @ 0x%08X", BTSO_SCOM_EventCallback, NatData->PortDataInternalHandle);

                  Result = -1;
                  Attempts = 0;

                  if((Result = SCOM_OpenRemoteConnection(BD_ADDR, BTSO_SCOM_EventCallback, (void *)(NatData->PortDataInternalHandle), &Status)) > 0)
                  {
                     SS1_LOGD("BTSO Port: Searching (IH=%u)", NatData->PortDataInternalHandle);
                     if((Port = BTSO_AcquirePortData(NatData->PortDataInternalHandle)) != NULL)
                     {
                        SS1_LOGD("BTSO Port: Acquired (IH=%u)", NatData->PortDataInternalHandle);

                        Port->Handle        = (unsigned int)Result;
                        Port->ClientStatus  = btsoClientConnected;
                        Port->RemoteAddress = BD_ADDR;

                        BTSO_ReleasePortData(Port);
                        Port = NULL;
                        SS1_LOGD("BTSO Port Mutex: Released (IH=%u)", NatData->PortDataInternalHandle);

                        /* Success                                      */
                        Result = 0;
                     }
                     else
                     {
                        /* We couldn't reaquire the lock over the port  */
                        /* data, so close the port immediately.         */
                        SS1_LOGE("Failed to re-acquire port data after connect");
                        SCOM_CloseConnection((unsigned int)Result);
                        Result = EAGAIN;
                     }
                  }
                  else
                  {
                     SS1_LOGD("SCO Connection failed with code %d/%d", Result, Status);
                     //XXX SCO
                     Result = SPPMErrorToErrno(Result);
                  }
                  break;

               case SOCKET_TYPE_L2CAP:
               default:
                  SS1_LOGE("Unsupported socket type (%d)", ProtocolType);
                  Result = ENOSYS;
            }
         }
         else
         {
            if(Result == 0)
               Result = EBADF;
         }
      }
      else
         jniThrowIOException(Env, EINVAL);
   }
   else
   {
      Result = ENETDOWN;
      SS1_LOGE("Unable to access Bluetooth platform");
   }

   if(NatData && Port)
      CHECK_MUTEX(&(Port->Mutex));

   if(Result != 0)
   {
      SS1_LOGE("Failed to make RFCOMM connection after %u tries.", Attempts);
      jniThrowIOException(Env, Result);
   }
   else
   {
      if(Attempts > 1)
         SS1_LOGI("RFCOMM connection successful after %u attempts.", Attempts);
   }
#else /* HAVE_BLUETOOTH */
   jniThrowIOException(Env, ENOSYS);
#endif
   SS1_LOGD("Exit");
}


   /* Returns errno instead of throwing, so java can check errno        */
static int BTSO_BindListenNative(JNIEnv *Env, jobject Object)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   int                Result;
   jint               ret_val;
   jint               ProtocolType;
   BD_ADDR_t          BD_ADDR;
   Boolean_t          ServerPresent;
   unsigned int       Channel;
   unsigned long      Flags;
   BTSO_PortData_t   *Port;
   BTSO_NativeData_t *NatData;

   Result  = 0;
   ret_val = EINVAL;
   NatData = NULL;

   /* Make sure this process has a connection to the Bluetopia daemon.  */
   if(InitBTPMClientNoRetries(NULL, NULL, NULL) == 0)
   {
      if((NatData = GetNativeData(Env, Object)) != NULL)
      {
         SS1_LOGD("BTSO Port: Searching (IH=%u)", NatData->PortDataInternalHandle);
         if((Port = BTSO_AcquirePortData(NatData->PortDataInternalHandle)) != NULL)
         {
            SS1_LOGD("BTSO Port: Acquired (IH=%u)", NatData->PortDataInternalHandle);

            /* Only Bind/Listen if this object hasn't been opened       */
            /* yet nor connected as a client. If it had been            */
            /* connected as a client, ServerStatus would be set to      */
            /* btsoServerNotAvailable.                                  */
            if(Port->Handle == 0)
            {
               if((NatData->Type == btsoTypeUnknown) || ((NatData->Type == btsoTypeServer) && (Port->ServerStatus == btsoServerUninitialized)))
               {
                  /* Now we know this is a BluetoothServerSocket object */
                  /* since BindListenNative() is never called on a plain*/
                  /* BluetoothSocket object.                            */
                  NatData->Type = btsoTypeServer;

                  ProtocolType = Env->GetIntField(Object, Field_mType);
                  switch(ProtocolType)
                  {
                     case SOCKET_TYPE_RFCOMM:
                        Flags = SPPM_REGISTER_SERVER_PORT_FLAGS_REQUIRE_AUTHORIZATION;
                        if(Env->GetBooleanField(Object, Field_mAuth))
                           Flags |= SPPM_REGISTER_SERVER_PORT_FLAGS_REQUIRE_AUTHENTICATION;
                        if(Env->GetBooleanField(Object, Field_mEncrypt))
                           Flags |= SPPM_REGISTER_SERVER_PORT_FLAGS_REQUIRE_ENCRYPTION;

                        Channel = (unsigned int)Env->GetIntField(Object, Field_mPort);
                        if(RFCOMM_VALID_SERVER_CHANNEL_ID(Channel))
                        {
                           if((!SPPM_QueryServerPresent(Channel, &ServerPresent)) && (!ServerPresent))
                           {
                              SS1_LOGD("Registering RFCOMM server channel %u, with callback @ %p and param (IH=%u)", Channel, BTSO_SPPM_EventCallback, NatData->PortDataInternalHandle);
                              if((Result = SPPM_RegisterServerPort(Channel, Flags, BTSO_SPPM_EventCallback, (void *)(NatData->PortDataInternalHandle))) > 0)
                              {
                                 ret_val            = 0;
                                 Port->Handle       = (unsigned int)Result;
                                 Port->ServerStatus = btsoServerListening;
                              }
                              else
                              {
                                 SS1_LOGE("Socket bind/listen failed (%d)", Result);
                                 ret_val = SPPMErrorToErrno(Result);
                              }
                           }
                           else
                           {
                              SS1_LOGE("RFCOMM channel already in use (%u)", Channel);
                              ret_val = EADDRNOTAVAIL;
                           }
                        }
                        else
                        {
                           SS1_LOGE("Bad RFCOMM channel (%u)", Channel);
                           ret_val = EADDRNOTAVAIL;
                        }
                        break;

                     case SOCKET_TYPE_SCO:
                        SS1_LOGD("Registering SCO server, with callback @ %p and param (IH=%u)", BTSO_SCOM_EventCallback, NatData->PortDataInternalHandle);

                        ASSIGN_BD_ADDR(BD_ADDR, 0, 0, 0, 0, 0, 0);
                        if((Result = SCOM_RegisterServerConnection(TRUE, BD_ADDR, BTSO_SCOM_EventCallback, (void *)(NatData->PortDataInternalHandle))) > 0)
                        {
                           ret_val            = 0;
                           Port->Handle       = (unsigned int)Result;
                           Port->ServerStatus = btsoServerListening;
                        }
                        else
                        {
                           SS1_LOGE("Socket bind/listen failed (%d)", Result);
                           //XXX SCO
                           ret_val = SPPMErrorToErrno(Result);
                        }
                        break;

                     case SOCKET_TYPE_L2CAP:
                     default:
                        SS1_LOGE("Unsupported socket type (%d)", ProtocolType);
                        ret_val = ENOSYS;
                  }
               }
            }
            else
            {
               SS1_LOGE("Socket in bad state (IH=%u, H=%u, Server Status = %d)", NatData->PortDataInternalHandle, Port->Handle, Port->ServerStatus);
            }

            BTSO_ReleasePortData(Port);
            Port = NULL;
            SS1_LOGD("BTSO Port Mutex: Released (IH=%u)", NatData->PortDataInternalHandle);
         }
         else
            SS1_LOGE("Unable to acquire port data lock");
      }
      else
         SS1_LOGE("Object uninitialized");
   }
   else
   {
      ret_val = ENETDOWN;
      SS1_LOGE("Unable to access Bluetooth platform");
   }

   if(ret_val != 0)
      SS1_LOGE("Result = %d (%s)", ret_val, strerror(ret_val));
   else
      SS1_LOGD("Result = Success (%d)", Result);

   if(NatData && Port)
      CHECK_MUTEX(&(Port->Mutex));

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", ENOSYS);
   return(ENOSYS);
#endif
}

static jobject BTSO_AcceptNative(JNIEnv *Env, jobject Object, int Timeout)
{
   // TODO Handle SCO and L2CAP variants alongside RFCOMM
   SS1_LOGD("Enter (%d)", Timeout);
#ifdef HAVE_BLUETOOTH
   int                 Result;
   char                AddrBuffer[32];
   jint                ProtocolType;
   jobject             ret_val;
   jobject             SocketObject;
   jstring             Address;
   jboolean            RequireEncryption;
   jboolean            RequireAuthentication;
   BD_ADDR_t           BD_ADDR;
   struct timeval      TimeVal;
   struct timespec     TimeSpec;
   BTSO_PortData_t    *Port;
   BTSO_NativeData_t  *NatData;
   BTSO_ClientStatus_t ClientStatus;

   Result       = EIO;
   ret_val      = NULL;
   SocketObject = NULL;
   ClientStatus = btsoClientDisconnected;

   memset(&BD_ADDR, 0, sizeof(BD_ADDR));

   if((NatData = GetNativeData(Env, Object)) != NULL)
   {
      SS1_LOGD("BTSO Port: Searching (IH=%u)", NatData->PortDataInternalHandle);
      if((Port = BTSO_AcquirePortData(NatData->PortDataInternalHandle)) != NULL)
      {
         SS1_LOGD("BTSO Port: Acquired (IH=%u)", NatData->PortDataInternalHandle);

         SS1_LOGD("Attempting to accept connection for Port (%p), H=%u IH=%u", Port, Port->Handle, NatData->PortDataInternalHandle);
         if(NatData->Type == btsoTypeServer)
         {
            if(Port->ServerStatus == btsoServerListening)
            {
               SS1_LOGD("Server is still listening");

               if(Port->ClientStatus == btsoClientAccepting)
               {
                  /* A connection come in while we weren't watching.    */
                  /* Finish the accept immediately.                     */
                  SS1_LOGD("Found pending client connection, H=%u IH=%u", Port->Handle, NatData->PortDataInternalHandle);
                  Result             = 0;
                  Port->ClientStatus = btsoClientAccepted;
               }
               else
               {
                  if(Timeout > 0)
                  {
                     /* We were given a timeout (in milliseconds), so   */
                     /* only wait that long.                            */
                     gettimeofday(&TimeVal, NULL);
                     TimeSpec.tv_sec  = TimeVal.tv_sec;
                     TimeSpec.tv_nsec = (TimeVal.tv_usec * 1000L);
                     
                     /* Add the Timeout to the current time, adding     */
                     /* seconds and nanoseconds separately to avoid     */
                     /* overflow.                                       */
                     if(Timeout >= 1000)
                     {
                        TimeSpec.tv_sec += (Timeout / 1000); 
                        Timeout         -= ((Timeout / 1000) * 1000);
                     }

                     TimeSpec.tv_nsec += (Timeout * 1000L * 1000L);

                     /* Now, normalize the timespec_t by ensuring the   */
                     /* nanosecond field is less than one second. Since */
                     /* we can guarantee that less than one second of   */
                     /* time was added to the tv_nsec field, we only    */
                     /* need to shift up to one second of time into     */
                     /* tv_sec so we can avoid any 64-bit division.     */
                     if(TimeSpec.tv_nsec >= 1000000000L)
                     {
                        TimeSpec.tv_sec  += 1;
                        TimeSpec.tv_nsec -= 1000000000L;
                     }

                     SS1_LOGD("Waiting for ClientAcceptCond (%p) for H=%u IH=%u, for %d ms (%u s, %lu ns)", &(Port->ClientAcceptCond), Port->Handle, NatData->PortDataInternalHandle, Timeout, (unsigned int)TimeSpec.tv_sec, (unsigned long)TimeSpec.tv_nsec);
                     Result = BTSO_WaitOnPortCondition(Port, &(Port->ClientAcceptCond), &TimeSpec);
                     SS1_LOGD("Wait finished for ClientAcceptCond (%p) for H=%u IH=%u", &(Port->ClientAcceptCond), Port->Handle, NatData->PortDataInternalHandle);
                  }
                  else
                  {
                     if(Timeout < 0)
                     {
                        /* No valid timeout was specified, so wait      */
                        /* forever.                                     */
                        SS1_LOGD("Waiting for ClientAcceptCond (%p), H=%u IH=%u, indefinitely", &(Port->ClientAcceptCond), Port->Handle, NatData->PortDataInternalHandle);
                        Result = BTSO_WaitOnPortCondition(Port, &(Port->ClientAcceptCond), NULL);
                        SS1_LOGD("Wait finished for ClientAcceptCond (%p), H=%u IH=%u", &(Port->ClientAcceptCond), Port->Handle, NatData->PortDataInternalHandle);
                     }
                     else
                     {
                        /* We were given a timeout of zero (no blocking)*/
                        /* but no incoming connection was waiting to be */
                        /* accepted.                                    */
                        Result = EAGAIN;
                        SS1_LOGD("No timeout, and no pending connection for H=%u IH=%u", Port->Handle, NatData->PortDataInternalHandle);
                     }
                  }

                  SS1_LOGD("Woke up (ServerStatus = %d)", Port->ServerStatus);

                  /* First, make sure any wait completed without error. */
                  if(Result == 0)
                  {
                     if(Port->ServerStatus == btsoServerAborted)
                     {
                        SS1_LOGD("accept() canceled, H=%u IH=%u", Port->Handle, NatData->PortDataInternalHandle);
                        Result = ECANCELED;
                     }
                     else
                     {
                        /* We weren't canceled, so we probably woke up  */
                        /* due to an incoming connection. Check to be   */
                        /* sure.                                        */
                        if(Port->ClientStatus == btsoClientAccepting)
                        {
                           SS1_LOGD("Found client, getting data, H=%u IH=%u", Port->Handle, NatData->PortDataInternalHandle);
                           Port->ClientStatus = btsoClientAccepted;
                        }
                        else
                        {
                           SS1_LOGD("No connection found for H=%u IH=%u (%d,%d)", Port->Handle, NatData->PortDataInternalHandle, Port->ServerStatus, Port->ClientStatus);
                           Result = EAGAIN;
                        }
                     }
                  }
                  else
                     SS1_LOGD("Possibly cancelled, H=%u IH=%u (Result = %d)", Result, Port->Handle, NatData->PortDataInternalHandle);
               }
            }
            else
            {
               SS1_LOGE("Server not in Listening state");
               Result = EINVAL;
            }
         }
         else
         {
            SS1_LOGE("Called on non-server object");
            Result = EBADF;
         }

         if((Result == 0) && (Port->ClientStatus == btsoClientAccepted))
         {
            SS1_LOGD("Client now connected, H=%u IH=%u", Port->Handle, NatData->PortDataInternalHandle);

            /* We found a client waiting to connect and have accepted   */
            /* the connection. Change the client status to reflect this.*/
            Port->ClientStatus = btsoClientConnected;

            /* Cache the remote device address for later.               */
            BD_ADDR = Port->RemoteAddress;

            /* Build the BluetoothSocket representing the client-half of*/
            /* this connection which we will return to the caller.      */
            ProtocolType          = Env->GetIntField(Object, Field_mType);
            RequireEncryption     = Env->GetBooleanField(Object, Field_mEncrypt);
            RequireAuthentication = Env->GetBooleanField(Object, Field_mAuth);

            /* Release the port data now, because the construction of   */
            /* the new BluetoothSocket will attempt to acquire this lock*/
            /* on this port.                                            */
            BTSO_ReleasePortData(Port);
            Port = NULL;
            SS1_LOGD("BTSO Port Mutex: Released (IH=%u)", NatData->PortDataInternalHandle);

            if(BD_ADDRToStr(AddrBuffer, sizeof(AddrBuffer), BD_ADDR))
            {
               if((Address = Env->NewStringUTF(AddrBuffer)) != NULL)
               {
                  SocketObject = Env->NewObject(Class_BluetoothSocket, Method_BluetoothSocket_ctor, ProtocolType, (jint)(NatData->PortDataInternalHandle), RequireAuthentication, RequireEncryption, Address, -1);
                  Env->DeleteLocalRef(Address);

                  SS1_LOGD("Created client socket object @ %p", SocketObject);

                  if(SocketObject)
                     SS1_LOGD("Client socket object created successfully for device %s", AddrBuffer);
                  else
                  {
                     SS1_LOGE("Error creating client socket object for device %s", AddrBuffer);
                     jniThrowException(Env, "java/lang/OutOfMemoryError", "Could not allocate new BluetoothSocket object");
                  }
               }
               else
                  Result = ENOMEM;
            }
            else
               Result = EINVAL;
         }
         else
         {
            BTSO_ReleasePortData(Port);
            Port = NULL;
            SS1_LOGD("BTSO Port Mutex: Released (IH=%u)", NatData->PortDataInternalHandle);
         }
      }
      else
         SS1_LOGE("Unable to lock mutex");
   }
   else
      SS1_LOGE("Called on uninitialized object");

   if(Result != 0)
   {
      SS1_LOGD("Accept failed, throwing IOException(%d)", Result);
      jniThrowIOException(Env, Result);
   }

   if(NatData && Port)
      CHECK_MUTEX(&(Port->Mutex));

   SS1_LOGD("Exit (%p)", SocketObject);

   return(SocketObject);
#else /* HAVE_BLUETOOTH */
   jniThrowIOException(Env, ENOSYS);
   SS1_LOGD("Exit (%p)", NULL);
   return(NULL);
#endif
}

static jint BTSO_AvailableNative(JNIEnv *Env, jobject Object)
{
   /* Debug logs are disabled in this function because normal usage for */
   /* the BluetoothSocket API involves calling this function repeatedly.*/
   /* Under typical conditions, this would flood the system log with    */
   /* uninteresting entries.                                            */

   //SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   jint               ret_val;
   BTSO_PortData_t   *Port;
   BTSO_NativeData_t *NatData;

   ret_val = -1;
   NatData = NULL;

   /* Make sure this process has a connection to the Bluetopia daemon.  */
   if(InitBTPMClientNoRetries(NULL, NULL, NULL) == 0)
   {
      if((NatData = GetNativeData(Env, Object)) != NULL)
      {
         //SS1_LOGD("BTSO Port: Searching (IH=%u)", NatData->PortDataInternalHandle);
         if((Port = BTSO_AcquirePortData(NatData->PortDataInternalHandle)) != NULL)
         {
            //SS1_LOGD("BTSO Port: Acquired (IH=%u)", NatData->PortDataInternalHandle);

            if(NatData->Type == btsoTypeClient)
            {
               if(Port->ClientStatus == btsoClientConnected)
               {
                  if((ret_val = SPPM_ReadData(Port->Handle, SPPM_READ_DATA_READ_TIMEOUT_IMMEDIATE, 0, NULL)) < 0)
                  {
                     /* Check return code and determine appropriate     */
                     /* errno code to throw.                            */
                     SS1_LOGD("ReadData returned error %d for H=%u (IH=%u)", ret_val, Port->Handle, NatData->PortDataInternalHandle);
                     jniThrowIOException(Env, SPPMErrorToErrno(ret_val));
                     ret_val = -1;
                  }
                  else
                  {
                     SS1_LOGD("H=%u (IH=%u) has %d bytes available.", Port->Handle, NatData->PortDataInternalHandle, ret_val);
                  }
               }
               else
               {
                  ret_val = -1;
                  jniThrowIOException(Env, ENOTCONN);
               }
            }
            else
            {
               SS1_LOGE("Called on non-client object");
               jniThrowIOException(Env, EINVAL);
            }

            BTSO_ReleasePortData(Port);
            Port = NULL;
            //SS1_LOGD("BTSO Port: Released (IH=%u)", NatData->PortDataInternalHandle);
         }
         else
            SS1_LOGE("Unable to obtain lock");
      }
      else
         SS1_LOGE("Object uninitialized");
   }
   else
      SS1_LOGE("Unable to access Bluetooth platform");

   if(NatData && Port)
      CHECK_MUTEX(&(Port->Mutex));

   //SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH */
   jniThrowIOException(Env, ENOSYS);
   //SS1_LOGD("Exit (%d)", -1);
   return(-1);
#endif
}

static jint BTSO_ReadNative(JNIEnv *Env, jobject Object, jbyteArray Buffer, jint Offset, jint Length)
{
   // TODO Add protocol switch
   SS1_LOGD("Enter (%p, %d, %d)", Buffer, Offset, Length);
#ifdef HAVE_BLUETOOTH
   jint                ret_val;
   jint                BufferSize;
   jint                ProtocolType;
   jbyte              *BufferRawBytes;
   unsigned int        PortHandle;
   BTSO_PortData_t    *Port;
   BTSO_PortType_t     PortType;
   BTSO_NativeData_t  *NatData;
   BTSO_ClientStatus_t ClientStatus;

   SS1_LOGD("Called with Buffer = %p, Offset = %d, Length = %d", Buffer, Offset, Length);

   ret_val = -1;
   NatData = NULL;

   /* Make sure this process has a connection to the Bluetopia daemon.  */
   if(InitBTPMClientNoRetries(NULL, NULL, NULL) == 0)
   {
      if((NatData = GetNativeData(Env, Object)) != NULL)
      {
         SS1_LOGD("BTSO Port: Searching (IH=%u)", NatData->PortDataInternalHandle);
         if((Port = BTSO_AcquirePortData(NatData->PortDataInternalHandle)) != NULL)
         {
            SS1_LOGD("BTSO Port: Acquired (IH=%u)", NatData->PortDataInternalHandle);

            PortType     = NatData->Type;
            PortHandle   = Port->Handle;
            ClientStatus = Port->ClientStatus;

            BTSO_ReleasePortData(Port);
            Port = NULL;
            SS1_LOGD("BTSO Port: Released (IH=%u)", NatData->PortDataInternalHandle);

            /* Check that we have a valid, connected port. If this port */
            /* is associated with a listening server, make sure it's not*/
            /* the server port.                                         */
            if((PortHandle > 0) && (PortType == btsoTypeClient))
            {
               if(ClientStatus == btsoClientConnected)
               {
                  if(Buffer)
                  {
                     /* Short-circuit the processing if Length is zero. */
                     if(Length == 0)
                        ret_val = 0;
                     else
                     {
                        BufferSize = Env->GetArrayLength(Buffer);
                        if((Offset >= 0) && (Length >= 0) && (BufferSize >= (Offset + Length)))
                        {
                           ProtocolType = Env->GetIntField(Object, Field_mType);

                           switch(ProtocolType)
                           {
                              case SOCKET_TYPE_RFCOMM:
                                 SS1_LOGD("Reading from SPPM connection");

                                 /* Get a handle to the backing buffer  */
                                 /* of the Java Byte[] object.          */
                                 if((BufferRawBytes = Env->GetByteArrayElements(Buffer, NULL)) != NULL)
                                 {
                                    ret_val = SPPM_ReadData(PortHandle, SPPM_READ_DATA_READ_TIMEOUT_INFINITE, Length, (unsigned char *)&(BufferRawBytes[Offset]));
                                    SS1_LOGD("SPPM_ReadData finished with value %d", ret_val);

                                    /* Commit the buffer back to the    */
                                    /* Java Byte[].                     */
                                    Env->ReleaseByteArrayElements(Buffer, BufferRawBytes, 0);
                                 }

                                 if(ret_val < 0)
                                 {
                                    /* An error ocurred. Throw an       */
                                    /* IOException with a message       */
                                    /* appropriate for a BSD Socket API */
                                    /* error code.                      */
                                    jniThrowIOException(Env, SPPMErrorToErrno(ret_val));
                                    ret_val = -1;
                                 }

                                 /* DEBUG */
                                 if(ret_val == 0)
                                    SS1_LOGE("Server now reports %d bytes were available for reading", SPPM_ReadData(PortHandle, 0, 0, NULL));
                                 break;
                              case SOCKET_TYPE_SCO:
                                 SS1_LOGD("Reading from SCOM connection");

                                 /* SCO data is not actually expected   */
                                 /* to be retreivable in this manner.   */
                                 /* Instead, the Phone app uses this    */
                                 /* call to block a thread until the    */
                                 /* connection is closed.               */
                                 SS1_LOGD("BTSO Port: Searching (IH=%u)", NatData->PortDataInternalHandle);
                                 if((Port = BTSO_AcquirePortData(NatData->PortDataInternalHandle)) != NULL)
                                 {
                                    SS1_LOGD("BTSO Port: Acquired (IH=%u)", NatData->PortDataInternalHandle);

                                    SS1_LOGD("Waiting for a SCOM connection event (%p), H=%u IH=%u", &(Port->ClientConnectCond), Port->Handle, NatData->PortDataInternalHandle);
                                    BTSO_WaitOnPortCondition(Port, &(Port->ClientConnectCond), NULL);

                                    ClientStatus = Port->ClientStatus;

                                    BTSO_ReleasePortData(Port);
                                    Port = NULL;
                                    SS1_LOGD("BTSO Port: Released (IH=%u)", NatData->PortDataInternalHandle);

                                    if(ClientStatus == btsoClientDisconnected)
                                       jniThrowIOException(Env, EINVAL);
                                 }
                                 else
                                    jniThrowIOException(Env, EINVAL);

                                 break;
                              case SOCKET_TYPE_L2CAP:
                              default:
                                 SS1_LOGE("Unsupported socket type (%d)", ProtocolType);
                                 break;
                           }
                        }
                        else
                           jniThrowIOException(Env, EINVAL);
                     }
                  }
                  else
                     jniThrowIOException(Env, EINVAL);
               }
               else
                  jniThrowIOException(Env, ENOTCONN);
            }
            else
            {
               SS1_LOGE("Called on non-client object");
               jniThrowIOException(Env, EINVAL);
            }
         }
      }
      else
         jniThrowIOException(Env, EBADF);
   }
   else
   {
      jniThrowIOException(Env, ENETDOWN);
      SS1_LOGE("Unable to access Bluetooth platform");
   }

   if(NatData && Port)
      CHECK_MUTEX(&(Port->Mutex));

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#endif /* HAVE_BLUETOOTH */
   jniThrowIOException(Env, ENOSYS);
   SS1_LOGD("Exit (%d)", -1);
   return(-1);
}

static jint BTSO_WriteNative(JNIEnv *Env, jobject Object, jbyteArray Buffer, jint Offset, jint Length)
{
   // TODO Add protocol switch
   SS1_LOGD("Enter (%p, %d, %d)", Buffer, Offset, Length);
#ifdef HAVE_BLUETOOTH
   jint                ret_val;
   jint                BufferSize;
   jbyte              *BufferRawBytes;
   unsigned int        DataSent;
   unsigned int        PortHandle;
   unsigned int        ChunkLength;
   unsigned int        RemainingLength;
   BTSO_PortData_t    *Port;
   BTSO_PortType_t     PortType;
   BTSO_NativeData_t  *NatData;
   BTSO_ClientStatus_t ClientStatus;

   ret_val = 0;
   NatData = NULL;

   /* Make sure this process has a connection to the Bluetopia daemon.  */
   if(InitBTPMClientNoRetries(NULL, NULL, NULL) == 0)
   {
      if((NatData = GetNativeData(Env, Object)) != NULL)
      {
         SS1_LOGD("BTSO Port: Searching (IH=%u)", NatData->PortDataInternalHandle);
         if((Port = BTSO_AcquirePortData(NatData->PortDataInternalHandle)) != NULL)
         {
            SS1_LOGD("BTSO Port: Acquired (IH=%u)", NatData->PortDataInternalHandle);

            PortType     = NatData->Type;
            PortHandle   = Port->Handle;
            ClientStatus = Port->ClientStatus;

            BTSO_ReleasePortData(Port);
            Port = NULL;
            SS1_LOGD("BTSO Port: Released (IH=%u)", NatData->PortDataInternalHandle);

            if((PortHandle > 0) && (PortType == btsoTypeClient))
            {
               if(ClientStatus == btsoClientConnected)
               {
                  if(Buffer)
                  {
                     /* Short-circuit the processing if Length is zero. */
                     if(Length == 0)
                        ret_val = 0;
                     else
                     {
                        BufferSize = Env->GetArrayLength(Buffer);

                        if((Offset >= 0) && (Length > 0) && (BufferSize >= (Offset + Length)))
                        {
                           /* Get a handle to the backing buffer of the */
                           /* Java Byte[] object.                       */
                           if((BufferRawBytes = Env->GetByteArrayElements(Buffer, NULL)) != NULL)
                           {
                              DataSent = 0;
                              RemainingLength = Length;

                              /* Loop until we have sent all the data.  */
                              while((ret_val >= 0) && (DataSent < (unsigned int)Length))
                              {
                                 /* Send as much data as we can in our  */
                                 /* maximum chunk size.                 */
                                 ret_val = SPPM_WriteData(PortHandle, SPPM_WRITE_DATA_WRITE_TIMEOUT_INFINITE, (RemainingLength < BTSO_MAXIMUM_CHUNK_SIZE)?RemainingLength:BTSO_MAXIMUM_CHUNK_SIZE, (unsigned char *)&(BufferRawBytes[Offset+DataSent]));
                                 SS1_LOGD("SPPM_WriteData finished with value %d", ret_val);

                                 if(ret_val < 0)
                                 {
                                    /* An error ocurred. Throw an       */
                                    /* IOException with a message       */
                                    /* appropriate for a BSD Socket API */
                                    /* error code.                      */
                                    jniThrowIOException(Env, SPPMErrorToErrno(ret_val));
                                    ret_val = -1;
                                 }
                                 else
                                 {
                                    /* Update how much data we actually */
                                    /* were able to send.               */
                                    DataSent += ret_val;
                                    RemainingLength -= ret_val;
                                 }
                              }

                              if(ret_val >= 0)
                                 ret_val = DataSent;

                              /* Release the buffer reference.          */
                              Env->ReleaseByteArrayElements(Buffer, BufferRawBytes, 0);

                           }
                           else
                           {
                              jniThrowException(Env, "java/lang/OutOfMemoryException", NULL);
                              ret_val = -1;
                           }
                        }
                        else
                           jniThrowIOException(Env, EINVAL);
                     }
                  }
                  else
                     jniThrowIOException(Env, EINVAL);
               }
               else
                  jniThrowIOException(Env, EBADF);
            }
            else
               jniThrowIOException(Env, EINVAL);
         }
      }
      else
         jniThrowIOException(Env, EBADF);
   }
   else
   {
      jniThrowIOException(Env, ENETDOWN);
      SS1_LOGE("Unable to access Bluetooth platform");
   }

   if(NatData && Port)
      CHECK_MUTEX(&(Port->Mutex));

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH */
   jniThrowIOException(Env, ENOSYS);
   SS1_LOGD("Exit (%d)", -1);
   return(-1);
#endif
}

static void BTSO_AbortNative(JNIEnv *Env, jobject Object)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   jint               ProtocolType;
   BTSO_PortData_t   *Port;
   BTSO_NativeData_t *NatData;

   /* Connect to the Bluetooth daemon, but only try once. Since we're   */
   /* only closing ports, if the service is down, then the connections  */
   /* are already closed. If we only lost IPC connectivity, the daemon  */
   /* should have closed our connections for us when it noticed we had  */
   /* dropped off.                                                      */
   InitBTPMClientNoRetries(NULL, NULL, NULL);

   if((NatData = GetNativeData(Env, Object)) != NULL)
   {
      SS1_LOGD("BTSO Port: Searching (IH=%u)", NatData->PortDataInternalHandle);
      if((Port = BTSO_AcquirePortData(NatData->PortDataInternalHandle)) != NULL)
      {
         SS1_LOGD("BTSO Port: Acquired (IH=%u)", NatData->PortDataInternalHandle);

         if(Port->Handle > 0)
         {
            if(NatData->Type == btsoTypeServer)
            {
               SS1_LOGD("Called on Server object (%u)", Port->ServerStatus);
               /* This function was called on the Server object.        */
               if(Port->ServerStatus == btsoServerListening)
               {
                  SS1_LOGD("Signalling \"client accepted\" condition to interrupt any pending acceptNative() call.");
                  /* If there's a thread waiting on an Accept event,    */
                  /* interrupt it. Update the server status so the      */
                  /* waiting thread knows we interrupted it on purpose. */
                  Port->ServerStatus = btsoServerAborted;
                  SS1_LOGD("Broadcast ClientAcceptCond (%p) for PortData @ %p, H=%u IH=%u - Server Aborted", &(Port->ClientAcceptCond), Port, Port->Handle, NatData->PortDataInternalHandle);
                  pthread_cond_broadcast(&(Port->ClientAcceptCond));
               }

               //XXX Currently, just rejecting in authorization handler.
               //
               ///* If no client is connected, we can safely unregister   */
               ///* the server socket -- this will immediately prevent    */
               ///* further connections from being accepted by the socket.*/
               //if(Port->ClientStatus == btsoClientDisconnected)
               //{
               //   SS1_LOGD("No client connected on aborted server port (H=%u IH=%u), so unregistering server.", Port->Handle, NatData->PortDataInternalHandle);
               //   SPPM_UnRegisterServerPort(Port->Handle);
               //}
            }
            else
            {
               /* Don't assume we're a client just because we're not    */
               /* a server. It's possible the Java object is being      */
               /* destroyed (which involves a call to Abort()) before   */
               /* any call to Connect() or BindListen(), leaving the    */
               /* socket in an uninitialized state.                     */
               if(NatData->Type == btsoTypeClient)
               {
                  SS1_LOGD("Called on Client object (%u)", Port->ClientStatus);
                  /* This function was called on the client object.     */
                  if(Port->ClientStatus == btsoClientConnected)
                  {
                     SS1_LOGD("Client is connected, so closing port to interrupt pending read/writeNative() calls.");
                     /* Technically, Abort() isn't supposed to close    */
                     /* the port, but it's the only way to immediately  */
                     /* interrupt a blocking Read() or Write(), which   */
                     /* is the expected behavior of Abort(). In         */
                     /* BluetoothSocket, Abort() is only called from    */
                     /* within Close(), and is followed immediately     */
                     /* by Destroy() (which _is_ supposed to close      */
                     /* the port), so this side-effect should not be    */
                     /* noticable in practice.                          */
                     Port->ClientStatus = btsoClientDisconnected;

                     /* Wake up any threads waiting asynchronously for a*/
                     /* outgoing connection to complete.                */
                     SS1_LOGD("Broadcast ClientConnectCond (%p) for PortData @ %p, H=%u IH=%u - Client Disconnected", &(Port->ClientConnectCond), Port, Port->Handle, NatData->PortDataInternalHandle);
                     pthread_cond_broadcast(&(Port->ClientConnectCond));

                     ProtocolType = Env->GetIntField(Object, Field_mType);

                     /* Close the client connection. How this is done   */
                     /* depends on the socket type.                     */
                     switch(ProtocolType)
                     {
                        case SOCKET_TYPE_RFCOMM:
                           SS1_LOGD("Closing SPPM connection from TID 0x%x for Port %p", gettid(), Port);
                           SPPM_ClosePort(Port->Handle, BTSO_CLOSE_PORT_TIMEOUT);
                           break;
                        case SOCKET_TYPE_SCO:
                           SS1_LOGD("Closing SCOM connection from TID 0x%x for Port %p", gettid(), Port);
                           SCOM_CloseConnection(Port->Handle);
                           break;
                        case SOCKET_TYPE_L2CAP:
                        default:
                           SS1_LOGE("Unsupported socket type (%d)", ProtocolType);
                           break;
                     }
                  }
               }
               else
                  SS1_LOGE("Called on uninitialized object");
            }
         }

         BTSO_ReleasePortData(Port);
         Port = NULL;
         SS1_LOGD("BTSO Port: Released (IH=%u)", NatData->PortDataInternalHandle);
      }
   }

   if(NatData && Port)
      CHECK_MUTEX(&(Port->Mutex));

#else /* HAVE_BLUETOOTH */
   jniThrowIOException(Env, ENOSYS);
#endif
   SS1_LOGD("Exit");
}

static void BTSO_DestroyNative(JNIEnv *Env, jobject Object)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   jint               ProtocolType;
   unsigned int       Count;
   unsigned int       InternalHandle;
   BTSO_PortData_t   *PortData;
   BTSO_NativeData_t *NatData;

   /* Connect to the Bluetooth daemon, but only try once. Since we're   */
   /* only closing ports, if the service is down, then the connections  */
   /* are already closed. If we only lost IPC connectivity, the daemon  */
   /* should have closed our connections for us when it noticed we had  */
   /* dropped off.                                                      */
   InitBTPMClientNoRetries(NULL, NULL, NULL);

   if((NatData = GetNativeData(Env, Object)) != NULL)
   {
      Env->SetIntField(Object, Field_mSocketData, (jint)NULL);

      InternalHandle = NatData->PortDataInternalHandle;

      free(NatData);

      PortData = BTSO_AcquirePortData(InternalHandle);
      SS1_LOGD("Got port data @ %p and destroyed native data", PortData);

      if(PortData)
      {
         Count = BTSO_DecRefCount(&(PortData->RefCount));
         SS1_LOGD("Port data reference count == %u", Count);

         if(Count == 0)
         {
            ProtocolType = Env->GetIntField(Object, Field_mType);

            /* The reference count on the Port data reached zero, so    */
            /* we held the last claimed reference. We should clean up   */
            /* any contained resources, now. There's no need to lock    */
            /* the structure's mutex since no one else should have a    */
            /* reference to the object at this point.                   */
            if(PortData->ClientStatus != btsoClientDisconnected)
            {
               SS1_LOGD("Closing client connection");
               PortData->ClientStatus = btsoClientDisconnected;

               /* We hold the last reference, so there shouldn't be     */
               /* anyone waiting on events. Signal them just in case.   */
               SS1_LOGD("Broadcast ClientConnectCond (%p) for PortData @ %p, H=%u IH=%u - Client Disconnected", &(PortData->ClientConnectCond), PortData, PortData->Handle, PortData->InternalHandle);
               pthread_cond_broadcast(&(PortData->ClientConnectCond));

               /* Close the client connection. How this is done depends */
               /* on the socket type.                                   */
               switch(ProtocolType)
               {
                  case SOCKET_TYPE_RFCOMM:
                     SPPM_ClosePort(PortData->Handle, BTSO_CLOSE_PORT_TIMEOUT);
                     break;
                  case SOCKET_TYPE_SCO:
                     SCOM_CloseConnection(PortData->Handle);
                     break;
                  case SOCKET_TYPE_L2CAP:
                  default:
                     SS1_LOGE("Unsupported socket type (%d)", ProtocolType);
                     break;
               }
            }

            if((PortData->ServerStatus == btsoServerListening) || (PortData->ServerStatus == btsoServerAborted))
            {
               SS1_LOGD("Closing server registration");
               PortData->ServerStatus = btsoServerClosed;

               /* We hold the last reference, so there shouldn't be     */
               /* anyone waiting on events. Signal them just in case.   */
               SS1_LOGD("Broadcast ClientAcceptCond (%p) for PortData @ %p, H=%u IH=%u - Server Closed", &(PortData->ClientAcceptCond), PortData, PortData->Handle, PortData->InternalHandle);
               pthread_cond_broadcast(&(PortData->ClientAcceptCond));

               switch(ProtocolType)
               {
                  case SOCKET_TYPE_RFCOMM:
                     SPPM_UnRegisterServerPort(PortData->Handle);
                     break;
                  case SOCKET_TYPE_SCO:
                     SCOM_UnRegisterServerConnection(PortData->Handle);
                     break;
                  case SOCKET_TYPE_L2CAP:
                  default:
                     SS1_LOGE("Unsupported socket type (%d)", ProtocolType);
                     break;
               }
            }

            /* Release the PortData and yield to pending threads to     */
            /* allow any thread awoken by the above broadcasts the      */
            /* chance to respond to the port closure.                   */
            BTSO_ReleasePortData(PortData);
            sched_yield();

            /* Reacquire the PortData and destroy it.                   */
            if((PortData = BTSO_AcquirePortData(InternalHandle)) != NULL)
            {
               BTSO_DestroyPortData(PortData);
               SS1_LOGD("Destroyed port data @ %p", PortData);
            }
            else
               SS1_LOGD("Port data (IH=%u) already destroyed.", InternalHandle);
         }
         else
         {
            if((signed int)Count == -1)
               SS1_LOGE("Reference count for port data @ %p was already 0. Most likely, another thread is in the process of destroying it.", PortData);
            else
               SS1_LOGD("Reference count == %u for port data @ %p (IH %u), not destroying", Count, PortData, (PortData ? PortData->InternalHandle : 0));

            BTSO_ReleasePortData(PortData);
         }
      }
      else
         SS1_LOGD("Port data uninitialized");
   }
   else
      SS1_LOGE("Called on object with uninitialized data!");

#else /* HAVE_BLUETOOTH */
   jniThrowIOException(Env, ENOSYS);
#endif
   SS1_LOGD("Exit");
}

static void BTSO_ThrowErrnoNative(JNIEnv *Env, jobject Object, jint Err)
{
   jniThrowIOException(Env, Err);
}

static JNINativeMethod NativeMethods[] =
{
   {"initSocketNative",       "()V",                                    (void *) BTSO_InitSocketNative},
   {"initSocketFromFdNative", "(I)V",                                   (void *) BTSO_InitSocketFromFdNative},
   {"connectNative",          "()V",                                    (void *) BTSO_ConnectNative},
   {"bindListenNative",       "()I",                                    (void *) BTSO_BindListenNative},
   {"acceptNative",           "(I)Landroid/bluetooth/BluetoothSocket;", (void *) BTSO_AcceptNative},
   {"availableNative",        "()I",                                    (void *) BTSO_AvailableNative},
   {"readNative",             "([BII)I",                                (void *) BTSO_ReadNative},
   {"writeNative",            "([BII)I",                                (void *) BTSO_WriteNative},
   {"abortNative",            "()V",                                    (void *) BTSO_AbortNative},
   {"destroyNative",          "()V",                                    (void *) BTSO_DestroyNative},
   {"throwErrnoNative",       "(I)V",                                   (void *) BTSO_ThrowErrnoNative},
};

int SS1API register_android_bluetooth_BluetoothSocket(JNIEnv *Env)
{
   int    ret_val;
   jclass Clazz;

   if((Clazz = Env->FindClass("android/bluetooth/BluetoothSocket")) != NULL)
   {
      Class_BluetoothSocket       = (jclass)(Env->NewGlobalRef(Clazz));

      Field_mType                 = Env->GetFieldID(Clazz, "mType", "I");
      Field_mAddress              = Env->GetFieldID(Clazz, "mAddress", "Ljava/lang/String;");
      Field_mPort                 = Env->GetFieldID(Clazz, "mPort", "I");
      Field_mAuth                 = Env->GetFieldID(Clazz, "mAuth", "Z");
      Field_mEncrypt              = Env->GetFieldID(Clazz, "mEncrypt", "Z");
      Field_mSocketData           = Env->GetFieldID(Clazz, "mSocketData", "I");
      Method_BluetoothSocket_ctor = Env->GetMethodID(Clazz, "<init>", "(IIZZLjava/lang/String;I)V");

      ret_val = AndroidRuntime::registerNativeMethods(Env, "android/bluetooth/BluetoothSocket", NativeMethods, NELEM(NativeMethods));
   }
   else
      ret_val = -1;

   return(ret_val);
}

#ifdef HAVE_BLUETOOTH

   /* Must be called while PortDataListMutex is held.                   */
static BTSO_PortData_t *AddPortDataDirect(BTSO_PortData_t *EntryToAdd)
{
   BTSO_PortData_t *tmpEntry;

   /* First let's verify that values passed in are semi-valid.          */
   if((EntryToAdd) && (!EntryToAdd->NextEntry))
   {
      /* First, let's check to see if there are any elements already    */
      /* present in the List that was passed in.                        */
      if((tmpEntry = PortDataList) != NULL)
      {
         /* Head Pointer was not NULL, so we will traverse the list     */
         /* until we reach the last element.                            */
         while(tmpEntry)
         {
            if(tmpEntry->InternalHandle == EntryToAdd->InternalHandle)
            {
               /* Entry was already added, so flag an error to the      */
               /* caller and abort the search.                          */
               EntryToAdd = NULL;
               tmpEntry   = NULL;
            }
            else
            {
               /* OK, we need to see if we are at the last element of   */
               /* the List. If we are, we simply break out of the list  */
               /* traversal because we know there are NO duplicates AND */
               /* we are at the end of the list.                        */
               if(tmpEntry->NextEntry)
                  tmpEntry = tmpEntry->NextEntry;
               else
                  break;
            }
         }

         if(EntryToAdd)
         {
            /* Last element found, simply Add the entry.                */
            tmpEntry->NextEntry   = EntryToAdd;
            EntryToAdd->NextEntry = NULL;
            SS1_LOGD("Added Entry %p w/ IH=%u", EntryToAdd, EntryToAdd->InternalHandle);
         }
         else
            SS1_LOGD("Unable to add Entry %p w/ IH=%u", EntryToAdd, EntryToAdd->InternalHandle);
      }
      else
         PortDataList = EntryToAdd;
   }
   else
      EntryToAdd = NULL;

   return(EntryToAdd);
}

   /* Must be called while PortDataListMutex is held.                   */
static BTSO_PortData_t *GetPortDataByInternalHandle(unsigned int InternalHandle)
{
   BTSO_PortData_t *FoundEntry;

   SS1_LOGD("Searching for Entry w/ IH=%u", InternalHandle);

   /* Now, let's search the list until we find the correct entry.       */
   FoundEntry = PortDataList;

   while((FoundEntry) && (FoundEntry->InternalHandle != InternalHandle))
      FoundEntry = FoundEntry->NextEntry;

   if(FoundEntry)
      SS1_LOGD("Found Entry %p", FoundEntry);
   else
      SS1_LOGD("Entry not found");

   return(FoundEntry);
}

   /* Must be called while PortDataListMutex is held.                   */
static BTSO_PortData_t *RemovePortDataByInternalHandle(unsigned int InternalHandle)
{
   BTSO_PortData_t *FoundEntry = NULL;
   BTSO_PortData_t *LastEntry  = NULL;

   SS1_LOGD("Searching to remove Entry w/ IH=%u", InternalHandle);

   /* Now, let's search the list until we find the correct entry.       */
   FoundEntry = PortDataList;

   while((FoundEntry) && (FoundEntry->InternalHandle != InternalHandle))
   {
      LastEntry  = FoundEntry;
      FoundEntry = FoundEntry->NextEntry;
   }

   /* Check to see if we found the specified entry.                     */
   if(FoundEntry)
   {
      SS1_LOGD("Removed Entry %p w/ IH=%u", FoundEntry, InternalHandle);

      /* OK, now let's remove the entry from the list. We have to check */
      /* to see if the entry was the first entry in the list.           */
      if(LastEntry)
      {
         /* Entry was NOT the first entry in the list.                  */
         LastEntry->NextEntry = FoundEntry->NextEntry;
      }
      else
         PortDataList = FoundEntry->NextEntry;

      FoundEntry->NextEntry = NULL;
   }
   else
      SS1_LOGD("Unable to find Entry w/ IH=%u", InternalHandle);

   return(FoundEntry);
}

int BTSO_CreatePortData()
{
   int              ret_val;
   BTSO_PortData_t *PortData;

   static unsigned int NextInternalHandle = 0;

   if((PortData = (BTSO_PortData_t *)malloc(sizeof(BTSO_PortData_t))) != NULL)
   {
      memset(PortData, 0, sizeof(BTSO_PortData_t));

      PortData->ServerStatus = btsoServerUninitialized;
      PortData->ClientStatus = btsoClientDisconnected;

      BTSO_InitRefCount(&(PortData->RefCount));

      pthread_cond_init(&(PortData->ClientAcceptCond), NULL);
      pthread_cond_init(&(PortData->ClientConnectCond), NULL);
      pthread_mutex_init(&(PortData->Mutex), NULL);

      if(!pthread_mutex_lock(&PortDataListMutex))
      {
         PortData->InternalHandle = NextInternalHandle;

         SS1_LOGD("Creating PortData with IH=%u", PortData->InternalHandle);

         if(AddPortDataDirect(PortData) != NULL)
         {
            /* Success.  Return the handle for this PortData.           */
            ret_val = (int)PortData->InternalHandle;

            /* Prepare the internal handle for the next PortData.       */
            NextInternalHandle       = ((NextInternalHandle + 1) % INT_MAX);
         }
         else
         {
            /* Failed to add the new PortData to the list.              */
            ret_val = -1;
         }

         pthread_mutex_unlock(&PortDataListMutex);
      }
      else
      {
         /* Failed to lock the list.                                    */
         ret_val = -1;
      }

      if(ret_val < 0)
      {
         /* Initialization failed. Clean up all the allocated resources.*/
         BTSO_DestroyRefCount(&(PortData->RefCount));
         pthread_cond_destroy(&(PortData->ClientAcceptCond));
         pthread_cond_destroy(&(PortData->ClientConnectCond));
         pthread_mutex_destroy(&(PortData->Mutex));

         free(PortData);

         PortData = NULL;
      }
   }
   else
      ret_val = -1;

   return(ret_val);
}

   /* Upon successful return of the requested PortData, the PortData    */
   /* will be locked.  On error, this will return NULL.                 */
BTSO_PortData_t *BTSO_AcquirePortData(unsigned int InternalHandle)
{
   int              Result;
   BTSO_PortData_t *PortData;

   if(!pthread_mutex_lock(&PortDataListMutex))
   {
      Result = EBUSY;

      while(Result == EBUSY)
      {
         if((PortData = GetPortDataByInternalHandle(InternalHandle)) != NULL)
         {
            Result = pthread_mutex_trylock(&(PortData->Mutex));

            if(Result)
            {
               /* We failed to acquire the lock, so abandon the PortData*/
               /* we located.                                           */
               PortData = NULL;

               /* If the failure was due to the lock already being held,*/
               /* wait for the lock to be released.                     */
               if(Result == EBUSY)
                  pthread_cond_wait(&PortDataReleased, &PortDataListMutex);
            }
         }
         else
            Result = 0;
      }

      pthread_mutex_unlock(&PortDataListMutex);
   }
   else
      PortData = NULL;

   return(PortData);
}

   /* The PortData provided here must be locked.  In other words, it    */
   /* must have been obtained via BTSO_AcquirePortData.                 */
void BTSO_ReleasePortData(BTSO_PortData_t *PortData)
{
   if(PortData)
   {
      /* First, acquire the list mutex to prevent other threads from    */
      /* waking up until the the PortData mutex is cleared.             */
      pthread_mutex_lock(&PortDataListMutex);

      /* Wake up threads waiting for a PortData to be released.         */
      /* ** NOTE ** This will wake up ALL threads waiting in            */
      /*            BTSO_AcquirePortData, regardless of the             */
      /*            InternalHandle they want.  Each thread will rescan  */
      /*            the list for its desired PortData and re-attempt    */
      /*            to lock it.  Any threads waiting on the same        */
      /*            InternalHandle will race for ownership.             */
      pthread_cond_broadcast(&PortDataReleased);

      /* Unlock this PortData.                                          */
      pthread_mutex_unlock(&(PortData->Mutex));

      /* Unlock the list mutex.                                         */
      pthread_mutex_unlock(&PortDataListMutex);
   }
}

   /* The PortData provided here must be locked.  In other words, it    */
   /* must have been obtained via BTSO_AcquirePortData.                 */
bool BTSO_DestroyPortData(BTSO_PortData_t *PortData)
{
   bool             ret_val;
   unsigned int     InternalHandle;
   BTSO_PortData_t *DeletedPort;

   if(PortData)
   {
      InternalHandle = PortData->InternalHandle;

      if(!pthread_mutex_lock(&PortDataListMutex))
      {
         if((DeletedPort = RemovePortDataByInternalHandle(InternalHandle)) != NULL)
         {
            /* Wake up threads waiting for a PortData to be released.   */
            /* ** NOTE ** This will wake up ALL threads waiting in      */
            /*            BTSO_AcquirePortData, regardless of the       */
            /*            InternalHandle they want.  Each thread will   */
            /*            rescan the list for its desired PortData and  */
            /*            re-attempt to lock it.  Any threads waiting on*/
            /*            this PortData, currently being deleted, will  */
            /*            find that its InternalHandle can no longer be */
            /*            found in the list and will fail.              */
            pthread_cond_broadcast(&PortDataReleased);

            pthread_mutex_unlock(&PortDataListMutex);

            BTSO_DestroyRefCount(&(DeletedPort->RefCount));

            while(pthread_cond_destroy(&(DeletedPort->ClientAcceptCond)) == EBUSY)
            {
               /* Other threads are waiting on this condition.  Wake    */
               /* them up, release the mutex, and yield, to give these  */
               /* threads a chance to respond to the broadcast.  Then   */
               /* re-acquire the PortData's mutex when those threads are*/
               /* finished.                                             */
               pthread_cond_broadcast(&(DeletedPort->ClientAcceptCond));

               pthread_mutex_unlock(&(DeletedPort->Mutex));
               sched_yield();
               pthread_mutex_lock(&(DeletedPort->Mutex));
            }

            while(pthread_cond_destroy(&(DeletedPort->ClientConnectCond)) == EBUSY)
            {
               /* Other threads are waiting on this condition.  Wake    */
               /* them up, release the mutex, and yield, to give these  */
               /* threads a chance to respond to the broadcast.  Then   */
               /* re-acquire the PortData's mutex when those threads are*/
               /* finished.                                             */
               pthread_cond_broadcast(&(DeletedPort->ClientConnectCond));

               pthread_mutex_unlock(&(DeletedPort->Mutex));
               sched_yield();
               pthread_mutex_lock(&(DeletedPort->Mutex));
            }

            pthread_mutex_unlock(&(DeletedPort->Mutex));

            while(pthread_mutex_destroy(&(DeletedPort->Mutex)) == EBUSY)
            {
               /* The mutex couldn't be destroyed because the mutex is  */
               /* held by another thread.  Wait for the mutex to become */
               /* available, then re-attempt to destroy it.             */
               SS1_LOGD("Waiting for port data to become unused");
               pthread_mutex_lock(&(DeletedPort->Mutex));

               /* The mutex must be released before it can be destroyed.*/
               pthread_mutex_unlock(&(DeletedPort->Mutex));

               /* Yield to other threads to make sure that any thread   */
               /* waiting on this mutex can acquire and release it      */
               /* before we attempt to delete it, again.                */
               sched_yield();
            }

            /* All resources have been freed, so we can delete the      */
            /* actual PortData.                                         */
            free(DeletedPort);

            SS1_LOGD("Port data (%p, IH=%u) destroyed", DeletedPort, InternalHandle);

            ret_val = true;
         }
         else
         {
            pthread_mutex_unlock(&PortDataListMutex);

            SS1_LOGD("Port requested for deletion not found (IH=%u). This probably means it has already been deleted.", InternalHandle);
            ret_val = false;
         }
      }
      else
         ret_val = false;
   }
   else
      ret_val = false;

   return(ret_val);
}

   /* The PortData provided here must be locked.  In other words, it    */
   /* must have been obtained via BTSO_AcquirePortData.                 */
int BTSO_WaitOnPortCondition(BTSO_PortData_t *PortData, pthread_cond_t *Condition, struct timespec *Timeout)
{
   int ret_val;

   if((PortData) && (Condition))
   {
      /* Confirm that the provided condition really belongs to the      */
      /* specified port data.                                           */
      if((Condition == &(PortData->ClientAcceptCond)) || (Condition == &(PortData->ClientConnectCond)))
      {
         /* Announce that the PortData is about to be released.         */
         pthread_cond_broadcast(&PortDataReleased);

         /* Wait for the condition.  The PortData mutex will be         */
         /* released before the wait begins, and will be automatically  */
         /* re-acquired after the condition is triggered.               */
         if(Timeout)
            ret_val = pthread_cond_timedwait(Condition, &(PortData->Mutex), Timeout);
         else
            ret_val = pthread_cond_wait(Condition, &(PortData->Mutex));
      }
      else
         ret_val = EINVAL;
   }
   else
      ret_val = EINVAL;

   return(ret_val);
}

   /* The PortData provided here must be locked.  In other words, it    */
   /* must have been obtained via BTSO_AcquirePortData.                 */
int BTSO_WaitOnPortConditionMS(BTSO_PortData_t *PortData, pthread_cond_t *Condition, unsigned int TimeoutMS)
{
   int ret_val;

   if((PortData) && (Condition))
   {
      /* Confirm that the provided condition really belongs to the      */
      /* specified port data.                                           */
      if((Condition == &(PortData->ClientAcceptCond)) || (Condition == &(PortData->ClientConnectCond)))
      {
         /* Announce that the PortData is about to be released.         */
         pthread_cond_broadcast(&PortDataReleased);

         /* Wait for the condition.  The PortData mutex will be         */
         /* released before the wait begins, and will be automatically  */
         /* re-acquired after the condition is triggered.               */
         PthreadCondWaitMS(Condition, &(PortData->Mutex), TimeoutMS);
      }
      else
         ret_val = EINVAL;
   }
   else
      ret_val = EINVAL;

   return(ret_val);
}

int SPPMErrorToErrno(int SPPMError)
{
   int error;

   switch(SPPMError)
   {
      //XXX Add SCOM Errors
      case SPPM_OPEN_REMOTE_PORT_STATUS_FAILURE_TIMEOUT:
      case BTPM_ERROR_CODE_UNABLE_TO_CONNECT_REMOTE_SERIAL_PORT:
         error = ETIMEDOUT;
         break;
      case SPPM_OPEN_REMOTE_PORT_STATUS_FAILURE_REFUSED:
      case BTPM_ERROR_CODE_REMOTE_SERIAL_PORT_CONNECTION_REFUSED:
         error = ECONNREFUSED;
         break;
      case SPPM_OPEN_REMOTE_PORT_STATUS_FAILURE_SECURITY:
         error = EPERM;
         break;
      case SPPM_OPEN_REMOTE_PORT_STATUS_FAILURE_DEVICE_POWER_OFF:
         error = ENETUNREACH;
         break;
      case SPPM_OPEN_REMOTE_PORT_STATUS_FAILURE_UNKNOWN:
         error = EIO;
         break;
      case BTPM_ERROR_CODE_SERIAL_PORT_CLOSE_IN_PROGRESS:
      case BTPM_ERROR_CODE_INVALID_SERIAL_PORT:
         error = EBADF;
         break;
      case BTPS_ERROR_RFCOMM_ADDING_SERVER_INFORMATION:
         error = EADDRINUSE;
         break;
      default:
         SS1_LOGW("Unknown SPPM error (%d)", SPPMError);
         error = -1;
   }

   return(error);
}


void BTSO_SPPM_EventCallback(SPPM_Event_Data_t *EventData, void *CallbackParameter)
{
   SS1_LOGD("Enter (%p, %p)", EventData, CallbackParameter);

   BD_ADDR_t        RemoteAddress;
   unsigned int     Result;
   BTSO_PortData_t *PortData;

   SS1_LOGD("Called with IH = %u", (unsigned int)CallbackParameter);
   SS1_LOGD("Called from TID 0x%x", gettid());

   if(EventData)
   {
      SS1_LOGD("BTSO Port: Searching (IH=%u)", (unsigned int)CallbackParameter);
      if((PortData = BTSO_AcquirePortData((unsigned int)CallbackParameter)) != NULL)
      {
         SS1_LOGD("BTSO Port: Acquired (IH=%u) (%p)", (unsigned int)CallbackParameter, PortData);

         switch(EventData->EventType)
         {
            case setServerPortOpenRequest:
               SS1_LOGD("Signal: (SPPM) ServerPortOpenRequest (%d)", EventData->EventData.ServerPortOpenRequestEventData.PortHandle);
               /* We should only see this message if                    */
               /* SPPM_REGISTER_SERVER_PORT_FLAGS_REQUIRE_AUTHORIZATION */
               /* is set on the server port.                            */
               if(PortData->Handle != EventData->EventData.ServerPortOpenRequestEventData.PortHandle)
                  SS1_LOGE("ERROR: ServerPortOpenRequest event is for H=%u, but callback data is for H=%u (%p)", EventData->EventData.ServerPortOpenRequestEventData.PortHandle, PortData->Handle, PortData);

               if(PortData->ServerStatus == btsoServerListening)
               {
                  if(PortData->AuthorizationHandler)
                     Result = (*PortData->AuthorizationHandler)(PortData->RemoteAddress);
                  else
                     Result = TRUE;
               }
               else
               {
                  /* This server has been closed or aborted, so reject  */
                  /* any connection attempts.                           */
                  Result = FALSE;
               }

               SPPM_OpenServerPortRequestResponse(PortData->Handle, (Result ? TRUE : FALSE));
               break;

            case setServerPortOpen:
               SS1_LOGD("Signal: (SPPM) ServerPortOpen (%d, %02X:%02X:%02X:%02X:%02X:%02X)", EventData->EventData.ServerPortOpenEventData.PortHandle, EventData->EventData.ServerPortOpenEventData.RemoteDeviceAddress.BD_ADDR5, EventData->EventData.ServerPortOpenEventData.RemoteDeviceAddress.BD_ADDR4, EventData->EventData.ServerPortOpenEventData.RemoteDeviceAddress.BD_ADDR3, EventData->EventData.ServerPortOpenEventData.RemoteDeviceAddress.BD_ADDR2, EventData->EventData.ServerPortOpenEventData.RemoteDeviceAddress.BD_ADDR1, EventData->EventData.ServerPortOpenEventData.RemoteDeviceAddress.BD_ADDR0);
               if(PortData->Handle != EventData->EventData.ServerPortOpenEventData.PortHandle)
                  SS1_LOGE("ERROR: ServerPortOpen event is for H=%u, but callback data is for H=%u (%p)", EventData->EventData.ServerPortOpenEventData.PortHandle, PortData->Handle, PortData);

               if(PortData->ClientStatus == btsoClientDisconnected)
               {
                  PortData->ClientStatus  = btsoClientAccepting;
                  PortData->RemoteAddress = EventData->EventData.ServerPortOpenEventData.RemoteDeviceAddress;
                  /* It only makes sense for a single thread to         */
                  /* accept an incoming connection, so announce the     */
                  /* connection with pthread_cond_signal() rather than  */
                  /* pthread_cond_broadcast(). Having more than one     */
                  /* thread block on an accept event doesn't make sense */
                  /* anyway since each SPP server channel can support   */
                  /* only one client connection at a time, so a second  */
                  /* thread would never have a connection to handle     */
                  /* concurrently.                                      */
                  SS1_LOGD("Signal ClientAcceptCond (%p) for PortData @ %p, H=%u IH=%u - Client Accepting", &(PortData->ClientAcceptCond), PortData, PortData->Handle, PortData->InternalHandle);
                  pthread_cond_signal(&(PortData->ClientAcceptCond));
               }
               else
                  SS1_LOGE("Client Port Opened message received while not in Disconnected state (Old state = %d)", PortData->ClientStatus);
               break;

            case setPortClose:
               SS1_LOGD("Signal: (SPPM) PortClose (%d)", EventData->EventData.PortCloseEventData.PortHandle);
               if(PortData->Handle != EventData->EventData.PortCloseEventData.PortHandle)
                  SS1_LOGE("ERROR: PortClose event is for H=%u, but callback data is for H=%u (%p)", EventData->EventData.PortCloseEventData.PortHandle, PortData->Handle, PortData);

               if(PortData->ClientStatus != btsoClientDisconnected)
               {
                  /* Set the new state and wake up any threads waiting  */
                  /* on events from this port.                          */
                  SS1_LOGE("Client Port (H=%u IH=%u) switched from state %u to closed", PortData->Handle, PortData->InternalHandle, PortData->ClientStatus);
                  PortData->ClientStatus = btsoClientDisconnected;
                  SS1_LOGD("Broadcast ClientAcceptCond (%p) for PortData @ %p, H=%u IH=%u - Client Disconnected", &(PortData->ClientAcceptCond), PortData, PortData->Handle, PortData->InternalHandle);
                  pthread_cond_broadcast(&(PortData->ClientAcceptCond));
                  SS1_LOGD("Broadcast ClientConnectCond (%p) for PortData @ %p, H=%u IH=%u - Client Disconnected", &(PortData->ClientConnectCond), PortData, PortData->Handle, PortData->InternalHandle);
                  pthread_cond_broadcast(&(PortData->ClientConnectCond));
               }
               else
                  SS1_LOGE("Client Port Closed message received while already in Disconnected state (%d)", PortData->ClientStatus);
               break;

            case setRemotePortOpenStatus:
               SS1_LOGD("Signal: (SPPM) RemotePortOpenStatus (%d, %d)", EventData->EventData.RemotePortOpenStatusEventData.PortHandle, EventData->EventData.RemotePortOpenStatusEventData.Status);
               if(PortData->Handle != EventData->EventData.RemotePortOpenStatusEventData.PortHandle)
                  SS1_LOGE("ERROR: RemotePortOpenStatus event is for H=%u, but callback data is for H=%u (%p)", EventData->EventData.RemotePortOpenStatusEventData.PortHandle, PortData->Handle, PortData);

               if(PortData->ClientStatus != btsoClientConnecting)
                  SS1_LOGW("WARNING: Received \"outgoing connect complete\" event when not in connecting state (%d)", PortData->ClientStatus);

               if(EventData->EventData.RemotePortOpenStatusEventData.Status == SPPM_OPEN_REMOTE_PORT_STATUS_SUCCESS)
               {
                  SS1_LOGD("H=%u connected", EventData->EventData.RemotePortOpenStatusEventData.PortHandle);
                  PortData->ClientStatus = btsoClientConnected;
               }
               else
               {
                  SS1_LOGD("H=%u failed to connect (%d)", EventData->EventData.RemotePortOpenStatusEventData.PortHandle, EventData->EventData.RemotePortOpenStatusEventData.Status);
                  PortData->ClientStatus = btsoClientDisconnected;
               }

               SS1_LOGD("Broadcast ClientConnectCond (%p) for PortData @ %p, H=%u IH=%u - %s", &(PortData->ClientConnectCond), PortData, PortData->Handle, PortData->InternalHandle, (PortData->ClientStatus == btsoClientConnected ? "Client Connected" : "Client Disconnected"));
               pthread_cond_broadcast(&(PortData->ClientConnectCond));
               break;

            case setLineStatusChanged:
               SS1_LOGD("Signal: (SPPM) LineStatusChanged (%d, 0x%08lX)", EventData->EventData.LineStatusChangedEventData.PortHandle, EventData->EventData.LineStatusChangedEventData.LineStatusMask);
               if(PortData->Handle != EventData->EventData.LineStatusChangedEventData.PortHandle)
                  SS1_LOGE("ERROR: LineStatusChanged event is for H=%u, but callback data is for H=%u (%p)", EventData->EventData.LineStatusChangedEventData.PortHandle, PortData->Handle, PortData);
               break;

            case setPortStatusChanged:
               SS1_LOGD("Signal: (SPPM) PortStatusChanged (%d)", EventData->EventData.PortStatusChangedEventData.PortHandle);
               if(PortData->Handle != EventData->EventData.PortStatusChangedEventData.PortHandle)
                  SS1_LOGE("ERROR: PortStatusChanged event is for H=%u, but callback data is for H=%u (%p)", EventData->EventData.PortStatusChangedEventData.PortHandle, PortData->Handle, PortData);
               break;

            case setDataReceived:
               SS1_LOGD("Signal: (SPPM) DataReceived (%d, %d)", EventData->EventData.DataReceivedEventData.PortHandle, EventData->EventData.DataReceivedEventData.DataLength);
               if(PortData->Handle != EventData->EventData.DataReceivedEventData.PortHandle)
                  SS1_LOGE("ERROR: DataReceived event is for H=%u, but callback data is for H=%u (%p)", EventData->EventData.DataReceivedEventData.PortHandle, PortData->Handle, PortData);
               break;

            case setTransmitBufferEmpty:
               SS1_LOGD("Signal: (SPPM) TransmitBufferEmpty (%d)", EventData->EventData.TransmitBufferEmptyEventData.PortHandle);
               if(PortData->Handle != EventData->EventData.TransmitBufferEmptyEventData.PortHandle)
                  SS1_LOGE("ERROR: TransmitBufferEmpty event is for H=%u, but callback data is for H=%u (%p)", EventData->EventData.TransmitBufferEmptyEventData.PortHandle, PortData->Handle, PortData);
               break;

            default:
               SS1_LOGE("Received unknown event code %d", EventData->EventType);
         }

         BTSO_ReleasePortData(PortData);
         PortData = NULL;
         SS1_LOGD("BTSO Port: Released (IH=%u)", (unsigned int)CallbackParameter);
      }
      else
      {
         SS1_LOGE("Port Data could not be located.");

         if(EventData)
         {
            switch(EventData->EventType)
            {
               case setServerPortOpenRequest:
                  SS1_LOGD("Signal (Unexpected): (SPPM) ServerPortOpenRequest (%d)", EventData->EventData.ServerPortOpenRequestEventData.PortHandle);
                  break;

               case setServerPortOpen:
                  SS1_LOGD("Signal (Unexpected): (SPPM) ServerPortOpen (%d, %02X:%02X:%02X:%02X:%02X:%02X)", EventData->EventData.ServerPortOpenEventData.PortHandle, EventData->EventData.ServerPortOpenEventData.RemoteDeviceAddress.BD_ADDR5, EventData->EventData.ServerPortOpenEventData.RemoteDeviceAddress.BD_ADDR4, EventData->EventData.ServerPortOpenEventData.RemoteDeviceAddress.BD_ADDR3, EventData->EventData.ServerPortOpenEventData.RemoteDeviceAddress.BD_ADDR2, EventData->EventData.ServerPortOpenEventData.RemoteDeviceAddress.BD_ADDR1, EventData->EventData.ServerPortOpenEventData.RemoteDeviceAddress.BD_ADDR0);
                  break;

               case setPortClose:
                  SS1_LOGD("Signal (Unexpected): (SPPM) PortClose (%d)", EventData->EventData.PortCloseEventData.PortHandle);
                  break;

               case setRemotePortOpenStatus:
                  SS1_LOGD("Signal (Unexpected): (SPPM) RemotePortOpenStatus (%d, %d)", EventData->EventData.RemotePortOpenStatusEventData.PortHandle, EventData->EventData.RemotePortOpenStatusEventData.Status);
                  break;

               case setLineStatusChanged:
                  SS1_LOGD("Signal (Unexpected): (SPPM) LineStatusChanged (%d, 0x%08lX)", EventData->EventData.LineStatusChangedEventData.PortHandle, EventData->EventData.LineStatusChangedEventData.LineStatusMask);
                  break;

               case setPortStatusChanged:
                  SS1_LOGD("Signal (Unexpected): (SPPM) PortStatusChanged (%d)", EventData->EventData.PortStatusChangedEventData.PortHandle);
                  break;

               case setDataReceived:
                  SS1_LOGD("Signal (Unexpected): (SPPM) DataReceived (%d, %d)", EventData->EventData.DataReceivedEventData.PortHandle, EventData->EventData.DataReceivedEventData.DataLength);
                  break;

               case setTransmitBufferEmpty:
                  SS1_LOGD("Signal (Unexpected): (SPPM) TransmitBufferEmpty (%d)", EventData->EventData.TransmitBufferEmptyEventData.PortHandle);
                  break;

               default:
                  SS1_LOGE("Received unknown event code %d", EventData->EventType);
            }
         }
      }
   }
   else
      SS1_LOGE("Callback did not provided a valid event.");

   if(PortData)
      CHECK_MUTEX(&(PortData->Mutex));

   SS1_LOGD("Exit");
}

void BTSO_SCOM_EventCallback(SCOM_Event_Data_t *EventData, void *CallbackParameter)
{
   SS1_LOGD("Enter (%p, %p)", EventData, CallbackParameter);

   BD_ADDR_t        RemoteAddress;
   unsigned int     Result;
   BTSO_PortData_t *PortData;

   SS1_LOGD("Called with param @ %p", CallbackParameter);
   SS1_LOGD("Called from TID 0x%x", gettid());

   if(EventData)
   {
      SS1_LOGD("BTSO Port: Searching (IH=%u)", (unsigned int)CallbackParameter);
      if((PortData = BTSO_AcquirePortData((unsigned int)CallbackParameter)) != NULL)
      {
         SS1_LOGD("BTSO Port: Acquired (IH=%u) (%p)", (unsigned int)CallbackParameter, PortData);

         switch(EventData->EventType)
         {
            //XXX FIXME
            case setServerConnectionOpen:
               SS1_LOGD("Signal: (SCOM) ServerConnectionOpen");
               if(PortData->Handle != EventData->EventData.ServerConnectionOpenEventData.ConnectionID)
                  SS1_LOGE("ERROR: ServerConnectionOpen event is for H=%u, but callback data is for H=%u (%p)", EventData->EventData.ServerConnectionOpenEventData.ConnectionID, PortData->Handle, PortData);

               if(PortData->ClientStatus == btsoClientDisconnected)
               {
                  PortData->ClientStatus  = btsoClientAccepting;
                  PortData->RemoteAddress = EventData->EventData.ServerConnectionOpenEventData.RemoteDeviceAddress;

                  /* It only makes sense for a single thread to         */
                  /* accept an incoming connection, so announce the     */
                  /* connection with pthread_cond_signal() rather than  */
                  /* pthread_cond_broadcast().                          */
                  SS1_LOGD("Signal ClientAcceptCond (%p) for PortData @ %p, H=%u IH=%u - Client Accepting", &(PortData->ClientAcceptCond), PortData, PortData->Handle, PortData->InternalHandle);
                  pthread_cond_signal(&(PortData->ClientAcceptCond));
               }
               else
               {
                  SS1_LOGE("Server Connection Open received while not in Disconnected state (Old state = %d)", PortData->ClientStatus);
                  SCOM_CloseConnection(EventData->EventData.ServerConnectionOpenEventData.ConnectionID);
               }
               break;

            case setConnectionClose:
               SS1_LOGD("Signal: (SCOM) ConnectionClose");
               if(PortData->Handle != EventData->EventData.ConnectionCloseEventData.ConnectionID)
                  SS1_LOGE("ERROR: ConnectionClose event is for H=%u, but callback data is for H=%u (%p)", EventData->EventData.ConnectionCloseEventData.ConnectionID, PortData->Handle, PortData);

               if(PortData->ClientStatus != btsoClientDisconnected)
               {
                  /* Set the new state and wake up any threads waiting  */
                  /* on events from this Connection.                    */
                  SS1_LOGE("Client Connection (H=%u IH=%u) switched from state %u to closed", PortData->Handle, PortData->InternalHandle, PortData->ClientStatus);
                  PortData->ClientStatus = btsoClientDisconnected;
                  SS1_LOGD("Broadcast ClientAcceptCond (%p) for PortData @ %p, H=%u IH=%u - Client Disconnecting", &(PortData->ClientAcceptCond), PortData, PortData->Handle, PortData->InternalHandle);
                  pthread_cond_broadcast(&(PortData->ClientAcceptCond));
                  SS1_LOGD("Broadcast ClientConnectCond (%p) for PortData @ %p, H=%u IH=%u - Client Disconnecting", &(PortData->ClientConnectCond), PortData, PortData->Handle, PortData->InternalHandle);
                  pthread_cond_broadcast(&(PortData->ClientConnectCond));
               }
               else
                  SS1_LOGE("Client Connection Closed message received while already in Disconnected state (%d)", PortData->ClientStatus);
               break;

            case setRemoteConnectionOpenStatus:
               SS1_LOGD("Signal: (SCOM) RemoteConnectionOpenStatus");
               if(PortData->Handle != EventData->EventData.RemoteConnectionOpenStatusEventData.ConnectionID)
                  SS1_LOGE("ERROR: RemoteConnectionOpenStatus event is for H=%u, but callback data is for H=%u (%p)", EventData->EventData.RemoteConnectionOpenStatusEventData.ConnectionID, PortData->Handle, PortData);

               if(PortData->ClientStatus != btsoClientConnecting)
                  SS1_LOGW("WARNING: Received \"outgoing connect complete\" event when not in connecting state (%d)", PortData->ClientStatus);

               if(EventData->EventData.RemoteConnectionOpenStatusEventData.Status == 0)
               {
                  SS1_LOGD("Connection H=%u connected", EventData->EventData.RemoteConnectionOpenStatusEventData.ConnectionID);
                  PortData->ClientStatus = btsoClientConnected;
               }
               else
               {
                  SS1_LOGD("Connection H=%u failed to connect (%d)", EventData->EventData.RemoteConnectionOpenStatusEventData.ConnectionID, EventData->EventData.RemoteConnectionOpenStatusEventData.Status);
                  PortData->ClientStatus = btsoClientDisconnected;
               }

               SS1_LOGD("Broadcast ClientConnectCond (%p) for PortData @ %p, H=%u IH=%u - Client %s", &(PortData->ClientConnectCond), PortData, PortData->Handle, PortData->InternalHandle, (PortData->ClientStatus == btsoClientConnected ? "Connected" : "Disconnected"));
               pthread_cond_broadcast(&(PortData->ClientConnectCond));
               break;

            default:
               SS1_LOGE("Received unknown event code %d", EventData->EventType);
         }

         BTSO_ReleasePortData(PortData);
         PortData = NULL;
         SS1_LOGD("BTSO Port: Released (IH=%u)", (unsigned int)CallbackParameter);
      }
      else
         SS1_LOGE("Could not lock socket mutex");
   }
   else
      SS1_LOGE("Callback did not provide a valid event.");

   if(PortData)
      CHECK_MUTEX(&(PortData->Mutex));

   SS1_LOGD("Exit");
}

void BTSO_InitRefCount(BTSO_RefCount_t *RefCount)
{
   if(RefCount)
   {
      RefCount->Count = 0;
      pthread_mutex_init(&(RefCount->Mutex), NULL);
   }
}

bool BTSO_DestroyRefCount(BTSO_RefCount_t *RefCount)
{
   bool ret_val;

   if(RefCount)
   {
      ret_val = (pthread_mutex_destroy(&(RefCount->Mutex)) == 0);
      if(!ret_val)
      {
         SS1_LOGD("BTSO Port Mutex: Locking");
         if(pthread_mutex_lock(&(RefCount->Mutex)) == 0)
         {
            SS1_LOGD("BTSO Port Mutex: Locked");
            pthread_mutex_unlock(&(RefCount->Mutex));
            SS1_LOGD("BTSO Port Mutex: Unlocked");
         }
         ret_val = (pthread_mutex_destroy(&(RefCount->Mutex)) == 0);
      }
   }
   else
      ret_val = false;

   return(ret_val);
}

int BTSO_IncRefCount(BTSO_RefCount_t *RefCount)
{
   int ret_val;

   SS1_LOGD("BTSO Port Mutex: Locking");
   if(RefCount && (pthread_mutex_lock(&(RefCount->Mutex)) == 0))
   {
      SS1_LOGD("BTSO Port Mutex: Locked");
      RefCount->Count += 1;
      ret_val = (int)(RefCount->Count);

      pthread_mutex_unlock(&(RefCount->Mutex));
      SS1_LOGD("BTSO Port Mutex: Unlocked");
   }
   else
      ret_val = -1;

   if(RefCount)
      CHECK_MUTEX(&(RefCount->Mutex));

   return(ret_val);
}

int BTSO_DecRefCount(BTSO_RefCount_t *RefCount)
{
   int ret_val;

   SS1_LOGD("BTSO Port Mutex: Locking");
   if(RefCount && (pthread_mutex_lock(&(RefCount->Mutex)) == 0))
   {
      SS1_LOGD("BTSO Port Mutex: Locked");
      if(RefCount->Count > 0)
      {
         RefCount->Count -= 1;
         ret_val = (int)(RefCount->Count);
      }
      else
         ret_val = -1;

      pthread_mutex_unlock(&(RefCount->Mutex));
      SS1_LOGD("BTSO Port Mutex: Unlocked");
   }
   else
      ret_val = -1;

   if(RefCount)
      CHECK_MUTEX(&(RefCount->Mutex));

   return(ret_val);
}

#endif /* HAVE_BLUETOOTH */

} /* namespace android */

