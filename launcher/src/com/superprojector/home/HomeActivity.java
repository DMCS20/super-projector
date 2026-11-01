package com.superprojector.home;

import android.app.Activity;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends Activity {
    private static final String SETTINGS = "com.jixin.settings5";
    private static final String[][] FEATURED = {
        {"Netflix", "com.netflix.mediaclient"},
        {"YouTube", "com.google.android.youtube.tv"},
        {"Disney+", "com.disney.disneyplus"},
        {"Paramount+", "com.cbs.ca"},
    };

    private LinearLayout apps;
    private TextView network;
    private TextView date;
    private ConnectivityManager connectivity;
    private final ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
        @Override public void onAvailable(Network n) { runOnUiThread(HomeActivity.this::showNetwork); }
        @Override public void onLost(Network n) { runOnUiThread(HomeActivity.this::showNetwork); }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.GROUND);
        root.setPadding(Ui.px(this, 64), Ui.px(this, 44), Ui.px(this, 64), Ui.px(this, 48));

        root.addView(header());
        root.addView(title("Apps", 38));
        apps = new LinearLayout(this);
        root.addView(apps, new LinearLayout.LayoutParams(-1, Ui.px(this, 156)));
        root.addView(title("Proyector", 34));
        root.addView(projectorRow(), new LinearLayout.LayoutParams(-1, Ui.px(this, 112)));
        setContentView(root);

        connectivity = getSystemService(ConnectivityManager.class);
        connectivity.registerDefaultNetworkCallback(callback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        fillApps();
        showNetwork();
        date.setText(new java.text.SimpleDateFormat("EEEE d 'de' MMMM", new java.util.Locale("es")).format(new java.util.Date()));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Volver con la tecla Inicio siempre deja el foco en la primera app.
        if (apps.getChildCount() > 0) apps.getChildAt(0).requestFocus();
    }

    @Override
    protected void onDestroy() {
        connectivity.unregisterNetworkCallback(callback);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        // El inicio es la raíz: Atrás no debe salir de aquí.
    }

    private View header() {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.BOTTOM);

        TextClock clock = new TextClock(this);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("HH:mm");
        clock.setTextColor(Ui.TEXT);
        clock.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, Ui.px(this, 88));
        clock.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        clock.setIncludeFontPadding(false);
        clock.setLetterSpacing(-0.03f);
        header.addView(clock);

        date = new TextView(this);
        date.setTextColor(Ui.MUTED);
        date.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, Ui.px(this, 22));
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(0, -2, 1f);
        dateParams.leftMargin = Ui.px(this, 24);
        dateParams.bottomMargin = Ui.px(this, 10);
        header.addView(date, dateParams);

        network = Ui.text(this, "", 20, Ui.MUTED, false);
        LinearLayout.LayoutParams networkParams = new LinearLayout.LayoutParams(-2, -2);
        networkParams.bottomMargin = Ui.px(this, 10);
        header.addView(network, networkParams);
        return header;
    }

    private TextView title(String value, int top) {
        TextView view = Ui.text(this, value, 20, Ui.MUTED, true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
        params.topMargin = Ui.px(this, top);
        params.bottomMargin = Ui.px(this, 14);
        view.setLayoutParams(params);
        return view;
    }

    private void addCell(LinearLayout row, View card, boolean first) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1f);
        if (!first) params.leftMargin = Ui.px(this, 16);
        row.addView(card, params);
    }

    private void fillApps() {
        View focused = getCurrentFocus();
        boolean hadFocusInApps = focused != null && focused.getParent() == apps;
        apps.removeAllViews();
        List<View> cards = new ArrayList<>();
        for (String[] app : FEATURED) {
            Intent open = Launch.forPackage(this, app[1]);
            if (open != null) {
                cards.add(Ui.card(this, null, app[0], false, v -> Launch.start(this, open)));
            }
        }
        cards.add(Ui.card(this, null, "Todas", false,
                v -> startActivity(new Intent(this, AppsActivity.class))));
        for (int i = 0; i < cards.size(); i++) addCell(apps, cards.get(i), i == 0);
        if (focused == null || hadFocusInApps) cards.get(0).requestFocus();
    }

    private View projectorRow() {
        LinearLayout row = new LinearLayout(this);
        Intent input = Launch.action(SETTINGS, "jixin.intent.action.INPUT_SOURCE_SELECT");
        Intent settings = Launch.action(SETTINGS, "jixin.intent.action.SETTINGS");
        addCell(row, Ui.card(this, "Entrada", "Elegir", true, v -> Launch.start(this, input)), true);
        addCell(row, Ui.card(this, "Imagen", "Enfoque", true, v -> Launch.start(this, settings)), false);
        addCell(row, Ui.card(this, "Imagen", "Keystone", true, v -> {
            Intent keystone = Launch.forPackage(this, "com.droidlogic.keystone");
            Launch.start(this, keystone != null ? keystone : settings);
        }), false);
        addCell(row, Ui.card(this, "Sistema", "Wi-Fi", true,
                v -> Launch.start(this, new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS))), false);
        addCell(row, Ui.card(this, "Sistema", "Ajustes", true, v -> Launch.start(this, settings)), false);
        return row;
    }

    private void showNetwork() {
        NetworkCapabilities caps = connectivity.getNetworkCapabilities(connectivity.getActiveNetwork());
        boolean online = caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        network.setText(online ? "Wi-Fi conectado" : "Sin Wi-Fi");
        android.graphics.drawable.GradientDrawable dot = new android.graphics.drawable.GradientDrawable();
        dot.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        dot.setColor(online ? Ui.ACCENT : Ui.LINE);
        dot.setSize(Ui.px(this, 10), Ui.px(this, 10));
        network.setCompoundDrawablesRelativeWithIntrinsicBounds(dot, null, null, null);
        network.setCompoundDrawablePadding(Ui.px(this, 12));
    }
}
