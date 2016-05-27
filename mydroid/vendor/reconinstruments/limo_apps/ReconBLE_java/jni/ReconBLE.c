#include <string.h>
#include <jni.h>

#include <asm/ioctl.h>
#include <sys/types.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <asm-generic/poll.h>  

#include "cc2540_ble.h"   // driver interface
#include "ReconBLE.h"
#include "BLENative.h"

// local definitions
#define SLAVE_CHAR 0xfe
#define BLE_DEVICE  "/dev/cc2540_ble" 

// logging
#include <android/log.h>
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,   "ReconBLE", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,   "ReconBLE", __VA_ARGS__)
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE,   "ReconBLE", __VA_ARGS__)

/* Opens BLE Device */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_openDevice
(
 JNIEnv* env,    // environment ponter
 jobject jthis   // parent stub java class
 )
{
  // open device file and put it in slave mode
  int fd_ble = open(BLE_DEVICE, O_RDWR);
  if (fd_ble == -1)
    {
      LOGE("*** BLENative_createSession: Failed to open BLE Device (%s) ***", BLE_DEVICE);
      return 0;
    }

  // reset device as well
  //ioctl(fd_ble, CC2540_BLE_RESET_CMD);
    
  // return cookie. It will be cast back to object every time
  return (jint)fd_ble;
}

/* Closes BLE Device. context is invalidated */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_closeDevice
(
 JNIEnv* env,     // environment ponter
 jobject jthis,   // parent stub java class
 jint    context  // session pointer
 )
{
  int fd_ble = (int)context;
  close (fd_ble);
    
  return com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS;
}

/*
 * Class:     com_reconinstruments_reconble_BLENative
 * Method:    resetDevice
 * Signature: (I)I
 *
 * Full Reset of BLE Board. This blocks short time in Kernel right now
 * Didn't want to do it async
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_resetDevice
(
 JNIEnv* env,     // environment ponter
 jobject jthis,   // parent stub java class
 jint    context  // session pointer
 )
{
  int err = ioctl ((int)context, CC2540_BLE_RESET_CMD);
        
  if (err != 0)
    {
      LOGE("***BLENative_resetBoard: Failure (0x%x) ***", err);
      return com_reconinstruments_reconble_ReconBLE_ERR_FAIL;
    }
    
  return com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS;
}

JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_setSlave
(
 JNIEnv* env,     // environment ponter
 jobject jthis,   // parent stub java class
 jint    context  // session pointer
 )
{
  int fd_ble = (int)context;
  char ch = SLAVE_CHAR; 
    
  // must reset board first
  // this will block in kernel
  int err = Java_com_reconinstruments_reconble_BLENative_resetDevice (env, jthis, context);
  if (err == com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS)  
    err = ioctl (fd_ble, CC2540_BLE_NORMALCMD, &ch);   // now put device to slave mode
        
  if (err != 0)
    {
      LOGE("***BLENative_setSlave: Could not put device to Slave mode (0x%x) ***", err);
      return com_reconinstruments_reconble_ReconBLE_ERR_SLAVE;
    }
    
  return com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS;
}
/*
 * Class:     com_reconinstruments_reconble_BLENative
 * Method:    setMaster
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_setMaster
(
 JNIEnv* env,      // environment ponter
 jobject jthis,    // parent stub java class
 jint    context   // session pointer
 )
{

    int fd_ble = (int)context;
  char ch = 0xf0; 		/* fixme: put constant name */
    
  // must reset board first
  // this will block in kernel
  int err = Java_com_reconinstruments_reconble_BLENative_resetDevice (env, jthis, context);
  if (err == com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS)  
    err = ioctl (fd_ble, CC2540_BLE_NORMALCMD, &ch);   // now put device to slave mode
        
  if (err != 0)
    {
      LOGE("***BLENative_setMaster: Could not put device to Master mode (0x%x) ***", err);
      return com_reconinstruments_reconble_ReconBLE_ERR_SLAVE;
    }
    
  return com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS;


}   
  
