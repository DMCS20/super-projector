package com.superprojector.home;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Ajustes: icono grande a la izquierda y lista a la derecha. */
public class SettingsActivity extends Page {
    private ImageView hero;
    private TextView heroText;
    private LinearLayout list;
    private Shelf shelf;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        shelf = new Shelf(this);
        LinearLayout content = new LinearLayout(this);

        LinearLayout side = new LinearLayout(this);
        side.setOrientation(LinearLayout.VERTICAL);
        side.setGravity(Gravity.CENTER);
        hero = new ImageView(this);
        int heroSize = Ui.px(this, 180);
        side.addView(hero, new LinearLayout.LayoutParams(heroSize, heroSize));
        heroText = Ui.text(this, "", 22, Ui.MUTED, false);
        heroText.setGravity(Gravity.CENTER);
        heroText.setSingleLine(false);
        heroText.setMaxLines(2);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(Ui.px(this, 400), -2);
        textParams.topMargin = Ui.px(this, 28);
        side.addView(heroText, textParams);
        content.addView(side, new LinearLayout.LayoutParams(0, -1, 1f));

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setClipChildren(false);
        list.setClipToPadding(false);
        list.setPadding(Ui.px(this, 12), Ui.px(this, 44), Ui.px(this, 64), Ui.px(this, 60));
        list.addView(Ui.text(this, "Ajustes", 44, Ui.TEXT, true));
        View spacer = new View(this);
        list.addView(spacer, new LinearLayout.LayoutParams(1, Ui.px(this, 20)));

        Intent projector = Launch.action("com.jixin.settings5", "jixin.intent.action.SETTINGS");
        row(R.drawable.ic_settings, "Panel rápido", "Volumen, brillo y temperatura", 0xFF3C4046,
                new Intent(this, QuickPanelActivity.class));
        row(R.drawable.ic_wifi, "Wi-Fi", "Conéctate a una red", 0xFF0A5FB0, new Intent(Settings.ACTION_WIFI_SETTINGS));
        row(R.drawable.ic_bluetooth, "Bluetooth", "Mandos, audífonos y parlantes", 0xFF3A3A9E, new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
        row(R.drawable.ic_input, "Entrada", "HDMI y USB", 0xFF3A3A9E, null);
        row(R.drawable.ic_focus, "Proyector", "Enfoque, keystone e imagen", 0xFF0B6B60, projector);
        Intent speaker = Launch.forPackage(this, "com.jixin.btspeaker");
        row(R.drawable.ic_sound, "Altavoz Bluetooth", "Usa el proyector como parlante del teléfono", 0xFF3A3A9E, speaker);
        row(R.drawable.ic_sound, "Sonido", "Volumen y salida de audio", 0xFFA34A14, new Intent(Settings.ACTION_SOUND_SETTINGS));
        row(R.drawable.ic_language, "Idioma", "Idioma del sistema", 0xFF0B6B60, new Intent(Settings.ACTION_LOCALE_SETTINGS));
        row(R.drawable.ic_clock, "Fecha y hora", "Zona horaria y formato", 0xFF3C4046, new Intent(Settings.ACTION_DATE_SETTINGS));
        row(R.drawable.ic_apps, "Apps", "Administra lo instalado", 0xFF3C4046, new Intent(Settings.ACTION_APPLICATION_SETTINGS));
        row(R.drawable.ic_info, "Información", "Versión y almacenamiento", 0xFF3C4046, new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS));

        ScrollView scroll = new ScrollView(this);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(list);
        content.addView(scroll, new LinearLayout.LayoutParams(Ui.px(this, 660), -1));
        shelf.addView(content, new FrameLayout.LayoutParams(-1, -1));
        setContentView(shelf);
        list.getChildAt(2).requestFocus();
    }

    private void row(int icon, String label, String info, int color, Intent target) {
        LinearLayout row = Ui.listRow(this, icon, label, v -> {
            if (target == null) Page.open(this, InputActivity.class);
            else Launch.start(this, target);
        }, (view, focused) -> {
            if (focused) {
                hero.setImageResource(icon);
                hero.setAlpha(0f);
                hero.setScaleX(0.9f);
                hero.setScaleY(0.9f);
                hero.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(220).start();
                heroText.setText(info);
                shelf.show(new android.graphics.drawable.ColorDrawable(color));
            }
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, Ui.px(this, 58));
        params.topMargin = Ui.px(this, 6);
        list.addView(row, params);
    }
}
