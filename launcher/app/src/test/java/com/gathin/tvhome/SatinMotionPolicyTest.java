package com.gathin.tvhome;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class SatinMotionPolicyTest {
    @Test public void deliberateNavigationAfterLongIdleStillGetsReflection() {
        assertTrue(SatinMotionPolicy.shouldSweep(1, 2, false, 3000L));
        assertTrue(SatinMotionPolicy.shouldSweep(1, 2, false, 30000L));
        assertTrue(SatinMotionPolicy.shouldSweep(1, 2, false, 300000L));
    }

    @Test public void fastRepeatedRemotePressesRetargetInsteadOfDisappear() {
        assertTrue(SatinMotionPolicy.shouldSweep(1, 2, false, 0L));
        assertTrue(SatinMotionPolicy.shouldSweep(2, 1, false, 80L));
        assertTrue(SatinMotionPolicy.shouldSweep(1, 2, false, 144L));
        assertTrue(SatinMotionPolicy.shouldSweep(1, 2, false, 145L));
        assertTrue(SatinMotionPolicy.isRapid(0L));
        assertTrue(SatinMotionPolicy.isRapid(144L));
        assertFalse(SatinMotionPolicy.isRapid(145L));
        assertFalse(SatinMotionPolicy.shouldSweep(1, 2, false, -1L));
    }

    @Test public void sameTileAndFirstFocusDoNotAnimate() {
        assertFalse(SatinMotionPolicy.shouldSweep(-1, 0, false, 1000L));
        assertFalse(SatinMotionPolicy.shouldSweep(2, 2, false, 1000L));
    }

    @Test public void returnFromOtherAppNeverReplaysDirectionalSweep() {
        assertFalse(SatinMotionPolicy.shouldSweep(1, 2, true, 3000L));
    }
}
