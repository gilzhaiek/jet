/*
******************************************************************
* COPYRIGHT © Teleca AB                                          *
*----------------------------------------------------------------*
* MODULE     : SuplLocationProvider.cpp                          *
*                                                                *
* PROGRAMMER : Dmitriy Kardakov                                  *
* DATE       : 28 Feb 2009                                       *
* VERSION    : 1.0                                               *
*                                                                *
*----------------------------------------------------------------*
* MODULE SUMMARY : This module implements the methods necessary  *
* for SUPL controller to interact with Android OS in order to    *
* retrieve GSM information and access the secure network         *
* connection, provides implementations of the native methods     *
* for SuplLocationProvider Java class. This module contains the  *
* entry point of SuplLocationProvider native library,            *
* enables/disables the SUPL client and EE client, implements the *
* SUPL_Control() method (SUPL command interface) and SUPL        *
* "callback" response method.                                    *
*----------------------------------------------------------------*
* MODIFICATION RECORDS                                           *
* Dmitriy Kardakov - 28 Feb 2009 - Initial version               *
******************************************************************
*/

#define __SUPL_TEST

//#include "mcpf_nal.h"
#include "android/ags_androidlog.h"
#include "suplcontroller/SUPLController.h"
#include "jni.h"
#include "android/android_networking_CNet.h"
#include "android/android_supl_SuplLocationProvider.h"
#include "android/android_networking_CNetTLSProviderBase.h"
#include <sys/types.h>

#if defined(__SUPL_TEST)
#include "android/SUPLTests.h"
#endif

#include <utils/Log.h>

using namespace Engine;
using namespace Platform;


/* SuplLocationProvider aplication id */
//#define SUPL_APP_ID 0x00FA
#define SUPL_APP_ID 1001 //zz_Aby - appID

/* aplication id for ee client */
#define EE_CLIENT_APPLICATION_ID 0x5050
/* timeout between attempts to stop navigation*/
#define SUPL_STOP_NAVIGATING_WAIT_TIMEOUT_MS 50
/* minimal interval between fixes*/
#define MIN_SUPL_FIX_INTERVAL_S 60
/* first request timeuot*/
#define FIRST_SUPL_REQUEST_TIMEOUT_S 2
/* SMS/WAP listen*/
#define _SMS_           0x1
#define _WAP_           0x2
#define _LISTEN_ENABLE  0x1
#define _LISTEN_DISABLE 0x0
/* gsm specific data array size (ci, lac, mcc, mnc)*/
#define GSM_INT_CI_LAC_MCC_MNC 5
#define GSM_INT_CI_LAC_MCC_MNC_NMR 53
#if defined(_DEBUG_IMSI)
/* debug IMSI code */
static const char *g_imsi_dbg = "2551397473821851";
#endif

#if 0 //APK
/* callback for ee client */
bool_t EECallBack(Engine::uint16_t appId,
                  Engine::uint32_t Command,
                  void *inbuf);
#endif


JavaVM                *g_JVMSuplLibrary            = NULL;
static pthread_mutex_t g_CloseMutex                = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t  g_CloseCond                 = PTHREAD_COND_INITIALIZER;
static pthread_mutex_t g_NavigatingMutex           = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t  g_NavigatingCond            = PTHREAD_COND_INITIALIZER;
static pthread_t       g_UnitTestThread;
static const char     *g_CNetClass                 = "android/supl/CNet";
static const char     *g_CSUPL_LP_Class            = "android/supl/CSUPL_LP";

static bool_t          gb_IsSuplInitizialised      = FALSE;
static bool_t          gb_IsSuplNavigatingStarted  = FALSE;
static bool_t          gb_IsSuplStopped            = TRUE;
static sig_atomic_t    g_NeedStopSuplNavigating    = 0;
static pthread_t       g_SuplNavigatingThread      = 0;
static pthread_mutex_t g_StartMutex                = PTHREAD_MUTEX_INITIALIZER;

static pthread_mutex_t g_EventMutex                = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t  g_EventCond                 = PTHREAD_COND_INITIALIZER;
static Platform::Position_t g_Position;
static pthread_mutex_t g_PositionMutex             = PTHREAD_MUTEX_INITIALIZER;

static sig_atomic_t    gi_Counter                  = 0;
static int             gi_FixInterval = SUPL_STOP_NAVIGATING_WAIT_TIMEOUT_MS;

#if defined(__SUPL_TEST)
/* Variables for tests */
extern int resp_flag;
static const char *unit_test_hp = "208.8.164.7:7275";
#endif

static int DetachCurrentThread ();
/*
*******************************************************************
* Function: AttachCurrentThread
*
* Description : Attaches the current thread to a Java VM.Returns a JNI
* interface pointer in the JNIEnv argument. A native thread cannot be
* attached simultaneously to two Java VMs
*
* Parameters :
* p_env:  w pointer to the location where the JNI interface pointer of
*           the current thread will be placed
*
* Returns : TRUE on success, FALSE on failure.
*
*******************************************************************
*/
static int AttachCurrentThread (JNIEnv **p_env)
{
    int ret = -1;
    if (g_JVMSuplLibrary)
    {
        if ((ret = g_JVMSuplLibrary->AttachCurrentThread( p_env, 0))
            == JNI_OK)
        {
            return TRUE;
        }
        debugMessage("%s, error attach thread %d", __FUNCTION__, ret);
    }
    return FALSE;
}

/*
*******************************************************************
* Function: AttachFindClass
*
* Description : Attaches the current thread to a Java VM and loads a
* locally-defined class.
*
* Parameters :
* p_env:    w pointer to the location where the JNI interface pointer
*             of the current thread will be placed
* JavaClass r fully-qualified class name (that is, a package name,
*             delimited by "/", followed by the class name). If the
*             name begins with "[" (the array signature character),
*             it returns an array class. The string is encoded in
*             modified UTF-8
* Returns : a class object from a fully-qualified name, or NULL if
* the class cannot be found.
*
*******************************************************************
*/
static jclass AttachFindClass (JNIEnv **p_env, const char *JavaClass)
{
    jclass jclazz = NULL;
    int ret = -1;
    if (g_JVMSuplLibrary)
    {
        if ((ret = g_JVMSuplLibrary->AttachCurrentThread( p_env, 0))
            == JNI_OK)
        {
            if ((jclazz = (*p_env)->FindClass(JavaClass)) != NULL)
            {
                return jclazz;
            }
            else
            {
                debugMessage("%s, error (*env)->FindClass(%s) ",
                             __FUNCTION__,
                             JavaClass);

                if (DetachCurrentThread() < 0)
                {
                    debugMessage("%s, error detach current thread",
                                 __FUNCTION__);
                }

                return NULL;
            }
        }
        debugMessage("%s, error attach thread %d", __FUNCTION__, ret);
    }
    return NULL;
}
/*
*******************************************************************
* Function: DetachCurrentThread
*
* Description : Detaches the current thread from a Java VM. All Java
* monitors held by this thread are released. All Java threads waiting
* for this thread to die are notified.
*
* Parameters : None
*
* Returns : TRUE on success, FALSE on failure.
*
*******************************************************************
*/
static int DetachCurrentThread ()
{
    int ret = FALSE;
    if (g_JVMSuplLibrary)
    {
        if ((ret = g_JVMSuplLibrary->DetachCurrentThread())
            == JNI_OK)
        {
            return TRUE;
        }
    }
    return ret;
}
// Integration with GPS driver
static jboolean checkAppId (int AppId)
{
    jboolean ret = JNI_FALSE;
    JNIEnv *env = NULL;
    jclass clazz = NULL;
    if ((clazz = AttachFindClass (&env, g_CSUPL_LP_Class)) != NULL)
    {

        jmethodID mid = env->GetStaticMethodID(clazz, "checkAppId", "(I)Z");
        if (mid)
        {
            ret = (jboolean) env->CallStaticBooleanMethod(clazz, mid, AppId);
        }
        DetachCurrentThread ();
    }

    return ret;
}

static void putMessage (int AppId, int command, char *data)
{
    JNIEnv *env = NULL;
    jclass clazz = NULL;
    if ((clazz = AttachFindClass (&env, g_CSUPL_LP_Class)) != NULL)
    {
        jmethodID mid = env->GetStaticMethodID(clazz, "putMessage",
                                               "(IILjava/lang/String;)V");
        if (mid)
        {
            jstring str_data = env->NewStringUTF(data);
            env->CallStaticVoidMethod(clazz, mid, AppId, command,
                                      str_data);
        }

        DetachCurrentThread ();
    }
}

static void generateFileName (char *stringFileName, int appId, int command)
{
    int nPos = 0;
    int nVal = appId;
    strcpy(stringFileName, "/data/supl_exchange/");
    nPos += strlen(stringFileName);
    do
    {
        *(stringFileName + (nPos++)) = (nVal % 10) + '0';
        nVal = nVal / 10;
    }
    while (nVal != 0);

    *(stringFileName + (nPos++)) = '_';

    nVal = command;

    do
    {
        *(stringFileName + (nPos++)) = (nVal % 10) + '0';
        nVal = nVal / 10;
    }
    while (nVal != 0);

    stringFileName[nPos] = '\0';
    debugMessage("generateFileName: %s", stringFileName);
}

/*******************************************/


/*
*******************************************************************
* Function: JNI_OnLoad
*
* Description : Native library entry point.
*
* Parameters :
* vm        r Pointer to Java virtual machine
* reserved  r Not used
*
* Returns : JNI version number required
*
*******************************************************************
*/
JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved)
{
		LOGD("SUPLLocationProvider.cpp: JNI_OnLoad: ##########");
      g_JVMSuplLibrary = vm;
      return JNI_VERSION_1_2;
}

