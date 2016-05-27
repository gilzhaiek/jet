/*
 *******************************************************************************
 *         TEXAS INSTRUMENTS INCORPORATED PROPRIETARY INFORMATION
 *
 * Property of Texas Instruments, Unauthorized reproduction and/or distribution
 * is strictly prohibited.  This product  is  protected  under  copyright  law
 * and  trade  secret law as an  unpublished work.
 * (C) Copyright Texas Instruments.  All rights reserved.
 *
 * FileName         :   gps.c
 *
 * Description      :
 *
 * Author           :   TI
 * Date             :   15th June 2009
 *
 ******************************************************************************
 */


#include <hardware/gps.h>
#include <cutils/properties.h>
#include <math.h>
#include <signal.h>

/* For Socket Programming. */
#include <sys/ioctl.h>
#include <string.h>
#include <ctype.h>
#include <unistd.h>
#include <stdlib.h>
#include <signal.h>
#include <sys/wait.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/time.h>

#include <sys/un.h>
#include <netdb.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#include <semaphore.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>

#include "mcp_hal_misc.h"
#include "mcp_hal_fs.h"
#include "mcpf_services.h"
#include "gpsc_types.h"
#include "navc_api.h"
#include "suplc_api.h"
#include "hostSocket.h"
#include "navl_api.h"
#include "ti_gps.h"


#define LOG_TAG "GPS_HAL"
#define LOG_NDEBUG 0 // Enable LOGV
#include <utils/Log.h>
#include <cutils/properties.h>

#define GPS_LOG_VERBOSE_PROPERTY "gps.log.verbose"

static int g_gps_log_ver = 0;
static void log_init(void);

#define GPS_LOG_VER(...) if (g_gps_log_ver) LOGV(__VA_ARGS__)
#define GPS_LOG_DBG(...) LOGD(__VA_ARGS__)
#define GPS_LOG_INF(...) LOGI(__VA_ARGS__)
#define GPS_LOG_WAR(...) LOGW(__VA_ARGS__)
#define GPS_LOG_ERR(...) LOGE(__VA_ARGS__)


#define GPS_CONFIG_FILE_PATH "/system/etc/gps/config/GpsConfigFile.txt"
#define GPS_PERIODIC_CONFIG_FILE_PATH "/system/etc/gps/config/PeriodicConfFile.cfg"
#define RDONLY  "r"
#define MAX_BUF_LENGTH 128

#define GPS_MODE       "gps_mode"
#define GPS_AUTONOMOUS "autonomous_mode"
#define GPS_MSBASED    "msbased_mode"
#define GPS_MSASSISTED "msassisted_mode"
#define GPS_AUTOSUPL   "autosupl_mode"

#define GPS_POSITION_MODE_AUTOSUPL   4

#define EXPIRY_TIME_SECONDS          5
#define DONT_CARE                   -1

#define EE_SUPPORT "ee_support"
#define HOR_ACC    "hor_acc"
#define LOC_AGE    "loc_age"
#define RSP_TIME   "response_time"

#define MAX_HOR_ACC_VALUE  0xFFFF
#define MAX_LOC_AGE_VALUE  0xFF
#define MAX_RSP_TIME_VALUE 0xFFFF

#define ENABLE_EE "yes"

#define MIN_SIG_STRENGTH_FOR_SV_VISIBILITY 5.0

#define MCPF_SOCKET_PORT      4121
#define MCPF_HOST_NAME        "127.0.6.1"

#define SOC_NAME_4121         "/data/gps/gps4121"
#define SOC_NAME_6121         "/data/gps/gps6121"

/*Macros and Global variables for MTLR stuff*/
#define NI_SOCKET_PORT        6121
#define NI_HOST_NAME          "localhost"
#define MAX_CONNECTIONS       100
#define MAX_BUF_SIZE          1024
#define AUTONOMOUS_SESSION    254

#define VERDIRECT             0x01
#define BEARING               0x02
#define HORSPEED              0x04
#define VERSPEED              0x08
#define HORUNCERTSPEED        0x10
#define VERUNCERTSPEED        0x20
#define POS_ALTITUDE          0x01
#define POS_UNCERTAINTY       0x02
#define POS_CONFIDENCE        0x04
#define POS_VELOCITY          0x08

#define GPSM_NMEA_FILE
#define NMEA_MASK 0x003F
#define NMEA_MASK1 (GPSC_RESULT_NMEA_GGA | GPSC_RESULT_NMEA_GLL | GPSC_RESULT_NMEA_GSA | GPSC_RESULT_NMEA_GSV | GPSC_RESULT_NMEA_RMC | GPSC_RESULT_NMEA_VTG)
#define PRINT_MSG

#define FUNCTION_NAME __func__

/* Only these fields are supported by TNAVC_delAssist */
#define DEL_AIDING_TIME            0x1           /* Delete time                    */
#define DEL_AIDING_POSITION        0x2           /* Delete position                */
#define DEL_AIDING_EPHEMERIS       0x4           /* Delete ephemeris               */
#define DEL_AIDING_ALMANAC         0x8           /* Delete almanac                 */
#define DEL_AIDING_IONO_UTC        0x10          /* Delete Iono/UTC                */
#define DEL_AIDING_SVHEALTH        0x20          /* Delete SV health               */
#define DEL_AIDING_SVDIR           0x40          /* Delete SV steering             */
                                                 /* Delete all the data. Cold start */
#define DEL_AIDING_ALL             DEL_AIDING_TIME | DEL_AIDING_POSITION | DEL_AIDING_EPHEMERIS | \
                                   DEL_AIDING_ALMANAC |  DEL_AIDING_IONO_UTC | DEL_AIDING_SVHEALTH

#define C_LSB_ELEV_MASK       (90.0/128.0)      /* Elevation degrees */
#define C_LSB_AZIM_MASK       (360.0/256)       /* Azimuth degrees */

#define TRUE 1
#define FALSE 0

#define STATUS_CB            (1 << 0)
#define NMEA_CB              (1 << 1)
#define LOCATION_CB          (1 << 2)
#define SV_STATUS_CB         (1 << 3)
#define MAX_NMEA_BUFF_SIZE 101

typedef enum {
    NAVC_SUPL_MO = 1,
    NAVC_SUPL_MT_POSITIONING,
    NAVC_SUPL_MT_NO_POSITIONING,
    NAVC_E911,
    NAVC_CP_MTLR,
    NAVC_CP_NILR,
    NAVC_STANDLONE
} T_GPSC_call_type;

#ifdef HAVE_GPS_HARDWARE

/* Interfaces exposed by framework for supporting location based services. */
GpsInterface Interface = {
    sizeof(GpsInterface),       /* Size of the GPS interface */
    hgps_init,                  /* Requests MCPF to create Navigation Controller. */
    hgps_start,                 /* Requests MCPF to start GPS receiver
                                   and issue location fix request. */
    hgps_stop,                  /* Requests MCPF to stop location fix (if any)
                                   and stop GPS receiver. */
    hgps_cleanup,               /* Does cleanup activities. */
    hgps_inject_time,           /* Not Implemented. */
    hgps_inject_location,       /* Not Implemented. */
    hgps_delete_aiding_data,    /* Delete the specified aiding data. */
    hgps_set_position_mode,     /* Set the location fix mode requested by applications.
                                   (Autonomous or MS-Based or MS-Assisted). */
    hgps_get_extension,         /* Get extra interfaces for supporting additional functionalities.
                                   For ex: SUPL. */
};

/* Interfaces exposed by framework for supporting extra interfaces. */
GpsXtraInterface XtraInterface = {
    sizeof(GpsXtraInterface),
    hgps_xtra_init,
    hgps_xtra_data,
};

/* AGPS Intefaces */
AGpsInterface AGPSInterface= {
    sizeof(AGpsInterface),
    hgps_agps_init,
    hgps_agps_data_conn_open,
    hgps_agps_data_conn_closed,
    hgps_agps_data_conn_failed,
    hgps_agps_set_server,
};

/* GPS NI Interface */
GpsNiInterface NIInterface= {
    sizeof(GpsNiInterface),
    hgps_ni_init,
    hgps_ni_response,
};

typedef enum {
    NI_RESPONSE_ACCEPT = 1,
    NI_RESPONSE_DENY   = 2,
    NI_RESPONSE_NORESP = 3,
} eGPSAL_NIResp;

typedef pthread_t       THREAD_HANDLE;
typedef sem_t*          SEM_HANDLE;

/* Boolean Values. */
typedef enum boolVal {
    GPSAL_FALSE = 0,
    GPSAL_TRUE  = 1,
} eGPSAL_Bool;

/* For Thread Communication & Synchronisation. */
typedef struct gpsalObject{
    THREAD_HANDLE   p_threadHandle;         /* Thread Handle. */
    THREAD_HANDLE   p_nithreadHandle;       /* Thread Handle for NI. */
    THREAD_HANDLE   p_CBthreadHandle;       /* Thread Handle for CB. */
    SEM_HANDLE      p_semHandle;            /* Semaphore Handle. */
    SEM_HANDLE      p_CbSemHandle;          /* Semaphore Handle for CB. */
    McpS16          u16_socketDescriptor;   /* Socket Descriptor. */
    eGPSAL_Bool     e_isAckRequired;        /* Ack Request Status. */
    eGPSAL_Bool     e_isConnectionValid;    /* Connection Status with MCPF. */
    GpsCallbacks    *p_callbacks;           /* Framework Callbacks. */
    AGpsCallbacks   *p_agpsCallback;        /* Framework callback for AGPS. */
    GpsNiCallbacks  *p_niCallback;          /* Framework callback for NI. */

    McpU16          u16_responseOpcode;     /* Response message opcode. */
    McpU32          u32_responseMsgLen;     /* Response message length. */
    McpU8           u8_cbType;              /* Variable to set call back type */

    void            *p_responseData;        /* MCPF's response message. */
    GpsSvStatus Svstatus;
    char nmea_buff[MAX_NMEA_BUFF_SIZE];
} TGPSAL_Object;

//sDK
typedef struct {
    pthread_mutex_t mutex_lock;
    int             notif_id;
    int             in_progress;
    int             response_time_left;
} ni_request_state_t;

ni_request_state_t ni_state;

#ifdef CONFIG_PERIODIC_REPORT
typedef struct {
    McpU8 enable;
    McpU16 ReportPeriod; // reporting period in seconds
    McpU16 NumReports;
} PeriodicConfFile;
#endif /* CONFIG_PERIODIC_REPORT */


/*************************************************************************
 *
 *  Periodic Report Feature
 *  Use Periodic Report Generator Tool to create PeriodicConfFile.cfg File.
 *  Structure for this Feature
 *
 *************************************************************************/

static GpsStatus       g_hgps_status;       /* Used to send Status of GPS. */
static AGpsStatus      g_aGpsStatus;        /* Used to send status of aGPS. */

static TGPSAL_Object  *gp_gpsalObject = NULL;
static McpS16          g_socketDescriptor = -1;
static McpS16          cplc_socketDescriptor = -1;

static McpU32   g_locSessId = 0;
static T_GPSC_loc_fix_mode g_e_locationFixMode = GPSC_FIXMODE_AUTONOMOUS;
static T_GPSC_call_type g_e_Call_type = NAVC_STANDLONE;

static McpS32 g_s32_fixFrequency = 1;

static McpU8 g_locFixMode = 0;
static McpU8 g_gpsInitialized = 0;

static McpU8 g_eeSupported = 0;
static McpU8 g_suplCoreCreated = 0;
static int startorlocfix = 0;

static McpU16 g_hor_acc  = MAX_HOR_ACC_VALUE;
static McpU8  g_loc_age  = MAX_LOC_AGE_VALUE;
static McpU32 g_rsp_time = MAX_RSP_TIME_VALUE;

//TGPSAL_Object   *agps_ClientObject;
//Global copy of GPSAL client socket
static int clientSockFD = -1;

static pthread_mutex_t g_waitMutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t g_waitCondVar = PTHREAD_COND_INITIALIZER;

static pthread_mutex_t g_CBWaitMutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t g_CBWaitCondVar = PTHREAD_COND_INITIALIZER;

static int g_IsInitComplete = DONT_CARE;
static int g_IsStartComplete = DONT_CARE;
static int g_IsStopComplete = DONT_CARE;
static int g_IsSaveAssistComplete = DONT_CARE;
static int g_GPSStartCount = 0;
static int g_GPSNmeaFifo = 0;
static int g_GPSCalbackCounter = 0;
static int g_TTFFflag = 0; // 0 still wait location fix, 1 already got location fix

static GpsSvStatus s_gpsSvStatus;
static GpsLocation s_gpsLocation;

#endif /* HAVE_GPS_HARDWARE */


/****************************************************/

/* Error Conditions. */
typedef enum{
    GPSAL_SUCCESS                    = 1,
    GPSAL_FAIL                       = 0,
    GPSAL_ERR_UNKNOWN_HOST           = -1,
    GPSAL_ERR_SOCK_CREATION          = -2,
    GPSAL_ERR_CONNECTION_FAILED      = -3,
    GPSAL_ERR_WRITE_FAILED           = -4,
    GPSAL_ERR_SEM_CREATE_FAILED      = -5,
    GPSAL_ERR_THREAD_CREATION_FAILED = -6,
    GPSAL_ERR_NO_CONNECTION          = -7,
    GPSAL_ERR_UNKNOWN                = -8,
    GPSAL_ERR_SEM_POST_FAILED        = -9,
    GPSAL_ERR_SEM_WAIT_FAILED        = -10,
    GPSAL_ERR_NULL_PTS               = -11,
    GPSAL_ERR_CALCULUS_FAILED        = -12,
    GPSAL_ERR_INIT_FAILED            = -13,
    GPSAL_ERR_DATA_PROCESS_FAILED    = -14,

} eGPSAL_Result;

int readAGPSStatus();
int readTheAGPSMode();

void set_navl_server(int state);
void set_rxn_intapp (int state);


/**
 * Function:        gps_find_hardware
 * Brief:           It gets the GPS interface.
 * Description:
 * Note:            Internal function.
 * Params:          p_outGpsInterface - Double pointer to GPS Interface.
 * Return:          None.
 */
static void gps_find_hardware(GpsInterface **p_outGpsInterface);

/**
 * Function:        gps_get_hardware_interface
 * Brief:           It gets the GPS hardware interface.
 * Description:
 * Note:            Internal function.
 * Params:          None.
 * Return:          Handle to GPS Hardware Interface.
 */
static GpsInterface* gps_get_hardware_interface();

/**
 * Function:        connectWithMcpfSocket
 * Brief:           Creates a socket and connects with it.
 * Description:     A socket is exposed by MCPF for external applications to communicate
                    with it. GPS_AL writes the requests on this socket.
 * Note:            Internal function.
 * Params:          u16_inPortNumber - Port Number.
                    p_inHostName - Host Name.
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE.
 */
static eGPSAL_Result connectWithMcpfSocket(const McpU16 u16_inPortNumber,
                                           const McpU8 *const p_inHostName);

