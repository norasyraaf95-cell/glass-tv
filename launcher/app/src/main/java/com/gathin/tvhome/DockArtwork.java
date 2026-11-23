package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;

/**
 * Five source-extracted, user-approved V7.8 Liquid Crystal cards.
 * This is ONE bitmap decode per atlas (not one bitmap per frame). No shader
 * generation, no bitmap allocations during the DPAD transition.
 */
final class DockArtwork {
    private static DockArtwork instance;
    final Bitmap[] idle = new Bitmap[5];
    final Bitmap[] focus = new Bitmap[5];
    final Bitmap focusHalo;
    final Bitmap reflections;
    final Bitmap[] activeReflections = new Bitmap[5];
    final boolean valid;

    private DockArtwork(Context context) {
        // Crystal Clear uses native wallpaper-aligned glass, not old purple V7.8 WebP art.
        // This also protects the preview if a user later copies their backup assets into it.
        if ((context.getPackageName().startsWith("com.gathin.tvhome.crystalclear")
                || context.getPackageName().startsWith("com.gathin.tvhome.crystalcontrol"))) {
            focusHalo = null;
            reflections = null;
            valid = false;
            return;
        }
        File dir = context.getExternalFilesDir(null);
        Bitmap idleAtlas = decode(dir, "liquid_dock_base.webp");
        Bitmap focusAtlas = decode(dir, "liquid_focus_illumination.webp");
        Bitmap activeReflectionAtlas = decode(dir, "liquid_focus_reflection.webp");
        focusHalo = decode(dir, "liquid_focus_glow.webp");
        reflections = decode(dir, "liquid_static_reflections.webp");
        boolean all = idleAtlas != null && focusAtlas != null
                && focusHalo != null && reflections != null && activeReflectionAtlas != null;
        if (all && idleAtlas.getWidth() == 1500 && idleAtlas.getHeight() == 156
                && focusAtlas.getWidth() == 1500 && focusAtlas.getHeight() == 156
                && activeReflectionAtlas.getWidth() == 1500 && activeReflectionAtlas.getHeight() == 94) {
            for (int i = 0; i < 5; i++) {
                idle[i] = Bitmap.createBitmap(idleAtlas, i * 300, 0, 300, 156);
                focus[i] = Bitmap.createBitmap(focusAtlas, i * 300, 0, 300, 156);
                activeReflections[i] = Bitmap.createBitmap(activeReflectionAtlas, i * 300, 0, 300, 94);
            }
            valid = true;
        } else {
            valid = false;
            Log.w("GlassTV.V78", "Reference artwork missing; copy asset ZIP via Bugjaeger and restart");
        }
    }

    private static Bitmap decode(File directory, String filename) {
        if (directory == null) return null;
        File file = new File(directory, filename);
        if (!file.isFile() || file.length() == 0) return null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    }

    static synchronized DockArtwork get(Context context) {
        if (instance == null) instance = new DockArtwork(context.getApplicationContext());
        return instance;
    }

    static synchronized void reset() { instance = null; }
}
