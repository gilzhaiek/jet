/*
 * Copyright (C) 2015 Recon Instruments 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */ 

package reconinstruments.HUDBluetoothLowEnergy.cts;

import java.util.UUID;

import android.util.Log;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

import java.nio.FloatBuffer;
import java.nio.ByteBuffer;

/**
 * Helper Class for the JET custom android.bluetooth.BluetoothGatt* cts test
 *
 * Provides required UUIDs regarding the TI HeartRate BLE dongle
 * 
 * All information is sourced from the Bluetooth Developer Portal: https://developer.bluetooth.org/Pages/default.aspx
 * Additional information is obtained through iOS app: BLE Utility
 */
public class TargetAttributes
{
    // Debug Variables
    private static final String TAG = "TargetAttributes";
    private static final boolean DEBUG = true;

    // TI HeartRate BLE dongle device information
    static final String DEVICE_ADDR = "90:D7:EB:B2:66:DE";
    static final String DEVICE_NAME = "Heart Rate Sensor";

    /**
     * Enum consisting all BluetoothGattService related information
     * Enum consisting all BluetoothGattService static comparison tests
     */
    public enum serviceUUID{
        SERVICE_GENERIC_ACCESS("00001800-0000-1000-8000-00805f9b34fb") {
            @Override
            public boolean checkService(UUID targetUUID, int size, int position)
            {
                if (size != 5)
                    return false;
                switch(position) {
                    case 0: return targetUUID.equals(characteristicUUID.CHARACTERISTIC_DEVICE_NAME.toUuid());
                    case 1: return targetUUID.equals(characteristicUUID.CHARACTERISTIC_APPEARENCE.toUuid());
                    case 2: return targetUUID.equals(characteristicUUID.CHARACTERISTIC_PRIVACY_FLAG.toUuid());
                    case 3: return targetUUID.equals(characteristicUUID.CHARACTERISTIC_RECONNECTION_ADDRESS.toUuid());
                    case 4: return targetUUID.equals(characteristicUUID.CHARACTERISTIC_PERIPHERAL_PRED.toUuid());
                    default: return false;
                }
            }

        },
            SERVICE_GENERIC_ATTRIBUTE("00001801-0000-1000-8000-00805f9b34fb") {
                @Override
                public boolean checkService(UUID targetUUID, int size, int position)
                {
                    if (size != 1)
                        return false;
                    switch(position) {
                        case 0: return targetUUID.equals(characteristicUUID.CHARACTERISTIC_SERVICE_CHANGE.toUuid());
                        default: return false;
                    }
                }
            },
            SERVICE_HEART_RATE("0000180d-0000-1000-8000-00805f9b34fb") {
                @Override
                public boolean checkService(UUID targetUUID, int size, int position)
                {
                    if (size != 3)
                        return false;
                    switch(position) {
                        case 0: return targetUUID.equals(characteristicUUID.CHARACTERISTIC_HEART_RATE_MEASURE.toUuid());
                        case 1: return targetUUID.equals(characteristicUUID.CHARACTERISTIC_BODY_SENSOR_LOCATION.toUuid());
                        case 2: return targetUUID.equals(characteristicUUID.CHARACTERISTIC_HEART_RATE_CTRL_POINT.toUuid());
                        default: return false;
                    }
                }
            },
            SERVICE_DEVICE_INFORMATION("0000180a-0000-1000-8000-00805f9b34fb") {
                @Override
                public boolean checkService(UUID targetUUID, int size, int position)
                {
                    if (size != 9)
                        return false;
                    switch(position) {
                        case 0: return targetUUID.equals(characteristicUUID.CHARACTERISTIC_SYSTEM_ID.toUuid());
                        case 1: return targetUUID.equals(characteristicUUID.CHARACTERISTIC_MODEL_NUMBER.toUuid());
                        case 2: return targetUUID.equals(characteristicUUID.CHARACTERISTIC_SERIAL_NUMBER.toUuid());
                        case 3: return targetUUID.equals(characteristicUUID.CHARACTERISTIC_FIRMWARE_REV.toUuid());
                        case 4: return targetUUID.equals(characteristicUUID.CHARACTERISTIC_HARDWARE_REV.toUuid());
                        case 5: return targetUUID.equals(characteristicUUID.CHARACTERISTIC_SOFTWARE_REV.toUuid());
                        case 6: return targetUUID.equals(characteristicUUID.CHARACTERISTIC_MANUFACTURE_NAME.toUuid());
                        case 7: return targetUUID.equals(characteristicUUID.CHARACTERISTIC_IEEE_CERTIFICATE.toUuid());
                        case 8: return targetUUID.equals(characteristicUUID.CHARACTERISTIC_PNP_ID.toUuid());
                        default: return false;
                    }
                }
            },
            SERVICE_BATTERY_SERVICE("0000180f-0000-1000-8000-00805f9b34fb") {
                @Override
                public boolean checkService(UUID targetUUID, int size, int position)
                {
                    if (size != 1)
                        return false;
                    switch(position) {
                        case 0: return targetUUID.equals(characteristicUUID.CHARACTERISTIC_BATTERY_LEVEL.toUuid());
                        default: return false;
                    }
                }
            };

