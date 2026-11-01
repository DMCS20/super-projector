package com.superprojector.home;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Panel lateral sobre lo que se esté viendo: volumen, brillo y estado del equipo. */
public class QuickPanelActivity extends Page {
    private AudioManager audio;
    private TextView volume;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        audio = getSystemService(AudioManager.class);
        getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getWindow().setDimAmount(0.35f);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setClipChildren(false);
        panel.setClipToPadding(false);
        panel.setPadding(Ui.px(this, 28), Ui.px(this, 36), Ui.px(this, 28), Ui.px(this, 28));
        GradientDrawable fill = new GradientDrawable();
        fill.setColor(0xF01B1E24);
        fill.setCornerRadius(Ui.px(this, 22));
        panel.setBackground(fill);
        panel.addView(Ui.text(this, "Panel rápido", 34, Ui.TEXT, true));
        String temperature = Hardware.getProperty("jx.projector.temperature");
        if (!temperature.isEmpty()) {
            panel.addView(Ui.text(this, "Temperatura " + Math.round(Double.parseDouble(temperature)) + " °C", 20, Ui.MUTED, false));
        }
        volume = Ui.text(this, "", 22, Ui.MUTED, false);
        LinearLayout.LayoutParams volumeParams = new LinearLayout.LayoutParams(-1, -2);
        volumeParams.topMargin = Ui.px(this, 18);
        panel.addView(volume, volumeParams);

        View down = row(panel, R.drawable.ic_sound, "Bajar volumen", v -> changeVolume(AudioManager.ADJUST_LOWER));
        row(panel, R.drawable.ic_sound, "Subir volumen", v -> changeVolume(AudioManager.ADJUST_RAISE));
        row(panel, R.drawable.ic_input, "Brillo", v -> {
            sendBroadcast(new Intent("jixin.intent.action.BRIGHNESS_MODE_DIALOG").setPackage("com.jixin.settings5"));
            finish();
        });
        row(panel, R.drawable.ic_focus, "Enfoque", v -> Page.open(this, FocusActivity.class));
        row(panel, R.drawable.ic_cast, "Compartir", v -> Page.open(this, ShareActivity.class));
        row(panel, R.drawable.ic_settings, "Ajustes", v -> Page.open(this, SettingsActivity.class));

        FrameLayout root = new FrameLayout(this);
        int margin = Ui.px(this, 20);
        FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(Ui.px(this, 400), -1, Gravity.END);
        panelParams.setMargins(margin, margin, margin, margin);
        root.addView(panel, panelParams);
        setContentView(root);
        panel.setTranslationX(Ui.px(this, 420));
        panel.animate().translationX(0).setDuration(240)
                .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        showVolume();
        down.requestFocus();
    }

    private void changeVolume(int direction) {
        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0);
        showVolume();
    }

    private void showVolume() {
        int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int now = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        volume.setText("Volumen " + Math.round(now * 100f / max) + " %");
    }

    private View row(LinearLayout panel, int icon, String label, View.OnClickListener action) {
        View row = Ui.listRow(this, icon, label, action, null);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, Ui.px(this, 58));
        params.topMargin = Ui.px(this, 6);
        panel.addView(row, params);
        return row;
    }
}
