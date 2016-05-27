/*
 *******************************************************************************
 *         
 *
 * FileName         :   GpsDriver.cpp
 *
 * Description      :   Implementation of GPS Driver Base Class
 *
 * Author           :   RECON Instruments
 * Date             :   November 2013
 *
 * Comments         :  
 ******************************************************************************
 */   

#define LOG_TAG "RECON.GPS"   // our identification for logcat (adb logcat RECON.GPS:V *:S)

#include <hardware/gps.h>
#include <stdlib.h>
#include <fcntl.h>
#include <stdio.h>
#include <sys/stat.h>
#include <unistd.h>
#include <errno.h>

#include "recongps.h"    // main HAL include
#include "gpsdriver.h"   // this header


// concrete OEM headers (for factory)
#include "tigpsdriver.h"               // TI GPS Driver Interface

// literal constants initialization
const unsigned int GPSDriver::MAX_BUF_LENGTH = 128;
const char* GPSDriver::GPS_CONFIG_FILE_TOKEN_SEPARATOR = ":";

const char* GPSDriver::DATA_PROCESSING_THREAD    = "dataCBthread";
const char* GPSDriver::STATUS_PROCESSING_THREAD  = "statusCBthread";

const char* GPSDriver::TOKEN_GPS_MODE            = "gps_mode";
const char* GPSDriver::TOKEN_RECURRENCE          = "recurrence";
const char* GPSDriver::TOKEN_FIX_FREQUENCY       = "fix_frequency";
const char* GPSDriver::TOKEN_HOR_ACC             = "hor_acc";
const char* GPSDriver::TOKEN_RSP_TIME            = "response_time";
const char* GPSDriver::TOKEN_LOC_AGE             = "loc_age";
const char* GPSDriver::TOKEN_TT_FF               = "ttff";

const char* GPSDriver::GPS_AUTONOMOUS_MODE       = "autonomous_mode";
const char* GPSDriver::GPS_MSBASED_MODE          = "msbased_mode";
const char* GPSDriver::GPS_MSASSISTED_MODE       = "msassisted_mode";

const char* GPSDriver::DATA_PORT                 = "/data/gps/recongps";
const char* GPSDriver::STATUS_PORT              = "/data/gps/reconcontrol";

const int   GPSDriver::SLEEP_RECONNECT_SECS      = 10;

const unsigned short GPSDriver::MAX_HOR_ACC_VALUE  = 0xFFFF;
const unsigned short GPSDriver::MAX_LOC_AGE_VALUE  = 0xFF;
const unsigned short GPSDriver::MAX_RSP_TIME_VALUE = 0xFFFF;
const unsigned short GPSDriver::MAX_TT_FF_VALUE    = 500;   // seconds to first fix



GPSDriver::GPSDriver()
:m_dataPort(-1), m_statusPort(-1), m_pResponseHandler(0), m_pGpsWatchdog(NULL), m_enableWatchdog(0), m_sessionID(0)
{
}

// even if destructor is pure virtual, it must have a body; otherwise there will be link error
GPSDriver::~GPSDriver()   
{
    this->closeDataPort();
    this->closeStatusPort();
    this->disableWatchdog();
}



/* Factory method for OEM specific instantiation */
GPSDriver* GPSDriver::Factory(OemDriverEnum id)
{
   GPSDriver* ret = 0;

   GPS_LOG_TRACE("Enter\n");
   switch (id)
   {
       case TI_GPS:
       {
          GPS_LOG_INF("Allocating TI NaviLink GPS Driver");
          ret = new TIGPSDriver();
       }
       break;

       default:
          GPS_LOG_ERR("Requested OEM Driver (%d) not supported", id);
          return ret;
   }

   if (!ret)
   {
      GPS_LOG_ERR("Memory Allocation Error\n");
      return ret;
   }

   GPS_LOG_TRACE("OK\n");
   return ret;
}



/* Configuration File Parsing. Common functionality abstracted in base class
   Derived classes can override if they have specific settings, or different format. */
