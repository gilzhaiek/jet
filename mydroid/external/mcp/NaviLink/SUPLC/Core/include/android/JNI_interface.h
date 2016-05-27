#ifndef _JNI_INTERFACE_H_
#define _JNI_INTERFACE_H_


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

int MessageBox(int verification, NotificationBytes_t * strings, int size, int Id, int timeout); // - X
//LocationProvider metod:            int  (boolean verification, byte []  Requestor, int enc1 , byte [] clientName, int enc2, int sessionId, int timeout )
int getSubscriberId(char* buffer, int buffer_size);
//LocationProvider metod:             byte[] getSubscriberId() 
int getMSISDN(char* buffer, int buffer_size);
int getHSlp(int *buffer);
int getMccMncSize();
//LocationProvider metod:            int getMccMncSize()
int getGSMInfo(int * info);
//LocationProvider metod:             int[] getGSMInfo()

//SMS/WAP-push
int startSMSListening();
int startWAPListening();

void stopSMSListening();
void stopWAPListening();
//Network metods
//metods calls from networking_CNet.so library
int Init_native() ;
int Send_native(void *data, unsigned int len) ;
int CreateConnection_native(char* Host_port) ;
void FreeConnection_native() ;
int IsActive_native() ;
int Receive_native() ;


//---native metods
//native boolean PostSLPMessageToQueue (byte [] mess);
// + native metods for positioning
#endif // _JNI_INTERFACE_H_
