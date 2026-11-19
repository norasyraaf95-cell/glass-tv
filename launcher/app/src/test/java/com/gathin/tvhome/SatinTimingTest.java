package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class SatinTimingTest {
    @Test public void responsiveFocusSettlesBeforeOptics() {
        assertEquals(218L, SatinTiming.FOCUS_ILLUMINATION_MS);
        assertEquals(270L, SatinTiming.OPTICAL_MOTION_MS);
        assertTrue(SatinTiming.FOCUS_ILLUMINATION_MS < SatinTiming.OPTICAL_MOTION_MS);
    }

    @Test public void fastRemoteInputSuppressesOnlyDecoration() {
        assertEquals(145L, SatinTiming.FAST_DPAD_MS);
        assertTrue(SatinMotionPolicy.shouldSweep(1, 2, false, 270L));
    }
}