/**
 * Function:        sendRequestToMcpf
 * Brief:           Sends requests to MCPF.
 * Description:
 * Note:            Internal function.
 * Params:          e_reqCmd - Commands to be sent.
                    p_inData - Payload.
                    u32_msgSize - Size.
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static eGPSAL_Result sendRequestToMcpf(const McpU16 e_reqCmd,
                                       const void *const p_inData,
                                       const eGPSAL_Bool e_isAckRequired,
                                       McpU32 u32_msgSize);

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
 * Function:        createSemaphore
 * Brief:           Creates unnamed semaphore.
 * Description:
 * Note:            Internal function.
 * Params:          p_semHandle - Semaphore Handle.
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static eGPSAL_Result createSemaphore(SEM_HANDLE p_semHandle);

/**
 * Function:        createResponseHandler
 * Brief:           Creates a thread for waiting on responses from MCPF.
 * Description:
 * Note:            Internal function.
 * Params:          p_inGpsalObject - GPSAL Object Handle.
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static eGPSAL_Result createResponseHandler(const TGPSAL_Object *const p_inGpsalObject);

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
static eGPSAL_Result initializeMemory(void *const p_memoryLocation, McpU32 u32_inMemSize);

/**
 * Function:        issueLocationFixRequest
 * Brief:           Issue location fix requests after receiver sends START confirmation.
 * Description:
 * Note:            Internal function.
 * Params:          void - None.
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static eGPSAL_Result issueLocationFixRequest(void);

/**
 * Function:        waitForSemaphore
 * Brief:           Blocks on a semaphore.
 * Description:
 * Note:            Internal function.
 * Params:          p_semaphoreHandle - Semaphore Handle.
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static eGPSAL_Result waitForSemaphore(SEM_HANDLE p_semaphoreHandle);

/**
 * Function:        releaseSemaphore
 * Brief:           Releaase the semaphore.
 * Description:
 * Note:            Internal function.
 * Params:          p_semaphoreHandle - Semaphore Handle.
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static eGPSAL_Result releaseSemaphore(SEM_HANDLE p_semaphoreHandle);

/**
 * Function:        handleMcpfResponse
 * Brief:           Handles MCPF's responses and call appropriate FW callbacks.
 * Description:
 * Note:            Internal function.
 * Params:          p_inGpsalObject -  - GPSAL Object Handle..
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static eGPSAL_Result handleMcpfResponse(const TGPSAL_Object *const p_inGpsalObject);

/**
 * Function:        processCmdCompleteResponse
 * Brief:           Handles NAVC's command completion responses and
                    call appropriate FW callbacks.
 * Description:
 * Note:            Internal function.
 * Params:          p_inPayload - Data payload.
                    p_callbacks - Callback handler.
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static eGPSAL_Result processCmdCompleteResponse(const void *const p_inPayload,
                                                const GpsCallbacks *const p_callbacks);

/**
 * Function:        processNmeaData
 * Brief:           Processes NMEA data and call appropriate FW callbacks.
 * Description:
 * Note:            Internal function.
 * Params:          p_inNmeaResponse - Nmea Response.
                    p_callbacks - Callback handler.
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static eGPSAL_Result processNmeaData(const void *const p_inNmeaResponse,
                                     const GpsCallbacks *const p_callbacks);

/**
 * Function:        entryFunctionForHandler
 * Brief:           Creates a thread for waiting on responses from MCPF.
 * Description:
 * Note:            Internal function.
 * Params:          p_inGpsalObject - GPSAL Object Handle.
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static void entryFunctionForHandler(void *inputHandler);

/**
 * Function:        gpsResponseHandler
 * Brief:           Creates a thread for sending responsed to Android location manager.
 * Description:
 * Note:            Internal function.
 * Params:          p_inGpsalObject - GPSAL Object Handle.
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static void gpsResponseHandler(void *inputHandler);

/**
 * Function:        processResponsesFromMcpf
 * Brief:           Processes MCPF's responses.
 * Description:
 * Note:            Internal function.
 * Params:          p_inGpsalObject - GPSAL Object Handle.
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static eGPSAL_Result processResponsesFromMcpf(const TGPSAL_Object *const p_inGpsalObject);

/**
 * Function:        processLocationFixResponse
 * Brief:           Handles MCPF location fix responses and
                    call appropriate FW callbacks.
 * Description:
 * Note:            Internal function.
 * Params:          p_inPayload - Data payload.
                    p_callbacks - Callback handler.
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static eGPSAL_Result processLocationFixResponse(const void *const p_inPayload,
                                                const GpsCallbacks *const p_callbacks);

/**
 * Function:        readConfigFile
 * Brief:
 * Description:
 * Note:            Internal function.
 * Params:
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static eGPSAL_Result readConfigFile();

static eGPSAL_Result stringCompare(const char *const p_string_1,
                                   const char *const p_string_2);

#if 1
/**
 * Function:        processRawMeasurement
 * Brief:           Processes PNR SNR report and  FW callbacks.
 * Description:
 * Note:            Internal function.
 * Params:          p_RawMeas -
                    p_measurement -
                    p_callbacks - Callback handler.
                    p_outGpsSVStatus - Satelite Vehicle Information.
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static eGPSAL_Result processProtMeasurement(const T_GPSC_prot_measurement *const p_ProtMeas,
                                            const T_GPSC_sv_measurement *const p_measurement,
                                            const GpsCallbacks *const p_callbacks,
                                            GpsSvStatus *const p_outGpsSVStatus);

/**
 * Function:        ProcessRawPosition
 * Brief:           Processes Raw Position and call appropriate FW callbacks.
 * Description:
 * Note:            Internal function.
 * Params:          p_RawPosition -
                    p_gpsPosition -
                    p_callbacks - Callback handler.
                    p_outLocation - Location Information.
                    p_outGpsSVStatus -
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static eGPSAL_Result ProcessRawPosition(const T_GPSC_raw_position * const p_RawPosition,
                                        const T_GPSC_position *const p_gpsPosition,
                                        const GpsCallbacks *const p_callbacks,
                                        GpsLocation *p_outLocation,
                                        GpsSvStatus *const p_outGpsSVStatus);

/**
 * Function:        processProtPosition
 * Brief:           Processes Prot Position and call appropriate FW callbacks.
 * Description:
 * Note:            Internal function.
 * Params:          p_inLocFixResponseUnion - Location response payload.
                    p_callbacks - Callback handler.
                    p_outLocation - Location Information.
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static eGPSAL_Result processProtPosition(const T_GPSC_prot_position * p_RrlpPosition,
                                         const T_GPSC_ellip_alt_unc_ellip *const p_RrlpEllipInfo,
                                         const GpsCallbacks *const p_callbacks,
                                         GpsLocation *const p_outLocation);

/**
 * Function:        calculatePrnAndSnr
 * Brief:           Processes Raw Measurements and call appropriate FW callbacks.
 * Description:
 * Note:            Internal function.
 * Params:          p_outGpsSVStatus - Satelite Vehicle Information.
                    p_inRawMeasurement - Raw Measurements.
                    p_inMeasurement -
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static eGPSAL_Result calculatePrnAndSnr(GpsSvStatus *const p_outGpsSvStatus,
                                        const T_GPSC_prot_measurement *const p_inProtMeasurement,
                                        const T_GPSC_sv_measurement *const p_inMeasurement);

/**
 * Function:        calculateSpeed
 * Brief:           Processes Raw Position and call appropriate FW callbacks.
 * Description:
 * Note:            Internal function.
 * Params:          p_outLocation - Location Data.
                    p_outGpsSVStatus - Satelite Vehicle Information.
                    e_is2dFix - 2d/3d Fix.
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static eGPSAL_Result calculateSpeed(GpsLocation *const p_outLocation,
                                    const T_GPSC_position *const p_inGpsSvStatus,
                                    const McpBool e_is2dFix);

/**
 * Function: calculateBearingAndTime
 */
static eGPSAL_Result calculateBearingAndTime(GpsLocation *const p_outLocation,
                                             const T_GPSC_position *const p_inGpsSvStatus);

/**
 * Function:        calculateUtcTime
 * Brief:           Calculate UTC Time.
 * Description:
 * Note:            Internal function.
 * Params:          p_outLocation - Location Information.
                    p_inGpsSvStatus - GPS Satellite Vehicle Information.
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
//static eGPSAL_Result calculateUtcTime(GpsLocation *const p_outLocation,
//                                      const T_GPSC_position *const p_inGpsSvStatus);

static  void calculateUtcTime(GpsLocation *const location,
                              const T_GPSC_toa *const pToa);

/**
 * Function:        calculateAccuracy
 * Brief:           Calculate Accuracy.
 * Description:
 * Note:            Internal function.
 * Params:          p_outLocation - Location Information.
                    p_inGpsSvStatus - GPS Satellite Vehicle Information.
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static eGPSAL_Result calculateAccuracy(GpsLocation *const p_outLocation,
                                       const T_GPSC_position *const p_inGpsSvStatus);

/**
 * Function: calculateSVsUsedInFixMask
 */
static eGPSAL_Result calculateSVsUsedInFixMask(GpsSvStatus *const SvStatus,
                                               const T_GPSC_position *const gpsSvStatus);

#endif

/**
 * Function: populateDeleteBitmap
 */
static void populateDeleteBitmap(TNAVC_delAssist *const pDelAssist, const GpsAidingData flags);


/**
 * Function: WaitForCBCompletion
 */
int WaitForCBCompletion(int timeout);

/**
 * Function: SignalCBCompletion
 */
void SignalCBCompletion();

/**
 * Function: log_init
 */
void log_init(void)
{
    char enable[PROPERTY_VALUE_MAX];

    //if (property_get(GPS_LOG_VERBOSE_PROPERTY, enable, "0"))
    {
        //g_gps_log_ver = atoi(enable);
        g_gps_log_ver = 1;
        GPS_LOG_INF("log_init: Verbose = %d", g_gps_log_ver);
    }
}

/**
 * Function: hgps_thread
 */
static void hgps_thread(void *p)
{
}

/**
 * Function: ti_get_gps_interface
 */
const GpsInterface* ti_get_gps_interface(struct gps_device_t* dev)
{
    GPS_LOG_INF("ti_get_gps_interface: Returning Interface\n");
    return &Interface;
}

/**
 * Function: open_gps
 */
static int open_gps(const struct hw_module_t* module, char const* name,
                    struct hw_device_t** device)
{
    
    struct gps_device_t *dev = (struct gps_device_t *) malloc(sizeof(struct gps_device_t));
    memset(dev, 0, sizeof(*dev));

    GPS_LOG_INF("open_gps: Entering\n");
    dev->common.tag = HARDWARE_DEVICE_TAG;
    dev->common.version = 0;
    dev->common.module = (struct hw_module_t*)module;
    dev->get_gps_interface = ti_get_gps_interface;

    *device = (struct hw_device_t*)dev;
   
    GPS_LOG_VER("open_gps: Exiting\n");
    return 0;
}

/**
 * Function: WaitForCBCompletion
 */
int WaitForCBCompletion(int timeout)
{
    int retValue;
    struct timespec expiryTime;
    struct timeval currTime;

    GPS_LOG_VER("WaitForCBCompletion: Entering\n");

    retValue = gettimeofday(&currTime, NULL);
    /* Calculate expiry time in timespec */
    expiryTime.tv_sec  = currTime.tv_sec;
    expiryTime.tv_nsec = currTime.tv_usec * 1000;
    expiryTime.tv_sec += timeout;
    retValue = pthread_cond_timedwait( &g_CBWaitCondVar, &g_CBWaitMutex,  &expiryTime);
    GPS_LOG_VER("WaitForCBCompletion: pthread_cond_timedwait returned %d\n", retValue);
    return retValue;
}

/**
 * Function: SignalCBCompletion
 */
void SignalCBCompletion()
{
    pthread_cond_signal(&g_CBWaitCondVar);
}


/**
 * Function:        hgps_init.
 * Brief:           Requests MCPF for initializing itself for supporting navigational apps.
 * Description:
 * Note:            External function.
 * Params:          callbacks - Callback functions for carrying the responses
                                to GPS Location Provider.
 * Return:          Success: 0.
                    Failure: -1
 */
int hgps_init(GpsCallbacks* callbacks)
{
    GPS_LOG_INF("hgps_init: Entering");

    eGPSAL_Result retVal =GPSAL_SUCCESS ;
    eGPSAL_Bool e_isAckRequired = GPSAL_FALSE;
    int timeout = 10;
    log_init();

    /* start navl_server */
    set_navl_server(1);

    /*  Let the android boots before starting the GPS baseband */
    retVal = readConfigFile();
    if (GPSAL_SUCCESS != retVal)
    {
        GPS_LOG_ERR("ERROR: hgps_init: Config file reading FAILED !!!");
        set_navl_server(0);
        return retVal;
    }

    gp_gpsalObject = (TGPSAL_Object *)allocateMemory(sizeof(TGPSAL_Object));
    if ( (gp_gpsalObject == NULL) )
    {
        GPS_LOG_ERR("ERROR: hgps_init: Memory Allocation FAILED !!!");
        set_navl_server(0);
        return GPSAL_ERR_NULL_PTS;
    }

    gp_gpsalObject->p_semHandle = (SEM_HANDLE) allocateMemory(sizeof(sem_t));
    if (NULL == gp_gpsalObject->p_semHandle)
    {
        GPS_LOG_ERR("ERROR: hgps_init: Memory Allocation for p_semHandle FAILED !!!");
        freeMemory((void **)&gp_gpsalObject);
        set_navl_server(0);
        return GPSAL_ERR_NULL_PTS;
    }

    gp_gpsalObject->p_CbSemHandle = (SEM_HANDLE) allocateMemory(sizeof(sem_t));
    if (NULL == gp_gpsalObject->p_CbSemHandle)
    {
        GPS_LOG_ERR("ERROR: hgps_init: Memory Allocation for p_CbSemHandle FAILED !!!");
        freeMemory((void **)&gp_gpsalObject);
        set_navl_server(0);
        return GPSAL_ERR_NULL_PTS;
    }

    while (1)
    {
        retVal = connectWithMcpfSocket(MCPF_SOCKET_PORT, (McpU8 *)MCPF_HOST_NAME);
        if (retVal != GPSAL_SUCCESS)
        {
            GPS_LOG_VER("hgps_init: trying to connect to Navl Server !");
            sleep(1);
            timeout--;
            if (timeout == 0)
            {
                GPS_LOG_ERR("ERROR: hgps_init: Socket Connection FAILED !!!");

                freeMemory((void **)&gp_gpsalObject->p_semHandle);
                freeMemory((void **)&gp_gpsalObject->p_CbSemHandle);
                freeMemory((void **)&gp_gpsalObject);
                retVal = GPSAL_FAIL;
                set_navl_server(0);
                return retVal;
            }

            continue;
        }
        else
        {
            break;
        }
    }

    GPS_LOG_VER("hgps_init: Socket Connected.");

    /* Fill the object for inter-thread communication. */
    gp_gpsalObject->u16_socketDescriptor = g_socketDescriptor;
    gp_gpsalObject->e_isConnectionValid = GPSAL_TRUE;
    gp_gpsalObject->e_isAckRequired = e_isAckRequired;
    gp_gpsalObject->p_callbacks = callbacks;

    retVal = createSemaphore(gp_gpsalObject->p_semHandle);
    if ( (gp_gpsalObject->p_semHandle == NULL) || (retVal != GPSAL_SUCCESS) )
    {
        GPS_LOG_ERR("ERROR: hgps_init: Semaphore1 Creation FAILED !!!");
        freeMemory((void **)&gp_gpsalObject->p_semHandle);
        freeMemory((void **)&gp_gpsalObject);
        set_navl_server(0);
        return retVal;
    }
if (gp_gpsalObject->p_callbacks)
	{
        if( gp_gpsalObject->p_CBthreadHandle == NULL)
		{
            gp_gpsalObject->p_callbacks->set_capabilities_cb( GPS_CAPABILITY_MSB | GPS_CAPABILITY_MSA);
            gp_gpsalObject->p_CBthreadHandle = gp_gpsalObject->p_callbacks->create_thread_cb("hgpsCBthread",
                                                                                           gpsResponseHandler,
                                                                                           (void *)gp_gpsalObject);
            GPS_LOG_VER("gp_gpsalObject->p_CBthreadHandle created during initialization");
		}

	}
	else
	{
        GPS_LOG_ERR("ERROR: hgps_init: callbacks is NULL");
		return -1;
	}
    retVal = createSemaphore(gp_gpsalObject->p_CbSemHandle);
    if ( (gp_gpsalObject->p_CbSemHandle == NULL) || (retVal != GPSAL_SUCCESS) )
    {
        GPS_LOG_ERR("ERROR: hgps_init: Semaphore2 Creation FAILED !!!");
        freeMemory((void **)&gp_gpsalObject->p_CbSemHandle);
        freeMemory((void **)&gp_gpsalObject);
        set_navl_server(0);
        return retVal;
    }

    /* Create response handler */
    //retVal = createResponseHandler(gp_gpsalObject);
   // if (retVal != GPSAL_SUCCESS)
    //{
     //   GPS_LOG_ERR("ERROR: hgps_init: createResponseHandler failed.");
     //   return retVal;
    //}
  gp_gpsalObject->p_threadHandle = gp_gpsalObject->p_callbacks->create_thread_cb("hgps_thread",
		    entryFunctionForHandler, (void *)gp_gpsalObject);
    /* Start NAVC core */
    retVal = sendRequestToMcpf(NAVC_CMD_START, NULL, e_isAckRequired, 0);
    if (retVal != GPSAL_SUCCESS)
    {
        GPS_LOG_ERR("ERROR: hgps_init: Sending request to MCPF FAILED !!!");
        return retVal;
    }
	
	//the RXN_IntApp is never killed as it needs to be running alwasys
    //set_rxn_intapp (1);
   //send a signal to the Group indicating that the NAVD is up and running
    //kill(0,SIGUSR1);

    GPS_LOG_VER("hgps_init: Exiting Successfully");
    return 0;
}

/**
 * Function:        hgps_start.
 * Brief:           Called from android_location_GpsLocationProvider.cpp for starting navigation.
 * Description:
 * Note:            External function. GPS BaseBand will be power up.
                    Sends MCPF_NAVC_CMD_START command to MCPF.
 * Params:
 * Return:          Success: 0.
                    Failure: -1
 */
int hgps_start()
{
    eGPSAL_Result retVal = GPSAL_SUCCESS;
    eGPSAL_Bool e_isAckRequired = GPSAL_FALSE;
    int retValue;
    struct timespec expiryTime;
    struct timeval currTime;

    GPS_LOG_INF("hgps_start: Entering");

    g_TTFFflag = 0;
    g_IsStartComplete = FALSE;
    retVal = issueLocationFixRequest();
    if (GPSAL_SUCCESS != retVal)
    {
        GPS_LOG_ERR("ERROR: hgps_start: Issuing location fix FAILED !!!.");
        return -1;
    }
    /* Set the below flag to indicate location req started */
    startorlocfix = 1;
    /* Clear the used_in_fix_mask  at the begining of every session */
    gp_gpsalObject->Svstatus.used_in_fix_mask = 0;

    retValue = gettimeofday(&currTime, NULL);
    /* Calculate expiry time in timespec */
    expiryTime.tv_sec  = currTime.tv_sec;
    expiryTime.tv_nsec = currTime.tv_usec * 1000;
    expiryTime.tv_sec += EXPIRY_TIME_SECONDS;

    while(g_IsStartComplete == FALSE)
    {
        GPS_LOG_VER("hgps_start: Waiting on Start Complete");
        pthread_mutex_lock(&g_waitMutex);
        retValue = pthread_cond_timedwait( &g_waitCondVar, &g_waitMutex,  &expiryTime);
        GPS_LOG_VER("hgps_start: pthread_cond_wait returned %d", retValue);
        pthread_mutex_unlock(&g_waitMutex);
        if(retValue == ETIMEDOUT)
        {
            GPS_LOG_ERR("ERROR: hgps_start: pthread_cond_wait TIMED OUT!!!");
            break;
        }
    }

    g_IsStartComplete = DONT_CARE;

    GPS_LOG_VER("hgps_start: Send update GPS_STATUS_SESSION_BEGIN");
    g_hgps_status.status = GPS_STATUS_SESSION_BEGIN;
    gp_gpsalObject->p_callbacks->status_cb(&g_hgps_status);
    GPS_LOG_VER("hgps_start: Exiting Successfully");
    return 0;
}

