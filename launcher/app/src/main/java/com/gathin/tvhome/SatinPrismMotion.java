package com.gathin.tvhome;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.animation.PathInterpolator;

import androidx.annotation.NonNull;

/**
 * V8.4 EXPERIMENTAL: foreground-only micro-optics for the five reference dock cards.
 *
 * Never modifies or replaces V7.8.1 bitmap artwork, icons, tile geometry, wallpaper,
 * static glass or original focus pink rim. No allocations/decodes/blur in draw().
 * One animator per visible dock holder, strictly event-driven (zero idle frames).
 */
final class SatinPrismMotion extends Drawable {
    private static final int NONE = 0, SWEEP = 1, CLICK = 2, RETURN = 3;
    private static final float REFLECTION_INTENSITY = .55f;

    private final Paint optical = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint micro = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shade = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final LinearGradient opticalGradient = new LinearGradient(
            0f, 0f, 1f, 0f,
            new int[]{0x00F7FCFF, 0x33F7FCFF, 0xB3F8FCFF, 0x22EFF7FF, 0x00EFF7FF},
            new float[]{0f, .2f, .44f, .72f, 1f}, Shader.TileMode.CLAMP);
    private final LinearGradient microGradient = new LinearGradient(
            0f, 0f, 1f, 0f,
            new int[]{0x00EDF8FF, 0x43E5F4FF, 0x00EDF8FF},
            null, Shader.TileMode.CLAMP);
    private final Matrix matrix = new Matrix();
    private final Path clip = new Path();
    private final ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
    private final float density;
    private int mode = NONE;
    private int direction = 1;
    private float phase;
    private int globalAlpha = 255;

    SatinPrismMotion(float density) {
        this.density = density;
        optical.setShader(opticalGradient);
        micro.setShader(microGradient);
        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeWidth(Math.max(.7f, .65f * density));
        edge.setColor(0xFFDDEFFF);
        shade.setColor(0xFF130D1D);
        animator.setInterpolator(new PathInterpolator(.19f, .68f, .31f, 1f));
        animator.addUpdateListener(a -> {
            phase = (float) a.getAnimatedValue();
            invalidateSelf();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                mode = NONE;
                phase = 0f;
                invalidateSelf();
            }
        });
    }

    @Override protected void onBoundsChange(Rect bounds) {
        clip.reset();
        if (!bounds.isEmpty()) {
            float radius = Math.min(bounds.height() / 2f, 19f * density);
            clip.addRoundRect(bounds.left, bounds.top, bounds.right, bounds.bottom,
                    radius, radius, Path.Direction.CW);
        }
    }

    void playSweep(int direction) {
        start(SWEEP, direction, 360L);
    }

    void playClick() {
        start(CLICK, direction, 140L);
    }

    void playSoftReturn() {
        start(RETURN, direction, 160L);
    }

    void stop() {
        animator.cancel();
        mode = NONE;
        phase = 0f;
        invalidateSelf();
    }

    private void start(int nextMode, int nextDirection, long ms) {
        animator.cancel(); // Cancels stale effects on repeated remote presses.
        direction = nextDirection < 0 ? -1 : 1;
        mode = nextMode;
        phase = 0f;
        animator.setFloatValues(0f, 1f);
        animator.setDuration(ms);
        animator.start();
    }

    @Override public void draw(@NonNull Canvas canvas) {
        if (mode == NONE || globalAlpha == 0) return;
        Rect b = getBounds();
        if (b.isEmpty()) return;
        float w = b.width(), h = b.height();
        float r = Math.min(h / 2f, 19f * density);
        float fade = (1f - phase) * (1f - phase);
        int save = canvas.save();
        canvas.clipPath(clip);
        if (mode == SWEEP) {
            // Delayed optical decay, subtly direction-aware; no shifting artwork.
            float position = direction > 0 ? (-.18f + 1.36f * phase)
                    : (1.18f - 1.36f * phase);
            float center = b.left + position * w;
            float strip = w * .22f;
            matrix.setScale(strip * 2f, 1f);
            matrix.postTranslate(center - strip, 0f);
            opticalGradient.setLocalMatrix(matrix);
            optical.setAlpha(Math.round(255f * REFLECTION_INTENSITY
                    * Math.min(1f, phase * 6f) * fade * globalAlpha / 255f));
            canvas.drawRect(center - strip, b.top, center + strip, b.bottom, optical);

            // Optical micro-refraction: low-alpha painted strip, not live blur.
            matrix.setScale(strip * 1.5f, 1f);
            matrix.postTranslate(center - strip * .75f, 0f);
            microGradient.setLocalMatrix(matrix);
            micro.setAlpha(Math.round(255f * .23f * fade * globalAlpha / 255f));
            canvas.drawRect(center - strip * .75f, b.top + .08f*h,
                    center + strip * .75f, b.bottom - .08f*h, micro);

            // Fresnel/Crystal edge trace: short top glint; never a second focus rim.
            edge.setAlpha(Math.round(255f * .36f * Math.min(1f,phase*5f)
                    * fade * globalAlpha / 255f));
            canvas.drawLine(b.left + r, b.top + density,
                    b.right - r, b.top + density, edge);
        } else if (mode == CLICK) {
            // Instant tactile acknowledgement; launch remains synchronous.
            shade.setAlpha(Math.round(255f * .08f * (float)Math.sin(Math.PI*phase)
                    * globalAlpha / 255f));
            canvas.drawRoundRect(b.left,b.top,b.right,b.bottom,r,r,shade);
        } else if (mode == RETURN) {
            // Single quiet optical settle. Never replays directional sweep on Back.
            edge.setAlpha(Math.round(255f * .23f * (1f-phase)
                    * globalAlpha / 255f));
            canvas.drawLine(b.left+r,b.top+density,b.right-r,b.top+density,edge);
        }
        canvas.restoreToCount(save);
    }

    @Override public void setAlpha(int alpha) {
        globalAlpha = Math.max(0, Math.min(255, alpha));
        invalidateSelf();
    }
    @Override public void setColorFilter(ColorFilter filter) {}
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
};
