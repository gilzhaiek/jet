/*****< audioencoder.c >*******************************************************/
/*      Copyright 2011 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/* AUDIOENCODER  - Sample code using Bluetopia Platform Manager Audio Manager */
/*                 Application Programming Interface (API).                   */
/*                                                                            */
/*  Author:  Greg Hensley                                                     */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   01/13/11  G. Hensley     Initial creation. (Based on LinuxAUDM)          */
/******************************************************************************/
#include <stdio.h>
#include <errno.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

#include "SS1BTAUDM.h"     /* Audio Manager Application Programming Interface.*/
#include "SS1BTPM.h"       /* BTPM Application Programming Interface.         */

#include "AudioDecoder.h"  /* Audio Decoder sample.                           */

/* Use the newer ALSA API */
#define ALSA_PCM_NEW_HW_PARAMS_API

#include <alsa/asoundlib.h>

   /* Constants that define the Input/Output Buffer Sizes for decoding  */
   /* the input the data.                                               */
#define PCM_BUFFER_SIZE                                           (4096 * 4)

   /* The following defines the max number of SBC buffers.              */
#define NUMBER_OF_RECEIVE_BUFFERS                                 1000

   /* Constants that define the Decoded Output Buffer Sizes for decoding*/
   /* the input data.                                                   */
#define CHANNEL_BUFFER_SIZE                                       4096

   /* The following defines the low and high water marks for            */
   /* filling/flushing the buffer. For flushing, the audio stops while  */
   /* the buffer is drained to the high water mark. For filling, the    */
   /* audio stops until the buffer is filled to the low water mark.     */
#define SBC_LOW_WATER_MARK                                        150
#define SBC_HIGH_WATER_MARK                                       (NUMBER_OF_RECEIVE_BUFFERS - 250)

   /* The following defines the low and high water marks for            */
   /* reconfiguring the sample frequency.                               */
#define SBC_SAMPE_FREQUENCY_LOW_WATER_MARK                        15
#define SBC_SAMPE_FREQUENCY_HIGH_WATER_MARK                       (NUMBER_OF_RECEIVE_BUFFERS - 100)

   /* The following defines the 'resolution' to adjust the pcm playback.*/
#define PRECISION_FACTOR                                          100
#define VARIATION_THRESHOLD                                       (1 * PRECISION_FACTOR)

   /* The following defines the delay between switching alsa devices.   */
#define PING_PONG_ALSA_MICROSECOND_DELAY                        900000

   /* The following defines the limits and steps for adjusting the      */
   /* sample frequency.                                                 */
#define MIN_LIMIT_SAMPLE_FREQUENCY                               500
#define MAX_LIMIT_SAMPLE_FREQUENCY                               500
#define SAMPLE_FREQUENCY_STEP                                    100

   /* The following constants represent offsets and constants relating  */
   /* to the WAV Header.  These constants should not be changed.        */
#define WAVE_FILE_HEADER_WRITE_BUFFER_SIZE                        (128)
#define MINIMUM_WAVE_FILE_HEADER_BUFFER_LENGTH                    (44)
#define WAVE_FILE_HEADER_SUB_CHUNK_1_ID_OFFSET                    (12)
#define WAVE_FILE_HEADER_SUB_CHUNK_1_SIZE_OFFSET                  (16)
#define WAVE_FILE_HEADER_SUB_CHUNK_1_AUDIO_FORMAT_OFFSET          (20)
#define WAVE_FILE_HEADER_SUB_CHUNK_1_NUMBER_CHANNELS_OFFSET       (22)
#define WAVE_FILE_HEADER_SUB_CHUNK_1_SAMPLE_RATE_OFFSET           (24)
#define WAVE_FILE_HEADER_SUB_CHUNK_1_BYTE_RATE_OFFSET             (28)
#define WAVE_FILE_HEADER_SUB_CHUNK_1_BLOCK_ALIGN_OFFSET           (32)
#define WAVE_FILE_HEADER_SUB_CHUNK_1_BITS_PER_SAMPLE_OFFSET       (34)
#define WAVE_FILE_HEADER_SUB_CHUNK_2_ID_OFFSET                    (36)
#define WAVE_FILE_HEADER_SUB_CHUNK_2_SIZE_OFFSET                  (40)

   /* Following structure is represents buffer information for received */
   /* data.                                                             */
typedef struct _tagAudioDataBufferInfo_t
{
   unsigned char  *Buffer;
   unsigned int    Length;
} AudioDataBufferInfo_t;

   /* The following enum defines the different states of playback.      */
typedef enum
{
   adInvalid,
   adFlushingBuffer,
   adFillingBuffer,
   adPlaying
} PlaybackState_et;

   /* The following type definition represents the structure which holds*/
   /* the current audio playback context information.                   */
