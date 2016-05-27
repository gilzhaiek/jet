/*****< com_stonestreetone_bluetopiapm.cpp >***********************************/
/*      Copyright 2010 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  com_stonestreetone_bluetopiapm - Utility function implementations for the */
/*                                   Stonestreet One Bluetopia Platform       */
/*                                   Manager Java API JNI modules.            */
/*                                                                            */
/*  Author:  Greg Hensley                                                     */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   10/15/11  G. Hensley     Initial creation.                               */
/******************************************************************************/

#include <jni.h>
#include <pthread.h>
#include <stdlib.h>
#include <time.h>
#include <unistd.h>

#include "com_stonestreetone_bluetopiapm.h"
#include "com_stonestreetone_bluetopiapm_SDPM_DataElement.h"

#ifdef __ANDROID_TREE__
#include "SS1CFG.h"
#endif

#define MAXIMUM_ATTRIBUTE_RECURSION_FAILSAFE 0x0100
#define INIT_MAXIMUM_RETRIES                 3
#define INIT_TIMEOUT_MS                      50

typedef struct _tagIPCLostCallbackParameter_t
{
   BTPM_Server_UnRegistration_Callback_t Callback;
   void                                 *Parameter;
} IPCLostCallbackParameter_t;

   /* State tracking structure for SDP Data Element parsing.            */
typedef struct _tagParseDataElementState_t
{
      jclass    URIClass;
      jclass    UUIDClass;
      jclass    StringClass;
      jclass    ByteArrayClass;
      jclass    DataElementClass;
      jclass    DataElementArrayClass;
      jfieldID  UuidDataField;
      jfieldID  DataElementTypeField;
      jfieldID  DataElementNumericDataField;
      jfieldID  DataElementGenericDataField;
      jmethodID URIMethod_toASCIIString;
} ParseDataElementState_t;

   /* Prototype for the native module registration function each module */
   /* must implement.                                                   */
typedef int (*RegistrationFunction)(JNIEnv *);


static JavaVM *JVM;

static WEAK_REF        BluetoothAddressClass_WeakRef;
static jmethodID       BluetoothAddressConstructor_MethodID;
static pthread_mutex_t BluetoothAddressClassMutex = PTHREAD_MUTEX_INITIALIZER;

   /* BTPM connections are shared across the process, so we use global  */
   /* storage to track connection state.                                */
static volatile Boolean_t         BTPMServiceConnected       = FALSE;
static volatile unsigned int      BTPMServiceClientCount     = 0;
static pthread_mutex_t            BTPMServiceConnectedMutex  = PTHREAD_MUTEX_INITIALIZER;

   /* Module registration functions.                                    */
extern int register_com_stonestreetone_bluetopiapm_ANCM(JNIEnv *);
extern int register_com_stonestreetone_bluetopiapm_ANPM(JNIEnv *);
extern int register_com_stonestreetone_bluetopiapm_ANTM(JNIEnv *);
extern int register_com_stonestreetone_bluetopiapm_AUDM(JNIEnv *);
extern int register_com_stonestreetone_bluetopiapm_AVRCP(JNIEnv *);
extern int register_com_stonestreetone_bluetopiapm_BASM(JNIEnv *);
extern int register_com_stonestreetone_bluetopiapm_BLPM(JNIEnv *);
extern int register_com_stonestreetone_bluetopiapm_DEVM(JNIEnv *);
extern int register_com_stonestreetone_bluetopiapm_FMPM(JNIEnv *);
extern int register_com_stonestreetone_bluetopiapm_FTPM(JNIEnv *);
extern int register_com_stonestreetone_bluetopiapm_GATM(JNIEnv *);
extern int register_com_stonestreetone_bluetopiapm_GLPM(JNIEnv *);
extern int register_com_stonestreetone_bluetopiapm_HDDM(JNIEnv *);
extern int register_com_stonestreetone_bluetopiapm_HDPM(JNIEnv *);
extern int register_com_stonestreetone_bluetopiapm_HDSM(JNIEnv *);
extern int register_com_stonestreetone_bluetopiapm_HFRM(JNIEnv *);
extern int register_com_stonestreetone_bluetopiapm_HIDM(JNIEnv *);
extern int register_com_stonestreetone_bluetopiapm_HRPM(JNIEnv *);
extern int register_com_stonestreetone_bluetopiapm_HTPM(JNIEnv *);
extern int register_com_stonestreetone_bluetopiapm_MAPM(JNIEnv *);
extern int register_com_stonestreetone_bluetopiapm_PANM(JNIEnv *);
extern int register_com_stonestreetone_bluetopiapm_PASM(JNIEnv *);
extern int register_com_stonestreetone_bluetopiapm_PBAM(JNIEnv *);
extern int register_com_stonestreetone_bluetopiapm_PXPM(JNIEnv *);
extern int register_com_stonestreetone_bluetopiapm_SBC(JNIEnv *);
extern int register_com_stonestreetone_bluetopiapm_SPPM(JNIEnv *);
extern int register_com_stonestreetone_bluetopiapm_TIPM(JNIEnv *);

static RegistrationFunction RegFuncs[] = {
   register_com_stonestreetone_bluetopiapm_DEVM,
   register_com_stonestreetone_bluetopiapm_SPPM,
#ifdef SS1_MODULE_ENABLED_ANCM
   register_com_stonestreetone_bluetopiapm_ANCM,
#endif
#ifdef SS1_MODULE_ENABLED_ANPM
   register_com_stonestreetone_bluetopiapm_ANPM,
#endif
#ifdef SS1_MODULE_ENABLED_ANTM
   register_com_stonestreetone_bluetopiapm_ANTM,
#endif
#ifdef SS1_MODULE_ENABLED_AUDM
   register_com_stonestreetone_bluetopiapm_AUDM,
   register_com_stonestreetone_bluetopiapm_AVRCP,
#endif
#ifdef SS1_MODULE_ENABLED_BASM
   register_com_stonestreetone_bluetopiapm_BASM,
#endif
#ifdef SS1_MODULE_ENABLED_BLPM
   register_com_stonestreetone_bluetopiapm_BLPM,
#endif
#ifdef SS1_MODULE_ENABLED_FMPM
   register_com_stonestreetone_bluetopiapm_FMPM,
#endif
#ifdef SS1_MODULE_ENABLED_FTPM
   register_com_stonestreetone_bluetopiapm_FTPM,
#endif
#ifdef SS1_MODULE_ENABLED_GATM
   register_com_stonestreetone_bluetopiapm_GATM,
#endif
#ifdef SS1_MODULE_ENABLED_GLPM
   register_com_stonestreetone_bluetopiapm_GLPM,
#endif
#ifdef SS1_MODULE_ENABLED_HDDM
   register_com_stonestreetone_bluetopiapm_HDDM,
#endif
#ifdef SS1_MODULE_ENABLED_HDPM
   register_com_stonestreetone_bluetopiapm_HDPM,
#endif
#ifdef SS1_MODULE_ENABLED_HDSM
   register_com_stonestreetone_bluetopiapm_HDSM,
#endif
#ifdef SS1_MODULE_ENABLED_HFRM
   register_com_stonestreetone_bluetopiapm_HFRM,
#endif
#ifdef SS1_MODULE_ENABLED_HIDM
   register_com_stonestreetone_bluetopiapm_HIDM,
#endif
#ifdef SS1_MODULE_ENABLED_HRPM
   register_com_stonestreetone_bluetopiapm_HRPM,
#endif
#ifdef SS1_MODULE_ENABLED_HTPM
   register_com_stonestreetone_bluetopiapm_HTPM,
#endif
#ifdef SS1_MODULE_ENABLED_MAPM
   register_com_stonestreetone_bluetopiapm_MAPM,
#endif
#ifdef SS1_MODULE_ENABLED_PANM
   register_com_stonestreetone_bluetopiapm_PANM,
#endif
#ifdef SS1_MODULE_ENABLED_PASM
   register_com_stonestreetone_bluetopiapm_PASM,
#endif
#ifdef SS1_MODULE_ENABLED_PBAM
   register_com_stonestreetone_bluetopiapm_PBAM,
#endif
#ifdef SS1_MODULE_ENABLED_PXPM
   register_com_stonestreetone_bluetopiapm_PXPM,
#endif
#ifdef SS1_MODULE_ENABLED_SBC
   register_com_stonestreetone_bluetopiapm_SBC,
#endif
#ifdef SS1_MODULE_ENABLED_TIPM
   register_com_stonestreetone_bluetopiapm_TIPM,
#endif
};

