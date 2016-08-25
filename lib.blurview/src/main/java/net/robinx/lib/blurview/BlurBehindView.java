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
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnScrollChangedListener;
import android.widget.RelativeLayout;

import net.robinx.lib.blurview.processor.BlurProcessor;

import java.io.FileOutputStream;

public class BlurBehindView extends RelativeLayout {
    public static final int UPDATE_CONTINOUSLY = 2;
    public static final int UPDATE_NEVER = 0;
    public static final int UPDATE_SCROLL_CHANGED = 1;
    private int blurRadius = 8;
    private BlurRender mBlurRender;
    private boolean clipCircleOutline = false;
    private float clipCircleRadius = 1.0F;
    private float cornerRadius = 0;
    private float sizeDivider = 12.0F;
    //private int overlayColor = Color.TRANSPARENT;
    private int updateMode = UPDATE_NEVER;
    private BlurProcessor mProcessor;

    private static final int TAG_VIEW = 10000;

    public BlurBehindView(Context context) {
        super(context);
        this.init();
    }

    public BlurBehindView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.init();
    }

    public BlurBehindView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.init();
    }

    private void init() {
        this.mBlurRender = new BlurRender(this.getContext());
        this.addView(this.mBlurRender, 0, new LayoutParams(-1, -1));
        this.setTag(TAG_VIEW);
    }

    private boolean isSizeKnown() {
        if (this.mBlurRender.getWidth() != 0) {
            return true;
        }
        return false;
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

    public BlurBehindView blurRadius(int blurRadius) {
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

    public BlurBehindView clipPath(Path clipPath) {
        this.mBlurRender.setClipPath(clipPath);
        this.mBlurRender.invalidate();
        return this;
    }

    public float getClipCircleRadius() {
        return this.clipCircleRadius;
    }

    public BlurBehindView clipCircleRadius(float radius) {
        this.clipCircleRadius = Math.max(0.0F, Math.min(1.0F, radius));
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
        public static final int PADDING_SIDE_TO_AVOID_FLICKER = 15;
        private Bitmap blurBitmap;
        private Canvas blurCanvas;
        int[] childPositionInWindow = new int[2];
        final Path circlePath = new Path();
        private Path roundPath = new Path();
        private Path clipPath;
        float extraPaddingOnSides;
        int halfPaddingOnSides;
        boolean isInDrawPassFromThisView = false;
        private Bitmap snapShotBitmap;
        int[] thisPositionInWindow = new int[2];
        private BlurProcessor mProcessor;

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

        public BlurRender(Context context) {
            super(context);
            this.setLayerType(LAYER_TYPE_HARDWARE, null);

            //RenderScript
            this.initRenderScript(getContext());
        }

        private void printViewsBehind(ViewGroup rootView) {
            if (!this.isInEditMode() && !(rootView instanceof BlurBehindView) && rootView.getVisibility() == View.VISIBLE && rootView.getAlpha() != 0.0F) {
                if (rootView.getBackground() != null) {
                    this.blurCanvas.save();
                    this.blurCanvas.translate((float) (this.childPositionInWindow[0] - this.thisPositionInWindow[0] + this.halfPaddingOnSides), (float) (this.halfPaddingOnSides + this.childPositionInWindow[1] - this.thisPositionInWindow[1]));
                    rootView.getBackground().draw(this.blurCanvas);
                    this.blurCanvas.restore();
                }

                for (int i = 0; i < rootView.getChildCount(); ++i) {
                    View childView = rootView.getChildAt(i);
                    if (childView.findViewWithTag(TAG_VIEW) != null & rootView.getVisibility() == View.VISIBLE) {
                        this.printViewsBehind((ViewGroup) childView);
                    } else if (childView.getVisibility() == View.VISIBLE) {
                        this.blurCanvas.save();
                        childView.getLocationOnScreen(this.childPositionInWindow);
                        this.blurCanvas.translate((float) (this.halfPaddingOnSides + this.childPositionInWindow[0] - this.thisPositionInWindow[0]), (float) (this.halfPaddingOnSides + this.childPositionInWindow[1] - this.thisPositionInWindow[1]));
                        this.blurCanvas.scale(childView.getScaleX(), childView.getScaleY());
                        childView.draw(this.blurCanvas);
                        this.blurCanvas.restore();
                    }
                }
            }
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
                if (BlurBehindView.this.clipCircleOutline) {
                    canvas.clipPath(this.circlePath);
                }else if (clipPath != null){
                    canvas.clipPath(this.clipPath);
                } else if (this.roundPath != null) {
                    canvas.clipPath(this.roundPath);
                }

                canvas.drawBitmap(this.blurBitmap, null, new Rect(-this.halfPaddingOnSides, -this.halfPaddingOnSides, this.getWidth() + this.halfPaddingOnSides, this.getHeight() + this.halfPaddingOnSides), (Paint) null);
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
            if (this.snapShotBitmap == null) {
                this.initDrawingBitmap();
                this.initCirclePath();
                this.initRoundRectPath();
            }
        }

        private void initRoundRectPath() {
            this.roundPath.reset();
            RectF rectF = new RectF();
            rectF.set(0, 0, this.getWidth(), this.getHeight());
            roundPath.addRoundRect(rectF, BlurBehindView.this.cornerRadius, BlurBehindView.this.cornerRadius, Direction.CCW);
        }

        public void initCirclePath() {
            this.circlePath.reset();
            this.circlePath.addCircle((float) (this.getWidth() / 2), (float) (this.getHeight() / 2), Math.min((float) this.getWidth(), (float) (this.getHeight() / 2) * BlurBehindView.this.clipCircleRadius), Direction.CCW);
        }

        public void initDrawingBitmap() {
            this.extraPaddingOnSides = PADDING_SIDE_TO_AVOID_FLICKER * this.getResources().getDisplayMetrics().density * BlurBehindView.this.sizeDivider;
            this.halfPaddingOnSides = (int) (this.extraPaddingOnSides / 2.0F);
            this.snapShotBitmap = Bitmap.createBitmap((int) ((float) this.getWidth() / BlurBehindView.this.sizeDivider + this.extraPaddingOnSides / BlurBehindView.this.sizeDivider), (int) ((float) this.getHeight() / BlurBehindView.this.sizeDivider + this.extraPaddingOnSides / BlurBehindView.this.sizeDivider), Config.ARGB_8888);
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
            if (this.blurCanvas != null) {
                this.blurCanvas.save();
                this.blurCanvas.drawColor(0, Mode.CLEAR);
                //this.blurCanvas.drawColor(BlurBehindView.this.overlayColor,Mode.SRC_OVER);
                this.blurCanvas.scale(1.0F / BlurBehindView.this.sizeDivider, 1.0F / BlurBehindView.this.sizeDivider);
                this.getLocationOnScreen(this.thisPositionInWindow);
                this.printViewsBehind((ViewGroup) this.getRootView());
                if (!this.isInEditMode()) {

                    //saveIntoFile("/sdcard/"+System.currentTimeMillis()+".png",snapShotBitmap);

                    if (mProcessor != null) {
                        this.blurBitmap = mProcessor.process(this.snapShotBitmap, BlurBehindView.this.blurRadius);
                        Log.i("robin", "Do Blur Processor");
                    } else {
                        //this.blurBitmap = NdkStackBlurProcessor.INSTANCE.process(this.snapShotBitmap, BlurBehindView.this.blurRadius);

                        //RenderScript
                        if (mBlurInput != null && mBlurScript != null && mBlurOutput != null && this.snapShotBitmap != null) {
                            if (blurBitmap == null) {
                                this.blurBitmap = Bitmap.createBitmap((int) ((float) this.getWidth() / BlurBehindView.this.sizeDivider + this.extraPaddingOnSides / BlurBehindView.this.sizeDivider), (int) ((float) this.getHeight() / BlurBehindView.this.sizeDivider + this.extraPaddingOnSides / BlurBehindView.this.sizeDivider), Config.ARGB_8888);
                            }
                            mBlurInput.copyFrom(this.snapShotBitmap);
                            mBlurScript.setInput(mBlurInput);
                            mBlurScript.forEach(mBlurOutput);
                            mBlurOutput.copyTo(this.blurBitmap);
                            Log.i("robin", "Do Blur RenderScript");
                        }

                    }

                }

                this.invalidate();
                this.blurCanvas.restore();
            }

        }

        public BlurRender setClipPath(Path clipPath) {
            this.clipPath = clipPath;
            return this;
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

        public BlurRender setRenderScriptBlurRadius(int radius){
            //RenderScript
            if (mBlurScript != null) {
                mBlurScript.setRadius(radius);
            }
            return this;
        }
    }
}
