//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.dashlivestats;

import android.content.*;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.KeyEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import com.reconinstruments.commonwidgets.PopUpDialog;
import com.reconinstruments.dashelement1.ColumnElementFragmentActivity;
import com.reconinstruments.dashlauncher.livestats.CustomDashFragment;
import com.reconinstruments.dashlauncher.livestats.widgets.ReconPowerWidget;
import com.reconinstruments.utils.stats.ActivityUtil;

import com.reconinstruments.utils.DeviceUtils;
import com.reconinstruments.utils.SettingsUtil;
import com.reconinstruments.utils.stats.TranscendUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.reconinstruments.utils.stats.TranscendUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.ArrayList;


public abstract class LiveStatsActivityBase extends ColumnElementFragmentActivity{
    public static final String TAG = "LiveStatsActivityBase";
    protected SharedPreferences mPrefs;
    protected ArrayList<Fragment> dashFragments;
    protected static int DEFAULT_DASH_POSITION = 0;
    protected int currentPosition = DEFAULT_DASH_POSITION;
    protected static int NO_PREVIOUS_POSITION = -1;
    public static final int MAX_NUM_OF_DASHES = 8;
    private static final int POPUP_TIMEOUT = 4;
    protected FrameLayout fragmentFrameLayout;
    protected FragmentManager fm;
    protected Integer mCurrentSport = -1;
    public static final String CUSTOM_DASH_XML_UPDATED =
        "com.reconinstruments.CUSTOM_DASH_XML_UPDATED";
    private PopUpDialog mPopupDialog;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Debug.waitForDebugger();
        Log.v(TAG, "onCreate()");

