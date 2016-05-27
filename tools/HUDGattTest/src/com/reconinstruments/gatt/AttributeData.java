package com.reconinstruments.gatt;

/**
 * AttributeData class
 */
public class AttributeData {
    String address;
    int handle;
    byte[] values;
    AttributeData(String address, int handle, byte[] values) {
        this.address = address;
        this.handle = handle;
        this.values = values;
    }
    
    AttributeData(String address, int handle) {
        this.address = address;
        this.handle = handle;
        this.values = null;
    }
}