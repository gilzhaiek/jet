/*
 *******************************************************************************
 *         
 *
 * FileName         :   GPSResponseHander.cpp
 *
 * Description      :   Implementation of GPSResponseHandler object
 *
 * Author           :   RECON Instruments
 * Date             :   November 2013
 *
 * Comments         :   This object performs final processing of received GPS data
 *                      Client Handoff is performed via derived class 
 * 
 *                      Implementation is OEM independant (thus no knowledge of 
 *                      driver-specific data structs, etc -- in MCP in TI case) or
 *                      HAL repiorting mechanism (GpsCallbacks in Android case)
 ******************************************************************************
 */

#define LOG_TAG "RECON.GPS"   // our identification for logcat (adb logcat RECON.GPS:V *:S)

#include <signal.h>
#include <errno.h>

#include "gpsresponsehandler.h"    // definition of this object

const unsigned int GPSResponseHandler::TEST_COMPLETE_MSEC = 10;    // timeout testing if processing thread is still active
const unsigned int GPSResponseHandler::WAIT_COMPLETE_MSEC = 5000;  // timeout waiting for signalled exit of Receiver Thread
const unsigned int GPSResponseHandler::TEST_SIGNAL_MSEC   = 100;   // timeout testing if exit has been signaled

/* Construction - Destruction */
GPSResponseHandler::GPSResponseHandler ()
:m_pCallbackContext(0), m_controlThread(NULL_HANDLE), m_dataThread(NULL_HANDLE), m_statusThread(NULL_HANDLE),
m_controlExit(false), m_dataExit(false), m_statusExit(false), m_controlExited(false), m_dataExited(false), m_statusExited(false)
{
    pthread_mutex_init(&m_controlMutex, NULL);
    pthread_mutex_init(&m_dataMutex, NULL);
    pthread_mutex_init(&m_statusMutex, NULL);
    pthread_cond_init(&m_controlCond, NULL);
    pthread_cond_init(&m_dataCond, NULL);
    pthread_cond_init(&m_statusCond, NULL);
}

GPSResponseHandler::GPSResponseHandler (void* context) 
:m_pCallbackContext(context), m_controlThread(NULL_HANDLE), m_dataThread(NULL_HANDLE), m_statusThread(NULL_HANDLE),
m_controlExit(false), m_dataExit(false), m_statusExit(false), m_controlExited(false), m_dataExited(false), m_statusExited(false)
{
    pthread_mutex_init(&m_controlMutex, NULL);
    pthread_mutex_init(&m_dataMutex, NULL);
    pthread_mutex_init(&m_statusMutex, NULL);
    pthread_cond_init(&m_controlCond, NULL);
    pthread_cond_init(&m_dataCond, NULL);
    pthread_cond_init(&m_statusCond, NULL);
}

GPSResponseHandler::~GPSResponseHandler()
{
   if ((m_controlThread != NULL_HANDLE) || (m_statusThread != NULL_HANDLE) || (m_dataThread != NULL_HANDLE))
     this->endReceiver ();

   pthread_mutex_destroy(&(m_controlMutex) );
   pthread_mutex_destroy(&(m_dataMutex) );
   pthread_mutex_destroy(&(m_statusMutex) );
   pthread_cond_destroy(&m_controlCond);
   pthread_cond_destroy(&m_dataCond);
   pthread_cond_destroy(&m_statusCond);
}

GPSStatus GPSResponseHandler::thread_create 
(
     pthread_t*   pt,
     const char*  ident,
     gps_proc_fct fct,
     void*        prm
)
{

   // create control processing thread using standard POSIX thread API and default parameters
   int val = pthread_create(pt, NULL,  fct, prm);
   if (val != 0)
   {
       *pt = NULL_HANDLE;   // null just in case
       return GPSStatusCode::GPS_E_THREAD_ALLOC;
   }

   // all ok!
   return GPSStatusCode::GPS_SUCCESS;
}

/* Default Implementation of Receiver Hook-up: Spawns receiver Thread
   using pthread API (HAL Receiver will override, as thread creation
   must be executed within GpsCallbacks context pointer */
GPSStatus  GPSResponseHandler::startReceiver (
         const char*       control_ident,    // control ident; in async processing, thread name
         gps_proc_fct      control_fct,      // control processing function; 
         void*             control_prm,      // control processing function parameter
         const char*       data_ident,
         gps_proc_fct      data_fct,
         void*             data_prm,
         const char*       status_ident,
         gps_proc_fct      status_fct,
         void*             status_prm)
{

 //  GPS_LOG_INF("+++ GPSResponseHandler::startReceiver.  control [%s, %x], data [%s, %x], status [%s, %x] +++\n",
  //     control_ident, control_fct, data_ident, data_fct, status_ident, status_fct);

   GPSStatus stat = this->thread_create(&m_controlThread, control_ident, control_fct, control_prm);
   if (stat != GPSStatusCode::GPS_SUCCESS)
   {
      GPS_LOG_ERR("Control Receiver Thread could not be created");
      return stat;
   }

   GPS_LOG_INF("Control Receiver Thread (%s) successfully started!", control_ident);

   if (data_ident) // separate data thread?
   {
      stat = this->thread_create(&m_dataThread, data_ident, data_fct, data_prm);
      if (stat != GPSStatusCode::GPS_SUCCESS)
      {
         GPS_LOG_ERR("Data Receiver Thread could not be created");
         return stat;
      }

      GPS_LOG_INF("Data Receiver Thread (%s) successfully started!", data_ident);
   }

   if (status_ident) // separate status thread?
   {
      stat = this->thread_create(&m_statusThread, status_ident, status_fct, status_prm);
      if (stat != GPSStatusCode::GPS_SUCCESS)
      {
         GPS_LOG_ERR("Status Receiver Thread could not be created");
         return stat;
      }

      GPS_LOG_INF("Status Receiver Thread (%s) successfully started!", status_ident);
   }

   return GPSStatusCode::GPS_SUCCESS;
}