#define REFERENCE_COUNT_EXCLUSIVE   ((DWord_t)(-1))
#define REFERENCE_COUNT_MAX         ((DWord_t)(REFERENCE_COUNT_EXCLUSIVE - 1))

typedef struct _tagCountedReference_t {
   DWord_t  Count;
   void    *Reference;
} CountedReference_t;

Event_t ReleasedReferenceEvent;

   /* The following function is responsible for noting when the IPC     */
   /* connection to the BluetopiaPM service is lost. It will also       */
   /* call a additional callback if one has been registered via         */
   /* InitBTPMClient().                                                 */
static void IPCLostCallback(void *CallbackParameter)
{
   if(pthread_mutex_lock(&BTPMServiceConnectedMutex) == 0)
   {
      BTPMServiceConnected = FALSE;
      pthread_mutex_unlock(&BTPMServiceConnectedMutex);
   }
   else
   {
      /* Having the mutex is nice, but set the flag anyway even when the*/
      /* mutex throws an error. If the mutex is working, we'll never hit*/
      /* this, so it's only an emergency fallback.                      */
      BTPMServiceConnected = FALSE;
   }
}

static jclass GetBluetoothAddressClassWithConstructor(JNIEnv *Env, jmethodID *BluetoothAddressConstructor)
{
   jclass BluetoothAddressClass;

   if(pthread_mutex_lock(&BluetoothAddressClassMutex) == 0)
   {
      BluetoothAddressClass = (jclass)(Env->NewLocalRef(BluetoothAddressClass_WeakRef));

      if((BluetoothAddressClass == NULL) && (Env->IsSameObject(BluetoothAddressClass_WeakRef, NULL) == JNI_TRUE))
      {
         if(BluetoothAddressClass_WeakRef != NULL)
         {
            DeleteWeakRef(Env, BluetoothAddressClass_WeakRef);
            BluetoothAddressClass_WeakRef = NULL;
         }

         if((BluetoothAddressClass = Env->FindClass("com/stonestreetone/bluetopiapm/BluetoothAddress")) != NULL)
         {
            BluetoothAddressClass_WeakRef = NewWeakRef(Env, BluetoothAddressClass);
            BluetoothAddressConstructor_MethodID = Env->GetMethodID(BluetoothAddressClass, "<init>", "(BBBBBB)V");
         }
      }

      if((BluetoothAddressClass) && (BluetoothAddressConstructor))
         *BluetoothAddressConstructor = BluetoothAddressConstructor_MethodID;

      pthread_mutex_unlock(&BluetoothAddressClassMutex);
   }
   else
   {
      PRINT_DEBUG("Unable to obtain mutex protecting local class reference to BluetoothAddress");
      BluetoothAddressClass = NULL;
   }

   return BluetoothAddressClass;
}