typedef struct _tagPlaybackContext
{
   Mutex_t                      Mutex;
   unsigned int                 ConfigFlags;
   PlaybackState_et             PlaybackState;
   Decoder_t                    DecoderHandle;
   FILE                        *PcmFileDescriptor;
   FILE                        *WavFileDescriptor;
   AUD_Stream_Configuration_t   StreamConfig;
   SBC_Decode_Configuration_t   DecodeConfiguration;
   unsigned short               LeftChannelData[CHANNEL_BUFFER_SIZE];
   unsigned short               RightChannelData[CHANNEL_BUFFER_SIZE];
   unsigned int                 SbcFrameCountLowWaterMark;
   unsigned int                 SbcFrameCountHighWaterMark;

   unsigned int                 CurrentRecievedAudioDataIndex;
   unsigned int                 CurrentProccessAudioDataIndex;
   AudioDataBufferInfo_t        ReceivedAudioData[NUMBER_OF_RECEIVE_BUFFERS];
   Event_t                      StartProcessing;
   Event_t                      ShutdownEvent;
   unsigned int                 ActiveSampleFrequency;
   unsigned int                 NewSampleFrequency;

   snd_pcm_t                   *ActivePcmHandle;
   snd_pcm_t                   *PcmHandle1;
   snd_pcm_t                   *PcmHandle2;
   snd_pcm_uframes_t            PcmFramesToWrite;
} PlaybackContext_t;

   /* Single instance of the Playback Context                           */
static PlaybackContext_t PlaybackContext;

   /* Flag to indicated if the module is initialized                    */
static Boolean_t Initialized = FALSE;

static int QueryBufferDiffernce(void);
static void WriteSbcToFile(unsigned char *RawAudioDataFrame, unsigned int RawAudioDataFrameLength);
static void WriteWavAudioDataToFile(unsigned char *Buffer, unsigned int Length);
static int  ConfigureAudio(snd_pcm_t *PcmHandle);
static int  SbcDecode(unsigned char *SbcData, unsigned int SbcDataLength, unsigned char *PcmData, unsigned int *PcmDataLength);
static void AdjustPcmPlayback(int BufferDiff);
static void *PlaybackThread(void *ThreadParameter);

   /* The following function returns the buffer difference (from the    */
   /* read to the write).  This function expect the mutex to be held.   */
static int QueryBufferDiffernce(void)
{
   int BufferDiff;

   if(PlaybackContext.CurrentProccessAudioDataIndex <= PlaybackContext.CurrentRecievedAudioDataIndex)
   {
      BufferDiff = PlaybackContext.CurrentRecievedAudioDataIndex - PlaybackContext.CurrentProccessAudioDataIndex;
   }
   else
   {
      BufferDiff = (PlaybackContext.CurrentRecievedAudioDataIndex + NUMBER_OF_RECEIVE_BUFFERS) - PlaybackContext.CurrentProccessAudioDataIndex;
   }

   return BufferDiff;
}

   /* The following utility function writes the raw audio data to a     */
   /* file.                                                             */
static void WriteSbcToFile(unsigned char *RawAudioDataFrame, unsigned int RawAudioDataFrameLength)
{
   int Result;

   /* Validate the module is initialized                                */
   if(Initialized)
   {
      if((RawAudioDataFrameLength) && (RawAudioDataFrame))
      {
         /* If the file is not open, attempt to open it.                */
         if(NULL == PlaybackContext.PcmFileDescriptor)
         {
            PlaybackContext.PcmFileDescriptor = fopen(DECODER_SBC_FILE_NAME,"w");
         }

         if(NULL != PlaybackContext.PcmFileDescriptor)
         {
            Result = fwrite(RawAudioDataFrame, 1, RawAudioDataFrameLength,  PlaybackContext.PcmFileDescriptor);

            if(Result < 0)
            {
               printf("failed to write\n");
            }
            else if(Result != RawAudioDataFrameLength)
            {
               printf("Partial write %d\n", Result);
            }
         }
         else
         {
            printf("Failed to open file\n");
         }
      }
   }
   else
   {
      printf("Invalid parameters\n");
   }
}

   /* The following utility function writes the raw audio data to a     */
   /* file.                                                             */
