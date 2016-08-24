package net.robinx.lib.blurview;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import java.io.FileOutputStream;

/**
 * Created by Robin on 2016/8/22 12:20.
 */
public class BlurDrawable extends ColorDrawable {

    private final View mBlurredView;
    private Bitmap mSnapShotBitmap, mBlurredBitmap;
    private int mBlurredViewWidth, mBlurredViewHeight;
    private boolean mSizeDividerChanged;
    private Canvas mBlurCanvas;

    private float mCornerRadius = 0;
    private final Path mRoundClipPath = new Path();
    private final RectF mRoundRectF = new RectF();

    private int mDrawableContainerId;
    private float mOffsetX;
    private float mOffsetY;
    private int mBlurRadius = 1;
    private int mSizeDivider;
    private int mOverlayColor = Color.TRANSPARENT;

    private static boolean sEnabled = true;

    //RenderScript
    private RenderScript mRenderScript;
    private ScriptIntrinsicBlur mBlurScript;
    private Allocation mBlurInput, mBlurOutput;

    public BlurDrawable(View blurredView) {
        this.mBlurredView = blurredView;
        if (sEnabled) {
            initRenderScript(blurredView.getContext());
            initDrawingBitmap();
        }
        overlayColor(mOverlayColor);
    }

    public BlurDrawable(Activity activity) {
        this(activity.getWindow().getDecorView());
    }

    public BlurDrawable(Window blurredWindow) {
        this(blurredWindow.getDecorView());
    }

    private void initRenderScript(Context context) {
        mRenderScript = RenderScript.create(context);
        mBlurScript = ScriptIntrinsicBlur.create(mRenderScript, Element.U8_4(mRenderScript));
        blurRadius(mBlurRadius);
        sizeDivider(8);
    }

    private boolean initDrawingBitmap() {
        final int width = mBlurredView.getWidth();
        final int height = mBlurredView.getHeight();
        if (mBlurCanvas == null
                || mSizeDividerChanged
                || mBlurredViewWidth != width
                || mBlurredViewHeight != height) {
            mSizeDividerChanged = false;

            mBlurredViewWidth = width;
            mBlurredViewHeight = height;

            int scaledWidth = width / mSizeDivider;
            int scaledHeight = height / mSizeDivider;

            scaledWidth = scaledWidth - scaledWidth % 4 + 4;
            scaledHeight = scaledHeight - scaledHeight % 4 + 4;

            if (mBlurredBitmap == null
                    || mBlurredBitmap.getWidth() != scaledWidth
                    || mBlurredBitmap.getHeight() != scaledHeight) {
                mSnapShotBitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888);
                if (mSnapShotBitmap == null) {
                    return false;
                }

                mBlurredBitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888);
                if (mBlurredBitmap == null) {
                    return false;
                }
            }


//        mBlurredBitmap = Bitmap.createBitmap((int) ((float) mBlurredView.getWidth() / mSizeDivider), (int) ((float) mBlurredView.getHeight() / mSizeDivider), Bitmap.Config.ARGB_8888);
//
//        this.mSnapShotBitmap = Bitmap.createBitmap((int) ((float) mBlurredView.getWidth() / mSizeDivider), (int) ((float) mBlurredView.getHeight() / mSizeDivider), Bitmap.Config.ARGB_8888);
            mBlurCanvas = new Canvas(mSnapShotBitmap);
            mBlurCanvas.scale(1.0F / mSizeDivider, 1.0F / mSizeDivider);

