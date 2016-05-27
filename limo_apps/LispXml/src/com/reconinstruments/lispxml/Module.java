package com.reconinstruments.lispxml;
import java.io.StringReader;
import android.content.Context;
import android.content.ComponentName;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.*; 
import android.util.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
abstract public class Module {
    protected LispXmlParser mOwner;
    protected String mPrefix = "";
    public abstract String getNamespace();
    public String getPrefix() {
	return mPrefix;
    }
    public static final String TAG ="Module";
    public boolean nodeis(String ns,String tagname, Element n) {
	String nodens = n.getNamespaceURI();
	if (nodens == null) nodens = coreModule.NS;
	String nodetag = n.getLocalName();
	if (nodetag == null) nodetag = n.getTagName();
	return (nodens.equals(ns) && tagname.equals(nodetag));
    }
    public boolean nodeis(String tagname, Element n) {
	return nodeis(getNamespace(), tagname, n);
    }
    public Module(LispXmlParser owner) {
	mOwner = owner;
    }
    public Element exec (Element el, Document doc) throws Exception {
	// Log.v(TAG, "exec");
	// Log.v(TAG, el.getTagName());
	String ns = el.getNamespaceURI();
	if (ns == null) ns = coreModule.NS;
	if (!ns.equals(getNamespace())) {
	    return mOwner.exec(el,doc);
	} else {
	    return el;
	}
    }
    // Get ith node of type element from NodeList
    public static Element getElement(int i, NodeList nl) {
	Element result = null;
	int m = -1;
	for (int j=0;j<nl.getLength(); j++) {
	    if (nl.item(j).getNodeType() == Node.ELEMENT_NODE)
		m++;
	    if (m == i) return (Element) nl.item(j);
	}
	return result;
    }
}
