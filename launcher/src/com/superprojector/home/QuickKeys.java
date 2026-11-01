package com.superprojector.home;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/** Abre el panel rápido con la tecla Menú desde cualquier app. */
public class QuickKeys extends AccessibilityService {
    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        android.util.Log.i("SuperProjector", "tecla " + event.getKeyCode());
        if (event.getKeyCode() != KeyEvent.KEYCODE_MENU) return false;
        if (event.getAction() == KeyEvent.ACTION_UP) {
            startActivity(new Intent(this, QuickPanelActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
        return true;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {}
    @Override public void onInterrupt() {}
}
