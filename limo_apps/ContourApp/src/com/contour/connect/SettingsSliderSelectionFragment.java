package com.contour.connect;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.contour.api.CameraSettings;
import com.contour.utils.SysUtils;
import com.contour.utils.api.actionbarcompat.ActionBarActivity;

public class SettingsSliderSelectionFragment extends ListFragment implements OnSeekBarChangeListener {

    public interface OnSettingsSliderChangedListener {
        public void onSettingsSliderChanged(int switchPos, int categoryPos, int itemPosition);
        public void onSettingsSliderCancelled(CameraSettings unchangedSettings);
    }
    
    static final String ARG_SETTINGS = "settingsobj";
    private static final String ARG_TITLE_POS = "arg_title_pos";  

    
    OnSettingsSliderChangedListener mCallback;
    CameraSettings mUnchangedSettings;
    int mTitleCategoryPosition;
    
    public static SettingsSliderSelectionFragment newInstance(int titleCategoryPos, CameraSettings cameraSettings) {
    	
        SettingsSliderSelectionFragment f = new SettingsSliderSelectionFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TITLE_POS, titleCategoryPos);
        args.putParcelable(ARG_SETTINGS, cameraSettings);
        f.setArguments(args);        
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle b = getArguments();
        if(b != null) {
            mTitleCategoryPosition = b.getInt(ARG_TITLE_POS);
            mUnchangedSettings = b.getParcelable(ARG_SETTINGS);

//            SettingsItemListAdapter sla = new SettingsItemListAdapter(getActivity(),data);
//            setListAdapter(sla);
            
        } else {
            this.getFragmentManager().popBackStack();
        }
    }
    @Override
    public void onStart() {
        super.onStart();
        String title = getResources().getStringArray(R.array.settings_items)[mTitleCategoryPosition];
        TextView tv = (TextView)((ActionBarActivity) getActivity()).getActionBarHelper().setTitleView(R.layout.text_title_bar);
        tv.setText(title);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception.
        try {
            mCallback = (OnSettingsSliderChangedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnSettingsItemSelectedListener");
        }
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            mUnchangedSettings = savedInstanceState.getParcelable(ARG_SETTINGS);
             mTitleCategoryPosition = savedInstanceState.getInt(ARG_TITLE_POS, 0);
         }
        
        SettingsSliderListAdapter sla = new SettingsSliderListAdapter(this);
        try {
            sla.mCameraSettings = (CameraSettings) mUnchangedSettings.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        setListAdapter(sla);
        
        Resources r = getActivity().getResources();
        final ListView lv = this.getListView();
        LinearLayout footerView = (LinearLayout)getActivity().getLayoutInflater().inflate(R.layout.list_footer, null);
        lv.addFooterView(footerView);
        lv.setOverscrollFooter(r.getDrawable(R.drawable.list_footer_bg));
        lv.setCacheColorHint(Color.BLACK);
        lv.setFocusableInTouchMode(true);
        lv.setDivider(r.getDrawable(R.drawable.list_divider));
        lv.setDividerHeight(SysUtils.getDimension(getActivity(), R.dimen.listview_divider_height));
        lv.setOnKeyListener(new OnKeyListener()
        {
            @Override
            public boolean onKey( View v, int keyCode, KeyEvent event )
            {	
//            	Log.i("Slider","position "+ lv.getSelectedItemPosition());
	            if((event.getAction() == KeyEvent.ACTION_DOWN)&&(keyCode == KeyEvent.KEYCODE_DPAD_LEFT)) 
	            { 
//	                    Log.i("Slider","left");
	                    
	                    onProgressChangedButton(lv.getSelectedItemPosition(),-1);
	                    event.startTracking();
	                    return true;
	            }
	    		
	    		if((event.getAction() == KeyEvent.ACTION_DOWN)&&(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)) 
	            {
//	                    Log.i("Slider","right");
	                    onProgressChangedButton(lv.getSelectedItemPosition(),1);
	                    event.startTracking();
	                    return true;
	            }
	    		
	    		
	    		//set changes when release button
	    		if((event.getAction() == KeyEvent.ACTION_UP)&&(keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)) 
	            {
//	                    Log.i("Slider","button released,apply changes");
	                    
	                    onProgressChangedButtonSetChanges(lv.getSelectedItemPosition());
	                    
	                    return true;
	            }
	   
                return false;
            }
            
          
        } );
        
        lv.setItemsCanFocus(false);
        
    }
    
    
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(ARG_SETTINGS, mUnchangedSettings);
        outState.putInt(ARG_TITLE_POS, mTitleCategoryPosition);
    }
  

    public void onProgressChangedButton(int position,int delta) {
        SettingsSliderListAdapter ssla = (SettingsSliderListAdapter) getListAdapter();
        	int categoryPosition = 0;
            int sliderVal = 0;
            if (mTitleCategoryPosition == SettingsFragment.SETTINGS_ITEM_ADVANCED) {

                if (position == 0) {
                	categoryPosition = SettingsFragment.SETTINGS_ITEM_CONTRAST;
                    int oldProgress = ssla.mCameraSettings.getContrast()-1;
                    if ( (delta<0 && oldProgress>0) || (delta>0 && oldProgress <254)){
                    	int progress = oldProgress+delta;

                    	sliderVal = progress + 1;
                    	ssla.mCameraSettings.setContrast(sliderVal);
                    }
                }
                if (position == 1) {
                	categoryPosition = SettingsFragment.SETTINGS_ITEM_EXPOSURE;
                    int oldProgress = ssla.mCameraSettings.getExposure()+4;
                    if(oldProgress>8){
                    	oldProgress =0+4;
                    }
                    if ( (delta<0 && oldProgress>0) || (delta>0 && oldProgress <8)){
                    	int progress = oldProgress+delta;
                    	sliderVal = progress - 4;
                    	ssla.mCameraSettings.setExposure(sliderVal);
                    }
                }
                if (position == 2) {
                	categoryPosition = SettingsFragment.SETTINGS_ITEM_SHARPNESS;
                    int oldProgress = ssla.mCameraSettings.getSharpness()-1;
                    if ( (delta<0 && oldProgress>0) || (delta>0 && oldProgress <4)){
                    	int progress = oldProgress+delta;
                    
                    	sliderVal = progress + 1;
                    	ssla.mCameraSettings.setSharpness(sliderVal);
                    }
                }
            } 
            else {
                if (position == 0) {
                	categoryPosition = SettingsFragment.SETTINGS_ITEM_INTERNAL_MIC;
                    int oldProgress = ssla.mCameraSettings.getInternalMic();
                    if ( (delta<0 && oldProgress>0) || (delta>0 && oldProgress <59)){
                    	int progress = oldProgress+delta;

                    	sliderVal = progress;
                    	ssla.mCameraSettings.setInternalMic(sliderVal);
                    }

   
                }
                if (position == 1) {
                	
                	categoryPosition = SettingsFragment.SETTINGS_ITEM_EXTERNAL_MIC;
                    int oldProgress = ssla.mCameraSettings.getExternalMic();
                    if ( (delta<0 && oldProgress>0) || (delta>0 && oldProgress <59)){
                    	int progress = oldProgress+delta;

                    	sliderVal = progress;
                    	ssla.mCameraSettings.setExternalMic((byte)sliderVal);
                    }
                }
            }
//            mCallback.onSettingsSliderChanged(mUnchangedSettings.switchPosition, categoryPosition, sliderVal);
            ((BaseAdapter) this.getListAdapter()).notifyDataSetChanged();
    }
    
    public void onProgressChangedButtonSetChanges(int position) {
        SettingsSliderListAdapter ssla = (SettingsSliderListAdapter) getListAdapter();
        	int categoryPosition = 0;
            int sliderVal = 0;
            if (mTitleCategoryPosition == SettingsFragment.SETTINGS_ITEM_ADVANCED) {

                if (position == 0) {
                	categoryPosition = SettingsFragment.SETTINGS_ITEM_CONTRAST;
                    int progress = ssla.mCameraSettings.getContrast()-1;
                    sliderVal = progress + 1;
                   
                }
                if (position == 1) {
                	categoryPosition = SettingsFragment.SETTINGS_ITEM_EXPOSURE;
                    int progress = ssla.mCameraSettings.getExposure()+4;
                    sliderVal = progress - 4;
                    
                }
                if (position == 2) {
                	categoryPosition = SettingsFragment.SETTINGS_ITEM_SHARPNESS;
                    int progress = ssla.mCameraSettings.getSharpness()-1;
                   
                    	sliderVal = progress + 1;
                    	ssla.mCameraSettings.setSharpness(sliderVal);
                    
                }
            } 
            else {
                if (position == 0) {
                	categoryPosition = SettingsFragment.SETTINGS_ITEM_INTERNAL_MIC;
                    int progress = ssla.mCameraSettings.getInternalMic();
                    
                    sliderVal = progress;

                }
                if (position == 1) {
                	
                	categoryPosition = SettingsFragment.SETTINGS_ITEM_EXTERNAL_MIC;
                    int progress = ssla.mCameraSettings.getExternalMic();
                   
                    sliderVal = progress;

                }
            }
            mCallback.onSettingsSliderChanged(mUnchangedSettings.switchPosition, categoryPosition, sliderVal);
         
    }
    
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//        SettingsSliderListAdapter ssla = (SettingsSliderListAdapter) getListAdapter();
////        Log.i("SettingsSliderSelection","onProgressChanged");
//        if (fromUser) {
//            int position = (Integer) seekBar.getTag();
//
//            int sliderVal = 0;
//            if (mTitleCategoryPosition == SettingsFragment.SETTINGS_ITEM_ADVANCED) {
//
//                if (position == 0) {
// 
//                    sliderVal = progress + 1;
//                    ssla.mCameraSettings.setContrast(sliderVal);
//
//                }
//                if (position == 1) {
//
//                    sliderVal = progress - 4;
//                    ssla.mCameraSettings.setExposure(sliderVal);
//
//                }
//                if (position == 2) {
//
//                    sliderVal = progress + 1;
//                    ssla.mCameraSettings.setSharpness(sliderVal);
//
//                }
//            } else {
//                if (position == 0) {
//                    sliderVal = progress;
//                    ssla.mCameraSettings.setInternalMic(sliderVal);
//                }
//                if (position == 1) {
//                    sliderVal = progress;
//                    ssla.mCameraSettings.setExternalMic((byte) sliderVal);
//                }
//            }
//        }
    }

    
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        int position = (Integer) seekBar.getTag();
        int progress = seekBar.getProgress();
        int categoryPosition = 0;
        int sliderVal = 0;
        if (mTitleCategoryPosition == SettingsFragment.SETTINGS_ITEM_ADVANCED) {
            if (position == 0) {
                categoryPosition = SettingsFragment.SETTINGS_ITEM_CONTRAST;
                sliderVal = progress + 1;
            }
            if (position == 1) {
                categoryPosition = SettingsFragment.SETTINGS_ITEM_EXPOSURE;
                sliderVal = progress - 4;
            }
            if (position == 2) {
                categoryPosition = SettingsFragment.SETTINGS_ITEM_SHARPNESS;
                sliderVal = progress + 1;

            }
        } else {
            if (position == 0) {
                categoryPosition = SettingsFragment.SETTINGS_ITEM_INTERNAL_MIC;
                sliderVal = progress;
            }
            if (position == 1) {
                categoryPosition = SettingsFragment.SETTINGS_ITEM_EXTERNAL_MIC;
                sliderVal = progress;

            }
        }
        mCallback.onSettingsSliderChanged(mUnchangedSettings.switchPosition, categoryPosition, sliderVal);
        ((BaseAdapter) this.getListAdapter()).notifyDataSetChanged();
    }
    
    public static class SettingsSliderListAdapter extends BaseAdapter {

        private LayoutInflater mInflater;
        private String[] mLabels;
        private SettingsSliderSelectionFragment mSettingsSliderFrag;
        CameraSettings mCameraSettings; 
        final boolean mVideoSettings;
        
        public SettingsSliderListAdapter(SettingsSliderSelectionFragment sssf) {
            mInflater = LayoutInflater.from(sssf.getActivity()); 
            if(sssf.mTitleCategoryPosition == SettingsFragment.SETTINGS_ITEM_ADVANCED)
                mVideoSettings = true;
            else
                mVideoSettings = false;
            if(mVideoSettings) 
                mLabels = sssf.getActivity().getResources().getStringArray(R.array.video_settings_items);
            else
                mLabels = sssf.getActivity().getResources().getStringArray(R.array.audio_settings_items);

            mSettingsSliderFrag = sssf;
        }
        
        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return mVideoSettings ? 3 : 2;
        }

        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return position;
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            
            if(convertView == null) {
                convertView = mInflater.inflate(R.layout.list_item_slider, null);
                holder = new ViewHolder();
                holder.title = (TextView) convertView.findViewById(R.id.slider_title);
                holder.value = (TextView) convertView.findViewById(R.id.slider_value);
                holder.seekbar = (SeekBar) convertView.findViewById(R.id.slider_seekbar);
                holder.seekbar.setOnSeekBarChangeListener(mSettingsSliderFrag);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.title.setText(mLabels[position]);
            holder.seekbar.setTag(Integer.valueOf(position));
            if(mVideoSettings) {
                setSeekBarProgressForVideo(holder,position);
                setValuesForVideo(holder,position);
            }
            else {
                setSeekBarProgressForAudio(holder,position);
                setValuesForAudio(holder,position);
            }
            return convertView;
        }
        
        static class ViewHolder {
            TextView title;
            TextView value;
            SeekBar seekbar;
        }
        
        private void setSeekBarProgressForVideo(ViewHolder holder, int position) {
            if(position == 0) {
                //Contrast 
                holder.seekbar.setMax(254);
                int contrast = mCameraSettings.getContrast();
                holder.seekbar.setProgress(contrast-1);
               
            } else if (position == 1) {
                //Exposure
                holder.seekbar.setMax(8);
                int exposure = mCameraSettings.getExposure();
                if (exposure>8)exposure=0; //darrell fix
                holder.seekbar.setProgress(exposure+4);
                
            } else if (position == 2) {
                //Sharpness
                holder.seekbar.setMax(4);
                int sharpness = mCameraSettings.getSharpness();
                holder.seekbar.setProgress(sharpness-1);
               
            }
        }
        
        private void setSeekBarProgressForAudio(ViewHolder holder, int position) {
            holder.seekbar.setMax(59);
            int value = 0;
            if(position == 0) {
                value =  mCameraSettings.getInternalMic();
            } else if (position == 1) {
                //Exposure
                value =  mCameraSettings.getExternalMic();
            }
            holder.seekbar.setProgress(value);
        }
        
        private void setValuesForVideo(ViewHolder holder, int position) {
            if(position == 0) {
                //Contrast 
                int contrast = mCameraSettings.getContrast();
                holder.value.setText(String.valueOf(contrast));
            } else if (position == 1) {
                //Exposure
                int exposure = mCameraSettings.getExposure();
                if (exposure>8)exposure=0; //darrell fix
                holder.value.setText(String.valueOf(exposure));
               
            } else if (position == 2) {
                //Sharpness
                int sharpness = mCameraSettings.getSharpness();
                holder.value.setText(String.valueOf(sharpness));
            }
        }
        
        private void setValuesForAudio(ViewHolder holder, int position) {
            int value = 0;
            if(position == 0) {
                value =  mCameraSettings.getInternalMic();
            } else if (position == 1) {
                //Exposure
                value =  mCameraSettings.getExternalMic();
            }
            holder.value.setText(String.valueOf(value));
        }
    }




}
