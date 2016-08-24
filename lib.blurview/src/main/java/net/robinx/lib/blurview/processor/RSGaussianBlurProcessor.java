package net.robinx.lib.blurview.processor;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v8.renderscript.RenderScript;

import net.robinx.lib.blurview.algorithm.IBlur;
import net.robinx.lib.blurview.algorithm.rs.RSGaussianBlur;

/**
 * Created by Robin on 2016/8/20 15:51.
 */
public class RSGaussianBlurProcessor implements BlurProcessor {

    private volatile static RSGaussianBlurProcessor INSTANCE;

    private RSGaussianBlurProcessor(Context context) {
        mRSGaussianBlur = new RSGaussianBlur(RenderScript.create(context));
    }

    public static RSGaussianBlurProcessor getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (RSGaussianBlurProcessor.class) {
                if (INSTANCE == null) {
                    INSTANCE = new RSGaussianBlurProcessor(context);
                }
            }
        }
        return INSTANCE;
    }


    private IBlur mRSGaussianBlur;


    @Override
    public Bitmap process(Bitmap original, int radius) {
        return mRSGaussianBlur.blur(radius, original);
    }
}
