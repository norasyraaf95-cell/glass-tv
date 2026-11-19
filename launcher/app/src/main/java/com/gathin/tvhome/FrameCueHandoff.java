package com.gathin.tvhome;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

/**
 * Opt-in frame-aware launch for the SmartTube dock tile.
 *
 * The key-down/click animation is already started by AppAdapter. Launch after
 * its next frame has been SUBMITTED to the Android renderer, with a strict
 * timeout so stalled rendering never blocks the app. Submission is not proof
 * of physical visibility: only TV footage can validate perceived handoff.
 *
 * Android API 29+ registerFrameCommitCallback works only on hardware-rendered
 * views. Older, detached, or software-rendered views launch immediately.
 */
final class FrameCueHandoff {
    static final long MAX_FRAME_WAIT_MS = 64L;
    private static final String TAG = "GlassTV.V846";
    private final Handler main = new Handler(Looper.getMainLooper());
    private Runnable pendingAction;
    private Runnable timeout;
    private Runnable frameCallback;
    private ViewTreeObserver observer;
    private long startedAt;

    boolean isPending() {
        return pendingAction != null;
    }

    /** Returns false for a duplicate OK while another launch is awaiting a frame. */
    boolean afterCueFrame(View cue, Runnable launch) {
        if (isPending()) return false;
        if (cue == null || !cue.isAttachedToWindow() || !cue.isHardwareAccelerated()
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Log.i(TAG, "Cue frame unavailable; immediate launch");
            launch.run();
            return true;
        }
        ViewTreeObserver tree = cue.getViewTreeObserver();
        if (!tree.isAlive()) {
            launch.run();
            return true;
        }
        pendingAction = launch;
        observer = tree;
        startedAt = SystemClock.uptimeMillis();
        frameCallback = () -> main.post(() -> finish("frame-committed"));
        timeout = () -> finish("bounded-fallback");
        tree.registerFrameCommitCallback(frameCallback);
        // The already-running key-down/click pulse invalidates its drawable.
        // Also request a traversal even if the OEM drops that invalidation.
        cue.postInvalidateOnAnimation();
        main.postDelayed(timeout, MAX_FRAME_WAIT_MS);
        return true;
    }

    /** Cancels a handoff if Home is interrupted before the destination launch. */
    void cancel() {
        if (!isPending()) return;
        clear();
        Log.i(TAG, "Pending cue launch cancelled");
    }

    private void finish(String reason) {
        if (!isPending()) return; // Commit and timeout can race: exactly one launch.
        Runnable action = pendingAction;
        long elapsed = SystemClock.uptimeMillis() - startedAt;
        clear();
        Log.i(TAG, "SmartTube handoff=" + reason + " elapsedMs=" + elapsed);
        action.run();
    }

    private void clear() {
        if (timeout != null) main.removeCallbacks(timeout);
        // The callback could already be on the render thread's queue.
        // It is harmless because finish() checks pendingAction.
        if (observer != null && observer.isAlive() && frameCallback != null
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            observer.unregisterFrameCommitCallback(frameCallback);
        }
        pendingAction = null;
        timeout = null;
        frameCallback = null;
        observer = null;
    }
}
