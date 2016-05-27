/*****< ARBTJBHB.cpp >*********************************************************/
/*      Copyright 2010 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  ARBTJBHB - android.bluetooth.HeadsetBase module for Stonestreet One       */
/*             Android Runtime Bluetooth JNI Bridge                           */
/*                                                                            */
/*  Author:  Greg Hensley                                                     */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   06/30/10  G. Hensley     Initial creation.                               */
/******************************************************************************/

#define LOG_TAG "ARBTJBHB"

#include "android_runtime/AndroidRuntime.h"
#include "JNIHelp.h"
#include "jni.h"
#include "utils/Log.h"
#include "utils/misc.h"

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <errno.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/socket.h>
#include <sys/uio.h>
#include <sys/poll.h>


#ifdef HAVE_BLUETOOTH
extern "C"
{
#include "SS1BTPM.h"
}

#include "ARBTJBBTSO.h"

#endif /* HAVE_BLUETOOTH */

#include "SS1UTIL.h"

namespace android
{

#ifdef HAVE_BLUETOOTH
static jfieldID Field_mNativeData;
static jfieldID Field_mAddress;
static jfieldID Field_mRfcommChannel;
static jfieldID Field_mTimeoutRemainingMs;

static const char CRLF[]   = "\xd\xa";
static const int  CRLF_LEN = 2;

typedef struct _tagHB_NativeData_t
{
   int              LastReadError;
   unsigned char    ReadBuffer[512];
   unsigned int     ReadBufferLength;
   unsigned int     PortDataInternalHandle;
   pthread_mutex_t  ReadBufferMutex;
} HB_NativeData_t;


static HB_NativeData_t *GetNativeData(JNIEnv *Env, jobject Object)
{
   HB_NativeData_t *NatData;

   if((NatData = (HB_NativeData_t *)(Env->GetIntField(Object, Field_mNativeData))) == NULL)
      jniThrowException(Env, "java/io/IOException", "HeadsetBase not initialized (stack failure?)");

   return NatData;
}

#endif

static void HB_ClassInitNative(JNIEnv *Env, jclass Clazz)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   Field_mNativeData         = Env->GetFieldID(Clazz, "mNativeData",         "I");
   Field_mAddress            = Env->GetFieldID(Clazz, "mAddress",            "Ljava/lang/String;");
   Field_mTimeoutRemainingMs = Env->GetFieldID(Clazz, "mTimeoutRemainingMs", "I");
   Field_mRfcommChannel      = Env->GetFieldID(Clazz, "mRfcommChannel",      "I");
#endif
   SS1_LOGD("Exit");
}

static void HB_InitializeNativeDataNative(JNIEnv *Env, jobject Object, jint Fd)
{
   SS1_LOGD("Enter (%d)", Fd);
#ifdef HAVE_BLUETOOTH
   int              Result;
   unsigned int     Count;
   BTSO_PortData_t *Port;
   HB_NativeData_t *NatData;

   NatData = (HB_NativeData_t *)calloc(1, sizeof(HB_NativeData_t));

   Env->SetIntField(Object, Field_mNativeData, (jint)NatData);

   if(NatData)
   {
      if(Fd == -1)
      {
         /* We are creating a completely new "socket" for this          */
         /* HeadsetBase. Unlike BluetoothSocket, HeadsetBase can only   */
         /* represent the client connection side of an SPP port, so we  */
         /* don't keep a Type.                                          */
         NatData->LastReadError    = 0;
         NatData->ReadBufferLength = 0;
         pthread_mutex_init(&(NatData->ReadBufferMutex), NULL);

         Result = BTSO_CreatePortData();

         if(Result >= 0)
         {
            NatData->PortDataInternalHandle = (unsigned int)Result;
            
            Port = BTSO_AcquirePortData(NatData->PortDataInternalHandle);
            SS1_LOGI("Created NatData @ %p with new Port @ %p", NatData, Port);

            /* Assign any non-default Port fields. Since                */
            /* this was created by HeadsetBase, rather than             */
            /* BluetoothAudioGateway, we know a server will never be    */
            /* invoked on the port.                                     */
            Port->ServerStatus = btsoServerNotAvailable;
         }
         else
         {
            free(NatData);
            Port = NULL;

            jniThrowException(Env, "java/lang/OutOfMemoryError", "Out of memory while configuring HeadsetBase");
         }
      }
      else
      {
         /* We are creating a HeadsetBase tied to an existing, open     */
         /* connection. This (hopefully) means the "Fd" came from       */
         /* accepting an incoming connection via a BluetoothAudioGateway*/
         /* instance. In this case, the "Fd" is really the address of a */
         /* Port Data structure. This structure will be shared between  */
         /* the two objects until both are destroyed. This is necessary */
         /* because the server registration must remain open while the  */
         /* client is connected but the Android API is based on BSD     */
         /* Sockets and so recommends closing the server-side of the    */
         /* port as soon as the client connection is established. The   */
         /* server object will fake this close() if there is a client   */
         /* connected.                                                  */
         if((Port = BTSO_AcquirePortData((unsigned int)Fd)) != NULL)
         {
            NatData->PortDataInternalHandle = (unsigned int)Fd;

            SS1_LOGI("Created NatData @ %p with old Port @ %p", NatData, Port);
         }
         else
         {
            SS1_LOGD("Error: Unable to locate IH %u *** THIS SHOULD NEVER HAPPEN ***", (unsigned int)Fd);
            NatData->PortDataInternalHandle = 0;
         }
      }

      if(Port)
      {
         Count = BTSO_IncRefCount(&(Port->RefCount));
         SS1_LOGI("Incremented reference count on port data @ %p (IH=%u): new count = %u", Port, NatData->PortDataInternalHandle, Count);
         
         BTSO_ReleasePortData(Port);
      }
   }
   else
      jniThrowException(Env, "java/lang/OutOfMemoryError", "Out of memory while constructing HeadsetBase");
#endif
   SS1_LOGD("Exit");
}

