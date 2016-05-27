/*
 * amplifier.c
 *
 *  Created on: Sept 3, 2013
 *  Author: Li Chen
 */
#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <unistd.h>
#include <string.h>
#include <math.h>
#include "Tfa98xx.h"
#include "Tfa98xx_Registers.h"
#define I2S_CLOCK_MODULE

#ifdef I2S_CLOCK_MODULE
#include "i2s_clock.h"
#endif

#ifndef WIN32
#define Sleep(ms) usleep((ms)*1000)
#define _fileno fileno
#endif
/*
 * module globals for output control
 */

#define I2C_8BIT_ADDRESS 0x68
#define LOCATION_FILES "/system/etc/amplifier/"

#define PATCH_FILENAME "TFA9887_N1D2_4_1_1.patch"

#define SPEAKER_FILENAME "Jet.speaker" 

#define PRESET_FILENAME "Jet.preset"
#define CONFIG_FILENAME "Jet.config"
#define EQ_FILENAME "Jet.eq"


#define print_if_false(e) if (!(e)) fprintf(stderr, "%s %d err!-----\n",__FUNCTION__,__LINE__)

static float tCoefFromSpeaker(Tfa98xx_SpeakerParameters_t speakerBytes)
{
	int iCoef;

	/* tCoef(A) is the last parameter of the speaker */
	iCoef = (speakerBytes[TFA98XX_SPEAKERPARAMETER_LENGTH-3]<<16) + (speakerBytes[TFA98XX_SPEAKERPARAMETER_LENGTH-2]<<8) + speakerBytes[TFA98XX_SPEAKERPARAMETER_LENGTH-1];

	return (float)iCoef/(1<<23);
}

static void tCoefToSpeaker(Tfa98xx_SpeakerParameters_t speakerBytes, float tCoef)
{
	int iCoef;

	iCoef =(int)(tCoef*(1<<23));

	speakerBytes[TFA98XX_SPEAKERPARAMETER_LENGTH-3] = (iCoef>>16)&0xFF;
	speakerBytes[TFA98XX_SPEAKERPARAMETER_LENGTH-2] = (iCoef>>8)&0xFF;
	speakerBytes[TFA98XX_SPEAKERPARAMETER_LENGTH-1] = (iCoef)&0xFF;
}

static char* stateFlagsStr(int stateFlags)
{
	static char flags[10];

	flags[0] = (stateFlags & (0x1<<Tfa98xx_SpeakerBoost_Activity)) ? 'A':'a';
	flags[1] = (stateFlags & (0x1<<Tfa98xx_SpeakerBoost_S_Ctrl)) ? 'S':'s';
	flags[2] = (stateFlags & (0x1<<Tfa98xx_SpeakerBoost_Muted)) ? 'M':'m';
	flags[3] = (stateFlags & (0x1<<Tfa98xx_SpeakerBoost_X_Ctrl)) ? 'X':'x';
	flags[4] = (stateFlags & (0x1<<Tfa98xx_SpeakerBoost_T_Ctrl)) ? 'T':'t';
	flags[5] = (stateFlags & (0x1<<Tfa98xx_SpeakerBoost_NewModel)) ? 'L':'l';
	flags[6] = (stateFlags & (0x1<<Tfa98xx_SpeakerBoost_VolumeRdy)) ? 'V':'v';
	flags[7] = (stateFlags & (0x1<<Tfa98xx_SpeakerBoost_Damaged)) ? 'D':'d';
	flags[8] = (stateFlags & (0x1<<Tfa98xx_SpeakerBoost_SignalClipping)) ? 'C':'c';

	flags[9] = 0;
	return flags;
}

static void dump_state_info(Tfa98xx_StateInfo_t* pState)
{
  printf("state: flags %s, agcGain %2.1f\tlimGain %2.1f\tsMax %2.1f\tT %d\tX1 %2.1f\tX2 %2.1f\tRe %2.2f\tshortOnMips %d\n",
				stateFlagsStr(pState->statusFlag),
				pState->agcGain,
				pState->limGain,
				pState->sMax,
				pState->T,
				pState->X1,
				pState->X2,
				pState->Re,
				pState->shortOnMips);
}

