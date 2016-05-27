/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/***************************************************************************
 *
 *  Sensor Event Reader: 
 *
 *        -- processing of data buffer retrieved from MUX device
 *        -- event caching, based on sensors.conf
 *
 ***************************************************************************/

/* Include Files */

#include "risensors.h"

/* local definitions */


/*************************************** Class Implementation **************************************/

RIEventReader::RIEventReader ()
:m_pdata(0), m_buffersize(0), m_offset(0), m_ctxhandle(0), m_ctxsize(0)
{
}

RIEventReader::~RIEventReader ()
{
   delete [] m_pdata;
}

/* allocates buffer large enough buffer to hold all that can be retrieved 
   at single read from mux, based on PROXMUX_IOCTL_GET_CONFIG */
int RIEventReader::allocateBuffer (unsigned int isize)
{
   if (m_pdata)
   {
      delete [] m_pdata;  
      m_pdata = 0; m_buffersize = 0;
   }

   m_offset = 0; m_datasize = 0;
   m_pdata = new unsigned char[isize];
   if (m_pdata == 0) return -ENOMEM;

   m_buffersize = isize;
   return 0;
}



/* Single I/O. Blocks. Returns # of bytes available for processing in internal buffer, or negative value on failure */
int RIEventReader::Read (int mux)
{
    if (m_datasize >= m_buffersize)
    {
       ALOGE("*** Unable to read from MUX device -- Input Buffer Full ***\n");
       return -EIO;
    }

    int  iRead = read (mux, m_pdata + m_datasize, m_buffersize - m_datasize);
    if (iRead < 0)
    {
       // this was a bug in initial implementation. Must check for negative value, otherwise offsets
       // will get all screwed up!!!
       ALOGE("*** Error (%d) transfering data from Kernel to User space! ***", errno);
       return -errno;
    }

    // if we get 0, this is a warning but we can continue
    if (iRead == 0)
         ALOGI("*** MUX signal received, but no events returned. ***\n");

    m_datasize += iRead;
    return (m_datasize - m_offset);   // how much is available for processing
}

/* Parse internal buffer. Offsets are adjusted and Cache populated, based on settings */
int RIEventReader::Parse(sensors_event_t* data, int count, RISensorsConf* pConf)
{
   int numEvents = 0;
   sensors_event_t* pevent = data;          // start of google buffer
   
   while (m_offset < m_datasize)
   {
       // get the context
       if (m_ctxhandle == 0x00)
       {
           memcpy (&m_ctxhandle, m_pdata + m_offset, sizeof(RI_SENSOR_HANDLE) ); m_offset += sizeof(RI_SENSOR_HANDLE);
           memcpy (&m_ctxsize, m_pdata + m_offset, sizeof(RI_DATA_SIZE) ); m_offset+= sizeof(RI_DATA_SIZE);
       }
       else
           SENSD("*** RIEventReader::Parse  Processing Previous Read. Sensor [0x%x], Remaining Payload: [%d]. ***\n", m_ctxhandle, m_ctxsize);

       while (m_ctxsize > 0)
       {
           // check if google buffer is full
           if (numEvents >= count)
           {
              SENSD("*** RIEventReader::Parse  Android Buffer exhausted. Current Event: [0x%x], Left: [%d]. Total unprocessed bytes: [%d] ***\n", m_ctxhandle, m_ctxsize, m_datasize - m_offset);

              return numEvents;
           }

           // process this event. This also adjusts internal buffers, and adds to cache
           int evt = data_handler (pevent, pConf);

           // increment google buffer
           numEvents+= evt; pevent+= evt;
       }

       // if we are here, reset context 
       m_ctxhandle = 0x00; m_ctxsize = 0;
   }

   // if we are here, we processed entire buffer
   m_offset = 0; m_datasize = 0;
 
   return numEvents;
}

