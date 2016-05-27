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

public class LispXmlParser {
    private static final String TAG = "LispXmlParser";
    private Document mDocument;
    public Element mResponse;
    public Hashtable mSymbols;
    public Hashtable mModules;
    public Context mContext;
    public void loadModule(Module m) {
	mModules.put(m.getNamespace(), m);
    }
    public LispXmlParser(Context c, String program) {
	mContext = c;
	mModules = new Hashtable();
	mModules.put(coreModule.NS,
		     new coreModule(this));
	mSymbols = new Hashtable();
	try {
	    mDocument = getDoc(program);
	    mDocument.normalizeDocument();
	} catch (Exception e) {
	    Log.w(TAG, "Can't parse program");
	    e.printStackTrace();
	}
    }

    private Document getDoc(String program) throws Exception {
	    InputSource is = new InputSource();
	    is.setCharacterStream(new StringReader(program));
	    DocumentBuilderFactory factory =
		DocumentBuilderFactory.newInstance();
	    factory.setNamespaceAware(true);
	    return factory.newDocumentBuilder().parse(is);
    }
    public Element execute() {
	return execute(mDocument);
    }

    public Element execute(Document doc) {
	mResponse = doc.createElement("lispxml");
	NodeList lispxml = doc.getElementsByTagName("lispxml");
	Element el;
	try {
	    el =  exec((Element)lispxml.item(0),doc);
	}
	catch (Exception e) {
	    el = doc.createElement("error");
	    if (e.getMessage() != null) {
		el.appendChild(doc.createTextNode(e.getMessage()));
	    }
	    e.printStackTrace();
	}
	mResponse.appendChild(el);
	return mResponse;
    }

    public Element execute(String program) {
	Document d;
	try {
	    d = getDoc(program);
	    return execute(d);
	} catch (Exception e) {
	    Log.w(TAG, "Can't parse program");
	    e.printStackTrace();
	}
	return null;
    }

    public Element exec (Element el,Document doc) throws Exception {
	// If user defined do that
	String command = el.getTagName();
	if (mSymbols.get(command) != null) {
	    // Symbol has been defined or overwritten
	    return exec((Element)mSymbols.get(command),doc);
	}
	// Not user defined relegate to the correct module
	String ns = el.getNamespaceURI();
	if (ns == null) ns = coreModule.NS;
	return ((Module)mModules.get(ns)).exec(el,doc);
    }


    public static String elementToString(Element el) {
	return elementToString(el,true);
    }
    public static String elementToString(Element el, boolean xmlDeclare) {
	StringWriter sw = new StringWriter();
	try {
	    Transformer t = TransformerFactory.newInstance().newTransformer();
	    if (xmlDeclare) {
		t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
	    } else {
		t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
	    }
	    t.setOutputProperty(OutputKeys.INDENT, "yes");
	    t.transform(new DOMSource(el), new StreamResult(sw));
	} catch (TransformerException te) {
	    System.out.println("nodeToString Transformer Exception");
	}
	return sw.toString();
    }
}
