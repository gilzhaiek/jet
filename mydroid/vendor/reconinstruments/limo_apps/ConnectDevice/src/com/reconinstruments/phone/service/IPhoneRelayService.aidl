package com.reconinstruments.phone.service;
interface IPhoneRelayService {
   int getHfpStatus();
   int getMapStatus();
   String getBluetoothDeviceName();
   boolean remoteConnectToHfpDevice(String macaddress);
   boolean remoteConnectToMapDevice(String macaddress);
   boolean remoteDisconnectHfpDevice();
   boolean remoteDisconnectMapDevice();
}
