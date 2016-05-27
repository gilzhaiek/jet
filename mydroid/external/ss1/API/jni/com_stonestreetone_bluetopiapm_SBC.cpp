/*****< com_stonestreetone_bluetopiapm_SBC.cpp >******************************/
/*      Copyright 2010 - 2014 Stonestreet One.                                */
/*      All Rights Reserved.                                                  */
/*                                                                            */
/*  com_stonestreetone_bluetopiapm_SBC - JNI Module for Stonestreet One       */
/*                                       Bluetopia Platform Manager SBC       */
/*                                       Java API.                            */
/*                                                                            */
/*  Author:  Greg Hensley                                                     */
/*                                                                            */
/*** MODIFICATION HISTORY *****************************************************/
/*                                                                            */
/*   mm/dd/yy  F. Lastname    Description of Modification                     */
/*   --------  -----------    ------------------------------------------------*/
/*   03/04/12  G. Hensley     Initial creation.                               */
/******************************************************************************/

extern "C" {
#include "SS1SBC.h"
}

#include "com_stonestreetone_bluetopiapm.h"
#include "com_stonestreetone_bluetopiapm_SBC.h"

#define MAXIMUM_DECODED_BUFFER_SIZE_BYTES                   512

typedef struct _tagList_t
{
   _tagList_t *Next;
   _tagList_t *Prev;
} List_t;

typedef struct _tagDecodedFrameData_t
{
   List_t                     FrameList;

   Word_t                    *Buffer;
   unsigned int               BufferSize;
   SBC_Decode_Configuration_t Config;
} DecodedFrameData_t;

typedef struct _tagDecoderData_t
{
   Decoder_t    Handle;

   Mutex_t      DecodedFrameListLock;
   unsigned int DecodedFrameListLength;
   List_t       DecodedFrameList;
} DecoderData_t;

static inline Boolean_t ListInitialize(List_t *ListEntry)
{
   if(ListEntry)
   {
      ListEntry->Next = ListEntry;
      ListEntry->Prev = ListEntry;

      return TRUE;
   }
   else
      return FALSE;
}

static inline Boolean_t ListIsEmpty(List_t *ListHead)
{
   return ((ListHead) && (ListHead->Next == ListHead) && (ListHead->Prev == ListHead));
}

#define ListGetEntry(ListNodePtr, EntryType, EntryListMember) \
({ \
   List_t *_Ptr = (ListNodePtr); \
   (_Ptr == NULL) ? NULL : ((EntryType *)((Byte_t *)_Ptr - STRUCTURE_OFFSET(EntryType, EntryListMember))); \
})

static inline List_t *ListNext(List_t *List)
{
   return ((List) ? List->Next : NULL);
}

static inline Boolean_t ListInsertBefore(List_t *List, List_t *NewEntry)
{
   if((List) && (NewEntry) && (List->Prev))
   {
      NewEntry->Next = List;
      NewEntry->Prev = List->Prev;

      List->Prev->Next = NewEntry;
      List->Prev       = NewEntry;

      return TRUE;
   }
   else
      return FALSE;
}

static inline List_t *ListRemove(List_t *ListEntry)
{
   if((ListEntry) && (!ListIsEmpty(ListEntry)))
   {
      ListEntry->Prev->Next = ListEntry->Next;
      ListEntry->Next->Prev = ListEntry->Prev;

      return (ListInitialize(ListEntry) ? ListEntry : NULL);
   }
   else
      return NULL;
}

static inline DecodedFrameData_t *AllocateDecodedFrameData()
{
   DecodedFrameData_t *FrameData;

   if((FrameData = (DecodedFrameData_t *)BTPS_AllocateMemory(sizeof(DecodedFrameData_t))) != NULL)
   {
      if((FrameData->Buffer = (Word_t *)BTPS_AllocateMemory(MAXIMUM_DECODED_BUFFER_SIZE_BYTES)) != NULL)
      {
         FrameData->BufferSize = (MAXIMUM_DECODED_BUFFER_SIZE_BYTES / sizeof(FrameData->Buffer[0]));

         BTPS_MemInitialize(&(FrameData->Config), 0, sizeof(FrameData->Config));
         ListInitialize(&(FrameData->FrameList));
      }
      else
      {
         BTPS_FreeMemory(FrameData);
         FrameData = NULL;
      }
   }
   else
      FrameData = NULL;

   return FrameData;
}

