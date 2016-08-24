package net.robinx.lib.blurview.processor;

import android.graphics.Bitmap;

import net.robinx.lib.blurview.algorithm.IBlur;
import net.robinx.lib.blurview.algorithm.java.GaussianFastBlur;

/**
 * Created by Robin on 2016/8/20 16:16.
 */
public enum JavaGaussianFastBlurProcessor implements BlurProcessor {

    INSTANCE;

    private JavaGaussianFastBlurProcessor() {
        mGaussianFastBlur = new GaussianFastBlur();
    }

    private IBlur mGaussianFastBlur;

    @Override
    public Bitmap process(Bitmap original, int radius) {
        return mGaussianFastBlur.blur(radius, original);
    }
}
