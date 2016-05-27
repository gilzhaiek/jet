

//#ifdef CUSTOM_CODES
					/*** Status Updates  ***/
	#define SESS_START	 				"0x00000000"  //A new AGPS session starts. 
	#define SESS_END	 				"0x00000001"  //An AGPS session ends
	#define SESS_QOP	 				"0x00000002"  //Session QoP (single or periodic positioning)
	#define SESS_POS_RESULT				"0x00000003"  //Positioning result 
	#define SESS_EPH_AIDING				"0x00000004"  //Auxiliary information sent to GPS receiver 
	#define SESS_AGPS_SW_VERNO			"0x00000005"  //AGPS software version number
	#define SESS_NW_CONN_SUCCESS		"0x01000000"  //Network connection successful 
	#define SESS_SERVER_CONN_SUCCESS    "0x01000001"  //Server connection successful 
	#define SESS_OTA_MSG_TYPE			"0x02000000"  //OTA message type
					/*** Warning Messages   ***/
	#define SESS_WARN_SET_INIT			"0x10000000"  //SET-Initiated session, no network connection, default to standalone.
	#define SESS_WARN_UNKWN_OTA_MSG		"0x11000000"  //Unknown OTA message received,  discard.
	#define SESS_WARN_UNKWN_GPS_MSG		"0x12000000"  //Unknown GPS message received,  discard.
	#define SESS_WARN_GPS_POS			"0x13000000"  //GPS cannot produce a position within resp_time.
					/*** ERROR Messages   ***/
	#define SESS_ERR_UNEXP_OTA_MSG		"0x20000000"  //Unexpected OTA message received. FSM out-of-sync
	#define SESS_ERR_SET_INIT			"0x20000001"  //SET-Initiated Session: wrong input configuration
	#define SESS_ERR_NW_CONN_FAIL		"0x21000000"  //Network Connection Failure
	#define SESS_ERR_SERVER_CONN_FAIL	"0x21000001"  //Server connection failure
	#define SESS_ERR_CONN_GPS_LOST		"0x21000002"  //Connection to GPS lost during AGPS session, or at GPS reset.
	#define SESS_ERR_TLS_FAIL			"0x21000003"  //TLS Failure during SUPL
	#define SESS_ERR_SUPL_END			"0x22000000"  //SUPL END message with error status code received.
	#define SESS_ERR_OTA_MSG_TIME_OUT	"0x22000001"  //Time-out for OTA message (SUPL message). 
	#define SESS_ERR_DECODE_OTA_MSG		"0x22000002"  //OTA message (SUPL message) decoding error.
	#define SESS_ERR_GPS_MSG_TIME_OUT	"0x23000000"  //Time-out for message sent by GPS receiver. 
//#else







//#endif
	
	
	
	