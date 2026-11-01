package com.superprojector.home;

import android.app.Activity;

/** Pantalla interna: entra y sale con la misma transición suave. */
public abstract class Page extends Activity {
    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.page_in, R.anim.page_out);
    }

    static void open(Activity from, Class<?> page) {
        from.startActivity(new android.content.Intent(from, page));
        from.overridePendingTransition(R.anim.page_in, R.anim.page_out);
    }
}