/*
*******************************************************************
* Function: JNI_OnUnload
*
* Description : This function is called when the native is unloaded
*
* Parameters :
* vm        r Pointer to Java virtual machine
* reserved  r Not used
*
* Returns : None
*
*******************************************************************
*/
JNIEXPORT void JNICALL
JNI_OnUnload(JavaVM *vm, void *reserved)
{
}

/*
*******************************************************************
* Function: Java_android_networking_CNetTLSProviderBase_PostSLPMessageToQueue
*
* Description : Posts message received over the TLS connection to
* SUPL controller message queue.
*
* Parameters :
* env       r Pointer to JNI environment.
* obj       r Java object against that the native method is called.
* ByteArray r Java array object with message data.
* Returns : JNI_TRUE on success, JNI_FALSE on failure.
*
*******************************************************************
*/
JNIEXPORT jboolean JNICALL
Java_android_networking_CNetTLSProviderBase_PostSLPMessageToQueue
(JNIEnv *env, jobject obj, jbyteArray ByteArray)
{
	LOGD("CNetTLSProviderBase_PostSLPMessageToQueue: ##########");
    if(CSUPLController::GetInstance().SUPLStart() != TRUE)
    {
        debugMessage("CSUPLController::GetInstance().SUPLStart() != TRUE");
        return JNI_FALSE;
    }

    jsize arrSize = env->GetArrayLength(ByteArray);
    jbyte *bytes = env->GetByteArrayElements(ByteArray, NULL);
    if (bytes)
    {
        CSUPLController::
        GetInstance().GetNetwork().HandleMessage((const char *) bytes, arrSize);
        env->ReleaseByteArrayElements(ByteArray, bytes, JNI_ABORT);
        debugMessage("%s FINE!", __FUNCTION__);
        return JNI_TRUE;
    }
    else
    {
        debugMessage("%s error!", __FUNCTION__);
        return JNI_FALSE;
    }
}

/*
*******************************************************************
* Function: Java_android_supl_SuplLocationProvider_PostBodyToQueue
*
* Description : Posts SUPL_INIT message parsed from SMS/WAP-push
* message to SUPL controller message queue.
*
* Parameters :
* env       r Pointer to JNI environment.
* obj       r Java object against that the native method is called
* ByteArray r Java array object with message data.
* Returns : JNI_TRUE on success, JNI_FALSE on failure.
*
*******************************************************************
*/
JNIEXPORT jboolean JNICALL
Java_android_supl_CSUPL_1LP_PostBodyToQueue
(JNIEnv *env, jobject obj, jbyteArray ByteArray)
{
	LOGD("+Java_android_supl_SuplLocationProvider_PostBodyToQueue");	
	if(CSUPLController::GetInstance().SUPLStart() != TRUE)
    {
		LOGD("-Java_android_supl_SuplLocationProvider_PostBodyToQueue: Error SUPL");

		return JNI_FALSE;
    }

    jsize arrSize = env->GetArrayLength(ByteArray);
    jbyte *bytes = env->GetByteArrayElements(ByteArray, NULL);
    if (bytes)
    {
        CSUPLController::
        GetInstance().GetNetwork().HandleMessage ((char *) bytes,
                                                  arrSize);
        env->ReleaseByteArrayElements(ByteArray, bytes, JNI_ABORT);
		LOGD("-Java_android_supl_SuplLocationProvider_PostBodyToQueue");
        return JNI_TRUE;
    }
    else
    {
        debugMessage("%s error!", __FUNCTION__);
		LOGD("-Java_android_supl_SuplLocationProvider_PostBodyToQueue: Error");
        return JNI_FALSE;
    }
}

/*
*******************************************************************
* Function: Java_android_supl_SuplLocationProvider_NotificationResult
*
* Description : Provides the user notification result to SUPL
* controller.
*
* Parameters :
* env       r Pointer to JNI environment.
* clazz     r Java class against that the static native method
              is called.
* res       r user choice.
*             1 - connection to SUPL server is allowed
*             2 - connection rejected by user
* tid       r unique notification Id.
* Returns : None.
*
*******************************************************************
*/
JNIEXPORT void JNICALL
Java_android_supl_CSUPL_1LP_NotificationResult
(JNIEnv *env, jclass clazz, jint res, jint tid)
{
    if(CSUPLController::GetInstance().SUPLStart() != TRUE)
    {
        return;
    }

    CSUPLController::GetInstance().GetDevice().NotificationResult(res, tid);
}


jboolean JDumpMessage(JNIEnv * env, jobject obj, jbyteArray ByteArray);

/*
*******************************************************************
* Function: Java_android_networking_CNetTLSProviderBase_DumpMessage
*
* Description : Stores message to file for debug purposes.
*
* Parameters :
* env       r Pointer to JNI environment.
* obj       r Java object against that the native method is called
* ByteArray r Java array object with message data.
* Returns : JNI_TRUE on success, JNI_FALSE on failure.
*
*******************************************************************
*/
JNIEXPORT jboolean JNICALL Java_android_networking_CNetTLSProviderBase_DumpMessage
  (JNIEnv *env, jobject obj, jbyteArray ByteArray)

{
    return JDumpMessage(env, obj, ByteArray);
}

/*
*******************************************************************
* Function: Init_native
*
* Description : Initializes a network component. Function attaches
* the current thread to JVM and detaches it before exiting.
*
* Parameters : None

* Returns : 0 upon successful completion, otherwise, the function
*           return negative error code.
*
*******************************************************************
*/
int Init_native()
{
    eNAL_Result retVal = NAL_TRUE;

    debugMessage(" Init_native: Entering \n");

    retVal = CSUPLController::GetInstance().
             p_nalExecuteCommand(MCPF_NAL_TLS_INIT, NULL);


    if(retVal != NAL_TRUE)
    {
        debugMessage(" Init_native: Command execution FAILED !!! \n");
        return -1;
    }

    debugMessage(" Init_native: Exiting Successfully \n");
    return 0;
}

/*
*******************************************************************
* Function: Send_native
*
* Description : Sends len bytes from the specified data buffer to
* SUPL Server using secure connection. Function is blocked until all
* data has been sent. Function attaches the current thread to JVM
* and detaches it before exiting.
*
* Parameters :
* data       r data buffer from which bytes are to be written
* length     r the number of bytes to write
* Returns : a non-negative integer indicating the number of bytes
* actually written, otherwise, the negative error code if some I/O
* error occurs.
*
*******************************************************************
*/
int Send_native(void *data, unsigned int length)
{
    s_NAL_slpData s_payload;
    eNAL_Result retVal = NAL_TRUE;

    debugMessage(" Send_native: Entering \n");

    /* TBD: Send 'data' and 'length' with the command. */

    memcpy((void *) &s_payload.a_buffer, data, length);
    s_payload.length = length;
    retVal = CSUPLController::GetInstance().
             p_nalExecuteCommand(MCPF_NAL_TLS_SEND, (void *) &s_payload);
    if(retVal != NAL_TRUE)
    {
        debugMessage(" Send_native: Command execution FAILED !!! \n");
        return -1;
    }

    debugMessage(" Send_native: Exiting Successfully \n");
    return 0;
}

/*
*******************************************************************
* Function: CreateConnection_native
*
* Description : Creates a secure connection to the specified port
* number at the specified IP address. Function attaches the current
* thread to JVM and detaches it before exiting.
*
* Parameters :
* host_port  r null-terminated string with a remote host IP address
* and port number (host:port)
* Returns : the negative error code if an I/0 error occurs,
* 0 otherwise.
*
*******************************************************************
*/
int CreateConnection_native(const char *host_port)
{
    s_NAL_createConnection s_payload;
    eNAL_Result retVal = NAL_TRUE;

    debugMessage(" CreateConnection_native: Entering \n");

    /* TBD: Send 'host_port' with the command. */
    memcpy( (void *)&s_payload.a_hostPort,(void *)host_port, MAX_FQDN_LENGTH);
    retVal = CSUPLController::GetInstance().
             p_nalExecuteCommand(MCPF_NAL_TLS_CREATE_CONNECTION, (void *)&s_payload);
    if(retVal != NAL_TRUE)
    {
        debugMessage(" CreateConnection_native: Command execution FAILED !!! \n");
        return -1;
    }

    debugMessage(" CreateConnection_native: Exiting Successfully \n");
    return 0;
}

/*
*******************************************************************
* Function: FreeConnection_native
*
* Description : Closes a secure network connection. After a secure
* connection is be closed, any further attempt to invoke Send_native
* operation upon it will return -1, if Receive_native() function
* has been already invoked then receiving thread will closed.
* This function may be invoked at any time. If some other thread has
* already invoked it, however, then another invocation will be blocked
* until the first invocation completes, after which it will do nothing
* and return. Function attaches the current thread to JVM and
* detaches it before exiting.
* Parameters : None
* Returns : None
*
*******************************************************************
*/
void FreeConnection_native()
{
    eNAL_Result retVal = NAL_TRUE;

    debugMessage(" FreeConnection_native: Entering \n");

    /* TBD: Send 'host_port' with the command. */
    retVal = CSUPLController::GetInstance().
             p_nalExecuteCommand(MCPF_NAL_TLS_FREE_CONNECTION, NULL);
    if(retVal != NAL_TRUE)
    {
        debugMessage(" FreeConnection_native: Command execution FAILED !!! \n");
        //return -1;
    }

    debugMessage(" FreeConnection_native: Exiting Successfully \n");
    //return 0;
}