static Boolean_t ParseDataElementHelper(JNIEnv *Env, jobject JavaDataElement, SDP_Data_Element_t *DataElement, unsigned int Depth, const ParseDataElementState_t &State)
{
   jint                Type;
   jbyte              *ByteArray;
   jlong               NumericData;
   jobject             TargetData;
   jobject             GenericData;
   Boolean_t           ret_val;
   const char         *JStringChars;
   unsigned int        Index;

   if(Depth < MAXIMUM_ATTRIBUTE_RECURSION_FAILSAFE)
   {
      if((Env) && (JavaDataElement) && (DataElement))
      {
         if(Env->IsInstanceOf(JavaDataElement, State.DataElementClass))
         {
            Type = Env->GetIntField(JavaDataElement, State.DataElementTypeField);

            if((DataElement = (SDP_Data_Element_t *)calloc(1, sizeof(SDP_Data_Element_t))) != NULL)
            {
               switch(Type)
               {
                  case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_NIL:
                     DataElement->SDP_Data_Element_Type   = deNIL;
                     DataElement->SDP_Data_Element_Length = 0;

                     ret_val = TRUE;
                     break;
                  case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_UNSIGNED_INT_1_BYTE:
                     NumericData = Env->GetLongField(JavaDataElement, State.DataElementNumericDataField);

                     DataElement->SDP_Data_Element_Type   = deUnsignedInteger1Byte;
                     DataElement->SDP_Data_Element_Length = sizeof(DataElement->SDP_Data_Element.UnsignedInteger1Byte);
                     DataElement->SDP_Data_Element.UnsignedInteger1Byte = (Byte_t)NumericData;

                     ret_val = TRUE;
                     break;
                  case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_UNSIGNED_INT_2_BYTE:
                     NumericData = Env->GetLongField(JavaDataElement, State.DataElementNumericDataField);

                     DataElement->SDP_Data_Element_Type   = deUnsignedInteger2Bytes;
                     DataElement->SDP_Data_Element_Length = sizeof(DataElement->SDP_Data_Element.UnsignedInteger2Bytes);
                     DataElement->SDP_Data_Element.UnsignedInteger2Bytes = (Word_t)NumericData;

                     ret_val = TRUE;
                     break;
                  case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_UNSIGNED_INT_4_BYTE:
                     NumericData = Env->GetLongField(JavaDataElement, State.DataElementNumericDataField);

                     DataElement->SDP_Data_Element_Type   = deUnsignedInteger4Bytes;
                     DataElement->SDP_Data_Element_Length = 1;
                     DataElement->SDP_Data_Element.UnsignedInteger4Bytes = (DWord_t)NumericData;

                     ret_val = TRUE;
                     break;
                  case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_UNSIGNED_INT_8_BYTE:
                     NumericData = Env->GetLongField(JavaDataElement, State.DataElementNumericDataField);

                     DataElement->SDP_Data_Element_Type   = deUnsignedInteger8Bytes;
                     DataElement->SDP_Data_Element_Length = 1;
                     DataElement->SDP_Data_Element.UnsignedInteger8Bytes[0] = (Byte_t)((NumericData >> (8 * 7)) & 0x0FF);
                     DataElement->SDP_Data_Element.UnsignedInteger8Bytes[1] = (Byte_t)((NumericData >> (8 * 6)) & 0x0FF);
                     DataElement->SDP_Data_Element.UnsignedInteger8Bytes[2] = (Byte_t)((NumericData >> (8 * 5)) & 0x0FF);
                     DataElement->SDP_Data_Element.UnsignedInteger8Bytes[3] = (Byte_t)((NumericData >> (8 * 4)) & 0x0FF);
                     DataElement->SDP_Data_Element.UnsignedInteger8Bytes[4] = (Byte_t)((NumericData >> (8 * 3)) & 0x0FF);
                     DataElement->SDP_Data_Element.UnsignedInteger8Bytes[5] = (Byte_t)((NumericData >> (8 * 2)) & 0x0FF);
                     DataElement->SDP_Data_Element.UnsignedInteger8Bytes[6] = (Byte_t)((NumericData >> (8 * 1)) & 0x0FF);
                     DataElement->SDP_Data_Element.UnsignedInteger8Bytes[7] = (Byte_t)((NumericData >> (8 * 0)) & 0x0FF);

                     ret_val = TRUE;
                     break;
                  case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_UNSIGNED_INT_16_BYTE:
                     if((GenericData = Env->GetObjectField(JavaDataElement, State.DataElementGenericDataField)) != NULL)
                     {
                        if(Env->IsInstanceOf(GenericData, State.ByteArrayClass))
                        {
                           if((ByteArray = Env->GetByteArrayElements((jbyteArray)GenericData, 0)) != NULL)
                           {
                              DataElement->SDP_Data_Element_Type   = deUnsignedInteger16Bytes;
                              DataElement->SDP_Data_Element_Length = 1;
                              memcpy(DataElement->SDP_Data_Element.UnsignedInteger16Bytes, ByteArray, 16);

                              ret_val = TRUE;

                              Env->ReleaseByteArrayElements((jbyteArray)GenericData, ByteArray, JNI_ABORT);
                           }
                           else
                              ret_val = FALSE;
                        }
                        else
                           ret_val = FALSE;

                        Env->DeleteLocalRef(GenericData);
                     }
                     else
                        ret_val = FALSE;

                     break;
                  case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_SIGNED_INT_1_BYTE:
                     NumericData = Env->GetLongField(JavaDataElement, State.DataElementNumericDataField);

                     DataElement->SDP_Data_Element_Type   = deSignedInteger1Byte;
                     DataElement->SDP_Data_Element_Length = 1;
                     DataElement->SDP_Data_Element.SignedInteger1Byte = (SByte_t)NumericData;

                     ret_val = TRUE;
                     break;
                  case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_SIGNED_INT_2_BYTE:
                     NumericData = Env->GetLongField(JavaDataElement, State.DataElementNumericDataField);

                     DataElement->SDP_Data_Element_Type   = deSignedInteger2Bytes;
                     DataElement->SDP_Data_Element_Length = 1;
                     DataElement->SDP_Data_Element.SignedInteger2Bytes = (SWord_t)NumericData;

                     ret_val = TRUE;
                     break;
                  case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_SIGNED_INT_4_BYTE:
                     NumericData = Env->GetLongField(JavaDataElement, State.DataElementNumericDataField);

                     DataElement->SDP_Data_Element_Type   = deSignedInteger4Bytes;
                     DataElement->SDP_Data_Element_Length = 1;
                     DataElement->SDP_Data_Element.SignedInteger4Bytes = (SDWord_t)NumericData;

                     ret_val = TRUE;
                     break;
                  case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_SIGNED_INT_8_BYTE:
                     NumericData = Env->GetLongField(JavaDataElement, State.DataElementNumericDataField);

                     DataElement->SDP_Data_Element_Type   = deSignedInteger8Bytes;
                     DataElement->SDP_Data_Element_Length = 1;
                     DataElement->SDP_Data_Element.UnsignedInteger8Bytes[0] = (Byte_t)((NumericData >> (8 * 7)) & 0x0FF);
                     DataElement->SDP_Data_Element.UnsignedInteger8Bytes[1] = (Byte_t)((NumericData >> (8 * 6)) & 0x0FF);
                     DataElement->SDP_Data_Element.UnsignedInteger8Bytes[2] = (Byte_t)((NumericData >> (8 * 5)) & 0x0FF);
                     DataElement->SDP_Data_Element.UnsignedInteger8Bytes[3] = (Byte_t)((NumericData >> (8 * 4)) & 0x0FF);
                     DataElement->SDP_Data_Element.UnsignedInteger8Bytes[4] = (Byte_t)((NumericData >> (8 * 3)) & 0x0FF);
                     DataElement->SDP_Data_Element.UnsignedInteger8Bytes[5] = (Byte_t)((NumericData >> (8 * 2)) & 0x0FF);
                     DataElement->SDP_Data_Element.UnsignedInteger8Bytes[6] = (Byte_t)((NumericData >> (8 * 1)) & 0x0FF);
                     DataElement->SDP_Data_Element.UnsignedInteger8Bytes[7] = (Byte_t)((NumericData >> (8 * 0)) & 0x0FF);

                     ret_val = TRUE;
                     break;
                  case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_SIGNED_INT_16_BYTE:
                     if((GenericData = Env->GetObjectField(JavaDataElement, State.DataElementGenericDataField)) != NULL)
                     {
                        if(Env->IsInstanceOf(GenericData, State.ByteArrayClass))
                        {
                           if((ByteArray = Env->GetByteArrayElements((jbyteArray)GenericData, 0)) != NULL)
                           {
                              DataElement->SDP_Data_Element_Type   = deSignedInteger16Bytes;
                              DataElement->SDP_Data_Element_Length = 1;
                              memcpy(DataElement->SDP_Data_Element.SignedInteger16Bytes, ByteArray, 16);

                              ret_val = TRUE;

                              Env->ReleaseByteArrayElements((jbyteArray)GenericData, ByteArray, JNI_ABORT);
                           }
                           else
                              ret_val = FALSE;
                        }
                        else
                           ret_val = FALSE;

                        Env->DeleteLocalRef(GenericData);
                     }
                     else
                        ret_val = FALSE;

                     break;
                  case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_TEXT_STRING:
                     if((GenericData = Env->GetObjectField(JavaDataElement, State.DataElementGenericDataField)) != NULL)
                     {
                        if(Env->IsInstanceOf(GenericData, State.StringClass))
                        {
                           if((JStringChars = Env->GetStringUTFChars((jstring)GenericData, 0)) != NULL)
                           {
                              DataElement->SDP_Data_Element_Type   = deTextString;
                              DataElement->SDP_Data_Element_Length = Env->GetStringUTFLength((jstring)GenericData);

                              if((DataElement->SDP_Data_Element.TextString = (Byte_t *)calloc(DataElement->SDP_Data_Element_Length, sizeof(char))) != NULL)
                              {
                                 memcpy(DataElement->SDP_Data_Element.TextString, JStringChars, DataElement->SDP_Data_Element_Length);

                                 ret_val = TRUE;
                              }
                              else
                                 ret_val = FALSE;

                              Env->ReleaseStringUTFChars((jstring)GenericData, JStringChars);
                           }
                           else
                              ret_val = FALSE;
                        }
                        else
                           ret_val = FALSE;

                        Env->DeleteLocalRef(GenericData);
                     }
                     else
                        ret_val = FALSE;

                     break;
                  case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_BOOLEAN:
                     NumericData = Env->GetLongField(JavaDataElement, State.DataElementNumericDataField);

                     DataElement->SDP_Data_Element_Type   = deBoolean;
                     DataElement->SDP_Data_Element_Length = 2;
                     DataElement->SDP_Data_Element.Boolean = ((NumericData == 0) ? FALSE : TRUE);

                     ret_val = TRUE;
                     break;
                  case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_URL:
                     if((GenericData = Env->GetObjectField(JavaDataElement, State.DataElementGenericDataField)) != NULL)
                     {
                        if(Env->IsInstanceOf(GenericData, State.URIClass))
                        {
                           TargetData = Env->CallObjectMethod(GenericData, State.URIMethod_toASCIIString);
                           if(!(Env->ExceptionCheck()))
                           {
                              if((JStringChars = Env->GetStringUTFChars((jstring)TargetData, 0)) != NULL)
                              {
                                 DataElement->SDP_Data_Element_Type   = deURL;
                                 DataElement->SDP_Data_Element_Length = Env->GetStringUTFLength((jstring)GenericData);

                                 if((DataElement->SDP_Data_Element.TextString = (Byte_t *)calloc(DataElement->SDP_Data_Element_Length, sizeof(char))) != NULL)
                                 {
                                    memcpy(DataElement->SDP_Data_Element.TextString, JStringChars, DataElement->SDP_Data_Element_Length);

                                    ret_val = TRUE;
                                 }
                                 else
                                    ret_val = FALSE;

                                 Env->ReleaseStringUTFChars((jstring)TargetData, JStringChars);
                              }
                              else
                                 ret_val = FALSE;

                              Env->DeleteLocalRef(TargetData);
                           }
                           else
                           {
                              Env->ExceptionDescribe();
                              Env->ExceptionClear();

                              ret_val = FALSE;
                           }
                        }
                        else
                           ret_val = FALSE;

                        Env->DeleteLocalRef(GenericData);
                     }
                     else
                        ret_val = FALSE;

                     break;
                  case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_UUID_16:
                  case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_UUID_32:
                  case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_UUID_128:
                     if((GenericData = Env->GetObjectField(JavaDataElement, State.DataElementGenericDataField)) != NULL)
                     {
                        if(Env->IsInstanceOf(GenericData, State.UUIDClass))
                        {
                           if((TargetData = Env->GetObjectField(GenericData, State.UuidDataField)) != NULL)
                           {
                              if((ByteArray = Env->GetByteArrayElements((jbyteArray)TargetData, 0)) != NULL)
                              {
                                 switch(Env->GetArrayLength((jbyteArray)TargetData))
                                 {
                                    case 16:
                                       DataElement->SDP_Data_Element_Type   = deUUID_128;
                                       DataElement->SDP_Data_Element_Length = sizeof(DataElement->SDP_Data_Element.UUID_128);
                                       ASSIGN_SDP_UUID_128(DataElement->SDP_Data_Element.UUID_128, ByteArray[0], ByteArray[1], ByteArray[2], ByteArray[3], ByteArray[4], ByteArray[5], ByteArray[6], ByteArray[7], ByteArray[8], ByteArray[9], ByteArray[10], ByteArray[11], ByteArray[12], ByteArray[13], ByteArray[14], ByteArray[15]);

                                       ret_val = TRUE;
                                       break;
                                    case 4:
                                       DataElement->SDP_Data_Element_Type   = deUUID_32;
                                       DataElement->SDP_Data_Element_Length = sizeof(DataElement->SDP_Data_Element.UUID_32);
                                       ASSIGN_SDP_UUID_32(DataElement->SDP_Data_Element.UUID_32, ByteArray[0], ByteArray[1], ByteArray[2], ByteArray[3]);

                                       ret_val = TRUE;
                                       break;
                                    case 2:
                                       DataElement->SDP_Data_Element_Type   = deUUID_16;
                                       DataElement->SDP_Data_Element_Length = sizeof(DataElement->SDP_Data_Element.UUID_16);
                                       ASSIGN_SDP_UUID_16(DataElement->SDP_Data_Element.UUID_16, ByteArray[0], ByteArray[1]);

                                       ret_val = TRUE;
                                       break;
                                    default:
                                       ret_val = FALSE;
                                 }

                                 Env->ReleaseByteArrayElements((jbyteArray)TargetData, ByteArray, JNI_ABORT);
                              }
                              else
                                 ret_val = FALSE;

                              Env->DeleteLocalRef(TargetData);
                           }
                           else
                              ret_val = FALSE;
                        }
                        else
                           ret_val = FALSE;

                        Env->DeleteLocalRef(GenericData);
                     }
                     else
                        ret_val = FALSE;

                     break;
                  case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_SEQUENCE:
                     if((GenericData = Env->GetObjectField(JavaDataElement, State.DataElementGenericDataField)) != NULL)
                     {
                        if(Env->IsInstanceOf(GenericData, State.DataElementArrayClass))
                        {
                           DataElement->SDP_Data_Element_Type   = deSequence;
                           DataElement->SDP_Data_Element_Length = Env->GetArrayLength((jobjectArray)GenericData);

                           if(DataElement->SDP_Data_Element_Length > 0)
                           {
                              DataElement->SDP_Data_Element.SDP_Data_Element_Sequence = (SDP_Data_Element_t *)calloc(DataElement->SDP_Data_Element_Length, sizeof(SDP_Data_Element_t));

                              /* Assume success at this point. If anything */
                              /* fails, we'll immediately bail out of the  */
                              /* loop.                                     */
                              ret_val = TRUE;

                              for(Index = 0; ((Index < DataElement->SDP_Data_Element_Length) && (ret_val == TRUE)); Index++)
                              {
                                 /* Allocate a new JavaVM reference frame. */
                                 /* This protects against exceeding the    */
                                 /* local reference allocation limit due to*/
                                 /* the recursion. The frame size accounts */
                                 /* for the immediate reference we create  */
                                 /* to the array member as well as any     */
                                 /* incidental references the next call to */
                                 /* ParseDataElementHelper may create (up  */
                                 /* to 3 references, currently).           */
                                 if(Env->PushLocalFrame(4) == 0)
                                 {
                                    if((TargetData = Env->GetObjectArrayElement((jobjectArray)GenericData, Index)) != NULL)
                                    {
                                       if(Env->IsInstanceOf(TargetData, State.DataElementClass))
                                          ret_val = ParseDataElementHelper(Env, TargetData, &(DataElement->SDP_Data_Element.SDP_Data_Element_Sequence[Index]), (Depth + 1), State);
                                       else
                                          ret_val = FALSE;
                                    }
                                    else
                                    {
                                       if(Env->ExceptionCheck())
                                       {
                                          Env->ExceptionDescribe();
                                          Env->ExceptionClear();

                                          ret_val = FALSE;
                                       }
                                    }

                                    /* Pop the local frame. This takes care*/
                                    /* of cleaning up the TargetData       */
                                    /* reference to the array member, too. */
                                    Env->PopLocalFrame(0);
                                 }
                              }
                           }
                           else
                           {
                              /* This is an empty sequence. This is     */
                              /* valid, so return success.              */
                              ret_val = TRUE;
                           }
                        }
                        else
                           ret_val = FALSE;

                        Env->DeleteLocalRef(GenericData);
                     }
                     else
                        ret_val = FALSE;

                     break;
                  case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_ALTERNATIVE:
                     if((GenericData = Env->GetObjectField(JavaDataElement, State.DataElementGenericDataField)) != NULL)
                     {
                        if(Env->IsInstanceOf(GenericData, State.DataElementArrayClass))
                        {
                           DataElement->SDP_Data_Element_Type   = deAlternative;
                           DataElement->SDP_Data_Element_Length = Env->GetArrayLength((jobjectArray)GenericData);

                           if(DataElement->SDP_Data_Element_Length > 0)
                           {
                              DataElement->SDP_Data_Element.SDP_Data_Element_Sequence = (SDP_Data_Element_t *)calloc(DataElement->SDP_Data_Element_Length, sizeof(SDP_Data_Element_t));

                              /* Assume success at this point. If anything */
                              /* fails, we'll immediately bail out of the  */
                              /* loop.                                     */
                              ret_val = TRUE;

                              for(Index = 0; ((Index < DataElement->SDP_Data_Element_Length) && (ret_val == TRUE)); Index++)
                              {
                                 /* Allocate a new JavaVM reference frame. */
                                 /* This protects against exceeding the    */
                                 /* local reference allocation limit due to*/
                                 /* the recursion. The frame size accounts */
                                 /* for the immediate reference we create  */
                                 /* to the array member as well as any     */
                                 /* incidental references the next call to */
                                 /* ParseDataElementHelper may create (up  */
                                 /* to 3 references, currently).           */
                                 if(Env->PushLocalFrame(4) == 0)
                                 {
                                    if((TargetData = Env->GetObjectArrayElement((jobjectArray)GenericData, Index)) != NULL)
                                    {
                                       if(Env->IsInstanceOf(TargetData, State.DataElementClass))
                                          ret_val = ParseDataElementHelper(Env, TargetData, &(DataElement->SDP_Data_Element.SDP_Data_Element_Sequence[Index]), (Depth + 1), State);
                                       else
                                          ret_val = FALSE;
                                    }
                                    else
                                    {
                                       if(Env->ExceptionCheck())
                                       {
                                          Env->ExceptionDescribe();
                                          Env->ExceptionClear();

                                          ret_val = FALSE;
                                       }
                                    }

                                    /* Pop the local frame. This takes care*/
                                    /* of cleaning up the TargetData       */
                                    /* reference to the array member, too. */
                                    Env->PopLocalFrame(0);
                                 }
                              }
                           }
                           else
                              ret_val = TRUE;
                        }
                        else
                           ret_val = FALSE;

                        Env->DeleteLocalRef(GenericData);
                     }
                     else
                        ret_val = FALSE;

                     break;
                  default:
                     DataElement->SDP_Data_Element_Type   = deNULL;
                     DataElement->SDP_Data_Element_Length = 0;
                     
                     ret_val = TRUE;
                     break;
               }
            }
            else
               ret_val = FALSE;
         }
         else
            ret_val = FALSE;
      }
      else
         ret_val = FALSE;
   }
   else
      ret_val = FALSE;

   return ret_val;
}

