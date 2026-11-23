package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/** User-controlled appearance. A copy is edited live; only Save persists. */
final class CrystalAppearance {
    static final String STORE = "crystal_control_center_v1";
    static final String LOCKED = "magnetic";
    static final String BASE_WALLPAPER = "wallpaper_original.jpg";

    int widgetTint = 0xFFE1F7FF;
    int widgetText = 0xFF10344D;
    int widgetAccent = 0xFF75EAF9;
    int widgetOpacity = 72;
    int widgetHighlight = 65;
    int dockTint = 0xFFCAFAFF;
    int dockOpacity = 58;
    int dockBorder = 84;
    int dockReflection = 75;
    int spotlightTint = 0xFF9FF7FF;
    int spotlightIntensity = 75;
    int spotlightSoftness = 65;
    String motion = LOCKED;
    int duration = 470;
    String easing = "smooth";
    boolean adaptive = false;
    int adaptSensitivity = 75;
    String adaptStyle = "balanced";
    String wallpaperFit = "crop";
    int wallpaperBrightness = 100;
    int wallpaperSaturation = 100;
    int wallpaperBlur = 0;

    CrystalAppearance copy() {
        CrystalAppearance s = new CrystalAppearance();
        s.widgetTint = widgetTint; s.widgetText = widgetText; s.widgetAccent = widgetAccent;
        s.widgetOpacity = widgetOpacity; s.widgetHighlight = widgetHighlight;
        s.dockTint = dockTint; s.dockOpacity = dockOpacity; s.dockBorder = dockBorder;
        s.dockReflection = dockReflection; s.spotlightTint = spotlightTint;
        s.spotlightIntensity = spotlightIntensity; s.spotlightSoftness = spotlightSoftness;
        s.motion = motion; s.duration = duration; s.easing = easing;
        s.adaptive = adaptive; s.adaptSensitivity = adaptSensitivity; s.adaptStyle = adaptStyle;
        s.wallpaperFit = wallpaperFit; s.wallpaperBrightness = wallpaperBrightness;
        s.wallpaperSaturation = wallpaperSaturation; s.wallpaperBlur = wallpaperBlur;
        return s;
    }

    static CrystalAppearance load(Context c) {
        String s = c.getSharedPreferences(STORE, Context.MODE_PRIVATE).getString("appearance", null);
        if (s == null) return new CrystalAppearance();
        try { return fromJson(new JSONObject(s)); }
        catch (Exception ex) { return new CrystalAppearance(); }
    }

    static CrystalAppearance fromJson(JSONObject j) {
        CrystalAppearance s = new CrystalAppearance();
        s.widgetTint = color(j, "widgetTint", s.widgetTint);
        s.widgetText = color(j, "widgetText", s.widgetText);
        s.widgetAccent = color(j, "widgetAccent", s.widgetAccent);
        s.widgetOpacity = percent(j, "widgetOpacity", s.widgetOpacity);
        s.widgetHighlight = percent(j, "widgetHighlight", s.widgetHighlight);
        s.dockTint = color(j, "dockTint", s.dockTint);
        s.dockOpacity = percent(j, "dockOpacity", s.dockOpacity);
        s.dockBorder = percent(j, "dockBorder", s.dockBorder);
        s.dockReflection = percent(j, "dockReflection", s.dockReflection);
        s.spotlightTint = color(j, "spotlightTint", s.spotlightTint);
        s.spotlightIntensity = percent(j, "spotlightIntensity", s.spotlightIntensity);
        s.spotlightSoftness = percent(j, "spotlightSoftness", s.spotlightSoftness);
        s.motion = choice(j.optString("motion", LOCKED), "magnetic", "bloom", "satin", "instant");
        s.duration = Math.max(150, Math.min(800, j.optInt("duration", 470)));
        s.easing = choice(j.optString("easing", "smooth"), "smooth", "soft", "linear");
        s.adaptive = j.optBoolean("adaptive", false);
        s.adaptSensitivity = percent(j, "adaptSensitivity", s.adaptSensitivity);
        s.adaptStyle = choice(j.optString("adaptStyle", "balanced"), "soft", "balanced", "strong");
        s.wallpaperFit = choice(j.optString("wallpaperFit", "crop"), "crop", "fit");
        s.wallpaperBrightness = Math.max(35, Math.min(150, j.optInt("wallpaperBrightness", 100)));
        s.wallpaperSaturation = Math.max(0, Math.min(180, j.optInt("wallpaperSaturation", 100)));
        s.wallpaperBlur = Math.max(0, Math.min(20, j.optInt("wallpaperBlur", 0)));
        return s;
    }

    private static String choice(String v, String def, String... choices) {
        for (String c : choices) if (c.equals(v)) return c;
        return def;
    }
    private static int percent(JSONObject j, String k, int fallback) {
        return Math.max(0, Math.min(100, j.optInt(k, fallback)));
    }
    private static int color(JSONObject j, String k, int fallback) {
        try { return Color.parseColor(j.optString(k)); }
        catch (Exception ex) { return fallback; }
    }
    private static String hex(int argb) { return String.format(java.util.Locale.US, "#%08X", argb); }

