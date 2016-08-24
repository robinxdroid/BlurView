package net.robinx.lib.blurview.processor;

import android.graphics.Bitmap;

import net.robinx.lib.blurview.algorithm.IBlur;
import net.robinx.lib.blurview.algorithm.IgnoreBlur;

/**
 * Created by Robin on 2016/8/20 16:16.
 */
public enum IgnoreBlurProcessor implements BlurProcessor {

    INSTANCE;

    private IgnoreBlurProcessor() {
        mIgnoreBlur = new IgnoreBlur();
    }

    private IBlur mIgnoreBlur;

    @Override
    public Bitmap process(Bitmap original, int radius) {
        return mIgnoreBlur.blur(radius, original);
    }
}
