package net.robinx.lib.blurview;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.PorterDuffXfermode;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.util.AttributeSet;
import android.util.Log;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnScrollChangedListener;
import android.widget.RelativeLayout;
import net.robinx.lib.blurview.processor.BlurProcessor;

import java.util.ArrayList;
import java.io.FileOutputStream;

public class BlurBehindView extends RelativeLayout {
    public static final int UPDATE_CONTINOUSLY = 2;
    public static final int UPDATE_NEVER = 0;
    public static final int UPDATE_SCROLL_CHANGED = 1;
    private float blurRadius = 8.0f;
    private BlurRender mBlurRender;
    private boolean clipCircleOutline = false;
    private float clipCircleRadius = 1.0F;
    private float cornerRadius = 0;
    private float sizeDivider = 12.0F;
    private float paddingSide = 15.0F;
    //private int overlayColor = Color.TRANSPARENT;
    private int updateMode = UPDATE_NEVER;
    private BlurProcessor mProcessor;

    private static final int TAG_VIEW = 12345;
    public static final String TAG = "BlurBehindView";

    public BlurBehindView(Context context, View parentView) {
        super(context);
        this.init(parentView);
    }

    private void init(View parentView) {
        this.mBlurRender = new BlurRender(this.getContext(), parentView);
        this.addView(this.mBlurRender, 0, new LayoutParams(-1, -1));
        this.setTag(TAG_VIEW);
    }

    private boolean isSizeKnown() {
        return this.mBlurRender.getWidth() > 0 && this.mBlurRender.getHeight() > 0;
    }

    public float getSizeDivider() {
        return this.sizeDivider;
    }

    public BlurBehindView sizeDivider(float sizeDivider) {
        this.sizeDivider = sizeDivider;
        if (this.isSizeKnown()) {
            this.mBlurRender.initDrawingBitmap();
        }
        return this;
    }

    public float getBlurRadius() {
        return this.blurRadius;
    }

    public BlurBehindView blurRadius(float blurRadius) {
        if (this.blurRadius != blurRadius) {
            this.blurRadius = blurRadius;
            this.mBlurRender.setRenderScriptBlurRadius(blurRadius);
            this.mBlurRender.drawToBitmap();
        }
        return this;
    }

    /*public BlurBehindView overlayColor(int overlayColor) {
        this.overlayColor = overlayColor;
        this.mBlurRender.drawToBitmap();
        return this;
    }*/

    public boolean isClipCircleOutline() {
        return this.clipCircleOutline;
    }

    public BlurBehindView clipCircleOutline(boolean clipCircleOutline) {
        this.clipCircleOutline = clipCircleOutline;
        return this;
    }

    public BlurBehindView clipPath(String svgPath) {
        this.mBlurRender.initClipPath(svgPath);
        return this;
    }

    public float getClipCircleRadius() {
        return this.clipCircleRadius;
    }

    public BlurBehindView clipCircleRadius(float radius) {
        this.clipCircleRadius = radius;
        this.mBlurRender.initCirclePath();
        return this;
    }

    public float getCornerRadius() {
        return cornerRadius;
    }

    public BlurBehindView cornerRadius(float cornerRadius) {
        this.cornerRadius = Math.max(0.0F, cornerRadius);
        this.mBlurRender.initRoundRectPath();
        return this;
    }

    public float getPaddingSide() {
        return this.paddingSide;
    }

    public BlurBehindView paddingSide(float paddingSide) {
        this.paddingSide = Math.max(0.0F, paddingSide);
        this.mBlurRender.drawToBitmap();
        return this;
    }

    public BlurBehindView updateMode(int updateMode) {
        this.updateMode = updateMode;
        this.mBlurRender.drawToBitmap();
        return this;
    }

    public BlurRender getBlurRender() {
        return mBlurRender;
    }

    public BlurBehindView processor(BlurProcessor processor) {
        mProcessor = processor;
        mBlurRender.setProcessor(processor);
        this.mBlurRender.drawToBitmap();
        return this;
    }