GPSStatus GPSDriver::readConfigFile(const char* pszPath)
{
    char  a_inputBuffer[GPSDriver::MAX_BUF_LENGTH] = {'\0'};
    char* p_token = 0;

    GPS_LOG_TRACE("Enter\n");

    FILE* fp = fopen(pszPath, "r");
    if (fp == 0)
    {
        GPS_LOG_ERR("Failed to open [%s] Configuration File !!!", pszPath);
        return GPSStatusCode::GPS_E_CONFIG;
    }

    while( (fgets(a_inputBuffer, sizeof(a_inputBuffer), fp) ) &&
           (strcmp(a_inputBuffer, "\n") != 0) )
    {
        p_token = (char*)strtok(a_inputBuffer, GPSDriver::GPS_CONFIG_FILE_TOKEN_SEPARATOR);
        if (!p_token )
            continue;
  
        /* Parsing gpspositionmode::mode */
        if ((strcmp(p_token, GPSDriver::TOKEN_GPS_MODE) == 0) )
        {
            p_token = (char*) strtok(0, GPSDriver::GPS_CONFIG_FILE_TOKEN_SEPARATOR);

            if (!p_token)
            {
                GPS_LOG_ERR("Error parsing [%s] Configuration File (strtok returned NULL) !!!", pszPath);
                fclose(fp);

                return GPSStatusCode::GPS_E_CONFIG;
            }

            if (strcmp(p_token, GPSDriver::GPS_AUTONOMOUS_MODE) == 0 )
            {
                m_settings.mode = GPS_POSITION_MODE_STANDALONE;  // gps.h
            }

            else if (strcmp(p_token, GPSDriver::GPS_MSBASED_MODE) == 0)
            {
                m_settings.mode = GPS_POSITION_MODE_MS_BASED;
            }

            else if (strcmp(p_token, GPSDriver::GPS_MSASSISTED_MODE) == 0)
            {
                m_settings.mode = GPS_POSITION_MODE_MS_ASSISTED;
            }

            else
            {
                GPS_LOG_ERR("Position Fix Mode [%s] not supported\n", p_token);
                fclose(fp);

                return GPSStatusCode::GPS_E_CONFIG;
            }
       }

       /* Parsing recurrence */
       else if ((strcmp(p_token, GPSDriver::TOKEN_RECURRENCE) == 0))
       {
           p_token = (char*) strtok(0, GPSDriver::GPS_CONFIG_FILE_TOKEN_SEPARATOR);
           if (!p_token)
           {
                GPS_LOG_ERR("Error parsing [%s] Configuration File (strtok returned NULL) !!!", pszPath);
                fclose(fp);

                return GPSStatusCode::GPS_E_CONFIG;
           }
           else
           {
                m_settings.recurrence = atoi(p_token);
           }
       }

       /* Parsing fix_frequency */
       else if ((strcmp(p_token, GPSDriver::TOKEN_FIX_FREQUENCY) == 0))
       {
           p_token = (char*) strtok(0, GPSDriver::GPS_CONFIG_FILE_TOKEN_SEPARATOR);
           if (!p_token)
           {
                GPS_LOG_ERR("Error parsing [%s] Configuration File (strtok returned NULL) !!!", pszPath);
                fclose(fp);

                return GPSStatusCode::GPS_E_CONFIG;
           }
           else
           {
                m_settings.fix_frequency = atoi(p_token);
           }
       }
   
       /* Parsing preferred_accuracy */
       else if ((strcmp(p_token, GPSDriver::TOKEN_HOR_ACC) == 0))
       {
            p_token = (char*) strtok(0, GPSDriver::GPS_CONFIG_FILE_TOKEN_SEPARATOR);
            if (!p_token)
            {
                GPS_LOG_ERR("Error parsing [%s] Configuration File (strtok returned NULL) !!!", pszPath);
                fclose(fp);

                return GPSStatusCode::GPS_E_CONFIG;
            }

            else
            {
                m_settings.preferred_accuracy = atoi(p_token);
                if (m_settings.preferred_accuracy > GPSDriver::MAX_HOR_ACC_VALUE)
                   m_settings.preferred_accuracy = GPSDriver::MAX_HOR_ACC_VALUE;
            }
        }

       /* Parsing response time */
        else if ((strcmp(p_token, GPSDriver::TOKEN_RSP_TIME) == 0))
        {
            p_token = (char*) strtok(0, GPSDriver::GPS_CONFIG_FILE_TOKEN_SEPARATOR);
            if (!p_token)
            {
                GPS_LOG_ERR("Error parsing [%s] Configuration File (strtok returned NULL) !!!", pszPath);
                fclose(fp);

                return GPSStatusCode::GPS_E_CONFIG;
            }
            else
            {
                m_settings.preferred_time = atoi(p_token);

               if (m_settings.preferred_time > GPSDriver::MAX_RSP_TIME_VALUE)
                    m_settings.preferred_time = GPSDriver::MAX_RSP_TIME_VALUE; 
            }
        }

        /* Parsing location age */
        else if ((strcmp(p_token, GPSDriver::TOKEN_LOC_AGE) == 0))
        {
            p_token = (char*) strtok(0, GPSDriver::GPS_CONFIG_FILE_TOKEN_SEPARATOR);
            if (!p_token)
            {
                GPS_LOG_ERR("Error parsing [%s] Configuration File (strtok returned NULL) !!!", pszPath);
                fclose(fp);

                return GPSStatusCode::GPS_E_CONFIG;
            }
            else
            {  
                m_locage = atoi(p_token);

                if (m_locage > GPSDriver::MAX_LOC_AGE_VALUE)
                    m_locage = GPSDriver::MAX_LOC_AGE_VALUE; 
            }
        }

        // Parsing time to first fix
        else if ((strcmp(p_token, GPSDriver::TOKEN_TT_FF) == 0))
        {
            p_token = (char*) strtok(0, GPSDriver::GPS_CONFIG_FILE_TOKEN_SEPARATOR);
            if (!p_token)
            {
                GPS_LOG_ERR("Error parsing [%s] Configuration File (strtok returned NULL) !!!", pszPath);
                fclose(fp);

                return GPSStatusCode::GPS_E_CONFIG;
            }
            else
            {  
                m_settings.tt_ff = atoi(p_token);

                if (m_settings.tt_ff > GPSDriver::MAX_TT_FF_VALUE)
                    m_settings.tt_ff = GPSDriver::MAX_TT_FF_VALUE; 
            }
        }
        /** NOTE: Recurrence (whatever that means) and fix frequency not currently in TI config file ***/
    }

    fclose(fp);

    GPS_LOG_INF("GPSDriver Config Settings:  Mode [%d], Recurrence [%d], Fix Frequency [%d], Preferred Accuracy [%d], Preferred Time: [%d], Location Age: [%d]\n",
        m_settings.mode, m_settings.recurrence, m_settings.fix_frequency, m_settings.preferred_accuracy, m_settings.preferred_time, m_locage);


    GPS_LOG_TRACE("OK\n");
    return GPSStatusCode::GPS_SUCCESS;
}

