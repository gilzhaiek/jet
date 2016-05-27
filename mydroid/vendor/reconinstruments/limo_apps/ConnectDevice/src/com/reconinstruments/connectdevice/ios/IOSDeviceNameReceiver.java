package com.reconinstruments.connectdevice.ios;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.reconinstruments.connectdevice.PreferencesUtils;

//JIRA: MODLIVE-772 Implement bluetooth connection wizard on MODLIVE
public class IOSDeviceNameReceiver extends BroadcastReceiver {
	private static final String TAG = "IOSDeviceNameReceiver";

	@Override
	public void onReceive(Context c, Intent i) {
		Bundle bundle = i.getExtras();
		String message = bundle.getString("message");
		Log.d(TAG, "message: " + message);
		String deviceName = getDeviceName(message);
		Log.d(TAG, "deviceName: " + deviceName);
		if (deviceName != null) {
			PreferencesUtils.setDeviceName(c, deviceName);
		}
	}

	private String getDeviceName(String message) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(message));

			Document doc = db.parse(is);
			NodeList nodes = doc.getElementsByTagName("device");
			Node rootNode = nodes.item(0);
			NamedNodeMap nnm = rootNode.getAttributes();
			Node n = nnm.getNamedItem("name");
			String type = n.getNodeValue();
			return type;

		} catch (Exception e) {
			e.printStackTrace();
			Log.w(TAG, "Failed to parse xml:" + e.getMessage());
		}
		return null;
	}
}
// End of JIRA: MODLIVE-772