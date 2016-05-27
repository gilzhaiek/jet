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
public class PkgListMessage extends XMLMessage {

	static final String TAG = "ApkListMessage.java";

	static String intent = "RECON_PKGLIST_REQUEST";

	static final String NODE_PKGLIST = "pkglist";
	//static final String NODE_PKGLIST_ACTION_ATTR = "action";

	public String compose()
	{
		String message = composeHead( intent );

		message += String.format("<%s />", 
				intent
				//NODE_PKG_NAME_ATTR, ab.pkgName
				);

		message = appendEnding( message );

		return message;
	}

	public boolean parse(String message) {

		Document doc = super.validate(intent,message);
		if(doc==null) return false;

		return true;
	}

}
