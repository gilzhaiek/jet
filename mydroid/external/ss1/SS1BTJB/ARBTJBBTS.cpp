/*****< ARBTJBBTS.cpp >********************************************************/
/*      Copyright 2010 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  ARBTJBBTS - android.server.BluetoothService module for Stonestreet One    */
/*              Android Runtime Bluetooth JNI Bridge                          */
/*                                                                            */
/*  Author:  Greg Hensley                                                     */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   06/30/10  G. Hensley     Initial creation.                               */
/******************************************************************************/

#define LOG_TAG "ARBTJBBTS"

#include "android_runtime/AndroidRuntime.h"
#include "cutils/properties.h"
#include "JNIHelp.h"
#include "jni.h"
#include "utils/Log.h"
#include "utils/misc.h"

#include <ctype.h>
#include <errno.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>

#include "SS1UTIL.h"

#ifdef HAVE_BLUETOOTH
extern "C"
{
#include "SS1BTPM.h"

#if SS1_SUPPORT_A2DP
#include "SS1BTAUDM.h"
#endif

#if (SS1_PLATFORM_SDK_VERSION >= 14)

#if SS1_SUPPORT_HID
#include "HIDMAPI.h"
#endif

#if SS1_SUPPORT_PAN
#include "PANMAPI.h"
#endif

#endif
}

#include "SPPAPI.h"

#include "ARBTJBCOM.h"
#include "ARBTJBBTAS.h"
#include "ARBTJBBTEL.h"

#endif /* HAVE_BLUETOOTH */

namespace android
{

//#define BLUETOOTH_CLASS_ERROR 0xFF000000
//#define PROPERTIES_NREFS 10

#ifdef HAVE_BLUETOOTH

#define BTPM_SERVICE_NAME   "ss1btpm"
#define BTPM_SERVICE_SOCKET "/data/SS1BTPMS"

#define BTS_DISCOVERY_LENGTH_SECONDS 20

#define HFAG_SERVICE_CHANNEL            10
#define HFAG_SERVICE_NAME               "Voice Gateway"
#define HFAG_SERVICE_VERSION            0x0105
#define HFAG_SERVICE_SUPPORTED_FEATURES 0x0003
#define HFAG_SERVICE_NETWORK_TYPE       0x01

#define HSAG_SERVICE_CHANNEL 11
#define HSAG_SERVICE_NAME    "Voice Gateway"
#define HSAG_SERVICE_VERSION 0x0102

#define OPP_SERVICE_CHANNEL 12
#define OPP_SERVICE_NAME    "OBEX Object Push"

#define PBAP_SERVICE_CHANNEL                19
#define PBAP_SERVICE_NAME                   "OBEX Phonebook Access Server"
#define PBAP_SERVICE_VERSION                0x0100
#define PBAP_SERVICE_SUPPORTED_REPOSITORIES 0x01

static jfieldID Field_mNativeData;
static jfieldID Field_mEventLoop;

static jmethodID Method_isRemoteDeviceInCache;
#if (SS1_PLATFORM_SDK_VERSION < 14)
static jmethodID Method_addRemoteDeviceProperties;
static jmethodID Method_getRemoteDeviceProperty;
#endif

typedef struct _tagBTS_NativeData_t
{
   int             AuthCallbackID;
   int             DEVMCallbackID;
#if (SS1_PLATFORM_SDK_VERSION >= 14)
   int             HIDMCallbackID;
   int             PANMCallbackID;
#endif
   unsigned int    DiscoverableTimeout;
} BTS_NativeData_t;

   /* The following structure is used with the                          */
   /* DelayedCreatePairedResultThread() function which is invoked as a  */
   /* thread from the BTS_CreatePairedDeviceNative() JNI routine.       */
typedef struct _tagDelayedResultData_t
{
   JavaVM       *JVM;
   jint          JNIEnvVersion;
   jobject       EventLoopObject;
   unsigned long Delay;
   jstring       Address;
   int           ResultCode;
} DelayedResultData_t;

   /* The following structure is used with the                          */
   /* DelayedPropertyChangedEventThread() function which is invoked as a*/
   /* thread from the BTS_AddReservedServiceRecordsNative() JNI routine.*/
typedef struct _tagDelayedPropertyData_t
{
   JavaVM       *JVM;
   jint          JNIEnvVersion;
   jobject       EventLoopObject;
   unsigned long Delay;
   jobjectArray  PropertyArray;
} DelayedPropertyData_t;

   /* Internal Function Prototypes.                                     */
static jint BTS_IsEnabledNative(JNIEnv *Env, jobject Object);
static jint BTS_DisableNative(JNIEnv *env, jobject object);
static jboolean BTS_SetAdapterPropertyBooleanNative(JNIEnv *Env, jobject Object, jstring Key, jint Value);

   /* The following function is provided to be called as a              */
   /* thread-main() in order to delay a response to a pairing           */
   /* request. This is necessary because the Java implementation        */
   /* expects this result to come via a callback at some point after    */
   /* BTS_CreatePairedDeviceNative() returns. The parameter passed to   */
   /* the thread should be a pointer to a DelayedResultData_t structure.*/
static void *DelayedCreatePairedResultThread(void *ThreadData)
{
   int                  AttachResult;
   JNIEnv              *Env;
   DelayedResultData_t *ResultData;

   if((ResultData = (DelayedResultData_t *)(ThreadData)) != NULL)
   {
      if(ResultData->JVM)
      {
         if(ResultData->Delay > 0)
            BTPS_Delay(ResultData->Delay);

         if((AttachResult = AttachThreadToJVM(ResultData->JVM, ResultData->JNIEnvVersion, &Env)) >= 0)
         {
            if(ResultData->EventLoopObject)
               BTEL_CallOnCreatePairedDeviceResult(Env, ResultData->EventLoopObject, ResultData->Address, ResultData->ResultCode);

            /* Clean up global references.                              */
            Env->DeleteGlobalRef(ResultData->EventLoopObject);
            Env->DeleteGlobalRef(ResultData->Address);

            if(AttachResult > 0)
               ResultData->JVM->DetachCurrentThread();
         }
         else
            SS1_LOGE("Unable to release references from Bluetooth pairing process, memory leak may occur.");
      }

      free(ResultData);
   }

   return(NULL);
}


   /* The following function is used to provide the result of           */
   /* a pairing request via a Java callback after a specified           */
   /* delay. This is necessary because the Java implementation of       */
   /* BluetoothService expects the result to come at some time after    */
   /* BTS_CreatePairedDeviceNative() returns via a callback into the    */
   /* BluetoothEventLoop object associated with the BluetoothService    */
   /* instance.                                                         */
static bool SendDelayedCreatePairedDeviceResult(JNIEnv *Env, jobject Object, jstring AddressString, int ResultCode, unsigned long DelayMS)
{
   bool                 ret_val;
   jobject              EventLoopObject;
   pthread_t            ResultThread;
   pthread_attr_t       ResultThreadAttr;
   DelayedResultData_t *ResultThreadData;

   ret_val = false;

   if(Env && Object && AddressString)
   {
      if((EventLoopObject = Env->GetObjectField(Object, Field_mEventLoop)) != NULL)
      {
         if(pthread_attr_init(&ResultThreadAttr) == 0)
         {
            if(pthread_attr_setdetachstate(&ResultThreadAttr, PTHREAD_CREATE_DETACHED) == 0)
            {
               if((ResultThreadData = (DelayedResultData_t *)malloc(sizeof(DelayedResultData_t))) != NULL)
               {
                  Env->GetJavaVM(&(ResultThreadData->JVM));
                  ResultThreadData->JNIEnvVersion   = Env->GetVersion();
                  ResultThreadData->EventLoopObject = Env->NewGlobalRef(EventLoopObject);
                  ResultThreadData->Delay           = ((DelayMS >= 500) ? DelayMS : 500);
                  ResultThreadData->Address         = (jstring)(Env->NewGlobalRef(AddressString));
                  ResultThreadData->ResultCode      = ResultCode;

                  if(pthread_create(&ResultThread, &ResultThreadAttr, DelayedCreatePairedResultThread, ResultThreadData) == 0)
                  {
                     ret_val = true;
                  }
                  else
                  {
                     /* Thread creation failed. Clean up the global     */
                     /* references we created.                          */
                     Env->DeleteGlobalRef(ResultThreadData->EventLoopObject);
                     Env->DeleteGlobalRef(ResultThreadData->Address);
                     free(ResultThreadData);
                  }
               }
            }

            pthread_attr_destroy(&ResultThreadAttr);
         }

         Env->DeleteLocalRef(EventLoopObject);
      }
   }

   return(ret_val);
}


   /* The following function is provided to be called as a              */
   /* thread-main() in order to delay a response to a pairing           */
   /* request. This is necessary because the Java implementation        */
   /* expects this result to come via a callback at some point after    */
   /* BTS_CreatePairedDeviceNative() returns. The parameter passed to   */
   /* the thread should be a pointer to a DelayedResultData_t structure.*/
static void *DelayedConnectInputDeviceResultThread(void *ThreadData)
{
   int                  AttachResult;
   JNIEnv              *Env;
   DelayedResultData_t *ResultData;

   if((ResultData = (DelayedResultData_t *)(ThreadData)) != NULL)
   {
      if(ResultData->JVM)
      {
         if(ResultData->Delay > 0)
            BTPS_Delay(ResultData->Delay);

         if((AttachResult = AttachThreadToJVM(ResultData->JVM, ResultData->JNIEnvVersion, &Env)) >= 0)
         {
            if(ResultData->EventLoopObject)
               BTEL_CallOnInputDeviceConnectionResult(Env, ResultData->EventLoopObject, ResultData->Address, ResultData->ResultCode);

            /* Clean up global references.                              */
            Env->DeleteGlobalRef(ResultData->EventLoopObject);
            Env->DeleteGlobalRef(ResultData->Address);

            if(AttachResult > 0)
               ResultData->JVM->DetachCurrentThread();
         }
         else
            SS1_LOGE("Unable to release references from Bluetooth pairing process, memory leak may occur.");
      }

      free(ResultData);
   }

   return(NULL);
}


   /* The following function is used to provide the result of a         */
   /* HID connection request via a Java callback after a specified      */
   /* delay. This is necessary because the Java implementation of       */
   /* BluetoothService expects the result to come at some time after    */
   /* BTS_ConnectInputDeviceNative() returns via a callback into the    */
   /* BluetoothEventLoop object associated with the BluetoothService    */
   /* instance.                                                         */
static bool SendDelayedConnectInputDeviceResult(JNIEnv *Env, jobject Object, jstring AddressString, int ResultCode, unsigned long DelayMS)
{
   bool                 ret_val;
   jobject              EventLoopObject;
   pthread_t            ResultThread;
   pthread_attr_t       ResultThreadAttr;
   DelayedResultData_t *ResultThreadData;

   ret_val = false;

   if(Env && Object && AddressString)
   {
      if((EventLoopObject = Env->GetObjectField(Object, Field_mEventLoop)) != NULL)
      {
         if(pthread_attr_init(&ResultThreadAttr) == 0)
         {
            if(pthread_attr_setdetachstate(&ResultThreadAttr, PTHREAD_CREATE_DETACHED) == 0)
            {
               if((ResultThreadData = (DelayedResultData_t *)malloc(sizeof(DelayedResultData_t))) != NULL)
               {
                  Env->GetJavaVM(&(ResultThreadData->JVM));
                  ResultThreadData->JNIEnvVersion   = Env->GetVersion();
                  ResultThreadData->EventLoopObject = Env->NewGlobalRef(EventLoopObject);
                  ResultThreadData->Delay           = ((DelayMS >= 500) ? DelayMS : 500);
                  ResultThreadData->Address         = (jstring)(Env->NewGlobalRef(AddressString));
                  ResultThreadData->ResultCode      = ResultCode;

                  if(pthread_create(&ResultThread, &ResultThreadAttr, DelayedConnectInputDeviceResultThread, ResultThreadData) == 0)
                  {
                     ret_val = true;
                  }
                  else
                  {
                     /* Thread creation failed. Clean up the global     */
                     /* references we created.                          */
                     Env->DeleteGlobalRef(ResultThreadData->EventLoopObject);
                     Env->DeleteGlobalRef(ResultThreadData->Address);
                     free(ResultThreadData);
                  }
               }
            }

            pthread_attr_destroy(&ResultThreadAttr);
         }

         Env->DeleteLocalRef(EventLoopObject);
      }
   }

   return(ret_val);
}


   /* The following function is provided to be called as a thread-main()*/
   /* in order to delay a response to a PAN connection request.         */
   /* This is necessary because the Java implementation expects         */
   /* this result to come via a callback at some point after            */
   /* BTS_ConnectPanDeviceNative() returns. The parameter passed to the */
   /* thread should be a pointer to a DelayedResultData_t structure.    */
static void *DelayedConnectPanDeviceResultThread(void *ThreadData)
{
   int                  AttachResult;
   JNIEnv              *Env;
   DelayedResultData_t *ResultData;

   if((ResultData = (DelayedResultData_t *)(ThreadData)) != NULL)
   {
      if(ResultData->JVM)
      {
         if(ResultData->Delay > 0)
            BTPS_Delay(ResultData->Delay);

         if((AttachResult = AttachThreadToJVM(ResultData->JVM, ResultData->JNIEnvVersion, &Env)) >= 0)
         {
            if(ResultData->EventLoopObject)
               BTEL_CallOnPanDeviceConnectionResult(Env, ResultData->EventLoopObject, ResultData->Address, ResultData->ResultCode);

            /* Clean up global references.                              */
            Env->DeleteGlobalRef(ResultData->EventLoopObject);
            Env->DeleteGlobalRef(ResultData->Address);

            if(AttachResult > 0)
               ResultData->JVM->DetachCurrentThread();
         }
         else
            SS1_LOGE("Unable to release references from Bluetooth pairing process, memory leak may occur.");
      }

      free(ResultData);
   }

   return(NULL);
}


   /* The following function is used to provide the result of a         */
   /* PAN connection request via a Java callback after a specified      */
   /* delay. This is necessary because the Java implementation of       */
   /* BluetoothService expects the result to come at some time after    */
   /* BTS_ConnectPanDeviceNative() returns via a callback into the      */
   /* BluetoothEventLoop object associated with the BluetoothService    */
   /* instance.                                                         */
static bool SendDelayedConnectPanDeviceResult(JNIEnv *Env, jobject Object, jstring AddressString, int ResultCode, unsigned long DelayMS)
{
   bool                 ret_val;
   jobject              EventLoopObject;
   pthread_t            ResultThread;
   pthread_attr_t       ResultThreadAttr;
   DelayedResultData_t *ResultThreadData;

   ret_val = false;

   if(Env && Object && AddressString)
   {
      if((EventLoopObject = Env->GetObjectField(Object, Field_mEventLoop)) != NULL)
      {
         if(pthread_attr_init(&ResultThreadAttr) == 0)
         {
            if(pthread_attr_setdetachstate(&ResultThreadAttr, PTHREAD_CREATE_DETACHED) == 0)
            {
               if((ResultThreadData = (DelayedResultData_t *)malloc(sizeof(DelayedResultData_t))) != NULL)
               {
                  Env->GetJavaVM(&(ResultThreadData->JVM));
                  ResultThreadData->JNIEnvVersion   = Env->GetVersion();
                  ResultThreadData->EventLoopObject = Env->NewGlobalRef(EventLoopObject);
                  ResultThreadData->Delay           = ((DelayMS >= 500) ? DelayMS : 500);
                  ResultThreadData->Address         = (jstring)(Env->NewGlobalRef(AddressString));
                  ResultThreadData->ResultCode      = ResultCode;

                  if(pthread_create(&ResultThread, &ResultThreadAttr, DelayedConnectPanDeviceResultThread, ResultThreadData) == 0)
                  {
                     ret_val = true;
                  }
                  else
                  {
                     /* Thread creation failed. Clean up the global     */
                     /* references we created.                          */
                     Env->DeleteGlobalRef(ResultThreadData->EventLoopObject);
                     Env->DeleteGlobalRef(ResultThreadData->Address);
                     free(ResultThreadData);
                  }
               }
            }

            pthread_attr_destroy(&ResultThreadAttr);
         }

         Env->DeleteLocalRef(EventLoopObject);
      }
   }

   return(ret_val);
}


   /* The following function is provided to be called as a              */
   /* thread-main() in order to delay a response to a pairing           */
   /* request. This is necessary because the Java implementation        */
   /* expects this result to come via a callback at some point after    */
   /* BTS_CreatePairedDeviceNative() returns. The parameter passed to   */
   /* the thread should be a pointer to a DelayedResultData_t structure.*/
static void *DelayedPropertyChangedEventThread(void *ThreadData)
{
   int                    AttachResult;
   JNIEnv                *Env;
   DelayedPropertyData_t *ResultData;

   if((ResultData = (DelayedPropertyData_t *)(ThreadData)) != NULL)
   {
      if(ResultData->JVM)
      {
         if(ResultData->Delay > 0)
            BTPS_Delay(ResultData->Delay);

         if((AttachResult = AttachThreadToJVM(ResultData->JVM, ResultData->JNIEnvVersion, &Env)) >= 0)
         {
            if(ResultData->EventLoopObject)
               BTEL_CallOnPropertyChanged(Env, ResultData->EventLoopObject, ResultData->PropertyArray);

            /* Clean up global references.                              */
            Env->DeleteGlobalRef(ResultData->EventLoopObject);
            Env->DeleteGlobalRef(ResultData->PropertyArray);

            if(AttachResult > 0)
               ResultData->JVM->DetachCurrentThread();
         }
         else
            SS1_LOGE("Unable to release references from Bluetooth pairing process, memory leak may occur.");
      }

      free(ResultData);
   }

   return(NULL);
}


