package com.reconinstruments.lispxml;
import android.content.Context;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*; 
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class AndroidModule extends Module {
    public static final String TAG = "AndroidModule";
    public static final String NS = "com.reconinstruments.lispxml.module.android";
    protected String mPrefix= NS; 	
				// then prefix not defined
    public AndroidModule (LispXmlParser lp) {
	super(lp);
    }
    @Override
    public String getNamespace() {
	return NS;
    }
    @Override
    public Element exec (Element el, Document doc) throws Exception{
	mPrefix = el.getPrefix();
	String command = el.getLocalName();
	if (command.equals("start-activity")) {
	    return start_activity(el.getChildNodes(),doc);
	}
	if (command.equals("send-broadcast")) {
	    return send_broadcast(el.getChildNodes(),doc);
	}
	if (command.equals("reboot")) {
	    return reboot(el.getChildNodes(),doc);
	}
	if (command.equals("get-battery-level")) {
	    return getBatteryLevel(doc);
	}
	if (command.equals("scan-sdcard")) {
	    return scanSDCard(doc);
	}
	if (command.equals("scan-file")) {
	    return scanFile(el.getChildNodes(),doc);
	}
	if (command.equals("uninstall")) {
	    return uninstallPackage(el.getChildNodes(),doc);
	}
	return super.exec(el,doc);
    }

    private Element start_activity(NodeList nl,Document doc) throws Exception {
	Element el1 = mOwner.exec(getElement(0,nl),doc);
	if (nodeis(coreModule.NS,"s",el1)) {
	    Intent i = new Intent(el1.getTextContent());
	    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    mOwner.mContext.startActivity(i);
	} else {
	    throw new Exception("Bad start-activity argument");
	}
	return doc.createElement("nil");
    }
    private Element send_broadcast(NodeList nl,Document doc) throws Exception {
	Element el1 = mOwner.exec(getElement(0,nl),doc);
	if (nodeis(coreModule.NS,"s",el1)) {
	    Intent i = new Intent(el1.getTextContent());
	    mOwner.mContext.sendBroadcast(i);
	} else {
	    throw new Exception("Bad send-broadcast argument");
	}
	return doc.createElement("nil");
    }

    private Element reboot(NodeList nl,Document doc) throws Exception {
	Element el1 = mOwner.exec(getElement(0,nl),doc);

	PowerManager pm = (PowerManager) mOwner.mContext
	    .getSystemService(Context.POWER_SERVICE);

	if (nodeis(coreModule.NS,"nil",el1)) {
	    pm.reboot(""); // Magic reason that causes shutdown as
	}
	else if (nodeis(coreModule.NS,"s",el1)) {
	    String reason = el1.getTextContent();
	    pm.reboot(reason);
	}
	else {
	    throw new Exception("Bad reboot argument");
	}
	return doc.createElement("nil");
    }

    private Element getBatteryLevel(Document doc) throws Exception {
	Intent batteryIntent = mOwner.mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
	int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
	int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
	int result = 0;
	// Error checking that probably isn't needed but I added just in case.
	if(level == -1 || scale == -1) {
	    throw new Exception("Can't read battery");
	}
	else {
	    result = (level * 100)/scale;
	}
	return ((MathModule)(mOwner.mModules.get(MathModule.NS))).int2element(result,doc);
    }
    private Element scanSDCard(Document doc) throws Exception {
	mOwner.mContext
	    .sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri
				      .parse("file://"
					     + Environment
					     .getExternalStorageDirectory())));
	return doc.createElement("nil");
    }
    private Element scanFile(NodeList nl, Document doc) throws Exception {
	List<String> fileNames = new ArrayList<String>();
	Element fileList = mOwner.exec(getElement(0,nl),doc);
	if (nodeis(coreModule.NS,"list",fileList)) { //
	    NodeList files = fileList.getChildNodes();
	    Node n;
	    for (int i = 0; i < files.getLength();i++) {
		n = files.item(i);
		if (n.getNodeType() != Node.ELEMENT_NODE){
		    continue;	// next item
		}
		// Element type
		if (nodeis(coreModule.NS,"s",(Element)n)) {
		    fileNames.add(n.getTextContent());
		}
		else {
		    throw new Exception("Bad list item, need string");
		}
	    }
	    // Now We have the file list
	    String[] paths = (String[]) fileNames.toArray(new String[fileNames.size()]);
	    MediaScannerConnection.scanFile(mOwner.mContext,
					    paths,null,null);
	    return doc.createElement("nil");
	}
	else {
	    throw new Exception("Require a list for argument");
	}
    }
    private Element uninstallPackage(NodeList nl, Document doc) throws Exception {
	Element el1 = mOwner.exec(getElement(0,nl),doc);
	if (!nodeis(coreModule.NS,"s",el1)) {
	    throw new Exception ("Bad uninstall argument");
	}
	String packageName = el1.getTextContent();
	PackageManager pm = mOwner.mContext.getPackageManager();
	Class<?> iPackageDeleteObserver =
	    Class.forName("android.content.pm.IPackageDeleteObserver");
	Class<?>[] types = new Class[] {String.class,
					iPackageDeleteObserver,
					int.class};
	Method deletePackageMethod =
	    pm.getClass().getMethod("deletePackage", types);
	// Use Dynamic proxies if you really want to implement a delete observer
	// In the context of LispXML we don't really need a callback
	deletePackageMethod.invoke(pm, new Object[] {packageName,null,0});
	return doc.createElement("nil");
    }
}
