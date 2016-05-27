package com.reconinstruments.socialsharing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.util.Pair;

import com.reconinstruments.mobilesdk.social.FacebookThreadMessage;
import com.reconinstruments.mobilesdk.social.FacebookPostMessage;
import com.reconinstruments.mobilesdk.social.FacebookPostMessage.Post;
import com.reconinstruments.mobilesdk.social.FacebookPostMessage.Like;
import com.reconinstruments.mobilesdk.social.FacebookPostMessage.Comment;
import com.reconinstruments.mobilesdk.social.FacebookThreadMessage.Thread;
import com.reconinstruments.messagecenter.ReconMessageAPI.ReconNotification;
import com.reconinstruments.socialsharing.R;
import com.reconinstruments.messagecenter.ReconMessageAPI;
import com.reconinstruments.mobilesdk.hudconnectivity.HUDConnectivityMessage;
import com.reconinstruments.commonwidgets.ReconToast;
import com.reconinstruments.hud_phone_status_exchange.PhoneStateMessage;
import com.reconinstruments.mobilesdk.social.SocialStatsAckMessage;
import com.reconinstruments.ifisoakley.OakleyDecider;
import com.reconinstruments.messagecenter.ReconMessageAPI;

public class SocialMessageReceiver extends BroadcastReceiver {
	private static final String TAG = "SocialMessageReceiver";

	public static final int NOTIFICATION_POSTING = 99;
	public static final String FACEBOOK_LIKE_SUFFIX = " liked this post";
	public static final String TEXT_FORMAT_PREFIX = "<b><font color=#6395ff>";
	public static final String TEXT_FORMAT_SUFFIX = "</font></b> &nbsp;&nbsp;";

	private static final String INTENT_TRACKER = "com.reconinstruments.social.facebook.fromhud";
	private static final String INTENT_TRACKER_FB_LIKES = "com.reconinstruments.social.facebook.likes.fromhud";
	private static final String INTENT_TRACKER_FB_COMMENTS = "com.reconinstruments.social.facebook.comments.fromhud";
	private static final String INTENT_SHAREDAY = "com.reconinstruments.social.facebook.ShareDayActivity";
	private static final String INTENT_FACEBOOK_STATUS = "com.reconinstruments.social.facebook.status";
	private static final String INTENT_FACEBOOK_SHARE_STATUS = "com.reconinstruments.social.tohud.share_status";
	private static final String INTENT_FACEBOOK_POST_FAILED = "com.reconinstruments.social.facebook.post.failed";
	private static final String INTENT_FACEBOOK_MESSAGE = "com.reconinstruments.social.facebook.tohud.thread";
	private static final String INTENT_FACEBOOK_LIKES_AND_COMMENTS = "com.reconinstruments.social.facebook.tohud.likes_and_comments";
    private static final String INTENT_FIRST_RUN_OF_THE_DAY = "com.reconinstruments.applauncher.transcend.FIRST_RUN_OF_THE_DAY";
	private static final String FACEBOOK_GRP_URI = "com.reconinstruments.social.facebook";
	private static final String FACEBOOK_GRP_DESC = "FACEBOOK";
	private static final int FACEBOOK_GRP_ICON = R.drawable.fb_icon;
	private static final int FACEBOOK_COMMENT_ICON = R.drawable.facebook_comment;
	private static final int FACEBOOK_LIKE_ICON = R.drawable.facebook_like;
	private static final int FACEBOOK_MESSAGE_ICON = R.drawable.facebook_message;

