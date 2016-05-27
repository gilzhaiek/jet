package com.reconinstruments.dashlauncher.settings;

import java.io.File;
import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.platform.UBootEnvNative;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.reconinstruments.dashlauncher.R;

public class UpdateActivity extends ListActivity {
	static private final String TAG = "UpdateActivity";

	static private final String UBOOT_OPT_NAME = "BOOT_OPT";
	static private final String UBOOT_UPDATE_FIRMWARE = "UPDATE_REQ";
	static private final String UBOOT_RESET_FIRMWARE = "FACTORY_REQ";

	private ArrayList<SettingItem> updateList; 

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.setting_layout);

		TextView title = (TextView) findViewById(R.id.setting_title);
		title.setText("Software Update");

		File externalStorage = Environment.getExternalStorageDirectory();

		String path = externalStorage.getPath() + "/update.zip";
		File firmwareFile = new File( path );

		path = externalStorage.getPath() + "/recovery/command";
		File commandFile = new File( path );

		LinearLayout desc_layout = (LinearLayout) findViewById(R.id.setting_desc);
		desc_layout.setVisibility(View.VISIBLE);
		TextView desc = (TextView) findViewById(R.id.setting_desc_text);

		updateList = new ArrayList<SettingItem>();

		SettingItem item = new SettingItem("Update" );

		if( firmwareFile.isFile() && firmwareFile.exists() && commandFile.isFile() && commandFile.exists() )
		{
			desc.setText("Software Update Available");
			item.titleAlpha = 255;
			updateList.add(item);
		}
		else{
			desc.setText("No Update Files");
		}

		

		item = new SettingItem("Cancel");
		updateList.add(item);

		setListAdapter(new SettingAdapter(this, 0, updateList));

		this.getListView().setOnItemClickListener(updateListener);
	}

	private OnItemClickListener updateListener = new OnItemClickListener(){

		public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

			if (updateList.get(position).title.equalsIgnoreCase("Update")){

				File externalStorage = Environment.getExternalStorageDirectory();

				String path = externalStorage.getPath() + "/update.zip";
				File firmwareFile = new File( path );

				path = externalStorage.getPath() + "/recovery/command";
				File commandFile = new File( path );
	
				if( firmwareFile.isFile() && firmwareFile.exists() && commandFile.isFile() && commandFile.exists() )
					doFirmwareUpgrade();
			}
			else{
				finish();
			}

		}

	};

	private void doFirmwareUpgrade() {
		Toast.makeText(this, "doFirmwareUpgrade", Toast.LENGTH_LONG).show();
		PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
		UBootEnvNative.Set_UBootVar(UBOOT_OPT_NAME, UBOOT_UPDATE_FIRMWARE);
		pm.reboot(null);
		finish();
	}



}
