package android.os;

/**
 * @hide
 */
public class ServiceManager {
    public static native android.os.IBinder getService(String serviceName);
}