static void FreeParsedDataElementHelper(SDP_Data_Element_t *DataElement, unsigned int Depth)
{
   unsigned int Index;

   if(Depth < MAXIMUM_ATTRIBUTE_RECURSION_FAILSAFE)
   {
      if(DataElement)
      {
         switch(DataElement->SDP_Data_Element_Type)
         {
            case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_TEXT_STRING:
               free(DataElement->SDP_Data_Element.TextString);

               break;
            case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_URL:
               free(DataElement->SDP_Data_Element.URL);

               break;
            case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_SEQUENCE:
               for(Index = 0; Index < (DataElement->SDP_Data_Element_Length); Index++)
                  FreeParsedDataElementHelper(&(DataElement->SDP_Data_Element.SDP_Data_Element_Sequence[Index]), (Depth + 1));
               free(DataElement->SDP_Data_Element.SDP_Data_Element_Sequence);

               break;
            case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_ALTERNATIVE:
               for(Index = 0; Index < (DataElement->SDP_Data_Element_Length); Index++)
                  FreeParsedDataElementHelper(&(DataElement->SDP_Data_Element.SDP_Data_Element_Alternative[Index]), (Depth + 1));
               free(DataElement->SDP_Data_Element.SDP_Data_Element_Alternative);

               break;
            case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_NIL:
            case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_UNSIGNED_INT_1_BYTE:
            case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_UNSIGNED_INT_2_BYTE:
            case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_UNSIGNED_INT_4_BYTE:
            case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_UNSIGNED_INT_8_BYTE:
            case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_UNSIGNED_INT_16_BYTE:
            case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_SIGNED_INT_1_BYTE:
            case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_SIGNED_INT_2_BYTE:
            case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_SIGNED_INT_4_BYTE:
            case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_SIGNED_INT_8_BYTE:
            case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_SIGNED_INT_16_BYTE:
            case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_BOOLEAN:
            case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_UUID_16:
            case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_UUID_32:
            case com_stonestreetone_bluetopiapm_SDPM_DataElement_SDP_ELEMENT_TYPE_UUID_128:
            default:
               break;
         }

         BTPS_MemInitialize(DataElement, 0, sizeof(*DataElement));
      }
   }
}

