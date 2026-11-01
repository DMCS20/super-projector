package com.superprojector.home;

import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/** Acceso directo al motor de enfoque y a la corrección de imagen del proyector. */
final class Hardware {
    private static final String TAG = "SuperProjector";
    private static final String MOTOR = "/sys/class/jixin-motor/";
    static final String KEYSTONE = "persist.vendor.hwc.keystone";

    private Hardware() {}

    /** Mueve el enfoque. Un paso positivo acerca hacia adelante y uno negativo hacia atrás. */
    static boolean focus(int steps) {
        try (FileWriter out = new FileWriter(MOTOR + (steps > 0 ? "len_goforward" : "len_goback"))) {
            out.write(Integer.toString(Math.abs(steps)));
            return true;
        } catch (IOException e) {
            Log.w(TAG, "no se pudo mover el motor", e);
            return false;
        }
    }

    static String focusPosition() {
        try (BufferedReader in = new BufferedReader(new FileReader(MOTOR + "motor_position"))) {
            return in.readLine();
        } catch (IOException e) {
            return null;
        }
    }

    static void autoFocus() {
        setProperty("persist.jfocus.usermode", "jfocus");
        setProperty("jx.sensor.status", "steady");
        setProperty("ctl.start", "jfocus");
        setProperty("ctl.start", "tof_da0103");
        setProperty("ctl.start", "tof_da0301");
    }

    static boolean setProperty(String key, String value) {
        try {
            Process process = new ProcessBuilder("/system/bin/setprop", key, value).redirectErrorStream(true).start();
            int code = process.waitFor();
            Log.i(TAG, "setprop " + key + " -> " + code);
            return code == 0;
        } catch (IOException | InterruptedException e) {
            Log.w(TAG, "setprop falló " + key, e);
            return false;
        }
    }

    static String getProperty(String key) {
        try {
            Process process = new ProcessBuilder("/system/bin/getprop", key).start();
            try (BufferedReader in = new BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String line = in.readLine();
                return line == null ? "" : line.trim();
            }
        } catch (IOException e) {
            return "";
        }
    }
}
