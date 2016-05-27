#include "mcpf_nal.h"

/* For Socket Programming. */
#include <sys/ioctl.h>
#include <string.h>
#include <ctype.h>
#include <unistd.h>
#include <stdlib.h>
#include <signal.h>
#include <sys/wait.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <netdb.h>
#include <netinet/in.h>
#include <utils/Log.h>

#define ENABLE_NAL_DEBUG

/* #define ENABLE_NAL_DEBUG */
#ifdef ENABLE_NAL_DEBUG
    #define LOG_TAG "mcpf_nal"
    #include <utils/Log.h>
    #define  DEBUG_MCPF_NAL(...)   LOGD(__VA_ARGS__)
#else
    #define  DEBUG_MCPF_NAL(...)   ((void)0)
#endif /* ENABLE_NAL_DEBUG */

#define HS_PORT 4000
#define HS_HOST_ADDR "localhost"

/*TI_PATCH start*/
#define SOC_NAME_4000             "\0/org/gps4000"
/*TI_PATCH start*/



#define MAX_LENGTH 256

static int connectWithHelperService(const int u16_inPortNumber, const int *const p_inHostName);

static int sendDataToHelperService(void *p_buffer, int size, eNetCommands cmd);



static int          g_socketDescriptor=0;              /* */


EMcpfRes NAL_connectToHS()
{
	int retVal = 0;
    retVal = connectWithHelperService(HS_PORT, HS_HOST_ADDR);
    if (0 != retVal)
    {
        DEBUG_MCPF_NAL(" NAL_connectToHS: connectWithHelperService FAILED !!! \n");
        return RES_ERROR;
    }

	return RES_COMPLETE;

}

EMcpfRes NAL_closeConnectionToHS()
{
	//close(g_socketDescriptor);
	//g_socketDescriptor= 0;
	return RES_COMPLETE;
}


