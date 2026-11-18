package com.gathin.tvhome;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.view.Surface;
import android.view.TextureView;
import android.util.AttributeSet;

import java.io.File;
import java.io.IOException;

/**
 * Replays the user's exact Windy Clouds V2 motion beneath the native UI.
 * The source is a small, silent, 1280x720 15fps H.264 file in this app's external files:
 *    windy_clouds.mp4
 *
 * When Home pauses, release the decoder completely: important on a 1 GB Mi TV Stick.
 * If missing or unsupported, Glass TV's static wallpaper remains behind this transparent view.
 */
public final class WindyCloudView extends TextureView
        implements TextureView.SurfaceTextureListener {
    private final File video;
    private boolean enabled;
    private boolean foreground;
    private MediaPlayer player;
    private Surface surface;

    public WindyCloudView(Context context, AttributeSet attrs) {
        super(context, attrs);
        File dir = context.getExternalFilesDir(null);
        video = dir == null ? null : new File(dir, "windy_clouds.mp4");
        setSurfaceTextureListener(this);
        setOpaque(false);
        setAlpha(0f);
        setFocusable(false);
        setClickable(false);
    }

    boolean hasVideo() {
        return video != null && video.isFile() && video.length() > 1024;
    }

    void setCloudsEnabled(boolean value) {
        enabled = value;
        if (!enabled || !hasVideo()) {
            releasePlayer();
            setAlpha(0f);
            setVisibility(GONE);
            return;
        }
        setVisibility(VISIBLE);
        maybeStart();
    }

    void onHomeResume() {
        foreground = true;
        if (enabled && hasVideo()) {
            setVisibility(VISIBLE);
            maybeStart();
        }
    }

    void onHomePause() {
        foreground = false;
        releasePlayer(); // No video decoding while YouTube, Netflix etc are in front.
        setAlpha(0f);
    }

    private void maybeStart() {
        if (!foreground || !enabled || !hasVideo() || player != null || !isAvailable()) return;
        try {
            surface = new Surface(getSurfaceTexture());
            MediaPlayer mp = new MediaPlayer();
            player = mp;
            mp.setSurface(surface);
            mp.setDataSource(video.getAbsolutePath());
            mp.setVolume(0f, 0f);
            mp.setLooping(true);
            mp.setOnPreparedListener(p -> {
                if (foreground && enabled) {
                    p.start();
                    setAlpha(1f);
                }
            });
            mp.setOnErrorListener((p, what, extra) -> {
                releasePlayer();
                setAlpha(0f);
                return true; // Fail safely to the static wallpaper.
            });
            mp.prepareAsync();
        } catch (Exception ex) {
            releasePlayer();
            setAlpha(0f);
        }
    }

    private void releasePlayer() {
        MediaPlayer mp = player;
        player = null;
        if (mp != null) {
            mp.setOnPreparedListener(null);
            mp.setOnErrorListener(null);
            try { mp.stop(); } catch (IllegalStateException ignored) {}
            mp.release();
        }
        if (surface != null) {
            surface.release();
            surface = null;
        }
    }

    @Override public void onSurfaceTextureAvailable(SurfaceTexture s, int w, int h) {
        maybeStart();
    }

    @Override public void onSurfaceTextureSizeChanged(SurfaceTexture s, int w, int h) {}

    @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture s) {
        releasePlayer();
        setAlpha(0f);
        return true;
    }

    @Override public void onSurfaceTextureUpdated(SurfaceTexture s) {}

    @Override protected void onDetachedFromWindow() {
        releasePlayer();
        super.onDetachedFromWindow();
    }
}
