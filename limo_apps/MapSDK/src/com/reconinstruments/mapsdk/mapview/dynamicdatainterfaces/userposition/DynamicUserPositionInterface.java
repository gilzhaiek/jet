package com.reconinstruments.mapsdk.mapview.dynamicdatainterfaces.userposition;


public class DynamicUserPositionInterface {
	public interface IDynamicUserPosition {
		public void SetUserPosition(float longitude, float latitude);		
		public void SetUserHeading(float heading);
		public void SetUserPitch(float pitch);
	}
}
