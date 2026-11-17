package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.PathInterpolator;

import androidx.annotation.NonNull;

/**
 * Frosted glass matching the custom reference. Draws the PRE-BLURRED STATIC WALLPAPER patch
 * through each card using Backdrop.Fill (already calculated off the UI thread once per wallpaper).
 * It intentionally does not attempt to live-blur every decoded movie frame on a 1GB Android 10 TV.
 *
 * Material layers: aligned wallpaper blur -> translucent smoke/purple -> diffuse white internal
 * reflection -> refractive rim -> focus-only magenta halo. Zero bitmap allocations per draw.
 */
final class PremiumGlassDrawable extends Drawable {
    private final View owner;
    private final boolean dock;
    private final float density;
    private final Backdrop.Fill softened = new Backdrop.Fill();
    private final RectF bounds = new RectF();
    private final RectF inner = new RectF();
    private final Paint dark = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint reflection = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lowerGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint focusRim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint focusWash = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix matrix = new Matrix();
    private float opacity = 1f;
    // Selected full-card brand artwork is rendered by a lightweight ImageView underneath.
    // In this mode only paint the animated focus, never another glass border on the logo image.
    private boolean artworkOnlyFocus;
    /** A reused animator fades the magenta focus between cards instead of flashing instantly. */
    private final ValueAnimator focusFade;
    private float focusAmount;

    PremiumGlassDrawable(View owner, boolean dock) {
        this.owner = owner;
        this.dock = dock;
        density = owner.getResources().getDisplayMetrics().density;
        // Reference is softly translucent lavender glass; the V4 near-black tint
        // covered the blurred wallpaper. Keep the dusk colours visible beneath.
        dark.setShader(new LinearGradient(0f, 0f, 0f, 1f,
                dock
                        ? new int[]{0x77453660, 0x8B312548, 0x9A201D35}
                        : new int[]{0x4A48335E, 0x4D35284F, 0x5C2C2247},
                new float[]{0f, .48f, 1f}, Shader.TileMode.CLAMP));
        // Wide diffuse highlight across the top of the glass, no sharp white border.
        reflection.setShader(new LinearGradient(0f, 0f, 0f, 1f,
                dock
                        ? new int[]{0x57FFF4FF, 0x19E4D8FF, 0x00FFFFFF}
                        : new int[]{0x35FFF4FF, 0x14E9DFFF, 0x00FFFFFF},
                new float[]{0f, .39f, .87f}, Shader.TileMode.CLAMP));
        lowerGlow.setShader(new LinearGradient(0f, 0f, 0f, 1f,
                dock ? new int[]{0x00000000, 0x00FFA2E4, 0x18FF7DD2}
                        : new int[]{0x00000000, 0x00FFA2E4, 0x0DFFA6E5},
                new float[]{0f, .68f, 1f}, Shader.TileMode.CLAMP));
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth((dock ? .72f : .64f) * density);
        rim.setShader(new LinearGradient(0f, 0f, 1f, 1f,
                dock ? new int[]{0xC4FFF0FF, 0x57D6C3EF, 0x38C7B0E4, 0x8CE6CFF5}
                        : new int[]{0xA8F6E8FF, 0x52E0CDEE, 0x43C5A5D7, 0x87EDCFF2},
                new float[]{0f, .28f, .73f, 1f}, Shader.TileMode.CLAMP));
        // A single anti-aliased border replaces the three concentric strokes.
        // The remaining soft halo comes from the View's colored elevation shadow.
        focusRim.setStyle(Paint.Style.STROKE);
        focusRim.setStrokeWidth(1.55f * density);
        focusRim.setShader(new LinearGradient(0f, 0f, 1f, 1f,
                new int[]{0xF3FFA8DC, 0xEFFF69C3, 0xDCF641AD, 0xF0FF9ED7},
                new float[]{0f, .28f, .72f, 1f}, Shader.TileMode.CLAMP));
        focusWash.setColor(0x20F348B5);
        focusFade = ValueAnimator.ofFloat(0f, 1f);
        focusFade.setDuration(195L);
        focusFade.setInterpolator(new PathInterpolator(.2f, .72f, .25f, 1f));
        focusFade.addUpdateListener(a -> {
            focusAmount = (float) a.getAnimatedValue();
            invalidateSelf();
        });
    }