/*
 * Class:     com_reconinstruments_reconble_BLENative
 * Method:    getMode
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_getMode
(
 JNIEnv* env,      // environment ponter
 jobject jthis,    // parent stub java class
 jint    context   // session pointer
 )
{
  // this feature is not implemented yet in the driver
  LOGE("*** BLENative_setMaster: This feature has not been implemented yet ***");
  return com_reconinstruments_reconble_ReconBLE_ERR_FAIL;
}



  
/*
 * Class:     com_reconinstruments_reconble_BLENative
 * Method:    PairMaster
 * Signature: ()I
 *
 * master pairing. MAC address in passed 6 byte buffer. This typically comes from
 * BLE.RIB, we try to bond but also scan new search
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_pairMaster
(
 JNIEnv* env,      // environment ponter
 jobject jthis,    // parent stub java class
 jint    context,  // session pointer
 jbyteArray mac    // mac address byte array
 )
{
  int fd_ble = (int)context;
  jbyte buf[16+3];	  /* FIXME 16->irk_size.  More: As for 3: it
			     is payload_len byte, cmd and
			     boundstatus*/

  /* The extra bytes are for ioctl commands */
    
  int err = com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS;

  /* Create another buffer with three more bytes first the length of payload and
     second the command: BLE_BOND_CMD: [8 BLE_BOND_CMD 1 CRAP_MAC]*/


  /* First insert the headers */

  buf[0] =
    com_reconinstruments_reconble_ReconBLE_MAC_ADDRESS_SIZE + 2; /* len payload */

  buf[1] = BLE_BOND_CMD;
  buf[2] = 2;		/* Means bond with the given mac address
			   only */

  /* Copy the mac adderss now */
  (*env)->GetByteArrayRegion(env, mac, 0, com_reconinstruments_reconble_ReconBLE_MAC_ADDRESS_SIZE, &buf[3]);

    
  err = ioctl(fd_ble, CC2540_BLE_GENERALCMD, buf);
  if (err != 0)
    {
      LOGE("*** BLENative_pairMaster: CC2540_BLE_SET_BOND_NEW_SEARCH Failed (0x%x) ***", err);
      return com_reconinstruments_reconble_ReconBLE_ERR_FAIL;
    }
    
  return com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS;   // success on initializing pairing; 
  // connection status comes async
}
 
/*
 * Class:     com_reconinstruments_reconble_BLENative
 * Method:    getMasterList
 * Signature: (I[B)I
 * get master list after status returned REMOTE_LIST_READY. 
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_getMasterList
(
 JNIEnv*    env,      // environment ponter
 jobject    jthis,    // parent stub java class
 jint       context,  // session pointer
 jbyteArray addrList  // retval: mac address byte array
 )
{
  int fd_ble = (int)context;
  jbyte buf[1 + com_reconinstruments_reconble_ReconBLE_MAC_REMOTE_LIST_NUM * com_reconinstruments_reconble_ReconBLE_MAC_ADDRESS_SIZE];
  int err = ioctl(fd_ble, CC2540_BLE_GET_REMOTE_LIST, buf);
  if (err != 0)
    {
      LOGE("*** BLENative_getMasterList: CC2540_BLE_GET_REMOTE_LIST Failed (0x%x) ***", err);
      return com_reconinstruments_reconble_ReconBLE_ERR_FAIL;
    }
   
  (*env)->SetByteArrayRegion(env, addrList, 0,
			     1 + com_reconinstruments_reconble_ReconBLE_MAC_REMOTE_LIST_NUM * com_reconinstruments_reconble_ReconBLE_MAC_ADDRESS_SIZE, buf);
   
  return com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS;
}
 
 
/*
 * Class:     com_reconinstruments_reconble_BLENative
 * Method:    bondMaster
 * Signature: (I[B)I
 *
 * Tries to bond with broadcasted Master list
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_bondMaster
(
 JNIEnv*    env,      // environment ponter
 jobject    jthis,    // parent stub java class
 jint       context,  // session pointer
 jbyteArray mac       // mac address to bond with
 )
{
  int fd_ble = (int)context;
  jbyte buf[com_reconinstruments_reconble_ReconBLE_MAC_ADDRESS_SIZE+3]; /* 3 for ioctl */
  int err = com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS;
    
  /* First add header info */
  /* len payload: */
  buf[0] = com_reconinstruments_reconble_ReconBLE_MAC_ADDRESS_SIZE+ 2;

  buf[1] = BLE_BOND_CMD;	/* ioctl command */
  buf[2] = 2;		/* Bond with the address (no new master search) */
    
  /* Now copy the mac address */
  (*env)->GetByteArrayRegion(env, mac, 0, com_reconinstruments_reconble_ReconBLE_MAC_ADDRESS_SIZE, &buf[3]);


  err = ioctl(fd_ble, CC2540_BLE_GENERALCMD, buf);
  if (err != 0)
    {
      LOGE("*** BLENative_bondMaster: CC2540_BLE_SET_BOND Failed (0x%x) ***", err);
      return com_reconinstruments_reconble_ReconBLE_ERR_FAIL;
    }
    
  return com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS;   // success on initializing bond; 
  // connection status comes async
}
 
/*
 * Class:     com_reconinstruments_reconble_BLENative
 * Method:    newSearch
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_newSearch
(
 JNIEnv*    env,      // environment ponter
 jobject    jthis,    // parent stub java class
 jint       context   // session pointer
 )
{
  int fd_ble = (int)context;
  jbyte buf[3];		/* room for our command args */

  buf[0] = 2;		/* len payload */
  buf[1] = BLE_BOND_CMD;
  buf[2] = 0;		/* Search for new */

  int err = ioctl(fd_ble, CC2540_BLE_GENERALCMD, buf);
 
  if (err != 0)
    {
      LOGE("*** BLENative_newSearch: CC2540_BLE_NEW_PAIR_CMD Failed (0x%x) ***", err);
      return com_reconinstruments_reconble_ReconBLE_ERR_FAIL;
    }
     
  return com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS;
}
 
