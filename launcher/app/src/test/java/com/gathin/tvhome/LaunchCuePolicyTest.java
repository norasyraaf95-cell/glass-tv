package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class LaunchCuePolicyTest {
    @Test public void onlyControlledModesAreAccepted() {
        assertTrue(LaunchCuePolicy.isSupported(0));
        assertTrue(LaunchCuePolicy.isSupported(120));
        assertTrue(LaunchCuePolicy.isSupported(180));
        assertTrue(LaunchCuePolicy.isSupported(240));
        assertFalse(LaunchCuePolicy.isSupported(-1));
        assertFalse(LaunchCuePolicy.isSupported(64));
        assertFalse(LaunchCuePolicy.isSupported(2500));
    }

    @Test public void invalidConfigurationFallsBackTo240ms() {
        assertEquals(240, LaunchCuePolicy.DEFAULT_MS);
        assertEquals(240, LaunchCuePolicy.normalize(-1));
        assertEquals(240, LaunchCuePolicy.normalize(4000));
        assertEquals(240, LaunchCuePolicy.normalize(240));
        assertEquals(180, LaunchCuePolicy.normalize(180));
        assertEquals(0, LaunchCuePolicy.normalize(0));
    }
}