GPSStatus GPSDriver::openDataPort  ()
{   
    this->closeDataPort();

    // now open data port
    m_dataPort = ::open(GPSDriver::DATA_PORT, O_RDONLY);
    if (m_dataPort == -1)
    {
       GPS_LOG_ERR("+++ GPS Data Port [%s] Could not be open (error %d) +++\n", GPSDriver::DATA_PORT, -errno);
       return GPSStatusCode::GPS_E_CONFIG; 
    }

    return GPSStatusCode::GPS_SUCCESS;
}

void      GPSDriver::closeDataPort ()
{
   if (m_dataPort != -1)
   {
      ::close(m_dataPort);
      m_dataPort = -1;

      GPS_LOG_INF("+++ GPS Data Port has been closed +++\n");
   }

}

GPSStatus GPSDriver::openStatusPort  ()
{   
    this->closeStatusPort();

    // now open status port
    m_statusPort = ::open(GPSDriver::STATUS_PORT, O_RDONLY);
    if (m_statusPort == -1)
    {
       GPS_LOG_ERR("+++ GPS Status Port [%s] Could not be open (error %d) +++\n", GPSDriver::STATUS_PORT, -errno);
       return GPSStatusCode::GPS_E_CONFIG; 
    }

    return GPSStatusCode::GPS_SUCCESS;
}

