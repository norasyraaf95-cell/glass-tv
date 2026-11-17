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
    private final Paint lowerGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint focusOuter = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint focusMid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint focusInner = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix matrix = new Matrix();
    private float opacity = 1f;

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
        focusOuter.setStyle(Paint.Style.STROKE);
        focusMid.setStyle(Paint.Style.STROKE);
        focusInner.setStyle(Paint.Style.STROKE);
        focusOuter.setStrokeWidth(9.5f * density);
        focusMid.setStrokeWidth(5f * density);
        focusInner.setStrokeWidth(1.4f * density);
        focusOuter.setColor(0x25FF3DBB);
        focusMid.setColor(0x66F93DAE);
        focusInner.setColor(0xE8FF9BD9);
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
        lowerGlow.getShader().setLocalMatrix(matrix);
        lowerGlow.setAlpha(Math.round(255f * opacity));
        canvas.drawRoundRect(bounds, radius, radius, lowerGlow);

        inner.set(bounds);
        float inset = .6f * density;
        inner.inset(inset, inset);
        matrix.setScale(bounds.width(), bounds.height());
        matrix.postTranslate(bounds.left, bounds.top);
        rim.getShader().setLocalMatrix(matrix);
        rim.setAlpha(Math.round(255f * opacity));
        canvas.drawRoundRect(inner, radius - inset, radius - inset, rim);

        if (focused) {
            // Soft luminous halo in layers; light should bloom gently, not form a rigid box.
            drawHalo(canvas, focusOuter, 4f, radius, 0x25);
            drawHalo(canvas, focusMid, 2.6f, radius, 0x66);
            drawHalo(canvas, focusInner, .8f, radius, 0xE8);
        }
    }

    // Reuses one rectangle and three Paints; no per-focus-frame array allocations.
    private void drawHalo(Canvas canvas, Paint paint, float insetDp, float radius, int alpha) {
        float inset = insetDp * density;
        inner.set(bounds);
        inner.inset(inset, inset);
        paint.setAlpha(Math.round(alpha * opacity));
        canvas.drawRoundRect(inner, radius - inset, radius - inset, paint);
    }

    @Override public void setAlpha(int alpha) {
        opacity = Math.max(0f, Math.min(1f, alpha / 255f));
        invalidateSelf();
    }
    @Override public void setColorFilter(ColorFilter ignored) {}
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