/**
 * Function:        hgps_stop.
 * Brief:           Called from android_location_GpsLocationProvider.cpp to Stop Navigation.
 * Description:     Shutting Down GPS Baseband.  Sends MCPF_CMD_NAVC_STOP command to MCPF.
 * Note:            External function.
 * Params:
 * Return:          Success: 0.
                    Failure: -1
 */
int hgps_stop()
{
    static TNAVC_stopLocFix *pStopLocFix = NULL;
    int retValue;
    struct timespec expiryTime;
    struct timeval currTime;

    eGPSAL_Result retVal = GPSAL_SUCCESS;
    eGPSAL_Bool e_isAckRequired = GPSAL_FALSE;

    GPS_LOG_INF("hgps_stop: Entering");

    pStopLocFix = (TNAVC_stopLocFix *)allocateMemory(sizeof(TNAVC_stopLocFix));
    if (NULL == pStopLocFix)
    {
        GPS_LOG_ERR("ERROR: hgps_stop: Memory Allocation FAILED !!!");
        return GPSAL_ERR_NULL_PTS;
    }

    g_IsStopComplete = FALSE;
    g_IsSaveAssistComplete = FALSE;

    pStopLocFix->uSessionId = AUTONOMOUS_SESSION;
    GPS_LOG_VER("hgps_stop:  NAVC_CMD_STOP_FIX\n");
    retVal = sendRequestToMcpf(NAVC_CMD_STOP_FIX,
                               (void *)pStopLocFix,
                               e_isAckRequired,
                               sizeof(TNAVC_stopLocFix));

    freeMemory((void **)&pStopLocFix);
    if (retVal != GPSAL_SUCCESS)
    {
        GPS_LOG_ERR("ERROR: hgps_stop: Sending NAVC_CMD_STOP_FIX request to MCPF FAILED !!!");
        return retVal;
    }


    /*
    */

    retValue = gettimeofday(&currTime, NULL);
    /* Calculate expiry time in timespec */
    expiryTime.tv_sec  = currTime.tv_sec;
    expiryTime.tv_nsec = currTime.tv_usec * 1000;
    expiryTime.tv_sec += EXPIRY_TIME_SECONDS;


    while(g_IsStopComplete == FALSE)
    {
        GPS_LOG_VER("hgps_stop: Waiting on Stop Complete and Save assistance complete");
        pthread_mutex_lock(&g_waitMutex);
        retValue = pthread_cond_timedwait( &g_waitCondVar, &g_waitMutex,  &expiryTime);
        GPS_LOG_VER("hgps_stop: pthread_cond_wait returned %d", retValue);
        pthread_mutex_unlock(&g_waitMutex);

        if(retValue == ETIMEDOUT)
        {
            GPS_LOG_ERR("ERROR: hgps_stop: pthread_cond_wait TIMED OUT!!!");
            break;
        }
    }

    g_IsStopComplete = DONT_CARE;
    GPS_LOG_VER("hgps_stop:  NAVC_CMD_SAVE_ASSISTANCE\n");
    retVal = sendRequestToMcpf(NAVC_CMD_SAVE_ASSISTANCE,
                               NULL,
                               e_isAckRequired,
                               0);
    if (retVal != GPSAL_SUCCESS)
    {
        GPS_LOG_ERR("ERROR: hgps_stop: Sending NAVC_CMD_SAVE_ASSISTANCE request to MCPF FAILED !!!");
        return retVal;
    }
    retValue = gettimeofday(&currTime, NULL);
    expiryTime.tv_sec  = currTime.tv_sec;
    expiryTime.tv_nsec = currTime.tv_usec * 1000;
    expiryTime.tv_sec += EXPIRY_TIME_SECONDS;
    while(g_IsSaveAssistComplete == FALSE)
    {
        GPS_LOG_VER("hgps_stop: Waiting on Save assistance complete");
        pthread_mutex_lock(&g_waitMutex);
        retValue = pthread_cond_timedwait( &g_waitCondVar, &g_waitMutex,  &expiryTime);
        GPS_LOG_VER("hgps_stop: pthread_cond_wait returned %d", retValue);
        pthread_mutex_unlock(&g_waitMutex);
        if(retValue == ETIMEDOUT)
        {
            GPS_LOG_ERR("ERROR: hgps_stop: pthread_cond_wait TIMED OUT!!!");
            break;
        }
    }
    g_IsSaveAssistComplete = DONT_CARE;

    /* Send the status to framework. */
    GPS_LOG_VER("hgps_stop: Send update GPS_STATUS_SESSION_END");
    g_hgps_status.status = GPS_STATUS_SESSION_END;
    gp_gpsalObject->p_callbacks->status_cb(&g_hgps_status);

    GPS_LOG_VER("hgps_stop: Exiting Successfully");
    return 0;
}

/**
 * Function:        hgps_set_fix_frequency.
 * Brief:           Called from android_location_GpsLocationProvider.cpp.
 * Description:
 * Note:            External function.
 * Params:
 * Return:          Success: 0.
                    Failure: -1
 */
void hgps_set_fix_frequency(int frequency)
{
    GPS_LOG_VER("hgps_set_fix_frequency: Entering");

    GPS_LOG_VER("hgps_set_fix_frequency: Exiting Successfully");
}

/**
 * Function:        hgps_cleanup.
 * Brief:           Clean up.
 * Description:
 * Note:            External function.
 * Params:
 * Return:          Success: 0.
                    Failure: -1
 */
void hgps_cleanup(void)
{
    eGPSAL_Result retVal = GPSAL_SUCCESS;
    eGPSAL_Bool e_isAckRequired = GPSAL_FALSE;
    int res = 0, fd = -1;
    void* ignoredValue;
	char exit_cmd[] = "exit";

    GPS_LOG_INF("hgps_cleanup: Entering - do nothing");

    if(startorlocfix)
    {
        hgps_stop();
        startorlocfix = 0;
        GPS_LOG_VER("hgps_cleanup: stop the location req if exist");
	}
    if(g_suplCoreCreated == 1)
    {
        hgps_agps_deinit();
    }
    fd = open("/data/gps/navlfifo", O_CREAT |O_RDWR| O_TRUNC );
    if(fd < 0)
    {
        GPS_LOG_ERR(" hgps_stop: FIFO Opening FAILED !!! \n");
		perror("Cannot open output file\n");
        return;
    }
	retVal = sendRequestToMcpf(NAVC_CMD_STOP,
                               NULL,
                               e_isAckRequired,
                               0);
    if (retVal != GPSAL_SUCCESS)
    {
        GPS_LOG_ERR(" hgps_cleanup: Sending request NAVC_CMD_STOP to MCPF FAILED !!! \n");
        return;
    }
    if (e_isAckRequired == GPSAL_TRUE)
    {
        retVal = waitForSemaphore(gp_gpsalObject->p_semHandle);
        if (retVal != GPSAL_SUCCESS)
        {
            GPS_LOG_ERR(" hgps_cleanup: Waiting for MCPF Responses FAILED !!! \n");
            return;
        }
        retVal = handleMcpfResponse(gp_gpsalObject);
        if (retVal != GPSAL_SUCCESS)
        {
            GPS_LOG_ERR(" hgps_cleanup: Message Handling FAILED!!! \n");
            return;
        }
    }
	sleep(2);
	((TGPSAL_Object *)(gp_gpsalObject))->e_isConnectionValid = GPSAL_FALSE;

	LOGD("Deleting the thread\n");

        pthread_mutex_unlock(&g_CBWaitMutex);
        releaseSemaphore(gp_gpsalObject->p_CbSemHandle);
        pthread_join(gp_gpsalObject->p_CBthreadHandle, &ignoredValue);
        gp_gpsalObject->p_CBthreadHandle = NULL;

	GPS_LOG_VER(" hgps_cleanup: ****** closing sockets start  ******\n");
	
	pthread_join(gp_gpsalObject->p_threadHandle, &ignoredValue);
	gp_gpsalObject->p_threadHandle = NULL;
	

	GPS_LOG_VER(" hgps_cleanup: ****** closing sockets  ******\n");
    if ((gp_gpsalObject != NULL) && (((TGPSAL_Object *)(gp_gpsalObject))->u16_socketDescriptor > 0))
    {
        shutdown(((TGPSAL_Object *)(gp_gpsalObject))->u16_socketDescriptor, SHUT_RDWR);
        close(((TGPSAL_Object *)(gp_gpsalObject))->u16_socketDescriptor);
        ((TGPSAL_Object *)(gp_gpsalObject))->u16_socketDescriptor = -1;
        g_socketDescriptor = -1;
    }
	freeMemory((void **)&gp_gpsalObject->p_semHandle);
	freeMemory((void **)&gp_gpsalObject->p_CbSemHandle);
	freeMemory((void **)&gp_gpsalObject);
	startorlocfix = 0;
	usleep(500);
     if ( write(fd, exit_cmd, strlen(exit_cmd)) < 0 )
    {
        D(" hgps_cleanup: Write operation FAILED !!! \n");
        close(fd);
        return;
    }

    close(fd);

    GPS_LOG_VER(" hgps_cleanup: ****** closing socket 1 ******\n");
	shutdown(cplc_socketDescriptor, SHUT_RDWR);
    close(cplc_socketDescriptor);

	sleep(3);
    GPS_LOG_VER(" hgps_cleanup: Exiting Successfully \n");
    return;
}
void set_navl_server(int state)
{
    int fd = -1;
    char exit_cmd[] = "exit";

    GPS_LOG_VER("set_navl_server: Entering!");
    if (state == 1)
    {
        if (property_set("ctl.start", "navl_server") < 0)
        {
            GPS_LOG_ERR("set_navl_server: Failed to start navl_server\n");
            return;
        }

        /* Allow Navl server to init */
        sleep(3);
        GPS_LOG_VER("navl_server START");
		//the RXN_IntApp is never killed as it needs to be running alwasy.
		//set_rxn_intapp (1);
		//send a signal to the Group indicating that the NAVD is up and running
		//kill(0,SIGUSR1);
    }
    else if (state == 0)
    {
        /* Open the pipe for reading */
        fd = open("/data/gps/navlfifo", O_CREAT |O_RDWR| O_TRUNC );
        if(fd < 0)
        {
            GPS_LOG_ERR("set_navl_server: FIFO Opening FAILED !!!");
            perror("Cannot open output file\n");
            return;
        }

        // Write exit
        if ( write(fd, exit_cmd, strlen(exit_cmd)) < 0 )
        {
            GPS_LOG_ERR("set_navl_server: Write operation FAILED !!!");
            close(fd);
            return;
        }

        close(fd);

        GPS_LOG_VER("navl_server STOP");
    }
}


void set_rxn_intapp (int state)
{
    switch (state)
    {
        case 1:
            if (property_set ("ctl.start", "rxn_intapp") < 0) {
                LOGD("set_rxn_intapp: Failed to start rxn_intapp!");                    
                return;
            }                
            LOGD("set_rxn_intapp: Start Success...");
            break;

        case 0:
            if (property_set ("ctl.stop", "rxn_intapp") < 0) {
                LOGD("set_rxn_intapp: Failed to stop rxn_intapp!");                    
                return;
            }                
            LOGD("set_rxn_intapp: Stop Success...");
            break;

        default:
            LOGD("set_rxn_intapp: Invalid Option: %d", state);                    
    }                
}      

/**
 * Function:        hgps_inject_time.
 * Brief:           Inject Time.
 * Description:
 * Note:            External function.
 * Params:
 * Return:          Success: 0.
                    Failure: -1
 */
int hgps_inject_time(GpsUtcTime time, int64_t timeReference, int uncertainty)
{
    GPS_LOG_INF("hgps_inject_time: Entering");

    GPS_LOG_VER("hgps_inject_time: Exiting Successfully");
    return 0;
}

/*
 *
 * Inject Current Location from other location provider ( typically Cell ID ).
 *
 */
int hgps_inject_location(double latitude, double  longitude, float accuracy)
{
    GPS_LOG_INF("hgps_inject_location: Entering");

    GPS_LOG_VER("hgps_inject_location: Exiting Successfully");
    return 0;
}

/**
 * Function:        hgps_delete_aiding_data.
 * Brief:           Delete Assistance Data.
 * Description:     To send delete request for aiding information i.e. almanac,ephemeris etc.
 * Note:            External function.
                    TBD: In MCPF/NAVC there is no definition for different bits of uDelAssistBitmap.
                    typedef struct
                    {
                        McpU8       uDelAssistBitmap;
                        McpU32      uSvBitmap;
                    } TNAVC_delAssist;
 * Params:          flags - Bitmap
 * Return:          Success: 0.
                    Failure: -1
 */
void hgps_delete_aiding_data(GpsAidingData flags)
{
    eGPSAL_Result retVal = GPSAL_SUCCESS;
    eGPSAL_Bool e_isAckRequired = GPSAL_FALSE;

    TNAVC_delAssist *pDelAssist = NULL;

    GPS_LOG_INF("hgps_delete_aiding_data: Entering");
    GPS_LOG_INF("hgps_delete_aiding_data: incoming delete bitmask = %x\n",flags);

    /* ####### Del EPH ####### */
    if (((flags & GPS_DELETE_ALL) == GPS_DELETE_ALL) ||
        (flags & GPS_DELETE_EPHEMERIS))
    {
        pDelAssist = (TNAVC_delAssist*)allocateMemory(sizeof(TNAVC_delAssist));
        if (NULL == pDelAssist)
        {
            GPS_LOG_ERR("ERROR: hgps_delete_aiding_data: 1 Memory Allocation FAILED !!!");
        }
        else
        {
            pDelAssist->uDelAssistBitmap = 0;
            pDelAssist->uDelAssistBitmap |= DEL_AIDING_EPHEMERIS;

            pDelAssist->uSvBitmap = 0;

            retVal = sendRequestToMcpf(NAVC_CMD_DELETE_ASISTANCE,
                                       (void *)pDelAssist,
                                       e_isAckRequired,
                                       sizeof(TNAVC_delAssist));

            freeMemory((void **)&pDelAssist);

            if (retVal != GPSAL_SUCCESS)
            {
                GPS_LOG_ERR("ERROR: hgps_delete_aiding_data: 1 Sending request to MCPF FAILED !!!");
            }
        }
    }

    /* ####### Del ALM ####### */
    if (((flags & GPS_DELETE_ALL) == GPS_DELETE_ALL) ||
        (flags & GPS_DELETE_ALMANAC))
    {
        pDelAssist = (TNAVC_delAssist*)allocateMemory(sizeof(TNAVC_delAssist));
        if (NULL == pDelAssist)
        {
            GPS_LOG_ERR("ERROR: hgps_delete_aiding_data: 2 Memory Allocation FAILED !!!");
        }
        else
        {
            pDelAssist->uDelAssistBitmap = 0;
            pDelAssist->uDelAssistBitmap |= DEL_AIDING_ALMANAC;

            pDelAssist->uSvBitmap = 0;

            retVal = sendRequestToMcpf(NAVC_CMD_DELETE_ASISTANCE,
                                       (void *)pDelAssist,
                                       e_isAckRequired,
                                       sizeof(TNAVC_delAssist));

            freeMemory((void **)&pDelAssist);

            if (retVal != GPSAL_SUCCESS)
            {
                GPS_LOG_ERR("ERROR: hgps_delete_aiding_data: 2 Sending request to MCPF FAILED !!!");
            }
        }
    }

    /* ####### Del Other ####### */
    pDelAssist = (TNAVC_delAssist*)allocateMemory(sizeof(TNAVC_delAssist));
    if (NULL == pDelAssist)
    {
        GPS_LOG_ERR("ERROR: hgps_delete_aiding_data: 3 Memory Allocation FAILED !!!");
    }
    else
    {
        pDelAssist->uDelAssistBitmap = 0;
        pDelAssist->uSvBitmap = 0;

        populateDeleteBitmap(pDelAssist, flags);

        retVal = sendRequestToMcpf(NAVC_CMD_DELETE_ASISTANCE,
                                   (void *)pDelAssist,
                                   e_isAckRequired,
                                   sizeof(TNAVC_delAssist));

        freeMemory((void **)&pDelAssist);

        if (retVal != GPSAL_SUCCESS)
        {
            GPS_LOG_ERR("ERROR: hgps_delete_aiding_data: 3 Sending request to MCPF FAILED !!!");
        }
    }

    /* Wait and process MCPF's responses from here. */
    if (e_isAckRequired == GPSAL_TRUE)
    {
        retVal = waitForSemaphore(gp_gpsalObject->p_semHandle);
        if (retVal != GPSAL_SUCCESS)
        {
            GPS_LOG_ERR("ERROR: hgps_delete_aiding_data: Waiting for MCPF Responses FAILED !!!");
            //return retVal;
        }

        retVal = handleMcpfResponse(gp_gpsalObject);
        if (retVal != GPSAL_SUCCESS)
        {
            GPS_LOG_ERR("ERROR: hgps_delete_aiding_data: Message Handling FAILED!!!");
            //return retVal;
        }
    }

    GPS_LOG_VER("hgps_delete_aiding_data(): Exiting Successfully");
    //return 0;
}

