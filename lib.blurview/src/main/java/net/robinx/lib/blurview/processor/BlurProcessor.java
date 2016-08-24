package net.robinx.lib.blurview.processor;

import android.graphics.Bitmap;

import net.robinx.lib.blurview.algorithm.IBlur;

/**
 * Created by Robin on 2016/8/20 15:18.
 */
public interface BlurProcessor {
    Bitmap process(Bitmap original,int radius);
}
