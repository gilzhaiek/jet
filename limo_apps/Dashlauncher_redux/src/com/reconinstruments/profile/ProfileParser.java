package com.reconinstruments.profile;

import java.io.StringReader;
import android.content.Context;
import android.content.ComponentName;
import javax.xml.parsers.DocumentBuilderFactory;
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
import com.reconinstruments.dashlauncherredux.ColumnHolder;
import com.reconinstruments.dashlauncherredux.Column;
import com.reconinstruments.dashlauncherredux.ColumnElementInfo;

/**
 * Class to handle profile parsing. Profiles are originially in XML format. This
 * class parses them and can generate relevant objects used by dashlauncher and
 * dashboard.
 * 
 * @author <a href="mailto:ali@reconinstruments.com">Ali R. Mohazab</a>
 */
public class ProfileParser {
    private static final String TAG = "ProfileParser";
    private Document mDocument;

    /**
     * <code>SAMPLE_PROFILE</code> is used for debug purposes.
     * 
     */
    public static final String SAMPLE_PROFILE = "<profile name=\"default\"><column><element activity=\"com.reconinstruments.dashmusic/.MusicActivity\"/></column><column><element activity=\"com.reconinstruments.dashradar/.ReconRadarActivity\"/></column><column class=\"home\"><element activity=\"com.reconinstruments.dashlivestats/.LiveStatsActivity\"/></column><column><element activity=\"com.reconinstruments.dashnotification/.NotificationsActivity\"/></column><column><element activity=\"com.reconinstruments.dashsettings/.AppsOrSettingsActivity\"/></column></profile>";

    /**
     * Creates a new <code>ProfileParser</code> instance.
     * 
     * @param profile
     *            a <code>String</code> value
     */
    public ProfileParser(String profile) {
	try {
	    InputSource is = new InputSource();
	    is.setCharacterStream(new StringReader(profile));
	    mDocument = DocumentBuilderFactory.newInstance()
		.newDocumentBuilder().parse(is);
	} catch (Exception e) {
	    Log.w(TAG, "Can't parse Profile");
	    e.printStackTrace();
	}
    }

    /**
     * Retruns the name of the profile which the parser is holding.
     * 
     * @return <code>String</code>
     */
    public String getProfileName() {
	NodeList profileNodes = mDocument.getElementsByTagName("profile");
	if (profileNodes == null)
	    return null;
	Element profileNodeE = (Element) profileNodes.item(0);
	if (profileNodeE == null)
	    return null;
	return profileNodeE.getAttribute("name");
    }

    private ComponentName getComponentName(Element columnElementInfoNode) {
	// Helper funciton to extract name attribute from column
	// element info
	String activity = columnElementInfoNode.getAttribute("activity");
	if (activity != null) {
	    ComponentName ci = ComponentName.unflattenFromString(activity);
	    return ci;
	} else {
	    Log.w(TAG, "Bad component name");
	    return null;
	}
    }

    private String getExtras (Node columnElementInfoNode) {
	// Helper function to extract extra info of columnElement in the XML
	NodeList extraInfoNodes = ((Element) columnElementInfoNode)
	    .getElementsByTagName("extras");
	int length = extraInfoNodes.getLength();
	if (length == 0) {
	    Log.v(TAG,"No extra info for the column element");
	    return null;
	}
	else if (length == 1) {
	    Log.v(TAG,"Element has extra info");
	}
	else {
	    Log.w(TAG,"Elements has more than one extra info. Ignore all but first");
	}
	Node extrasNode = extraInfoNodes.item(0);
	String extras = nodeToString(extrasNode);
	Log.v(TAG,"extras is "+extras);
	return extras;
    }

    private static String nodeToString(Node node) {
	// Helper function to convert an xml node to string
	StringWriter sw = new StringWriter();
	try {
	    Transformer t = TransformerFactory.newInstance().newTransformer();
	    t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
	    t.transform(new DOMSource(node), new StreamResult(sw));
	} catch (TransformerException te) {
	    Log.w(TAG,"nodeToString Transformer Exception");
	}
	return sw.toString();
    }

    private boolean isHome(Element e) {
	String classattr = e.getAttribute("class");
	return classattr.equals("home");
    }

    /**
     * Constructs a fully populated <code>ColumnHolder</code> and returns it.
     * 
     * @param context
     *            a <code>Context</code> value
     * @return a <code>ColumnHolder</code> value
     */
    public ColumnHolder generateColumnHolder(Context context) {
	// Here is the strategy: Get column nodes from the xml; for
	// each node: create column and get element nodes of the xml;
	// for each element node create elementinfo; add the element
	// info to the column if it is valid (actually corresponds
	// to an activity); add the column to the column holder if
	// it is not empty;
	Log.d(TAG, "ColumnHolder created");
	ColumnHolder columnHolder = new ColumnHolder();
	int validColumns = -1;
	int validElements = -1;
	NodeList columnNodes = mDocument.getElementsByTagName("column");
	for (int i = 0; i < columnNodes.getLength(); i++) {
	    Node columnNode = columnNodes.item(i);
	    Log.d(TAG, "Column created");
	    Column column = new Column();
	    NodeList columnElementInfoNodes = ((Element) columnNode)
		.getElementsByTagName("element");
	    validElements = -1;
	    for (int j = 0; j < columnElementInfoNodes.getLength(); j++) {
		Element columnElementInfoNode = (Element) columnElementInfoNodes
		    .item(j);
		ComponentName cn = getComponentName(columnElementInfoNode);
		if (cn != null) {
		    String extras = getExtras(columnElementInfoNode);
		    ColumnElementInfo cei = new ColumnElementInfo(cn,extras);
		    if (cei.isValid(context)) {
			if (isHome((Element) columnElementInfoNode)) {
			    column.setHomeElementIndex(validElements+1);
			    columnHolder.setHomeColumnIndex(validColumns+1);
			}
			column.addColumnElementInfo(cei);
			Log.d(TAG, "Element added");
			validElements++;
		    } else {
			Log.w(TAG, "Invalid ColumnElementInfo. Skipping");
		    }
		} else {
		    Log.w(TAG, "Skipping ColumnElementInfo");
		}
	    }
	    if (!column.theList.isEmpty()) { // Only add if column non-empty
		if (isHome((Element) columnNode)) {
		    columnHolder.setHomeColumnIndex(validColumns+1);
		}
		columnHolder.addColumn(column);
		Log.d(TAG, "Column added");
		validColumns++;
	    } else {
		Log.w(TAG, "Skipping empty column");
	    }
	}
	return columnHolder;
    }
}
