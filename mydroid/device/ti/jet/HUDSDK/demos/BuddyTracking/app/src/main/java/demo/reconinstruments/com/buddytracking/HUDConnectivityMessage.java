package demo.reconinstruments.com.buddytracking;

import android.os.Parcel;
import android.os.Parcelable;

import java.nio.ByteBuffer;

/**
 * Created by gil on 4/24/15.
 */
public class HUDConnectivityMessage implements Parcelable {
    public static String TAG = "HUDConnectivityMessage";

    protected int requestKey;
    protected String sender;
    protected String intentFilter;
    protected String info;
    protected byte[] data;

    public static final Parcelable.Creator<HUDConnectivityMessage> CREATOR = new Parcelable.Creator<HUDConnectivityMessage>() {
        @Override
        public HUDConnectivityMessage createFromParcel(Parcel in) {
            return new HUDConnectivityMessage(in);
        }

        @Override
        public HUDConnectivityMessage[] newArray(int size) {
            return new HUDConnectivityMessage[size];
        }
    };

    public HUDConnectivityMessage() {

    }

    private HUDConnectivityMessage(Parcel in) {
        readFromParcel(in);
    }

    public HUDConnectivityMessage(int requestKey, String intentFilter, String sender, String info, byte[] data) {
        this.requestKey = requestKey;
        this.intentFilter = intentFilter;
        this.data = data;
        this.info = info;
        this.sender = sender;
    }

    public HUDConnectivityMessage(byte[] data) {
//		Log.d(TAG, "data.length=" + data.length);
        ByteBuffer buff = ByteBuffer.wrap(data, 0, data.length);
//		Log.d(TAG, "buff.capacity=" + buff.capacity());
        if (data != null && data.length >= 20) {
            try {
                int totalLen = buff.getInt();
                buff.slice();

                if (totalLen == data.length) {

                    int requestKey = buff.getInt();
//					Log.d(TAG, "requestKey = " + requestKey);
                    buff.slice();

                    int senderLen = buff.getInt();
//					Log.d(TAG, "senderLen = " + senderLen);
                    buff.slice();

                    if (senderLen < 50331648) { // maximum heap size
                        byte[] tmp = new byte[senderLen];
                        buff.get(tmp);
                        String sender = new String(tmp);
//						Log.d(TAG, "sender = " + sender);
                        buff.slice();

                        int intentFilterLen = buff.getInt();
//						Log.d(TAG, "intentFilterLen = " + intentFilterLen);
                        buff.slice();

                        if (intentFilterLen < 50331648) { // maximum heap size

                            if (intentFilterLen > 0) {
                                tmp = new byte[intentFilterLen];
                                buff.get(tmp);
                                String intentFilter = new String(tmp);
                                this.intentFilter = intentFilter;
//								Log.d(TAG, "intentFilter = " + intentFilter);
                                buff.slice();
                            }

                            int infoLen = buff.getInt();
//							Log.d(TAG, "infoLen = " + infoLen);
                            buff.slice();

                            if (infoLen < 50331648) { // maximum heap size

                                if (infoLen > 0) {
                                    tmp = new byte[infoLen];
                                    buff.get(tmp);
                                    String info = new String(tmp);
//									Log.d(TAG, "info = " + info);
                                    buff.slice();
                                    this.info = info;
                                } else {
                                    this.info = "";
                                }

                                byte[] dataField = new byte[buff.remaining()];
                                buff.get(dataField);
                                // Log.d(TAG, "dataField.length = " + dataField.length);

//								if(dataField.length> 0 && dataField.length < 1024){
//									Log.d(TAG, "dataField = " + new String(dataField));
//								}

                                this.requestKey = requestKey;

                                this.sender = sender;

                                this.data = dataField;
                            }
                        }
                    }
                }
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public int getRequestKey() {
        return requestKey;
    }

    public void setRequestKey(int requestKey) {
        this.requestKey = requestKey;
    }

    public String getIntentFilter() {
        return intentFilter;
    }

    public void setIntentFilter(String intentFilter) {
        this.intentFilter = intentFilter;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(intentFilter);
        if (data != null) {
            dest.writeInt(data.length);
            dest.writeByteArray(data);
        }
        dest.writeString(sender);
        dest.writeInt(requestKey);
    }

    private void readFromParcel(Parcel in) {
        intentFilter = in.readString();
        data = new byte[in.readInt()];
        in.readByteArray(data);
        sender = in.readString();
        requestKey = in.readInt();
    }

    @Override
    public String toString() {
        return "HUDConnectivityMessage [intentFilter=" + intentFilter + ", sender=" + sender + "]";
    }

    public byte[] toByteArray() {
        if (sender == null) {
            sender = "";
        }
        if (info == null) {
            info = "";
        }
        if (data == null) {
            data = new byte[0];
        }
//		Log.w(TAG, "data =" + (data==null));
//		if(intentFilter != null && data != null){
        if (intentFilter != null) {
            int totalLen = 20 + sender.length() + intentFilter.length() + info.length() + data.length;
            ByteBuffer buffer = ByteBuffer.allocate(totalLen);
            buffer.putInt(totalLen);

            buffer.putInt(requestKey);

            buffer.putInt(sender.length());
            if (sender.length() > 0) {
                buffer.put(sender.getBytes());
            }

            buffer.putInt(intentFilter.length());
            if (intentFilter.length() > 0) {
                buffer.put(intentFilter.getBytes());
            }

            buffer.putInt(info.length());
            if (info.length() > 0) {
                buffer.put(info.getBytes());
            }

            if (data.length >= 0) {
                buffer.put(data);
            }
//				Log.d(TAG, "bytesToHex(buffer.array()) = " + bytesToHex(buffer.array()));
            return buffer.array();
        } else {
            return null;
        }
    }

    private String bytesToHex(byte[] bytes) {
        final char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