    public class BlurRender extends View {
        private Bitmap blurBitmap;
        private Canvas blurCanvas;
        int[] childPositionInWindow = new int[2];
        final Path circlePath = new Path();
        private Path roundPath = new Path();
        private Path clipPath;
        private View blurView;
        float extraPaddingOnSides;
        int halfPaddingOnSides;
        boolean isInDrawPassFromThisView = false;
        private Bitmap snapShotBitmap;
        int[] thisPositionInWindow = new int[2];
        private BlurProcessor mProcessor;
        private float density = 1.0F;

        //RenderScript
        private RenderScript mRenderScript;
        private ScriptIntrinsicBlur mBlurScript;
        private Allocation mBlurInput, mBlurOutput;

        public OnScrollChangedListener onscrollChangedListener = new OnScrollChangedListener() {
            public void onScrollChanged() {
                if (BlurBehindView.this.updateMode == UPDATE_SCROLL_CHANGED && !BlurRender.this.isInDrawPassFromThisView) {
                    BlurRender.this.isInDrawPassFromThisView = true;
                    BlurRender.this.drawToBitmap();
                }
            }
        };

        public BlurRender(Context context, View blurView) {
            super(context);
            this.blurView = blurView;
            this.setLayerType(LAYER_TYPE_HARDWARE, null);
            this.density = this.getResources().getDisplayMetrics().density;

            //RenderScript
            this.initRenderScript(getContext());
        }

        private boolean printViewsBehind(ViewGroup rootView) {
            if (rootView == this.blurView) { 
                return true;
            }
            if (rootView.getBackground() != null) {
                this.blurCanvas.save();
                rootView.getLocationOnScreen(this.childPositionInWindow);
                this.blurCanvas.translate(
                    (float) (this.childPositionInWindow[0] 
                        - this.thisPositionInWindow[0] 
                        + this.halfPaddingOnSides), 
                    (float) (this.halfPaddingOnSides 
                        + this.childPositionInWindow[1] 
                        - this.thisPositionInWindow[1]));
                // TODO: it seems the a black background with opacity is rendered at 100% opacity
                this.blurCanvas.scale(rootView.getScaleX(), rootView.getScaleY());
                rootView.getBackground().draw(this.blurCanvas);
                this.blurCanvas.restore();
            }
            boolean renderChildViews = rootView.getAlpha() != 0.0F && rootView.getVisibility() == View.VISIBLE;
            for (int i = 0; i < rootView.getChildCount(); ++i) {
                View childView = rootView.getChildAt(i);
                /* WEIRDNESS: findViewById checks if childView is equal to id to
                 * however due to Android views being collapsed as an optimisation
                 * layer in React Native, the findViewById method will return false
                 * as the blur view may be a sibling, and not a child */
                if (childView.getId() == this.blurView.getId()) {
                    return true;
                }
                if (childView.findViewById(this.blurView.getId()) != null) {
                    if (BuildConfig.DEBUG && (rootView instanceof BlurBehindView)) {
                        Log.w(TAG, "blur view is nesting another blur view");
                    }
                    this.blurCanvas.save();
                    // no translate required
                    if (BuildConfig.DEBUG && BlurBehindView.this.blurRadius > 0 && (rootView.getScaleX() != 1 || rootView.getScaleY() != 1)) {
                        Log.w(TAG, "blur view is being scaled - this may cause render issues");
                    }
                    this.blurCanvas.scale(rootView.getScaleX(), rootView.getScaleY());
                    boolean found = this.printViewsBehind((ViewGroup) childView);
                    this.blurCanvas.restore();
                    if (found) {
                        return true;
                    }
                }
                else if (renderChildViews == true
                    && childView.getVisibility() == View.VISIBLE
                    && childView.getAlpha() != 0.0F
                    && !(childView instanceof BlurBehindView)
                ) {
                    this.blurCanvas.save();
                    childView.getLocationOnScreen(this.childPositionInWindow);
                    this.blurCanvas.translate(
                        (float) (this.halfPaddingOnSides 
                            + this.childPositionInWindow[0] 
                            - this.thisPositionInWindow[0]), 
                        (float) (this.halfPaddingOnSides 
                            + this.childPositionInWindow[1] 
                            - this.thisPositionInWindow[1]));
                    this.blurCanvas.scale(childView.getScaleX(), childView.getScaleY());
                    childView.draw(this.blurCanvas);
                    this.blurCanvas.restore();
                }
            }
            return false;
        }        

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            if (!this.isInEditMode()) {
                ((Activity) this.getContext()).getWindow().getDecorView().getViewTreeObserver().addOnScrollChangedListener(this.onscrollChangedListener);
            }
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            if (blurBitmap != null) {
                this.blurBitmap.recycle();
                this.blurBitmap = null;
            }