static void WriteWavAudioDataToFile(unsigned char *Buffer, unsigned int Length)
{
   int                  Result;
   static unsigned char WaveFileHeaderBuffer[WAVE_FILE_HEADER_WRITE_BUFFER_SIZE];
   unsigned int         NumberChannels;
   unsigned int         NumberSampleFrequency;

   /* Validate the module is initialized                                */
   if(Initialized)
   {
      if((Length) && (Buffer))
      {
         /* If the file is not open, attempt to open it.                */
         if(NULL == PlaybackContext.WavFileDescriptor)
         {
            if(NULL != (PlaybackContext.WavFileDescriptor = fopen(DECODER_WAV_FILE_NAME,"w")))
            {
               /* This is the first frame decoded. First add the wave   */
               /* header to the file using the configuration from the   */
               /* decoded output.                                       */

               /* First add the RIFF chunk descriptor. Leaving room for */
               /* the Chunk Size to be added later.                     */
               sprintf((char *)WaveFileHeaderBuffer, "RIFF    WAVE");

               /* Next add the fmt sub-chunk. Starting with the header. */
               sprintf((char *)&WaveFileHeaderBuffer[WAVE_FILE_HEADER_SUB_CHUNK_1_ID_OFFSET], "fmt ");

               /* Then the sub-chunk size, this is always 16 for PCM.   */
               /* This value must be written little endian.             */
               ASSIGN_HOST_DWORD_TO_LITTLE_ENDIAN_UNALIGNED_DWORD(&WaveFileHeaderBuffer[WAVE_FILE_HEADER_SUB_CHUNK_1_SIZE_OFFSET], 16);

               /* Next the Audio Format. This value is always 1 for PCM.*/
               /* This value must be written little endian.             */
               ASSIGN_HOST_WORD_TO_LITTLE_ENDIAN_UNALIGNED_WORD(&WaveFileHeaderBuffer[WAVE_FILE_HEADER_SUB_CHUNK_1_AUDIO_FORMAT_OFFSET], 1);

               /* Next the Number of Channels. This value will be the   */
               /* value from the frame of decoded data. This value must */
               /* be written little endian.                             */
               if(PlaybackContext.DecodeConfiguration.ChannelMode == cmMono)
                  NumberChannels = 1;
               else
                  NumberChannels = 2;

               /* Finally write out the number of channels.             */
               ASSIGN_HOST_WORD_TO_LITTLE_ENDIAN_UNALIGNED_WORD(&WaveFileHeaderBuffer[WAVE_FILE_HEADER_SUB_CHUNK_1_NUMBER_CHANNELS_OFFSET], NumberChannels);

               /* Next the Sample Rate. This value will be the value    */
               /* from the frame of decoded data. This value must be    */
               /* written little endian.                                */
               NumberSampleFrequency = PlaybackContext.StreamConfig.StreamFormat.SampleFrequency;

               /* Finally write out the Sample Frequency.               */
               ASSIGN_HOST_DWORD_TO_LITTLE_ENDIAN_UNALIGNED_DWORD(&WaveFileHeaderBuffer[WAVE_FILE_HEADER_SUB_CHUNK_1_SAMPLE_RATE_OFFSET], NumberSampleFrequency);

               /* Next the Byte Rate. This value is                     */
               /* (SampleRate*NumChannels*BitsPerSample/8). Bits Per    */
               /* Sample will always be 16 in our case. This value must */
               /* be written little endian.                             */
               ASSIGN_HOST_DWORD_TO_LITTLE_ENDIAN_UNALIGNED_DWORD(&WaveFileHeaderBuffer[WAVE_FILE_HEADER_SUB_CHUNK_1_BYTE_RATE_OFFSET], (NumberSampleFrequency * NumberChannels * 16/8));

               /* Next the block alignment. This value is               */
               /* (NumChannels*BitsPerSample/8). Bits Per sample will   */
               /* always be 16 in our case. This value must be written  */
               /* little endian.                                        */
               ASSIGN_HOST_WORD_TO_LITTLE_ENDIAN_UNALIGNED_WORD(&WaveFileHeaderBuffer[WAVE_FILE_HEADER_SUB_CHUNK_1_BLOCK_ALIGN_OFFSET], (NumberChannels * 16/8));

               /* Next write the finally data in sub chunk 1, the Bits  */
               /* Per Sample. This will always be 16 in our case. This  */
               /* value must be written little endian.                  */
               ASSIGN_HOST_WORD_TO_LITTLE_ENDIAN_UNALIGNED_WORD(&WaveFileHeaderBuffer[WAVE_FILE_HEADER_SUB_CHUNK_1_BITS_PER_SAMPLE_OFFSET], 16);

               /* All through writing sub chunk 1, next write the sub   */
               /* chunk 2 ID.                                           */
               sprintf((char *)&WaveFileHeaderBuffer[WAVE_FILE_HEADER_SUB_CHUNK_2_ID_OFFSET], "data");

               /* Next leave some space for the sub chunk 2 size. This  */
               /* value must be written little endian.                  */
               ASSIGN_HOST_DWORD_TO_LITTLE_ENDIAN_UNALIGNED_DWORD(&WaveFileHeaderBuffer[WAVE_FILE_HEADER_SUB_CHUNK_2_SIZE_OFFSET], 0);

               /* The rest of sub chunk two is data. Write out the built*/
               /* wave header.                                          */
               fwrite(WaveFileHeaderBuffer, sizeof(unsigned char), MINIMUM_WAVE_FILE_HEADER_BUFFER_LENGTH, PlaybackContext.WavFileDescriptor);
            }
         }

         if(NULL != PlaybackContext.WavFileDescriptor)
         {
            Result = fwrite(Buffer, 1, Length, PlaybackContext.WavFileDescriptor);

            if(Result < 0)
            {
               printf("failed to write\n");
            }
            else if(Result != Length)
            {
               printf("Partial write %d\n", Result);
            }
         }
         else
         {
            printf("Failed to open file\n");
         }
      }
      else
      {
         printf("Invalid param\n");
      }
   }
   else
   {
      printf("Invalid parameters\n");
   }
}

   /* The following function is used to configure the host audio        */
   /* subsystem                                                         */