static inline void FreeDecodedFrameData(DecodedFrameData_t *FrameData)
{
   if(FrameData)
   {
      if(!ListIsEmpty(&(FrameData->FrameList)))
         ListRemove(&(FrameData->FrameList));

      if(FrameData->Buffer)
         BTPS_FreeMemory(FrameData->Buffer);

      BTPS_FreeMemory(FrameData);
   }
}

static Boolean_t SetEncoderConfiguration(SBC_Encode_Configuration_t *Config, jint SamplingFrequency, jint BlockSize, jint ChannelMode, jint AllocationMethod, jint Subbands, jint MaximumBitRate)
{
   Boolean_t ret_val = TRUE;

   if(Config)
   {
      switch(SamplingFrequency)
      {
         case 16000:
            Config->SamplingFrequency = sf16kHz;
            break;
         case 32000:
            Config->SamplingFrequency = sf32kHz;
            break;
         case 44100:
            Config->SamplingFrequency = sf441kHz;
            break;
         case 48000:
            Config->SamplingFrequency = sf48kHz;
            break;
         default:
            ret_val = FALSE;
            break;
      }

      switch(BlockSize)
      {
         case 4:
            Config->BlockSize = bsFour;
            break;
         case 8:
            Config->BlockSize = bsEight;
            break;
         case 12:
            Config->BlockSize = bsTwelve;
            break;
         case 16:
            Config->BlockSize = bsSixteen;
            break;
         default:
            ret_val = FALSE;
            break;
      }

      switch(ChannelMode)
      {
         case com_stonestreetone_bluetopiapm_SBC_CHANNEL_MODE_MONO:
            Config->ChannelMode = cmMono;
            break;
         case com_stonestreetone_bluetopiapm_SBC_CHANNEL_MODE_DUAL_CHANNEL:
            Config->ChannelMode = cmDualChannel;
            break;
         case com_stonestreetone_bluetopiapm_SBC_CHANNEL_MODE_STEREO:
            Config->ChannelMode = cmStereo;
            break;
         case com_stonestreetone_bluetopiapm_SBC_CHANNEL_MODE_JOINT_STEREO:
            Config->ChannelMode = cmJointStereo;
            break;
         default:
            ret_val = FALSE;
            break;
      }

      switch(AllocationMethod)
      {
         case com_stonestreetone_bluetopiapm_SBC_ALLOCATION_METHOD_LOUNDNESS:
            Config->AllocationMethod = amLoudness;
            break;
         case com_stonestreetone_bluetopiapm_SBC_ALLOCATION_METHOD_SNR:
            Config->AllocationMethod = amSNR;
            break;
         default:
            ret_val = FALSE;
            break;
      }

      switch(Subbands)
      {
         case 4:
            Config->Subbands = sbFour;
            break;
         case 8:
            Config->Subbands = sbEight;
            break;
         default:
            ret_val = FALSE;
            break;
      }

      Config->MaximumBitRate = MaximumBitRate;
   }
   else
      ret_val = FALSE;

   return ret_val;
}

