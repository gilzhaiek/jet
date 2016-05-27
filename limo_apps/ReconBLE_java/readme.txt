Basic ReconBLE API documentation
--------------------------------

The project is composed of 2 packages:
   1) com.reconinstruments.reconble     --- BLE API Library
   2) com.reconinstruments.bletest      --- "Driver" : Sample usage of BLE API. 
   

Native Functionality
--------------------
* implemented inside libRecon-BLE.so shared library. See "jni" folder
* cc2540_ble.h is driver header file. Right now this is separate copy which makes updates difficult
* You can run Android "ndk-build" (in root folder) to build "so" file
* You can run "build_header.bat" batch script that builds native headers on which the implementation depends
* ReconBLE.h:  Constants (from ReconBLE.java)      BLENative.h: Function prototypes (from BLENative.java)
* Android will unpack "so" file in  <app root>/data/data/ folder. Once debugged I recommend to move it permanently to /system/lib
  
  BLE API
----------
* Extensively documented inside Java source files; 
* Client ("BLETestActivity" in this case) interface is ReconBLE class. No other package parts need to be used directly
* Supports 3 main Asynchronous Tasks:  Connection Monitoring + Outgoing (Tx) + Incoming (Rx)
* Current internal Implementation is Polling Based. This is inefficient due to expensive User/Kernel
  switch that must be performed in tight loop. Need to move to IRQ based
* Although ReconBLE is singleton within calling Task, it does not guarantee (required) system-wide
  single instance. Thus only client of this API is probably some sort of BLE Service. Details TBA
* This is likely only partial, 1st stage implementation -- as I have not worked in Master mode.

"Driver" (Test Activity)
------------------------
* Single Java file, comments inside -- self explanatory
* BLE Service can use it as starting point
* Due to plenty of bugs on BLE layer (Kernel --- iPhone) right now it is structured as Unit Test app
  with various parts of API attached to buttons.
  