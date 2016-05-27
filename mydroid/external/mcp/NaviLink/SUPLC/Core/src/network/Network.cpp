/*
******************************************************************
* COPYRIGHT © Teleca AB                                          *
*----------------------------------------------------------------*
* MODULE     : Network.cpp                                       *
*                                                                *
* PROGRAMMER : Dmitriy Kardakov                                  *
* DATE       : 28 Feb 2009                                       *
* VERSION    : 1.0                                               *
*                                                                *
*----------------------------------------------------------------*
* MODULE SUMMARY : INetwork class and its subclasses             *
*                  implementation. Provides functions for working*
*                  with TLS connection and starting the SMS/WAP  *
*                  -push listen.                                 *
*----------------------------------------------------------------*
* MODIFICATION RECORDS                                           *
* Alexander V. Morozov                                           *
* Ildar Abdullin   - 23 Nov 2007 - Initial version               *
* Dmitriy Kardakov - 28 Feb 2009 - Porting to Android            *
******************************************************************
*/
#include "suplcontroller/SUPLController.h"
#include "network/supl_hash.h"
#include "network/Network.h"
#include "common/types.h"

using namespace Engine;
using namespace Platform;
#define SHA1CircularShift(bits,word) \
                ((((word) << (bits)) & 0xFFFFFFFF) | \
                ((word) >> (32-(bits))))

INetwork* INetwork::NewNetworkIF()
{
#if defined(_LINUX_)
    return new(std::nothrow) CLinNetwork;
#endif
}
#if defined(_LINUX_)
/*
*******************************************************************
* Function: TLS_CreateConnection
*
* Description : Creates a secure connection to the specified port 
* number at the specified IP address and starts the thread receiving 
* messages from SUPL server using secure connection. See CLinTLSCon 
* class for details.
*
* Parameters : 
* host_port  r null-terminated string with a remote host IP address 
* and port number (host:port)
* Returns : -1 if an I/0 error occurs, 0 - otherwise.
*
*******************************************************************
*/
int CLinNetwork::TLS_CreateConnection(char *host_port)
{


    return tls.CreateConnection(host_port);

    
    /* start continuous listen in the thread */
    //return tls.Recieve();
}
/*
*******************************************************************
* Function: TLS_Send
*
* Description : Sends size bytes from the specified data buffer to 
* SUPL Server using secure connection. Method is blocked until all
* data has been sent. See CLinTLSCon class for details.
*
* Parameters : 
* buffer   r data buffer from which bytes are to be sent
* size     r the number of bytes to write
* Returns : 0 if all size bytes are sent, -1 if some I/O error occurs.
*
*******************************************************************
*/
int CLinNetwork::TLS_Send(void *buffer, Engine::uint32_t size)
{
    return tls.Send(buffer, size);
}

/*
*******************************************************************
* Function: TLS_IsActive
*
* Description : Checks the secure network connection current status. 
* See CLinTLSCon class for details.
*
* Parameters : None
* Returns : 0 - connection is active, otherwise, -1 - network 
* connection is not reachable.
*
*******************************************************************
*/
int CLinNetwork::TLS_IsActive()
{
    return tls.IsActive();
}

/*
*******************************************************************
* Function: TLS_CloseConnection
*
* Description : Closes a secure network connection. 
*               See FreeConnection method implementation of 
*               CLinTLSCon class for details.

* Parameters : None
* Returns : None
*
*******************************************************************
*/
void CLinNetwork::TLS_CloseConnection()
{
    tls.FreeConnection();
}

