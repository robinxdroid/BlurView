package net.robinx.blur.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    private void init() {
        this.findViewById(R.id.tv_blur).setOnClickListener(getOnClickListener());
        this.findViewById(R.id.tv_blur_drawable).setOnClickListener(getOnClickListener());
        this.findViewById(R.id.tv_blur_behind_view).setOnClickListener(getOnClickListener());
        this.findViewById(R.id.tv_blur_behind_view2).setOnClickListener(getOnClickListener());
    }

    private View.OnClickListener  mOnClickListener;
    private View.OnClickListener getOnClickListener() {
        if (mOnClickListener == null) {
            mOnClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    switch (view.getId()) {
                        case R.id.tv_blur:
                            startActivity(new Intent(MainActivity.this,BlurActivity.class));
                            break;
                        case R.id.tv_blur_drawable:
                            startActivity(new Intent(MainActivity.this,BlurDrawableActivity.class));
                            break;
                        case R.id.tv_blur_behind_view:
                            startActivity(new Intent(MainActivity.this,BlurBehindViewActivity.class));
                            break;
                        case R.id.tv_blur_behind_view2:
                            startActivity(new Intent(MainActivity.this,BlurBehindViewActivity2.class));
                            break;
                    }
                }
            };
        }
        return mOnClickListener;
    }
}
