/*****< SS1A2DP.c >************************************************************/
/*      Copyright 2000 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  SS1A2DP - Stonestreet One A2DP interface for Android AudioFlinger         */
/*                                                                            */
/*  Author:  Greg Hensley                                                     */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   09/15/10  G. Hensley     Initial creation.                               */
/******************************************************************************/

#define LOG_TAG "SS1A2DP"

#include "utils/Log.h"

#include <pthread.h>
#include <errno.h>
#include <time.h>

#include "SS1BTPM.h"
#include "SS1BTAUDM.h"

#include "SS1A2DP.h"


   /* Send timing information to the system log.                        */
#define ENABLE_TIMING_LOGS 0

   /* Send SBC codec details to the system log.                         */
#define ENABLE_SBC_LOGS 0

   /* The audio sample rate must be corrected for skew. By default,     */
   /* we correct by varying the number of SBC frames included in each   */
   /* packet.  If CORRECT_SKEW_VIA_TIMING is non-zero, instead send     */
   /* a constant number of SBC frames while varying the frequency of    */
   /* packet transmission.                                              */
#define CORRECT_SKEW_VIA_TIMING 0

   /* If non-zero, reorder the calculations for packet delay such that  */
   /* additional rounding error is introduced. This is less exact but   */
   /* mimics the behavior of bluez.                                     */ 
#define PACKET_DELAY_MICROSEC 0

   /* Android provides a definition of clock_nanosleep() but fails to   */
   /* include it in time.h as POSIX defines it should be.               */
extern int clock_nanosleep(clockid_t clock_id, int flags, const struct timespec *request, struct timespec *remain);

#define NANOSEC_S  (1000000000L)
#define NANOSEC_MS (1000000L)
#define NANOSEC_US (1000L)
#define MICROSEC_S (1000000L)

#define CONNECT_FLAGS (AUDM_CONNECT_AUDIO_STREAM_FLAGS_REQUIRE_AUTHENTICATION | AUDM_CONNECT_AUDIO_STREAM_FLAGS_REQUIRE_ENCRYPTION)

#define MAXIMUM_BIT_POOL 55

#define STATE_TRANSITION_TIMEOUT_MS 10000
#define CONNECTION_TIMEOUT_MS        2000

#define VALID_FREQUENCY(F) ((F == 48000) || (F == 44100) || (F == 32000) || (F == 1600))
#define VALID_CHANNELS(C)  ((C == 2) || (C == 1))


typedef enum _tagA2DP_State_t
{
   a2dpStateDisconnected,
   a2dpStateConnected,
   a2dpStateResuming,
   a2dpStatePlaying,
   a2dpStateSuspending
} A2DP_State_t;

typedef struct _tagA2DP_NativeData_t
{
   int             CallbackID;
   Boolean_t       InitCatchup;
   BD_ADDR_t       SinkAddress;
   unsigned int    Frequency;
   unsigned int    Channels;
   unsigned int    MTU;
   A2DP_State_t    State;
   pthread_cond_t  StateChangeCond;
   unsigned int    LastConnectionResult;

#if ENABLE_TIMING_LOGS
   Boolean_t       FirstPacket;
#endif

   /* The SessionMutex protects access to the session state (the above  */
   /* members, initialized in a2dp_init()). The WriteMutex protects     */
   /* access to those members initialized after connection establishment*/
   /* (below).                                                          */
   pthread_mutex_t SessionMutex;
   pthread_mutex_t WriteMutex;

   /* The following members are initialized after a connection is       */
   /* established and are cleaned up when the connection is torn down.  */
   Encoder_t       Encoder;
   unsigned char  *SBCFrameBuffer;
   unsigned int    SBCFrameCount;
   unsigned int    SBCFrameSize;
   unsigned int    SBCFramesPerPacket;
   unsigned int    SampleSize;
#if CORRECT_SKEW_VIA_TIMING
   long long       ExtraTimePerInterval;
   long long       UnderrunTime;
   long long       ExtraTimeCount;
#else
   unsigned int    ExtraSamplesPerInterval;
   unsigned int    UnderrunSamples;
   unsigned int    ExtraSamplesCount;
#endif
   struct timespec NextWriteTime;
   struct timespec PacketDelay;
} A2DP_NativeData_t;

   /* The stack currently only supports connecting a single A2DP device */
   /* at a time. To protect our state against two calls to a2dp_init()  */
   /* without an intervening a2dp_cleanup(), we keep track of the       */
   /* currently valid A2DP handle.                                      */
volatile A2DP_Handle_t CurrentHandle = NULL;

   /* When cleaning up the connection state, we set the CurrentHandle   */
   /* to NULL to avoid future API calls from accessing the state        */
   /* while it is being destroyed. The exception to this is the event   */
   /* calback handler: we need to be able to receive and process the    */
   /* Disconnected event. To that end, the currently _closing_ handle   */
   /* is stored here. Only the cleanup and event callback functions may */
   /* access this storage.                                              */
volatile A2DP_Handle_t ClosingHandle = NULL;

#if ENABLE_TIMING_LOGS

#define TimeSpecToNS(TS) ((((long long)((TS).tv_sec)) * NANOSEC_S) + (long long)((TS).tv_nsec))

static inline long long CurrentTimeNS()
{
   struct timespec Time;

   if(clock_gettime(CLOCK_MONOTONIC, &Time) == 0)
      return(TimeSpecToNS(Time));
   else
      return(0);
}

#endif

   /* The following function is provided to assert that a timespec      */
   /* struct is in a valid format. To be valid, the tv_nsec field must  */
   /* contain a sub-second offset and both the second and nanosecond    */
   /* fields must be positive. In other words, tv_nsec must be in the   */
   /* interval [0,999999999], inclusive, and tv_sec must be >= 0.       */
static inline int IsValidTimespec(const struct timespec *TS)
{
   return((TS) && (TS->tv_nsec < NANOSEC_S) && (TS->tv_nsec >= 0) && (TS->tv_sec >= 0));
}

   /* The following function is used to transform an invalid timespec   */
   /* structure into a valid form. See IsValidTimespec() for a          */
   /* definition of a valid timespec. The result will be a timespec     */
   /* which encodes a positive magnitude where both component vectors   */
   /* (tv_sec and tv_nsec) are greater than or equal to zero.           */
static inline void NormalizeTimespec(struct timespec *TS)
{
   if(TS)
   {
      /* Ensure the tv_nsec field is positive so we can easily correct a*/
      /* magnitude of 1 second (1e9 nanoseconds) or more.               */
      if(TS->tv_nsec < 0)
      {
         TS->tv_sec  = -(TS->tv_sec);
         TS->tv_nsec = -(TS->tv_nsec);
      }

      /* Fix a positive out-of-bounds state in the tv_nsec field. If the*/
      /* tv_nsec field's value represents one second of time or more,   */
      /* add the number of whole seconds to the tv_sec field and store  */
      /* only the sub-second component in tv_nsec.                      */
      if(TS->tv_nsec >= (2 * NANOSEC_S))
      {
         TS->tv_sec += (TS->tv_nsec / NANOSEC_S);
         TS->tv_nsec = (TS->tv_nsec % NANOSEC_S);
      }
      else
      {
         if(TS->tv_nsec >= NANOSEC_S)
         {
            TS->tv_sec  += 1;
            TS->tv_nsec -= NANOSEC_S;
         }
      }

      /* Now, the nanosecond offset is guaranteed to be a positive      */
      /* value between 0 and 999999999, and the magnitude component     */
      /* represented by the tv_sec field is either zero or greater than */
      /* that of tv_nsec. This means the overall sign of the timespec is*/
      /* determined by the tv_sec field.                                */

      /* Check whether the timespec is negative.                        */
      if(TS->tv_sec < 0)
      {
         /* The timespec is negative, so we'll flip the timespec and    */
         /* re-normalize the possibly negative nanosecond field.        */
         TS->tv_sec = -(TS->tv_sec);

         if(TS->tv_nsec > 0)
         {
            /* tv_nsec was positive and will become negative when       */
            /* flipped. Fix this by shifting one positive second of time*/
            /* from tv_sec to tv_nsec so tv_nsec will again contain a   */
            /* positive offset. This can't make tv_sec negative again   */
            /* because it must have contained a magnitude of at least 1 */
            /* in order to reach this codepath.                         */
            TS->tv_sec -= 1;
            TS->tv_nsec = (NANOSEC_S - TS->tv_nsec);
         }
      }
   }
}

   /* The following function calculates the sum of two timespec         */
   /* structures. The timespec structures must be valid according       */
   /* to IsValidTimespec(). If either parameter is NULL, a timespec     */
   /* representing zero time will be returned. If either parameter is an*/
   /* invalid timespec structure, the result is undefined.              */