/* load a DSP ROM code patch from file */
static void dspPatch(Tfa98xx_handle_t handle, const char* fileName)
{
	int ret;
	int fileSize;
	unsigned char* buffer;
	FILE* f;
	struct stat st;
	Tfa98xx_Error_t err;

	printf("Loading patch file %s\n", fileName);

	f=fopen(fileName, "rb");
	print_if_false(f!=NULL);
	ret = fstat(_fileno(f), &st);
	print_if_false(ret == 0);
	fileSize = st.st_size;
	buffer = malloc(fileSize);
	print_if_false(buffer != NULL);
	ret = fread(buffer, 1, fileSize, f);
	print_if_false(ret == fileSize);
	err = Tfa98xx_DspPatch(handle, fileSize, buffer);
	print_if_false(err == Tfa98xx_Error_Ok);
	fclose(f);
	free(buffer);
}

static void coldStartup(Tfa98xx_handle_t handle)
{
	Tfa98xx_Error_t err;
	unsigned short status;
	int ready=0;
	/* load the optimal TFA9887 in HW settings */
	err = Tfa98xx_Init(handle);
	print_if_false(err == Tfa98xx_Error_Ok);

	err = Tfa98xx_SetSampleRate(handle, 48000);
	print_if_false(err == Tfa98xx_Error_Ok);
	


	err = Tfa98xx_Powerdown(handle, 0);//enable power
	print_if_false(err == Tfa98xx_Error_Ok);

	printf("Waiting for IC to start up\n");
	err = Tfa98xx_ReadRegister16(handle, TFA98XX_STATUSREG, &status);
	print_if_false(err == Tfa98xx_Error_Ok);
	while ( (status & TFA98XX_STATUSREG_AREFS_MSK) == 0)
	{
		/* not ok yet */
		err = Tfa98xx_ReadRegister16(handle, TFA98XX_STATUSREG, &status);
		print_if_false(err == Tfa98xx_Error_Ok);
		printf("Waiting for PLL lock\n");
	}

	/*  powered on
	 *    - now it is allowed to access DSP specifics
	 *    - stall DSP by setting reset
	 * */
	err = Tfa98xx_DspReset(handle, 1);
	print_if_false(err == Tfa98xx_Error_Ok);

	/*  wait until the DSP subsystem hardware is ready
	 *    note that the DSP CPU is not running yet (RST=1)
	 * */
	while ( ready == 0)
	{
		/* are we ready? */
		err = Tfa98xx_DspSystemStable(handle, &ready);
		print_if_false(err == Tfa98xx_Error_Ok);
	}
	/* Load cold-boot patch for the first time to force cold start-up.
	*  use the patchload only to write the internal register
	* */
#if 1
	dspPatch(handle, LOCATION_FILES "coldboot.patch");
#else
	/* cold patch*/
	Tfa98xx_DspPatch(handle, sizeof(coldboot) ,coldboot);
#endif
	err = Tfa98xx_ReadRegister16(handle, TFA98XX_STATUSREG, &status);
	print_if_false(err==Tfa98xx_Error_Ok);
	print_if_false(status & TFA98XX_STATUSREG_ACS_MSK);  /* ensure cold booted */

	print_if_false(status & TFA98XX_STATUSREG_ACS_MSK);  /* ensure cold booted */

	/* cold boot, need to load all parameters and patches */
	/* patch the ROM code */
	dspPatch(handle, LOCATION_FILES PATCH_FILENAME);
	
}

static void loadSpeakerFile(const char* fileName, Tfa98xx_SpeakerParameters_t speakerBytes)
{
int ret;
	FILE* f;

	printf("using speaker %s\n", fileName);

	f=fopen(fileName, "rb");
	print_if_false(f != NULL);

	ret = fread(speakerBytes, 1, sizeof(Tfa98xx_SpeakerParameters_t), f);

	print_if_false(ret == sizeof(Tfa98xx_SpeakerParameters_t));
	fclose(f);
}