    @Override public void draw(@NonNull Canvas canvas) {
        bounds.set(getBounds());
        if (bounds.width() <= 0f || bounds.height() <= 0f) return;
        float radius = Math.min(bounds.height() / 2f,
                (dock ? 19f : 28f) * density);
        boolean focused = dock && focusAmount > .005f;
        if (artworkOnlyFocus) {
            // V7.3: brand tiles already contain their own softly blended material.
            // Never overlay another white/pink outline on top of their corners.
            // Only a low-opacity, whole-surface tint marks remote focus.
            if (focused) {
                float inset = 1.5f * density;
                inner.set(bounds);
                inner.inset(inset, inset);
                focusWash.setAlpha(Math.round(0x22 * opacity * focusAmount));
                canvas.drawRoundRect(inner, Math.max(0f, radius - inset),
                        Math.max(0f, radius - inset), focusWash);
            }
            return;
        }

        // The native launcher already prepares a blurred wallpaper at half resolution on
        // background worker; use its exact screen-aligned texture instead of a fake flat fill.
        softened.draw(canvas, owner, bounds, radius, opacity);

        // Both shaders are unit-height, so local matrices position the gradient at this card.
        matrix.setScale(1f, bounds.height());
        matrix.postTranslate(0f, bounds.top);
        dark.getShader().setLocalMatrix(matrix);
        dark.setAlpha(Math.round(255f * opacity));
        canvas.drawRoundRect(bounds, radius, radius, dark);

        reflection.getShader().setLocalMatrix(matrix);
        reflection.setAlpha(Math.round(255f * opacity));
        canvas.drawRoundRect(bounds, radius, radius, reflection);
        lowerGlow.getShader().setLocalMatrix(matrix);
        lowerGlow.setAlpha(Math.round(255f * opacity));
        canvas.drawRoundRect(bounds, radius, radius, lowerGlow);

        inner.set(bounds);
        float inset = .6f * density;
        inner.inset(inset, inset);
        matrix.setScale(bounds.width(), bounds.height());
        matrix.postTranslate(bounds.left, bounds.top);
        rim.getShader().setLocalMatrix(matrix);
        // Fade the normal pale rim OUT as the magenta focus fades IN: no double outline.
        rim.setAlpha(Math.round(255f * opacity * (dock ? 1f - focusAmount : 1f)));
        if (!dock || focusAmount < .999f)
            canvas.drawRoundRect(inner, radius - inset, radius - inset, rim);

        if (focused) {
            // Uniform round geometry with the Android clip outline (19dp in dock).
            // Gentle interior tint adds depth without drawing extra hard-edged rings.
            float focusInset = 1.1f * density;
            inner.set(bounds);
            inner.inset(focusInset, focusInset);
            float focusRadius = Math.max(0f, radius - focusInset);
            focusWash.setAlpha(Math.round(0x20 * opacity * focusAmount));
            canvas.drawRoundRect(inner, focusRadius, focusRadius, focusWash);
            matrix.setScale(bounds.width(), bounds.height());
            matrix.postTranslate(bounds.left, bounds.top);
            focusRim.getShader().setLocalMatrix(matrix);
            focusRim.setAlpha(Math.round(255f * opacity * focusAmount));
            canvas.drawRoundRect(inner, focusRadius, focusRadius, focusRim);
        }
    }

    void setArtworkOnlyFocus(boolean enabled) {
        if (artworkOnlyFocus != enabled) {
            artworkOnlyFocus = enabled;
            invalidateSelf();
        }
    }

    /** Start from the current brightness, including when the remote is pressed rapidly. */
    void animateFocus(boolean focused) {
        if (!dock) return;
        focusFade.cancel();
        focusFade.setFloatValues(focusAmount, focused ? 1f : 0f);
        focusFade.start();
    }

    /** RecyclerView rebind must not flash a recycled tile with the previous focus glow. */
    void syncFocus(boolean focused) {
        if (!dock) return;
        focusFade.cancel();
        focusAmount = focused ? 1f : 0f;
        invalidateSelf();
    }

    void stopFocus() {
        focusFade.cancel();
    }

    @Override public void setAlpha(int alpha) {
        opacity = Math.max(0f, Math.min(1f, alpha / 255f));
        invalidateSelf();
    }
    @Override public void setColorFilter(ColorFilter ignored) {}
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