int RegisterNativeFunctions(JNIEnv *Env, const char *ClassName, const JNINativeMethod *Methods, int NumberMethods)
{
   int Result;
   jclass Clazz;

   PRINT_VERBOSE("Registering natives for class: %s\n", ClassName);

   if((Clazz = Env->FindClass(ClassName)) != NULL)
   {
      if((Result = Env->RegisterNatives(Clazz, Methods, NumberMethods)) < 0)
         PRINT_ERROR("Native registration failed for class: %s (%d)\n", ClassName, Result);
   }
   else
   {
      PRINT_ERROR("Native registration unable to find class: %s\n", ClassName);
      Result = -1;
   }

   return Result;
}

int GetJavaEnv(JNIEnv **Env)
{
   int               Result   = 0;
#ifdef JNI_VERSION_1_2
   JavaVMAttachArgs  AttachArgs;
#endif

   if(JVM)
   {
      Result = JVM->GetEnv((void **)Env, PREFERRED_JNI_VERSION);

      if(Result == JNI_EDETACHED)
      {
         /*
          * The current thread is not attached to the Java VM.
          */
         if(PREFERRED_JNI_VERSION == JNI_VERSION_1_1)
            Result = JVM->AttachCurrentThread(Env, 0);
         else
         {
#ifdef JNI_VERSION_1_2
            AttachArgs.version = PREFERRED_JNI_VERSION;
            AttachArgs.name    = 0;
            AttachArgs.group   = 0;

            Result = JVM->AttachCurrentThread(Env, &AttachArgs);
#else
            Result = JNI_EVERSION;
#endif
         }

         /*
          * If we succeeded in attaching the current thread, notify the caller
          * that DetachJavaEnv() must be called when they are finished with the
          * JNIEnv reference.
          */
         if(Result == JNI_OK)
            Result = 1;
      }
   }
   else
   {
      *Env   = 0;
      Result = JNI_ERR;
   }

   return Result;
}

