package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Choreographer;
import android.view.View;
import android.view.ViewGroup;

/**
 * V8.4.7 — a visible, launcher-owned SmartTube OK cue BEFORE the task switch.
 *
 * FrameCueHandoff (V8.4.6) submitted only one 64ms frame, so the 180ms click
 * animation could be interrupted by onPause before it was actually perceived.
 * This keeps Home in control for a short, explicitly chosen 120/180ms sequence,
 * then launches the third-party app without relying on OEM cross-task fades.
 *
 * Choreographer time is independent of Android's animator-duration scale (the
 * owner's TV currently uses 0.5x). A watchdog guarantees that a stalled frame
 * loop can never prevent SmartTube from opening.
 *
 * Nothing animates when idle; source artwork and the original pink focus rim
 * are not modified. This overlay paints only a low-alpha surround and silver
 * outline around the already selected dock card.
 */
final class VisibleLaunchCue {
    private static final String TAG = "GlassTV.V847";
    static final long WATCHDOG_MARGIN_MS = 72L;
    private final ViewGroup host;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Choreographer choreographer = Choreographer.getInstance();
    private final CueDrawable overlay = new CueDrawable();
    private final Rect cardRect = new Rect();
    private final RectF selected = new RectF();
    private Runnable pendingAction;
    private Runnable watchdog;
    private Runnable settle;
    private Choreographer.FrameCallback frame;
    private long startUptimeMs;
    private int durationMs;
    private boolean showing;

    VisibleLaunchCue(ViewGroup host) {
        this.host = host;
        host.getOverlay().add(overlay);
    }

    boolean isPending() {
        return pendingAction != null;
    }

    /** Returns false only when a previous launch is still pending. */
    boolean play(View card, int requestedMs, Runnable launch) {
        if (isPending()) return false;
        // If the TV is still laying out Home, never block the app launch.
        if (card == null || !card.isAttachedToWindow() || card.getWidth() <= 0
                || card.getHeight() <= 0 || host.getWidth() <= 0 || host.getHeight() <= 0) {
            Log.i(TAG, "No measured cue view; launch immediately");
            launch.run();
            return true;
        }
        clear();
        cardRect.set(0, 0, card.getWidth(), card.getHeight());
        host.offsetDescendantRectToMyCoords(card, cardRect);
        selected.set(cardRect);
        final float d = host.getResources().getDisplayMetrics().density;
        selected.inset(-1.5f * d, -1.5f * d);
        durationMs = LaunchCuePolicy.durationFor(requestedMs);
        startUptimeMs = SystemClock.uptimeMillis();
        pendingAction = launch;
        showing = true;
        overlay.setBounds(0, 0, host.getWidth(), host.getHeight());
        overlay.setPhase(0.05f);
        frame = frameTimeNanos -> {
            if (!isPending()) return;
            float elapsed = SystemClock.uptimeMillis() - startUptimeMs;
            float phase = Math.min(1f, Math.max(0f, elapsed / durationMs));
            overlay.setPhase(phase);
            if (phase >= 1f) finish("cue-complete");
            else choreographer.postFrameCallback(frame);
        };
        watchdog = () -> finish("watchdog");
        // The first draw is requested synchronously; ensuing ticks follow vsync.
        choreographer.postFrameCallback(frame);
        handler.postDelayed(watchdog, durationMs + WATCHDOG_MARGIN_MS);
        Log.i(TAG, "Visible cue started durationMs=" + durationMs);
        return true;
    }

    /** Called from MainActivity.onPause (via AppAdapter). */
    void cancel() {
        clear();
    }

    private void finish(String reason) {
        if (!isPending()) return; // A frame and watchdog may finish together.
        Runnable action = pendingAction;
        pendingAction = null;
        stopCallbacks();
        Log.i(TAG, "Visible cue handoff=" + reason + " elapsedMs="
                + (SystemClock.uptimeMillis() - startUptimeMs));
        // Keep the completed cue on Home during the Activity handoff, until
        // onPause clears it. If the target fails to open, clear after a short
        // safety window. Never delay startActivity for this cleanup.
        settle = this::clear;
        handler.postDelayed(settle, 450L);
        action.run();
    }

    private void stopCallbacks() {
        if (frame != null) choreographer.removeFrameCallback(frame);
        if (watchdog != null) handler.removeCallbacks(watchdog);
        frame = null;
        watchdog = null;
    }

    private void clear() {
        pendingAction = null;
        stopCallbacks();
        if (settle != null) handler.removeCallbacks(settle);
        settle = null;
        showing = false;
        overlay.setPhase(0f);
    }

    private final class CueDrawable extends Drawable {
        private final Paint dim = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float phase;

        CueDrawable() {
            dim.setColor(0xFF000000);
            rim.setColor(0xFFF0F8FF);
            rim.setStyle(Paint.Style.STROKE);
            rim.setStrokeWidth(Math.max(.8f, host.getResources().getDisplayMetrics().density));
        }

        void setPhase(float value) {
            phase = Math.max(0f, Math.min(1f, value));
            invalidateSelf();
        }

        @Override public void draw(Canvas canvas) {
            if (!showing || phase <= 0f || selected.isEmpty()) return;
            Rect b = getBounds();
            // Fast onset, then hold the selected-card emphasis until handoff.
            float emphasis = Math.min(1f, phase * 3.5f);
            // Four rectangles avoid expensive clipPath / offscreen layers.
            dim.setAlpha(Math.round(255f * .12f * emphasis));
            canvas.drawRect(b.left, b.top, b.right, selected.top, dim);
            canvas.drawRect(b.left, selected.top, selected.left, selected.bottom, dim);
            canvas.drawRect(selected.right, selected.top, b.right, selected.bottom, dim);
            canvas.drawRect(b.left, selected.bottom, b.right, b.bottom, dim);
            float shimmer = (float) Math.sin(Math.PI * Math.min(1f, phase));
            rim.setAlpha(Math.round(255f * (.16f + .32f * shimmer) * emphasis));
            float radius = Math.min(selected.height() * .5f, 19f * host.getResources().getDisplayMetrics().density);
            canvas.drawRoundRect(selected, radius, radius, rim);
        }
        @Override public void setAlpha(int alpha) {}
        @Override public void setColorFilter(ColorFilter filter) {}
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