static void HB_CleanupNativeDataNative(JNIEnv *Env, jobject Object)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   unsigned int     Count;
   unsigned int     InternalHandle;
   BTSO_PortData_t *PortData;
   HB_NativeData_t *NatData;

   /* Connect to the Bluetooth daemon, but only try once. Since we're   */
   /* only closing ports, if the service is down, then the connections  */
   /* are already closed. If we only lost IPC connectivity, the daemon  */
   /* should have closed our connections for us when it noticed we had  */
   /* dropped off.                                                      */
   InitBTPMClientNoRetries(NULL, NULL, NULL);

   if((NatData = GetNativeData(Env, Object)) != NULL)
   {
      Env->SetIntField(Object, Field_mNativeData, (jint)NULL);

      InternalHandle = NatData->PortDataInternalHandle;
      free(NatData);

      SS1_LOGI("Destroyed native data. Proceeding with port data (IH=%u).", InternalHandle);

      if((PortData = BTSO_AcquirePortData(InternalHandle)) != NULL)
      {

         Count = BTSO_DecRefCount(&(PortData->RefCount));
         SS1_LOGI("Port data reference count == %u", Count);
         
         if(Count == 0)
         {
            /* The reference count on the Port data reached zero, so    */
            /* we held the last claimed reference. We should clean up   */
            /* any contained resources, now. There's no need to lock    */
            /* the structure's mutex since no one else should have a    */
            /* reference to the object at this point.                   */
            if(PortData->ClientStatus != btsoClientDisconnected)
            {
               SS1_LOGI("Closing client connection");
               PortData->ClientStatus = btsoClientDisconnected;

               /* We hold the last reference, so there shouldn't be     */
               /* anyone waiting on events. Signal them just in case.   */
               pthread_cond_broadcast(&(PortData->ClientConnectCond));
               SPPM_ClosePort(PortData->Handle, BTSO_CLOSE_PORT_TIMEOUT);
            }

            if(PortData->ServerStatus == btsoServerListening)
            {
               SS1_LOGI("Closing server registration");
               PortData->ServerStatus = btsoServerClosed;

               /* We hold the last reference, so there shouldn't be     */
               /* anyone waiting on events. Signal them just in case.   */
               pthread_cond_broadcast(&(PortData->ClientAcceptCond));
               SPPM_UnRegisterServerPort(PortData->Handle);
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
               SS1_LOGI("Destroyed port data @ %p", PortData);
            }
            else
               SS1_LOGD("Port data (IH=%u) already destroyed.", InternalHandle);
         }
         else
         {
            if((signed int)Count == -1)
               SS1_LOGE("Reference count for port data @ %p was already 0. Most likely, another thread is in the process of destroying it.", PortData);
            else
               SS1_LOGI("Reference count == %u for port data @ %p, not destroying", Count, PortData);

            BTSO_ReleasePortData(PortData);
         }
      }
      else
         SS1_LOGD("Port data not found (IH=%u)", InternalHandle);
   }
   else
      SS1_LOGE("Called on object with uninitialized data!");

#endif
   SS1_LOGD("Exit");
}

