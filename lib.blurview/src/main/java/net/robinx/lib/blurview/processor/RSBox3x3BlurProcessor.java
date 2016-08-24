package net.robinx.lib.blurview.processor;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v8.renderscript.RenderScript;

import net.robinx.lib.blurview.algorithm.IBlur;
import net.robinx.lib.blurview.algorithm.rs.RSBox3x3Blur;

/**
 * Created by Robin on 2016/8/20 15:51.
 */
public class RSBox3x3BlurProcessor implements BlurProcessor {

    private volatile static RSBox3x3BlurProcessor INSTANCE;

    private RSBox3x3BlurProcessor(Context context) {
        mRSBox3x3Blur = new RSBox3x3Blur(RenderScript.create(context));
    }

    public static RSBox3x3BlurProcessor getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (RSBox3x3BlurProcessor.class) {
                if (INSTANCE == null) {
                    INSTANCE = new RSBox3x3BlurProcessor(context);
                }
            }
        }
        return INSTANCE;
    }


    private IBlur mRSBox3x3Blur;


    @Override
    public Bitmap process(Bitmap original, int radius) {
        return mRSBox3x3Blur.blur(radius, original);
    }
}
