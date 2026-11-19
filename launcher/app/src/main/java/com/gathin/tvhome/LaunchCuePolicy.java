package com.gathin.tvhome;

/**
 * V8.4.8 controlled launch-choreography modes for physical-TV A/B testing.
 *
 * 0 = V8.4.6 frame-commit baseline (64ms maximum).
 * 120/180 = a visible launcher-owned prelaunch animation, then startActivity.
 * These are experimental timings, NOT claims about perceived smoothness.
 */
final class LaunchCuePolicy {
    static final int BASELINE_MS = 0;
    static final int QUICK_MS = 120;
    static final int SILK_MS = 180;
    static final int DEFAULT_MS = SILK_MS;

    private LaunchCuePolicy() {}

    static boolean isSupported(int candidate) {
        return candidate == BASELINE_MS || candidate == QUICK_MS || candidate == SILK_MS;
    }

    static int normalize(int candidate) {
        return isSupported(candidate) ? candidate : DEFAULT_MS;
    }
}
