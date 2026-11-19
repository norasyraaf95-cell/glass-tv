package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public final class LaunchCuePolicyTest {
    @Test public void onlyTwoVisibleModes() {
        assertEquals(120, LaunchCuePolicy.durationForMode("cinematic120"));
        assertEquals(180, LaunchCuePolicy.durationForMode("cinematic180"));
        assertEquals(0, LaunchCuePolicy.durationForMode("immediate"));
        assertEquals(0, LaunchCuePolicy.durationForMode("frame"));
    }

    @Test public void invalidDurationFailsSafeToShortCue() {
        assertEquals(120, LaunchCuePolicy.durationFor(-300));
        assertEquals(120, LaunchCuePolicy.durationFor(9999));
        assertEquals(180, LaunchCuePolicy.durationFor(180));
    }
}
