package com.gathin.tvhome;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;

import androidx.annotation.NonNull;

/**
 * V8.4.7 controlled, launcher-owned prelaunch cue.
 *
 * One short event-driven animator: gentle compression of the selected card,
 * a low-alpha darkening of everything EXCEPT that card, and a narrow silver
 * outline. No wallpaper blur, atlas edits, texture decoding or idle frames.
 *
 * Only SmartTube on the fixed dock uses this during the A/B experiment.
 * A 120/180ms launch delay is intentional and explicitly testable; it is NOT
 * a guarantee of cross-application animation once Android starts SmartTube.
 */
final class PremiumLaunchCue {
    private static final String TAG = "GlassTV.V847";
    private static final float PRESSED_SCALE = .978f;
    private static final float DIM_ALPHA = .17f;

    private final ViewGroup host;
    private final float density;
    private final DimScrim scrim;

    private View pressedCard;
    private ValueAnimator animator;
    private Runnable pendingLaunch;
    private float savedScaleX = 1f, savedScaleY = 1f;
    private long startedAt;
    private boolean overlayAttached;

    PremiumLaunchCue(ViewGroup host) {
        this.host = host;
        density = host.getResources().getDisplayMetrics().density;
        scrim = new DimScrim(density);
    }

    boolean isPending() {
        return pendingLaunch != null;
    }

    /**
     * Launches normally if the UI cannot be animated. Returns false only when
     * another handoff is already pending, to suppress duplicate OK events.
     */
    boolean play(View card, int durationMs, Runnable launch) {
        if (isPending()) return false;
        if (card == null || !card.isAttachedToWindow() || card.getWidth() <= 0
                || host.getWidth() <= 0 || host.getHeight() <= 0
                || !LaunchCuePolicy.isSupported(durationMs)
                || durationMs == LaunchCuePolicy.BASELINE_MS) {
            Log.i(TAG, "Animated cue unavailable; launching immediately");
            launch.run();
            return true;
        }

        final Rect mapped = new Rect(0, 0, card.getWidth(), card.getHeight());
        try {
            host.offsetDescendantRectToMyCoords(card, mapped);
        } catch (IllegalArgumentException ex) {
            Log.w(TAG, "Launch card is not in Home root; launching immediately", ex);
            launch.run();
            return true;
        }

        pressedCard = card;
        savedScaleX = card.getScaleX();
        savedScaleY = card.getScaleY();
        pendingLaunch = launch;
        startedAt = SystemClock.uptimeMillis();

        scrim.place(mapped, host.getWidth(), host.getHeight());
        scrim.setProgress(0f);
        host.getOverlay().add(scrim);
        overlayAttached = true;

        final ValueAnimator running = ValueAnimator.ofFloat(0f, 1f);
        animator = running;
        running.setDuration(durationMs);
        running.setInterpolator(new PathInterpolator(.20f, .0f, .28f, 1f));
        running.addUpdateListener(a -> {
            if (animator != running) return;
            float p = (float) a.getAnimatedValue();
            // Only the selected card is compressed; logos and artwork stay intact.
            if (pressedCard != null) {
                pressedCard.setScaleX(savedScaleX * (1f - (1f - PRESSED_SCALE) * p));
                pressedCard.setScaleY(savedScaleY * (1f - (1f - PRESSED_SCALE) * p));
            }
            scrim.setProgress(p);
        });
        running.addListener(new AnimatorListenerAdapter() {
            private boolean wasCancelled;

            @Override public void onAnimationCancel(Animator animation) {
                wasCancelled = true;
            }

            @Override public void onAnimationEnd(Animator animation) {
                if (wasCancelled || animator != running || pendingLaunch != launch) return;
                // The cue is now fully composed. Leave the last appearance in
                // place until Home pauses; Android controls the following frames.
                pendingLaunch = null;
                animator = null;
                if (card.isAttachedToWindow()) {
                    Log.i(TAG, "Cue rendered for " + (SystemClock.uptimeMillis() - startedAt)
                            + "ms; requesting SmartTube start");
                    launch.run();
                } else {
                    clearAppearance();
                    Log.i(TAG, "Card detached before launch; handoff cancelled");
                }
            }
        });
        Log.i(TAG, "Premium SmartTube prelaunch cue " + durationMs + "ms");
        running.start();
        return true;
    }

    /**
     * Called when Home pauses, user presses HOME again, or the selected
     * card loses focus. Cancelling must never call a deferred startActivity.
     */
    void cancel() {
        pendingLaunch = null;
        ValueAnimator old = animator;
        animator = null;
        if (old != null) old.cancel();
        clearAppearance();
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
            cardBounds.set(card.left - pad, card.top - pad, card.right + pad, card.bottom + pad);
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
            // API 26+ (app minSdk 26). The focused card remains legible.
            int save = canvas.save();
            canvas.clipOutPath(exclusion);
            dim.setColor(Color.argb(Math.round(DIM_ALPHA * 255f * progress), 0, 0, 0));
            canvas.drawRect(getBounds(), dim);
            canvas.restoreToCount(save);

            // A restrained single silver edge, not a neon or expanded halo.
            silver.setColor(Color.argb(Math.round(0.43f * 255f * progress),
                    241, 248, 255));
            canvas.drawRoundRect(cardBounds, cornerRadius, cornerRadius, silver);
        }

        @Override public void setAlpha(int alpha) {}
        @Override public void setColorFilter(ColorFilter colorFilter) {}
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
