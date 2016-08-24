package net.robinx.lib.blurview.processor;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v8.renderscript.RenderScript;

import net.robinx.lib.blurview.algorithm.IBlur;
import net.robinx.lib.blurview.algorithm.rs.RSGaussian5x5Blur;

/**
 * Created by Robin on 2016/8/20 15:51.
 */
public class RSGaussian5x5BlurProcessor implements BlurProcessor {

    private volatile static RSGaussian5x5BlurProcessor INSTANCE;

    private RSGaussian5x5BlurProcessor(Context context) {
        mRSGaussian5x5Blur = new RSGaussian5x5Blur(RenderScript.create(context));
    }

    public static RSGaussian5x5BlurProcessor getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (RSGaussian5x5BlurProcessor.class) {
                if (INSTANCE == null) {
                    INSTANCE = new RSGaussian5x5BlurProcessor(context);
                }
            }
        }
        return INSTANCE;
    }


    private IBlur mRSGaussian5x5Blur;


    @Override
    public Bitmap process(Bitmap original, int radius) {
        return mRSGaussian5x5Blur.blur(radius, original);
    }
}
