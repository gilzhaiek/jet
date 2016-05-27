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
public class coreModule extends Module {
    public static final String NS = "com.reconinstruments.lispxml.module.core";
    public static final String TAG = "coreModule";
    public coreModule (LispXmlParser lp) {
	super(lp);
	if (lp.mModules.get(MathModule.NS) == null) {
	    lp.loadModule(new MathModule(lp));
	}
    }
    @Override
    public String getNamespace() {
	return NS;
    }
    @Override
    public Element exec (Element el, Document doc) throws Exception{
	String command = el.getLocalName();
	if (command.equals("s")) { // string literal
	    return el;
	}
	if (command.equals("nil")) { // nil and false
	    return el;
	}
	if (command.equals("if")) {
	    return If(el.getChildNodes(),doc);
	}
	if (command.equals("lispxml")) {
	    return progn(el.getChildNodes(),doc);
	}
	if (command.equals("progn")) {
	    return progn(el.getChildNodes(),doc);
	}
	if (command.equals("exec-id")) {
	    return execId(el.getChildNodes(),doc);
	}
	if (command.equals("setq")) {
	    return setq(el.getChildNodes(),doc);
	}
	if (command.equals("quote")) {
	    return  getElement(0,el.getChildNodes());
	}
	if (command.equals("eval")) {
	    return eval(el.getChildNodes(),doc);
	}
	if (command.equals("noprogn")) {
	    return doc.createElement("nil");
	}
	if (command.equals("cat")) {
	    return cat(el.getChildNodes(),doc);
	}
	if (command.equals("leaf")) {
	    return leaf(el.getChildNodes(),doc);
	}
	if (command.equals("branch")) {
	    return branch(el.getChildNodes(),doc);
	}
	if (command.equals("list")) {
	    return list(el.getChildNodes(),doc);
	}
	if (command.equals("car")) {
	    return car(el.getChildNodes(),doc);
	}
	if (command.equals("cdr")) {
	    return cdr(el.getChildNodes(),doc);
	}
	if (command.equals("set-attribute")) {
	    return set_attribute(el.getChildNodes(),doc);
	}
	if (command.equals("to-string")) {
	    return elementToString(el.getChildNodes(),doc);
	}
	if (command.equals("sleep")) {
	    return sleep(el.getChildNodes(),doc);
	}
	return super.exec(el,doc);
    }
    Element execId(NodeList nl, Document doc) throws Exception {
	Element el =  getElement(0,nl);
	el = mOwner.exec(el,doc);
	String tag = el.getTagName();
	if (tag.equals("s")) {
	    String id = el.getTextContent();
	    //Log.v("LispXmlParser", "Inside execId");
	    Element theElement = doc.getElementById(id);
	    //Log.v("The tag is",theElement.getTagName());
	    return mOwner.exec(theElement,doc);
	}
	else {
	    Log.e("LispXmlError", "Bad execid argument" + tag);
	}
	return el;
    }

    Element progn(NodeList nl, Document doc) throws Exception {
	Element result = null;
	Node n;
	for (int i = 0; i< nl.getLength(); i++) {
	    n = nl.item(i);
	    if (n.getNodeType() == Node.ELEMENT_NODE) {
		result = mOwner.exec((Element)n,doc);
	    }
	}
	return result;
    }

    Element If(NodeList nl, Document doc) throws Exception {
	Element cond = getElement(0,nl);
	Element iftrue = getElement(1,nl);
	Element iffalse = getElement(2,nl);
	cond = mOwner.exec(cond,doc);
	if (nodeis("nil",cond)) {
	    return mOwner.exec(iffalse,doc);
	} else {
	    return mOwner.exec(iftrue,doc);
	}
    }

