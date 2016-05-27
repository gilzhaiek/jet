/*
 * climax.c
 *
 *  Created on: Apr 3, 2012
 *      Author: nlv02095
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#ifndef WIN32
#include <unistd.h>
#else
#include <windows.h>
#endif
#include <ctype.h>
#include <stdint.h>
#include "cmdline.h"
#include "climax.h"
#include "nxpTfa98xx.h"
#include "lxScribo.h"
#include "tfa98xxLiveData.h"
Tfa98xx_handle_t Handle;
#define I2C_8BIT_ADDRESS 0x68
int main(int argc, char *argv[])
{
	Tfa98xx_Error_t err;
	Tfa98xx_Open(I2C_8BIT_ADDRESS, &handle );
	err = Tfa98xx_Init(handle);
	PrintAssert(err);
	err = Tfa98xx_SetSampleRate(handle, TFA_PROFILE0_SAMPLERATE); //TODO which samplerate?
	PrintAssert(err);
//////////////////////////////////////
    char *devicename, *xarg;
    int status, fd;
    Tfa98xx_handle_t handlesIn[] ={-1, -1};

    devicename = cliInit(argc, argv);
	xarg = argv[optind]; // this is the remaining argv

    if ( gCmdLine.slave_given )
        tfa98xxI2cSlave = gCmdLine.slave_arg;

    if ( cli_verbose )
        printf("devicename=%s, i2c=0x%02x\n" ,devicename, tfa98xxI2cSlave);

    fd = cliTargetDevice(devicename);
           
    tfa98xxLiveData_verbose = cli_verbose;
    status = cliCommands(fd, xarg, handlesIn);	// execute the commands

    if ( gCmdLine.record_given ) {  // TODO run in thread
        int loopcount=gCmdLine.count_arg;
        FILE *outfile;

        if ( gCmdLine.output_given) {
            outfile = fopen(gCmdLine.output_arg,"w");
        } else
            outfile = stdout;

        tfa98xxPrintRecordHeader(outfile, gCmdLine.record_arg);
        //
        if (  nxpTfa98xx_Error_Ok == nxpTfa98xxOpenLiveDataSlaves(handlesIn, tfa98xxI2cSlave, 1))
            do {
                tfa98xxPrintRecord(handlesIn, outfile, 0);
                loopcount = ( gCmdLine.count_arg == 0) ? 1 : loopcount-1 ;
#ifdef WIN32
				Sleep(gCmdLine.record_arg); // is msinterval
#else
                usleep(gCmdLine.record_arg*1000); // is msinterval
#endif
            } while (loopcount>0) ;

        if ( gCmdLine.output_given) {
            printf("written to %s\n", gCmdLine.output_arg);
            fclose(outfile);
        }
    }
    if (gCmdLine.recordStereo_given)
    {
       int loopcount=gCmdLine.count_arg;
        FILE *outfile;

        if ( gCmdLine.output_given) {
            outfile = fopen(gCmdLine.output_arg,"w");
        } else
            outfile = stdout;

        tfa98xxPrintRecordHeader(outfile, gCmdLine.recordStereo_arg);
        //
        if (  nxpTfa98xx_Error_Ok == nxpTfa98xxOpenLiveDataSlaves(handlesIn, tfa98xxI2cSlave, 2))
            do {
                tfa98xxPrintRecordStereo(handlesIn, outfile);
                loopcount = ( gCmdLine.count_arg == 0) ? 1 : loopcount-1 ;
#ifdef WIN32
				   Sleep(gCmdLine.recordStereo_arg); // is msinterval
#else
                usleep(gCmdLine.recordStereo_arg*1000); // is msinterval
#endif
            } while (loopcount>0) ;

        if ( gCmdLine.output_given) {
            printf("written to %s\n", gCmdLine.output_arg);
            fclose(outfile);
        }
    }
#ifndef WIN32
    if ( gCmdLine.server_given ) {
        printf("statusreg:0x%02x\n", tfa98xxReadRegister(0,handlesIn)); // read to ensure device is opened
        cliSocketServer(gCmdLine.server_arg); // note socket is ascii string
    }
    if ( gCmdLine.slave_given ) {
        printf("statusreg:0x%02x\n", tfa98xxReadRegister(0,handlesIn)); // read to ensure device is opened
        cliClientServer(gCmdLine.slave_arg); // note socket is ascii string
    }
#endif
    exit (status);

}

#ifndef WIN32
/*
 *
 */