int DetachJavaEnv(JNIEnv *Env)
{
   if(JVM && Env)
      return JVM->DetachCurrentThread();
   else
      return JNI_ERR;
}


/* The following function wraps the BTPM_Initialize() routine to     */
/* initialize an IPC link to the BluetopiaPM service. In addition to */
/* the services provided by BTPM_Initialize() (configuration options */
/* and "link-lost" callback registration), this function will also   */
/* repeatedly retry a failed initialization. The parameter indicates */
/* whether the connection should be considered persistent. If true,  */
/* the connection will not be disconnected until CloseBTPMClient() is*/
/* called. For the connection to be closed, CloseBTPMClient() mus be */
/* called a number of times equal to the number of calls to          */
/* InitBTPMClient() with persistence enabled. Connection state is    */
/* maintained, so initialization will only be attempted if the IPC   */
/* link is not yet connected or has been lost. On success (or if the */
/* link is already established), the function returns 0. A negative  */
/* value indicates an error.                                         */
int InitBTPMClient(Boolean_t PersistentConnection)
{
   int             ret_val;
   unsigned int    RetryCount;
   struct timespec DelayTime;

   PRINT_DEBUG("%s: Enter, BTPMServiceClientCount = %u", __FUNCTION__, BTPMServiceClientCount);

   if(pthread_mutex_lock(&BTPMServiceConnectedMutex) == 0)
   {
      if(BTPMServiceConnected == FALSE)
      {
         BTPM_Cleanup();

         DelayTime.tv_sec  = (INIT_TIMEOUT_MS / (unsigned int)1000);
         DelayTime.tv_nsec = ((INIT_TIMEOUT_MS - (DelayTime.tv_sec * 1000)) * 1000000L);

#ifdef __ANDROID_TREE__

         /* Set the debug zone mask even before initializing the client */
         /* library. This will avoid any unwanted chatter during the    */
         /* handshake with the service.                                 */
         BTPM_DebugSetZoneMask((BTPM_CLIENT_DEBUG_ZONES | BTPM_CLIENT_DEBUG_LEVELS));
         BTPM_DebugSetZoneMask((BTPM_CLIENT_DEBUG_ZONES_PAGE_1 | BTPM_CLIENT_DEBUG_LEVELS));
         BTPM_DebugSetZoneMask((BTPM_CLIENT_DEBUG_ZONES_PAGE_2 | BTPM_CLIENT_DEBUG_LEVELS));

#endif

         ret_val = -1;
         for(RetryCount = 0; ((RetryCount <= INIT_MAXIMUM_RETRIES) && (ret_val != 0)); RetryCount++)
         {
            if((ret_val = BTPM_Initialize(getpid(), NULL, IPCLostCallback, NULL)) != 0)
            {
               if(ret_val == BTPM_ERROR_CODE_ALREADY_INITIALIZED)
                  ret_val = 0;
               else
               {
                  if(RetryCount < INIT_MAXIMUM_RETRIES)
                     nanosleep(&DelayTime, NULL);
               }
            }
         }

         /* If we were able to connect to the stack service, flag it.   */
         if(ret_val == 0)
         {
            PRINT_DEBUG("Bluetooth library initialized for process %u", getpid());
            BTPMServiceConnected = TRUE;

            if(PersistentConnection)
               BTPMServiceClientCount += 1;
         }
         else
         {
            PRINT_ERROR("Bluetooth library initialization failed (%d)", ret_val);
         }
      }
      else
      {
         /* Already initialized.                                        */
         ret_val = 0;

         if(PersistentConnection)
            BTPMServiceClientCount += 1;
      }

      pthread_mutex_unlock(&BTPMServiceConnectedMutex);
   }
   else
   {
      PRINT_ERROR("Failed to acquire lock");
      ret_val = -1;
   }

   PRINT_DEBUG("%s: Exit (%d), BTPMServiceClientCount = %u", __FUNCTION__, ret_val, BTPMServiceClientCount);

   return(ret_val);
}

/* The following function is used to terminate an IPC link which was */
/* established via InitBTPMClient().                                 */
void CloseBTPMClient()
{
   int HoldResult;

   PRINT_DEBUG("%s: Enter, BTPMServiceClientCount = %u", __FUNCTION__, BTPMServiceClientCount);

   HoldResult = pthread_mutex_lock(&BTPMServiceConnectedMutex);

   if(BTPMServiceClientCount > 1)
   {
      BTPMServiceClientCount -= 1;
   }
   else
   {
      BTPM_Cleanup();
      BTPMServiceConnected   = FALSE;
      BTPMServiceClientCount = 0;
      PRINT_DEBUG("Client disconnected from BTPM service");
   }

   if(HoldResult == 0)
      pthread_mutex_unlock(&BTPMServiceConnectedMutex);

   PRINT_DEBUG("%s: Exit, BTPMServiceClientCount = %u", __FUNCTION__, BTPMServiceClientCount);
}