/*
*******************************************************************
* Function: IsActive_native
*
* Description : Checks the secure network connection current status.
* Function attaches the current thread to JVM and detaches it before
* exiting.
* Parameters : None
* Returns : 0 - connection is accessible, -1 otherwise - the network
* connection is not reachable.
*
*******************************************************************
*/
int IsActive_native()
{
    eNAL_Result retVal = NAL_TRUE;
	int isActive =0;

    debugMessage(" IsActive_native: Entering \n");

    /* TBD: Send 'host_port' with the command. */
    retVal = CSUPLController::GetInstance().
             p_nalExecuteCommand(MCPF_NAL_TLS_IS_ACTIVE, &isActive);

    if (retVal == NAL_UNKNOWN) return -1;

    if(retVal != NAL_TRUE)
    {
        debugMessage(" IsActive_native: Command execution FAILED !!! \n");
        return -1;
    }

    debugMessage(" IsActive_native: %d =  \n",isActive);
    return isActive;
}


/*
*******************************************************************
* Function: Receive_native
*
* Description : Non-blocking function. Launches the Java thread
* responsible for receiving massages from SUPL server. Function
* attaches the current thread to JVM and detaches it before exiting.
*
* Parameters : None
* Returns : 0 - if operation is successful, otherwise, the negative
* error code.
*
*******************************************************************
*/
int Receive_native()
{
    eNAL_Result retVal = NAL_TRUE;

    debugMessage(" Receive_native: Entering \n");

    retVal = CSUPLController::GetInstance().p_nalExecuteCommand(MCPF_NAL_TLS_START_RECEIVE, NULL);
    if(retVal != NAL_TRUE)
    {
        debugMessage(" Receive_native: Command execution FAILED !!! \n");
        return -1;
    }

    debugMessage(" Receive_native: Exiting Successfully \n");
    return 0;
}

/*
*******************************************************************
* Function: MessageBox
*
* Description : Displays the message box with notification about
* network initiated session. Function attaches the current thread
* to JVM and detaches it before exiting.
*
* Parameters :
* verification r Message box type.
*                1 - message box has two buttons - "ALLOW"/"DENY"
*                and allows user to deny network initiated session.
*                0 - message box without option.
* strings      r pointer to array of NotificationBytes_t structures
*                which contain the specific message data.
* size         r number of elements in array.
* Id           r unique notification Id.
* timeout      r message box timeout. Message box will be displayed
                 during this timeout.

* Returns : 0 - if operation successfully,otherwise, the negative
* error code.
*
*******************************************************************
*/

int MessageBox(int verification,
               NotificationBytes_t *strings,
               int size,
               int Id,
               int timeout)
{
	LOGD("+MessageBox..");
      LOGD("MessageBox:verification=%d", verification);
      LOGD("MessageBox:requestorId[0]=[%s]s[%d]e[%d]", strings[0].bytes, strings[0].size, strings[0].encoding);
	  LOGD("MessageBox:requestorId[1]=[%s]s[%d]e[%d]", strings[1].bytes, strings[1].size, strings[1].encoding);
	  LOGD("MessageBox:size=[%d][check-2]", size);
      LOGD("MessageBox:sessionId=%d", Id);
      LOGD("MessageBox:timeout=%d",timeout);	

	TNAL_cmdPaylaod s_payload;

    eNAL_Result retVal = NAL_TRUE;

    s_payload.s_msgBox.verification = verification;
	memcpy( (void *)&s_payload.s_msgBox.strings,(void *)strings, 2 * sizeof(NotificationBytes_t));
	s_payload.s_msgBox.size = size;
	s_payload.s_msgBox.Id = Id;
	s_payload.s_msgBox.timeout = timeout;

      LOGD("MessageBox:verification=%d", s_payload.s_msgBox.verification);
      LOGD("MessageBox:requestorId[0]=[%s]s[%d]e[%d]", s_payload.s_msgBox.strings[0].bytes, s_payload.s_msgBox.strings[0].size, s_payload.s_msgBox.strings[0].encoding);
      LOGD("MessageBox:requestorId[1]=[%s]s[%d]e[%d]", s_payload.s_msgBox.strings[1].bytes, s_payload.s_msgBox.strings[1].size, s_payload.s_msgBox.strings[1].encoding);
	  LOGD("MessageBox:size=[%d][check-2]", size);
      LOGD("MessageBox:sessionId=%d", s_payload.s_msgBox.Id);
      LOGD("MessageBox:timeout=%d", s_payload.s_msgBox.timeout);	


	LOGD("MessageBox: ->MCPF_NAL_MSGBOX");
    retVal = CSUPLController::GetInstance().
             p_nalExecuteCommand(MCPF_NAL_MSGBOX, (void *)&s_payload);
    if(retVal != NAL_TRUE)
    {
  		LOGD("-MessageBox: Command execution FAILED !!!");	
        debugMessage(" MessageBox: Command execution FAILED !!! \n");
        return -1;
    }

    debugMessage(" MessageBox: Exiting Successfully \n");
	LOGD("-MessageBox");	
    return 0;




#if 0
    JNIEnv *env = NULL;
    jint ret = -1;
    if (AttachCurrentThread(&env) == TRUE)
    {
        jbyteArray requestorId = NULL;
        jbyteArray clientName = NULL;
        jboolean verif = JNI_TRUE;

        if (verification <= 0)
        {
            verif = JNI_FALSE;
        }

        if (size == 1 && strings != NULL)
        {
            requestorId = env->NewByteArray(strings[0].size);
            if (requestorId)
            {
                env->SetByteArrayRegion(requestorId, 0, strings[0].size,
                                       (jbyte *) strings[0].bytes);
            }
        }
        else if (size >= 2 && strings != NULL)
        {
            requestorId = env->NewByteArray(strings[0].size);
            if (requestorId)
            {
                env->SetByteArrayRegion(requestorId, 0, strings[0].size,
                                       (jbyte *) strings[0].bytes);
            }

            clientName = env->NewByteArray(strings[1].size);
            if (clientName)
            {
                env->SetByteArrayRegion(clientName, 0, strings[1].size,
                                       (jbyte *) strings[1].bytes);
            }
        }

        jclass clazz = env->FindClass(g_CSUPL_LP_Class);
        if (clazz)
        {
        	LOGD("MessageBox: showSUPLInitDialog");
            jmethodID mid = env->GetStaticMethodID(clazz,
                                                   "showSUPLInitDialog",
                                                   "(Z[BI[BIII)V");
            if (mid)
            {
            	LOGD("MessageBox: ->showSUPLInitDialog");
                env->CallStaticVoidMethod(clazz, mid, verif,
                                          requestorId, strings[0].encoding,
                                          clientName, strings[1].encoding,
                                          Id, timeout);
                ret = 0;
            }
        }
		else
			{
				LOGD("MessageBox: Failed FindClass");
			}

        DetachCurrentThread();
    }

    if (ret < 0)
    {
		LOGD("MessageBox: Failed");

		debugMessage("%s error.", __FUNCTION__);
    }
	LOGD("-MessageBox");
    return ret;
#endif    
}


/*
*******************************************************************
* Function: getSubscriberIdType
*
* Description : Gets the subscriber Id Type 
* Function attaches the current thread to JVM and detaches it
* before exiting.
*
* Parameters : None
*
* Returns : 0 for IMSI, 1 for MSISDN, -ve value for faliure.
*
*******************************************************************
*/
int getSubscriberIdType()
{
    int retVal;

    debugMessage(" getSubscriberIdType: Entering \n");

    retVal = CSUPLController::GetInstance().
             p_nalExecuteCommand(MCPF_NAL_GET_SUBSCRIBER_ID_TYPE, NULL);
    if(retVal < 0)
    {
        debugMessage(" getSubscriberIdType: Command execution FAILED !!! \n");
        return -1;
    }

    debugMessage(" getSubscriberIdType: Exiting Successfully \n");
    return retVal;	
}

