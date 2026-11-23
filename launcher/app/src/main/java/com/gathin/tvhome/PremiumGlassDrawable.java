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
    private final Paint tintOverlay = new Paint(Paint.ANTI_ALIAS_FLAG);
    private CrystalAppearance appearance = new CrystalAppearance();
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
                        ? new int[]{0xABE5F9FC, 0xC5D2F2F8, 0xAAC5E5E8}
                        : new int[]{0xBEEFFAFF, 0xD7EAF8FC, 0xC1C6EFF7},
                new float[]{0f, .48f, 1f}, Shader.TileMode.CLAMP));
        // Wide diffuse highlight across the top of the glass, no sharp white border.
        reflection.setShader(new LinearGradient(0f, 0f, 0f, 1f,
                dock
                        ? new int[]{0xA8FFFFFF, 0x3EFFFFFF, 0x00FFFFFF}
                        : new int[]{0xB4FFFFFF, 0x55FFFFFF, 0x00FFFFFF},
                new float[]{0f, .39f, .87f}, Shader.TileMode.CLAMP));
        lowerGlow.setShader(new LinearGradient(0f, 0f, 0f, 1f,
                dock ? new int[]{0x00000000, 0x00C5F8FF, 0x34A0F5FF}
                        : new int[]{0x00000000, 0x00B5F8FF, 0x2495F4FF},
                new float[]{0f, .68f, 1f}, Shader.TileMode.CLAMP));
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth((dock ? .72f : .64f) * density);
        rim.setShader(new LinearGradient(0f, 0f, 1f, 1f,
                dock ? new int[]{0xF7FFFFFF, 0xBCE3FAFF, 0x77B9DCE8, 0xE8FFFFFF}
                        : new int[]{0xFBFFFFFF, 0xB7FFFFFF, 0x84B6D9ED, 0xF6FFFFFF},
                new float[]{0f, .28f, .73f, 1f}, Shader.TileMode.CLAMP));
        // A single anti-aliased border replaces the three concentric strokes.
        // The remaining soft halo comes from the View's colored elevation shadow.
        focusRim.setStyle(Paint.Style.STROKE);
        focusRim.setStrokeWidth(1.05f * density);
        focusRim.setShader(new LinearGradient(0f, 0f, 1f, 1f,
                new int[]{0xFBFFFFFF, 0xD3A1F9FF, 0xBF6BDAF2, 0xDEFFFFFF},
                new float[]{0f, .28f, .72f, 1f}, Shader.TileMode.CLAMP));
        focusWash.setColor(0x2969E4F5);
        focusFade = ValueAnimator.ofFloat(0f, 1f);
        focusFade.setDuration(195L);
        focusFade.setInterpolator(new PathInterpolator(.2f, .72f, .25f, 1f));
        focusFade.addUpdateListener(a -> {
            focusAmount = (float) a.getAnimatedValue();
            invalidateSelf();
        });
    }

    void setAppearance(CrystalAppearance value) {
        appearance = value.copy();
        tintOverlay.setColor(appearance.widgetTint);
        focusWash.setColor(appearance.widgetAccent);
        invalidateSelf();
    }

    @Override public void draw(@NonNull Canvas canvas) {
        bounds.set(getBounds());
        if (bounds.width() <= 0f || bounds.height() <= 0f) return;
        float radius = Math.min(bounds.height() / 2f,
                (dock ? 19f : 28f) * density);
        boolean focused = dock && focusAmount > .005f;
        if (artworkOnlyFocus) {
            // V7.5 Option 1: the five static WebP tiles include their own transparent
            // glass surfaces. The only runtime change is a restrained, one-stroke
            // focus highlight, drawn INSIDE the rounded outline. No view shadow,
            // no realtime blur, no external glow or extra bitmap allocations.
            if (focused) {
                float inset = 1.2f * density;
                inner.set(bounds);
                inner.inset(inset, inset);
                float innerRadius = Math.max(0f, radius - inset);
                focusWash.setAlpha(Math.round(0x17 * opacity * focusAmount));
                canvas.drawRoundRect(inner, innerRadius, innerRadius, focusWash);
                matrix.setScale(bounds.width(), bounds.height());
                matrix.postTranslate(bounds.left, bounds.top);
                focusRim.getShader().setLocalMatrix(matrix);
                focusRim.setAlpha(Math.round(205f * opacity * focusAmount));
                canvas.drawRoundRect(inner, innerRadius, innerRadius, focusRim);
            }
            return;
        }

        // The native launcher already prepares a blurred wallpaper at half resolution on
        // background worker; use its exact screen-aligned texture instead of a fake flat fill.
        float glassOpacity = dock ? opacity : opacity * Math.min(1f, appearance.widgetOpacity / 72f);
        softened.draw(canvas, owner, bounds, radius, glassOpacity);

        // Both shaders are unit-height, so local matrices position the gradient at this card.
        matrix.setScale(1f, bounds.height());
        matrix.postTranslate(0f, bounds.top);
        dark.getShader().setLocalMatrix(matrix);
        dark.setAlpha(Math.round(255f * glassOpacity));
        canvas.drawRoundRect(bounds, radius, radius, dark);

        reflection.getShader().setLocalMatrix(matrix);
        reflection.setAlpha(Math.round(255f * glassOpacity));
        canvas.drawRoundRect(bounds, radius, radius, reflection);
        lowerGlow.getShader().setLocalMatrix(matrix);
        lowerGlow.setAlpha(Math.round(255f * glassOpacity));
        canvas.drawRoundRect(bounds, radius, radius, lowerGlow);

        inner.set(bounds);
        float inset = .6f * density;
        inner.inset(inset, inset);
        matrix.setScale(bounds.width(), bounds.height());
        matrix.postTranslate(bounds.left, bounds.top);
        rim.getShader().setLocalMatrix(matrix);
        tintOverlay.setAlpha(Math.round(27f * glassOpacity));
        canvas.drawRoundRect(bounds,radius,radius,tintOverlay);
        // Fade the normal pale rim OUT as the magenta focus fades IN: no double outline.
        rim.setAlpha(Math.round(255f * glassOpacity * (dock ? 1f - focusAmount : 1f)));
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
