"""Isolated V8.4.9 motion refinement source guards; physical TV test still required."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "launcher/app/src/main/java/com/gathin/tvhome"

def source(name):
    return (JAVA / name).read_text(encoding="utf-8")

class V849MotionContractTest(unittest.TestCase):
    def test_separate_preview_never_replaces_installed_home(self):
        gradle = (ROOT / "launcher/app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("applicationIdSuffix '.referencev781v850smarttube'", gradle)
        self.assertIn("versionCode 34", gradle)\n        self.assertNotIn("applicationIdSuffix '.referencev781v848handoff'", gradle)

    def test_fast_navigation_retargets_both_gap_and_tile_effects(self):
        adapter = source("AppAdapter.java")
        timing = source("SatinTiming.java")
        policy = source("SatinMotionPolicy.java")
        self.assertIn("SatinMotionPolicy.isRapid(interval)", adapter)
        self.assertIn("satinBridge.travel(previousDockCard, h.card, rapid)", adapter)
        self.assertIn("h.satinMotion.playSweep(position > previousDockFocus ? 1 : -1, rapid)", adapter)
        self.assertIn("RAPID_OPTICAL_MOTION_MS = 150L", timing)
        self.assertIn("elapsedMs >= 0L", policy)
        self.assertIn("animator.cancel()", source("SatinPrismMotion.java"))
        self.assertIn("animator.cancel()", source("SatinLightBridge.java"))

    def test_launch_cue_is_bounded_light_and_smarttube_only(self):
        policy = source("LaunchCuePolicy.java")
        cue = source("PremiumLaunchCue.java")
        adapter = source("AppAdapter.java")
        self.assertIn("REFINED_MS = 240", policy)
        self.assertIn("DEFAULT_MS = REFINED_MS", policy)
        self.assertIn("PRESSED_SCALE = .93f", cue)
        self.assertIn("DIM_ALPHA = .14f", cue)
        self.assertNotIn("travellingHighlight", cue)
        self.assertIn("durationMs + STALL_GRACE_MS", cue)
        self.assertIn("choreographer.removeFrameCallback(frameCallback)", cue)\n        self.assertIn("handoffStarted = true", cue)\n        self.assertIn("void onHomePausing()", cue)
        self.assertIn('inDock && "org.smarttube.stable".equals(e.pkg)', adapter)

    def test_prayer_weather_wallpaper_and_dock_assets_untouched(self):
        activity = source("MainActivity.java")
        prayer = source("ShahAlamWidgets.java")
        weather = source("WindyCloudView.java")
        self.assertIn('"floating_prism_prayer.webp"', activity)
        self.assertIn('"floating_prism_weather.webp"', activity)
        self.assertIn("V78AssetInstaller.prepare(this)", activity)
        self.assertIn('PRAYER_LOG = "GlassTV.Prayer"', prayer)
        self.assertIn('new File(dir, "windy_clouds.mp4")', weather)

if __name__ == "__main__":
    unittest.main()