static jboolean HB_ConnectNative(JNIEnv *Env, jobject Object)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTHa
   int              Result;
   bool             CanConnect;
   jstring          Address;
   BD_ADDR_t        BD_ADDR;
   const char      *AddrString;
   unsigned int     Channel;
   unsigned int     Status;
   unsigned long    Flags;
   BTSO_PortData_t *Port;
   HB_NativeData_t *NatData;

   Result     = 0;
   CanConnect = false;

   /* Make sure this process has a connection to the Bluetopia daemon.  */
   if(InitBTPMClientNoRetries(NULL, NULL, NULL) == 0)
   {
      if((NatData = GetNativeData(Env, Object)) != NULL)
      {
         SS1_LOGD("HF Port: Searching (IH=%u)", NatData->PortDataInternalHandle);
         if((Port = BTSO_AcquirePortData(NatData->PortDataInternalHandle)) != NULL)
         {
            SS1_LOGD("HF Port: Acquired (IH=%u) (%p)", NatData->PortDataInternalHandle, Port);

            /* Make sure we don't already have a connection.            */
            if(Port->Handle == 0)
            {
               if(Port->ClientStatus == btsoClientDisconnected)
               {
                  /* The port currently has no remote connection, so we */
                  /* are clear to initiate a connection.                */
                  Port->ClientStatus = btsoClientConnecting;

                  CanConnect = true;
               }
               else
               {
                  /* If the port is not disconnected, then it's either  */
                  /* already connected or in the process of connecting  */
                  /* so we shouldn't try making another connection yet. */
                  if((Port->ClientStatus == btsoClientConnected) || (Port->ClientStatus == btsoClientConnecting))
                     Result = 0;
                  else
                     Result = EBADF;
               }
            }
            else
               Result = EINVAL;

            BTSO_ReleasePortData(Port);
            Port = NULL;
            SS1_LOGD("HF Port: Released (IH=%u)", NatData->PortDataInternalHandle);
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

            Flags   = SPPM_OPEN_REMOTE_PORT_FLAGS_REQUIRE_AUTHENTICATION | SPPM_OPEN_REMOTE_PORT_FLAGS_REQUIRE_ENCRYPTION;
            Channel = (unsigned int)Env->GetIntField(Object, Field_mRfcommChannel);

            if(RFCOMM_VALID_SERVER_CHANNEL_ID(Channel))
            {
               SS1_LOGD("HF Port: Searching (IH=%u)", NatData->PortDataInternalHandle);
               if((Port = BTSO_AcquirePortData(NatData->PortDataInternalHandle)) != NULL)
               {
                  SS1_LOGD("HF Port: Acquired (IH=%u) (%p)", NatData->PortDataInternalHandle, Port);

                  SS1_LOGI("Opening remote port to channel %u, with callback @ %p and param @ 0x%08X", Channel, BTSO_SPPM_EventCallback, NatData->PortDataInternalHandle);
                  if((Result = SPPM_OpenRemotePort(BD_ADDR, Channel, Flags, BTSO_SPPM_EventCallback, (void *)(NatData->PortDataInternalHandle), &Status)) > 0)
                  {
                     Port->Handle        = (unsigned int)Result;
                     Port->ClientStatus  = btsoClientConnected;
                     Port->RemoteAddress = BD_ADDR;

                     /* Success                                         */
                     Result = 0;
                  }
                  else
                  {
                     SS1_LOGI("RFCOMM Connection failed with code %d/%u", Result, Status);
                     Port->ClientStatus = btsoClientDisconnected;
                     Result = SPPMErrorToErrno(Status);
                  }

                  BTSO_ReleasePortData(Port);
                  Port = NULL;
                  SS1_LOGD("HF Port: Released (IH=%u)", NatData->PortDataInternalHandle);
               }
               else
               {
                  SS1_LOGE("Failed to re-acquire port mutex after connect");
                  Result = EINVAL;
               }
            }
            else
            {
               /* Bad RFCOMM Channel (port) number.                     */
               Result = ENETUNREACH;
            }
         }
         else
         {
            if(Result == 0)
               Result = EBADF;
         }
      }
      else
         Result = EINVAL;
   }
   else
   {
      SS1_LOGE("Unable to access Bluetooth platform");
      Result = EINVAL;
   }

   if(NatData && Port)
      CHECK_MUTEX(&(Port->Mutex));

   SS1_LOGD("Exit (%d)", ((Result == 0) ? JNI_TRUE : JNI_FALSE));

   return((Result == 0) ? JNI_TRUE : JNI_FALSE);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(JNI_FALSE);
#endif
}