static Boolean_t GetDecoderConfiguration(SBC_Decode_Configuration_t *Config, jint *ConfigArray)
{
   Boolean_t ret_val = TRUE;

   if((Config) && (ConfigArray))
   {
      switch(Config->SamplingFrequency)
      {
         case sf16kHz:
            ConfigArray[0] = 16000;
            break;
         case sf32kHz:
            ConfigArray[0] = 32000;
            break;
         case sf441kHz:
            ConfigArray[0] = 44100;
            break;
         case sf48kHz:
            ConfigArray[0] = 48000;
            break;
         default:
            ret_val = FALSE;
            break;
      }

      switch(Config->BlockSize)
      {
         case bsFour:
            ConfigArray[1] = 4;
            break;
         case bsEight:
            ConfigArray[1] = 8;
            break;
         case bsTwelve:
            ConfigArray[1] = 12;
            break;
         case bsSixteen:
            ConfigArray[1] = 16;
            break;
         default:
            ret_val = FALSE;
            break;
      }

      switch(Config->ChannelMode)
      {
         case cmMono:
            ConfigArray[2] = com_stonestreetone_bluetopiapm_SBC_CHANNEL_MODE_MONO;
            break;
         case cmDualChannel:
            ConfigArray[2] = com_stonestreetone_bluetopiapm_SBC_CHANNEL_MODE_DUAL_CHANNEL;
            break;
         case cmStereo:
            ConfigArray[2] = com_stonestreetone_bluetopiapm_SBC_CHANNEL_MODE_STEREO;
            break;
         case cmJointStereo:
            ConfigArray[2] = com_stonestreetone_bluetopiapm_SBC_CHANNEL_MODE_JOINT_STEREO;
            break;
         default:
            ret_val = FALSE;
            break;
      }

      switch(Config->AllocationMethod)
      {
         case amLoudness:
            ConfigArray[3] = com_stonestreetone_bluetopiapm_SBC_ALLOCATION_METHOD_LOUNDNESS;
            break;
         case amSNR:
            ConfigArray[3] = com_stonestreetone_bluetopiapm_SBC_ALLOCATION_METHOD_SNR;
            break;
         default:
            ret_val = FALSE;
            break;
      }

      switch(Config->Subbands)
      {
         case sbFour:
            ConfigArray[4] = 4;
            break;
         case sbEight:
            ConfigArray[4] = 8;
            break;
         default:
            ret_val = FALSE;
            break;
      }

      ConfigArray[5] = (jint)(Config->BitRate);
      ConfigArray[6] = (jint)(Config->BitPool);
      ConfigArray[7] = (jint)(Config->FrameLength);
   }
   else
      ret_val = FALSE;

   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_SBC
 * Method:    calculateEncoderBitPoolSizeNative
 * Signature: (IIIIII)I
 */
static jint CalculateEncoderBitPoolSizeNative(JNIEnv *Env, jobject Object, jint SamplingFrequency, jint BlockSize, jint ChannelMode, jint AllocationMethod, jint Subbands, jint MaximumBitRate)
{
   jint                       ret_val;
   SBC_Encode_Configuration_t Config;

   if(SetEncoderConfiguration(&Config, SamplingFrequency, BlockSize, ChannelMode, AllocationMethod, Subbands, MaximumBitRate))
      ret_val = SBC_CalculateEncoderBitPoolSize(&Config);
   else
      ret_val = SBC_ERROR_INVALID_PARAMETER;

   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_SBC
 * Method:    calculateEncoderFrameLengthNative
 * Signature: (IIIIII)I
 */
static jint CalculateEncoderFrameLengthNative(JNIEnv *Env, jobject Object, jint SamplingFrequency, jint BlockSize, jint ChannelMode, jint AllocationMethod, jint Subbands, jint MaximumBitRate)
{
   jint                       ret_val;
   SBC_Encode_Configuration_t Config;

   if(SetEncoderConfiguration(&Config, SamplingFrequency, BlockSize, ChannelMode, AllocationMethod, Subbands, MaximumBitRate))
      ret_val = SBC_CalculateEncoderFrameLength(&Config);
   else
      ret_val = SBC_ERROR_INVALID_PARAMETER;

   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_SBC
 * Method:    calculateEncoderBitRateNative
 * Signature: (IIIIII)I
 */
static jint CalculateEncoderBitRateNative(JNIEnv *Env, jobject Object, jint SamplingFrequency, jint BlockSize, jint ChannelMode, jint AllocationMethod, jint Subbands, jint MaximumBitRate)
{
   jint                       ret_val;
   SBC_Encode_Configuration_t Config;

   if(SetEncoderConfiguration(&Config, SamplingFrequency, BlockSize, ChannelMode, AllocationMethod, Subbands, MaximumBitRate))
      ret_val = SBC_CalculateEncoderBitRate(&Config);
   else
      ret_val = SBC_ERROR_INVALID_PARAMETER;

   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_SBC
 * Method:    calculateDecoderFrameSizeNative
 * Signature: ([B)I
 */
static jint CalculateDecoderFrameSizeNative(JNIEnv *Env, jobject Object, jbyteArray BitStreamData)
{
   jint   ret_val;
   jbyte *Data;

   if(BitStreamData)
   {
      if((Data = Env->GetByteArrayElements(BitStreamData, NULL)) != NULL)
      {
         ret_val = SBC_CalculateDecoderFrameSize(Env->GetArrayLength(BitStreamData), (Byte_t*)Data);
         Env->ReleaseByteArrayElements(BitStreamData, (jbyte*)Data, JNI_ABORT);
      }
      else
         ret_val = SBC_ERROR_INSUFFICIENT_RESOURCES;
   }
   else
      ret_val = SBC_ERROR_INVALID_PARAMETER;

   return ret_val;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_SBC
 * Method:    initializeEncoderNative
 * Signature: (IIIIII)J
 */
static jlong InitializeEncoderNative(JNIEnv *Env, jobject Object, jint SamplingFrequency, jint BlockSize, jint ChannelMode, jint AllocationMethod, jint Subbands, jint MaximumBitRate)
{
   SBC_Encode_Configuration_t EncoderConfiguration;

   switch(SamplingFrequency)
   {
      case 16000:
         EncoderConfiguration.SamplingFrequency = sf16kHz;
         break;
      case 32000:
         EncoderConfiguration.SamplingFrequency = sf32kHz;
         break;
      case 44100:
         EncoderConfiguration.SamplingFrequency = sf441kHz;
         break;
      case 48000:
         EncoderConfiguration.SamplingFrequency = sf48kHz;
   }

   switch(BlockSize)
   {
      case 4:
         EncoderConfiguration.BlockSize = bsFour;
         break;
      case 8:
         EncoderConfiguration.BlockSize = bsEight;
         break;
      case 12:
         EncoderConfiguration.BlockSize = bsTwelve;
         break;
      case 16:
         EncoderConfiguration.BlockSize = bsSixteen;
   }

   switch(ChannelMode)
   {
      case com_stonestreetone_bluetopiapm_SBC_CHANNEL_MODE_MONO:
         EncoderConfiguration.ChannelMode = cmMono;
         break;
      case com_stonestreetone_bluetopiapm_SBC_CHANNEL_MODE_DUAL_CHANNEL:
         EncoderConfiguration.ChannelMode = cmDualChannel;
         break;
      case com_stonestreetone_bluetopiapm_SBC_CHANNEL_MODE_STEREO:
         EncoderConfiguration.ChannelMode = cmStereo;
         break;
      case com_stonestreetone_bluetopiapm_SBC_CHANNEL_MODE_JOINT_STEREO:
         EncoderConfiguration.ChannelMode = cmJointStereo;
   }

   switch(AllocationMethod)
   {
      case com_stonestreetone_bluetopiapm_SBC_ALLOCATION_METHOD_LOUNDNESS:
         EncoderConfiguration.AllocationMethod = amLoudness;
         break;
      case com_stonestreetone_bluetopiapm_SBC_ALLOCATION_METHOD_SNR:
         EncoderConfiguration.AllocationMethod = amSNR;
   }

   switch(Subbands)
   {
      case 4:
         EncoderConfiguration.Subbands = sbFour;
         break;
      case 8:
         EncoderConfiguration.Subbands = sbEight;
   }

   EncoderConfiguration.MaximumBitRate = (DWord_t)MaximumBitRate;

   return (jlong)SBC_Initialize_Encoder(&EncoderConfiguration);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_SBC
 * Method:    cleanupEncoderNative
 * Signature: (J)V
 */
static void CleanupEncoderNative(JNIEnv *Env, jobject Object, jlong EncoderHandle)
{
   SBC_Cleanup_Encoder((Encoder_t)EncoderHandle);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_SBC
 * Method:    changeEncoderConfigurationNative
 * Signature: (JIIIIII)I
 */
static jint ChangeEncoderConfigurationNative(JNIEnv *Env, jobject Object, jlong EncoderHandle, jint SamplingFrequency, jint BlockSize, jint ChannelMode, jint AllocationMethod, jint Subbands, jint MaximumBitRate)
{
   SBC_Encode_Configuration_t EncoderConfiguration;

   switch(SamplingFrequency)
   {
      case 16000:
         EncoderConfiguration.SamplingFrequency = sf16kHz;
         break;
      case 32000:
         EncoderConfiguration.SamplingFrequency = sf32kHz;
         break;
      case 44100:
         EncoderConfiguration.SamplingFrequency = sf441kHz;
         break;
      case 48000:
         EncoderConfiguration.SamplingFrequency = sf48kHz;
   }

   switch(BlockSize)
   {
      case 4:
         EncoderConfiguration.BlockSize = bsFour;
         break;
      case 8:
         EncoderConfiguration.BlockSize = bsEight;
         break;
      case 12:
         EncoderConfiguration.BlockSize = bsTwelve;
         break;
      case 16:
         EncoderConfiguration.BlockSize = bsSixteen;
   }

   switch(ChannelMode)
   {
      case com_stonestreetone_bluetopiapm_SBC_CHANNEL_MODE_MONO:
         EncoderConfiguration.ChannelMode = cmMono;
         break;
      case com_stonestreetone_bluetopiapm_SBC_CHANNEL_MODE_DUAL_CHANNEL:
         EncoderConfiguration.ChannelMode = cmDualChannel;
         break;
      case com_stonestreetone_bluetopiapm_SBC_CHANNEL_MODE_STEREO:
         EncoderConfiguration.ChannelMode = cmStereo;
         break;
      case com_stonestreetone_bluetopiapm_SBC_CHANNEL_MODE_JOINT_STEREO:
         EncoderConfiguration.ChannelMode = cmJointStereo;
   }

   switch(AllocationMethod)
   {
      case com_stonestreetone_bluetopiapm_SBC_ALLOCATION_METHOD_LOUNDNESS:
         EncoderConfiguration.AllocationMethod = amLoudness;
         break;
      case com_stonestreetone_bluetopiapm_SBC_ALLOCATION_METHOD_SNR:
         EncoderConfiguration.AllocationMethod = amSNR;
   }

   switch(Subbands)
   {
      case 4:
         EncoderConfiguration.Subbands = sbFour;
         break;
      case 8:
         EncoderConfiguration.Subbands = sbEight;
   }

   EncoderConfiguration.MaximumBitRate = (DWord_t)MaximumBitRate;

   return SBC_Change_Encoder_Configuration((Encoder_t)EncoderHandle, &EncoderConfiguration);
}

/*
 * Class:     com_stonestreetone_bluetopiapm_SBC
 * Method:    encodeDataNative
 * Signature: (J[B[BI)I
 */
static jint EncodeDataNative(JNIEnv *Env, jobject Object, jlong EncoderHandle, jbyteArray Data, jbyteArray EncodedData, jint SampleLength)
{
   int                           Result;
   int                           RawDataLength;
   int                           EncodedDataLength;
   unsigned char                 EncodedDataBuffer[8192];
   jint                          NumberFrames;
   jbyte                        *RawData;
   jbyte                        *SBCData;
   SBC_Encode_Data_t             EncodeData;
   SBC_Encode_Bit_Stream_Data_t  EncodeBitStreamData;

   if((RawData = Env->GetByteArrayElements(Data, NULL)) != NULL)
   {
      if((SBCData = Env->GetByteArrayElements(EncodedData, NULL)) != NULL)
      {
         Result            = SBC_PROCESSING_COMPLETE;  
         NumberFrames      = 0;
         RawDataLength     = Env->GetArrayLength(Data);
         EncodedDataLength = 0;

         EncodeData.ChannelDataLength       = RawDataLength/SampleLength;
         EncodeData.UnusedChannelDataLength = EncodeData.ChannelDataLength;

         /* Setup our bitstream structure.                              */
         EncodeBitStreamData.BitStreamDataPtr    = EncodedDataBuffer;
         EncodeBitStreamData.BitStreamDataSize   = sizeof(EncodedDataBuffer);
         EncodeBitStreamData.BitStreamDataLength = 0;

         while((EncodeData.UnusedChannelDataLength > 0) && (Result == SBC_PROCESSING_COMPLETE))
         {
            EncodeData.LeftChannelDataPtr  = (unsigned short *)&(RawData[RawDataLength - (EncodeData.UnusedChannelDataLength * SampleLength)]);
            EncodeData.RightChannelDataPtr = EncodeData.LeftChannelDataPtr + 1;

            Result = SBC_Encode_Data((Encoder_t)EncoderHandle, &EncodeData, &EncodeBitStreamData);

            if(Result == SBC_PROCESSING_COMPLETE)
            {
               BTPS_MemCopy(&(SBCData[EncodedDataLength]), EncodeBitStreamData.BitStreamDataPtr, EncodeBitStreamData.BitStreamDataLength);

               NumberFrames++;
               EncodeData.ChannelDataLength = EncodeData.UnusedChannelDataLength;
               EncodedDataLength += EncodeBitStreamData.BitStreamDataLength;
            }
         }

         Env->ReleaseByteArrayElements(EncodedData, SBCData, 0);
      }
      else
         NumberFrames = BTPM_ERROR_CODE_INVALID_PARAMETER;

      Env->ReleaseByteArrayElements(Data, RawData, JNI_ABORT);
   }
   else
      NumberFrames = BTPM_ERROR_CODE_INVALID_PARAMETER;

   return NumberFrames;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_SBC
 * Method:    initializeDecoderNative
 * Signature: ()J
 */
static jlong InitializeDecoderNative(JNIEnv *Env, jobject Object)
{
   DecoderData_t *DecoderData;

   if((DecoderData = (DecoderData_t *)BTPS_AllocateMemory(sizeof(DecoderData_t))) != NULL)
   {
      BTPS_MemInitialize(DecoderData, 0, sizeof(DecoderData_t));

      DecoderData->Handle = SBC_Initialize_Decoder();

      if(DecoderData->Handle)
      {
         DecoderData->DecodedFrameListLock   = BTPS_CreateMutex(TRUE);
         DecoderData->DecodedFrameListLength = 0;
         ListInitialize(&(DecoderData->DecodedFrameList));
         BTPS_ReleaseMutex(DecoderData->DecodedFrameListLock);
      }
   }

   return (jlong)DecoderData;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_SBC
 * Method:    cleanupDecoderNative
 * Signature: (J)V
 */
static void CleanupDecoderNative(JNIEnv *Env, jobject Object, jlong DecoderHandle)
{
   List_t             *ListEntry;
   DecoderData_t      *DecoderData;
   DecodedFrameData_t *DecodedFrameData;

   if((DecoderData = (DecoderData_t *)DecoderHandle) != NULL)
   {
      if(DecoderData->Handle)
      {
         SBC_Cleanup_Decoder(DecoderData->Handle);
         DecoderData->Handle = 0;
      }

      BTPS_WaitMutex(DecoderData->DecodedFrameListLock, BTPS_INFINITE_WAIT);

      while(!ListIsEmpty(&(DecoderData->DecodedFrameList)))
      {
         ListEntry = ListRemove(ListNext(&(DecoderData->DecodedFrameList)));

         if((DecodedFrameData = ListGetEntry(ListEntry, DecodedFrameData_t, FrameList)) != NULL)
            FreeDecodedFrameData(DecodedFrameData);
      }

      DecoderData->DecodedFrameListLength = 0;

      BTPS_ReleaseMutex(DecoderData->DecodedFrameListLock);
      BTPS_CloseMutex(DecoderData->DecodedFrameListLock);

      BTPS_FreeMemory(DecoderData);
   }
}

/*
 * Class:     com_stonestreetone_bluetopiapm_SBC
 * Method:    decodeDataNative
 * Signature: (J[B)I
 */
static jint DecodeDataNative(JNIEnv *Env, jobject Object, jlong DecoderHandle, jbyteArray Data)
{
   int                         Result;
   jbyte                      *EncodedDataBuffer;
   jsize                       EncodedDataLength;
   Byte_t                     *EncodedDataFrame;
   List_t                      CompletedFramesList;
   unsigned int                CompletedFramesCount;
   unsigned int                UnusedDataLength;
   DecoderData_t              *DecoderData;
   SBC_Decode_Data_t           DecoderOutput;
   DecodedFrameData_t         *DecodedFrameData;

   CompletedFramesCount = 0;
   ListInitialize(&CompletedFramesList);

   if(((DecoderData = (DecoderData_t *)DecoderHandle) != NULL) && (Data))
   {
      EncodedDataLength = Env->GetArrayLength(Data);

      if(EncodedDataLength > 0)
      {
         if((EncodedDataBuffer = Env->GetByteArrayElements(Data, NULL)) != NULL)
         {
            Result           = SBC_PROCESSING_COMPLETE;
            EncodedDataFrame = (Byte_t *)EncodedDataBuffer;

            while((Result == SBC_PROCESSING_COMPLETE) && (EncodedDataLength > 0))
            {
               // TODO When decode doesn't complete, store allocated buffer. Then, check here if we already have space for decoding before allocating new.

               if((DecodedFrameData = AllocateDecodedFrameData()) != NULL)
               {
                  DecoderOutput.ChannelDataSize        = DecodedFrameData->BufferSize;
                  DecoderOutput.LeftChannelDataPtr     = &(DecodedFrameData->Buffer[0]);
                  DecoderOutput.LeftChannelDataLength  = 0;
                  DecoderOutput.RightChannelDataPtr    = &(DecodedFrameData->Buffer[1]);
                  DecoderOutput.RightChannelDataLength = 0;

                  Result = SBC_Decode_Data(DecoderData->Handle, EncodedDataLength, EncodedDataFrame, &(DecodedFrameData->Config), &DecoderOutput, &UnusedDataLength);

                  if((Result == SBC_PROCESSING_COMPLETE) || (Result == SBC_PROCESSING_DATA))
                  {
                     /* No error occurred, so advance to the next       */
                     /* encoded SBC frame in the input buffer.          */
                     EncodedDataFrame  += (EncodedDataLength - UnusedDataLength);
                     EncodedDataLength -= (EncodedDataLength - UnusedDataLength);
                  }

                  /* Check whether the decode process produced a        */
                  /* complete frame.                                    */
                  if(Result == SBC_PROCESSING_COMPLETE)
                  {
                     /* We have a full decoded frame. Store the decoded */
                     /* frame size and add it to the list of completed  */
                     /* frames.                                         */
                     DecodedFrameData->BufferSize = DecoderOutput.LeftChannelDataLength + DecoderOutput.RightChannelDataLength;

                     if(ListInsertBefore(&(CompletedFramesList), &(DecodedFrameData->FrameList)))
                        CompletedFramesCount++;
                     else
                     {
                        /* The list insertion action should never fail. */
                        Result = SBC_ERROR_UNKNOWN_ERROR;
                        FreeDecodedFrameData(DecodedFrameData);
                     }
                  }
                  else
                  {
                     /* The decode did not produce a complete frame     */
                     /* (either an error occurred or the decoder needs  */
                     /* additional data. In either case, the output     */
                     /* buffer was not used, so free it.                */
                     FreeDecodedFrameData(DecodedFrameData);
                  }
               }
               else
               {
                  /* Allocation of the decoder output list entry failed.*/
                  Result = SBC_ERROR_INSUFFICIENT_RESOURCES;
               }
            }

            Env->ReleaseByteArrayElements(Data, EncodedDataBuffer, JNI_ABORT);
         }
         else
            Result = SBC_ERROR_INVALID_PARAMETER;
      }
      else
         Result = SBC_ERROR_INVALID_PARAMETER;

      /* Check whether we were able to complete any frames. If we       */
      /* completed at least one frame during this pass and no errors    */
      /* occurred, load the frames into the decoded frame queue and     */
      /* return the number of completed frames.                         */
      if((Result == SBC_PROCESSING_COMPLETE) || (Result == SBC_PROCESSING_DATA))
      {
         BTPS_WaitMutex(DecoderData->DecodedFrameListLock, BTPS_INFINITE_WAIT);

         while(!ListIsEmpty(&CompletedFramesList))
         {
            DecodedFrameData = ListGetEntry(ListRemove(ListNext(&CompletedFramesList)), DecodedFrameData_t, DecodedFrameData_t::FrameList);
            ListInsertBefore(&(DecoderData->DecodedFrameList), &(DecodedFrameData->FrameList));
            DecoderData->DecodedFrameListLength++;
         }

         BTPS_ReleaseMutex(DecoderData->DecodedFrameListLock);

         Result = CompletedFramesCount;
      }
      else
      {
         /* An error occurred. Throw away all the completed frames and     */
         /* return the error code.                                         */
         while(!ListIsEmpty(&CompletedFramesList))
         {
            DecodedFrameData = ListGetEntry(ListRemove(ListNext(&CompletedFramesList)), DecodedFrameData_t, DecodedFrameData_t::FrameList);
            FreeDecodedFrameData(DecodedFrameData);
         }
      }
   }
   else
      Result = SBC_ERROR_INVALID_PARAMETER;


   return Result;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_SBC
 * Method:    availableAudioFramesNative
 * Signature: (J)I
 */
static jint AvailableAudioFramesNative(JNIEnv *Env, jobject Object, jlong DecoderHandle)
{
   jint           FrameCount;
   DecoderData_t *DecoderData;

   if((DecoderData = (DecoderData_t *)DecoderHandle) != NULL)
   {
      BTPS_WaitMutex(DecoderData->DecodedFrameListLock, BTPS_INFINITE_WAIT);

      FrameCount = DecoderData->DecodedFrameListLength;

      BTPS_ReleaseMutex(DecoderData->DecodedFrameListLock);
   }
   else
      FrameCount = 0;

   return FrameCount;
}

/*
 * Class:     com_stonestreetone_bluetopiapm_SBC
 * Method:    getAudioFrameNative
 * Signature: (J[B[I)I
 */
static jint GetAudioFrameNative(JNIEnv *Env, jobject Object, jlong DecoderHandle, jbyteArray Data, jintArray Config)
{
   jint                Result;
   jint               *ConfigArray;
   jbyte              *OutputBuffer;
   jsize               OutputBufferSize;
   List_t             *DecodedFrameListEntry;
   DecoderData_t      *DecoderData;
   DecodedFrameData_t *DecodedFrame;

   if(((DecoderData = (DecoderData_t *)DecoderHandle) != NULL) && (Data))
   {
      BTPS_WaitMutex(DecoderData->DecodedFrameListLock, BTPS_INFINITE_WAIT);

      /* Get the next decoded frame, remove it from the queue, and      */
      /* translate the queue entry back to the frame structure.         */
      DecodedFrameListEntry = ListNext(&(DecoderData->DecodedFrameList));
      DecodedFrame          = ListGetEntry(DecodedFrameListEntry, DecodedFrameData_t, DecodedFrameData_t::FrameList);

      if(DecodedFrame)
      {
         /* We were able to grab a frame. First make sure the output    */
         /* buffer is big enough to hold the data.                      */
         OutputBufferSize = Env->GetArrayLength(Data);

         if(((unsigned int)OutputBufferSize) >= (DecodedFrame->BufferSize * sizeof(Word_t)))
         {
            /* The output buffer is big enough, so now remove the frame */
            /* from the queue, decrement the queue length and release   */
            /* the mutex.                                               */
            ListRemove(DecodedFrameListEntry);
            DecoderData->DecodedFrameListLength--;
            BTPS_ReleaseMutex(DecoderData->DecodedFrameListLock);

            Result = DecodedFrame->BufferSize * sizeof(Word_t);
            Env->SetByteArrayRegion(Data, 0, Result, (jbyte *)(DecodedFrame->Buffer));

            /* If the caller requested the SBC frame configuration, copy*/
            /* the configuration data now.                              */
            if((Config) && (Env->GetArrayLength(Config) == 8))
            {
               if((ConfigArray = (jint *)(Env->GetPrimitiveArrayCritical(Config, NULL))) != NULL)
               {
                  if(GetDecoderConfiguration(&(DecodedFrame->Config), ConfigArray) == FALSE)
                  {
                     /* Something is wrong in the configuration         */
                     /* structure. Return all zeros.                    */
                     BTPS_MemInitialize(ConfigArray, 0, 8);
                  }

                  Env->ReleasePrimitiveArrayCritical(Config, ConfigArray, 0);
               }
            }

            /* We are now finished with this frame, so free it.         */
            FreeDecodedFrameData(DecodedFrame);
         }
         else
         {
            /* User-provided buffer is too small. Return the required   */
            /* size * -1. Note that the decoded frame buffer is of type */
            /* Word_t while the Java buffer is in bytes.                */
            Result = (-1 * (DecodedFrame->BufferSize * sizeof(Word_t)));

            /* Release the queue mutex now that we are finished         */
            /* accessing the decoded frame structure.                   */
            BTPS_ReleaseMutex(DecoderData->DecodedFrameListLock);
         }
      }
      else
      {
         /* We were unable to pull an entry from the decoded frame      */
         /* queue. Most likely, the queue was empty. Release the lock   */
         /* and tell the user there is no data available.               */
         BTPS_ReleaseMutex(DecoderData->DecodedFrameListLock);
         Result = 0;
      }
   }
   else
   {
      /* Either our DecoderData was never initialized correctly or the  */
      /* caller failed to provide an output buffer. Either way, tell the*/
      /* caller that we are returning no data.                          */
      Result = 0;
   }

   return Result;
}

static JNINativeMethod Methods[] = {
   {"calculateEncoderBitPoolSizeNative", "(IIIIII)I",  (void*) CalculateEncoderBitPoolSizeNative},
   {"calculateEncoderFrameLengthNative", "(IIIIII)I",  (void*) CalculateEncoderFrameLengthNative},
   {"calculateEncoderBitRateNative",     "(IIIIII)I",  (void*) CalculateEncoderBitRateNative},
   {"calculateDecoderFrameSizeNative",   "([B)I",      (void*) CalculateDecoderFrameSizeNative},
   {"initializeEncoderNative",           "(IIIIII)J",  (void*) InitializeEncoderNative},
   {"cleanupEncoderNative",              "(J)V",       (void*) CleanupEncoderNative},
   {"changeEncoderConfigurationNative",  "(JIIIIII)I", (void*) ChangeEncoderConfigurationNative},
   {"encodeDataNative",                  "(J[B[BI)I",  (void*) EncodeDataNative},
   {"initializeDecoderNative",           "()J",        (void*) InitializeDecoderNative},
   {"cleanupDecoderNative",              "(J)V",       (void*) CleanupDecoderNative},
   {"decodeDataNative",                  "(J[B)I",     (void*) DecodeDataNative},
   {"availableAudioFramesNative",        "(J)I",       (void*) AvailableAudioFramesNative},
   {"getAudioFrameNative",               "(J[B[I)I",   (void*) GetAudioFrameNative}
};

int register_com_stonestreetone_bluetopiapm_SBC(JNIEnv *Env)
{
   PRINT_DEBUG("%s: Enter", __FUNCTION__);

   int Result;
   jclass Class;
   const char *ClassName = "com/stonestreetone/bluetopiapm/SBC";

   Result = -1;

   PRINT_DEBUG("Registering SBC native functions");

   if((Class = Env->FindClass(ClassName)) != 0)
   {
      Result = RegisterNativeFunctions(Env, ClassName, Methods, (sizeof(Methods) / sizeof(Methods[0])));
   }

   return Result;
}