/*
 * Class:     com_reconinstruments_reconble_BLENative
 * Method:    unpairMaster
 * Signature: (I)I
 *
 * Until driver supports this via dedicated ioctl, we
 * implement via (re)setting slave mode -- which effectively 
 * resets BLE chip as well
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_unpairMaster
(
 JNIEnv*    env,      // environment ponter
 jobject    jthis,    // parent stub java class
 jint       context   // session pointer
 )
{

  jbyte buf[5];
  buf[0] = 4;
  buf[1] = BLE_TERMINATE_CMD;
  buf[2] = 8;			/* FIXME: should adjust for master or
				   slave for now set to default slave. i.e. 8*/
  buf[3] = 0;			/* FIXME: buf [3,4] is used for
				   connection handle, small
				   endian. for slave mode it suffices
				   to put everthing as 0 */
  buf[4] = 0;
  int fd_ble = (int)context;
  int err = ioctl(fd_ble, CC2540_BLE_GENERALCMD, buf);

  if (err != com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS)
    {
      LOGE("*** BLENative_unpairMaster: Failure ***");
      return err;
    }
     
  return com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS;
}

/*
 * Class:     com_reconinstruments_reconble_BLENative
 * Method:    unpairSlave
 * Signature: (II)I
 *
 * Unpairs with slave associated with argument arg. For remote arg is
 * 0
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_unpairSlave
  (JNIEnv *env , jobject jthis, jint context, jint arg)
{
    jbyte buf[5];
  buf[0] = 4;
  buf[1] = BLE_TERMINATE_CMD;
  buf[2] = 10;			/* FIXME: change to constant */
  buf[3] = arg;			/* We only have three devices so the
				   first small endian device number
				   should suffice: for remote arg is  */
  buf[4] = 0;
  int fd_ble = (int)context;
  int err = ioctl(fd_ble, CC2540_BLE_GENERALCMD, buf);

  if (err != com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS)
    {
      LOGE("*** BLENative_unpairMaster: Failure ***");
      return err;
    }
     
  return com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS;

}



/* Added by Ali
 * Class:     com_reconinstruments_reconble_BLENative
 * Method:    slaveRequestSecurity
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_slaveRequestSecurity
(JNIEnv *env, jobject jthis, jint context)
{
  int fd_ble = (int)context;
  unsigned char buffer[] = {BLE_SLAVESECURITYREQUEST_CMD}; 
  int err = ioctl(fd_ble, CC2540_BLE_NORMALCMD, buffer);
  if (err != 0)
    {
      LOGE("*** BLENative_slaveRequest: CC2540_BLE_SLAVE_REQUEST_CMD Failed (0x%x) ***", err);
      return com_reconinstruments_reconble_ReconBLE_ERR_FAIL;
    }
  else
    {
      return com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS;
    }

 
}

/*
 * Class:     com_reconinstruments_reconble_BLENative
 * Method:    getIrk
 * Signature: (I[B)I
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_getIrk
(
 JNIEnv* env,      // environment ponter
 jobject jthis,    // parent stub java class
 jint    context,  // session pointer
 jbyteArray irk    // mac address byte array
 )
{

  int fd_ble = (int)context;
  jbyte buf[16];		/* FIXME: 16-> constant naem it should
				   be constant from ReconBLE.h */
  int err = ioctl(fd_ble, CC2540_BLE_GET_IRK, buf);
  if (err != 0)
    {
      LOGE("*** BLENative_getMasterList: CC2540_BLE_GET_IRK Failed (0x%x) ***", err);
      return com_reconinstruments_reconble_ReconBLE_ERR_FAIL;
    }
  
  (*env)->SetByteArrayRegion(env, irk, 0,
			     16, buf); /* FIXME: 16 -> constant name */
  
  return com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS;

  
}
  
/*
 * Class:     com_reconinstruments_reconble_BLENative
 * Method:    getMiscData
 * Signature: (I[B)I
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_getMiscData
(
 JNIEnv* env,      // environment ponter
 jobject jthis,    // parent stub java class
 jint    context,  // session pointer
 jbyteArray miscdata    // mac address byte array
 )
{
  int fd_ble = (int)context;
  jbyte buf[60];		/* FIXME: 60-> constant name it should
				   be constant from ReconBLE.h
				   max size of misc data*/
  int err = ioctl(fd_ble, CC2540_BLE_GET_MISC_DATA, buf);
  if (err != 0)
    {
      LOGE("*** BLENative_getMasterList: CC2540_BLE_GET_MISC_DATA Failed (0x%x) ***", err);
      return com_reconinstruments_reconble_ReconBLE_ERR_FAIL;
    }
  
  (*env)->SetByteArrayRegion(env, miscdata, 0,
			     60, buf); /* FIXME: 16 -> constant name */
  
  return com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS;

  
}


