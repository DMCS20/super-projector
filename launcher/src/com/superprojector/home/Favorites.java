package com.superprojector.home;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Apps de la fila de inicio. La última abierta pasa a ocupar el primer lugar. */
final class Favorites {
    private static final String[] DEFAULTS = {
        "com.netflix.mediaclient", "com.google.android.youtube.tv", "com.disney.disneyplus", "com.cbs.ca",
    };
    static final int MAX = 4;

    private Favorites() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences("favorites", Context.MODE_PRIVATE);
    }

    static List<String> load(Context context) {
        String saved = prefs(context).getString("list", null);
        if (saved == null) return new ArrayList<>(Arrays.asList(DEFAULTS));
        return saved.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(saved.split(",")));
    }

    private static void save(Context context, List<String> list) {
        prefs(context).edit().putString("list", String.join(",", list)).apply();
    }

    static void opened(Context context, String pkg) {
        List<String> list = load(context);
        if (list.remove(pkg)) {
            list.add(0, pkg);
            save(context, list);
        }
    }

    /** Devuelve true si la app quedó como favorita. */
    static boolean toggle(Context context, String pkg) {
        List<String> list = load(context);
        boolean added = !list.remove(pkg);
        if (added) {
            list.add(0, pkg);
            while (list.size() > MAX) list.remove(list.size() - 1);
        }
        save(context, list);
        return added;
    }
}
