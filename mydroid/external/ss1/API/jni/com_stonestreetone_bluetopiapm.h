/*****< com_stonestreetone_bluetopiapm.h >*************************************/
/*      Copyright 2010 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  com_stonestreetone_bluetopiapm - Utility function, constant, and          */
/*                                   prototype declarations for the           */
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

#ifndef COM_STONESTREETONE_BLUETOPIAPM_H_
#define COM_STONESTREETONE_BLUETOPIAPM_H_

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

#include "SS1BTPM.h"

#ifdef __cplusplus
}
#endif

   /* Global toggle for log messages in the Java API JNI layer.         */
#define ENABLE_SS1_JNI_DEBUG 0

   /* NOTE: If the NIO interface or GetObjectRefType() needs to be      */
   /*       supported, this return value should be changed to           */
   /*       JNI_VERSION_1_4 or JNI_VERSION_1_6, respectively.           */
#define PREFERRED_JNI_VERSION JNI_VERSION_1_4

#if ENABLE_SS1_JNI_DEBUG

#ifdef __ANDROID__

#include <android/log.h>
#define LOG_TAG "BTPMJ"
#define PRINT_ERROR(format, ...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, format, ## __VA_ARGS__)
#define PRINT_INFO(format, ...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, format, ## __VA_ARGS__)
#define PRINT_DEBUG(format, ...)   __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, format, ## __VA_ARGS__)
#define PRINT_VERBOSE(format, ...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, format, ## __VA_ARGS__)

#elif defined(__linux__)

#include <stdio.h>
#define PRINT_ERROR(format, ...) fprintf(stderr, format "\r\n", ## __VA_ARGS__)
#define PRINT_INFO(format, ...) fprintf(stdout, format "\r\n", ## __VA_ARGS__)
#define PRINT_DEBUG(format, ...) fprintf(stderr, format "\r\n", ## __VA_ARGS__)
#define PRINT_VERBOSE(format, ...) fprintf(stderr, format "\r\n", ## __VA_ARGS__)

#else

#define PRINT_ERROR(format, ...) do{}while(0)
#define PRINT_INFO(format, ...) do{}while(0)
#define PRINT_DEBUG(format, ...) do{}while(0)
#define PRINT_VERBOSE(format, ...) do{}while(0)

#endif

#else /* ENABLE_SS1_JNI_DEBUG */

#define PRINT_ERROR(format, ...) do{}while(0)
#define PRINT_INFO(format, ...) do{}while(0)
#define PRINT_DEBUG(format, ...) do{}while(0)
#define PRINT_VERBOSE(format, ...) do{}while(0)

#endif /* ENABLE_SS1_JNI_DEBUG */

   /* Wrappers for Weak Global Reference support for JNI.               */

   /* The __ANDROID_API__ constant is provided in the NDK by api-leve.h.*/
   /* In the Android source tree, the make variable PRODUCT_SDK_VERSION */
   /* serves this purpose. This module's makefile will define           */
   /* __ANDROID_API__ in terms of PRODUCT_SDK_VERSION if we are building*/
   /* out of the Android source tree. Otherwise, include the header.    */
#ifndef __ANDROID_API__
#include <android/api-level.h>
#endif

   /* Weak Global References are supported as of Android 2.2 (API 8).   */
#if __ANDROID_API__ >= 8

#define WEAK_REF_SUPPORTED 1
#define WEAK_REF jweak

#ifdef __cplusplus
#define NewWeakRef(Env, Object) ((Env)->NewWeakGlobalRef(Object))
#define DeleteWeakRef(Env, Ref) ((Env)->DeleteWeakGlobalRef(Ref))
#else
#define NewWeakRef(Env, Object) ((*(Env))->NewWeakGlobalRef((Env), (Object)))
#define DeleteWeakRef(Env, Ref) ((*(Env))->DeleteWeakGlobalRef((Env), (Ref)))
#endif

#else

#define WEAK_REF jobject

#ifdef __cplusplus
#define NewWeakRef(Env, Object) ((Env)->NewGlobalRef(Object))
#define DeleteWeakRef(Env, Ref) ((Env)->DeleteGlobalRef(Ref))
#else
#define NewWeakRef(Env, Object) ((*(Env))->NewGlobalRef((Env), (Object)))
#define DeleteWeakRef(Env, Ref) ((*(Env))->DeleteGlobalRef((Env), (Ref)))
#endif

#endif

   /* JNI Helper Routines                                               */

int RegisterNativeFunctions(JNIEnv *Env, const char *Class, const JNINativeMethod *Methods, int MethodsLength);

int GetJavaEnv(JNIEnv **Env);

int DetachJavaEnv(JNIEnv *Env);

int InitBTPMClient(Boolean_t PersistentConnection);

void CloseBTPMClient();

jobject NewBluetoothAddress(JNIEnv *Env, BD_ADDR_t BD_ADDR);

jobject NewBluetoothAddress(JNIEnv *Env, Byte_t Addr1, Byte_t Addr2, Byte_t Addr3, Byte_t Addr4, Byte_t Addr5, Byte_t Addr6);

jclass GetBluetoothAddressClass(JNIEnv *Env);

void *AcquireReferenceCountedField(JNIEnv *Env, jobject Object, jfieldID LongDataField, Boolean_t Exclusive);
void ReleaseReferenceCountedField(JNIEnv *Env, jobject Object, jfieldID LongDataField, void *Reference);
void SetReferenceCountedField(JNIEnv *Env, jobject Object, jfieldID LongDataField, void *Reference);

#if BTPM_CONFIGURATION_GENERIC_ATTRIBUTE_MANAGER_SUPPORTED
void LongsFromUUID(GATT_UUID_t GATT_UUID, jlong *MostSignificantBits, jlong *LeastSignificantBits);
GATT_UUID_t LongsToUUID(jlong MostSignificantBits, jlong LeastSignificantBits);
#endif

   /* SDP Helpers Routines                                              */

Boolean_t ParseDataElement(JNIEnv *Env, jobject JavaDataElement, SDP_Data_Element_t *DataElement);

void FreeParsedDataElement(SDP_Data_Element_t *DataElement);

SDP_Service_Attribute_Value_Data_t *FindSDPAttribute(SDP_Service_Attribute_Response_Data_t *ServiceRecord, Word_t AttributeID);

SDP_Data_Element_t *FindEntryInProtocolDescriptorList(SDP_Service_Attribute_Value_Data_t *ProtocolDescriptorListAttribute, UUID_128_t UUID);

#endif /* COM_STONESTREETONE_BLUETOIPAPM_H_ */