static inline struct timespec AddTimespec(const struct timespec *TS1, const struct timespec *TS2)
{
   struct timespec Dest;

   if(TS1 && TS2)
   {
      Dest.tv_sec  = TS1->tv_sec + TS2->tv_sec;
      Dest.tv_nsec = TS1->tv_nsec + TS2->tv_nsec;
      NormalizeTimespec(&Dest);
   }
   else
      memset(&Dest, 0, sizeof(Dest));

   return(Dest);
}

   /* The following function calculates the absolute difference         */
   /* between two timespec structures. The timespec structures must     */
   /* be valid according to IsValidTimespec(). If either parameter is   */
   /* NULL, a timespec representing zero time will be returned. If      */
   /* either parameter is an invalid timespec structure, the result is  */
   /* undefined.                                                        */
static inline struct timespec DiffTimespec(const struct timespec *TS1, const struct timespec *TS2)
{
   struct timespec Dest;

   if(TS1 && TS2)
   {
      Dest.tv_sec  = TS1->tv_sec - TS2->tv_sec;
      Dest.tv_nsec = TS1->tv_nsec - TS2->tv_nsec;
      NormalizeTimespec(&Dest);
   }
   else
      memset(&Dest, 0, sizeof(Dest));

   return(Dest);
}

   /* The following function is used to compare two timespec structures.*/
   /* The structures must be valid according to IsValidTimespec(). The  */
   /* function returns -1, 0, or 1 if TS1 is, respectively, less than,  */
   /* equal to, or greater than TS2. If either parameter is NULL, the   */
   /* value 0 is returned. If either parameter is an invalid timespec   */
   /* structure, the result is undefined.                               */
static inline int CmpTimespec(const struct timespec *TS1, const struct timespec *TS2)
{
   int ret_val;

   if(TS1 && TS2)
   {
      if(TS1->tv_sec == TS2->tv_sec)
      {
         if(TS1->tv_nsec == TS2->tv_nsec)
            ret_val = 0;
         else
         {
            if(TS1->tv_nsec > TS2->tv_nsec)
               ret_val = 1;
            else
               ret_val = -1;
         }
      }
      else
      {
         if(TS1->tv_sec > TS2->tv_sec)
            ret_val = 1;
         else
            ret_val = -1;
      }
   }
   else
      ret_val = 0;

   return(ret_val);
}

static void A2DP_AUDMEventCallback(AUDM_Event_Data_t *EventData, void *CallbackParameter)
{
   A2DP_NativeData_t *NatData;

   SS1_LOGD("Enter (%p, %p)", EventData, CallbackParameter);

   if(((NatData = (A2DP_NativeData_t *)CallbackParameter) != NULL) && ((NatData == CurrentHandle) || (NatData == ClosingHandle)))
   {
      if(EventData)
      {
         switch(EventData->EventType)
         {
            case aetAudioStreamConnected:
               SS1_LOGD("Signal: AudioStreamConnected");
               break;

            case aetAudioStreamConnectionStatus:
               SS1_LOGD("Signal: AudioStreamConnectionStatus (%d)", EventData->EventData.AudioStreamConnectionStatusEventData.ConnectionStatus);
               if(EventData->EventData.AudioStreamConnectionStatusEventData.StreamType == astSRC)
               {
                  if(pthread_mutex_lock(&(NatData->SessionMutex)) == 0)
                  {
                     if(EventData->EventData.AudioStreamConnectionStatusEventData.ConnectionStatus == AUDM_STREAM_CONNECTION_STATUS_SUCCESS)
                     {
                        NatData->MTU   = EventData->EventData.AudioStreamConnectionStatusEventData.MediaMTU;
                        NatData->State = a2dpStateConnected;
                     }
                     else
                        NatData->State = a2dpStateDisconnected;

                     NatData->LastConnectionResult = EventData->EventData.AudioStreamConnectionStatusEventData.ConnectionStatus;

                     pthread_cond_broadcast(&(NatData->StateChangeCond));
                     pthread_mutex_unlock(&(NatData->SessionMutex));
                  }
               }
               break;

            case aetAudioStreamDisconnected:
               SS1_LOGD("Signal: AudioStreamDisconnected");
               if(EventData->EventData.AudioStreamDisconnectedEventData.StreamType == astSRC)
               {
                  SS1_LOGD("Audio device disconnected");

                  if(pthread_mutex_lock(&(NatData->SessionMutex)) == 0)
                  {
                     NatData->State = a2dpStateDisconnected;

                     pthread_cond_broadcast(&(NatData->StateChangeCond));
                     pthread_mutex_unlock(&(NatData->SessionMutex));
                  }
               }
               break;

            case aetAudioStreamStateChanged:
               SS1_LOGD("Signal: AudioStreamStateChanged");
               if(EventData->EventData.AudioStreamStateChangedEventData.StreamType == astSRC)
               {
                  SS1_LOGD("Playback state changed to: %d", EventData->EventData.AudioStreamStateChangedEventData.StreamState);

                  if(pthread_mutex_lock(&(NatData->SessionMutex)) == 0)
                  {
                     if(EventData->EventData.AudioStreamStateChangedEventData.StreamState == astStreamStopped)
                        NatData->State = a2dpStateConnected;
                     else
                     {
                        NatData->State = a2dpStatePlaying;

#if ENABLE_TIMING_LOGS
                        NatData->FirstPacket = TRUE;
#endif
                     }

                     pthread_cond_broadcast(&(NatData->StateChangeCond));
                     pthread_mutex_unlock(&(NatData->SessionMutex));
                  }
               }
               break;

            case aetChangeAudioStreamStateStatus:
               SS1_LOGD("Signal: ChangeAudioStreamStateStatus (%d)", EventData->EventData.ChangeAudioStreamStateStatusEventData.Successful);
               if(EventData->EventData.ChangeAudioStreamStateStatusEventData.StreamType == astSRC)
               {
                  if(EventData->EventData.ChangeAudioStreamStateStatusEventData.Successful)
                  {
                     SS1_LOGD("Playback state changed to: %d", EventData->EventData.ChangeAudioStreamStateStatusEventData.StreamState);

                     if(pthread_mutex_lock(&(NatData->SessionMutex)) == 0)
                     {
                        if(EventData->EventData.ChangeAudioStreamStateStatusEventData.StreamState == astStreamStopped)
                           NatData->State = a2dpStateConnected;
                        else
                        {
                           NatData->State = a2dpStatePlaying;

#if ENABLE_TIMING_LOGS
                           NatData->FirstPacket = TRUE;
#endif
                        }

                        pthread_cond_broadcast(&(NatData->StateChangeCond));
                        pthread_mutex_unlock(&(NatData->SessionMutex));
                     }
                  }
                  else
                     SS1_LOGE("Failed to change playback state. Active state: %d", EventData->EventData.ChangeAudioStreamStateStatusEventData.StreamState);
               }
               break;

            case aetAudioStreamFormatChanged:
               SS1_LOGD("Signal: AudioStreamFormatChanged");
               break;

            case aetChangeAudioStreamFormatStatus:
               SS1_LOGD("Signal: ChangeAudioStreamFormatStatus");
               break;

            case aetIncomingConnectionRequest:
            case aetEncodedAudioStreamData:
            case aetRemoteControlConnected:
            case aetRemoteControlConnectionStatus:
            case aetRemoteControlDisconnected:
            case aetRemoteControlCommandIndication:
            case aetRemoteControlCommandConfirmation:
               /* These events are valid but are not used in the A2DP   */
               /* SRC role handled by this module. Ignore them.         */
               break;

            default:
               SS1_LOGW("Unknown signal (%d)", EventData->EventType);
               break;
         }
      }
      else
         SS1_LOGE("Event data is invalid");
   }
   else
      SS1_LOGE("Callback made on old state data (%p, %p)", NatData, CurrentHandle);

   SS1_LOGD("Exit");
}

