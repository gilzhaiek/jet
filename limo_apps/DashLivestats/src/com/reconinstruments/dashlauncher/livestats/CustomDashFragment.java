//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.dashlauncher.livestats;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.reconinstruments.commonwidgets.BreadcrumbView;
import com.reconinstruments.dashlauncher.livestats.widgets.ReconChronoWidget4x1;
import com.reconinstruments.dashlauncher.livestats.widgets.ReconDashboardHashmap;
import com.reconinstruments.dashlauncher.livestats.widgets.ReconDashboardWidget;
import com.reconinstruments.dashlauncher.livestats.widgets.ReconPowerWidget;
import com.reconinstruments.dashlauncher.livestats.widgets.ReconWidgetHolder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class CustomDashFragment extends Fragment implements IDashFragment {
    public static final String TAG = "CustomDashFragment";
    //private String layoutXML = "";
    private ReconDashboardLayout layout;
    private Messenger mMODConnectionMessenger;
    private Bundle mFullInfoBundle = null; // Most recent full info bundle
    private static int mSize;
    private int mPos;
    // Breadcrumb toast and view
    private Toast breadcrumbToast;
    private BreadcrumbView mBreadcrumbView;
    private View mWidgetView = null;
    private Handler mHandler = new Handler();
    private String mLayoutXML;
    private Context mContext;
    List<ReconDashboardWidget> mWidgets = new ArrayList<ReconDashboardWidget>();
    
    public static CustomDashFragment newInstance(Activity activity, String layoutXML, int size, int pos) {
        CustomDashFragment c = new CustomDashFragment();
        // Supply num input as an argument.
        c.mPos = pos;
        c.mLayoutXML = layoutXML;
        c.mContext = activity.getApplicationContext();
        c.buildWidgets(layoutXML); //prepare the widget objects
        Bundle args = new Bundle();
        args.putString("layout", layoutXML);
        c.setArguments(args);
        mSize = size;
        return c;
    }
        
    public String getLayout() {
        String layoutXML = getArguments() != null ? getArguments().getString("layout") : "";
        return layoutXML;
    }

    public boolean hasChronoWidget() {
        return layout.hasChrono();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (mWidgetView == null) {
            mWidgetView = buildView();
            mWidgetView.setPadding(30, 20, 30, 10);
            return mWidgetView;
        }
        ((ViewGroup)(mWidgetView.getParent())).removeView(mWidgetView); // black magic
        // container.removeView(mWidgetView); // will crash cuz don't know container
        mWidgetView.setPadding(30, 20, 30, 10);
        return mWidgetView;
    }
    
    public boolean hasWidget(Class clz) {
        for(ReconDashboardWidget widget : mWidgets){
            if(clz.isInstance(widget)) return true;
        }
        return false;
    }

    //generate widgets from xml string provided and put into the widget list
    private void buildWidgets(String xml){
        ReconDashboardHashmap dashboardHashmap = new ReconDashboardHashmap();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml));
            Document doc = db.parse(is);

            // get layout element
            Node layoutNode = doc.getElementsByTagName("layout").item(0);
            String id = layoutNode.getAttributes().getNamedItem("layout_id")
                .getNodeValue();
            layout = new ReconDashboardLayout(id, mContext);

            // Get all the widgets of the layout from XML
            NodeList widgetList = layoutNode.getChildNodes();

            String widgetName;
            for (int i = 0; i < widgetList.getLength(); i++) {

                // Get the next widget from widget list
                Node w = widgetList.item(i);
                if (w.hasAttributes()) { // make sure it is a valid one
                    // Has attributes

                    widgetName = w.getAttributes().getNamedItem("name")
                        .getNodeValue();

                    // Generate ReconDashboardWidget Object from widgetName
                    int widgetId = dashboardHashmap.WidgetHashMap
                        .get(widgetName);
                    ReconDashboardWidget widget = ReconDashboardWidget
                                .spitReconDashboardWidget(widgetId, mContext);
                    mWidgets.add(widget);
                }
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private View buildView() {
        for(ReconDashboardWidget widget : mWidgets){
                layout.mAllWidgets.add(widget);
        }
        layout.populate();
        return layout.mTheBigView;
    }

    public void updateFields(Bundle fullInfoBundle, boolean inActivity) {
        if(layout == null) // update the fields only the view is ready 
            return;
        for (ReconDashboardWidget r : layout.mAllWidgets) {
            r.updateInfo(fullInfoBundle, inActivity);
        }
    }

    class ReconDashboardLayout {
        public ReconDashboardHashmap dashboardhash = null ;
        public View mTheBigView;
        public ArrayList<ReconDashboardWidget> mAllWidgets;
        public String id;

        public ReconDashboardLayout(String layout_id, Context c) {
            if (dashboardhash == null) dashboardhash = new ReconDashboardHashmap();
            id = layout_id;
            mAllWidgets = new ArrayList<ReconDashboardWidget>();
            // Let's inflate the baby:
            LayoutInflater inflater = (LayoutInflater) c
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mTheBigView = inflater.inflate(
                                           dashboardhash.LayoutHashMap.get(layout_id), null);
        }

        public void populate() {
            // Goes through all
            for (int i = 0; i < mAllWidgets.size(); i++) {
                ((ReconWidgetHolder) mTheBigView
                 .findViewById(dashboardhash.PlaceholderMap.get(i)))
                .addView(mAllWidgets.get(i));

            }
        }

        public void updateInfo(Bundle fullInfo, boolean inActivity) {
            for (int i = 0; i < mAllWidgets.size(); i++) {
                mAllWidgets.get(i).updateInfo(fullInfo, inActivity);
            }
        }

        public boolean hasChrono() {
            for (Object w : mAllWidgets) {
                if (w instanceof ReconChronoWidget4x1)
                    return true;
            }
            return false;
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        hideVertBreadcrumb();
    }

    @Override
    public void onResume() {
        super.onResume();
        showVertBreadcrumb(getActivity().getApplicationContext(), mSize, mPos);
    }
    public void hideVertBreadcrumb(){
        if(breadcrumbToast != null){
            breadcrumbToast.cancel();
            breadcrumbToast = null;
        }
    }

    public void showVertBreadcrumb(final Context context, final int size, final int currentPosition) {
        if(size == 0) return; // don't show breadcrumb if fragment is not ready
        mHandler.removeCallbacksAndMessages(null);

        mHandler.postDelayed(new Runnable() { 
            public void run() { 
                if(context != null){
                    int[] dashFrags = new int[size];
                    for(int i=0; i<dashFrags.length; i++)
                        dashFrags[i] = BreadcrumbView.DYNAMIC_ICON;

                    mBreadcrumbView = new BreadcrumbView(context, false, true, currentPosition, dashFrags);

                    mBreadcrumbView.invalidate();

                    if(breadcrumbToast == null) 
                        breadcrumbToast = Toast.makeText(context, "", Toast.LENGTH_SHORT);

                    breadcrumbToast.setGravity(Gravity.RIGHT | Gravity.BOTTOM, 0, 0);
                    breadcrumbToast.setView(mBreadcrumbView);
                    breadcrumbToast.show();
                }
            }
        }, 0 ); 
        
    }

}