            if (snapShotBitmap != null) {
                this.snapShotBitmap.recycle();
                this.snapShotBitmap = null;
            }

            //RenderScript
            if (mRenderScript != null) {
                mRenderScript.destroy();
            }
            if (!this.isInEditMode()) {
                ((Activity) this.getContext()).getWindow().getDecorView().getViewTreeObserver().removeOnScrollChangedListener(this.onscrollChangedListener);
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (this.isInEditMode()) {
                canvas.drawColor(Color.TRANSPARENT);
            } else if (this.blurBitmap != null && BlurBehindView.this.blurRadius > 0.0F) {
                Paint paint = null;
                if (BlurBehindView.this.clipCircleOutline == true || this.clipPath != null || this.roundPath != null) {
                    paint = new Paint();
                    paint.setAntiAlias(true);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(0xff424242); // amount of antialiasing?
                    canvas.drawARGB(0, 0, 0, 0);
                    if (BlurBehindView.this.clipCircleOutline == true) {
                        canvas.drawPath(this.circlePath, paint);
                    }else if (this.clipPath != null) {
                        canvas.drawPath(this.clipPath, paint);
                    } else if (this.roundPath != null) {
                        canvas.drawPath(this.roundPath, paint);
                    }
                    paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
                    paint.setFlags(paint.getFlags() | Paint.FILTER_BITMAP_FLAG);
                    paint.setFilterBitmap(true);
                }
                canvas.drawBitmap(
                    this.blurBitmap,
                    null,
                    new Rect(
                        -this.halfPaddingOnSides,
                        -this.halfPaddingOnSides,
                        this.getWidth() + this.halfPaddingOnSides,
                        this.getHeight() + this.halfPaddingOnSides
                    ),
                    paint
                );

                if (BlurBehindView.this.getBackground() != null) {
                    BlurBehindView.this.getBackground().draw(canvas);
                }
                
                this.isInDrawPassFromThisView = false;
                if (BlurBehindView.this.updateMode == UPDATE_CONTINOUSLY) {
                    this.drawToBitmap();
                }
            }
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            super.onLayout(changed, left, top, right, bottom);
            if (this.snapShotBitmap == null && (right - left) > 0 && (bottom - top) > 0) {
                this.initDrawingBitmap();
                this.initCirclePath();
                this.initRoundRectPath();
            }
        }

        private void initRoundRectPath() {
            this.roundPath.reset();
            float cornerRadius = BlurBehindView.this.cornerRadius * this.density;
            RectF rectF = new RectF();
            rectF.set(0, 0, this.getWidth(), this.getHeight());
            roundPath.addRoundRect(rectF, cornerRadius, cornerRadius, Direction.CCW);
        }

        public void initCirclePath() {
            float circleRadius = BlurBehindView.this.clipCircleRadius * this.density;
            /* backward compatibility for 100% width/height */
            if (BlurBehindView.this.clipCircleRadius == 1.0) {
                circleRadius = Math.min((float) this.getWidth(), (float) (this.getHeight() / 2));
            }
            this.circlePath.reset();
            this.circlePath.addCircle(
                (float) (this.getWidth() / 2),
                (float) (this.getHeight() / 2), 
                circleRadius,
                Direction.CCW
            );
        }

        public void initClipPath(String svgPath) {
            this.clipPath = SVGParser.parsePath(svgPath, this.density);
        }