/*
*******************************************************************
* Function: getSubscriberId
*
* Description : Gets the unique subscriber Id from SIM card
* Function attaches the current thread to JVM and detaches it
* before exiting.
*
* Parameters :
* buffer       w   buffer for data storing.
* buffer_size  r   buffer size.
*
* Returns : Number of bytes actually coped to buffer, otherwise, the
* negative error code.
*
*******************************************************************
*/
int getSubscriberId(char *buffer, int buffer_size)
{
    s_NAL_subscriberId s_payload;
    eNAL_Result retVal = NAL_TRUE;

    debugMessage(" getSubscriberId: Entering \n");

    memset(&s_payload, '\0', sizeof(s_payload) );

    retVal = CSUPLController::GetInstance().
             p_nalExecuteCommand(MCPF_NAL_GET_SUBSCRIBER_ID, (void *)&s_payload);
    if(retVal != NAL_TRUE)
    {
        debugMessage(" getSubscriberId: Command execution FAILED !!! \n");
        return -1;
    }

//    memcpy((void *)buffer, (void *)s_payload.a_imsiData, sizeof(buffer));
    memcpy((void *)buffer, (void *)s_payload.a_imsiData, s_payload.s32_size);

    debugMessage(" getSubscriberId: buffer_size = %d \n", buffer_size);
    for(int count = 0; count < s_payload.s32_size; count++)
          debugMessage(" getSubscriberId: %x <== \n", buffer[count]);

    debugMessage(" getSubscriberId: Exiting Successfully \n");
    return s_payload.s32_size;
}
/*
*******************************************************************
* Function: getMSISDN
*
* Description : Gets the unique MSISDN from SIM card
* Function attaches the current thread to JVM and detaches it
* before exiting.
*
* Parameters :
* buffer       w   buffer for data storing.
* buffer_size  r   buffer size.
*
* Returns : Number of bytes actually coped to buffer, otherwise, the
* negative error code.
*
*******************************************************************
*/
int getMSISDN(char *buffer, int buffer_size)
{
    s_NAL_msisdn s_payload;
    eNAL_Result retVal = NAL_TRUE;
    debugMessage(" getMSISDNId: Entering \n");
    memset(&s_payload, '\0', sizeof(s_payload) );
    retVal = CSUPLController::GetInstance().
             p_nalExecuteCommand(MCPF_NAL_GET_MSISDN, (void *)&s_payload);
    if(retVal != NAL_TRUE)
    {
        debugMessage(" getMSISDN: Command execution FAILED !!! \n");
        return -1;
    }
    memcpy((void *)buffer, (void *)s_payload.a_msisdnData, s_payload.s32_size);
    debugMessage(" getMSISDN: buffer_size = %d \n", buffer_size);
    for(int count = 0; count < s_payload.s32_size; count++)
          debugMessage(" getMSISDN: %x <== \n", buffer[count]);
    debugMessage(" getMSISDN: Exiting Successfully \n");
    return s_payload.s32_size;
}
/*
*******************************************************************
* Function: getHSLP
*
* Description : Gets the unique MSISDN from SIM card
* Function attaches the current thread to JVM and detaches it
* before exiting.
*
* Parameters :
* buffer       w   buffer for data storing.
* buffer_size  r   buffer size.
*
* Returns : Number of bytes actually coped to buffer, otherwise, the
* negative error code.
*
*******************************************************************
*/
int getHSlp(int *buffer)
{
    s_NAL_hslp s_payload;
    eNAL_Result retVal = NAL_TRUE;
    debugMessage(" getHSLP: Entering \n");
    memset(&s_payload, '\0', sizeof(s_payload) );
    retVal = CSUPLController::GetInstance().
             p_nalExecuteCommand(MCPF_NAL_GET_HSLP, (void *)s_payload.a_hslpData);
    if(retVal != NAL_TRUE)
    {
        debugMessage(" getHSlp: Command execution FAILED !!! \n");
        return -1;
    }
    memcpy((void *)buffer, (void *)s_payload.a_hslpData,sizeof(int)*MAX_HSLP_PARAMS);  
    for(int count = 0; count < MAX_HSLP_PARAMS; count++)
          debugMessage(" getHSLP: %x <== \n", buffer[count]);
    debugMessage(" getHSLP: Exiting Successfully \n");
    return MAX_HSLP_PARAMS;
}
/*
*******************************************************************
* Function: getMccMncSize
*
* Description : Returns the MCC code length plus MNC code length
* Function attaches the current thread to JVM and detaches it
* before exiting.
*
* Parameters : None
*
* Returns : length of MCC code plus length of MNC code, otherwise,
* the negative error code.
*
*******************************************************************
*/
int getMccMncSize()
{
    JNIEnv *env = NULL;
    jclass clazz = NULL;
    jint ret = -1;

    if ((clazz = AttachFindClass (&env, g_CSUPL_LP_Class)) != NULL)
    {
        jmethodID mid = env->GetStaticMethodID(clazz,
                                               "getMccMncSize",
                                               "()I");
        if (mid)
        {
            ret = env->CallStaticIntMethod(clazz, mid);
        }

        DetachCurrentThread();
    }

    if (ret < 0)
    {
        debugMessage("%s error.", __FUNCTION__);
    }

    return ret;
}

/*
*******************************************************************
* Function: getGSMInfo
*
* Description : Gets the GSM info data (MCC, MNC, Cell-Id, Lac)
* Function attaches the current thread to JVM and detaches it
* before exiting.
*
* Parameters :
* info    w pointer to integer array for data storing.
* Returns : number of elements actually filled in array, otherwise,
* the negative error code.
*
*******************************************************************
*/
int getGSMInfo(int *info)
{
    s_NAL_GsmCellInfo s_payload;
    eNAL_Result retVal = NAL_TRUE;
    int count = 0;

    debugMessage(" getGSMInfo: Entering \n");

    memset(&s_payload, '\0', sizeof(s_payload) );

    retVal = CSUPLController::GetInstance().
             p_nalExecuteCommand(MCPF_NAL_GET_CELL_INFO, (void *)&s_payload);
    if(retVal != NAL_TRUE)
    {
        debugMessage(" getGSMInfo: Command execution FAILED !!! \n");
        return -1;
    }

    for (count = 0; count < GSM_INT_CI_LAC_MCC_MNC; count++)
    {
        *(info + count) = s_payload.a_cellInfo[count];
    }

    for (count = GSM_INT_CI_LAC_MCC_MNC; count < GSM_INT_CI_LAC_MCC_MNC_NMR; count++)
    {
        *(info + count) = s_payload.a_nmr[count-GSM_INT_CI_LAC_MCC_MNC];
    }
    debugMessage(" getGSMInfo: Exiting Successfully \n");
    return 0;

}

/*
*******************************************************************
* Function: startSMSListening
*
* Description : Enables the SMS message listening
*
* Parameters : None
* Returns : 0 - if operation successfully, otherwise, the negative
* error code.
*
*******************************************************************
*/
int startSMSListening()
{
    LOGD("+startSMSListening");
    eNAL_Result retVal = NAL_TRUE;

    debugMessage(" startSMSListening: Entering \n");

    retVal = CSUPLController::GetInstance().
             p_nalExecuteCommand(MCPF_NAL_START_SMS_LISTEN, NULL);
    if(retVal != NAL_TRUE)
    {
        LOGD("-startSMSListening: Command execution FAILED !!!");	    
        debugMessage(" startSMSListening: Command execution FAILED !!! \n");
        return -1;
    }

    debugMessage(" startSMSListening: Exiting Successfully \n");
    LOGD("-startSMSListening");	
    return 0;
}

/*
*******************************************************************
* Function: stopSMSListening
*
* Description : Disables the SMS message listening
*
* Parameters : None
* Returns : 0 - if operation successfully, otherwise, the negative
* error code.
*
*******************************************************************
*/
int stopSMSListening()
{
    LOGD("+stopSMSListening");

    eNAL_Result retVal = NAL_TRUE;

    debugMessage(" stopSMSListening: Entering \n");

    retVal = CSUPLController::GetInstance().
             p_nalExecuteCommand(MCPF_NAL_STOP_SMS_LISTEN, NULL);
    if(retVal != NAL_TRUE)
    {
        LOGD("-stopSMSListening: Command execution FAILED !!!");	
        debugMessage(" stopSMSListening: Command execution FAILED !!! \n");
        return -1;
    }

    debugMessage(" stopSMSListening: Exiting Successfully \n");
    LOGD("-stopSMSListening");	
    return 0;
}


/*
*******************************************************************
* Function: startWAPListening
*
* Description : Enables the WAP message listening
*
* Parameters : None
* Returns : 0 - if operation successfully, otherwise, the negative
* error code.
*
*******************************************************************
*/
int startWAPListening()
{
    LOGD("+startWAPListening");

    eNAL_Result retVal = NAL_TRUE;

    debugMessage(" startWAPListening: Entering \n");

    retVal = CSUPLController::GetInstance().
             p_nalExecuteCommand(MCPF_NAL_START_WAP_LISTEN, NULL);
    if(retVal != NAL_TRUE)
    {
	    LOGD("-startWAPListening: Command execution FAILED !!!");	    
        debugMessage(" startWAPListening: Command execution FAILED !!! \n");
        return -1;
    }

    debugMessage(" startWAPListening: Exiting Successfully \n");
    LOGD("-startWAPListening");	
    return 0;
}

/*
*******************************************************************
* Function: stopWAPListening
*
* Description : Disables the WAP message listening
*
* Parameters : None
* Returns : 0 - if operation successfully, otherwise, the negative
* error code.
*
*******************************************************************
*/
int stopWAPListening()
{
	LOGD("+stopWAPListening");

    eNAL_Result retVal = NAL_TRUE;

    debugMessage(" stopWAPListening: Entering \n");

    retVal = CSUPLController::GetInstance().
             p_nalExecuteCommand(MCPF_NAL_STOP_WAP_LISTEN, NULL);
    if(retVal != NAL_TRUE)
    {
  		LOGD("-stopWAPListening: Command execution FAILED !!!");	
        debugMessage(" stopWAPListening: Command execution FAILED !!! \n");
        return -1;
    }

    debugMessage(" stopWAPListening: Exiting Successfully \n");
	LOGD("-stopWAPListening");	
    return 0;
}


/*
*******************************************************************
* Function: SUPL_Deinit
*
* Description : deinitialize the SUPL client
*
* Parameters : None
* Returns : TRUE.
*
*******************************************************************
*/
Engine::uint8_t SUPL_Deinit()
{

    Engine::CSUPLController::GetInstance().SUPLCleanup();

    /* value is ignored */
    return TRUE;
}

/*
*******************************************************************
* Function: SUPL_Init
*
* Description : Initializes the SUPL client and specifies the gpsclbk
* function as a SUPL response method.
*
* Parameters :
* gpsclbk   r SUPL "callback" function or NULL
* Returns : TRUE if no error occurs during the SUPL client
* initialization.
*
*******************************************************************
*/
Engine::uint8_t SUPL_Init(GPSCallBack gpsclbk)
{
    if(gpsclbk == NULL)
    {
        if(!gb_IsSuplInitizialised)
        {
            return FALSE;
        }

        return CSUPLController::GetInstance().SUPLStart();
    }
    else
    {
        CSUPLController::GetInstance().gps_callback = gpsclbk;
        if(CSUPLController::GetInstance().SUPLInit(NULL) != TRUE)
        {
            return FALSE;
        }

        gb_IsSuplInitizialised = TRUE;
    }

    return TRUE;
}