static jint HB_ConnectAsyncNative(JNIEnv *Env, jobject Object)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   int              Result;
   bool             CanConnect;
   jstring          Address;
   BD_ADDR_t        BD_ADDR;
   const char      *AddrString;
   unsigned int     Channel;
   unsigned long    Flags;
   BTSO_PortData_t *Port;
   HB_NativeData_t *NatData;

   Result     = 0;
   NatData    = NULL;
   CanConnect = false;

   /* Make sure this process has a connection to the Bluetopia daemon.  */
   if(InitBTPMClientNoRetries(NULL, NULL, NULL) == 0)
   {
      if((NatData = GetNativeData(Env, Object)) != NULL)
      {
         SS1_LOGD("HF Port: Searching (IH=%u)", NatData->PortDataInternalHandle);
         if((Port = BTSO_AcquirePortData(NatData->PortDataInternalHandle)) != NULL)
         {
            SS1_LOGD("HF Port: Acquired (IH=%u) (%p)", NatData->PortDataInternalHandle, Port);

            /* Make sure we don't already have a connection.            */
            if(Port->ClientStatus == btsoClientDisconnected)
            {
               /* The port currently has no remote connection, so we are*/
               /* clear to initiate a connection.                       */

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

               Flags   = SPPM_OPEN_REMOTE_PORT_FLAGS_REQUIRE_AUTHENTICATION | SPPM_OPEN_REMOTE_PORT_FLAGS_REQUIRE_ENCRYPTION;
               Channel = (unsigned int)Env->GetIntField(Object, Field_mRfcommChannel);

               if(RFCOMM_VALID_SERVER_CHANNEL_ID(Channel))
               {
                  {
                     /* XXX DEBUG */
                     char address[32];
                     BD_ADDRToStr(address, sizeof(address), BD_ADDR);
                     SS1_LOGI("Opening remote port to %s, channel %u, with callback @ %p and param @ 0x%08X", address, Channel, BTSO_SPPM_EventCallback, NatData->PortDataInternalHandle);
                  }
                  if((Result = SPPM_OpenRemotePort(BD_ADDR, Channel, Flags, BTSO_SPPM_EventCallback, (void *)(NatData->PortDataInternalHandle), NULL)) > 0)
                  {
                     Port->Handle        = (unsigned int)Result;
                     Port->ClientStatus  = btsoClientConnecting;
                     Port->RemoteAddress = BD_ADDR;

                     SS1_LOGI("Connection process started. Handle = %d", Result);

                     /* Success                                         */
                     Result = 0;
                  }
                  else
                  {
                     if((Result == BTPM_ERROR_CODE_DEVICE_CONNECTION_IN_PROGRESS) && (Port->ClientStatus == btsoClientConnecting))
                     {
                        SS1_LOGI("Connection establishment already in progress");
                        Result = 0;
                     }
                     else
                     {
                        SS1_LOGI("RFCOMM Connection failed with code %d", Result);
                        Result = SPPMErrorToErrno(Result);
                     }
                  }
               }
               else
               {
                  /* Bad RFCOMM Channel (port) number.                  */
                  SS1_LOGI("Invalid RFCOMM channel number (%u)", Channel);
                  Result = ENETUNREACH;
               }
            }
            else
            {
               /* If the port is not disconnected, then it's either     */
               /* already connected or in the process of connecting so  */
               /* we shouldn't try making another connection yet.       */
               if((Port->ClientStatus == btsoClientConnecting) || (Port->ClientStatus == btsoClientConnected))
                  Result = 0;
               else
                  Result = EBADF;
            }

            BTSO_ReleasePortData(Port);
            Port = NULL;
            SS1_LOGD("HF Port: Released (IH=%u)", NatData->PortDataInternalHandle);
         }
         else
            Result = EINVAL;
      }
      else
         Result = EINVAL;
   }
   else
   {
      SS1_LOGE("Unable to access Bluetooth platform");
      Result = EINVAL;
   }

   if(NatData && Port)
      CHECK_MUTEX(&(Port->Mutex));

   SS1_LOGD("Exit (%d)", Result);

   return(Result);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", -1);
   return(-1);
#endif
}