static int ConfigureAudio(snd_pcm_t *PcmHandle)
{
   int                  Result      = -1;
   int                  SubUnitDirection;
   snd_pcm_hw_params_t *PcmHwParams;
   snd_pcm_sw_params_t *PcmSwParams;

   SubUnitDirection = 0;
   PcmHwParams      = NULL;
   PcmSwParams      = NULL;

   /* Allocate a hardware parameters object.                            */
   snd_pcm_hw_params_alloca(&PcmHwParams);

   /* Fill it in with default values.                                   */
   if(0 <= (Result = snd_pcm_hw_params_any(PcmHandle, PcmHwParams)))
   {
      /* Set the desired hardware parameters.                           */

      /* Interleaved mode                                               */
      if(0 <= (Result = snd_pcm_hw_params_set_access(PcmHandle, PcmHwParams, SND_PCM_ACCESS_RW_INTERLEAVED)))
      {
         /* Signed 16-bit little-endian format                          */
         if(0 <= (Result == snd_pcm_hw_params_set_format(PcmHandle, PcmHwParams, SND_PCM_FORMAT_S16_LE)))
         {
            /* Set the channels                                         */
            if(0 <= (Result = snd_pcm_hw_params_set_channels(PcmHandle, PcmHwParams, PlaybackContext.StreamConfig.StreamFormat.NumberChannels)))
            {
               /* Configure the frequency                               */
               if(0 <= (Result = snd_pcm_hw_params_set_rate_near(PcmHandle, PcmHwParams, &PlaybackContext.NewSampleFrequency, &SubUnitDirection)))
               {
                  PlaybackContext.ActiveSampleFrequency = PlaybackContext.NewSampleFrequency;
                  printf("Sample rate set to %d\n", PlaybackContext.ActiveSampleFrequency);

                  /* Set period size to 32 frames.                      */
                  PlaybackContext.PcmFramesToWrite = 32;
                  if(0 <= (Result = snd_pcm_hw_params_set_period_size_near(PcmHandle, PcmHwParams, &(PlaybackContext.PcmFramesToWrite), &SubUnitDirection)))
                  {
                     /* Write the parameters to the driver              */
                     if(0 <= (Result = snd_pcm_hw_params(PcmHandle, PcmHwParams)))
                     {
                        /* Configure software                           */
                        snd_pcm_sw_params_alloca(&PcmSwParams);
                        if(0 <= (Result = snd_pcm_sw_params_current(PcmHandle, PcmSwParams)))
                        {
                           /* Disable any buffering in software.        */
                           if(0 <= (Result = snd_pcm_sw_params_set_start_threshold(PcmHandle, PcmSwParams, 0)))
                           {
                              Result = 0;
                           }
                           else
                           {
                              printf("Unable to set start threshold %s\n", snd_strerror(Result));
                           }
                        }
                        else
                        {
                           printf("Unable to determine current swparams %s\n", snd_strerror(Result));
                        }
                     }
                     else
                     {
                        printf("Failed to write, not supported (%s)\n", snd_strerror(Result));
                     }
                  }
                  else
                  {
                     printf("Failed to write period (%s)\n", snd_strerror(Result));
                  }
               }
               else
               {
                  printf("Sample rate, %d, not supported (%s)\n", (int)PlaybackContext.ActiveSampleFrequency, snd_strerror(Result));
               }
            }
            else
            {
               printf("Channels, %d, not supported (%s)\n", PlaybackContext.StreamConfig.StreamFormat.NumberChannels, snd_strerror(Result));
            }
         }
         else
         {
            printf("Unable to set format (%s)\n", snd_strerror(Result));
         }
      }
      else
      {
         printf("Failed to set access (%s)\n", snd_strerror(Result));
      }
   }

   return Result;
}

   /* The following function performs the decoding of SBC data          */
static int SbcDecode(unsigned char *SbcData, unsigned int SbcDataLength, unsigned char *PcmData, unsigned int *PcmDataLength)
{
   SBC_Decode_Data_t   DecodedData;
   unsigned int        UnusedDataLength;
   unsigned int        WaveFileWriteBufferLength;
   unsigned int        OutputBufferSize;
   unsigned int        Index;
   int                 Result;
   unsigned int        NumberSamples;
   unsigned short     *Buffer;

   if(Initialized)
   {
      if((SbcData) && (SbcDataLength) && (PcmData) && (PcmDataLength))
      {
         /* Note the output size.                                       */
         OutputBufferSize = (*PcmDataLength)/sizeof(unsigned short);

         /* Clear the length, because we are going to update it to the actual length */
         *PcmDataLength = 0;

         DecodedData.ChannelDataSize        = CHANNEL_BUFFER_SIZE;
         DecodedData.LeftChannelDataLength  = 0;
         DecodedData.RightChannelDataLength = 0;
         DecodedData.LeftChannelDataPtr     = PlaybackContext.LeftChannelData;
         DecodedData.RightChannelDataPtr    = PlaybackContext.RightChannelData;

         /* Typecast to a short to help the copy */
         Buffer = (unsigned short *)PcmData;

         UnusedDataLength = SbcDataLength;
         while(UnusedDataLength > 0)
         {
            /* Pass the SBC data into the decoder.                      */
            if((Result = SBC_Decode_Data(PlaybackContext.DecoderHandle, UnusedDataLength, &SbcData[(SbcDataLength - UnusedDataLength)], &PlaybackContext.DecodeConfiguration, &DecodedData, &UnusedDataLength)) == SBC_PROCESSING_COMPLETE)
            {
               /* Now we need to write out the samples. We need to      */
               /* possibly interleave them if this is stereo.           */
               NumberSamples = DecodedData.LeftChannelDataLength + DecodedData.RightChannelDataLength;
               while(NumberSamples > 0)
               {
                  WaveFileWriteBufferLength = NumberSamples;
                  if(WaveFileWriteBufferLength > OutputBufferSize)
                  {
                     printf("Number of samples %d\n",NumberSamples);
                     WaveFileWriteBufferLength = OutputBufferSize;
                     if(0 == OutputBufferSize)
                     {
                        printf("Buffer 0\n");
                        break;
                     }
                  }

                  for(Index = 0; Index < (WaveFileWriteBufferLength/2); Buffer++, Index++)
                  {
                     *Buffer = DecodedData.LeftChannelDataPtr[Index];

                     if(PlaybackContext.DecodeConfiguration.ChannelMode != cmMono)
                     {
                        Buffer++;
                        *Buffer = DecodedData.RightChannelDataPtr[Index];
                     }
                  }

                  /* Note the total length                              */
                  *PcmDataLength = (*PcmDataLength) + (WaveFileWriteBufferLength * 2);

                  /* Decrement the number of samples that were written. */
                  NumberSamples -= WaveFileWriteBufferLength;

                  OutputBufferSize -= (WaveFileWriteBufferLength * 2);
               }

               DecodedData.LeftChannelDataLength  = 0;
               DecodedData.RightChannelDataLength = 0;
            }
            else
            {
               printf("SBC Result %d\n", Result);
            }
         }

         if(DECODER_CONFIG_FLAGS_ENABLE_LOGGING_WAV & PlaybackContext.ConfigFlags)
            WriteWavAudioDataToFile((unsigned char *)PcmData, *PcmDataLength);
      }
      else
      {
         Result = -2;
         printf("invalid parameter(s)\n");
      }
   }
   else
   {
      Result = -1;
   }

   return Result;
}

   /* This following function is used to monitor the playback and       */
   /* possibly adjust the frequency of the playback. The general        */
   /* idea, is to calculate the trend of playback, (some device play    */
   /* fast/slow). Then adjust the frequency when the buffer is getting  */
   /* close to empty/full.                                              */
