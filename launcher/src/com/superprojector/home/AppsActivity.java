package com.superprojector.home;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.util.List;

public class AppsActivity extends Activity {
    private static final int COLUMNS = 5;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(Ui.px(this, 64), Ui.px(this, 44), Ui.px(this, 64), Ui.px(this, 48));
        list.addView(Ui.text(this, "Todas las apps", 44, Ui.TEXT, true));

        List<Launch.App> apps = Launch.allApps(this);
        View first = null;
        for (int i = 0; i < apps.size(); i += COLUMNS) {
            LinearLayout row = new LinearLayout(this);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, Ui.px(this, 112));
            rowParams.topMargin = Ui.px(this, i == 0 ? 32 : 16);
            for (int c = 0; c < COLUMNS; c++) {
                LinearLayout.LayoutParams cell = new LinearLayout.LayoutParams(0, -1, 1f);
                if (c > 0) cell.leftMargin = Ui.px(this, 16);
                if (i + c < apps.size()) {
                    Launch.App app = apps.get(i + c);
                    View card = Ui.card(this, null, app.label, false, v -> Launch.start(this, app.intent));
                    if (first == null) first = card;
                    row.addView(card, cell);
                } else {
                    row.addView(new View(this), cell);
                }
            }
            list.addView(row, rowParams);
        }
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.GROUND);
        scroll.addView(list);
        setContentView(scroll);
        if (first != null) first.requestFocus();
    }
}
