package com.reconinstruments.dashlauncher.settings.advanced;

import java.util.ArrayList;

import com.reconinstruments.dashlauncher.settings.SettingAdapter;
import com.reconinstruments.dashlauncher.settings.SettingButtonAdapter;
import com.reconinstruments.dashlauncher.settings.SettingItem;
import com.reconinstruments.dashsettings.R;
import com.reconinstruments.messagecenter.MessageDBSchema;
import com.reconinstruments.messagecenter.MessageDBSchema.GrpSchema;
import com.reconinstruments.messagecenter.ReconMessageAPI;
import com.reconinstruments.phone.PhoneLogProvider;
import com.reconinstruments.commonwidgets.TwoOptionsJumpFixer;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


public class CLearPhoneHistoryActivity extends ListActivity {
	private static final String TAG = "CLearPhoneHistoryActivity";
	private TwoOptionsJumpFixer twoOptionsJumpFixer;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.setting_layout);
		
		ImageView headerIcon = (ImageView) findViewById(R.id.setting_icon);
		headerIcon.setVisibility(View.GONE);
		TextView title = (TextView) findViewById(R.id.setting_title);
		title.setText("CLEAR MESSAGE HISTORY");
		
		LinearLayout desc_layout = (LinearLayout) findViewById(R.id.setting_desc);
		desc_layout.setVisibility(View.VISIBLE);
		TextView desc = (TextView) findViewById(R.id.setting_desc_text);
		desc.setText("All notification messages will be removed from your HUD.");
		
		
		ArrayList<SettingItem> clearPhoneHistoryList = new ArrayList<SettingItem>();

		clearPhoneHistoryList.add(new SettingItem("CLEAR HISTORY" ));
		clearPhoneHistoryList.add(new SettingItem("CANCEL" ));
		
		setListAdapter(new SettingButtonAdapter(this, clearPhoneHistoryList));
		twoOptionsJumpFixer = new TwoOptionsJumpFixer(getListView());
		twoOptionsJumpFixer.start();
		
		this.getListView().setOnItemClickListener(new OnItemClickListener(){

			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
				
				if(twoOptionsJumpFixer != null){
					twoOptionsJumpFixer.stop();
				}
				if (position == 0){
					// Delete all phone records from DB
					int count = getContentResolver().delete(PhoneLogProvider.CONTENT_URI, null, null);
					Log.v(TAG, "phone records deleted: " + count);
					
					// deletes calls and texts groups from message center
					// String select = GrpSchema.COL_URI+"='com.reconinstruments.calls' OR "+
					// 				GrpSchema.COL_URI+"='com.reconinstruments.texts'";
					String select = null; // This deletes everttying
					count = getContentResolver().delete(ReconMessageAPI.GROUPS_URI, select , null);
					Log.v(TAG, "phone messages deleted: " + count);
					
					//Toast.makeText(CLearPhoneHistoryActivity.this, "Phone History Deleted", Toast.LENGTH_LONG).show();
					finish();
				}
				else{
					finish();	
				}				
			}});
		
	}



}