        this.setContentView(R.layout.livestats_main_layout);
        buildContentView(savedInstanceState);
    }
    
    protected void buildContentView(Bundle savedInstanceState){
        dashFragments = new ArrayList<Fragment>(MAX_NUM_OF_DASHES);
        ArrayList<String> customLayouts = getCustomLayouts();
        if (customLayouts != null) {
            int i = 0;
            for (String layout : customLayouts) {
                // make sure layout is valid & there are now more than
                // max num of dashes
                if (layout != null && !layout.isEmpty() &&
                    dashFragments.size() <= MAX_NUM_OF_DASHES) {
                    dashFragments.add(CustomDashFragment.newInstance(this, layout, customLayouts.size(), i));
                    i++;
                }
            }
        }
        else {
            Log.e(TAG,"no layouts");
            return;
        }
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        currentPosition = mPrefs.getInt("current_dashboard",
                                        DEFAULT_DASH_POSITION);
        if(currentPosition >= dashFragments.size())
            currentPosition = DEFAULT_DASH_POSITION;

        Log.d(TAG, "current dashboard position: "+currentPosition);
        
        // only create fragments once or else they will be recreated..
        // fragments 
        if(savedInstanceState==null && mCurrentSport > -1){
            setDashboard(currentPosition, NO_PREVIOUS_POSITION);
        }
                
        fragmentFrameLayout = (FrameLayout) findViewById(R.id.dashboard_frame);
        fm = getSupportFragmentManager();

        sFullInfo = TranscendUtils.getFullInfoBundle(this);
    }

    public void onPause() {
        super.onPause();

        // No longer need active GPS. Request for passive.
        Intent intent = new Intent("RECON_DEACTIVATE_GPS");
        intent.putExtra("RECON_GPS_CLIENT", TAG);
        sendBroadcast(intent);

        unregisterReceiver(getTranscendReceiver());
        unregisterReceiver(DashXmlUpdateReceiver);
        SharedPreferences.Editor ed = mPrefs.edit();
        ed.putInt("current_dashboard", currentPosition);
        ed.commit();
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        int oldPosition = currentPosition;
        switch (keyCode) {
        case KeyEvent.KEYCODE_DPAD_UP:
            if (currentPosition > 0) {
                currentPosition--;
                setDashboard(currentPosition, oldPosition);
            } else {
                Animation upShake = AnimationUtils.loadAnimation(this, R.anim.shake_up);
                fragmentFrameLayout.startAnimation(upShake);
            }
            return true;

        case KeyEvent.KEYCODE_DPAD_DOWN:
            if (currentPosition < dashFragments.size() - 1) {
                currentPosition++;
                setDashboard(currentPosition, oldPosition);
            } else {
                Animation upShake = AnimationUtils.loadAnimation(this, R.anim.shake_down);
                fragmentFrameLayout.startAnimation(upShake);
            }
            return true;
        default:
            return super.onKeyUp(keyCode,event);
        }
    }

    abstract protected BroadcastReceiver getTranscendReceiver();
    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "onResume");
        updateDashesIfNeeded(this);

        // Request for active GPS.
        Intent intent = new Intent("RECON_ACTIVATE_GPS");
        intent.putExtra("RECON_GPS_CLIENT", TAG);
        sendBroadcast(intent);

        Intent i = registerReceiver(getTranscendReceiver(),
                                    new IntentFilter(TranscendUtils.FULL_INFO_UPDATED));
        registerReceiver(DashXmlUpdateReceiver,
                         new IntentFilter(CUSTOM_DASH_XML_UPDATED));
        if (i != null) {
            sFullInfo = 
                i.getBundleExtra("FullInfo");
        }
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            // Override up and down buttons
        case KeyEvent.KEYCODE_DPAD_DOWN:
        case KeyEvent.KEYCODE_DPAD_UP:
            return true;
        default:
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent("com.reconinstruments.itemhost");
        startActivity(intent);
    }
    /**
     * Describe <code>setDashboard</code> method here.
     *
     * @param position an <code>int</code> value
     * @param prevPosition an <code>int</code> value 
     * If prevPosition == -1, then the fragment view is empty
     */
    public void setDashboard(int position, int prevPosition) {
        Log.d(TAG, "setDashboard(position="+position+",prevPosition="+prevPosition);
        if(dashFragments.size() == 0) return;
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_NONE);
        if (prevPosition > -1) {
            if (position > prevPosition) {
                ft.setCustomAnimations(R.anim.slide_in_bottom,
                                       R.anim.fade_out_top);
            }
            else {
                ft.setCustomAnimations(R.anim.slide_in_top,
                                       R.anim.fade_out_bottom);
            }
            ft.remove(dashFragments.get(prevPosition));
        }
        if (ft.isEmpty()) {
            ft.add(R.id.dashboard_frame, dashFragments.get(position));
        }
        else {
            ft.replace(R.id.dashboard_frame, dashFragments.get(position));
        }
        ft.commitAllowingStateLoss();
        postSetDashboard();
    }

    public void setDashboard(int position, boolean autoRotateToNext) {
        if (dashFragments.size() > 0) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setTransition(FragmentTransaction.TRANSIT_NONE);
            ft.setCustomAnimations(R.anim.slide_in_top, R.anim.fade_out_bottom);
            ft.replace(R.id.dashboard_frame, (Fragment)(dashFragments.get(position)));
            ft.commitAllowingStateLoss();
            postSetDashboard();
        }
    }

    protected boolean hasWidget(Class clz){
        if (dashFragments.size() < 1) return false;
        for(Fragment fragment : dashFragments){
            if(((CustomDashFragment)fragment).hasWidget(clz)) return true;
        }
        return false;
    }
    
    /**
     * let jet and snow do extra work after setup dashboard
     */
    abstract protected void postSetDashboard();
    
    /**
     * Parsese Custom dashlayout from the storage.
     *
     */
    private ArrayList<String> getCustomLayouts() {
        String userCustomDashXML;
        try {
            StringBuffer fileData = new StringBuffer(100);
            BufferedReader reader =
                new BufferedReader(new FileReader(new File(Environment
                                                           .getExternalStorageDirectory()
                                                           .getAbsolutePath()+
                                                           "/ReconApps/Dashboard/user_dashboard.xml")));

            char[] buf = new char[1024];
            int numRead = 0;
            while ((numRead = reader.read(buf)) != -1) {
                fileData.append(buf, 0, numRead);
            }
            reader.close();

            // Remove line breaks to simplify regex processing later
            userCustomDashXML = fileData.toString().replace("\n", "");
        } catch (Exception e) {
            Log.e(TAG, e.toString());

            // Couldn't load custom xml
            return null;
        }
        
        ArrayList<String> matches = new ArrayList<String>();

        // Get the list of layouts from the xml doc
        try {
            DocumentBuilderFactory dbf =
                DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(userCustomDashXML));
            Document doc = db.parse(is);
            mCurrentSport =
                new Integer(ActivityUtil.getCurrentSportsType(sFullInfo));
            // get layout element
            NodeList layoutNodes = doc.getElementsByTagName("layout");
            String layoutSport;
            Node l;
            for (int i=0; i< layoutNodes.getLength(); i++) {
                l = layoutNodes.item(i);
                layoutSport = ((Element)l).getAttribute("sport_id");
                if (layoutSport.equals("") || // Use short circuit!
                    mCurrentSport.equals(new Integer(layoutSport))) {
                    matches.add(elementToString((Element)l));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return matches;
    }

    BroadcastReceiver DashXmlUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                updateDashesIfNeeded(c);
            }
        };
    protected void updateDashesIfNeeded(Context context) {
        if (SettingsUtil.getCachableSystemIntOrSet(context,CUSTOM_DASH_XML_UPDATED,0)
            == 1) {
            Log.v(TAG,"updating dashes");
            buildContentView(null);
            SettingsUtil.setSystemInt(context,CUSTOM_DASH_XML_UPDATED,0);
            mPopupDialog = new PopUpDialog("Updated", "Dashboards",
                    this, PopUpDialog.ICONTYPE.CHECKMARK).showDialog(POPUP_TIMEOUT, null);
        }
    }

    ///////////////////////////////////////////////////////////////
    // New paradigm for accessing data from Transcend service
    ///////////////////////////////////////////////////////////////
    static Bundle sFullInfo;
    ///////////////////////////////////////////////////////////////
    protected boolean inActivity() {
        return !(ActivityUtil.getActivityState(sFullInfo) ==
                ActivityUtil.SPORTS_ACTIVITY_STATUS_NO_ACTIVITY);
    }

    public static String elementToString(Element el) {
        StringWriter sw = new StringWriter();
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            t.setOutputProperty(OutputKeys.INDENT, "no");
            t.transform(new DOMSource(el), new StreamResult(sw));
        } catch (TransformerException te) {
            System.out.println("nodeToString Transformer Exception");
        }
        return sw.toString();
    }

}