   /* The following function is used to provide the result of a         */
   /* HID connection request via a Java callback after a specified      */
   /* delay. This is necessary because the Java implementation of       */
   /* BluetoothService expects the result to come at some time after    */
   /* BTS_ConnectInputDeviceNative() returns via a callback into the    */
   /* BluetoothEventLoop object associated with the BluetoothService    */
   /* instance.                                                         */
static bool SendDelayedPropertyChangedEvent(JNIEnv *Env, jobject Object, jobjectArray PropertyArray, unsigned long DelayMS)
{
   bool                   ret_val;
   jobject                EventLoopObject;
   pthread_t              PropertyChangedThread;
   pthread_attr_t         PropertyChangedThreadAttr;
   DelayedPropertyData_t *PropertyData;

   ret_val = false;

   if(Env && Object && PropertyArray)
   {
      if((EventLoopObject = Env->GetObjectField(Object, Field_mEventLoop)) != NULL)
      {
         if(pthread_attr_init(&PropertyChangedThreadAttr) == 0)
         {
            if(pthread_attr_setdetachstate(&PropertyChangedThreadAttr, PTHREAD_CREATE_DETACHED) == 0)
            {
               if((PropertyData = (DelayedPropertyData_t *)malloc(sizeof(DelayedPropertyData_t))) != NULL)
               {
                  Env->GetJavaVM(&(PropertyData->JVM));
                  PropertyData->JNIEnvVersion   = Env->GetVersion();
                  PropertyData->EventLoopObject = Env->NewGlobalRef(EventLoopObject);
                  PropertyData->Delay           = ((DelayMS >= 500) ? DelayMS : 500);
                  PropertyData->PropertyArray   = (jobjectArray)(Env->NewGlobalRef(PropertyArray));

                  if(pthread_create(&PropertyChangedThread, &PropertyChangedThreadAttr, DelayedPropertyChangedEventThread, PropertyData) == 0)
                  {
                     ret_val = true;
                  }
                  else
                  {
                     /* Thread creation failed. Clean up the global     */
                     /* references we created.                          */
                     Env->DeleteGlobalRef(PropertyData->EventLoopObject);
                     Env->DeleteGlobalRef(PropertyData->PropertyArray);
                     free(PropertyData);
                  }
               }
            }

            pthread_attr_destroy(&PropertyChangedThreadAttr);
         }

         Env->DeleteLocalRef(EventLoopObject);
      }
   }

   return(ret_val);
}


static bool SendDelayedPropertyChangedEvent(JNIEnv *Env, jobject Object, const char *PropertyKey, const char *PropertyValue, unsigned long DelayMS)
{
   bool         ret_val;
   jobjectArray PropStringArray;

   if((PropStringArray = Env->NewObjectArray(2, Env->FindClass("java/lang/String"), NULL)) != NULL)
   {
      SetStringArrayElement(Env, PropStringArray, 0, PropertyKey);
      SetStringArrayElement(Env, PropStringArray, 1, PropertyValue);

      ret_val = SendDelayedPropertyChangedEvent(Env, Object, PropStringArray, 100L);

      Env->DeleteLocalRef(PropStringArray);
   }
   else
      ret_val = false;

   return(ret_val);
}


static bool SendDelayedPropertyChangedEvent(JNIEnv *Env, jobject Object, const char *PropertyKey, unsigned int PropertyValue, unsigned long DelayMS)
{
   bool         ret_val;
   char         PropertyValueString[16];
   jobjectArray PropStringArray;

   if((PropStringArray = Env->NewObjectArray(2, Env->FindClass("java/lang/String"), NULL)) != NULL)
   {
      memset(PropertyValueString, 0, sizeof(PropertyValueString));
      snprintf(PropertyValueString, sizeof(PropertyValueString), "%u", PropertyValue);

      SetStringArrayElement(Env, PropStringArray, 0, PropertyKey);
      SetStringArrayElement(Env, PropStringArray, 1, PropertyValueString);

      ret_val = SendDelayedPropertyChangedEvent(Env, Object, PropStringArray, 100L);

      Env->DeleteLocalRef(PropStringArray);
   }
   else
      ret_val = false;

   return(ret_val);
}


int RegisterSDPRecordForSPP(SPP_SDP_Service_Record_t *SDPServiceRecord, unsigned int ServiceChannel, const char *ServiceName, unsigned int *SDPServiceRecordHandle)
{
   int                 ret_val;
   long                RecordHandle;
   unsigned int        Index;
   unsigned int        tmpCount;
   unsigned int        NumberUUIDEntries;
   SDP_UUID_Entry_t   *SDPUUIDEntries;
   SDP_UUID_Entry_t    SDPUUIDEntries_Default;
   SDP_Data_Element_t *SDP_Data_Element;
   SDP_Data_Element_t  SDP_Data_Element_Main;
   SDP_Data_Element_t  SDP_Data_Element_L2CAP;
   SDP_Data_Element_t  SDP_Data_Element_RFCOMM[2];
   SDP_Data_Element_t  SDP_Data_Element_Language[4];

   /* Make sure that the input parameters that were passed to us are    */
   /* semi-valid.                                                       */
   if((ServiceChannel) && (ServiceName) && (BTPS_StringLength(ServiceName)) && (SDPServiceRecordHandle))
   {
      /* Now let's generate a generic SPP SDP Record and publish it.    */

      /* Now check to see if the caller specified any information for   */
      /* the Service Class.                                             */
      if((SDPServiceRecord) && (SDPServiceRecord->NumberServiceClassUUID))
      {
         if(SDPServiceRecord->SDPUUIDEntries)
         {
            NumberUUIDEntries = SDPServiceRecord->NumberServiceClassUUID;
            SDPUUIDEntries    = SDPServiceRecord->SDPUUIDEntries;
         }
         else
            NumberUUIDEntries = 0;
      }
      else
      {
         /* Initialize the Default SPP UUID Entry Information.          */
         NumberUUIDEntries = 1;
         SDPUUIDEntries    = &SDPUUIDEntries_Default;

         /* Initialize the Serial Port Profile.                         */
         SDPUUIDEntries_Default.SDP_Data_Element_Type = deUUID_16;
         SDP_ASSIGN_SERIAL_PORT_PROFILE_UUID_16(SDPUUIDEntries_Default.UUID_Value.UUID_16);
      }

      /* Only continue if there were Service Classes specified.         */
      if(NumberUUIDEntries)
      {
         if((RecordHandle = DEVM_CreateServiceRecord(FALSE, NumberUUIDEntries, SDPUUIDEntries)) > 0)
         {
            /* Note the Service Record Handle.                          */
            *SDPServiceRecordHandle = (unsigned int)RecordHandle;

            /* Calculate how many extra Protocol List Entries the User  */
            /* has requested to be added to the Protocol List.          */
            if((SDPServiceRecord) && (SDPServiceRecord->ProtocolList))
            {
               if(SDPServiceRecord->ProtocolList[0].SDP_Data_Element_Type == deSequence)
                  tmpCount = 2 + SDPServiceRecord->ProtocolList[0].SDP_Data_Element_Length;
               else
                  tmpCount = 0;
            }
            else
               tmpCount = 2;

            /* Make sure that an error hasn't occurred.                 */
            if(tmpCount)
            {
               /* Now allocate enough space to hold the SDP Data Element*/
               /* Sequence List.                                        */
               if((SDP_Data_Element = (SDP_Data_Element_t *)BTPS_AllocateMemory(tmpCount*SDP_DATA_ELEMENT_SIZE)) != NULL)
               {
                  SDP_Data_Element[0].SDP_Data_Element_Type                      = deSequence;
                  SDP_Data_Element[0].SDP_Data_Element_Length                    = 1;
                  SDP_Data_Element[0].SDP_Data_Element.SDP_Data_Element_Sequence = &SDP_Data_Element_L2CAP;

                  SDP_Data_Element_L2CAP.SDP_Data_Element_Type                   = deUUID_16;
                  SDP_Data_Element_L2CAP.SDP_Data_Element_Length                 = sizeof(SDP_Data_Element_L2CAP.SDP_Data_Element.UUID_16);
                  SDP_ASSIGN_L2CAP_UUID_16(SDP_Data_Element_L2CAP.SDP_Data_Element.UUID_16);

                  SDP_Data_Element[1].SDP_Data_Element_Type                      = deSequence;
                  SDP_Data_Element[1].SDP_Data_Element_Length                    = 2;
                  SDP_Data_Element[1].SDP_Data_Element.SDP_Data_Element_Sequence = SDP_Data_Element_RFCOMM;

                  SDP_Data_Element_RFCOMM[0].SDP_Data_Element_Type                 = deUUID_16;
                  SDP_Data_Element_RFCOMM[0].SDP_Data_Element_Length               = UUID_16_SIZE;
                  SDP_ASSIGN_RFCOMM_UUID_16(SDP_Data_Element_RFCOMM[0].SDP_Data_Element.UUID_16);

                  SDP_Data_Element_RFCOMM[1].SDP_Data_Element_Type                 = deUnsignedInteger1Byte;
                  SDP_Data_Element_RFCOMM[1].SDP_Data_Element_Length               = sizeof(SDP_Data_Element_RFCOMM[1].SDP_Data_Element.UnsignedInteger1Byte);
                  SDP_Data_Element_RFCOMM[1].SDP_Data_Element.UnsignedInteger1Byte = (Byte_t)ServiceChannel;

                  /* Now let's add any Information that the caller has  */
                  /* specified.                                         */
                  for(Index=0; Index<(tmpCount-2); Index++)
                     SDP_Data_Element[2+Index] = SDPServiceRecord->ProtocolList[0].SDP_Data_Element.SDP_Data_Element_Sequence[Index];

                  /* Format the Main SDP Data Element Sequence.         */
                  SDP_Data_Element_Main.SDP_Data_Element_Type                      = deSequence;
                  SDP_Data_Element_Main.SDP_Data_Element_Length                    = tmpCount;
                  SDP_Data_Element_Main.SDP_Data_Element.SDP_Data_Element_Sequence = SDP_Data_Element;

                  /* Now let's add all necessary Attributes.            */
                  ret_val = DEVM_AddServiceRecordAttribute(*SDPServiceRecordHandle, SDP_ATTRIBUTE_ID_PROTOCOL_DESCRIPTOR_LIST, &SDP_Data_Element_Main);

                  /* Free any memory that was allocated.                */
                  BTPS_FreeMemory(SDP_Data_Element);

                  /* Now we need to add the name.                       */
                  if(!ret_val)
                  {
                     /* Add a default Language Attribute ID List Entry  */
                     /* for UTF-8 English.                              */
                     SDP_Data_Element_Language[0].SDP_Data_Element_Type                      = deSequence;
                     SDP_Data_Element_Language[0].SDP_Data_Element_Length                    = 3;
                     SDP_Data_Element_Language[0].SDP_Data_Element.SDP_Data_Element_Sequence = &SDP_Data_Element_Language[1];
                     SDP_Data_Element_Language[1].SDP_Data_Element_Type                      = deUnsignedInteger2Bytes;
                     SDP_Data_Element_Language[1].SDP_Data_Element_Length                    = SDP_UNSIGNED_INTEGER_2_BYTES_SIZE;
                     SDP_Data_Element_Language[1].SDP_Data_Element.UnsignedInteger2Bytes     = SDP_NATURAL_LANGUAGE_ENGLISH_UTF_8;
                     SDP_Data_Element_Language[2].SDP_Data_Element_Type                      = deUnsignedInteger2Bytes;
                     SDP_Data_Element_Language[2].SDP_Data_Element_Length                    = SDP_UNSIGNED_INTEGER_2_BYTES_SIZE;
                     SDP_Data_Element_Language[2].SDP_Data_Element.UnsignedInteger2Bytes     = SDP_UTF_8_CHARACTER_ENCODING;
                     SDP_Data_Element_Language[3].SDP_Data_Element_Type                      = deUnsignedInteger2Bytes;
                     SDP_Data_Element_Language[3].SDP_Data_Element_Length                    = SDP_UNSIGNED_INTEGER_2_BYTES_SIZE;
                     SDP_Data_Element_Language[3].SDP_Data_Element.UnsignedInteger2Bytes     = SDP_DEFAULT_LANGUAGE_BASE_ATTRIBUTE_ID;

                     ret_val = DEVM_AddServiceRecordAttribute(*SDPServiceRecordHandle, SDP_ATTRIBUTE_ID_LANGUAGE_BASE_ATTRIBUTE_ID_LIST, SDP_Data_Element_Language);

                     /* Finally Add the Service Name to the SDP         */
                     /* Database.                                       */
                     if(!ret_val)
                     {
                        SDP_Data_Element_Main.SDP_Data_Element_Type       = deTextString;
                        SDP_Data_Element_Main.SDP_Data_Element_Length     = BTPS_StringLength(ServiceName);
                        SDP_Data_Element_Main.SDP_Data_Element.TextString = (Byte_t *)ServiceName;

                        ret_val = DEVM_AddServiceRecordAttribute(*SDPServiceRecordHandle, (SDP_DEFAULT_LANGUAGE_BASE_ATTRIBUTE_ID + SDP_ATTRIBUTE_OFFSET_ID_SERVICE_NAME), &SDP_Data_Element_Main);
                     }
                  }
               }
               else
                  ret_val = BTPS_ERROR_MEMORY_ALLOCATION_ERROR;
            }
            else
               ret_val = BTPS_ERROR_INVALID_PARAMETER;

            /* If an error condition occurred, we need to make sure we  */
            /* delete the Service Record Entry from the SDP Database.   */
            if(ret_val < 0)
               DEVM_DeleteServiceRecord(*SDPServiceRecordHandle);
         }
         else
            ret_val = (int)RecordHandle;
      }
      else
         ret_val = BTPS_ERROR_INVALID_PARAMETER;
   }
   else
      ret_val = BTPS_ERROR_INVALID_PARAMETER;

   return(ret_val);
}


int BTPSAPI RegisterSDPRecordForGOEP(GOEP_SDP_Service_Record_t *SDPServiceRecord, unsigned int SPPChannel, const char *ServiceName, unsigned int *SDPServiceRecordHandle)
{
   int                       ret_val;
   unsigned int              Index;
   unsigned int              tmpCount;
   SDP_Data_Element_t       *SDP_Data_Element;
   SDP_Data_Element_t        SDP_Data_Element_Main;
   SDP_Data_Element_t        SDP_Data_Element_OBEX;
   SPP_SDP_Service_Record_t  SPP_SDP_Service_Record;

   /* Make sure that the input parameters that were passed to us are    */
   /* semi-valid.                                                       */
   if((SPPChannel) && (ServiceName) && (BTPS_StringLength(ServiceName)) && (SDPServiceRecord) && (SDPServiceRecordHandle))
   {
      /* Now let's register a generic OBEX SDP Record.                  */

      /* Now check to see if the caller specified information for the   */
      /* Service Class.                                                 */
      if((SDPServiceRecord) && (SDPServiceRecord->NumberServiceClassUUID))
      {
         /* UUID Service Record Information was specified, so let's add */
         /* the OBEX SDP Record to the SDP Database letting SPP do most */
         /* of the grunt work for us.                                   */

         /* Calculate how many extra Protocol List Entries the User has */
         /* requested to be added to the Protocol List.                 */
         if((SDPServiceRecord) && (SDPServiceRecord->ProtocolList))
         {
            if(SDPServiceRecord->ProtocolList[0].SDP_Data_Element_Type == deSequence)
               tmpCount = 1 + SDPServiceRecord->ProtocolList[0].SDP_Data_Element_Length;
            else
               tmpCount = 0;
         }
         else
            tmpCount = 1;

         /* Make sure that an error hasn't occurred.                    */
         if(tmpCount)
         {
            /* Now allocate enough space to hold the SDP Data Element   */
            /* Sequence List.                                           */
            if((SDP_Data_Element = (SDP_Data_Element_t *)BTPS_AllocateMemory(tmpCount*SDP_DATA_ELEMENT_SIZE)) != NULL)
            {
               SDP_Data_Element[0].SDP_Data_Element_Type                      = deSequence;
               SDP_Data_Element[0].SDP_Data_Element_Length                    = 1;
               SDP_Data_Element[0].SDP_Data_Element.SDP_Data_Element_Sequence = &SDP_Data_Element_OBEX;

               SDP_Data_Element_OBEX.SDP_Data_Element_Type                    = deUUID_16;
               SDP_Data_Element_OBEX.SDP_Data_Element_Length                  = sizeof(SDP_Data_Element_OBEX.SDP_Data_Element.UUID_16);
               SDP_ASSIGN_OBEX_UUID_16(SDP_Data_Element_OBEX.SDP_Data_Element.UUID_16);

               /* Now let's add any Information that the caller has     */
               /* specified.                                            */

               for(Index=0; Index<(tmpCount-1); Index++)
                  SDP_Data_Element[1+Index] = SDPServiceRecord->ProtocolList[0].SDP_Data_Element.SDP_Data_Element_Sequence[Index];

               /* Format the Main SDP Data Element Sequence.            */
               SDP_Data_Element_Main.SDP_Data_Element_Type                      = deSequence;
               SDP_Data_Element_Main.SDP_Data_Element_Length                    = tmpCount;
               SDP_Data_Element_Main.SDP_Data_Element.SDP_Data_Element_Sequence = SDP_Data_Element;

               /* Finally build an SPP SDP Service Record with the new  */
               /* data and call the SPP SDP Register Function.          */
               SPP_SDP_Service_Record.NumberServiceClassUUID = SDPServiceRecord->NumberServiceClassUUID;
               SPP_SDP_Service_Record.SDPUUIDEntries         = SDPServiceRecord->SDPUUIDEntries;
               SPP_SDP_Service_Record.ProtocolList           = &SDP_Data_Element_Main;

               ret_val = RegisterSDPRecordForSPP(&SPP_SDP_Service_Record, SPPChannel, ServiceName, SDPServiceRecordHandle);

               /* Free any memory that was allocated.                   */
               BTPS_FreeMemory(SDP_Data_Element);
            }
            else
               ret_val = BTPS_ERROR_MEMORY_ALLOCATION_ERROR;
         }
         else
            ret_val = BTPS_ERROR_INVALID_PARAMETER;
      }
      else
         ret_val = BTPS_ERROR_INVALID_PARAMETER;
   }
   else
      ret_val = BTPS_ERROR_INVALID_PARAMETER;

   return(ret_val);
}


static int RegisterSDPRecordForHeadset(unsigned int *SDPServiceRecordHandlePtr)
{
   int                       ret_val;
   unsigned int              SDPServiceRecordHandle;
   SDP_UUID_Entry_t          SDPUUIDEntries[2];
   SDP_Data_Element_t        SDP_Data_Element_Main;
   SDP_Data_Element_t        SDP_Data_Element_List[4];
   SPP_SDP_Service_Record_t  SPP_SDP_Service_Record;

   /* Now let's Add a generic SPP SDP Record to the SDP Database.       */

   /* Initialize the Headset Service Class.                             */
   SDPUUIDEntries[0].SDP_Data_Element_Type = deUUID_16;
   ASSIGN_SDP_UUID_16(SDPUUIDEntries[0].UUID_Value.UUID_16, 0x11, 0x12);

   /* Next, initialize the Generic Audio Service Class.                 */
   SDPUUIDEntries[1].SDP_Data_Element_Type = deUUID_16;
   ASSIGN_SDP_UUID_16(SDPUUIDEntries[1].UUID_Value.UUID_16, 0X12, 0X03);

   SPP_SDP_Service_Record.NumberServiceClassUUID = 2;
   SPP_SDP_Service_Record.SDPUUIDEntries         = SDPUUIDEntries;
   SPP_SDP_Service_Record.ProtocolList           = NULL;

   /* Create the service record using SPP as a template.                */
   if((ret_val = RegisterSDPRecordForSPP(&SPP_SDP_Service_Record, HSAG_SERVICE_CHANNEL, HSAG_SERVICE_NAME, &SDPServiceRecordHandle)) == 0)
   {
      /* Now let's add the Optional Profile Descriptor List Attribute to*/
      /* the SDP Database Record.                                       */

      /* The Profile Descriptor List is a Data Element Sequence. Each   */
      /* Entry in the Data Element Sequence is, in fact, another Data   */
      /* Element Sequence. For the Headset it contains two items, the   */
      /* first is the Headset UUID, and the second is the Profile       */
      /* Version.                                                       */
      SDP_Data_Element_Main.SDP_Data_Element_Type                         = deSequence;
      SDP_Data_Element_Main.SDP_Data_Element_Length                       = 1;
      SDP_Data_Element_Main.SDP_Data_Element.SDP_Data_Element_Sequence    = SDP_Data_Element_List;

      SDP_Data_Element_List[0].SDP_Data_Element_Type                      = deSequence;
      SDP_Data_Element_List[0].SDP_Data_Element_Length                    = 2;
      SDP_Data_Element_List[0].SDP_Data_Element.SDP_Data_Element_Sequence = &(SDP_Data_Element_List[1]);

      /* The first element in the Data Element Sequence is the Headset  */
      /* Profile UUID.                                                  */
      SDP_Data_Element_List[1].SDP_Data_Element_Type                      = deUUID_16;
      SDP_Data_Element_List[1].SDP_Data_Element_Length                    = UUID_16_SIZE;
      ASSIGN_SDP_UUID_16(SDP_Data_Element_List[1].SDP_Data_Element.UUID_16, 0x11, 0x08);

      /* The second element in the Data Element Sequence is the Version */
      /* Number value (UINT 16).                                        */
      SDP_Data_Element_List[2].SDP_Data_Element_Type                      = deUnsignedInteger2Bytes;
      SDP_Data_Element_List[2].SDP_Data_Element_Length                    = WORD_SIZE;
      SDP_Data_Element_List[2].SDP_Data_Element.UnsignedInteger2Bytes     = HSAG_SERVICE_VERSION;

      /* Now let's add the Attribute.                                   */
      if((ret_val = DEVM_AddServiceRecordAttribute(SDPServiceRecordHandle, SDP_ATTRIBUTE_ID_BLUETOOTH_PROFILE_DESCRIPTOR_LIST, &SDP_Data_Element_Main)) == 0)
      {
         if(SDPServiceRecordHandlePtr)
            *SDPServiceRecordHandlePtr = SDPServiceRecordHandle;
      }
      else
         DEVM_DeleteServiceRecord(SDPServiceRecordHandle);
   }

   /* Return the result to the caller.                                  */
   return(ret_val);
}


static int RegisterSDPRecordForHandsFree(unsigned int *SDPServiceRecordHandlePtr)
{
   int                       ret_val;
   unsigned int              SDPServiceRecordHandle;
   SDP_UUID_Entry_t          SDPUUIDEntries[2];
   SDP_Data_Element_t        SDP_Data_Element_Main;
   SDP_Data_Element_t        SDP_Data_Element_List[4];
   SPP_SDP_Service_Record_t  SPP_SDP_Service_Record;

   /* Initialize the Hands-Free Service Class.                          */
   SDPUUIDEntries[0].SDP_Data_Element_Type = deUUID_16;
   ASSIGN_SDP_UUID_16(SDPUUIDEntries[0].UUID_Value.UUID_16, 0x11, 0x1F);

   /* Next, initialize the Generic Audio Service Class.                 */
   SDPUUIDEntries[1].SDP_Data_Element_Type = deUUID_16;
   ASSIGN_SDP_UUID_16(SDPUUIDEntries[1].UUID_Value.UUID_16, 0x12, 0x03);

   SPP_SDP_Service_Record.NumberServiceClassUUID = 2;
   SPP_SDP_Service_Record.SDPUUIDEntries         = SDPUUIDEntries;
   SPP_SDP_Service_Record.ProtocolList           = NULL;

   /* Create the service record using SPP as a template.                */
   if((ret_val = RegisterSDPRecordForSPP(&SPP_SDP_Service_Record, HFAG_SERVICE_CHANNEL, HFAG_SERVICE_NAME, &SDPServiceRecordHandle)) == 0)
   {
      /* Now let's add the Optional Profile Descriptor List Attribute to*/
      /* the SDP Database Record.                                       */

      /* The Profile Descriptor List is a Data Element Sequence. Each   */
      /* Entry in the Data Element Sequence is, in fact, another Data   */
      /* Element Sequence. For the Hands-Free it contains two items, the*/
      /* first is the Hands-Free UUID, and the second is the Profile    */
      /* Version.                                                       */
      SDP_Data_Element_Main.SDP_Data_Element_Type                         = deSequence;
      SDP_Data_Element_Main.SDP_Data_Element_Length                       = 1;
      SDP_Data_Element_Main.SDP_Data_Element.SDP_Data_Element_Sequence    = SDP_Data_Element_List;

      SDP_Data_Element_List[0].SDP_Data_Element_Type                      = deSequence;
      SDP_Data_Element_List[0].SDP_Data_Element_Length                    = 2;
      SDP_Data_Element_List[0].SDP_Data_Element.SDP_Data_Element_Sequence = &(SDP_Data_Element_List[1]);

      /* The first element in the Data Element Sequence is the          */
      /* Hands-Free Profile UUID.                                       */
      SDP_Data_Element_List[1].SDP_Data_Element_Type                      = deUUID_16;
      SDP_Data_Element_List[1].SDP_Data_Element_Length                    = UUID_16_SIZE;
      ASSIGN_SDP_UUID_16(SDP_Data_Element_List[1].SDP_Data_Element.UUID_16, 0x11, 0x1E);

      /* The second element in the Data Element Sequence is the Version */
      /* Number value (UINT 16).                                        */
      SDP_Data_Element_List[2].SDP_Data_Element_Type                      = deUnsignedInteger2Bytes;
      SDP_Data_Element_List[2].SDP_Data_Element_Length                    = WORD_SIZE;
      SDP_Data_Element_List[2].SDP_Data_Element.UnsignedInteger2Bytes     = HFAG_SERVICE_VERSION;

      /* Now let's add the Attribute                                    */
      if((ret_val = DEVM_AddServiceRecordAttribute(SDPServiceRecordHandle, SDP_ATTRIBUTE_ID_BLUETOOTH_PROFILE_DESCRIPTOR_LIST, &SDP_Data_Element_Main)) == 0)
      {
         /* Now let's add the Network Attribute.                        */
         SDP_Data_Element_Main.SDP_Data_Element_Type                 = deUnsignedInteger1Byte;
         SDP_Data_Element_Main.SDP_Data_Element_Length               = BYTE_SIZE;
         SDP_Data_Element_Main.SDP_Data_Element.UnsignedInteger1Byte = (Byte_t)HFAG_SERVICE_NETWORK_TYPE;

         /* Now let's add the Attribute                                 */
         if((ret_val = DEVM_AddServiceRecordAttribute(SDPServiceRecordHandle, SDP_ATTRIBUTE_ID_NETWORK, &SDP_Data_Element_Main)) == 0)
         {
            /* Now let's add the Supported Features Attribute.          */
            SDP_Data_Element_Main.SDP_Data_Element_Type                  = deUnsignedInteger2Bytes;
            SDP_Data_Element_Main.SDP_Data_Element_Length                = WORD_SIZE;
            SDP_Data_Element_Main.SDP_Data_Element.UnsignedInteger2Bytes = (Word_t)HFAG_SERVICE_SUPPORTED_FEATURES;

            if((ret_val = DEVM_AddServiceRecordAttribute(SDPServiceRecordHandle, SDP_ATTRIBUTE_ID_SUPPORTED_FEATURES, &SDP_Data_Element_Main)) >= 0)
            {
               if(SDPServiceRecordHandlePtr)
                  *SDPServiceRecordHandlePtr = SDPServiceRecordHandle;
            }
            else
               DEVM_DeleteServiceRecord(SDPServiceRecordHandle);
         }
         else
            DEVM_DeleteServiceRecord(SDPServiceRecordHandle);
      }
      else
         DEVM_DeleteServiceRecord(SDPServiceRecordHandle);
   }

   /* Return the result to the caller.                                  */
   return(ret_val);
}


static int RegisterSDPRecordForOPP(unsigned int *SDPServiceRecordHandlePtr)
{
   int                        ret_val;
   unsigned int               Index;
   unsigned int               NumberSupportedFormats;
   unsigned int               SDPServiceRecordHandle;
   SDP_UUID_Entry_t           SDPServiceClassUUIDEntry;
   SDP_Data_Element_t        *SDP_Data_Element_Supported_Formats;
   SDP_Data_Element_t         GenericSDPDataElement;
   SDP_Data_Element_t         BluetoothProfileDescriptorListDataElement;
   SDP_Data_Element_t         ProfileDescriptorDataElement;
   SDP_Data_Element_t         OPPProfileDescriptorDataElementList[2];
   GOEP_SDP_Service_Record_t  GOEPSDPServiceRecord;

   /* Initialize the Service Class UUIDs.                               */
   SDPServiceClassUUIDEntry.SDP_Data_Element_Type = deUUID_16;
   ASSIGN_SDP_UUID_16(SDPServiceClassUUIDEntry.UUID_Value.UUID_16, 0x11, 0x05);

   /* Initialize the GOEP SDP Service Record structure with the Basic   */
   /* Imaging Profile Imaging Responder Service UUIDs.                  */
   GOEPSDPServiceRecord.NumberServiceClassUUID = sizeof(SDPServiceClassUUIDEntry)/sizeof(SDP_UUID_Entry_t);
   GOEPSDPServiceRecord.SDPUUIDEntries         = &SDPServiceClassUUIDEntry;
   GOEPSDPServiceRecord.ProtocolList           = NULL;

   /* Next use the GOEP Register SDP Record function to create the      */
   /* service record and add it to the SDP database for us.             */
   SS1_LOGI("Registering SDP Record");
   ret_val = RegisterSDPRecordForGOEP(&GOEPSDPServiceRecord, OPP_SERVICE_CHANNEL, OPP_SERVICE_NAME, &SDPServiceRecordHandle);

   if(ret_val >= 0)
   {
      /* The Service Record was successfully created, all that remains  */
      /* is adding the Bluetooth Profile Descriptor List to this SDP    */
      /* Record.                                                        */

      /* Format the Bluetooth Profile Descriptor List Data Element.     */
      BluetoothProfileDescriptorListDataElement.SDP_Data_Element_Type                      = deSequence;
      BluetoothProfileDescriptorListDataElement.SDP_Data_Element_Length                    = sizeof(ProfileDescriptorDataElement)/sizeof(SDP_Data_Element_t);
      BluetoothProfileDescriptorListDataElement.SDP_Data_Element.SDP_Data_Element_Sequence = &ProfileDescriptorDataElement;

      /* Initialize Profile Descriptor #0.                              */
      ProfileDescriptorDataElement.SDP_Data_Element_Type                      = deSequence;
      ProfileDescriptorDataElement.SDP_Data_Element_Length                    = sizeof(OPPProfileDescriptorDataElementList)/sizeof(SDP_Data_Element_t);
      ProfileDescriptorDataElement.SDP_Data_Element.SDP_Data_Element_Sequence = OPPProfileDescriptorDataElementList;

      /* Initialize the Profile UUID and Version Number Data Elements   */
      /* for the Object Push Profile. UUID (16-bit) is 0x1105 and       */
      /* Version is 0x0100.                                             */
      OPPProfileDescriptorDataElementList[0].SDP_Data_Element_Type   = deUUID_16;
      OPPProfileDescriptorDataElementList[0].SDP_Data_Element_Length = sizeof(OPPProfileDescriptorDataElementList[0].SDP_Data_Element.UUID_16);
      ASSIGN_SDP_UUID_16(OPPProfileDescriptorDataElementList[0].SDP_Data_Element.UUID_16, 0x11, 0x05);

      OPPProfileDescriptorDataElementList[1].SDP_Data_Element_Type                  = deUnsignedInteger2Bytes;
      OPPProfileDescriptorDataElementList[1].SDP_Data_Element_Length                = sizeof(OPPProfileDescriptorDataElementList[1].SDP_Data_Element.UnsignedInteger2Bytes);
      OPPProfileDescriptorDataElementList[1].SDP_Data_Element.UnsignedInteger2Bytes = 0x0100;

      /* Now attempt to add the Bluetooth Profile Descriptor List       */
      /* Attribute.                                                     */
      SS1_LOGI("Adding Profile Descriptor List");
      if((ret_val = DEVM_AddServiceRecordAttribute(SDPServiceRecordHandle, SDP_ATTRIBUTE_ID_BLUETOOTH_PROFILE_DESCRIPTOR_LIST, &BluetoothProfileDescriptorListDataElement)) >= 0)
      {
         /* Finally, let's add the Supported Formats List.              */
         NumberSupportedFormats = 7;

         /* First, let's allocate memory to hold the SDP Data Element   */
         /* Sequence List.                                              */
         if((SDP_Data_Element_Supported_Formats = (SDP_Data_Element_t *)BTPS_AllocateMemory(SDP_DATA_ELEMENT_SIZE*NumberSupportedFormats)) != NULL)
         {
            /* Next, let's build the SDP Data Element Sequence.         */
            GenericSDPDataElement.SDP_Data_Element_Type                      = deSequence;
            GenericSDPDataElement.SDP_Data_Element_Length                    = NumberSupportedFormats;
            GenericSDPDataElement.SDP_Data_Element.SDP_Data_Element_Sequence = SDP_Data_Element_Supported_Formats;

            /* Now we will add each Supported Format/Object to the List */
            /* individually.                                            */

            /* Any Object                                               */
            SDP_Data_Element_Supported_Formats[0].SDP_Data_Element_Type                 = deUnsignedInteger1Byte;
            SDP_Data_Element_Supported_Formats[0].SDP_Data_Element_Length               = BYTE_SIZE;
            SDP_Data_Element_Supported_Formats[0].SDP_Data_Element.UnsignedInteger1Byte = 0xff;
            /* vCard v2.1                                               */
            SDP_Data_Element_Supported_Formats[1].SDP_Data_Element_Type                 = deUnsignedInteger1Byte;
            SDP_Data_Element_Supported_Formats[1].SDP_Data_Element_Length               = BYTE_SIZE;
            SDP_Data_Element_Supported_Formats[1].SDP_Data_Element.UnsignedInteger1Byte = 0x01;
            /* vCard v3.0                                               */
            SDP_Data_Element_Supported_Formats[2].SDP_Data_Element_Type                 = deUnsignedInteger1Byte;
            SDP_Data_Element_Supported_Formats[2].SDP_Data_Element_Length               = BYTE_SIZE;
            SDP_Data_Element_Supported_Formats[2].SDP_Data_Element.UnsignedInteger1Byte = 0x02;
            /* vCalendar v1.0                                           */
            SDP_Data_Element_Supported_Formats[3].SDP_Data_Element_Type                 = deUnsignedInteger1Byte;
            SDP_Data_Element_Supported_Formats[3].SDP_Data_Element_Length               = BYTE_SIZE;
            SDP_Data_Element_Supported_Formats[3].SDP_Data_Element.UnsignedInteger1Byte = 0x03;
            /* iCalendar v1.0                                           */
            SDP_Data_Element_Supported_Formats[4].SDP_Data_Element_Type                 = deUnsignedInteger1Byte;
            SDP_Data_Element_Supported_Formats[4].SDP_Data_Element_Length               = BYTE_SIZE;
            SDP_Data_Element_Supported_Formats[4].SDP_Data_Element.UnsignedInteger1Byte = 0x04;
            /* vNote                                                    */
            SDP_Data_Element_Supported_Formats[5].SDP_Data_Element_Type                 = deUnsignedInteger1Byte;
            SDP_Data_Element_Supported_Formats[5].SDP_Data_Element_Length               = BYTE_SIZE;
            SDP_Data_Element_Supported_Formats[5].SDP_Data_Element.UnsignedInteger1Byte = 0x05;
            /* vMessage                                                 */
            SDP_Data_Element_Supported_Formats[6].SDP_Data_Element_Type                 = deUnsignedInteger1Byte;
            SDP_Data_Element_Supported_Formats[6].SDP_Data_Element_Length               = BYTE_SIZE;
            SDP_Data_Element_Supported_Formats[6].SDP_Data_Element.UnsignedInteger1Byte = 0x06;

            /* Now that we have built the Supported Formats List, we    */
            /* need to add it to the SDP Database.                      */
            SS1_LOGI("Adding Supported Formats List");
            ret_val = DEVM_AddServiceRecordAttribute(SDPServiceRecordHandle, SDP_ATTRIBUTE_ID_SUPPORTED_FORMATS_LIST, &GenericSDPDataElement);

            /* All finished with the Memory we allocated so free the    */
            /* memory.                                                  */
            BTPS_FreeMemory(SDP_Data_Element_Supported_Formats);

            /* Check whether the attribute was added successfully.      */
            if(ret_val >= 0)
            {
               /* The record was built correctly, so return the handle. */
               SS1_LOGI("Record built successfully");
               if(SDPServiceRecordHandlePtr)
                  *SDPServiceRecordHandlePtr = SDPServiceRecordHandle;
            }
            else
            {
               /* The attribute failed to be added, so remove the       */
               /* record.                                               */
               SS1_LOGI("Unable to add Supported Formats List attribute (%d)", ret_val);
               DEVM_DeleteServiceRecord(SDPServiceRecordHandle);
            }
         }
         else
         {
            /* Since we are unable to allocate the required memory, we  */
            /* need to flag an error to the caller and delete the SDP   */
            /* Record from the SDP Database.                            */
            SS1_LOGE("Memory allocation failure");
            DEVM_DeleteServiceRecord(SDPServiceRecordHandle);

            ret_val = -1;
         }
      }
      else
      {
         /* Unable to add attribute, go ahead and delete the record.    */
         SS1_LOGE("Failed to add Profile descriptor list to SDP record (%d)", ret_val);
         DEVM_DeleteServiceRecord(SDPServiceRecordHandle);
      }
   }
   else
      SS1_LOGE("SDP Record registration failed (%d)", ret_val);

   return(ret_val);
}


static int RegisterSDPRecordForPBAP(unsigned int *SDPServiceRecordHandlePtr)
{
   int                        ret_val;
   unsigned int               SDPServiceRecordHandle;
   SDP_UUID_Entry_t           SDPServiceClassUUIDEntry;
   SDP_Data_Element_t         GenericDataElement;
   SDP_Data_Element_t         BluetoothProfileDescriptorListDataElement;
   SDP_Data_Element_t         ProfileDescriptorDataElement;
   SDP_Data_Element_t         GenericDataElementList[4];
   GOEP_SDP_Service_Record_t  GOEPSDPServiceRecord;

   /* Initialize PBAP Server Service ID UUID                            */
   SDPServiceClassUUIDEntry.SDP_Data_Element_Type = deUUID_16;
   ASSIGN_SDP_UUID_16(SDPServiceClassUUIDEntry.UUID_Value.UUID_16, 0x11, 0x2F);

   /* Initialize the GOEP SDP Service Record structure with the Service */
   /* UUIDs.                                                            */
   GOEPSDPServiceRecord.NumberServiceClassUUID = sizeof(SDPServiceClassUUIDEntry)/sizeof(SDP_UUID_Entry_t);
   GOEPSDPServiceRecord.SDPUUIDEntries         = &SDPServiceClassUUIDEntry;
   GOEPSDPServiceRecord.ProtocolList           = NULL;

   /* Next use the GOEP Register SDP Record function to create the      */
   /* service record and add it to the SDP database for us.             */
   if((ret_val = RegisterSDPRecordForGOEP(&GOEPSDPServiceRecord, PBAP_SERVICE_CHANNEL, PBAP_SERVICE_NAME, &SDPServiceRecordHandle)) == 0)
   {
      /* The Service Record was successfully created. GOEP Register     */
      /* automatically creates protocol descriptor list that is common  */
      /* to all service types for PBAP.                                 */

      /* We must now we must create the Bluetooth Profile Descriptor    */
      /* List to this SDP Record.                                       */

      /* Setup element.                                                 */
      GenericDataElementList[0].SDP_Data_Element_Type   = deUUID_16;
      GenericDataElementList[0].SDP_Data_Element_Length = sizeof(GenericDataElementList[0].SDP_Data_Element.UUID_16);

      /* Initialize the Profile UUID Data Element for the PBAP Server.  */
      ASSIGN_SDP_UUID_16(GenericDataElementList[0].SDP_Data_Element.UUID_16, 0x11, 0x30);

      /* Continue setting common profile descriptor list element data.  */
      GenericDataElementList[1].SDP_Data_Element_Type                  = deUnsignedInteger2Bytes;
      GenericDataElementList[1].SDP_Data_Element_Length                = sizeof(GenericDataElementList[1].SDP_Data_Element.UnsignedInteger2Bytes);
      GenericDataElementList[1].SDP_Data_Element.UnsignedInteger2Bytes = PBAP_SERVICE_VERSION;

      /* Initialize Profile Descriptor #0.                              */
      ProfileDescriptorDataElement.SDP_Data_Element_Type                      = deSequence;

      /* Hard coded because we use less of this buffer here than later. */
      ProfileDescriptorDataElement.SDP_Data_Element_Length                    = 2;
      ProfileDescriptorDataElement.SDP_Data_Element.SDP_Data_Element_Sequence = GenericDataElementList;

      /* Format the Bluetooth Profile Descriptor List Data Element.     */
      BluetoothProfileDescriptorListDataElement.SDP_Data_Element_Type                      = deSequence;
      BluetoothProfileDescriptorListDataElement.SDP_Data_Element_Length                    = sizeof(ProfileDescriptorDataElement)/sizeof(SDP_Data_Element_t);
      BluetoothProfileDescriptorListDataElement.SDP_Data_Element.SDP_Data_Element_Sequence = &ProfileDescriptorDataElement;

      /* Now attempt to add the Bluetooth Profile Descriptor List       */
      /* Attribute.                                                     */
      if((ret_val = DEVM_AddServiceRecordAttribute(SDPServiceRecordHandle, SDP_ATTRIBUTE_ID_BLUETOOTH_PROFILE_DESCRIPTOR_LIST, &BluetoothProfileDescriptorListDataElement)) < 0)
      {
         /* Failed to add attribute - remove already registered SDP     */
         /* record and return error code.                               */
         DEVM_DeleteServiceRecord(SDPServiceRecordHandle);
      }

      /* We also need to add a Supported Repositories data element.     */
      /* Check to see if an error occurred up until this point.         */
      if(!ret_val)
      {
         /* Create the supported repositories data element.             */
         GenericDataElement.SDP_Data_Element_Type                 = deUnsignedInteger1Byte;
         GenericDataElement.SDP_Data_Element_Length               = sizeof(GenericDataElement.SDP_Data_Element.UnsignedInteger1Byte);
         GenericDataElement.SDP_Data_Element.UnsignedInteger1Byte = (Byte_t)PBAP_SERVICE_SUPPORTED_REPOSITORIES;

         /* Now that we have built the Supported Repositories List, we  */
         /* need to add it to the SDP Database.                         */
         if((ret_val = DEVM_AddServiceRecordAttribute(SDPServiceRecordHandle, SDP_ATTRIBUTE_ID_SUPPORTED_REPOSITORIES, &GenericDataElement)) >= 0)
         {
            /* Success. Pass handle back via argument, if requested.    */
            if(SDPServiceRecordHandlePtr)
               *SDPServiceRecordHandlePtr = SDPServiceRecordHandle;
         }
         else
         {
            /* Failed to add attribute - remove already registered SDP  */
            /* record and return error code.                            */
            DEVM_DeleteServiceRecord(SDPServiceRecordHandle);
         }
      }
   }

   return(ret_val);
}

#endif /* Have_BLUETOOTH */