static jint HB_WaitForAsyncConnectNative(JNIEnv *Env, jobject Object, jint TimeoutMS)
{
   SS1_LOGD("Enter (%d)", TimeoutMS);
#ifdef HAVE_BLUETOOTH
   jint             ret_val;
   struct timeval   TimeValGoal;
   struct timeval   TimeValCurrent;
   struct timeval   TimeValDiff;
   struct timespec  TimeSpec;
   BTSO_PortData_t *Port;
   HB_NativeData_t *NatData;

   ret_val = -1;

   SS1_LOGI("Called with timeout = %d ms", TimeoutMS);

   if((NatData = GetNativeData(Env, Object)) != NULL)
   {
      SS1_LOGD("HF Port: Searching (IH=%u)", NatData->PortDataInternalHandle);
      if((Port = BTSO_AcquirePortData(NatData->PortDataInternalHandle)) != NULL)
      {
         SS1_LOGD("HF Port: Acquired (IH=%u) (%p)", NatData->PortDataInternalHandle, Port);

         Env->SetIntField(Object, Field_mTimeoutRemainingMs, TimeoutMS);

         if(Port->ClientStatus == btsoClientDisconnected)
         {
            SS1_LOGI("Disconnected, calling connectAsyncNative()");

            BTSO_ReleasePortData(Port);
            Port = NULL;
            SS1_LOGD("HF Port: Released (IH=%u)", NatData->PortDataInternalHandle);

            ret_val = HB_ConnectAsyncNative(Env, Object);

            if(ret_val == 0)
               SS1_LOGI("connectAsyncNative() claims already connected or connecting");

            SS1_LOGD("HF Port: Searching (IH=%u)", NatData->PortDataInternalHandle);
            if((Port = BTSO_AcquirePortData(NatData->PortDataInternalHandle)) != NULL)
            {
               SS1_LOGD("HF Port: Acquired (IH=%u) (%p)", NatData->PortDataInternalHandle, Port);

               SS1_LOGI("Connection state = %d", Port->ClientStatus);
            }
            else
               SS1_LOGE("Unable to reacquire the port data (IH=%u)", NatData->PortDataInternalHandle);
         }

         if(Port)
         {
            if(Port->ClientStatus == btsoClientConnecting)
            {
               SS1_LOGI("Connecting, will wait for condition");
               if(TimeoutMS >= 0)
               {
                  /* We were given a timeout (in milliseconds), so only */
                  /* wait that long.                                    */
                  gettimeofday(&TimeValGoal, NULL);
                  TimeValDiff.tv_sec = 0;
                  TimeValDiff.tv_usec = (TimeoutMS * 1000L);
                  timeradd(&TimeValGoal, &TimeValDiff, &TimeValGoal);
                  TimeSpec.tv_sec      = TimeValGoal.tv_sec;
                  TimeSpec.tv_nsec     = (TimeValGoal.tv_usec * 1000L);

                  ret_val = BTSO_WaitOnPortCondition(Port, &(Port->ClientConnectCond), &TimeSpec);

                  if(TimeoutMS > 0)
                  {
                     /* Record how much time is remaining from the      */
                     /* specified timeout, if any.                      */
                     gettimeofday(&TimeValCurrent, NULL);
                     if(!timercmp(&TimeValGoal, &TimeValCurrent, <))
                     {
                        /* We have not exceeded the timeout value, so   */
                        /* record the remaining timeout.                */
                        timersub(&TimeValGoal, &TimeValCurrent, &TimeValDiff);
                        TimeoutMS = (TimeValDiff.tv_sec * 1000L) + (TimeValDiff.tv_usec / 1000L);
                        SS1_LOGI("Setting remaining timeout to %d ms", TimeoutMS);
                        Env->SetIntField(Object, Field_mTimeoutRemainingMs, TimeoutMS);
                     }
                     else
                        Env->SetIntField(Object, Field_mTimeoutRemainingMs, 0);
                  }
               }
               else
               {
                  /* No valid timeout was specified, so wait forever.   */
                  ret_val = BTSO_WaitOnPortCondition(Port, &(Port->ClientConnectCond), NULL);
               }

               if(ret_val == 0)
               {
                  if(Port->ClientStatus == btsoClientConnected)
                  {
                     /* We received a signal before the timeout, and the*/
                     /* connection was made successfully.               */
                     SS1_LOGI("Client connected");
                     ret_val = 1;
                  }
                  else
                  {
                     /* We were signaled out of the wait, but we're     */
                     /* still not connected. Report this as an error    */
                     /* condition.  As it probably means the connection */
                     /* attempt failed or we're closing the port.       */
                     SS1_LOGI("Signaled but no connection (%d)", Port->ClientStatus);
                     ret_val = -1;
                  }
               }
               else
               {
                  /* We timed out or were interrupted. Report either    */
                  /* case as a timeout.                                 */
                  SS1_LOGI("Timed out or interrupted");
                  ret_val = 0;
               }
            }
            else
            {
               if(Port->ClientStatus == btsoClientConnected)
               {
                  SS1_LOGI("Already connected");
                  ret_val = 1;
               }
            }

            BTSO_ReleasePortData(Port);
            Port = NULL;
            SS1_LOGD("HF Port: Released (IH=%u)", NatData->PortDataInternalHandle);
         }
      }
   }

   if(NatData && Port)
      CHECK_MUTEX(&(Port->Mutex));

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", -1);
   return(-1);
#endif
}

static void HB_DisconnectNative(JNIEnv *Env, jobject Object)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   BTSO_PortData_t *Port;
   HB_NativeData_t *NatData;

   /* Connect to the Bluetooth daemon, but only try once. Since we're   */
   /* only closing ports, if the service is down, then the connections  */
   /* are already closed. If we only lost IPC connectivity, the daemon  */
   /* should have closed our connections for us when it noticed we had  */
   /* dropped off.                                                      */
   InitBTPMClientNoRetries(NULL, NULL, NULL);

   if((NatData = GetNativeData(Env, Object)) != NULL)
   {
      SS1_LOGD("HF Port: Searching (IH=%u)", NatData->PortDataInternalHandle);
      if((Port = BTSO_AcquirePortData(NatData->PortDataInternalHandle)) != NULL)
      {
         SS1_LOGD("HF Port: Acquired (IH=%u) (%p)", NatData->PortDataInternalHandle, Port);

         /* Set the port status to disconnected so any threads we wake  */
         /* will see this.                                              */
         Port->ClientStatus = btsoClientDisconnected;

         /* Wake up any threads waiting on events on the port.          */
         pthread_cond_broadcast(&(Port->ClientConnectCond));

         /* Close the connection.                                       */
         SPPM_ClosePort(Port->Handle, BTSO_CLOSE_PORT_TIMEOUT);

         BTSO_ReleasePortData(Port);
         Port = NULL;
         SS1_LOGD("HF Port: Released (IH=%u)", NatData->PortDataInternalHandle);
      }
   }
