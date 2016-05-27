package com.reconinstruments.lispxml;
import java.util.*; 
import android.util.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class MathModule extends Module {
    public static final String TAG = "MathModule";
    public static final String NS = "com.reconinstruments.lispxml.module.math";
    protected String mPrefix= NS; 	
				// then prefix not defined
    public MathModule (LispXmlParser lp) {
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
	if (command.equals("add")||
	    command.equals("sub")||
	    command.equals("div")||
	    command.equals("mul")||
	    command.equals("mod")||
	    command.equals("gt") ||
	    command.equals("lt") ||
	    command.equals("eq")) {
	    return operator2(el.getChildNodes(),doc,command);
	}
	if (command.equals("i")) {
	    return el;
	}
	
	return super.exec(el,doc);
    }

    private Element operator2(NodeList nl,Document doc, String op) throws Exception {
	Element el1 = mOwner.exec(getElement(0,nl),doc);
	Element el2 = mOwner.exec(getElement(1,nl),doc);
	if (nodeis("i",el1) && nodeis("i",el2)) {
	    int i1 = Integer.parseInt(el1.getTextContent());
	    int i2 = Integer.parseInt(el2.getTextContent());
	    if (op.equals("gt") || op.equals("lt") || op.equals("eq")) {
		return compare(i1,i2,op,doc);
	    }
	    if (op.equals("add") || op.equals("sub") || op.equals("div")
		|| op.equals("mul") || op.equals("mod")) {
		return arithmetic (i1,i2,op,doc);
	    }
	}
	else {
	    Log.e(TAG,"Can't apply gt on nodes other than i");
	}
	return doc.createElement("nil");
    }
    private Element compare(int i1, int i2, String op,
			    Document doc) throws Exception {
	boolean result = false;
	if (op.equals("gt")){
	    result = (i1 > i2);
	} else if (op.equals("lt")) {
	    result = (i1 < i2);
	} else if (op.equals("eq")) {
	    result = (i1 == i2);
	}
	if (result) {
	    return doc.createElement("true");
	}
	else {
	    return doc.createElement("nil");
	}
    }
    private Element arithmetic (int i1, int i2, String op,Document doc)
	throws Exception  {
	int result = 0;
	if (op.equals("add")) {
	    result = i1 + i2;
	}
	else if (op.equals("sub")) {
	    result = i1 - i2;
	}
	else if (op.equals("div")) {
	    result = i1 / i2;
	}
	else if (op.equals("mul")) {
	    result = i1 * i2;
	}
	else if (op.equals("mod")) {
	    result = i1 % i2;
	}
	return int2element(result, doc);
    }

    public Element int2element(int i, Document doc) {
	Element el = doc.createElementNS(NS,mPrefix+":i");
	el.appendChild(doc.createTextNode(""+i));
	return el;
    }

    public static Element i2s (Element i,Document doc) {
	Element el = doc.createElement("s");
	el.appendChild(doc.createTextNode(i.getTextContent()));
	return el;
    }
    public static int element2int(Element el) {
	return Integer.parseInt(el.getTextContent());
    }
}
