package com.reconinstruments.dashlauncher.applauncher.pagescroll;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class GridListAdapter extends BaseAdapter implements ListAdapter{
    private AppLauncherActivity context;
    
    public GridListAdapter(AppLauncherActivity context) {
    	this.context = context;
    }

    public int getCount() {
        return context.getApps().size()/6;
    }
    // return first app
    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
    	
    	//LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		//return (ImageView) vi.inflate(R.layout.app_6grid, null);
		
		TextView tv = new TextView(context);
		tv.setHeight(190);
		tv.setBackgroundColor(0xFFFF0000);
    	tv.setText(""+position);
    	tv.setLayoutParams(new ListView.LayoutParams(378, 190));
    	
    	return tv;
    	/*
    	GridView gridview = (GridView) convertView;
		
    	if(gridview==null){
			LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			gridview = (GridView) vi.inflate(R.layout.app_6grid, null);
		} 
    	AppGridAdapter adapter = new AppGridAdapter(context,position);
    	gridview.setAdapter(adapter);
    
	    return gridview;*/
    }
}