        public void initDrawingBitmap() {
            this.extraPaddingOnSides = BlurBehindView.this.paddingSide * this.density * BlurBehindView.this.sizeDivider;
            this.halfPaddingOnSides = (int) (this.extraPaddingOnSides / 2.0F);
            this.snapShotBitmap = Bitmap.createBitmap(
                (int) (
                    (float) this.getWidth() / BlurBehindView.this.sizeDivider + 
                    this.extraPaddingOnSides / BlurBehindView.this.sizeDivider
                ), 
                (int) (
                    (float) this.getHeight() / BlurBehindView.this.sizeDivider + 
                    this.extraPaddingOnSides / BlurBehindView.this.sizeDivider
                ), Config.ARGB_8888);
            this.blurCanvas = new Canvas(this.snapShotBitmap);

            //RenderScript
            mBlurInput = Allocation.createFromBitmap(mRenderScript, this.snapShotBitmap,
                    Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
            mBlurOutput = Allocation.createTyped(mRenderScript, mBlurInput.getType());

            this.drawToBitmap();

        }

        private void initRenderScript(Context context) {
            //RenderScript
            mRenderScript = RenderScript.create(context);
            mBlurScript = ScriptIntrinsicBlur.create(mRenderScript, Element.U8_4(mRenderScript));
            blurRadius(blurRadius);
        }

        private void drawToBitmap() {
            if (BlurBehindView.this.blurRadius <= 0.0f) {
                this.invalidate();
                return;
            }
            if (this.blurCanvas != null) {
                this.blurCanvas.save();
//                this.blurCanvas.drawColor(0, Mode.CLEAR);
                this.blurCanvas.drawColor(Color.BLACK, Mode.SRC_OVER);
                this.blurCanvas.scale(1.0F / BlurBehindView.this.sizeDivider, 1.0F / BlurBehindView.this.sizeDivider);
                this.getLocationOnScreen(this.thisPositionInWindow);
                

                if (!this.isInEditMode()) {
                    // Log.i(TAG, "blur view print views behind initiated");
                    boolean found = this.printViewsBehind((ViewGroup) this.getRootView());
                    if (!found) {
                        Log.e(TAG, "blur view was rendered however all children were rendered (blur view was not found in the traversal)");
                    }
                    //saveIntoFile("/sdcard/"+System.currentTimeMillis()+".png",snapShotBitmap);

                    if (mProcessor != null) {
                        this.blurBitmap = mProcessor.process(this.snapShotBitmap, Math.round(BlurBehindView.this.blurRadius));
                    } else {
                        //this.blurBitmap = NdkStackBlurProcessor.INSTANCE.process(this.snapShotBitmap, BlurBehindView.this.blurRadius);

                        //RenderScript
                        if (mBlurInput != null && mBlurScript != null && mBlurOutput != null && this.snapShotBitmap != null) {
                            if (blurBitmap == null) {
                                this.blurBitmap = Bitmap.createBitmap(
                                    (int) ((float) this.getWidth() / BlurBehindView.this.sizeDivider 
                                        + this.extraPaddingOnSides / BlurBehindView.this.sizeDivider), 
                                    (int) ((float) this.getHeight() / BlurBehindView.this.sizeDivider 
                                        + this.extraPaddingOnSides / BlurBehindView.this.sizeDivider), 
                                    Config.ARGB_8888);
                            }
                            mBlurInput.copyFrom(this.snapShotBitmap);
                            mBlurScript.setInput(mBlurInput);
                            mBlurScript.forEach(mBlurOutput);
                            mBlurOutput.copyTo(this.blurBitmap);
                        }
                    }
                }
                this.invalidate();
                this.blurCanvas.restore();
            }
        }

        public void saveIntoFile (String path, Bitmap bitmap){
            try {
                FileOutputStream out = new FileOutputStream(path);
                bitmap.compress(Bitmap.CompressFormat.PNG, 80, out);
                out.flush();
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public BlurRender setProcessor(BlurProcessor processor) {
            mProcessor = processor;
            return this;
        }

        public BlurRender setRenderScriptBlurRadius(float radius){
            //RenderScript
            if (mBlurScript != null && radius > 0.0f) {
                mBlurScript.setRadius(radius);
            }
            return this;
        }
    }
}
