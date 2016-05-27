package com.mycompany.notificationhudreceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class NCReceiver extends BroadcastReceiver {
	private String TAG = this.getClass().getSimpleName();
 
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "onReceive");
		String tickerText = intent.getStringExtra("tickerText");
		Log.d(TAG, "ticketText: "+ tickerText);
		
		LayoutInflater inflater = LayoutInflater.from(context);
		View view = inflater.inflate(R.layout.cust_toast_layout, null);
		TextView title = (TextView)view.findViewById(R.id.toastText);
		title.setText(tickerText);

		if(intent.getBooleanExtra("hasIcon", false)) {
			Bitmap bitmap = (Bitmap)intent.getParcelableExtra("icon");
			ImageView icon = (ImageView)view.findViewById(R.id.toastIcon);
			icon.setImageBitmap(bitmap);
		}
		
		Toast toast = new Toast(context);
		toast.setView(view);
		toast.show();
		//Toast.makeText(context, ticketText, Toast.LENGTH_LONG).show();
	}

}
