package com.reconinstruments.lispxml;
import java.util.*; 
import android.util.Log;
import android.content.Intent;
import android.content.Context;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import android.os.PowerManager;
import android.location.Location;
import android.location.LocationManager;

public class LocationModule extends Module {
    public static final String TAG = "LocationModule";
    public static final String NS = "com.reconinstruments.lispxml.module.location";
    protected String mPrefix= NS;
    protected LocationManager mLocManager;
    public LocationModule (LispXmlParser lp) {
	super(lp);
	mLocManager = (LocationManager) mOwner.mContext
	    .getSystemService(Context.LOCATION_SERVICE);
    }
    @Override
    public String getNamespace() {
	return NS;
    }
    @Override
    public Element exec (Element el, Document doc) throws Exception{
	mPrefix = el.getPrefix();
	String command = el.getLocalName();
	if (command.equals("last-location-stamp")) {
	    return last_loc_stamp(el.getChildNodes(),doc);
	}
	return super.exec(el,doc);
    }

    private Element last_loc_stamp(NodeList nl,Document doc) throws Exception {
	Element el1 = mOwner.exec(getElement(0,nl),doc);
	if (nodeis(coreModule.NS,"s",el1)) {
	    String locationProvider = el1.getTextContent();
	    Location lastKnownLocation = mLocManager
		.getLastKnownLocation(locationProvider);
	    if (lastKnownLocation == null) return doc.createElement("nil");
	    String result = lastKnownLocation.getTime() + "," +
		lastKnownLocation.getLatitude() + "," +
		lastKnownLocation.getLongitude();
	    Element el = doc.createElement("s");
	    el.appendChild(doc.createTextNode(result));
	    return el;
	}
	else {
	    throw new Exception("Bad last-location-stamp argument");
	}
    }
}