/**
 * Function:        hgps_set_position_mode.
 * Brief:
 * Description:     Setting kind of GPS Mode ( StandAlone, MS-Based and MS-Assisted ) and Frequency of fix
 * Note:            External function.
 * Params:          mode - Fix Mode.
                    fix_frequency - Fix frequency.
 * Return:          Success: 0.
                    Failure: -1
 */
int hgps_set_position_mode(GpsPositionMode mode, GpsPositionRecurrence recurrence, uint32_t fix_frequency,
                           uint32_t preferred_accuracy, uint32_t preferred_time)
{
    TSUPLC_FixMode *p_locationFixMode = NULL;

    eGPSAL_Result retVal = GPSAL_SUCCESS;
    eGPSAL_Bool e_isAckRequired = GPSAL_FALSE;
    TNAVC_updateFixRate *s_update_fix_rate = NULL;

    GPS_LOG_INF("hgps_set_position_mode: Entering");
    GPS_LOG_INF("hgps_set_position_mode: update_rate   [%d]\n",fix_frequency);

    /* Convert fix_frequency from milli-second to second */
    LOGD(" hgps_set_position_mode: Entering %d\n",fix_frequency);
    	if(fix_frequency == 0){
	fix_frequency = 1;
	}
	else{
	fix_frequency = fix_frequency/1000; /* convert to seconds */
	}
    LOGD("hgps_set_position_mode: update_rate   [%d]\n",fix_frequency);

    p_locationFixMode = (TSUPLC_FixMode *)allocateMemory(sizeof(TSUPLC_FixMode));
    if (NULL == p_locationFixMode)
    {
        GPS_LOG_ERR("ERROR: hgps_set_position_mode: Memory Allocation FAILED !!!");
        return GPSAL_ERR_NULL_PTS;
    }

    /* ===>>> NOTE: Temporary Hack. */
    if (g_locFixMode == 3)
         mode = GPS_POSITION_MODE_AUTOSUPL;
    else if (g_locFixMode == 2)
         mode = GPS_POSITION_MODE_MS_ASSISTED;
    else if (g_locFixMode == 1)
       mode = GPS_POSITION_MODE_MS_BASED;
    else if(g_locFixMode == 0)
        mode = GPS_POSITION_MODE_STANDALONE;

    /* Fill position mode structure. */
    switch(mode)
    {
        case GPS_POSITION_MODE_AUTOSUPL:
               g_e_locationFixMode = GPSC_FIXMODE_AUTOSUPL;
               p_locationFixMode->e_locFixMode = SUPLC_FIXMODE_AUTONOMOUS;
        break;

        case GPS_POSITION_MODE_MS_BASED:
               g_e_locationFixMode = GPSC_FIXMODE_MSBASED;
               p_locationFixMode->e_locFixMode = SUPLC_FIXMODE_MSBASED;
               g_e_Call_type = NAVC_SUPL_MO;
        break;

        case GPS_POSITION_MODE_MS_ASSISTED:
               g_e_locationFixMode = GPSC_FIXMODE_MSASSISTED;
               p_locationFixMode->e_locFixMode = SUPLC_FIXMODE_MSASSISTED;
               g_e_Call_type = NAVC_SUPL_MO;
        break;

        case GPS_POSITION_MODE_STANDALONE:
        default:
               g_e_locationFixMode = GPSC_FIXMODE_AUTONOMOUS;
               p_locationFixMode->e_locFixMode = SUPLC_FIXMODE_AUTONOMOUS;
               g_e_Call_type = NAVC_STANDLONE;
        break;
    }

    g_s32_fixFrequency = (McpU32)(fix_frequency);
    p_locationFixMode->u16_fixFrequency = (McpU16)g_s32_fixFrequency;

    if (g_locFixMode > 0)
    {
         /* Send request to SUPLC. */
         retVal = sendRequestToMcpf(SUPLC_CMD_SET_MODE,
                                    (void *)p_locationFixMode,
                                    e_isAckRequired,
                                    sizeof(TSUPLC_FixMode));
    }

    freeMemory((void **)&p_locationFixMode);
    if (retVal != GPSAL_SUCCESS)
    {
        GPS_LOG_ERR("ERROR: hgps_set_position_mode: Sending Location Fix Mode MCPF FAILED !!!");
        return retVal;
    }
    s_update_fix_rate = (TNAVC_updateFixRate *)allocateMemory(sizeof(TNAVC_updateFixRate));

    if (s_update_fix_rate == NULL)
    {
        GPS_LOG_ERR("ERROR: hgps_set_position_mode: s_update_fix_rate Memory initialization FAILED !!!");
        return retVal;
    }

    s_update_fix_rate->update_rate = (McpU32)(fix_frequency);
    s_update_fix_rate->loc_sessID  = g_locSessId;

    /* Send request to NAVC. */

    if(s_update_fix_rate->loc_sessID != 0)
    {
        retVal = sendRequestToMcpf(NAVC_CMD_QOP_UPDATE,
                                   (void *)s_update_fix_rate,
                                    e_isAckRequired,
                                    sizeof(TNAVC_updateFixRate));

        freeMemory((void **)&s_update_fix_rate);

        if (retVal != GPSAL_SUCCESS)
        {
            GPS_LOG_ERR("ERROR: hgps_set_position_mode: Sending NAVC_CMD_QOP_UPDATE to MCPF FAILED !!!");
            return retVal;
        }

        GPS_LOG_VER("hgps_set_position_mode: NAVC_CMD_QOP_UPDATE send to NAVC");
    }

    GPS_LOG_VER("hgps_set_position_mode: Exiting Successfully");
    return 0;
}

/**
 * Function:        hgps_get_extension.
 * Brief:           This function is called from GPS_LP_JNI for getting
                    any extra interfaces supported by GPS_AL.
 * Description:
 * Note:            External function.
 * Params:          name: Type of interface requested.
                    (For ex: GPS_SUPL_INTERFACE or GPS_XTRA_INTERFACE)
 * Return:          void* - Handle to required interface is supported.
                            NULL otherwise.
 */
const void* hgps_get_extension(const char* name)
{
    McpU8 comparisionResult = 0;
    GPS_LOG_INF("hgps_get_extension: Entering");
    GPS_LOG_INF("hgps_get_extension: name   [%s] \n", name);

    /* Include SUPL Support. */
    comparisionResult = strcmp(name, AGPS_INTERFACE);
    if (comparisionResult == 0)
    {
        GPS_LOG_VER("hgps_get_extension: Returning AGPSInterface\n");
        return (void *)&AGPSInterface;
    }

    /* Include NI Support. */
    comparisionResult = strcmp(name, GPS_NI_INTERFACE);
    if (comparisionResult == 0)
    {
        GPS_LOG_VER("hgps_get_extension: Returning NI Interface\n");
        return (void *)&NIInterface;
    }

    /* No XTRA Interface Support Included. */
    comparisionResult = strcmp(name, GPS_XTRA_INTERFACE);
    if (comparisionResult == 0)
    {
        /* */
        return NULL;
    }

    GPS_LOG_VER("hgps_get_extension: Exiting Successfully");
    return NULL;
}

/**
 * Function: hgps_agps_init
 */
void hgps_agps_init(AGpsCallbacks *agpscb)
{
    eGPSAL_Bool e_isAckRequired = GPSAL_FALSE;
    eGPSAL_Result retVal = GPSAL_SUCCESS;

    GPS_LOG_INF("hgps_agps_init: Entering");
    if (gp_gpsalObject == NULL)
    {
        GPS_LOG_ERR("ERROR: hgps_agps_init: SUPL NOT Created");
        return;
    }
    if (!g_suplCoreCreated)
    {
        GPS_LOG_VER("Beofre sending SUPLC_CMD_INIT_CORE\n");
        retVal = sendRequestToMcpf(SUPLC_CMD_INIT_CORE,
                                   NULL,
                                   e_isAckRequired,
                                   0);
        if (retVal != GPSAL_SUCCESS)
        {
            GPS_LOG_ERR("ERROR: hgps_agps_init: Sending request SUPLC_CMD_INIT_CORE to MCPF FAILED !!!");
            return;
        }
        GPS_LOG_VER("After sending SUPLC_CMD_INIT_CORE\n");
    }
    g_suplCoreCreated = 1;

    gp_gpsalObject->p_agpsCallback = agpscb;

    /* Send the status to framework. */
    g_aGpsStatus.type = AGPS_TYPE_SUPL;
    g_aGpsStatus.status = GPS_AGPS_DATA_CONNECTED;
    GPS_LOG_VER("hgps_agps_init: Before calling status callback\n");
    gp_gpsalObject->p_agpsCallback->status_cb(&g_aGpsStatus);

    GPS_LOG_VER("hgps_agps_init: SUPL Successfully Created\n");

    /* Removed here and added at hgps_init() */
    return;
}

/**
 * Function: hgps_agps_deinit
 */
void hgps_agps_deinit(void)
{
    eGPSAL_Bool e_isAckRequired = GPSAL_FALSE;
    eGPSAL_Result retVal = GPSAL_SUCCESS;

    GPS_LOG_VER("hgps_agps_deinit");

    if (gp_gpsalObject == NULL)
    {
        GPS_LOG_ERR("ERROR: hgps_agps_deinit: SUPL NOT Created");
        return;
    }

    if (g_suplCoreCreated)
    {
        retVal = sendRequestToMcpf(SUPLC_CMD_DEINIT_CORE,
                                   NULL,
                                   e_isAckRequired,
                                   0);
        if (retVal != GPSAL_SUCCESS)
        {
            GPS_LOG_ERR("ERROR: hgps_agps_deinit: Sending request SUPLC_CMD_DEINIT_CORE to MCPF FAILED !!!");
            return;
        }
    }
    g_suplCoreCreated = 0;

    /* Send the status to framework. */
    g_aGpsStatus.type = NULL;
    g_aGpsStatus.status = NULL;
    gp_gpsalObject->p_agpsCallback->status_cb(&g_aGpsStatus);

    GPS_LOG_VER("hgps_agps_deinit: SUPL Successfully Destroyed\n");
    return;
}

/**
 * Function: readData
 */
int readData(const TGPSAL_Object *const p_inGpsalObject)
{
    int bytesRead = 0;
    GpsNiNotification *ni;

    GPS_LOG_VER("readData: Entering");
    ni = (GpsNiNotification *)allocateMemory(sizeof(GpsNiNotification));
    if ( (ni == NULL) )
    {
        GPS_LOG_ERR("ERROR: readData: Memory Allocation FAILED !!!");
        return 1;
    }

    GPS_LOG_VER("Sizeof GpsNiNotification = %d\n",sizeof(GpsNiNotification));
    bytesRead = read(clientSockFD, (GpsNiNotification *)ni, sizeof(GpsNiNotification));
    if (bytesRead < 0)
    {
        perror("Read Failed");
        GPS_LOG_ERR("ERROR: readData: ERRNO = %d\n",errno);
        GPS_LOG_ERR("ERROR: readData: read FAILED !!!");
        return 1;
    }
    GPS_LOG_VER("readData: Bytes read = %d\n",bytesRead);

#if 0
    ni->size = sizeof(GpsNiNotification);
    ni->notification_id = 100;
    ni->ni_type = GPS_NI_TYPE_UMTS_SUPL;
    ni->notify_flags = GPS_NI_NEED_VERIFY | GPS_NI_NEED_NOTIFY;
    ni->timeout = 0;
    ni->default_response = GPS_NI_RESPONSE_NORESP;
    strcpy(ni->requestor_id, "100");
    strcpy(ni->text, "1000");
    ni->requestor_id_encoding = GPS_ENC_NONE;
    ni->text_encoding = GPS_ENC_NONE;
#endif

    pthread_mutex_lock (&ni_state.mutex_lock);
    if (ni_state.in_progress == 0)
    {
        ni_state.in_progress = 1;
        ni_state.notif_id = ni->notification_id;
        ni_state.response_time_left = ni->timeout;
        GPS_LOG_VER("readData: NI Time Out set to %d\n", ni_state.response_time_left);
        /* Call the JNI Call BAck Function */
        gp_gpsalObject->p_niCallback->notify_cb(ni);
    }
    else {
        GPS_LOG_VER("readData: NI Notification (ID=%d) already in progess! Ignoring!!! \n", ni_state.notif_id);
    }
    pthread_mutex_unlock (&ni_state.mutex_lock);
    GPS_LOG_VER("readData: Exiting Successfully.");
    return 0;
}

/**
 * Function: GPSALRecv_ThreadFunc
 */
static void GPSALRecv_ThreadFunc(void *handler)
{
    TGPSAL_Object *p_gpsalObject = NULL;

    struct sockaddr_un serverAddress;
    struct sockaddr_un clientAddress;

    socklen_t clientAddressLength;
    struct hostent *p_host;
    int bytesRead = 0;
    int retVal = 1;

    p_gpsalObject = (TGPSAL_Object *)handler;
    /* Just in case... */
    if (p_gpsalObject == NULL)
    {
        GPS_LOG_ERR("ERROR: GPSALRecv_ThreadFunc: NULL Pointers.");
        return;
    }


    /* Clear the structure. */
    memset( &serverAddress, 0, sizeof(serverAddress) );

    /* Set address type. */
    serverAddress.sun_family = AF_UNIX;
    strcpy(serverAddress.sun_path, SOC_NAME_6121);


    /* Create a new socket. */
    cplc_socketDescriptor = socket(AF_UNIX, SOCK_STREAM, 0);
    if (cplc_socketDescriptor < 0)
    {
        GPS_LOG_ERR("ERROR: GPSALRecv_ThreadFunc: Socket creation FAILED !!!");
        return;
    }

    /* Bind. */
    if ( bind( cplc_socketDescriptor, (struct sockaddr *) &serverAddress,
                sizeof(serverAddress)) < 0 )
    {
        GPS_LOG_ERR("ERROR: GPSALRecv_ThreadFunc: bind FAILED !!!");
        perror("Failed");
        GPS_LOG_ERR("ERRNO = %d\n",errno);

        close(cplc_socketDescriptor);
        return;
    }

    /* Listen. */
    if ( listen( cplc_socketDescriptor, MAX_CONNECTIONS) < 0 )
    {
        GPS_LOG_ERR("ERROR: GPSALRecv_ThreadFunc: listen FAILED !!!");
        close (cplc_socketDescriptor);
        return;
    }
    do
    {
        /* Accept a connection. */
        GPS_LOG_VER("GPSALRecv_ThreadFunc:Waiting for connection.....\n");
        clientAddressLength = sizeof(clientAddress);
        clientSockFD = accept( cplc_socketDescriptor,
                               (struct sockaddr *) &clientAddress,
                               &clientAddressLength);
        if ( clientSockFD < 0)
        {
            GPS_LOG_ERR("ERROR: GPSALRecv_ThreadFunc: accept FAILED !!!");
            close(cplc_socketDescriptor);
            return;
        }

        /* Read data. */
        retVal = readData(p_gpsalObject);
        if (0 != retVal)
        {
            GPS_LOG_ERR("ERROR: GPSALRecv_ThreadFunc: readData FAILED !!!");
            close(clientSockFD);
            continue;
        }

        /* Close the client socket only after sending response*/
        //GPS_LOG_VER("GPSALRecv_ThreadFunc: Closing socket %d \n", clientSockFD);
        //close(clientSockFD);

    }while (1);
    //Just in case
    return;
}

/**
 * Function:        hgps_ni_init.
 * Brief:
 * Description:
 * Note:            External function.
 * Params:          data -
 length -
 * Return:          Void.
 */
void hgps_ni_init(GpsNiCallbacks* nicb)
{
    pthread_t thread_GPSAL;

    GPS_LOG_INF("hgps_ni_init: Entering");

    pthread_mutex_init (&ni_state.mutex_lock, NULL);
    ni_state.notif_id = -1;
    ni_state.in_progress = 0;
    ni_state.response_time_left = 0;

    gp_gpsalObject->p_niCallback = nicb;
    if(gp_gpsalObject->p_nithreadHandle == NULL)
    {
       gp_gpsalObject->p_nithreadHandle = gp_gpsalObject->p_callbacks->create_thread_cb("GPSAL_Thread", GPSALRecv_ThreadFunc, (void *)gp_gpsalObject);
       GPS_LOG_VER("gp_gpsalObject->p_nithreadHandle created once during init");
    }

    GPS_LOG_VER("hgps_ni_init: Exiting Successfully");
}

/**
 * Function:        hgps_ni_response.
 * Brief:
 * Description:
 * Note:            External function.
 * Params:          data -
 length -
 * Return:          Void.
 */
void hgps_ni_response(int notifyID, int response)
{
    GPS_LOG_INF("hgps_ni_response: Entering");

    pthread_mutex_lock (&ni_state.mutex_lock);
    ni_state.notif_id = -1;
    ni_state.response_time_left = 0;
    ni_state.in_progress = 0;
    pthread_mutex_unlock (&ni_state.mutex_lock);

    switch(response)
    {
        case NI_RESPONSE_ACCEPT:
        {
            GPS_LOG_INF("hgps_ni_response: User pressed Accept\n");
        }
        break;

        case NI_RESPONSE_DENY:
        {
            GPS_LOG_INF("hgps_ni_response: User pressed Deny\n");
        }
        break;

        case NI_RESPONSE_NORESP:
        {
            GPS_LOG_INF("hgps_ni_response: No response from user/timeout\n");
        }
        break;
    }

    if ( write(clientSockFD, &response, sizeof(int) ) < 0)
    {
        GPS_LOG_ERR("ERROR: hgps_ni_response: Message Sending FAILED !!!");
        close(clientSockFD);
    }
    else{
        GPS_LOG_VER("hgps_ni_response: Closing socket %d \n", clientSockFD);
        close(clientSockFD);
    }

    GPS_LOG_VER("hgps_ni_response: Exiting Successfully");
}

