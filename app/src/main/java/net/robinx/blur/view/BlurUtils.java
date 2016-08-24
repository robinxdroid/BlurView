package net.robinx.blur.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;

import java.lang.reflect.Field;

/**
 * Created by Robin on 2016/7/25 12:12.
 */
public class BlurUtils {

    public static Bitmap compressBitmap(Bitmap bitmap,int compressFactor){
        Matrix matrix = new Matrix();
        matrix.postScale(1.0f / compressFactor, 1.0f / compressFactor);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0,bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return bitmap;
    }

    public static int getStatusBarHeight(Context context) {
        Class<?> c = null;
        Object obj = null;
        Field field = null;
        int x = 0;
        try {
            c = Class.forName("com.android.internal.R$dimen");
            obj = c.newInstance();
            field = c.getField("status_bar_height");
            x = Integer.parseInt(field.get(obj).toString());
            return context.getResources().getDimensionPixelSize(x);
        } catch (Exception e1) {
            e1.printStackTrace();
            return 75;
        }
    }
}