/*
*******************************************************************
* Function: SUPL_Control
*
* Description : Provides the SUPL client command interface.
*
* Parameters :
* AppId   r application Id
* dwCode  r SUPL command code
* pBufIn  r specific command data
* Returns : TRUE if operation is successful.
*
*******************************************************************
*/
Engine::uint8_t SUPL_Control(Engine::uint16_t AppId,
                             Engine::uint32_t dwCode,
                             Engine::uint8_t *pBufIn)
{
    if(CSUPLController::GetInstance().hThread != 0)
    {
        switch (dwCode)
        {
            case UPL_START_REQ:
            case UPL_DATA_REQ:
            case UPL_STOP_REQ:
            case UPL_NOTIFY_RESP:
                {
                    CGPSCommand *cmd = NULL;
                    cmd = CGPSCommand::CreateCommand((Engine::uint64_t) dwCode,
                                                      AppId, NULL, 0, pBufIn);
                    if(!cmd)
                    {
                        return FALSE;
                    }

                    if(Engine::CSUPLController::
                       GetInstance().GetGPS().AddCommand(cmd) != TRUE)
                    {
                        delete cmd;
                        return FALSE;
                    }
                    break;
                }
            default: return FALSE;
        }
        debugMessage("SUPL:SUPL_Control was successfully called");
        return TRUE;
    }
    else
    {
        debugMessage("SUPL controller thread is not started!");
        return FALSE;
    }

}


int saveMessage (char *stringFileName, void *inbuf, int i_size);
/*
*******************************************************************
* Function: ReceivedCommand
*
* Description : The SUPL "callback" response method.
*
* Parameters :
* appId    r Application Id
* Command  r SUPL controller command
* inbuf    r pointer to command data
* Returns : TRUE if no error occurs.
*
*******************************************************************
*/
static bool_t ReceivedCommand(Engine::uint16_t appId,
                              Engine::uint32_t Command,
                              void *inbuf)
{
    int i_size = 0;
    char stringFileName[48];
    debugMessage("Callback is called... Command = %d", Command);
	LOGD("+ReceivedCommand: cmd=[%d]", Command);

    if (inbuf == NULL)
    {
        return FALSE;
    }

    if (checkAppId(appId) == JNI_TRUE)
    {
        switch (Command)
        {
            case UPL_NOTIFY_IND:
                i_size = sizeof (Platform::NotifyIndData);
                break;
            case UPL_START_LOC_IND:
                i_size = sizeof (Platform::StartLocIndData);
                break;
            case UPL_POS_IND:
                i_size = sizeof (Platform::PosIndData);
                break;
            case UPL_DATA_IND:
                i_size = sizeof (Platform::DataIndData);
                break;
            default:
		if(inbuf != NULL)
		free(inbuf);
                return FALSE;
        }

        generateFileName(stringFileName, appId, Command);
        saveMessage(stringFileName, inbuf, i_size);
        putMessage(appId, Command, stringFileName);
    }

    if (Command == UPL_STOP_IND)
    {
        Platform::StopIndData *dta = (Platform::StopIndData *) inbuf;
        debugMessage("%s callback error: %d, app_id: %d",
                     __FUNCTION__,
                     dta->gps_stoped,
                     appId);
    }

    if(appId == SUPL_APP_ID && Command == UPL_POS_IND)
    {

        Platform::PosIndData *dta = (Platform::PosIndData *) inbuf;
        Platform::Position_t *position = &((Platform::PosIndData *) inbuf)->position;
        debugMessage("Callback with UPL_POS_IND!");
        debugMessage("%s, UTCTimeStampNumByte: %d",
                     __FUNCTION__,
                     position->UTCTimeStampNumByte);

        debugMessage("%s, latitude_sign: %d",
                     __FUNCTION__,
                     position->latitude_sign);

        debugMessage("%s, pos_opt_bitmap: %d",
                     __FUNCTION__,
                     position->pos_opt_bitmap);

        debugMessage("%s, altitudeDirection: %d",
                     __FUNCTION__,
                     position->altitudeDirection);

        debugMessage("%s, altUncertainty: %d",
                     __FUNCTION__,
                     position->altUncertainty);

        debugMessage("%s, uncertaintySemiMajor: %d",
                     __FUNCTION__,
                     position->uncertaintySemiMajor);

        debugMessage("%s, uncertaintySemiMinor: %d",
                     __FUNCTION__,
                     position->uncertaintySemiMinor);

        debugMessage("%s, orientationMajorAxis: %d",
                     __FUNCTION__,
                     position->orientationMajorAxis);

        debugMessage("%s, confidence: %d",
                     __FUNCTION__,
                     position->confidence);

        debugMessage("%s, latitude: %d",
                     __FUNCTION__,
                     position->latitude);

        debugMessage("%s, longtitude: %d",
                     __FUNCTION__,
                     position->longtitude);

        debugMessage("%s, altitude: %d",
                     __FUNCTION__,
                     position->altitude);

        pthread_mutex_lock(&g_PositionMutex);
        memcpy((void *) &g_Position, position, sizeof(Platform::Position_t));
        pthread_mutex_unlock(&g_PositionMutex);

        // Send the event
        pthread_mutex_lock(&g_EventMutex);
        pthread_cond_signal(&g_EventCond);
        pthread_mutex_unlock(&g_EventMutex);
    }

    if(appId == EE_CLIENT_APPLICATION_ID)
    {
        //return EECallBack(appId, Command, inbuf);
    }

#if defined(__SUPL_TEST)
    if (Command == UPL_START_LOC_IND)
    {
        Platform::StartReqData *data_struct;
        data_struct = (StartReqData *)malloc(sizeof(StartReqData));
		if(data_struct == NULL)
		return FALSE;
        FillStartReqData(data_struct);
        debugMessage("%s: NI with appId = %d", __FUNCTION__, appId);
        SUPL_Control(appId, UPL_START_REQ, (Engine::uint8_t *)data_struct);
    }

    resp_flag = 1;
#endif

    free(inbuf);
    debugMessage("%s free() inbuf OK", __FUNCTION__);
	LOGD("-ReceivedCommand");
    return TRUE;
}


/*
*******************************************************************
* Function: FillStartReqData
*
* Description : Fills the SUPL start request data structure
* according to E-CID positioning mode.
*
* Parameters :
* data w pointer to StartReqData structure
* Returns : 0 on success, otherwise, the negative error code.
*
*******************************************************************
*/
int FillStartReqData(Platform::StartReqData *data)
{
    int info[GSM_INT_CI_LAC_MCC_MNC_NMR];
    debugMessage("%s : BEGIN", __FUNCTION__);
    memset(data, 0, sizeof(Platform::StartReqData));
#if defined(_DEBUG_IMSI)
    memcpy(data->imsi.imsi_buf, g_imsi_dbg, IMSI_CODE_SIZE);
    data->imsi.imsi_size = IMSI_CODE_SIZE;
#endif
    if (getGSMInfo(info) < 0)
    {
        debugMessage("%s, error get GSM info data", __FUNCTION__);
        return -1;
    }
    debugMessage("%s, MCC: %d, MNC: %d, LAC: %d, CI: %d", __FUNCTION__,
                 info[2], info[3], info[1], info[0]);
    data->set.pos_technology_bitmap = E_CID_OFFSET;
    data->set.pref_method = Platform::AGPS_NO_PREF;
    data->set.pos_protocol_bitmap = Platform::RRLP_MASK | RRC_MASK;
#if defined(DOCOMO_SUPPORT_WCDMA)
    data->lac.cell_info.cell_type = Platform::CellInfo_t::WCDMA;
    data->lac.cell_info.cell_info_wcdma.wcdma_mcc = info[2];
    data->lac.cell_info.cell_info_wcdma.wcdma_mnc = info[3];
    data->lac.cell_info.cell_info_wcdma.wcdma_ci = info[0];
    data->lac.cell_info.cell_info_wcdma.wcdma_opt_param = 0;
    data->lac.cell_info_status = Platform::PRESENT_CELL_INFO;
#else
    data->lac.cell_info.cell_type = Platform::CellInfo_t::GSM;
    data->lac.cell_info.cell_info_gsm.gsm_mcc = info[2];
    data->lac.cell_info.cell_info_gsm.gsm_mnc = info[3];
    data->lac.cell_info.cell_info_gsm.gsm_lac = info[1];
    data->lac.cell_info.cell_info_gsm.gsm_ci  = info[0];
    data->lac.cell_info.cell_info_gsm.gsm_opt_param = 0;
    data->lac.cell_info_status = Platform::PRESENT_CELL_INFO;
#endif
    /*Filling a_data field */
    data->a_data.assitance_req_bitmap = 0;

    /*Filling app_id field */
    data->app_id = SUPL_APP_ID;
    debugMessage("%s : END", __FUNCTION__);
    return 0;
}

/*
*******************************************************************
* Function: AlarmHandler
*
* Description : SIGALRM signal handler.
*
* Parameters : None
* Returns : None
*
*******************************************************************
*/
void AlarmHandler()
{
    gi_Counter++;
    debugMessage("AlarmHandler gi_Counter = %d", gi_Counter);
    pthread_mutex_lock(&g_NavigatingMutex);
    pthread_cond_signal(&g_NavigatingCond);
    pthread_mutex_unlock(&g_NavigatingMutex);
}

/*
*******************************************************************
* Function: MilliSleep
*
* Description : Sleeps for the specified number of milliseconds.
*
* Parameters :
* ms     r sleep time in milliseconds
* Returns : none
*
*******************************************************************
*/
void MilliSleep(int ms)
{
    struct timespec ts;

    ts.tv_sec = 0;
    ts.tv_nsec = ms * 1000 * 1000;
    nanosleep (&ts, NULL);
}