/**
 * Function:        hgps_supl_set_apn.
 * Brief:
 * Description:
 * Note:            External function.
 * Params:
 * Return:          Success: 0.
                    Failure: -1.
 */
int hgps_supl_set_apn(const char* apn)
{
    GPS_LOG_VER("hgps_supl_set_apn: Entering");

    GPS_LOG_VER("hgps_supl_set_apn: Exiting Successfully");
    return 0;
}

/**
 * Function:        hgps_xtra_init.
 * Brief:
 * Description:
 * Note:            External function.
 * Params:
 * Return:          Success: 0.
                    Failure: -1.
 */
int hgps_xtra_init(GpsXtraCallbacks* callbacks)
{
    GPS_LOG_INF("hgps_xtra_init: Entering");

    GPS_LOG_VER("hgps_xtra_init: Exiting Successfully");
    return 0;
}

/**
 * Function:        hgps_xtra_data.
 * Brief:
 * Description:
 * Note:            External function.
 * Params:          data -
                    length -
 * Return:          Success: 0.
                    Failure: -1.
 */
int hgps_xtra_data(char *data, int length)
{
    GPS_LOG_INF("hgps_xtra_data: Entering");

    GPS_LOG_VER("hgps_xtra_data: Exiting Successfully");
    return 0;
}

/**
 * Function:        connectWithMcpfSocket
 * Brief:           Creates a socket and connects with it.
 * Description:     A socket is exposed by MCPF for external applications to communicate
                    with it. GPS_AL writes the requests on this socket.
 * Note:            Internal function.
 * Params:          u16_inPortNumber - Port Number.
                    p_inHostName - Host Name.
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static eGPSAL_Result connectWithMcpfSocket(const McpU16 u16_inPortNumber,
                                           const McpU8 *const p_inHostName)
{
    struct sockaddr_un serverAddress;       /* Internet Address of Server. */

    struct hostent *p_host = NULL;          /* The host (server) info. */
    McpS16 u16_sockDescriptor = -1;
    McpS8 retVal = 0;

    GPS_LOG_VER("connectWithMcpfSocket: Entering");

    /* Obtain host information. */
    p_host = gethostbyname((char *)p_inHostName);
    if (p_host == (struct hostent *) NULL )
    {
        GPS_LOG_ERR("ERROR: connectWithMcpfSocket: gethostbyname FAILED !!!");
        return GPSAL_ERR_UNKNOWN_HOST;
    }

    /* Clear the structure. */
    memset( &serverAddress, 0, sizeof(serverAddress) );

    serverAddress.sun_family = AF_UNIX;
    strcpy(serverAddress.sun_path, SOC_NAME_4121);


    /* Create a new socket. */
    u16_sockDescriptor = socket(AF_UNIX, SOCK_STREAM, 0);

    if (u16_sockDescriptor < 0)
    {
        GPS_LOG_ERR("ERROR: connectWithMcpfSocket: Socket creation FAILED !!!");
        return GPSAL_ERR_SOCK_CREATION;
    }

    /* Connect with MCPF. */
    retVal = connect(u16_sockDescriptor,
                     (struct sockaddr *)&serverAddress,
                     sizeof(serverAddress) );
    if (retVal < 0)
    {
        GPS_LOG_ERR("ERROR: connectWithMcpfSocket: Connection with MCPF FAILED !!!");
        close(u16_sockDescriptor);
        return GPSAL_ERR_CONNECTION_FAILED;
    }

    /* Maintain a global variable for keeping the socket descriptor. */
    g_socketDescriptor = u16_sockDescriptor;

    GPS_LOG_VER("connectWithMcpfSocket: Exiting Successfully.");
    return GPSAL_SUCCESS;
}

/**
 * Function:        sendRequestToMcpf
 * Brief:           Sends requests to MCPF.
 * Description:
 * Note:            Internal function.
 * Params:          e_reqCmd - Commands to be sent.
                    p_inData - Payload.
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static eGPSAL_Result sendRequestToMcpf(const McpU16 e_reqCmd,
                                       const void *const p_inData,
                                       const eGPSAL_Bool e_isAckRequired,
                                       McpU32 u32_msgSize)
{
    hostSocket_header_t s_MsgHdr;
    eGPSAL_Result retVal = GPSAL_SUCCESS;
    McpU8 uDestQueId = 0;

    void *p_payload = NULL;

    GPS_LOG_VER("sendRequestToMcpf: Entering");
    if (gp_gpsalObject == NULL)
    {
        GPS_LOG_ERR("ERROR: sendRequestToMcpf: gp_gpsalObject NULL");
        retVal = GPSAL_FAIL;
        return retVal;
    }

    retVal = initializeMemory((void *)&s_MsgHdr, sizeof(hostSocket_header_t));
    if (GPSAL_SUCCESS != retVal)
    {
        GPS_LOG_ERR("ERROR: sendRequestToMcpf: Memory initialization FAILED !!!");
        return retVal;
    }

    gp_gpsalObject->e_isAckRequired = e_isAckRequired;

    switch(e_reqCmd)
    {
        case NAVC_CMD_START:
        case NAVC_CMD_STOP:
        {
            /* Send the request. */
            s_MsgHdr.msgClass = NAVL_CLASS_INTERNAL;
            s_MsgHdr.opCode = e_reqCmd;
            s_MsgHdr.payloadLen =  u32_msgSize;
            s_MsgHdr.syncSrart = HOSTSOCKET_MESSAGE_SYNC_START;
        }
        break;

        case NAVC_CMD_REQUEST_FIX:
        case NAVC_CMD_STOP_FIX:
        case NAVC_CMD_DELETE_ASISTANCE:
        case NAVC_CMD_SET_APM_PARAMS:
        case NAVC_CMD_SAVE_ASSISTANCE:
        case NAVC_CMD_QOP_UPDATE:
        {
            /* Send the request. */
            s_MsgHdr.msgClass = NAVL_CLASS_GPS;
            s_MsgHdr.opCode = e_reqCmd;
            s_MsgHdr.payloadLen =  u32_msgSize;
            s_MsgHdr.syncSrart = HOSTSOCKET_MESSAGE_SYNC_START;

            p_payload = (void *)p_inData;

        }
        break;

        case SUPLC_CMD_SET_MODE:
        case SUPLC_CMD_INIT_CORE:
        case SUPLC_CMD_EE_ACTIVATE:
        {
            /* Send the request. */
            s_MsgHdr.msgClass = NAVL_CLASS_SUPL;
            s_MsgHdr.opCode = e_reqCmd;
            s_MsgHdr.payloadLen =  u32_msgSize;
            s_MsgHdr.syncSrart = HOSTSOCKET_MESSAGE_SYNC_START;

            p_payload = (void *)p_inData;
        }
        break;

        case SUPLC_CMD_DEINIT_CORE:
        {
            GPS_LOG_VER("sendRequestToMcpf: SUPLC_CMD_DEINIT_CORE");
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
            GPS_LOG_ERR("ERROR: sendRequestToMcpf: Unhandled REQ CMD");
        }
        break;
    }

    /* Just in case... */
    if (g_socketDescriptor == 0 || g_socketDescriptor == -1 )
    {
        GPS_LOG_ERR("ERROR: sendRequestToMcpf: Connection with MCPF not yet established.");
        return GPSAL_ERR_NO_CONNECTION;
    }

    /* Send the command to MCPF. */
    GPS_LOG_VER("sendRequestToMcpf: g_socketDescriptor = %d \n", g_socketDescriptor);
    if ( write(g_socketDescriptor, (void *)&s_MsgHdr, sizeof(s_MsgHdr) ) < 0)
    {
        GPS_LOG_ERR("ERROR: sendRequestToMcpf: Message Sending FAILED !!!");
        return GPSAL_ERR_WRITE_FAILED;
    }

    /* Send payload if any. */
    if (u32_msgSize)
    {
        if ( write(g_socketDescriptor, (void *)p_payload, u32_msgSize ) < 0)
        {
            GPS_LOG_ERR("ERROR: sendRequestToMcpf: Payload Sending FAILED !!!");
            return GPSAL_ERR_WRITE_FAILED;
        }
    }

    GPS_LOG_VER("sendRequestToMcpf: Exiting Successfully.");
    return retVal;
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
    GPS_LOG_VER("allocateMemory: Entering.");

    GPS_LOG_VER("allocateMemory: Exiting Successfully.");
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
 * Function:        createSemaphore
 * Brief:           Creates unnamed semaphore.
 * Description:
 * Note:            Internal function.
 * Params:          p_semHandle - Semaphore Handle.
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static eGPSAL_Result createSemaphore(SEM_HANDLE p_semHandle)
{
    McpS8 retVal = 0;
    GPS_LOG_VER("createSemaphore: Entering.");

    /* Create an unnamed semaphore */
    retVal = sem_init(p_semHandle,                 /* p_semHandle. */
                      0,                           /* Not shared between processes */
                      0);                          /* Not available */
    if (retVal < 0)
    {
        GPS_LOG_ERR("ERROR: createSemaphore: Exiting Successfully.");
        return GPSAL_ERR_SEM_CREATE_FAILED;
    }

    GPS_LOG_VER("createSemaphore: Exiting Successfully. p_semHandle = %x \n", p_semHandle);
    return GPSAL_SUCCESS;
}

#if 0
/**
 * Function:        createResponseHandler
 * Brief:           Creates a thread for waiting on responses from MCPF.
 * Description:
 * Note:            Internal function.
 * Params:          p_inGpsalObject - GPSAL Object Handle.
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static eGPSAL_Result createResponseHandler(const TGPSAL_Object *const p_inGpsalObject)
{
    McpS8 retVal = 0;
    GPS_LOG_VER("createResponseHandler: Entering.");

    retVal = pthread_create((pthread_t *)&p_inGpsalObject->p_threadHandle,   /* Thread Handle. */
                            NULL,                               /* Default Atributes. */
                            entryFunctionForHandler,            /* Entry Function. */
                            (void *)p_inGpsalObject);           /* Parameters. */
    if (retVal < 0)
    {
        GPS_LOG_ERR("ERROR: createResponseHandler: Thread Creation FAILED !!!");
        return GPSAL_ERR_THREAD_CREATION_FAILED;
    }

    GPS_LOG_VER("createResponseHandler: Exiting Successfully.");
    return GPSAL_SUCCESS;
}
#endif

/**
 * Function:        gpsResponseHandler
 * Brief:           Creates a thread for sending responses back to Android location manager.
 * Description:
 * Note:            Internal function.
 * Params:          p_inGpsalObject - GPSAL Object Handle.
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
**/
static void gpsResponseHandler(void *data)
{
   eGPSAL_Result retVal = GPSAL_SUCCESS;
   TGPSAL_Object *p_gpsalObject = (TGPSAL_Object *)data;

   GPS_LOG_VER("gpsResponseHandler: Entering.");
   while((p_gpsalObject != NULL) && (p_gpsalObject->e_isConnectionValid != GPSAL_FALSE))
   {
      /* Wait for the semaphore */
      retVal = waitForSemaphore(p_gpsalObject->p_CbSemHandle);
      if(retVal != GPSAL_SUCCESS)
      {
         GPS_LOG_ERR("ERROR: gpsResponseHandler: Waiting for p_CbSemHandle sem failed.");
         break;
      }
      pthread_mutex_lock(&g_CBWaitMutex);
      if (p_gpsalObject->u8_cbType & STATUS_CB)
      {
          GPS_LOG_VER("gpsResponseHandler: status_cb");
          p_gpsalObject->p_callbacks->status_cb(&g_hgps_status);
          p_gpsalObject->u8_cbType &= ~STATUS_CB;
      }
      if (p_gpsalObject->u8_cbType & NMEA_CB)
      {
          GPS_LOG_VER("gpsResponseHandler: nmea_cb");
          p_gpsalObject->p_callbacks->nmea_cb(1, &p_gpsalObject->nmea_buff[0], strlen(p_gpsalObject->nmea_buff));
          p_gpsalObject->u8_cbType &= ~NMEA_CB;
      }
      if (p_gpsalObject->u8_cbType & SV_STATUS_CB)
      {
          GPS_LOG_VER("gpsResponseHandler: sv_status_cb");
          p_gpsalObject->p_callbacks->sv_status_cb(&s_gpsSvStatus);
          p_gpsalObject->u8_cbType &= ~SV_STATUS_CB;
      }
      if (p_gpsalObject->u8_cbType & LOCATION_CB)
      {
          GPS_LOG_VER("gpsResponseHandler: location_cb");
          p_gpsalObject->p_callbacks->location_cb(&s_gpsLocation);
          p_gpsalObject->u8_cbType &= ~LOCATION_CB;
      }
      /* Signal call back completion */
      SignalCBCompletion(); 
      pthread_mutex_unlock(&g_CBWaitMutex);
   }
   GPS_LOG_VER("gpsResponseHandler: Exiting.");
}

/**
 * Function:        entryFunctionForHandler
 * Brief:           Creates a thread for waiting on responses from MCPF.
 * Description:
 * Note:            Internal function.
 * Params:          p_inGpsalObject - GPSAL Object Handle.
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
**/
static void entryFunctionForHandler(void *inputHandler)
{
    //TGPSAL_Object *p_gpsalObject = NULL;
    eGPSAL_Result retVal = GPSAL_SUCCESS;

    hostSocket_header_t s_msgHdr;

    McpU8 *p_readBuffer = NULL;
    McpS32 s32_bytesRead = 0;

    GPS_LOG_VER("entryFunctionForHandler: Entering.");

    retVal = initializeMemory((void *)&s_msgHdr, sizeof(hostSocket_header_t));
    if (GPSAL_SUCCESS != retVal)
    {
        GPS_LOG_ERR("ERROR: entryFunctionForHandler: Memory initialization FAILED !!!");
        return;
    }

    //p_gpsalObject = (TGPSAL_Object *)inputHandler;
    /* Just in case... */
    if (gp_gpsalObject == NULL)
    {
        GPS_LOG_ERR("ERROR: entryFunctionForHandler: NULL Pointers.");
        GPS_LOG_ERR("ERROR: entryFunctionForHandler: *** Terminate Response Handler ***");
        return;
    }

    // while(p_gpsalObject->e_isConnectionValid != GPSAL_FALSE)
    while((gp_gpsalObject != NULL) && (gp_gpsalObject->e_isConnectionValid != GPSAL_FALSE))
    {
        /* Read Header. */
        s32_bytesRead = read(gp_gpsalObject->u16_socketDescriptor,
                             (McpS8 *)&s_msgHdr,
                             HOSTSOCKET_MESSAGE_HEADER_SIZE );

        if (gp_gpsalObject == NULL) {
            GPS_LOG_VER("null while reading");
            continue;
        }

        if (s32_bytesRead < 0)
        {
            GPS_LOG_ERR("ERROR: entryFunctionForHandler: Read Header FAILED!!!");
            GPS_LOG_ERR("ERROR: entryFunctionForHandler: *** Continue Response Handler ***");
            continue;
        }

        gp_gpsalObject->u16_responseOpcode = s_msgHdr.opCode;
        gp_gpsalObject->u32_responseMsgLen = s_msgHdr.payloadLen;

        /* Read Message Data if any. */
        if (gp_gpsalObject->u32_responseMsgLen)
        {
            s32_bytesRead = 0;

           /* Allocate Memory. */
           p_readBuffer = (McpU8 *) allocateMemory(gp_gpsalObject->u32_responseMsgLen);
           if ( (p_readBuffer == NULL) )
           {
              GPS_LOG_ERR("ERROR: entryFunctionForHandler: Memory Allocation FAILED !!!");
              GPS_LOG_ERR("ERROR: entryFunctionForHandler: *** Continue Response Handler ***");
              continue;
           }
            s32_bytesRead = read(gp_gpsalObject->u16_socketDescriptor,
                                 p_readBuffer,
                                 gp_gpsalObject->u32_responseMsgLen);

            if (gp_gpsalObject == NULL) {
                GPS_LOG_VER("null while reading2");
                continue;
            }

            if (s32_bytesRead < 0)
            {
                GPS_LOG_ERR("ERROR: entryFunctionForHandler: Read Payload FAILED!!!");
                GPS_LOG_ERR("ERROR: entryFunctionForHandler: *** Continue Response Handler ***");
                freeMemory((void **)&p_readBuffer);
                continue;
            }

            /* Process MCPF's messages accordingly. */
            gp_gpsalObject->p_responseData = (void *)p_readBuffer;
        }

        retVal = processResponsesFromMcpf(gp_gpsalObject);
        if (retVal != GPSAL_SUCCESS)
        {
            GPS_LOG_ERR("ERROR: entryFunctionForHandler: Read FAILED!!!");
        }

        /* Release memory if response was processed from this context. */
        freeMemory((void **)&p_readBuffer);
    }

    GPS_LOG_VER("entryFunctionForHandler: Exiting Successfully.");
    return;
}