static int ConfigureConnection(A2DP_NativeData_t *NatData, AUD_Stream_Configuration_t *StreamConfig)
{
   int                                            ret_val;
   unsigned int                                   MTU;
   unsigned int                                   BlockSize;
   unsigned int                                   Subbands;
   unsigned int                                   Channels;
   unsigned int                                   Frequency;
   unsigned int                                   SamplesPerFrame;
   unsigned int                                   ActualSamplesPerPacket;
#if !CORRECT_SKEW_VIA_TIMING
   unsigned int                                   TargetSamplesPerPacket;
#endif
   unsigned long long                             PacketDelayNS;
   unsigned long long                             PacketDelayNSTrunc;
   SBC_Encode_Configuration_t                     EncoderConfig;
   A2DP_SBC_Codec_Specific_Information_Element_t *CodecInfo;

   if(NatData && StreamConfig)
   {
      if((StreamConfig->MediaCodecType == A2DP_MEDIA_CODEC_TYPE_SBC) && (StreamConfig->MediaCodecInfoLength == sizeof(A2DP_SBC_Codec_Specific_Information_Element_t)))
      {
         if(pthread_mutex_lock(&(NatData->WriteMutex)) == 0)
         {
            /* We use an MTU one byte less than the connection's MTU to    */
            /* account for the size of the A2DP Header.                    */
            MTU      = StreamConfig->MediaMTU - 1;

            Channels = StreamConfig->StreamFormat.NumberChannels;

            CodecInfo = ((A2DP_SBC_Codec_Specific_Information_Element_t *)&(StreamConfig->MediaCodecInformation));

#if ENABLE_SBC_LOGS
            SS1_LOGD("Building SBC configuration based on these connection parameters:\n"
                     "MediaMTU:     %u\n"
                     "Frequency:    %lu\n"
                     "Channels:     %u\n"
                     "FormatFlags:  0x%02lX\n"
                     "CodecType:    %s\n"
                     "SFCM:         0x%02hhX\n"
                     "BLSAM:        0x%02hhX\n"
                     "Min Bit Pool: %hhu\n"
                     "Max Bit Pool: %hhu\n",
                     (StreamConfig->MediaMTU),
                     (StreamConfig->StreamFormat.SampleFrequency),
                     (StreamConfig->StreamFormat.NumberChannels),
                     (StreamConfig->StreamFormat.FormatFlags),
                     (StreamConfig->MediaCodecType == A2DP_MEDIA_CODEC_TYPE_SBC ? "SBC" : (StreamConfig->MediaCodecType == A2DP_MEDIA_CODEC_TYPE_MPEG_1_2_AUDIO ? "MPEG 1/2" : (StreamConfig->MediaCodecType == A2DP_MEDIA_CODEC_TYPE_MPEG_2_4_AUDIO ? "MPEG 2/4" : (StreamConfig->MediaCodecType == A2DP_MEDIA_CODEC_TYPE_ATRAC ? "ATRAC" : (StreamConfig->MediaCodecType == A2DP_MEDIA_CODEC_TYPE_NON_A2DP ? "NON-A2DP" : "UNKNOWN"))))),
                     (CodecInfo->SamplingFrequencyChannelMode),
                     (CodecInfo->BlockLengthSubbandsAllocationMethod),
                     (CodecInfo->MinimumBitPoolValue),
                     (CodecInfo->MaximumBitPoolValue));
#endif

            Frequency = StreamConfig->StreamFormat.SampleFrequency;
            switch(Frequency)
            {
               case 48000:
                  EncoderConfig.SamplingFrequency = sf48kHz;
                  break;
               case 44100:
                  EncoderConfig.SamplingFrequency = sf441kHz;
                  break;
               case 32000:
                  EncoderConfig.SamplingFrequency = sf32kHz;
                  break;
               case 16000:
                  EncoderConfig.SamplingFrequency = sf16kHz;
                  break;
               default:
                  SS1_LOGE("Unsupported frequency (%u)", Frequency);
                  EncoderConfig.SamplingFrequency = sf441kHz;
                  Frequency                       = 0;
            }

            switch(CodecInfo->BlockLengthSubbandsAllocationMethod & A2DP_SBC_BLOCK_LENGTH_MASK)
            {
               case A2DP_SBC_BLOCK_LENGTH_SIXTEEN_VALUE:
                  EncoderConfig.BlockSize = bsSixteen;
                  BlockSize               = 16;
                  break;
               case A2DP_SBC_BLOCK_LENGTH_TWELVE_VALUE:
                  EncoderConfig.BlockSize = bsTwelve;
                  BlockSize               = 12;
                  break;
               case A2DP_SBC_BLOCK_LENGTH_EIGHT_VALUE:
                  EncoderConfig.BlockSize = bsEight;
                  BlockSize               = 8;
                  break;
               case A2DP_SBC_BLOCK_LENGTH_FOUR_VALUE:
                  EncoderConfig.BlockSize = bsFour;
                  BlockSize               = 4;
                  break;
               default:
                  SS1_LOGE("Unsupported block length (0x%02X)", (CodecInfo->BlockLengthSubbandsAllocationMethod & A2DP_SBC_BLOCK_LENGTH_MASK));
                  EncoderConfig.BlockSize = bsSixteen;
                  BlockSize               = 0;
            }

            switch(CodecInfo->SamplingFrequencyChannelMode & A2DP_SBC_CHANNEL_MODE_MASK)
            {
               case A2DP_SBC_CHANNEL_MODE_JOINT_STEREO_VALUE:
                  EncoderConfig.ChannelMode = cmJointStereo;
                  Channels                  = 2;
                  break;
               case A2DP_SBC_CHANNEL_MODE_STEREO_VALUE:
                  EncoderConfig.ChannelMode = cmStereo;
                  Channels                  = 2;
                  break;
               case A2DP_SBC_CHANNEL_MODE_DUAL_CHANNEL_VALUE:
                  EncoderConfig.ChannelMode = cmDualChannel;
                  Channels                  = 2;
                  break;
               case A2DP_SBC_CHANNEL_MODE_MONO_VALUE:
                  EncoderConfig.ChannelMode = cmMono;
                  Channels                  = 1;
                  break;
               default:
                  SS1_LOGE("Unsuppored channel mode (0x%02X)", (CodecInfo->SamplingFrequencyChannelMode & A2DP_SBC_CHANNEL_MODE_MASK));
                  EncoderConfig.ChannelMode = cmJointStereo;
                  Channels                  = 2;
            }

            switch(CodecInfo->BlockLengthSubbandsAllocationMethod & A2DP_SBC_ALLOCATION_METHOD_MASK)
            {
               case A2DP_SBC_ALLOCATION_METHOD_LOUDNESS_VALUE:
                  EncoderConfig.AllocationMethod = amLoudness;
                  break;
               case A2DP_SBC_ALLOCATION_METHOD_SNR_VALUE:
                  EncoderConfig.AllocationMethod = amSNR;
                  break;
               default:
                  SS1_LOGE("Unsupported allocation method (0x%02X)", (CodecInfo->BlockLengthSubbandsAllocationMethod & A2DP_SBC_ALLOCATION_METHOD_MASK));
                  EncoderConfig.AllocationMethod = amLoudness;
            }

            switch(CodecInfo->BlockLengthSubbandsAllocationMethod & A2DP_SBC_SUBBANDS_MASK)
            {
               case A2DP_SBC_SUBBANDS_EIGHT_VALUE:
                  EncoderConfig.Subbands = sbEight;
                  Subbands               = 8;
                  break;
               case A2DP_SBC_SUBBANDS_FOUR_VALUE:
                  EncoderConfig.Subbands = sbFour;
                  Subbands               = 4;
                  break;
               default:
                  SS1_LOGE("Unsupported number of subbands (0x%02X)", (CodecInfo->BlockLengthSubbandsAllocationMethod & A2DP_SBC_SUBBANDS_MASK));
                  EncoderConfig.Subbands = sbEight;
                  Subbands               = 0;
                  break;
            }

            /* The MaximumBitRate field doubles as a BitPool field when */
            /* the value is < 256. Choose the lesser bit pool of either */
            /* the sink's maximum supported bit pool or the maximum     */
            /* allowed bit pool (defined as MAXIMUM_BIT_POOL).          */
            EncoderConfig.MaximumBitRate = ((CodecInfo->MaximumBitPoolValue < MAXIMUM_BIT_POOL) ? CodecInfo->MaximumBitPoolValue : MAXIMUM_BIT_POOL);

            /* TODO: Resolve requested frequency ("Frequency") with frequency */
            /* selected by BTPM ("EncoderConfig.SamplingFrequency").          */
            if(NatData->Frequency != Frequency)
               SS1_LOGE("Frequency mismatch: Requested = %u, Actual = %u", NatData->Frequency, Frequency);

#if ENABLE_SBC_LOGS
            SS1_LOGD("Initializing SBC encoder using parameters:\n"
                     "Freq:         %u\n"
                     "Block Size:   %u\n"
                     "Channel Mode: %s\n"
                     "Allocation:   %s\n"
                     "Subbands:     %u\n"
                     "Bit Pool:     %lu\n",
                     (EncoderConfig.SamplingFrequency == sf16kHz ? 16000 : (EncoderConfig.SamplingFrequency == sf32kHz ? 32000 : (EncoderConfig.SamplingFrequency == sf441kHz ? 44100 : (EncoderConfig.SamplingFrequency == sf48kHz ? 48000 : -1)))),
                     (EncoderConfig.BlockSize == bsFour ? 4 : (EncoderConfig.BlockSize == bsEight ? 8 : (EncoderConfig.BlockSize == bsTwelve ? 12 : (EncoderConfig.BlockSize == bsSixteen ? 16 : -1)))),
                     (EncoderConfig.ChannelMode == cmMono ? "Mono" : (EncoderConfig.ChannelMode == cmDualChannel ? "DualChan" : (EncoderConfig.ChannelMode == cmStereo ? "Stereo" : (EncoderConfig.ChannelMode == cmJointStereo ? "Joint Stereo" : "UNKNOWN")))),
                     (EncoderConfig.AllocationMethod == amLoudness ? "Loudness" : (EncoderConfig.AllocationMethod == amSNR ? "SNR": "UNKNOWN")),
                     (EncoderConfig.Subbands == sbFour ? 4 : (EncoderConfig.Subbands == sbEight ? 8 : -1)),
                     (EncoderConfig.MaximumBitRate));
#endif

            if((NatData->Encoder = SBC_Initialize_Encoder(&EncoderConfig)) != NULL)
            {
#if ENABLE_SBC_LOGS
               SS1_LOGD("Encoder initialized");
#endif

               NatData->SampleSize = 2 * Channels;

               if((BlockSize > 0) && ((ret_val = SBC_CalculateEncoderFrameLength(&EncoderConfig)) > 0))
               {
                  NatData->SBCFrameSize        = (unsigned int)ret_val;
                  NatData->SBCFramesPerPacket  = (MTU / (unsigned int)ret_val);

                  /* Only up to 15 SBC frames can be packed into a      */
                  /* packet, no matter how many could actually fit in   */
                  /* the payload.                                       */
                  if(NatData->SBCFramesPerPacket > 15)
                     NatData->SBCFramesPerPacket = 15;

                  /* Calculate the number of samples we'll be sending   */
                  /* per packet.                                        */
                  SamplesPerFrame        = BlockSize * Subbands;
                  ActualSamplesPerPacket = SamplesPerFrame * NatData->SBCFramesPerPacket;

                  /* Calculate the timing delay between packets. Round  */
                  /* the delay down to the nearest microsecond.         */

#if PACKET_DELAY_MICROSEC

                  /* Calculate the delay per frame in microseconds      */
                  /* first then multiply for the delay per packet and   */
                  /* lastly convert to nanoseconds. This option         */
                  /* imitates the calculations done by Bluez.           */
                  PacketDelayNS      = (((Subbands * BlockSize * (long long)MICROSEC_S) / Frequency) * (NatData->SBCFramesPerPacket) * NANOSEC_US);
                  PacketDelayNSTrunc = PacketDelayNS;

#else

                  PacketDelayNS      = (((Subbands * BlockSize * (long long)NatData->SBCFramesPerPacket) * NANOSEC_S) / Frequency);
                  PacketDelayNSTrunc = ((PacketDelayNS / NANOSEC_US) * NANOSEC_US);

#endif

                  NatData->PacketDelay.tv_sec  = (time_t)(PacketDelayNSTrunc / NANOSEC_S);
                  NatData->PacketDelay.tv_nsec = (long)(PacketDelayNSTrunc % NANOSEC_S);

#if CORRECT_SKEW_VIA_TIMING

                  NatData->ExtraTimePerInterval = (PacketDelayNS - PacketDelayNSTrunc);
                  NatData->UnderrunTime         = (NANOSEC_MS - NatData->ExtraTimePerInterval);

#if ENABLE_SBC_LOGS
                  SS1_LOGD("MTU:                %u", MTU);
                  SS1_LOGD("Frame Size:         %u", NatData->SBCFrameSize);
                  SS1_LOGD("Frames per Packet:  %u", NatData->SBCFramesPerPacket);
                  SS1_LOGD("Packet Delay (NS):  %llu", PacketDelayNS);
                  SS1_LOGD("Packet Delay:       %lu.%09ld sec", NatData->PacketDelay.tv_sec, NatData->PacketDelay.tv_nsec);
                  SS1_LOGD("Extra Time/Pkt:     %llu", NatData->ExtraTimePerInterval);
                  SS1_LOGD("Underrun Time:      %llu", NatData->UnderrunTime);
#endif // ENABLE_SBC_LOGS

#else // !CORRECT_SKEW_VIA_TIMING

                  /* Calculate the optimal number of samples per packet.*/
                  TargetSamplesPerPacket = (unsigned int)((PacketDelayNSTrunc * ActualSamplesPerPacket) / PacketDelayNS);

                  /* Some skew is introduced into the timing because    */
                  /* SBC frame sizes are fixed and don't necessarily    */
                  /* match the optimal rate of samples per second. To   */
                  /* correct for this, we keep track of the number of   */
                  /* extra samples we send in each packet (in excess of */
                  /* the optimal number of samples) and, when this count*/
                  /* exceeds a threshold, we send one less SBC frame to */
                  /* offset the overrun.                                */
                  NatData->ExtraSamplesPerInterval = ActualSamplesPerPacket - TargetSamplesPerPacket;
                  NatData->UnderrunSamples         = TargetSamplesPerPacket - (ActualSamplesPerPacket - SamplesPerFrame);

#if ENABLE_SBC_LOGS
                  SS1_LOGD("MTU:                %u", MTU);
                  SS1_LOGD("Frame Size:         %u", NatData->SBCFrameSize);
                  SS1_LOGD("Frames per Packet:  %u", NatData->SBCFramesPerPacket);
                  SS1_LOGD("Packet Delay (NS):  %llu", PacketDelayNS);
                  SS1_LOGD("Packet Delay:       %lu.%09ld sec", NatData->PacketDelay.tv_sec, NatData->PacketDelay.tv_nsec);
                  SS1_LOGD("Max Samples/Pkt:    %u", ActualSamplesPerPacket);
                  SS1_LOGD("Target Samples/Pkt: %u", TargetSamplesPerPacket);
                  SS1_LOGD("Sample Overrun:     %u", NatData->ExtraSamplesPerInterval);
                  SS1_LOGD("Sample Underrun:    %u", NatData->UnderrunSamples);
#endif // ENABLE_SBC_LOGS

#endif // CORRECT_SKEW_VIA_TIMING

                  /* Allocate buffer with enough room for               */
                  /* SBCFramesPerPacket frames plus one Word_t. The     */
                  /* extra Word_t is to hold the 1-byte AVDTP Header    */
                  /* Information structure. While the structure only    */
                  /* consumes one byte, we load the buffer in units     */
                  /* of Word_t (2-bytes), so adding two bytes to the    */
                  /* buffer size keeps everything word-aligned. To      */
                  /* this end, we will start loading the buffer at      */
                  /* SBCFrameBuffer[2], store the AVDTP header at       */
                  /* SBCFrameBuffer[1], and specificy the buffer to be  */
                  /* sent as &(SBCFrameBuffer[1]).                      */
                  if((NatData->SBCFrameBuffer = (unsigned char *)calloc(((NatData->SBCFrameSize * NatData->SBCFramesPerPacket) + sizeof(Word_t)), sizeof(unsigned char))) != NULL)
                     ret_val = 0;
                  else
                  {
                     SS1_LOGE("Out of memory");
                     ret_val = -ENOMEM;
                  }
               }
               else
               {
                  ret_val = -EINVAL;
                  SS1_LOGE("Encoder parameters are invalid -  Block size (%u) or could not determine frame length) (%d)", BlockSize, ret_val);
               }
            }
            else
            {
               ret_val = -ENOMEM;
               SS1_LOGE("Could not initialize SBC encoder (%p)", NatData->Encoder);
            }

            if(ret_val != 0)
               free(NatData->SBCFrameBuffer);

            pthread_mutex_unlock(&(NatData->WriteMutex));
         }
         else
         {
            ret_val = -EINVAL;
            SS1_LOGE("Could not lock Write mutex");
         }
      }
      else
      {
         ret_val = -EINVAL;
         SS1_LOGE("Unsupported stream type or invalid configuration");
      }
   }
   else
   {
      ret_val = -EINVAL;
      SS1_LOGE("Invalid parameters");
   }

   return(ret_val);
}

   /* The following function will attempt to initiate a connection to   */
   /* the A2DP sink given by the SinkAddress member of the provided     */
   /* A2DP_NativeData_t structure. It will return 0 if successful,      */
   /* negative on a stack error, or positive on other error (errno      */
   /* code). It should be called while holding the session mutex.       */
