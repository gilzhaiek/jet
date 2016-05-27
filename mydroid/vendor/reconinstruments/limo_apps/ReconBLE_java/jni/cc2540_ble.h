#ifndef __CC2540_BLE_H__
#define __CC2540_BLE_H__

#define BLE_CENTRAL_CMD 0xF0
#define BLE_PERIPHERAL_CMD 0xFE

#define BLE_CENTRAL_ACK 0xF0
#define STREM_ACK_CMD 0xF6 //stream rx ack
#define BLE_NEW_MASTER_CMD 0xFB//send new master mac address to remote
#define BLE_READ 0xFC
#define BLE_TERMINATE_CMD 0xFD
#define BLE_PERIPHERAL_ACK 0xFE
#define BLE_SWITCH_NORMAL_CMD 0//send ip the ble goggle switch to normal state now

//#define BLE_SWITCH_NORMAL_CMD 0xFF//send ip the ble goggle switch to normal state now



#define BLE_BOND_CMD 0xA0
//: 0, 1,2

#define BLE_CONNECT_CMD 0xA1
#define BLE_CTL_IOS_CMD 0xA2
#define BLE_SLAVESECURITYREQUEST_CMD 0xA3

#define BLE_REQ_SEND_CMD 0xA4
#define BLE_READY_RSP_CMD 0xA5
#define BLE_EOT_CMD 0xA6//end of sending



#define BLE_TX_RX_TEST_CMD 0xA8


#define BLE_SCAN_ADVERTISE_CMD 0xA9
#define BLE_WRITE_ATT_HANDLE_CMD 0xAA
#define BLE_SEND_MAC_CMD 0xAB
#define BLE_FIND_UUID_HANDLE_CMD 0xAC

#define BLE_CONNECTED_ACK 0xB0
#define BLE_NEW_PAIR_ACK 0xB1
#define BLE_IOS_CTL_ACK 0xB2
#define BLE_DISCONNECTED_ACK 0xB3

#define IP_READY_ACK 0xB4//IP ready to receive
#define IP_REQ_ACK 0xB5
#define IP_EOR_ACK 0xB6 //end of receiving


#define BLE_CANCEL_BONDING_ACK 0xB7
#define BLE_SCAN_STOP_ACK 0xB8
#define BLE_TEMP_ACK 0xB9


#define AD_DATA_ACK 0xBA //report the remote adver/scan data
#define BLE_OTHERS_CONNECTED_ACK 0xBB
#define BLE_OTHERS_DISCONNECTED_ACK 0xBC
#define BLE_GET_UUID_HANDLE_ACK 0xBD
#define BLE_NOTIFICATION_ACK 0xBE
#define BLE_READY_PAIR_ACK 0xBF 

#define BLE_CMD_NACK 0xC0
#define BLE_SEND_IP_NACK 0xC1
#define BLE_SCAN_ADVERT_STOP 0xC2
//////////////////////////////////////////////////////////////////

#define BLE_BLOCK_SIZE 57
#define BLOCK_NUM_MAX 18 
#define BUFFER_SIZE 1026//57*18
//packet header--------
#define ODD_BIT 0x20
#define ACK_BIT 0x40

#define SPI_FAIL       0x2000
#define EOR_BIT_REPEAT        0x1000
#define TX_RETRY              0x800
#define IP_NACK               0x400
#define IOS_ACK               0x200//iOS ack goggle's Tx data
#define TX_TO                 0x100
#define EOR_BIT               0x80
#define NOEOR_BIT             0x40
#define Rx_BIT                0x20
#define EOT_ACK               0x10
#define EOT_BIT               0x8
#define NOEOT_ACK             0x4
#define GOGGLE_ACK_NEEDED     0x2
#define Tx_BIT                0x1
//NOT