int activeSocket;
void cliSocketServer(char *socket)
{
	 int length, i;
	 uint8_t cmd[2], buf[256], *ptr, *devname;

	activeSocket=lxScriboListenSocketInit(socket);

	if(activeSocket<0) {
		fprintf(stderr, "something wrong with socket %s\n", socket);
		exit(1);
	}

	while(1){
		length = read(activeSocket, buf, 256);
		if (socket_verbose & (length>0)) {
			printf("recv: ");
			for(i=0;i<length;i++)
			     printf("0x%02x ", buf[i]);
			printf("\n");
		}
		if (length>0)
		  CmdProcess(buf,  length);
		else {
			close(activeSocket);
			usleep(10000);
			activeSocket=lxScriboListenSocketInit(socket);
		}
	}

}

/*
 *
 */
int activeServer;
void cliClientServer(char *server)
{
	 int length, i;
	 uint8_t cmd[2], buf[256], *ptr, *devname;

	activeServer=lxScriboSocketInit(server);

	if(activeServer<0) {
		fprintf(stderr, "something wrong with client %s\n", server);
		exit(1);
	}

	while(1){
		length = read(activeSocket, buf, 256);
		if (socket_verbose & (length>0)) {
			printf("recv: ");
			for(i=0;i<length;i++)
			     printf("0x%02x ", buf[i]);
			printf("\n");
		}
		if (length>0)
		  CmdProcess(buf,  length);
		else {
			close(activeServer);
			usleep(10000);
			activeServer=lxScriboSocketInit(server);
		}
	}
}
#endif
/*
 *
 */
cliSpeakerSide_t cliParseSpeakerSide(char *name)
{
	char firstLetter;
	cliSpeakerSide_t side;


	if (name==NULL)
		side=cli_speaker_none;
	else
	{
		firstLetter = tolower(name[0]);
		switch (firstLetter) {
			case '0':
			case 'l':
				side = cli_speaker_left;
				break;
			case '1':
			case 'r':
				side = cli_speaker_right;
				break;
			case 'b':
				side = cli_speaker_both;
				break;
			default:
				side = cli_speaker_none;
				break;
		}
	}
    if ( cli_verbose )
    	printf ("selected speaker: %s \n", speakerSideName[side]);
    else if (side==cli_speaker_none)
    	fprintf(stderr, "Warning: no speaker selected!\n");

    return side;
}
/*
 *
 */
nxpTfa98xxParamsType_t cliParseFiletype(char *filename)
{
	char *ext;
	nxpTfa98xxParamsType_t ftype;

	// get filename extension

	ext = strrchr(filename, '.'); // point to filename extension

	if ( ext == NULL ) {
		ftype = tfa_no_params;	// no '.'
	}

	// now look for supported type
	else if ( strcmp(ext, ".patch")==0 )
		ftype = tfa_patch_params;
	else if ( strcmp(ext, ".speaker")==0 )
		ftype = tfa_speaker_params;
	else if ( strcmp(ext, ".preset")==0 )
		ftype = tfa_preset_params;
	else if ( strcmp(ext, ".config")==0 )
		ftype = tfa_config_params;
	else if ( strcmp(ext, ".eq")==0 )
		ftype = tfa_equalizer_params;

    if ( cli_verbose )
    	printf("file %s is a %s.\n" , filename, filetypeName[ftype]);

    return ftype;

}
/*
 * init the gengetopt stuff
 */
char *cliInit(int argc, char **argv)
{
	char *devicename;

    cmdline_parser (argc, argv, &gCmdLine);
    if(argc==1) // nothing on cmdline
    {
    		cmdline_parser_print_help();
    		exit(1);
    }
    // extra command line arg for test settings
    lxDummyArg = argv[optind]; // this is the remaining argv

    // generic flags
    cli_verbose=gCmdLine.verbose_given;
    //lxScribo_verbose=
    tfa98xx_verbose = cli_verbose;
    tfa98xx_quiet = gCmdLine.quiet_given;
    cli_trace=gCmdLine.trace_given;
//    tfa98xx_trace = cli_trace;
#ifndef WIN32
    i2c_trace = cli_verbose;
#endif
    NXP_I2C_verbose= cli_trace;
    cli_quiet=gCmdLine.quiet_given;

    if (gCmdLine.device_given)
    	devicename=gCmdLine.device_arg;
    else
#ifdef TFA_I2CDEVICE
    	devicename=TFA_I2CDEVICE;
#else
    	devicename=DEVNAME;
#endif
    return devicename;

}
