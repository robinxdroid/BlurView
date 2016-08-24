package net.robinx.lib.blurview.processor;

import android.graphics.Bitmap;

import net.robinx.lib.blurview.algorithm.IBlur;
import net.robinx.lib.blurview.algorithm.java.BoxBlur;

/**
 * Created by Robin on 2016/8/20 16:16.
 */
public enum JavaBoxBlurProcessor implements BlurProcessor {

    INSTANCE;

    private JavaBoxBlurProcessor() {
        mBoxBlur = new BoxBlur();
    }

    private IBlur mBoxBlur;

    @Override
    public Bitmap process(Bitmap original, int radius) {
        return mBoxBlur.blur(radius, original);
    }
}