enum {BLE_INIT=0, BLE_IDLE=1, BLE_DISCONNECT=2,BLE_CONNECT=3,
          //switch to BLE_DISCONNECT--------------------
          REMOTE_LIST_READY=10,BLE_UNPAIR=11,BLE_REMOTE_CANCEL_BONDING=12,
          //switch to BLE_CONNECT--------
          BLE_MAC_READY=24,BLE_IRK_READY=25,BLE_MISC_DATA_READY=26,
          BLE_SCAN_DONE=27,
          //switch to BLE_IDLE----------------
          BLE_FAIL_ACTIVE=0xF1,//BLE_SPI_FAIL=0xF2,//BLE_CENTRAL_ACK,BLE_PERIPHERAL_ACK
          };
struct cc2540_ble_platform_data{
 int dc_gpio; //for temperaute reading event
 int dd_gpio; //for ble report and SPI data communication
 int reset_gpio;
};

//flashing command-----------------------
#define CC2540_BLE_IOCTL_BASE 'b' //

#define CC2540_BLE_READ_BYTE   _IOR(CC2540_BLE_IOCTL_BASE, 1, unsigned char *)
#define CC2540_BLE_GET_MAC _IOR(CC2540_BLE_IOCTL_BASE, 2,int)
#define CC2540_BLE_GET_IRK _IOR(CC2540_BLE_IOCTL_BASE, 3,int)
#define CC2540_BLE_DD_READ   _IOR(CC2540_BLE_IOCTL_BASE, 4, unsigned char *)
#define CC2540_BLE_GET_STATUS   _IOR(CC2540_BLE_IOCTL_BASE, 5, int)
#define CC2540_BLE_GET_REMOTE_LIST   _IOR(CC2540_BLE_IOCTL_BASE, 6, int)
#define CC2540_BLE_GET_MISC_DATA _IOR(CC2540_BLE_IOCTL_BASE, 7,int)
#define CC2540_BLE_GET_SHIP_TYPE _IOR(CC2540_BLE_IOCTL_BASE, 8,int)
#define CC2540_BLE_GET_OWN_MAC _IOR(CC2540_BLE_IOCTL_BASE, 9,int)
#define CC2540_BLE_GET_STREAM_INDEX   _IOR(CC2540_BLE_IOCTL_BASE, 10, int)

#define CC2540_BLE_WRITE_BYTE   _IOW(CC2540_BLE_IOCTL_BASE, 0, unsigned char *)
//#define CC2540_BLE_SPCCMD _IOW(CC2540_BLE_IOCTL_BASE, 1,int)
#define CC2540_BLE_NORMALCMD _IOW(CC2540_BLE_IOCTL_BASE, 2,int)
#define CC2540_BLE_EOT _IOW(CC2540_BLE_IOCTL_BASE, 3, int)
#define CC2540_BLE_SET_TXSHIP _IOW(CC2540_BLE_IOCTL_BASE, 4, int)
#define CC2540_BLE_GENERALCMD  _IOW(CC2540_BLE_IOCTL_BASE, 5,int)

#define CC2540_BLE_SPCCMD _IO(CC2540_BLE_IOCTL_BASE, 0)
#define CC2540_BLE_RESET_CMD _IO(CC2540_BLE_IOCTL_BASE, 1)
#define CC2540_BLE_DD_OUT   _IO(CC2540_BLE_IOCTL_BASE, 2)
#define CC2540_BLE_DD_IN   _IO(CC2540_BLE_IOCTL_BASE, 3 )
#define CC2540_BLE_CLEAR_CMD  _IO(CC2540_BLE_IOCTL_BASE, 5)
#define CC2540_BLE_DEBUG_INT_CMD  _IO(CC2540_BLE_IOCTL_BASE, 9)
#define CC2540_BLE_DEBUG_EXIT_CMD  _IO(CC2540_BLE_IOCTL_BASE, 10)

//#define CC2540_BLE_SEND _IO(CC2540_BLE_IOCTL_BASE, 1)
#endif /*__CC2540_BLE_H__*/

