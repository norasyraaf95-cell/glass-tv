package com.gathin.tvhome;

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
    private final Paint focusOuter = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint focusMid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint focusInner = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix matrix = new Matrix();
    private float opacity = 1f;

    PremiumGlassDrawable(View owner, boolean dock) {
        this.owner = owner;
        this.dock = dock;
        density = owner.getResources().getDisplayMetrics().density;
        // Interior darkness separates the panel from the busy purple sky.
        dark.setShader(new LinearGradient(0f, 0f, 0f, 1f,
                dock
                        ? new int[]{0xB62A213C, 0xC6211930, 0xD31A1829}
                        : new int[]{0x7B9D759F, 0x755B4A78, 0x86714582},
                new float[]{0f, .42f, 1f}, Shader.TileMode.CLAMP));
        // A diffused top-facing reflection, rather than a cheap uniformly bright outline.
        reflection.setShader(new LinearGradient(0f, 0f, 0f, 1f,
                dock
                        ? new int[]{0x3FFFFFFF, 0x10FFFFFF, 0x00FFFFFF}
                        : new int[]{0x46FFFFFF, 0x16FFFFFF, 0x00FFFFFF},
                new float[]{0f, .30f, .80f}, Shader.TileMode.CLAMP));
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(.95f * density);
        rim.setShader(new LinearGradient(0f, 0f, 1f, 1f,
                new int[]{0xDAFFEDFF, 0x81D6C5EB, 0x4BC5A8E0, 0xBFE7D3F4},
                new float[]{0f, .25f, .74f, 1f}, Shader.TileMode.CLAMP));
        focusOuter.setStyle(Paint.Style.STROKE);
        focusMid.setStyle(Paint.Style.STROKE);
        focusInner.setStyle(Paint.Style.STROKE);
        focusOuter.setStrokeWidth(6.2f * density);
        focusMid.setStrokeWidth(3.7f * density);
        focusInner.setStrokeWidth(1.7f * density);
        focusOuter.setColor(0x3CFF3DBB);
        focusMid.setColor(0x88FA37B8);
        focusInner.setColor(0xFFFF9BDE);
    }

    @Override public void draw(@NonNull Canvas canvas) {
        bounds.set(getBounds());
        if (bounds.width() <= 0f || bounds.height() <= 0f) return;
        float radius = dock ? 19f * density : Math.min(bounds.height() / 2f, 28f * density);
        boolean focused = dock && owner.isFocused();

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

        inner.set(bounds);
        float inset = .6f * density;
        inner.inset(inset, inset);
        matrix.setScale(bounds.width(), bounds.height());
        matrix.postTranslate(bounds.left, bounds.top);
        rim.getShader().setLocalMatrix(matrix);
        rim.setAlpha(Math.round(255f * opacity));
        canvas.drawRoundRect(inner, radius - inset, radius - inset, rim);

        if (focused) {
            // Controlled neon focus: three low-cost concentric passes, no per-frame blur/shadow.
            float[] widths = {3.5f, 2.3f, .85f};
            Paint[] paints = {focusOuter, focusMid, focusInner};
            for (int i = 0; i < paints.length; i++) {
                inner.set(bounds);
                inner.inset(widths[i] * density, widths[i] * density);
                paints[i].setAlpha(Math.round(
                        (i == 0 ? 0x3C : i == 1 ? 0x88 : 0xFF) * opacity));
                canvas.drawRoundRect(inner, radius - widths[i] * density,
                        radius - widths[i] * density, paints[i]);
            }
        }
    }

    @Override public void setAlpha(int alpha) {
        opacity = Math.max(0f, Math.min(1f, alpha / 255f));
        invalidateSelf();
    }
    @Override public void setColorFilter(ColorFilter ignored) {}
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
