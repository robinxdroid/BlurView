package net.robinx.blur.view;

import android.graphics.Path;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import net.robinx.lib.blurview.BlurBehindView;
import net.robinx.lib.blurview.processor.NdkStackBlurProcessor;
import net.robinx.lib.blurview.processor.RSGaussianBlurProcessor;

import java.util.ArrayList;
import java.util.List;


public class BlurBehindViewActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blur_behind_view);

        init();
    }

    private void init() {

        int diameter = 400;
        Path path = new Path();
        path.moveTo(0, 0);
        path.lineTo(diameter / 2, diameter);
        path.lineTo(diameter, 0);
        path.close();

        //Blur view
        final BlurBehindView blurBehindView = (BlurBehindView) findViewById(R.id.blur_behind_view);
        blurBehindView.updateMode(BlurBehindView.UPDATE_CONTINOUSLY)
                .blurRadius(8)
                .sizeDivider(10)
                .clipPath(path);

        final BlurBehindView blurBehindView2 = (BlurBehindView) findViewById(R.id.blur_behind_view2);
        blurBehindView2.updateMode(BlurBehindView.UPDATE_CONTINOUSLY)
                .blurRadius(8)
                .sizeDivider(10)
                .clipCircleOutline(true)
                .clipCircleRadius(1.0f);

        //Spinner
        Spinner spinner = (Spinner) this.findViewById(R.id.sp);
        List<String> strings = new ArrayList<>();
        strings.add("Scroll");
        strings.add("Continuously");
        strings.add("Never");
        spinner.setAdapter(new SpinnerAdapter(strings));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                switch (i) {
                    case 0:
                        blurBehindView.updateMode(BlurBehindView.UPDATE_SCROLL_CHANGED);
                        blurBehindView2.updateMode(BlurBehindView.UPDATE_SCROLL_CHANGED);
                        break;
                    case 1:
                        blurBehindView.updateMode(BlurBehindView.UPDATE_CONTINOUSLY);
                        blurBehindView2.updateMode(BlurBehindView.UPDATE_CONTINOUSLY);
                        break;
                    case 2:
                        blurBehindView.updateMode(BlurBehindView.UPDATE_NEVER);
                        blurBehindView2.updateMode(BlurBehindView.UPDATE_NEVER);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }


    public static class SpinnerAdapter extends BaseAdapter {

        private List<String> mStrings;

        public SpinnerAdapter(List<String> strings) {
            mStrings = strings;
        }

        @Override
        public int getCount() {
            return mStrings.size();
        }

        @Override
        public Object getItem(int i) {
            return mStrings.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            if (view == null) {
                viewHolder = new ViewHolder();
                view = View.inflate(viewGroup.getContext(), R.layout.item_spinner, null);
                viewHolder.mTextView = (TextView) view.findViewById(R.id.tv);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            viewHolder.mTextView.setText(mStrings.get(i));

            return view;
        }

        class ViewHolder {
            TextView mTextView;
        }
    }


}
