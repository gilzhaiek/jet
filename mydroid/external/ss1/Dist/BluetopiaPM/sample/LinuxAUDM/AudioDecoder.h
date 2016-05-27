/*****< audioencoder.h >*******************************************************/
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
/*   01/13/11  G. Hensley     Initial creation. (Based on LinuxDEVM)          */
/******************************************************************************/
#ifndef __AUDIODECODERH__
#define __AUDIODECODERH__

#define DECODER_CONFIG_FLAGS_ENABLE_LOGGING_SBC                         0x01
#define DECODER_CONFIG_FLAGS_ENABLE_LOGGING_WAV                         0x02

   /* The following defines the name of the files for logging wav, and  */
   /* sbc data.                                                         */
#define DECODER_WAV_FILE_NAME                                           "audio.wav"
#define DECODER_SBC_FILE_NAME                                           "audio.sbc"


   /* The following function initializes the audio decoder. The first   */
   /* parameter is an optional configuration flags. This function will  */
   /* return zero on success and negative on error.                     */
int InitializeAudioDecoder(unsigned int ConfigFlags);

   /* The following function is responsible for freeing all resources   */
   /* that were previously allocated for an audio decoder.              */
void CleanupAudioDecoder(void);

   /* The following function is used to process audio data. The first   */
   /* parameter is buffer to the raw SBC audio. The second parameter is */
   /* the length of the audio buffer. A negative value will be returned */
   /* on error                                                          */
int ProcessAudioData(void *Buffer, unsigned int Length);

#endif

