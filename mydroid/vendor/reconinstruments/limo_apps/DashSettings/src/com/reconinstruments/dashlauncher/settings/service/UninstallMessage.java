package com.reconinstruments.dashlauncher.settings.service;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.reconinstruments.modlivemobile.dto.message.XMLMessage;

import android.util.Log;

/**
 *Copyright 2012 Recon Instruments
 *All Rights Reserved.
 */
/**
 * @author Patrick Cho
 * 
 *  This class is used to compose / parse  message associated with uninstallation
 */
public class UninstallMessage extends XMLMessage {

	static final String TAG = "UninstallMessage.java";

	static String intent = "RECON_UNINSTALL";

	static final String NODE_UNINSTALL = "uninstall";
	static final String NODE_PKG_NAME_ATTR = "name";

	public static class UninstallBundle {
		// PKG name
		public String pkgName;
		
		public UninstallBundle(String pkgName) {
			this.pkgName = pkgName;
		}
	}

	public String compose( UninstallBundle ab )
	{
		String message = composeHead( intent );

		message += String.format("<%s %s=\"%s\" />", 
				NODE_UNINSTALL,
				NODE_PKG_NAME_ATTR, ab.pkgName
				);

		message = appendEnding( message );

		return message;
	}

	public UninstallBundle parse(String message) {

		Document doc = super.validate(intent,message);
		if(doc==null) return null;

		UninstallBundle uninstBundle = null;

		try {
			Element uninstallElement = (Element)doc.getElementsByTagName(NODE_UNINSTALL).item(0);
			String packageName = uninstallElement.getAttribute(NODE_PKG_NAME_ATTR);
			
			uninstBundle = new UninstallBundle(packageName);
		} 
		catch (Exception e) {
			Log.e( TAG, "Error parsing XML message",e);
		} 
		return uninstBundle;
	}

}