/**
 * Function:        processResponsesFromMcpf
 * Brief:           Processes MCPF's responses.
 * Description:
 * Note:            Internal function.
 * Params:          p_inGpsalObject - GPSAL Object Handle.
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static eGPSAL_Result processResponsesFromMcpf(const TGPSAL_Object *const p_inGpsalObject)
{
    eGPSAL_Result retVal = GPSAL_SUCCESS;
    GPS_LOG_VER("processResponsesFromMcpf: Entering.");

    /** Process MCPF's responses in Response Handler's context
      * only if main GPSAL thread has issued a async request to MCPF. */
    switch(p_inGpsalObject->e_isAckRequired)
    {
        case GPSAL_TRUE:
        {
            /** Acknowledgement required by GPSAL.
              * Process the message from GPSAL's context. */
            retVal = releaseSemaphore(p_inGpsalObject->p_semHandle);
            if (retVal != GPSAL_SUCCESS)
            {
                GPS_LOG_ERR("ERROR: processResponsesFromMcpf: Semaphore Release FAILED!!!");
            }
        }
        break;

        case GPSAL_FALSE:
        {
            /* Process From Response Handler's context. */
            retVal = handleMcpfResponse(p_inGpsalObject);
            if (retVal != GPSAL_SUCCESS)
            {
                GPS_LOG_ERR("ERROR: processResponsesFromMcpf: Message Handling FAILED!!!");
            }
        }
        break;

        default:
        {
            GPS_LOG_ERR("ERROR: processResponsesFromMcpf: Default case.");
            return GPSAL_ERR_UNKNOWN;
        }
        break;
    }

    GPS_LOG_VER("processResponsesFromMcpf: Exiting Successfully.");
    return retVal;
}

/**
 * Function:        handleMcpfResponse
 * Brief:           Handles MCPF's responses and call appropriate FW callbacks.
 * Description:
 * Note:            Internal function.
 * Params:          p_inGpsalObject -  - GPSAL Object Handle..
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static eGPSAL_Result handleMcpfResponse(const TGPSAL_Object *const p_inGpsalObject)
{
    TmcpfMsg *p_mcpfResponse = NULL;
    TNAVC_evt *pEvt = NULL;

    eGPSAL_Result retVal = GPSAL_SUCCESS;

    //GPS_LOG_VER("handleMcpfResponse: Entering.");

    if (gp_gpsalObject == NULL)
    {
        GPS_LOG_ERR("ERROR: handleMcpfResponse: gp_gpsalObject NULL");
        retVal = GPSAL_FAIL;
        return retVal;
    }

    if (p_inGpsalObject->u32_responseMsgLen)
    {
        /* Payload. */
        p_mcpfResponse = (TmcpfMsg *)p_inGpsalObject->p_responseData;
        /* Just in case... */
        if (p_mcpfResponse == NULL)
        {
            GPS_LOG_ERR("ERROR: handleMcpfResponse: NULL Pointers.");
            return GPSAL_ERR_NULL_PTS;
        }
    }

    switch(p_inGpsalObject->u16_responseOpcode)
    {
        case NAVC_EVT_CMD_COMPLETE:
        {
            if (NULL == p_mcpfResponse) return GPSAL_ERR_NULL_PTS;

            retVal = processCmdCompleteResponse(p_mcpfResponse, p_inGpsalObject->p_callbacks);
            if (retVal != GPSAL_SUCCESS)
            {
                GPS_LOG_ERR("ERROR: %s: processCmdCompleteResponse FAILED !!! \n", FUNCTION_NAME);
                return retVal;
            }
        }
        break;

        case NAVC_EVT_POSITION_FIX_REPORT:
        {
            if (NULL == p_mcpfResponse) return GPSAL_ERR_NULL_PTS;

            retVal = processLocationFixResponse(p_mcpfResponse, p_inGpsalObject->p_callbacks);
            if (retVal != GPSAL_SUCCESS)
            {
                GPS_LOG_ERR("ERROR: handleMcpfResponse: NAVC Location Fix Processing FAILED !!!");
                return retVal;
            }
        }
        break;

        case NAVC_EVT_ASSIST_DEL_RES:
        {
            pEvt = (TNAVC_evt *)(p_mcpfResponse);
            if (pEvt == NULL)
            {
                GPS_LOG_ERR("ERROR: handleMcpfResponse: NULL Pointers.");
                return GPSAL_ERR_NULL_PTS;
            }
            if( (pEvt->eComplCmd == NAVC_CMD_DELETE_ASISTANCE) && (pEvt->eResult == RES_OK) )
            {
                GPS_LOG_VER("delete assistance complete");
                GPS_LOG_VER("==@@ GPShandleEvent: NAVC_CMD_DELETE_ASISTANCE == NAVC_EVT_CMD_COMPLETE: Result = %d", pEvt->eResult);
            }
        }
        break;

        default:
        {
             GPS_LOG_ERR("ERROR: handleMcpfResponse: received unhandled response from NAVC. Response OpCode = %d\n",
                                                           p_inGpsalObject->u16_responseOpcode);
        }
        break;
    }

    //GPS_LOG_VER("handleMcpfResponse: Exiting Successfully.");
    return retVal;
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
static eGPSAL_Result initializeMemory(void *const p_memoryLocation, McpU32 u32_inMemSize)
{
    //GPS_LOG_VER("initializeMemory: Entering.");

    memset(p_memoryLocation, '\0', u32_inMemSize);

    //GPS_LOG_VER("initializeMemory: Exiting Successfully.");
    return GPSAL_SUCCESS;
}

/**
 * Function:        issueLocationFixRequest
 * Brief:           Issue location fix requests after receiver sends START confirmation.
 * Description:
 * Note:            Internal function.
 * Params:          void - None.
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static eGPSAL_Result issueLocationFixRequest(void)
{
    static TNAVC_reqLocFix  *pReqLocFix = NULL;

    eGPSAL_Bool e_isAckRequired = GPSAL_FALSE;
    eGPSAL_Result retVal = GPSAL_SUCCESS;

    GPS_LOG_VER("issueLocationFixRequest: Entering.");

    pReqLocFix = (TNAVC_reqLocFix *)allocateMemory(sizeof(TNAVC_reqLocFix));
    if (NULL == pReqLocFix)
    {
        GPS_LOG_ERR("ERROR: issueLocationFixRequest: Memory Allocation FAILED !!!");
        return GPSAL_ERR_NULL_PTS;
    }

    /** Request MCPF for location fix.
      * Fix Mode and Fix frequency were already set.
      * MCPF should start a location session with NAVC with this information. */

    pReqLocFix->loc_fix_session_id = AUTONOMOUS_SESSION;
    g_locSessId = pReqLocFix->loc_fix_session_id;
    pReqLocFix->loc_fix_mode = g_e_locationFixMode;
    pReqLocFix->call_type = g_e_Call_type;
    GPS_LOG_VER("issueLocationFixRequest: loc_fix_mode %d \n",pReqLocFix->loc_fix_mode);
    GPS_LOG_VER("issueLocationFixRequest: call_type %d \n",pReqLocFix->call_type);

#ifdef GPSM_NMEA_FILE
    pReqLocFix->loc_fix_result_type_bitmap = (GPSC_RESULT_PROT | NMEA_MASK | GPSC_RESULT_RAW);
#else
    pReqLocFix->loc_fix_result_type_bitmap = (GPSC_RESULT_PROT | GPSC_RESULT_RAW);
#endif /* GPSM_NMEA_FILE */

#ifdef CONFIG_PERIODIC_REPORT
    /* Get the periodic report parameters from the configfile and configure the session */
    McpHalFsFileDesc filedes;
    PeriodicConfFile PeriodicConf;
    McpU32 BytesRead=0;
    EMcpfRes ret;

    ret = mcpf_file_open(NULL,(McpU8*) GPS_PERIODIC_CONFIG_FILE_PATH, MCP_HAL_FS_O_RDONLY,&filedes);
    if ( ret == MCP_HAL_FS_STATUS_SUCCESS)
    {
        BytesRead = mcpf_file_read (NULL, filedes, (McpU8 *)&PeriodicConf, sizeof(PeriodicConfFile));
        if ( BytesRead == sizeof(PeriodicConfFile) )
        {
             if (PeriodicConf.enable )
             {
                 pReqLocFix->loc_fix_period = PeriodicConf.ReportPeriod;
                 pReqLocFix->loc_fix_num_reports =PeriodicConf.NumReports;
             }
        }

      mcpf_file_close(NULL,filedes);
    }
#else
    pReqLocFix->loc_fix_period = g_s32_fixFrequency;
    pReqLocFix->loc_fix_num_reports = 0;
    GPS_LOG_VER("issueLocationFixRequest: Exiting Successfully.");

#endif /* CONFIG_PERIODIC_REPORT */

    pReqLocFix->loc_fix_max_ttff = 500;
    pReqLocFix->loc_fix_qop.horizontal_accuracy=g_hor_acc;
    pReqLocFix->loc_fix_qop.max_response_time=g_rsp_time;
    pReqLocFix->loc_fix_qop.max_loc_age=g_loc_age;

    e_isAckRequired = GPSAL_FALSE;
    retVal = sendRequestToMcpf(NAVC_CMD_REQUEST_FIX,
                               (void *)pReqLocFix,
                               e_isAckRequired,
                               sizeof(TNAVC_reqLocFix));

    freeMemory((void **)&pReqLocFix);
    if (retVal != GPSAL_SUCCESS)
    {
        GPS_LOG_ERR("ERROR: issueLocationFixRequest: Sending location request to MCPF FAILED !!!");
        return retVal;
    }

    GPS_LOG_VER("issueLocationFixRequest: Exiting Successfully.");
    return retVal;
}

/**
 * Function:        waitForSemaphore
 * Brief:           Blocks on a semaphore.
 * Description:
 * Note:            Internal function.
 * Params:          p_semaphoreHandle - Semaphore Handle.
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static eGPSAL_Result waitForSemaphore(SEM_HANDLE p_semaphoreHandle)
{
    McpS8 retVal = 0;
    GPS_LOG_VER("waitForSemaphore: Entering.");

    retVal = sem_wait(p_semaphoreHandle);
    if (retVal != 0)
    {
        GPS_LOG_ERR("ERROR: waitForSemaphore: sem_wait FAILED.");
        return GPSAL_ERR_SEM_WAIT_FAILED;
    }

    GPS_LOG_VER("waitForSemaphore: Exiting Successfully.");
    return GPSAL_SUCCESS;
}

/**
 * Function:        releaseSemaphore
 * Brief:           Releaase the semaphore.
 * Description:
 * Note:            Internal function.
 * Params:          p_semaphoreHandle - Semaphore Handle.
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static eGPSAL_Result releaseSemaphore(SEM_HANDLE p_semaphoreHandle)
{
    McpS8 retVal = 0;
    GPS_LOG_VER("releaseSemaphore: Entering.");

    retVal = sem_post(p_semaphoreHandle);
    if (retVal != 0)
    {
        GPS_LOG_ERR("ERROR: releaseSemaphore: sem_post FAILED.");
        return GPSAL_ERR_SEM_POST_FAILED;
    }

    GPS_LOG_VER("releaseSemaphore: Exiting Successfully.");
    return GPSAL_SUCCESS;
}

/* APK - Start */
/**
 * Function:        processCmdCompleteResponse
 * Brief:           Handles MCPFcommand completion responses and
                    call appropriate FW callbacks.
 * Description:
 * Note:            Internal function.
 * Params:          p_inPayload - Data payload.
                    p_callbacks - Callback handler.
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static eGPSAL_Result processCmdCompleteResponse(const void *const p_inPayload,
                                                const GpsCallbacks *const p_callbacks)
{
    eGPSAL_Result retVal = GPSAL_SUCCESS;
    TNAVC_evt *pEvt = (TNAVC_evt *)p_inPayload;
    int retValue;

    if (pEvt == NULL)
    {
        GPS_LOG_ERR("ERROR: %s: NULL Pointers. \n", FUNCTION_NAME);
        return GPSAL_ERR_NULL_PTS;
    }

    switch (pEvt->eComplCmd)
    {
        case NAVC_CMD_START:
        {
            if (pEvt->eResult != RES_OK)
            {
                GPS_LOG_ERR("ERROR: %s: NAVC_CMD_START FAILED !!!. \n", FUNCTION_NAME);
                retVal = GPSAL_ERR_UNKNOWN;
                g_IsInitComplete = FALSE;
            }
            else
            {
                GPS_LOG_VER("%s: NAVC_CMD_START SUCCESS \n", FUNCTION_NAME);
                g_hgps_status.status = GPS_STATUS_ENGINE_ON;
                gp_gpsalObject->u8_cbType |= STATUS_CB;
                retVal = releaseSemaphore(gp_gpsalObject->p_CbSemHandle);
                if(retVal != GPSAL_SUCCESS)
                {
                   GPS_LOG_ERR("ERROR: processCmdCompleteResponse: Semaphore Release failed.");
                }
                g_IsInitComplete = TRUE;
            }
        }
        break;

        case NAVC_CMD_STOP:
        {
            if (pEvt->eResult != RES_OK)
            {
                GPS_LOG_ERR("ERROR: %s: NAVC_CMD_STOP FAILED !!!. \n", FUNCTION_NAME);
                retVal = GPSAL_ERR_UNKNOWN;
            }
            else
            {
                GPS_LOG_VER("%s: NAVC_CMD_STOP SUCCESS \n", FUNCTION_NAME);
                g_hgps_status.status = GPS_STATUS_ENGINE_OFF;
                gp_gpsalObject->u8_cbType |= STATUS_CB;
                retVal = releaseSemaphore(gp_gpsalObject->p_CbSemHandle);
                if(retVal != GPSAL_SUCCESS)
                {
                   GPS_LOG_ERR("ERROR: processCmdCompleteResponse: Semaphore Release failed.");
                }
            }
        }
        break;

        case NAVC_CMD_REQUEST_FIX:
        {
            if (pEvt->eResult != RES_OK)
            {
                GPS_LOG_ERR("ERROR: %s: NAVC_CMD_REQUEST_FIX FAILED !!!. \n", FUNCTION_NAME);
                retVal = GPSAL_ERR_UNKNOWN;
            }
            else
            {
                GPS_LOG_VER("%s: NAVC_CMD_REQUEST_FIX SUCCESS \n", FUNCTION_NAME);
            }
        }
        break;

        case NAVC_CMD_STOP_FIX:
        {
            if(g_IsStopComplete == FALSE)
            {
                /* Release the wait irrespective of success or failure */
                pthread_mutex_lock(&g_waitMutex);
                GPS_LOG_VER("%s: Release wait on NAVC_CMD_STOP_FIX ", FUNCTION_NAME);
                g_IsStopComplete = TRUE;
                retValue = pthread_cond_signal(&g_waitCondVar);
                pthread_mutex_unlock(&g_waitMutex);
            }
            if (pEvt->eResult != RES_OK)
            {
                GPS_LOG_ERR("ERROR: %s: NAVC_CMD_STOP_FIX FAILED !!!.. \n", FUNCTION_NAME);
                retVal = GPSAL_ERR_UNKNOWN;
            }
            else
            {
                GPS_LOG_VER("%s: NAVC_CMD_STOP_FIX SUCCESS \n", FUNCTION_NAME);
            }
        }
        break;

        case NAVC_CMD_SAVE_ASSISTANCE:
        {
            if(g_IsSaveAssistComplete == FALSE)
            {
                pthread_mutex_lock(&g_waitMutex);
                /* Release the wait irrespective of success or failure */
                GPS_LOG_VER("%s: Release wait on NAVC_CMD_SAVE_ASSISTANCE ", FUNCTION_NAME);
                g_IsSaveAssistComplete = TRUE;
                retValue = pthread_cond_signal(&g_waitCondVar);
                pthread_mutex_unlock(&g_waitMutex);
            }
            if (pEvt->eResult != RES_OK)
            {
                GPS_LOG_ERR("ERROR: %s: NAVC_CMD_SAVE_ASSISTANCE FAILED !!!. \n", FUNCTION_NAME);
                retVal = GPSAL_ERR_UNKNOWN;
            }
            else
            {
                GPS_LOG_VER("%s: NAVC_CMD_SAVE_ASSISTANCE SUCCESS \n", FUNCTION_NAME);
            }
        }
        break;

        default:
        {
        }
        break;
    }

    GPS_LOG_VER("processCmdCompleteResponse: Exiting %s. \n", ( (retVal != GPSAL_SUCCESS) ? "with FAILURE.>!!!" : "Successfully"));
    return retVal;
}
/* APK - End */

/**
 * Function:        processLocationFixResponse
 * Brief:           Handles MCPF location fix responses and
                    call appropriate FW callbacks.
 * Description:
 * Note:            Internal function.
 * Params:          p_inPayload - Data payload.
                    p_callbacks - Callback handler.
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */


static eGPSAL_Result processLocationFixResponse(const void *const p_inPayload,
                                                const GpsCallbacks *const p_callbacks)
{
    TNAVC_evt   *pEvt = (TNAVC_evt *)p_inPayload;
    eGPSAL_Result retVal = GPSAL_SUCCESS;
    int retValue;

    if (pEvt->eResult != RES_OK)
    {
        GPS_LOG_ERR("ERROR: %s: NAVC_CMD_LOC_FIX FAILED !!!. \n", FUNCTION_NAME);
        retVal = GPSAL_ERR_UNKNOWN;
    }
    else
    {
        T_GPSC_loc_fix_response *loc_fix_response = &pEvt->tParams.tLocFixReport;
        T_GPSC_loc_fix_response_union *p_LocFixRespUnion = &loc_fix_response->loc_fix_response_union;
        if(g_IsStartComplete == FALSE)
        {
            pthread_mutex_lock(&g_waitMutex);
            GPS_LOG_VER("%s: Release wait on NAVC_CMD_START ", FUNCTION_NAME);
            g_IsStartComplete = TRUE;
            retValue = pthread_cond_signal(&g_waitCondVar);
            pthread_mutex_unlock(&g_waitMutex);
        }

        /** GPS-Driver returned success.
          * Process the first response from AL thread's context. There after in response thread's context. */
        GPS_LOG_VER("%s: NAVC_CMD_START SUCCESS \n", FUNCTION_NAME);

        if (loc_fix_response->ctrl_loc_fix_response_union == GPSC_RESULT_NMEA)
        {
            T_GPSC_nmea_response *p_nmeaPos = &p_LocFixRespUnion->nmea_response;

            GPS_LOG_VER("***************** GPSC_RESULT_NMEA **********************");
            retVal = processNmeaData(p_nmeaPos, p_callbacks);
            if (retVal != GPSAL_SUCCESS)
            {
                GPS_LOG_ERR("ERROR: processLocationFixResponse: Nmea Data Processing FAILED !!!");
                return GPSAL_ERR_DATA_PROCESS_FAILED;
            }
        }

        if ( loc_fix_response->ctrl_loc_fix_response_union == GPSC_RESULT_PROT_MEASUREMENT)
        {
            T_GPSC_prot_measurement *p_ProtMeas = &p_LocFixRespUnion->prot_measurement;
            T_GPSC_sv_measurement *p_measurement = (T_GPSC_sv_measurement *)&p_ProtMeas->sv_measurement;

            GPS_LOG_VER("***************** GPSC_RESULT_PROT_MEASUREMENT **********************");
            retVal = processProtMeasurement(p_ProtMeas, p_measurement, p_callbacks, &s_gpsSvStatus);
            if (retVal != GPSAL_SUCCESS)
            {
                GPS_LOG_ERR("ERROR: processLocationFixResponse: Prot measurement Data Processing FAILED !!!");
                return GPSAL_ERR_DATA_PROCESS_FAILED;
            }
        }

        if ( loc_fix_response->ctrl_loc_fix_response_union == GPSC_RESULT_RAW_POSITION)
        {
            T_GPSC_raw_position *p_RawPosition = &p_LocFixRespUnion->raw_position;
            T_GPSC_position *gpsSvStatus = &p_RawPosition->position;
            T_GPSC_toa *p_toa = &p_RawPosition->toa;

            GPS_LOG_VER("***************** GPSC_RESULT_RAW_POSITION **********************");
            retVal = ProcessRawPosition(p_RawPosition, gpsSvStatus,
                                        p_callbacks, &s_gpsLocation, &s_gpsSvStatus);
            if (retVal != GPSAL_SUCCESS)
            {
                GPS_LOG_ERR("ERROR: processLocationFixResponse: Raw Position Data Processing FAILED !!!");
                return GPSAL_ERR_DATA_PROCESS_FAILED;
            }

            calculateUtcTime(&s_gpsLocation,p_toa );
        }

        if ( loc_fix_response->ctrl_loc_fix_response_union == GPSC_RESULT_PROT_POSITION)
        {
            T_GPSC_prot_position *p_RrlpPosition = &p_LocFixRespUnion->prot_position;
            T_GPSC_ellip_alt_unc_ellip *p_RrlpEllipInfo = &p_RrlpPosition->ellip_alt_unc_ellip;

            GPS_LOG_VER("***************** GPSC_RESULT_PROT_POSITION **********************");
            retVal = processProtPosition(p_RrlpPosition, p_RrlpEllipInfo,
                                         p_callbacks, &s_gpsLocation);
            if (retVal != GPSAL_SUCCESS)
            {
                GPS_LOG_ERR("ERROR: processLocationFixResponse: Prot Position Data Processing FAILED !!!");
                return GPSAL_ERR_DATA_PROCESS_FAILED;
            }
        }
    }

    GPS_LOG_VER("processLocationFixResponse: Exiting %s \n", (retVal != GPSAL_SUCCESS) ? "with FAILURE !!!" : "Successfully.");
    return retVal;
}

/**
 * Function:        processNmeaData
 * Brief:           Processes NMEA data and call appropriate FW callbacks.
 * Description:
 * Note:            Internal function.
 * Params:          p_inNmeaResponse - Nmea Response.
                    p_callbacks - Callback handler.
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static eGPSAL_Result processNmeaData(const void *const p_inNmeaResponse,
                                     const GpsCallbacks *const p_callbacks)
{
    eGPSAL_Result retVal = GPSAL_SUCCESS;

    GPS_LOG_VER("processNmeaData: Entering.");

    char * temp_ptr = (char*)p_inNmeaResponse;
    char *p_token = NULL;
    memset(gp_gpsalObject->nmea_buff, '\0', sizeof(gp_gpsalObject->nmea_buff));

    p_token = strtok(temp_ptr, "$");

    if(p_token != NULL)
    {
        if((strlen(p_token)) <= (MAX_NMEA_BUFF_SIZE-1))
        {
            sprintf(gp_gpsalObject->nmea_buff, "$%s", p_token);
        }
        GPS_LOG_VER("===>>> gp_gpsalObject->nmea_buff= %s ", gp_gpsalObject->nmea_buff);

        pthread_mutex_lock(&g_CBWaitMutex);
        gp_gpsalObject->u8_cbType |= NMEA_CB;
        retVal = releaseSemaphore(gp_gpsalObject->p_CbSemHandle);
        if(retVal != GPSAL_SUCCESS)
        {
           GPS_LOG_ERR("ERROR: processNmeaData: Semaphore Release failed.");
        }
 
        /* Wait to a maximum of 2 seconds for the Android framework CB to return */
        if((WaitForCBCompletion(2)) == ETIMEDOUT) 
        {
           GPS_LOG_ERR("ERROR: processNmeaData: NMEA CB timed out");
        }
        pthread_mutex_unlock(&g_CBWaitMutex);
    }
    GPS_LOG_VER("processNmeaData: Exiting Successfully.");
    return retVal;
}

/**
 * Function:        ProcessRawPosition
 * Brief:           Processes Raw Position and call appropriate FW callbacks.
 * Description:
 * Note:            Internal function.
 * Params:          p_RawPosition -
                    p_gpsPosition -
                    p_callbacks - Callback handler.
                    p_outLocation - Location Information.
                    p_outGpsSVStatus -
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static eGPSAL_Result ProcessRawPosition(const T_GPSC_raw_position * const p_RawPosition,
                                        const T_GPSC_position *const p_gpsPosition,
                                        const GpsCallbacks *const p_callbacks,
                                        GpsLocation *p_outLocation,
                                        GpsSvStatus *const p_outGpsSVStatus)
{
    eGPSAL_Result retVal = GPSAL_SUCCESS;

    GPS_LOG_VER("processRawPosition: Entering.");

    memset((void *)&s_gpsLocation, 0, sizeof(s_gpsLocation));

    p_outLocation->flags = 0;

    calculateSpeed(p_outLocation, p_gpsPosition, MCP_FALSE);
    calculateAccuracy(p_outLocation, p_gpsPosition);
    calculateSVsUsedInFixMask(p_outGpsSVStatus, p_gpsPosition);
    /* Bearing and Timestamp calculation is pending. */
    calculateBearingAndTime(p_outLocation, p_gpsPosition);

    GPS_LOG_VER("processRawPosition: Exiting Successfully.");
    return retVal;
}

/**
 * Function:        processProtPosition
 * Brief:           Processes Prot Position and call appropriate FW callbacks.
 * Description:
 * Note:            Internal function.
 * Params:          p_inLocFixResponseUnion - Location response payload.
                    p_callbacks - Callback handler.
                    p_outLocation - Location Information.
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static eGPSAL_Result processProtPosition(const T_GPSC_prot_position * p_RrlpPosition,
                                         const T_GPSC_ellip_alt_unc_ellip *const p_RrlpEllipInfo,
                                         const GpsCallbacks *const p_callbacks,
                                         GpsLocation *const p_outLocation)
{

#define TWO_TO_23   8388608.0F
#define TWO_TO_24   16777216.0F
#define LAT_SCALE (TWO_TO_23 / 90.0F)
#define LONG_SCALE (TWO_TO_24 / 360.0F)
#define TWO_TO_15               32768.0F
#define HEADING_SCALE           (TWO_TO_15 / 180.0F)

#define GPS2UTC 315964800000
#define LEAP_SECS 15000
#define SEC_MSEC 1000

    FLT f_Latitude = 0.0;
    FLT f_Longitude = 0.0;
    FLT f_Altitude = 0.0; /* TBD: This field has to be updated. */
    static McpDBL timeval;
    eGPSAL_Result retVal = GPSAL_SUCCESS;

    switch(p_RrlpPosition->prot_fix_result)
    {
        case GPSC_PROT_FIXRESULT_2D:
        {
            GPS_LOG_VER("2D fix detected");
            p_outLocation->altitude = 0;
            p_outLocation->flags |= GPS_LOCATION_HAS_LAT_LONG;
        }
        break;

        case GPSC_PROT_FIXRESULT_3D:
        {
            GPS_LOG_VER("3D fix detected");
            f_Altitude = p_RrlpEllipInfo->altitude;

            if(p_RrlpEllipInfo->altitude_sign)
                p_outLocation->altitude = -f_Altitude;
            else
                p_outLocation->altitude = f_Altitude;

            p_outLocation->flags |= (GPS_LOCATION_HAS_LAT_LONG | GPS_LOCATION_HAS_ALTITUDE);
        }
        break;

        case GPSC_PROT_FIXRESULT_NOFIX:
            GPS_LOG_VER("No fix detected");
            return GPSAL_SUCCESS;
        break;

        default:
            GPS_LOG_ERR("ERROR: Unknown case");
            return GPSAL_ERR_UNKNOWN;
        break;
    }

    f_Latitude = (FLT) ((DBL)p_RrlpEllipInfo->latitude / LAT_SCALE);
    f_Longitude = (FLT) ((DBL) p_RrlpEllipInfo->longitude / LONG_SCALE);

    if(p_RrlpEllipInfo->latitude_sign)
        p_outLocation->latitude = -f_Latitude;
    else
        p_outLocation->latitude = f_Latitude;

    p_outLocation->longitude = f_Longitude;


    if ((p_RrlpEllipInfo->velocity_flag) & HORSPEED)
    {
        p_outLocation->speed = p_RrlpEllipInfo->horspeed;
        GPS_LOG_VER("SPEED in GPS.c =  %f\n", p_outLocation->speed );
        p_outLocation->flags |= GPS_LOCATION_HAS_SPEED;
    }

    if ((p_RrlpEllipInfo->velocity_flag) & BEARING)
    {
        p_outLocation->bearing  = (float) (p_RrlpEllipInfo->bearing / HEADING_SCALE);
        GPS_LOG_VER("BEARING in GPS.c =  %f\n", p_outLocation->bearing );
        p_outLocation->flags |= GPS_LOCATION_HAS_BEARING;
    }

    if ((p_RrlpEllipInfo->velocity_flag) & HORUNCERTSPEED)
    {
        p_outLocation->accuracy = (FLT)p_RrlpEllipInfo->horuncertspeed;
        p_outLocation->flags |=GPS_LOCATION_HAS_ACCURACY;
    }

    p_outLocation->timestamp = (int64_t)(p_RrlpPosition->ellip_alt_unc_ellip.utctime *(int64_t)SEC_MSEC)+(int64_t)GPS2UTC;

    if (g_TTFFflag == 0)
    {
        g_TTFFflag = 1;
        GPS_LOG_VER("Session=%d TTFF=%d\n", g_GPSStartCount, g_GPSCalbackCounter);

        pthread_mutex_lock(&g_CBWaitMutex);
        gp_gpsalObject->u8_cbType |= SV_STATUS_CB;
        retVal = releaseSemaphore(gp_gpsalObject->p_CbSemHandle);
        if(retVal != GPSAL_SUCCESS)
        {
            GPS_LOG_ERR("ERROR: processProtPosition: Semaphore Release failed.");
        }
        /* Wait to a maximum of 2 seconds for the Android framework CB to return */
        if((WaitForCBCompletion(2)) == ETIMEDOUT)
        {
           GPS_LOG_ERR("ERROR: processProtPosition: SV_STATUS_CB timed out");
        }
        pthread_mutex_unlock(&g_CBWaitMutex);
    }

    GPS_LOG_INF("[Lat %3.06f Deg] [Lon %3.06f Deg] [Alt %3.06f m] [acc=%f] [speed=%f] [bearing=%f] [timestamp=%llu] [flags=%x]",
        p_outLocation->latitude,
        p_outLocation->longitude,
        p_outLocation->altitude,
        p_outLocation->accuracy,
        p_outLocation->speed,
        p_outLocation->bearing,
        p_outLocation->timestamp,
        p_outLocation->flags);

    /* Other fields of 'location' are filled in GPSC_RESULT_RAW_POSITION.
    Order of calls: 1. GPSC_RESULT_RAW_POSITION... 2. GPSC_RESULT_PROT_POSITION. */
    pthread_mutex_lock(&g_CBWaitMutex);
    gp_gpsalObject->u8_cbType |= LOCATION_CB;
    retVal = releaseSemaphore(gp_gpsalObject->p_CbSemHandle);
    if(retVal != GPSAL_SUCCESS)
    {
       GPS_LOG_ERR("ERROR: processProtPosition: Semaphore Release failed.");
    }
    /* Wait to a maximum of 2 seconds for the Android framework CB to return */
    if((WaitForCBCompletion(2))== ETIMEDOUT)
    {
        GPS_LOG_ERR("ERROR: processProtPosition: LOCATION_CB timed out");
    }
    pthread_mutex_unlock(&g_CBWaitMutex);

    return GPSAL_SUCCESS;
}

/**
 * Function:        processRawMeasurement
 * Brief:           Processes PNR SNR report and  FW callbacks.
 * Description:
 * Note:            Internal function.
 * Params:          p_RawMeas -
                    p_measurement -
                    p_callbacks - Callback handler.
                    p_outGpsSVStatus - Satelite Vehicle Information.
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static eGPSAL_Result processProtMeasurement(const T_GPSC_prot_measurement *const p_ProtMeas,
                                           const T_GPSC_sv_measurement *const p_measurement,
                                           const GpsCallbacks *const p_callbacks,
                                           GpsSvStatus *const p_outGpsSVStatus)
{
    eGPSAL_Result retVal = GPSAL_SUCCESS;

    GPS_LOG_VER("processProtMeasurement: Entering.");

    if ( p_outGpsSVStatus == NULL)
    {
        GPS_LOG_ERR("ERROR: processProtMeasurement: Null pointers.");
        return GPSAL_ERR_NULL_PTS;
    }

    memset((void *)&s_gpsSvStatus, 0, sizeof(s_gpsSvStatus));

    calculatePrnAndSnr(p_outGpsSVStatus, p_ProtMeas, p_measurement);

    p_outGpsSVStatus->used_in_fix_mask = gp_gpsalObject->Svstatus.used_in_fix_mask;

    p_outGpsSVStatus->ephemeris_mask = ~(p_ProtMeas->eph_availability_flags);
    p_outGpsSVStatus->ephemeris_mask |= ~(p_ProtMeas->pred_eph_availability_flags);
    p_outGpsSVStatus->almanac_mask = ~(p_ProtMeas->alm_availability_flags);
    GPS_LOG_VER("PRM: EPH: %x, ALM: %x",p_ProtMeas->eph_availability_flags, p_ProtMeas->alm_availability_flags);

    /* Send the response to framework. */
    g_GPSCalbackCounter++;
    memcpy(&gp_gpsalObject->Svstatus, (void *)p_outGpsSVStatus,sizeof(GpsSvStatus));

    pthread_mutex_lock(&g_CBWaitMutex);
    gp_gpsalObject->u8_cbType |= SV_STATUS_CB;
    retVal = releaseSemaphore(gp_gpsalObject->p_CbSemHandle);
    if(retVal != GPSAL_SUCCESS)
    {
       GPS_LOG_ERR("ERROR: processProtMeasurement: Semaphore Release failed.");
    }
    /* Wait to a maximum of 2 seconds for the Android framework CB to return */
    if((WaitForCBCompletion(2)) == ETIMEDOUT)
    {
        GPS_LOG_ERR("ERROR: processProtMeasurement: SV_STATUS_CB timed out");
    }
    pthread_mutex_unlock(&g_CBWaitMutex);
    GPS_LOG_VER("processProtMeasurement: Exiting Successfully.");
    return GPSAL_SUCCESS;
}