/*
*******************************************************************
* Function: CMN_IsDataExist
*
* Description : Checks if the SUPL controller message queue is not 
* empty. Function copies the next message to the raw_data data buffer if 
* queue is not empty and raw_data is non-null pointer.
*
* Parameters :
* raw_data w data buffer for data storing.
* Returns : 0 if queue has no messages, otherwise, the 
* size of message actually stored in raw_data buffer.
*
*******************************************************************
*/
int CLinNetwork::CMN_IsDataExist(CNetworkComponentBase::RawData_t *raw_data)
{
    CNetworkComponentBase::Lock();
    if(CNetworkComponentBase::nw_queue.empty())
    {
        CNetworkComponentBase::UnLock();
        return 0;
    }
	
	if (raw_data == NULL)
	{
		return (CNetworkComponentBase::nw_queue.front())->size;
	}
	
    raw_data->RawData = 
              new(nothrow) 
              unsigned char[(CNetworkComponentBase::nw_queue.front())->size];
	
    if(!raw_data->RawData)
    {
		delete CNetworkComponentBase::nw_queue.front();
		raw_data->RawData = NULL;
        CNetworkComponentBase::nw_queue.pop();
        CNetworkComponentBase::UnLock();
        return 0;
    }
    
    memcpy(raw_data->RawData,
          (CNetworkComponentBase::nw_queue.front())->RawData,
          (CNetworkComponentBase::nw_queue.front())->size);

    raw_data->size = (CNetworkComponentBase::nw_queue.front())->size;
    delete CNetworkComponentBase::nw_queue.front();
    CNetworkComponentBase::nw_queue.pop();
    CNetworkComponentBase::UnLock();
    return raw_data->size;
}
/*
*******************************************************************
* Function: LSR_StartListen
*
* Description : Enables the SMS/WAP-push listening.

* Parameters : None
* Returns : 0 - if operation is successful, otherwise, the negative 
* error code.
*
*******************************************************************
*/
int CLinNetwork::LSR_StartListen()
{
    return listener.StartListen();
}

/*
*******************************************************************
* Function: TLS_Init
*
* Description : Initializes a network. See CLinTLSCon class for 
* details.
*
* Parameters : None
*
* Returns : 0 if operation is successful, and -1 otherwise.
*
*******************************************************************
*/
int CLinNetwork::TLS_Init()
{
    if(tls.Init() != 0)
    {
        return -1;
    }
    return 0;
}

/*
*******************************************************************
* Function: HandleMessage
*
* Description : Posts message to SUPL controller queue for 
* processing.
*
* Parameters :
* body  r pinter to message data pointer.
* size  r message size.
* Returns : 0 if operation is successful and -1 if some error occurs.
*******************************************************************
*/
int CLinNetwork::HandleMessage(const char *body, int size)
{
    if (body == NULL || size < 0)
    {
        return -1;
    }
    
    CNetworkComponentBase::Lock();

    if(CSUPLController::GetInstance().SUPLStart() != TRUE)
    {
        CNetworkComponentBase::UnLock();
        return -1;
    }
    CNetworkComponentBase::UnLock();
    /*Copy raw data to SUPL queue */
    CNetworkComponentBase::RawData_t* data = new(nothrow) 
                                             CNetworkComponentBase::RawData_t;
    if(!data)
    {
        return -1;
    }
    
    data->RawData = new(nothrow) unsigned char[size];
    if(!data->RawData)
    {
        delete data;
        return -1;
    }
    memcpy(data->RawData, body, size);
    data->size = size;
    CNetworkComponentBase::Lock();
    CNetworkComponentBase::nw_queue.push(data);
    CNetworkComponentBase::UnLock();
    return 0;
}

