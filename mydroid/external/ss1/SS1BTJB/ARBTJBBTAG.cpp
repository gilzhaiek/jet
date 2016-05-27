/*****< ARBTJBBTAG.cpp >*******************************************************/
/*      Copyright 2010 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  ARBTJBBTAG - android.bluetooth.BluetoothAudioGateway module for           */
/*               Stonestreet One Android Runtime Bluetooth JNI Bridge         */
/*                                                                            */
/*  Author:  Greg Hensley                                                     */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   06/30/10  G. Hensley     Initial creation.                               */
/******************************************************************************/

#define LOG_TAG "ARBTJBBTAG"

#include "android_runtime/AndroidRuntime.h"
#include "JNIHelp.h"
#include "jni.h"
#include "utils/Log.h"
#include "utils/misc.h"

#include <time.h>

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
   /* in */
static jfieldID Field_mHandsfreeAgRfcommChannel;
static jfieldID Field_mHeadsetAgRfcommChannel;
   /* out */
static jfieldID Field_mTimeoutRemainingMs; /* out */

static jfieldID Field_mConnectingHeadsetAddress;
static jfieldID Field_mConnectingHeadsetRfcommChannel; /* -1 when not connected */
static jfieldID Field_mConnectingHeadsetSocketFd;

static jfieldID Field_mConnectingHandsfreeAddress;
static jfieldID Field_mConnectingHandsfreeRfcommChannel; /* -1 when not connected */
static jfieldID Field_mConnectingHandsfreeSocketFd;


typedef struct _tagBTAG_NativeData_t
{
   pthread_t      HSSignalAcceptThread;
   pthread_t      HFSignalAcceptThread;
   pthread_cond_t ClientAcceptCond;
   unsigned int   HSPortInternalHandle;
   unsigned int   HFPortInternalHandle;
} BTAG_NativeData_t;

typedef struct _tagBTAG_ThreadData_t
{
   pthread_cond_t *ClientAcceptCond;
   unsigned int    PortInternalHandle;
} BTAG_ThreadData_t;

void *AcceptSignalThread(void *ThreadParameter)
{
   unsigned int     PortInternalHandle;
   pthread_cond_t  *BTAGClientAcceptCond;
   BTSO_PortData_t *Port;

   Port = NULL;

   if(ThreadParameter)
   {
      BTAGClientAcceptCond   = ((BTAG_ThreadData_t *)ThreadParameter)->ClientAcceptCond;
      PortInternalHandle = ((BTAG_ThreadData_t *)ThreadParameter)->PortInternalHandle;

      free(ThreadParameter);

      if(BTAGClientAcceptCond)
      {
         SS1_LOGD("BTAG Port: Searching (IH=%u)", PortInternalHandle);
         if((Port = BTSO_AcquirePortData(PortInternalHandle)) != NULL)
         {
            SS1_LOGD("BTAG Port: Acquired (IH=%u)", PortInternalHandle);

            while(Port->ServerStatus == btsoServerListening)
            {
               //SS1_LOGD("Waiting on accept event for server handle %u", Port->Handle);
               BTSO_WaitOnPortConditionMS(Port, &(Port->ClientAcceptCond), 60000);
               //SS1_LOGI("Woke up for accept event for server handle %u", Port->Handle);

               /* We were signaled awake. If the server port is still   */
               /* in a listening state and its client status has been   */
               /* placed in an Accepting state, signal the general      */
               /* accept condition.                                     */
               if((Port->ServerStatus == btsoServerListening) && (Port->ClientStatus == btsoClientAccepting))
               {
                  SS1_LOGD("Client connection is being accepted on handle %u (IH=%u), signalling event", Port->Handle, PortInternalHandle);

                  /* **NOTE** Android suffers a race condition when     */
                  /* the incoming connection also triggered a pairing   */
                  /* attempt. In that case, Android's processing of the */
                  /* nearly simultaneous "device paired" signal and     */
                  /* this incoming connection notification causes the   */
                  /* phone app and system_server (BluetoothService) to  */
                  /* deadlock against one another. The user-visible     */
                  /* effect is that the Settings app will freeze as it  */
                  /* attempts to make a call into the BluetoothService. */
                  /* To avoid this, introduce a small, artificial       */
                  /* delay in the reporting of the incoming Hands-Free  */
                  /* connection, here.                                  */
                  {
                     struct timespec Delay;

                     Delay.tv_sec  = 0;
                     Delay.tv_nsec = 100000000;

                     nanosleep(&Delay, NULL);
                  }

                  SS1_LOGD("Signalling delay complete.");
                  pthread_cond_broadcast(BTAGClientAcceptCond);
               }
            }

            SS1_LOGD("Server no longer listening, thread shutting down");

            /* The server port is no longer in a listening state, so    */
            /* release resources and exit.                              */
            BTSO_ReleasePortData(Port);
            Port = NULL;
            SS1_LOGD("BTAG Port: Released (IH=%u)", PortInternalHandle);
         }
         else
            SS1_LOGE("Unable to acquire port data (IH=%u)", PortInternalHandle);
      }
      else
         SS1_LOGE("Parameter is invalid");
   }
   else
      SS1_LOGE("Parameter is uninitialized");

   if(Port)
      CHECK_MUTEX(&(Port->Mutex));

   return(NULL);
}