#endif
   SS1_LOGD("Exit");
}

static jboolean HB_SendURCNative(JNIEnv *Env, jobject Object, jstring Urc)
{
   SS1_LOGD("Enter (%p)", Urc);
#ifdef HAVE_BLUETOOTH
   int                 WriteResult;
   char               *StringBuffer;
   jboolean            ret_val;
   const char         *UrcString;
   unsigned int        PortHandle;
   unsigned int        StringBufferLength;
   BTSO_PortData_t    *Port;
   HB_NativeData_t    *NatData;
   BTSO_ClientStatus_t ClientStatus;

   ret_val = JNI_FALSE;
   NatData = NULL;

   /* Make sure this process has a connection to the Bluetopia daemon.  */
   if(InitBTPMClientNoRetries(NULL, NULL, NULL) == 0)
   {
      if((NatData = GetNativeData(Env, Object)) != NULL)
      {
         SS1_LOGD("HF Port: Searching (IH=%u)", NatData->PortDataInternalHandle);
         if((Port = BTSO_AcquirePortData(NatData->PortDataInternalHandle)) != NULL)
         {
            SS1_LOGD("HF Port: Acquired (IH=%u) (%p)", NatData->PortDataInternalHandle, Port);

            PortHandle   = Port->Handle;
            ClientStatus = Port->ClientStatus;

            BTSO_ReleasePortData(Port);
            Port = NULL;
            SS1_LOGD("HF Port: Released (IH=%u)", NatData->PortDataInternalHandle);

            if(ClientStatus == btsoClientConnected)
            {
               if((UrcString = Env->GetStringUTFChars(Urc, NULL)) != NULL)
               {
                  StringBufferLength = strlen(UrcString) + (CRLF_LEN * 2) + 1;

                  if((StringBuffer = (char *)calloc(1, StringBufferLength)) != NULL)
                  {
                     snprintf(StringBuffer, StringBufferLength, "%s%s%s", CRLF, UrcString, CRLF);
                     if((WriteResult = SPPM_WriteData(PortHandle, SPPM_WRITE_DATA_WRITE_TIMEOUT_INFINITE, (StringBufferLength - 1), (unsigned char *)StringBuffer)) >= 0)
                     {
                        SS1_LOGI("Sent %d bytes", WriteResult);
                        if(((unsigned int)WriteResult) == (StringBufferLength - 1))
                           ret_val = JNI_TRUE;
                     }
                     else
                        SS1_LOGI("Error writing data (%d)", WriteResult);

                     free(StringBuffer);
                  }
                  else
                     SS1_LOGE("Not enough memory for edit buffer");

                  Env->ReleaseStringUTFChars(Urc, UrcString);
               }
               else
                  SS1_LOGE("not enough memory for string buffer");
            }
            else
               SS1_LOGE("Called on unconnected client");
         }
         else
            SS1_LOGE("Unable to lock mutex");
      }
      else
         SS1_LOGE("Object not initialized");
   }
   else
      SS1_LOGE("Unable to access Bluetooth platform");

   if(NatData && Port)
      CHECK_MUTEX(&(Port->Mutex));

   SS1_LOGI("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH */
   SS1_LOGI("Exit (%d)", JNI_FALSE);
   return(JNI_FALSE);
#endif
}

static jstring HB_ReadNative(JNIEnv *Env, jobject Object, jint TimeoutMS)
{
   SS1_LOGD("Enter (%d)", TimeoutMS);
#ifdef HAVE_BLUETOOTH
   int                 ReadResult;
   jstring             ATCommandString;
   unsigned int        Position;
   unsigned int        PortHandle;
   struct timeval      Timeout;
   HB_NativeData_t    *NatData;
   BTSO_PortData_t    *Port;
   BTSO_ClientStatus_t ClientStatus;

   NatData         = NULL;
   ReadResult      = 0;
   ATCommandString = NULL;

   /* Make sure this process has a connection to the Bluetopia daemon.  */
   if(InitBTPMClientNoRetries(NULL, NULL, NULL) == 0)
   {
      if(TimeoutMS == 0)
         TimeoutMS = SPPM_READ_DATA_READ_TIMEOUT_IMMEDIATE;
      else
      {
         if(TimeoutMS < 0)
            TimeoutMS = SPPM_READ_DATA_READ_TIMEOUT_INFINITE;
      }

      if((NatData = GetNativeData(Env, Object)) != NULL)
      {
         SS1_LOGD("HF Port: Searching (IH=%u)", NatData->PortDataInternalHandle);
         if((Port = BTSO_AcquirePortData(NatData->PortDataInternalHandle)) != NULL)
         {
            SS1_LOGD("HF Port: Acquired (IH=%u) (%p)", NatData->PortDataInternalHandle, Port);

            PortHandle   = Port->Handle;
            ClientStatus = Port->ClientStatus;

            BTSO_ReleasePortData(Port);
            Port = NULL;
            SS1_LOGD("HF Port: Released (IH=%u)", NatData->PortDataInternalHandle);

            if(ClientStatus == btsoClientConnected)
            {
               if(pthread_mutex_lock(&(NatData->ReadBufferMutex)) == 0)
               {
                  Position = 0;

                  /* If the read buffer is empty, populate it with      */
                  /* something to start with.                           */
                  if(NatData->ReadBufferLength == 0)
                  {
                     if((ReadResult = SPPM_ReadData(PortHandle, TimeoutMS, (sizeof(NatData->ReadBuffer) - NatData->ReadBufferLength), NatData->ReadBuffer)) > 0)
                        NatData->ReadBufferLength = ReadResult;
                  }
                  else
                  {
                     /* We already had some data in the buffer from     */
                     /* a previous call to this function, so set        */
                     /* ReadResult to simulate having just read the     */
                     /* buffer contents.                                */
                     ReadResult = NatData->ReadBufferLength;
                  }

                  while((Position < NatData->ReadBufferLength) && (NatData->ReadBuffer[Position] != 0x0d) && (ReadResult > 0))
                  {
                     Position++;

                     /* Handle the case where a misbehaving device is   */
                     /* sending <cr><lf> terminated commands instead of */
                     /* just <cr>. In that case, the <lf> will be left  */
                     /* over from the last read so this will dump it.   */
                     /* If this character shows up in the middle of a   */
                     /* command, everything read up to this point will  */
                     /* be dumped, too. This is acceptable since the    */
                     /* presence of non-printable characters invalidates*/
                     /* the command, anyway.                            */
                     if(NatData->ReadBuffer[Position - 1] == 0x0a)
                     {
                        SS1_LOGI("Encountered 0x0a unexpectedly, flushing buffer");
                        {
                           /* XXX DEBUG */
                           char DebugBuffer[600];
                           memcpy(DebugBuffer, NatData->ReadBuffer, sizeof(NatData->ReadBuffer));
                           DebugBuffer[NatData->ReadBufferLength] = '\0';
                           SS1_LOGI("Buffer contents was: [%s]", DebugBuffer);
                        }

                        /* Shift any remaining buffer contents back to  */
                        /* the beginning of the buffer so we can always */
                        /* accept the longest possible command string.  */
                        NatData->ReadBufferLength -= Position;
                        memmove(NatData->ReadBuffer, &(NatData->ReadBuffer[Position]), NatData->ReadBufferLength);
                        Position = 0;

                        {
                           /* XXX DEBUG */
                           char DebugBuffer[600];
                           memcpy(DebugBuffer, NatData->ReadBuffer, sizeof(NatData->ReadBuffer));
                           DebugBuffer[NatData->ReadBufferLength] = '\0';
                           SS1_LOGI("Buffer contents is:  [%s]", DebugBuffer);
                        }
                     }

                     /* If we've exhausted the buffer contents but still*/
                     /* have buffer space available, try reading more   */
                     /* data into the tail of the buffer.               */
                     if((Position == NatData->ReadBufferLength) && (NatData->ReadBufferLength < sizeof(NatData->ReadBuffer)))
                     {
                        if((ReadResult = SPPM_ReadData(PortHandle, TimeoutMS, (sizeof(NatData->ReadBuffer) - NatData->ReadBufferLength), &(NatData->ReadBuffer[NatData->ReadBufferLength]))) > 0)
                           NatData->ReadBufferLength += ReadResult;
                     }
                  }

                  /* Record any read error that occurred.               */
                  if(ReadResult < 0)
                     NatData->LastReadError = SPPMErrorToErrno(ReadResult);

                  /* If we have any data to send back, build the command*/
                  /* string to return. Errors during the read are       */
                  /* indicated by returning NULL.                       */
                  if(NatData->ReadBufferLength > 0)
                  {
                     /* At this point, we have either found the end of  */
                     /* the AT command, filled the buffer, or timed out */
                     /* before fetching more data.                      */

                     /* If we have reached the end of the buffer or     */
                     /* timed out before reading more data, move back to*/
                     /* the last valid buffer position.                 */
                     if(Position == NatData->ReadBufferLength)
                        Position--;

                     /* Null-terminate the command string. Normally,    */
                     /* this will overwrite the <CR> character which    */
                     /* delimits AT commands. If we filled the buffer   */
                     /* without finding the <CR>, this will overwrite   */
                     /* potentially valid data. This is acceptable      */
                     /* because the command is too long for us to read  */
                     /* anyway -- regardless of whether we preserve this*/
                     /* byte, the next call to this function will catch */
                     /* the remainder of the command string which will  */
                     /* also be considered invalid (for missing it's    */
                     /* prefix).                                        */
                     NatData->ReadBuffer[Position] = '\0';

                     SS1_LOGI("Read %u bytes", Position - 1);

                     /* Make sure we didn't get sent bogus data         */
                     /* which could cause a crash on the call to        */
                     /* NewStringUTF().                                 */
                     SanitizeUTF8((char *)(NatData->ReadBuffer), Position);

                     ATCommandString = Env->NewStringUTF((char *)(NatData->ReadBuffer));

                     /* Shift any remaining data back to the beginning  */
                     /* of the buffer in preparation for the next call  */
                     /* to this function.                               */
                     Position++;
                     NatData->ReadBufferLength -= Position;
                     memmove(NatData->ReadBuffer, &(NatData->ReadBuffer[Position]), NatData->ReadBufferLength);
                  }
                  else
                  {
                     /* We have no data to send. In this case           */
                     /* we return NULL, whether due to no data          */
                     /* available or an error. The caller can call      */
                     /* getLastReadStatusNative() to determine the      */
                     /* reason for the NULL result. A reason of zero    */
                     /* implies a timeout with no data, while anything  */
                     /* else corresponds to an errno-compatible error   */
                     /* code.                                           */
                     ATCommandString = NULL;
                  }

                  pthread_mutex_unlock(&(NatData->ReadBufferMutex));
               }
            }
            else
            {
               SS1_LOGE("Called on unconnected client");
               if(pthread_mutex_lock(&(NatData->ReadBufferMutex)) == 0)
               {
                  NatData->LastReadError = ENOTCONN;
                  pthread_mutex_unlock(&(NatData->ReadBufferMutex));
               }
            }
         }
         else
            SS1_LOGE("Unable to lock mutex");
      }
      else
         SS1_LOGE("Object not initialized");
   }
   else
      SS1_LOGE("Unable to access Bluetooth platform");

   if(NatData)
   {
      if(Port)
         CHECK_MUTEX(&(Port->Mutex));
      CHECK_MUTEX(&(NatData->ReadBufferMutex));
   }

   /* DEBUG */
   {
      if(Env->IsSameObject(ATCommandString, NULL))
         SS1_LOGI("Returning NULL");
      else
      {
         const char *strptr = Env->GetStringUTFChars(ATCommandString, NULL);
         SS1_LOGI("Returning \"%s\"", strptr);
         Env->ReleaseStringUTFChars(ATCommandString, strptr);
      }
   }

   SS1_LOGD("Exit (%p)", ATCommandString);

   return(ATCommandString);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%p)", NULL);
   return(NULL);
#endif
}