/*
*******************************************************************
* Function: SuplLocationProviderProc
*
* Description : Thread function, it sends periodic position requests
* to SUPL server.
*
* Parameters :
* Params  r thread function argument
* Returns : thread execution result
*
*******************************************************************
*/
void* SuplLocationProviderProc(void *Params)
{
    Engine::uint32_t out_size = 0;
    StartReqData *data_struct;
    struct sigaction  _sig;
    struct sigaction  _sig1;
    struct itimerval  _old;
    struct itimerval  _new;

    pthread_mutex_lock(&g_StartMutex);
    pthread_mutex_unlock(&g_StartMutex);
    _sig.sa_handler = (__sighandler_t) AlarmHandler;
    sigemptyset(&_sig.sa_mask);
    sigaddset(&_sig.sa_mask, SIGALRM);
    _sig.sa_flags = 0;
    sigaction(SIGALRM, &_sig, NULL);

    _new.it_interval.tv_usec = 0;
    _new.it_interval.tv_sec = gi_FixInterval;
    _new.it_value.tv_usec = 0;
    /*first ALARM will be sent after 5 sec */
    _new.it_value.tv_sec = FIRST_SUPL_REQUEST_TIMEOUT_S;

    if (setitimer(ITIMER_REAL, &_new, &_old) < 0)
    {
        gb_IsSuplNavigatingStarted = FALSE;
        return (void *) -1;
    }
    else
    {
        debugMessage("old.it_value.tv_sec = %ld, interval = %ld",
                     _old.it_value.tv_sec, _new.it_interval.tv_sec);
    }

    while (1)
    {
        debugMessage("Waiting for alarm:");
        pthread_mutex_lock(&g_NavigatingMutex);
        pthread_cond_wait(&g_NavigatingCond, &g_NavigatingMutex);
        pthread_mutex_unlock(&g_NavigatingMutex);

        if (g_NeedStopSuplNavigating)
        {
            setitimer(ITIMER_REAL, &_old, NULL);
            gb_IsSuplNavigatingStarted = FALSE;
            g_NeedStopSuplNavigating = 0;
            debugMessage("Exit from loop:");
            return (void *) NULL;
        }
        debugMessage("Try to request the position:");
        data_struct = (StartReqData *) malloc(sizeof(StartReqData));
	if(data_struct == NULL)
	return (void *) NULL;
        if (FillStartReqData(data_struct) == 0)
        {
            if (SUPL_Control(data_struct->app_id, UPL_START_REQ,
                            (Engine::uint8_t *) data_struct) != TRUE)
            {
                free(data_struct);
                debugMessage("%s, SUPL_Control() error.", __FUNCTION__);
            }
        }
    }
}

/*
*******************************************************************
* Function: SuplStopNavigating
*
* Description : Signals to stop periodic position requests and
* waits until SuplLocationProviderProc thread has finished.
*
* Parameters : None
* Returns : None
*
*******************************************************************
*/
void SuplStopNavigating()
{
    while(gb_IsSuplNavigatingStarted)
    {
        g_NeedStopSuplNavigating = 1;
        pthread_mutex_lock(&g_NavigatingMutex);
        pthread_cond_signal(&g_NavigatingCond);
        pthread_mutex_unlock(&g_NavigatingMutex);

        MilliSleep(SUPL_STOP_NAVIGATING_WAIT_TIMEOUT_MS);

        debugMessage("SuplStopNavigating!");
    }
}
/*
*******************************************************************
* Function: SuplStartNavigating
*
* Description : Starts the periodic position requests with
* specified period.
*
* Parameters :
* NavigatingPeriod  r position requests period.
* Returns : 0 if operation is successful.
*           -1 if navigating thread creation fails
*******************************************************************
*/
int SuplStartNavigating(int NavigatingPeriod)
{
    if (gb_IsSuplNavigatingStarted == TRUE)
    {
        return 0;
    }
    g_SuplNavigatingThread = 0;
    pthread_attr_t pAttr;
    pthread_attr_init(&pAttr);
    pthread_attr_setdetachstate(&pAttr, PTHREAD_CREATE_DETACHED);

    pthread_mutex_lock(&g_StartMutex);

    int r = pthread_create(&g_SuplNavigatingThread, &pAttr,
                           SuplLocationProviderProc, NULL);
    if (r < 0)
    {
        pthread_mutex_unlock(&g_StartMutex);
        return -1;
    }

    pthread_attr_destroy(&pAttr);
    g_NeedStopSuplNavigating = 0;
    gb_IsSuplNavigatingStarted = TRUE;
    gi_Counter = 0;
    pthread_mutex_unlock(&g_StartMutex);

    return 0;
}
/*
*******************************************************************
* Function: SuplInitProc
*
* Description : Initializes SUPL Client
*
* Parameters :
* Par  r not used.
* Returns : see SUPL_Init function in SUPL Client
*******************************************************************
*/
static void* SuplInitProc(void *Par)
{
    int iret;
    debugMessage("%s :before the SUPL_Init() call", __FUNCTION__);

    iret = SUPL_Init(ReceivedCommand);
    if (iret != TRUE)
    {
        debugMessage("%s :SUPL_INIT with callback error: %d", __FUNCTION__, iret);
        return (void *) iret;
    }

    iret = SUPL_Init(NULL);
    if (iret != TRUE)
    {
        debugMessage("%s :SUPL_INIT without callback error: %d", __FUNCTION__, iret);
        return (void *) iret;
    }

    debugMessage("%s : SUPL initialization success: %d", __FUNCTION__, iret);
    return (void *) iret;
}


/*
*******************************************************************
* Function: Java_android_supl_SuplLocationProvider_nativePopulateSuplPosition
*
* Description : native implementation of nativePopulateSuplPosition method
*               of android_supl_SuplLocationProvider class. See
*               SuplLocationProvider Java class for details
*
* Parameters : see SuplLocationProvider Java class
* Returns : See SuplLocationProvider Java class
*******************************************************************
*/
JNIEXPORT jboolean JNICALL
Java_android_supl_SuplLocationProvider_nativePopulateSuplPosition
(JNIEnv *env, jobject obj, jobject pos)
{
    struct Platform::Position_t mPos;
    debugMessage("%s BEGIN", __FUNCTION__);
    pthread_mutex_lock(&g_PositionMutex);
    memcpy((void *) &mPos, (void *) &g_Position, sizeof(Platform::Position_t));
    pthread_mutex_unlock(&g_PositionMutex);

    jclass clazz = env->GetObjectClass(pos);
    if (!clazz)
    {
        return JNI_FALSE;
    }

    // UTCTimeStampNumByte
    env->SetIntField(pos,
                     env->GetFieldID(clazz, "UTCTimeStampNumByte", "I"),
                     (jint) mPos.UTCTimeStampNumByte);

    // UTCTimeStamp
    jbyteArray arr;
    arr = (jbyteArray)env->GetObjectField(pos, env->GetFieldID(clazz,
                                                               "UTCTimeStamp",
                                                               "[B"));
    env->SetByteArrayRegion(arr, 0, 20, (jbyte *) &mPos.UTCTimeStamp[0]);

    // latitude_sign
    env->SetIntField(pos,
                     env->GetFieldID(clazz, "latitude_sign", "I"),
                     (jint) mPos.latitude_sign);

    // pos_opt_bitmap
    env->SetIntField(pos,
                     env->GetFieldID(clazz, "pos_opt_bitmap", "I"),
                     (jint) mPos.pos_opt_bitmap);

    // altitudeDirection
    env->SetIntField(pos,
                     env->GetFieldID(clazz, "altitudeDirection", "I"),
                     (jint) mPos.altitudeDirection);

    // altUncertainty
    env->SetIntField(pos,
                     env->GetFieldID(clazz, "altUncertainty", "I"),
                     (jint) mPos.altitudeDirection);

    // uncertaintySemiMajor
    env->SetIntField(pos,
                     env->GetFieldID(clazz, "uncertaintySemiMajor", "I"),
                     (jint) mPos.uncertaintySemiMajor);

    // uncertaintySemiMinor
    env->SetIntField(pos,
                     env->GetFieldID(clazz, "uncertaintySemiMinor", "I"),
                     (jint) mPos.uncertaintySemiMinor);

    // orientationMajorAxis
    env->SetIntField(pos,
                     env->GetFieldID(clazz, "orientationMajorAxis", "I"),
                     (jint) mPos.orientationMajorAxis);

    // confidence
    env->SetIntField(pos,
                     env->GetFieldID(clazz, "confidence", "I"),
                     (jint) mPos.confidence);

    // latitude
    env->SetIntField(pos,
                     env->GetFieldID(clazz, "latitude", "I"),
                     (jint) mPos.latitude);

    // longtitude
    env->SetIntField(pos,
                     env->GetFieldID(clazz, "longtitude", "I"),
                     (jint) mPos.longtitude);

    // altitude
    env->SetIntField(pos,
                     env->GetFieldID(clazz, "altitude", "I"),
                     (jint) mPos.altitude);

    debugMessage("%s END", __FUNCTION__);
    return JNI_TRUE;
}

/*
*******************************************************************
* Function: Java_android_supl_SuplLocationProvider_nativeWaitForEvent
*
* Description : native implementation of nativeWaitForEvent method
*               of android_supl_SuplLocationProvider class. See
*               SuplLocationProvider Java class for details
*
* Parameters : see SuplLocationProvider Java class
* Returns : See SuplLocationProvider Java class
*******************************************************************
*/
JNIEXPORT jint JNICALL
Java_android_supl_SuplLocationProvider_nativeWaitForEvent
(JNIEnv *, jobject)
{
    pthread_mutex_lock(&g_EventMutex);
    pthread_cond_wait(&g_EventCond, &g_EventMutex);
    pthread_mutex_unlock(&g_EventMutex);
    return 0;
}

