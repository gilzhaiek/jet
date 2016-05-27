package com.reconinstruments.bletest;

interface IBLEService {
  int getTemperature();
  boolean getIsMaster();
  boolean isConnected();
  byte[] getOwnMacAddress();
  boolean hasEverConnectedAsSlave();
  void setInMusicApp(boolean flag);
  boolean getInMusicApp();
  boolean ifCanSendXml();
  boolean ifCanSendMusicXml();
  int incrementFailedPushCounter();
  int sendControlByte(byte ctrl);
}

