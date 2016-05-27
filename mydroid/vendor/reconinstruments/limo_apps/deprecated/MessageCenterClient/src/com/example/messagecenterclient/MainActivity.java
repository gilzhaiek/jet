package com.example.messagecenterclient;

import java.util.ArrayList;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.annotation.SuppressLint;
import android.content.Intent;

import com.reconinstruments.messagecenter.*;

@SuppressWarnings("unused")
@SuppressLint({ "DefaultLocale" })   
    public class MainActivity extends Activity {
	private TextView mServerResponse;
	private TextView mNotificationMessage;
   
	private static final String TAG = "MessageCenterClient"; 
	
	private ReconMessageGroup    mMessageGroup    = new ReconMessageGroup    (); 
	private ReconMessageCategory mMessageCategory = new ReconMessageCategory ();
   
	private class MessageEvents implements IMessageCenterEvents {
	    @Override
	    public void onMessageCenterConnected() {
		String strMsg = String.format("Message Center Connected!");
		mServerResponse.setText(strMsg);
	    }

	    @Override 
		public void onMessageGroupRegistered (ReconMessageGroup grp) {
		String strMsg = String.format("Message Group registered. Group ID: [%d]", grp.groupID);
		mServerResponse.setText(strMsg);

		mMessageGroup = grp;
	    }

	    @Override
	    public void onMessageCategoryRegistered(ReconMessageCategory cat) {
		String strMsg = String.format("Message Category registered. Category ID: [%d]", cat.categoryID);
		mServerResponse.setText(strMsg);

		mMessageCategory = cat;
	    }

	    @Override
	    public void onServerResponse (int response, int status, int extra) {
		String strMsg = new String();
		    
		if (status != MessageCenterConstants.CompletionCode.STATUS_ERR_NONE) {
		    strMsg = String.format("Server Response: [%d], status: [%d]!. Extra parameter:", response, status, extra);
		    mServerResponse.setText(strMsg);
		}
		else {
		    switch (response) {
		    case MessageCenterConstants.ResponseCode.MSG_ID_RESP_DELETE_ALL_MESSAGE_GROUPS: {
			strMsg = String.format("Number of Message Groups Deleted: [%d]", extra);
		    }
			break;
				    
		    case MessageCenterConstants.ResponseCode.MSG_ID_RESP_DELETE_ALL_MESSAGE_CATEGORIES: {
			strMsg = String.format("Number of Message Categories Deleted: [%d]", extra);
		    }
			break;
				    
		    case MessageCenterConstants.ResponseCode.MSG_ID_RESP_DELETE_NOTIFICATIONS: {
			strMsg = String.format("Number of Notifications Deleted: [%d]", extra); 
		    }
			break; 
		    }
		}
		    
		mServerResponse.setText(strMsg);
	    }

	    @Override
	    public void onGroupQueryCompleted(ArrayList<ReconMessageGroup> ga) {
		// just put them in toast
		String strData = new String("Registered Message Groups: ");
    	   
		for (int i = 0; i < ga.size(); i++) {
		    ReconMessageGroup grp = ga.get(i);
		    strData += "\n" + grp.Description;
		}
    	   
		if (ga.size() > 0) mMessageGroup = ga.get(0);
		Toast.makeText(getApplicationContext(), strData, Toast.LENGTH_LONG).show();
	    }
       
	    @Override
	    public void onCategoryQueryCompleted(ReconMessageGroup grp, ArrayList<ReconMessageCategory> ca) {
		// just put them in toast
		String strData = new String("Registered Message Categories for Group ") + grp.Description  + " :";
    	   
		for (int i = 0; i < ca.size(); i++) {
		    ReconMessageCategory cat = ca.get(i);
		    strData += "\n" + cat.Description;
		}
    	   
		Toast.makeText(getApplicationContext(), strData, Toast.LENGTH_LONG).show();
	    }

	    @Override
	    public void onMessageSent(ReconNotification not) {
		String strMsg = String.format("Notification successfully sent");
		mServerResponse.setText(strMsg);
	    }
       
       
	} 
	private MessageCenterAdmin  mNotifyManager = new MessageCenterAdmin ();
	private MessageEvents       mMessageEvents = new MessageEvents();
   
	@Override
	protected void onCreate(Bundle savedInstanceState) {
	    Log.d(TAG, "onCreate");
		
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.activity_main);
		
	    // connect to server. We'll be notified asynchronously on success
	    mNotifyManager.connect(this, mMessageEvents);

	    // get our edits
	    mServerResponse = (TextView) findViewById(R.id.lblServerResponse);
	    mNotificationMessage = (TextView) findViewById(R.id.edit_message);
		
	}
	
	@Override
	protected void onDestroy() {
	    Log.d(TAG, "onDestroy");
	    mNotifyManager.release();   // avoid service leaked logcat warning
		
	    super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    // Inflate the menu; this adds items to the action bar if it is present.
	    getMenuInflater().inflate(R.menu.main, menu);
	    return true;
	}
	
	public void register_group (View view) {
	    mMessageGroup.ResourceContext = "com.example.messagecenterclient";
	    //grp.Icon = R.drawable.ic_launcher;   
	    mMessageGroup.Icon = R.drawable.zoom;

	    mMessageGroup.Description = "Test Client Group";
    	
	    try {
		mNotifyManager.registerGroup(mMessageGroup);  
	    } 
	    catch (Throwable e)
		{
		    String strMsg = String.format("Exception Thrown: [%s]", e.getMessage() );
		    //Toast.makeText(getApplicationContext(), strMsg, Toast.LENGTH_SHORT).show();
		    mServerResponse.setText(strMsg);
		}
	} 
     
	public void register_category (View view) {
	    mMessageCategory.GroupID = mMessageGroup.groupID;
	    mMessageCategory.HandlerName = this.getLocalClassName(); 
	    mMessageCategory.Description = "This is test Category";
	    mMessageCategory.Icon = R.drawable.game;
    	
	    mMessageCategory.handlerIntent = new Intent(this, MainActivity.class);
	    mMessageCategory.handlerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	
	    mMessageCategory.Aggregatable = false;
    	
	    try {
		mNotifyManager.registerCategory(mMessageCategory);
	    } 
	    catch (Throwable e) {
		String strMsg = String.format("Exception Thrown: [%s]", e.getMessage() );
		mServerResponse.setText(strMsg);
	    }
	}
       
	public void deleteallgroups (View view) {
	    try {
		mNotifyManager.DeleteAllMessageGroups();
	    }
	    catch (Throwable e) {
		String strMsg = String.format("Exception Thrown: [%s]", e.getMessage() );
		mServerResponse.setText(strMsg);
	    }
	}
    
	public void deleteallcategories (View view) {
	    try {
		mNotifyManager.DeleteAllMessageCategories();
	    }
	    catch (Throwable e) {
		String strMsg = String.format("Exception Thrown: [%s]", e.getMessage() );
		mServerResponse.setText(strMsg);
	    }
	}
    
	public void deleteallnotifications (View view) {
	    try {
		mNotifyManager.DeleteAllNotifications(true);
	    }
	    catch (Throwable e) {
		String strMsg = String.format("Exception Thrown: [%s]", e.getMessage() );
		mServerResponse.setText(strMsg);
	    }
	}
      
	public void notify (View view) {
	    EditText editText = (EditText) findViewById(R.id.edit_message);
        
	    ReconNotification not = new ReconNotification ();
        
	    not.Category = mMessageCategory.categoryID;
	    not.Message =  editText.getText().toString();
	    not.Priority = ReconNotification.MessagePriority.MESSAGE_PRIORITY_IMPORTANT;
	    not.Icon = R.drawable.bullet;
	    try {
		mNotifyManager.notify(not);
	    }
	    catch (Throwable e) {
		String strMsg = String.format("Exception Thrown: [%s]", e.getMessage() );
		//Toast.makeText(getApplicationContext(), strMsg, Toast.LENGTH_SHORT).show();
		mServerResponse.setText(strMsg);
	    } 

	}

	public void getallgroups (View view) {
	    try {
		mNotifyManager.getRegisteredGroups();
	    }
	    catch (Throwable e) {
		String strMsg = String.format("Exception Thrown: [%s]", e.getMessage() );
		mServerResponse.setText(strMsg);
	    }
	}

	public void getallcategories (View view) {
	    try {
		mNotifyManager.getRegisteredCategories(mMessageGroup);
	    }
	    catch (Throwable e) {
		String strMsg = String.format("Exception Thrown: [%s]", e.getMessage() );
		mServerResponse.setText(strMsg);
	    }
	}
      
    }