static int checkMTPEX(Tfa98xx_handle_t handle)
{
	unsigned short mtp;
	Tfa98xx_Error_t err;
	err = Tfa98xx_ReadRegister16(handle, TFA98XX_MTP, &mtp);
	print_if_false(err == Tfa98xx_Error_Ok);
  
	if ( mtp & (1<<1))	/* check MTP bit1 (MTPEX) */
		return 1;					/* MTPEX is 1, calibration is done */
	else 
		return 0;					/* MTPEX is 0, calibration is not done yet */
}

static void setConfig(Tfa98xx_handle_t handle, const char* fileName)
{
	Tfa98xx_Error_t err;
	Tfa98xx_Config_t config;
	int ret;
	FILE* f;

	printf("using config %s\n", fileName);

	/* read from file and check the size */
	f=fopen(fileName, "rb");
	print_if_false(f!=NULL);
	ret = fread(config, 1, sizeof(config), f);

	fclose(f);

	/* now send bytes to the DSP */
	err = Tfa98xx_DspWriteConfig(handle, sizeof(Tfa98xx_Config_t), config);
	print_if_false(err == Tfa98xx_Error_Ok);
}

/* load a preset from a file, as generated by the GUI, can be done at runtime */
static void setPreset(Tfa98xx_handle_t handle, const char* fileName)
{
	int ret;
	int presetSize;
	unsigned char* buffer;
	FILE* f;
	struct stat st;
	Tfa98xx_Error_t err;

	printf("using preset %s\n", fileName);

	f=fopen(fileName, "rb");
	print_if_false(f!=NULL);
	ret = fstat(_fileno(f), &st);
	print_if_false(ret == 0);
	presetSize = st.st_size;
	print_if_false(presetSize == TFA98XX_PRESET_LENGTH);

	buffer = (unsigned char*)malloc(presetSize);
	print_if_false(buffer != NULL);
	ret = fread(buffer, 1, presetSize, f);
	print_if_false(ret == presetSize);
	err = Tfa98xx_DspWritePreset(handle, TFA98XX_PRESET_LENGTH, buffer);
	print_if_false(err == Tfa98xx_Error_Ok);
	fclose(f);
	free(buffer);
}

/* load a set of EQ settings from a file, as generated by the GUI, can be done at runtime */
static void setEQ(Tfa98xx_handle_t handle, const char* fileName)
{
	int ret;
	FILE* f;
	Tfa98xx_Error_t err;
	int ind; /* biquad index */
	float b0, b1, b2, a1, a2; /* the coefficients */
	int line = 1;
	char buffer[256];

	printf("using EQ %s\n", fileName);

	f=fopen(fileName, "rb");
	print_if_false(f!=NULL);

	while (!feof(f))
	{
		if (NULL == fgets(buffer, sizeof(buffer)-1, f) )
		{
			break;
		}
		ret = sscanf(buffer, "%d %f %f %f %f %f", &ind, &b0, &b1, &b2, &a1, &a2);
		if (ret == 6)
		{
			if ((b0 != 1) || (b1 != 0) || (b2 != 0) || (a1 != 0) || (a2 != 0)) {
				err = Tfa98xx_DspBiquad_SetCoeff(handle, ind, b0, b1, b2, a1, a2);
				print_if_false(err == Tfa98xx_Error_Ok);
				printf("Loaded biquad %d\n", ind);
	  		} else {
				err = Tfa98xx_DspBiquad_Disable(handle, ind);
						print_if_false(err == Tfa98xx_Error_Ok);
						printf("Disabled biquad %d\n", ind);
			}
		}
		else {
			//printf("error parsing file, line %d\n", line);
			break;
		}
		line++;
	}
	fclose(f);
}

static void load_all_settings(Tfa98xx_handle_t handle, Tfa98xx_SpeakerParameters_t speakerBytes, const char* configFile, const char* presetFile, const char* eqFile)
{
	Tfa98xx_Error_t err;

	/* load fullmodel */
	err = Tfa98xx_DspWriteSpeakerParameters(handle, sizeof(Tfa98xx_SpeakerParameters_t), speakerBytes);
	print_if_false(err == Tfa98xx_Error_Ok);

	/* load the settings */
	setConfig(handle, configFile);
	/* load a preset */
	setPreset(handle, presetFile);
	/* load an EQ file */
	setEQ(handle, eqFile);
}

