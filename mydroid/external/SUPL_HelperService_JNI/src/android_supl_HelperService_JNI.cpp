/*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName         :   android_supl_HelperService.cpp
 *
 * Description      :   JNI Interface for communicating with Helper Service.
                        (android.supl.jar and services.jar.
 *
 * Author           :   Praneet Kumar A
 * Date             :   15th June 2009
 *
 ******************************************************************************
 */


#include "android_supl_HelperService_JNI.h"
#include "mcpf_nal_common.h"

#include "JNIHelp.h"
#include "android_runtime/AndroidRuntime.h"

/* For Socket Programming. */
#include <stdio.h>
#include <sys/ioctl.h>
#include <string.h>
#include <ctype.h>
#include <unistd.h>
#include <stdlib.h>
#include <sys/wait.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <netdb.h>
#include <netinet/in.h>

#include "mcpf_nal_common.h"
#include "mcp_hal_misc.h"
#include "mcpf_services.h"
#include "gpsc_types.h"
#include "navc_api.h"
#include "gpsc_data.h"

#include "hostSocket.h"
#include "navl_api.h"

#include "suplc_api.h"

#include <utils/Log.h>

typedef struct
{
    char bytes[128];
    int size;
    int encoding;
} NotificationBytes_t;


#define ENABLE_HS_JNI_DEBUG
#ifdef ENABLE_HS_JNI_DEBUG
#include <utils/Log.h>
#define LOG_TAG "hs_jni"
#define  DEBUG_HS_JNI(...)   LOGD(__VA_ARGS__)
#define  ERROR_HS_JNI(...)   LOGE(__VA_ARGS__)
#else
#define  DEBUG_HS_JNI(...)   ((void)0)
#define  ERROR_HS_JNI(...)   ((void)0)
#endif /* ENABLE_HS_JNI_DEBUG */

#define HS_PORT_NUM         4000


/*TI_PATCH start*/
#define SOC_NAME_4121		  "/data/gps/gps4121"
#define SOC_NAME_4000             "\0/org/gps4000"

/*TI_PATCH start*/



#define MAX_BUFER_LENGTH   (1 * 1024)

#define MAX_CONNECTIONS     100
#define FOREVER             1

#define MCPF_SOCKET_PORT        4121
#define MCPF_HOST_NAME          "localhost"




//#define PRINT_MSG

typedef enum ret
{
    HSJNI_CONNECTION_ACTIVE = 1,
    HSJNI_CONNECTION_NOT_ACTIVE = 0,

    HSJNI_SUCCESS = 0,
    HSJNI_ERROR = -1,

}eHSJNI_Result;

int g_MsgBoxRes[2];
long g_s32_inSocketId;

McpU8 conRes=0;



/* Static Functions. */
static jclass AttachFindClass (JNIEnv **p_env, const char *JavaClass);
static int DetachCurrentThread ();

static eHSJNI_Result createSocketServer(const McpU16 u16_inPortNum);
static eHSJNI_Result readClientData(const McpS32 s32_inSocketId);
static eHSJNI_Result processClientMessage(const McpS8 *const p_inClientData,
        const McpS32 s32_inSocketId);
static eHSJNI_Result processSubscribeToMessage(int allow, int sms_wap);
static eHSJNI_Result processTlsInitRequest();
static eHSJNI_Result processTlsIsActiveRequest();
static eHSJNI_Result processTlsConnectRequest(void *p_inNalPayload);
static eHSJNI_Result processTlsFreeConnectionRequest();
static eHSJNI_Result processTlsSendRequest(void *p_inNalPayload);
static eHSJNI_Result processTlsStartReceiveRequest();

static int processGetSubscriberIdRequest(char *p_imsiBuffer);
static int processGetMSISDNRequest(char * p_msisdnBuffer);
static int processGetHSLPRequest(int *p_hslpBuffer);


static int processGetCellInfoRequest(int *p_cellInfo);
static int processGetNeighbourCellInfoRequest(int *p_NMRInfo);

static eHSJNI_Result sendResponseToNal(const McpS32 s32_inSocketId,
                                       const eHSJNI_Result *e_result );
static eHSJNI_Result sendResponseToNal(const McpS32 s32_inSocketId,
                                       const void *const p_buffer,
                                       const int s32_inSize);

static eHSJNI_Result processMessageBox(int verification,
                                       NotificationBytes_t *strings,
                                       int size,
                                       int Id,
                                       int timeout);

/**
 * Function:        connectWithMcpfSocket
 * Brief:           Creates a socket and connects with it.
 * Description:     A socket is exposed by MCPF for external applications to communicate
                    with it. GPS_AL writes the requests on this socket.
 * Note:            Internal function.
 * Params:          u16_inPortNumber - Port Number.
                    p_inHostName - Host Name.
 * Return:          Success: HSJNI_SUCCESS.
                    Failure: HSJNI_FAILURE_XXX.
 */
static eHSJNI_Result connectWithMcpfSocket(const McpU16 u16_inPortNumber,
                                           const McpU8 *const p_inHostName,
                                           McpS16 *const p_sockDescriptor);

/**
 * Function:        sendRequestToMcpf
 * Brief:           Sends requests to MCPF.
 * Description:
 * Note:            Internal function.
 * Params:          e_reqCmd - Commands to be sent.
                    p_inData - Payload.
 * Return:          Success: HSJNI_SUCCESS.
                    Failure: HSJNI_FAILURE_XXX.
 */
static eHSJNI_Result sendRequestToMcpf(const McpU16 e_reqCmd,
                                       const void *const p_inData,
                                       McpU32 u32_msgSize);
static void closeConnectionWithMcpf(const McpS16 *const p_socketId,
                                    eHSJNI_Result *const p_connectionWithMcpf);

/**
 * Function:        allocateMemory
 * Brief:           Allocates Memory.
 * Description:
 * Note:            Internal function.
 * Params:          u32_sizeInBytes - Number of bytes requested.
 * Return:          Address of Memory Chunck.
 */
static void * allocateMemory(const McpU32 u32_sizeInBytes);


/**
 * Function:        freeMemory
 * Brief:           Frees Memory.
 * Description:
 * Note:            Internal function.
 * Params:          p_memoryChunk: Memory address to be freed.
 * Return:
 */
static void freeMemory(void **p_memoryChunk);

/**
 * Function:        initializeMemory
 * Brief:           Initialize the memory.
 * Description:
 * Note:            Internal function.
 * Params:          p_memoryLocation - Memory Location.
                    u32_inMemSize - Size.
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static eHSJNI_Result initializeMemory(void *const p_memoryLocation, McpU32 u32_inMemSize);



static JavaVM * g_JVMSuplLibrary = NULL;
static const char     *g_CNetClass  = "android/supl/CNet";
static const char     *g_CSUPL_LP_Class = "android/supl/CSUPL_LP";

/* CNet */
JNIEnv *g_env_CNetClass = NULL;
jclass g_class_CNet = NULL;

/* CSUPL_LP */
JNIEnv *g_env_CSUPL_LP_Class = NULL;
jclass g_class_CSUPL_LP = NULL;

/* CNet Method IDs */
static jmethodID method_Init;
static jmethodID method_IsActive;
static jmethodID method_CreateConnection;
static jmethodID method_FreeConnection;
static jmethodID method_Send;
static jmethodID method_Receive;

/* CSUPL_LP Method IDs */
static jmethodID method_showSUPLInitDialog;
static jmethodID method_messageListen;
static jmethodID method_getSubscriberId;
static jmethodID method_getMSISDN;
static jmethodID method_getGSMInfo;

/**
 * Function:        JNI_OnLoad
 * Brief:
 * Description:     This functions is called whenever this library is loaded.
                    It initializes the native functions.
 * Note:            External Function.
 * Params:
 * Return:
 */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
{
    JNIEnv *env;
    g_JVMSuplLibrary = vm;

    LOGD("android_supl_HelperService_JNI.cpp: JNI_OnLoad: ##########");

    DEBUG_HS_JNI(" JNI_OnLoad: Entering \n");

    /* Get the environment. */
    if (vm->GetEnv((void**) &env, JNI_VERSION_1_2) != JNI_OK)
    {
        DEBUG_HS_JNI(" JNI_OnLoad: Failed to get the environment using GetEnv() \n");
        return -1;
    }

    /* Register native functions. */
    JNINativeMethod methods[] =
    {
        {
            "nativeStartSocketServer",
            "()Z",
            (void *)Java_startSocketServer
        }
    };

    if ( JNI_OK != android::AndroidRuntime::registerNativeMethods(env,
            "com/android/server/SUPLServer",
            methods,
            NELEM(methods)) )
    {
        DEBUG_HS_JNI(" JNI_OnLoad: 1. Failed to register native methods");
        LOGD(" JNI_OnLoad: 1. Failed to register native methods");
        return -1;
    }


    JNINativeMethod methods_CNet[] =
    {
        {
            "PostSLPMessageToQueue",
            "([B)Z",
            (void *)Java_PostSLPMessageToQueue
        },
        {
            "ConnectionRespose",
            "(B)Z",
            (void *)Java_ConnectionResponse
        }
    };

    if ( JNI_OK != android::AndroidRuntime::registerNativeMethods(env,
            "android/supl/CNetTLSProviderBase",
            methods_CNet,
            NELEM(methods_CNet)) )
    {
        DEBUG_HS_JNI(" JNI_OnLoad: 2. Failed to register native methods");
        return -1;
    }

    JNINativeMethod methods_CSUPL[] =
    {
        {
            "PostBodyToQueue",
            "([B)Z",
            (void *)Java_PostSLPMessageToQueue
        },
        {
            "NotificationResult",
            "([I)Z",
            (void *)Java_PostRespToQueue
        }

    };

    if ( JNI_OK != android::AndroidRuntime::registerNativeMethods(env,
            "android/supl/CSUPL_LP",
            methods_CSUPL,
            NELEM(methods_CSUPL)) )
    {
        DEBUG_HS_JNI(" JNI_OnLoad: 3. Failed to register native methods");
        return -1;
    }

    return JNI_VERSION_1_2;
}



