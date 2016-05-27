package com.reconinstruments.mapsdk.mapfragment.subclass;

import android.content.Context;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;


import com.reconinstruments.mapsdk.R;


import com.reconinstruments.mapsdk.mapview.SansRegularTextView;
import com.reconinstruments.mapsdk.mapview.renderinglayers.buddylayer.BuddyItem;
import java.util.ArrayList;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.mapsdk.mapview.MapView;
import com.reconinstruments.utils.ConversionUtil;
import com.reconinstruments.utils.SettingsUtil;


public class MapFragment_Find extends MapFragment_Zoom implements MapView.OnBuddiesUpdatedListener {
	private static final String 	TAG = "MapFragment_Find";


    private final String STRING_FORMAT = "%.0f";

	//ImageView 			mZoomImgView = null;
	int currentBuddyIndex = 0;
	int mNumBuddies = 0;
	public ArrayList<BuddyItem>	mSortedBuddyList;
	
	public boolean isDebug = true;

    private Context mContext;
	protected View view;

    protected RelativeLayout buddyDistanceContainer = null;
	protected SansRegularTextView zoomButton = null;
	protected SansRegularTextView findButton = null;
    protected SansRegularTextView buddyDistance = null;
	protected ImageView fadeBackground = null;
	protected boolean	mStatusBarVisible = true;
    protected boolean   mShowFindFriendsButton = false;
    protected boolean   mInBaseMapMode = true;
    protected volatile String mChosenBuddyID = null;

    private TextAppearanceSpan mTextAppearanceSpan;


	public MapFragment_Find() {
		super();
		currentBuddyIndex = 0;
        mChosenBuddyID = "";
	}

    public void setContext(Context context){
        mContext = context;
        mTextAppearanceSpan = new TextAppearanceSpan(context, R.style.SubscriptTextStyle);
    }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = super.onCreateView(inflater, container, savedInstanceState, R.layout.fragment_map_find);

        buddyDistanceContainer = (RelativeLayout) view.findViewById(R.id.buddy_distance_container);
		zoomButton = (SansRegularTextView) view.findViewById(R.id.zoomBtn);
		findButton = (SansRegularTextView) view.findViewById(R.id.findBtn);
        buddyDistance = (SansRegularTextView) view.findViewById(R.id.buddy_distance);
		fadeBackground = (ImageView) view.findViewById(R.id.fadeBackground);

        showOrHideBuddyDistance(false);

		mMapView.SetCameraToRotateWithUser(true);
		mMapView.SetCameraToPitchWithUser(true);
        mMapView.setOnBuddiesUpdatedListener(this);
		