/*
*******************************************************************
* Function: CMN_HMAC
*
* Description : SHA function. Is't not implemented yet.
*
* Parameters :
* Returns :
*******************************************************************
*/
void SHA1ProcessMessageBlock(SHA1Context *context)
{

const unsigned K[] =            /* Constants defined in SHA-1   */
    {
        0x5A827999,
        0x6ED9EBA1,
        0x8F1BBCDC,
        0xCA62C1D6
    };
    U16         t;                  /* Loop counter                 */
    U32    temp;               /* Temporary word value         */
    U32    W[80];              /* Word sequence                */
    U32    A, B, C, D, E;      /* Word buffers                 */

    /*
     *  Initialize the first 16 words in the array W
     */
    for(t = 0; t < 16; t++){
        W[t] = ((unsigned) context->Message_Block[t * 4]) << 24;
        W[t] |= ((unsigned) context->Message_Block[t * 4 + 1]) << 16;
        W[t] |= ((unsigned) context->Message_Block[t * 4 + 2]) << 8;
        W[t] |= ((unsigned) context->Message_Block[t * 4 + 3]);
    }

    for(t = 16; t < 80; t++)
       W[t] = SHA1CircularShift(1,W[t-3] ^ W[t-8] ^ W[t-14] ^ W[t-16]);

    A = context->Message_Digest[0];
    B = context->Message_Digest[1];
    C = context->Message_Digest[2];
    D = context->Message_Digest[3];
    E = context->Message_Digest[4];

    for(t = 0; t < 20; t++){
        temp =  SHA1CircularShift(5,A) +
                ((B & C) | ((~B) & D)) + E + W[t] + K[0];
        temp &= 0xFFFFFFFF;
        E = D;
        D = C;
        C = SHA1CircularShift(30,B);
        B = A;
        A = temp;
    }

    for(t = 20; t < 40; t++){
        temp = SHA1CircularShift(5,A) + (B ^ C ^ D) + E + W[t] + K[1];
        temp &= 0xFFFFFFFF;
        E = D;
        D = C;
        C = SHA1CircularShift(30,B);
        B = A;
        A = temp;
    }

    for(t = 40; t < 60; t++){
        temp = SHA1CircularShift(5,A) +
               ((B & C) | (B & D) | (C & D)) + E + W[t] + K[2];
        temp &= 0xFFFFFFFF;
        E = D;
        D = C;
        C = SHA1CircularShift(30,B);
        B = A;
        A = temp;
    }

    for(t = 60; t < 80; t++){
        temp = SHA1CircularShift(5,A) + (B ^ C ^ D) + E + W[t] + K[3];
        temp &= 0xFFFFFFFF;
        E = D;
        D = C;
        C = SHA1CircularShift(30,B);
        B = A;
        A = temp;
    }

    context->Message_Digest[0] =(context->Message_Digest[0] + A) & 0xFFFFFFFF;
    context->Message_Digest[1] =(context->Message_Digest[1] + B) & 0xFFFFFFFF;
    context->Message_Digest[2] =(context->Message_Digest[2] + C) & 0xFFFFFFFF;
    context->Message_Digest[3] =(context->Message_Digest[3] + D) & 0xFFFFFFFF;
    context->Message_Digest[4] =(context->Message_Digest[4] + E) & 0xFFFFFFFF;

    context->Message_Block_Index = 0;

}

void SHA1Input(SHA1Context *context, const U8 *message_array, U32 length){

if (!length)
        return;

    if (context->Computed || context->Corrupted)
    {
        context->Corrupted = 1;
        return;
    }

    while(length-- && !context->Corrupted){
        context->Message_Block[context->Message_Block_Index++] =(*message_array & 0xFF);

        context->Length_Low += 8;
        /* Force it to 32 bits */
        context->Length_Low &= 0xFFFFFFFF;
        if (context->Length_Low == 0)
        {
            context->Length_High++;
            /* Force it to 32 bits */
            context->Length_High &= 0xFFFFFFFF;
            if (context->Length_High == 0)
            {
                /* Message is too long */
                context->Corrupted = 1;
            }
        }

        if (context->Message_Block_Index == 64){
            SHA1ProcessMessageBlock(context);
            context->Message_Block_Index = 0;

        }
        message_array++;
    }

}


void SHA1PadMessage(SHA1Context *context)
{
	/*
		*  Check to see if the current message block is too small to hold
		*  the initial padding bits and length.  If so, we will pad the
		*  block, process it, and then continue padding into a second
		*  block.
		*/
	   if (context->Message_Block_Index > 55){
	
		   context->Message_Block[context->Message_Block_Index++] = 0x80;
		   while(context->Message_Block_Index < 64)
			   context->Message_Block[context->Message_Block_Index++] = 0;
	
		   SHA1ProcessMessageBlock(context);
	
		   while(context->Message_Block_Index < 56)
			   context->Message_Block[context->Message_Block_Index++] = 0;
	   }
	   else{
		   context->Message_Block[context->Message_Block_Index++] = 0x80;
		   while(context->Message_Block_Index < 56)
			   context->Message_Block[context->Message_Block_Index++] = 0;
	   }
	
	   /*
		*  Store the message length as the last 8 octets
		*/
	   context->Message_Block[56] = 0x00;
	   context->Message_Block[57] = 0x00;
	   context->Message_Block[58] = 0x00;
	   context->Message_Block[59] = 0x00;
	   context->Message_Block[60] = 0x00;
	   context->Message_Block[61] = 0x00;
	   context->Message_Block[62] = 0x00;
	   context->Message_Block[63] = 0x00;
	
	   context->Message_Block[56]|=((context->Length_High >> 24));
	   context->Message_Block[57]|=((context->Length_High >> 16));
	   context->Message_Block[58]|=((context->Length_High >> 8));
	   context->Message_Block[59]|=(context->Length_High);
	   context->Message_Block[60]|=((context->Length_Low >> 24));
	   context->Message_Block[61]|=((context->Length_Low >> 16));
	   context->Message_Block[62]|=((context->Length_Low>> 8));
	   context->Message_Block[63]|=(context->Length_Low);
	
	   SHA1ProcessMessageBlock(context);

}

