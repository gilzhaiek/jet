package com.reconinstruments.mapImages.bo;

import com.reconinstruments.mapImages.dal.ResortsInfoDAL;
import com.reconinstruments.mapImages.objects.CountryInfo;
import com.reconinstruments.mapImages.objects.RegionInfo;
import com.reconinstruments.mapImages.objects.ResortHolder;
import com.reconinstruments.mapImages.objects.ResortsInfo;

import android.content.Context;
import android.util.Log;

public class ResortsInfoBO {
	protected static final String TAG = "ResortsInfoBO";
	
	protected boolean			mIsCanceled = false;
	protected ResortsInfo		mResortsInfo = null;
	protected ResortsInfoDAL	mResortsInfoDAL = null;
	protected Context			mContext = null;
	
	public ResortsInfoBO(Context context)
	{
		mContext = context;
	};
	
	
	public void CancelLoad()
	{
		
	}
	
	public void DebugPrintResortsInfo(ResortsInfo resortsInfo) {
		for(int i = 0; i < resortsInfo.mHoldersMap.size(); i++) {
			ResortHolder resortHolder = resortsInfo.mHoldersMap.valueAt(i);
			if(CountryInfo.IsMyInstance(resortHolder))
			{
				Log.v(TAG,resortHolder.Name + "[" + resortHolder.ID + "]"); 
			}
			else if(RegionInfo.IsMyInstance(resortHolder)) {
				Log.v(TAG,"\t" + resortHolder.Name + "[" + resortHolder.ID + "]");
			}
			
			for(int j = 0; j < resortHolder.mResorts.size(); j++ ) {
				Log.v(TAG,"\t\t" + resortHolder.mResorts.get(j).Name + " ["+ resortHolder.mResorts.get(j).ResortID + "]"); 
			}
		}
	}

	public void Load(ResortsInfo resortsInfo) throws Exception
	{		
		if(mResortsInfoDAL == null) {
			mResortsInfoDAL = new ResortsInfoDAL(mContext);
		}
		
		mResortsInfoDAL.LoadResortsInfo(resortsInfo);
		
		//DebugPrintResortsInfo(resortsInfo);
	}
}