static BTAG_NativeData_t *GetNativeData(JNIEnv *Env, jobject Object)
{
   BTAG_NativeData_t *NatData;

   if((NatData = (BTAG_NativeData_t *)(Env->GetIntField(Object, Field_mNativeData))) == NULL)
      jniThrowException(Env, "java/io/IOException", "BluetoothAudioGateway not initialized (stack failure?)");

   return NatData;
}

#endif /* HAVE_BLUETOOTH */

static void BTAG_ClassInitNative(JNIEnv *Env, jclass Clazz)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   Field_mNativeData                       = Env->GetFieldID(Clazz, "mNativeData",                       "I");
   Field_mHandsfreeAgRfcommChannel         = Env->GetFieldID(Clazz, "mHandsfreeAgRfcommChannel",         "I");
   Field_mHeadsetAgRfcommChannel           = Env->GetFieldID(Clazz, "mHeadsetAgRfcommChannel",           "I");

   Field_mConnectingHeadsetAddress         = Env->GetFieldID(Clazz, "mConnectingHeadsetAddress",         "Ljava/lang/String;");
   Field_mConnectingHeadsetRfcommChannel   = Env->GetFieldID(Clazz, "mConnectingHeadsetRfcommChannel",   "I");
   Field_mConnectingHeadsetSocketFd        = Env->GetFieldID(Clazz, "mConnectingHeadsetSocketFd",        "I");

   Field_mConnectingHandsfreeAddress       = Env->GetFieldID(Clazz, "mConnectingHandsfreeAddress",       "Ljava/lang/String;");
   Field_mConnectingHandsfreeRfcommChannel = Env->GetFieldID(Clazz, "mConnectingHandsfreeRfcommChannel", "I");
   Field_mConnectingHandsfreeSocketFd      = Env->GetFieldID(Clazz, "mConnectingHandsfreeSocketFd",      "I");

   Field_mTimeoutRemainingMs               = Env->GetFieldID(Clazz, "mTimeoutRemainingMs",               "I");
#endif /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit");
}

static void BTAG_InitializeNativeDataNative(JNIEnv *Env, jobject Object)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   int                Result;
   unsigned int       Count;
   BTSO_PortData_t   *HSPort;
   BTSO_PortData_t   *HFPort;
   BTAG_NativeData_t *NatData;

   NatData = (BTAG_NativeData_t *)calloc(1, sizeof(BTAG_NativeData_t));

   HSPort = NULL;
   HFPort = NULL;

   Env->SetIntField(Object, Field_mNativeData, (jint)NatData);

   if(NatData)
   {
      /* We are creating a completely new "socket" for this             */
      /* BluetoothAudioGateway. Unlike BluetoothSocket,                 */
      /* BluetoothAudioGateway can only represent the server side of an */
      /* SPP port, so we don't keep a Type.                             */
      if((Result = BTSO_CreatePortData()) >= 0)
      {
         NatData->HSPortInternalHandle = (unsigned int)Result;

         SS1_LOGD("BTAG Port: Searching (IH=%u)", NatData->HSPortInternalHandle);
         if((HSPort = BTSO_AcquirePortData(NatData->HSPortInternalHandle)) != NULL)
            SS1_LOGD("BTAG Port: Acquired (IH=%u)", NatData->HSPortInternalHandle);

         if((Result = BTSO_CreatePortData()) >= 0)
         {
            NatData->HFPortInternalHandle = (unsigned int)Result;

            SS1_LOGD("BTAG Port: Searching (IH=%u)", NatData->HFPortInternalHandle);
            if((HFPort = BTSO_AcquirePortData(NatData->HFPortInternalHandle)) != NULL)
               SS1_LOGD("BTAG Port: Acquired (IH=%u)", NatData->HFPortInternalHandle);

            SS1_LOGD("Created NatData @ %p with new Headset Port (IH=%u) and Handsfree Port (IH=%u)", NatData, NatData->HSPortInternalHandle, NatData->HFPortInternalHandle);
         }
      }

      if((HSPort) && (HFPort))
      {
         Count = BTSO_IncRefCount(&(HSPort->RefCount));
         SS1_LOGD("Incremented reference count on Headset port data (IH=%u): new count = %u", NatData->HSPortInternalHandle, Count);

         Count = BTSO_IncRefCount(&(HFPort->RefCount));
         SS1_LOGD("Incremented reference count on Handsfree port data (IH=%u): new count = %u", NatData->HFPortInternalHandle, Count);

         pthread_cond_init(&(NatData->ClientAcceptCond), NULL);

         BTSO_ReleasePortData(HSPort);
         HSPort = NULL;
         SS1_LOGD("BTAG Port: Released (IH=%u)", NatData->HSPortInternalHandle);

         BTSO_ReleasePortData(HFPort);
         HFPort = NULL;
         SS1_LOGD("BTAG Port: Released (IH=%u)", NatData->HFPortInternalHandle);
      }
      else
      {
         if(HSPort)
            BTSO_DestroyPortData(HSPort);

         if(HFPort)
            BTSO_DestroyPortData(HFPort);

         free(NatData);
         jniThrowException(Env, "java/lang/OutOfMemoryError", "Out of memory while configuring HeadsetBase");
      }
   }
   else
   {
      SS1_LOGE("Unable to allocate storage");
      jniThrowException(Env, "java/lang/OutOfMemoryError", "Out of memory while constructing BluetoothAudioGateway native component");
   }

   /* Initialize some state variables of the Java class.                */
   Env->SetIntField(Object, Field_mConnectingHeadsetRfcommChannel, -1);
   Env->SetIntField(Object, Field_mConnectingHandsfreeRfcommChannel, -1);
