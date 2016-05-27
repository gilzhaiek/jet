package demo.reconinstruments.com.buddytracking;

/**
 * Created by gil on 4/23/15.
 */
public class BuddyInfo {
    private final String TAG = this.getClass().getSimpleName();

    final static long MILLIS_IN_SECONDS = 1000;

    private int mID;
    private String mName;
    private double mLat;
    private double mLong;
    private long mUpdateTimeMillis;

    public BuddyInfo(int id, String name, double latitude, double longitude, long updateTime) {
        update(id, name, latitude, longitude, updateTime);
    }

    public void update(int id, String name, double latitude, double longitude, long updateTime) {
        mID = id;
        mName = name;
        mLat = latitude;
        mLong = longitude;
        mUpdateTimeMillis = updateTime;
    }

    public int getID() {
        return mID;
    }

    public String getName() {
        return mName;
    }

    public double getLat() {
        return mLat;
    }

    public double getLong() {
        return mLong;
    }

    public long getSecondsSinceLastUpdate() {
        return (System.currentTimeMillis() - mUpdateTimeMillis) / MILLIS_IN_SECONDS;
    }
}
