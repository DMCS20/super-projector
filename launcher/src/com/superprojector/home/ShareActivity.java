package com.superprojector.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Compartir desde el teléfono. AirPlay ya está activo; Android necesita abrir su receptor. */
public class ShareActivity extends Page {
    private static final String IPHONE = "1. Conecta el iPhone a la misma red Wi-Fi.\n"
            + "2. Abre Centro de control y toca Duplicar pantalla.\n3. Elige \"projector\".";
    private static final String ANDROID = "1. Pulsa OK aquí para abrir el receptor.\n"
            + "2. En el teléfono abre Transmitir, Smart View o Pantalla inalámbrica.\n3. Elige \"projector\".";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Shelf shelf = new Shelf(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setClipChildren(false);
        content.setClipToPadding(false);
        content.setPadding(Ui.px(this, 64), Ui.px(this, 90), Ui.px(this, 64), 0);
        TextView title = Ui.text(this, "Compartir", 56, Ui.TEXT, true);
        content.addView(title);
        TextView steps = Ui.text(this, "", 26, Ui.MUTED, false);
        steps.setSingleLine(false);
        steps.setMaxLines(4);
        steps.setEllipsize(null);
        steps.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams stepParams = new LinearLayout.LayoutParams(-1, Ui.px(this, 150));
        stepParams.topMargin = Ui.px(this, 24);
        content.addView(steps, stepParams);

        Ui.OnTileFocus focus = (art, name, info) -> {
            title.setText(name);
            steps.setText(info);
            shelf.show(art);
        };
        LinearLayout row = new LinearLayout(this);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        row.setGravity(Gravity.CENTER);
        View iphone = Ui.iconTile(this, R.drawable.ic_cast, "iPhone y Mac", IPHONE, 0xFF0091FF, 0xFF0A5FB0, focus,
                v -> android.widget.Toast.makeText(this, "Ya está listo: elige \"projector\" en tu iPhone", android.widget.Toast.LENGTH_LONG).show());
        View android = Ui.iconTile(this, R.drawable.ic_cast, "Android", ANDROID, 0xFF12A594, 0xFF0B6B60, focus,
                v -> open("com.toofifi.miracast"));
        View more = Ui.iconTile(this, R.drawable.ic_apps, "Más opciones", "Abre todas las formas de proyectar del equipo.", 0xFF6B7078, 0xFF3C4046, focus,
                v -> open("com.android.toofifi"));
        for (View tile : new View[] {iphone, android, more}) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(Ui.px(this, 260), Ui.px(this, 150));
            params.leftMargin = params.rightMargin = Ui.px(this, 16);
            row.addView(tile, params);
        }
        content.addView(row, new LinearLayout.LayoutParams(-1, Ui.px(this, 170)));
        shelf.addView(content, new FrameLayout.LayoutParams(-1, -1));
        setContentView(shelf);
        iphone.requestFocus();
    }

    private void open(String pkg) {
        Intent intent = Launch.forPackage(this, pkg);
        if (intent != null) Launch.start(this, intent);
    }
}
