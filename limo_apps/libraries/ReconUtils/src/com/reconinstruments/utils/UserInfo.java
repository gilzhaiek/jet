package com.reconinstruments.utils;
import android.os.Environment;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.Exception;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 *  <code>UserInfo</code> contains read only info on the user of Hud.
 *  Typically a copy is fetched like
 *  <code>UserInfo.getACopyOfSystemUserInfo()</code>
 *  
 *  Refer to https://github.com/reconinstruments/limo_apps/wiki/user_info_on_hud
 *  for the spec
 */
public class UserInfo {
    // Constants
    public static final String USER = "user";
    public static final String NAME = "name";
    public static final String HEIGHT = "height";
    public static final String WEIGHT = "weight";
    public static final String GENDER = "gender";
    public static final String MALE = "M";
    public static final String FEMALE = "F";
    public static final String BIRTH_YEAR = "birth_year";
    private static final String USER_INFO_LOC = "ReconApps/UserInfo/user_info.xml";
    
    private static final String TAG = "UserInfo";
    public static UserInfo getACopyOfSystemUserInfo() {
	String uis = "<user/>";
	try {
	    uis = FileUtils.getStringFromSDCard(Environment.getExternalStorageDirectory(),
						USER_INFO_LOC);
	} catch (Exception e) {
	    Log.w(TAG,"Bad system user info file");
	}
	return new UserInfo(uis);
    }
    private Element mUser;
    private void prepare(String userInfoString) {
	try {
	    InputSource is = new InputSource();
	    is.setCharacterStream(new StringReader(userInfoString));
	    Document document = DocumentBuilderFactory.newInstance()
		.newDocumentBuilder().parse(is);
	    mUser = (Element) document.getElementsByTagName("user").item(0);
	} catch (Exception e) {
	    Log.w(TAG, "Can't parse UserInfo");
	    e.printStackTrace();
	}
    }
    public UserInfo(String userInfoString) {
	prepare(userInfoString);
    }
    public String get(String attribute) {
	return mUser.getAttribute(attribute);
    }
}