static void AdjustPcmPlayback(int BufferDiff)
{
   static unsigned int RollingSum         = 0;
   static unsigned int Iterations         = 0;
   static unsigned int CurrentAverage;
   static unsigned int LastAverage        = 0;
   int                 Difference;
   int                 Result;
   Boolean_t           Reconfigure        = FALSE;
   Boolean_t           ValidData          = FALSE;

   /* Confirm we are in the playing state (if we are flushing or        */
   /* filling) don't count the data.                                    */
   if(PlaybackContext.PlaybackState == adPlaying)
   {
      if(LastAverage)
      {
         /* See if this value is within range of what we expect         */
         if(abs(LastAverage - (BufferDiff * PRECISION_FACTOR)) < (50 * PRECISION_FACTOR))
         {
            ValidData = TRUE;
         }
         else
         {
            /* This value considered 'extreme'. The most likely cause   */
            /* is from RF interference or range. Throw out data until   */
            /* things settle down.                                      */
            printf("Out of range %d, %d, %d\n", LastAverage, BufferDiff, abs(LastAverage - (BufferDiff * PRECISION_FACTOR)));
         }
      }
      else
      {
         /* If we don't have a last average, we have nothing to compare. */
         /* go ahead and consider the data valid. */
         ValidData = TRUE;
      }
   }

   if(ValidData)
   {
      if(Iterations++ > 0)
      {
         RollingSum += BufferDiff;

         if(1000 == Iterations)
         {
            CurrentAverage = ((RollingSum * PRECISION_FACTOR)/(Iterations));

            if(LastAverage != 0)
            {
               Difference = LastAverage - CurrentAverage;

               printf("Variation %d %d %d\n", Difference, LastAverage, CurrentAverage);

               /* Confirm we have not already adjusted the frequency       */
               if(PlaybackContext.ActiveSampleFrequency == PlaybackContext.NewSampleFrequency)
               {
                  /* We want to compare the two averages to see if we are  */
                  /* going up or down                                      */
                  if(Difference > VARIATION_THRESHOLD)
                  {
                     printf("Slow down playback");
                     if((PlaybackContext.StreamConfig.StreamFormat.SampleFrequency - (PlaybackContext.ActiveSampleFrequency - SAMPLE_FREQUENCY_STEP)) > MIN_LIMIT_SAMPLE_FREQUENCY)
                     {
                        printf("; however, limits reached (%ld, %d)\n", PlaybackContext.StreamConfig.StreamFormat.SampleFrequency, PlaybackContext.ActiveSampleFrequency);
                     }
                     else
                     {
                        PlaybackContext.NewSampleFrequency -= SAMPLE_FREQUENCY_STEP;
                        printf(" %d\n", PlaybackContext.NewSampleFrequency);
                     }
                  }
                  else if(Difference < -VARIATION_THRESHOLD)
                  {
                     printf("Speed up playback");
                     if((PlaybackContext.StreamConfig.StreamFormat.SampleFrequency - (PlaybackContext.ActiveSampleFrequency + SAMPLE_FREQUENCY_STEP)) > MIN_LIMIT_SAMPLE_FREQUENCY)
                     {
                        printf("; however, limits reached (%ld, %d)\n", PlaybackContext.StreamConfig.StreamFormat.SampleFrequency, PlaybackContext.ActiveSampleFrequency);
                     }
                     else
                     {
                        PlaybackContext.NewSampleFrequency += SAMPLE_FREQUENCY_STEP;
                        printf(" %d\n", PlaybackContext.NewSampleFrequency);
                     }
                  }
                  else
                  {
                     printf("Stable\n");
                  }
               }

               LastAverage = CurrentAverage;
            }
            else
            {
               LastAverage  = CurrentAverage;
               printf("First average %d\n", CurrentAverage);
            }

            Iterations  = 0;
            RollingSum  = 0;
         }
      }
   }
   else
   {
      LastAverage = 0;
      RollingSum  = 0;

      if(PlaybackContext.PlaybackState != adPlaying)
      {
         /* Because we are not in the playing state, ignore some of the */
         /* beginning data. For example, when filling the buffer the    */
         /* hardware will continue to accept data faster than we are    */
         /* playing.                                                    */
         Iterations = -250;
      }
      else
      {
         Iterations = 0;
      }
   }

   if(PlaybackContext.ActiveSampleFrequency != PlaybackContext.NewSampleFrequency)
   {
      /* Only reconfigure if we are outside of the limits */
      if((Difference < SBC_SAMPE_FREQUENCY_LOW_WATER_MARK) || (Difference > SBC_SAMPE_FREQUENCY_HIGH_WATER_MARK))
      {
         Reconfigure = TRUE;
      }
   }

   if(Reconfigure)
   {
      /* Don't bother adjusting the frequency until we pass the*/
      /* limits                                                */
      printf("Attempting to reconfigure\n");

      /* The following handles reconfiguring the sample     */
      /* frequency. This is done by ping/pong alsa devices. */
      if(PlaybackContext.ActivePcmHandle == PlaybackContext.PcmHandle1)
      {
         PlaybackContext.ActivePcmHandle = PlaybackContext.PcmHandle2;
      }
      else
      {
         PlaybackContext.ActivePcmHandle = PlaybackContext.PcmHandle1;
      }

      if(0 == (Result = ConfigureAudio(PlaybackContext.ActivePcmHandle)))
      {
         printf("Reconfigured\n");

         /* Put in a delay for the hardware to flush before starting the*/
         /* next stream.                                                */
         usleep(PING_PONG_ALSA_MICROSECOND_DELAY);

         /* Reset because we just reconfigured                          */
         LastAverage = 0;
         RollingSum  = 0;
      }
      else
      {
         printf("Reconfigure failed %d\n", Result);
      }
   }
}

   /* The following thread function is used to process SBC data         */
   /* retrieved by the remote device.                                   */
