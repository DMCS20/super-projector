package com.superprojector.home;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Abre apps y las pantallas del fabricante que todavía controlan el hardware. */
final class Launch {
    private Launch() {}

    static final class App {
        final String label;
        final Intent intent;

        App(String label, Intent intent) {
            this.label = label;
            this.intent = intent;
        }
    }

    static Intent forPackage(Context context, String pkg) {
        PackageManager pm = context.getPackageManager();
        Intent intent = pm.getLeanbackLaunchIntentForPackage(pkg);
        return intent != null ? intent : pm.getLaunchIntentForPackage(pkg);
    }

    static Intent action(String pkg, String action) {
        return new Intent(action).setPackage(pkg);
    }

    static void start(Context context, Intent intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (RuntimeException e) {
            Toast.makeText(context, "No se pudo abrir", Toast.LENGTH_SHORT).show();
        }
    }

    static List<App> allApps(Context context) {
        PackageManager pm = context.getPackageManager();
        Map<String, App> apps = new LinkedHashMap<>();
        for (String category : new String[] {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            Intent query = new Intent(Intent.ACTION_MAIN).addCategory(category);
            for (ResolveInfo info : pm.queryIntentActivities(query, 0)) {
                String pkg = info.activityInfo.packageName;
                if (pkg.equals(context.getPackageName()) || apps.containsKey(pkg)) continue;
                Intent open = new Intent(Intent.ACTION_MAIN).addCategory(category)
                        .setComponent(new ComponentName(pkg, info.activityInfo.name));
                apps.put(pkg, new App(info.loadLabel(pm).toString(), open));
            }
        }
        List<App> sorted = new ArrayList<>(apps.values());
        Collections.sort(sorted, (a, b) -> a.label.compareToIgnoreCase(b.label));
        return sorted;
    }
}
