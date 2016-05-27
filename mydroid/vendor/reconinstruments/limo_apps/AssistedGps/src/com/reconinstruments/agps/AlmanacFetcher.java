package com.reconinstruments.agps;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import com.reconinstruments.webapi.IReconHttpCallback;
import com.reconinstruments.webapi.ReconHttpRequest;
import com.reconinstruments.webapi.ReconHttpResponse;
import com.reconinstruments.webapi.ReconOSHttpClient;
import com.reconinstruments.webapi.ReconWebApiClient;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
/**
 * This class deals with all the complexitiy of fetching an almanac
 * file from the web and storing it on the device. The Fetch mechanism
 * is via ReconWebApi.
 *
 */
public class AlmanacFetcher {
    ReconAGpsContext mOwner;
    private static final String TAG =  "AlmanacFetcher";
    private ReconOSHttpClient mClient;
    private boolean mKeepRetrying = false;
    private Handler retryHandler = new Handler();
    final Runnable reFetchAlmanac = new Runnable(){
	    public void run()     {
		fetchAlmanacFile();
	    }
	};
    static final int RETRY_INTERVAL =  60000; // retry interval
    static final int FETCH_TIME_OUT =  10000; // 10 seconds
    public AlmanacFetcher(ReconAGpsContext agpsa) {
	mOwner = agpsa;
	mClient = new ReconOSHttpClient(mOwner.getContext(), mClientCallback);
    }
    // Some -------------test URLs---------------------------------------
    // static String DEFAULT_ALAMANC_URL_STRING =
    // 	"http://navcen.uscg.gov/?pageName=currentAlmanac&format=yuma-txt";
    // static String DEFAULT_ALAMANC_URL_STRING =
    // 	"http://echo.200please.com";
    // ---------------------------------------------------------------
    static String DEFAULT_ALAMANC_URL_STRING =
	"http://www.navcen.uscg.gov/?Do=getAlmanac&almanac=";
    // Note that day of year needs to be appended to the above url
    // TODO get external storage. For now sdcard is encoded
    public static final String ALMANAC_FILE = "/sdcard/almanac_YUMA.alm";
    private int getDayOfYear() {
	// It is assumed that the device is reasonably sane in date
	//Calendar localCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
	Calendar localCalendar = Calendar.getInstance();
	int dayOfYear = localCalendar.get(Calendar.DAY_OF_YEAR);
	Toast.makeText(mOwner.getContext(), "day of year "+dayOfYear, Toast.LENGTH_SHORT).show();	
	return dayOfYear;
    }
    /**
     * Setter for This function decides if the fetcher should keep
     * trying in case it fails to fetch the almanac for whatever
     * reason. The fetcher only tries fetching from the
     * <code>DEFAULT_ALAMANC_URL_STRING</code>
     *
     * @param val a <code>boolean</code> value
     */
    public synchronized void setKeepRetrying(boolean val) {
	mKeepRetrying = val;
    }
    /**
     * Getter for knowing if should keep trying after failing to fetch
     * almanac
     *
     * @return a <code>boolean</code> value
     */
    public boolean getKeepRetrying() {
	return mKeepRetrying;
    }
    private ReconHttpRequest setupTheHttpRequest(URL url) {
	Map<String, List<String>> headers = new HashMap<String, List<String>>();
	headers.put("Accept", Arrays.asList(new String[] { "text/plain" }));
	return new ReconHttpRequest("GET", url, FETCH_TIME_OUT, headers, null);
    }
    private void fetchAlmanacFile(URL url) {
	sendRequest(setupTheHttpRequest(url));
    }
    /**
     * Fetch the almanac from the default URL and store it in
     * <code>ALMANAC_FILE</code>
     *
     */
    public void fetchAlmanacFile() {
	try {
	    URL url = new URL(DEFAULT_ALAMANC_URL_STRING+getDayOfYear());
	    fetchAlmanacFile(url);
	} catch (MalformedURLException e) {

	}
    }
    private void sendRequest(ReconHttpRequest request) {
	if (-1 == mClient.sendRequest(request)) {
	    Log.d(TAG,"Hud not connected");
	    somethingWentWrong();
	} else { 
	    Log.d(TAG,"request sent");
	}
    }
    private IReconHttpCallback mClientCallback = new IReconHttpCallback() {
	    @Override
	    public void onReceive(int requestId, ReconHttpResponse response) {
		Log.v(TAG,new String(response.getBody()));
		writeByteArrayToAlmanacFile(response.getBody(),ALMANAC_FILE);
		setKeepRetrying(false);
		// Notify the state Mahince
		mOwner.mStateMachine.almanacUpdated();
	    }
	    @Override
	    public void onError(int requestId, ERROR_TYPE type, String message) {
		Log.w(TAG, "Error: " + type.toString() + "(" + message + ")");
		somethingWentWrong();
	    }
	};
    private void somethingWentWrong() {
	// For now we keep trying // regardles of the type of
	// the // error
	Toast.makeText(mOwner.getContext(),
		       "Could not fetch almanac", Toast.LENGTH_SHORT).show();
	if (mKeepRetrying) { 
	    retryHandler.postDelayed(reFetchAlmanac,RETRY_INTERVAL);
	}
    }
    private static void writeByteArrayToAlmanacFile(byte[] theBA, String strFilePath) {
	try {
	    FileOutputStream fos = new FileOutputStream(strFilePath);
	    fos.write(theBA);
	    fos.close();
	} catch(FileNotFoundException ex) {
	    Log.e(TAG,"FileNotFoundException : " + ex);
	} catch(IOException ioe) {
	    Log.e(TAG,"IOException : " + ioe);
	}
    }
}