static void waitCalibration(Tfa98xx_handle_t handle, int *calibrateDone)
{
	Tfa98xx_Error_t err;
	int tries = 0;
	unsigned short mtp;
#define WAIT_TRIES 1000

	err = Tfa98xx_ReadRegister16(handle, TFA98XX_MTP, &mtp);

	/* in case of calibrate once wait for MTPEX */
	if ( mtp & TFA98XX_MTP_MTPOTC) {
		while ( (*calibrateDone == 0) && (tries < WAIT_TRIES))
		{   // TODO optimise with wait estimation
			err = Tfa98xx_ReadRegister16(handle, TFA98XX_MTP, &mtp);
			*calibrateDone = ( mtp & TFA98XX_MTP_MTPEX);    /* check MTP bit1 (MTPEX) */
			tries++;
		}
	} else /* poll xmem for calibrate always */
	{
		while ((*calibrateDone == 0) && (tries<WAIT_TRIES) )
		{   // TODO optimise with wait estimation
			err = Tfa98xx_DspReadMem(handle, 231, 1, calibrateDone);
			tries++;
		}
		if(tries==WAIT_TRIES)
			printf("calibrateDone 231 timedout\n");
	}

}

Tfa98xx_Error_t	calculateSpeakertCoefA(
										Tfa98xx_handle_t handle,
										Tfa98xx_SpeakerParameters_t loadedSpeaker)
{
  Tfa98xx_Error_t err;
	float re25, tCoefA, tCoef;
	int Tcal; /* temperature at which the calibration happened */
	int T0;
	int calibrateDone = 0;

	err = Tfa98xx_DspGetCalibrationImpedance(handle, &re25);
	print_if_false(err == Tfa98xx_Error_Ok);
	print_if_false(fabs(re25) < 0.1); /* no calibration done yet */

	tCoef = tCoefFromSpeaker(loadedSpeaker);

	/* use dummy tCoefA, also eases the calculations, because tCoefB=re25 */
	tCoefToSpeaker(loadedSpeaker, 0.0f); 

	load_all_settings(handle, loadedSpeaker, LOCATION_FILES CONFIG_FILENAME, LOCATION_FILES PRESET_FILENAME, LOCATION_FILES EQ_FILENAME);


	/* start calibration and wait for result */
	err = Tfa98xx_SetConfigured(handle);
	print_if_false(err == Tfa98xx_Error_Ok);

	waitCalibration(handle, &calibrateDone);
	if (calibrateDone)
	{
	  Tfa98xx_DspGetCalibrationImpedance(handle,&re25);
	}
	else
	{
	  re25 = 0;
	}
	err = Tfa98xx_DspReadMem(handle, 232, 1, &Tcal);
	print_if_false(err == Tfa98xx_Error_Ok);
	printf("Resistance of speaker is %2.2f ohm @ %d degrees\n", re25, Tcal);

	/* calculate the tCoefA */
	T0 = 25; /* definition of temperature for Re0 */
	tCoefA = tCoef * re25 / (tCoef * (Tcal - T0)+1); /* TODO: need Rapp influence */
	printf("Calculated tCoefA %1.5f\n", tCoefA);

	/* update the speaker model */
	tCoefToSpeaker(loadedSpeaker, tCoefA);
	/* !!! The value is only written to the speaker file loadspeaker located in the memory of 
	* the host and not in the physical file itself.
	* The physical file will always contain tCoef. The host needs to save tCoefA in this "loadedSpeaker" as it is needed 
	* after the next cold boot to write the tCoefA value into MTP !!! */
	return err;
}

