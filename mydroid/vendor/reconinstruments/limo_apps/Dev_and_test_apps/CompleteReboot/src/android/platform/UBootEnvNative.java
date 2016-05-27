package android.platform;

public class UBootEnvNative 
{
    public native static boolean Set_UBootVar(String bootOpt, String optValue);
    public native static String Get_UBootVar(String bootOpt);
}
