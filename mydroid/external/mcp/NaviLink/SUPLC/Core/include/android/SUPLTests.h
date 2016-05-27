#include "suplcontroller/SUPLController.h"
#include "android/_linux_specific.h"
//#include "android/supl.h"
//#include <sys/types.h>
//#include <sys/stat.h>

#include "jni.h"

//void debugMessage(const char * TAG, const char * MSG, JNIEnv * env);
extern int FillStartReqData(Platform::StartReqData * data);
extern Engine::uint8_t SUPL_Control(
					Engine::uint16_t AppId, 
					Engine::uint32_t dwCode,  //the control code for the operation
					Engine::uint8_t* pBufIn/*, 
					
					
					Engine::uint32_t dwLenIn */ //the size, in bytes, of the buffer
				   );

// OMA Test functions 
void * SUPL_OMA_BT1(void *);
void * SUPL_OMA_BT2(void *);
void * SUPL_OMA_BT3(void *);
void * SUPL_OMA_BT4(void *);
void * SUPL_OMA_BT5(void *);
void * SUPL_OMA_BT6(void *);
void * SUPL_OMA_BT7(void *);
void * SUPL_OMA_BT8(void *);
void * SUPL_OMA_BT9(void *);
void * SUPL_OMA_BT10(void *);
void * SUPL_OMA_BT11(void *);
void * SUPL_OMA_BT12(void *);


