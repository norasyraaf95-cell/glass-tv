package com.gathin.tvhome;

/**
 * V8.4.7 source-only A/B choices for SmartTube launch.
 *
 * The physical-TV pass starts on cinematic120. The same source can be tested
 * with cinematic180 / frame / immediate without modifying Prayer, Weather, or
 * the approved dock assets.
 */
final class LaunchCuePolicy {
    static final String CINEMATIC_120 = "cinematic120";
    static final String CINEMATIC_180 = "cinematic180";
    static final String FRAME = "frame";
    static final String IMMEDIATE = "immediate";
    static final int SHORT_MS = 120;
    static final int LONG_MS = 180;

    private LaunchCuePolicy() {}

    static int durationForMode(String mode) {
        if (CINEMATIC_180.equals(mode)) return LONG_MS;
        if (CINEMATIC_120.equals(mode)) return SHORT_MS;
        return 0;
    }

    static int durationFor(int requested) {
        return requested == LONG_MS ? LONG_MS : SHORT_MS;
    }
}
