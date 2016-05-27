package com.reconinstruments.phone.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

//import android.bluetooth.BluetoothMapMas;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

class IncomingMessageReceiver extends BroadcastReceiver {

	// Handles new SMS message notifications
	private static final String TAG = "IncomingMessageReceiver";
	public PhoneRelayService mTheService;
	public IncomingMessageReceiver(PhoneRelayService theService) {
		mTheService = theService;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "Received " + intent.getAction());

		String theAction = intent.getAction();
		// Default message based on type. Device will send
		// notification only for SMS_GSM and SMS_CDMA
		mTheService.mEvents.add(0, "New SMS");
		// Decode BMESSAGE object according to Bluetooth MAP
		// specification (BNF) In this example - extracted only
		// originator name and message body.
		String bmsg;
		bmsg = intent.getStringExtra("BMSG");// SS1 Message
		String name = "";
		String body = "";
		boolean benvStarted = false;
		if (bmsg != null && bmsg.length() > 0) {
			BufferedReader reader = new BufferedReader(new StringReader(bmsg));
			try {
				String s;
				do {
					s = reader.readLine();
					if (s != null) {
					    if (s.startsWith("BEGIN:BENV")){
							benvStarted = true;
					    }
					    else {
							// originator vcard is optional, skip if it is inside envelope
							if (s.startsWith("BEGIN:VCARD") && !benvStarted) {
								do {
									s = reader.readLine();
									if (s != null) {
										// Normal case, TAG N without parameters
										if (s.startsWith("N:")) {
											name = s.substring(2);
											break;
										} else
											// iPhone case : uses
											// obsolete CHARSET
											// parameter in VCARD Also
											// iPhone does not fill
											// TEL tag, so it is
											// useless to try to
											// resolve it via phone
											// book and it is needed
											// to rely on N or FN tags
											if (s.startsWith("N;")) {
												// TODO: convert from
												// other charsets if
												// needed. Now it is
												// UTF-8, so nothing
												// needed
												name = s.substring(s.indexOf(":") + 1);
												break;
											}
									}
								} while (s != null);
							} else if (s.startsWith("BEGIN:MSG")) {
								do {
									s = reader.readLine();
									if (s != null && !s.startsWith("END:MSG"))
										body += s + "\n";
									else
										break;
								} while (s != null);
							}
					    }
					}
				} while (s != null);
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			//Toast.makeText(mTheService,
			//		String.format("From: %s\n%s", name, body),
			//		Toast.LENGTH_LONG).show();
			if (body.equals("")) {
			    body = "[Picture or video received]";
			}

			// Note that iphone does not fill tel tag so we leave it empty
			mTheService.gotSMS(name, "", body);
		}
	}
}	