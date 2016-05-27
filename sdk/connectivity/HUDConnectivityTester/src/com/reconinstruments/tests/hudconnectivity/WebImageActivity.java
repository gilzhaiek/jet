package com.reconinstruments.tests.hudconnectivity;

import java.io.InputStream;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.reconinstruments.hudconnectivitylib.http.HUDHttpRequest;
import com.reconinstruments.hudconnectivitylib.http.HUDHttpRequest.RequestMethod;
import com.reconinstruments.hudconnectivitylib.http.HUDHttpResponse;

public class WebImageActivity extends Activity {
    private final String TAG = this.getClass().getSimpleName();

    private static final boolean DEBUG = true;

    public static String EXTRA_IMAGE_URL = "IMAGE_URL";

    ImageView mImage = null;
    int mDialogRefCnt = 0;
    private ProgressDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDialog = new ProgressDialog(this);
        mDialog.setMessage("Downloading...");

        setContentView(R.layout.image_view);

        mImage = (ImageView) findViewById(R.id.imageView);
    }

    @Override
    protected void onResume() {
        super.onResume();

        String url = "http://www.reconinstruments.com/wp-content/themes/recon/img/jet/slide-3.jpg";
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            url = extras.getString(EXTRA_IMAGE_URL);
        }
        new DownloadHUDImageTask(mImage).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);
    }

    private class DownloadHUDImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadHUDImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        @Override
        protected void onPreExecute() {
            synchronized (this) {
                if (mDialogRefCnt == 0) {
                    mDialog.show();
                }
                mDialogRefCnt++;
            }
        }

        @Override
        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;

            try {

                HUDHttpRequest request = new HUDHttpRequest(RequestMethod.GET, urldisplay);
                if (DEBUG) Log.d(TAG, "sendWebRequest:" + request.getURL());
                HUDHttpResponse response = TestActivity.mHUDConnectivityManager.sendWebRequest(request);
                if (DEBUG) Log.d(TAG, "sendWebRequest(response):" + request.getURL());

                if (response.hasBody()) {
                    mIcon11 = BitmapFactory.decodeByteArray(response.getBody(), 0, response.getBody().length);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            return mIcon11;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);

            synchronized (this) {
                if (mDialogRefCnt <= 1) {
                    if (mDialog.isShowing()) {
                        mDialog.dismiss();
                    }
                    mDialogRefCnt = 0;
                } else {
                    mDialogRefCnt--;
                }
            }
        }
    }

    @SuppressWarnings("unused")
    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        @Override
        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }

}
