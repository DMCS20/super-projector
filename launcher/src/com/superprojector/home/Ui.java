package com.superprojector.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

/** Colores y mosaicos compartidos. Las medidas siguen una pantalla de 1280x720. */
final class Ui {
    static final int GROUND = 0xFF0A0C10;
    static final int TEXT = 0xFFF5F5F7;
    static final int MUTED = 0xCCE3E5EA;

    interface OnTileFocus {
        void onFocus(Drawable art, String title, String detail);
    }

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

    private static FrameLayout frame(Context context, View.OnClickListener action) {
        FrameLayout tile = new FrameLayout(context);
        tile.setFocusable(true);
        tile.setClickable(true);
        tile.setOnClickListener(action);
        tile.setElevation(px(context, 4));
        tile.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), px(context, 14));
            }
        });
        tile.setClipToOutline(true);
        return tile;
    }

    /** Al recibir el foco el mosaico crece y se eleva, como en las interfaces de TV modernas. */
    private static void animateFocus(Context context, View tile, Drawable art, String title, String detail, OnTileFocus listener) {
        tile.setOnFocusChangeListener((view, focused) -> {
            float scale = focused ? 1.1f : 1f;
            view.animate().scaleX(scale).scaleY(scale).translationZ(focused ? px(context, 24) : 0)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(focused ? 2.2f : 0f))
                    .setDuration(focused ? 240 : 160).start();
            if (focused && listener != null) listener.onFocus(art, title, detail);
        });
    }

    /** Mosaico de una app: usa su banner real o, si no tiene, su icono sobre su propio color. */
    static View appTile(Context context, Launch.App app, OnTileFocus listener, View.OnClickListener action) {
        FrameLayout tile = frame(context, action);
        ImageView image = new ImageView(context);
        if (app.banner != null) {
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setImageDrawable(app.banner);
            tile.addView(image, new FrameLayout.LayoutParams(-1, -1));
        } else {
            tile.setBackground(new ColorDrawable(averageColor(app.icon)));
            image.setImageDrawable(app.icon);
            int size = px(context, 72);
            tile.addView(image, new FrameLayout.LayoutParams(size, size, Gravity.CENTER));
        }
        animateFocus(context, tile, app.banner != null ? app.banner : app.icon, app.label, "", listener);
        return tile;
    }

    /** Mosaico de una función del sistema: icono vectorial sobre un degradado de color. */
    static View iconTile(Context context, int icon, String label, String detail, int top, int bottom,
            OnTileFocus listener, View.OnClickListener action) {
        FrameLayout tile = frame(context, action);
        GradientDrawable fill = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] {top, bottom});
        tile.setBackground(fill);
        ImageView image = new ImageView(context);
        image.setImageResource(icon);
        int size = px(context, 44);
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(size, size, Gravity.CENTER);
        iconParams.bottomMargin = px(context, 18);
        tile.addView(image, iconParams);
        TextView name = text(context, label, 18, TEXT, true);
        FrameLayout.LayoutParams nameParams = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        nameParams.bottomMargin = px(context, 10);
        tile.addView(name, nameParams);
        animateFocus(context, tile, fill.getConstantState().newDrawable(), label, detail, listener);
        return tile;
    }

    /** Reduce la imagen a unos pocos píxeles: al ampliarla queda difuminada casi sin gastar memoria. */
    static Bitmap tiny(Drawable art, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Drawable copy = art.getConstantState() != null ? art.getConstantState().newDrawable().mutate() : art;
        copy.setBounds(0, 0, width, height);
        copy.draw(new Canvas(bitmap));
        return bitmap;
    }

    /** Color más vivo de la imagen, para teñir el fondo sin que un logo oscuro lo apague. */
    static int vividColor(Drawable art) {
        Bitmap sample = tiny(art, 16, 9);
        int best = averageColor(art);
        float bestScore = 0.12f;
        float[] hsv = new float[3];
        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 16; x++) {
                int pixel = sample.getPixel(x, y);
                android.graphics.Color.colorToHSV(pixel, hsv);
                float score = hsv[1] * hsv[2];
                if (score > bestScore) {
                    bestScore = score;
                    best = pixel | 0xFF000000;
                }
            }
        }
        sample.recycle();
        return best;
    }

    static int averageColor(Drawable art) {
        Bitmap pixel = tiny(art, 1, 1);
        int color = pixel.getPixel(0, 0);
        pixel.recycle();
        return color | 0xFF000000;
    }
}