static int ConnectSink(A2DP_NativeData_t *NatData)
{
   int                        ret_val;
   int                        Result;
   BD_ADDR_t                  RemoteDeviceAddress;
   unsigned int               ConnectionStatus;
   AUD_Stream_State_t         StreamState;

   SS1_LOGD("Enter");

   if(NatData)
   {
      ret_val = 0;

      if(NatData->State == a2dpStateDisconnected)
      {
         memset(&RemoteDeviceAddress, 0, sizeof(RemoteDeviceAddress));
         Result = AUDM_Query_Audio_Stream_Connected(astSRC, &RemoteDeviceAddress);

         if(Result == AUDM_AUDIO_STREAM_CONNECTED_STATE_CONNECTING)
         {
            if(COMPARE_BD_ADDR(RemoteDeviceAddress, NatData->SinkAddress))
            {
               if((ret_val = PthreadCondWaitMS(&(NatData->StateChangeCond), &(NatData->SessionMutex), CONNECTION_TIMEOUT_MS)) != 0)
               {
                  ConnectionStatus = AUDM_STREAM_CONNECTION_STATUS_FAILURE_TIMEOUT;
               }
               else
                  ConnectionStatus = NatData->LastConnectionResult;

               if(NatData->State == a2dpStateConnected)
               {
                  SS1_LOGD("Sink successfully connected");
               }
               else
               {
                  SS1_LOGE("Unable to connect to A2DP sink (%u)", ConnectionStatus);
                  switch(ConnectionStatus)
                  {
                     case AUDM_STREAM_CONNECTION_STATUS_FAILURE_TIMEOUT:
                        ret_val = ETIMEDOUT;
                        break;
                     case AUDM_STREAM_CONNECTION_STATUS_FAILURE_REFUSED:
                        ret_val = ECONNREFUSED;
                        break;
                     case AUDM_STREAM_CONNECTION_STATUS_FAILURE_SECURITY:
                        ret_val = EACCES;
                        break;
                     case AUDM_STREAM_CONNECTION_STATUS_FAILURE_DEVICE_POWER_OFF:
                        ret_val = ENONET;
                        break;
                     case AUDM_STREAM_CONNECTION_STATUS_FAILURE_UNKNOWN:
                        /* Fall through.                                   */
                     default:
                        ret_val = 1;
                  }
               }
            }
            else
            {
               ret_val        = ENONET;
               NatData->State = a2dpStateDisconnected;
               SS1_LOGE("Connection is in progress but to a different A2DP Sink (Expected: %02x:%02x:%02x:%02x:%02x:%02x, Connecting: %02x:%02x:%02x:%02x:%02x:%02x)", NatData->SinkAddress.BD_ADDR5, NatData->SinkAddress.BD_ADDR4, NatData->SinkAddress.BD_ADDR3, NatData->SinkAddress.BD_ADDR2, NatData->SinkAddress.BD_ADDR1, NatData->SinkAddress.BD_ADDR0, RemoteDeviceAddress.BD_ADDR5, RemoteDeviceAddress.BD_ADDR4, RemoteDeviceAddress.BD_ADDR3, RemoteDeviceAddress.BD_ADDR2, RemoteDeviceAddress.BD_ADDR1, RemoteDeviceAddress.BD_ADDR0);
            }
         }
         else
         {
            if(Result == AUDM_AUDIO_STREAM_CONNECTED_STATE_CONNECTED)
            {
               if(COMPARE_BD_ADDR(RemoteDeviceAddress, NatData->SinkAddress))
               {
                  /* Already connected.                                 */
                  SS1_LOGD("Sink appears to be connected already.");
                  NatData->State = a2dpStateConnected;
               }
               else
               {
                  ret_val        = ENONET;
                  NatData->State = a2dpStateDisconnected;
                  SS1_LOGE("A sink is connected, but not the one which was requested (Expected: %02x:%02x:%02x:%02x:%02x:%02x, Connecting: %02x:%02x:%02x:%02x:%02x:%02x)", NatData->SinkAddress.BD_ADDR5, NatData->SinkAddress.BD_ADDR4, NatData->SinkAddress.BD_ADDR3, NatData->SinkAddress.BD_ADDR2, NatData->SinkAddress.BD_ADDR1, NatData->SinkAddress.BD_ADDR0, RemoteDeviceAddress.BD_ADDR5, RemoteDeviceAddress.BD_ADDR4, RemoteDeviceAddress.BD_ADDR3, RemoteDeviceAddress.BD_ADDR2, RemoteDeviceAddress.BD_ADDR1, RemoteDeviceAddress.BD_ADDR0);
               }
            }
            else
            {
               ret_val        = ENONET;
               NatData->State = a2dpStateDisconnected;
               SS1_LOGE("No audio sink is connected currently (%d)", Result);
            }
         }
      }
   }
   else
      ret_val = EINVAL;

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
}

   /* The following function will attempt to transition the A2DP        */
   /* connection into the correct state for audio playback. If          */
   /* successful, it will return 0. A negative return value indicates a */
   /* stack error while a positive value is an error represented by an  */
   /* errno value. It should be called while holding the session mutex. */
