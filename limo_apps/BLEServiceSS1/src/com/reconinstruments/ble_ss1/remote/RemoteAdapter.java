package com.reconinstruments.ble_ss1.remote;

import java.util.ArrayList;

import android.content.Context;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.reconinstruments.ble_ss1.BLEUtils;
import com.reconinstruments.ble_ss1.R;
import com.reconinstruments.ble_ss1.TheBLEService.BLEDevice;
import com.reconinstruments.commonwidgets.CircularListAdapter;
import com.stonestreetone.bluetopiapm.BluetoothAddress;


public class RemoteAdapter extends ArrayAdapter<BLEDevice> implements CircularListAdapter{

	private static final String TAG = "RemoteAdapter";

	Context context = null;
	ArrayList<BLEDevice> remotes;

	public RemoteAdapter(Context context, ArrayList<BLEDevice> remotes) {
		super(context, R.layout.remote_item, remotes);

		this.context = context;
		this.remotes = remotes;
	}

	@Override
	public int getCount() {
		return Integer.MAX_VALUE;
	}
	
	@Override
	public BLEDevice getItem(int position) {
		return super.getItem(position % remotes.size());
	}

	@Override
	public void add(BLEDevice remote) {
		
		//ignore existing remotes
		for(int i=0;i<remotes.size();i++){
			if(remotes.get(i).address.equals(remote.address)) return;
		}
		super.add(remote);
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent){

		if(convertView==null){
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			convertView = inflater.inflate(R.layout.remote_item, null);
		}

		BLEDevice item = getItem(position);

		if (item != null){
			TextView title = (TextView) convertView.findViewById(R.id.remote_text);

			item.address.toByteArray();

			title.setText(getLast3Bytes(item.address));

			convertView.setTag(item);
		}
		
		convertView.setBackgroundResource(0);
		if(remotes.size() == 2){
		    if(position == 1){
		        convertView.setBackgroundResource(R.drawable.fade_bottom);
		    }
		}else if(remotes.size() == 3){
            if(position == 0){
                convertView.setBackgroundResource(R.drawable.fade_top);
            }else if(position == 2){
                convertView.setBackgroundResource(R.drawable.fade_bottom);
            }
		}
		
		return convertView;
	}
	public String getLast3Bytes(BluetoothAddress address){
		byte[] bytes = address.toByteArray();
		return BLEUtils.byteArrayToHex(new byte[]{bytes[3],bytes[4],bytes[5]});
	}

	@Override
	public int getRealCount() {
		return remotes.size();
	}
}