   /* The following function implements the Java native method:         */
   /*    android.server.BluetoothService.classInitNative()              */
   /*                                                                   */
   /* This function is used to initialize process-scope data structures */
   /* for the native component of the BluetoothService class. It is     */
   /* called from the static initializer of the class.                  */
static void BTS_ClassInitNative(JNIEnv *Env, jclass Clazz)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   Field_mNativeData = Env->GetFieldID(Clazz, "mNativeData", "I");
   Field_mEventLoop  = Env->GetFieldID(Clazz, "mEventLoop",  "Landroid/server/BluetoothEventLoop;");

   Method_isRemoteDeviceInCache     = Env->GetMethodID(Clazz, "isRemoteDeviceInCache", "(Ljava/lang/String;)Z");

#if (SS1_PLATFORM_SDK_VERSION < 14)
   Method_addRemoteDeviceProperties = Env->GetMethodID(Clazz, "addRemoteDeviceProperties", "(Ljava/lang/String;[Ljava/lang/String;)V");
   Method_getRemoteDeviceProperty   = Env->GetMethodID(Clazz, "getRemoteDeviceProperty", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
#endif

#endif
   SS1_LOGD("Exit");
}

   /* The following function implements the Java native method:         */
   /*    android.server.BluetoothService.initializeNativeDataNative()   */
   /*                                                                   */
   /* This function is used to initialize per-instance data structures  */
   /* and resources for objects of the BluetoothService class. It is    */
   /* called from within the class ctor.                                */
static jboolean BTS_InitializeNativeDataNative(JNIEnv *Env, jobject Object)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   jboolean          ret_val = JNI_FALSE;
   BTS_NativeData_t *NatData;

   if((*((void **)&NatData) = calloc(1, sizeof(BTS_NativeData_t))) != NULL)
   {
      Env->SetIntField(Object, Field_mNativeData, (jint)NatData);
      NatData->AuthCallbackID      = -1;
      NatData->DEVMCallbackID      = -1;
#if (SS1_PLATFORM_SDK_VERSION >= 14)
      NatData->HIDMCallbackID      = -1;
      NatData->PANMCallbackID      = -1;
#endif

      NatData->DiscoverableTimeout = 120;

      ret_val = JNI_TRUE;
   }
   else
      SS1_LOGE("Out of memory");

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else  /*HAVE_BLUETOOTH*/
   SS1_LOGD("Exit (%d)", JNI_TRUE);
   return(JNI_TRUE);
#endif
}

   /* The following function implements the Java native method:         */
   /*    android.server.BluetoothService.setupNativeDataNative()        */
   /*                                                                   */
   /* This function provides any additional initialization required     */
   /* by the native component of BluetoothService which can only be     */
   /* completed after the Bluetooth stack is started. It is called after*/
   /* the event loop is started, before fetching the list of bonded     */
   /* devices.                                                          */
static jboolean BTS_SetupNativeDataNative(JNIEnv *Env, jobject Object)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   int                AuthCallbackID;
   int                HIDMCallbackID;
   int                PANMCallbackID;
   jobject            EventLoopObject;
   jboolean           ret_val;
   BTS_NativeData_t  *NatData;
   BTEL_NativeData_t *EventLoopNatData;

   ret_val = JNI_FALSE;

   if(BTS_IsEnabledNative(Env, Object))
   {
      if((EventLoopObject = Env->GetObjectField(Object, Field_mEventLoop)) != NULL)
      {
         if((EventLoopNatData = BTEL_GetNativeData(Env, EventLoopObject)) != NULL)
         {
            if((NatData = (BTS_NativeData_t *)(Env->GetIntField(Object, Field_mNativeData))) != NULL)
            {
               /* Register the Authentication Callback                  */
               if((AuthCallbackID = DEVM_RegisterAuthentication(BTEL_DEVMAuthenticationCallback, EventLoopNatData)) > 0)
               {
                  /* Store the Callback ID for later unregistration. */
                  NatData->AuthCallbackID          = AuthCallbackID;
                  EventLoopNatData->AuthCallbackID = AuthCallbackID;

                  /* Success, so return true.                           */
                  ret_val = JNI_TRUE;
               }
               else
                  SS1_LOGE("Unable to register Bluetooth authentication handler (%d)", AuthCallbackID);

#if (SS1_PLATFORM_SDK_VERSION >= 14)

               if(ret_val == JNI_TRUE)
               {
#if SS1_SUPPORT_HID
                  if((HIDMCallbackID = HIDM_Register_Event_Callback(BTEL_HIDMEventCallback, EventLoopNatData)) > 0)
                  {
                     /* Store the Callback ID for later unregistration. */
                     NatData->HIDMCallbackID = HIDMCallbackID;
                  }
                  else
                     SS1_LOGE("HID profile is disabled. Bluetooth keyboards and mice will not be usable. (%d)", HIDMCallbackID);
#endif

#if SS1_SUPPORT_PAN
                  if((PANMCallbackID = PANM_Register_Event_Callback(BTEL_PANMEventCallback, EventLoopNatData)) > 0)
                  {
                     /* Store the Callback ID for later unregistration. */
                     NatData->PANMCallbackID = PANMCallbackID;
                  }
                  else
                     SS1_LOGE("PAN profile is disabled. Bluetooth networking will be unavailable. (%d)", PANMCallbackID);
#endif
               }
#endif
            }
            else
               SS1_LOGE("BTS NatData is invalid");
         }
         else
            SS1_LOGE("BTEL NatData is invalid");
      }
      else
         SS1_LOGE("Could not find associated BTEL object");
   }
   else
      SS1_LOGE("Called while Bluetooth is disabled");

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", JNI_TRUE);
   return(JNI_TRUE);
#endif
}

   /* The following function implements the Java native method:         */
   /*    android.server.BluetoothService.tearDownNativeDataNative()     */
   /*                                                                   */
   /* This function cleans up resources obtained from the Bluetooth     */
   /* stack, which should be freed before terminating the stack.        */
static jboolean BTS_TearDownNativeDataNative(JNIEnv *Env, jobject Object)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   jobject            EventLoopObject;
   jboolean           ret_val;
   BTS_NativeData_t  *NatData;
   BTEL_NativeData_t *EventLoopNatData;

   if((NatData = (BTS_NativeData_t *)(Env->GetIntField(Object, Field_mNativeData))) != NULL)
   {
      if(NatData->AuthCallbackID > 0)
         DEVM_UnRegisterAuthentication(NatData->AuthCallbackID);

      NatData->AuthCallbackID = -1;

#if (SS1_PLATFORM_SDK_VERSION >= 14)

#if SS1_SUPPORT_HID
      if(NatData->HIDMCallbackID > 0)
         HIDM_Un_Register_Event_Callback(NatData->HIDMCallbackID);

      NatData->HIDMCallbackID = -1;
#endif

#if SS1_SUPPORT_PAN
      if(NatData->PANMCallbackID > 0)
         PANM_Un_Register_Event_Callback(NatData->PANMCallbackID);

      NatData->PANMCallbackID = -1;
#endif

#endif

      if((EventLoopObject = Env->GetObjectField(Object, Field_mEventLoop)) != NULL)
         if((EventLoopNatData = BTEL_GetNativeData(Env, EventLoopObject)) != NULL)
            EventLoopNatData->AuthCallbackID = -1;

      ret_val = JNI_TRUE;
   }
   else
      ret_val = JNI_FALSE;

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", JNI_TRUE);
   return(JNI_TRUE);
#endif
}

   /* The following function implements the Java native method:         */
   /*    android.server.BluetoothService.cleanupNativeDataNative()      */
   /*                                                                   */
   /* This function performs the final cleanup of any system resources  */
   /* used by the native component of BluetoothService objects. It is   */
   /* called from within the class dtor.                                */
static void BTS_CleanupNativeDataNative(JNIEnv *Env, jobject Object)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   BTS_NativeData_t *NatData;

   if((NatData = (BTS_NativeData_t *)(Env->GetIntField(Object, Field_mNativeData))) != NULL)
   {
      Env->SetIntField(Object, Field_mNativeData, (jint)NULL);
      free(NatData);
   }
#endif /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit");
}

   /* The following function implements the Java native method:         */
   /*    android.server.BluetoothService.getAdapterPathNative()         */
   /*                                                                   */
   /* This function provides the string identifier for the Bluetooth    */
   /* adapter. This is an artifact of the BlueZ-based design of         */
   /* Android's Bluetooth API. In BlueZ, each adapter is represented    */
   /* by a DBUS path and each remote device is referenced as            */
   /* <AdapterPath>/dev_<RemoteMAC>. We need no such paths, so this     */
   /* function simply returns the empty string. Therefore, whenever     */
   /* the Java API passes an "Object Path" string to us (representing   */
   /* a remote device), the string will contain "/dev_" + REMOTE_ADDR,  */
   /* where REMOTE_ADDR is the Bluetooth address of the remote device   */
   /* represented in capitalized hexadecimal with each octet delimited  */
   /* by underscores.                                                   */
static jstring BTS_GetAdapterPathNative(JNIEnv *Env, jobject Object)
{
#ifdef HAVE_BLUETOOTH
   return(Env->NewStringUTF(""));
#else /* HAVE_BLUETOOTH */
   return(NULL);
#endif
}

   /* The following function implements the Java native method:         */
   /*    android.server.BluetoothService.isEnabledNative()              */
   /*                                                                   */
   /* This function is used to query the current state of the Bluetooth */
   /* stack. It returns 1 if Bluetooth is enabled, or 0 if it is not. A */
   /* negative value indiciates an error.                               */
static jint BTS_IsEnabledNative(JNIEnv *Env, jobject Object)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   jint ret_val;

   /* Check whether the stack is online.  DEVM_QueryDevicePowerState()  */
   /* should return a positive value if the stack is online.            */
   if(DEVM_QueryDevicePowerState() > 0)
   {
      SS1_LOGI("BT Daemon appears to be running");
      ret_val = 1;
   }
   else
   {
      SS1_LOGW("BT Daemon appears to be stopped");
      ret_val = 0;
   }

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else
   SS1_LOGD("Exit (%d)", -1);
   return(-1);
#endif
}

   /* The following function implements the Java native method:         */
   /*    android.server.BluetoothService.enableNative()                 */
   /*                                                                   */
   /* This function is used to enable the Bluetooth stack. This         */
   /* includes starting the userspace daemon if it is not already       */
   /* running, initializing the daemon's client-side library,           */
   /* registering any necessary callbacks, and issuing a "power on"     */
   /* command to enable the Bluetooth adapter. This function returns    */
   /* zero on success, or -1 if an error occurred.                      */
static jint BTS_EnableNative(JNIEnv *Env, jobject Object)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   int                            DEVMCallbackID;
   int                            Result;
   int                            PropertyLength;
   char                           PropertyValue[PROPERTY_VALUE_MAX];
   jobject                        EventLoopObject;
   BTS_NativeData_t              *NatData;
   Class_of_Device_t              ClassOfDevice;
   BTEL_NativeData_t             *EventLoopNatData;
   DEVM_Local_Device_Properties_t LocalDevProps;

   /* Set known states in case operations fail before these are given   */
   /* meaningful values.                                                */
   DEVMCallbackID = -1;
   EventLoopNatData = NULL;

   /* FIXME HACK: Until the SS1BTPM service implements a signal handler */
   /* to gracefully handle a stop command from init, the server socket  */
   /* may be left behind. Delete it if we can find it.                  */
   if((PropertyLength = property_get("init.svc." BTPM_SERVICE_NAME, PropertyValue, "invalid")) > 0)
   {
      if(strncmp(PropertyValue, "stopped", PropertyLength + 1) == 0)
      {
         /* If the service is not running, remove the socket file if it */
         /* exists. It should only exist if the service was not stopped */
         /* cleanly or the phone crashed and rebooted.                  */
         unlink(BTPM_SERVICE_SOCKET);
      }
      else
      {
         if(strncmp(PropertyValue, "invalid", PropertyLength + 1) == 0)
         {
            /* Not getting a response back "probably" means that init   */
            /* hasn't seen it, yet -- i.e., the service has never been  */
            /* started before. Just in case, stop it explicitly and     */
            /* remove the socket file so it's definately ready to be    */
            /* started.                                                 */
            SS1_LOGI("Unable to query the status of the Bluetooth daemon");
            property_set("ctl.stop", BTPM_SERVICE_NAME);
            unlink(BTPM_SERVICE_SOCKET);
         }
         else
         {
            if(strncmp(PropertyValue, "running", PropertyLength + 1) == 0)
               SS1_LOGW("WARNING: Bluetooth started but deamon already running.");
            else
            {
               /* Guarantee that the string is NULL-terminated.         */
               PropertyValue[PROPERTY_VALUE_MAX-1] = '\0';
               SS1_LOGW("WARNING: Query of Bluetooth daemon status gave unexpected value: \"%s\"", PropertyValue);
            }
         }
      }
   }
   else
      SS1_LOGE("PropGet failed for query of BT daemon status.");

   /* Obtain a reference to the native data object of the               */
   /* BluetoothEventLoop object associated with this instance of        */
   /* BluetoothService, for use in callback registrations.              */
   if((EventLoopObject = Env->GetObjectField(Object, Field_mEventLoop)) != NULL)
   {
      EventLoopNatData = BTEL_GetNativeData(Env, EventLoopObject);
      SS1_LOGI("Fetched BTEL NatData = %p", EventLoopNatData);
      Env->DeleteLocalRef(EventLoopObject);
   }

   /* Start the Stonestreet One BluetopiaPM Service                     */
   if((Result = property_set("ctl.start", BTPM_SERVICE_NAME)) >= 0)
   {
      /* Now check that the service really is being started. If the     */
      /* serivce is not definied in an init.rc file, the init.svc.*     */
      /* status property will still be undefined.                       */
      //if((PropertyLength = property_get("init.svc." BTPM_SERVICE_NAME, PropertyValue, "invalid")) > 0)
      //{
      //   if(strncmp(PropertyValue, "invalid", PropertyLength + 1) != 0)
      //   {
      //      /* The status property is defined, so the service definately*/
      //      /* has a definition in init.rc or init.*.rc.                */
      //   }
      //}

      SS1_LOGI("Service started");

      /* Initialize the client library. InitBTPMClient should retry a   */
      /* set number of times in case the daemon is slow to load.        */
      if((Result = InitBTPMClient(NULL, BTEL_IPCLostCallback, EventLoopNatData, 10, BTPM_CLIENT_INIT_DEFAULT_DELAY_MS)) == 0)
      {
         SS1_LOGI("Stack Initialized (%d)", Result);

         /* Register callbacks                                          */
         if((EventLoopNatData) && (DEVMCallbackID = DEVM_RegisterEventCallback(BTEL_DEVMEventCallback, EventLoopNatData)) > 0)
         {
            SS1_LOGI("Message handler registered (ID = %d, CB = %p, CBD = %p)", DEVMCallbackID, BTEL_DEVMEventCallback, EventLoopNatData);

            /* Store the DEVMCallbackID for later unregistration.       */
            NatData = (BTS_NativeData_t *)(Env->GetIntField(Object, Field_mNativeData));
            if(NatData)
               NatData->DEVMCallbackID = DEVMCallbackID;

            /* Issue the "Power On" command.                            */
            Result = DEVM_PowerOnDevice();
            if((Result == 0) || (Result == BTPM_ERROR_CODE_LOCAL_DEVICE_ALREADY_POWERED_UP))
            {
               SS1_LOGI("Bluetooth enabled");

               /* Success.                                              */

               /* Set the stack to be non-connectable and               */
               /* non-discoverable.                                     */
               BTS_SetAdapterPropertyBooleanNative(Env, Object, Env->NewStringUTF("Powered"), JNI_FALSE);

               /* Set the stack to be non-pairable. Android will change */
               /* this after initialization is complete.                */
               BTS_SetAdapterPropertyBooleanNative(Env, Object, Env->NewStringUTF("Pairable"), JNI_FALSE);

#if (SS1_PLATFORM_SDK_VERSION < 11)

               /* Android 2.3 and earlier expect the Bluetooth stack    */
               /* to be immediately connectible and pairable after      */
               /* initialization.                                       */
               BTS_SetAdapterPropertyBooleanNative(Env, Object, Env->NewStringUTF("Powered"), JNI_TRUE);
               BTS_SetAdapterPropertyBooleanNative(Env, Object, Env->NewStringUTF("Pairable"), JNI_TRUE);

#endif /*  (SS1_PLATFORM_SDK_VERSION < 11) */

#if (SS1_PLATFORM_SDK_VERSION < 14)
               /* Add SDP records for static, system-provided           */
               /* services. In Android 4.0 and newer,                   */
               /* BluetoothService handles this by calling              */
               /* addReservedServiceRecordsNative().                    */
               SS1_LOGI("Registering SDP record for HSAG");
               RegisterSDPRecordForHeadset(NULL);
               SS1_LOGI("Registering SDP record for HFAG");
               RegisterSDPRecordForHandsFree(NULL);
               SS1_LOGI("Registering SDP record for OPP");
               RegisterSDPRecordForOPP(NULL);
               SS1_LOGI("Registering SDP record for PBAP");
               RegisterSDPRecordForPBAP(NULL);
#endif
            }
            else
            {
               SS1_LOGE("Bluetooth enabled FAILED (%d)", Result);

               /* There was a problem powering up the adapter.  Shutdown*/
               /* the stack and report the error.                       */
               BTS_DisableNative(Env, Object);
               DEVMCallbackID = 0;
            }
         }
         else
         {
            SS1_LOGE("Callback registration FAILED (%p, %d)", EventLoopNatData, DEVMCallbackID);
         }
      }
      else
         SS1_LOGE("Stack initialization FAILED (%d)", Result);
   }
   else
      SS1_LOGE("Problem starting service (%d)", Result);

   if(DEVMCallbackID > 0)
   {
      /* Startup succeeded. Inform the A2DP service (running in the same*/
      /* process) so it can perform any necessary initialization.       */
      BTAS_SignalBluetoothEnabled();
   }
   else
   {
      /* Startup failed, so undo the library initialization.            */
      BTS_DisableNative(Env, Object);
   }

   SS1_LOGD("Exit (%d)", ((DEVMCallbackID > 0) ? 0 : -1));
   return((DEVMCallbackID > 0) ? 0 : -1);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", -1);
   return(-1);
#endif
}

   /* The following function implements the Java native method:         */
   /*    android.server.BluetoothService.disableNative()                */
   /*                                                                   */
   /* This function is used to disable the Bluetooth stack. The process */
   /* is the reverse of that used by BTS_EnableNative(). That is,       */
   /* unregister any callbacks, stop the userspace daemon, and release  */
   /* the client-side library. This function returns zero on success, or*/
   /* -1 if an error occurred.                                          */