static int ResumePlayback(A2DP_NativeData_t *NatData)
{
   int ret_val;

   SS1_LOGD("Enter");

   if(NatData)
   {
      ret_val = 0;

      if(NatData->State == a2dpStateSuspending)
      {
         ret_val = PthreadCondWaitMS(&(NatData->StateChangeCond), &(NatData->SessionMutex), STATE_TRANSITION_TIMEOUT_MS);

         if(ret_val != 0)
            SS1_LOGD("Timed out waiting for state to change from Suspending to Connected");
      }

      if(NatData->State == a2dpStateDisconnected)
      {
         ret_val = ConnectSink(NatData);
      }

      if(NatData->State == a2dpStateConnected)
      {
         if((ret_val = AUDM_Change_Audio_Stream_State(astSRC, astStreamStarted)) == 0)
         {
            NatData->State = a2dpStateResuming;
         }
         else
         {
            if(ret_val == BTPM_ERROR_CODE_AUDIO_STREAM_STATE_IS_ALREADY_STARTED)
            {
               NatData->State = a2dpStatePlaying;

#if ENABLE_TIMING_LOGS
               NatData->FirstPacket = TRUE;
#endif
            }
            else
               SS1_LOGE("Problem with AUDM_Change_Audio_Stream_State (%d)", ret_val);
         }
      }

      if(NatData->State == a2dpStateResuming)
      {
         ret_val = PthreadCondWaitMS(&(NatData->StateChangeCond), &(NatData->SessionMutex), STATE_TRANSITION_TIMEOUT_MS);

         if(ret_val != 0)
            SS1_LOGD("Timed out waiting for state to change from Resuming to Playing");
      }

      if(NatData->State == a2dpStatePlaying)
         ret_val = 0;
      else
         SS1_LOGE("Could not transition to the \"playing\" state (%d)", NatData->State);
   }
   else
      ret_val = EINVAL;

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
}