void GPSDriver::closeStatusPort ()
{
   if (m_statusPort != -1)
   {
      ::close(m_statusPort);
      m_statusPort = -1;

      GPS_LOG_INF("+++ GPS Status Port has been closed +++\n");
   }
}

/* Internal Helper to read complete data buffer. Returns 0 if there is nothing to read on
   the fd. Returns # of bytes read on successful read, this will be the requested size.
   On Failure, it will be < 0 */
int GPSDriver::read_buffer(int descriptor, unsigned char* buffer, unsigned int size)
{
    unsigned int iRead = 0;
    struct timeval tv;
    fd_set rfds;
    int res;

    for (;;) {
        tv.tv_sec = 0;
        tv.tv_usec = READ_BUF_TIMEOUT;
        FD_ZERO(&rfds);
        FD_SET(descriptor, &rfds);
        res = select(descriptor + 1, &rfds, NULL, NULL, &tv);
        if (res < 0) {
            GPS_LOG_ERR("read_buffer: Select returned error: %d", res);
            break;
        } else if (res == 0) {
            // If nothing to read, break out because we don't want to block.
            break;
        } else if (res > 0) {
            while (iRead < size)
            {
                int i = ::read(descriptor, buffer + iRead, size - iRead);
                if (i <= 0)
                {
                   GPS_LOG_ERR("Error [%d] reading on connected descriptor!\n", errno);
                   return -1; // will signal error
                }
                iRead += i;
            }

            return iRead;
        }
    }
    return res;
}

/* Internal helper to write entire data buffer. Returns # of bytes written
   on success, this will be entire buffer size. On Failure, it will be between 0 and requested */
unsigned int GPSDriver::write_buffer (int descriptor, unsigned char* buffer, unsigned int size)
{
    unsigned int iWritten = 0;
    while (iWritten < size)
    {
        int i = ::write(descriptor, buffer + iWritten, size - iWritten);
        if (i <= 0) 
        {
           GPS_LOG_ERR("Error [%d] writting on connected descriptor!\n", errno);
           break;
        }
        iWritten += i;
    }

    return iWritten;
}

void GPSDriver::pingWatchdog(int ping) {
    if (m_enableWatchdog & ping) {
        m_pGpsWatchdog->pingWatchdog(ping);
    }
}

void GPSDriver::enableWatchdog(int enable) {
    m_enableWatchdog |= enable;
    if (m_enableWatchdog != 0 && m_pGpsWatchdog == NULL) {
        GPS_LOG_INF("Creating new watchdog thread");
        m_pGpsWatchdog = new GpsWatchdog(m_pResponseHandler, enable);
        m_pGpsWatchdog->run("GpsWatchdog");
    }
}

void GPSDriver::disableWatchdog() {
    if (m_enableWatchdog && m_pGpsWatchdog != NULL) {
        m_enableWatchdog = 0;
        m_pGpsWatchdog->requestExit();
        m_pGpsWatchdog.clear();
    }
}

int GPSDriver::getEnableWatchdog() {
    return m_enableWatchdog;
}

