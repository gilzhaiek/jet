package com.reconinstruments.messagecenter;

import android.provider.BaseColumns;

/* Schema for MessageCenter Database */
public class MessageDBSchema 
{	
	public enum MessagePriority
    {
    	MESSAGE_PRIORITY_NORMAL,
    	MESSAGE_PRIORITY_IMPORTANT;
    }
	/* MessageGroups Table */
	public static abstract class GrpSchema implements BaseColumns
	{
		public static final String TABLE = "MessageGroups";

		public static final String COL_URI 			= "uri";	
		public static final String COL_APK			= "APK";
		public static final String COL_ICON         = "Icon";
		public static final String COL_DESCRIPTION  = "Description";
	}
	
    /* MessageCategories Table */
	public static abstract class CatSchema implements BaseColumns
	{
		public static final String TABLE = "MessageCategories";

		public static final String COL_URI    			= "Uri";
		public static final String COL_GROUP_ID     	= "GroupID";
		
		public static final String COL_ICON            	= "Icon";
		public static final String COL_AGGREGATABLE    	= "Aggregatable";
		public static final String COL_DESCRIPTION     	= "Description";

		public static final String COL_PRESS_INTENT  	= "PressAction";  //when the button is pressed when viewing a message
		public static final String COL_HOLD_INTENT  	= "HoldAction";   //when the button is held down when viewing a message
		public static final String COL_VIEWER_INTENT  	= "ViewerAction"; //when the category is selected
		
		public static final String COL_PRESS_CAPTION 	= "PressCaption";
		public static final String COL_HOLD_CAPTION 	= "HoldCaption";
	}
	
	/* Messages Table */
	public static abstract class MsgSchema implements BaseColumns
	{
		public static final String TABLE = "Messages";
		
		public static final String COL_CATEGORY_ID     	= "CategoryID";
		public static final String COL_PRIORITY        	= "Priority";
		public static final String COL_TEXT            	= "Text";
		public static final String COL_TIMESTAMP       	= "Timestamp";

		public static final String COL_PROCESSED       	= "Processed";
		public static final String COL_ICON            	= "Icon";

		//extra data to be passed with intents
		public static final String COL_EXTRA			= "Data";
	}
}