EMcpfRes NAL_executeCommand(eNetCommands cmd,void *data)
{
    s_NAL_CmdParams s_nalCmdParams;
    s_NAL_subscriberId *p_subscriberId = NULL;
	int *p_hslp = NULL;

    int retVal = 0;

	DEBUG_MCPF_NAL(" +NAL_executeCommand: Zoom2!!! \n");



    s_nalCmdParams.e_nalCommand = cmd;
    switch(cmd)
    {
        case MCPF_NAL_TLS_SEND:
        {
            if (data == NULL)
            {
                DEBUG_MCPF_NAL(" NAL_executeCommand*: data NULL !!! \n");
            }
            else
            {
                memcpy((void *)&s_nalCmdParams.u_payload, data, sizeof(s_NAL_slpData) );
            }
        }
        break;

        case MCPF_NAL_TLS_CREATE_CONNECTION:
        {
            if (data == NULL)
            {
                DEBUG_MCPF_NAL(" NAL_executeCommand: data NULL !!! \n");
            }
            else
            {
                s_NAL_createConnection *p_data = NULL;
                p_data  = (s_NAL_createConnection *)data;

                DEBUG_MCPF_NAL(" NAL_executeCommand: a_hostPort = %s \n", p_data->a_hostPort);


                DEBUG_MCPF_NAL(" NAL_executeCommand: FAILED IN!!!!!!!!!!!!!!!!!!! \n");
                memcpy((void *)&s_nalCmdParams.u_payload, data, sizeof(s_NAL_createConnection) );
                DEBUG_MCPF_NAL(" NAL_executeCommand: FAILED OUT!!!!!!!!!!!!!!!!!!! \n");
            }
        }
        break;
		case MCPF_NAL_MSGBOX:
  		{
            if (data == NULL)
            {
                DEBUG_MCPF_NAL(" NAL_executeCommand: data NULL !!! \n");
            }
            else
            {
			    memcpy((void *)&s_nalCmdParams.u_payload, data, sizeof(s_NAL_msgBox) );
            }  
	    }
		break;
        default:
        {
   				DEBUG_MCPF_NAL(" NAL_executeCommand: UNKNOWN CMD [%d] \n", cmd);
        }
        break;

    }

    retVal = sendDataToHelperService((void *)&s_nalCmdParams,
                                     sizeof(s_nalCmdParams),
                                     s_nalCmdParams.e_nalCommand  );
    if (0 > retVal)
    {
        DEBUG_MCPF_NAL(" NAL_executeCommand: sendDataToHelperService FAILED !!! \n");
        return RES_ERROR;
    }

    /* APK: Temporary Hack. */
    if (MCPF_NAL_GET_SUBSCRIBER_ID == s_nalCmdParams.e_nalCommand)
    {
        /* In this case, 'data' is an out parameter. Handle it accordingly. */

        p_subscriberId = (s_NAL_subscriberId *)data;

        if (!p_subscriberId->a_imsiData || !s_nalCmdParams.u_payload.s_subscriberId.a_imsiData)
        {
            DEBUG_MCPF_NAL(" NAL_executeCommand: sendDataToHelperService NULL ptr \n");
            return RES_ERROR;
        }

        memcpy((void *)p_subscriberId->a_imsiData,
               (void *)s_nalCmdParams.u_payload.s_subscriberId.a_imsiData,
               sizeof(p_subscriberId->a_imsiData));
        p_subscriberId->s32_size = s_nalCmdParams.u_payload.s_subscriberId.s32_size;

        DEBUG_MCPF_NAL(" NAL_executeCommand: s32_size = %d \n", p_subscriberId->s32_size);

    }

    if (MCPF_NAL_GET_CELL_INFO == s_nalCmdParams.e_nalCommand)
    {
        s_NAL_GsmCellInfo *p_gsmCellInfo = NULL;
        McpU8 count = 0;


        /* In this case, 'data' is an out parameter. Handle it accordingly. */
        p_gsmCellInfo = (s_NAL_subscriberId *)data;

        for (count = 0; count < MAX_CELL_PARAMS; count++)
        {
            p_gsmCellInfo->a_cellInfo[count] = s_nalCmdParams.u_payload.s_cellInfo.a_cellInfo[count];
            DEBUG_MCPF_NAL(" NAL_executeCommand:  a_cellInfo[%d] is %d \n", count,
                                                        p_gsmCellInfo->a_cellInfo[count]);
        }


    }

	if (MCPF_NAL_GET_HSLP== s_nalCmdParams.e_nalCommand)
	{


		p_hslp = (int*)data;
        if (p_hslp==NULL)
        {
            DEBUG_MCPF_NAL(" NAL_executeCommand: sendDataToHelperService NULL ptr \n");
            return RES_ERROR;
        }

        memcpy((void *)p_hslp,
               (void *)s_nalCmdParams.u_payload.s_hslpData.a_hslpData,
               sizeof(int)*MAX_HSLP_PARAMS);
          
        //DEBUG_MCPF_NAL(" NAL_executeCommand: MCPF_NAL_GET_HSLP - s32_size = %d, mnc =%d, mcc =%d \n", p_hslp->s32_size,p_hslp->a_hslpData[0],p_hslp->a_hslpData[1]);
    }	

	if (MCPF_NAL_TLS_IS_ACTIVE== s_nalCmdParams.e_nalCommand)
	{
		int *p_isActive=(int *)data;
		*p_isActive=retVal;
		
	}

    DEBUG_MCPF_NAL(" NAL_executeCommand: Exiting. Received cmd = %d from SUPL Core \n", cmd);
    return RES_COMPLETE;
}

