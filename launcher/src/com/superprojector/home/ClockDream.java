package com.superprojector.home;

import android.service.dreams.DreamService;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextClock;

/** Salvapantallas: reloj grande que cambia de sitio poco a poco para no marcar la imagen. */
public class ClockDream extends DreamService {
    private final Runnable move = new Runnable() {
        @Override public void run() {
            if (clock == null) return;
            FrameLayout parent = (FrameLayout) clock.getParent();
            int x = (int) (Math.random() * Math.max(1, parent.getWidth() - clock.getWidth()));
            int y = (int) (Math.random() * Math.max(1, parent.getHeight() - clock.getHeight()));
            clock.animate().alpha(0f).setDuration(800).withEndAction(() -> {
                clock.setTranslationX(x);
                clock.setTranslationY(y);
                clock.animate().alpha(1f).setDuration(800).start();
            }).start();
            clock.postDelayed(this, 60000);
        }
    };
    private TextClock clock;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setFullscreen(true);
        setInteractive(false);
        Shelf shelf = new Shelf(this);
        clock = new TextClock(this);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("HH:mm");
        clock.setTextColor(Ui.TEXT);
        clock.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, Ui.px(this, 150));
        clock.setTypeface(android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL));
        shelf.addView(clock, new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START));
        setContentView(shelf);
        shelf.show(new android.graphics.drawable.ColorDrawable(0xFF0A5FB0));
        clock.post(move);
    }

    @Override
    public void onDetachedFromWindow() {
        clock.removeCallbacks(move);
        clock = null;
        super.onDetachedFromWindow();
    }
}
