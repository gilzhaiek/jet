/*****< ARBTJBBTS.h >**********************************************************/
/*      Copyright 2010 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  ARBTJBBTS - Stonestreet One Android Runtime Bluetooth JNI Bridge Type     */
/*              Definitions, Constants, and Prototypes for the                */
/*              BluetoothService module.                                      */
/*                                                                            */
/*  Author:  Greg Hensley                                                     */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   07/29/10  G. Hensley     Initial creation.                               */
/******************************************************************************/

#ifndef __ARBTJBBTSH__
#define __ARBTJBBTSH__

#include "JNIHelp.h"
#include "jni.h"

#ifdef HAVE_BLUETOOTH

extern "C" {
#include "SS1BTPM.h"
}

namespace android
{

jboolean BTS_CallIsRemoteDeviceInCache(JNIEnv *Env, jobject BTSObject, jstring Address);

#if SS1_PLATFORM_SDK_VERSION < 14
jboolean BTS_CallAddRemoteDeviceProperties(JNIEnv *Env, jobject BTSObject, jstring Address, jobject PropArray);
jstring BTS_CallGetRemoteDeviceProperty(JNIEnv *Env, jobject BTSObject, jstring Address, jstring Property);
#endif

}

#endif /* HAVE_BLUETOOTH */

#endif /* __ARBTJBBTSH__ */