U8 SHA1Result(SHA1Context *context)
{

	if (context->Corrupted)
			return 0;
	
		if (!context->Computed)
		{
			SHA1PadMessage(context);
			context->Computed = 1;
		}
	
		return 1;
}
void SHA1Reset(SHA1Context *context)
{
	context->Length_Low 			= 0;
		context->Length_High			= 0;
		context->Message_Block_Index	= 0;
	
		context->Message_Digest[0]		= 0x67452301;
		context->Message_Digest[1]		= 0xEFCDAB89;
		context->Message_Digest[2]		= 0x98BADCFE;
		context->Message_Digest[3]		= 0x10325476;
		context->Message_Digest[4]		= 0xC3D2E1F0;
	
		context->Computed	= 0;
		context->Corrupted	= 0;

}

void sha1(U8 *fqdn, U8 *msg, U32 *MessageDigest, U32 length)
{
	  SHA1Context sha;
	  U8 i;
	
	  SHA1Reset(&sha);
	  SHA1Input(&sha, fqdn,64);
	  SHA1Input(&sha, msg,length);
	
	  if (!SHA1Result(&sha))
			LOGD("error\n");
	  else{
		for(i=0;i<5;i++)
		  MessageDigest[i] = sha.Message_Digest[i];
	  }
}

void movebits(U32 * source, U8 * destination )
{
	U8 i, j, count;
	U32 temp;
	
	count=0;
	for(i=0;i<5;i++){
	  for(j=4;j>0;j--){
		destination[count]=0x00;
		temp = source[i];
		temp>>=(8*(j-1));
		destination[count++]|=(U8)temp;
		}
	  }
}


void CLinNetwork::generating_hash(const char * msg,int length,unsigned char* output, const char* fqdn_id)
{
  
  	  U8 *temp = (U8 *)malloc(length);//length is greater than U16 so malloc is used
	  U32 MessageDigest[5];
	  /* www.spirent-lcs.com: fqdn for the ULTS */
	  U8 K[64] = "\0,";
	  U8 temp1[64];
	  U8 c[20];
	  U8 i;
  	  memcpy(K, fqdn_id, strlen(fqdn_id));
	  memcpy(temp, msg, length);
	  for(i=strlen(fqdn_id);i<64;i++)
		K[i] = 0x00;
  
	  for(i=0;i<64;i++)
		  temp1[i]= K[i] ^ 0x36;
  
	  sha1(temp1,temp,MessageDigest, length);
	  movebits(MessageDigest, c);
	  for(i=0;i<64;i++)
		  temp1[i] = K[i] ^ 0x5c;
	  sha1(temp1,c,MessageDigest, 20);
	  movebits(MessageDigest, c);
	  for(i=0;i<8;i++)
		output[i] = c[i];
	  free(temp);
	  return;
  
}


unsigned char* CLinNetwork::CMN_HMAC(const void *key, int key_len, 
                                     unsigned char *data, int data_len)
{
 	int i;
    /*Now wrapper only */
	unsigned char *u_Hash;
	u_Hash = (unsigned char*)malloc(8);
	
	CLinNetwork::generating_hash((const char*)data,data_len,u_Hash, (const char*)key);
	
	for(i=0;i<8;i++)
		LOGD("Calculated hash is %x", u_Hash[i]);

    return u_Hash;
}

#endif //_LINUX_
