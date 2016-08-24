package net.robinx.lib.blurview.processor;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v8.renderscript.RenderScript;

import net.robinx.lib.blurview.algorithm.IBlur;
import net.robinx.lib.blurview.algorithm.rs.RSBox5x5Blur;

/**
 * Created by Robin on 2016/8/20 15:51.
 */
public class RSBox5x5BlurProcessor implements BlurProcessor {

    private volatile static RSBox5x5BlurProcessor INSTANCE;

    private RSBox5x5BlurProcessor(Context context) {
        mRSBox5x5Blur = new RSBox5x5Blur(RenderScript.create(context));
    }

    public static RSBox5x5BlurProcessor getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (RSBox5x5BlurProcessor.class) {
                if (INSTANCE == null) {
                    INSTANCE = new RSBox5x5BlurProcessor(context);
                }
            }
        }
        return INSTANCE;
    }


    private IBlur mRSBox5x5Blur;


    @Override
    public Bitmap process(Bitmap original, int radius) {
        return mRSBox5x5Blur.blur(radius, original);
    }
}
