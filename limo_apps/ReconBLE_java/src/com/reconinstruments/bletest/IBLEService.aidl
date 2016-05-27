package com.reconinstruments.bletest;
interface IBLEService {
  int getTemperature();
  boolean getIsMaster();
  boolean getIsMasterBeforeonCreate();	
  boolean isConnected();
  byte[] getOwnMacAddress();
  int getRemoteControlVersionNumber();	
  boolean hasEverConnectedAsSlave();
  void setInMusicApp(boolean flag);
  boolean getInMusicApp();
  boolean ifCanSendXml();
  boolean ifCanSendMusicXml();
  int incrementFailedPushCounter();
  int sendControlByte(byte ctrl);
  int pushIncrementalRib(String bytesString);
  String getiOSDeviceName();
  int pushXml(String xmlString);
  int getiOSRemoteStatus();

}

