package net.robinx.lib.blurview.algorithm;

import android.graphics.Bitmap;

/**
 * This is the default algorithm, that does nothing but returns
 * the original bitmap
 */
public class IgnoreBlur implements IBlur {
	@Override
	public Bitmap blur(int radius, Bitmap original) {
		return original;
	}
}