int RIEventReader::data_handler   (sensors_event_t* pa, RISensorsConf* pc)
{
     ri_sensors_conf* pconf = pc->GetConfEntry(m_ctxhandle);
     if (pconf == 0)   // can not proceed, as we don't know data size
     {
        SENSD("+++ RIEventReader::data_handler Not processing event [0x%x] -- Configuration Entry not found +++\n", m_ctxhandle);
        return 0;
     }

     // set the event type
     pa->sensor = m_ctxhandle;

     // fill rest of common part
     fill_common (pa);

     // transfer data from risensorevent
     switch (pconf->dataSize)
     {
          // Data is 2 bytes long
          case DS_SHORT:
          {
             short* v  = (short*)(m_pdata + m_offset); 
             pa->data[0] = *v;
          }
          break;

          // Data is 4 bytes long
          case DS_LONG:
          {
             int* v = (int*)(m_pdata + m_offset);
	          pa->data[0] = *v; 
          }
          break;

          // Data is 6 bytes long (3 shorts, i.e. accelerometer)
          case DS_SHORT3:
          {
             short* v  = (short*)(m_pdata + m_offset); 
             pa->data[0] = *v; 

             v = (short*)(m_pdata + m_offset + sizeof(short) );
             pa->data[1] = *v; 

             v = (short*)(m_pdata + m_offset + 2*sizeof(short) );
             pa->data[2] = *v; 

          }
          break;
 
          // Data is event time in jiffies; use that and set event itself to 0
          case DS_TIMESTAMP:
          {
             struct timespec* t = (struct timespec*)(m_pdata + m_offset);
             pa->timestamp = int64_t(t->tv_sec)*1000000000LL + t->tv_nsec;

             memset (pa->data, 0, sizeof(float) * 16);
             ALOGI("+++ RIEventReader::data_handler. DS_TIMESTAMP [%ld] sec, [%ld] nsec +++", t->tv_sec, t->tv_nsec);
          }
          break;

          default:
              ALOGI("+++ RIEventReader::data_handler Not processing event [0x%x] -- Unknown / Unsupported Data Size +++\n", m_ctxhandle);
              return 0; 

       }

       // adjust offset
       m_offset += pconf->dataSize;
       m_ctxsize -= pconf->dataSize;

       // filter data based on conf rules
       filter_data (pa, pconf);
 
       return 1;  // all ok, single event filled
}

// common fill of event data regardles of handler
void RIEventReader::fill_common (sensors_event_t* pa)
{
     pa->type = RISensorUtilities::TypeFromHandle(pa->sensor);
     pa->version   = sizeof(struct sensors_event_t);
     pa->timestamp = RISensorUtilities::getTimestamp(); 
}

// post-process filtering based on sensors.conf rules. This is internal
// routine & we expect pointers to be valid
void RIEventReader::filter_data
(
  sensors_event_t*   pa,     // constructed event
  ri_sensors_conf*   pconf   // configuration entry for this event
)
{
     // perform conversion based on sensors.conf rules
     switch (pconf->dataSize)
     {
         // Data is 2 bytes long
         case DS_SHORT:
         {
            pa->data[0] = (pa->data[0] * pconf->conv_A[0]) + pconf->conv_B[0];
         }
         break;

         // Data is 4 bytes long
         case DS_LONG:
         {
            pa->data[0] = (pa->data[0] * pconf->conv_A[0]) + pconf->conv_B[0];
         }
         break;

         // Data is 6 bytes long (3 shorts, i.e. accelerometer)
         case DS_SHORT3:
         {
            float value[3];
            if (pconf->multiplier_matrix_enabled == MATRIX_PARSE_COMPLETE){
                memcpy(value, pa->data, sizeof(value));

                pa->data[0] = pconf->mul_A1[0]*value[0] + pconf->mul_A1[1]*value[1] + pconf->mul_A1[2]*value[2] + pconf->conv_B[0];
                pa->data[1] = pconf->mul_A2[0]*value[0] + pconf->mul_A2[1]*value[1] + pconf->mul_A2[2]*value[2] + pconf->conv_B[1];
                pa->data[2] = pconf->mul_A3[0]*value[0] + pconf->mul_A3[1]*value[1] + pconf->mul_A3[2]*value[2] + pconf->conv_B[2];
            }
            else {
                pa->data[0] = (pa->data[0] * pconf->conv_A[0]) + pconf->conv_B[0];
                pa->data[1] = (pa->data[1] * pconf->conv_A[1]) + pconf->conv_B[1];
                pa->data[2] = (pa->data[2] * pconf->conv_A[2]) + pconf->conv_B[2];
            }
            

            // A x = b
            // b0 = A00 x0 + A01 x1 + A02 x2
            // b1 = A10 x0 + A11 x1 + A12 x2
            // b2 = A20 x0 + A21 x1 + A22 x2
            if (pconf->rot_enabled == MATRIX_PARSE_COMPLETE)
            {
                memcpy(value, pa->data, sizeof(value));
                pa->data[0] = pconf->rot_A[0]*value[0] + pconf->rot_A[1]*value[1] + pconf->rot_A[2]*value[2];
                pa->data[1] = pconf->rot_B[0]*value[0] + pconf->rot_B[1]*value[1] + pconf->rot_B[2]*value[2];
                pa->data[2] = pconf->rot_C[0]*value[0] + pconf->rot_C[1]*value[1] + pconf->rot_C[2]*value[2];
            }
         }
         break;

         default:
            break;  // simply leave as is 

      }   // switch
}