static void *PlaybackThread(void *ThreadParameter)
{
   int                   Result;
   unsigned int          BufferOffset;
   int                   size;
   unsigned char        *SbcBuffer;
   unsigned int          SbcBufferSize;
   static unsigned char  PcmBuffer[PCM_BUFFER_SIZE];
   unsigned int          PcmBufferSize = PCM_BUFFER_SIZE;
   int                   BufferDiff;

   /* Configure the audio subsystem                                     */
   if(0 == (Result = ConfigureAudio(PlaybackContext.ActivePcmHandle)))
   {
      size         = PlaybackContext.PcmFramesToWrite * 4; /* 2 bytes/sample, 2 channels */
      BufferOffset = 0;

      while(Initialized)
      {
         /* Take possession of the buffer                               */
         if(BTPS_WaitMutex(PlaybackContext.Mutex, BTPS_INFINITE_WAIT))
         {
            BufferDiff = QueryBufferDiffernce();

            /* Confirm we have buffers to process.                      */
            if((BufferDiff != 0) && ((PlaybackContext.PlaybackState == adPlaying) || (PlaybackContext.PlaybackState == adFlushingBuffer)))
            {
               if(NULL != (SbcBuffer = PlaybackContext.ReceivedAudioData[PlaybackContext.CurrentProccessAudioDataIndex].Buffer))
               {
                  SbcBufferSize                                                                           = PlaybackContext.ReceivedAudioData[PlaybackContext.CurrentProccessAudioDataIndex].Length;
                  PlaybackContext.ReceivedAudioData[PlaybackContext.CurrentProccessAudioDataIndex].Buffer = NULL;

                  /* Update the index we are processing and move to the */
                  /* next.                                              */
                  PlaybackContext.CurrentProccessAudioDataIndex++;
                  PlaybackContext.CurrentProccessAudioDataIndex %= NUMBER_OF_RECEIVE_BUFFERS;

                  BTPS_ReleaseMutex(PlaybackContext.Mutex);

                  /* Decode the data                                    */
                  PcmBufferSize = PCM_BUFFER_SIZE;
                  if(0 == (Result = SbcDecode(SbcBuffer, SbcBufferSize, PcmBuffer, &PcmBufferSize)))
                  {
                     BufferOffset = 0;

                     AdjustPcmPlayback(BufferDiff);

                     while(BufferOffset < PcmBufferSize)
                     {
                        Result = snd_pcm_writei(PlaybackContext.ActivePcmHandle, &(PcmBuffer[BufferOffset]), PlaybackContext.PcmFramesToWrite);
                        if (Result == -EPIPE)
                        {
                           /* EPIPE means underrun                      */
                           printf("Alsa underrun\n");
                           snd_pcm_prepare(PlaybackContext.ActivePcmHandle);
                        }
                        else if (Result < 0)
                        {
                           printf("Error from writei: %s\n", snd_strerror(Result));
                           break;
                        }
                        else if (Result != (int)PlaybackContext.PcmFramesToWrite)
                        {
                           printf("Short write. %d frames\n", Result);
                           break;
                        }

                        BufferOffset += size;
                     }
                  }
                  else
                  {
                     printf("Failed to decode %d\n", Result);
                  }

                  BTPS_FreeMemory(SbcBuffer);
               }
               else
               {
                  BTPS_ReleaseMutex(PlaybackContext.Mutex);

                  /* The index should be valid, so this error should    */
                  /* never occur. Go ahead and print the error, in case */
                  /* someone modifies the code to introduces the problem*/
                  /* bug.                                               */
                  printf("Error, buffer should be present\n");
                  sleep(1);
               }
            }
            else
            {
               if(BufferDiff == 0)
               {
                  printf("Filling buffer\n");

                  /* Wait for the buffers to fill up to the low water   */
                  /* mark.                                              */
                  PlaybackContext.PlaybackState = adFillingBuffer;
               }

               BTPS_ResetEvent(PlaybackContext.StartProcessing);

               BTPS_ReleaseMutex(PlaybackContext.Mutex);

               BTPS_WaitEvent(PlaybackContext.StartProcessing, BTPS_INFINITE_WAIT);
            }
         }
      }

      snd_pcm_drain(PlaybackContext.ActivePcmHandle);

      /* Set the event to shutdown */
      BTPS_SetEvent(PlaybackContext.ShutdownEvent);
   }
   else
   {
      printf("Failed to configure audio %d\n", Result);
   }

   printf("Exit playback thread\n");

   return NULL;
}

   /* The following function initializes the audio decoder. The first   */
   /* parameter is a valid Audio Manager Data Handler ID (registered    */
   /* via call to the AUDM_Register_Data_Event_Callback() function),    */
   /* followed by optional configuration flags. This function will      */
   /* return zero on success and negative on error.                     */
