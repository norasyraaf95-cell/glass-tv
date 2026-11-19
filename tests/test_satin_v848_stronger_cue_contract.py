"""V8.4.8 stronger premium cue guards. Source checks do not prove TV visibility."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "launcher/app/src/main/java/com/gathin/tvhome"


class V848CueContractTest(unittest.TestCase):
    def test_distinct_package_not_installed_v847(self):
        gradle = (ROOT / "launcher/app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("versionCode 35", gradle)
        self.assertIn("applicationIdSuffix '.referencev781v851immediate'", gradle)
        self.assertNotIn("applicationIdSuffix '.referencev781v847'", gradle)

    def test_visible_but_non_glowing_premium_cue(self):
        cue = (JAVA / "PremiumLaunchCue.java").read_text(encoding="utf-8")
        self.assertIn("PRESSED_SCALE = .93f", cue)
        self.assertIn("DIM_ALPHA = .14f", cue)
        self.assertIn("1.25f", cue)
        self.assertNotIn("travellingHighlight", cue)
        self.assertIn("canvas.drawRoundRect(cardBounds, cornerRadius, cornerRadius, silver)", cue)
        self.assertNotIn("Math.sin(Math.PI * sweep)", cue)
        self.assertIn("canvas.clipOutPath(exclusion)", cue)
        self.assertNotIn("BlurMaskFilter", cue)
        self.assertNotIn("ValueAnimator.ofFloat(", cue)

    def test_180ms_default_and_120ms_variant_preserved(self):
        modes = (JAVA / "LaunchCuePolicy.java").read_text(encoding="utf-8")
        self.assertIn("DEFAULT_MS = REFINED_MS", modes)
        self.assertIn("SILK_MS = 180", modes)
        self.assertIn("REFINED_MS = 240", modes)
        self.assertIn("QUICK_MS = 120", modes)
        self.assertIn("BASELINE_MS = 0", modes)

    def test_no_affected_widgets_or_dock_motion(self):
        adapter = (JAVA / "AppAdapter.java").read_text(encoding="utf-8")
        cloud = (JAVA / "WindyCloudView.java").read_text(encoding="utf-8")
        prayer = (JAVA / "ShahAlamWidgets.java").read_text(encoding="utf-8")
        self.assertIn('inDock && "org.smarttube.stable".equals(e.pkg)', adapter)
        self.assertIn("SatinTiming.OPTICAL_MOTION_MS",
            (JAVA / "SatinLightBridge.java").read_text(encoding="utf-8"))
        self.assertIn('new File(dir, "windy_clouds.mp4")', cloud)
        self.assertIn('PRAYER_LOG = "GlassTV.Prayer"', prayer)


if __name__ == "__main__":
    unittest.main()