jobject NewBluetoothAddress(JNIEnv *Env, BD_ADDR_t BD_ADDR)
{
   return NewBluetoothAddress(Env, BD_ADDR.BD_ADDR5, BD_ADDR.BD_ADDR4, BD_ADDR.BD_ADDR3, BD_ADDR.BD_ADDR2, BD_ADDR.BD_ADDR1, BD_ADDR.BD_ADDR0);
}

jobject NewBluetoothAddress(JNIEnv *Env, Byte_t Addr1, Byte_t Addr2, Byte_t Addr3, Byte_t Addr4, Byte_t Addr5, Byte_t Addr6)
{
   jclass    BluetoothAddressClass;
   jobject   BluetoothAddress;
   jmethodID BluetoothAddressConstructor;

   if((BluetoothAddressClass = GetBluetoothAddressClassWithConstructor(Env, &BluetoothAddressConstructor)) != NULL)
   {
      BluetoothAddress = Env->NewObject(BluetoothAddressClass, BluetoothAddressConstructor, Addr1, Addr2, Addr3, Addr4, Addr5, Addr6);
      Env->DeleteLocalRef(BluetoothAddressClass);
   }
   else
      BluetoothAddress = NULL;

   return BluetoothAddress;
}

jclass GetBluetoothAddressClass(JNIEnv *Env)
{
   return GetBluetoothAddressClassWithConstructor(Env, NULL);
}

void *AcquireReferenceCountedField(JNIEnv *Env, jobject Object, jfieldID LongDataField, Boolean_t Exclusive)
{
   Boolean_t           Error = FALSE;
   CountedReference_t *CRef  = NULL;

   while((!CRef) && (!Error))
   {
      if(Env->MonitorEnter(Object) == JNI_OK)
      {
         /* Acquire the reference object and confirm that the reference */
         /* is valid.                                                   */
         if(((CRef = (CountedReference_t*)(Env->GetLongField(Object, LongDataField))) != NULL) && (CRef->Reference))
         {
            if((Exclusive) && (CRef->Count == 0))
            {
               /* Object is unclaimed, so mark the object for exclusive */
               /* ownership.                                            */
               CRef->Count = REFERENCE_COUNT_EXCLUSIVE;

               Env->MonitorExit(Object);
            }
            else
            {
               if((!Exclusive) && (CRef->Count < REFERENCE_COUNT_MAX))
               {
                  /* The caller does not need exclusive ownership and   */
                  /* the object has room for additional non-exclusive   */
                  /* owners, so this thread can claim joint ownership.  */
                  CRef->Count++;

                  Env->MonitorExit(Object);
               }
               else
               {
                  /* The object is not available for ownership (either  */
                  /* the reference is exclusively owned, this thread has*/
                  /* requested exclusive ownership and the reference is */
                  /* currently non-exclusively owned, or the reference  */
                  /* has reached the maximum possible number of owners),*/
                  /* so this thread must wait for the object to become  */
                  /* available.                                         */
                  CRef = NULL;

                  BTPS_ResetEvent(ReleasedReferenceEvent);
                  Env->MonitorExit(Object);
                  
                  if(BTPS_WaitEvent(ReleasedReferenceEvent, BTPS_INFINITE_WAIT))
                  {
                     /* Event was set.  This indicates that the object's*/
                     /* ownership was released, so attempt to acquire   */
                     /* ownership again.                                */
                  }
                  else
                  {
                     /* Event failed.  This indicates a serious failure,*/
                     /* as this should never happen.  Fail the operation*/
                     /* so the caller can take react gracefully.        */
                     Error = TRUE;
                  }
               }
            }
         }
         else
         {
            /* The reference is not valid. Release the monitor and      */
            /* return failure.                                          */
            Env->MonitorExit(Object);

            CRef  = NULL;
            Error = TRUE;
         }
      }
      else
         Error = TRUE;
   }

   return(CRef ? CRef->Reference : NULL);
}

void ReleaseReferenceCountedField(JNIEnv *Env, jobject Object, jfieldID LongDataField, void *Reference)
{
   CountedReference_t *CRef;

   if(Env->MonitorEnter(Object) == JNI_OK)
   {
      /* Acquire the reference object and confirm that the reference    */
      /* refers to the same object being released.                      */
      if(((CRef = (CountedReference_t*)(Env->GetLongField(Object, LongDataField))) != NULL) && (CRef->Reference == Reference))
      {
         if((CRef->Count) && (CRef->Count <= REFERENCE_COUNT_MAX))
         {
            /* This object has one or more non-exclusive owners, so     */
            /* remove this thread's claim and be done.                  */
            CRef->Count--;
         }
         else
         {
            /* This thread is the only owner.  Whether its ownership    */
            /* is exclusive or not, we can simply return the reference  */
            /* count to zero.                                           */
            CRef->Count = 0;
         }
         
         /* Announce that the object is now available for new ownership */
         /* claims.                                                     */
         BTPS_SetEvent(ReleasedReferenceEvent);
      }

      Env->MonitorExit(Object);
   }
}

void SetReferenceCountedField(JNIEnv *Env, jobject Object, jfieldID LongDataField, void *Reference)
{
   CountedReference_t *CRef;

   if(Env->MonitorEnter(Object) == JNI_OK)
   {
      /* Acquire the reference object.                                  */
      CRef = (CountedReference_t*)(Env->GetLongField(Object, LongDataField));

      if(!CRef)
      {
         /* If the Java object's field is unpopulated, so create a new  */
         /* reference object.                                           */
         if((CRef = (CountedReference_t*)BTPS_AllocateMemory(sizeof(CountedReference_t))) != NULL)
         {
            BTPS_MemInitialize(CRef, 0, sizeof(CountedReference_t));

            Env->SetLongField(Object, LongDataField, (jlong)CRef);
         }
      }

      /* Actually set the reference */
      if(CRef)
         CRef->Reference = Reference;

      Env->MonitorExit(Object);
   }
}

