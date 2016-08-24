package net.robinx.lib.blurview.processor;

import android.graphics.Bitmap;

import net.robinx.lib.blurview.algorithm.IBlur;
import net.robinx.lib.blurview.algorithm.ndk.NdkStackBlur;

/**
 * Created by Robin on 2016/8/20 15:42.
 */
public enum NdkStackBlurProcessor implements BlurProcessor {
    INSTANCE;
    private NdkStackBlurProcessor(){
        mNdkStackBlur = NdkStackBlur.createMultithreaded();
    }

    private IBlur mNdkStackBlur;

    @Override
    public Bitmap process(Bitmap original, int radius) {
        return mNdkStackBlur.blur(radius, original);
    }
}