// Called within Driver processing loop to retrieve the exit flag depending on receiverType
bool GPSResponseHandler::getExitSignal(receiverType type)
{
    bool exit = false;

    if (type == CONTROL_THREAD) {
        pthread_mutex_lock(&m_controlMutex);
        exit = m_controlExit;
        pthread_mutex_unlock(&m_controlMutex);
    } else if (type == DATA_THREAD) {
        pthread_mutex_lock(&m_dataMutex);
        exit = m_dataExit;
        pthread_mutex_unlock(&m_dataMutex);
    } else if (type == STATUS_THREAD) {
        pthread_mutex_lock(&m_statusMutex);
        exit = m_statusExit;
        pthread_mutex_unlock(&m_statusMutex);
    }

    return exit;
}

// Signal exit to specified thread type and wait for thread termination
void GPSResponseHandler::setExitSignal(receiverType type)
{
    GPS_LOG_TRACE("setExitSignal: type: %d", type);
    int rc;
    void *result;
    if (type == DATA_THREAD)
    {
        pthread_mutex_lock(&m_dataMutex);
        m_dataExit = true;
        while (!m_dataExited) {
            pthread_cond_wait(&m_dataCond, &m_dataMutex);
        }
        pthread_mutex_unlock(&m_dataMutex);
        GPS_LOG_INF("setExitSignal: DATA_THREAD exited");
    }
    else if (type == CONTROL_THREAD)
    {
        pthread_mutex_lock(&m_controlMutex);
        m_controlExit = true;
        while (!m_controlExited) {
            pthread_cond_wait(&m_controlCond, &m_controlMutex);
        }
        pthread_mutex_unlock(&m_controlMutex);
        GPS_LOG_INF("setExitSignal: CONTROL_THREAD exited");
    }
    else if (type == STATUS_THREAD)
    {
        pthread_mutex_lock(&m_statusMutex);
        m_statusExit = true;
        while (!m_statusExited) {
            pthread_cond_wait(&m_statusCond, &m_statusMutex);
        }
        pthread_mutex_unlock(&m_statusMutex);
        GPS_LOG_INF("setExitSignal: STATUS_THREAD exited");
    }
    GPS_LOG_TRACE("EXIT - setExitSignal: type: %d", type);
}

// Called within Driver processing thread to indicate it is about to exit
void GPSResponseHandler::exit(receiverType type)
{
    if (type == CONTROL_THREAD) {
        pthread_mutex_lock(&m_controlMutex);
        m_controlExited = true;
        pthread_cond_signal(&m_controlCond);
        pthread_mutex_unlock(&m_controlMutex);
    } else if (type == DATA_THREAD) {
        pthread_mutex_lock(&m_dataMutex);
        m_dataExited = true;
        pthread_cond_signal(&m_dataCond);
        pthread_mutex_unlock(&m_dataMutex);
    } else if (type == STATUS_THREAD) {
        pthread_mutex_lock(&m_statusMutex);
        m_statusExited = true;
        pthread_cond_signal(&m_statusCond);
        pthread_mutex_unlock(&m_statusMutex);
    }
}

// Public export to terminate Reciever thread. Signals exit condition, then
// waits by joining thread. Essentially clean shutdown. Direct trigger from
// gpsinterface::stop
GPSStatus GPSResponseHandler::endReceiver ()
{
    if ( (m_controlThread == NULL_HANDLE) && (m_dataThread == NULL_HANDLE) && (m_statusThread == NULL_HANDLE) )   
    {
       GPS_LOG_WARN("GPS Receiver has not been started!\n");
       return GPSStatusCode::GPS_SUCCESS;
    }

    // signal exit condition to Control Thread
    if (m_controlThread != NULL_HANDLE)
    {
       GPS_LOG_INF("%s: Signaling exit to Control Thread\n", "GPSResponseHandler::endReceiver");
       this->setExitSignal(CONTROL_THREAD);
       m_controlThread = NULL_HANDLE;
    }
    
    // signal exit condition to Data Thread
    if (m_dataThread != NULL_HANDLE)
    {
       GPS_LOG_INF("%s: Signaling exit to Data Thread\n", "GPSResponseHandler::endReceiver");
       this->setExitSignal(DATA_THREAD);
       m_dataThread = NULL_HANDLE;
    }

    // signal exit condition to Status Thread
    if (m_statusThread != NULL_HANDLE)
    {
       GPS_LOG_INF("%s: Signaling exit to Status Thread\n", "GPSResponseHandler::endReceiver");
       this->setExitSignal(STATUS_THREAD);
       m_statusThread = NULL_HANDLE;
    }


   return GPSStatusCode::GPS_SUCCESS;
} 



// No-op implementation of various status messages issued by Driver, that are not
// result of issued command
void GPSResponseHandler::handleStatus (GPSStatus status, GPSStatus extra)
{
   GPS_LOG_TRACE("Driver Status [0x%x] not reported to Client; they are not interested\n", status);
}





