package com.superprojector.home;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Consumer;

/** Clima de la ciudad detectada por la IP. Se consulta como máximo cada media hora. */
final class Weather {
    private static final long MAX_AGE = 30 * 60 * 1000L;

    private Weather() {}

    static void load(Context context, Consumer<String> done) {
        SharedPreferences prefs = context.getSharedPreferences("weather", Context.MODE_PRIVATE);
        String cached = prefs.getString("text", "");
        if (!cached.isEmpty()) done.accept(cached);
        if (System.currentTimeMillis() - prefs.getLong("at", 0) < MAX_AGE) return;
        new Thread(() -> {
            try {
                JSONObject place = new JSONObject(get("https://ipwho.is/?fields=city,latitude,longitude"));
                JSONObject current = new JSONObject(get("https://api.open-meteo.com/v1/forecast?latitude="
                        + place.getDouble("latitude") + "&longitude=" + place.getDouble("longitude")
                        + "&current=temperature_2m")).getJSONObject("current");
                String text = Math.round(current.getDouble("temperature_2m")) + "°  " + place.getString("city");
                prefs.edit().putString("text", text).putLong("at", System.currentTimeMillis()).apply();
                done.accept(text);
            } catch (Exception e) {
                android.util.Log.i("SuperProjector", "clima no disponible: " + e);
            }
        }).start();
    }

    private static String get(String address) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(6000);
        connection.setReadTimeout(6000);
        try (InputStream in = connection.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            in.transferTo(out);
            return out.toString("UTF-8");
        } finally {
            connection.disconnect();
        }
    }
}
