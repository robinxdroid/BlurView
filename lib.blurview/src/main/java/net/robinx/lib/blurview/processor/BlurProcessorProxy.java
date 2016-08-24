package net.robinx.lib.blurview.processor;

import android.graphics.Bitmap;

/**
 * Created by Robin on 2016/8/22 10:12.
 */
public enum BlurProcessorProxy implements BlurProcessor {

    INSTANCE;

    private BlurProcessorProxy() {
    }

    private BlurProcessor mBlurProcessor;
    private boolean mCopy;

    @Override
    public Bitmap process(Bitmap original, int radius) {
        Bitmap bitmap = buildBitmap(original, mCopy);
        if (mBlurProcessor == null) {
            mBlurProcessor = NdkStackBlurProcessor.INSTANCE;
        }
        return mBlurProcessor.process(bitmap, radius);
    }

    private static Bitmap buildBitmap(Bitmap bitmap, boolean copy) {
        // If can copy return copy
        Bitmap rBitmap;
        if (copy) {
            rBitmap = bitmap.copy(bitmap.getConfig(), true);
        } else {
            rBitmap = bitmap;
        }
        return (rBitmap);
    }

    public BlurProcessorProxy processor(BlurProcessor processor) {
        mBlurProcessor = processor;
        return this;
    }

    public BlurProcessorProxy copy(boolean copy) {
        mCopy = copy;
        return this;
    }
}