	@Override
	public void onReceive(Context context, Intent intent) {

		Log.d(TAG, "action: " + intent.getAction());
		String intentAction = intent.getAction();
		if (intentAction.equals(PhoneStateMessage.INTENT)) {
			byte[] msgBytes = intent.getExtras().getByteArray("message");
			HUDConnectivityMessage cMsg = new HUDConnectivityMessage(msgBytes);
			PhoneStateMessage psMsg = new PhoneStateMessage(new String(cMsg.getData()));
			if (psMsg.getSocialNetworks() != null) {
				if (psMsg.getSocialNetworks().contains(PhoneStateMessage.SOCIAL_NETWORK.FACEBOOK)) {
					Settings.System.putInt(context.getContentResolver(), "isFacebookConnected", 1);
				} else {
					Settings.System.putInt(context.getContentResolver(), "isFacebookConnected", 0);
				}
			}
		} else if (intentAction	.equals(SocialStatsAckMessage.INTENT)) {
			byte[] msgBytes = intent.getExtras().getByteArray("message");
			HUDConnectivityMessage cMsg = new HUDConnectivityMessage(msgBytes);
			Pair<Boolean, String> result = SocialStatsAckMessage
					.parse(new String(cMsg.getData()));
			//temporarily commented out until new implementation
			/*if (Boolean.TRUE.equals(result.first)){ //avoid NullPointerException
                (new ReconToast(context, com.reconinstruments.commonwidgets.R.drawable.checkbox_icon, "Message posted on Facebook")).show();
			}else{
		        (new ReconToast(context, com.reconinstruments.commonwidgets.R.drawable.error_icon, result.second)).show();
			}*/
			NotificationManager notificationManager = (NotificationManager) 
					context.getSystemService(Context.NOTIFICATION_SERVICE); 
			notificationManager.cancel(NOTIFICATION_POSTING);
		}else if (intentAction.equals(INTENT_FACEBOOK_MESSAGE)) {
			byte[] msgBytes = intent.getExtras().getByteArray("message");
			HUDConnectivityMessage cMsg = new HUDConnectivityMessage(msgBytes);
			Collection<Thread> threads = FacebookThreadMessage
					.parse(new String(cMsg.getData()));
			for (FacebookThreadMessage.Thread thread : threads) {
				List<FacebookThreadMessage.Message> messages = new ArrayList<FacebookThreadMessage.Message>(thread.messages);
				Collections.reverse(messages);
				for (FacebookThreadMessage.Message message : messages) {
					sendFacebookMessage(context, thread.id,
							collectionConvertToString(thread.participants), message.text);
				}
			}
		} else if (intentAction.equals(INTENT_FACEBOOK_LIKES_AND_COMMENTS)) {
			byte[] msgBytes = intent.getExtras().getByteArray("message");
			HUDConnectivityMessage cMsg = new HUDConnectivityMessage(msgBytes);
			Collection<Post> posts = FacebookPostMessage.parse(new String(
					cMsg.getData()));
			for (FacebookPostMessage.Post post : posts) {
				for (FacebookPostMessage.Like like : post.likes) {
					sendFacebookLikes(context, post.id + ".like", rebuildTitle(post.title), like.sender + FACEBOOK_LIKE_SUFFIX);
				}
				List<FacebookPostMessage.Like> likeList = new ArrayList<FacebookPostMessage.Like>(post.likes);
				if(likeList.size() > 1){
					postFacebookLikeToStatusBar(context, rebuildTitle(post.title), likeList.get(0).sender + " and " + (likeList.size() - 1) + " others like: " + rebuildTitle(post.title));
				}else if(likeList.size() > 0){
					postFacebookLikeToStatusBar(context, rebuildTitle(post.title), likeList.get(0).sender + " likes: " + rebuildTitle(post.title));
				}
				if(post.comments.size() > 1){
					sendFacebookComments(context, post.id + ".comment",
							rebuildTitle(post.title), TEXT_FORMAT_PREFIX + post.comments.get(0).sender + TEXT_FORMAT_SUFFIX + post.comments.get(0).message, true, TEXT_FORMAT_PREFIX + post.comments.get(0).sender + TEXT_FORMAT_SUFFIX + " and " + (post.comments.size() - 1) + " others commented");
					for (int i = 1; i<post.comments.size(); i++ ) {
						FacebookPostMessage.Comment comment = post.comments.get(i);
						sendFacebookComments(context, post.id + ".comment",
								rebuildTitle(post.title), TEXT_FORMAT_PREFIX + comment.sender + TEXT_FORMAT_SUFFIX + comment.message, false, null);
					}
				}else if(post.comments.size() > 0){
					for (FacebookPostMessage.Comment comment : post.comments) {
						sendFacebookComments(context, post.id + ".comment",
								rebuildTitle(post.title), TEXT_FORMAT_PREFIX + comment.sender + TEXT_FORMAT_SUFFIX + comment.message, true, null);
					}
				}
			}
		}
	}
	
