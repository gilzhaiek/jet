package com.contour.utils;

import android.content.Context;

import com.contour.api.BaseCameraComms;
import com.contour.connect.R;

public class ContourUtils {

    public static String getCameraModel(Context context, int deviceModel) {
        if (deviceModel == BaseCameraComms.MODEL_PLUS_2) {
            return context.getString(R.string.camera_contour_plus_two);
        } else if (deviceModel == BaseCameraComms.MODEL_PLUS) {
            return context.getString(R.string.camera_contour_plus);

        } else if (deviceModel == BaseCameraComms.MODEL_GPS) {
            return context.getString(R.string.camera_contour_gps);
        } else {
            return context.getString(R.string.contourcamera);
        }
    }
}