static int sendDataToHelperService(void *p_buffer, int size, eNetCommands cmd)
{
    char read_buffer[MAX_LENGTH] = {'\0'};
	int retVal = 0;
    DEBUG_MCPF_NAL(" sendDataToHelperService: Entering \n");

    /* Just in case... */
    if (g_socketDescriptor == 0)
    {
        DEBUG_MCPF_NAL(" sendDataToHelperService: Connection with MCPF not yet established. \n");
        return -1;
    }

    /* Send the command to MCPF. */
    if ( send(g_socketDescriptor, p_buffer, size, 0  ) < 0)
    {
        
		g_socketDescriptor=0;
		retVal = connectWithHelperService(HS_PORT, HS_HOST_ADDR);
    	if (0 != retVal)
    	{
	        DEBUG_MCPF_NAL(" NAL_executeCommand: connectWithHelperService FAILED !!! \n");
	        return -2;
    	}
		 if ( send(g_socketDescriptor, p_buffer, size, 0  ) < 0)
    	{
        DEBUG_MCPF_NAL(" sendDataToHelperService: Message Sending FAILED !!! \n");
			g_socketDescriptor=0;
        return -2;
		 }
    }

#if 1
    DEBUG_MCPF_NAL(" ===>>> sendDataToHelperService: Blocked on read \n");
    if ( read(g_socketDescriptor, read_buffer, MAX_LENGTH) < 0)
    {
        DEBUG_MCPF_NAL(" sendDataToHelperService: Message reading FAILED !!! \n");
		g_socketDescriptor=0;
        return -3;
    }
    /* APK: Temporary Hack to handle SUBSCRIBER_ID request. */
    if ( ( cmd == MCPF_NAL_GET_SUBSCRIBER_ID) ||( cmd == MCPF_NAL_GET_HSLP) ||
         (cmd == MCPF_NAL_GET_CELL_INFO) )
    {
        s_NAL_CmdParams *p_nalCmdParams = NULL;

        p_nalCmdParams = (s_NAL_CmdParams *)p_buffer;
        memcpy((void *)&p_nalCmdParams->u_payload, (void *)read_buffer, sizeof(p_nalCmdParams->u_payload) );
        return 0;
    }

    DEBUG_MCPF_NAL(" ===>>> sendDataToHelperService: Response received. read_buffer = %s === %d \n", read_buffer, atoi(read_buffer));
    if ( atoi(read_buffer) < 0)
    {
        DEBUG_MCPF_NAL(" sendDataToHelperService: Response FAILED !!! \n");
        return -4;
    }

    if ( atoi(read_buffer) > 0)
    {
        DEBUG_MCPF_NAL(" sendDataToHelperService: Response SUCCESS !!! \n");
        return 1;

    }
#endif

    return 0;
}

static int connectWithHelperService(const int u16_inPortNumber,
                                     const int *const p_inHostName)
{
	if(g_socketDescriptor==0)
	{
    /*TI_PATCH start*/
	//  struct sockaddr_in serverAddress;       /* Internet Address of Server. */
	    struct sockaddr_un serverAddress;       /* Internet Address of Server. */
	/*TI_PATCH end*/
    struct hostent *p_host = NULL;          /* The host (server) info. */
    int u16_sockDescriptor = 0;
    int retVal = 0;

    DEBUG_MCPF_NAL(" connectWithHelperService: Entering \n");

    /* Obtain host information. */
    p_host = gethostbyname((char *)p_inHostName);
    if (p_host == (struct hostent *) NULL )
    {
        DEBUG_MCPF_NAL(" connectWithHelperService: gethostbyname FAILED !!!");
        return -1;
    }

    /* Clear the structure. */
    memset( &serverAddress, 0, sizeof(serverAddress) );

    /* Set address type. */
    /*TI_PATCH start*/
	#if 0
    serverAddress.sin_family = AF_INET;
    memcpy(&serverAddress.sin_addr, p_host->h_addr, p_host->h_length);
    serverAddress.sin_port = htons(u16_inPortNumber);
	#else
	    serverAddress.sun_family = AF_UNIX;
	    strcpy(serverAddress.sun_path, SOC_NAME_4000);
	#endif
/*TI_PATCH end*/

    /* Create a new socket. */
    /*TI_PATCH start*/
    #if 0
    u16_sockDescriptor = socket(AF_INET, SOCK_STREAM, 0);
    #else
    u16_sockDescriptor = socket(AF_UNIX, SOCK_STREAM, 0);
    #endif
    /*TI_PATCH end*/
    if (u16_sockDescriptor < 0)
    {
        DEBUG_MCPF_NAL(" connectWithHelperService: Socket creation FAILED !!!");
        return -2;
    }

    /* Connect with MCPF. */
    retVal = connect(u16_sockDescriptor,
                     (struct sockaddr *)&serverAddress,
                     sizeof(serverAddress) );
    if (retVal < 0)
    {
        DEBUG_MCPF_NAL(" connectWithHelperService: Connection with MCPF FAILED !!! \n");
        return -3;
    }

    /* Maintain a global variable for keeping the socket descriptor. */
    g_socketDescriptor = u16_sockDescriptor;

    DEBUG_MCPF_NAL(" connectWithHelperService: Exiting Successfully. \n");
	}
	else
	{
		DEBUG_MCPF_NAL(" connectWithHelperService: ALREADY CONNECTED \n");
	}
    return 0;

}

