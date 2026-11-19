package com.gathin.tvhome;

/**
 * Protects one SmartTube launch from a second OK while the first Activity is
 * taking over. This does not delay normal input or affect the other dock apps.
 */
final class SatinLaunchPolicy {
    static final long REPEAT_GUARD_MS = 450L;

    private SatinLaunchPolicy() {}

    static boolean acceptNewPress(long previousAtMs, long nowMs) {
        // Uptime can start at zero and future tests can reset the clock.
        return previousAtMs < 0 || nowMs < previousAtMs
                || nowMs - previousAtMs >= REPEAT_GUARD_MS;
    }
}