int createSuplExchangeDir(const char *path);
/*
*******************************************************************
* Function: Java_android_supl_SuplLocationProvider_nativeEnable
*
* Description : native implementation of nativeEnable method
*               of android_supl_SuplLocationProvider class. See
*               SuplLocationProvider Java class for details
*
* Parameters : see SuplLocationProvider Java class
* Returns : See SuplLocationProvider Java class
*******************************************************************
*/
JNIEXPORT jboolean JNICALL
Java_android_supl_SuplLocationProvider_nativeEnable
(JNIEnv *env, jobject obj)
{
    int *addr = NULL;
    pthread_t initHId;
    debugMessage("%s BEGIN", __FUNCTION__);
    if (gb_IsSuplStopped)
    {
        int r = pthread_create(&initHId, NULL, SuplInitProc, NULL);
        if (r < 0)
        {
            return JNI_FALSE;
        }

        debugMessage("%s try to join init process", __FUNCTION__);
        pthread_join(initHId, (void **) &addr);
        if ((int) addr != TRUE)
        {
            debugMessage("SUPL is not started!");
            gb_IsSuplStopped = TRUE;
            return JNI_FALSE;
        }
        else
        {
            debugMessage("SUPL is started!");
            gb_IsSuplStopped = FALSE;
        if (createSuplExchangeDir("/data/supl_exchange/") != 0)
            {
            debugMessage("/data/supl_exchange directory is not created");
                return JNI_FALSE;
            }
            return JNI_TRUE;
        }
    }

    return JNI_TRUE;
}

/*
*******************************************************************
* Function: Java_android_supl_SuplLocationProvider_nativeDisable
*
* Description : native implementation of nativeDisable method
*               of android_supl_SuplLocationProvider class. See
*               SuplLocationProvider Java class for details
*
* Parameters : see SuplLocationProvider Java class
* Returns : See SuplLocationProvider Java class
*******************************************************************
*/
JNIEXPORT void JNICALL
Java_android_supl_SuplLocationProvider_nativeDisable
(JNIEnv *env, jobject obj)
{
    if (gb_IsSuplNavigatingStarted == TRUE)
    {
        SuplStopNavigating();
    }

    if (!gb_IsSuplStopped)
    {
        SUPL_Deinit();
    }

    gb_IsSuplStopped = TRUE;
    pthread_mutex_lock(&g_CloseMutex);
    pthread_cond_signal(&g_CloseCond);
    pthread_mutex_unlock(&g_CloseMutex);
}

/*
*******************************************************************
* Function: Java_android_supl_SuplLocationProvider_nativeChangeFixInterval
*
* Description : native implementation of nativeChangeFixInterval method
*               of android_supl_SuplLocationProvider class. See
*               SuplLocationProvider Java class for details
*
* Parameters : see SuplLocationProvider Java class
* Returns : See SuplLocationProvider Java class
*******************************************************************
*/
JNIEXPORT void JNICALL
Java_android_supl_SuplLocationProvider_nativeChangeFixInterval
(JNIEnv *env, jobject obj, jint fixPeriod)

{
    if (fixPeriod < MIN_SUPL_FIX_INTERVAL_S)
    {
        fixPeriod = MIN_SUPL_FIX_INTERVAL_S;
    }

    gi_FixInterval = fixPeriod;

    if (gb_IsSuplNavigatingStarted == TRUE)
    {
        SuplStopNavigating();
        SuplStartNavigating(fixPeriod);
    }
}

/*
*******************************************************************
* Function: Java_android_supl_SuplLocationProvider_nativeStop
*
* Description : native implementation of nativeStop method
*               of android_supl_SuplLocationProvider class. See
*               SuplLocationProvider Java class for details
*
* Parameters : see SuplLocationProvider Java class
* Returns : See SuplLocationProvider Java class
*******************************************************************
*/
JNIEXPORT jboolean JNICALL
Java_android_supl_SuplLocationProvider_nativeStop
(JNIEnv *env, jobject obj)
{
    if (gb_IsSuplNavigatingStarted == TRUE)
    {
        SuplStopNavigating();
    }
    return JNI_TRUE;
}

/*
*******************************************************************
* Function: Java_android_supl_SuplLocationProvider_nativeStart
*
* Description : native implementation of nativeStart method
*               of android_supl_SuplLocationProvider class. See
*               SuplLocationProvider Java class for details
*
* Parameters : see SuplLocationProvider Java class
* Returns : See SuplLocationProvider Java class
*******************************************************************
*/
JNIEXPORT jboolean JNICALL
Java_android_supl_SuplLocationProvider_nativeStart
(JNIEnv *env, jobject obj, jint fixPeriod)
{
    if (fixPeriod < MIN_SUPL_FIX_INTERVAL_S)
    {
        fixPeriod = MIN_SUPL_FIX_INTERVAL_S;
    }

    gi_FixInterval = fixPeriod;

    if (gb_IsSuplNavigatingStarted == FALSE)
    {
        SuplStartNavigating(fixPeriod);
    }
    return JNI_TRUE;
}

/*
* FIXME: remove NetworkUnitTestProc and
* Java_android_networking_CNet_StartNativeUnitTest functions
* in final release
*/
#if defined(__SUPL_TEST)
void * NetworkUnitTestProc(void *arg)
{
    ///---??????? Test Scenario
    char message [] = {0x0, 0x1b, 0x1, 0x0, 0x0,
                       0x84, 0xe2, 0xe, 0x2, 0x44, //0x44
                       0x8d, 0x15, 0x9e, 0x24, 0x7,
                       0xfc, 0x41, 0x4, 0x40, 0xf,
                       0xa0, 0xe, 0x50, 0x1c, 0x22,
                       0x1c, 0x80};

    unsigned char suplposinit[]={0x0, 0x24, 0x1, 0x0,
                                 0x0, 0xc4, 0xe2, 0xe,
                                 0x2, 0x44, 0x8d, 0x15,
                                 0x9e, 0x24, 0x7, 0xfc,
                                 0x0, 0x0, 0xc6, 0x49,
                                 0x3, 0x0, 0x2, 0x33,
                                 0xfc, 0x30, 0x8, 0x22,
                                 0x0, 0x7d, 0x0, 0x72,
                                 0x80, 0xe1, 0x10, 0xe4};

    int len = 27;
    int len1 = 36;
    int ret = -1;

    if (Init_native() < 0)
    {
        goto exit;
    }

    if (CreateConnection_native(unit_test_hp)  < 0)
    {
        goto exit;
    }

    if (IsActive_native() == 0)
    {
        goto exit;
    }

    if (Receive_native() < 0)
    {
        FreeConnection_native();
        goto exit;
    }

    if (Send_native(message, len)  < 0)
    {
        FreeConnection_native() ;
        goto exit;
    }

    sleep(10);

    if (Send_native(suplposinit, len1)  < 0)
    {
        FreeConnection_native() ;
        goto exit;
    }

    sleep(10);
    FreeConnection_native() ;

exit:
    pthread_mutex_lock(&g_CloseMutex);
    pthread_cond_signal(&g_CloseCond);
    pthread_mutex_unlock(&g_CloseMutex);
    return (void *)ret;
}

JNIEXPORT void JNICALL
Java_android_networking_CNet_StartNativeUnitTest(JNIEnv *env, jclass clazz)
{
    int r = pthread_create(&g_UnitTestThread, 0, NetworkUnitTestProc, 0);

    if (r != 0)
    {
        //debugMessage(__FUNCTION__, "Unable to create network thread", env);
        return;
    }
    //else
    //{
    //    debugMessage(__FUNCTION__, "Network thread started", env);
    //    debugMessage(__FUNCTION__, "Wait for close signal...", env);
    //}

    pthread_mutex_lock(&g_CloseMutex);
    pthread_cond_wait(&g_CloseCond, &g_CloseMutex);
    pthread_mutex_unlock(&g_CloseMutex);


    //debugMessage(__FUNCTION__, "***CLOSE SIGNAL ***", env);
}
#endif


/*
 * Class:     android_supl_SuplLocationProvider
 * Method:    nativeSUPL_Control_Start
 * Signature: (IILandroid/supl/SuplInterface/StartReqData;)S
 */