/*
 * Class:     com_reconinstruments_reconble_BLENative
 * Method:    pairWithSlave
 * Signature: (II[B)I
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_pairWithSlave
  (JNIEnv * env, jobject jthis, jint context, jint bondstatus, jbyteArray mac)
{
    int fd_ble = (int)context;

    jbyte buf[com_reconinstruments_reconble_ReconBLE_MAC_ADDRESS_SIZE+3];
      /* As for 3: it is payload_len byte, cmd and boundstatus*/
    /* The extra bytes are for ioctl commands */

    int err = com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS;
    /* Create another buffer with three more bytes first the length of
     payload and second the command: BLE_BOND_CMD: [8 BLE_BOND_CMD 1
     CRAP_MAC]*/


    /* First insert the headers */
    buf[1] = BLE_BOND_CMD;
    buf[2] = bondstatus;

    /* Three sort of behaviour based on bondstatus */
    if (bondstatus == 0)
      {
	buf[0] = 2; /* len payload */
	/* note that in this case we don't copy the mac address  */
	
      }
    else if (bondstatus == 1)
      {
	buf[0] =
      com_reconinstruments_reconble_ReconBLE_MAC_ADDRESS_SIZE + 2; /* len payload */

	/* Copy the mac adderss now */
	(*env)->GetByteArrayRegion(env, mac, 0, com_reconinstruments_reconble_ReconBLE_MAC_ADDRESS_SIZE, &buf[3]);
      }
    else if (bondstatus == 2 || bondstatus == 3)
      {
	buf[0] =
	  com_reconinstruments_reconble_ReconBLE_MAC_ADDRESS_SIZE + 2; /* len payload */

	/* Copy the mac adderss now */
	(*env)->GetByteArrayRegion(env, mac, 0, com_reconinstruments_reconble_ReconBLE_MAC_ADDRESS_SIZE, &buf[3]);
      }

    err = ioctl(fd_ble, CC2540_BLE_GENERALCMD, buf);
    if (err != 0)
      {
	LOGE("*** BLENative_pairMaster: CC2540_BLE_SET_BOND_NEW_SEARCH Failed (0x%x) ***", err);
	return com_reconinstruments_reconble_ReconBLE_ERR_FAIL;
      }
    
    return com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS;   // success on initializing pairing; 
  // connection status comes async

    
}

/*
 * Class:     com_reconinstruments_reconble_BLENative
 * Method:    pairWithMaster
 * Signature: (II[B)I
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_pairWithMaster
  (JNIEnv * env, jobject jthis, jint context, jint bondstatus, jbyteArray irk)
{
  int fd_ble = (int)context;

  jbyte buf[16+3];
  /* FIXME: 16 -> size of irk */
  /* As for 3: it is payload_len byte, cmd and boundstatus*/
  /* The extra bytes are for ioctl commands */

  int err = com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS;
  /* Create another buffer with three more bytes first the length of
     payload and second the command: BLE_BOND_CMD: [8 BLE_BOND_CMD 1
     CRAP_MAC]*/


  /* First insert the headers */
  buf[1] = BLE_BOND_CMD;
  buf[2] = bondstatus;

  /* Three sort of behaviour based on bondstatus */
  if (bondstatus == 0)
    {
      buf[0] = 2; /* len payload */
      /* note that in this case we don't copy the mac address  */
	
    }
  else if (bondstatus == 1)
    {
      buf[0] =16 + 2; /* len payload */
      /* 16 -> len irk */

      /* Copy the irk now */
      (*env)->GetByteArrayRegion(env, irk, 0, 16, &buf[3]);
      /* FIXME: 16 -> len irk */
    }
  else if (bondstatus == 2)
    {
      buf[0] =  16 + 2; /* len payload */
      /* FIXME: 16 -> len irk */
      /* Copy irk now */
      (*env)->GetByteArrayRegion(env, irk, 0, 16, &buf[3]);
    }

  err = ioctl(fd_ble, CC2540_BLE_GENERALCMD, buf);
  if (err != 0)
    {
      LOGE("*** BLENative_pairWithMaster: Failed (0x%x) ***", err);
      return com_reconinstruments_reconble_ReconBLE_ERR_FAIL;
    }
    
  return com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS;   // success on initializing pairing; 
  // connection status comes async


}



