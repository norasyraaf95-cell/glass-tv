package com.gathin.tvhome;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;

/**
 * A tiny event-only connector in the gap between two fixed V7.8.1 dock cards.
 * Drawn through the existing grid overlay; it never shifts or redraws artwork.
 */
final class SatinLightBridge extends Drawable {
    private final ViewGroup host;
    private final Rect fromRect = new Rect(), toRect = new Rect();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
    private float startX, endX, y, progress;
    private boolean active;

    SatinLightBridge(ViewGroup host) {
        this.host = host;
        paint.setColor(0xFFF3F9FF);
        paint.setStrokeWidth(Math.max(.7f, .85f * host.getResources().getDisplayMetrics().density));
        paint.setStrokeCap(Paint.Cap.ROUND);
        animator.setDuration(240L);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            invalidateSelf();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                active = false;
                invalidateSelf();
            }
        });
        host.getOverlay().add(this);
    }

    void travel(View from, View to) {
        stop();
        if (from == null || to == null || from == to
                || from.getWidth() <= 0 || to.getWidth() <= 0) return;
        fromRect.set(0, 0, from.getWidth(), from.getHeight());
        toRect.set(0, 0, to.getWidth(), to.getHeight());
        host.offsetDescendantRectToMyCoords(from, fromRect);
        host.offsetDescendantRectToMyCoords(to, toRect);
        boolean movingRight = fromRect.centerX() < toRect.centerX();
        startX = movingRight ? fromRect.right : fromRect.left;
        endX = movingRight ? toRect.left : toRect.right;
        if (Math.abs(endX - startX) < 1f) return;
        y = toRect.top + toRect.height() * .16f;
        setBounds(0, 0, host.getWidth(), host.getHeight());
        progress = 0f;
        active = true;
        animator.setFloatValues(0f, 1f);
        animator.start();
    }

    void stop() {
        animator.cancel();
        active = false;
        invalidateSelf();
    }

    @Override public void draw(@NonNull Canvas canvas) {
        if (!active) return;
        // Retain the thin silver connector through the gap, then decay.
        // No change to static glass, pink rim, tile positions or idle state.
        float opacity = progress < .18f ? progress / .18f
                : (progress < .67f ? 1f : (1f-progress) / .33f);
        paint.setAlpha(Math.round(255f * .42f * opacity));
        canvas.drawLine(startX, y, startX+(endX-startX)*progress, y, paint);
    }
    @Override public void setAlpha(int alpha) {}
    @Override public void setColorFilter(ColorFilter filter) {}
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
