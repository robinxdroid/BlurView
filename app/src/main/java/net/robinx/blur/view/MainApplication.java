package net.robinx.blur.view;

import android.app.Application;

import jp.wasabeef.takt.Takt;

/**
 * Created by Robin on 2016/8/23 12:02.
 */
public class MainApplication extends Application {

    public void onCreate() {
        super.onCreate();
        Takt.stock(this).size(20f).play();
    }

    @Override public void onTerminate() {
        Takt.finish();
        super.onTerminate();
    }
}
