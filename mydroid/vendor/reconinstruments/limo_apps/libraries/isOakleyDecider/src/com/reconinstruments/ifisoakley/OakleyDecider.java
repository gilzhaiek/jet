//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.ifisoakley;
import android.os.Build;
import java.io.File;
import com.reconinstruments.utils.DeviceUtils;

public class OakleyDecider {
    protected static final String SECRECT_FILE = "/OakleyFile";
    protected static final String SECRECT_FILE_SNOW2 = "/factory/oakley";
    public static final boolean isOakley() {
        // ModLive logic
        if (DeviceUtils.isLimo()) {
            File OakleyFile = new File(SECRECT_FILE);
            return OakleyFile.exists();
        }
        else if (DeviceUtils.isSnow2()) { // Snow device
            File OakleyFile2 = new File(SECRECT_FILE_SNOW2);
            return (OakleyFile2.exists() || (Build.SERIAL.startsWith("28")));
        }
        else {                  // jet device
            return false;       // Jet is never oakley
        }
    }
}