        private final String uuid;
        private serviceUUID(final String uuid)
        {
            this.uuid = uuid;
        }

        public String toString()
        {
            return uuid;
        }

        public UUID toUuid()
        {
            return UUID.fromString(uuid);
        }

        public static serviceUUID getUuid(UUID desired)
        {
            for (serviceUUID service : values())
            {
                if (service.toUuid().equals(desired))
                    return service;
            }
            return null;
        } 

        public abstract boolean checkService (UUID targetUUID, int size, int position);
    }

    /**
     * Enum consisting all BluetoothGattCharacteristic related information
     * Enum consisting all BluetoothGattCharacteristic static comparison tests
     */
    public enum characteristicUUID
    {
        CHARACTERISTIC_DEVICE_NAME("00002a00-0000-1000-8000-00805f9b34fb")
        {
            @Override
            public boolean check_getDescriptors(UUID targetUUID, int size, int position)
            {
                return false;
            }

            @Override
            public boolean checkBluetoothGattCallback(BluetoothGattCharacteristic characteristic)
            {
                // Using default encode format: UTF-8
                return (new String(characteristic.getValue()).equals("Heart Rate Sensor"));
            }

            @Override
            public boolean check_getProperties(int properties, int id)
            {
                return (properties & BluetoothGattCharacteristic.PROPERTY_READ) != 0 && id == 3;
            }
        },
            CHARACTERISTIC_APPEARENCE("00002a01-0000-1000-8000-00805f9b34fb")
            {
                @Override
                public boolean check_getDescriptors(UUID targetUUID, int size, int position)
                {
                    return false;
                }

                @Override
                public boolean checkBluetoothGattCallback(BluetoothGattCharacteristic characteristic)
                {
                    return false;
                }

                @Override
                public boolean check_getProperties(int properties, int id)
                {
                    return (properties & BluetoothGattCharacteristic.PROPERTY_READ) != 0 && id == 5;
                }
            },
            CHARACTERISTIC_PRIVACY_FLAG("00002a02-0000-1000-8000-00805f9b34fb")
            {
                @Override
                public boolean check_getDescriptors(UUID targetUUID, int size, int position)
                {
                    return false;
                }

                @Override
                public boolean checkBluetoothGattCallback(BluetoothGattCharacteristic characteristic)
                {
                    return false;
                }

                @Override
                public boolean check_getProperties(int properties, int id)
                {
                    return (properties & BluetoothGattCharacteristic.PROPERTY_READ) != 0 && id == 7;
                }
            },
            CHARACTERISTIC_RECONNECTION_ADDRESS("00002a03-0000-1000-8000-00805f9b34fb")
            {
                @Override
                public boolean check_getDescriptors(UUID targetUUID, int size, int position)
                {
                    return false;
                }

                @Override
                public boolean checkBluetoothGattCallback(BluetoothGattCharacteristic characteristic)
                {
                    return false;
                }

                @Override
                public boolean check_getProperties(int properties, int id)
                {
                    return (properties & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 && id == 9;
                }
            },
            CHARACTERISTIC_PERIPHERAL_PRED("00002a04-0000-1000-8000-00805f9b34fb")
            {
                @Override
                public boolean check_getDescriptors(UUID targetUUID, int size, int position)
                {
                    return false;
                }

                @Override
                public boolean checkBluetoothGattCallback(BluetoothGattCharacteristic characteristic)
                {
                    return false;
                }

                @Override
                public boolean check_getProperties(int properties, int id)
                {
                    return (properties & BluetoothGattCharacteristic.PROPERTY_READ) != 0 && id == 11;
                }
            },
            CHARACTERISTIC_SERVICE_CHANGE("00002a05-0000-1000-8000-00805f9b34fb")
            {
                @Override
                public boolean check_getDescriptors(UUID targetUUID, int size, int position)
                {
                    if (size != 1)
                        return false;
                    switch(position) {
                        case 0: return targetUUID.equals(descriptorUUID.CLIENT_CHARACTERISTIC_CONFIGURATION.toUuid());
                        default: return false;
                    }
                }

                @Override
                public boolean checkBluetoothGattCallback(BluetoothGattCharacteristic characteristic)
                {
                    return false;
                }

                @Override
                public boolean check_getProperties(int properties, int id)
                {
                    return (properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0 && id == 14;
                }
            },
            CHARACTERISTIC_HEART_RATE_MEASURE("00002a37-0000-1000-8000-00805f9b34fb")
            {
                @Override
                public boolean check_getDescriptors(UUID targetUUID, int size, int position)
                {
                    if (size != 1)
                        return false;
                    switch (position) {
                        case 0: return targetUUID.equals(descriptorUUID.CLIENT_CHARACTERISTIC_CONFIGURATION.toUuid());
                        default: return false;
                    }
                }

                @Override
                public boolean checkBluetoothGattCallback(BluetoothGattCharacteristic characteristic)
                {
                    int heart_rate = byteArrayToInt(characteristic.getValue());
                    return heart_rate >= 70 && heart_rate <= 80;
                }

                @Override
                public boolean check_getProperties(int properties, int id)
                {
                    return (properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0 && id == 18;
                }
            },
            CHARACTERISTIC_BODY_SENSOR_LOCATION("00002a38-0000-1000-8000-00805f9b34fb")
            {
                @Override
                public boolean check_getDescriptors(UUID targetUUID, int size, int position)
                {
                    return false;
                }

                @Override
                public boolean checkBluetoothGattCallback(BluetoothGattCharacteristic characteristic)
                {
                    return false;
                }

                @Override
                public boolean check_getProperties(int properties, int id)
                {
                    return (properties & BluetoothGattCharacteristic.PROPERTY_READ) != 0 && id == 21;
                }
            },
            CHARACTERISTIC_HEART_RATE_CTRL_POINT("00002a39-0000-1000-8000-00805f9b34fb")
            {
                @Override 
                public boolean check_getDescriptors(UUID targetUUID, int size, int position)
                {
                    return false;
                }

                @Override
                public boolean checkBluetoothGattCallback(BluetoothGattCharacteristic characteristic)
                {
                    return byteArrayToInt(characteristic.getValue()) == 1;
                }

                @Override
                public boolean check_getProperties(int properties, int id)
                {
                    return (properties & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 && id == 23;
                }
            },

            CHARACTERISTIC_MANUFACTURE_NAME("00002a29-0000-1000-8000-00805f9b34fb")
            {
                @Override
                public boolean check_getDescriptors(UUID targetUUID, int size, int position)
                {
                    return false;
                }

                @Override
                public boolean checkBluetoothGattCallback(BluetoothGattCharacteristic characteristic)
                {
                    return false;
                }

                @Override
                public boolean check_getProperties(int properties, int id)
                {
                    return (properties & BluetoothGattCharacteristic.PROPERTY_READ) != 0 && id == 38;
                }
            },
            CHARACTERISTIC_MODEL_NUMBER("00002a24-0000-1000-8000-00805f9b34fb")
            {
                @Override
                public boolean check_getDescriptors(UUID targetUUID, int size, int position)
                {
                    return false;
                }

                @Override
                public boolean checkBluetoothGattCallback(BluetoothGattCharacteristic characteristic)
                {
                    return false;
                }

                @Override
                public boolean check_getProperties(int properties, int id)
                {
                    return (properties & BluetoothGattCharacteristic.PROPERTY_READ) != 0 && id == 28;
                }
            },
            CHARACTERISTIC_SERIAL_NUMBER("00002a25-0000-1000-8000-00805f9b34fb")
            {
                @Override
                public boolean check_getDescriptors(UUID targetUUID, int size, int position)
                {
                    return false;
                }

                @Override
                public boolean checkBluetoothGattCallback(BluetoothGattCharacteristic characteristic)
                {
                    return false;
                }

                @Override
                public boolean check_getProperties(int properties, int id)
                {
                    return (properties & BluetoothGattCharacteristic.PROPERTY_READ) != 0 && id == 30;
                }
            },
            CHARACTERISTIC_HARDWARE_REV("00002a27-0000-1000-8000-00805f9b34fb")
            {
                @Override
                public boolean check_getDescriptors(UUID targetUUID, int size, int position)
                {
                    return false;
                }

                @Override
                public boolean checkBluetoothGattCallback(BluetoothGattCharacteristic characteristic)
                {
                    return false;
                }

                @Override
                public boolean check_getProperties(int properties, int id)
                {
                    return (properties & BluetoothGattCharacteristic.PROPERTY_READ) != 0 && id == 34;
                }
            },
            CHARACTERISTIC_FIRMWARE_REV("00002a26-0000-1000-8000-00805f9b34fb")
            {
                @Override
                public boolean check_getDescriptors(UUID targetUUID, int size, int position)
                {
                    return false;
                }

                @Override
                public boolean checkBluetoothGattCallback(BluetoothGattCharacteristic characteristic)
                {
                    return false;
                }

                @Override
                public boolean check_getProperties(int properties, int id)
                {
                    return (properties & BluetoothGattCharacteristic.PROPERTY_READ) != 0 && id == 32;
                }
            },
            CHARACTERISTIC_SOFTWARE_REV("00002a28-0000-1000-8000-00805f9b34fb")
            {
                @Override
                public boolean check_getDescriptors(UUID targetUUID, int size, int position)
                {
                    return false;
                }

                @Override
                public boolean checkBluetoothGattCallback(BluetoothGattCharacteristic characteristic)
                {
                    return false;
                }

                @Override
                public boolean check_getProperties(int properties, int id)
                {
                    return (properties & BluetoothGattCharacteristic.PROPERTY_READ) != 0 && id == 36;
                }
            },
            CHARACTERISTIC_SYSTEM_ID("00002a23-0000-1000-8000-00805f9b34fb")
            {
                @Override
                public boolean check_getDescriptors(UUID targetUUID, int size, int position)
                {
                    return false;
                }

                @Override
                public boolean checkBluetoothGattCallback(BluetoothGattCharacteristic characteristic)
                {
                    return false;
                }

                @Override
                public boolean check_getProperties(int properties, int id)
                {
                    return (properties & BluetoothGattCharacteristic.PROPERTY_READ) != 0 && id == 26;
                }
            },
            CHARACTERISTIC_IEEE_CERTIFICATE("00002a2a-0000-1000-8000-00805f9b34fb")
            {
                @Override
                public boolean check_getDescriptors(UUID targetUUID, int size, int position)
                {
                    return false;
                }

                @Override
                public boolean checkBluetoothGattCallback(BluetoothGattCharacteristic characteristic)
                {
                    return false;
                }

                @Override
                public boolean check_getProperties(int properties, int id)
                {
                    return (properties & BluetoothGattCharacteristic.PROPERTY_READ) != 0 && id == 40;
                }
            },
            CHARACTERISTIC_PNP_ID("00002a50-0000-1000-8000-00805f9b34fb")
            {
                @Override
                public boolean check_getDescriptors(UUID targetUUID, int size, int position)
                {
                    return false;
                }

                @Override
                public boolean checkBluetoothGattCallback(BluetoothGattCharacteristic characteristic)
                {
                    byteArrayToInt(characteristic.getValue());
                    byte[] data = characteristic.getValue();
                    return data[0] == 1 && data[1] == 13 && data[2] == 0 &&
                        data[3] == 0 && data[4] == 0 && data[5] == 16 && data[6] == 1;
                }

                @Override
                public boolean check_getProperties(int properties, int id)
                {
                    return (properties & BluetoothGattCharacteristic.PROPERTY_READ) != 0 && id == 42;
                }
            },

            CHARACTERISTIC_BATTERY_LEVEL("00002a19-0000-1000-8000-00805f9b34fb")
            {
                @Override
                public boolean check_getDescriptors(UUID targetUUID, int size, int position)
                {
                    if (size != 2)
                        return false;
                    switch (position) {
                        case 0: return targetUUID.equals(descriptorUUID.CLIENT_CHARACTERISTIC_CONFIGURATION.toUuid());
                        case 1: return targetUUID.equals(descriptorUUID.REPORT_REFERENCE.toUuid());
                        default: return false;
                    }
                }

                @Override
                public boolean checkBluetoothGattCallback(BluetoothGattCharacteristic characteristic)
                {
                    int battery_level = byteArrayToInt(characteristic.getValue());
                    return battery_level >= 0 && battery_level <= 100 &&
                        (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0 &&
                        (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
                }

                @Override
                public boolean check_getProperties(int properties, int id)
                {
                    return (properties & BluetoothGattCharacteristic.PROPERTY_READ) != 0 &&
                        (properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0 && id == 45;
                }
            };

        private final String uuid;
        private characteristicUUID(final String uuid)
        {
            this.uuid = uuid;
        }

        public String toString()
        {
            return uuid;
        }

        public UUID toUuid()
        {
            return UUID.fromString(uuid);
        }

        public static characteristicUUID getUuid(UUID desired)
        {
            for (characteristicUUID characteristic : values())
            {
                if (characteristic.toUuid().equals(desired))
                    return characteristic;
            }
            return null;
        }

        public abstract boolean check_getDescriptors(UUID targetUUID, int size, int position);
        public abstract boolean checkBluetoothGattCallback(BluetoothGattCharacteristic characteristic);
        public abstract boolean check_getProperties(int properties, int id);
    }

    /**
     * Enum consisting all BluetoothGattDescriptor related information
     * Enum consisting all BluetoothGattDescriptor static comparison tests
     */
    public enum descriptorUUID
    {
        CLIENT_CHARACTERISTIC_CONFIGURATION("00002902-0000-1000-8000-00805f9b34fb")
        {
            @Override
            public boolean checkDescriptor(BluetoothGattDescriptor descriptor)
            {
                return byteArrayToInt(descriptor.getValue()) == 0;
            }
        },
            REPORT_REFERENCE("00002908-0000-1000-8000-00805f9b34fb")
            {
                @Override
                public boolean checkDescriptor(BluetoothGattDescriptor descriptor)
                {
                    byte[] data = descriptor.getValue();
                    return data[0] == 4 && data[1] == 1 && data.length == 2;
                }
            };

        private final String uuid;
        private descriptorUUID(final String uuid)
        {
            this.uuid = uuid;
        }

        public String toString()
        {
            return uuid;
        }

        public UUID toUuid()
        {
            return UUID.fromString(uuid);
        }

        public static descriptorUUID getUuid(UUID desired)
        {
            for (descriptorUUID descriptor : values())
            {
                if (descriptor.toUuid().equals(desired))
                    return descriptor;
            }
            return null;
        }

        public abstract boolean checkDescriptor(BluetoothGattDescriptor descriptor);
    }

    /**
     * Private helper to convert byte to int
     */
    private static int byteArrayToInt(byte[] data)
    {
        int value = 0;
        for (int i = 0; i < data.length; i++)
        {
            value = value + data[i];
            if(DEBUG)
            {
                Log.d(TAG, "DEBUG: byteArrayToInt bits: "+String.format("%8s", Integer.toBinaryString(data[i] & 0xff)).replace(' ', '0'));
                Log.d(TAG, "DEBUG: byteArrayToInt value: "+value);
            }
        }

        return value;
    }
}
