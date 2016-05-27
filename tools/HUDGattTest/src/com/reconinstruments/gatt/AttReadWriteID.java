package com.reconinstruments.gatt;

public class AttReadWriteID {
    int transactionID;
    int clientid;
    //true for write; false for read
    boolean rw;
    AttReadWriteID(int transactionID, int clientid, boolean rw) {
        this.transactionID = transactionID;
        this.clientid = clientid;
        this.rw = rw;
    }
}
