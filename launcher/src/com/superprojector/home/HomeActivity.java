package com.superprojector.home;

import android.app.Activity;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends Activity {
    private static final String SETTINGS = "com.jixin.settings5";
    private static final String[] FEATURED = {
        "com.netflix.mediaclient",
        "com.google.android.youtube.tv",
        "com.disney.disneyplus",
        "com.cbs.ca",
    };

    private Shelf shelf;
    private LinearLayout apps;
    private TextView title;
    private TextView detail;
    private TextView date;
    private ImageView network;
    private String shown;
    private ConnectivityManager connectivity;
    private final ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
        @Override public void onAvailable(Network n) { runOnUiThread(HomeActivity.this::showNetwork); }
        @Override public void onLost(Network n) { runOnUiThread(HomeActivity.this::showNetwork); }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        shelf = new Shelf(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setClipChildren(false);
        content.setClipToPadding(false);
        content.setPadding(Ui.px(this, 64), Ui.px(this, 36), Ui.px(this, 64), Ui.px(this, 40));

        content.addView(header());
        View spacer = new View(this);
        content.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1f));
        title = Ui.text(this, "", 56, Ui.TEXT, true);
        title.setIncludeFontPadding(false);
        content.addView(title);
        detail = Ui.text(this, "", 22, Ui.MUTED, false);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, Ui.px(this, 32));
        detailParams.topMargin = Ui.px(this, 8);
        detailParams.bottomMargin = Ui.px(this, 18);
        content.addView(detail, detailParams);

        apps = row();
        content.addView(apps, new LinearLayout.LayoutParams(-1, Ui.px(this, 124)));
        LinearLayout controls = projectorRow();
        LinearLayout.LayoutParams controlsParams = new LinearLayout.LayoutParams(-1, Ui.px(this, 124));
        controlsParams.topMargin = Ui.px(this, 26);
        content.addView(controls, controlsParams);

        shelf.addView(content, new FrameLayout.LayoutParams(-1, -1));
        setContentView(shelf);

        connectivity = getSystemService(ConnectivityManager.class);
        connectivity.registerDefaultNetworkCallback(callback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        fillApps();
        showNetwork();
        date.setText(new java.text.SimpleDateFormat("EEE d MMM", new java.util.Locale("es")).format(new java.util.Date()));
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

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        return row;
    }

    private View header() {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        network = new ImageView(this);
        int size = Ui.px(this, 24);
        header.addView(network, new LinearLayout.LayoutParams(size, size));
        date = Ui.text(this, "", 22, Ui.MUTED, false);
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(-2, -2);
        dateParams.leftMargin = Ui.px(this, 18);
        header.addView(date, dateParams);
        TextClock clock = new TextClock(this);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("HH:mm");
        clock.setTextColor(Ui.TEXT);
        clock.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, Ui.px(this, 26));
        clock.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        LinearLayout.LayoutParams clockParams = new LinearLayout.LayoutParams(-2, -2);
        clockParams.leftMargin = Ui.px(this, 14);
        header.addView(clock, clockParams);
        return header;
    }

    private void addCell(LinearLayout row, View tile) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1f);
        if (row.getChildCount() > 0) params.leftMargin = Ui.px(this, 24);
        row.addView(tile, params);
    }

    private void onTileFocus(android.graphics.drawable.Drawable art, String name, String info) {
        title.setText(name);
        detail.setText(info);
        shelf.show(art);
    }

    private void fillApps() {
        StringBuilder signature = new StringBuilder();
        List<Launch.App> found = new ArrayList<>();
        for (String pkg : FEATURED) {
            Launch.App app = Launch.featured(this, pkg);
            if (app != null) {
                found.add(app);
                signature.append(pkg).append(',');
            }
        }
        // Reconstruir la fila mueve el foco, así que solo se hace si cambiaron las apps.
        if (signature.toString().equals(shown)) return;
        shown = signature.toString();
        apps.removeAllViews();
        List<View> tiles = new ArrayList<>();
        for (Launch.App app : found) {
            tiles.add(Ui.appTile(this, app, this::onTileFocus, v -> Launch.start(this, app.intent)));
        }
        tiles.add(Ui.iconTile(this, R.drawable.ic_apps, "Todas", "Todas tus aplicaciones", 0xFF4A4F57, 0xFF2A2D33,
                this::onTileFocus, v -> Page.open(this, AppsActivity.class)));
        for (View tile : tiles) addCell(apps, tile);
        tiles.get(0).requestFocus();
    }

    private LinearLayout projectorRow() {
        LinearLayout row = row();
        Intent settings = Launch.action(SETTINGS, "jixin.intent.action.SETTINGS");
        addCell(row, Ui.iconTile(this, R.drawable.ic_input, "Entrada", "HDMI y otras fuentes", 0xFF5B5BD6, 0xFF3A3A9E,
                this::onTileFocus, v -> Page.open(this, InputActivity.class)));
        addCell(row, Ui.iconTile(this, R.drawable.ic_focus, "Enfoque", "Nitidez de la imagen", 0xFF12A594, 0xFF0B6B60,
                this::onTileFocus, v -> Launch.start(this, settings)));
        addCell(row, Ui.iconTile(this, R.drawable.ic_keystone, "Keystone", "Corrige la forma de la imagen", 0xFFE5762E, 0xFFA34A14,
                this::onTileFocus, v -> {
                    Intent keystone = Launch.forPackage(this, "com.droidlogic.keystone");
                    Launch.start(this, keystone != null ? keystone : settings);
                }));
        addCell(row, Ui.iconTile(this, R.drawable.ic_wifi, "Wi-Fi", "Redes e internet", 0xFF0091FF, 0xFF0A5FB0,
                this::onTileFocus, v -> Launch.start(this, new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS))));
        addCell(row, Ui.iconTile(this, R.drawable.ic_settings, "Ajustes", "Sistema y proyector", 0xFF6B7078, 0xFF3C4046,
                this::onTileFocus, v -> Page.open(this, SettingsActivity.class)));
        return row;
    }

    private void showNetwork() {
        NetworkCapabilities caps = connectivity.getNetworkCapabilities(connectivity.getActiveNetwork());
        boolean online = caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        network.setImageResource(R.drawable.ic_wifi);
        network.setAlpha(online ? 1f : 0.3f);
        network.setContentDescription(online ? "Wi-Fi conectado" : "Sin Wi-Fi");
    }
}
