package com.superprojector.home;

import android.content.Intent;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

/** Selector de entradas. Muestra el aparato conectado a cada HDMI cuando el sistema lo conoce. */
public class InputActivity extends Page {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Shelf shelf = new Shelf(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.setClipChildren(false);
        content.setClipToPadding(false);
        android.widget.TextView title = Ui.text(this, "Entrada", 56, Ui.TEXT, true);
        title.setGravity(Gravity.CENTER);
        content.addView(title, new LinearLayout.LayoutParams(-1, -2));
        android.widget.TextView detail = Ui.text(this, "Elige qué quieres ver", 22, Ui.MUTED, false);
        detail.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, -2);
        detailParams.topMargin = Ui.px(this, 8);
        detailParams.bottomMargin = Ui.px(this, 44);
        content.addView(detail, detailParams);

        LinearLayout row = new LinearLayout(this);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        row.setGravity(Gravity.CENTER);
        List<View> tiles = new ArrayList<>();
        Ui.OnTileFocus focus = (art, name, info) -> {
            title.setText(name);
            detail.setText(info);
            shelf.show(art);
        };
        for (TvInputInfo input : hdmiInputs()) {
            tiles.add(Ui.iconTile(this, R.drawable.ic_hdmi, "HDMI", "Consola, computadora u otro aparato", 0xFF5B5BD6, 0xFF3A3A9E, focus, v -> {
                Intent watch = new Intent(this, WatchActivity.class).putExtra(WatchActivity.INPUT, input.getId());
                startActivity(watch);
                overridePendingTransition(R.anim.page_in, R.anim.page_out);
            }));
        }
        tiles.add(Ui.iconTile(this, R.drawable.ic_usb, "USB", "Archivos de un pendrive o disco", 0xFF12A594, 0xFF0B6B60, focus, v -> {
            Intent files = Launch.forPackage(this, "com.jixin.filebrowser");
            if (files != null) Launch.start(this, files);
        }));
        for (View tile : tiles) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(Ui.px(this, 240), Ui.px(this, 140));
            params.leftMargin = params.rightMargin = Ui.px(this, 14);
            row.addView(tile, params);
        }
        content.addView(row, new LinearLayout.LayoutParams(-1, Ui.px(this, 150)));
        shelf.addView(content, new FrameLayout.LayoutParams(-1, -1));
        setContentView(shelf);
        tiles.get(0).requestFocus();
    }

    /** Si un puerto tiene aparato CEC, el sistema lista una entrada hija: se prefiere esa. */
    private List<TvInputInfo> hdmiInputs() {
        List<TvInputInfo> all = getSystemService(TvInputManager.class).getTvInputList();
        List<TvInputInfo> hdmi = new ArrayList<>();
        for (TvInputInfo input : all) {
            if (input.getType() != TvInputInfo.TYPE_HDMI) continue;
            boolean hasChild = false;
            for (TvInputInfo other : all) {
                if (input.getId().equals(other.getParentId())) hasChild = true;
            }
            if (!hasChild) hdmi.add(input);
        }
        return hdmi;
    }
}
