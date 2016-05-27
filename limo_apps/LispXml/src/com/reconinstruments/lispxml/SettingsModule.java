package com.reconinstruments.lispxml;
import java.util.*; 
import android.util.Log;
import android.app.AlarmManager;
import android.content.Context;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.io.File;
import java.util.TimeZone;
import com.reconinstruments.utils.SettingsUtil;

public class SettingsModule extends Module {
    public static final String TAG = "SettingsModule";
    public SettingsModule (LispXmlParser lp) {
	super(lp);
	// Relies on Math Module
	if (lp.mModules.get(MathModule.NS) == null) {
	    lp.loadModule(new MathModule(lp));
	}
    }
    @Override
    public String getNamespace() {
	return "com.reconinstruments.lispxml.module.settings";
    }
    @Override
    public Element exec (Element el, Document doc) throws Exception{
	String command = el.getLocalName();
	mPrefix = el.getPrefix();
	if (command.equals("get-int")) {
	    return getInt(el.getChildNodes(),doc);
	}
	if (command.equals("set-int")) {
	    return setInt(el.getChildNodes(),doc);
	}
	if (command.equals("get-secure-int")) {
	    return getSecureInt(el.getChildNodes(),doc);
	}
	if (command.equals("set-secure-int")) {
	    return setSecureInt(el.getChildNodes(),doc);
	}
	if (command.equals("get-time-zone")) {
	    return getTimeZone(el.getChildNodes(),doc);
	}
	if (command.equals("set-time-zone")) {
	    return setTimeZone(el.getChildNodes(),doc);
	}
	if (command.equals("set-time")) {
	    return setTime(el.getChildNodes(),doc);
	}
	if (command.equals("current-time-secs")) {
	    return currentTimeSecs(doc);
	}
	return super.exec(el,doc);
    }
    private Element getInt(NodeList nl, Document doc) throws Exception {
	Element el1 = mOwner.exec(Module.getElement(0,nl),doc); // prop
	Element el2 = mOwner.exec(Module.getElement(1,nl),doc); // default val

	if (nodeis(coreModule.NS, "s",el1) &&
	    nodeis(MathModule.NS, "i",el2)) {
	    String field = el1.getTextContent();
	    int dflt = Integer.parseInt(el2.getTextContent());
	    return ((MathModule)(mOwner.mModules.get(MathModule.NS))).int2element(SettingsUtil.getCachableSystemIntOrSet(mOwner.mContext,field, dflt),doc);
	}
	else {
	    throw new Exception("Bad get-int argument");
	}
    }
    private Element setInt(NodeList nl, Document doc) throws Exception {
	Element el1 = mOwner.exec(Module.getElement(0,nl),doc); // prop
	Element el2 = mOwner.exec(Module.getElement(1,nl),doc); // default val

	if (nodeis(coreModule.NS, "s",el1) &&
	    nodeis(MathModule.NS, "i",el2)) {
	    String field = el1.getTextContent();
	    int dflt = Integer.parseInt(el2.getTextContent());
	    SettingsUtil.setSystemInt(mOwner.mContext,field, dflt);
	    return doc.createElement("nil");
	}
	else {
	    throw new Exception("Bad set-prop argument");
	}
    }
    private Element getSecureInt(NodeList nl, Document doc) throws Exception {
	Element el1 = mOwner.exec(Module.getElement(0,nl),doc); // prop
	Element el2 = mOwner.exec(Module.getElement(1,nl),doc); // default val

	if (nodeis(coreModule.NS, "s",el1) &&
	    nodeis(MathModule.NS, "i",el2)) {
	    String field = el1.getTextContent();
	    int dflt = Integer.parseInt(el2.getTextContent());
	    return ((MathModule)(mOwner.mModules.get(MathModule.NS))).int2element(SettingsUtil.getSecureIntOrSet(mOwner.mContext,field, dflt),doc);
	}
	else {
	    throw new Exception("Bad get-int argument");
	}
    }
    private Element setSecureInt(NodeList nl, Document doc) throws Exception {
	Element el1 = mOwner.exec(Module.getElement(0,nl),doc); // prop
	Element el2 = mOwner.exec(Module.getElement(1,nl),doc); // default val

	if (nodeis(coreModule.NS, "s",el1) &&
	    nodeis(MathModule.NS, "i",el2)) {
	    String field = el1.getTextContent();
	    int dflt = Integer.parseInt(el2.getTextContent());
	    SettingsUtil.setSecureInt(mOwner.mContext,field, dflt);
	    return doc.createElement("nil");
	}
	else {
	    throw new Exception("Bad set-prop argument");
	}
    }
    private Element setTimeZone(NodeList nl, Document doc) throws Exception {
	Element el1 = mOwner.exec(Module.getElement(0,nl),doc); // prop
	if (nodeis(coreModule.NS, "s",el1))  {
	    // Update the system timezone value
	    AlarmManager alarm = (AlarmManager) mOwner.mContext
		.getSystemService(Context.ALARM_SERVICE);
	    alarm.setTimeZone(el1.getTextContent());
	    return doc.createElement("nil");
	}
	else {
	    throw new Exception("Bad set-time-zone argument");
	}
    }
    private Element getTimeZone(NodeList nl, Document doc) throws Exception {
	String tzone = TimeZone.getDefault().getID();
	Element els = doc.createElement("s");
	els.appendChild(doc.createTextNode(tzone));
	return els;
    }
    private Element setTime(NodeList nl, Document doc) throws Exception {
	Element el1 = mOwner.exec(Module.getElement(0,nl),doc);
	if (!nodeis(MathModule.NS,"i",el1)) {
	    throw new Exception("Bad set-time argument. Need integer");
	}
	AlarmManager alarm = (AlarmManager) mOwner.mContext
	    .getSystemService(Context.ALARM_SERVICE);
	long i = (long)MathModule.element2int(el1);
	alarm.setTime(i*1000);	// seconds to milliseconds
	return doc.createElement("nil");
    }
    private Element currentTimeSecs(Document doc) throws Exception {
	return ((MathModule)(mOwner.mModules.get(MathModule.NS)))
	    .int2element((int)(System.currentTimeMillis()/1000),doc);
    }
}
