#ifndef __gpsresponsehandler_h__
#define __gpsresponsehandler_h__

#include "recongps.h"


/* Object responsible for processing of responses from GPSDriver.
   On success, invokes Android callbacks */

class GPSResponseHandler
{
   // constants
   protected:
      static const unsigned int TEST_COMPLETE_MSEC;   // Timeout test if Receiver has terminated
      static const unsigned int WAIT_COMPLETE_MSEC;   // Timeout wait for Reciever to finish when exit is signalled
      static const unsigned int TEST_SIGNAL_MSEC;     // Timeout wait for Receiver to test whether exit has been signalled

   public:

      // receiver start. This is essentially thread creation, but made virtual as for
      // HAL it must execute within context of Android specified callbacks. Default implementation
      // uses simple pthreads
      GPSStatus  startReceiver (
         const char*       control_ident,    // control ident; in async processing, thread name
         gps_proc_fct      control_fct,      // control processing function; 
         void*             control_prm,      // control processing function parameter
         const char*       data_ident,       // data ident
         gps_proc_fct      data_fct,         // data processing function
         void*             data_prm,         // data processing function parameter
         const char*       status_ident,     // status ident
         gps_proc_fct      status_fct,       // status processing function
         void*             status_prm);      // status processing function parameter


      // response handler, specific to each client
      virtual void  commandCompleted(GPSStatus cmd, GPSStatus result) = 0; // response to issued driver command
      virtual void  handleData      (gpsmessage*) = 0;                     // gps data
      virtual void  handleStatus    (GPSStatus status, GPSStatus extra = 0);   // other driver events (not command responses)

   public:
      GPSResponseHandler (void* context);
      virtual ~GPSResponseHandler ();

      // signals end of receiver thread
      GPSStatus endReceiver();   

      // called inside processing function, to test and retrieve exit condition
      bool getExitSignal(receiverType type);
      void setExitSignal(receiverType type);

      void exit(receiverType type);

   protected:
      void*           m_pCallbackContext;                       // receiver callback context; will be GpsCallbacks for HAL

      pthread_t       m_controlThread;                          // Control Processing thread handle
      pthread_t       m_dataThread;                             // Data Processing thread handle
      pthread_t       m_statusThread;                           // Status Processing thread handle

      pthread_mutex_t m_controlMutex;                           // Control Processing Thread Mutex
      pthread_mutex_t m_dataMutex;                              // Data Processing Thread Mutex
      pthread_mutex_t m_statusMutex;                            // Status Processing Thread Mutex

      pthread_cond_t  m_controlCond;                            // Control processing thread exit condition
      pthread_cond_t  m_dataCond;                               // Data processing thread exit condition
      pthread_cond_t  m_statusCond;                             // Status processing thread exit condition

      bool            m_controlExit;                            // Control processing thread exit flag
      bool            m_dataExit;                               // Data processing thread exit flag
      bool            m_statusExit;                             // Status processing thread exit flag

      bool            m_controlExited;                          // Control processing thread has exited flag
      bool            m_dataExited;                             // Data processing thread has exited flag
      bool            m_statusExited;                           // Status processing thraed has exited flag


      virtual GPSStatus  thread_create (
         pthread_t*        pt,
         const char*       ident,    
         gps_proc_fct      fct,      
         void*             prm);     

   protected:
      GPSResponseHandler ();

};

#endif  // __gpsresponsehandler_h_
