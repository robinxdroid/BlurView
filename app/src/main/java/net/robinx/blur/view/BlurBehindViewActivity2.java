package net.robinx.blur.view;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;

import net.robinx.lib.blurview.BlurBehindView;
import net.robinx.lib.blurview.processor.NdkStackBlurProcessor;


public class BlurBehindViewActivity2 extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blur_behind_view2);

        init();
    }

    private void init() {

        BlurBehindView blurBehindView2 = (BlurBehindView) findViewById(R.id.blur_behind_view);
        blurBehindView2.updateMode(BlurBehindView.UPDATE_SCROLL_CHANGED)
                .cornerRadius(10)
                .processor(NdkStackBlurProcessor.INSTANCE);

    }
}
