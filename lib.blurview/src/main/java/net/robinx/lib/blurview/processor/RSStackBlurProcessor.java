package net.robinx.lib.blurview.processor;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v8.renderscript.RenderScript;

import net.robinx.lib.blurview.algorithm.IBlur;
import net.robinx.lib.blurview.algorithm.rs.RSStackBlur;

/**
 * Created by Robin on 2016/8/20 15:51.
 */
public class RSStackBlurProcessor implements BlurProcessor {

    private volatile static RSStackBlurProcessor INSTANCE;

    private RSStackBlurProcessor(Context context) {
        mRSStackBlur = new RSStackBlur(RenderScript.create(context), context);
    }

    public static RSStackBlurProcessor getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (RSStackBlurProcessor.class) {
                if (INSTANCE == null) {
                    INSTANCE = new RSStackBlurProcessor(context);
                }
            }
        }
        return INSTANCE;
    }


    private IBlur mRSStackBlur;


    @Override
    public Bitmap process(Bitmap original, int radius) {
        return mRSStackBlur.blur(radius, original);
    }
}
