package com.reconinstruments.navigation.navigation;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;

public class ImageHelper {
    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int pixels) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth()+2*pixels, bitmap.getHeight()+2*pixels, Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xffffffff;
        final Paint paint = new Paint();
        final Rect origRect = new Rect(0, 0, bitmap.getWidth()+2*pixels, bitmap.getHeight()+2*pixels);
        final Rect imageRect = new Rect(pixels, pixels, bitmap.getWidth()+pixels, bitmap.getHeight()+pixels);
        final RectF rectF = new RectF(origRect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, pixels, pixels, paint);

        canvas.drawBitmap(bitmap, pixels, pixels, null);

        return output;
    }
}