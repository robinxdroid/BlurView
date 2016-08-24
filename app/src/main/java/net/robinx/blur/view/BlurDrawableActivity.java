package net.robinx.blur.view;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import net.robinx.lib.blurview.BlurDrawable;


public class BlurDrawableActivity extends AppCompatActivity {

    private ScrollView mScrollView;
    private RelativeLayout mBlurDrawableRelativeLayout;

    private ViewTreeObserver.OnScrollChangedListener mOnScrollChangedListener;

    private  BlurDrawable mBlurDrawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blur_drawable);

        init();
    }

    private void init() {
        mBlurDrawableRelativeLayout = (RelativeLayout) this.findViewById(R.id.blur_drawable_container);
        mBlurDrawableRelativeLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mBlurDrawableRelativeLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                mBlurDrawable = new BlurDrawable(BlurDrawableActivity.this);
                mBlurDrawable.drawableContainerId(R.id.blur_drawable_container)
                        .cornerRadius(10)
                        .blurRadius(10)
                        .overlayColor(Color.parseColor("#64ffffff"))
                        .offset(mBlurDrawableRelativeLayout.getLeft(), mBlurDrawableRelativeLayout.getTop() );
                mBlurDrawableRelativeLayout.setBackgroundDrawable(mBlurDrawable);
            }
        });

        mScrollView = (ScrollView) this.findViewById(R.id.sv);
        mScrollView.getViewTreeObserver().addOnScrollChangedListener(getOnScrollChangedListener());
    }

    public ViewTreeObserver.OnScrollChangedListener getOnScrollChangedListener() {
        if (mOnScrollChangedListener == null) {
            mOnScrollChangedListener = new ViewTreeObserver.OnScrollChangedListener() {
                @Override
                public void onScrollChanged() {
                    //BlurDrawable
                    ViewCompat.postInvalidateOnAnimation(mBlurDrawableRelativeLayout);

                }
            };
        }
        return mOnScrollChangedListener;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mOnScrollChangedListener != null) {
            mScrollView.getViewTreeObserver().removeOnScrollChangedListener(mOnScrollChangedListener);
            mOnScrollChangedListener = null;
        }

        mBlurDrawable.onDestroy();

    }
}