Boolean_t ParseDataElement(JNIEnv *Env, jobject JavaDataElement, SDP_Data_Element_t *DataElement)
{
   Boolean_t               ret_val;
   ParseDataElementState_t State;

   if((Env) && (JavaDataElement) && (DataElement))
   {
      ret_val = FALSE;

      /* Create a local JavaVM reference frame. Since we don't know who */
      /* is calling us, this ensures we won't exceed the VM's local     */
      /* reference allocation limit. It should be sized to account for  */
      /* the new references we allocate here, as well as the maximum    */
      /* possible number of simultaneous references we could be holding */
      /* in ParseDataElementHelper() (currently 3).                     */
      if(Env->PushLocalFrame(16) == 0)
      {
         if((State.URIClass = Env->FindClass("Ljava/net/URI;")) != NULL)
         {
            if((State.UUIDClass = Env->FindClass("com/stonestreetone/bluetopiapm/SDPM/UUID")) != NULL)
            {
               if((State.StringClass = Env->FindClass("Ljava/lang/String;")) != NULL)
               {
                  if((State.ByteArrayClass = Env->FindClass("[B")) != NULL)
                  {
                     if((State.DataElementClass = Env->FindClass("com/stonestreetone/bluetopiapm/SDPM/DataElement")) != NULL)
                     {
                        if((State.DataElementArrayClass = Env->FindClass("[Lcom/stonestreetone/bluetopiapm/SDPM/DataSequence;")) != NULL)
                        {
                           if((State.UuidDataField = Env->GetFieldID(State.UUIDClass, "uuid", "[B")) != NULL)
                           {
                              if((State.DataElementTypeField = Env->GetFieldID(State.DataElementClass, "type", "I")) != NULL)
                              {
                                 if((State.DataElementNumericDataField = Env->GetFieldID(State.DataElementClass, "numericData", "Lcom/stonestreetone/bluetopiapm/SDPM/DataElement;")) != NULL)
                                 {
                                    if((State.DataElementGenericDataField = Env->GetFieldID(State.DataElementClass, "genericData", "Lcom/stonestreetone/bluetopiapm/SDPM/DataElement;")) != NULL)
                                    {
                                       if((State.URIMethod_toASCIIString = Env->GetMethodID(State.URIClass, "toASCIIString", "()Ljava/lang/String;")) != NULL)
                                       {
                                          ret_val = ParseDataElementHelper(Env, JavaDataElement, DataElement, 0, State);
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         if(Env->ExceptionCheck())
         {
            Env->ExceptionDescribe();
            Env->ExceptionClear();

            ret_val = FALSE;
         }

         Env->PopLocalFrame(0);
      }
      else
      {
         if(Env->ExceptionCheck())
         {
            Env->ExceptionDescribe();
            Env->ExceptionClear();
         }

         ret_val = FALSE;
      }
   }
   else
      ret_val = FALSE;

   return ret_val;
}

void FreeParsedDataElement(SDP_Data_Element_t *DataElement)
{
   FreeParsedDataElementHelper(DataElement, 0);
}

SDP_Service_Attribute_Value_Data_t *FindSDPAttribute(SDP_Service_Attribute_Response_Data_t *ServiceRecord, Word_t AttributeID)
{
   unsigned int                        Index;
   SDP_Service_Attribute_Value_Data_t *Attribute;

   Attribute = NULL;

   if(ServiceRecord)
   {
      for(Index = 0; Index < ServiceRecord->Number_Attribute_Values; Index++)
      {
         if(ServiceRecord->SDP_Service_Attribute_Value_Data[Index].Attribute_ID == AttributeID)
            Attribute = &(ServiceRecord->SDP_Service_Attribute_Value_Data[Index]);
      }
   }

   return Attribute;
}

SDP_Data_Element_t *FindEntryInProtocolDescriptorList(SDP_Service_Attribute_Value_Data_t *ProtocolDescriptorListAttribute, UUID_128_t UUID)
{
   unsigned int        Index;
   UUID_128_t          ProtocolUUID;
   SDP_Data_Element_t *Element;
   SDP_Data_Element_t *Descriptor;
   SDP_Data_Element_t *ProtocolDescriptorList;

   Element = NULL;

   if((ProtocolDescriptorListAttribute) && (ProtocolDescriptorListAttribute->Attribute_ID == SDP_ATTRIBUTE_ID_PROTOCOL_DESCRIPTOR_LIST))
   {
      ProtocolDescriptorList = ProtocolDescriptorListAttribute->SDP_Data_Element;

      if((ProtocolDescriptorList) && (ProtocolDescriptorList->SDP_Data_Element_Type == deSequence))
      {
         for(Index = 0; ((!Element) && (Index < ProtocolDescriptorList->SDP_Data_Element_Length)); Index++)
         {
            Descriptor = &(ProtocolDescriptorList->SDP_Data_Element.SDP_Data_Element_Sequence[Index]);

            if((Descriptor->SDP_Data_Element_Type == deSequence) && (Descriptor->SDP_Data_Element_Length > 0))
            {
               switch(Descriptor->SDP_Data_Element.SDP_Data_Element_Sequence[0].SDP_Data_Element_Type)
               {
                  case deUUID_128:
                     ProtocolUUID = Descriptor->SDP_Data_Element.SDP_Data_Element_Sequence[0].SDP_Data_Element.UUID_128;
                     break;
                  case deUUID_32:
                     SDP_ASSIGN_BASE_UUID(ProtocolUUID);
                     ASSIGN_SDP_UUID_32_TO_SDP_UUID_128(ProtocolUUID, Descriptor->SDP_Data_Element.SDP_Data_Element_Sequence[0].SDP_Data_Element.UUID_32);
                     break;
                  case deUUID_16:
                     SDP_ASSIGN_BASE_UUID(ProtocolUUID);
                     ASSIGN_SDP_UUID_16_TO_SDP_UUID_128(ProtocolUUID, Descriptor->SDP_Data_Element.SDP_Data_Element_Sequence[0].SDP_Data_Element.UUID_16);
                     break;
                  default:
                     /* Invalid protocol descriptor list. Ignore it.    */
                     ASSIGN_SDP_UUID_128(ProtocolUUID, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0); 
                     break;
               }

               if(COMPARE_UUID_128(UUID, ProtocolUUID))
                  Element = Descriptor;
            }
         }
      }
   }

   return Element;
}

/*
 * Main entry-point for loading this JNI library.
 */
__attribute__ ((visibility ("default")))
jint JNI_OnLoad(JavaVM *VM, void *Reserved)
{
   int     NeedsDetach;
   JNIEnv *Env;

   PRINT_DEBUG("JNI_OnLoad");

   JVM = VM;

   if(!ReleasedReferenceEvent)
      ReleasedReferenceEvent = BTPS_CreateEvent(FALSE);

   if((NeedsDetach = GetJavaEnv(&Env)) >= 0)
   {
      for(unsigned int i = 0; i < (sizeof(RegFuncs) / sizeof(RegFuncs[0])); i++)
      {
         if(RegFuncs[i])
            (RegFuncs[i])(Env);
      }

      if(NeedsDetach)
         DetachJavaEnv(Env);
   }

   return PREFERRED_JNI_VERSION;
}

/*
 * Exit-point for unloading this JNI library.
 */
__attribute__ ((visibility ("default")))
void JNI_OnUnload(JavaVM *VM, void *Reserved)
{
   if(ReleasedReferenceEvent)
      BTPS_CloseEvent(ReleasedReferenceEvent);
}