JNIEXPORT jshort
JNICALL Java_android_supl_SuplLocationProvider_nativeSUPL_1Control_1Start
(JNIEnv *env, jobject obj, jint AppId, jint code, jobject data)
{
    debugMessage("SUPL_Control was called from JAVA");
    jclass clazz = env->GetObjectClass(data);
    if (!clazz)
    {
        debugMessage("Can't get object class");
        return -1;
    }

    using namespace Platform;
    Platform::StartReqData *data_struct;
    data_struct = (StartReqData *)malloc(sizeof(StartReqData));
    /* KW changes*/
    if(data_struct == NULL)
	return -1;

    Engine::uint16_t app_id = AppId;
    Engine::uint32_t dwCode = code;

    // AppId
    data_struct->app_id = app_id;
    // start_opt_param
    data_struct->start_opt_param = env->GetShortField(data, env->GetFieldID(clazz, "start_opt_param", "S"));
    // Set capabilities
    data_struct->set.pos_technology_bitmap = env->GetShortField(data, env->GetFieldID(clazz, "pos_technology_bitmap", "S"));
    data_struct->set.pos_protocol_bitmap = env->GetShortField(data, env->GetFieldID(clazz, "pos_protocol_bitmap", "S"));
    data_struct->set.pref_method = (Platform::PrefMethod_t) env->GetShortField(data, env->GetFieldID(clazz, "pref_method", "S"));
    // LocationID
    data_struct->lac.cell_info.cell_type = (Platform::CellInfo_t::CELL_TYPE) env->GetShortField(data, env->GetFieldID(clazz, "cell_type", "S"));
    data_struct->lac.cell_info.cell_info_gsm.gsm_mcc = env->GetIntField(data, env->GetFieldID(clazz, "gsm_mcc", "I"));
    data_struct->lac.cell_info.cell_info_gsm.gsm_mnc = env->GetIntField(data, env->GetFieldID(clazz, "gsm_mnc", "I"));
    data_struct->lac.cell_info.cell_info_gsm.gsm_lac = env->GetIntField(data, env->GetFieldID(clazz, "gsm_lac", "I"));;
    data_struct->lac.cell_info.cell_info_gsm.gsm_ci = env->GetIntField(data, env->GetFieldID(clazz, "gsm_ci", "I"));;
    data_struct->lac.cell_info.cell_info_gsm.nmr_quantity = env->GetShortField(data, env->GetFieldID(clazz, "nmr_quantity", "S"));
    data_struct->lac.cell_info.cell_info_gsm.gsm_ta = env->GetShortField(data, env->GetFieldID(clazz, "gsm_ta", "S"));
    data_struct->lac.cell_info.cell_info_gsm.gsm_opt_param = env->GetShortField(data, env->GetFieldID(clazz, "gsm_opt_param", "S"));
    data_struct->lac.cell_info_status = (Platform::CellInfoStatus_t) env->GetShortField(data, env->GetFieldID(clazz, "cell_info_status", "S"));

    jshortArray bsic = (jshortArray) env->GetObjectField(data, env->GetFieldID(clazz, "bsic", "[S"));
    jshortArray rxlev = (jshortArray) env->GetObjectField(data, env->GetFieldID(clazz, "rxlev", "[S"));
    jintArray arfcn = (jintArray) env->GetObjectField(data, env->GetFieldID(clazz, "arfcn", "[I"));
    if (bsic && rxlev && arfcn)
    {
        jsize size = env->GetArrayLength(bsic);
        jshort bsic_buffer[size];
        env->GetShortArrayRegion(bsic, 0, size, (jshort *) bsic_buffer);
        jshort rxlev_buffer[size];
        env->GetShortArrayRegion(rxlev, 0, size, (jshort *) rxlev_buffer);
        jint arfcn_buffer[size];
        env->GetIntArrayRegion(arfcn, 0, size, (jint *) arfcn_buffer);
        for (int i = 0; i < size; i++)
        {
            (*(data_struct->lac.cell_info.cell_info_gsm.gsm_nmr + i)).bsic = (Engine::uint8_t) *(bsic_buffer + i);
            (*(data_struct->lac.cell_info.cell_info_gsm.gsm_nmr + i)).rxlev = (Engine::uint8_t) *(rxlev_buffer + i);
            (*(data_struct->lac.cell_info.cell_info_gsm.gsm_nmr + i)).arfcn = (Engine::uint16_t) *(arfcn_buffer + i);
        }
    }
    // QoP
    data_struct->qop.horr_acc = env->GetShortField(data, env->GetFieldID(clazz, "horr_acc", "S"));
    data_struct->qop.qop_optional_bitmap = env->GetShortField(data, env->GetFieldID(clazz, "qop_optional_bitmap", "S"));
    data_struct->qop.ver_acc = env->GetShortField(data, env->GetFieldID(clazz, "ver_acc", "S"));
    data_struct->qop.max_loc_age = env->GetIntField(data, env->GetFieldID(clazz, "max_loc_age", "I"));
    data_struct->qop.delay = env->GetShortField(data, env->GetFieldID(clazz, "delay", "S"));
    // ReqAssistedData
    data_struct->a_data.assitance_req_bitmap = env->GetIntField(data, env->GetFieldID(clazz, "assistance_req_bitmap", "I"));
    data_struct->a_data.gpsWeak = env->GetIntField(data, env->GetFieldID(clazz, "gpsWeak", "I"));
    data_struct->a_data.gpsToe = env->GetShortField(data, env->GetFieldID(clazz, "gpsToe", "S"));
    data_struct->a_data.nSAT = env->GetShortField(data, env->GetFieldID(clazz, "nSAT", "S"));
    data_struct->a_data.toeLimit = env->GetShortField(data, env->GetFieldID(clazz, "toeLimit", "S"));
    data_struct->a_data.num_sat_info = env->GetShortField(data, env->GetFieldID(clazz, "num_sat_info", "S"));

    jshortArray satelliteID = (jshortArray) env->GetObjectField(data, env->GetFieldID(clazz, "satelliteID", "[S"));
    jshortArray iode = (jshortArray) env->GetObjectField(data, env->GetFieldID(clazz, "iode", "[S"));
    if (satelliteID && iode)
    {
        jsize size = env->GetArrayLength(satelliteID);
        short satelliteID_buffer[size];
        env->GetShortArrayRegion(satelliteID, 0, size, (jshort *) satelliteID_buffer);
        jshort iode_buffer[size];
        env->GetShortArrayRegion(iode, 0, size, (jshort *) iode_buffer);
        for (int i = 0; i < size; i++)
        {
            (*(data_struct->a_data.sat_info_elemnet + i)).satelliteID = (Engine::uint8_t) *(satelliteID_buffer + i);
            (*(data_struct->a_data.sat_info_elemnet + i)).iode = (Engine::uint8_t) *(iode_buffer + i);
        }
    }

    data_struct->a_data.ee_info.validity = env->GetIntField(data, env->GetFieldID(clazz, "validity", "I"));
    data_struct->a_data.ee_info.StartCurExt.week = env->GetIntField(data, env->GetFieldID(clazz, "start_cur_ext_week", "I"));
    data_struct->a_data.ee_info.EndCurExt.week = env->GetIntField(data, env->GetFieldID(clazz, "end_cur_ext_week", "I"));
    data_struct->a_data.ee_info.StartCurExt.HourOfWeek = env->GetShortField(data, env->GetFieldID(clazz, "start_cur_ext_hour_of_week", "S"));
    data_struct->a_data.ee_info.EndCurExt.HourOfWeek = env->GetShortField(data, env->GetFieldID(clazz, "end_cur_ext_hour_of_week", "S"));

    debugMessage("app_id = %d | start_opt_param = %d", app_id, data_struct->start_opt_param);
    SUPL_Control(data_struct->app_id, code, (Engine::uint8_t*) data_struct);
    debugMessage("SUPL_Control_Start was called!");

    return 1;
}

/*
 * Class:     android_supl_SuplLocationProvider
 * Method:    nativeSUPL_Control_Data
 * Signature: (IILandroid/supl/SuplInterface/DataReqData;)S
 */
JNIEXPORT jshort JNICALL Java_android_supl_SuplLocationProvider_nativeSUPL_1Control_1Data
  (JNIEnv *env, jobject obj, jint AppId, jint code, jobject data)
{
    debugMessage("SUPL_Control was called from JAVA");
    jclass clazz = env->GetObjectClass(data);
    if (!clazz)
    {
        debugMessage("Can't get object class");
        return -1;
    }

    using namespace Platform;
    Platform::DataReqData *data_struct;
    data_struct = (DataReqData *) malloc(sizeof(DataReqData));

        /* KW changes*/
    if(data_struct == NULL)
	return -1;
    Engine::uint16_t app_id = AppId;
    Engine::uint32_t dwCode = code;

    // AppId
    data_struct->app_id = app_id;
    // Last RRLP packet
    data_struct->last_rrlp_packet = env->GetShortField(data, env->GetFieldID(clazz, "last_rrlp_packet", "S"));
    // SUPLPosPayload
    data_struct->pos_payload.type = (Platform::SUPLPosPayload_t::PayloadType) env->GetShortField(data, env->GetFieldID(clazz, "payload_type", "S"));
    data_struct->pos_payload.payload.ctrl_pdu_len = env->GetIntField(data, env->GetFieldID(clazz, "ctrl_pdu_len", "I"));

    jshortArray ctrl_pdu = (jshortArray) env->GetObjectField(data, env->GetFieldID(clazz, "ctrl_pdu", "[S"));
    if (ctrl_pdu)
    {
        jsize size = env->GetArrayLength(ctrl_pdu);
        debugMessage("SUPL_Control_Data: size = %d", size);
        jshort buffer[size];
        env->GetShortArrayRegion(ctrl_pdu, 0, size, (jshort *) buffer);
        for (int i = 0; i < size; i++)
        {
            *(data_struct->pos_payload.payload.ctrl_pdu + i) = (Engine::uint8_t) *(buffer + i);
        }
    }

    SUPL_Control(data_struct->app_id, code, (Engine::uint8_t *) data_struct);
    debugMessage("SUPL_Control_Data was called!");

    return 1;
}

/*
 * Class:     android_supl_SuplLocationProvider
 * Method:    nativeSUPL_Control_Stop
 * Signature: (IILandroid/supl/SuplInterface/StopReqData;)S
 */
JNIEXPORT jshort
JNICALL Java_android_supl_SuplLocationProvider_nativeSUPL_1Control_1Stop
(JNIEnv *env, jobject obj, jint AppId, jint code, jobject data)
{
    debugMessage("SUPL_Control was called from JAVA");
    jclass clazz = env->GetObjectClass(data);
    if (!clazz)
    {
        debugMessage("Can't get object class");
        return -1;
    }

    using namespace Platform;
    Platform::StopReqData* data_struct;
    data_struct = (StopReqData*)malloc(sizeof(StopReqData));

    if(data_struct == NULL)
	return -1;
    Engine::uint16_t app_id = AppId;
    Engine::uint32_t dwCode = code;

    // AppId
    data_struct->app_id = app_id;
    // Status Code
    data_struct->gps_stoped = (Platform::StatusCode)
                               env->GetShortField(data,
                                                  env->GetFieldID(clazz,
                                                                  "gps_stoped",
                                                                  "S"));
    SUPL_Control(data_struct->app_id, code, (Engine::uint8_t*) data_struct);
    debugMessage("SUPL_Control_Stop was called!");

    return 1;
}