/*
 * Class:     com_reconinstruments_reconble_BLENative
 * Method:    checkStatus
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_checkStatus
(
 JNIEnv* env,       // environment ponter
 jobject jthis,     // parent stub java class
 jint    context,    // session pointer
 jint withPolling
 )
{

  int fd_ble = (int)context;
  struct pollfd fds;
  fds.events = POLLIN;
  fds.fd = fd_ble;
  //LOGD("Polling");
  if (withPolling) {
    int ret=poll (&fds, 1, -1) ; //poll for infinity
  }
  unsigned char buffer[6];
  int err = ioctl(fd_ble, CC2540_BLE_GET_STATUS, buffer);
  //LOGD("CHECK_STATUS","CHECK STATUS");
  if (err != 0)
    {
      LOGE("*** BLENative_checkStatus: CC2540_BLE_GET_STATUS Failed (0x%x) ***", err);
      return com_reconinstruments_reconble_ReconBLE_ERR_FAIL;
    }
     
  // status is in 1st byte
  return buffer[0];
}
  


/*
 * Class:     com_reconinstruments_reconble_BLENative
 * Method:    flushBuffers
 * Signature: (I)I
 *
 * Forcefully flushes the buffers on chip I/O registers. Well behaved client
 * should never have to do this
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_flushBuffers
(
 JNIEnv* env,      // environment ponter
 jobject jthis,    // parent stub java class
 jint    context   // session pointer
 )
{
  int err = ioctl ((int)context, CC2540_BLE_CLEAR_CMD);
        
  if (err != 0)
    {
      LOGE("***BLENative_flushBuffers: Failure (0x%x) ***", err);
      return com_reconinstruments_reconble_ReconBLE_ERR_FAIL;
    }
    
  return com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS;
}


/*
 * Class:     com_reconinstruments_reconble_BLENative
 * Method:    beginTransmission
 * Signature: (I)I
 *
 * Starts Tx session with BLE driver
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_beginTransmission
(
 JNIEnv*    env,      // environment ponter
 jobject    jthis,    // parent stub java class
 jint       context   // session pointer
 )
{
  int fd_ble = (int)context;
  int status = com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS;
  unsigned char buffer[3];
    
  buffer[0] = BLE_REQ_SEND_CMD;
    
  status = ioctl(fd_ble, CC2540_BLE_NORMALCMD, buffer);  
  if (status != 0)
    {
      LOGE("*** BLENative_beginTransmission: CC2540_BLE_NORMALCMD Failed (0x%x) ***", status);
      return com_reconinstruments_reconble_ReconBLE_ERR_FAIL;
    }
    
  return com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS;
    
}
  
/*
 * Class:     com_reconinstruments_reconble_BLENative
 * Method:    sendData
 * Signature: (I[B)I
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_sendData
(
 JNIEnv*    env,      // environment ponter
 jobject    jthis,    // parent stub java class
 jint       context,  // session pointer
 jbyteArray data,     // raw byte buffer to send
 jint       toSend    // number of bytes from raw buffer
 )
{
  int fd_ble = (int)context;
  int status = com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS;
  int size = (*env)->GetArrayLength(env, data); 
  jbyte* carr = 0;
    
  // sanity check: toSend can not be more than 1026 bytes -- or actual buffer size
  if ( (toSend > size) || (toSend > com_reconinstruments_reconble_ReconBLE_BUFFER_IO_SIZE) )
    {
      LOGE("*** BLENative_sendData: Invalid Send size ( [%d] bytes ). Data Buffer [%d], Maximum [%d] ***",
	   toSend, size, 
	   com_reconinstruments_reconble_ReconBLE_BUFFER_IO_SIZE);
          
      return  com_reconinstruments_reconble_ReconBLE_ERR_WRITE;
    }
    
  // lock allocated byffer from jni environment
  carr = (*env)->GetByteArrayElements(env, data, NULL);
  if (carr == NULL) 
    {
      LOGE("*** BLENative_sendData: Internal JNI Error - Could not access Data Buffer ***");
      return com_reconinstruments_reconble_ReconBLE_ERR_FAIL;  /* exception occurred */
    }
    
  // no status checking at this level -- we simply route to driver
  // state information is managed in Java layer, as well as data chunk partitioning
  LOGV("*** BLENative_sendData: Attempting to send [%d] bytes", toSend);
  
    
  if (write (fd_ble, carr, toSend) != toSend)
    status = com_reconinstruments_reconble_ReconBLE_ERR_WRITE;
  
  // release lock
  (*env)->ReleaseByteArrayElements(env, data, carr, 0);
    
  if (status != com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS)
    LOGE("*** BLENative_sendBuffer Failure ***");
       
  return status;
}
 
