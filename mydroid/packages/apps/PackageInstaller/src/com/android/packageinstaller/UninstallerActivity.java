/*
**
** Copyright 2007, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/
package com.android.packageinstaller;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;

/*
 * This activity presents UI to uninstall an application. Usually launched with intent
 * Intent.ACTION_UNINSTALL_PKG_COMMAND and attribute 
 * com.android.packageinstaller.PackageName set to the application package name
 */
public class UninstallerActivity extends AlertActivity implements 
		DialogInterface.OnClickListener {
    private static final String TAG = "UninstallerActivity";
    private boolean localLOGV = false;
    PackageManager mPm;
    private ApplicationInfo mAppInfo;
    private Button mOk;
    private Button mCancel;

    // Dialog identifiers used in showDialog
    private static final int DLG_BASE = 0;
    private static final int DLG_APP_NOT_FOUND = DLG_BASE + 1;
    private static final int DLG_UNINSTALL_FAILED = DLG_BASE + 2;

    @Override
    public Dialog onCreateDialog(int id) {
        switch (id) {
        case DLG_APP_NOT_FOUND :
            return new AlertDialog.Builder(this, com.android.internal.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle(R.string.app_not_found_dlg_title)
                    .setMessage(R.string.app_not_found_dlg_text)
                    .setNeutralButton(getString(R.string.dlg_ok), 
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    setResult(Activity.RESULT_FIRST_USER);
                                    finish();
                                }})
                    .create();
        case DLG_UNINSTALL_FAILED :
            // Guaranteed not to be null. will default to package name if not set by app
           CharSequence appTitle = mPm.getApplicationLabel(mAppInfo);
           String dlgText = getString(R.string.uninstall_failed_msg,
                    appTitle.toString());
            // Display uninstall failed dialog
            return new AlertDialog.Builder(this, com.android.internal.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle(R.string.uninstall_failed)
                    .setMessage(dlgText)
                    .setNeutralButton(getString(R.string.dlg_ok), 
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    setResult(Activity.RESULT_FIRST_USER);
                                    finish();
                                }})
                    .create();
        }
        return null;
    }

    private void startUninstallProgress() {
        Intent newIntent = new Intent(Intent.ACTION_VIEW);
        newIntent.putExtra(PackageUtil.INTENT_ATTR_APPLICATION_INFO, 
                                                  mAppInfo);
        if (getIntent().getBooleanExtra(Intent.EXTRA_RETURN_RESULT, false)) {
            newIntent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
            newIntent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        }
        newIntent.setClass(this, UninstallAppProgress.class);
        startActivity(newIntent);
        finish();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        // Get intent information.
        // We expect an intent with URI of the form package://<packageName>#<className>
        // className is optional; if specified, it is the activity the user chose to uninstall
        final Intent intent = getIntent();
        Uri packageURI = intent.getData();
        String packageName = packageURI.getEncodedSchemeSpecificPart();
        if(packageName == null) {
            Log.e(TAG, "Invalid package name:" + packageName);
            showDialog(DLG_APP_NOT_FOUND);
            return;
        }

        mPm = getPackageManager();
        boolean errFlag = false;
        try {
            mAppInfo = mPm.getApplicationInfo(packageName, PackageManager.GET_UNINSTALLED_PACKAGES);
        } catch (NameNotFoundException e) {
            errFlag = true;
        }

        // The class name may have been specified (e.g. when deleting an app from all apps)
        String className = packageURI.getFragment();
        ActivityInfo activityInfo = null;
        if (className != null) {
            try {
                activityInfo = mPm.getActivityInfo(new ComponentName(packageName, className), 0);
            } catch (NameNotFoundException e) {
                errFlag = true;
            }
        }

        if(mAppInfo == null || errFlag) {
            Log.e(TAG, "Invalid packageName or componentName in " + packageURI.toString());
            showDialog(DLG_APP_NOT_FOUND);
        } else {
            boolean isUpdate = ((mAppInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0);

            // Detail View Initialization Part
            View contentView = getLayoutInflater().inflate(R.layout.app_details, null);
            PackageUtil.initSnippetForInstalledApp(this, mAppInfo, contentView);
            TextView confirm = (TextView) contentView.findViewById(R.id.uninstall_confirm);
            confirm.setText(isUpdate? R.string.uninstall_update_text : R.string.uninstall_application_text);
            // If an activity was specified (e.g. when dragging from All Apps to trash can),
            // give a bit more info if the activity label isn't the same as the package label.
            // Patrcick Cho : This should not matter since we disabled settings 
            if (activityInfo != null) {
                CharSequence activityLabel = activityInfo.loadLabel(mPm);
                if (!activityLabel.equals(mAppInfo.loadLabel(mPm))) {
                    TextView activityText = (TextView) contentView.findViewById(R.id.activity_text);
                    CharSequence text = getString(R.string.uninstall_activity_text, activityLabel);
                    activityText.setText(text);
                    activityText.setVisibility(View.VISIBLE);
                }
            }
            
            final AlertController.AlertParams p = mAlertParams;
            p.mTitle = getString(isUpdate? R.string.uninstall_update_title : R.string.uninstall_application_title);
            p.mView = contentView;
            p.mPositiveButtonText = getString(R.string.ok);
            p.mPositiveButtonListener = this;
            p.mNegativeButtonText = getString(R.string.cancel);
            p.mNegativeButtonListener = this;
            setupAlert();
            
        }
    }

	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch (which) {
			case DialogInterface.BUTTON_POSITIVE:
				startUninstallProgress();
				break;
			case DialogInterface.BUTTON_NEGATIVE:
				dialog.dismiss();
	            finish();
				break;	
		}
	}
}
