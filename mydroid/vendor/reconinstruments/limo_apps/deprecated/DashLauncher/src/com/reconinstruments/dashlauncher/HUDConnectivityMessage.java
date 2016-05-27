/**
 * Copyright (C) 2013 Recon Instruments Inc.
 *
 * All Rights Reserved.
 */

package com.reconinstruments.dashlauncher;

import java.nio.ByteBuffer;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * The message transferring between SDK and 3rd-party.
 */
public class HUDConnectivityMessage implements Parcelable {
	static String TAG = "HUDConnectivityMessage";
	
	protected int requestKey;
	protected String sender;
	protected String intentFilter;
	protected byte[] data;

	public static final Parcelable.Creator<HUDConnectivityMessage> CREATOR = new Parcelable.Creator<HUDConnectivityMessage>() {
		public HUDConnectivityMessage createFromParcel(Parcel in) {
			return new HUDConnectivityMessage(in);
		}

		public HUDConnectivityMessage[] newArray(int size) {
			return new HUDConnectivityMessage[size];
		}
	};

	public HUDConnectivityMessage() {

	}

	private HUDConnectivityMessage(Parcel in) {
		readFromParcel(in);
	}

	public HUDConnectivityMessage(int requestKey, String intentFilter,
			String sender, byte[] data) {
		this.requestKey = requestKey;
		this.intentFilter = intentFilter;
		this.data = data;
		this.sender = sender;
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

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(intentFilter);
		dest.writeInt(data.length); 
		dest.writeByteArray(data);
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
		return "HUDConnectivityMessage [intentFilter=" + intentFilter
				+ ", sender=" + sender + "]";
	}
	
	public HUDConnectivityMessage(byte[] data) {
		ByteBuffer buff = ByteBuffer.wrap(data, 0, data.length);
		if (data != null && data.length >= 16) {
			try {
				int totalLen = buff.getInt();
				buff.slice();
				if (totalLen == data.length) {
					int requestKey = buff.getInt();
					buff.slice();
					
					int senderLen = buff.getInt();
					buff.slice();
					
					byte[] tmp = new byte[senderLen];
					buff.get(tmp);
					String sender = new String(tmp);
					buff.slice();
				
					int intentFilterLen = buff.getInt();
					buff.slice();

					tmp = new byte[intentFilterLen];
					buff.get(tmp);
					String intentFilter = new String(tmp);
					buff.slice();
					
					byte[] dataField = new byte[buff.remaining()];
					buff.get(dataField);
					
					this.requestKey = requestKey;
					this.intentFilter = intentFilter;
					this.data = dataField;
					this.sender = sender;
				}
			} catch (IndexOutOfBoundsException e) {
				e.printStackTrace();
			}
		}
	}
	
	public byte[] ToByteArray(){
		int totalLen = 16 + sender.length() + intentFilter.length() + data.length;
		ByteBuffer buffer = ByteBuffer.allocate(totalLen);
		buffer.putInt(totalLen);
		buffer.putInt(requestKey);
		buffer.putInt(sender.length());
		buffer.put(sender.getBytes());
		buffer.putInt(intentFilter.length());
		buffer.put(intentFilter.getBytes());
		buffer.put(data);
		return buffer.array();
	}
}