    JSONObject toJson() {
        JSONObject j = new JSONObject();
        try {
            j.put("schema", 1);
            j.put("widgetTint", hex(widgetTint)); j.put("widgetText", hex(widgetText));
            j.put("widgetAccent", hex(widgetAccent)); j.put("widgetOpacity", widgetOpacity);
            j.put("widgetHighlight", widgetHighlight);
            j.put("dockTint", hex(dockTint)); j.put("dockOpacity", dockOpacity);
            j.put("dockBorder", dockBorder); j.put("dockReflection", dockReflection);
            j.put("spotlightTint", hex(spotlightTint));
            j.put("spotlightIntensity", spotlightIntensity);
            j.put("spotlightSoftness", spotlightSoftness);
            j.put("motion", motion); j.put("duration", duration); j.put("easing", easing);
            j.put("adaptive", adaptive); j.put("adaptSensitivity", adaptSensitivity);
            j.put("adaptStyle", adaptStyle);
            j.put("wallpaperFit", wallpaperFit); j.put("wallpaperBrightness", wallpaperBrightness);
            j.put("wallpaperSaturation", wallpaperSaturation); j.put("wallpaperBlur", wallpaperBlur);
        } catch (Exception ignored) {}
        return j;
    }

    void save(Context c) {
        c.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit()
                .putString("appearance", toJson().toString()).apply();
        // This plain-text backup remains in the app's own external folder for export/recovery.
        try { write(new File(c.getExternalFilesDir(null), "appearance.json"), toJson().toString()); }
        catch (Exception ignored) {}
    }

    static boolean exportTheme(Context c, CrystalAppearance s) {
        try {
            File dir = c.getExternalFilesDir(null);
            if (dir == null) return false;
            write(new File(dir, "CrystalClear_Theme_Backup.json"), s.toJson().toString(2));
            return true;
        } catch (Exception ex) { return false; }
    }

    static CrystalAppearance importTheme(Context c, android.net.Uri uri) throws Exception {
        try (java.io.InputStream in = c.getContentResolver().openInputStream(uri);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (in == null) throw new java.io.IOException("No file stream");
            byte[] buf = new byte[4096]; int n; int total = 0;
            while ((n = in.read(buf)) >= 0) {
                total += n; if (total > 65536) throw new java.io.IOException("Theme too large");
                out.write(buf, 0, n);
            }
            JSONObject data = new JSONObject(new String(out.toByteArray(), StandardCharsets.UTF_8));
            if (data.optInt("schema", 0) != 1) throw new java.io.IOException("Unsupported theme");
            return fromJson(data);
        }
    }

    static void write(File file, String text) throws Exception {
        if (file == null) throw new java.io.IOException("No storage");
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
            out.getFD().sync();
        }
    }

    /** Hue calculation runs only when a picture is selected, never for each frame. */
    static int sampleWallpaperTint(Bitmap image) {
        if (image == null || image.isRecycled()) return 0xFF9FF7FF;
        long red = 0, green = 0, blue = 0, weightTotal = 0;
        int stepX = Math.max(1, image.getWidth() / 40);
        int stepY = Math.max(1, image.getHeight() / 24);
        for (int y = 0; y < image.getHeight(); y += stepY) {
            for (int x = 0; x < image.getWidth(); x += stepX) {
                int c = image.getPixel(x, y);
                float[] hsv = new float[3];
                Color.colorToHSV(c, hsv);
                if (hsv[1] < .16f || hsv[2] < .16f) continue;
                int w = 1 + (int)(hsv[1] * 4);
                red += Color.red(c) * w; green += Color.green(c) * w; blue += Color.blue(c) * w;
                weightTotal += w;
            }
        }
        if (weightTotal == 0) return 0xFF9FF7FF;
        float[] hsv = new float[3];
        Color.RGBToHSV((int)(red / weightTotal), (int)(green / weightTotal),
                (int)(blue / weightTotal), hsv);
        hsv[1] = Math.min(.58f, Math.max(.25f, hsv[1] * .65f));
        hsv[2] = 1f;
        return Color.HSVToColor(hsv);
    }

    static int mix(int a, int b, float fraction) {
        float t = Math.max(0f, Math.min(1f, fraction));
        return Color.argb(255,
                (int)(Color.red(a) * (1f-t) + Color.red(b) * t),
                (int)(Color.green(a) * (1f-t) + Color.green(b) * t),
                (int)(Color.blue(a) * (1f-t) + Color.blue(b) * t));
    }

    CrystalAppearance adapted(int wallpaperColor) {
        if (!adaptive) return this;
        CrystalAppearance s = copy();
        float strength = adaptSensitivity / 100f
                * ("soft".equals(adaptStyle) ? .32f : "strong".equals(adaptStyle) ? .83f : .56f);
        s.widgetTint = mix(widgetTint, wallpaperColor, strength * .52f);
        s.dockTint = mix(dockTint, wallpaperColor, strength * .68f);
        s.spotlightTint = mix(spotlightTint, wallpaperColor, strength * .78f);
        return s;
    }

    static CrystalAppearance preset(String id) {
        CrystalAppearance s = new CrystalAppearance();
        if ("emerald".equals(id)) {
            s.dockTint=0xFF79D9B3; s.widgetAccent=0xFF73E3B1; s.spotlightTint=0xFF7CFFD7;
        } else if ("sunset".equals(id)) {
            s.dockTint=0xFFE0C1A0; s.widgetAccent=0xFFFFD290; s.spotlightTint=0xFFFFD493;
        } else if ("sapphire".equals(id)) {
            s.dockTint=0xFF9AB8F7; s.widgetAccent=0xFFB4CDFF; s.spotlightTint=0xFF87B4FF;
        }
        return s;
    }
}