static void resetMtpEx(Tfa98xx_handle_t handle)
{
	Tfa98xx_Error_t err;
	unsigned short mtp;
	unsigned short status;

	/* reset MTPEX bit because calibration happened with wrong tCoefA */
	err = Tfa98xx_ReadRegister16(handle, TFA98XX_MTP, &mtp);
	print_if_false(err == Tfa98xx_Error_Ok);
	/* all settings loaded, signal the DSP to start calibration, only needed once after cold boot */

	/* reset MTPEX bit if needed */
	if ( (mtp & TFA98XX_MTP_MTPOTC) && (mtp & TFA98XX_MTP_MTPEX)) 
	{
		err = Tfa98xx_WriteRegister16(handle, 0x0B, 0x5A); /* unlock key2 */
		print_if_false(err == Tfa98xx_Error_Ok);

		err = Tfa98xx_WriteRegister16(handle, TFA98XX_MTP, 1); /* MTPOTC=1, MTPEX=0 */
		print_if_false(err == Tfa98xx_Error_Ok);
		err = Tfa98xx_WriteRegister16(handle, 0x62, 1<<11); /* CIMTP=1 */
		print_if_false(err == Tfa98xx_Error_Ok);
	}

	do
	{
		Sleep(10);
		err = Tfa98xx_ReadRegister16(handle, TFA98XX_STATUSREG, &status);
		print_if_false(err == Tfa98xx_Error_Ok);
		
	} while ( (status & TFA98XX_STATUSREG_MTPB_MSK) == TFA98XX_STATUSREG_MTPB_MSK);
	print_if_false( (status & TFA98XX_STATUSREG_MTPB_MSK) == 0);
}

static void muteAmplifier(Tfa98xx_handle_t handle)
{
	Tfa98xx_Error_t err;
	unsigned short status;

	/* signal the TFA98xx to mute plop free and turn off the amplifier */
	err = Tfa98xx_SetMute(handle, Tfa98xx_Mute_Amplifier);
	print_if_false(err == Tfa98xx_Error_Ok);

	/* now wait for the amplifier to turn off */
	err = Tfa98xx_ReadRegister16(handle, TFA98XX_STATUSREG, &status);
	print_if_false(err == Tfa98xx_Error_Ok);
	while ( (status & TFA98XX_STATUSREG_SWS_MSK) == TFA98XX_STATUSREG_SWS_MSK)
	{
		err = Tfa98xx_ReadRegister16(handle, TFA98XX_STATUSREG, &status);
		print_if_false(err == Tfa98xx_Error_Ok);
	}
}
/*
 * @param otcOn: =1: set One time calibration; =0: always need calibration
*/
static void setOtc(Tfa98xx_handle_t handle, int otcOn)
{
	Tfa98xx_Error_t err;
	unsigned short mtp;
	unsigned short status;
	int mtpChanged = 0;

	err = Tfa98xx_ReadRegister16(handle, TFA98XX_MTP, &mtp);
	print_if_false(err == Tfa98xx_Error_Ok);
	
	print_if_false((otcOn == 0) || (otcOn == 1) );

	/* set reset MTPEX bit if needed */
	if ( (mtp & TFA98XX_MTP_MTPOTC) != otcOn) 
	{
		/* need to change the OTC bit, set MTPEX=0 in any case */
		err = Tfa98xx_WriteRegister16(handle, 0x0B, 0x5A); /* unlock key2 */
		print_if_false(err == Tfa98xx_Error_Ok);

		err = Tfa98xx_WriteRegister16(handle, TFA98XX_MTP, (unsigned short)otcOn); /* MTPOTC=otcOn, MTPEX=0 */
		print_if_false(err == Tfa98xx_Error_Ok);
		err = Tfa98xx_WriteRegister16(handle, 0x62, 1<<11); /* CIMTP=1 */
		print_if_false(err == Tfa98xx_Error_Ok);
		
		mtpChanged =1;
		
	}
	//Sleep(13*16); /* need to wait until all parameters are copied into MTP */
	do
	{
		Sleep(10);
		err = Tfa98xx_ReadRegister16(handle, TFA98XX_STATUSREG, &status);
		print_if_false(err == Tfa98xx_Error_Ok);
		
	} while ( (status & TFA98XX_STATUSREG_MTPB_MSK) == TFA98XX_STATUSREG_MTPB_MSK);
	print_if_false( (status & TFA98XX_STATUSREG_MTPB_MSK) == 0);
}