/*
 * Class:     com_reconinstruments_reconble_BLENative
 * Method:    endTransmission
 * Signature: (I)I
 *
 * End Tx session with BLE driver. Necessary only when last packet is exactly 
 * BLE_BLOCK_SIZE * BLOCK_NUM_MAX  ( 57*18 ) bytes
 *
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_endTransmission
(
 JNIEnv*    env,      // environment ponter
 jobject    jthis,    // parent stub java class
 jint       context,   // session pointer
 jint prior,
 jint numbytes
 )
{
  
  int fd_ble = (int)context;
  unsigned char buffer[2];
  buffer[0] = numbytes;		/* For now we assume the effective
				   length is still 57 in some deep
				   multiplexing of putting multipe
				   priorities in the buffer and etc
				   this number may change */
  buffer[1] = prior;
  int status =  ioctl(fd_ble, CC2540_BLE_EOT,buffer);
  
  if (status != 0)
    {
      LOGE("*** BLENative_endTransmission: CC2540_BLE_EOT Failed (0x%x) ***", status);
      return com_reconinstruments_reconble_ReconBLE_ERR_FAIL;
    }
    
  return com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS;
    
}


/*
 * Class:     com_reconinstruments_reconble_BLENative
 * Method:    startReceive
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_startReceive
  (JNIEnv * eng,
   jobject jthis,
   jint context,
   jboolean shouldAck)
{
    int fd_ble = (int)context;
    //For now comment out the whole thing
    int err = 0;
    //int err = ioctl (fd_ble, CC2540_BLE_SET_RX_MODE);
    LOGD("***BLENative_startReceive");

    if (shouldAck)
      {
	char buffer=BLE_READY_RSP_CMD;  //0xA5
	ioctl(fd_ble, CC2540_BLE_NORMALCMD, &buffer);
	LOGD("***BLENative_startReceive");
      }
        
  if (err != 0)
    {
      LOGE("***BLENative_startReceive: Failure (0x%x) ***", err);
      return com_reconinstruments_reconble_ReconBLE_ERR_FAIL;
    }
    
  return com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS;
    
}


/*
 * Class:     com_reconinstruments_reconble_BLENative
 * Method:    receiveData
 * Signature: (I[BI)I
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_receiveData
(
 JNIEnv*    env,      // environment ponter
 jobject    jthis,    // parent stub java class
 jint       context,  // session pointer
 jbyteArray data      // raw byte buffer to fill. Always com_reconinstruments_reconble_ReconBLE_BUFFER_IO_SIZE
 )
{
  int fd_ble = (int)context;
  jbyte* carr = 0;
  jint   iread = 0;
    
  LOGV("*** BLENative_readData: Attempting to read up to [%d] bytes", com_reconinstruments_reconble_ReconBLE_BUFFER_IO_SIZE);
  
  // lock allocated byffer from jni environment
  carr = (*env)->GetByteArrayElements(env, data, NULL);
  if (carr == NULL) 
    {
      LOGE("*** BLENative_readData: Internal JNI Error - Could not access Data Buffer ***");
      return com_reconinstruments_reconble_ReconBLE_ERR_FAIL;  /* exception occurred */
    }
   
  // perform read from the driver
  iread = read(fd_ble, carr, com_reconinstruments_reconble_ReconBLE_BUFFER_IO_SIZE);
    
  // release lock
  (*env)->ReleaseByteArrayElements(env, data, carr, 0);
 
  // set retval -- number of bytes read
  if (iread == -1)
    LOGE("*** BLENative_readData Failure ***");
       
  return iread;
}


/*
 * Class:     com_reconinstruments_reconble_BLENative
 * Method:    setSendPriority
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_setSendPriority
 (
   JNIEnv *env,
   jobject jthis,
   jint context,
   jint prio
  )
{
  int fd_ble = (int)context;
  unsigned char buffer[1];
  buffer[0] = prio;
  int status = ioctl ((int)context, CC2540_BLE_SET_TXSHIP,buffer);
  LOGD("*** BLENative_setSendPriority");
  if (status != 0)
    {
      LOGE("*** BLENative_setSendPriority: CC2540_BLE_EOT Failed (0x%x) ***", status);
      return com_reconinstruments_reconble_ReconBLE_ERR_FAIL;
    }
  
  return com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS;

}
 
/*
 * Class:     com_reconinstruments_reconble_BLENative
 * Method:    getCurrentSendPriority
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_getCurrentSendPriority
  (
   JNIEnv *env,
   jobject jthis,
   jint context
   )
{
  int fd_ble = (int)context;
  unsigned char buffer[3];
  ioctl(fd_ble, CC2540_BLE_GET_SHIP_TYPE, buffer);
  /* buffer[0] is send priority and buffer[1] is receive priority
     indicates if we are sending or receiving. For now we only care
     about buffer[0] */
  return buffer[0];
}

