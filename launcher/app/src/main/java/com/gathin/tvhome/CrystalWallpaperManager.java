package com.gathin.tvhome;

import android.content.Context;
import android.net.Uri;
import android.graphics.BitmapFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

/**
 * Wallpaper is never written to wallpaper.jpg during live preview.
 * Save stages an atomic replacement; original is preserved for Restore.
 */
final class CrystalWallpaperManager {
    static final String ACTIVE = "wallpaper.jpg";
    static final String STAGED = "wallpaper_pending.jpg";
    private static final int MAX_BYTES = 24 * 1024 * 1024;
    private static final int MAX_EDGE = 10000;

    static File dir(Context ctx) { return ctx.getExternalFilesDir(null); }
    static File active(Context ctx) { return new File(dir(ctx), ACTIVE); }
    static File staged(Context ctx) { return new File(dir(ctx), STAGED); }
    static File original(Context ctx) { return new File(dir(ctx), CrystalAppearance.BASE_WALLPAPER); }

    static File stage(Context ctx, Uri uri) throws Exception {
        File dir = dir(ctx);
        if (dir == null || (!dir.exists() && !dir.mkdirs())) throw new IOException("No storage");
        File temp = new File(dir, "wallpaper_receiving.tmp");
        try (InputStream in = ctx.getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IOException("Cannot read image");
            copy(in,temp,MAX_BYTES);
        }
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(temp.getAbsolutePath(),bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0
                || bounds.outWidth > MAX_EDGE || bounds.outHeight > MAX_EDGE) {
            temp.delete(); throw new IOException("Unsupported wallpaper dimensions");
        }
        File stage = staged(ctx);
        if (stage.exists() && !stage.delete()) throw new IOException("Can't replace staged image");
        if (!temp.renameTo(stage)) { copyFile(temp,stage,MAX_BYTES);temp.delete(); }
        return stage;
    }

    static File stageOriginal(Context ctx) throws Exception {
        File orig = original(ctx);
        if (!orig.isFile()) throw new IOException("No original wallpaper backup yet");
        File pending = staged(ctx);
        copyFile(orig,pending,MAX_BYTES);
        return pending;
    }

    static boolean hasPending(Context ctx) {
        File f=staged(ctx);return f.isFile() && f.length()>0;
    }

    static void cancel(Context ctx) {
        File pending=staged(ctx); if(pending.isFile()) pending.delete();
    }

    static void commit(Context ctx) throws Exception {
        if (!hasPending(ctx)) return;
        File active = active(ctx), orig = original(ctx), pending = staged(ctx);
        // The approved alpine photo is backed up exactly ONCE, before first edit.
        if (active.isFile() && !orig.exists()) copyFile(active,orig,MAX_BYTES);
        File replacement = new File(dir(ctx),"wallpaper_replacement.tmp");
        copyFile(pending,replacement,MAX_BYTES);
        // Preserve recoverable previous version until replacement is complete.
        File previous = new File(dir(ctx),"wallpaper_previous.jpg");
        if(active.exists()) copyFile(active,previous,MAX_BYTES);
        if(active.exists() && !active.delete()) throw new IOException("Cannot replace old wallpaper");
        if(!replacement.renameTo(active)) {
            if(previous.exists()) copyFile(previous,active,MAX_BYTES);
            throw new IOException("Cannot activate wallpaper");
        }
        pending.delete();
    }

    static void copyFile(File from, File to, int limit) throws Exception {
        try(InputStream in=new FileInputStream(from)) {copy(in,to,limit);}
    }
    private static void copy(InputStream from,File target,int maxBytes) throws Exception {
        try(FileOutputStream out=new FileOutputStream(target)) {
            byte[] b=new byte[8192];int n;long sum=0;
            while((n=from.read(b))!=-1) {
                sum+=n;
                if(sum>maxBytes) throw new IOException("Image exceeds 24 MB limit");
                out.write(b,0,n);
            }
            out.getFD().sync();
        } catch(Exception e) {target.delete();throw e;}
    }
}