static int dspSupporttCoef(Tfa98xx_handle_t handle)
{
	Tfa98xx_Error_t err;
	int bSupporttCoef;

	err = Tfa98xx_DspSupporttCoef(handle, &bSupporttCoef);
	print_if_false(err == Tfa98xx_Error_Ok);
	fprintf(stdout, "bSupporttCoef=%d\n",bSupporttCoef);
	return bSupporttCoef;
}

static void statusCheck(Tfa98xx_handle_t handle)
{
	Tfa98xx_Error_t err;
	unsigned short status;

	/* Check status from register 0*/
	err = Tfa98xx_ReadRegister16(handle, TFA98XX_STATUSREG, &status);
	if (status & TFA98XX_STATUSREG_WDS_MSK)
	{
	  printf("DSP watchDog triggerd");
	  return;
	}
}

// status register errors to check for not 1
#define TFA98XX_STATUSREG_ERROR1_SET_MSK (  \
		TFA98XX_STATUSREG_OCDS  )
#define TFA98XX_STATUSREG_ERROR2_SET_MSK (  \
		TFA98XX_STATUSREG_ACS |   \
		TFA98XX_STATUSREG_WDS )

static int tfaRun_CheckEvents(unsigned short regval) {
	int severity=0;

	//see if following alarms are set
	if ( regval & TFA98XX_STATUSREG_ERROR1_SET_MSK ) //	
		severity = 1;
	// next will overwrite if set
	if ( regval & TFA98XX_STATUSREG_ERROR2_SET_MSK )
		severity = 2;
	// check error conditions

	return severity;
}

Tfa98xx_Error_t tfaRun_PowerCycleCF(Tfa98xx_handle_t handle){
	Tfa98xx_Error_t err;

	err = Tfa98xx_Powerdown(handle, 1);
	print_if_false(err == Tfa98xx_Error_Ok);
	err = Tfa98xx_Powerdown(handle, 0);
	print_if_false(err == Tfa98xx_Error_Ok);

	return err;
}

void getDspInfo(Tfa98xx_handle_t handle)
{
	int i,err;
	unsigned short status;
	Tfa98xx_StateInfo_t stateInfo;
	for (i=0; i<50; ++i)
	{
		err = Tfa98xx_DspGetStateInfo(handle, &stateInfo);
		print_if_false(err == Tfa98xx_Error_Ok);
		dump_state_info(&stateInfo);
		Sleep(1000);
		err = Tfa98xx_ReadRegister16(handle, TFA98XX_STATUSREG, &status);
		switch(tfaRun_CheckEvents(status)) 
		{//idx=0
			case 1: 
				printf(">>>>>>>>>>repower CF\n");
				tfaRun_PowerCycleCF(handle);
			break;
			case 2:
				printf(">>>>>>>>>>full reset required!!\n");
			break;
			default:
			break;
		}

	}
}

/*
 * main
 */
