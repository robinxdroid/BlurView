package net.robinx.lib.blurview.processor;

import android.graphics.Bitmap;

import net.robinx.lib.blurview.algorithm.IBlur;
import net.robinx.lib.blurview.algorithm.java.SuperFastBlur;

/**
 * Created by Robin on 2016/8/20 16:16.
 */
public enum JavaSuperFastBlurProcessor implements BlurProcessor {

    INSTANCE;

    private JavaSuperFastBlurProcessor() {
        mSuperFastBlur = new SuperFastBlur();
    }

    private IBlur mSuperFastBlur;

    @Override
    public Bitmap process(Bitmap original, int radius) {
        return mSuperFastBlur.blur(radius, original);
    }
}
