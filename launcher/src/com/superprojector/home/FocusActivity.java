package com.superprojector.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

/** Enfoque manual sobre una carta de prueba: izquierda y derecha mueven la lente. */
public class FocusActivity extends Page {
    private static final int STEP = 12;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(new Chart(this));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                Hardware.focus(-STEP);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                Hardware.focus(STEP);
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                Hardware.autoFocus();
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    private static final class Chart extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        Chart(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawColor(Ui.GROUND);
            int w = getWidth();
            int h = getHeight();
            float u = w / 1280f;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1);
            paint.setColor(0xFF3A4048);
            for (int x = 0; x <= 1280; x += 80) canvas.drawLine(x * u, 0, x * u, h, paint);
            for (int y = 0; y <= 720; y += 80) canvas.drawLine(0, y * u, w, y * u, paint);
            paint.setColor(Ui.TEXT);
            for (int r = 40; r <= 200; r += 40) canvas.drawCircle(w / 2f, h / 2f, r * u, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(52 * u);
            canvas.drawText("Enfoque", w / 2f, 110 * u, paint);
            paint.setColor(Ui.MUTED);
            paint.setTextSize(24 * u);
            canvas.drawText("Usa las flechas ◀ ▶ hasta que el texto se vea nítido.  OK: automático.", w / 2f, 620 * u, paint);
            paint.setColor(Ui.TEXT);
            int y = 655;
            for (int size : new int[] {14, 18}) {
                paint.setTextSize(size * u);
                canvas.drawText("El rápido zorro marrón salta sobre el perro perezoso 0123456789", w / 2f, y * u, paint);
                y += size + 12;
            }
        }
    }
}