void* GPSDriver::DataResponseHandler(void* contextPtr)
{
    GPSDriver* pThis = (GPSDriver*)contextPtr;
    GPSStatus retVal = GPSStatusCode::GPS_SUCCESS;
    unsigned int i_oldlen = 0;
    gpsmessage msg;   // c-tor initializes
    unsigned int i_headerLen = sizeof(msg.opcode) + sizeof(msg.i_msglen);

    GPS_LOG_INF("+++ GPSDriver: RECON Data Thread is starting +++\n");

    // first time around, open data port. This will block till server has enabled it, but this is ok
    // as we are executing in separate thread and not blocking HAL
    retVal = pThis->openDataPort ();
    if (retVal != GPSStatusCode::GPS_SUCCESS)
    { 
        GPS_LOG_INF("+++ GPSDriver: RECON Data Port could not be opened. Data Processing thread exiting +++\n");
        return 0;
    }

    GPS_LOG_INF("+++ GPSDriver: Data thread beginning main processing loop +++\n");

    pThis->pingWatchdog(DATA_TD_PING);

    // central processing loop. Tests exit signal on top, then blocking read on named pipe
    // should blocking read fail, this probably means socket has been disconnected. It does not terminate
    // on its own, instead keeps spinning until exit has been signalled by whoever is using services of this object
    while (!pThis->m_pResponseHandler->getExitSignal(DATA_THREAD))
    { 
        msg.opcode = 0; msg.i_msglen = 0;

        // read header
        int iread = pThis->read_buffer(pThis->m_dataPort, reinterpret_cast<unsigned char*>(&msg), i_headerLen);
        //GPS_LOG_INF("+++ GPSDriver::DataResponseHandler - Header [Code: %d, Payload: %d] +++\n", msg.opcode, msg.i_msglen);

        if (iread == (int)i_headerLen)
        {
            // do we need more memory?
            if (i_oldlen < msg.i_msglen)
            {
               delete [] msg.pPayload; msg.pPayload = 0; // safe to delete null pointer
               msg.pPayload = new unsigned char[msg.i_msglen * 2];  // alloc double to reduce fragmentation
               if (msg.pPayload == 0)
               {
                    GPS_LOG_ERR("Memory Allocation Error! Data Processing Function is terminating...\n");
                    return 0;
               }
               i_oldlen = msg.i_msglen * 2; // remember current length
            }

            // Reset read counter
            iread = 0;

            // Continuously try to read from the pipe until there is something read or an error occurs
            while (iread == 0) {
                iread = pThis->read_buffer(pThis->m_dataPort, msg.pPayload, msg.i_msglen);
                // now read data
                if (iread == (int)msg.i_msglen) {
                    pThis->GPSDriver::handleGps(&msg);  // call base directly for now, I'm not sure if there is mix-up with nav codes
                } else if (iread < 0) {
                    GPS_LOG_ERR("Not processing Data Pipe message [Code = %d]. Received Payload size (%d) is less than required [%d]\n",
                        msg.opcode, iread, msg.i_msglen);
                    break;
                }
            }

            pThis->pingWatchdog(DATA_WD_PING);
        } else if (iread < 0) {
            GPS_LOG_ERR("Not processing Data Pipe Message - received header size (%d) is less than required (%d)\n", iread, i_headerLen);
            break;
        }
    }

    // Signal that the DATA_THREAD is exiting
    pThis->m_pResponseHandler->exit(DATA_THREAD);

    GPS_LOG_INF("+++ GPSDriver: RECON Data Thread is exiting +++\n");
    return 0;
}