	   return view;
	}
	
	@Override
	public void ConfigurePostMapViewInit() {
		super.ConfigurePostMapViewInit();  // sets mMapView
		//mMapView.SetCameraToFollowUser(true);
		//mMapView.SetCameraToRotateWithUser(true);
		//mMapView.ShowUserIcon(true);
	}


	@Override
	protected void gotoMessageMode() {
		super.gotoMessageMode();
		
	}
	
	@Override
	protected void gotoLoadMapMode() {
		super.gotoLoadMapMode();
		
	}
	
	@Override
	protected void gotoBaseMapMode() {
		super.gotoBaseMapMode();
		mMapView.SetCameraToFollowUser(true);
		mMapView.SetCameraToRotateWithUser(true);
		mMapView.SetCameraToPitchWithUser(!mPitchDisabled);
		showMapButtonsFromThread();
        mInBaseMapMode = true;
        mChosenBuddyID = "";
        showOrHideBuddyDistance(false);
	}
	
	public void gotoBaseMapModeFromFindBuddy() {
		super.gotoBaseMapMode();
		mMapView.SetCameraToFollowUser(true);
		mMapView.SetCameraToRotateWithUser(true);
		mMapView.SetCameraToPitchWithUser(!mPitchDisabled);
		mMapView.setFindModeEnabled(false);
		mMapView.setHighlightedBuddy("");
		showMapBtns(true);
        mInBaseMapMode = true;
        mChosenBuddyID = "";
        showOrHideBuddyDistance(false);
	}
	
	public void gotoFindBuddyMode() {
		mMapView.SetCameraToFollowUser(false);
		mMapView.SetCameraToRotateWithUser(false);
		mMapView.SetCameraToPitchWithUser(false);
		mMapView.setFindModeEnabled(true);
		hideMapBtns();
        mInBaseMapMode = false;
        buddyDistance.setText("");
        buddyDistanceContainer.setVisibility(View.VISIBLE);
        buddyDistanceContainer.setAlpha(0f);
    }
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		super.onKeyDown(keyCode, event);
		return false;

	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
	    return false;
	}
	
	public void setStatusBarVisible(boolean isVisible) {
		mStatusBarVisible = isVisible;
	}
	
   public ArrayList<BuddyItem>  getSortedBuddyList()
   {
	  
	   mSortedBuddyList = mMapView.GetSortedBuddyList();
	   
	   mNumBuddies = mSortedBuddyList.size();
	   return mSortedBuddyList;
	   
   }
   
   public BuddyItem getNextBuddy()
   {
	   ++currentBuddyIndex;
	   
	   BuddyItem buddyItem = mSortedBuddyList.get(currentBuddyIndex);
	   
	   return buddyItem;
   }
   
   public BuddyItem getNextBuddy(int buddyIndex)
   {
	   
	   BuddyItem buddyItem = mSortedBuddyList.get(buddyIndex);
	   
           return buddyItem;
   }
   
   public GeoRegion getGeoRegionContainBuddy( int buddyIndex )
   {
       if(buddyIndex < 0 || buddyIndex > mNumBuddies) {
           mMapView.setHighlightedBuddy("");
           Log.e(TAG, "getGeoRegionContainBuddy, index out of bounds! buddyIndex=" + mNumBuddies + ", total_buddy=" + mNumBuddies);
           return null;
       }
       else {
           // find which buddy to highlight
           for (int i = 1; i < mNumBuddies; i++) {
               BuddyItem buddyItem = mSortedBuddyList.get(i);
               if (buddyIndex == i) {
                   mChosenBuddyID = buddyItem.mID;
                   mMapView.setHighlightedBuddy(buddyItem.mID);
                   buddyDistance.setText(determineUnitsAsString(buddyItem.mDistanceToMe) + " away");
                   showOrHideBuddyDistance(true);
               }
           }
           if(buddyIndex == 0) {
               mMapView.setHighlightedBuddy("");
               buddyDistance.setText("");
               showOrHideBuddyDistance(false);
           }
       }

	   Bound mBound = new Bound();
	   mBound.adjustBound(mSortedBuddyList);
	   
	   GeoRegion newGeoRegion = (new GeoRegion()).MakeUsingBoundingBox(mBound.left , mBound.top,
	    mBound.right, mBound.bottom );
	   
	   return newGeoRegion;
	   
   }
   
 
   public void showMapButtonsFromThread() {
	   Log.v(TAG, "showMapButtonsFromThread");
	   getActivity().runOnUiThread(showMapButtons);
	   
   }
   public void hideMapButtonsFromThread() {
	   getActivity().runOnUiThread(hideMapButtons);
	   
   }
   
	final Runnable showMapButtons = new Runnable() {
		public void run() {
			Log.v(TAG, "showMapButtons");
			showMapBtns(false);
	    }
	};
	
	final Runnable hideMapButtons = new Runnable() {
		public void run() {
			hideMapBtns();
	    }
	};

	public void showMapBtns(boolean isForceBottom) {
		Log.v(TAG, "showMapBtns, isForce= " + isForceBottom+", mStatusBarVisible="+mStatusBarVisible);

	
		int bottomMargin = 0;

		Log.v(TAG, "margin="+bottomMargin);
		RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) zoomButton.getLayoutParams();
		params1.setMargins(params1.leftMargin, params1.topMargin, params1.rightMargin, bottomMargin);
		zoomButton.setLayoutParams(params1);
		
		RelativeLayout.LayoutParams params2 = (RelativeLayout.LayoutParams) findButton.getLayoutParams();
		params2.setMargins(params2.leftMargin, params2.topMargin, params2.rightMargin, bottomMargin);
		findButton.setLayoutParams(params2);
		
		RelativeLayout.LayoutParams params3 = (RelativeLayout.LayoutParams) fadeBackground.getLayoutParams();
		params3.setMargins(params3.leftMargin, params3.topMargin, params3.rightMargin, bottomMargin);
		fadeBackground.setLayoutParams(params3);

		fadeBackground.setVisibility(View.VISIBLE);
		zoomButton.setVisibility(View.VISIBLE);
		findButton.setVisibility((mShowFindFriendsButton) ? View.VISIBLE : View.GONE);
	}

	public void hideMapBtns() {
		zoomButton.setVisibility(View.GONE);
		findButton.setVisibility(View.GONE);
		fadeBackground.setVisibility(View.GONE);
	}

    @Override
    public void onBuddiesUpdated(int numBuddies){
        mShowFindFriendsButton = (numBuddies > 0);

        if(mInBaseMapMode && (mMapView.mViewState == MapView.MapViewState.DRAW_LAYERS)) {
            findButton.setVisibility((mShowFindFriendsButton) ? View.VISIBLE : View.GONE);
        }

        //update the Friends button and append the # of available friends to the text
        updateAndAppendToFindButton(numBuddies);

        if(mChosenBuddyID.equals("")){
            buddyDistance.setText("");
            showOrHideBuddyDistance(false);
        }
        else {
            if (mSortedBuddyList != null) {
                for (int i = 0; i < mSortedBuddyList.size(); i++) {
                    BuddyItem buddyItem = mSortedBuddyList.get(i);
                    if (buddyItem.mID.equals(mChosenBuddyID)) {
                        buddyDistance.setText(determineUnitsAsString(buddyItem.mDistanceToMe) + " away");
                        showOrHideBuddyDistance(true);
                        break;
                    }
                }
            }
        }
    }

    private void updateAndAppendToFindButton(int numBuddies){
        String findButtonText = getResources().getString(R.string.find_friend_button);
        String numberBracketed = " (" + String.valueOf(numBuddies) + ")";
        int start = findButtonText.length();
        Spannable span = new SpannableString(findButtonText + numberBracketed);
        span.setSpan(mTextAppearanceSpan, start, start+numberBracketed.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        findButton.setText(span);
    }
	
   
   class Bound{
       final float DEFAULT_BOUND = 0.00125f;
       float left = 0.f;
       float right = 0.f;
       float top = 0.f;
       float bottom = 0.f;

       public Bound(float left, float right, float top, float bottom){
           
           this.left = left;
           this.right = right;
           this.top = top;
           this.bottom = bottom;
       }
       public Bound(){
           
           this.left = 0.f;
           this.right = 0.f;
           this.top =  0.f;
           this.bottom =  0.f;
       }
       
       public void adjustBound(float lng, float lat){
           if (left == 0)
               left = lng;
           if (right == 0)
               right = lng;
           if (top == 0)
               top = lat;
           if (bottom == 0)
               bottom = lat;
           
           if (lng < left)
               left = lng;
           if (lng > right)
               right = lng;
           if (lat < bottom)
               bottom = lat;
           if (lat > top)
               top = lat;
       }

       public void adjustBound(ArrayList<BuddyItem> list) {
           adjustBound(list, 1);
       }
       
       public void adjustBound(ArrayList<BuddyItem> list, int scaleLevel) {
    	   
    	   for (BuddyItem buddy: list) {
    		   if (buddy == null || buddy.mDLocation == null )
    			   continue;
    		   adjustBound(buddy.mDLocation.x, buddy.mDLocation.y);
    	   }

           float scaledBoundary = DEFAULT_BOUND * scaleLevel;
    	   left = left - scaledBoundary;
    	   right = right + scaledBoundary;
    	   bottom = bottom - scaledBoundary;
    	   top = top + scaledBoundary;
    	   Log.i(TAG, "Showing GeoBound (left,top,right,bottom): " + String.format("(%.6f, %.6f, %.6f, %.6f)", left,top,right,bottom));
       }
       
       public void getDefaultBound(float lng, float lat) {
    	   left = lng - DEFAULT_BOUND;
    	   right = lng + DEFAULT_BOUND;
    	   bottom = lat - DEFAULT_BOUND;
    	   top = lat + DEFAULT_BOUND;
       }

   }//class bound

    public boolean haveAnyBuddies(){
        return mShowFindFriendsButton;
    }

    private void showOrHideBuddyDistance(boolean showBuddyDistance){
        buddyDistanceContainer.setVisibility((showBuddyDistance) ? View.VISIBLE : View.INVISIBLE);
        buddyDistanceContainer.setAlpha((showBuddyDistance) ? 1f : 0f);
    }

    /**
     * Determines if units should be metric or imperial
     * @param inputInMeters
     * @return a string output of the unit (e.g. "X ft" or "Y m")
     */
    private String determineUnitsAsString(float inputInMeters){
        if(SettingsUtil.getUnits(getActivity()) == SettingsUtil.RECON_UNITS_IMPERIAL){
            return String.format(STRING_FORMAT, ConversionUtil.metersToFeet(inputInMeters)) + "ft";
        }
        else return String.format(STRING_FORMAT, inputInMeters) + "m";
    }
}//eof class 
