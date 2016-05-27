This code demonstrates how 3rd party can utilize JET sensor framework
together with RECON extensions of Android Sensor API to detect and process
Jump Events
---------------------------------------------
Prerequisites

  1) Java JAR file implementing Recon Sensor API. It can be built independently from ($JET)/mydroid:
     
	 ./build_module.sh com.reconinstruments.reconsensor
	 
     Android build system will install it in /system/framework/com.reconinstruments.reconsensor.jar
	 
  2) Native shared library  ("libreconsensor.so"). This is JNI layer
     used by reconsensor.jar. It can be built independently from ($JET)/mydroid: 
	 
	 ./build_module.sh libreconsensor
	 
	 Android build system will install it in  /system/lib/libreconsensor.so
	 
(If there are problems with building of either of above 2 pieces, talk to Gil)

--------------------------------------------------------------------

Following java source files from this package need to be dropped as part
of source code of client that utilizes the framework:

       --  JumpAnalyzer.java
	   --  JumpDebugLog.java
	   --  JumpEngine.java
	   
	   NOTE: JumpEndEvent.java and ReconJump.java are here for completeness
	         These files already exist as part of transcend source code
			 
	
Main Driver that demonstrates how client can utilize such framework is standalone
"JumpActivity.java". This is rough equivalent of ReconTranscendService.java. Key points are:

* Instantiation of JumpAnalyzer:
(Hooks up pieces of Recon Sensor Framework listed under 1) and 2). 

* Registration for FreeFall Event, via standard Android API

(If either of these 2 steps fails, client code MUST proceed under the 
 assumption that Jump Feature is not available)

* Upon receipt of FreeFall event, disable FreeFall Listener and start JumpAnalyzer
  passing following parameters:
     -- Last pressure measurement
	 -- Timestamp of last pressure measurement
	 -- FreeFall Event detection timestamp
				 
* Upon receipt of Landing Event from JumpAnalyzer, process ReconJump (if not null)
  and re-register for FreeFall Event
					 