int InitializeAudioDecoder(unsigned int ConfigFlags)
{
   int Result = -1;

   memset(&PlaybackContext, 0, sizeof(PlaybackContext_t));

   if(FALSE == Initialized)
   {
      if((Result = AUDM_Query_Audio_Stream_Configuration(astSNK, &PlaybackContext.StreamConfig)) == 0)
      {
         /* Note the current frequency                                  */
         PlaybackContext.ActiveSampleFrequency = PlaybackContext.StreamConfig.StreamFormat.SampleFrequency;
         PlaybackContext.NewSampleFrequency    = PlaybackContext.StreamConfig.StreamFormat.SampleFrequency;
         PlaybackContext.ConfigFlags           = ConfigFlags;

         if((PlaybackContext.StreamConfig.MediaCodecType == A2DP_MEDIA_CODEC_TYPE_SBC) && (PlaybackContext.StreamConfig.MediaCodecInfoLength == sizeof(A2DP_SBC_Codec_Specific_Information_Element_t)))
         {
            if((NULL != (PlaybackContext.ShutdownEvent = BTPS_CreateEvent(FALSE))) && (NULL != (PlaybackContext.Mutex = BTPS_CreateMutex(FALSE))) && (NULL != (PlaybackContext.StartProcessing = BTPS_CreateEvent(FALSE))))
            {
               if((Result = snd_pcm_open(&PlaybackContext.PcmHandle1, "default", SND_PCM_STREAM_PLAYBACK, 0)) >= 0)
               {
                  if((Result = snd_pcm_open(&PlaybackContext.PcmHandle2, "default", SND_PCM_STREAM_PLAYBACK, 0)) >= 0)
                  {
                     PlaybackContext.ActivePcmHandle = PlaybackContext.PcmHandle1;

                     /* Now, we are ready to start decoding. First,     */
                     /* let's initialize the Decoder.                   */
                     if((PlaybackContext.DecoderHandle = SBC_Initialize_Decoder()) != NULL)
                     {
                        Initialized = TRUE;

                        if(BTPS_CreateThread(PlaybackThread, 16384, NULL))
                        {
                           Result = 0;
                        }
                        else
                        {
                           Initialized = FALSE;
                           printf("Unable to start Playback Thread.\r\n");
                        }
                     }
                     else
                     {
                        printf("Failed to decoder.\r\n");
                     }
                  }
               }
               else
               {
                  printf("Failed to open pcm.\r\n");
               }
            }
            else
            {
               printf("Failed to create mutex/event.\r\n");
            }
         }
         else
         {
            printf("Unsupported stream type or invalid configuration\n");
         }
      }
   }
   else
   {
      printf("Unable to query audio connection configuration (%d)\n", Result);
   }

   return Result;
}

   /* The following function is responsible for freeing all resources   */
   /* that were previously allocated for an audio decoder.              */
void CleanupAudioDecoder(void)
{
   Initialized = FALSE;

   /* Set the event to kill the thread                                  */
   if((PlaybackContext.ShutdownEvent) && (PlaybackContext.StartProcessing))
   {
      BTPS_SetEvent(PlaybackContext.StartProcessing);

      printf("Waiting for thread...\n");
      BTPS_WaitEvent(PlaybackContext.ShutdownEvent, BTPS_INFINITE_WAIT);
      printf("Wait done\n");

      BTPS_CloseEvent(PlaybackContext.ShutdownEvent);
      PlaybackContext.ShutdownEvent = NULL;

      BTPS_CloseEvent(PlaybackContext.StartProcessing);
      PlaybackContext.StartProcessing = NULL;
   }


   if(NULL != PlaybackContext.PcmFileDescriptor)
   {
      fclose(PlaybackContext.PcmFileDescriptor);
      PlaybackContext.PcmFileDescriptor = NULL;
   }

   if(NULL != PlaybackContext.WavFileDescriptor)
   {
      fclose(PlaybackContext.WavFileDescriptor);
      PlaybackContext.WavFileDescriptor = NULL;
   }

   if(NULL != PlaybackContext.DecoderHandle)
   {
      /* We are all finished with the decoder, so we can inform the     */
      /* library that we are finished with the handle that we opened.   */
      SBC_Cleanup_Decoder(PlaybackContext.DecoderHandle);
      PlaybackContext.DecoderHandle = NULL;
   }

   if(NULL != PlaybackContext.PcmHandle1)
   {
      snd_pcm_close(PlaybackContext.PcmHandle1);
      PlaybackContext.PcmHandle1 = NULL;
   }

   if(NULL != PlaybackContext.PcmHandle2)
   {
      snd_pcm_close(PlaybackContext.PcmHandle2);
      PlaybackContext.PcmHandle2 = NULL;
   }

   if(PlaybackContext.Mutex)
   {
      BTPS_CloseMutex(PlaybackContext.Mutex);
      PlaybackContext.Mutex = NULL;
   }
}

   /* The following function is used to process audio data. The first   */
   /* parameter is buffer to the raw SBC audio. The second parameter is */
   /* the length of the audio buffer. A negative value will be returned */
   /* on error                                                          */
