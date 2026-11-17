package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

/**
 * Very light atmospheric drift: softly faded, low-alpha cloud wisps over a static wallpaper.
 * No video decoder, images, bitmap allocations or per-frame gradient construction.
 * At most 12 frames per second; completely stopped while Home is not foreground.
 */
public final class WindyCloudView extends View {
    private static final long FRAME_MS = 83;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Paint mist = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pink = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean homeVisible;
    private boolean userEnabled;
    private boolean scheduled;
    private long startTime;

    private final Runnable frame = new Runnable() {
        @Override public void run() {
            scheduled = false;
            if (!homeVisible || !userEnabled) return;
            invalidate();
            schedule();
        }
    };

    public WindyCloudView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mist.setShader(new RadialGradient(0f, 0f, 1f,
                new int[]{0x27EEE1FF, 0x18D8C7FF, 0x00D8C7FF},
                new float[]{0f, .45f, 1f}, Shader.TileMode.CLAMP));
        pink.setShader(new RadialGradient(0f, 0f, 1f,
                new int[]{0x22FFD7F7, 0x10FFC5EF, 0x00FFC5EF},
                new float[]{0f, .42f, 1f}, Shader.TileMode.CLAMP));
        setClickable(false);
        setFocusable(false);
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    void setCloudsEnabled(boolean enabled) {
        userEnabled = enabled;
        setVisibility(enabled ? VISIBLE : GONE);
        if (enabled && homeVisible) schedule();
        else handler.removeCallbacks(frame);
    }

    void onHomeResume() {
        homeVisible = true;
        if (startTime == 0L) startTime = SystemClock.uptimeMillis();
        schedule();
    }

    void onHomePause() {
        homeVisible = false;
        handler.removeCallbacks(frame);
        scheduled = false;
    }

    private void schedule() {
        if (!scheduled && homeVisible && userEnabled) {
            scheduled = true;
            handler.postDelayed(frame, FRAME_MS);
        }
    }

    @Override protected void onDraw(Canvas c) {
        if (!userEnabled || getWidth() == 0 || getHeight() == 0) return;
        float w = getWidth(), h = getHeight();
        float sec = (SystemClock.uptimeMillis() - startTime) / 1000f;
        // Offsets and speeds differ per layer, mimicking wind through the upper sky and valley.
        for (int i = 0; i < 5; i++) {
            float speed = w / (i % 2 == 0 ? 37f : 48f);
            float span = w * 1.95f;
            float x = ((sec * speed + i * (span / 5f)) % span) - w * .40f;
            float y = h * (i < 3 ? (.20f + .10f * i) : (.54f + .05f * (i - 3)));
            float rw = w * (i < 3 ? .37f : .46f);
            float rh = h * (i < 3 ? .087f : .075f);
            drawWisp(c, x, y, rw, rh, i % 2 == 0 ? mist : pink);
            drawWisp(c, x - rw * .34f, y + rh * .20f, rw * .72f, rh * .8f,
                    i % 2 == 0 ? mist : pink);
        }
    }

    private static void drawWisp(Canvas canvas, float x, float y, float rx, float ry, Paint p) {
        canvas.save();
        canvas.translate(x, y);
        canvas.scale(rx, ry);
        canvas.drawCircle(0f, 0f, 1f, p);
        canvas.restore();
    }

    @Override protected void onDetachedFromWindow() {
        onHomePause();
        super.onDetachedFromWindow();
    }
}