static jint BTS_DisableNative(JNIEnv *Env, jobject Object)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   int                Result;
   int                PropertyLength;
   char               PropertyValue[PROPERTY_VALUE_MAX + 1];
   jobject            EventLoopObject;
   Boolean_t          HoldingLock;
   struct timespec    WaitTime;
   BTS_NativeData_t  *NatData;
   BTEL_NativeData_t *EventLoopNatData;

   HoldingLock      = FALSE;
   EventLoopNatData = NULL;

   /* Warn the A2DP service (running in the same process) that the stack*/
   /* is going down so it can unregister itself.                        */
   BTAS_SignalBluetoothDisabled();

   /* Attempt to lock the DevicePoweredOffMutex from BluetoothEventLoop */
   /* for use in monitoring the progress of the power-off process.      */
   /* If we can't obtain it, for whatever reason, proceed with the      */
   /* shutdown anyway. We attempt to obtain the mutex before calling    */
   /* DEVM_PowerOffDevice() to ensure that, if the callback comes       */
   /* immediately, we'll be sure to receive the "Powered Off" condition */
   /* and not wait unnecessarily long.                                  */
   if((EventLoopObject = Env->GetObjectField(Object, Field_mEventLoop)) != NULL)
   {
      if((EventLoopNatData = BTEL_GetNativeData(Env, EventLoopObject)) != NULL)
      {
         if((Result = pthread_mutex_lock(&(EventLoopNatData->DevicePoweredOffMutex))) == 0)
         {
            HoldingLock = TRUE;
         }
         else
         {
            /* There was a problem obtaining the mutex. Clear our reference*/
            /* to the BluetoothEventLoop native data structure to indicate */
            /* this state.                                                 */
            EventLoopNatData = NULL;
            SS1_LOGD("Unable to obtain \"Device Powered Off\" mutex (%d)", Result);
         }
      }
      else
         SS1_LOGD("Unable to obtain BluetoothEventLoop native data");
   }
   else
      SS1_LOGW("Unable to obtain BluetoothEventLoop object. Has it already been destroyed?");

   /* DEBUG */ SS1_LOGD("Beginning Bluetooth power-off process");

   /* Issue the "Power Off" Command.                                    */
   Result = DEVM_PowerOffDevice();

   if((Result == 0) || (Result == BTPM_ERROR_CODE_LOCAL_DEVICE_ALREADY_POWERED_DOWN))
   {
      /* "Power Off" is non-blocking, so we need to wait for the        */
      /* DevicePoweredOff signal to inform us that everthing on the     */
      /* server-side had a chance to clean up. The server could take    */
      /* up to BTPM_CONFIGURATION_DEVICE_MANAGER_POWER_DOWN_TIME_MS to  */
      /* perform the power-down. In case a problem prevents us from     */
      /* receiving the shutdown notice, we'll only wait up to twice this*/
      /* duration.                                                      */
      if(EventLoopNatData)
      {
         /* We still have the BTEL NatData reference, so we can assume  */
         /* we hold the DevicePoweredOffMutex mutex and it's safe to    */
         /* proceed with the wait.                                      */
         clock_gettime(CLOCK_REALTIME, &WaitTime);
         WaitTime.tv_sec += (BTPM_CONFIGURATION_DEVICE_MANAGER_POWER_DOWN_TIME_MS / 500);

         while((Result = pthread_cond_timedwait(&(EventLoopNatData->DevicePoweredOffCondition), &(EventLoopNatData->DevicePoweredOffMutex), &WaitTime)) == EINTR)
         {
            /* Continue waiting so long as we are being interrupted. We */
            /* won't generate any clock skew in this since we're waiting*/
            /* on an absolute time.                                     */
         }

         if(HoldingLock)
         {
            HoldingLock = FALSE;
            pthread_mutex_unlock(&(EventLoopNatData->DevicePoweredOffMutex));
         }

         if(Result == ETIMEDOUT)
            SS1_LOGW("Bluetooth service has not finished powering down after %d ms. There might be a problem.", (BTPM_CONFIGURATION_DEVICE_MANAGER_POWER_DOWN_TIME_MS * 2));
         else
         {
            /* Notify Android that the stack is disabled.               */
            SendDelayedPropertyChangedEvent(Env, Object, "Powered", "false", 100L);
         }
      }
      else
      {
         if(HoldingLock)
         {
            HoldingLock = FALSE;
            pthread_mutex_unlock(&(EventLoopNatData->DevicePoweredOffMutex));
         }
      }

      SS1_LOGI("Bluetooth disabled");

      /* The deamon is running and accepted the command. Either the     */
      /* stack is now stopped or was already down.                      */

      /* If we previously registered a DEVM Event callback, unregister  */
      /* it now.                                                        */
      NatData = (BTS_NativeData_t *)(Env->GetIntField(Object, Field_mNativeData));
      if(NatData->DEVMCallbackID >= 0)
      {
         SS1_LOGI("Unregistering message handler");
         DEVM_UnRegisterEventCallback(NatData->DEVMCallbackID);
         NatData->DEVMCallbackID = -1;
      }
   }
   else
   {
      if(HoldingLock)
      {
         HoldingLock = FALSE;
         pthread_mutex_unlock(&(EventLoopNatData->DevicePoweredOffMutex));
      }

      SS1_LOGE("Unable to stop Bluetooth gracefully, service already stopped? (code %d)", Result);
   }

   /* Release the client library.                                       */
   CloseBTPMClient();
   SS1_LOGI("Bluetooth service interface shut down.");

   /* XXX DEBUG */
   /* TODO Perhaps adjust this to a test (if ! "running", don't stop).  */
   /* Is there a "starting" state that needs to be waited on?           */
   if((PropertyLength = property_get("init.svc." BTPM_SERVICE_NAME, PropertyValue, "invalid")) > 0)
   {
      PropertyValue[PropertyLength] = '\0';
      SS1_LOGI("service currently in state: %s", PropertyValue);
   }

   /* Stop BTPM Service                                                 */
   property_set("ctl.stop", BTPM_SERVICE_NAME);
   SS1_LOGI("Bluetooth service stopped");

   /* XXX DEBUG */
   /* TODO Perhaps change this to (if ! stopped, return error). Or wait */
   /* for some time while state is "stopping", if that's a valid state. */
   if((PropertyLength = property_get("init.svc." BTPM_SERVICE_NAME, PropertyValue, "invalid")) > 0)
   {
      PropertyValue[PropertyLength] = '\0';
      SS1_LOGI("service currently in state: %s", PropertyValue);
   }

   SS1_LOGD("Exit (%d)", 0);

   return(0);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", -1);
   return(-1);
#endif
}

#define INITIAL_KNOWN_DEVICES_REQUEST_SIZE 32

   /* The following function implements the Java native method:         */
   /*    android.server.BluetoothService.getAdapterPropertiesNative()   */
   /*                                                                   */
   /* This function is used to retreive a list of properties for the    */
   /* local Bluetooth adapter. The properties are returned as an array  */
   /* of Java String objects, containing (in sequence) the name of a    */
   /* property followed by the current value.                           */
static jobjectArray BTS_GetAdapterPropertiesNative(JNIEnv *Env, jobject Object)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   int                            Result;
   jobjectArray                   PropStringArray;
   DEVM_Local_Device_Properties_t LocalDevProps;

   /* Set some default values in case anything fails.                   */
   PropStringArray = NULL;

   /* Pull the Local Device properties.                                 */
   if((Result = DEVM_QueryLocalDeviceProperties(&LocalDevProps)) == 0)
   {
      /* Attempt to build the property list.                            */
      PropStringArray = BuildLocalPropertyStringArray(Env, LOCAL_PROPERTY_ALL, &LocalDevProps);
   }
   else
      SS1_LOGE("Local device property query failed (%d)", Result);

   /* XXX DEBUG OUTPUT */
   DebugDumpStringArray(Env, PropStringArray, __FUNCTION__);

   SS1_LOGD("Exit (%p)", PropStringArray);

   return(PropStringArray);
#else
   SS1_LOGD("Exit (%p)", NULL);
   return(NULL);
#endif
}

   /* The following function implements the Java native method:         */
   /*    android.server.BluetoothService.getDevicePropertiesNative()    */
   /*                                                                   */
   /* This function is used to retreive a list of properties for        */
   /* a remote Bluetooth device. The properties are returned as a       */
   /* formatted array of Java String objects, containing (in sequence)  */
   /* the name of a property followed by its current value.             */
static jobjectArray BTS_GetDevicePropertiesNative(JNIEnv *Env, jobject Object, jstring JPath)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   BD_ADDR_t                       RemoteAddress;
   const char                     *PathString;
   jobjectArray                    PropStringArray;
   DEVM_Remote_Device_Properties_t RemoteDevProps;

   /* Set some default values in case anything fails.                   */
   PropStringArray = NULL;

   if(JPath)
   {
      if((PathString = Env->GetStringUTFChars(JPath, NULL)) != NULL)
      {
         if(PathToBD_ADDR(&RemoteAddress, PathString))
         {
            if(DEVM_QueryRemoteDeviceProperties(RemoteAddress, FALSE, &RemoteDevProps) == 0)
            {
               /* The query completed successfully, so we can translate */
               /* the properties to a String[] format for sending up to */
               /* Java.                                                 */
               PropStringArray = BuildRemotePropertyStringArray(Env, REMOTE_PROPERTY_ALL, &RemoteDevProps);
            }
         }
         Env->ReleaseStringUTFChars(JPath, PathString);
      }
   }

   SS1_LOGD("Exit (%p)", PropStringArray);

   return(PropStringArray);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%p)", NULL);
   return(NULL);
#endif
}

static jboolean BTS_SetAdapterPropertyStringNative(JNIEnv *Env, jobject Object, jstring Key, jstring Value)
{
   SS1_LOGI("Enter (%p, %p)", Key, Value);
#ifdef HAVE_BLUETOOTH
   char                           PropBuffer[256];
   jobject                        EventLoopObject;
   jboolean                       ret_val;
   const char                    *PropKey;
   const char                    *PropValue;
   jobjectArray                   PropStringArray;
   DEVM_Local_Device_Properties_t NewDevProps;
   DEVM_Local_Device_Properties_t PreviousDevProps;

   ret_val = JNI_FALSE;

   /* Make sure both jstring paramters seem valid.                      */
   if((Key) && (Value))
   {
      /* Obtain character buffers containing the UTF-8 representations  */
      /* of the key and value.                                          */
      PropKey = Env->GetStringUTFChars(Key, NULL);
      PropValue = Env->GetStringUTFChars(Value, NULL);

      /* Check that the key and value buffers were allocated            */
      /* successfully.                                                  */
      if(PropKey && PropValue)
      {
         if(DEVM_QueryLocalDeviceProperties(&PreviousDevProps) == 0)
         {
            if(strcmp(PropKey, "Name") == 0)
            {
               /* Load the new name and length into a Local Device      */
               /* Properties structure, making sure the result is       */
               /* NULL-terminated.                                      */
               NewDevProps.DeviceNameLength = strlen(PropValue);
               strncpy(NewDevProps.DeviceName, PropValue, MAX_NAME_LENGTH);
               NewDevProps.DeviceName[MAX_NAME_LENGTH] = '\0';

               if((PreviousDevProps.DeviceNameLength != NewDevProps.DeviceNameLength) || (strncmp(PreviousDevProps.DeviceName, NewDevProps.DeviceName, PreviousDevProps.DeviceNameLength) != 0))
               {
                  SS1_LOGI("Setting BT Device name to \"%s\"", NewDevProps.DeviceName);

                  /* Try performing the update. We return success if    */
                  /* this works.                                        */
                  if(DEVM_UpdateLocalDeviceProperties(DEVM_UPDATE_LOCAL_DEVICE_PROPERTIES_DEVICE_NAME, &NewDevProps) == 0)
                     ret_val = JNI_TRUE;
               }
               else
               {
                  /* The device is already set to the requesetd name.   */
                  /* BlueZ sends notifications of state change when a   */
                  /* change is requested, even if that state is already */
                  /* set, so we need to simulate the event callback.    */
                  SendDelayedPropertyChangedEvent(Env, Object, "Name", NewDevProps.DeviceName, 100L);
               }
            }
            else
            {
               /* The "Name" property is the only string property       */
               /* available to update on the local adapter. This        */
               /* function should not be called with any other key.     */
               SS1_LOGE("Unknown string property %s", PropKey);
            }
         }
      }

      /* Release JNI resources.                                         */
      if(PropKey)
         Env->ReleaseStringUTFChars(Key, PropKey);

      if(PropValue)
         Env->ReleaseStringUTFChars(Value, PropValue);
   }

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(JNI_FALSE);
#endif
}

static jboolean BTS_SetAdapterPropertyBooleanNative(JNIEnv *Env, jobject Object, jstring Key, jint Value)
{
   SS1_LOGD("Enter (%p, %d)", Key, Value);
#ifdef HAVE_BLUETOOTH
   int                            Result;
   jobject                        EventLoopObject;
   jboolean                       ret_val;
   const char                    *PropKey;
   jobjectArray                   PropStringArray;
   BTS_NativeData_t              *NatData;
   DEVM_Local_Device_Properties_t NewDevProps;
   DEVM_Local_Device_Properties_t PreviousDevProps;

   ret_val = JNI_FALSE;

   /* Make sure the key seems valid.                                    */
   if(Key)
   {
      /* Obtain a character buffer containing the UTF-8 representation  */
      /* of the key.                                                    */
      PropKey = Env->GetStringUTFChars(Key, NULL);

      /* Check that the key buffer was allocated successfully.          */
      if(PropKey)
      {
         if(DEVM_QueryLocalDeviceProperties(&PreviousDevProps) == 0)
         {
            if(strcmp(PropKey, "Discoverable") == 0)
            {
               /* Load the new discoverable status into a Local Device  */
               /* Properties structure. When enabling Discoverable      */
               /* mode, BTPM expects to receive a DiscoverableTimeout   */
               /* in the same update request. A timeout of zero will    */
               /* imply General discoverability, while a positive,      */
               /* non-zero timeout will configure the device for        */
               /* limited discoverability and automatically disable     */
               /* discoverability after the timeout elapses.            */
               if((NatData = (BTS_NativeData_t *)(Env->GetIntField(Object, Field_mNativeData))) != NULL)
               {
                  if((PreviousDevProps.DiscoverableMode != Value) || (PreviousDevProps.DiscoverableModeTimeout != NatData->DiscoverableTimeout))
                  {
                     SS1_LOGI("Setting Discoverable Mode to %s, with %u second timeout", ((Value != JNI_FALSE) ? "TRUE" : "FALSE"), NatData->DiscoverableTimeout);
                     NewDevProps.DiscoverableMode        = ((Value != JNI_FALSE) ? TRUE : FALSE);
                     NewDevProps.DiscoverableModeTimeout = NatData->DiscoverableTimeout;

                     /* Try performing the update. We return success if */
                     /* this works.                                     */
                     if((Result = DEVM_UpdateLocalDeviceProperties(DEVM_UPDATE_LOCAL_DEVICE_PROPERTIES_DISCOVERABLE_MODE, &NewDevProps)) == 0)
                        ret_val = JNI_TRUE;
                     else
                        SS1_LOGE("Unable to change Discoverable Mode (%d)", Result);
                  }
                  else
                  {
                     /* The device is already in the requested          */
                     /* discoverable mode with the requested timeout.   */
                     /* BlueZ sends notifications of state change when  */
                     /* a change is requested, even if that state is    */
                     /* already set, so we need to simulate the event   */
                     /* callback.                                       */
                     SendDelayedPropertyChangedEvent(Env, Object, "Discoverable", ((Value != JNI_FALSE) ? "true" : "false"), 100L);
                  }
               }
               else
                  SS1_LOGE("Unable to access Service state data, failed to set Discoverable property");
            }
            else
            {
               if(strcmp(PropKey, "Pairable") == 0)
               {
                  if(PreviousDevProps.PairableMode != ((Value != JNI_FALSE) ? TRUE : FALSE))
                  {
                     SS1_LOGI("Setting Pairable Mode to %s", ((Value != JNI_FALSE) ? "TRUE" : "FALSE"));

                     /* Load the new pairable status into a Local       */
                     /* Device Properties structure. The device should  */
                     /* stay pairable, so a timeout of zero is used     */
                     /* (infinite).                                     */
                     NewDevProps.PairableMode        = ((Value != JNI_FALSE) ? TRUE : FALSE);
                     NewDevProps.PairableModeTimeout = 0;

                     /* Try performing the update. We return success if */
                     /* this works.                                     */
                     if((Result = DEVM_UpdateLocalDeviceProperties(DEVM_UPDATE_LOCAL_DEVICE_PROPERTIES_PAIRABLE_MODE, &NewDevProps)) == 0)
                        ret_val = JNI_TRUE;
                     else
                        SS1_LOGE("Unable to change Pairable Mode (%d)", Result);
                  }
                  else
                  {
                     /* The device is in the requested pairable mode.   */
                     /* BlueZ sends notifications of state change when  */
                     /* a change is requested, even if that state is    */
                     /* already set, so we need to simulate the event   */
                     /* callback.                                       */
                     SendDelayedPropertyChangedEvent(Env, Object, "Pairable", ((Value != JNI_FALSE) ? "true" : "false"), 100L);
                  }
               }
               else
               {
                  if(strcmp(PropKey, "Powered") == 0)
                  {
                     if(PreviousDevProps.ConnectableMode != ((Value != JNI_FALSE) ? TRUE : FALSE))
                     {
                        SS1_LOGI("Setting Powered (Connectable) Mode to %s", ((Value != JNI_FALSE) ? "TRUE" : "FALSE"));

                        /* Load the new connectable status into a Local */
                        /* Device Properties structure. The device      */
                        /* should stay connectable, so a timeout of zero*/
                        /* is used (infinite).                          */
                        NewDevProps.ConnectableMode        = ((Value != JNI_FALSE) ? TRUE : FALSE);
                        NewDevProps.ConnectableModeTimeout = 0;

                        /* Try performing the update. We return success */
                        /* if this works.                               */
                        if((Result = DEVM_UpdateLocalDeviceProperties(DEVM_UPDATE_LOCAL_DEVICE_PROPERTIES_CONNECTABLE_MODE, &NewDevProps)) == 0)
                           ret_val = JNI_TRUE;
                        else
                           SS1_LOGE("Unable to change Connectable Mode (%d)", Result);
                     }

                     /* In BlueZ, setting "Powered" will also affect    */
                     /* Discoverability.  Setting "Powered" to "true"   */
                     /* will reset Discoverability to the default state */
                     /* (the state which would be set automatically upon*/
                     /* stack startup). Setting "Powered" to "false"    */
                     /* will disable Discoverability. Here, we will     */
                     /* assume that the default state of Discoverability*/
                     /* is OFF, so we will unconditionally disable this */
                     /* mode.                                           */
                     if(PreviousDevProps.DiscoverableMode != FALSE)
                     {
                        SS1_LOGI("Setting Powered (Discoverable) Mode to FALSE");

                        NewDevProps.DiscoverableMode        = FALSE;
                        NewDevProps.DiscoverableModeTimeout = 0;

                        if((Result = DEVM_UpdateLocalDeviceProperties(DEVM_UPDATE_LOCAL_DEVICE_PROPERTIES_DISCOVERABLE_MODE, &NewDevProps)) == 0)
                           SS1_LOGE("Unable to change Discoverability Mode (%d)", Result);
                     }
                     else
                     {
                        /* The device is already in the requested          */
                        /* discoverable mode with the requested timeout.   */
                        /* BlueZ sends notifications of state change when  */
                        /* a change is requested, even if that state is    */
                        /* already set, so we need to simulate the event   */
                        /* callback.                                       */
                        SendDelayedPropertyChangedEvent(Env, Object, "Discoverable", "false", 100L);
                     }

                     if(Value == JNI_FALSE)
                     {
                        /* Android expects to receive a                 */
                        /* "Powered"="false" property update when the   */
                        /* device becomes non-connectable. When becoming*/
                        /* connectable, it expects only a "Discoverable"*/
                        /* property update, which is generated above.   */
                        SendDelayedPropertyChangedEvent(Env, Object, "Powered", "false", 100L);
                     }
                  }
                  else
                  {
                     /* The "Discoverable", "Pairable", and "Powered"   */
                     /* properties are the only boolean properties      */
                     /* available to update on the local adapter. This  */
                     /* function should not be called with any other    */
                     /* key.                                            */
                     SS1_LOGE("Unknown string property %s", PropKey);
                  }
               }
            }
         }

         Env->ReleaseStringUTFChars(Key, PropKey);
      }
   }

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(JNI_FALSE);
#endif
}

static jboolean BTS_SetAdapterPropertyIntegerNative(JNIEnv *Env, jobject Object, jstring Key, jint Value)
{
   SS1_LOGD("Enter (%p, %d)", Key, Value);
#ifdef HAVE_BLUETOOTH
   int                            Result;
   jboolean                       ret_val;
   const char                    *PropKey;
   BTS_NativeData_t              *NatData;
   DEVM_Local_Device_Properties_t NewDevProps;
   DEVM_Local_Device_Properties_t PreviousDevProps;

   ret_val = JNI_FALSE;

   /* Make sure the key seems valid.                                    */
   if(Key)
   {
      /* Obtain a character buffer containing the UTF-8 representation  */
      /* of the key.                                                    */
      PropKey = Env->GetStringUTFChars(Key, NULL);

      /* Check that the key buffer was allocated successfully.          */
      if(PropKey)
      {
         if(DEVM_QueryLocalDeviceProperties(&PreviousDevProps) == 0)
         {
            if(strcmp(PropKey, "DiscoverableTimeout") == 0)
            {
               /* DiscoverableTimeout is not maintaned by Bluetopia     */
               /* -- it has to be specified every time Discoverable     */
               /* mode is enabled. Therefore, we maintain it in our     */
               /* BTS_NativeData_t structure. It starts at a default    */
               /* value, set in BTS_InitializeNativeDataNative().       */
               if((NatData = (BTS_NativeData_t *)(Env->GetIntField(Object, Field_mNativeData))) != NULL)
               {
                  if(Value < 0)
                  {
                     SS1_LOGW("Received request to set DiscoverableTimeout to a negative value (%d)", Value);
                     Value = 120;
                  }
                  SS1_LOGI("Setting Discoverable Timeout to %d", Value);
                  NatData->DiscoverableTimeout = (unsigned)Value;
               }

               ret_val = JNI_TRUE;

               /* If we are currently discoverable, update the current  */
               /* timeout with the new value.                           */
               if(PreviousDevProps.DiscoverableMode == TRUE)
               {
                  NewDevProps.DiscoverableMode        = TRUE;
                  NewDevProps.DiscoverableModeTimeout = NatData->DiscoverableTimeout;

                  /* Try performing the update.                         */
                  if((Result = DEVM_UpdateLocalDeviceProperties(DEVM_UPDATE_LOCAL_DEVICE_PROPERTIES_DISCOVERABLE_MODE, &NewDevProps)) == 0)
                  {
                     /* Updating discoverability timeout always         */
                     /* generates a property update event from PM, so   */
                     /* nothing need be done in this case.              */
                  }
                  else
                  {
                     SS1_LOGE("Unable to update the Timeout for an active Discoverable Mode (%d)", Result);

                     /* The update failed. Leave discoverability mode   */
                     /* active with the existing timeout and fake an    */
                     /* event indicating that the timeout was updated.  */
                     /* The new value will, at least, be valid the next */
                     /* next time discoverability is enabled.           */
                     SendDelayedPropertyChangedEvent(Env, Object, "DiscoverableTimeout", NatData->DiscoverableTimeout, 100L);
                  }
               }
               else
               {
                  /* BlueZ sends notifications of state change when a   */
                  /* change is requested, regardless of the previous    */
                  /* state, so we need to simulate the event callback.  */
                  SendDelayedPropertyChangedEvent(Env, Object, "DiscoverableTimeout", NatData->DiscoverableTimeout, 100L);
               }
            }
            else
            {
               if(strcmp(PropKey, "Class") == 0)
               {
                  /* Load the new device class into a Local Device      */
                  /* Properties structure.                              */
                  ASSIGN_CLASS_OF_DEVICE(NewDevProps.ClassOfDevice, ((Value >> 16) & 0x0ff), ((Value >> 8) & 0x0ff), (Value & 0x0ff));

                  if(COMPARE_CLASS_OF_DEVICE(PreviousDevProps.ClassOfDevice, NewDevProps.ClassOfDevice) == FALSE)
                  {
                     SS1_LOGI("Setting Class Of Device to 0x%02x%02x%02x", NewDevProps.ClassOfDevice.Class_of_Device0, NewDevProps.ClassOfDevice.Class_of_Device1, NewDevProps.ClassOfDevice.Class_of_Device2);
                     /* Try performing the update. We return success if */
                     /* this works.                                     */
                     if(DEVM_UpdateLocalDeviceProperties(DEVM_UPDATE_LOCAL_DEVICE_PROPERTIES_CLASS_OF_DEVICE, &NewDevProps) == 0)
                        ret_val = JNI_TRUE;
                  }
                  else
                  {
                     /* BlueZ sends notifications of state change       */
                     /* when a change is requested, regardless of the   */
                     /* previous state, so we need to simulate the event*/
                     /* callback.                                       */
                     SendDelayedPropertyChangedEvent(Env, Object, "Class", (0x00ffffff & ((NewDevProps.ClassOfDevice.Class_of_Device2 << 16) | (NewDevProps.ClassOfDevice.Class_of_Device1 << 8) | (NewDevProps.ClassOfDevice.Class_of_Device0))), 100L);
                  }
               }
               else
               {
                  /* The "Class" and "DiscoverableTimeout" properties   */
                  /* are the only integer properties available to update*/
                  /* on the local adapter. This function should not be  */
                  /* called with any other key.                         */
                  SS1_LOGE("Unknown string property %s", PropKey);
               }
            }
         }

         /* Clean up JNI resources.                                     */
         Env->ReleaseStringUTFChars(Key, PropKey);
      }
   }

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(JNI_FALSE);
#endif
}

static jboolean BTS_SetDevicePropertyBooleanNative(JNIEnv *Env, jobject Object, jstring Path, jstring Key, jint Value)
{
   SS1_LOGD("Enter (%p, %p, %d)", Path, Key, Value);
#ifdef HAVE_BLUETOOTH
   jboolean                        ret_val;
   BD_ADDR_t                       BD_ADDR;
   const char                     *DevicePath;
   const char                     *PropKey;
   DEVM_Remote_Device_Properties_t RemoteDevProps;

   ret_val = JNI_FALSE;

   /* Make sure the key seems valid.                                    */
   if(Key)
   {
      /* Obtain a character buffer containing the UTF-8 representation  */
      /* of the device address and property key.                        */
      if((PropKey = Env->GetStringUTFChars(Key, NULL)) != NULL)
      {
         if((DevicePath = Env->GetStringUTFChars(Path, NULL)) != NULL)
         {
            if(strcmp(PropKey, "Trusted") == 0)
            {
               /* Bluetopia currently has no concept of "Trusted".      */
               /* It is a property that should be maintained at the     */
               /* application level, per-service. In order to maintain  */
               /* Android compatibility, though, we emulate this via the*/
               /* ApplicationData field of the remote device properties.*/
               PathToBD_ADDR(&BD_ADDR, DevicePath);
               if(DEVM_QueryRemoteDeviceProperties(BD_ADDR, FALSE, &RemoteDevProps) == 0)
               {
                  if(!(RemoteDevProps.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_APPLICATION_DATA_VALID))
                     memset(&(RemoteDevProps.ApplicationData), 0, sizeof(RemoteDevProps.ApplicationData));

                  if(Value)
                     RemoteDevProps.ApplicationData.ApplicationInfo |= REMOTE_DEVICE_APPLICATION_INFO_FLAG_TRUSTED;
                  else
                     RemoteDevProps.ApplicationData.ApplicationInfo &= ~REMOTE_DEVICE_APPLICATION_INFO_FLAG_TRUSTED;

                  if(DEVM_UpdateRemoteDeviceApplicationData(BD_ADDR, &(RemoteDevProps.ApplicationData)) == 0)
                     ret_val = JNI_TRUE;
               }
            }
            else
            {
               /* The "Class" and "DiscoverableTimeout" properties are  */
               /* the only integer properties available to update on the*/
               /* local adapter. This function should not be called with*/
               /* any other key.                                        */
               SS1_LOGE("Unknown boolean property %s", PropKey);
            }
            Env->ReleaseStringUTFChars(Path, DevicePath);
         }
         Env->ReleaseStringUTFChars(Key, PropKey);
      }
   }

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(JNI_FALSE);
#endif
}

