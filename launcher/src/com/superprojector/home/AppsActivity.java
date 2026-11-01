package com.superprojector.home;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.util.List;

public class AppsActivity extends Page {
    private static final int COLUMNS = 5;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Shelf shelf = new Shelf(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setClipChildren(false);
        list.setClipToPadding(false);
        list.setPadding(Ui.px(this, 64), Ui.px(this, 40), Ui.px(this, 64), Ui.px(this, 48));
        list.addView(Ui.text(this, "Todas las apps", 44, Ui.TEXT, true));

        List<Launch.App> apps = Launch.allApps(this);
        View first = null;
        for (int i = 0; i < apps.size(); i += COLUMNS) {
            LinearLayout row = new LinearLayout(this);
            row.setClipChildren(false);
            row.setClipToPadding(false);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, Ui.px(this, 124));
            rowParams.topMargin = Ui.px(this, i == 0 ? 32 : 26);
            for (int c = 0; c < COLUMNS; c++) {
                LinearLayout.LayoutParams cell = new LinearLayout.LayoutParams(0, -1, 1f);
                if (c > 0) cell.leftMargin = Ui.px(this, 24);
                if (i + c < apps.size()) {
                    Launch.App app = apps.get(i + c);
                    View tile = Ui.appTile(this, app, (art, name, info) -> shelf.show(art),
                            v -> Launch.start(this, app.intent));
                    if (first == null) first = tile;
                    row.addView(tile, cell);
                } else {
                    row.addView(new View(this), cell);
                }
            }
            list.addView(row, rowParams);
        }
        ScrollView scroll = new ScrollView(this);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        scroll.addView(list);
        shelf.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        setContentView(shelf);
        if (first != null) first.requestFocus();
    }
}