static jint HB_GetLastReadStatusNative(JNIEnv *Env, jobject Object)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   int              ret_val;
   HB_NativeData_t *NatData;

   ret_val = 0;

   if((NatData = GetNativeData(Env, Object)) != NULL)
   {
      if(pthread_mutex_lock(&(NatData->ReadBufferMutex)) == 0)
      {
         ret_val = NatData->LastReadError;

         pthread_mutex_unlock(&(NatData->ReadBufferMutex));
      }
      else
         SS1_LOGE("Unable to lock mutex");
   }
   else
      SS1_LOGE("Object not initialized");

   if(NatData)
      CHECK_MUTEX(&(NatData->ReadBufferMutex));

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", 0);
   return(0);
#endif
}

static JNINativeMethod NativeMethods[] =
{
   /* name, signature, funcPtr */
   {"classInitNative",            "()V",                   (void *)HB_ClassInitNative},
   {"initializeNativeDataNative", "(I)V",                  (void *)HB_InitializeNativeDataNative},
   {"cleanupNativeDataNative",    "()V",                   (void *)HB_CleanupNativeDataNative},
   {"connectNative",              "()Z",                   (void *)HB_ConnectNative},
   {"connectAsyncNative",         "()I",                   (void *)HB_ConnectAsyncNative},
   {"waitForAsyncConnectNative",  "(I)I",                  (void *)HB_WaitForAsyncConnectNative},
   {"disconnectNative",           "()V",                   (void *)HB_DisconnectNative},
   {"sendURCNative",              "(Ljava/lang/String;)Z", (void *)HB_SendURCNative},
   {"readNative",                 "(I)Ljava/lang/String;", (void *)HB_ReadNative},
   {"getLastReadStatusNative",    "()I",                   (void *)HB_GetLastReadStatusNative},
};

int SS1API register_android_bluetooth_HeadsetBase(JNIEnv *Env)
{
   return(AndroidRuntime::registerNativeMethods(Env, "android/bluetooth/HeadsetBase", NativeMethods, NELEM(NativeMethods)));
}

} /* namespace android */