#if (SS1_PLATFORM_SDK_VERSION >= 14)
static jboolean BTS_SetDevicePropertyStringNative(JNIEnv *Env, jobject Object, jstring Path, jstring Key, jstring Value)
{
   SS1_LOGD("Enter (%p, %p, %p)", Path, Key, Value);
#ifdef HAVE_BLUETOOTH
   jboolean                        ret_val;
   BD_ADDR_t                       BD_ADDR;
   const char                     *DevicePath;
   const char                     *PropKey;
   const char                     *PropValue;
   unsigned int                    NameLength;
   DEVM_Remote_Device_Properties_t RemoteDevProps;

   ret_val = JNI_FALSE;

   /* Make sure the key seems valid.                                    */
   if(Key)
   {
      /* Obtain a character buffer containing the UTF-8 representation  */
      /* of the device address and property key.                        */
      if((PropKey = Env->GetStringUTFChars(Key, NULL)) != NULL)
      {
         if((DevicePath = Env->GetStringUTFChars(Path, NULL)) != NULL)
         {
            if(strcmp(PropKey, "Alias") == 0)
            {
               /* The ApplicationData field of the Remote Device        */
               /* Properties must be set as a single entity. Therefore, */
               /* we first pull the current ApplicationData in order to */
               /* preserve the current values of other fields.          */
               PathToBD_ADDR(&BD_ADDR, DevicePath);
               if(DEVM_QueryRemoteDeviceProperties(BD_ADDR, FALSE, &RemoteDevProps) == 0)
               {
                  if(!(RemoteDevProps.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_APPLICATION_DATA_VALID))
                     memset(&(RemoteDevProps.ApplicationData), 0, sizeof(RemoteDevProps.ApplicationData));

                  if((PropValue = Env->GetStringUTFChars(Value, NULL)) != NULL)
                  {
                     NameLength = strlen(PropValue);
                     if(NameLength > MAX_NAME_LENGTH)
                        NameLength = MAX_NAME_LENGTH;

                     /* Erase the old name in case the new name is      */
                     /* shorter to avoid leaving behind the tail of the */
                     /* old name.                                       */
                     memset(RemoteDevProps.ApplicationData.FriendlyName, 0, sizeof(RemoteDevProps.ApplicationData.FriendlyName));

                     RemoteDevProps.ApplicationData.FriendlyNameLength = NameLength;

                     if(NameLength)
                     {
                        memcpy(RemoteDevProps.ApplicationData.FriendlyName, PropValue, NameLength);
                        RemoteDevProps.ApplicationData.FriendlyName[NameLength] = '\0';
                     }

                     if(DEVM_UpdateRemoteDeviceApplicationData(BD_ADDR, &(RemoteDevProps.ApplicationData)) == 0)
                        ret_val = JNI_TRUE;

                     Env->ReleaseStringUTFChars(Value, PropValue);
                  }
               }
            }
            else
            {
               /* The "Alias" property is the only string property      */
               /* available to update on the remote device. This        */
               /* function should not be called with any other key.     */
               SS1_LOGE("Unknown string property %s", PropKey);
            }

            Env->ReleaseStringUTFChars(Path, DevicePath);
         }

         Env->ReleaseStringUTFChars(Key, PropKey);
      }
   }

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(JNI_FALSE);
#endif
}
#endif

static jboolean BTS_StartDiscoveryNative(JNIEnv *Env, jobject Object)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   int                            Result;
   jboolean                       ret_val;
   DEVM_Local_Device_Properties_t LocalDevProps;

   ret_val = JNI_FALSE;

   if(DEVM_QueryLocalDeviceProperties(&LocalDevProps) == 0)
   {
      /* Start a device inquiry and name discovery process which will   */
      /* automatically end after BTS_DISCOVERY_LENGTH_SECONDS seconds.  */
      Result = DEVM_StartDeviceDiscovery(BTS_DISCOVERY_LENGTH_SECONDS);
      if((Result >= 0) || (Result == BTPM_ERROR_CODE_DEVICE_DISCOVERY_IN_PROGRESS))
         ret_val = JNI_TRUE;
      else
         SS1_LOGE("Unable to start BR/EDR Device Discovery (%d)", Result);

      /* If the local device supports LE Bluetooth communication, start */
      /* a low-energy scan.                                             */
      if(LocalDevProps.LocalDeviceFlags & DEVM_LOCAL_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY)
      {
         /* Start a device scan which will automatically end after      */
         /* BTS_DISCOVERY_LENGTH_SECONDS seconds.                       */
         Result = DEVM_StartDeviceScan(BTS_DISCOVERY_LENGTH_SECONDS);
         if((Result >= 0) || (Result == BTPM_ERROR_CODE_DEVICE_DISCOVERY_IN_PROGRESS))
            ret_val = JNI_TRUE;
         else
            SS1_LOGE("Unable to start LE Device Scan (%d)", Result);
      }
   }

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(JNI_FALSE);
#endif
}

static void BTS_StopDiscoveryNative(JNIEnv *Env, jobject Object)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   DEVM_Local_Device_Properties_t LocalDevProps;

   /* Only stop the discovery if we're running a discovery. The idea    */
   /* is to prevent the daemon from sending a Property Changed signal   */
   /* for the DISCOVERY_IN_PROGRESS flag unless it's really going       */
   /* from 1->0. The daemon sends the signal even if the "transition"   */
   /* is 0->0, which Android doesn't expect. For some reason, when      */
   /* it gets a "Discovering = 0" signal, it turns around and calls     */
   /* StopDiscovery again, even though there's no discovery to stop.    */
   /* This check works around the loop which that behavior causes.      */
   if(DEVM_QueryLocalDeviceProperties(&LocalDevProps) == 0)
   {
      /* Stop BR/EDR Device Discovery.                                  */
      if(LocalDevProps.LocalDeviceFlags & DEVM_LOCAL_DEVICE_FLAGS_DEVICE_DISCOVERY_IN_PROGRESS)
         DEVM_StopDeviceDiscovery();

      /* Stop LE Device Scan.                                           */
      if(LocalDevProps.LocalDeviceFlags & DEVM_LOCAL_DEVICE_FLAGS_LE_SCANNING_IN_PROGRESS)
         DEVM_StopDeviceScan();
   }
#endif
   SS1_LOGD("Exit");
}


   /* The following function implements the Java native method:         */
   /*    android.server.BluetoothService.createDeviceNative()           */
   /*                                                                   */
   /* This function is used to make the Bluetooth stack aware           */
   /* of a remote device without initiating a full inquiry (see         */
   /* BTS_StartDiscovery()). Given the remote device MAC address, this  */
   /* will initiate service discovery and name request processes against*/
   /* the device, if the remote device is within radio range and is     */
   /* accepting connections. This function returns true if it is able   */
   /* to communicate with the Bluetopia daemon. Otherwise, it returns   */
   /* false. The result of the device creation is indicated by a call to*/
   /* BluetoothEventLoop.onCreateDeviceResult().                        */
static jboolean BTS_CreateDeviceNative(JNIEnv *Env, jobject Object, jstring Address)
{
   SS1_LOGD("Enter (%p)", Address);
#ifdef HAVE_BLUETOOTH
   int                             Result;
   int                             AddDeviceResult;
   jobject                         EventLoopObject;
   jboolean                        ret_val;
   BD_ADDR_t                       BD_ADDR;
   const char                     *AddressString;
   Class_of_Device_t               ClassOfDevice;
   BTEL_NativeData_t              *EventLoopNatData;
   DEVM_Local_Device_Properties_t  LocalDevProps;

   ret_val = JNI_FALSE;

   if(Address)
   {
      if((AddressString = Env->GetStringUTFChars(Address, NULL)) != NULL)
      {
         if((EventLoopObject = Env->GetObjectField(Object, Field_mEventLoop)) != NULL)
         {
            if((EventLoopNatData = BTEL_GetNativeData(Env, EventLoopObject)) != NULL)
            {
               if(StrToBD_ADDR(&BD_ADDR, AddressString))
               {
                  ASSIGN_CLASS_OF_DEVICE(ClassOfDevice, 0, 0, 0);
                  // TODO Add support for Remote Device Application Data to implement "Trusted" flag (used by stock PBAP)
                  AddDeviceResult = DEVM_AddRemoteDevice(BD_ADDR, ClassOfDevice, NULL);
                  if(AddDeviceResult == 0)
                  {
                     SS1_LOGD("Added new device (%s) to PM's database.", AddressString);

                     BTEL_CallOnCreateDeviceResult(Env, EventLoopObject, Address, CREATE_DEVICE_SUCCESS);

                     if(pthread_mutex_lock(&(EventLoopNatData->PendingCreateDeviceMutex)) == 0)
                     {
                        /* If the list of outstanding device creation   */
                        /* requests is full, wait until it empties; but */
                        /* only for a limited amount of time.           */
                        if(EventLoopNatData->PendingCreateDeviceNumber == (sizeof(EventLoopNatData->PendingCreateDeviceList) / sizeof(EventLoopNatData->PendingCreateDeviceList[0])))
                           PthreadCondWaitMS(&(EventLoopNatData->PendingCreateDeviceRemovalCondition), &(EventLoopNatData->PendingCreateDeviceMutex), 20000);

                        /* Only add the device to the list of           */
                        /* outstanding device creation requests if the  */
                        /* list is not full.                            */
                        if(EventLoopNatData->PendingCreateDeviceNumber < (sizeof(EventLoopNatData->PendingCreateDeviceList) / sizeof(EventLoopNatData->PendingCreateDeviceList[0])))
                        {
                           EventLoopNatData->PendingCreateDeviceList[EventLoopNatData->PendingCreateDeviceNumber] = BD_ADDR;
                           EventLoopNatData->PendingCreateDeviceNumber += 1;

                           SS1_LOGD("Manually triggering SDP query against %s.", AddressString);

                           /* The SDP query will fail if we are         */
                           /* currently discovering devices. Stop BR/EDR*/
                           /* device discovery, but only if a discovery */
                           /* process is in progress.                   */
                           if((DEVM_QueryLocalDeviceProperties(&LocalDevProps) == 0) && (LocalDevProps.LocalDeviceFlags & DEVM_LOCAL_DEVICE_FLAGS_DEVICE_DISCOVERY_IN_PROGRESS))
                              DEVM_StopDeviceDiscovery();

                           Result = DEVM_QueryRemoteDeviceServices(BD_ADDR, TRUE, 0, NULL, NULL);
                           if((Result >= 0) || (Result == BTPM_ERROR_CODE_SERVICE_DISCOVERY_IN_PROGRESS))
                           {
                              /* The query was submitted successfully   */
                              /* or one was already in progress. Treat  */
                              /* both of these cases as successful since*/
                              /* the result will be the same from the   */
                              /* perspective of the upper API layers.   */
                              SS1_LOGD("Reporting success to BluetoothService.");

                              ret_val = JNI_TRUE;
                           }
                           else
                           {
                              /* The query failed to be submitted,      */
                              /* so reverse our modifications to the    */
                              /* pending list.                          */
                              SS1_LOGD("Reporting failure to BluetoothService (%d).", Result);

                              EventLoopNatData->PendingCreateDeviceNumber -= 1;
                              ret_val = JNI_FALSE;
                           }
                        }

                        pthread_mutex_unlock(&(EventLoopNatData->PendingCreateDeviceMutex));
                     }
                  }
                  else
                  {
                     if(AddDeviceResult == BTPM_ERROR_CODE_BLUETOOTH_DEVICE_ALREADY_EXISTS)
                     {
                        SS1_LOGD("Requested device (%s) already exists in PM's database.", AddressString);

                        ret_val = JNI_TRUE;
                        BTEL_CallOnCreateDeviceResult(Env, EventLoopObject, Address, CREATE_DEVICE_ALREADY_EXISTS);
                     }
                     else
                     {
                        SS1_LOGD("Error adding device (%s) to PM's database.", AddressString);

                        ret_val = JNI_TRUE;
                        BTEL_CallOnCreateDeviceResult(Env, EventLoopObject, Address, CREATE_DEVICE_FAILED);
                     }
                  }
               }
            }

            Env->DeleteLocalRef(EventLoopObject);
         }

         Env->ReleaseStringUTFChars(Address, AddressString);
      }
   }

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(JNI_FALSE);
#endif
}


   /* The following function implements the Java native method:         */
   /*    android.server.BluetoothService.createPairedDeviceNative()     */
   /*                                                                   */
   /* This function is used to initiate a bond with a remote device.    */
   /* The timeout parameter is ignored in this implementation.          */
   /* This function returns JNI_TRUE if it is able to successfully      */
   /* process the pairing request, or JNI_FALSE otherwise. Note that    */
   /* this return value is not related to the bonding process. The      */
   /* result of the bonding process is passed back via a callback       */
   /* to BluetoothEventLoop::onCreatePairingDeviceResult(). This        */
   /* callback may be called either from here (if the bonding fails     */
   /* immediately due to pre-existing conditions, like "already         */
   /* paired") or at some point in the future as a result of a          */
   /* detRemoteDeviceServicesStatus DEVM Event message. The result      */
   /* code passed to onCreatePairingDeviceResult will be one of the     */
   /* BOND_RESULT_* constants as defined by the BTEL module.            */
static jboolean BTS_CreatePairedDeviceNative(JNIEnv *Env, jobject Object, jstring Address, jint TimeoutMS)
{
   SS1_LOGD("Enter (%p, %d)", Address, TimeoutMS);
#ifdef HAVE_BLUETOOTH
   int                              PairResult;
   jobject                          EventLoopObject;
   jboolean                         ret_val;
   BD_ADDR_t                        BD_ADDR;
   const char                      *AddressString;
   DEVM_Local_Device_Properties_t   LocalDevProps;
   DEVM_Remote_Device_Properties_t  RemoteDevProps;

   ret_val = JNI_FALSE;

   if(Address)
   {
      if((AddressString = Env->GetStringUTFChars(Address, NULL)) != NULL)
      {
         if(StrToBD_ADDR(&BD_ADDR, AddressString))
         {
            if((DEVM_QueryLocalDeviceProperties(&LocalDevProps) == 0) && (LocalDevProps.LocalDeviceFlags & DEVM_LOCAL_DEVICE_FLAGS_DEVICE_DISCOVERY_IN_PROGRESS))
            {
               SendDelayedCreatePairedDeviceResult(Env, Object, Address, BOND_RESULT_DISCOVERY_IN_PROGRESS, 1000L);
               ret_val = JNI_TRUE;
            }
            else
            {
               if(DEVM_QueryRemoteDeviceProperties(BD_ADDR, DEVM_QUERY_REMOTE_DEVICE_PROPERTIES_FLAGS_LOW_ENERGY, &RemoteDevProps) == 0)
               {
                  if((RemoteDevProps.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_LOW_ENERGY) && (!(RemoteDevProps.RemoteDeviceFlags & DEVM_REMOTE_DEVICE_FLAGS_DEVICE_SUPPORTS_BR_EDR)))
                  {
                     /* This device only supports LE, so default to an  */
                     /* LE-style pairing.                               */
                     PairResult = DEVM_PairWithRemoteDevice(BD_ADDR, DEVM_PAIR_WITH_REMOTE_DEVICE_FLAGS_LOW_ENERGY);
                  }
                  else
                  {
                     /* This device supports BR/EDR, so default to this */
                     /* connection type since this is the only type     */
                     /* supported natively by Android.                  */
                     PairResult = DEVM_PairWithRemoteDevice(BD_ADDR, 0);
                  }
               }
               else
               {
                  /* This device is unknown to the stack, so assume that*/
                  /* it supports BR/EDR since this is the only type     */
                  /* supported natively by Android.                     */
                  PairResult = DEVM_PairWithRemoteDevice(BD_ADDR, 0);
               }

               switch(PairResult)
               {
                  case 0:
                     SS1_LOGI("Pairing process initiated successfully");
                     ret_val = JNI_TRUE;
                     break;

                  case BTPM_ERROR_CODE_BLUETOOTH_DEVICE_ALREADY_PAIRED:
                     SS1_LOGI("Remote device already paired");
                     ret_val = JNI_TRUE;

                     SS1_LOGI("Sending success notification via delayed callback");
                     SendDelayedCreatePairedDeviceResult(Env, Object, Address, BOND_RESULT_SUCCESS, 1000L);
                     break;

                  default:
                     SS1_LOGW("Pairing attempt failed immediately with code (%d)", PairResult);
                     ret_val = JNI_FALSE;
                     break;
               }
            }
         }

         Env->ReleaseStringUTFChars(Address, AddressString);
      }
   }

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(JNI_FALSE);
#endif
}


   /* The following function implements the Java native method:         */
   /*    android.server.BluetoothService.cancelDeviceCreationNative()   */
   /*                                                                   */
   /* This function is used to cancel a pending bond attempt that was   */
   /* initiated locally. This function returns JNI_TRUE on success, or  */
   /* JNI_FALSE on error.                                               */
static jboolean BTS_CancelDeviceCreationNative(JNIEnv *Env, jobject Object, jstring Address)
{
   SS1_LOGD("Enter (%p)", Address);
#ifdef HAVE_BLUETOOTH
   /* This function is currently only called from                       */
   /* BluetoothService::cancelBondProcess(), so all we need to do is    */
   /* cancel any pending bond.                                          */

   jboolean    ret_val;
   BD_ADDR_t   BD_ADDR;
   const char *AddressString;

   ret_val = JNI_FALSE;

   if(Address)
   {
      if((AddressString = Env->GetStringUTFChars(Address, NULL)) != NULL)
      {
         if(StrToBD_ADDR(&BD_ADDR, AddressString))
            if(DEVM_CancelPairWithRemoteDevice(BD_ADDR) == 0)
               ret_val = JNI_TRUE;

         Env->ReleaseStringUTFChars(Address, AddressString);
      }
   }

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(JNI_FALSE);
#endif
}

   /* The following function implements the Java native method:         */
   /*    android.server.BluetoothService.removeDeviceNative()           */
   /*                                                                   */
   /* This function is used to remove the link key, if it exists,       */
   /* representing a Bonded/Paired relationship with this device. Any   */
   /* active connections which are authenticated and/or encrypted are   */
   /* closed. The function returns JNI_TRUE is sucessful, or JNI_FALSE  */
   /* otherwise.                                                        */
   /*                                                                   */
   /* TODO: This function should also disconnect any active             */
   /* connections which require authentication or encryption,           */
   /* as these states require a valid link key. Currently, the          */
   /* BluetopiaPM API does not support this. As a workaround, we use    */
   /* DEVM_DisconnectRemoteDevice(), though this function causes ALL    */
   /* connections to be dropped uncleanly (client applications cannot   */
   /* tell whether the connection was closed locally or the remote      */
   /* device went out of range and should be reconnected.               */
static jboolean BTS_RemoveDeviceNative(JNIEnv *Env, jobject Object, jstring Path)
{
   SS1_LOGD("Enter (%p)", Path);
#ifdef HAVE_BLUETOOTH
   int         Result;
   jboolean    ret_val;
   BD_ADDR_t   BD_ADDR;
   const char *PathString;

   ret_val = JNI_FALSE;

   if(Path)
   {
      if((PathString = Env->GetStringUTFChars(Path, NULL)) != NULL)
      {
         if(PathToBD_ADDR(&BD_ADDR, PathString))
         {
            /* Attempt to unpair a BR/EDR device.                       */
            if((Result = DEVM_UnPairRemoteDevice(BD_ADDR, 0)) == BTPM_ERROR_CODE_DEVICE_PAIRING_IN_PROGRESS)
            {
               /* Cannot unpair because the device is currently being   */
               /* paired. Attempt to cancel the pairing process.        */
               if(DEVM_CancelPairWithRemoteDevice(BD_ADDR) == BTPM_ERROR_CODE_DEVICE_NOT_CURRENTLY_IN_PAIRING_PROCESS)
               {
                  /* The pairing process was not active at the time of  */
                  /* the cancel request. It is possible that the pairing*/
                  /* attempt finished before the cancel request was     */
                  /* processed, so attempt to unpair one last time.     */
                  DEVM_UnPairRemoteDevice(BD_ADDR, 0);
               }

               /* Whether cancelled or unpaired, we can now consider the*/
               /* device to not be paired.                              */
               Result = 0;
            }

            if(!Result)
            {
               SS1_LOGI("Unpaired remote BR/EDR device (%s)", PathString);
               if((Result = DEVM_DisconnectRemoteDevice(BD_ADDR, DEVM_DISCONNECT_FROM_REMOTE_DEVICE_FLAGS_FORCE)) == 0)
               {
                  SS1_LOGI("Disconnected remote BR/EDR device (%s)", PathString);
                  ret_val = JNI_TRUE;
               }
               else
                  SS1_LOGE("Failed to disconnect remote BR/EDR device (%s) (Reason: %d). This is not necessarily an error.", PathString, Result);
            }
            else
               SS1_LOGE("Failed to unpair remote BR/EDR device (%s) (Reason: %d). This is not necessarily an error.", PathString, Result);

            /* Attempt to unpair an LE device.                          */
            if((Result = DEVM_UnPairRemoteDevice(BD_ADDR, DEVM_UNPAIR_REMOTE_DEVICE_FLAGS_LOW_ENERGY)) == BTPM_ERROR_CODE_DEVICE_PAIRING_IN_PROGRESS)
            {
               /* Cannot unpair because the device is currently being   */
               /* paired. Attempt to cancel the pairing process.        */
               if(DEVM_CancelPairWithRemoteDevice(BD_ADDR) == BTPM_ERROR_CODE_DEVICE_NOT_CURRENTLY_IN_PAIRING_PROCESS)
               {
                  /* The pairing process was not active at the time of  */
                  /* the cancel request. It is possible that the pairing*/
                  /* attempt finished before the cancel request was     */
                  /* processed, so attempt to unpair one last time.     */
                  DEVM_UnPairRemoteDevice(BD_ADDR, DEVM_UNPAIR_REMOTE_DEVICE_FLAGS_LOW_ENERGY);
               }

               /* Whether cancelled or unpaired, we can now consider the*/
               /* device to not be paired.                              */
               Result = 0;
            }

            if(!Result)
            {
               SS1_LOGI("Unpaired remote LE device (%s)", PathString);
               if((Result = DEVM_DisconnectRemoteDevice(BD_ADDR, (DEVM_DISCONNECT_FROM_REMOTE_DEVICE_FLAGS_FORCE | DEVM_DISCONNECT_FROM_REMOTE_DEVICE_FLAGS_LOW_ENERGY))) == 0)
               {
                  SS1_LOGI("Disconnected remote LE device (%s)", PathString);

                  /* If at least one type of bond was un-paired         */
                  /* successfully, consider the attempt successful.     */
                  ret_val = JNI_TRUE;
               }
               else
                  SS1_LOGE("Failed to disconnect remote LE device (%s) (Reason: %d). This is not necessarily an error.", PathString, Result);
            }
            else
               SS1_LOGE("Failed to unpair remote LE device (%s) (Reason: %d). This is not necessarily an error.", PathString, Result);
         }

         Env->ReleaseStringUTFChars(Path, PathString);
      }
      else
         SS1_LOGE("out of resources");
   }
   else
      SS1_LOGE("invalid parameters");

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(JNI_FALSE);
#endif
}

   /* The following function implements the Java native method:         */
   /*    android.server.BluetoothService.setPairingConfirmationNative() */
   /*                                                                   */
   /* This function is used to respond to a Pairing Confirmation Request*/
   /* event. The fourth parameter shall contain the same value passed to*/
   /* BluetoothEventLoop.onRequestPasskeyConfirmation() when the request*/
   /* event was received. In this implementation, that is a pointer     */
   /* to a DEVM_Authentication_Information_t structure representing     */
   /* the request which will also be used to format the response. This  */
   /* function returns JNI_TRUE on success or JNI_FALSE on error.       */