    Element setq(NodeList nl, Document doc) throws Exception {
	Element symbol = mOwner.exec(getElement(0,nl),doc);
	Element value = mOwner.exec(getElement(1,nl),doc);
	if (nodeis("s",symbol)) {
	    String id = symbol.getTextContent();
	    mOwner.mSymbols.put(id,value);
	}
	else {
	    Log.e("LispXml","bad argument");
	}
	return doc.createElement("nil");	
    }
    Element cat(NodeList nl,Document doc) throws Exception {
	Node n;
	String result="";
	Element el;
	for (int i = 0; i< nl.getLength(); i++) {
	    n = nl.item(i);
	    if (n.getNodeType() == Node.ELEMENT_NODE) {
		el = mOwner.exec((Element)n,doc);
		result = result + el.getTextContent();
	    }
	}
	el = doc.createElement("s");
	el.appendChild(doc.createTextNode(result));
	return el;
    }
    Element leaf(NodeList nl, Document doc) throws Exception {
	Element elName = mOwner.exec(getElement(0,nl),doc);
	Element elNS = mOwner.exec(getElement(1,nl),doc);
	Element elText = mOwner.exec(getElement(2,nl),doc);
	Element result;
	if (!(nodeis("nil",elNS) || nodeis("s",elNS)) ||
	    !nodeis("s",elName) || !nodeis("s",elText)) {
	    throw new Exception("Bad leaf argument");
	}
	String name = elName.getTextContent();
	String text = elText.getTextContent();
	if (nodeis("nil",elNS)) {
	    result = doc.createElement(name);
	}
	else {
	    String ns = elNS.getTextContent();
	    result = doc.createElementNS(ns,name);
	}
	result.appendChild(doc.createTextNode(text));
	return result;
    }
    Element branch(NodeList nl, Document doc) throws Exception {
	Element elName = mOwner.exec(getElement(0,nl),doc);
	Element elNS = mOwner.exec(getElement(1,nl),doc);
	Element elList = mOwner.exec(getElement(2,nl),doc);
	Element result;
	if (!(nodeis("nil",elNS) || nodeis("s",elNS)) ||
	    !nodeis("s",elName) || !nodeis("list",elList)) {
	    throw new Exception("Bad branch argument");
	}
	String name = elName.getTextContent();
	if (nodeis("nil",elNS)) {
	    result = doc.createElement(name);
	}
	else {
	    String ns = elNS.getTextContent();
	    result = doc.createElementNS(ns,name);
	}
	NodeList lnl = elList.getChildNodes();
	Node n;
	for (int i = 0; i <lnl.getLength(); i++) {
	    n = lnl.item(i);
	    if (n.getNodeType() == Node.ELEMENT_NODE) {
		result.appendChild(n);
	    }
	}
	return result;
    }
	
    Element list(NodeList nl, Document doc) throws Exception {
	Element result = doc.createElement("list");
	Element el;
	Node n;
	for (int i = 0; i< nl.getLength(); i++) {
	    n = nl.item(i);
	    if (n.getNodeType() == Node.ELEMENT_NODE) {
		el = mOwner.exec((Element)n,doc);
		result.appendChild(el);
	    }
	}
	return result;
    }
    Element car(NodeList nl, Document doc) throws Exception {
	Element el = mOwner.exec(getElement(0,nl),doc);
	return getElement(0,el.getChildNodes());
    }

    Element cdr(NodeList nl, Document doc) throws Exception {
    	Element el = mOwner.exec(getElement(0,nl),doc);
    	Element elToRemove = getElement(0,el.getChildNodes());
    	if (elToRemove == null) return doc.createElement("nil");
    	el.removeChild(elToRemove);
	return el;
    }

    Element set_attribute(NodeList nl, Document doc) throws Exception {
	Element el = mOwner.exec(getElement(0,nl),doc);
	Element el_name = mOwner.exec(getElement(1,nl),doc);
	Element el_ns = mOwner.exec(getElement(2,nl),doc);
	Element el_value = mOwner.exec(getElement(3,nl),doc);
	if (!(nodeis("nil",el_ns) || nodeis("s",el_ns)) ||
	    !nodeis("s",el_name) || !nodeis("s",el_value)) {
	    throw new Exception("Bad set-attribuute argument");
	}
	String atr = el_name.getTextContent();
	String val = el_value.getTextContent();
	if (!nodeis("nil",el_ns)) {
	    el.setAttribute(atr,val);
	}
	else {
	    String ns = el_ns.getTextContent();
	    el.setAttributeNS(ns,atr,val);
	}
	return el;
    }
    
    Element eval(NodeList nl, Document doc) throws Exception {
	Element el = mOwner.exec(getElement(0,nl),doc);
	return mOwner.exec(el,doc);
    }

    Element elementToString(NodeList nl, Document doc) throws Exception {
	Element el = mOwner.exec(getElement(0,nl),doc);
	String result = LispXmlParser.elementToString(el,false);
	Element els = doc.createElement("s");
	els.appendChild(doc.createTextNode(result));
	return els;
    }
    Element sleep(NodeList nl,Document doc) throws Exception {
	Element el = mOwner.exec(getElement(0,nl),doc);
	if (nodeis(MathModule.NS,"i",el)) {
	    Thread.sleep(MathModule.element2int(el));
	    return doc.createElement("nil");
	}
	else {
	    throw new Exception("Bad sleep argument");
	}
    }
}
