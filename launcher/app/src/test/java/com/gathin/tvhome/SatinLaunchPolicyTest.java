package com.gathin.tvhome;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class SatinLaunchPolicyTest {
    @Test public void firstSmartTubePressAlwaysAccepted() {
        assertTrue(SatinLaunchPolicy.acceptNewPress(-1L, 0L));
    }

    @Test public void rapidRepeatDoesNotRelaunchApp() {
        assertFalse(SatinLaunchPolicy.acceptNewPress(1000L, 1050L));
        assertFalse(SatinLaunchPolicy.acceptNewPress(1000L, 1449L));
        assertTrue(SatinLaunchPolicy.acceptNewPress(1000L, 1450L));
    }

    @Test public void clockResetDoesNotLockLauncher() {
        assertTrue(SatinLaunchPolicy.acceptNewPress(1000L, 100L));
    }
}