	private String rebuildTitle(String oldTitle){
		String newTitle = null;
		if(oldTitle.contains("Vertical Milestone")){
			newTitle = "Vertical Milestone";
		}else if(oldTitle.contains("Distance Milestone")){
			newTitle = "Distance Milestone";
		}else if(oldTitle.contains("Max Altitude Milestone")){
			newTitle = "Max Altitude Milestone";
		}else if(oldTitle.contains("All Time Best Air")){
			newTitle = "All Time Best Air";
		}else if(oldTitle.contains("All Time Max Speed")){
			newTitle = "All Time Max Speed";
		}else{
			newTitle = oldTitle;
		}
		return newTitle;
	}

	private String collectionConvertToString(Collection<String> col){
		if(col == null || col.size() == 0){
			return "";
		}else{
			StringBuffer buffer = new StringBuffer();
			for(String name : col){
				buffer.append(name);
				buffer.append(", ");
			}
			return buffer.substring(0, buffer.length() - 2) ;
		}
	}
	
	private void sendFacebookMessage(Context context, String subGrpName,
			String subGrpDesc, String messageText) {
		ReconNotification notification = new ReconNotification(context,
				FACEBOOK_GRP_URI, FACEBOOK_GRP_DESC, FACEBOOK_GRP_ICON,
				subGrpName, subGrpDesc, FACEBOOK_MESSAGE_ICON, messageText);
		sendNotification(notification);
	}

	private void sendFacebookLikes(Context context, String subGrpName,
			String subGrpDesc, String messageText) {
		ReconNotification notification = new ReconNotification(context,
				FACEBOOK_GRP_URI, FACEBOOK_GRP_DESC, FACEBOOK_GRP_ICON,
				subGrpName, subGrpDesc, FACEBOOK_LIKE_ICON, messageText);
		Intent goToTrackerApp = new Intent(INTENT_TRACKER_FB_LIKES);
		notification.overrideMessageViewer(goToTrackerApp);
		ReconMessageAPI.postNotification(notification, false, false);
	}
    
    private void postFacebookLikeToStatusBar(Context context, String title, String text){
        ReconMessageAPI.showPassiveNotification(context, text, ReconMessageAPI.NOTIFICATION_LONG_DURATION);
    }

	private void sendFacebookComments(Context context, String subGrpName,
			String subGrpDesc, String messageText, boolean showInteractiveNotification, String overwrittenDesc) {
		ReconNotification notification = new ReconNotification(context,
				FACEBOOK_GRP_URI, FACEBOOK_GRP_DESC, FACEBOOK_GRP_ICON,
				subGrpName, subGrpDesc, FACEBOOK_COMMENT_ICON, messageText);
		Intent goToTrackerApp = new Intent(INTENT_TRACKER_FB_COMMENTS);
		notification.overrideMessageViewer(goToTrackerApp);
		if(overwrittenDesc == null || overwrittenDesc.length() == 0){
			ReconMessageAPI.postNotification(notification, false, showInteractiveNotification);
		}else{
			ReconMessageAPI.postNotification(notification, false, showInteractiveNotification, overwrittenDesc);
		}
	}

	private void sendNotification(ReconNotification notification) {
		Intent goToTrackerApp = new Intent(INTENT_TRACKER);
		notification.overrideMessageViewer(goToTrackerApp);
		ReconMessageAPI.postNotification(notification, false, true);
	}
	
    private void postShareTodayActivityNotification(Context context) {
		ReconNotification notification =
		    new ReconNotification(context, FACEBOOK_GRP_URI, FACEBOOK_GRP_DESC,
					  FACEBOOK_GRP_ICON,FACEBOOK_GRP_URI+".ShareActivity",
					  "Share Your Day",
					  FACEBOOK_COMMENT_ICON,
					  "Post your stats on Facebook");
		Intent goToTrackerApp = new Intent(INTENT_SHAREDAY);
		notification.overrideMessageViewer(goToTrackerApp);
		Uri uri = ReconMessageAPI.postNotification(notification, true, true);
    }
}
