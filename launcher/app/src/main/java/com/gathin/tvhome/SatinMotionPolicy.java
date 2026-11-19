package com.gathin.tvhome;

/**
 * Small pure-Java DPAD policy so V8.4 focus effects can be regression-tested
 * without an Android device. The previous 2.5-second upper bound incorrectly
 * suppressed a normal sweep after the user paused on one tile.
 */
final class SatinMotionPolicy {
    static final long FAST_DPAD_MS = SatinTiming.FAST_DPAD_MS;

    private SatinMotionPolicy() {}

    static boolean shouldSweep(int previous, int next, boolean suppressed, long elapsedMs) {
        return !suppressed
                && previous >= 0 && next >= 0
                && previous != next
                && elapsedMs >= FAST_DPAD_MS;
    }
}
