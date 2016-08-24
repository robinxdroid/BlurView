package net.robinx.lib.blurview.algorithm.ndk;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.robinx.lib.blurview.algorithm.IBlur;

/**
 * Blur using the NDK and native code.
 * from https://github.com/kikoso/android-stackblur/
 */
public class NdkStackBlur implements IBlur {

    private final int mExecutorThreads;
    private final ExecutorService mExecutor;

    public NdkStackBlur(int numThreads) {
        if (numThreads <= 1) {
            mExecutor = null;
            mExecutorThreads = 1;
        } else {
            mExecutorThreads = numThreads;
            mExecutor = Executors.newFixedThreadPool(mExecutorThreads);

        }
    }

    public static NdkStackBlur create() {
        return new NdkStackBlur(1);
    }

    public static NdkStackBlur createMultithreaded() {
        return new NdkStackBlur(Runtime.getRuntime().availableProcessors());
    }

    private static native void functionToBlur(Bitmap bitmapOut, int radius, int threadCount, int threadIndex, int round);

    static {
        System.loadLibrary("blur");
    }

    @Override
    public Bitmap blur(int radius, Bitmap bitmap) {
        if (mExecutorThreads == 1) {
            functionToBlur(bitmap, radius, 1, 0, 1);
            functionToBlur(bitmap, radius, 1, 0, 2);
        } else {
            int cores = mExecutorThreads;

            ArrayList<NativeTask> horizontal = new ArrayList<NativeTask>(cores);
            ArrayList<NativeTask> vertical = new ArrayList<NativeTask>(cores);
            for (int i = 0; i < cores; i++) {
                horizontal.add(new NativeTask(bitmap, radius, cores, i, 1));
                vertical.add(new NativeTask(bitmap, radius, cores, i, 2));
            }

            try {
                mExecutor.invokeAll(horizontal);
            } catch (InterruptedException e) {
                return bitmap;
            }

            try {
                mExecutor.invokeAll(vertical);
            } catch (InterruptedException e) {
                return bitmap;
            }
        }
        return bitmap;
    }

    private static class NativeTask implements Callable<Void> {
        private final Bitmap mBitmapOut;
        private final int mRadius;
        private final int mTotalCores;
        private final int mCoreIndex;
        private final int mRound;

        public NativeTask(Bitmap bitmapOut, int radius, int totalCores, int coreIndex, int round) {
            mBitmapOut = bitmapOut;
            mRadius = radius;
            mTotalCores = totalCores;
            mCoreIndex = coreIndex;
            mRound = round;
        }

        @Override
        public Void call() throws Exception {
            functionToBlur(mBitmapOut, mRadius, mTotalCores, mCoreIndex, mRound);
            return null;
        }
    }
}