/**
 * Function:        calculatePrnAndSnr
 * Brief:           Processes Raw Measurements and call appropriate FW callbacks.
 * Description:
 * Note:            Internal function.
 * Params:          p_outGpsSVStatus - Satelite Vehicle Information.
                    p_inRawMeasurement - Raw Measurements.
                    p_inMeasurement -
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static eGPSAL_Result calculatePrnAndSnr(GpsSvStatus *const p_outGpsSvStatus,
                                        const T_GPSC_prot_measurement *const p_inProtMeasurement,
                                        const T_GPSC_sv_measurement *const p_inMeasurement)
{
    eGPSAL_Result retVal = GPSAL_SUCCESS;
    McpS32 index = 0;
    McpS32 index_SNR = 0;

    GPS_LOG_VER("calculatePrnAndSnr: Entering.");

    GPS_LOG_VER("============= PRN and SNR Values ==================\n");
    /* Pseudo Random Noise and Signal to Noise Ratio */
    for(index = 0; ((index < p_inProtMeasurement->c_sv_measurement) && (index < 32)); index++)
    {
        /* Space Vehicle ID 0 - 63 represents SV PRN 1 - 64 */

        if(((FLT) (p_inMeasurement[index].snr) / 10) !=0) {
            p_outGpsSvStatus->sv_list[index_SNR].prn = p_inMeasurement[index].svid + 1;

            /* SV signal to noise ratio, unsigned, 0.1 dB per bit */
            p_outGpsSvStatus->sv_list[index_SNR].snr = (FLT) ((p_inMeasurement[index].cno/10));

        p_outGpsSvStatus->sv_list[index_SNR].elevation = (FLT) (p_inMeasurement[index].elevation * C_LSB_ELEV_MASK);
        p_outGpsSvStatus->sv_list[index_SNR].azimuth = (FLT) (p_inMeasurement[index].azimuth * C_LSB_AZIM_MASK);

            /* SV signal to noise ratio, unsigned, 0.1 dB per bit */
            index_SNR++;
        }
    }
    p_outGpsSvStatus->num_svs = index_SNR;

    return retVal;
}

/**
 * Function:        readConfigFile
 * Brief:
 * Description:
 * Note:            Internal function.
 * Params:
 * Return:          Success: GPSAL_SUCCESS.
                    Failure: GPSAL_FAILURE_XXX.
 */
static eGPSAL_Result readConfigFile()
{
    eGPSAL_Result retVal = GPSAL_SUCCESS;
    FILE *fp = NULL;

    char a_inputBuffer[MAX_BUF_LENGTH] = {'\0'};
    char *p_token = NULL;

    GPS_LOG_VER("readConfigFile: Entering");

    fp = fopen(GPS_CONFIG_FILE_PATH, RDONLY);
    if (NULL == fp)
    {
        GPS_LOG_ERR("ERROR: readConfigFile: fopen FAILED !!!");
        return GPSAL_ERR_UNKNOWN;
    }

    while( (fgets(a_inputBuffer, sizeof(a_inputBuffer), fp) ) &&
           (stringCompare(a_inputBuffer, "\n") != GPSAL_SUCCESS) )
    {
        GPS_LOG_VER("readConfigFile: a_inputBuffer = %s \n", a_inputBuffer);
        p_token = (char *)strtok(a_inputBuffer, ":");
        if ( NULL == p_token )
        {
            /* Continue with the next line. */
            continue;
        }

        if ((stringCompare(p_token, GPS_MODE) == GPSAL_SUCCESS) )
        {
            GPS_LOG_VER("readConfigFile: p_token = %s \n", p_token);
            p_token = (char *) strtok(NULL, ":");
            if (NULL == p_token)
            {
                GPS_LOG_ERR("ERROR: readConfigFile: strtok returned NULL !!!");
                fclose(fp);
                return GPSAL_ERR_NULL_PTS;
            }
            GPS_LOG_VER("readConfigFile: p_token = %s \n", p_token);
            if ( stringCompare(p_token, GPS_AUTONOMOUS) == GPSAL_SUCCESS )
            {
                GPS_LOG_VER("readConfigFile: Autonomous Fix Requested.");
                g_locFixMode = 0;
            }
            else if(stringCompare(p_token, GPS_MSBASED) == GPSAL_SUCCESS)
            {
                GPS_LOG_VER("readConfigFile: MS_Based Fix Requested.");
                g_locFixMode = 1;
            }
            else if(stringCompare(p_token, GPS_MSASSISTED) == GPSAL_SUCCESS)
            {
                GPS_LOG_VER("readConfigFile: MS_Assisted Fix Requested.");
                g_locFixMode = 2;
            }
            else if(stringCompare(p_token, GPS_AUTOSUPL) == GPSAL_SUCCESS)
            {
                GPS_LOG_VER("readConfigFile: Autosupl Fix Requested.");
                g_locFixMode = 3;
            }
            else
            {
                GPS_LOG_ERR("ERROR: readConfigFile: Fix Mode not supported");
                fclose(fp);
                return GPSAL_ERR_UNKNOWN;
            }
       }
       else if ((stringCompare(p_token, EE_SUPPORT) == GPSAL_SUCCESS))
       {
            p_token = (char *) strtok(NULL, ":");
            if (NULL == p_token)
            {
                GPS_LOG_ERR("ERROR: readConfigFile: strtok returned NULL !!!");
                fclose(fp);
                return GPSAL_ERR_NULL_PTS;
            }
            if ( stringCompare(p_token, ENABLE_EE) == GPSAL_SUCCESS )
            {
                g_eeSupported = 1;
                GPS_LOG_VER("readConfigFile: EE Support Requested.");
            }
            else
            {
                g_eeSupported = 0;
                GPS_LOG_VER("readConfigFile: EE Support Not Requested.");
            }
       }
       else if ((stringCompare(p_token, HOR_ACC) == GPSAL_SUCCESS))
       {
            p_token = (char *) strtok(NULL, ":");
            if (NULL == p_token)
            {
                GPS_LOG_ERR("ERROR: readConfigFile: strtok returned NULL !!!");
                fclose(fp);
                return GPSAL_ERR_NULL_PTS;
            }
            else
            {
                g_hor_acc= atoi(p_token);
                if(g_hor_acc>MAX_HOR_ACC_VALUE)
                    g_hor_acc = MAX_HOR_ACC_VALUE;
                GPS_LOG_VER("readConfigFile: HOR_ACC : %d \n",g_hor_acc);
            }
        }
        else if ((stringCompare(p_token, LOC_AGE) == GPSAL_SUCCESS))
        {
            p_token = (char *) strtok(NULL, ":");
            if (NULL == p_token)
            {
                GPS_LOG_ERR("ERROR: readConfigFile: strtok returned NULL !!!");
                fclose(fp);
                return GPSAL_ERR_NULL_PTS;
            }
            else
            {
                g_loc_age= atoi(p_token);
                if(g_loc_age>MAX_LOC_AGE_VALUE)
                   g_loc_age = MAX_LOC_AGE_VALUE;
                GPS_LOG_VER("readConfigFile: LOC_AGE : %d \n",g_loc_age);
            }
        }
        else if ((stringCompare(p_token, RSP_TIME) == GPSAL_SUCCESS))
        {
            p_token = (char *) strtok(NULL, ":");
            if (NULL == p_token)
            {
                GPS_LOG_ERR("ERROR: readConfigFile: strtok returned NULL !!!");
                fclose(fp);
                return GPSAL_ERR_NULL_PTS;
            }
            else
            {
                g_rsp_time= atoi(p_token);
                if(g_rsp_time>MAX_RSP_TIME_VALUE)
                    g_rsp_time = MAX_RSP_TIME_VALUE;
                GPS_LOG_VER("readConfigFile: RSP_TIME : %d \n",g_rsp_time);
            }
        }
    }

    fclose(fp);
    GPS_LOG_VER("readConfigFile: Exiting Successfully.");
    return retVal;
}

/**
 * Function: stringCompare
 */
static eGPSAL_Result stringCompare(const char *const p_string_1,
                                   const char *const p_string_2)
{
    if ( strcmp(p_string_1, p_string_2) == 0)
    {
        return GPSAL_SUCCESS;
    }

    return GPSAL_ERR_UNKNOWN;
}

/**
 * Function: calculateSpeed
 */
static eGPSAL_Result calculateSpeed(GpsLocation *const location,
                                    const T_GPSC_position *const gpsSvStatus,
                                    const McpBool is2dFix)
{
    #define C_LSB_EAST_VEL      (1000.0/65535.0)  /* -500 to 500 range, 16
                                                     bits: LSB = 0.0152590
                                                     meters/sec */
    #define C_LSB_NORTH_VEL     (1000.0/65535.0)  /* -500 to 500 range, 16
                                                     bits: LSB = 0.0152590
                                                     meters/sec */
    #define C_LSB_VERTICAL_VEL  (1000.0/65535.0)  /* -500 to 500 range, 16
                                                     bits: LSB = 0.0152590
                                                     meters/sec */

    FLT  x_VelEast_mps = 0.0;             /* East Velocity */
    FLT  x_VelNorth_mps = 0.0;            /* North Velocity */
    FLT  x_VelVert_mps = 0.0;             /* Vertical Velocity */
    FLT  x_y_speed_mps = 0.0;             /* horizontal speed  */

    x_VelEast_mps = gpsSvStatus->velocity_east * C_LSB_EAST_VEL;
    x_VelNorth_mps = gpsSvStatus->velocity_north * C_LSB_NORTH_VEL;
    if (!is2dFix)
        x_VelVert_mps = gpsSvStatus->velocity_vertical * C_LSB_VERTICAL_VEL;

    x_y_speed_mps = sqrt((x_VelEast_mps * x_VelEast_mps) + (x_VelNorth_mps * x_VelNorth_mps));
    location->speed = x_y_speed_mps;

    location->flags |= GPS_LOCATION_HAS_SPEED;

    GPS_LOG_VER("===>>> Speed is:%f (in meters/sec) \n", location->speed);
    return GPSAL_SUCCESS;
}

/**
 * Function: calculateBearingAndTime
 */
static eGPSAL_Result calculateBearingAndTime(GpsLocation *const location,
                                    const T_GPSC_position *const gpsSvStatus)
{
    #define TWO_TO_15               32768.0F
    #define HEADING_SCALE           (TWO_TO_15 / 180.0F)

    location->bearing = (float) (gpsSvStatus->heading_true/HEADING_SCALE);

    location->flags |= GPS_LOCATION_HAS_BEARING;

    GPS_LOG_VER("===>>> Bearing  is:%f Time = %ld \n", location->bearing, location->timestamp);

    return GPSAL_SUCCESS;
}

/**
 * Function: calculateUtcTime
 */
static  void calculateUtcTime(GpsLocation *const location,
                             const T_GPSC_toa *const pToa)
{
    #define TENTH_MSECS    100
    #define SEC_MSECS      1000
    #define MIN_MSECS      60*SEC_MSECS
    #define HOUR_MSECS     60*MIN_MSECS
    #define DAY_MSECS      24*HOUR_MSECS
    #define WEEK_MSEC      7*DAY_MSECS
    #define LeapMsec       15*SEC_MSECS

    /* calculate UTC time by using GPS time (GPS week and msec) information
     * UTC time started on Jan 1 1970 and GPS time started on Jan 6th 1980 UTC time
     * So GPS time is behind the UTC time by 10 yrs and 5-days
     * Of the 10-yrs we have 2 leap years i.1., 1972 and 1976 which have 366 days
     */

    int64_t gpsToUtcMillisecDiff = (int64_t)(8*365+2*366+5)*(int64_t)DAY_MSECS;

    /* GPS time is not corrected for the earth rotation where as the UTC time is corrected
     * for earth rotation hence teh GPS time is ahead of UTC time by the leap number of secs.
     * This leap sec increments every 500-days the right way is to read the leap sec from the
     *  GPSCConfigFile.cfg file.but for now use the value of 15 which current for this yr
     */

    int64_t leapMsec = (int64_t) 16*SEC_MSECS;
    int64_t gpsWeek = (int64_t) pToa->gps_week;
    int64_t gpsMsec = (int64_t) pToa->gps_msec;
    int64_t utcMsec = 0;

    if (gpsMsec >= leapMsec)
    {
        gpsMsec = gpsMsec - leapMsec;
    }
    else
    {
        gpsWeek = gpsWeek - 1;
        gpsMsec = gpsMsec - leapMsec + WEEK_MSEC;
    }

    utcMsec = gpsWeek*(int64_t) WEEK_MSEC + gpsMsec + gpsToUtcMillisecDiff;

    location->timestamp = utcMsec;

    return ;
}

/**
 * Function: calculateAccuracy
 */
static eGPSAL_Result calculateAccuracy(GpsLocation *const location,
                                       const T_GPSC_position *const gpsSvStatus)
{
    /* Vertical, horizontal, speed and heading accuracy can be calculated from the
     * information available in NAVC. Not sure which data has to be given to android application.
     * For now, consider horizontal accuracy */

    #define C_LSB_EAST_ER               (0.1)
    #define C_LSB_NORTH_ER              (0.1)

    FLT  f_EastUnc;
    FLT  f_NorthUnc;

    f_EastUnc = gpsSvStatus->uncertainty_east * (FLT) C_LSB_EAST_ER;
    f_NorthUnc = gpsSvStatus->uncertainty_north * (FLT) C_LSB_NORTH_ER;
    location->accuracy = (FLT) sqrt( (f_EastUnc * f_EastUnc) + (f_NorthUnc * f_NorthUnc) );
    location->flags |= GPS_LOCATION_HAS_ACCURACY;

    GPS_LOG_VER("calculateAccuracy: HORIZONTAL ACCURACY is:%f (in meters)\n", location->accuracy);
    return GPSAL_SUCCESS;
}

/**
 * Function: calculateSVsUsedInFixMask()
 */
static eGPSAL_Result calculateSVsUsedInFixMask(GpsSvStatus *const SvStatus,
                                               const T_GPSC_position *const gpsSvStatus)
{
    U8 index;
    uint32_t svUsedBitMap = 0;
    U8 numSVsUsedInFix = gpsSvStatus->c_position_sv_info;
    const T_GPSC_position_sv_info *p_positionSvInfo;

    for (index=0;(index<numSVsUsedInFix) && (index<GPSC_RAW_MAX_SV_REPORT);index++)
    {
        p_positionSvInfo = &gpsSvStatus->position_sv_info[index];
        GPS_LOG_VER("calculateSVsUsedInFixMask: SVID in PosInfo: %d\n",p_positionSvInfo->svid);
        svUsedBitMap |= (1<< (p_positionSvInfo->svid ));
        GPS_LOG_VER("SAD ==>>> FixMask index: %d, and svUsedBitMap : %x ",index, svUsedBitMap);
    }

    GPS_LOG_VER("calculateSVsUsedInFixMask: SVs used in fix = %d \n", numSVsUsedInFix);
    GPS_LOG_VER("calculateSVsUsedInFixMask: SVUsedBitMask = %x \n", svUsedBitMap);

    SvStatus->used_in_fix_mask = svUsedBitMap;
    gp_gpsalObject->Svstatus.used_in_fix_mask = SvStatus->used_in_fix_mask;
    GPS_LOG_VER("calculateSVsUsedInFixMask: used_in_fix_mask = %d \n", SvStatus->used_in_fix_mask);
    return GPSAL_SUCCESS;
}

/**
 * Function: populateDeleteBitmap()
 */
static void populateDeleteBitmap(TNAVC_delAssist *const pDelAssist, const GpsAidingData flags)
{
    pDelAssist->uDelAssistBitmap = 0x00;

    GPS_LOG_VER("populateDeleteBitmap(): incoming flags = %x\n",flags);
    GPS_LOG_VER("populateDeleteBitmap(): entering ... pDelAssist->uDelAssistBitmap= %x\n",pDelAssist->uDelAssistBitmap);
    if ((flags & GPS_DELETE_ALL) == GPS_DELETE_ALL)
      {
        pDelAssist->uDelAssistBitmap |= DEL_AIDING_TIME | DEL_AIDING_POSITION | DEL_AIDING_IONO_UTC | DEL_AIDING_SVHEALTH;
        GPS_LOG_VER("populateDeleteBitmap(): exiting ... deleteAll ... pDelAssist->uDelAssistBitmap= %x\n",pDelAssist->uDelAssistBitmap);
        return;
      }

    if (flags & GPS_DELETE_POSITION)
        pDelAssist->uDelAssistBitmap |= DEL_AIDING_POSITION;

    if (flags & GPS_DELETE_TIME)
        pDelAssist->uDelAssistBitmap |= DEL_AIDING_TIME;

    if ( (flags & GPS_DELETE_IONO) || (flags & GPS_DELETE_UTC) )
        pDelAssist->uDelAssistBitmap |= DEL_AIDING_IONO_UTC;

    if (flags & GPS_DELETE_HEALTH)
        pDelAssist->uDelAssistBitmap |= DEL_AIDING_SVHEALTH;

    if (flags & GPS_DELETE_SVDIR)
        pDelAssist->uDelAssistBitmap |= DEL_AIDING_SVDIR;

    GPS_LOG_VER("populateDeleteBitmap(): exiting ... pDelAssist->uDelAssistBitmap= %x\n",pDelAssist->uDelAssistBitmap);
    return;
}

int hgps_agps_data_conn_open(const char *apn)
{
    return 0;
}

int hgps_agps_data_conn_closed()
{
    return 0;
}

int hgps_agps_data_conn_failed()
{
    return 0;
}

int hgps_agps_set_server(AGpsType type, const char* hostname, int port )
{
    return 0;
}

void set_privacy_lock (int enable_lock )
{
}

static struct hw_module_methods_t ti_gps_module_methods = {
    .open = open_gps
};

struct hw_module_t HAL_MODULE_INFO_SYM = {
    .tag = HARDWARE_MODULE_TAG,
    .version_major = 1,
    .version_minor = 0,
    .id = GPS_HARDWARE_MODULE_ID,
    .name = "TI GPS Module",
    .author = "TI",
    .methods = &ti_gps_module_methods,
};
