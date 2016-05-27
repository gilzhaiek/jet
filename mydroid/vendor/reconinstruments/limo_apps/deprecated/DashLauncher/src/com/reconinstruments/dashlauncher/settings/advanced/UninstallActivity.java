package com.reconinstruments.dashlauncher.settings.advanced;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.reconinstruments.dashlauncher.R;


public class UninstallActivity extends ListActivity {

	// List of packages that cannot be uninstalled by the user
	private static final String[] RECON_PACKAGES = { "com.reconinstruments.reconstats", "com.reconinstruments.chrono" };
	
	private class AppInfo
	{
		String mTitle;			//title
		Drawable mIcon;			//application icon
		String mPackageName;
		String mClassName;
	}
	
	ArrayList<AppInfo> mAppInfos = null;
	ReconListAdapter uninstAdapter;
	
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.setting_layout);
        
        TextView title = (TextView) findViewById(R.id.setting_title);
        title.setText("Uninstall Apps");

        enumerateApps();
        
        uninstAdapter = new ReconListAdapter(this, R.layout.setting_uninst_item, mAppInfos );
 
        setListAdapter( uninstAdapter );
        
        ListView listView = getListView();

        //single selection mode
        listView.setItemsCanFocus(true);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.setOnItemClickListener( mItemClickListener );            
    }

    final static int  GET_UNINSTALL_RESULT = 1;
    private OnItemClickListener mItemClickListener = new OnItemClickListener( )
    {
    	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    	{
    		AppInfo appInfo = mAppInfos.get(position);
    		Uri packageURI = Uri.parse("package:" + appInfo.mPackageName);
    		Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
    		startActivityForResult(uninstallIntent, GET_UNINSTALL_RESULT);
    		
    	}    	
    };
	
	private void enumerateApps( )
    {
        PackageManager manager = getPackageManager();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        final List<ResolveInfo> apps = manager.queryIntentActivities(mainIntent, 0);
        Collections.sort(apps, new ResolveInfo.DisplayNameComparator(manager));

        if (apps != null) 
        {            

        	final int count = apps.size();
            if ( mAppInfos  == null) 
            {
            	mAppInfos = new ArrayList<AppInfo>( count );
            }
            
            mAppInfos.clear();
            
            for (int i = 0; i < count; i++)
            {
                AppInfo application = new AppInfo();
                ResolveInfo info = apps.get(i);
                
                
                application.mTitle = info.loadLabel(manager).toString();
                application.mPackageName = info.activityInfo.applicationInfo.packageName;
                application.mClassName = info.activityInfo.name;
                application.mIcon = info.activityInfo.loadIcon(manager);
                
                boolean restrictedApp = false;
                
                for(String a : RECON_PACKAGES) {
                	if(a.equals(application.mPackageName)) {
                		restrictedApp = true;
                		break;
                	}
                }
                
                if(!restrictedApp) mAppInfos.add(application);
            }
        }
    }
	
	public void onResume() {
		super.onResume();
		
		enumerateApps();
		uninstAdapter.notifyDataSetChanged();
	}
    
    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data)
    {
    	if( requestCode == GET_UNINSTALL_RESULT )
    	{
    		int idx = this.getListView().getSelectedItemPosition();

    		//check if the selected package installed or not after returning from the uninstall process
    		boolean deleted = searchForPackage( mAppInfos.get(idx).mPackageName ) == false;
    		if(  deleted )
    		{
    			//if the selected package is uninstalled,
    			//let's removed it from the list to avoid of
    			//double deletion
    			mAppInfos.remove( idx );
    			uninstAdapter.notifyDataSetChanged();
    			
    		}
    	}
    }
    
    //search for a package to see if it exists or not
    private boolean searchForPackage( String packageName )
    {
    	PackageManager manager = getPackageManager();
    	
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        final List<ResolveInfo> apps = manager.queryIntentActivities(mainIntent, 0);

        if (apps != null) 
        {            

        	final int count = apps.size();
            
            for (int i = 0; i < count; i++)
            {
                
                ResolveInfo info = apps.get(i);
                
                if( info.activityInfo.applicationInfo.packageName.equalsIgnoreCase( packageName ) )
                	return true;
                
            }
        }

        //other wise, can not find the app in 
        return false;

    }
	

	private class ReconListAdapter extends ArrayAdapter<AppInfo>
	{
		private ArrayList<AppInfo> mListItems;
		private int resId;
		
		public ReconListAdapter( Context context, int resourceId, ArrayList<AppInfo> items )
		{
			super( context, resourceId, items );
			
			mListItems = items;
			resId = resourceId;
		}
		
		@Override
		public View getView( int position, View convertView, ViewGroup parent )
		{
			ImageView icon;
			TextView title;
			
			if( convertView == null )
			{
				LayoutInflater inflater = (LayoutInflater)this.getContext().getSystemService(LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(resId , null);	
			}
			
			icon = (ImageView) convertView.findViewById(R.id.setting_uninst_icon);
			title = (TextView) convertView.findViewById(R.id.setting_uninst_name);
			
			title.setText( mListItems.get(position).mTitle );
			icon.setImageDrawable( mListItems.get(position).mIcon );		
			
			return convertView;
			
		}
		
	}
    
}
