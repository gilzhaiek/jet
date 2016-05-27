#include "jni.h"

#include <stdio.h>
#include <android/ags_androidlog.h>
#include "../include/android/_linux_specific.h"
#include <sys/types.h>
#include <sys/stat.h>
#include <errno.h>
#ifdef MCP_LOGD
#define LOGD ALOGD
#else
#define LOGD
#endif
static int mess_number = 0;
jboolean JDumpMessage (JNIEnv * env, jobject obj, jbyteArray ByteArray)
{
	jboolean ret = JNI_FALSE;
	char Dbg [32];
	char Fname [64];
	char *Ptr = Fname;

	sprintf (Fname, "/data/_SLP_message_%d_.slp", mess_number);
	mess_number++;
	debugMessage("Keep message %s.",__FUNCTION__);
	int mFd = open (Fname, O_TRUNC | O_CREAT | O_WRONLY | O_NONBLOCK,  S_IRWXU | S_IROTH);
	if (mFd > 0) 
	{
		
		jboolean isCopy = 1;
		jsize arrSize = env->GetArrayLength(ByteArray);
		jbyte * bytes = env->GetByteArrayElements(ByteArray, &isCopy);
		jint keep = 0;
		jsize byte_to_file = arrSize;
		while (arrSize > 0) 
		{
			keep = write(mFd, bytes, arrSize);
			if (keep < 0)
				break;
			else 
				arrSize-=keep;
		}
		
		if (arrSize == 0)  
		{
			debugMessage("%s: Saved %d bytes", __FUNCTION__, byte_to_file);
			ret = JNI_TRUE;
		}
		else 
			debugMessage("%s: Error save %d bytes", __FUNCTION__, byte_to_file);
		
		env->ReleaseByteArrayElements(ByteArray, bytes, JNI_ABORT);
		
		close(mFd);
	} 
	else 
	{
		debugMessage("%s: Error open dump file", __FUNCTION__);
	}
	return ret;
}



int saveMessage (char * stringFileName, void * inbuf, int i_size)
{
    //char fileName[32] = "/supl/";
    int byte_to_file = i_size;
    int keep = 0;
    int ret = -1;
    //strcat(fileName, stringFileName);
    debugMessage("saveMessage: %s", stringFileName);

    int mFd = open (stringFileName, O_TRUNC | O_CREAT | O_WRONLY | O_NONBLOCK,  S_IRWXU | S_IROTH);
    if (mFd > 0) 
    {
	while (i_size > 0) 
	{
	    keep = write(mFd, inbuf, i_size);
	    if (keep < 0)
		break;
	    else
		i_size -= keep;
	}
		
	if (i_size == 0)
	{
		debugMessage("%s: Saved %d bytes", __FUNCTION__, byte_to_file);
		ret = 1;
	}
	else 
		debugMessage("%s: Error save %d bytes", __FUNCTION__, byte_to_file);
		
	close(mFd);
    } 
    else 
    {
	debugMessage("%s: Error open dump file", __FUNCTION__);
    }
    
    return ret;
}

int createSuplExchangeDir(const char *path)
{
    struct stat st;
    if (stat(path, &st) >= 0)
    {
	if (S_ISDIR(st.st_mode) == 1)
	{
	    return 0;
	}
    }
    
    if (mkdir(path, 0777) < 0)
    {
	debugMessage("%s, %s - error create directory: %s", path, strerror(errno));
	return -1;
    }
    
    return 0;
}
