package com.gathin.tvhome;

/**
 * V8.4.6 dock motion contract.
 *
 * Focus illumination deliberately responds before decoration settles.
 * The bridge and prism overlay share a single duration/easing envelope.
 * Rapid input cancels only decoration; the underlying 218ms focus-light
 * animation always retargets from its currently rendered intensity.
 */
final class SatinTiming {
    static final long FOCUS_ILLUMINATION_MS = 218L;
    static final long OPTICAL_MOTION_MS = 270L;
    static final long FAST_DPAD_MS = 145L;
    static final long CLICK_MS = 180L;
    static final long RETURN_MS = 160L;

    private SatinTiming() {}
}