static jboolean BTS_SetPairingConfirmationNative(JNIEnv *Env, jobject Object, jstring Address, jboolean Confirm, int CallbackParam)
{
   SS1_LOGD("Enter (%p, %d, %d)", Address, Confirm, CallbackParam);
#ifdef HAVE_BLUETOOTH
   char                               AddressStringBuffer[64];
   jobject                            EventLoopObject;
   jboolean                           ret_val;
   BD_ADDR_t                          BD_ADDR;
   const char                        *AddressString;
   BTS_NativeData_t                  *NatData;
   BTEL_NativeData_t                 *EventLoopNatData;
   DEVM_Authentication_Information_t *AuthInfo;

   ret_val = JNI_FALSE;

   if(Address && CallbackParam)
   {
      AuthInfo = (DEVM_Authentication_Information_t *)CallbackParam;

      if((NatData = (BTS_NativeData_t *)(Env->GetIntField(Object, Field_mNativeData))) != NULL)
      {
         if(NatData->AuthCallbackID > 0)
         {
            /* Convert Address to BD_ADDR format for comparison.        */
            memset(&BD_ADDR, 0, sizeof(BD_ADDR));
            if((AddressString = Env->GetStringUTFChars(Address, NULL)) != NULL)
            {
               StrToBD_ADDR(&BD_ADDR, AddressString);
               strncpy(AddressStringBuffer, AddressString, sizeof(AddressStringBuffer));
               Env->ReleaseStringUTFChars(Address, AddressString);
            }

            /* Make sure this function is being called for the same     */
            /* remote device that the original authentication request   */
            /* came from.                                               */
            if(COMPARE_BD_ADDR(BD_ADDR, AuthInfo->BD_ADDR))
            {
               /* If everything checks out, send the response.          */
               switch(AuthInfo->AuthenticationAction & DEVM_AUTHENTICATION_ACTION_AUTHENTICATION_ACTION_MASK)
               {
                  case DEVM_AUTHENTICATION_ACTION_USER_CONFIRMATION_REQUEST:
                     /* If everything checks out, send the response.    */
                     if((AuthInfo->AuthenticationAction & DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK) && (AuthInfo->AuthenticationDataLength == 0))
                     {
                        /* User Confirmation under LE always involves   */
                        /* user consent because there is no passkey to  */
                        /* be compared. Just ask the user if they accept*/
                        /* the pairing.                                 */
                        SS1_LOGD("LE authentication user confirmation -> consent response: %s", ((Confirm == JNI_FALSE) ? "reject" : "accept"));
                        AuthInfo->AuthenticationAction            = (DEVM_AUTHENTICATION_ACTION_USER_CONFIRMATION_RESPONSE | DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK);
                        AuthInfo->AuthenticationDataLength        = sizeof(AuthInfo->AuthenticationData.Confirmation);
                        AuthInfo->AuthenticationData.Confirmation = ((Confirm == JNI_FALSE) ? FALSE : TRUE);

                        ret_val = ((DEVM_AuthenticationResponse(NatData->AuthCallbackID, AuthInfo) > 0) ? JNI_TRUE : JNI_FALSE);
                     }
                     else
                     {
                        if((!(AuthInfo->AuthenticationAction & DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK)) && (AuthInfo->AuthenticationDataLength == sizeof(AuthInfo->AuthenticationData.UserConfirmationRequestData)))
                        {
                           switch(AuthInfo->AuthenticationData.UserConfirmationRequestData.IOCapabilities.IO_Capability)
                           {
                              case icDisplayOnly:
                              case icDisplayYesNo:
                                 /* We requested passkey confirmation,  */
                                 /* which calls for a User Confirmation */
                                 /* response.                           */
                                 SS1_LOGD("Authentication user confirmation -> confirmation response: %s", ((Confirm == JNI_FALSE) ? "reject" : "accept"));
                                 AuthInfo->AuthenticationAction            = DEVM_AUTHENTICATION_ACTION_USER_CONFIRMATION_RESPONSE;
                                 AuthInfo->AuthenticationDataLength        = sizeof(AuthInfo->AuthenticationData.Confirmation);
                                 AuthInfo->AuthenticationData.Confirmation = ((Confirm == JNI_FALSE) ? FALSE : TRUE);

                                 ret_val = ((DEVM_AuthenticationResponse(NatData->AuthCallbackID, AuthInfo) > 0) ? JNI_TRUE : JNI_FALSE);
                                 break;
                              case icKeyboardOnly:
                                 /* We requested that the passkey only  */
                                 /* be displayed. Android still submits */
                                 /* a User Confirmation to confirm      */
                                 /* that the passkey was successfully   */
                                 /* displayed.                          */
                                 SS1_LOGD("Authentication user confirmation -> display request acknowledged.");
                                 AuthInfo->AuthenticationAction            = DEVM_AUTHENTICATION_ACTION_USER_CONFIRMATION_RESPONSE;
                                 AuthInfo->AuthenticationDataLength        = sizeof(AuthInfo->AuthenticationData.Confirmation);
                                 AuthInfo->AuthenticationData.Confirmation = ((Confirm == JNI_FALSE) ? FALSE : TRUE);

                                 ret_val = ((DEVM_AuthenticationResponse(NatData->AuthCallbackID, AuthInfo) > 0) ? JNI_TRUE : JNI_FALSE);
                                 break;
                              case icNoInputNoOutput:
                                 /* We requested pairing consent, which */
                                 /* which calls for a User Confirmation */
                                 /* response.                           */
                                 SS1_LOGD("Authentication user confirmation -> consent response: %s", ((Confirm == JNI_FALSE) ? "reject" : "accept"));
                                 AuthInfo->AuthenticationAction            = DEVM_AUTHENTICATION_ACTION_USER_CONFIRMATION_RESPONSE;
                                 AuthInfo->AuthenticationDataLength        = sizeof(AuthInfo->AuthenticationData.Confirmation);
                                 AuthInfo->AuthenticationData.Confirmation = ((Confirm == JNI_FALSE) ? FALSE : TRUE);

                                 ret_val = ((DEVM_AuthenticationResponse(NatData->AuthCallbackID, AuthInfo) > 0) ? JNI_TRUE : JNI_FALSE);
                                 break;
                           }
                        }
                        else
                        {
                           /* This authentication request is malformed. */
                           /* Reject it immediately.                    */
                           SS1_LOGE("Authentication request was malformed and should have already been rejected. Rejecting now.");
                           AuthInfo->AuthenticationAction            = (DEVM_AUTHENTICATION_ACTION_USER_CONFIRMATION_RESPONSE | (AuthInfo->AuthenticationAction & DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK));
                           AuthInfo->AuthenticationDataLength        = sizeof(AuthInfo->AuthenticationData.Confirmation);
                           AuthInfo->AuthenticationData.Confirmation = FALSE;

                           ret_val = ((DEVM_AuthenticationResponse(NatData->AuthCallbackID, AuthInfo) > 0) ? JNI_TRUE : JNI_FALSE);
                        }
                     }
                     break;
                  case DEVM_AUTHENTICATION_ACTION_PASSKEY_INDICATION:
                     /* The Passkey Indication event requires no        */
                     /* response, but Android still calls this method   */
                     /* to indicate that the passkey was successfully   */
                     /* displayed.                                      */
                     SS1_LOGD("Authentication passkey indication -> display request acknowledged.");
                     ret_val = JNI_TRUE;
                     break;
                  default:
                     /* No other authentication methods should terminate*/
                     /* here. Log this error.                           */
                     SS1_LOGE("Unexpected authentication method for this response type (%d)", (AuthInfo->AuthenticationAction & DEVM_AUTHENTICATION_ACTION_AUTHENTICATION_ACTION_MASK));
                     ret_val = JNI_FALSE;
                     break;
               }

               /* Attempt to add this device to the state list for      */
               /* pending pair attempts. This will start a timeout on   */
               /* the reply which will clean up for us if the remote    */
               /* device neither confirms the bond nor disconnects on   */
               /* failure. If we fail to add the device to the list,    */
               /* things will eventually clean up when tne remote device*/
               /* disconnects. Such a failure may be indicative of      */
               /* larger problems, though.                              */
               if((EventLoopObject = Env->GetObjectField(Object, Field_mEventLoop)) != NULL)
               {
                  if((EventLoopNatData = BTEL_GetNativeData(Env, EventLoopObject)) != NULL)
                  {
                     if(BTEL_AddPendingPairDevice(EventLoopNatData, BD_ADDR, (!!(AuthInfo->AuthenticationAction & DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK))))
                        SS1_LOGI("Device %s added to Pending Pair list", AddressStringBuffer);
                     else
                        SS1_LOGI("Device %s could not be added to Pending Pair list", AddressStringBuffer);
                  }

                  Env->DeleteLocalRef(EventLoopObject);
               }
            }
            else
               SS1_LOGE("Remote device address mismatch");
         }
         else
            SS1_LOGE("response provided but we are not the authentication handler for the stack");
      }
      else
         SS1_LOGE("Object not initialized");

      /* Clean up the Authentication Information structure which was    */
      /* passed upstream during the DEVM Authentication callback        */
      /* processing in BTEL_DEVMAuthCallback().                         */
      free(AuthInfo);
   }
   else
      SS1_LOGE("invalid parameters");

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(JNI_FALSE);
#endif
}

   /* The following function implements the Java native method:         */
   /*    android.server.BluetoothService.setPasskeyNative()             */
   /*                                                                   */
   /* This function is used to respond to a Passkey Request event.      */
   /* The fourth parameter shall contain the same value passed to       */
   /* BluetoothEventLoop.onRequestPasskey() when the request event      */
   /* was received. In this implementation, that is a pointer to a      */
   /* DEVM_Authentication_Information_t structure representing the      */
   /* request which will also be used to format the response. This      */
   /* function returns JNI_TRUE on success or JNI_FALSE on error.       */
static jboolean BTS_SetPasskeyNative(JNIEnv *Env, jobject Object, jstring Address, int Passkey, int CallbackParam)
{
   SS1_LOGD("Enter (%p, %d, %d)", Address, Passkey, CallbackParam);
#ifdef HAVE_BLUETOOTH
   char                               AddressStringBuffer[64];
   jobject                            EventLoopObject;
   jboolean                           ret_val;
   BD_ADDR_t                          BD_ADDR;
   const char                        *AddressString;
   BTS_NativeData_t                  *NatData;
   BTEL_NativeData_t                 *EventLoopNatData;
   DEVM_Authentication_Information_t *AuthInfo;

   ret_val = JNI_FALSE;

   if(Address && CallbackParam)
   {
      AuthInfo = (DEVM_Authentication_Information_t *)CallbackParam;

      if((NatData = (BTS_NativeData_t *)(Env->GetIntField(Object, Field_mNativeData))) != NULL)
      {
         if(NatData->AuthCallbackID > 0)
         {
            /* Convert Address to BD_ADDR format for comparison.        */
            memset(&BD_ADDR, 0, sizeof(BD_ADDR));
            if((AddressString = Env->GetStringUTFChars(Address, NULL)) != NULL)
            {
               StrToBD_ADDR(&BD_ADDR, AddressString);
               strncpy(AddressStringBuffer, AddressString, sizeof(AddressStringBuffer));
               Env->ReleaseStringUTFChars(Address, AddressString);
            }

            /* Make sure this function is being called for the same     */
            /* remote device that the original authentication request   */
            /* came from.                                               */
            if(COMPARE_BD_ADDR(BD_ADDR, AuthInfo->BD_ADDR))
            {
               /* If everything checks out, send the response.          */
               AuthInfo->AuthenticationAction       = (DEVM_AUTHENTICATION_ACTION_PASSKEY_RESPONSE | (AuthInfo->AuthenticationAction & DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK));
               AuthInfo->AuthenticationDataLength   = sizeof(AuthInfo->AuthenticationData.Passkey);
               AuthInfo->AuthenticationData.Passkey = Passkey;

               ret_val = ((DEVM_AuthenticationResponse(NatData->AuthCallbackID, AuthInfo) > 0) ? JNI_TRUE : JNI_FALSE);

               /* Attempt to add this device to the state list for      */
               /* pending pair attempts. This will start a timeout on   */
               /* the reply which will clean up for us if the remote    */
               /* device neither confirms the bond nor disconnects on   */
               /* failure. If we fail to add the device to the list,    */
               /* things will eventually clean up when tne remote device*/
               /* disconnects. Such a failure may be indicative of      */
               /* larger problems, though.                              */
               if((EventLoopObject = Env->GetObjectField(Object, Field_mEventLoop)) != NULL)
               {
                  if((EventLoopNatData = BTEL_GetNativeData(Env, EventLoopObject)) != NULL)
                  {
                     if(BTEL_AddPendingPairDevice(EventLoopNatData, BD_ADDR, (!!(AuthInfo->AuthenticationAction & DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK))))
                        SS1_LOGI("Device %s added to Pending Pair list", AddressStringBuffer);
                     else
                        SS1_LOGI("Device %s could not be added to Pending Pair list", AddressStringBuffer);
                  }

                  Env->DeleteLocalRef(EventLoopObject);
               }
            }
            else
               SS1_LOGE("Remote device address mismatch");
         }
         else
            SS1_LOGE("response provided but we are not the authentication handler for the stack");
      }
      else
         SS1_LOGE("Object not initialized");

      /* Clean up the Authentication Information structure which was    */
      /* passed upstream during the DEVM Authentication callback        */
      /* processing in BTEL_DEVMAuthCallback().                         */
      free(AuthInfo);
   }
   else
      SS1_LOGE("invalid parameters");

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(JNI_FALSE);
#endif
}

   /* The following function implements the Java native method:         */
   /*    android.server.BluetoothService.setPinNative()                 */
   /*                                                                   */
   /* This function is used to respond to a PIN Code Request event.     */
   /* The fourth parameter shall contain the same value passed to       */
   /* BluetoothEventLoop.onRequestPincode() when the request event      */
   /* was received. In this implementation, that is a pointer to a      */
   /* DEVM_Authentication_Information_t structure representing the      */
   /* request which will also be used to format the response. This      */
   /* function returns JNI_TRUE on success or JNI_FALSE on error.       */
static jboolean BTS_SetPinNative(JNIEnv *Env, jobject Object, jstring Address, jstring Pin, int CallbackParam)
{
   SS1_LOGD("Enter (%p, %p, %d)", Address, Pin, CallbackParam);
#ifdef HAVE_BLUETOOTH
   char                               AddressStringBuffer[64];
   jobject                            EventLoopObject;
   jboolean                           ret_val;
   BD_ADDR_t                          BD_ADDR;
   const char                        *AddressString;
   const char                        *PINString;
   PIN_Code_t                         PINCode;
   unsigned int                       PINCodeLength;
   BTS_NativeData_t                  *NatData;
   BTEL_NativeData_t                 *EventLoopNatData;
   DEVM_Authentication_Information_t *AuthInfo;

   ret_val = JNI_FALSE;

   if(Address && CallbackParam && Pin)
   {
      AuthInfo = (DEVM_Authentication_Information_t *) CallbackParam;

      if((NatData = (BTS_NativeData_t *)(Env->GetIntField(Object, Field_mNativeData))) != NULL)
      {
         if(NatData->AuthCallbackID > 0)
         {
            if((PINString = Env->GetStringUTFChars(Pin, NULL)) != NULL)
            {
               memset(&PINCode, 0, sizeof(PINCode));
               if((PINCodeLength = strlen(PINString)) > sizeof(PINCode))
                  PINCodeLength = sizeof(PINCode);
               memcpy(&PINCode, PINString, PINCodeLength);
               Env->ReleaseStringUTFChars(Pin, PINString);

               /* Convert Address to BD_ADDR format for comparison.     */
               memset(&BD_ADDR, 0, sizeof(BD_ADDR));
               if((AddressString = Env->GetStringUTFChars(Address, NULL)) != NULL)
               {
                  StrToBD_ADDR(&BD_ADDR, AddressString);
                  strncpy(AddressStringBuffer, AddressString, sizeof(AddressStringBuffer));
                  Env->ReleaseStringUTFChars(Address, AddressString);
               }

               /* Make sure this function is being called for the       */
               /* same remote device that the original authentication   */
               /* request came from.                                    */
               if(COMPARE_BD_ADDR(BD_ADDR, AuthInfo->BD_ADDR))
               {
                  /* If everything checks out, send the response.       */
                  AuthInfo->AuthenticationAction       = DEVM_AUTHENTICATION_ACTION_PIN_CODE_RESPONSE;
                  AuthInfo->AuthenticationDataLength   = PINCodeLength;
                  AuthInfo->AuthenticationData.PINCode = PINCode;

                  ret_val = ((DEVM_AuthenticationResponse(NatData->AuthCallbackID, AuthInfo) > 0) ? JNI_TRUE : JNI_FALSE);

                  /* Attempt to add this device to the state list for   */
                  /* pending pair attempts. This will start a timeout on*/
                  /* the reply which will clean up for us if the remote */
                  /* device neither confirms the bond nor disconnects   */
                  /* on failure. If we fail to add the device to the    */
                  /* list, things will eventually clean up when tne     */
                  /* remote device disconnects. Such a failure may be   */
                  /* indicative of larger problems, though.             */
                  if((EventLoopObject = Env->GetObjectField(Object, Field_mEventLoop)) != NULL)
                  {
                     if((EventLoopNatData = BTEL_GetNativeData(Env, EventLoopObject)) != NULL)
                     {
                        if(BTEL_AddPendingPairDevice(EventLoopNatData, BD_ADDR, (!!(AuthInfo->AuthenticationAction & DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK))))
                           SS1_LOGI("Device %s added to Pending Pair list", AddressStringBuffer);
                        else
                           SS1_LOGI("Device %s could not be added to Pending Pair list", AddressStringBuffer);
                     }
                     Env->DeleteLocalRef(EventLoopObject);
                  }
               }
               else
                  SS1_LOGE("Remote device address mismatch");
            }
            else
               SS1_LOGE("Unable to obtain PIN data");
         }
         else
            SS1_LOGE("response provided but we are not the authentication handler for the stack");
      }
      else
         SS1_LOGE("Object not initialized");

      /* Clean up the Authentication Information structure which was    */
      /* passed upstream during the DEVM Authentication callback        */
      /* processing in BTEL_DEVMAuthCallback().                         */
      free(AuthInfo);
   }
   else
      SS1_LOGE("invalid parameters");

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(JNI_FALSE);
#endif
}

   /* The following function implements the Java native method:         */
   /*    android.server.BluetoothService.cancelPairingUserInputNative() */
   /*                                                                   */
   /* This function is used to cancel a pending PIN Code, Passkey, or   */
   /* Passkey Confirmation request. The third parameter shall contain   */
   /* the same value passed to BluetoothEventLoop.onRequestPincode(),   */
   /* BluetoothEventLoop.onRequestPasskey(), or                         */
   /* BluetoothEventLoop.onRequestPasskeyConfirmation() when the request*/
   /* event was received. In this implementation, that is a pointer     */
   /* to a DEVM_Authentication_Information_t structure representing     */
   /* the request which will also be used to format the response. This  */
   /* function returns JNI_TRUE on success or JNI_FALSE on error.       */
static jboolean BTS_CancelPairingUserInputNative(JNIEnv *Env, jobject Object, jstring Address, int CallbackParam)
{
   SS1_LOGD("Enter (%p, %d)", Address, CallbackParam);
#ifdef HAVE_BLUETOOTH
   jboolean                           ret_val;
   BD_ADDR_t                          BD_ADDR;
   const char                        *AddressString;
   BTS_NativeData_t                  *NatData;
   DEVM_Remote_Device_Properties_t    RemoteDevProps;
   DEVM_Authentication_Information_t *AuthInfo;

   ret_val = JNI_FALSE;

   if(Address && CallbackParam)
   {
      AuthInfo = (DEVM_Authentication_Information_t *) CallbackParam;

      if((NatData = (BTS_NativeData_t *)(Env->GetIntField(Object, Field_mNativeData))) != NULL)
      {
         if(NatData->AuthCallbackID > 0)
         {
            /* Convert Address to BD_ADDR format for comparison.        */
            memset(&BD_ADDR, 0, sizeof(BD_ADDR));
            if((AddressString = Env->GetStringUTFChars(Address, NULL)) != NULL)
            {
               StrToBD_ADDR(&BD_ADDR, AddressString);
               Env->ReleaseStringUTFChars(Address, AddressString);
            }

            /* Make sure this function is being called for the same     */
            /* remote device that the original authentication request   */
            /* came from.                                               */
            if(COMPARE_BD_ADDR(BD_ADDR, AuthInfo->BD_ADDR))
            {
               /* If everything checks out, send the response.          */

               /* First, make sure we send the right response given the */
               /* pending request type.                                 */
               switch(AuthInfo->AuthenticationAction & DEVM_AUTHENTICATION_ACTION_AUTHENTICATION_ACTION_MASK)
               {
                  case DEVM_AUTHENTICATION_ACTION_PIN_CODE_REQUEST:
                     AuthInfo->AuthenticationAction = DEVM_AUTHENTICATION_ACTION_PIN_CODE_RESPONSE;
                     break;
                  case DEVM_AUTHENTICATION_ACTION_USER_CONFIRMATION_REQUEST:
                     AuthInfo->AuthenticationAction = (DEVM_AUTHENTICATION_ACTION_USER_CONFIRMATION_RESPONSE | (AuthInfo->AuthenticationAction & DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK));
                     break;
                  case DEVM_AUTHENTICATION_ACTION_PASSKEY_REQUEST:
                     AuthInfo->AuthenticationAction = (DEVM_AUTHENTICATION_ACTION_PASSKEY_RESPONSE | (AuthInfo->AuthenticationAction & DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK));
                     break;
                  case DEVM_AUTHENTICATION_ACTION_OUT_OF_BAND_DATA_REQUEST:
                     AuthInfo->AuthenticationAction = (DEVM_AUTHENTICATION_ACTION_OUT_OF_BAND_DATA_RESPONSE | (AuthInfo->AuthenticationAction & DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK));
                     break;
                  case DEVM_AUTHENTICATION_ACTION_IO_CAPABILITIES_REQUEST:
                     AuthInfo->AuthenticationAction = (DEVM_AUTHENTICATION_ACTION_IO_CAPABILITIES_RESPONSE | (AuthInfo->AuthenticationAction & DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK));
                     break;
                  default:
                     AuthInfo->AuthenticationAction = 0;
               }

               /* Only send the response if the cancel request was made */
               /* on an actual pending request. Some messages don't     */
               /* expect responses and cancelling on them would is an   */
               /* undefined operation.                                  */
               if(AuthInfo->AuthenticationAction > 0)
               {
                  /* A data length of zero indicates a cancellation.    */
                  /* Clear the data field, too, just to avoid any       */
                  /* confusion.                                         */
                  AuthInfo->AuthenticationDataLength = 0;
                  memset(&(AuthInfo->AuthenticationData), 0, sizeof(AuthInfo->AuthenticationData));
                  ret_val = ((DEVM_AuthenticationResponse(NatData->AuthCallbackID, AuthInfo) > 0) ? JNI_TRUE : JNI_FALSE);

                  /* Since the pairing operation was explicitly         */
                  /* cancelled by the user, remove any existing bond for*/
                  /* this device.                                       */
                  if(DEVM_QueryRemoteDeviceProperties(BD_ADDR, ((AuthInfo->AuthenticationAction & DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK) ? DEVM_QUERY_REMOTE_DEVICE_PROPERTIES_FLAGS_LOW_ENERGY : 0), &RemoteDevProps) == 0)
                  {
                     if(RemoteDevProps.RemoteDeviceFlags & ((AuthInfo->AuthenticationAction & DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK) ? DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_PAIRED_OVER_LE : DEVM_REMOTE_DEVICE_FLAGS_DEVICE_CURRENTLY_PAIRED))
                        DEVM_UnPairRemoteDevice(BD_ADDR, ((AuthInfo->AuthenticationAction & DEVM_AUTHENTICATION_ACTION_LOW_ENERGY_OPERATION_MASK) ? DEVM_UNPAIR_REMOTE_DEVICE_FLAGS_LOW_ENERGY : 0));
                  }
               }
               else
                  SS1_LOGE("Attempt to cancel pairing via response to info-only message (must use a request message)");
            }
            else
               SS1_LOGE("Remote device address mismatch");
         }
         else
            SS1_LOGE("response provided but we are not the authentication handler for the stack");
      }
      else
         SS1_LOGE("Object not initialized");

      /* Clean up the Authentication Information structure which was    */
      /* passed upstream during the DEVM Authentication callback        */
      /* processing in BTEL_DEVMAuthCallback().                         */
      free(AuthInfo);
   }
   else
      SS1_LOGE("invalid parameters");

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(JNI_FALSE);
#endif
}

   /* The following function implements the Java native method:         */
   /*    android.server.BluetoothService.discoverServicesNative()       */
   /*                                                                   */
   /* This function is used to query the services advertised by a       */
   /* remote Bluetooth device. The fourth parameter is included to      */
   /* allow filtering the result by UUID but is ignored in the current  */
   /* implementation. Rather, all available services are fetched.       */
   /* Service discovery is an lengthy process, so the operation is      */
   /* performed asynchronously. The final result of the operation is    */
   /* returned in a RemoteDeviceServicesStatus event. This function     */
   /* returns JNI_TRUE if it successfully begins the service discovery  */
   /* process and JNI_FALSE if an error occurs.                         */
static jboolean BTS_DiscoverServicesNative(JNIEnv *Env, jobject Object, jstring Path, jstring Pattern)
{
   SS1_LOGD("Enter (%p, %p)", Path, Pattern);
#ifdef HAVE_BLUETOOTH
   int                             Result;
   jobject                         EventLoopObject;
   jboolean                        ret_val;
   BD_ADDR_t                       BD_ADDR;
   const char                     *PathBytes;
   BTEL_NativeData_t              *EventLoopNatData;
   DEVM_Local_Device_Properties_t  LocalDevProps;

   ret_val = JNI_FALSE;

   if(Path)
   {
      if(BTS_IsEnabledNative(Env, Object))
      {
         if((EventLoopObject = Env->GetObjectField(Object, Field_mEventLoop)) != NULL)
         {
            if((EventLoopNatData = BTEL_GetNativeData(Env, EventLoopObject)) != NULL)
            {
               if(((PathBytes = Env->GetStringUTFChars(Path, NULL)) != NULL) && PathToBD_ADDR(&BD_ADDR, PathBytes))
               {
                  /* Add this device to the list of outstanding service */
                  /* discovery operations.                              */
/* DEBUG */       
                  SS1_LOGD("Adding device %s to service discovery tracking list", PathBytes);
                  Result = AddDeviceToList(&(EventLoopNatData->ServiceDiscoveryPendingDevices), BD_ADDR, FALSE, NULL);
                  if(Result == ADD_DEVICE_SUCCESS)
                  {
                     /* The SDP query will fail if we are currently     */
                     /* discovering devices. Stop BR/EDR device         */
                     /* discovery, but only if a discovery process is in*/
                     /* progress.                                       */
                     if((DEVM_QueryLocalDeviceProperties(&LocalDevProps) == 0) && (LocalDevProps.LocalDeviceFlags & DEVM_LOCAL_DEVICE_FLAGS_DEVICE_DISCOVERY_IN_PROGRESS))
                        DEVM_StopDeviceDiscovery();

                     /* The device was added to the tracking list       */
                     /* successfully. Now, submit the actual Service    */
                     /* Discovery request.                              */
                     Result = DEVM_QueryRemoteDeviceServices(BD_ADDR, TRUE, 0, NULL, NULL);
                     if((Result >= 0) || (Result == BTPM_ERROR_CODE_SERVICE_DISCOVERY_IN_PROGRESS))
                        ret_val = JNI_TRUE;
                     else
                     {
                        /* We were unable to submit the discovery       */
                        /* request.                                     */
                        SS1_LOGE("Failed to start service discovery (%d)", Result);

                        /* Remove the device from the tracking list.    */
                        if(RemoveDeviceFromList(&(EventLoopNatData->ServiceDiscoveryPendingDevices), BD_ADDR, FALSE, NULL) == false)
                           SS1_LOGE("Unable to remove device %s from the service discovery tracking list", PathBytes);
                     }

                     Env->ReleaseStringUTFChars(Path, PathBytes);
                  }
                  else
                  {
                     if(Result == ADD_DEVICE_ALREADY_EXISTS)
                     {
                        /* The device was already present in the service*/
                        /* discovery tracking list.  This indicates that*/
                        /* an SDP query is already in progress, so we   */
                        /* will simply piggy-back on the existing query.*/
                        ret_val = JNI_TRUE;
                     }
                     else
                        SS1_LOGE("Unable to add device to service discovery tracking list");
                  }
               }
               else
                  SS1_LOGE("Unable to parse remote device address");
            }
            else
               SS1_LOGE("BTEL NatData is invalid");
         }
         else
            SS1_LOGE("Could not find associated BTEL object");
      }
      else
         SS1_LOGE("Called while Bluetooth is disabled");
   }
   else
      SS1_LOGE("Invalid paramters");

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(JNI_FALSE);
#endif
}

   /* The following function implements the Java native method:         */
   /*    android.server.BluetoothService.getDeviceServiceChannelNative()*/
   /*                                                                   */
   /* This function is used to determine the service channel            */
   /* ("Port") of an RFCOMM-based service on a remote device.           */
   /* The fourth parameter specifies the UUID of the service            */
   /* in question and the fifth parameter specifies the SDP             */
   /* Attribute ID to use to determine the RFCOMM port. Note            */
   /* that only SDP_ATTRIBUTE_ID_PROTOCOL_DESCRIPTOR_LIST and           */
   /* SDP_ATTRIBUTE_ID_ADDITIONAL_PROTOCOL_DESCRIPTOR_LISTS are         */
   /* considered valid Attribute IDs for this purpose. This function    */
   /* returns a positive, non-zero RFCOMM service channel if successful,*/
   /* zero if the service UUID and attribute ID were found but specified*/
   /* no service channel, or a negative value if an error occurred.     */
static jint BTS_GetDeviceServiceChannelNative(JNIEnv *Env, jobject Object, jstring Path, jstring Pattern, jint AttributeID)
{
   SS1_LOGD("Enter (%p, %p, %d)", Path, Pattern, AttributeID);
#ifdef HAVE_BLUETOOTH

   jint        ret_val;
   bool        OK;
   BD_ADDR_t   BD_ADDR;
   UUID_128_t  UUID;
   const char *JStringChars;

   OK = false;
   ret_val = -1;

   if(Path && Pattern)
   {
      if((JStringChars = Env->GetStringUTFChars(Path, NULL)) != NULL)
      {
         OK = PathToBD_ADDR(&BD_ADDR, JStringChars);
         Env->ReleaseStringUTFChars(Path, JStringChars);

         if(OK && ((JStringChars = Env->GetStringUTFChars(Pattern, NULL)) != NULL))
         {
            OK = StrToUUID128(&UUID, JStringChars);
            Env->ReleaseStringUTFChars(Pattern, JStringChars);
         }
         else
            OK = false;
      }
      else
         OK = false;

      if(OK)
         ret_val = GetRemoteDeviceRFCOMMChannel(BD_ADDR, UUID, AttributeID);
   }
   else
      SS1_LOGE("Invalid parameters");

   /* XXX DEBUG */
   {
      char addrString[32];
      BD_ADDRToStr(addrString, sizeof(addrString), BD_ADDR);
      if(OK)
      {
         char uuidString[64];
         UUID128ToStr(uuidString, sizeof(uuidString), UUID);
         SS1_LOGI("For device \"%s\", found RFCOMM in Service %s @ channel %d", addrString, uuidString, ret_val);
      }
      else
         SS1_LOGE("Could not lookup RFCOMM channel for device \"%s\"", addrString);
   }

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", -1);
   return(-1);
#endif
}

   /* The following function implements the Java native method:         */
   /*    android.server.BluetoothService.addRfcommServiceRecordNative() */
   /*                                                                   */
   /* This function is used to build and publish an SDP record for      */
   /* an RFCOMM-/SPP-based service. The record will advertise the       */
   /* service using the provided Name, 128-bit UUID, and RFCOMM         */
   /* channel. Per the RFCOMM specification, the channel must be in     */
   /* the range (1,31), inclusive. On success, the function returns     */
   /* a positive handle to the record, for use in a future call to      */
   /* android.server.BluetoothService.removeServiceRecordNative(). A    */
   /* negative return value indicates an error.                         */
