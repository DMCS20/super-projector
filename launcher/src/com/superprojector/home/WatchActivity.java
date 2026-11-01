package com.superprojector.home;

import android.media.tv.TvContract;
import android.media.tv.TvView;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;

/** Muestra una entrada HDMI a pantalla completa. Atrás vuelve al selector. */
public class WatchActivity extends Page {
    static final String INPUT = "input";
    private static final String TAG = "SuperProjector";

    private TvView tv;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        tv = new TvView(this);
        tv.setCallback(new TvView.TvInputCallback() {
            @Override public void onConnectionFailed(String input) { Log.w(TAG, "conexión fallida " + input); }
            @Override public void onDisconnected(String input) { Log.w(TAG, "desconectado " + input); }
            @Override public void onVideoAvailable(String input) { Log.i(TAG, "video disponible " + input); }
            @Override public void onVideoUnavailable(String input, int reason) { Log.i(TAG, "sin video " + input + " razón " + reason); }
        });
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Ui.GROUND);
        root.addView(tv, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        String input = getIntent().getStringExtra(INPUT);
        tv.tune(input, TvContract.buildChannelUriForPassthroughInput(input));
    }

    @Override
    protected void onDestroy() {
        tv.reset();
        super.onDestroy();
    }
}