            mBlurInput = Allocation.createFromBitmap(mRenderScript, mSnapShotBitmap,
                    Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
            mBlurOutput = Allocation.createTyped(mRenderScript, mBlurInput.getType());

        }
        return true;
    }

    @Override
    public void draw(Canvas canvas) {

        if (mCornerRadius != 0) {
            mRoundClipPath.reset();
            mRoundRectF.set(0, 0, canvas.getWidth(), canvas.getHeight());
            mRoundClipPath.addRoundRect(mRoundRectF, mCornerRadius, mCornerRadius, Path.Direction.CCW);
            canvas.clipPath(mRoundClipPath);
        }
        if (sEnabled) {
            drawBlur(canvas);
        }
        //draw overlayColor
        super.draw(canvas);

    }

    private void drawBlur(Canvas canvas) {
        if (initDrawingBitmap()) {

            printView(mBlurredView);
            blur(mSnapShotBitmap, mBlurredBitmap);

            canvas.save();
            canvas.translate(mBlurredView.getX() - mOffsetX, mBlurredView.getY() - mOffsetY);
            canvas.scale(mSizeDivider, mSizeDivider);
            canvas.drawBitmap(mBlurredBitmap, 0, 0, null);
            canvas.restore();
        }

    }

    private void printView(View bluredView) {
        if (!(bluredView instanceof ViewGroup)) {
            bluredView.draw(mBlurCanvas);
            return;
        }

        ViewGroup rootView = (ViewGroup) bluredView;

        if (rootView.getVisibility() == View.VISIBLE && rootView.getAlpha() != 0.0F) {
            if (rootView.findViewById(mDrawableContainerId) == null) {
                rootView.draw(mBlurCanvas);
                return;
            }

            for (int i = 0; i < rootView.getChildCount(); ++i) {
                View childView = rootView.getChildAt(i);

                if (childView.findViewById(mDrawableContainerId) != null & rootView.getVisibility() == View.VISIBLE) {
                    this.printView(childView);
                } else if (childView.getVisibility() == View.VISIBLE) {
                    childView.draw(mBlurCanvas);
                }
            }
        }
    }

    private void blur(Bitmap bitmapToBlur, Bitmap blurredBitmap) {
        if (!sEnabled) {
            return;
        }

        //just test
        //saveIntoFile("/sdcard/"+System.currentTimeMillis()+".png",mSnapShotBitmap);

        mBlurInput.copyFrom(bitmapToBlur);
        mBlurScript.setInput(mBlurInput);
        mBlurScript.forEach(mBlurOutput);
        mBlurOutput.copyTo(blurredBitmap);

        //mBlurredBitmap = NdkStackBlurProcessor.INSTANCE.process(bitmapToBlur,mBlurRadius);
    }

    /**
     * used for test
     *
     * @param path
     * @param bitmap
     */
    public void saveIntoFile(String path, Bitmap bitmap) {
        try {
            FileOutputStream out = new FileOutputStream(path);
            bitmap.compress(Bitmap.CompressFormat.PNG, 80, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public BlurDrawable offset(float x, float y) {
        this.mOffsetX = x;
        this.mOffsetY = y;
        return this;
    }

    public BlurDrawable blurRadius(int radius) {
        if (!sEnabled) {
            return this;
        }
        this.mBlurRadius = radius;
        mBlurScript.setRadius(radius);

        return this;
    }

    public BlurDrawable sizeDivider(int sizeDivider) {
        if (!sEnabled) {
            return this;
        }
        if (mSizeDivider != sizeDivider) {
            mSizeDivider = sizeDivider;
            mSizeDividerChanged = true;
        }
        return this;
    }

    public BlurDrawable cornerRadius(float radius) {
        this.mCornerRadius = radius;
        return this;
    }

    public BlurDrawable overlayColor(int color) {
        mOverlayColor = color;
        setColor(color);
        return this;
    }

    public BlurDrawable drawableContainerId(int drawableContainerId) {
        mDrawableContainerId = drawableContainerId;
        return this;
    }

    public BlurDrawable enabled(boolean enabled) {
        BlurDrawable.sEnabled = enabled;
        return this;
    }

    public void onDestroy() {
        if (!sEnabled) {
            return;
        }
        if (mRenderScript != null) {
            mRenderScript.destroy();
        }
    }

}