/*
 * Class:     com_reconinstruments_reconble_BLENative
 * Method:    getCurrentReceivePriority
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_getCurrentReceivePriority
  (
   JNIEnv *env,
   jobject jthis,
   jint context
   )
{
  int fd_ble = (int)context;
  unsigned char buffer[3];
  ioctl(fd_ble, CC2540_BLE_GET_SHIP_TYPE, buffer);
  return buffer[1];
}

/*
 * Class:     com_reconinstruments_reconble_BLENative
 * Method:    getSendOrReceiveMode
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_getSendOrReceiveMode
  (JNIEnv *env, jobject this, jint context)
{
  int fd_ble = (int)context;
  unsigned char buffer[3];
  ioctl(fd_ble, CC2540_BLE_GET_SHIP_TYPE, buffer);
  return buffer[2];
}


/*
 * Class:     com_reconinstruments_reconble_BLENative
 * Method:    sendAck
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_sendAck
  (JNIEnv *env,
   jobject this,
   jint context)
{
  int fd_ble = (int)context;
  jbyte buf[1];
  buf[0] =BLE_READY_RSP_CMD;
  int err = ioctl(fd_ble, CC2540_BLE_NORMALCMD, buf);
  if (err != 0)
    {
      LOGE("*** BLENative_sendAck (0x%x) ***", err);
      return com_reconinstruments_reconble_ReconBLE_ERR_FAIL;
    }
    
  return com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS;  
}

/* /\* */
/*  * Class:     com_reconinstruments_reconble_BLENative */
/*  * Method:    switchToNormalMode */
/*  * Signature: (I)I */
/*  *\/ */
/* JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_switchToNormalMode */
/*   (JNIEnv *env, jobject this, jint context) */
/* { */
/*   int fd_ble = (int)context; */
/*   jbyte buf[1]; */
/*   buf[0] = BLE_SWITCH_NORMAL_CMD; */
/*   int err = ioctl(fd_ble, CC2540_BLE_NORMALCMD, buf); */
/*   if (err != 0) */
/*     { */
/*       LOGE("*** BLENative_switchToNormalMode (0x%x) ***", err); */
/*       return com_reconinstruments_reconble_ReconBLE_ERR_FAIL; */
/*     } */
    
/*   return com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS;   */
/* } */

/*
 * Class:     com_reconinstruments_reconble_BLENative
 * Method:    disconnectRemote
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_resetRemote
  (JNIEnv *env,
   jobject this,
   jint context,
   jbyteArray irk
)
{
  int fd_ble = (int)context;
  jbyte buf[8]; /* 3 for ioctl */
  int err = com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS;
    
  /* First add header info */
  /* Note for now we don't use the irk argument, later on if irk is
     not null we provide irk to the remote as opposed blindly reset
     it. For now we just blindly reset it  */
  buf[0] = 7;			/* payload, will change to 0x16 with irk */
  buf[1] = BLE_WRITE_ATT_HANDLE_CMD;	
  buf[2] = 0;			/* connection handleLSB  */
  buf[3] = 0;			/* connection handleMSB  */
  buf[4] = 0x18;		/* attribute handle LSB */
  buf[5] = 0;			/* attribute handle MSB */
  buf[6] = 1;			/* Message length */
  buf[7] = 0;			/* Mesage (which means disconnect) */
  err = ioctl(fd_ble, CC2540_BLE_GENERALCMD, buf);
  if (err != 0)
    {
      LOGE("*** BLENative_bondMaster: CC2540_BLE_SET_BOND Failed (0x%x) ***", err);
      return com_reconinstruments_reconble_ReconBLE_ERR_FAIL;
    }
    
  return com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS;   // success on initializing bond; 
  // connection status comes async

}

/*
 * Class:     com_reconinstruments_reconble_BLENative
 * Method:    getOwnMac
 * Signature: (I[B)I
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_getOwnMac
(JNIEnv *env, jobject this, jint context, jbyteArray mac)
{
  int fd_ble = (int)context;
  jbyte buf[6];		/* Size of mac address */
  int err = ioctl(fd_ble, CC2540_BLE_GET_OWN_MAC, buf);
  (*env)->SetByteArrayRegion(env, mac, 0,6, buf);
  if (err != 0)
    {
      LOGE("*** BLENative_getOwnMac (0x%x) ***", err);
      return com_reconinstruments_reconble_ReconBLE_ERR_FAIL;
    }
    
  return com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS;  

}

/*
 * Class:     com_reconinstruments_reconble_BLENative
 * Method:    getRemoteMac
 * Signature: (I[BI)I
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_getRemoteMac
(JNIEnv *env, jobject this, jint context, jbyteArray mac, jint needversion)
{
  int fd_ble = (int)context;
  jbyte buf[6+needversion];		/* Size of mac address */
  int err = ioctl(fd_ble, CC2540_BLE_GET_MAC, buf);
  (*env)->SetByteArrayRegion(env, mac, 0,6+needversion, buf);
  if (err != 0)
    {
      LOGE("*** BLENative_getRemoteMac (0x%x) ***", err);
      return com_reconinstruments_reconble_ReconBLE_ERR_FAIL;
    }
    
  return com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS;  

}


