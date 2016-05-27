/*
******************************************************************
* COPYRIGHT © Teleca AB                                          *
*----------------------------------------------------------------*
* MODULE     : ags_jni_interface.h                               *
*                                                                *
* PROGRAMMER : Dmitriy Kardakov                                  *
* DATE       : 28 Feb 2009                                       *
* VERSION    : 1.0                                               *
*                                                                *
*----------------------------------------------------------------*
* MODULE SUMMARY : Declares the Android OS-dependent interface   *
* needed for implementation of SUPL classes                      *
*----------------------------------------------------------------*
* MODIFICATION RECORDS                                           *
* Dmitriy Kardakov - 28 Feb 2009 - Initial version               *
******************************************************************
*/

#ifndef __AGS_JNI_INTERFACE_H__
#define __AGS_JNI_INTERFACE_H__

enum eMessageFormat
{
    UTF8   = 1,
	GSM_DEFAULT = 2,	
    UTF16  = 3
};

typedef struct {
    char bytes[128];
    int size;
    int encoding;
} NotificationBytes_t;

int MessageBox(int verification, NotificationBytes_t *strings,
               int size, int Id, int timeout);
int getSubscriberId(char* buffer, int buffer_size);
int getSubscriberIdType();
int getMSISDN(char* buffer, int buffer_size);
int getMccMncSize();
int getHSlp(char* buffer);
int getGSMInfo(int * info);
int startSMSListening();
int startWAPListening();

int stopSMSListening();
int stopWAPListening();

int Init_native();
int Send_native(void *data, unsigned int len);
int CreateConnection_native(const char* Host_port);
void FreeConnection_native();
int IsActive_native();
int Receive_native();

#endif // __AGS_JNI_INTERFACE_H__
