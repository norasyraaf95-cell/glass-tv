package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
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
import android.view.animation.PathInterpolator;

import androidx.annotation.NonNull;

/**
 * V8.4.7 launcher-owned SmartTube prelaunch animation.
 *
 * Uses Choreographer + uptime, NOT ValueAnimator: the user's Android 10 TV
 * has global animator_duration_scale=0.5, which would halve a 120/180ms
 * ValueAnimator cue. These modes must represent measured real elapsed time
 * so A/B comparisons are meaningful without touching Android settings.
 *
 * 120ms/180ms visually compress the selected card, dim the rest of Home and
 * briefly trace a silver edge before handing control to Android. No blur,
 * bitmap allocations or wallpaper changes on any frame.
 */
final class PremiumLaunchCue {
    private static final String TAG = "GlassTV.V847";
    private static final float PRESSED_SCALE = .978f;
    private static final float DIM_ALPHA = .17f;
    static final long STALL_GRACE_MS = 64L;

    private final ViewGroup host;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Choreographer choreographer = Choreographer.getInstance();
    private final PathInterpolator easing = new PathInterpolator(.20f, .0f, .28f, 1f);
    private final DimScrim scrim;

    private View pressedCard;
    private Runnable pendingLaunch;
    private Runnable deadline;
    private Choreographer.FrameCallback frameCallback;
    private float savedScaleX = 1f, savedScaleY = 1f;
    private long startedAt;
    private int selectedDurationMs;
    private boolean overlayAttached;

    PremiumLaunchCue(ViewGroup host) {
        this.host = host;
        scrim = new DimScrim(host.getResources().getDisplayMetrics().density);
    }

    boolean isPending() {
        return pendingLaunch != null;
    }

    /**
     * Return false only when a previous launch is pending. On invalid UI,
     * fail safe to immediate launch rather than losing the remote OK press.
     */
    boolean play(View card, int durationMs, Runnable launch) {
        if (isPending()) return false;
        if (card == null || !card.isAttachedToWindow() || card.getWidth() <= 0
                || host.getWidth() <= 0 || host.getHeight() <= 0
                || !LaunchCuePolicy.isSupported(durationMs)
                || durationMs == LaunchCuePolicy.BASELINE_MS) {
            Log.i(TAG, "No animatable card; immediate SmartTube launch");
            launch.run();
            return true;
        }

        Rect mapped = new Rect(0, 0, card.getWidth(), card.getHeight());
        try {
            host.offsetDescendantRectToMyCoords(card, mapped);
        } catch (IllegalArgumentException error) {
            Log.w(TAG, "Card is not a Home descendant; immediate launch", error);
            launch.run();
            return true;
        }

        pressedCard = card;
        savedScaleX = card.getScaleX();
        savedScaleY = card.getScaleY();
        pendingLaunch = launch;
        startedAt = SystemClock.uptimeMillis();
        selectedDurationMs = durationMs;

        scrim.place(mapped, host.getWidth(), host.getHeight());
        scrim.setProgress(0f);
        try {
            host.getOverlay().add(scrim);
            overlayAttached = true;
        } catch (RuntimeException error) {
            Log.w(TAG, "Overlay not supported by OEM; immediate launch", error);
            cancel();
            launch.run();
            return true;
        }

        frameCallback = frameTimeNanos -> {
            if (!isPending()) return;
            long elapsed = Math.max(0L, SystemClock.uptimeMillis() - startedAt);
            float fraction = Math.min(1f, (float) elapsed / selectedDurationMs);
            float eased = easing.getInterpolation(fraction);
            if (pressedCard != null) {
                float scale = 1f - (1f - PRESSED_SCALE) * eased;
                pressedCard.setScaleX(savedScaleX * scale);
                pressedCard.setScaleY(savedScaleY * scale);
            }
            scrim.setProgress(eased);
            if (fraction >= 1f) finishCue("animation-complete");
            else choreographer.postFrameCallback(frameCallback);
        };
        deadline = () -> finishCue("bounded-fallback");
        handler.postDelayed(deadline, durationMs + STALL_GRACE_MS);
        choreographer.postFrameCallback(frameCallback);
        Log.i(TAG, "Premium SmartTube cue requested durationMs=" + durationMs
                + " timing=Choreographer");
        return true;
    }

    /** Aborting via Home, navigation or onPause must NEVER launch SmartTube. */
    void cancel() {
        pendingLaunch = null;
        clearCallbacks();
        clearAppearance();
    }

    private void finishCue(String reason) {
        if (!isPending()) return; // Frame and deadline cannot double-launch.
        Runnable action = pendingLaunch;
        pendingLaunch = null;
        clearCallbacks();
        if (pressedCard != null && pressedCard.isAttachedToWindow()) {
            Log.i(TAG, "Premium cue " + reason + " after "
                    + (SystemClock.uptimeMillis() - startedAt)
                    + "ms; requesting SmartTube launch");
            // Keep the composed frame until Home's onPause to avoid a flash.
            action.run();
        } else {
            Log.i(TAG, "Card detached before launch; stale OK abandoned");
            clearAppearance();
        }
    }

    private void clearCallbacks() {
        if (deadline != null) handler.removeCallbacks(deadline);
        deadline = null;
        if (frameCallback != null) choreographer.removeFrameCallback(frameCallback);
        frameCallback = null;
    }

    private void clearAppearance() {
        if (overlayAttached) {
            host.getOverlay().remove(scrim);
            overlayAttached = false;
        }
        if (pressedCard != null) {
            pressedCard.setScaleX(savedScaleX);
            pressedCard.setScaleY(savedScaleY);
        }
        pressedCard = null;
        scrim.setProgress(0f);
    }

    private static final class DimScrim extends Drawable {
        private final Paint dim = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint silver = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path exclusion = new Path();
        private final RectF cardBounds = new RectF();
        private final float density;
        private float cornerRadius;
        private float progress;

        DimScrim(float density) {
            this.density = density;
            silver.setStyle(Paint.Style.STROKE);
            silver.setStrokeWidth(Math.max(.8f, density * .8f));
        }

        void place(Rect card, int width, int height) {
            float pad = 3f * density;
            cardBounds.set(card.left - pad, card.top - pad,
                    card.right + pad, card.bottom + pad);
            cornerRadius = 19f * density + pad;
            exclusion.reset();
            exclusion.addRoundRect(cardBounds, cornerRadius, cornerRadius, Path.Direction.CW);
            setBounds(0, 0, width, height);
        }

        void setProgress(float value) {
            progress = Math.max(0f, Math.min(1f, value));
            invalidateSelf();
        }

        @Override public void draw(@NonNull Canvas canvas) {
            if (progress <= 0f) return;
            int save = canvas.save();
            canvas.clipOutPath(exclusion); // minSdk 26
            dim.setColor(Color.argb(Math.round(DIM_ALPHA * 255f * progress),
                    0, 0, 0));
            canvas.drawRect(getBounds(), dim);
            canvas.restoreToCount(save);

            silver.setColor(Color.argb(Math.round(.43f * 255f * progress),
                    241, 248, 255));
            canvas.drawRoundRect(cardBounds, cornerRadius, cornerRadius, silver);
        }

        @Override public void setAlpha(int alpha) {}
        @Override public void setColorFilter(ColorFilter filter) {}
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
