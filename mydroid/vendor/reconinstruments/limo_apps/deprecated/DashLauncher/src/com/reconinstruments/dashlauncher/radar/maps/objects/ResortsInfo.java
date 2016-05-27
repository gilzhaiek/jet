package com.reconinstruments.dashlauncher.radar.maps.objects;

import android.util.SparseArray;


public class ResortsInfo extends ResortHolder {
	public SparseArray<ResortHolder> mHoldersMap = new SparseArray<ResortHolder>();
	
	protected boolean mIsCorrupted = false;
	
	public ResortsInfo() {
	}
	
	public boolean IsCorrupted() {return mIsCorrupted;}
	public void SetCorrupted(boolean value) {mIsCorrupted = value;}
	
	public void AddHolder(int id, ResortHolder resortHolder) {
		mHoldersMap.put(id, resortHolder);
	}
	
	public ResortHolder GetHolder(int id) {
		return mHoldersMap.get(id);
	}
	
	public void AddCountry(CountryInfo countryInfo) {
		AddSortHolder((ResortHolder)countryInfo);
	}
	
	public CountryInfo GetCountry( String countryName ) {
		return (CountryInfo)GetResortHolder(countryName );
	}
	
	public static boolean IsMyInstance(Object object) {
		return object instanceof ResortsInfo; 
	}
}
