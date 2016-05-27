package com.reconinstruments.ble_ss1.device;

import java.util.UUID;

import android.util.Log;

import com.stonestreetone.bluetopiapm.BluetoothAddress;
import com.stonestreetone.bluetopiapm.DEVM.RemoteDeviceProperties;
import com.stonestreetone.bluetopiapm.GATM.CharacteristicDefinition;
import com.stonestreetone.bluetopiapm.GATM.CharacteristicDescriptor;
import com.stonestreetone.bluetopiapm.GATM.CharacteristicProperty;
import com.stonestreetone.bluetopiapm.GATM.ServiceDefinition;
import com.stonestreetone.bluetopiapm.GATM.ServiceInformation;


public class BLEDevice {

	public static final String TAG = "BLEDevice";
	
	public String name;
	public BluetoothAddress address;
	public boolean connected;
	
	public ServiceDefinition[] services;

	enum DeviceClassEnum {
		REMOTE,
		CYCLING_SPEED_CADENCE,
		CYCLING_POWER_METER,
		HEART_RATE_MONITOR
	};
	class DeviceClass{
		public DeviceClass(DeviceClassEnum type,int serviceUUID, int characteristicUUID) {
			this.serviceUUID = serviceUUID;
			this.characteristicUUID = characteristicUUID;
			this.type = type;
		}
		int serviceUUID;
		int characteristicUUID;
		DeviceClassEnum type;
	}
	DeviceClass remote = new DeviceClass(DeviceClassEnum.REMOTE,0xFFE0,0xFFE1);
	DeviceClass cadence = new DeviceClass(DeviceClassEnum.CYCLING_SPEED_CADENCE,0x1816,0x2A5B);
	DeviceClass power = new DeviceClass(DeviceClassEnum.CYCLING_POWER_METER,0x1818,0x2A63);
	DeviceClass hrm = new DeviceClass(DeviceClassEnum.HEART_RATE_MONITOR,0x180D,0x2A37);
	
	DeviceClass[] devices = new DeviceClass[]{remote,cadence,power,hrm};
	
	public int notifyHandle = -1;
	
	DeviceClass deviceType;


	public BLEDevice(BluetoothAddress address) {
		this.address = address;
	}
	public BLEDevice(RemoteDeviceProperties deviceProperties) {
		name = deviceProperties.deviceName;
		address = deviceProperties.deviceAddress;
	}

	public void setServices(ServiceDefinition[] services){
		this.services = services;
		displayServices();
		findNotifyHandles();
	}
	
	public void findNotifyHandles(){
		if(services != null) {
			for(ServiceDefinition service : services) {

				int serviceId = longToShortUUID(service.serviceDetails.uuid);
				for(DeviceClass device:devices){
					if(serviceId==device.serviceUUID){
						displayMessage("Service found! "+Integer.toHexString(serviceId));
						for(CharacteristicDefinition characteristic : service.characteristics) {

							int charId = longToShortUUID(characteristic.uuid);					
							if(charId==device.characteristicUUID){
								displayMessage("Characteristic found! "+Integer.toHexString(charId)+" properties:");

								if(characteristic.properties.contains(CharacteristicProperty.NOTIFY)){
									notifyHandle = characteristic.descriptors[0].handle;
									deviceType = device;
									displayMessage("notifyHandle found! "+characteristic.descriptors[0].handle+" for "+device.type.name());
								}
							}
						}
					}
				}
			}
		}
	}
	// 128 bit UUIDs are encoded from 16 bit UUIDs, we want to extract the 16 bit one, its at byte 3-4
	int longToShortUUID(UUID uuid){
		int shortUUID = (int)((uuid.getMostSignificantBits()>>32)&0xFFFF);
		Log.d(TAG, "long UUID:"+uuid.toString());
		Log.d(TAG, "short UUID:"+Integer.toHexString((int)((uuid.getMostSignificantBits()>>32)&0xFFFF)));
	    return shortUUID;
	}
	void displayMessage(CharSequence string){
		UtilActivity.displayMessage(string);
	}

	
	void displayServices(){
		if(services != null) {
            displayMessage("Services: ");

            for(ServiceDefinition service : services) {
                StringBuilder sb = new StringBuilder();
                sb.append("Service UUID:          ").append(service.serviceDetails.uuid.toString()).append('\n');
                sb.append("Starting Handle:       ").append(service.serviceDetails.startHandle).append('\n');
                sb.append("Ending Handle:         ").append(service.serviceDetails.endHandle).append('\n');

                sb.append("Included Services:\n");
                if(service.includedServices.length > 0) {
                    for(ServiceInformation includedService : service.includedServices) {
                        sb.append("    * UUID:            ").append(includedService.uuid.toString()).append('\n');
                        sb.append("      Starting Handle: ").append(includedService.startHandle).append('\n');
                        sb.append("      Ending Handle:   ").append(includedService.endHandle).append('\n');
                    }
                } else {
                    sb.append("     -none-\n");
                }

                sb.append("Characteristics:\n");
                if(service.characteristics.length > 0) {
                    for(CharacteristicDefinition characteristic : service.characteristics) {
                        sb.append("    * UUID:            ").append(characteristic.uuid.toString()).append('\n');
                        sb.append("      Handle:          ").append(characteristic.handle).append('\n');

                        sb.append("      Properties:\n");
                        if(characteristic.properties.isEmpty()) {
                            sb.append("           -none-\n");
                        } else {
                            for(CharacteristicProperty prop : characteristic.properties)
                                sb.append("           ").append(prop.name()).append('\n');
                        }

                        sb.append("      Descriptors:\n");
                        if(characteristic.descriptors.length > 0) {
                            for(CharacteristicDescriptor descriptor : characteristic.descriptors) {
                                sb.append("          * UUID:      ").append(descriptor.uuid.toString()).append('\n');
                                sb.append("            Handle:    ").append(descriptor.handle).append('\n');
                            }
                        } else {
                            sb.append("           -none-\n");
                        }
                    }
                } else {
                    sb.append("     -none-\n");
                }

                displayMessage(sb);
                displayMessage("");
            }
        }
	}


}