int a2dp_init(int Frequency, int Channels, A2DP_Handle_t *HandlePtr)
{
   int                ret_val;
   A2DP_NativeData_t *NatData;

   SS1_LOGD("Enter (Freq:%d, Channels:%d, HandlePtr:%p)", Frequency, Channels, HandlePtr);

   ret_val = -1;

   if(CurrentHandle == NULL)
   {
      if(HandlePtr && VALID_FREQUENCY(Frequency) && VALID_CHANNELS(Channels))
      {
         if(InitBTPMClientNoRetries(NULL, NULL, NULL) == 0)
         {
            if((NatData = (A2DP_NativeData_t *)calloc(1, sizeof(A2DP_NativeData_t))) != NULL)
            {
               NatData->CallbackID = 0;
               NatData->Frequency  = (unsigned int)Frequency;
               NatData->Channels   = (unsigned int)Channels;
               NatData->MTU        = 0;
               NatData->State      = a2dpStateDisconnected;

               memset(&(NatData->SinkAddress), 0, sizeof(NatData->SinkAddress));
               pthread_mutex_init(&(NatData->SessionMutex), NULL);
               pthread_mutex_init(&(NatData->WriteMutex), NULL);
               pthread_cond_init(&(NatData->StateChangeCond), NULL);

               NatData->Encoder               = NULL;
               NatData->SBCFrameBuffer        = NULL;
               NatData->SBCFrameCount         = 0;
               NatData->SBCFrameSize          = 0;
               NatData->SBCFramesPerPacket    = 0;
               NatData->SampleSize            = 0;

               memset(&(NatData->NextWriteTime), 0, sizeof(NatData->NextWriteTime));
               memset(&(NatData->PacketDelay), 0, sizeof(NatData->PacketDelay));

               /* Register for data events. This also reserves the Audio*/
               /* Source role for this process -- no other processes    */
               /* will be able to hijack the audio stream.              */
               if((ret_val = AUDM_Register_Data_Event_Callback(astSRC, A2DP_AUDMEventCallback, NatData)) > 0)
               {
                  NatData->CallbackID = (unsigned int) ret_val;
                  ret_val             = 0;
               }
               else
               {
                  SS1_LOGE("Unable to register for audio events (%d)", ret_val);

                  /* We should never get zero back from                 */
                  /* AUDM_Register_Data_Event_Callback(), but just in   */
                  /* case, check for it and make it negative so it's    */
                  /* treated properly as an error state.                */
                  if(ret_val == 0)
                     ret_val = -1;
               }

               if(ret_val != 0)
               {
                  pthread_mutex_destroy(&(NatData->SessionMutex));
                  pthread_mutex_destroy(&(NatData->WriteMutex));
                  free(NatData->SBCFrameBuffer);
                  free(NatData);
                  NatData = NULL;
               }
            }
            else
               SS1_LOGE("Out of memory");
         }
         else
         {
            SS1_LOGE("Unable to access Bluetooth service");
            ret_val = -ENETDOWN;
         }
      }
      else
         SS1_LOGE("Invalid arguments");
   }
   else
      SS1_LOGE("Called while already tracking an active connection");

   /* If everything else initialized correctly, pass the state structure*/
   /* back via parameter (seen as an opaque handle by the client code). */
   if(ret_val >= 0)
   {
      SS1_LOGI("A2DP Init successful");

      *HandlePtr = (A2DP_Handle_t)NatData;
      CurrentHandle = (A2DP_Handle_t)NatData;
   }

   SS1_LOGD("Exit (%d, Handle: %p)", ret_val, *HandlePtr);
   return(ret_val);
}

void a2dp_set_sink(A2DP_Handle_t Handle, const char *Address)
{
   A2DP_NativeData_t *NatData;

   SS1_LOGD("Enter (Handle:%p, Address:%s)", Handle, Address);

   if(((NatData = (A2DP_NativeData_t *)Handle) == CurrentHandle) && (NatData))
   {
      if(pthread_mutex_lock(&(NatData->SessionMutex)) == 0)
      {
         StrToBD_ADDR(&(NatData->SinkAddress), Address);

         pthread_mutex_unlock(&(NatData->SessionMutex));
      }
   }

   SS1_LOGD("Exit");
}

   /* The following function is used to write BufferLength frames from  */
   /* Buffer to the A2DP Sink connection represented by Handle (as      */
   /* returned by a2dp_init()). On success, the number of frames written*/
   /* is returned. An error is indicated by a negative errno code.      */
   /* The second parameter must point to a buffer containing a whole    */
   /* number of samples (for 16-bit stereo audio, the buffer would be   */
   /* a multiple of 4 bytes). The third parameter is the length of      */
   /* the buffer in bytes. The buffer must contain a whole number of    */
   /* audio frames, where an audio frame contains one 16-bit sample per */
   /* channel (i.e., BufferLength % (2 * Channels) == 0).               */