int ProcessAudioData(void *RawAudioDataFrame, unsigned int RawAudioDataFrameLength)
{
   int   Result   = -1;
   void *AudioDataCopy;
   int   BufferDiff;

   /* Ignore data if we are not initialized                             */
   if(TRUE == Initialized)
   {
      /* Confirm we have a buffer, and the length.                      */
      if((RawAudioDataFrameLength) && (RawAudioDataFrame != NULL))
      {
         if(DECODER_CONFIG_FLAGS_ENABLE_LOGGING_SBC & PlaybackContext.ConfigFlags)
            WriteSbcToFile((unsigned char *)RawAudioDataFrame, RawAudioDataFrameLength);

         if(BTPS_WaitMutex(PlaybackContext.Mutex, BTPS_INFINITE_WAIT))
         {
            BufferDiff = QueryBufferDiffernce();

            /* Search for a full buffer (It is consider full to be 1    */
            /* less than, actual size. This is to handle the case of    */
            /* full/empty being the same value.)                        */
            if((BufferDiff != (NUMBER_OF_RECEIVE_BUFFERS - 1)) && (PlaybackContext.PlaybackState >= adFillingBuffer))
            {
               /* Confirm we have place to put the data.                */
               if(NULL == PlaybackContext.ReceivedAudioData[PlaybackContext.CurrentRecievedAudioDataIndex].Buffer)
               {
                  /* Allocate the buffer to hold the copy.              */
                  if(NULL != (AudioDataCopy = BTPS_AllocateMemory(RawAudioDataFrameLength)))
                  {
                     BTPS_MemCopy(AudioDataCopy, RawAudioDataFrame, RawAudioDataFrameLength);

                     PlaybackContext.ReceivedAudioData[PlaybackContext.CurrentRecievedAudioDataIndex].Length = RawAudioDataFrameLength;
                     PlaybackContext.ReceivedAudioData[PlaybackContext.CurrentRecievedAudioDataIndex].Buffer = AudioDataCopy;

                     /* Move to the next buffer in the circular list.   */
                     PlaybackContext.CurrentRecievedAudioDataIndex++;
                     PlaybackContext.CurrentRecievedAudioDataIndex %= NUMBER_OF_RECEIVE_BUFFERS;

                     /* Flag success.                                   */
                     Result = 0;

                     if(PlaybackContext.PlaybackState == adFillingBuffer)
                     {
                        if(PlaybackContext.ActiveSampleFrequency != PlaybackContext.NewSampleFrequency)
                        {
                           if(0 == (Result = ConfigureAudio(PlaybackContext.ActivePcmHandle)))
                           {
                              printf("Reconfigured\n");
                           }
                        }
                        if(BufferDiff > SBC_LOW_WATER_MARK)
                        {
                           /* We have cross back over the threshold, so */
                           /* go to playing state.                      */
                           PlaybackContext.PlaybackState = adPlaying;

                           BTPS_SetEvent(PlaybackContext.StartProcessing);

                           printf("Low water passed %d\n", BufferDiff);
                        }
                     }

                     BTPS_ReleaseMutex(PlaybackContext.Mutex);
                  }
                  else
                  {
                     BTPS_ReleaseMutex(PlaybackContext.Mutex);

                     printf("Dropping audio packet (No memory)\n");
                  }
               }
               else
               {
                  BTPS_ReleaseMutex(PlaybackContext.Mutex);

                  /* The index should be valid, so this error should    */
                  /* never occur. Go ahead and print the error, in case */
                  /* someone modifies the code to introduces the problem*/
                  /* bug.                                               */
                  printf("Error, buffer not processed.\n");
               }
            }
            else
            {
               if(BufferDiff == (NUMBER_OF_RECEIVE_BUFFERS - 1))
               {
                  /* Flush the buffers until we get below the high water*/
                  /* mark                                               */
                  PlaybackContext.PlaybackState = adFlushingBuffer;

                  printf("Over run, dropping audio\n");
               }
               else if(PlaybackContext.PlaybackState == adFlushingBuffer)
               {
                  if(BufferDiff < SBC_HIGH_WATER_MARK)
                  {
                     /* We have cross back under the threshold, so go   */
                     /* back to playing state.                          */
                     PlaybackContext.PlaybackState = adPlaying;

                     printf("Below high water %d\n", BufferDiff);
                  }
               }

               BTPS_ReleaseMutex(PlaybackContext.Mutex);
            }
         }
      }
      else
      {
         printf("Invalid parameters %d %p\n", RawAudioDataFrameLength, RawAudioDataFrame);
      }
   }

   return Result;
}