static jint BTS_AddRfcommServiceRecordNative(JNIEnv *Env, jobject Object, jstring Name, jlong UUIDMsb, jlong UUIDLsb, jshort Channel)
{
   SS1_LOGD("Enter (%p, %lld, %lld, %hd)", Name, UUIDMsb, UUIDLsb, Channel);
#ifdef HAVE_BLUETOOTH
   int                Result;
   long               Handle;
   const char        *NameUTF8;
   UUID_128_t         TestUUID;
   SDP_UUID_Entry_t   ServiceUUID;
   SDP_Data_Element_t ServiceName;
   SDP_Data_Element_t ServicePDList[3];
   SDP_Data_Element_t ServicePDList_L2CAP;
   SDP_Data_Element_t ServicePDList_RFCOMM[2];
   SDP_Data_Element_t ServiceLanguage[4];

   Handle = -1;

   if(Name && RFCOMM_VALID_SERVER_CHANNEL_ID(Channel))
   {
      memset(&ServiceUUID, 0, sizeof(ServiceUUID));

      ServiceUUID.UUID_Value.UUID_128.UUID_Byte0  = (Byte_t)(UUIDMsb >> 56);
      ServiceUUID.UUID_Value.UUID_128.UUID_Byte1  = (Byte_t)(UUIDMsb >> 48);
      ServiceUUID.UUID_Value.UUID_128.UUID_Byte2  = (Byte_t)(UUIDMsb >> 40);
      ServiceUUID.UUID_Value.UUID_128.UUID_Byte3  = (Byte_t)(UUIDMsb >> 32);
      ServiceUUID.UUID_Value.UUID_128.UUID_Byte4  = (Byte_t)(UUIDMsb >> 24);
      ServiceUUID.UUID_Value.UUID_128.UUID_Byte5  = (Byte_t)(UUIDMsb >> 16);
      ServiceUUID.UUID_Value.UUID_128.UUID_Byte6  = (Byte_t)(UUIDMsb >>  8);
      ServiceUUID.UUID_Value.UUID_128.UUID_Byte7  = (Byte_t)(UUIDMsb >>  0);
      ServiceUUID.UUID_Value.UUID_128.UUID_Byte8  = (Byte_t)(UUIDLsb >> 56);
      ServiceUUID.UUID_Value.UUID_128.UUID_Byte9  = (Byte_t)(UUIDLsb >> 48);
      ServiceUUID.UUID_Value.UUID_128.UUID_Byte10 = (Byte_t)(UUIDLsb >> 40);
      ServiceUUID.UUID_Value.UUID_128.UUID_Byte11 = (Byte_t)(UUIDLsb >> 32);
      ServiceUUID.UUID_Value.UUID_128.UUID_Byte12 = (Byte_t)(UUIDLsb >> 24);
      ServiceUUID.UUID_Value.UUID_128.UUID_Byte13 = (Byte_t)(UUIDLsb >> 16);
      ServiceUUID.UUID_Value.UUID_128.UUID_Byte14 = (Byte_t)(UUIDLsb >>  8);
      ServiceUUID.UUID_Value.UUID_128.UUID_Byte15 = (Byte_t)(UUIDLsb >>  0);

      /* Test whether this UUID is based on the SDP Base UUID. If it    */
      /* is, then we can reduce it to a 32- or 16-bit UUID.             */
      SDP_ASSIGN_BASE_UUID(TestUUID);
      ASSIGN_SDP_UUID_32_TO_SDP_UUID_128(TestUUID, ServiceUUID.UUID_Value.UUID_128);
      if(COMPARE_UUID_128(TestUUID, ServiceUUID.UUID_Value.UUID_128))
      {
         /* The lower 96 bits of the requested UUID matches the SDP     */
         /* Base UUID, so we can reduce it. Now check whether it's      */
         /* reducable to 16-bits or just to 32-bits.                    */
         if((ServiceUUID.UUID_Value.UUID_128.UUID_Byte0 == 0) && (ServiceUUID.UUID_Value.UUID_128.UUID_Byte1 == 0))
         {
            /* This UUID is representable in 16 bits.                   */
            ServiceUUID.SDP_Data_Element_Type = deUUID_16;

            /* 16-bit UUIDs contain the bytes in indices 2 and 3 of a   */
            /* 128-bit UUID (the rest is assumed to match the SDP Base  */
            /* UUID.                                                    */
            ServiceUUID.UUID_Value.UUID_128.UUID_Byte0 = ServiceUUID.UUID_Value.UUID_128.UUID_Byte2;
            ServiceUUID.UUID_Value.UUID_128.UUID_Byte1 = ServiceUUID.UUID_Value.UUID_128.UUID_Byte3;
         }
         else
         {
            /* This UUID requires at least 32 bits to represent. The    */
            /* 4 bytes of a 32-bit UUID match the 4 most-significant    */
            /* bytes of a 128 bit UUID, so no adjustment is necessary.  */
            ServiceUUID.SDP_Data_Element_Type = deUUID_32;
         }
      }
      else
      {
         /* This UUID is not based on the SDP Base UUID, so it requires */
         /* all 128 bits to represent.                                  */
         ServiceUUID.SDP_Data_Element_Type = deUUID_128;
      }

      {
         /* XXX DEBUG */
         char uuidString[64];
         UUID_128_t UUID;
         ASSIGN_SDP_UUID_128(UUID, (Byte_t)(UUIDMsb >> 56), (Byte_t)(UUIDMsb >> 48), (Byte_t)(UUIDMsb >> 40), (Byte_t)(UUIDMsb >> 32), (Byte_t)(UUIDMsb >> 24), (Byte_t)(UUIDMsb >> 16), (Byte_t)(UUIDMsb >>  8), (Byte_t)(UUIDMsb >>  0), (Byte_t)(UUIDLsb >> 56), (Byte_t)(UUIDLsb >> 48), (Byte_t)(UUIDLsb >> 40), (Byte_t)(UUIDLsb >> 32), (Byte_t)(UUIDLsb >> 24), (Byte_t)(UUIDLsb >> 16), (Byte_t)(UUIDLsb >>  8), (Byte_t)(UUIDLsb >>  0));
         UUID128ToStr(uuidString, sizeof(uuidString), UUID);
         SS1_LOGI("Creating service record for UUID %s", uuidString);
      }
      /* Attempt to create a new SDP record for the UUID.               */
      Handle = DEVM_CreateServiceRecord(FALSE, 1, &ServiceUUID);
      SS1_LOGI("Got SDP record handle %ld", Handle);
   }
   else
      SS1_LOGE("Invalid parameters");

   if(Handle >= 0)
   {
      /* The service record handle is valid, so let's add the Protocol  */
      /* Descriptor List to the record.                                 */
      ServicePDList[0].SDP_Data_Element_Type                        = deSequence;
      ServicePDList[0].SDP_Data_Element_Length                      = 2;
      ServicePDList[0].SDP_Data_Element.SDP_Data_Element_Sequence   = &ServicePDList[1];

      ServicePDList[1].SDP_Data_Element_Type                        = deSequence;
      ServicePDList[1].SDP_Data_Element_Length                      = 1;
      ServicePDList[1].SDP_Data_Element.SDP_Data_Element_Sequence   = &ServicePDList_L2CAP;

      ServicePDList_L2CAP.SDP_Data_Element_Type                     = deUUID_16;
      ServicePDList_L2CAP.SDP_Data_Element_Length                   = sizeof(ServicePDList_L2CAP.SDP_Data_Element.UUID_16);
      SDP_ASSIGN_L2CAP_UUID_16(ServicePDList_L2CAP.SDP_Data_Element.UUID_16);

      ServicePDList[2].SDP_Data_Element_Type                        = deSequence;
      ServicePDList[2].SDP_Data_Element_Length                      = 2;
      ServicePDList[2].SDP_Data_Element.SDP_Data_Element_Sequence   = ServicePDList_RFCOMM;

      ServicePDList_RFCOMM[0].SDP_Data_Element_Type                 = deUUID_16;
      ServicePDList_RFCOMM[0].SDP_Data_Element_Length               = UUID_16_SIZE;
      SDP_ASSIGN_RFCOMM_UUID_16(ServicePDList_RFCOMM[0].SDP_Data_Element.UUID_16);

      ServicePDList_RFCOMM[1].SDP_Data_Element_Type                 = deUnsignedInteger1Byte;
      ServicePDList_RFCOMM[1].SDP_Data_Element_Length               = sizeof(ServicePDList_RFCOMM[1].SDP_Data_Element.UnsignedInteger1Byte);
      ServicePDList_RFCOMM[1].SDP_Data_Element.UnsignedInteger1Byte = (Byte_t)Channel;

      Result = DEVM_AddServiceRecordAttribute((unsigned long)Handle, SDP_ATTRIBUTE_ID_PROTOCOL_DESCRIPTOR_LIST, ServicePDList);

      if(Result == 0)
      {
         /* Now specify the default Language as UTF-8 English.          */
         ServiceLanguage[0].SDP_Data_Element_Type                      = deSequence;
         ServiceLanguage[0].SDP_Data_Element_Length                    = 3;
         ServiceLanguage[0].SDP_Data_Element.SDP_Data_Element_Sequence = &ServiceLanguage[1];
         ServiceLanguage[1].SDP_Data_Element_Type                      = deUnsignedInteger2Bytes;
         ServiceLanguage[1].SDP_Data_Element_Length                    = SDP_UNSIGNED_INTEGER_2_BYTES_SIZE;
         ServiceLanguage[1].SDP_Data_Element.UnsignedInteger2Bytes     = SDP_NATURAL_LANGUAGE_ENGLISH_UTF_8;
         ServiceLanguage[2].SDP_Data_Element_Type                      = deUnsignedInteger2Bytes;
         ServiceLanguage[2].SDP_Data_Element_Length                    = SDP_UNSIGNED_INTEGER_2_BYTES_SIZE;
         ServiceLanguage[2].SDP_Data_Element.UnsignedInteger2Bytes     = SDP_UTF_8_CHARACTER_ENCODING;
         ServiceLanguage[3].SDP_Data_Element_Type                      = deUnsignedInteger2Bytes;
         ServiceLanguage[3].SDP_Data_Element_Length                    = SDP_UNSIGNED_INTEGER_2_BYTES_SIZE;
         ServiceLanguage[3].SDP_Data_Element.UnsignedInteger2Bytes     = SDP_DEFAULT_LANGUAGE_BASE_ATTRIBUTE_ID;

         Result = DEVM_AddServiceRecordAttribute((unsigned long)Handle, SDP_ATTRIBUTE_ID_LANGUAGE_BASE_ATTRIBUTE_ID_LIST, ServiceLanguage);

         if(Result == 0)
         {
            if((NameUTF8 = Env->GetStringUTFChars(Name, NULL)) != NULL)
            {
               ServiceName.SDP_Data_Element_Type       = deTextString;
               ServiceName.SDP_Data_Element_Length     = strlen(NameUTF8);
               ServiceName.SDP_Data_Element.TextString = (Byte_t *)NameUTF8;

               Result = DEVM_AddServiceRecordAttribute((unsigned long)Handle, (SDP_DEFAULT_LANGUAGE_BASE_ATTRIBUTE_ID + SDP_ATTRIBUTE_OFFSET_ID_SERVICE_NAME), &ServiceName);

               if(Result == 0)
               {
                  /* The SDP record has been created successfully.            */
                  {
                     /* XXX DEBUG */
                     char uuidString[64];
                     UUID_128_t UUID;
                     ASSIGN_SDP_UUID_128(UUID, (Byte_t)(UUIDMsb >> 56), (Byte_t)(UUIDMsb >> 48), (Byte_t)(UUIDMsb >> 40), (Byte_t)(UUIDMsb >> 32), (Byte_t)(UUIDMsb >> 24), (Byte_t)(UUIDMsb >> 16), (Byte_t)(UUIDMsb >>  8), (Byte_t)(UUIDMsb >>  0), (Byte_t)(UUIDLsb >> 56), (Byte_t)(UUIDLsb >> 48), (Byte_t)(UUIDLsb >> 40), (Byte_t)(UUIDLsb >> 32), (Byte_t)(UUIDLsb >> 24), (Byte_t)(UUIDLsb >> 16), (Byte_t)(UUIDLsb >>  8), (Byte_t)(UUIDLsb >>  0));
                     UUID128ToStr(uuidString, sizeof(uuidString), UUID);
                     SS1_LOGI("Created service record for UUID %s for Channel %hd with name \"%s\"", uuidString, Channel, NameUTF8);
                  }
               }
               else
                  SS1_LOGE("Failed to add SDP record name");

               Env->ReleaseStringUTFChars(Name, NameUTF8);
            }
            else
            {
               Result = -1;
               SS1_LOGE("Failed to allocate record name from jstring object");
            }
         }
         else
            SS1_LOGE("Failed to add SDP record language attribute");
      }
      else
         SS1_LOGE("Failed to add SDP record Protocol Descriptor List");

      if(Result != 0)
      {
         /* We could not add the SPD attributes, so remove the SDP      */
         /* record we created.                                          */
         DEVM_DeleteServiceRecord(Handle);
         Handle = -1;
      }
   }

   SS1_LOGD("Exit (%ld)", Handle);

   return(Handle);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%ld)", -1);
   return(-1);
#endif
}

   /* The following function implements the Java native method:         */
   /*    android.server.BluetoothService.removeServiceRecordNative()    */
   /*                                                                   */
   /* This function is used to unregister an existing SDP record. The   */
   /* third parameter is a handle to the record obtained from a call to */
   /* android.server.BluetoothService.addRfcommServiceRecordNative().   */
   /* The function returns JNI_TRUE on success, or JNI_FALSE on error.  */
static jboolean BTS_RemoveServiceRecordNative(JNIEnv *Env, jobject Object, jint Handle)
{
   SS1_LOGD("Enter (%d)", Handle);
#ifdef HAVE_BLUETOOTH
   int ret_val;

   if(DEVM_DeleteServiceRecord(Handle) == 0)
      ret_val = JNI_TRUE;
   else
      ret_val = JNI_FALSE;

   SS1_LOGD("Exit (%d)", ret_val);
   return(ret_val);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(JNI_FALSE);
#endif
}

#if (SS1_PLATFORM_SDK_VERSION >= 14)
   /* XXX Always 16-bit uuids. */
static jintArray BTS_AddReservedServiceRecordsNative(JNIEnv *Env, jobject Object, jintArray Uuids)
{
   SS1_LOGD("Enter (%p)", Uuids);
#ifdef HAVE_BLUETOOTH
   char         PropBuffer[64];
   jint        *UuidListEntries;
   jint        *HandleListEntries;
   jsize        Index;
   jsize        NumberUuids;
   Boolean_t    Error;
   jintArray    HandleList;
   UUID_128_t  *RegisteredUuids;
   UUID_128_t   UUID_128;
   unsigned int ServiceRecordHandle;
   jobjectArray PropStringArray;

   Error           = FALSE;
   NumberUuids     = 0;
   RegisteredUuids = NULL;

   if((UuidListEntries = Env->GetIntArrayElements(Uuids, NULL)) != NULL)
   {
      NumberUuids = Env->GetArrayLength(Uuids);

      if((HandleList = Env->NewIntArray(NumberUuids)) != NULL)
      {
         if((RegisteredUuids = (UUID_128_t *)malloc(NumberUuids * sizeof(UUID_128_t))) != NULL)
         {
            if((HandleListEntries = Env->GetIntArrayElements(HandleList, NULL)) != NULL)
            {
               memset(RegisteredUuids, 0, (NumberUuids * sizeof(UUID_128_t)));

               for(Index = 0; ((Index < NumberUuids) && (!Error)); Index++)
               {
                  switch(UuidListEntries[Index])
                  {
                     case 0x1112:
                        /* HSP AG                                       */
                        if(RegisterSDPRecordForHeadset(&ServiceRecordHandle) == 0)
                        {
                           SS1_LOGI("Registered SDP record for HSP AG");
                           HandleListEntries[Index] = ServiceRecordHandle;
                           ASSIGN_SDP_UUID_128(RegisteredUuids[Index], 0x00, 0x00, 0x11, 0x12, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5F, 0x9B, 0x34, 0xFB);
                        }
                        else
                           Error = TRUE;

                        break;
                     case 0x1105:
                        /* OPP                                          */
                        if(RegisterSDPRecordForOPP(&ServiceRecordHandle) == 0)
                        {
                           SS1_LOGI("Registered SDP record for OPP");
                           HandleListEntries[Index] = ServiceRecordHandle;
                           ASSIGN_SDP_UUID_128(RegisteredUuids[Index], 0x00, 0x00, 0x11, 0x05, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5F, 0x9B, 0x34, 0xFB);
                        }
                        else
                           Error = TRUE;

                        break;
                     case 0x111F:
                        /* HFP AG                                       */
                        if(RegisterSDPRecordForHandsFree(&ServiceRecordHandle) == 0)
                        {
                           SS1_LOGI("Registered SDP record for HFP AG");
                           HandleListEntries[Index] = ServiceRecordHandle;
                           ASSIGN_SDP_UUID_128(RegisteredUuids[Index], 0x00, 0x00, 0x11, 0x1F, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5F, 0x9B, 0x34, 0xFB);
                        }
                        else
                           Error = TRUE;

                        break;
                     case 0x112F:
                        /* PBAP PSE                                     */
                        if(RegisterSDPRecordForPBAP(&ServiceRecordHandle) == 0)
                        {
                           SS1_LOGI("Registered SDP record for PBAP PSE");
                           HandleListEntries[Index] = ServiceRecordHandle;
                           ASSIGN_SDP_UUID_128(RegisteredUuids[Index], 0x00, 0x00, 0x11, 0x2F, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5F, 0x9B, 0x34, 0xFB);
                        }
                        else
                           Error = TRUE;

                        break;
                     default:
                        /* Unknown                                      */
                        SS1_LOGE("Request for reserved service record not handled: 0x%04X", HandleListEntries[Index]);
                  }
               }

               Env->ReleaseIntArrayElements(HandleList, HandleListEntries, 0);
            }
            else
               Error = TRUE;
         }
         else
            Error = TRUE;
      }
      else
         Error = TRUE;

      Env->ReleaseIntArrayElements(Uuids, UuidListEntries, 0);
   }
   else
   {
      Error           = TRUE;
      HandleList      = NULL;
   }

   /* If all registrations were successful, generate a callback to      */
   /* BluetoothEventLoop.onPropertyChanged for property "UUIDs"         */
   /* containing a list of 128-bit UUIDs for the registered services.   */
   if(RegisteredUuids)
   {
      if(!Error)
      {
         /* XXX FIXME */
         /* XXX An extra three elements are added to the property array.*/
         /* XXX This is for the A2DP Source, AVRCP Target, and PAN NAP  */
         /* XXX services which Android expects to be available and      */
         /* XXX automatically registered by the Bluetooth stack. Until  */
         /* XXX PM supports reporting on registered SDP records, we     */
         /* XXX will add these entries to the list manually under the   */
         /* XXX assumption that they are being created correctly by the */
         /* XXX PM Server. Once we can query PM for a list of registered*/
         /* XXX SDP records, we can provide more accurate events through*/
         /* XXX an actual "Local Device Properties Changed" event       */
         /* XXX handler.                                                */
         if((PropStringArray = Env->NewObjectArray((NumberUuids + 2 + 3), Env->FindClass("java/lang/String"), NULL)) != NULL)
         {
            SetStringArrayElement(Env, PropStringArray, 0, "UUIDs");
            snprintf(PropBuffer, sizeof(PropBuffer), "%d", (NumberUuids + 3));
            SetStringArrayElement(Env, PropStringArray, 1, PropBuffer);

            for(Index = 0; Index < NumberUuids; Index++)
            {
               if(!UUID128ToStr(PropBuffer, sizeof(PropBuffer), RegisteredUuids[Index]))
               {
                  SS1_LOGE("Unable to convert UUID: %02hhX%02hhX%02hhX%02hhX-%02hhX%02hhX-%02hhX%02hhX-%02hhX%02hhX-%02hhX%02hhX%02hhX%02hhX%02hhX%02hhX", RegisteredUuids[Index].UUID_Byte0, RegisteredUuids[Index].UUID_Byte1, RegisteredUuids[Index].UUID_Byte2, RegisteredUuids[Index].UUID_Byte3, RegisteredUuids[Index].UUID_Byte4, RegisteredUuids[Index].UUID_Byte5, RegisteredUuids[Index].UUID_Byte6, RegisteredUuids[Index].UUID_Byte7, RegisteredUuids[Index].UUID_Byte8, RegisteredUuids[Index].UUID_Byte9, RegisteredUuids[Index].UUID_Byte10, RegisteredUuids[Index].UUID_Byte11, RegisteredUuids[Index].UUID_Byte12, RegisteredUuids[Index].UUID_Byte13, RegisteredUuids[Index].UUID_Byte14, RegisteredUuids[Index].UUID_Byte15);
                  BTPS_StringCopy(PropBuffer, "00000000-0000-0000-0000-000000000000");
               }

               SetStringArrayElement(Env, PropStringArray, (Index + 2), PropBuffer);
            }

            /* A2DP Sink                                                */
            SS1_LOGI("Including A2DP Sink in list of registered services");
            ASSIGN_SDP_UUID_128(UUID_128, 0x00, 0x00, 0x11, 0x0a, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5f, 0x9b, 0x34, 0xfb);
            UUID128ToStr(PropBuffer, sizeof(PropBuffer), UUID_128);
            SetStringArrayElement(Env, PropStringArray, (NumberUuids + 2), PropBuffer);

            /* AVRCP Target                                             */
            SS1_LOGI("Including AVRCP Target in list of registered services");
            ASSIGN_SDP_UUID_128(UUID_128, 0x00, 0x00, 0x11, 0x0c, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5f, 0x9b, 0x34, 0xfb);
            UUID128ToStr(PropBuffer, sizeof(PropBuffer), UUID_128);
            SetStringArrayElement(Env, PropStringArray, (NumberUuids + 3), PropBuffer);

            /* PAN NAP                                                  */
            SS1_LOGI("Including PAN NAP in list of registered services");
            ASSIGN_SDP_UUID_128(UUID_128, 0x00, 0x00, 0x11, 0x16, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5f, 0x9b, 0x34, 0xfb);
            UUID128ToStr(PropBuffer, sizeof(PropBuffer), UUID_128);
            SetStringArrayElement(Env, PropStringArray, (NumberUuids + 4), PropBuffer);

            SendDelayedPropertyChangedEvent(Env, Object, PropStringArray, 100L);

            Env->DeleteLocalRef(PropStringArray);
         }
      }

      free(RegisteredUuids);
   }

   SS1_LOGD("Exit (%d)", ((HandleList && !Error) ? Env->GetArrayLength(HandleList) : -1));
   return(HandleList);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (null)");
   return(NULL);
#endif
}
#endif

#if (SS1_PLATFORM_SDK_VERSION >= 14)
static jboolean BTS_RemoveReservedServiceRecordsNative(JNIEnv *Env, jobject Object, jintArray Handles)
{
   SS1_LOGD("Enter (%p)", Handles);
#ifdef HAVE_BLUETOOTH
   jint      *HandleList;
   jsize      Index;
   jsize      NumberHandles;
   jboolean   ret_val;

   if((HandleList = Env->GetIntArrayElements(Handles, NULL)) != NULL)
   {
      NumberHandles = Env->GetArrayLength(Handles);

      ret_val = JNI_TRUE;

      for(Index = 0; ((Index < NumberHandles) && (ret_val != JNI_FALSE)); Index++)
         ret_val = BTS_RemoveServiceRecordNative(Env, Object, HandleList[Index]);

      Env->ReleaseIntArrayElements(Handles, HandleList, 0);
   }
   else
      ret_val = JNI_FALSE;

   SS1_LOGD("Exit (%d)", ret_val);
   return(ret_val);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(NULL);
#endif
}
#endif

   /* The following function implements the Java native method:         */
   /*    android.server.BluetoothService.setLinkTimeoutNative()         */
   /*                                                                   */
   /* This function is used to configure a new link timeout (specified  */
   /* in Bluetooth timeslots) for the connection to a particular device.*/
   /* The device is specified as a DBus-style path ("/dev_<BD_ADDR>").  */
   /* The function returns JNI_TRUE if the link timeout is set          */
   /* successfully or JNI_FALSE if an error ocurrs.                     */
static jboolean BTS_SetLinkTimeoutNative(JNIEnv *Env, jobject Object, jstring Path, jint Slots)
{
   SS1_LOGD("Enter (%p, %d)", Path, Slots);
#ifdef HAVE_BLUETOOTH
   jboolean ret_val;

   ret_val = JNI_TRUE;
   // FIXME implement this


   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(JNI_FALSE);
#endif
}

   /* The following function implements the Java native method:         */
   /*    android.server.BluetoothService.connectInputDeviceNative()     */
   /*                                                                   */
   /* This function is used to create a new connection to a particular  */
   /* HID-capable device. The device is specified as a DBus-style       */
   /* path ("/dev_<BD_ADDR>"). The function returns JNI_TRUE if the     */
   /* conncetion request is accepted or JNI_FALSE if an error ocurrs.   */