int a2dp_write(A2DP_Handle_t Handle, const void *Buffer, int BufferLength)
{
   int                          ret_val;
   int                          Result;
   int                          RemainingAudioFrames;
   Word_t                      *BufferPosition;
   unsigned int                 Channels;
   unsigned int                 FramesPerPacket;
   unsigned int                 FramesInBuffer;
   struct timespec              SimulatedDelay;
   struct timespec              CurrentTime;
   A2DP_NativeData_t           *NatData;
   SBC_Encode_Data_t            EncoderInput;
   AUD_Stream_Configuration_t   StreamConfig;
   SBC_Encode_Bit_Stream_Data_t EncoderOutput;

#if ENABLE_TIMING_LOGS
   struct timespec     DebugTime;
   static unsigned int pktCount           = 0;
   static long long    ReturnTime         = 0;
   long long           CallTime;
   long long           CallDiff;
   long long           TimeFor_StateMutex = 0;
   long long           TimeFor_WriteMutex = 0;
   long long           TimeFor_SBCEncode  = 0;
   long long           TimeFor_Sleep      = 0;
   long long           TimeFor_Send       = 0;
   long long           TimeAt_Send        = 0;
   static long long    TimeAt_LastSend    = 0;
   int                 FallenBehind       = 0;

   //CallTime = CurrentTimeNS();
   //if(ReturnTime > 0)
   //   CallDiff = CallTime - ReturnTime;
   //else
   //   CallDiff = 0;

#endif

   //SS1_LOGD("Enter (Handle:%p, Buffer:%p, BufferLength:%d)", Handle, Buffer, BufferLength);

   Result  = 0;
   ret_val = 0;

   if((Handle == CurrentHandle) && (Handle) && (Buffer) && (BufferLength > 0))
   {
      if(InitBTPMClientNoRetries(NULL, NULL, NULL) == 0)
      {
         NatData = (A2DP_NativeData_t *)Handle;

#if ENABLE_TIMING_LOGS
         //ReturnTime = CurrentTimeNS();
#endif

         if(pthread_mutex_lock(&(NatData->SessionMutex)) == 0)
         {
#if ENABLE_TIMING_LOGS
            //TimeFor_StateMutex = CurrentTimeNS() - ReturnTime;
#endif

            Channels = NatData->Channels;

            if(NatData->State != a2dpStatePlaying)
            {
               /* Since we are not currently playing, this is the       */
               /* first PCM data written to this stream. We will need   */
               /* to transition into the playing state. To avoid any    */
               /* time-shift due to the transition delay, initialize the*/
               /* next scheduled transmission time to the current time. */
               /* By the time we start transimitting, we will appear    */
               /* to be behind schedule, but the timing algorithm will  */
               /* correct for this automatically.                       */
               /* TODO: We may need to check whether the current        */
               /*       NextWriteTime is in the future but still within */
               /*       a transmission delay window. This could occur if*/
               /*       the remote device moves the stream to a stopped */
               /*       state but the caller continues to provide audio */
               /*       data, implying that we should switch back to the*/
               /*       playing state. In that case, the NextWriteTime  */
               /*       should remain unmodified.                       */
               clock_gettime(CLOCK_MONOTONIC, &(NatData->NextWriteTime));

#if ENABLE_TIMING_LOGS
               clock_gettime(CLOCK_MONOTONIC, &DebugTime);
               SS1_LOGI("A2DP: Setting up playback @ %lld.%09lld", (long long)DebugTime.tv_sec, (long long)DebugTime.tv_nsec);
#endif

               /* We're not yet in a playing state. Switch states, flush*/
               /* any buffers and reset our next transmission time.     */

               SS1_LOGI("Resuming A2DP playback");

               Result = ResumePlayback(NatData);

               NatData->SBCFrameCount = 0;

#if CORRECT_SKEW_VIA_TIMING
               NatData->ExtraTimeCount        = 0;
#else
               NatData->ExtraSamplesCount     = 0;
#endif

               NatData->InitCatchup = TRUE;

#if ENABLE_TIMING_LOGS
               pktCount = 1;
#endif
            }

            if(NatData->State == a2dpStatePlaying)
            {
               if(NatData->Encoder == NULL)
               {
                  SS1_LOGD("Initializing A2DP audio encoder");

                  /* Initialize the SBC encoder.                        */
                  if((Result = AUDM_Query_Audio_Stream_Configuration(astSRC, &StreamConfig)) == 0)
                  {
                     Result = ConfigureConnection(NatData, &StreamConfig);

                     if(Result != 0)
                     {
                        SS1_LOGE("Unable to configure audio stream (%d)", ret_val);
                     }
                  }
                  else
                  {
                     SS1_LOGE("Unable to query audio connection configuration (%d)", ret_val);
                  }
               }
               else
               {
                  /* SBC Encoder is already initialized.                */
                  Result = 0;
               }
            }

#if ENABLE_TIMING_LOGS
            if((NatData->State == a2dpStatePlaying) && (Result == 0))
            {
               clock_gettime(CLOCK_MONOTONIC, &DebugTime);

               if(NatData->FirstPacket == TRUE)
                  SS1_LOGD("A2DP:    First Buffer @ %lld.%09lld", (long long)DebugTime.tv_sec, (long long)DebugTime.tv_nsec);
               else
                  SS1_LOGD("A2DP: Received Buffer @ %lld.%09lld", (long long)DebugTime.tv_sec, (long long)DebugTime.tv_nsec);
            }
#endif

            pthread_mutex_unlock(&(NatData->SessionMutex));

            if(Result == 0)
            {
#if ENABLE_TIMING_LOGS
               //ReturnTime = CurrentTimeNS();
#endif

               /* We are in a playing state, so we can continue with the*/
               /* data processing.                                      */
               if(pthread_mutex_lock(&(NatData->WriteMutex)) == 0)
               {
#if ENABLE_TIMING_LOGS
                  //TimeFor_WriteMutex = CurrentTimeNS() -  ReturnTime;
#endif

                  /* Each interleaved stereo audio frame contains one   */
                  /* sample for each channel. We want to count these    */
                  /* pairs of samplesi.                                 */
                  RemainingAudioFrames = (BufferLength / 4);
                  BufferPosition       = (Word_t *)Buffer;

#if CORRECT_SKEW_VIA_TIMING
                  FramesPerPacket = NatData->SBCFramesPerPacket;
#else
                  /* Check whether we're due to send a short packet for */
                  /* timing corrections.                                */
                  if(NatData->ExtraSamplesCount >= NatData->UnderrunSamples)
                     FramesPerPacket = NatData->SBCFramesPerPacket - 1;
                  else
                     FramesPerPacket = NatData->SBCFramesPerPacket;
#endif

                  /* Process all the data we've been given, so long as  */
                  /* an error hasn't ocurred.                           */
                  while((RemainingAudioFrames > 0) && (ret_val == 0))
                  {
                     Result = 0;

                     /* Process the next frame's worth of samples. We'll*/
                     /* just feed as much data as we have into the      */
                     /* encoder and it will stop when it has consumed   */
                     /* enough for a frame. In case it runs out of      */
                     /* samples before finishing the SBC frame, the     */
                     /* encoder will maintain its state (referenced by  */
                     /* NatData->Encoder) until we feed it more data in */
                     /* the next call to a2dp_init().                   */
                     EncoderInput.LeftChannelDataPtr  = BufferPosition;
                     EncoderInput.RightChannelDataPtr = BufferPosition + 1;
                     EncoderInput.ChannelDataLength   = RemainingAudioFrames;

                     EncoderOutput.BitStreamDataSize = (NatData->SBCFramesPerPacket - NatData->SBCFrameCount) * NatData->SBCFrameSize;
                     EncoderOutput.BitStreamDataPtr  = &(NatData->SBCFrameBuffer[2]) + (NatData->SBCFrameCount * NatData->SBCFrameSize);

#if ENABLE_TIMING_LOGS
                     //ReturnTime = CurrentTimeNS();
#endif

                     if((Result = SBC_Encode_Data(NatData->Encoder, &EncoderInput, &EncoderOutput)) >= 0)
                     {
#if ENABLE_TIMING_LOGS
                        //TimeFor_SBCEncode += (CurrentTimeNS() - ReturnTime);
#endif

                        RemainingAudioFrames = EncoderInput.UnusedChannelDataLength;
                        BufferPosition      += (EncoderInput.ChannelDataLength - RemainingAudioFrames) * 2;

                        if(Result == SBC_PROCESSING_COMPLETE)
                           NatData->SBCFrameCount += 1;
                     }
                     else
                     {
                        SS1_LOGE("SBC encoding failed (%d)", Result);
                        ret_val = Result;
                     }

                     if(NatData->SBCFrameCount == FramesPerPacket)
                     {
                        /* We have a full packet ready to go. Let's send*/
                        /* it, throttling ourselves if necessary.       */

                        /* Check whether we are behind schedule.        */
                        clock_gettime(CLOCK_MONOTONIC, &CurrentTime);
                        if((CmpTimespec(&CurrentTime, &(NatData->NextWriteTime)) <= 0) || (NatData->InitCatchup == FALSE))
                        {
                           NatData->InitCatchup = FALSE;

#if ENABLE_TIMING_LOGS
                           //ReturnTime = CurrentTimeNS();
#endif

                           /* We are either ahead of schedule or right  */
                           /* on time, so delay until the next packet   */
                           /* submission time and send the packet.      */
                           while(clock_nanosleep(CLOCK_MONOTONIC, TIMER_ABSTIME, &(NatData->NextWriteTime), NULL) == EINTR)
                           {
                              /* In case we get interrupted, keep       */
                              /* sleeping until we hit the scheduled    */
                              /* time.                                  */
                           }

#if ENABLE_TIMING_LOGS
                           //TimeFor_Sleep = CurrentTimeNS() - ReturnTime;
#endif

                           /* Set the AVDTP Header Information          */
                           /* structure. Since we're not fragmenting SBC*/
                           /* frames, all we care about is the number of*/
                           /* SBC frames in the buffer.                 */
                           NatData->SBCFrameBuffer[1] = (NatData->SBCFrameCount & A2DP_SBC_HEADER_NUMBER_FRAMES_MASK);

#if ENABLE_TIMING_LOGS
                           //ReturnTime = CurrentTimeNS();

                           clock_gettime(CLOCK_MONOTONIC, &DebugTime);
                           SS1_LOGD("A2DP: Packet encoded. Submitting @ %lld.%09lld", (long long)DebugTime.tv_sec, (long long)DebugTime.tv_nsec);
#endif

                           /* Send the encoded buffer. We've been       */
                           /* loading the buffer from index [2] to      */
                           /* leave room for the AVDTP header while     */
                           /* maintaining word-alignment. The header is */
                           /* only one byte, so we stored the header at */
                           /* index [1] and send using index [1] as our */
                           /* base buffer address.                      */
                           if((ret_val = AUDM_Send_Encoded_Audio_Data(NatData->CallbackID, ((NatData->SBCFrameCount * NatData->SBCFrameSize) + 1), &(NatData->SBCFrameBuffer[1]))) != 0)
                           {
                              SS1_LOGE("Error sending encoded audio data (%d)", ret_val);
                           }
                           else
                           {
#if ENABLE_TIMING_LOGS
                              NatData->FirstPacket = FALSE;
#endif
                           }

#if ENABLE_TIMING_LOGS
                           clock_gettime(CLOCK_MONOTONIC, &DebugTime);
                           SS1_LOGD("A2DP: Packet submission complete @ %lld.%09lld", (long long)DebugTime.tv_sec, (long long)DebugTime.tv_nsec);

                           //TimeAt_Send = CurrentTimeNS();
                           //TimeFor_Send = TimeAt_Send - ReturnTime;

                           //SS1_LOGD("SENT PKT %u containing %u frames at %lld ns (delta: %lld)\n  AF:%9lld, S.Mux:%9lld, W.Mux:%9lld, Enc:%9lld, Slp:%9lld, Snd:%9lld", pktCount, NatData->SBCFrameCount, TimeAt_Send, (TimeAt_Send - TimeAt_LastSend), CallDiff, TimeFor_StateMutex, TimeFor_WriteMutex, TimeFor_SBCEncode, TimeFor_Sleep, TimeFor_Send);
                           //TimeAt_LastSend    = TimeAt_Send;
                           //CallDiff           = -1;
                           //TimeFor_StateMutex = -1;
                           //TimeFor_WriteMutex = -1;
                           //TimeFor_SBCEncode  = -1;
                           //TimeFor_Sleep      = -1;
                           //TimeFor_Send       = -1;
                           //FallenBehind       = 0;
#endif
                        }
                        else
                        {
#if ENABLE_TIMING_LOGS
                           DebugTime = DiffTimespec(&CurrentTime, &(NatData->NextWriteTime));
                           SS1_LOGD("Packet %d: Behind schedule by (%ld s %ld ns)", pktCount, DebugTime.tv_sec, DebugTime.tv_nsec);
#endif
                        }

                        /* Prepare for the next packet by resetting the */
                        /* SBC frame count and determining the next     */
                        /* submission time.                             */
                        NatData->SBCFrameCount = 0;
                        NatData->NextWriteTime = AddTimespec(&(NatData->NextWriteTime), &(NatData->PacketDelay));

#if ENABLE_TIMING_LOGS
                        pktCount++;
#endif

#if CORRECT_SKEW_VIA_TIMING
                        if(NatData->ExtraTimeCount >= NatData->UnderrunTime)
                        {
                           /* The last packet we built put us over the  */
                           /* skew threshold, so the next packet needs  */
                           /* to be sent after a longer (corrective)    */
                           /* interval.                                 */
                           NatData->NextWriteTime.tv_nsec += NANOSEC_MS;
                           if(NatData->NextWriteTime.tv_nsec >= NANOSEC_S)
                           {
                              NatData->NextWriteTime.tv_nsec -= NANOSEC_S;
                              NatData->NextWriteTime.tv_sec  += 1;
                           }
                           NatData->ExtraTimeCount -= NatData->UnderrunTime;
                        }
                        else
                        {
                           /* After sending the last packet, we're still*/
                           /* under the skew threshold, so we'll use the*/
                           /* normal interval for the next packet.      */
                           NatData->ExtraTimeCount += NatData->ExtraTimePerInterval;
                        }
#else // !CORRECT_SKEW_VIA_TIMING
                        /* Check whether we just sent (or dropped) an   */
                        /* undersized packet.                           */
                        if(NatData->ExtraSamplesCount >= NatData->UnderrunSamples)
                        {
                           /* We should have just sent an undersized    */
                           /* packet. The next one will be normal-sized.*/
                           FramesPerPacket = NatData->SBCFramesPerPacket;

                           /* Correct the extra sample count.           */
                           NatData->ExtraSamplesCount -= NatData->UnderrunSamples;
                        }
                        else
                        {
                           /* The last packet was NOT a short packet,   */
                           /* so add the extra samples to the running   */
                           /* count and check whether the next packet   */
                           /* should be short.                          */
                           NatData->ExtraSamplesCount += NatData->ExtraSamplesPerInterval;

                           if(NatData->ExtraSamplesCount >= NatData->UnderrunSamples)
                              FramesPerPacket = NatData->SBCFramesPerPacket - 1;
                           else
                              FramesPerPacket = NatData->SBCFramesPerPacket;
                        }
#endif // !CORRECT_SKEW_VIA_TIMING
                     }
                  }

                  if(ret_val == 0)
                     ret_val = BufferLength - (RemainingAudioFrames * 2);

                  pthread_mutex_unlock(&(NatData->WriteMutex));
               }
               else
                  ret_val = -EINVAL;
            }
            else
               ret_val = -EAGAIN;
         }
         else
            ret_val = -EINVAL;
      }
      else
         ret_val = -ENODEV;
   }
   else
      ret_val = -EINVAL;

   //SS1_LOGD("Exit (%d)", ret_val);

#if ENABLE_TIMING_LOGS
   //ReturnTime = CurrentTimeNS();
#endif

   return(ret_val);
}

