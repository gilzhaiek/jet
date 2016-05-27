How to build:

delete /system/app/BLE_Connect.apk

Can't be building using normal ant build process due to the SS1 library. Must be built either using the jet build system (build_module) or eclipse.

If you use eclipse to build it, in Properties->Build Path->Order and Export: deselect Android Private Libraries (because it includes the SS1 jar)
then run install_and_run.sh


BLE documentation:
Connection via BLE is done with a single command, no pairing is necessary

after connecting, to request being notified of a specific value you have to get the attribute handle of the value
Normally a notification value is specified with a 16 bit characteristic id and a 16 bit service id externally.
To get the attribute handle from these two values in needs to be matched with the service information.

The BLE Utility app requests this information and extracts the handle after connection. 
It can find notification handles for 4 devices, the recon remote, wahoo kickr, cadence sensor and heart rate monitor.

Other applications can intercept all incoming BLE messages using the GenericAttributeClientManager, but can only determine the source based on the attribute handle and MAC address. 

Right now applications must hard code attribute handles, this is not ideal, attribute handles could potentially overlap, they are not unique identifiers. In this case it might be necessary to create a mapping of MAC addresses and handles to specific services and characteristics that is accessible to applications. This could be accessible via TheBLEService.


Ideally the UtilActivity that pairs with wahoo accessories should rely on TheBLEService, and not as much on the SS1 API. 
Then it can be converted into a simple automated process that can be performed from the WahooMonitor app.
This work was started in UtilActivity2, but it can't currently deal with getting services and notifications.
