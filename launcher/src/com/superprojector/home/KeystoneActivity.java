package com.superprojector.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

/** Corrección de keystone por cuatro esquinas: OK cambia de esquina y las flechas la mueven. */
public class KeystoneActivity extends Page {
    private static final int STEP = 6;
    private static final int MAX_X = 1920;
    private static final int MAX_Y = 1080;
    private static final String[] NAMES = {"Superior izquierda", "Superior derecha", "Inferior derecha", "Inferior izquierda"};

    /** x,y de cada esquina, en el orden que usa la propiedad del sistema. */
    private final int[] corners = {0, 0, MAX_X, 0, MAX_X, MAX_Y, 0, MAX_Y};
    private int selected;
    private Frame frame;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        String[] saved = Hardware.getProperty(Hardware.KEYSTONE).split(",");
        if (saved.length == corners.length) {
            for (int i = 0; i < saved.length; i++) corners[i] = Integer.parseInt(saved[i].trim());
        }
        frame = new Frame(this);
        setContentView(frame);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        int dx = 0;
        int dy = 0;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT: dx = -STEP; break;
            case KeyEvent.KEYCODE_DPAD_RIGHT: dx = STEP; break;
            case KeyEvent.KEYCODE_DPAD_UP: dy = -STEP; break;
            case KeyEvent.KEYCODE_DPAD_DOWN: dy = STEP; break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                selected = (selected + 1) % 4;
                frame.invalidate();
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
        corners[selected * 2] = clamp(corners[selected * 2] + dx, MAX_X);
        corners[selected * 2 + 1] = clamp(corners[selected * 2 + 1] + dy, MAX_Y);
        apply();
        frame.invalidate();
        return true;
    }

    private static int clamp(int value, int max) {
        return Math.max(0, Math.min(max, value));
    }

    private void apply() {
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < corners.length; i++) {
            if (i > 0) value.append(", ");
            value.append(corners[i]);
        }
        Hardware.setProperty(Hardware.KEYSTONE, value.toString());
    }

    private final class Frame extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        Frame(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth();
            int h = getHeight();
            float u = w / 1280f;
            canvas.drawColor(Ui.GROUND);
            float inset = 3 * u;
            Path border = new Path();
            border.addRect(inset, inset, w - inset, h - inset, Path.Direction.CW);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4 * u);
            paint.setColor(Ui.TEXT);
            canvas.drawPath(border, paint);

            float[][] points = {{0, 0}, {w, 0}, {w, h}, {0, h}};
            paint.setStyle(Paint.Style.FILL);
            for (int i = 0; i < 4; i++) {
                paint.setColor(i == selected ? 0xFF0091FF : 0xFF4A4F57);
                float x = Math.max(36 * u, Math.min(w - 36 * u, points[i][0]));
                float y = Math.max(36 * u, Math.min(h - 36 * u, points[i][1]));
                canvas.drawCircle(x, y, (i == selected ? 22 : 14) * u, paint);
            }
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(Ui.TEXT);
            paint.setTextSize(56 * u);
            canvas.drawText("Keystone", w / 2f, h / 2f - 40 * u, paint);
            paint.setTextSize(26 * u);
            canvas.drawText(NAMES[selected], w / 2f, h / 2f + 10 * u, paint);
            paint.setColor(Ui.MUTED);
            paint.setTextSize(22 * u);
            canvas.drawText("Las flechas mueven la esquina azul. OK cambia de esquina. Atrás guarda y sale.", w / 2f, h / 2f + 60 * u, paint);
        }
    }
}