int a2dp_stop(A2DP_Handle_t Handle)
{
   int                ret_val;
   A2DP_NativeData_t *NatData;

   SS1_LOGD("Enter (Handle:%p)", Handle);

   ret_val = -1;

   if(((NatData = (A2DP_NativeData_t *)Handle) == CurrentHandle) && (NatData))
   {
      if(pthread_mutex_lock(&(NatData->SessionMutex)) == 0)
      {
         SS1_LOGI("Stopping A2DP playback");

         if(NatData->State == a2dpStateResuming)
         {
            ret_val = PthreadCondWaitMS(&(NatData->StateChangeCond), &(NatData->SessionMutex), STATE_TRANSITION_TIMEOUT_MS);

            if(ret_val != 0)
               SS1_LOGD("Timed out waiting for state to finish in-progress transition to Playing");
         }

         if(NatData->State == a2dpStatePlaying)
         {
            ret_val = AUDM_Change_Audio_Stream_State(astSRC, astStreamStopped);

            if(ret_val == 0)
            {
               NatData->State = a2dpStateSuspending;
            }
            else
            {
               if(ret_val == BTPM_ERROR_CODE_AUDIO_STREAM_STATE_IS_ALREADY_SUSPENDED)
                  NatData->State = a2dpStateConnected;
            }
         }

         if(NatData->State == a2dpStateSuspending)
         {
            ret_val = PthreadCondWaitMS(&(NatData->StateChangeCond), &(NatData->SessionMutex), STATE_TRANSITION_TIMEOUT_MS);

            if(ret_val != 0)
               SS1_LOGD("Timed out waiting for state to change from Suspending to Connected (Stopped)");
         }

         if((NatData->State == a2dpStateConnected) || (NatData->State == a2dpStateDisconnected))
         {
            SS1_LOGD("A2DP stream stopped successfully");

            ret_val = 0;
         }
         else
         {
            SS1_LOGE("Unable to stop A2DP stream (%d)", ret_val);

            if(ret_val == 0)
               ret_val = -1;
         }

         pthread_mutex_unlock(&(NatData->SessionMutex));
      }
   }

   SS1_LOGD("Exit (%d)", ret_val);

   return(ret_val);
}

void a2dp_cleanup(A2DP_Handle_t Handle)
{
   int                Result;
   Boolean_t          SessionLockHeld;
   Encoder_t          Encoder;
   unsigned char     *SBCBuffer;
   A2DP_NativeData_t *NatData;

   SS1_LOGD("Enter (Handle:%p)", Handle);

   SessionLockHeld = FALSE;

   if(((NatData = (A2DP_NativeData_t *)Handle) == CurrentHandle) && (NatData))
   {
      /* Prevent future API calls from accessing the state structure,   */
      /* with the exception of the event callback in case we need to    */
      /* wait on an event during cleanup.                               */
      ClosingHandle = CurrentHandle;
      CurrentHandle = NULL;

      /* Lock the session mutex if it exists.                           */
      if(pthread_mutex_lock(&(NatData->SessionMutex)) == 0)
      {
         if(NatData->State != a2dpStateDisconnected)
         {
            Result = AUDM_Disconnect_Audio_Stream(astSRC);

            if(Result == 0)
               PthreadCondWaitMS(&(NatData->StateChangeCond), &(NatData->SessionMutex), STATE_TRANSITION_TIMEOUT_MS);
            else
            {
               if(Result == BTPM_ERROR_CODE_AUDIO_STREAM_NOT_CONNECTED)
                  NatData->State = a2dpStateDisconnected;
            }

            if(NatData->State == a2dpStateDisconnected)
               pthread_cond_broadcast(&(NatData->StateChangeCond));
            else
               SS1_LOGE("A2DP disconnect request failed (%d)", Result);
         }

         pthread_mutex_unlock(&(NatData->SessionMutex));
      }
      else
      {
         SS1_LOGE("Failed to obtain session lock, forcing stream disconnection");
         AUDM_Disconnect_Audio_Stream(astSRC);
      }


      /* We no longer expect to receive any events for this connection, */
      /* so remove the event callback's last reference to the state     */
      /* structure.                                                     */
      ClosingHandle = NULL;

      if(pthread_mutex_lock(&(NatData->WriteMutex)) == 0)
      {
         Encoder   = NatData->Encoder;
         SBCBuffer = NatData->SBCFrameBuffer;

         NatData->Encoder        = NULL;
         NatData->SBCFrameBuffer = NULL;

         pthread_mutex_unlock(&(NatData->WriteMutex));
      }
      else
      {
         SS1_LOGE("Failed to obtain write lock, forcably freeing resources");
         Encoder   = NatData->Encoder;
         SBCBuffer = NatData->SBCFrameBuffer;
      }

      if(Encoder)
         SBC_Cleanup_Encoder(Encoder);

      if(SBCBuffer)
         free(SBCBuffer);

      if(NatData->CallbackID > 0)
      {
         AUDM_Un_Register_Data_Event_Callback(NatData->CallbackID);
         NatData->CallbackID = 0;
      }

      if(pthread_cond_destroy(&(NatData->StateChangeCond)) == EBUSY)
         SS1_LOGE("Trying to clean up but threads are waiting on events");

#if ENABLE_SBC_LOGS
      if(NatData->Encoder)
         SS1_LOGE("ENCODER NEVER RELEASED");
      if(NatData->SBCFrameBuffer)
         SS1_LOGE("FRAME BUFFER NEVER RELEASED");
#endif

      pthread_mutex_destroy(&(NatData->WriteMutex));
      pthread_mutex_destroy(&(NatData->SessionMutex));
      free(NatData);
   }

   SS1_LOGD("Exit");
}



