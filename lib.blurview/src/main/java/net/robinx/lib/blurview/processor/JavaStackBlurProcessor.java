package net.robinx.lib.blurview.processor;

import android.graphics.Bitmap;

import net.robinx.lib.blurview.algorithm.IBlur;
import net.robinx.lib.blurview.algorithm.java.StackBlur;

/**
 * Created by Robin on 2016/8/20 16:16.
 */
public enum JavaStackBlurProcessor implements BlurProcessor {

    INSTANCE;

    private JavaStackBlurProcessor() {
        mStackBlur = new StackBlur();
    }

    private IBlur mStackBlur;

    @Override
    public Bitmap process(Bitmap original, int radius) {
        return mStackBlur.blur(radius, original);
    }
}