#endif /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit");
}

static void BTAG_CleanupNativeDataNative(JNIEnv *Env, jobject Object)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   unsigned int       Count;
   unsigned int       HSPortInternalHandle;
   unsigned int       HFPortInternalHandle;
   BTSO_PortData_t   *PortData;
   BTAG_NativeData_t *NatData;

   /* Connect to the Bluetooth daemon, but only try once. Since we're   */
   /* only closing ports, if the service is down, then the connections  */
   /* are already closed. If we only lost IPC connectivity, the daemon  */
   /* should have closed our connections for us when it noticed we had  */
   /* dropped off.                                                      */
   InitBTPMClientNoRetries(NULL, NULL, NULL);

   if((NatData = GetNativeData(Env, Object)) != NULL)
   {
      Env->SetIntField(Object, Field_mNativeData, (jint)NULL);

      HSPortInternalHandle = NatData->HSPortInternalHandle;
      HFPortInternalHandle = NatData->HFPortInternalHandle;

      while(pthread_cond_destroy(&(NatData->ClientAcceptCond)) == EBUSY)
      {
         pthread_cond_broadcast(&(NatData->ClientAcceptCond));
         sched_yield();
//XXX
      }

      free(NatData);

      SS1_LOGD("Got Headset port data (IH=%u) and Handsfree port data (IH=%u), and destroyed native data", HSPortInternalHandle, HFPortInternalHandle);

      if((PortData = BTSO_AcquirePortData(HSPortInternalHandle)) != NULL)
      {
         Count = BTSO_DecRefCount(&(PortData->RefCount));
         SS1_LOGD("Headset Port data reference count == %u", Count);

         if(Count == 0)
         {
            /* The reference count on the Port data reached zero, so    */
            /* we held the last claimed reference. We should clean up   */
            /* any contained resources, now. There's no need to lock    */
            /* the structure's mutex since no one else should have a    */
            /* reference to the object at this point.                   */
            if(PortData->ClientStatus != btsoClientDisconnected)
            {
               SS1_LOGI("Closing Headset client connection");
               PortData->ClientStatus = btsoClientDisconnected;

               /* We hold the last reference, so there shouldn't be     */
               /* anyone waiting on events. Signal them just in case.   */
               pthread_cond_broadcast(&(PortData->ClientConnectCond));
               SPPM_ClosePort(PortData->Handle, BTSO_CLOSE_PORT_TIMEOUT);
            }

            if(PortData->ServerStatus == btsoServerListening)
            {
               SS1_LOGI("Closing Headset server registration");
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
            if((PortData = BTSO_AcquirePortData(HSPortInternalHandle)) != NULL)
            {
               BTSO_DestroyPortData(PortData);
               SS1_LOGD("Destroyed Headset port data (IH=%u)", HSPortInternalHandle);
            }
            else
               SS1_LOGD("Headset port data (IH=%u) already destroyed.", HSPortInternalHandle);
         }
         else
         {
            if((signed int)Count == -1)
               SS1_LOGE("Reference count for Headset port data @ %p was already 0", PortData);
            else
               SS1_LOGD("Reference count == %u for Headset port data @ %p, not destroying", Count, PortData);

            BTSO_ReleasePortData(PortData);
         }
      }
      else
         SS1_LOGD("Headset Port data uninitialized");

      if((PortData = BTSO_AcquirePortData(HFPortInternalHandle)) != NULL)
      {
         Count = BTSO_DecRefCount(&(PortData->RefCount));
         SS1_LOGD("Handsfree Port data reference count == %u", Count);
         if(Count == 0)
         {
            /* The reference count on the Port data reached zero, so    */
            /* we held the last claimed reference. We should clean up   */
            /* any contained resources, now. There's no need to lock    */
            /* the structure's mutex since no one else should have a    */
            /* reference to the object at this point.                   */
            if(PortData->ClientStatus != btsoClientDisconnected)
            {
               SS1_LOGI("Closing Handsfree client connection");
               PortData->ClientStatus = btsoClientDisconnected;

               /* We hold the last reference, so there shouldn't be     */
               /* anyone waiting on events. Signal them just in case.   */
               pthread_cond_broadcast(&(PortData->ClientConnectCond));
               SPPM_ClosePort(PortData->Handle, BTSO_CLOSE_PORT_TIMEOUT);
            }

            if(PortData->ServerStatus == btsoServerListening)
            {
               SS1_LOGI("Closing Handsfree server registration");
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
            if((PortData = BTSO_AcquirePortData(HSPortInternalHandle)) != NULL)
            {
               BTSO_DestroyPortData(PortData);
               SS1_LOGD("Destroyed Handsfree port data (IH=%u)", HFPortInternalHandle);
            }
            else
               SS1_LOGD("Handsfree port data (IH=%u) already destroyed.", HSPortInternalHandle);
         }
         else
         {
            if((signed int)Count == -1)
               SS1_LOGE("Reference count for Handsfree port data @ %p was already 0", PortData);
            else
               SS1_LOGD("Reference count == %u for Handsfree port data @ %p, not destroying", Count, PortData);

            BTSO_ReleasePortData(PortData);
         }
      }
      else
         SS1_LOGD("Handsfree Port data uninitialized");
   }
   else
      SS1_LOGE("Called on object with uninitialized data!");
#endif /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit");
}

static jboolean BTAG_WaitForHandsfreeConnectNative(JNIEnv *Env, jobject Object, jint TimeoutMS)
{
//    SS1_LOGD("Enter (%d)", TimeoutMS);
#ifdef HAVE_BLUETOOTH
   int                WaitResult;
   char               AddressStringBuffer[32];
   jstring            AddressString;
   jboolean           ret_val;
   struct timeval     TimeValCurrent;
   struct timeval     TimeValGoal;
   struct timeval     TimeValDiff;
   pthread_mutex_t    WaitMutex = PTHREAD_MUTEX_INITIALIZER;
   struct timespec    TimeSpec;
   BTSO_PortData_t   *Port;
   BTAG_NativeData_t *NatData;

   ret_val    = JNI_FALSE;
   WaitResult = -1;

   //SS1_LOGI("Called with timeout = %d ms", TimeoutMS);

   if((NatData = GetNativeData(Env, Object)) != NULL)
   {
      if(pthread_mutex_lock(&(WaitMutex)) == 0)
      {
         Env->SetIntField(Object, Field_mTimeoutRemainingMs, TimeoutMS);

         //SS1_LOGI("Waiting for accept signal for %d ms", TimeoutMS);

         if(TimeoutMS >= 0)
         {
            /* We were given a timeout (in milliseconds), so only wait  */
            /* that long.                                               */
            gettimeofday(&TimeValGoal, NULL);
            //SS1_LOGI("Current time = %ds %du", TimeValGoal.tv_sec, TimeValGoal.tv_usec);
            TimeValDiff.tv_sec = 0;
            TimeValDiff.tv_usec = (TimeoutMS * 1000L);
            timeradd(&TimeValGoal, &TimeValDiff, &TimeValGoal);
            TimeSpec.tv_sec  = TimeValGoal.tv_sec;
            TimeSpec.tv_nsec = (TimeValGoal.tv_usec * 1000L);

            //SS1_LOGI("Waiting until %ds %du", TimeValGoal.tv_sec, TimeValGoal.tv_usec);

            WaitResult = pthread_cond_timedwait(&(NatData->ClientAcceptCond), &(WaitMutex), &TimeSpec);

            if(TimeoutMS > 0)
            {
               /* Record how much time is remaining from the specified  */
               /* timeout, if any.                                      */
               gettimeofday(&TimeValCurrent, NULL);
               //SS1_LOGI("Setting remaining timeout... (Goal = %ds %du, Current = %ds %du)", TimeValGoal.tv_sec, TimeValGoal.tv_usec, TimeValCurrent.tv_sec, TimeValCurrent.tv_usec);
               if(!timercmp(&TimeValGoal, &TimeValCurrent, <))
               {
                  /* We have not exceeded the timeout value, so record  */
                  /* the remaining timeout.                             */
                  timersub(&TimeValGoal, &TimeValCurrent, &TimeValDiff);
                  TimeoutMS = (TimeValDiff.tv_sec * 1000L) + (TimeValDiff.tv_usec / 1000L);
                  //SS1_LOGI("Setting remaining timeout to %d ms", TimeoutMS);
                  Env->SetIntField(Object, Field_mTimeoutRemainingMs, TimeoutMS);
               }
               else
                  Env->SetIntField(Object, Field_mTimeoutRemainingMs, 0);
            }
         }
         else
         {
            /* No valid timeout was specified, so wait forever.         */
            WaitResult = pthread_cond_wait(&(NatData->ClientAcceptCond), &(WaitMutex));
            Env->SetIntField(Object, Field_mTimeoutRemainingMs, 0);
         }

         //SS1_LOGI("Woke up (Result = %d)", WaitResult);

         if(WaitResult == 0)
         {
            /* Someone connected, so see who it is. We'll check         */
            /* Handsfree first.                                         */
            SS1_LOGD("BTAG Port: Searching (IH=%u)", NatData->HFPortInternalHandle);
            if((Port = BTSO_AcquirePortData(NatData->HFPortInternalHandle)) != NULL)
            {
               SS1_LOGD("BTAG Port: Acquired (IH=%u) (%p)", NatData->HFPortInternalHandle, Port);

               if(Port->ClientStatus == btsoClientAccepting)
               {
                  ret_val            = JNI_TRUE;
                  Port->ClientStatus = btsoClientConnected;

                  BD_ADDRToStr(AddressStringBuffer, sizeof(AddressStringBuffer), Port->RemoteAddress);
                  AddressString = Env->NewStringUTF(AddressStringBuffer);

                  Env->SetIntField(Object, Field_mConnectingHandsfreeSocketFd, (jint)(NatData->HFPortInternalHandle));
                  Env->SetObjectField(Object, Field_mConnectingHandsfreeAddress, AddressString);
                  Env->SetIntField(Object, Field_mConnectingHandsfreeRfcommChannel, Port->Handle);

                  Env->DeleteLocalRef(AddressString);
               }

               BTSO_ReleasePortData(Port);
               Port = NULL;
               SS1_LOGD("BTAG Port: Released (IH=%u)", NatData->HFPortInternalHandle);
            }

            SS1_LOGD("BTAG Port: Searching (IH=%u)", NatData->HSPortInternalHandle);
            if((Port = BTSO_AcquirePortData(NatData->HSPortInternalHandle)) != NULL)
            {
               SS1_LOGD("BTAG Port: Acquired (IH=%u) (%p)", NatData->HSPortInternalHandle, Port);

               if(Port->ClientStatus == btsoClientAccepting)
               {
                  ret_val            = JNI_TRUE;
                  Port->ClientStatus = btsoClientConnected;

                  BD_ADDRToStr(AddressStringBuffer, sizeof(AddressStringBuffer), Port->RemoteAddress);
                  AddressString = Env->NewStringUTF(AddressStringBuffer);

                  Env->SetIntField(Object, Field_mConnectingHeadsetSocketFd, (jint)(NatData->HSPortInternalHandle));
                  Env->SetObjectField(Object, Field_mConnectingHeadsetAddress, AddressString);
                  Env->SetIntField(Object, Field_mConnectingHeadsetRfcommChannel, Port->Handle);

                  Env->DeleteLocalRef(AddressString);
               }

               BTSO_ReleasePortData(Port);
               Port = NULL;
               SS1_LOGD("BTAG Port: Released (IH=%u)", NatData->HSPortInternalHandle);
            }
         }

         pthread_mutex_unlock(&(WaitMutex));
      }
   }

   if(NatData)
   {
      if(Port)
         CHECK_MUTEX(&(Port->Mutex));
   }

   //SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH */
   //SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(JNI_FALSE);
#endif
}

static jboolean BTAG_SetUpListeningSocketsNative(JNIEnv *Env, jobject Object)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   int                Result;
   jint               ProtocolType;
   jboolean           HSPortStatus;
   jboolean           HFPortStatus;
   unsigned int       Channel;
   unsigned long      Flags;
   BTSO_PortData_t   *Port;
   BTAG_NativeData_t *NatData;
   BTAG_ThreadData_t *ThreadData;

   NatData      = NULL;
   HSPortStatus = JNI_FALSE;
   HFPortStatus = JNI_FALSE;

   /* Make sure this process has a connection to the Bluetopia daemon.  */
   if(InitBTPMClientNoRetries(NULL, NULL, NULL) == 0)
   {
      if((NatData = GetNativeData(Env, Object)) != NULL)
      {
         SS1_LOGD("BTAG Port: Searching (IH=%u)", NatData->HSPortInternalHandle);
         if((Port = BTSO_AcquirePortData(NatData->HSPortInternalHandle)) != NULL)
         {
            SS1_LOGD("BTAG Port: Acquired (IH=%u)", NatData->HSPortInternalHandle);

            /* Only Bind/Listen if this object hasn't been opened       */
            /* yet nor connected as a client. If it had been            */
            /* connected as a client, ServerStatus would be set to      */
            /* btsoServerNotAvailable.                                  */
            if(Port->ServerStatus == btsoServerUninitialized)
            {
               if(Port->Handle == 0)
               {
                  Flags = SPPM_REGISTER_SERVER_PORT_FLAGS_REQUIRE_AUTHENTICATION | SPPM_REGISTER_SERVER_PORT_FLAGS_REQUIRE_ENCRYPTION;

                  Channel = (unsigned int)Env->GetIntField(Object, Field_mHeadsetAgRfcommChannel);
                  if(RFCOMM_VALID_SERVER_CHANNEL_ID(Channel))
                  {
                     SS1_LOGI("Registering Headset server channel %u, with callback @ %p and param (IH=%u)", Channel, BTSO_SPPM_EventCallback, NatData->HSPortInternalHandle);
                     if((Result = SPPM_RegisterServerPort(Channel, Flags, BTSO_SPPM_EventCallback, (void *)NatData->HSPortInternalHandle)) > 0)
                     {
                        HSPortStatus       = JNI_TRUE;
                        Port->Handle       = (unsigned int)Result;
                        Port->ServerStatus = btsoServerListening;
                     }
                     else
                        SS1_LOGE("Headset Socket bind/listen failed (%d)", Result);
                  }
                  else
                     SS1_LOGE("Bad Headset RFCOMM channel (%u)", Channel);
               }
               else
               {
                  SS1_LOGW("Headset port already initialized (H=%d, %d)", Port->Handle, Port->ServerStatus);

                  HSPortStatus       = JNI_TRUE;
                  Port->ServerStatus = btsoServerListening;
               }
            }
            else
               SS1_LOGE("Headset port not properly torn down (H=%d, %d)", Port->Handle, Port->ServerStatus);

            BTSO_ReleasePortData(Port);
            Port = NULL;
            SS1_LOGD("BTAG Port: Released (IH=%u)", NatData->HSPortInternalHandle);
         }
         else
            SS1_LOGE("Unable to obtain Headset lock (%d)", errno);

         if(HSPortStatus == JNI_TRUE)
         {
            SS1_LOGD("BTAG Port: Searching (IH=%u)", NatData->HFPortInternalHandle);
            if((Port = BTSO_AcquirePortData(NatData->HFPortInternalHandle)) != NULL)
            {
               SS1_LOGD("BTAG Port: Acquired (IH=%u)", NatData->HFPortInternalHandle);

               /* Only Bind/Listen if this object hasn't been opened    */
               /* yet nor connected as a client. If it had been         */
               /* connected as a client, ServerStatus would be set to   */
               /* btsoServerNotAvailable.                               */
               if(Port->ServerStatus == btsoServerUninitialized)
               {
                  if(Port->Handle == 0)
                  {
                     Flags = SPPM_REGISTER_SERVER_PORT_FLAGS_REQUIRE_AUTHENTICATION | SPPM_REGISTER_SERVER_PORT_FLAGS_REQUIRE_ENCRYPTION;

                     Channel = (unsigned int)Env->GetIntField(Object, Field_mHandsfreeAgRfcommChannel);
                     if(RFCOMM_VALID_SERVER_CHANNEL_ID(Channel))
                     {
                        SS1_LOGI("Registering Handsfree server channel %u, with callback @ %p and param (IH=%u)", Channel, BTSO_SPPM_EventCallback, NatData->HFPortInternalHandle);
                        if((Result = SPPM_RegisterServerPort(Channel, Flags, BTSO_SPPM_EventCallback, (void *)NatData->HFPortInternalHandle)) > 0)
                        {
                           HFPortStatus       = JNI_TRUE;
                           Port->Handle       = (unsigned int)Result;
                           Port->ServerStatus = btsoServerListening;
                        }
                        else
                           SS1_LOGE("Handsfree Socket bind/listen failed (%d)", Result);
                     }
                     else
                        SS1_LOGE("Bad Handsfree RFCOMM channel (%u)", Channel);
                  }
                  else
                  {
                     SS1_LOGW("Handsfree port already initialized (H=%d, %d)", Port->Handle, Port->ServerStatus);

                     HFPortStatus       = JNI_TRUE;
                     Port->ServerStatus = btsoServerListening;
                  }
               }
               else
                  SS1_LOGE("Handsfree port not properly torn down (H=%d, %d)", Port->Handle, Port->ServerStatus);

               BTSO_ReleasePortData(Port);
               Port = NULL;
               SS1_LOGD("BTAG Port: Released (IH=%u)", NatData->HFPortInternalHandle);
            }
            else
               SS1_LOGE("Unable to obtain Handsfree lock (%d)", errno);
         }

         /* Start threads to monitor accept signals. These threads will */
         /* server to combine accept notifications into one pthread     */
         /* condition we can wait on.                                   */
         if(HSPortStatus)
         {
            if((ThreadData = (BTAG_ThreadData_t *)malloc(sizeof(BTAG_ThreadData_t))) != NULL)
            {
               ThreadData->ClientAcceptCond   = &(NatData->ClientAcceptCond);
               ThreadData->PortInternalHandle = NatData->HSPortInternalHandle;

               if(pthread_create(&(NatData->HSSignalAcceptThread), NULL, AcceptSignalThread, ThreadData) != 0)
                  HSPortStatus = JNI_FALSE;
            }
            else
               HSPortStatus = JNI_FALSE;
         }

         if(HFPortStatus)
         {
            if((ThreadData = (BTAG_ThreadData_t *)malloc(sizeof(BTAG_ThreadData_t))) != NULL)
            {
               ThreadData->ClientAcceptCond   = &(NatData->ClientAcceptCond);
               ThreadData->PortInternalHandle = NatData->HFPortInternalHandle;

               if(pthread_create(&(NatData->HFSignalAcceptThread), NULL, AcceptSignalThread, ThreadData) != 0)
                  HFPortStatus = JNI_FALSE;
            }
            else
               HFPortStatus = JNI_FALSE;
         }

         /* If anything failed to start, shut down both ports.          */
         if((HSPortStatus == JNI_FALSE) || (HFPortStatus == JNI_FALSE))
         {
            /* Shut down Headset port.                                  */
            SS1_LOGD("BTAG Port: Searching (IH=%u)", NatData->HSPortInternalHandle);
            if((Port = BTSO_AcquirePortData(NatData->HSPortInternalHandle)) != NULL)
            {
               SS1_LOGD("BTAG Port: Acquired (IH=%u)", NatData->HSPortInternalHandle);

               SS1_LOGI("Server port setup failed, closing Headset");
               Port->ServerStatus = btsoServerUninitialized;

               /* Wake up accept signal thread and wait for it to exit. */
               pthread_cond_broadcast(&(Port->ClientAcceptCond));

               /* Release port data so the accept thread can acquire it.*/
               BTSO_ReleasePortData(Port);
               Port = NULL;
               SS1_LOGD("BTAG Port: Released (IH=%u)", NatData->HSPortInternalHandle);

               pthread_join(NatData->HSSignalAcceptThread, NULL);

               SS1_LOGD("BTAG Port: Searching (IH=%u)", NatData->HSPortInternalHandle);
               if((Port = BTSO_AcquirePortData(NatData->HSPortInternalHandle)) != NULL)
               {
                  SS1_LOGD("BTAG Port: Acquired (IH=%u)", NatData->HSPortInternalHandle);

                  /* Close the port.                                    */
                  SPPM_UnRegisterServerPort(Port->Handle);
                  SPPM_ClosePort(Port->Handle, SPPM_CLOSE_DATA_FLUSH_TIMEOUT_IMMEDIATE);
                  Port->Handle = 0;

                  BTSO_ReleasePortData(Port);
                  Port = NULL;
                  SS1_LOGD("BTAG Port: Released (IH=%u)", NatData->HSPortInternalHandle);
               }
            }
            else
               SS1_LOGE("Unable to obtain Headset lock (%d), could not close Headset socket.", errno);

            /* Shut down Handsfree port.                                */
            SS1_LOGD("BTAG Port: Searching (IH=%u)", NatData->HFPortInternalHandle);
            if((Port = BTSO_AcquirePortData(NatData->HFPortInternalHandle)) != NULL)
            {
               SS1_LOGD("BTAG Port: Acquired (IH=%u)", NatData->HFPortInternalHandle);

               SS1_LOGI("Server port setup failed, closing Handsfree");
               Port->ServerStatus = btsoServerUninitialized;

               /* Wake up accept signal thread and wait for it to exit. */
               pthread_cond_broadcast(&(Port->ClientAcceptCond));

               /* Release port data so the accept thread can acquire it.*/
               BTSO_ReleasePortData(Port);
               Port = NULL;
               SS1_LOGD("BTAG Port: Released (IH=%u)", NatData->HFPortInternalHandle);

               pthread_join(NatData->HFSignalAcceptThread, NULL);

               SS1_LOGD("BTAG Port: Searching (IH=%u)", NatData->HFPortInternalHandle);
               if((Port = BTSO_AcquirePortData(NatData->HFPortInternalHandle)) != NULL)
               {
                  SS1_LOGD("BTAG Port: Acquired (IH=%u)", NatData->HFPortInternalHandle);

                  /* Close the port.                                    */
                  SPPM_UnRegisterServerPort(Port->Handle);
                  SPPM_ClosePort(Port->Handle, SPPM_CLOSE_DATA_FLUSH_TIMEOUT_IMMEDIATE);
                  Port->Handle = 0;

                  BTSO_ReleasePortData(Port);
                  Port = NULL;
                  SS1_LOGD("BTAG Port: Released (IH=%u)", NatData->HFPortInternalHandle);
               }
            }
            else
               SS1_LOGE("Unable to obtain Handsfree lock (%d), could not close Handsfree socket.", errno);
         }
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
   }

   SS1_LOGD("Exit (%d)", (HSPortStatus && HFPortStatus));

   return(HSPortStatus && HFPortStatus);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(JNI_FALSE);
#endif
}


   /* private native void tearDownListeningSocketsNative();             */
static void BTAG_TearDownListeningSocketsNative(JNIEnv *Env, jobject Object)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   BTSO_PortData_t   *Port;
   BTAG_NativeData_t *NatData;

   /* Connect to the Bluetooth daemon, but only try once. Since we're   */
   /* only closing ports, if the service is down, then the connections  */
   /* are already closed. If we only lost IPC connectivity, the daemon  */
   /* should have closed our connections for us when it noticed we had  */
   /* dropped off.                                                      */
   InitBTPMClientNoRetries(NULL, NULL, NULL);

   if((NatData = GetNativeData(Env, Object)) != NULL)
   {
      /* DEBUG */ SS1_LOGI("Closing Headset server");

      SS1_LOGD("BTAG Port: Searching (IH=%u)", NatData->HSPortInternalHandle);
      if((Port = BTSO_AcquirePortData(NatData->HSPortInternalHandle)) != NULL)
      {
         SS1_LOGD("BTAG Port: Acquired (IH=%u)", NatData->HSPortInternalHandle);

         if(Port->ServerStatus == btsoServerListening)
         {
            Port->ServerStatus = btsoServerUninitialized;

            if(Port->ClientStatus != btsoClientDisconnected)
            {
               SPPM_ClosePort(Port->Handle, BTSO_CLOSE_PORT_TIMEOUT);
               Port->ClientStatus = btsoClientDisconnected;
            }

            SPPM_UnRegisterServerPort(Port->Handle);
            Port->Handle = 0;
            SS1_LOGI("Headset server closed");

            /* Wake up accept signal thread and wait for it to exit.    */
            /* This will also signal anyone waiting on accept events for*/
            /* the specific server port.                                */
            pthread_cond_broadcast(&(Port->ClientAcceptCond));

            BTSO_ReleasePortData(Port);
            Port = NULL;
            SS1_LOGD("BTAG Port: Released (IH=%u)", NatData->HSPortInternalHandle);

            /* DEBUG */SS1_LOGV("Waiting on Headset thread");
            pthread_join(NatData->HSSignalAcceptThread, NULL);
            /* DEBUG */SS1_LOGV("Headset thread finished");
         }
         else
         {
            BTSO_ReleasePortData(Port);
            Port = NULL;
            SS1_LOGD("BTAG Port: Released (IH=%u)", NatData->HSPortInternalHandle);
         }
      }
      else
         SS1_LOGE("Unable to obtain Headset lock");

      /* DEBUG */ SS1_LOGI("Closing Hands-Free server");

      SS1_LOGD("BTAG Port: Searching (IH=%u)", NatData->HFPortInternalHandle);
      if((Port = BTSO_AcquirePortData(NatData->HFPortInternalHandle)) != NULL)
      {
         SS1_LOGD("BTAG Port: Acquired (IH=%u)", NatData->HFPortInternalHandle);

         if(Port->ServerStatus == btsoServerListening)
         {
            Port->ServerStatus = btsoServerUninitialized;

            if(Port->ClientStatus != btsoClientDisconnected)
            {
               SPPM_ClosePort(Port->Handle, BTSO_CLOSE_PORT_TIMEOUT);
               Port->ClientStatus = btsoClientDisconnected;
            }

            SPPM_UnRegisterServerPort(Port->Handle);
            Port->Handle = 0;
            SS1_LOGI("Handsfree server closed");

            /* Wake up accept signal thread and wait for it to exit.    */
            /* This will also signal anyone waiting on accept events for*/
            /* the specific server port.                                */
            pthread_cond_broadcast(&(Port->ClientAcceptCond));

            BTSO_ReleasePortData(Port);
            Port = NULL;
            SS1_LOGD("BTAG Port: Released (IH=%u)", NatData->HFPortInternalHandle);

            /* DEBUG */SS1_LOGV("Waiting on Hands-Free thread");
            pthread_join(NatData->HFSignalAcceptThread, NULL);
            /* DEBUG */SS1_LOGV("Hands-Free thread finished");
         }
         else
         {
            BTSO_ReleasePortData(Port);
            Port = NULL;
            SS1_LOGD("BTAG Port: Released (IH=%u)", NatData->HFPortInternalHandle);
         }
      }
      else
         SS1_LOGE("Unable to obtain Headset lock");
   }
   else
      SS1_LOGE("Object not initialized");

   if(NatData)
   {
      if(Port)
         CHECK_MUTEX(&(Port->Mutex));
   }

   SS1_LOGD("Exit");
#endif /* HAVE_BLUETOOTH */
}

static JNINativeMethod NativeMethods[] =
{
   /* name, signature, funcPtr */
   {"classInitNative",                "()V",  (void *)BTAG_ClassInitNative},
   {"initializeNativeDataNative",     "()V",  (void *)BTAG_InitializeNativeDataNative},
   {"cleanupNativeDataNative",        "()V",  (void *)BTAG_CleanupNativeDataNative},

   {"setUpListeningSocketsNative",    "()Z",  (void *)BTAG_SetUpListeningSocketsNative},
   {"tearDownListeningSocketsNative", "()V",  (void *)BTAG_TearDownListeningSocketsNative},
   {"waitForHandsfreeConnectNative",  "(I)Z", (void *)BTAG_WaitForHandsfreeConnectNative},
};

int SS1API register_android_bluetooth_BluetoothAudioGateway(JNIEnv *Env)
{
   return(AndroidRuntime::registerNativeMethods(Env, "android/bluetooth/BluetoothAudioGateway", NativeMethods, NELEM(NativeMethods)));
}

} /* namespace android */
