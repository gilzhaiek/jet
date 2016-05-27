
package com.reconinstruments.dashboard;

import android.os.Environment;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import com.reconinstruments.utils.FileUtils;

/**
 * <code>DashLayout</code> load layout file from PROFILE_FOLDER, it uses
 * pre-defined DEFAULT_LAYOUT instead when the file is unavailable.
 */
public class DashLayout {

    private static final String TAG = DashLayout.class.getSimpleName();

    /**
     * The folder in which all the profile XMLs reside
     */
    public static final String PROFILE_FOLDER = "ReconApps/Dashboard/";
    /**
     * The name of the file whose content is the name of the chosen profile file
     */
    public static final String PROFILE_NAME = "dash_layout";

    private static final String TAG_ID = "dash-layout";
    private static final String TAG_MAIN = "main-metric";
    private static final String TAG_SECONDARY = "secondary-metrics";

    public String mainMetric;
    public List<String> secondaryMetrics = new ArrayList<String>();

    /**
     * load layout file by specified sports type
     * @param sportsType
     * @return
     */
    public static DashLayout loadLayout(int sportsType) {
        boolean handleMainAttribute = false;
        boolean handleSecondaryAttribute = false;
        boolean parseAttribute = false;
        String fileName = PROFILE_FOLDER + PROFILE_NAME + ".xml";
        String xmlStr = readFromFile(fileName);
        if(xmlStr == null){
            return null;
        }
        Log.i(TAG, "loaded layout file: " + fileName);
        DashLayout layout = new DashLayout();
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new StringReader(xmlStr));
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (handleMainAttribute && parseAttribute) {
                        layout.mainMetric = xpp.getName().trim();
                        Log.i(TAG, "layout.mainMetric = " + xpp.getName().trim());
                    } else if (handleSecondaryAttribute && parseAttribute) {
                        layout.secondaryMetrics.add(xpp.getName().trim());
                        Log.i(TAG, "layout.secondaryMetrics = " + xpp.getName().trim());
                    }
                    if (xpp.getName().trim().equalsIgnoreCase(TAG_MAIN)) {
                        handleMainAttribute = true;
                    } else if (xpp.getName().trim().equalsIgnoreCase(TAG_SECONDARY)) {
                        handleSecondaryAttribute = true;
                    } else if(xpp.getName().trim().equalsIgnoreCase(TAG_ID)){
                        int id = Integer.valueOf(xpp.getAttributeValue(null, "id"));
                        if(id != sportsType){
                            Log.i(TAG, "skip to parse sports id " + id);
                        }else{
                            Log.i(TAG, "parse sports id " + id);
                            parseAttribute = true;
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    if (xpp.getName().trim().equalsIgnoreCase(TAG_MAIN)) {
                        handleMainAttribute = false;
                    } else if (xpp.getName().trim().equalsIgnoreCase(TAG_SECONDARY)) {
                        handleSecondaryAttribute = false;
                    } else if(xpp.getName().trim().equalsIgnoreCase(TAG_ID)){
                        parseAttribute = false;
                    }
                }
                eventType = xpp.next();
            }
            if(layout.mainMetric == null){
                return null;
            }
            return layout;
        } catch (XmlPullParserException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * read file from sd card by specified file full name
     * @param relativePath
     * @return
     */
    private static String readFromFile(String relativePath) {
        String xmlStr = null;
        try {
            xmlStr = FileUtils.getStringFromSDCard(Environment.getExternalStorageDirectory(),
                    relativePath);
            return xmlStr;
        } catch (Exception e) {
            Log.i(TAG, "There is no custom dashboard layout");
            return null;
        }
    }
}
