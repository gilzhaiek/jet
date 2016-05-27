package com.reconinstruments.hudconnectivitylib.bluetooth;

/**
 * Keeps track of excess data, and validate if excess data should be clear up or not
 */
public class ExcessDataAgent {

    private byte[]  mData = null;

    /**
     * Verify mData has valid HUDBTHeader 
     *  condition 1: It has data and it is a header, return true 
     *  condition 2: It has no data, return false
     *  condition 3: Data has size greater than header but it is not a header. ResetData and return false 
     *  condition 4: Data has size less than header but it is not a header, return false
     * @return true 
     */
    public boolean hasValidData() {
        int bytes = getBytes();
        if(bytes > 0 ){
            if(bytes >= HUDBTHeaderFactory.HEADER_LENGTH){
                if(HUDBTHeaderFactory.isHeader(getData())){
                    return true;
                }    
                resetData();
                return false;
            }
            else {
                return false;
            }            
        }
        return false;
    }

    /**
     * @return data in byte array
     */
    public byte[] getData() {
        return mData;
    }

    /**
     * @return length of byte arrary, otherwise -1 if no data is null 
     */
    public int getBytes() {
        if(mData != null){
            return mData.length;
        }
        return -1;
    }

    /**
     * Append data only if appending data can regenerate the HUDBTHeader
     * @param data to append
     * @param bytes size of data to append
     * @return true if append is happening, otherwise false;
     */
    public boolean smartAppendData(byte[] data, int bytes) {
        if(getBytes() <= 0){
            return false;
        }

        byte[] tempHead = new byte[HUDBTHeaderFactory.HEADER_LENGTH];
        int lengthToCompleteHeader = HUDBTHeaderFactory.HEADER_LENGTH - getBytes();
        if(lengthToCompleteHeader > bytes){
            return false;
        }
        System.arraycopy(getData(), 0, tempHead, 0, getBytes());
        System.arraycopy(data, 0, tempHead, getBytes(), lengthToCompleteHeader);

        if(HUDBTHeaderFactory.isHeader(tempHead)){
            byte[] appendData = new byte[getBytes() + bytes];
            System.arraycopy(getData(), 0, appendData, 0, getBytes());
            System.arraycopy(data, 0, appendData, getBytes(), bytes);
            mData = appendData;
        }
        return true;
    }

    /**
     * Set existing data(byte array) to null 
     */
    public void resetData() {
        mData = null;
    }

    /**
     * Set Data
     * @param src the source array to copy the content.
     * @param srcPos the starting index of the content in {@code src}.
     * @param length the number of elements to be copied.
     */
    public void setData(byte[] src, int srcPos, int length) {
        mData = new byte[length];
        System.arraycopy(src, srcPos, mData, 0, length);
    }
}