int main(int argc, char *argv[])
{
	Tfa98xx_Error_t err;
	Tfa98xx_handle_t handle;
#ifdef I2S_CLOCK_MODULE
	pthread_t thread_id;
#endif
	Tfa98xx_SpeakerParameters_t loadedSpeaker;
	float re25, tCoefA;
	int calibrateDone = 0;
	int otcOn=1;
	int Dsp_Info=0;

	err=Tfa98xx_Open(I2C_8BIT_ADDRESS, &handle );
	print_if_false(err == Tfa98xx_Error_Ok);
	//Parse command
	if(argc>1){
		if(*argv[1]=='v'){
			float voldB=0;
			if(argc>2){
				voldB=(float)atoi(argv[2]);
			}
			fprintf(stdout, "Setting volume to %3.1f dB\n", voldB);
			err = Tfa98xx_SetVolume(handle, voldB);
			print_if_false(err == Tfa98xx_Error_Ok);
			goto exit;
		}
		else if (*argv[1]=='i')
		{
			fprintf(stdout,"Get DSP info\n");
			Dsp_Info=1;
			goto exit;
		}
		else if (*argv[1]=='f')
		{
			fprintf(stdout,"change to always calibrate\n");
			otcOn=0;
		}
	}

	/* ensure no audio during special calibration */
	err = Tfa98xx_SetMute(handle, Tfa98xx_Mute_Digital);
	print_if_false(err == Tfa98xx_Error_Ok);

	err = Tfa98xx_SelectChannel(handle, Tfa98xx_Channel_L);
	print_if_false(err== Tfa98xx_Error_Ok);
	err=Tfa98xx_SelectAmplifierInput(handle,Tfa98xx_AmpInputSel_DSP);
	print_if_false(err== Tfa98xx_Error_Ok);
	
	/* start at 0 dB */
	err = Tfa98xx_SetVolume(handle, 0.0);
	print_if_false(err== Tfa98xx_Error_Ok);
	/*Need enable I2S clock*/
#ifdef I2S_CLOCK_MODULE
	err=pthread_create(&thread_id, NULL, &i2sClockOn, NULL);
	print_if_false(err == 0);
#endif
	//Cold start-------------------
	coldStartup(handle);

	//Calibration
	setOtc(handle, otcOn);

	loadSpeakerFile(LOCATION_FILES SPEAKER_FILENAME, loadedSpeaker);
	/* Check if MTPEX bit is set for calibration once mode */
	if(checkMTPEX(handle) == 0)
	{
		printf("DSP not yet calibrated. Calibration will start.\n");
		/* When using tCoefA patch:                                  */
		/*   tCoefA will be saved into MTP automatically by CoolFlux */
		tCoefA = tCoefFromSpeaker(loadedSpeaker);

		/*Remark: only for Tfa9887 the tCoef does need two steps calibration to calculate tCoefA*/
		if (!dspSupporttCoef(handle))
		{
			/* ensure tCoefA */
			printf("tCoefA %1.4f.\n", tCoefA);
			//print_if_false(tCoefA > 0.005f);
			err = calculateSpeakertCoefA(handle, loadedSpeaker);
			print_if_false(err == Tfa98xx_Error_Ok);

			/* if we were in one-time calibration (OTC) mode, clear the calibration results 
			from MTP so next time 2nd calibartion step can start. */
			resetMtpEx(handle);

			/* force recalibration now with correct tCoefA */
			muteAmplifier(handle); /* clean shutdown to avoid plop */
			coldStartup(handle);
		}
		else
		{
			/* ensure a real tCoef, not a tCoefA */
			print_if_false(tCoefA < 0.005f);
		}
	}
	else
	{
		printf("DSP already calibrated. Calibration skipped and previous calibration results loaded from MTP.\n");
	}
	/* Load all settings (for TFA9887: this is the 2nd time. Now speaker model contains tCoefA. */
	load_all_settings(handle, loadedSpeaker, LOCATION_FILES CONFIG_FILENAME, LOCATION_FILES PRESET_FILENAME, LOCATION_FILES EQ_FILENAME);

	/* do calibration (again), if needed */
	err = Tfa98xx_SetConfigured(handle);
	print_if_false(err == Tfa98xx_Error_Ok);

	/* Wait until the calibration is done. 
	* The MTPEX bit would be set and remain as 1 if MTPOTC is set to 1 */
	waitCalibration(handle, &calibrateDone);
	if (calibrateDone)
	{
	  Tfa98xx_DspGetCalibrationImpedance(handle,&re25);
	}
	else
	{
	  re25 = 0;
	}
	printf("Calibration value is %2.2f ohm\n", re25);

	/*Checking the current status for DSP status and DCPVP */
	statusCheck(handle);

#ifdef I2S_CLOCK_MODULE
	err=pthread_kill(thread_id, SIGUSR1);
	print_if_false(err == 0);
	pthread_join(thread_id, NULL);
	printf("Kill child thread\n");
#endif

	err = Tfa98xx_SetMute(handle, Tfa98xx_Mute_Off);
	print_if_false(err == Tfa98xx_Error_Ok);

#ifdef I2S_CLOCK_MODULE
	//pthread_join(thread_id, NULL);
#endif
exit:
	if(Dsp_Info)
		getDspInfo(handle);
	err = Tfa98xx_Close(handle);
	print_if_false(err == Tfa98xx_Error_Ok);
	return err;
}
