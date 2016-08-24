package net.robinx.blur.view;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import net.robinx.lib.blurview.algorithm.rs.RSGaussianBlur;
import net.robinx.lib.blurview.processor.BlurProcessor;
import net.robinx.lib.blurview.processor.BlurProcessorProxy;
import net.robinx.lib.blurview.processor.IgnoreBlurProcessor;
import net.robinx.lib.blurview.processor.JavaBoxBlurProcessor;
import net.robinx.lib.blurview.processor.JavaGaussianFastBlurProcessor;
import net.robinx.lib.blurview.processor.JavaStackBlurProcessor;
import net.robinx.lib.blurview.processor.JavaSuperFastBlurProcessor;
import net.robinx.lib.blurview.processor.NdkStackBlurProcessor;
import net.robinx.lib.blurview.processor.RSBox3x3BlurProcessor;
import net.robinx.lib.blurview.processor.RSBox5x5BlurProcessor;
import net.robinx.lib.blurview.processor.RSGaussian5x5BlurProcessor;
import net.robinx.lib.blurview.processor.RSGaussianBlurProcessor;
import net.robinx.lib.blurview.processor.RSStackBlurProcessor;

import java.util.ArrayList;
import java.util.List;

public class BlurActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_CHOOSE_GALLERY_IMAGE = 0x01;
    private ImageView mRootImageView;
    private ImageView mOriginalImageView;
    private ImageView mBlurImageView;
    private CheckBox mCompressCheckBox;
    private TextView mBlurTimeTextView;

    private Bitmap mOriginalBitmap;
    private Bitmap mCompressedBitmap;

    private int blurRadius = 5;

    private static final int RS_GAUSS = 0,
            RS_BOX_3x3 = 1,
            RS_BOX_5x5 = 2,
            RS_GAUSS_5x5 = 3,
            RS_STACK = 4,
            NDK_STACK = 5,
            JAVA_GAUSS_FAST = 6,
            JAVA_BOX = 7,
            JAVA_SUPER_FAST = 8,
            JAVA_STACK = 9,
            NONE = 10;

    private int BlurMode = RS_GAUSS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blur);

        init();
    }

    private void init() {
        mRootImageView = (ImageView) this.findViewById(R.id.img_root_bg);
        mBlurTimeTextView = (TextView) this.findViewById(R.id.tv_blur_time);
        mOriginalImageView = (ImageView) this.findViewById(R.id.img_origin);
        mBlurImageView = (ImageView) this.findViewById(R.id.img_blur);
        this.findViewById(R.id.tv_choose).setOnClickListener(getOnClickListener());
        this.findViewById(R.id.tv_blur).setOnClickListener(getOnClickListener());
        Spinner spinner = (Spinner) this.findViewById(R.id.sp);
        mCompressCheckBox = (CheckBox) this.findViewById(R.id.cb_compress);
        final TextView radiusTextView = (TextView) this.findViewById(R.id.tv_radius);
        SeekBar radiusSeekBar = (SeekBar) this.findViewById(R.id.sb_radius);

        //background
        Bitmap bgBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.bg_2);
        Bitmap compressedBgBitmap = BlurUtils.compressBitmap(bgBitmap, 8);
        Bitmap blurBgBitmap = NdkStackBlurProcessor.INSTANCE.process(compressedBgBitmap, 25);
        mRootImageView.setImageBitmap(blurBgBitmap);
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(mRootImageView, View.ALPHA, 0, 1f);
        alphaAnimator.setDuration(2000);
        alphaAnimator.start();

        List<String> strings = new ArrayList<>();
        strings.add("RSGaussianBlurProcessor");
        strings.add("RSBox3x3BlurProcessor");
        strings.add("RSBox5x5BlurProcessor");
        strings.add("RSGaussian5x5BlurProcessor");
        strings.add("RSStackBlurProcessor");
        strings.add("NdkStackBlurProcessor");
        strings.add("JavaGaussianFastBlurProcessor");
        strings.add("JavaBoxBlurProcessor");
        strings.add("JavaSuperFastBlurProcessor");
        strings.add("JavaStackBlurProcessor");
        strings.add("IgnoreBlurProcessor");


        spinner.setAdapter(new SpinnerAdapter(strings));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                BlurMode = i;
                blur();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        mCompressCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                blur();
            }
        });

        radiusSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                blurRadius = i;
                radiusTextView.setText("blur radius:" + i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                blur();
            }
        });
    }

    private View.OnClickListener mOnClickListener;

    private View.OnClickListener getOnClickListener() {
        if (mOnClickListener == null) {
            mOnClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    switch (view.getId()) {
                        case R.id.tv_choose:
                            chooseGalleryImage(BlurActivity.this, REQUEST_CODE_CHOOSE_GALLERY_IMAGE);
                            break;
                        case R.id.tv_blur:

                            blur();
                            break;
                    }
                }
            };
        }
        return mOnClickListener;
    }

    private void blur() {

        if (mOriginalBitmap == null) {
            return;
        }

        final Bitmap willBlurBitmap;
        if (mCompressCheckBox.isChecked()) {
            willBlurBitmap = mCompressedBitmap;
        } else {
            willBlurBitmap = mOriginalBitmap;
        }

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(true);

        new AsyncTask<Void, Void, Bitmap>() {

            @Override
            protected void onPreExecute() {
                progressDialog.show();
            }

            @Override
            protected Bitmap doInBackground(Void... voids) {
                long blurStartTime = System.currentTimeMillis();
                Bitmap blurBitmap = null;
                BlurProcessor processor = null;
                switch (BlurMode) {
                    case RS_GAUSS:
                        Log.i("robin", "BlurMode:RS_GAUSS");
                        //blurBitmap = RSGaussianBlurProcessor.getInstance(BlurActivity.this).process(willBlurBitmap, blurRadius);
                        processor = RSGaussianBlurProcessor.getInstance(BlurActivity.this);
                        break;
                    case RS_BOX_3x3:
                        Log.i("robin", "BlurMode:RS_BOX_3x3");
                        //blurBitmap = RSBox3x3BlurProcessor.getInstance(BlurActivity.this).process(willBlurBitmap, blurRadius);
                        processor = RSBox3x3BlurProcessor.getInstance(BlurActivity.this);
                        break;
                    case RS_BOX_5x5:
                        Log.i("robin", "BlurMode:RS_BOX_5x5");
                        //blurBitmap = RSBox5x5BlurProcessor.getInstance(BlurActivity.this).process(willBlurBitmap, blurRadius);
                        processor = RSBox5x5BlurProcessor.getInstance(BlurActivity.this);
                        break;
                    case RS_GAUSS_5x5:
                        Log.i("robin", "BlurMode:RS_GAUSS_5x5");
                        //blurBitmap = RSGaussian5x5BlurProcessor.getInstance(BlurActivity.this).process(willBlurBitmap, blurRadius);
                        processor = RSGaussian5x5BlurProcessor.getInstance(BlurActivity.this);
                        break;
                    case RS_STACK:
                        Log.i("robin", "BlurMode:RS_STACK");
                        //blurBitmap = RSStackBlurProcessor.getInstance(BlurActivity.this).process(willBlurBitmap, blurRadius);
                        processor = RSStackBlurProcessor.getInstance(BlurActivity.this);
                        break;
                    case NDK_STACK:
                        Log.i("robin", "BlurMode:NDK_STACK");
                        //blurBitmap = NdkStackBlurProcessor.INSTANCE.process(willBlurBitmap, blurRadius);
                        processor = NdkStackBlurProcessor.INSTANCE;
                        break;
                    case JAVA_GAUSS_FAST:
                        Log.i("robin", "BlurMode:JAVA_GAUSS_FAST");
                        //blurBitmap = JavaGaussianFastBlurProcessor.INSTANCE.process(willBlurBitmap, blurRadius);
                        processor = JavaGaussianFastBlurProcessor.INSTANCE;
                        break;
                    case JAVA_BOX:
                        Log.i("robin", "BlurMode:JAVA_BOX");
                        //blurBitmap = JavaBoxBlurProcessor.INSTANCE.process(willBlurBitmap, blurRadius);
                        processor = JavaBoxBlurProcessor.INSTANCE;
                        break;
                    case JAVA_SUPER_FAST:
                        Log.i("robin", "BlurMode:JAVA_SUPER_FAST");
                        //blurBitmap = JavaSuperFastBlurProcessor.INSTANCE.process(willBlurBitmap, blurRadius);
                        processor = JavaSuperFastBlurProcessor.INSTANCE;
                        break;
                    case JAVA_STACK:
                        Log.i("robin", "BlurMode:JAVA_STACK");
                        //blurBitmap = JavaStackBlurProcessor.INSTANCE.process(willBlurBitmap, blurRadius);
                        processor = JavaStackBlurProcessor.INSTANCE;
                        break;
                    case NONE:
                        Log.i("robin", "BlurMode:NONE");
                        //blurBitmap = IgnoreBlurProcessor.INSTANCE.process(willBlurBitmap, blurRadius);
                        processor = IgnoreBlurProcessor.INSTANCE;
                        break;
                }

                blurBitmap = BlurProcessorProxy.INSTANCE
                        .processor(processor)
                        .copy(true)
                        .process(willBlurBitmap, blurRadius);

                final long blurTime = System.currentTimeMillis() - blurStartTime;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBlurTimeTextView.setText("blur time: " + blurTime + " ms");
                    }
                });
                return blurBitmap;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                progressDialog.dismiss();
                mBlurImageView.setImageBitmap(bitmap);
            }
        }.execute();


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_CHOOSE_GALLERY_IMAGE:
                if (data == null) {
                    return;
                }
                clearImageView();

                String picPath = chooseGalleryImageHandler(BlurActivity.this, data);
                Log.i("robin", "PicPath:" + picPath);

                // Original
                mOriginalBitmap = BitmapFactory.decodeFile(picPath);
                // Compress
                /*Matrix matrix = new Matrix();
                matrix.postScale(1.0f / 6, 1.0f / 6);
                mCompressedBitmap = Bitmap.createBitmap(mOriginalBitmap, 0, 0,
                        mOriginalBitmap.getWidth(), mOriginalBitmap.getHeight(), matrix, true);*/
                mCompressedBitmap = BlurUtils.compressBitmap(mOriginalBitmap, 6);

                mOriginalImageView.setImageBitmap(mOriginalBitmap);
                break;
        }
    }

    public void clearImageView() {
        mOriginalImageView.setImageBitmap(null);
        mBlurImageView.setImageBitmap(null);
    }

    public static void chooseGalleryImage(Activity activity, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK, null);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                "image/*");
        activity.startActivityForResult(intent, requestCode);
    }

    public static String chooseGalleryImageHandler(
            Context context, Intent data) {
        if (data == null) {
            return null;
        }
        Uri uri = data.getData();
        String filePath;
        if (uri.toString().substring(0, 4).equals("file")) {
            filePath = uri.getPath();
        } else {
            String[] proj = {MediaStore.Images.Media.DATA};
            Cursor cursor = context.getContentResolver().query(uri, proj, null, null, null);
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            filePath = cursor.getString(columnIndex);
        }
        return filePath;
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
