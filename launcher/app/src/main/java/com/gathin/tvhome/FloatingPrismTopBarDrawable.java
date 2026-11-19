package com.gathin.tvhome;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;

/**
 * External skin ONLY for the Prayer Times and Weather capsule backgrounds.
 * All text, timings, icons, control behaviours, positions, and dock layers
 * are existing native Android views. This skin can be replaced via Bugjaeger
 * without rebuilding this APK on the next design iteration.
 *
 * The supplied files are transparent WEBP textures at exactly 3x the original
 * XML dp sizes; Android scales them to their unchanged widget bounds.
 * No live blurring, moving rim, View shadow, GPU animation or screen tint.
 */
final class FloatingPrismTopBarDrawable extends Drawable {
    static final String PRAYER = "floating_prism_prayer.webp";
    static final String WEATHER = "floating_prism_weather.webp";
    private final Bitmap skin;
    private final Paint texture = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private int alpha = 255;

    private FloatingPrismTopBarDrawable(Bitmap bitmap) {
        skin = bitmap;
        texture.setDither(true);
    }

    static Drawable fromFiles(File directory, String filename) {
        if (directory == null || !(PRAYER.equals(filename) || WEATHER.equals(filename))) return null;
        File file = new File(directory, filename);
        if (!file.isFile() || file.length() < 100) return null;
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inScaled = false;
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
            if (bmp == null || !bmp.hasAlpha()
                    || bmp.getWidth() < 300 || bmp.getHeight() < 80) {
                if (bmp != null) bmp.recycle();
                Log.w("GlassTV.TopBar", "Invalid Floating Prism skin: " + filename);
                return null;
            }
            return new FloatingPrismTopBarDrawable(bmp);
        } catch (Exception ex) {
            Log.w("GlassTV.TopBar", "Cannot decode Floating Prism skin: " + filename, ex);
            return null;
        }
    }

    @Override public void draw(@NonNull Canvas canvas) {
        Rect dst = getBounds();
        if (dst.isEmpty()) return;
        texture.setAlpha(alpha);
        canvas.drawBitmap(skin, null, dst, texture);
    }

    @Override public void setAlpha(int value) {
        alpha = Math.max(0, Math.min(255, value));
        invalidateSelf();
    }

    @Override public void setColorFilter(ColorFilter filter) {
        texture.setColorFilter(filter);
        invalidateSelf();
    }

    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }

    @Override public int getIntrinsicWidth() { return -1; }
    @Override public int getIntrinsicHeight() { return -1; }
}