void* GPSDriver::StatusResponseHandler(void* contextPtr)
{
    GPSDriver* pThis = (GPSDriver*)contextPtr;
    GPSStatus retVal = GPSStatusCode::GPS_SUCCESS;
    unsigned int i_oldlen = 0;
    gpsmessage msg;   // c-tor initializes
    unsigned int i_headerLen = sizeof(msg.opcode) + sizeof(msg.i_msglen);

    GPS_LOG_INF("+++ GPSDriver: RECON Status Thread is starting +++\n");

    // first time around, open control port. This will block till server has enabled it, but this is ok
    // as we are executing in separate thread and not blocking anyone
    retVal = pThis->openStatusPort ();
    if (retVal != GPSStatusCode::GPS_SUCCESS)
    { 
        GPS_LOG_INF("+++ GPSDriver: RECON Status Port could not be opened. Status Processing thread exiting +++\n");
        return 0;
    }

    GPS_LOG_INF("+++ GPSDriver: Status thread beginning main processing loop +++\n");

    pThis->pingWatchdog(STATUS_TD_PING);

    // central processing loop. Tests exit signal on top, then blocking read on named pipe
    // should blocking read fail, this probably means socket has been disconnected. It does not terminate
    // on its own, instead keeps spinning until exit has been signalled by whoever is using services of this object
    while (!pThis->m_pResponseHandler->getExitSignal(STATUS_THREAD))
    { 
        msg.opcode = 0; msg.i_msglen = 0;

        // read header
        int iread = pThis->read_buffer(pThis->m_statusPort, reinterpret_cast<unsigned char*>(&msg), i_headerLen);

        if (iread == (int)i_headerLen)
        {
            // do we need more memory?
            if (i_oldlen < msg.i_msglen)
            {
               delete [] msg.pPayload; msg.pPayload = 0; // safe to delete null pointer
               msg.pPayload = new unsigned char[msg.i_msglen * 2];  // alloc double to reduce fragmentation
               if (msg.pPayload == 0)
               {
                    GPS_LOG_ERR("Memory Allocation Error! Control Processing Function is terminating...\n");
                    return 0;
               }
               i_oldlen = msg.i_msglen * 2; // remember current length
            }

            // Reset read counter
            iread = 0;

            // Continuously try to read from the pipe until there is something read or an error occurs
            while (iread == 0) {
                iread = pThis->read_buffer(pThis->m_statusPort, msg.pPayload, msg.i_msglen);

                // now read data
                if (iread == (int)msg.i_msglen) {
                    pThis->handleStatus(&msg);
                } else if (iread < 0) {
                    GPS_LOG_ERR("Not processing Status message [Code = %d]. Received Payload size (%d) is less than required [%d]\n",
                        msg.opcode, iread, msg.i_msglen);
                    break;
                }
            }

            pThis->pingWatchdog(STATUS_WD_PING);
        }
        else if (iread < 0) {
            GPS_LOG_ERR("Not processing Status Message - received header size (%d) is less than required (%d)\n", iread, i_headerLen);
            break;
        }
    }

    // Signal that the STATUS_THREAD is exiting
    pThis->m_pResponseHandler->exit(STATUS_THREAD);

    GPS_LOG_INF("+++ GPSDriver: RECON Status Thread is exiting +++\n");
    return 0;
}

// Recieved Status Dispatcher
void GPSDriver::handleStatus (gpsmessage* pmsg)
{
   switch (pmsg->opcode)
   {
      case GPSStatusCode::STATUS_GPS_REQUEST_ASSIST:
      {
         GPSDriver::processAssistanceRequest(pmsg);
      }
      break;

      default:
         GPS_LOG_ERR("Status [%d] not recognized", pmsg->opcode);
   }

}

// Received Response Dispatcher 
void GPSDriver::handleGps (gpsmessage* pmsg)
{
   switch (pmsg->opcode)
   {
      case GPSStatusCode::DATA_NMEA:
         GPSDriver::processNmeaData(pmsg);
      break;

      default:
         GPS_LOG_ERR("Data [%d] not recognized", pmsg->opcode);
   }

}

/* Assistance Request Handler */
void GPSDriver::processAssistanceRequest (gpsmessage* pmsg)
{
   GPSStatus* passist_mask = reinterpret_cast<GPSStatus*>(pmsg->pPayload); 
   GPS_LOG_INF("+++ %s: Assistance Request Received. Flags: [0x%x] +++\n", "GPSDriver::processAssistanceRequest", *passist_mask);
   m_pResponseHandler->handleStatus(GPSStatusCode::STATUS_GPS_REQUEST_ASSIST, *passist_mask);
}

/* NMEA Data Handler */
void GPSDriver::processNmeaData(gpsmessage* pmsg)
{
   char* temp_ptr = reinterpret_cast<char*>(pmsg->pPayload);
   char* p_token = strtok(temp_ptr, "$");

   if (!p_token)
   {
      pmsg->pPayload[pmsg->i_msglen] = '\0';
      GPS_LOG_ERR("Invalid NMEA buffer received (No leading '$' found). Data: [%s]\n", pmsg->pPayload);

      return;
   }

   // send message to response handler
   m_pResponseHandler->handleData(pmsg);   // must complete sync

}





