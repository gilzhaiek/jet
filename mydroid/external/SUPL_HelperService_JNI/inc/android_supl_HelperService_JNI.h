/*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName         :   suplc_core_wrapper.cpp
 *
 * Description      :   Common interface between SUPL Core and NaviLink.
 *
 * Author           :   Praneet Kumar A
 * Date             :   15th June 2009
 *
 ******************************************************************************
 */


#ifndef __ANDROID_SUPL_HELPER_SERVICE__
#define __ANDROID_SUPL_HELPER_SERVICE__

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

#include "jni.h"
//#include "JNIHelp.h"

/**
 * Function:        Java_startSocketServer
 * Brief:
 * Description:
 * Note:            External Function.
 * Params:
 * Return:
 */
JNIEXPORT jboolean JNICALL Java_startSocketServer(JNIEnv *, jobject);

JNIEXPORT jboolean JNICALL Java_PostSLPMessageToQueue(JNIEnv *env, jobject obj, jbyteArray ByteArray);
JNIEXPORT jboolean JNICALL Java_PostRespToQueue(JNIEnv *env, jobject obj, jintArray IntArray);

JNIEXPORT jboolean JNICALL Java_ConnectionResponse(JNIEnv *env, jobject obj, jbyte bRes);


#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif /* __ANDROID_SUPL_HELPER_SERVICE__ */
