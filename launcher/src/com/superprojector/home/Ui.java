package com.superprojector.home;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Colores y tarjetas compartidos. Las medidas siguen la propuesta visual de 1280x720. */
final class Ui {
    static final int GROUND = 0xFF0E1113;
    static final int SURFACE = 0xFF1B2024;
    static final int LINE = 0xFF2C3338;
    static final int TEXT = 0xFFF2F1EC;
    static final int MUTED = 0xFFA3ABB0;
    static final int ACCENT = 0xFFFFB224;

    private Ui() {}

    static int px(Context context, float designPx) {
        return Math.round(designPx * context.getResources().getDisplayMetrics().widthPixels / 1280f);
    }

    static TextView text(Context context, String value, float size, int color, boolean medium) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, px(context, size));
        view.setTypeface(Typeface.create(medium ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        view.setSingleLine(true);
        view.setEllipsize(android.text.TextUtils.TruncateAt.END);
        return view;
    }

    private static GradientDrawable shape(Context context, int fill, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(px(context, 10));
        drawable.setColor(fill);
        if (stroke != 0) drawable.setStroke(Math.max(1, px(context, 1)), stroke);
        return drawable;
    }

    /** Tarjeta enfocable: al recibir el foco se vuelve clara para leerse a distancia. */
    static LinearLayout card(Context context, String label, String value, boolean outlined, View.OnClickListener action) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(label == null ? Gravity.BOTTOM : Gravity.TOP);
        card.setPadding(px(context, 20), px(context, 16), px(context, 20), px(context, 18));
        card.setFocusable(true);
        card.setClickable(true);
        card.setOnClickListener(action);

        TextView top = null;
        if (label != null) {
            top = text(context, label, 19, MUTED, false);
            card.addView(top);
            View gap = new View(context);
            card.addView(gap, new LinearLayout.LayoutParams(1, 0, 1f));
        }
        TextView main = text(context, value, 28, TEXT, true);
        card.addView(main);

        int restFill = outlined ? 0 : SURFACE;
        int restStroke = outlined ? LINE : 0;
        final TextView labelView = top;
        card.setBackground(shape(context, restFill, restStroke));
        card.setOnFocusChangeListener((view, focused) -> {
            view.setBackground(focused ? shape(context, TEXT, 0) : shape(context, restFill, restStroke));
            main.setTextColor(focused ? GROUND : TEXT);
            if (labelView != null) labelView.setTextColor(focused ? 0xFF3A4247 : MUTED);
        });
        return card;
    }
}