/*
 * Class:     com_reconinstruments_reconble_BLENative
 * Method:    telliOSToClear
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_telliOSToClear
(JNIEnv *env, jobject this, jint context)
{
  int fd_ble = (int)context;
  unsigned char buffer[1];
  buffer[0] = 0xa2;
  LOGV("***BLENative_telliOSToClear");
  int err =  ioctl (fd_ble, CC2540_BLE_NORMALCMD, buffer);   // now ask iphone to clear
  if (err != 0)
    {
      LOGE("***BLENative_telliOSToClear: Could not put device to Slave mode (0x%x) ***", err);
      return com_reconinstruments_reconble_ReconBLE_ERR_SLAVE;
    }
    
  return com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS;

}

/*
 * Class:     com_reconinstruments_reconble_BLENative
 * Method:    sendControlMessage
 * Signature: (IB)I
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_sendControlByte
(JNIEnv *env, jobject this, jint context, jbyte ctrl)
{
  int fd_ble = (int)context;
  jbyte buf[3];	  /* FIXME 16->irk_size.  More: As for 3: it
			     is payload_len byte, cmd and
			     boundstatus*/

  /* The extra bytes are for ioctl commands */
    
  int err = com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS;

  /* Create another buffer with three more bytes first the length of payload and
     second the command: BLE_BOND_CMD: [8 BLE_BOND_CMD 1 CRAP_MAC]*/


  /* First insert the headers */

  buf[0] = 2;  /* len payload */
  buf[1] = 0xa2;
  buf[2] = ctrl; /*  */

  err = ioctl(fd_ble, CC2540_BLE_GENERALCMD, buf);
  if (err != 0)
    {
      LOGE("*** BLENative_sendControlByte Failed (0x%x) ***", err);
      return com_reconinstruments_reconble_ReconBLE_ERR_FAIL;
    }
    
  return com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS;   // success on initializing pairing; 

  
}

/*
 * Class:     com_reconinstruments_reconble_BLENative
 * Method:    sendSpecialCommand
 * Signature: (IB)I
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_sendSpecialCommand
(JNIEnv *env, jobject this, jint context, jbyte ctrl)
{
  int fd_ble = (int)context;
  jbyte buf[1]; 
  int err = com_reconinstruments_reconble_ReconBLE_ERR_SUCCESS;

  /* First insert the headers */

  buf[0] = ctrl; 
  err = ioctl(fd_ble, CC2540_BLE_SPCCMD, buf);
  if (err != 0)
    {
      LOGE("*** BLENative_sendControlByte Failed (0x%x) ***", err);
      return com_reconinstruments_reconble_ReconBLE_ERR_FAIL;
    }
    

}


/*
 * Class:     com_reconinstruments_reconble_BLENative
 * Method:    newCheckStatus
 * Signature: (II[B)I
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_newCheckStatus
(
 JNIEnv* env,       // environment ponter
 jobject jthis,     // parent stub java class
 jint    context,    // session pointer
 jint withPolling,
 jbyteArray theStatus
 )
{
  //LOGV("newCheckStatus");
  int fd_ble = (int)context;
  struct pollfd fds;
  fds.events = POLLIN;
  fds.fd = fd_ble;
  //LOGD("Polling");
  if (withPolling) {
    int ret=poll (&fds, 1, -1) ; //poll for infinity
  }
  jbyte buffer[6];
  int err = ioctl(fd_ble, CC2540_BLE_GET_STATUS, buffer);
  (*env)->SetByteArrayRegion(env, theStatus, 0,6, buffer);
  //LOGD("CHECK_STATUS","CHECK STATUS");
  if (err != 0)
    {
      LOGE("*** BLENative_checkStatus: CC2540_BLE_GET_STATUS Failed (0x%x) ***", err);
      return com_reconinstruments_reconble_ReconBLE_ERR_FAIL;
    }

  return buffer[0];
}

/*
 * Class:     com_reconinstruments_reconble_BLENative
 * Method:    noninvasiveCheckStatus
 * Signature: (I[B)I
 */
JNIEXPORT jint JNICALL Java_com_reconinstruments_reconble_BLENative_noninvasiveCheckStatus
(JNIEnv* env,
 jobject jthis,
 jint context,
 jbyteArray theStatus) {

  LOGV("nonInvasiveCheckStatus");
  int fd_ble = (int)context;
  jbyte buffer[2];
  int err = ioctl(fd_ble, CC2540_BLE_GET_STREAM_INDEX, buffer);
  (*env)->SetByteArrayRegion(env, theStatus, 0,2, buffer);
   if (err != 0)
    {
      LOGE("*** BLENative_checkStatus: CC2540_BLE_GET_STATUS Failed (0x%x) ***", err);
      return com_reconinstruments_reconble_ReconBLE_ERR_FAIL;
    }

  return buffer[0];
}

