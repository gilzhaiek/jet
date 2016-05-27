package demo.reconinstruments.com.buddytracking;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created by gil on 4/24/15.
 */
public class BuddyInfoParser {
    private final static String TAG = "BuddyInfoParser";

    static final String FRIEND_NODE_NAME = "friend";
    static final String FRIEND_ATTR_ID = "id";
    static final String FRIEND_ATTR_LAT = "lat";
    static final String FRIEND_ATTR_LNG = "lng";
    static final String FRIEND_ATTR_LOCATION_TIME = "loc_time";
    static final String FRIEND_ATTR_NAME = "name";

    /**
     * Parse a message string which is a XML chunk return an object
     *
     * @param intent
     * @param message
     * @return a {@link Document} instance, or <code>null</code> if the message does not have the correct <b>intent</b> or an Exception is caught
     */
    public static Document validate(String intent, String message) {
        // parse the buddyInfo which is a xml trunk for a list of buddy information
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(message));

            Document doc = db.parse(is);
            NodeList nodes = doc.getElementsByTagName("recon");
            Node rootNode = nodes.item(0);
            NamedNodeMap nnm = rootNode.getAttributes();
            Node n = nnm.getNamedItem("intent");
            String type = n.getNodeValue();

            // verify the message has the right intent
            if (type.compareTo(intent) != 0) {
                Log.e(TAG, "The XML protocol's intent should be " + intent);
                return null;
            } else {
                Log.v(TAG, "Has right intent");
                return doc;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse xml", e);
        }
        return null;
    }

    public static void parse(String intent, String message, List<BuddyInfo> buddyInfoList) {
        buddyInfoList.clear();
        Document doc = validate(intent, message);
        if (doc == null) {
            return;
        }

        Node rootNode = doc.getElementsByTagName("recon").item(0);

        try {
            Node buddyNode = rootNode.getFirstChild();

            Log.i(TAG, "Getting BuddyNode " + buddyNode);

            while (buddyNode != null) {
                // check if it is friend node
                if (buddyNode.getNodeType() == Node.ELEMENT_NODE && buddyNode.getNodeName().compareToIgnoreCase(FRIEND_NODE_NAME) == 0) {
                    NamedNodeMap attrs = buddyNode.getAttributes();

                    // id
                    Node idAttr = attrs.getNamedItem(FRIEND_ATTR_ID);
                    int id = Integer.parseInt(idAttr.getNodeValue());

                    // name
                    Node nameAttr = attrs.getNamedItem(FRIEND_ATTR_NAME);
                    String name = nameAttr.getNodeValue();
                    Log.i(TAG, "Got a friend node name = " + name);

                    // lat
                    Node latAttr = attrs.getNamedItem(FRIEND_ATTR_LAT);
                    double lat = Double.parseDouble(latAttr.getNodeValue());

                    // lng
                    Node lngAttr = attrs.getNamedItem(FRIEND_ATTR_LNG);
                    double lng = Double.parseDouble(lngAttr.getNodeValue());

                    // location time
                    Node locationTimeAttr = attrs.getNamedItem(FRIEND_ATTR_LOCATION_TIME);
                    long locationTime = Long.parseLong(locationTimeAttr.getNodeValue());

                    BuddyInfo buddy = new BuddyInfo(id, name, lat, lng, locationTime);
                    buddyInfoList.add(buddy);
                }
                buddyNode = buddyNode.getNextSibling();
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        } catch (NullPointerException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }
    }
}
