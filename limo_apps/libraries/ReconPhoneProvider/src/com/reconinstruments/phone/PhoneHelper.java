package com.reconinstruments.phone;

import java.util.Date;
import java.util.Random;

import com.reconinstruments.messagecenter.MessageDBSchema;
import com.reconinstruments.messagecenter.MessageDBSchema.MsgSchema;
import com.reconinstruments.messagecenter.ReconMessageAPI;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;


public class PhoneHelper
{

	public static Uri saveSMS(Context context,String number,String contact,String body,boolean incoming) {
		Random r = new Random(System.currentTimeMillis());
		int notif_id = r.nextInt();

		ContentValues values = new ContentValues();
		values.put(PhoneLogProvider.KEY_TYPE, PhoneLogProvider.TYPE_SMS);
		values.put(PhoneLogProvider.KEY_SOURCE, number);
		values.put(PhoneLogProvider.KEY_CONTACT, contact);
		values.put(PhoneLogProvider.KEY_BODY, body);
		values.put(PhoneLogProvider.KEY_DATE, (new Date()).getTime());
		values.put(PhoneLogProvider.KEY_INCOMING, incoming?1:0);
		values.put(PhoneLogProvider.KEY_NOTIF_ID, notif_id);

		Uri resultUri = context.getContentResolver().insert(PhoneLogProvider.CONTENT_URI, values);

		return resultUri;
	}
	public static Uri saveCall(Context context,String number,String contact,boolean incoming) {
		Random r = new Random(System.currentTimeMillis());
		int notif_id = r.nextInt();

		ContentValues values = new ContentValues();
		values.put(PhoneLogProvider.KEY_TYPE, PhoneLogProvider.TYPE_CALL);
		values.put(PhoneLogProvider.KEY_SOURCE, number);
		values.put(PhoneLogProvider.KEY_CONTACT, contact);
		values.put(PhoneLogProvider.KEY_DATE, (new Date()).getTime());
		values.put(PhoneLogProvider.KEY_MISSED, 1);
		values.put(PhoneLogProvider.KEY_INCOMING, incoming?1:0);
		values.put(PhoneLogProvider.KEY_NOTIF_ID, notif_id);

		Uri uri = context.getContentResolver().insert(PhoneLogProvider.CONTENT_URI, values);

		return uri;
	}

	public static void answeredCall(Context context,Uri callUri){
		ContentValues values = new ContentValues();
		values.put(PhoneLogProvider.KEY_MISSED, 0);
		long callid = ContentUris.parseId(callUri);

		context.getContentResolver().update(PhoneLogProvider.CONTENT_URI, values, "_id = "+callid, null);

		ReconMessageAPI.markMessageRead(context,
				MessageDBSchema.MsgSchema.COL_EXTRA+"='"+callUri.toString()+"'");
	}

}
