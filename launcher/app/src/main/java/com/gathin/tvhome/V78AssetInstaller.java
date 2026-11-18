package com.gathin.tvhome;

import android.content.Context;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Installs user-approved V7.8 artwork into this preview package's own files folder.
 * The launcher intentionally cannot access other app-private packages directly.
 * The supplied asset ZIP is moved there once via Bugjaeger, then safely unpacked.
 */
final class V78AssetInstaller {
    static final String ARCHIVE = "v781_asset_pack.zip";
    private static final Set<String> ALLOWED = new HashSet<>(Arrays.asList(
            "liquid_dock_base.webp", "liquid_dock_focused.webp",
            "liquid_focus_illumination.webp", "liquid_focus_reflection.webp",
            "liquid_focus_glow.webp", "liquid_static_reflections.webp",
            "wallpaper.jpg", "config.json"));
    private V78AssetInstaller() {}

    static boolean prepare(Context context) {
        File dir = context.getExternalFilesDir(null);
        if (dir == null) return false;
        File pack = new File(dir, ARCHIVE);
        if (!pack.isFile()) return new File(dir, "liquid_dock_base.webp").isFile()
                && new File(dir, "liquid_focus_illumination.webp").isFile()
                && new File(dir, "liquid_focus_reflection.webp").isFile();
        File marker = new File(dir, ".v781_artwork_installed");
        if (marker.isFile() && new File(dir, "liquid_focus_illumination.webp").isFile())
            return true;
        int count = 0;
        long total = 0;
        try (InputStream input = new BufferedInputStream(new java.io.FileInputStream(pack));
             ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory() || !ALLOWED.contains(entry.getName())) {
                    zip.closeEntry();
                    continue;
                }
                File dest = new File(dir, entry.getName());
                // Respect a wallpaper / personal configuration already placed here.
                if (dest.exists() && ("wallpaper.jpg".equals(entry.getName())
                        || "config.json".equals(entry.getName()))) {
                    zip.closeEntry();
                    continue;
                }
                File temp = new File(dir, entry.getName() + ".tmp");
                try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(temp))) {
                    byte[] buffer = new byte[8192];
                    int n;
                    long written = 0;
                    while ((n = zip.read(buffer)) != -1) {
                        written += n;
                        total += n;
                        if (written > 5_000_000L || total > 12_000_000L)
                            throw new java.io.IOException("Asset pack too large");
                        out.write(buffer, 0, n);
                    }
                }
                if (dest.exists() && !dest.delete()) throw new java.io.IOException("Cannot replace artwork");
                if (!temp.renameTo(dest)) throw new java.io.IOException("Cannot install artwork");
                count++;
                zip.closeEntry();
            }
            if (new File(dir, "liquid_dock_base.webp").isFile()
                    && new File(dir, "liquid_focus_illumination.webp").isFile()
                    && new File(dir, "liquid_focus_reflection.webp").isFile()
                    && new File(dir, "liquid_focus_glow.webp").isFile()
                    && new File(dir, "liquid_static_reflections.webp").isFile())
                marker.createNewFile();
            Log.i("GlassTV.V78", "Asset pack loaded, files installed=" + count);
        } catch (Exception error) {
            Log.e("GlassTV.V78", "Asset pack is invalid", error);
        }
        return new File(dir, "liquid_dock_base.webp").isFile()
                && new File(dir, "liquid_focus_illumination.webp").isFile()
                && new File(dir, "liquid_focus_reflection.webp").isFile();
    }
}
