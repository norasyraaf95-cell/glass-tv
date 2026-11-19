package com.gathin.tvhome;

/**
 * Small pure-Java DPAD policy so V8.4 focus effects can be regression-tested
 * without an Android device. Each genuine dock-to-dock move gets an optical
 * acknowledgement; rapid presses shorten the effect instead of removing it.
 */
final class SatinMotionPolicy {
    static final long FAST_DPAD_MS = SatinTiming.FAST_DPAD_MS;

    private SatinMotionPolicy() {}

    static boolean isRapid(long elapsedMs) {
        return elapsedMs >= 0L && elapsedMs < FAST_DPAD_MS;
    }

    static boolean shouldSweep(int previous, int next, boolean suppressed, long elapsedMs) {
        return !suppressed
                && previous >= 0 && next >= 0
                && previous != next
                && elapsedMs >= 0L;
    }
}
