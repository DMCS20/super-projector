package com.superprojector.home;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;

/** Fondo a pantalla completa que toma el color de lo que tiene el foco. */
final class Shelf extends FrameLayout {
    private final ImageView art;
    private Drawable current = new ColorDrawable(Ui.GROUND);

    Shelf(Context context) {
        super(context);
        setBackgroundColor(Ui.GROUND);
        art = new ImageView(context);
        art.setScaleType(ImageView.ScaleType.FIT_XY);
        art.setImageDrawable(current);
        addView(art, new LayoutParams(-1, Ui.px(context, 480), Gravity.TOP));
        android.view.View scrim = new android.view.View(context);
        scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {0x33000000, 0x8C0A0C10, Ui.GROUND}));
        addView(scrim, new LayoutParams(-1, Ui.px(context, 480), Gravity.TOP));
    }

    void show(Drawable source) {
        ColorDrawable next = new ColorDrawable(Ui.vividColor(source));
        TransitionDrawable fade = new TransitionDrawable(new Drawable[] {current, next});
        fade.setCrossFadeEnabled(true);
        art.setImageDrawable(fade);
        fade.startTransition(240);
        current = next;
    }
}