#if (SS1_PLATFORM_SDK_VERSION >= 14)
static jboolean BTS_ConnectInputDeviceNative(JNIEnv *Env, jobject Object, jstring Path)
{
   SS1_LOGD("Enter");
#if (defined(HAVE_BLUETOOTH) && (SS1_SUPPORT_HID))
   int                Result;
   jint               ResultCode;
   jobject            EventLoopObject;
   jboolean           ret_val;
   BD_ADDR_t          RemoteAddress;
   const char        *PathString;
   unsigned int       ConnectionFlags;
   BTEL_NativeData_t *EventLoopNatData;

   /* Set some default values in case anything fails.                   */
   ret_val = JNI_FALSE;

   if(BTS_IsEnabledNative(Env, Object))
   {
      if((EventLoopObject = Env->GetObjectField(Object, Field_mEventLoop)) != NULL)
      {
         if((EventLoopNatData = BTEL_GetNativeData(Env, EventLoopObject)) != NULL)
         {
            if(Path)
            {
               if((PathString = Env->GetStringUTFChars(Path, NULL)) != NULL)
               {
                  if(PathToBD_ADDR(&RemoteAddress, PathString))
                  {
                     ConnectionFlags = (HIDM_CONNECT_HID_DEVICE_FLAGS_REQUIRE_AUTHENTICATION | HIDM_CONNECT_HID_DEVICE_FLAGS_REQUIRE_ENCRYPTION | HIDM_CONNECT_HID_DEVICE_FLAGS_PARSE_BOOT);

                     if((Result = HIDM_Connect_Remote_Device(RemoteAddress, ConnectionFlags, BTEL_HIDMEventCallback, EventLoopNatData, NULL)) == 0)
                     {
                        /* The connection attempt was started           */
                        /* successfully.  Report success to the caller. */
                        ret_val = JNI_TRUE;
                     }
                     else
                     {
                        /* Map the failure result to the nearest state  */
                        /* of:                                          */
                        /*    - INPUT_DISCONNECT_FAILED_NOT_CONNECTED   */
                        /*    - INPUT_CONNECT_FAILED_ALREADY_CONNECTED  */
                        /*    - INPUT_CONNECT_FAILED_ATTEMPT_FAILED     */
                        /*    - INPUT_OPERATION_GENERIC_FAILURE         */
                        /*                                              */
                        if((Result == BTPM_ERROR_CODE_HID_DEVICE_ALREADY_CONNECTED) || (Result == BTPM_ERROR_CODE_HID_DEVICE_CONNECTION_IN_PROGRESS))
                           ResultCode = INPUT_CONNECT_FAILED_ALREADY_CONNECTED;
                        else
                           ResultCode = INPUT_CONNECT_FAILED_ATTEMPT_FAILED;

                        SendDelayedConnectInputDeviceResult(Env, Object, Path, ResultCode, 100L);
                     }
                  }

                  Env->ReleaseStringUTFChars(Path, PathString);
               }
            }
         }
         else
            SS1_LOGE("BTEL NatData is invalid");
      }
      else
         SS1_LOGE("Could not find associated BTEL object");
   }
   else
      SS1_LOGE("Called while Bluetooth is disabled");

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(JNI_FALSE);
#endif
}
#endif

   /* The following function implements the Java native method:         */
   /*    android.server.BluetoothService.disconnectInputDeviceNative()  */
   /*                                                                   */
   /* This function is used to close an existing connection to a        */
   /* HID-capable device. The device is specified as a DBus-style       */
   /* path ("/dev_<BD_ADDR>"). The function returns JNI_TRUE if the     */
   /* disconnect request is accepted or JNI_FALSE if an error ocurrs.   */
#if (SS1_PLATFORM_SDK_VERSION >= 14)
static jboolean BTS_DisconnectInputDeviceNative(JNIEnv *Env, jobject Object, jstring Path)
{
   SS1_LOGD("Enter");
#if (defined(HAVE_BLUETOOTH) && (SS1_SUPPORT_HID))
   int         Result;
   jboolean    ret_val;
   BD_ADDR_t   RemoteAddress;
   const char *PathString;

   ret_val = JNI_FALSE;

   /* Set some default values in case anything fails.                   */
   if(Path)
   {
      if((PathString = Env->GetStringUTFChars(Path, NULL)) != NULL)
      {
         if(PathToBD_ADDR(&RemoteAddress, PathString))
         {
            /* Attempt to disconnect the device.                        */
            if((Result = HIDM_Disconnect_Device(RemoteAddress, FALSE)) != 0)
            {
               /* The disconnect request failed. Log this error.        */
               SS1_LOGI("HID Device disconnect request failed: %d", Result);
            }

            /* Report success to the caller, regardless of whether the  */
            /* disconnection request was accepted. If the request was   */
            /* not accepted, then the device was not considered to be   */
            /* connected, anyway.                                       */
            ret_val = JNI_TRUE;
         }

         Env->ReleaseStringUTFChars(Path, PathString);
      }
   }

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (null)");
   return(JNI_FALSE);
#endif
}
#endif

#if (SS1_PLATFORM_SDK_VERSION >= 14)
static jboolean BTS_SetBluetoothTetheringNative(JNIEnv *Env, jobject Object, jboolean Value, jstring NAP, jstring Bridge)
{
   SS1_LOGD("Enter");
#if (defined(HAVE_BLUETOOTH) && (SS1_SUPPORT_PAN))
   //XXX

/*DEBUG*/{
/*DEBUG*/const char *String;
/*DEBUG*/SS1_LOGI("Given: Value  = \"%s\"", (Value == JNI_TRUE ? "true" : "false"));
/*DEBUG*/if((String = Env->GetStringUTFChars(NAP, NULL)) != NULL)
/*DEBUG*/{
/*DEBUG*/   SS1_LOGI("Given: NAP    = \"%s\"", String);
/*DEBUG*/   Env->ReleaseStringUTFChars(NAP, String);
/*DEBUG*/}
/*DEBUG*/if((String = Env->GetStringUTFChars(Bridge, NULL)) != NULL)
/*DEBUG*/{
/*DEBUG*/   SS1_LOGI("Given: Bridge = \"%s\"", String);
/*DEBUG*/   Env->ReleaseStringUTFChars(Bridge, String);
/*DEBUG*/}
/*DEBUG*/}
   SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(JNI_TRUE);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(JNI_FALSE);
#endif
}
#endif

#if (SS1_PLATFORM_SDK_VERSION >= 14)
static jboolean BTS_ConnectPanDeviceNative(JNIEnv *Env, jobject Object, jstring Path, jstring DstRole)
{
   SS1_LOGD("Enter");
#if (defined(HAVE_BLUETOOTH) && (SS1_SUPPORT_PAN))
   int                Result;
   jint               ResultCode;
   jobject            EventLoopObject;
   jboolean           ret_val;
   BD_ADDR_t          RemoteAddress;
   const char        *StringValue;
   BTEL_NativeData_t *EventLoopNatData;
   PAN_Service_Type_t LocalServiceType;
   PAN_Service_Type_t RemoteServiceType;

   /* Set some default values in case anything fails.                   */
   ret_val = JNI_FALSE;

   if(Path)
   {
      if((StringValue = Env->GetStringUTFChars(Path, NULL)) != NULL)
      {
         if(PathToBD_ADDR(&RemoteAddress, StringValue))
         {
            Env->ReleaseStringUTFChars(Path, StringValue);

            if((StringValue = Env->GetStringUTFChars(DstRole, NULL)) != NULL)
            {
               /* Determine the type of connection being requested.     */
               if(strncmp(StringValue, "nap", 4) == 0)
               {
                  /* Caller requested a connection to a Network Access  */
                  /* Point, so we are the PAN-User.                     */
                  LocalServiceType  = pstPersonalAreaNetworkUser;
                  RemoteServiceType = pstNetworkAccessPoint;

                  Result = 0;
               }
               else
               {
                  /* No other outbound connection type is supported by  */
                  /* Android. Return failure.                           */
                  Result = -1;
               }

               /* We are done with the Role string, so release it now.  */
               Env->ReleaseStringUTFChars(Path, StringValue);

               if(!Result)
               {
                  /* Obtain a reference to the EventLoop context so we  */
                  /* can specify the correct callback to receive the    */
                  /* result of the connection attempt.                  */
                  if((EventLoopObject = Env->GetObjectField(Object, Field_mEventLoop)) != NULL)
                  {
                     if((EventLoopNatData = BTEL_GetNativeData(Env, EventLoopObject)) != NULL)
                     {
                        /* Attempt to connect the remote PAN service.   */
                        if((Result = PANM_Connect_Remote_Device(RemoteAddress, LocalServiceType, RemoteServiceType, (PANM_CONNECT_REMOTE_DEVICE_FLAGS_REQUIRE_AUTHENTICATION | PANM_CONNECT_REMOTE_DEVICE_FLAGS_REQUIRE_ENCRYPTION), BTEL_PANMEventCallback, EventLoopNatData, NULL)) == 0)
                        {
                           /* Report success to the caller.             */
                           ret_val = JNI_TRUE;
                        }
                        else
                        {
                           /* The connect request failed. Log this      */
                           /* error.                                    */
                           SS1_LOGI("PAN Device connect request failed: %d", Result);

                           /* Map the result to the nearest state of:         */
                           /*    - PAN_DISCONNECT_FAILED_NOT_CONNECTED        */
                           /*    - PAN_CONNECT_FAILED_ALREADY_CONNECTED       */
                           /*    - PAN_CONNECT_FAILED_ATTEMPT_FAILED          */
                           /*    - PAN_OPERATION_GENERIC_FAILURE              */
                           /*    - PAN_OPERATION_SUCCESS                      */
                           if((Result == BTPM_ERROR_CODE_PAN_DEVICE_ALREADY_CONNECTED) || (Result == BTPM_ERROR_CODE_PAN_CONNECTION_IN_PROGRESS))
                              ResultCode = PAN_CONNECT_FAILED_ALREADY_CONNECTED;
                           else
                              ResultCode = PAN_CONNECT_FAILED_ATTEMPT_FAILED;

                           SendDelayedConnectPanDeviceResult(Env, Object, Path, ResultCode, 100L);
                        }
                     }
                     else
                        SS1_LOGE("BTEL NatData is invalid");
                  }
                  else
                     SS1_LOGE("Could not find associated BTEL object");
               }
            }
         }
         else
            Env->ReleaseStringUTFChars(Path, StringValue);
      }
   }

   SS1_LOGD("Exit (%d)", ret_val);
   return(ret_val);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(JNI_FALSE);
#endif
}
#endif

#if (SS1_PLATFORM_SDK_VERSION >= 14)
static jboolean BTS_DisconnectPanDeviceNative(JNIEnv *Env, jobject Object, jstring Path)
{
   SS1_LOGD("Enter");
#if (defined(HAVE_BLUETOOTH) && (SS1_SUPPORT_PAN))
   int         Result;
   jboolean    ret_val;
   BD_ADDR_t   RemoteAddress;
   const char *PathString;

   /* Set some default values in case anything fails.                   */
   ret_val = JNI_FALSE;

   if(Path)
   {
      if((PathString = Env->GetStringUTFChars(Path, NULL)) != NULL)
      {
         if(PathToBD_ADDR(&RemoteAddress, PathString))
         {
            Env->ReleaseStringUTFChars(Path, PathString);

            /* Attempt to disconnect the PAN session.                   */
            if((Result = PANM_Close_Connection(RemoteAddress)) != 0)
            {
               /* The disconnect request failed. Log this error.        */
               SS1_LOGI("PAN Device disconnect request failed: %d", Result);
            }

            /* Report success to the caller, regardless of whether the  */
            /* disconnection request was accepted. If the request was   */
            /* not accepted, then the device was not considered to be   */
            /* connected, anyway.                                       */
            ret_val = JNI_TRUE;
         }
         else
            Env->ReleaseStringUTFChars(Path, PathString);
      }
   }

   SS1_LOGD("Exit (%d)", ret_val);
   return(ret_val);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(JNI_FALSE);
#endif
}
#endif

#if (SS1_PLATFORM_SDK_VERSION >= 14)
static jboolean BTS_DisconnectPanServerDeviceNative(JNIEnv *Env, jobject Object, jstring Path, jstring Address, jstring Iface)
{
   SS1_LOGD("Enter");
#if (defined(HAVE_BLUETOOTH) && (SS1_SUPPORT_PAN))
   int         Result;
   jboolean    ret_val;
   BD_ADDR_t   RemoteAddress;
   const char *PathString;

   /* Set some default values in case anything fails.                   */
   ret_val = JNI_FALSE;

   if(Path)
   {
      if((PathString = Env->GetStringUTFChars(Path, NULL)) != NULL)
      {
/*DEBUG*/SS1_LOGD("Given: Path    = \"%s\"", PathString);
         if(PathToBD_ADDR(&RemoteAddress, PathString))
         {
            Env->ReleaseStringUTFChars(Path, PathString);
{
/*DEBUG*/const char *String;
/*DEBUG*/if((String = Env->GetStringUTFChars(Address, NULL)) != NULL)
/*DEBUG*/{
/*DEBUG*/   SS1_LOGD("       Address = \"%s\"", String);
/*DEBUG*/   Env->ReleaseStringUTFChars(Address, String);
/*DEBUG*/}
/*DEBUG*/if((String = Env->GetStringUTFChars(Iface, NULL)) != NULL)
/*DEBUG*/{
/*DEBUG*/   SS1_LOGD("       Iface   = \"%s\"", String);
/*DEBUG*/   Env->ReleaseStringUTFChars(Iface, String);
/*DEBUG*/}
}

            /* Attempt to disconnect the PAN session.                   */
            if((Result = PANM_Close_Connection(RemoteAddress)) != 0)
            {
               /* The disconnect request failed. Log this error.        */
               SS1_LOGI("PAN Device disconnect request failed: %d", Result);
            }

            /* Report success to the caller, regardless of whether the  */
            /* disconnection request was accepted. If the request was   */
            /* not accepted, then the device was not considered to be   */
            /* connected, anyway.                                       */
            ret_val = JNI_TRUE;
         }
         else
            Env->ReleaseStringUTFChars(Path, PathString);
      }
   }

   SS1_LOGD("Exit (%d)", ret_val);
   return(ret_val);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(JNI_FALSE);
#endif
}
#endif

#if (SS1_PLATFORM_SDK_VERSION >= 14)
static jstring BTS_RegisterHealthApplicationNative(JNIEnv *Env, jobject Object, jint DataType, jstring Role, jstring Name, jstring ChannelType)
{
   //XXX
   return(NULL);
}
#endif

#if (SS1_PLATFORM_SDK_VERSION >= 14)
static jstring BTS_RegisterSinkHealthApplicationNative(JNIEnv *Env, jobject Object, jint DataType, jstring Role, jstring Name)
{
   //XXX
   return(NULL);
}
#endif

#if (SS1_PLATFORM_SDK_VERSION >= 14)
static jboolean BTS_UnregisterHealthApplicationNative(JNIEnv *Env, jobject Object, jstring Path)
{
   //XXX
   return(JNI_FALSE);
}
#endif

#if (SS1_PLATFORM_SDK_VERSION >= 14)
static jboolean BTS_CreateChannelNative(JNIEnv *Env, jobject Object, jstring DevicePath, jstring AppPath, jstring ChannelType, jint Code)
{
   //XXX
   return(JNI_FALSE);
}
#endif

#if (SS1_PLATFORM_SDK_VERSION >= 14)
static jboolean BTS_DestroyChannelNative(JNIEnv *Env, jobject Object, jstring DevicePath, jstring Channelpath, jint Code)
{
   //XXX
   return(JNI_FALSE);
}
#endif

#if (SS1_PLATFORM_SDK_VERSION >= 14)
static jstring BTS_GetMainChannelNative(JNIEnv *Env, jobject Object, jstring Path)
{
   //XXX
   return(NULL);
}
#endif

#if (SS1_PLATFORM_SDK_VERSION >= 14)
static jstring BTS_GetChannelApplicationNative(JNIEnv *Env, jobject Object, jstring ChannelPath)
{
   //XXX
   return(NULL);
}
#endif

#if (SS1_PLATFORM_SDK_VERSION >= 14)
static jobject BTS_GetChannelFdNative(JNIEnv *Env, jobject Object, jstring ChannelPath)
{
   /* Returns ParcelFileDescriptor */
   //XXX
   return(NULL);
}
#endif

#if (SS1_PLATFORM_SDK_VERSION >= 14)
static jboolean BTS_ReleaseChannelFdNative(JNIEnv *Env, jobject Object, jstring ChannelPath)
{
   //XXX
   return(JNI_FALSE);
}
#endif

#if (SS1_PLATFORM_SDK_VERSION >= 14)
static jboolean BTS_SetAuthorizationNative(JNIEnv *Env, jobject Object, jstring Address, jboolean Value, jint Data)
{
   SS1_LOGD("Enter");
#ifdef HAVE_BLUETOOTH
   int         Result;
   jboolean    ret_val;
   BD_ADDR_t   RemoteAddress;
   const char *AddressString;

   /* Set some default values in case anything fails.                   */
   ret_val = JNI_FALSE;

   if(Address)
   {
      if((AddressString = Env->GetStringUTFChars(Address, NULL)) != NULL)
      {
         if(StrToBD_ADDR(&RemoteAddress, AddressString))
         {
            Env->ReleaseStringUTFChars(Address, AddressString);

            switch(Data)
            {
#if SS1_SUPPORT_A2DP
               case AUTHORIZATION_PROFILE_A2DP:
                  if((Result = AUDM_Connection_Request_Response(acrStream, RemoteAddress, ((Value == JNI_FALSE) ? FALSE : TRUE))) == 0)
                  {
                     SS1_LOGD("A2DP connection request %s.", ((Value == JNI_FALSE) ? "rejected" : "accepted"));
                     ret_val = JNI_TRUE;
                  }
                  else
                     SS1_LOGD("Failed to respond to A2DP connection request (%d)", Result);

                  break;
               case AUTHORIZATION_PROFILE_AVRCP:
                  if((Result = AUDM_Connection_Request_Response(acrRemoteControl, RemoteAddress, ((Value == JNI_FALSE) ? FALSE : TRUE))) == 0)
                  {
                     SS1_LOGD("AVRCP connection request %s.", ((Value == JNI_FALSE) ? "rejected" : "accepted"));
                     ret_val = JNI_TRUE;
                  }
                  else
                     SS1_LOGD("Failed to respond to AVRCP connection request (%d)", Result);
#endif

                  break;
#if SS1_SUPPORT_PAN
               case AUTHORIZATION_PROFILE_PAN:
                  if((Result = PANM_Connection_Request_Response(RemoteAddress, ((Value == JNI_FALSE) ? FALSE : TRUE))) == 0)
                  {
                     SS1_LOGD("PAN connection request %s.", ((Value == JNI_FALSE) ? "rejected" : "accepted"));
                     ret_val = JNI_TRUE;
                  }
                  else
                     SS1_LOGD("Failed to respond to PAN connection request (%d)", Result);

                  break;
#endif

#if SS1_SUPPORT_HID
               case AUTHORIZATION_PROFILE_HID:
                  /* XXX: If an internal HID Report parser is added,    */
                  /* these connection flags must change.                */
                  if((Result = HIDM_Connection_Request_Response(RemoteAddress, ((Value == JNI_FALSE) ? FALSE : TRUE), HIDM_CONNECTION_REQUEST_CONNECTION_FLAGS_PARSE_BOOT)) == 0)
                  {
                     SS1_LOGD("HID connection request %s.", ((Value == JNI_FALSE) ? "rejected" : "accepted"));
                     ret_val = JNI_TRUE;
                  }
                  else
                     SS1_LOGD("Failed to respond to HID connection request (%d)", Result);

                  break;
#endif
            }
         }
         else
            Env->ReleaseStringUTFChars(Address, AddressString);
      }
   }

   SS1_LOGD("Exit (%d)", ret_val);
   return(ret_val);
#else /* HAVE_BLUETOOTH */
   SS1_LOGD("Exit (%d)", JNI_FALSE);
   return(JNI_FALSE);
#endif
}
#endif

   /* This array maps Java method signatures to native function         */
   /* addresses. The format is { Name : Signature : Function Pointer }. */
static JNINativeMethod NativeMethods[] =
{
   {"classInitNative",                    "()V",                                                                         (void *)BTS_ClassInitNative},
   {"initializeNativeDataNative",         "()V",                                                                         (void *)BTS_InitializeNativeDataNative},
   {"setupNativeDataNative",              "()Z",                                                                         (void *)BTS_SetupNativeDataNative},
   {"tearDownNativeDataNative",           "()Z",                                                                         (void *)BTS_TearDownNativeDataNative},
   {"cleanupNativeDataNative",            "()V",                                                                         (void *)BTS_CleanupNativeDataNative},
   {"getAdapterPathNative",               "()Ljava/lang/String;",                                                        (void *)BTS_GetAdapterPathNative},

   {"isEnabledNative",                    "()I",                                                                         (void *)BTS_IsEnabledNative},
   {"enableNative",                       "()I",                                                                         (void *)BTS_EnableNative},
   {"disableNative",                      "()I",                                                                         (void *)BTS_DisableNative},

   {"getAdapterPropertiesNative",         "()[Ljava/lang/Object;",                                                       (void *)BTS_GetAdapterPropertiesNative},
   {"getDevicePropertiesNative",          "(Ljava/lang/String;)[Ljava/lang/Object;",                                     (void *)BTS_GetDevicePropertiesNative},
   {"setAdapterPropertyStringNative",     "(Ljava/lang/String;Ljava/lang/String;)Z",                                     (void *)BTS_SetAdapterPropertyStringNative},
   {"setAdapterPropertyBooleanNative",    "(Ljava/lang/String;I)Z",                                                      (void *)BTS_SetAdapterPropertyBooleanNative},
   {"setAdapterPropertyIntegerNative",    "(Ljava/lang/String;I)Z",                                                      (void *)BTS_SetAdapterPropertyIntegerNative},
   {"setDevicePropertyBooleanNative",     "(Ljava/lang/String;Ljava/lang/String;I)Z",                                    (void *)BTS_SetDevicePropertyBooleanNative},
#if (SS1_PLATFORM_SDK_VERSION >= 14)
   {"setDevicePropertyStringNative",      "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z",                   (void *)BTS_SetDevicePropertyStringNative},
#endif

   {"startDiscoveryNative",               "()Z",                                                                         (void *)BTS_StartDiscoveryNative},
   {"stopDiscoveryNative",                "()Z",                                                                         (void *)BTS_StopDiscoveryNative},

   {"createDeviceNative",                 "(Ljava/lang/String;)Z",                                                       (void *)BTS_CreateDeviceNative},
   {"createPairedDeviceNative",           "(Ljava/lang/String;I)Z",                                                      (void *)BTS_CreatePairedDeviceNative},
   {"cancelDeviceCreationNative",         "(Ljava/lang/String;)Z",                                                       (void *)BTS_CancelDeviceCreationNative},
   {"removeDeviceNative",                 "(Ljava/lang/String;)Z",                                                       (void *)BTS_RemoveDeviceNative},

   {"setPairingConfirmationNative",       "(Ljava/lang/String;ZI)Z",                                                     (void *)BTS_SetPairingConfirmationNative},
   {"setPasskeyNative",                   "(Ljava/lang/String;II)Z",                                                     (void *)BTS_SetPasskeyNative},
   {"setPinNative",                       "(Ljava/lang/String;Ljava/lang/String;I)Z",                                    (void *)BTS_SetPinNative},
   {"cancelPairingUserInputNative",       "(Ljava/lang/String;I)Z",                                                      (void *)BTS_CancelPairingUserInputNative},

   {"discoverServicesNative",             "(Ljava/lang/String;Ljava/lang/String;)Z",                                     (void *)BTS_DiscoverServicesNative},
   {"getDeviceServiceChannelNative",      "(Ljava/lang/String;Ljava/lang/String;I)I",                                    (void *)BTS_GetDeviceServiceChannelNative},

   {"addRfcommServiceRecordNative",       "(Ljava/lang/String;JJS)I",                                                    (void *)BTS_AddRfcommServiceRecordNative},
   {"removeServiceRecordNative",          "(I)Z",                                                                        (void *)BTS_RemoveServiceRecordNative},
#if (SS1_PLATFORM_SDK_VERSION >= 14)
   {"addReservedServiceRecordsNative",    "([I)[I",                                                                      (void *)BTS_AddReservedServiceRecordsNative},
   {"removeReservedServiceRecordsNative", "([I)Z",                                                                       (void *)BTS_RemoveReservedServiceRecordsNative},
#endif
#if (SS1_PLATFORM_SDK_VERSION >= 8)
   {"setLinkTimeoutNative",               "(Ljava/lang/String;I)Z",                                                      (void *)BTS_SetLinkTimeoutNative},
#endif

#if (SS1_PLATFORM_SDK_VERSION >= 14)
   /* HID functions                                                     */
   {"connectInputDeviceNative",           "(Ljava/lang/String;)Z",                                                       (void *)BTS_ConnectInputDeviceNative},
   {"disconnectInputDeviceNative",        "(Ljava/lang/String;)Z",                                                       (void *)BTS_DisconnectInputDeviceNative},

   /* PAN functions                                                     */
   {"setBluetoothTetheringNative",        "(ZLjava/lang/String;Ljava/lang/String;)Z",                                    (void *)BTS_SetBluetoothTetheringNative},
   {"connectPanDeviceNative",             "(Ljava/lang/String;Ljava/lang/String;)Z",                                     (void *)BTS_ConnectPanDeviceNative},
   {"disconnectPanDeviceNative",          "(Ljava/lang/String;)Z",                                                       (void *)BTS_DisconnectPanDeviceNative},
   {"disconnectPanServerDeviceNative",    "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z",                   (void *)BTS_DisconnectPanServerDeviceNative},

   /* Health function                                                   */
   {"registerHealthApplicationNative",    "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", (void *)BTS_RegisterHealthApplicationNative},
   {"registerHealthApplicationNative",    "(ILjava/lang/String;Ljava/lang/String;)Ljava/lang/String;",                   (void *)BTS_RegisterSinkHealthApplicationNative},
   {"unregisterHealthApplicationNative",  "(Ljava/lang/String;)Z",                                                       (void *)BTS_UnregisterHealthApplicationNative},
   {"createChannelNative",                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)Z",                  (void *)BTS_CreateChannelNative},
   {"destroyChannelNative",               "(Ljava/lang/String;Ljava/lang/String;I)Z",                                    (void *)BTS_DestroyChannelNative},
   {"getMainChannelNative",               "(Ljava/lang/String;)Ljava/lang/String;",                                      (void *)BTS_GetMainChannelNative},
   {"getChannelApplicationNative",        "(Ljava/lang/String;)Ljava/lang/String;",                                      (void *)BTS_GetChannelApplicationNative},
   {"getChannelFdNative",                 "(Ljava/lang/String;)Landroid/os/ParcelFileDescriptor;",                       (void *)BTS_GetChannelFdNative},
   {"releaseChannelFdNative",             "(Ljava/lang/String;)Z",                                                       (void *)BTS_ReleaseChannelFdNative},

   {"setAuthorizationNative",             "(Ljava/lang/String;ZI)Z",                                                     (void *)BTS_SetAuthorizationNative},
#endif
};

   /* The following function is used by android_runtime at load-time to */
   /* trigger the registration of the method implementations in this    */
   /* module with the JNI subsystem.                                    */
int SS1API register_android_server_BluetoothService(JNIEnv *Env)
{
   return(AndroidRuntime::registerNativeMethods(Env, "android/server/BluetoothService", NativeMethods, NELEM(NativeMethods)));
}

#ifdef HAVE_BLUETOOTH

jboolean BTS_CallIsRemoteDeviceInCache(JNIEnv *Env, jobject BTSObject, jstring Address)
{
   if(Env && BTSObject && Address)
      return(Env->CallBooleanMethod(BTSObject, Method_isRemoteDeviceInCache, Address));
   else
      return(JNI_FALSE);
}

#if (SS1_PLATFORM_SDK_VERSION < 14)

void BTS_CallAddRemoteDeviceProperties(JNIEnv *Env, jobject BTSObject, jstring Address, jobject PropArray)
{
   if(Env && BTSObject && Address && PropArray)
      Env->CallVoidMethod(BTSObject, Method_addRemoteDeviceProperties, Address, PropArray);
}

jstring BTS_CallGetRemoteDeviceProperty(JNIEnv *Env, jobject BTSObject, jstring Address, jstring Property)
{
   if(Env && BTSObject && Address && Property)
      return((jstring)Env->CallObjectMethod(BTSObject, Method_getRemoteDeviceProperty, Address, Property));
   else
      return(NULL);
}

#endif

#endif /* HAVE_BLUETOOTH */

} /* namespace android */
