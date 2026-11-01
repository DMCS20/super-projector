package com.superprojector.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.FrameLayout;

/** Fondo a pantalla completa: luces suaves con el color de lo que tiene el foco. */
final class Shelf extends FrameLayout {
    private final Aura back;
    private final Aura front;

    Shelf(Context context) {
        super(context);
        setBackgroundColor(Ui.GROUND);
        back = new Aura(context);
        front = new Aura(context);
        addView(back, new LayoutParams(-1, -1));
        addView(front, new LayoutParams(-1, -1));
    }

    void show(Drawable source) {
        int color = Ui.vividColor(source);
        if (color == front.color) return;
        back.setColor(front.color);
        back.setAlpha(1f);
        front.setColor(color);
        front.setAlpha(0f);
        front.animate().alpha(1f).setDuration(320).start();
    }

    /** Se dibuja con degradados y no con imágenes: no se pixela y casi no usa memoria. */
    private static final class Aura extends View {
        private final Paint main = new Paint(Paint.DITHER_FLAG);
        private final Paint side = new Paint(Paint.DITHER_FLAG);
        int color = Ui.GROUND;

        Aura(Context context) {
            super(context);
        }

        void setColor(int value) {
            color = value;
            rebuild();
            invalidate();
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldW, int oldH) {
            rebuild();
        }

        private void rebuild() {
            int w = getWidth();
            int h = getHeight();
            if (w == 0 || color == Ui.GROUND) return;
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);
            boolean neutral = hsv[1] < 0.15f;
            hsv[1] = neutral ? hsv[1] : Math.max(hsv[1], 0.65f);
            hsv[2] = Math.min(Math.max(hsv[2], 0.55f), 0.8f);
            int light = Color.HSVToColor(hsv);
            hsv[0] = (hsv[0] + 32f) % 360f;
            hsv[2] *= 0.8f;
            int second = Color.HSVToColor(hsv);
            main.setShader(new RadialGradient(w * 0.74f, h * 0.02f, w * 0.72f,
                    new int[] {withAlpha(light, 0xCC), withAlpha(light, 0x55), withAlpha(light, 0)},
                    new float[] {0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
            side.setShader(new RadialGradient(w * 0.08f, h * 0.1f, w * 0.52f,
                    new int[] {withAlpha(second, 0x80), withAlpha(second, 0x26), withAlpha(second, 0)},
                    new float[] {0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        }

        private static int withAlpha(int color, int alpha) {
            return (color & 0x00FFFFFF) | (alpha << 24);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawColor(Ui.GROUND);
            if (color == Ui.GROUND) return;
            canvas.drawPaint(side);
            canvas.drawPaint(main);
        }
    }
}