/**
 * Function:        Java_startSocketServer
 * Brief:
 * Description:
 * Note:            External Function.
 * Params:
 * Return:
 */
JNIEXPORT jboolean JNICALL Java_startSocketServer(JNIEnv *, jobject)
{
    eHSJNI_Result retVal = HSJNI_SUCCESS;

    DEBUG_HS_JNI(" Java_startSocketServer: Entering \n");

    retVal = createSocketServer(HS_PORT_NUM);
    if (HSJNI_SUCCESS != retVal)
    {
        DEBUG_HS_JNI(" Java_startSocketServer: createSocketServer FAILED !!! \n");
        return JNI_FALSE;
    }


    DEBUG_HS_JNI(" Java_startSocketServer: Exiting successfully. \n");
    return JNI_TRUE;
}

TSUPLC_NetworkMsg s_networkMsg;

JNIEXPORT jboolean JNICALL Java_PostSLPMessageToQueue(JNIEnv *env, jobject obj, jbyteArray ByteArray)
{
    jsize arrSize = env->GetArrayLength(ByteArray);
    jbyte *bytes = env->GetByteArrayElements(ByteArray, NULL);

    McpU8 *p_data = (McpU8 *)bytes;
    McpU16 count = 0;
    //static McpU8 u8_mcpfConnectionEstablished = 0;

    //TSUPLC_NetworkMsg *p_networkMsg = NULL;
    eHSJNI_Result retVal = HSJNI_SUCCESS;

    LOGD("Java_PostSLPMessageToQueue: ##########");

    retVal = initializeMemory((void *)&s_networkMsg, sizeof(TSUPLC_NetworkMsg));
    if (HSJNI_SUCCESS != retVal)
    {
        env->ReleaseByteArrayElements(ByteArray,bytes,0);
        DEBUG_HS_JNI(" Java_PostSLPMessageToQueue: Memory initialization FAILED !!! \n");
        return retVal;
    }
    DEBUG_HS_JNI(" Java_PostSLPMessageToQueue: Entering \n");

    DEBUG_HS_JNI(" Java_PostSLPMessageToQueue: arrSize = %d \n", arrSize);
#ifdef PRINT_MSG
    for (count = 0; count < arrSize; count++)
    {
        DEBUG_HS_JNI(" Java_PostSLPMessageToQueue: p_data[%d] is %x \n", count, p_data[count]);
    }
#endif

    memcpy((void *)s_networkMsg.a_message, (void *)p_data, arrSize);
    s_networkMsg.u16_msgSize = arrSize;
    retVal = sendRequestToMcpf(SUPLC_CMD_FROM_NETWORK,
            (void *)&s_networkMsg,
                               sizeof(TSUPLC_NetworkMsg) );

    env->ReleaseByteArrayElements(ByteArray, bytes, 0);

	if(obj)
	  env->DeleteLocalRef(obj);
    
    if (HSJNI_SUCCESS != retVal)
    {
        DEBUG_HS_JNI(" Java_PostSLPMessageToQueue: Send request to MCPF FAILED !!! \n");
        return JNI_FALSE;
    }

    DEBUG_HS_JNI(" Java_PostSLPMessageToQueue: Exiting Successfully \n");
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_ConnectionResponse(JNIEnv *env, jobject obj, jbyte bRes)
{

    eHSJNI_Result retVal = HSJNI_SUCCESS;

    LOGD("Java_ConnectionResponse: ##########");

    DEBUG_HS_JNI(" Java_ConnectionResponse: Entering \n");

	conRes=(McpU8)(bRes);
	LOGD("Java_ConnectionResponse: Con Res=[%d]", conRes);

    retVal = sendRequestToMcpf(SUPLC_CMD_SERVER_CONNECT,
                               (void *)&conRes,
                               sizeof(McpU8) );
    if(obj)
      env->DeleteLocalRef(obj);
    if (HSJNI_SUCCESS != retVal)
    {
        DEBUG_HS_JNI(" Java_ConnectionResponse: Send request to MCPF FAILED !!! \n");
        return JNI_FALSE;
    }

    DEBUG_HS_JNI(" Java_ConnectionResponse: Exiting Successfully \n");
    return JNI_TRUE;
}



JNIEXPORT jboolean JNICALL Java_PostRespToQueue(JNIEnv *env, jobject obj, jintArray IntArray)
{
    jsize arrSize = env->GetArrayLength(IntArray);
    jint *ints = env->GetIntArrayElements(IntArray, NULL);

    McpU32 *p_data = (McpU32 *)ints;

    s_NAL_msgBoxResp s_respMsg;

    eHSJNI_Result retVal = HSJNI_SUCCESS;

    LOGD("Java_PostRespToQueue: ##########");

    retVal = initializeMemory((void *)&s_respMsg, sizeof(s_NAL_msgBoxResp));
    if (HSJNI_SUCCESS != retVal)
    {
        DEBUG_HS_JNI(" Java_PostRespToQueue: Memory initialization FAILED !!! \n");
        return retVal;
    }

#ifdef PRINT_MSG
    LOGD("Java_PostRespToQueue: arrSize=[%d]", arrSize);
    LOGD("Java_PostRespToQueue: ints[0]=[%d]", ints[0]);
    LOGD("Java_PostRespToQueue: ints[1]=[%d]", ints[1]);	

    LOGD("Java_PostRespToQueue: p_data[0]=[%d]", p_data[0]);
    LOGD("Java_PostRespToQueue: p_data[1]=[%d]", p_data[1]);	
#endif /* PRINT_MSG */

    memcpy((void *)&s_respMsg, (void *)p_data, sizeof(s_NAL_msgBoxResp));
#ifdef PRINT_MSG
    LOGD("Java_PostRespToQueue: s_respMsg[0]=[%d]", s_respMsg[0]);
    LOGD("Java_PostRespToQueue: s_respMsg[1]=[%d]", s_respMsg[1]);
#endif /* PRINT_MSG */
    retVal = sendRequestToMcpf(SUPLC_CMD_MSGBOX_RESP,
                               (void *)&s_respMsg,
                               sizeof(s_NAL_msgBoxResp) );

	env->ReleaseIntArrayElements(IntArray, ints, 0);
    if(obj)
		env->DeleteLocalRef(obj);
		
    if (HSJNI_SUCCESS != retVal)
    {
        DEBUG_HS_JNI(" Java_PostRespToQueue: Send request to MCPF FAILED !!! \n");
        return JNI_FALSE;
    }

    DEBUG_HS_JNI(" Java_PostRespToQueue: Exiting Successfully \n");
    return JNI_TRUE;
}


/*
JNIEXPORT jboolean JNICALL Java_NotificationResult(JNIEnv *env, jobject obj, jint res, jint tid)
{
	LOGD("+Java_NotificationResult");
	McpU32 data[2];
	data[0]=(McpU32)res;
	data[1]=(McpU32)tid;
	sendResponseToNal((const McpS32) g_s32_inSocketId,
						(const void *const) &data,
						(const int) sizeof(data));

	LOGD("-Java_NotificationResult");
	return HSJNI_SUCCESS;
}
*/

static eHSJNI_Result connectionWithMcpf = HSJNI_CONNECTION_NOT_ACTIVE;
static McpS16 g_mcpfsocketId = 0;


static eHSJNI_Result createSocketServer(const McpU16 u16_inPortNum)
{

    /* TI_PATCH start */
	//  struct sockaddr_in serverAddress;
	    struct sockaddr_un serverAddress;
	/* TI_PATCH end */
    struct sockaddr_in clientAddress;

    socklen_t clientAddressLength;      /* Length of client address. */

    /* TI_PATCH start */
	    int tmp_errno;
	    int iret;
	/* TI_PATCH end */


    McpS32 s32_originalSocketId = 0;
    McpS32 s32_newSocketId = 0;
    McpS32 s32_bytesRead = 0;

    eHSJNI_Result retVal = HSJNI_SUCCESS;

    DEBUG_HS_JNI(" createSocketServer: Entering \n");

    /* Clear the structure. */
    memset( &serverAddress, 0, sizeof(serverAddress) );

    /* TI_PATCH start */
	#if 0
    serverAddress.sin_family = AF_INET;
    serverAddress.sin_addr.s_addr = htonl(INADDR_ANY); /* Any interface */
    serverAddress.sin_port = htons(u16_inPortNum);
	#else
	    serverAddress.sun_family = AF_UNIX;
	    strcpy(serverAddress.sun_path, SOC_NAME_4000);
	#endif
	/* TI_PATCH end */


    /* Create a new socket. */
	  /* TI_PATCH start */
	  //  s32_originalSocketId = socket(AF_INET, SOCK_STREAM, 0);
      s32_originalSocketId = socket(AF_UNIX, SOCK_STREAM, 0);
	/* TI_PATCH end */

    if (s32_originalSocketId < 0)
    {
        DEBUG_HS_JNI(" createSocketServer: Socket creation FAILED !!!");
        return HSJNI_ERROR;
    }


	/* TI_PATCH start */
	    /* unlink. */
	    DEBUG_HS_JNI(" createSocketServer: unlink !!! \n");

	    iret = unlink(SOC_NAME_4000);
	    tmp_errno = errno;

	    if( ( iret != 0 ) && ( tmp_errno != ENOENT ) )
	    {
	        DEBUG_HS_JNI(" createSocketServer: unlink FAILED !!! \n");
	        close(s32_originalSocketId);
	        return HSJNI_ERROR;
	    }
	/* TI_PATCH end */


    /* Bind. */
    if ( bind( s32_originalSocketId, (struct sockaddr *) &serverAddress,
               sizeof(serverAddress)) < 0 )
    {
        DEBUG_HS_JNI(" createSocketServer: bind FAILED !!! \n");
        close(s32_originalSocketId);
        return HSJNI_ERROR;
    }

    /* Listen. */
    if ( listen( s32_originalSocketId, MAX_CONNECTIONS) < 0 )
    {
        DEBUG_HS_JNI("createSocketServer: listen FAILED !!! \n");
        close (s32_originalSocketId);
        return HSJNI_ERROR;
    }

    /* Accept a connection. */
    do
    {
		DEBUG_HS_JNI(" createSocketServer: Waiting for Client Socket to Connect\n");
		
        clientAddressLength = sizeof(clientAddress);
        s32_newSocketId = accept( s32_originalSocketId,
                                  (struct sockaddr *) &clientAddress,
                                  &clientAddressLength);

		DEBUG_HS_JNI(" createSocketServer: Connected Client Socket  %d \n", s32_newSocketId);
		
        if ( s32_newSocketId < 0)
        {
            DEBUG_HS_JNI(" createSocketServer: accept FAILED !!! \n");
            close(s32_originalSocketId);
            continue;
        }

	    do
	    {        /* Read data. */
	        retVal = readClientData(s32_newSocketId);
	        DEBUG_HS_JNI(" createSocketServer: Read Return  %d \n", retVal);
	    }
	    while (HSJNI_ERROR != retVal);
		//Temporary - When Supl socket disconnects, Disconnect with Mcpf socket also and Reconnect on Next session
		closeConnectionWithMcpf(&g_mcpfsocketId, &connectionWithMcpf);
		
	}while (FOREVER);

    DEBUG_HS_JNI(" createSocketServer: Exiting Successfully \n");
    return HSJNI_SUCCESS;
}


static eHSJNI_Result readClientData(const McpS32 s32_inSocketId)
{
    McpS8 a_readBuffer[MAX_BUFER_LENGTH] = {'\0'};
    McpS32 s32_bytesRead = 0;

    eHSJNI_Result retVal = HSJNI_SUCCESS;

    DEBUG_HS_JNI(" readClientData: Entering \n");

    s32_bytesRead = recv(s32_inSocketId, a_readBuffer, sizeof(a_readBuffer),0);
    if (s32_bytesRead < 0)
    {
        DEBUG_HS_JNI(" readClientData: read FAILED !!! \n");
        return HSJNI_ERROR;
    }

    retVal = processClientMessage(a_readBuffer, s32_inSocketId);
    if (HSJNI_ERROR == retVal)
    {
        DEBUG_HS_JNI(" readClientData: processClientMessage FAILED !!! \n");
        return HSJNI_ERROR;
    }

    DEBUG_HS_JNI(" readClientData: Exiting Successfully. \n");
    return retVal;
}

static eHSJNI_Result processClientMessage(const McpS8 *const p_inClientData,
        const McpS32 s32_inSocketId)
{
    s_NAL_CmdParams *p_nalCmdParams = NULL;
    eHSJNI_Result retVal = HSJNI_SUCCESS;

    const int START=1;
    const int STOP=0;
    const int SMS=1;
    const int WAP=2;

    LOGD("+processClientMessage ");
    DEBUG_HS_JNI(" processClientMessage: Entering \n");

    g_s32_inSocketId = 0;
    p_nalCmdParams = (s_NAL_CmdParams *)p_inClientData;
    /* Just in case... */
    if (NULL == p_nalCmdParams)
    {
        LOGD("processClientMessage: p_nalCmdParams == NULL  ");
        DEBUG_HS_JNI(" processClientMessage: NULL Pointer. \n");
        return HSJNI_ERROR;
    }

    if (NULL == g_class_CNet)
    {
        g_class_CNet = AttachFindClass (&g_env_CNetClass, g_CNetClass);
        if (NULL == g_class_CNet)
        {
            DEBUG_HS_JNI(" processClientMessage: Failed to attach to CNetclass !!!");
            LOGD(" processClientMessage: Failed to attach to CNetclass !!!");
            return HSJNI_ERROR;
        }

        method_Init = g_env_CNetClass->GetStaticMethodID(g_class_CNet, "Init", "()I");
        method_Send = g_env_CNetClass->GetStaticMethodID(g_class_CNet, "Send", "([B)I");
        method_Receive = g_env_CNetClass->GetStaticMethodID(g_class_CNet, "Receive", "()I");
        method_IsActive = g_env_CNetClass->GetStaticMethodID(g_class_CNet, "IsActive", "()I");
        method_CreateConnection = g_env_CNetClass->GetStaticMethodID(g_class_CNet,
                                                                     "CreateConnection",
                                                                     "(Ljava/lang/String;)I");

        method_FreeConnection = g_env_CNetClass->GetStaticMethodID(g_class_CNet,
                                                                   "FreeConnection",
                                                                   "()V");
    }

    /* Attach to CSUPL_LP class. */
    if (NULL == g_class_CSUPL_LP)
    {
        g_class_CSUPL_LP = AttachFindClass (&g_env_CSUPL_LP_Class, g_CSUPL_LP_Class);
        if (NULL == g_class_CSUPL_LP)
        {
            DEBUG_HS_JNI(" processClientMessage: Failed to attach to CSUPL_LP !!!");
            LOGD(" processClientMessage: Failed to attach to CSUPL_LP !!!");
            return HSJNI_ERROR;
        }

        method_showSUPLInitDialog = g_env_CSUPL_LP_Class->GetStaticMethodID(g_class_CSUPL_LP,
                                                                            "showSUPLInitDialog",
                                                                            "(Z[BI[BIII)V");

        method_messageListen = g_env_CSUPL_LP_Class->GetStaticMethodID(g_class_CSUPL_LP,
                                                                       "messageListen",
                                                                       "(ZI)V");

        method_getSubscriberId = g_env_CSUPL_LP_Class->GetStaticMethodID(g_class_CSUPL_LP,
                                                                       "getSubscriberId",
                                                                       "()[B");


        method_getMSISDN = g_env_CSUPL_LP_Class->GetStaticMethodID(g_class_CSUPL_LP,
                                                                   "getMSISDN",
                                                                   "()[B");

        method_getGSMInfo = g_env_CSUPL_LP_Class->GetStaticMethodID(g_class_CSUPL_LP,
                                                                    "getGSMInfo",
                                                                    "()[I");
    }
    switch (p_nalCmdParams->e_nalCommand)
    {
    case MCPF_NAL_START_SMS_LISTEN:
    {
        LOGD("processClientMessage: MCPF_NAL_START_SMS_LISTEN");
        DEBUG_HS_JNI(" processClientMessage: Received MCPF_NAL_START_SMS_LISTEN \n");
        retVal = processSubscribeToMessage(START, SMS);
        if (HSJNI_ERROR == retVal)
        {
            DEBUG_HS_JNI(" processClientMessage: processSubscribeToMessage FAILED !!! \n");
        }
    }
    break;

    case MCPF_NAL_STOP_SMS_LISTEN:
    {
        LOGD("processClientMessage: MCPF_NAL_STOP_SMS_LISTEN");
        DEBUG_HS_JNI(" processClientMessage: Received MCPF_NAL_STOP_SMS_LISTEN \n");
        retVal = processSubscribeToMessage(STOP, SMS);
        if (HSJNI_ERROR == retVal)
        {
            DEBUG_HS_JNI(" processClientMessage: processSubscribeToMessage FAILED !!! \n");
        }
    }
    break;

    case MCPF_NAL_START_WAP_LISTEN:
    {
        LOGD("processClientMessage: MCPF_NAL_START_WAP_LISTEN");
        DEBUG_HS_JNI(" processClientMessage: Received MCPF_NAL_START_WAP_LISTEN \n");
        retVal = processSubscribeToMessage(START, WAP);
        if (HSJNI_ERROR == retVal)
        {
            DEBUG_HS_JNI(" processClientMessage: processSubscribeToMessage FAILED !!! \n");
        }
    }
    break;

    case MCPF_NAL_STOP_WAP_LISTEN:
    {
        LOGD("processClientMessage: MCPF_NAL_STOP_WAP_LISTEN");
        DEBUG_HS_JNI(" processClientMessage: Received MCPF_NAL_STOP_WAP_LISTEN \n");
        retVal = processSubscribeToMessage(STOP, WAP);
        if (HSJNI_ERROR == retVal)
        {
            DEBUG_HS_JNI(" processClientMessage: processSubscribeToMessage FAILED !!! \n");
        }
    }
    break;

    case MCPF_NAL_MSGBOX:
    {
        LOGD("processClientMessage: MCPF_NAL_MSG_BOX");
        g_s32_inSocketId = s32_inSocketId;

    #ifdef PRINT_MSG
        LOGD("MCPF_NAL_MSGBOX:verification=%d", (int)p_nalCmdParams->u_payload.s_msgBox.verification);
        LOGD("MCPF_NAL_MSGBOX:requestorId[0]=[%s]", p_nalCmdParams->u_payload.s_msgBox.strings[0].bytes);
        LOGD("MCPF_NAL_MSGBOX:requestorId[0]=s[%d]", p_nalCmdParams->u_payload.s_msgBox.strings[0].size);
        LOGD("MCPF_NAL_MSGBOX:requestorId[0]=e[%d]", p_nalCmdParams->u_payload.s_msgBox.strings[0].encoding);
        LOGD("MCPF_NAL_MSGBOX:requestorId[1]=[%s]", p_nalCmdParams->u_payload.s_msgBox.strings[1].bytes);
        LOGD("MCPF_NAL_MSGBOX:requestorId[1]=s[%d]", p_nalCmdParams->u_payload.s_msgBox.strings[1].size);
        LOGD("MCPF_NAL_MSGBOX:requestorId[1]=e[%d]", p_nalCmdParams->u_payload.s_msgBox.strings[1].encoding);

        LOGD("MCPF_NAL_MSGBOX:size=%d", (int)p_nalCmdParams->u_payload.s_msgBox.size);
        LOGD("MCPF_NAL_MSGBOX:sessionId=%d", (int)p_nalCmdParams->u_payload.s_msgBox.Id);
        LOGD("MCPF_NAL_MSGBOX:timeout=%d", (int)p_nalCmdParams->u_payload.s_msgBox.timeout);
    #endif /* PRINT_MSG */

        DEBUG_HS_JNI(" processClientMessage: Received MCPF_NAL_MSG_BOX \n");
        retVal = processMessageBox((int)p_nalCmdParams->u_payload.s_msgBox.verification,
                                   (NotificationBytes_t *)&p_nalCmdParams->u_payload.s_msgBox.strings,
                                   (int)p_nalCmdParams->u_payload.s_msgBox.size,
                                   (int)p_nalCmdParams->u_payload.s_msgBox.Id,
                                   (int)p_nalCmdParams->u_payload.s_msgBox.timeout);
        if (HSJNI_ERROR == retVal)
        {
            DEBUG_HS_JNI(" processClientMessage: processSubscribeToMessage FAILED !!! \n");
        }
    }
    break;

    case MCPF_NAL_TLS_INIT:
    {
        DEBUG_HS_JNI(" processClientMessage: Received MCPF_NAL_TLS_INIT \n");
        retVal = processTlsInitRequest();
        if (HSJNI_SUCCESS != retVal)
        {
            DEBUG_HS_JNI(" processClientMessage: processTlsInitRequest FAILED !!! \n");
        }
    }
    break;

    case MCPF_NAL_TLS_IS_ACTIVE:
    {
        DEBUG_HS_JNI(" processClientMessage: Received MCPF_NAL_TLS_IS_ACTIVE \n");
        retVal = processTlsIsActiveRequest();
        if (HSJNI_ERROR == retVal)
        {
            DEBUG_HS_JNI(" processClientMessage: processTlsIsActiveRequest FAILED !!! \n");
        }

    }
    break;

    case MCPF_NAL_TLS_CREATE_CONNECTION:
    {
        DEBUG_HS_JNI(" processClientMessage: Received MCPF_NAL_TLS_CREATE_CONNECTION \n");
        retVal = processTlsConnectRequest((void *)&p_nalCmdParams->u_payload);
        if (HSJNI_SUCCESS != retVal)
        {
            DEBUG_HS_JNI(" processClientMessage: processTlsConnectRequest FAILED !!! \n");
        }

    }
    break;

    case MCPF_NAL_TLS_FREE_CONNECTION:
    {
        DEBUG_HS_JNI(" processClientMessage: Received MCPF_NAL_TLS_FREE_CONNECTION \n");
        retVal = processTlsFreeConnectionRequest();
        if (HSJNI_SUCCESS != retVal)
        {
            DEBUG_HS_JNI(" processClientMessage: processTlsFreeConnectionRequest FAILED !!! \n");
        }
    }
    break;

    case MCPF_NAL_TLS_SEND:
    {
        DEBUG_HS_JNI(" processClientMessage: Received MCPF_NAL_TLS_SEND \n");
        retVal = processTlsSendRequest((void *)&p_nalCmdParams->u_payload);
        if (HSJNI_SUCCESS != retVal)
        {
            DEBUG_HS_JNI(" processClientMessage: processTlsSendRequest FAILED !!! \n");
        }
    }
    break;

    case MCPF_NAL_TLS_START_RECEIVE:
    {
        DEBUG_HS_JNI(" processClientMessage: Received MCPF_NAL_TLS_START_RECEIVE \n");
        retVal = processTlsStartReceiveRequest();
        if (HSJNI_SUCCESS != retVal)
        {
            DEBUG_HS_JNI(" processClientMessage: processTlsStartReceiveRequest FAILED !!! \n");
        }
    }
    break;

    case MCPF_NAL_GET_SUBSCRIBER_ID:
    {
        s_NAL_subscriberId s_subscriberId;
        int s32_size = 0;

        DEBUG_HS_JNI(" processClientMessage: Received MCPF_NAL_GET_SUBSCRIBER_ID \n");

        retVal = initializeMemory((void *)&s_subscriberId, sizeof(s_NAL_subscriberId));
        if (HSJNI_SUCCESS != retVal)
        {
            DEBUG_HS_JNI(" processClientMessage: Memory initialization FAILED !!! \n");
            return retVal;
        }

        s32_size = processGetSubscriberIdRequest(s_subscriberId.a_imsiData);
        if (s32_size < 0)
        {
            DEBUG_HS_JNI(" processClientMessage: processGetSubscriberIdRequest FAILED !!! \n");
            retVal = HSJNI_ERROR;

            /* APK: Temporary Hack. */
            retVal = sendResponseToNal(s32_inSocketId, &retVal);
            if (HSJNI_SUCCESS != retVal)
            {
                DEBUG_HS_JNI(" createSocketServer: sendResponseToNal FAILED !!! \n");
                return retVal;
            }

            break;
        }

        s_subscriberId.s32_size = s32_size;

        DEBUG_HS_JNI(" processClientMessage: s32_size = %d \n", s_subscriberId.s32_size);
        DEBUG_HS_JNI(" processClientMessage: a_imsiData: %x \n", s_subscriberId.a_imsiData );

#ifdef PRINT_MSG
        for (int count = 0; count < s32_size; count++)
            DEBUG_HS_JNI(" === a_imsiData: %x ==== \n", s_subscriberId.a_imsiData[count]);
#endif

        retVal = sendResponseToNal(s32_inSocketId, (void *)&s_subscriberId, sizeof(s_NAL_subscriberId) );
        if (retVal != HSJNI_SUCCESS)
        {
            DEBUG_HS_JNI(" processClientMessage: sendResponseToNal FAILED !!! \n");
        }
    }
    break;

	case MCPF_NAL_GET_MSISDN:
	{
		s_NAL_msisdn s_msisdn;
		int s32_size = 0;
		DEBUG_HS_JNI(" processClientMessage: Received MCPF_NAL_GET_MSISDN \n");
        retVal = initializeMemory((void *)&s_msisdn, sizeof(s_NAL_msisdn));
		if (HSJNI_SUCCESS != retVal)
		{
			DEBUG_HS_JNI(" processClientMessage: Memory initialization FAILED !!! \n");
			return retVal;
		}
		s32_size = processGetMSISDNRequest(s_msisdn.a_msisdnData);
		if (s32_size < 0)
		{
			DEBUG_HS_JNI(" processClientMessage: processGetMSISDNRequest FAILED !!! \n");
			retVal = HSJNI_ERROR;
			retVal = sendResponseToNal(s32_inSocketId, &retVal);
			if (HSJNI_SUCCESS != retVal)
			{
				DEBUG_HS_JNI(" createSocketServer: sendResponseToNal FAILED !!! \n");
				return retVal;
			}
			break;
		}
		s_msisdn.s32_size = s32_size;
        DEBUG_HS_JNI(" processClientMessage: s32_size = %d \n", s_msisdn.s32_size);
        DEBUG_HS_JNI(" processClientMessage: a_msisdnData: %x \n", s_msisdn.a_msisdnData);
				for (int count = 0; count < s32_size; count++)
					DEBUG_HS_JNI(" === a_msisdnData: %x ==== \n", s_msisdn.a_msisdnData[count]);
		retVal = sendResponseToNal(s32_inSocketId, (void *)&s_msisdn, sizeof(s_NAL_msisdn) );
        if (retVal != HSJNI_SUCCESS)
        {
            DEBUG_HS_JNI(" processClientMessage: sendResponseToNal FAILED !!! \n");
        }
	}
	break;
    case MCPF_NAL_GET_CELL_INFO:
    {
        s_NAL_GsmCellInfo s_gsmCellInfo;
        int ret = 0;

        DEBUG_HS_JNI(" processClientMessage: Received MCPF_NAL_GET_CELL_INFO \n");

        retVal = initializeMemory((void *)&s_gsmCellInfo, sizeof(s_NAL_GsmCellInfo));
        if (HSJNI_SUCCESS != retVal)
        {
            DEBUG_HS_JNI(" processClientMessage: Memory initialization FAILED !!! \n");
            return retVal;
        }

        ret = processGetCellInfoRequest(s_gsmCellInfo.a_cellInfo);
        if (ret < 0)
        {
            DEBUG_HS_JNI(" processClientMessage: processGetSubscriberIdRequest FAILED !!! \n");
            retVal = HSJNI_ERROR;

            /* APK: Temporary Hack. */
            retVal = sendResponseToNal(s32_inSocketId, &retVal);
            if (HSJNI_SUCCESS != retVal)
            {
                DEBUG_HS_JNI(" processClientMessage: sendResponseToNal FAILED !!! \n");
                return retVal;
            }
			break;
         }

		ret = processGetNeighbourCellInfoRequest(s_gsmCellInfo.a_nmr);
        if (ret < 0)
        {
            DEBUG_HS_JNI(" processClientMessage: processGetCellInfoRequest FAILED !!! \n");
            retVal = HSJNI_ERROR;

            /* APK: Temporary Hack. */
            retVal = sendResponseToNal(s32_inSocketId, &retVal);
            if (HSJNI_SUCCESS != retVal)
            {
                DEBUG_HS_JNI(" processClientMessage: sendResponseToNal FAILED !!! \n");
                return retVal;
            }		
            break;
        }

        for (int count = 0; count < 5; count++)
            DEBUG_HS_JNI(" processClientMessage: a_cellInfo[%d]: %d \n", count,
                         s_gsmCellInfo.a_cellInfo[count]);

        retVal = sendResponseToNal(s32_inSocketId, (void *)&s_gsmCellInfo, sizeof(s_NAL_GsmCellInfo) );
        if (retVal != HSJNI_SUCCESS)
        {
            DEBUG_HS_JNI(" processClientMessage: sendResponseToNal FAILED !!! \n");
        }
    }
	break;
	
	case MCPF_NAL_GET_HSLP:
	{
		s_NAL_hslp s_hslp;
		int s32_size = 0;
		DEBUG_HS_JNI(" processClientMessage: Received MCPF_NAL_GET_HSLP. \n");
        retVal = initializeMemory((void *)&s_hslp, sizeof(s_NAL_hslp));
		if (HSJNI_SUCCESS != retVal)
		{
			DEBUG_HS_JNI(" processClientMessage: Memory initialization FAILED !!! \n");
			return retVal;
		}
		s32_size = processGetHSLPRequest(s_hslp.a_hslpData);
		if (s32_size < 0)
		{
			DEBUG_HS_JNI(" processClientMessage: processGetHSLPRequest FAILED !!! \n");
			retVal = HSJNI_ERROR;
			retVal = sendResponseToNal(s32_inSocketId, &retVal);
			if (HSJNI_SUCCESS != retVal)
			{
				DEBUG_HS_JNI(" createSocketServer: sendResponseToNal FAILED !!! \n");
				return retVal;
			}
			break;
		}
		s_hslp.s32_size = s32_size;
        DEBUG_HS_JNI(" processClientMessage: s32_size = %d \n", s_hslp.s32_size);
        
				for (int count = 0; count < s32_size; count++)
					DEBUG_HS_JNI(" === a_hslpData: %d ==== \n", s_hslp.a_hslpData[count]);
		retVal = sendResponseToNal(s32_inSocketId, (const void *const)&s_hslp, sizeof(s_NAL_hslp) );
        if (retVal != HSJNI_SUCCESS)
        {
            DEBUG_HS_JNI(" processClientMessage: sendResponseToNal FAILED !!! \n");
        }
	}
    
    break;

    default:
    {
        LOGD(" processClientMessage: Unknown CMD received");
        DEBUG_HS_JNI(" processClientMessage: No CMD received \n");
    }
    break;
    }


    /* For Msg box do not set any response now. Sent from Notify */
    /* Send response to NAL. */	
    if ( (MCPF_NAL_GET_SUBSCRIBER_ID != p_nalCmdParams->e_nalCommand) &&
            (MCPF_NAL_GET_CELL_INFO != p_nalCmdParams->e_nalCommand) &&
            (MCPF_NAL_GET_HSLP != p_nalCmdParams->e_nalCommand) )
    {
    	DEBUG_HS_JNI(" processClientMessage: sendResponseToNal Cmd = %d \n", p_nalCmdParams->e_nalCommand);
        retVal = sendResponseToNal(s32_inSocketId, &retVal);
        if (HSJNI_SUCCESS != retVal)
        {
            DEBUG_HS_JNI(" processClientMessage: sendResponseToNal FAILED !!! \n");
            return retVal;
        }
    }


    DEBUG_HS_JNI(" processClientMessage: Exiting Successfully. \n");
    return retVal;
}

static eHSJNI_Result processMessageBox(int verification,
                                       NotificationBytes_t *strings,
                                       int size,
                                       int Id,
                                       int timeout)
{
    LOGD("+processMessageBox");

    DEBUG_HS_JNI(" processMessageBox: Entering \n");

#ifdef PRINT_MSG
    LOGD("processMessageBox:verification=%d", verification);
    LOGD("processMessageBox:requestorId[0]=[%s]", strings[0].bytes);
    LOGD("processMessageBox:requestorId[0]=s[%d]", strings[0].size);
    LOGD("processMessageBox:requestorId[0]=e[%d]", strings[0].encoding);
    LOGD("processMessageBox:requestorId[1]=[%s]", strings[1].bytes);
    LOGD("processMessageBox:requestorId[1]=s[%d]", strings[1].size);
    LOGD("processMessageBox:requestorId[1]=e[%d]", strings[1].encoding);
    LOGD("processMessageBox:size=%d", size);
    LOGD("processMessageBox:sessionId=%d", Id);
    LOGD("processMessageBox:timeout=%d",timeout);

    LOGD("processMessageBox: check1");
#endif /* PRINT_MSG */

    if (g_class_CSUPL_LP != NULL)
    {
        jbyteArray requestorId = NULL;
        jbyteArray clientName = NULL;
        jboolean verif = JNI_TRUE;

        if (verification <= 0)
        {
            LOGD("processMessageBox: check2");
            verif = JNI_FALSE;
            LOGD("processMessageBox: verif=", verif);
        }

        if (size == 1 && strings != NULL)
        {
            LOGD("processMessageBox: check3");
            requestorId = g_env_CSUPL_LP_Class->NewByteArray(strings[0].size);
            if (requestorId)
            {
                g_env_CSUPL_LP_Class->SetByteArrayRegion(requestorId, 0, strings[0].size,
                                        (jbyte *) strings[0].bytes);
                LOGD("processMessageBox: requestorId=", requestorId);
            }
        }
        else if (size >= 2 && strings != NULL)
        {
            LOGD("processMessageBox: check4");
            requestorId = g_env_CSUPL_LP_Class->NewByteArray(strings[0].size);
            if (requestorId)
            {
                g_env_CSUPL_LP_Class->SetByteArrayRegion(requestorId, 0, strings[0].size,
                                        (jbyte *) strings[0].bytes);
                LOGD("processMessageBox: requestorId=", requestorId);
            }

            clientName = g_env_CSUPL_LP_Class->NewByteArray(strings[1].size);
            if (clientName)
            {
                g_env_CSUPL_LP_Class->SetByteArrayRegion(clientName, 0, strings[1].size,
                                        (jbyte *) strings[1].bytes);
                LOGD("processMessageBox: clientName=", clientName);
            }
        }

        LOGD("MessageBox: -->showSUPLInitDialog");
        if (method_showSUPLInitDialog)
        {
            LOGD("MessageBox: ->showSUPLInitDialog");
#ifdef PRINT_MSG
                  LOGD(".processMessageBox:verification=%d", verif);
                  LOGD(".processMessageBox:requestorId=%s", requestorId);
            	  LOGD(".processMessageBox:encoding=%d", strings[0].encoding);
            	  LOGD(".processMessageBox:clientName=%s", clientName);
                  LOGD(".processMessageBox:sessionId=%d", Id);
                  LOGD(".processMessageBox:timeout=%d",timeout);
#endif /* PRINT_MSG */
            g_env_CSUPL_LP_Class->CallStaticVoidMethod(g_class_CSUPL_LP, method_showSUPLInitDialog,
                                      verif,
                                      requestorId,
                                      strings[0].encoding,
                                      clientName,
                                      strings[1].encoding,
                                      Id,
                                      timeout);
        }

        if(clientName)  g_env_CSUPL_LP_Class->DeleteLocalRef(clientName);
        if(requestorId) g_env_CSUPL_LP_Class->DeleteLocalRef(requestorId);

        //DetachCurrentThread();
    }
	
    DEBUG_HS_JNI(" processMessageBox: Exiting Successfully. \n");
    LOGD("-processMessageBox");
    return HSJNI_SUCCESS;
}


static eHSJNI_Result processSubscribeToMessage(int allow, int sms_wap)
{
    LOGD("+processSubscribeToMessage");
    DEBUG_HS_JNI(" processSubscribeToMessage: Entering \n");

    if (g_class_CSUPL_LP != NULL)
    {
        if (method_messageListen)
        {
            jboolean allow_ = JNI_FALSE;
            if (allow > 0)
            {
                allow_ = JNI_TRUE;
            }

            g_env_CSUPL_LP_Class->CallStaticVoidMethod(g_class_CSUPL_LP, method_messageListen, allow_, sms_wap);
        }

        //DetachCurrentThread();
    }


    DEBUG_HS_JNI(" processSubscribeToMessage: Exiting Successfully. \n");
    LOGD("-processSubscribeToMessage");
    return HSJNI_SUCCESS;
}

static eHSJNI_Result processTlsInitRequest()
{
    jint ret = -1;

    DEBUG_HS_JNI(" processTlsInitRequest: Entering \n");

    if (g_class_CNet != NULL)
    {
        if (method_Init)
        {
            ret = g_env_CNetClass->CallStaticIntMethod(g_class_CNet, method_Init);
        }
        //DetachCurrentThread();
    }

    if (ret < 0)
    {
        DEBUG_HS_JNI(" processTlsInitRequest ERROR !!!");
        return HSJNI_ERROR;
    }

    DEBUG_HS_JNI(" processTlsInitRequest: Exiting Successfully. \n");
    return HSJNI_SUCCESS;
}


static eHSJNI_Result processTlsIsActiveRequest()
{
    jint ret = -1;

    DEBUG_HS_JNI(" processTlsIsActiveRequest: Entering \n");

    if (g_class_CNet != NULL)
    {
        if (method_IsActive)
        {
            ret = g_env_CNetClass->CallStaticIntMethod(g_class_CNet, method_IsActive);
        }
    }

    if (ret == 1)
    {
        DEBUG_HS_JNI(" processTlsInitRequest: Connection is active \n");
        return HSJNI_CONNECTION_ACTIVE;
    }

    if (ret == 0)
    {
        DEBUG_HS_JNI(" processTlsInitRequest: Connection is not active \n");
        return HSJNI_CONNECTION_NOT_ACTIVE;
    }

    DEBUG_HS_JNI(" processTlsInitRequest: ERROR!!! \n");
    return HSJNI_ERROR;
}

static eHSJNI_Result processTlsConnectRequest(void *p_inNalPayload)
{
    s_NAL_createConnection *p_connectPayload = NULL;
    jint ret = -1;

    char * p_hostPort = NULL;

    DEBUG_HS_JNI(" processTlsConnectRequest: Entering \n");

    p_connectPayload = (s_NAL_createConnection *)&((TNAL_cmdPaylaod *)p_inNalPayload)->s_createConnection;
    /* Just in case... */
    if (NULL == p_connectPayload)
    {
        DEBUG_HS_JNI(" processTlsConnectRequest: NULL Pointers \n");
        return HSJNI_ERROR;
    }


    DEBUG_HS_JNI(" processTlsConnectRequest: a_hostPort = %s \n", p_connectPayload->a_hostPort);

    if (g_class_CNet != NULL)
    {
        if (method_CreateConnection)
        {
            jstring host_port = g_env_CNetClass->NewStringUTF(p_connectPayload->a_hostPort);
            ret = g_env_CNetClass->CallStaticIntMethod(g_class_CNet, method_CreateConnection, host_port);

            if(host_port)
                g_env_CNetClass->DeleteLocalRef(host_port);
        }
    }

    if (ret < 0)
    {
        DEBUG_HS_JNI(" processTlsConnectRequest ERROR !!!");
        return HSJNI_ERROR;
    }

    DEBUG_HS_JNI(" processTlsConnectRequest: Exiting Successfully \n");
    return HSJNI_SUCCESS;
}


static eHSJNI_Result processTlsFreeConnectionRequest()
{
    DEBUG_HS_JNI(" processTlsFreeConnectionRequest: Entering \n");

    if (g_class_CNet != NULL)
    {
        if (method_FreeConnection)
        {
            g_env_CNetClass->CallStaticVoidMethod(g_class_CNet, method_FreeConnection);
        }
    }

    DEBUG_HS_JNI(" processTlsFreeConnectionRequest: Exiting Successfully \n");
    return HSJNI_SUCCESS;
}

static eHSJNI_Result processTlsSendRequest(void *p_inNalPayload)
{
    s_NAL_slpData *p_slpPayload = NULL;

    jint ret = -1;

    DEBUG_HS_JNI(" processTlsSendRequest: Entering \n");

    p_slpPayload = (s_NAL_slpData *)&((TNAL_cmdPaylaod *)p_inNalPayload)->s_slpData;
    /* Just in case... */
    if (NULL == p_slpPayload)
    {
        DEBUG_HS_JNI(" processTlsSendRequest: NULL Pointers \n");
        return HSJNI_ERROR;
    }

    DEBUG_HS_JNI(" processTlsSendRequest: length = %d, a_buffer = %x \n", p_slpPayload->length, p_slpPayload->a_buffer);

#ifdef PRINT_MSG
	DEBUG_HS_JNI("[");
    for (int i = 0; i < p_slpPayload->length; i++)
    {
        //DEBUG_HS_JNI("===%x=== \t", p_slpPayload->a_buffer[i]);
        DEBUG_HS_JNI("0x%x[%c]", p_slpPayload->a_buffer[i], p_slpPayload->a_buffer[i]);
    }
	DEBUG_HS_JNI("]\n");
#endif

    if (g_class_CNet != NULL)
    {
        jbyteArray barr = g_env_CNetClass->NewByteArray(p_slpPayload->length);
        if (barr)
        {
            jbyte *bytes = g_env_CNetClass->GetByteArrayElements(barr, NULL);
            if (bytes)
            {
                memcpy (bytes, p_slpPayload->a_buffer, p_slpPayload->length);

                if (method_Send)
                {
                    ret = g_env_CNetClass->CallStaticIntMethod(g_class_CNet, method_Send, barr);
                }

                //g_env_CNetClass->ReleaseByteArrayElements(barr, bytes, 0 /*JNI_COMMIT*/);
            }

            g_env_CNetClass->ReleaseByteArrayElements(barr, bytes, 0 /*JNI_COMMIT*/);
        }

        if(barr) g_env_CNetClass->DeleteLocalRef(barr);
    }

    if (ret < 0)
    {
        DEBUG_HS_JNI(" processTlsSendRequest: ERROR !!! \n");
        return HSJNI_ERROR;
    }

    DEBUG_HS_JNI(" processTlsSendRequest: Exiting Successfully \n");
    return HSJNI_SUCCESS;
}


static eHSJNI_Result processTlsStartReceiveRequest()
{
    jint ret = -1;

    DEBUG_HS_JNI(" processTlsStartReceiveRequest: Entering \n");

    if (g_class_CNet != NULL)
    {
        if (method_Receive)
        {
            ret = g_env_CNetClass->CallStaticIntMethod(g_class_CNet, method_Receive);
        }
    }


    if (ret < 0)
    {
        DEBUG_HS_JNI(" processTlsStartReceiveRequest ERROR!!! \n");
        return HSJNI_ERROR;
    }

    DEBUG_HS_JNI(" processTlsStartReceiveRequest: Exiting Successfully \n");
    return HSJNI_SUCCESS;
}


static int processGetSubscriberIdRequest(char *p_imsiBuffer)
{
    jbyteArray imsi_byte = NULL;
    jint ret = -1;

    if ( (p_imsiBuffer != NULL) &&
            (g_class_CSUPL_LP != NULL))
    {
        if (method_getSubscriberId)
        {
            imsi_byte = (jbyteArray) g_env_CSUPL_LP_Class->CallStaticObjectMethod(g_class_CSUPL_LP, method_getSubscriberId);
            if (imsi_byte)
            {
                jint size = g_env_CSUPL_LP_Class->GetArrayLength(imsi_byte);
                if (size <= MAX_SIZE_IMSI_BUFFER)
                {
                    g_env_CSUPL_LP_Class->GetByteArrayRegion(imsi_byte,
                                            0,
                                            size,
                                            (jbyte *) p_imsiBuffer);

                    ret = size;

                    DEBUG_HS_JNI(" processGetSubscriberIdRequest. p_imsiBuffer = %x \n", p_imsiBuffer);

#ifdef PRINT_MSG
                    for (int count = 0; count < size; count++)
                        DEBUG_HS_JNI(" processGetSubscriberIdRequest: %x \n", p_imsiBuffer[count]);
#endif
                }
                else
                {
                    ret = -1;
                }

                if(imsi_byte) g_env_CSUPL_LP_Class->DeleteLocalRef(imsi_byte);
            }
            //DetachCurrentThread();

        }

        //DetachCurrentThread();

  }

    if (ret < 0)
    {
        DEBUG_HS_JNI(" processGetSubscriberIdRequest ERROR!!! \n");
    }

    return ret;
}


static int processGetMSISDNRequest(char *p_msisdnBuffer)
{
    jbyteArray msisdn_byte = NULL;
    jint ret = -1;

    if ( (p_msisdnBuffer != NULL) &&
            (g_class_CSUPL_LP != NULL))
    {
        if (method_getMSISDN)
        {
            msisdn_byte = (jbyteArray) g_env_CSUPL_LP_Class->CallStaticObjectMethod(g_class_CSUPL_LP, method_getMSISDN);
            if (msisdn_byte)
            {
                jint size = g_env_CSUPL_LP_Class->GetArrayLength(msisdn_byte);
                if (size <= MAX_SIZE_IMSI_BUFFER)
                {
                    g_env_CSUPL_LP_Class->GetByteArrayRegion(msisdn_byte,
                                            0,
                                            size,
                                            (jbyte *) p_msisdnBuffer);
                    ret = size;

                    DEBUG_HS_JNI(" processMSISDNRequest. p_msisdnBuffer = %x \n", p_msisdnBuffer);
                    for (int count = 0; count < size; count++)
                    {
                        DEBUG_HS_JNI(" processGetMSISDNRequest: %x \n", p_msisdnBuffer[count]);
                    }	
                }
                else
                {
                    ret = -1;
                }
                if(msisdn_byte)
                {
                    g_env_CSUPL_LP_Class->DeleteLocalRef(msisdn_byte);
                    //LOGD(" ===>>> [ByteArrayProfiling] - %s - Line %d: Deallocate", __func__, __LINE__);
                }
            }
        }
    }

    if (ret < 0)
    {
        DEBUG_HS_JNI(" processGetMSISDNRequest ERROR!!! \n");
    }

    return ret;
}
static int processGetHSLPRequest(int *p_hslpBuffer)
{
    JNIEnv *env = NULL;
    jintArray hslp = NULL;
    jint ret = -1;
    jclass clazz = NULL;
    // DEBUG_HS_JNI(" processGetHSLPRequest start \n");
    if ( (p_hslpBuffer != NULL) &&
            ((clazz = AttachFindClass (&env, g_CSUPL_LP_Class)) != NULL))
    {
       // DEBUG_HS_JNI(" processGetHSLPRequest after AttachFindClass \n");

        jmethodID mid = env->GetStaticMethodID(clazz, "getHSlp", "()[I");
        if (mid)
        { 
         //   DEBUG_HS_JNI(" processGetHSLPRequest after GetStaticMethodID \n");
            hslp = (jintArray) env->CallStaticObjectMethod(clazz, mid);
            DEBUG_HS_JNI(" processGetHSLPRequest after CallStaticObjectMethod \n");
            if (hslp)
            {
                jint size = env->GetArrayLength(hslp);
                if (size <= MAX_HSLP_PARAMS)
                {
                    env->GetIntArrayRegion(hslp,
                                            0,
                                            size,
                                            (jint *)p_hslpBuffer);
                    ret = size;
                    DEBUG_HS_JNI(" processGetHSLPRequest. p_hslpBuffer = %x \n", p_hslpBuffer);
                    for (int count = 0; count < size; count++)
                    {
                        DEBUG_HS_JNI(" processGetHSLPRequest: %x \n", p_hslpBuffer[count]);
                    }	
                }
                else
                {
                    ret = -1;
                }
				if(hslp)
					env->DeleteLocalRef(hslp);				
            }
        }
    }
    if (ret < 0)
    {
        DEBUG_HS_JNI(" processGetHSLPRequest ERROR!!! \n");
    }
    return ret;
}

static int processGetCellInfoRequest(int *p_cellInfo)
{
    jint ret = -1;

	DEBUG_HS_JNI(" processGetCellInfoRequest: Entering !!! \n");
    if (g_class_CSUPL_LP != NULL)
    {
        if (method_getGSMInfo)
        {
            jintArray info_ = (jintArray) g_env_CSUPL_LP_Class->CallStaticObjectMethod(g_class_CSUPL_LP, method_getGSMInfo);
            jint size = g_env_CSUPL_LP_Class->GetArrayLength(info_);
            g_env_CSUPL_LP_Class->GetIntArrayRegion(info_, 0, size, p_cellInfo);
            ret = size;
				if(info_)
                g_env_CSUPL_LP_Class->DeleteLocalRef(info_);
        }
        //DetachCurrentThread();
    }

    if (ret < 0)
    {
        DEBUG_HS_JNI(" processGetCellInfoRequest: FAILED !!! \n");
    }

    return ret;
}

static int processGetNeighbourCellInfoRequest(int *p_NMRInfo)
{

	//Kaushal.k - To do - Call RIL function to get Information
	DEBUG_HS_JNI(" processGetNeighbourCellInfoRequest: Entering !!! \n");
	 int nmr_data[16]={10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25};
	 memcpy((void *)p_NMRInfo, (void *)nmr_data, 16);
    return 0;
}

static eHSJNI_Result sendResponseToNal(const McpS32 s32_inSocketId,
                                       const eHSJNI_Result * pe_result )
{
    char s8_outBuffer[8] = {'\0'};
    int s16_sentBytes = 0;

    DEBUG_HS_JNI(" sendResponseToNal: Entering \n");

#if 1
    if ( *pe_result > 0)
        strcpy(s8_outBuffer, "1");
    else if (*pe_result == 0)
        strcpy(s8_outBuffer, "0");
    else
        strcpy(s8_outBuffer, "-1" );

    DEBUG_HS_JNI(" ===>>> sendResponseToNal: Writing %s. atoi() = %d \n", s8_outBuffer, atoi(s8_outBuffer));
    DEBUG_HS_JNI(" ===>>> sendResponseToNal: Writing %d to sockID %d \n", *pe_result, s32_inSocketId);
    s16_sentBytes = write(s32_inSocketId, (void *)s8_outBuffer, strlen(s8_outBuffer) );
    if (s16_sentBytes < 0)
    {
        DEBUG_HS_JNI(" sendResponseToNal ERROR!!! \n");
        return HSJNI_ERROR;
    }
#endif

    DEBUG_HS_JNI(" sendResponseToNal: Exiting Successfully \n");
    return HSJNI_SUCCESS;
}

static eHSJNI_Result sendResponseToNal(const McpS32 s32_inSocketId,
                                       const void *const p_buffer,
                                       const int s32_inSize)
{
    int s16_sentBytes = 0;

    DEBUG_HS_JNI(" sendResponseToNal*: Entering \n");

    s16_sentBytes = write(s32_inSocketId, (void *)p_buffer, s32_inSize);
    if (s16_sentBytes < 0)
    {
        DEBUG_HS_JNI(" sendResponseToNal* ERROR!!! \n");
        return HSJNI_ERROR;
    }

    DEBUG_HS_JNI(" sendResponseToNal*: Exiting Successfully \n");
    return HSJNI_SUCCESS;
}



/**
 * Function:        connectWithMcpfSocket
 * Brief:           Creates a socket and connects with it.
 * Description:     A socket is exposed by MCPF for external applications to communicate
                    with it. Helper Service writes the requests on this socket.
 * Note:            Internal function.
 * Params:          u16_inPortNumber - Port Number.
                    p_inHostName - Host Name.
 * Return:          Success: HSJNI_SUCCESS.
                    Failure: HSJNI_FAILURE_XXX.
 */
static eHSJNI_Result connectWithMcpfSocket(const McpU16 u16_inPortNumber,
                                           const McpU8 *const p_inHostName,
                                           McpS16 *const p_sockDescriptor)
{
    /* TI_PATCH start */
	//  struct sockaddr_in serverAddress;       /* Internet Address of Server. */
	    struct sockaddr_un serverAddress;       /* Internet Address of Server. */
	/* TI_PATCH end */

    struct hostent *p_host = NULL;          /* The host (server) info. */
    McpS16 u16_sockDescriptor = -1;
    McpS8 retVal = 0;

    DEBUG_HS_JNI(" connectWithMcpfSocket: Entering \n");

    /* Obtain host information. */
    p_host = gethostbyname((char *)p_inHostName);
    if (p_host == (struct hostent *) NULL )
    {
        DEBUG_HS_JNI(" connectWithMcpfSocket: gethostbyname FAILED !!!");
        return HSJNI_ERROR;
    }

    /* Clear the structure. */
    memset( &serverAddress, 0, sizeof(serverAddress) );

    /* Set address type. */
    /* TI_PATCH start */
	#if 0
    serverAddress.sin_family = AF_INET;
    memcpy(&serverAddress.sin_addr, p_host->h_addr, p_host->h_length);
    serverAddress.sin_port = htons(u16_inPortNumber);
	#else
	    serverAddress.sun_family = AF_UNIX;
	    strcpy(serverAddress.sun_path, SOC_NAME_4121);
	#endif
	/* TI_PATCH end */


    /* Create a new socket. */
   /* TI_PATCH start */
   //  u16_sockDescriptor = socket(AF_INET, SOCK_STREAM, 0);
       u16_sockDescriptor = socket(AF_UNIX, SOCK_STREAM, 0);
	/* TI_PATCH end */

    if (u16_sockDescriptor < 0)
    {
        DEBUG_HS_JNI(" connectWithMcpfSocket: Socket creation FAILED !!!");
        return HSJNI_ERROR;
    }

    /* Connect with MCPF. */
    retVal = connect(u16_sockDescriptor,
                     (struct sockaddr *)&serverAddress,
                     sizeof(serverAddress) );
    if (retVal < 0)
    {
        DEBUG_HS_JNI(" connectWithMcpfSocket: Connection with MCPF FAILED !!! \n");
        return HSJNI_ERROR;
    }

    /* Maintain a global variable for keeping the socket descriptor. */
    *p_sockDescriptor = u16_sockDescriptor;

    DEBUG_HS_JNI(" connectWithMcpfSocket: Exiting Successfully. \n");
    return HSJNI_SUCCESS;
}

/**
 * Function:        sendRequestToMcpf
 * Brief:           Sends requests to MCPF.
 * Description:
 * Note:            Internal function.
 * Params:          e_reqCmd - Commands to be sent.
                    p_inData - Payload.
 * Return:          Success: HSJNI_SUCCESS.
                    Failure: HSJNI_FAILURE_XXX.
 */


static eHSJNI_Result sendRequestToMcpf(const McpU16 e_reqCmd,
                                       const void *const p_inData,
                                       McpU32 u32_msgSize)
{
    hostSocket_header_t s_MsgHdr;
    eHSJNI_Result retVal = HSJNI_SUCCESS;

    McpU8 uDestQueId = 0;
    void *p_payload = NULL;

    McpU16 count = 0;
    McpU32 bytesWritten = 0;

    DEBUG_HS_JNI(" sendRequestToMcpf: Entering \n");

    retVal = initializeMemory((void *)&s_MsgHdr, sizeof(hostSocket_header_t));
    if (HSJNI_SUCCESS != retVal)
    {
        DEBUG_HS_JNI(" sendRequestToMcpf: Memory initialization FAILED !!! \n");
        return retVal;
    }

    switch (e_reqCmd)
    {
    case SUPLC_CMD_FROM_NETWORK:
    {
        /* Send the request. */
        s_MsgHdr.msgClass = NAVL_CLASS_SUPL;
        s_MsgHdr.opCode = e_reqCmd;
        s_MsgHdr.payloadLen =  u32_msgSize;
        s_MsgHdr.syncSrart = HOSTSOCKET_MESSAGE_SYNC_START;

        p_payload = (void *)p_inData;
    }
    break;

    case SUPLC_CMD_MSGBOX_RESP:
    {
        /* Send the request. */
        s_MsgHdr.msgClass = NAVL_CLASS_SUPL;
        s_MsgHdr.opCode = e_reqCmd;
        s_MsgHdr.payloadLen =  u32_msgSize;
        s_MsgHdr.syncSrart = HOSTSOCKET_MESSAGE_SYNC_START;

        p_payload = (void *)p_inData;
    }
    break;	

	case SUPLC_CMD_SERVER_CONNECT:
    {
        /* Send the request. */
        s_MsgHdr.msgClass = NAVL_CLASS_SUPL;
        s_MsgHdr.opCode = e_reqCmd;
        s_MsgHdr.payloadLen =  u32_msgSize;
        s_MsgHdr.syncSrart = HOSTSOCKET_MESSAGE_SYNC_START;

        p_payload = (void *)p_inData;
    }
    break;

    default:
    {
    }
    break;
    }

    if (HSJNI_CONNECTION_ACTIVE != connectionWithMcpf)
    {
        retVal = connectWithMcpfSocket(MCPF_SOCKET_PORT, (McpU8 *)MCPF_HOST_NAME, &g_mcpfsocketId);
        if (retVal != HSJNI_SUCCESS)
        {
            DEBUG_HS_JNI(" Java_PostSLPMessageToQueue: Socket Connection FAILED !!! \n");
            connectionWithMcpf = HSJNI_CONNECTION_NOT_ACTIVE;
            g_mcpfsocketId = -1;
            return retVal;
        }

        connectionWithMcpf = HSJNI_CONNECTION_ACTIVE;
    }
    /* Just in case... */
    if (g_mcpfsocketId == 0 || g_mcpfsocketId == -1)
    {
        DEBUG_HS_JNI(" sendRequestToMcpf: Connection with MCPF not yet established. \n");
        connectionWithMcpf = HSJNI_CONNECTION_NOT_ACTIVE;
        g_mcpfsocketId = -1;
        return HSJNI_ERROR;
    }

    /* Send the command to MCPF. */
    DEBUG_HS_JNI(" sendRequestToMcpf: g_mcpfsocketId = %d, e_opcode = %d, u32_msgSize = %d \n", g_mcpfsocketId,
                 s_MsgHdr.opCode,
                 s_MsgHdr.payloadLen);
    if ( (bytesWritten = write(g_mcpfsocketId, (void *)&s_MsgHdr, sizeof(s_MsgHdr) )) < 0)
    {
        DEBUG_HS_JNI(" sendRequestToMcpf: Message Sending FAILED Trying to connect !!! \n");
        closeConnectionWithMcpf(&g_mcpfsocketId, &connectionWithMcpf);
        retVal = connectWithMcpfSocket(MCPF_SOCKET_PORT, (McpU8 *)MCPF_HOST_NAME, &g_mcpfsocketId);
        if (retVal != HSJNI_SUCCESS)
        {
            ERROR_HS_JNI(" Java_PostSLPMessageToQueue: Socket Connection FAILED !!! \n");
            connectionWithMcpf = HSJNI_CONNECTION_NOT_ACTIVE;
            g_mcpfsocketId = -1;
            return HSJNI_ERROR;
        }
        if ( (bytesWritten = write(g_mcpfsocketId, (void *)&s_MsgHdr, sizeof(s_MsgHdr) )) < 0)
        {
			ERROR_HS_JNI(" sendRequestToMcpf: Message Sending FAILED !!! \n");
			closeConnectionWithMcpf(&g_mcpfsocketId, &connectionWithMcpf);
			connectionWithMcpf = HSJNI_CONNECTION_NOT_ACTIVE;
            g_mcpfsocketId = -1;
            return HSJNI_ERROR;
        }
    }

    /* Send payload if any. */
    if (u32_msgSize)
    {
        if ( write(g_mcpfsocketId, (void *)p_payload, u32_msgSize ) < 0)
        {
            ERROR_HS_JNI(" sendRequestToMcpf: Payload Sending FAILED !!! \n");
            closeConnectionWithMcpf(&g_mcpfsocketId, &connectionWithMcpf);
			connectionWithMcpf = HSJNI_CONNECTION_NOT_ACTIVE;
            g_mcpfsocketId = -1;
            return HSJNI_ERROR;
        }
    }


    DEBUG_HS_JNI(" sendRequestToMcpf: bytesWritten = %d \n", bytesWritten);

    /** It can be closed here but connection has to be established with MCPF each time a message is received.
      * TBD:
      */
    //closeConnectionWithMcpf(&g_mcpfsocketId, &connectionWithMcpf);

    DEBUG_HS_JNI(" sendRequestToMcpf: Exiting Successfully \n");
    return HSJNI_SUCCESS;
}

static void closeConnectionWithMcpf(const McpS16 *const p_socketId,
                                    eHSJNI_Result *const p_connectionWithMcpf)
{

    shutdown(*p_socketId, SHUT_RDWR);
    close(*p_socketId);
    *p_connectionWithMcpf = HSJNI_CONNECTION_NOT_ACTIVE;

}
static jclass AttachFindClass (JNIEnv **p_env, const char *JavaClass)
{
    jclass jclazz = NULL;
    int ret = -1;

    DEBUG_HS_JNI(" ===>>> AttachFindClass: g_JVMSuplLibrary = %x and JavaClass = %s\n", g_JVMSuplLibrary, JavaClass);

    if (g_JVMSuplLibrary)
    {
        if ((ret = g_JVMSuplLibrary->AttachCurrentThread(p_env, 0))
                == JNI_OK)
        {
                /*if ((jclazz = (*p_env)->FindClass(JavaClass)) != NULL)
            {
                return jclazz;
            }
            else
            {
                        DEBUG_HS_JNI(" ===>>> AttachFindClass: error (*p_env)->FindClass(%s) ", JavaClass);

                if (DetachCurrentThread() < 0)
                {
                    DEBUG_HS_JNI(" ===>>> AttachFindClass: error detach current thread ");
                }

                return NULL;
                }*/

                /*
                 * This is a little awkward because the JNI FindClass call uses the
                 * class loader associated with the native method we're executing in.
                 * Because this native method is part of a "boot" class, JNI doesn't
                 * look for the class in CLASSPATH, which unfortunately is a likely
                 * location for it.  (Had we issued the FindClass call before calling
                 * into the VM -- at which point there isn't a native method frame on
                 * the stack -- the VM would have checked CLASSPATH.  We have to do
                 * this because we call into Java Programming Language code and
                 * bounce back out.)
                 *
                 * JNI lacks a "find class in a specific class loader" operation, so we
                 * have to do things the hard way.
                 */
                //jclazz = (*p_env)->FindClass(JavaClass);

                jclass javaLangClassLoader;
                jmethodID getSystemClassLoader, loadClass;
                jobject systemClassLoader;
                jstring strClassName;

                /* find the "system" class loader; none of this is expected to fail */
                javaLangClassLoader = (*p_env)->FindClass("java/lang/ClassLoader");
                assert(javaLangClassLoader != NULL);
                DEBUG_HS_JNI(" ===>>> AttachFindClass: Got class java/lang/ClassLoader");
                getSystemClassLoader = (*p_env)->GetStaticMethodID(javaLangClassLoader, "getSystemClassLoader", "()Ljava/lang/ClassLoader;");
                DEBUG_HS_JNI(" ===>>> AttachFindClass: Got static method getSystemClassLoader");
                loadClass = (*p_env)->GetMethodID(javaLangClassLoader, "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;");
                assert(getSystemClassLoader != NULL && loadClass != NULL);
                DEBUG_HS_JNI(" ===>>> AttachFindClass: Got method ID loadClass");
                systemClassLoader = (*p_env)->CallStaticObjectMethod(javaLangClassLoader, getSystemClassLoader);
                assert(systemClassLoader != NULL);
                DEBUG_HS_JNI(" ===>>> AttachFindClass: Got class %s", JavaClass);

                /* create an object for the class name string; alloc could fail */
                strClassName = (*p_env)->NewStringUTF(JavaClass);
                if ((*p_env)->ExceptionCheck()) {
                        DEBUG_HS_JNI("ERROR: unable to convert '%s' to string\n", JavaClass);
                        return NULL;
                }
                DEBUG_HS_JNI("system class loader is %p, loading %p (%s)\n", systemClassLoader, strClassName, JavaClass);

                /* try to find the named class */
                jclazz = (jclass) (*p_env)->CallObjectMethod(systemClassLoader, loadClass, strClassName);
                if ((*p_env)->ExceptionCheck()) {
                        DEBUG_HS_JNI("ERROR: unable to load class '%s' from %p\n", JavaClass, systemClassLoader);
                        jclazz = NULL;
                }

                return jclazz;
        }
        DEBUG_HS_JNI(" ===>>> AttachFindClass: error attach thread %d", ret);
    }
    return NULL;
}

static int DetachCurrentThread ()
{
    int ret = -1;
    if (g_JVMSuplLibrary)
    {
        if ((ret = g_JVMSuplLibrary->DetachCurrentThread())
                == JNI_OK)
        {
            return 0;
        }
    }
    return ret;
}

/**
 * Function:        allocateMemory
 * Brief:           Allocates Memory.
 * Description:
 * Note:            Internal function.
 * Params:          u32_sizeInBytes - Number of bytes requested.
 * Return:          Address of Memory Chunck.
 */
static void * allocateMemory(const McpU32 u32_sizeInBytes)
{
    DEBUG_HS_JNI(" allocateMemory: Entering. \n");

    DEBUG_HS_JNI(" allocateMemory: Exiting Successfully. \n");
    return (void *) calloc(1, u32_sizeInBytes );
}


/**
 * Function:        freeMemory
 * Brief:           Frees Memory.
 * Description:
 * Note:            Internal function.
 * Params:          p_memoryChunk: Memory address to be freed.
 * Return:
 */
static void freeMemory(void **p_memoryChunk)
{
    if (*p_memoryChunk != NULL)
    {
        free(*p_memoryChunk);
        *p_memoryChunk = NULL;
    }
}


/**
 * Function:        initializeMemory
 * Brief:           Initialize the memory.
 * Description:
 * Note:            Internal function.
 * Params:          p_memoryLocation - Memory Location.
                    u32_inMemSize - Size.
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static eHSJNI_Result initializeMemory(void *const p_memoryLocation, McpU32 u32_inMemSize)
{
    DEBUG_HS_JNI(" initializeMemory: Entering. \n");

    memset(p_memoryLocation, '\0', u32_inMemSize);

    DEBUG_HS_JNI(" initializeMemory: Exiting Successfully. \n");
    return HSJNI_SUCCESS;
}



