"""V8.5.1: isolate SmartTube immediate-launch experiment from approved dock and widgets."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "launcher/app/src/main/java/com/gathin/tvhome"

def src(name):
    return (JAVA / name).read_text(encoding="utf-8")

class V851ImmediateLaunchTest(unittest.TestCase):
    def test_preview_package_is_isolated_from_current_home(self):
        gradle = (ROOT / "launcher/app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("applicationIdSuffix '.referencev781v851immediate'", gradle)
        self.assertIn("versionCode 35", gradle)
        self.assertNotIn("applicationIdSuffix '.referencev781v848handoff'", gradle)

    def test_smarttube_is_dispatched_without_prelaunch_wait(self):
        adapter = src("AppAdapter.java")
        self.assertIn("if (frameEligible && ctx.getSharedPreferences(\"satin_v851\"", adapter)
        block = adapter.split("// V8.5.1 isolated A/B test:", 1)[1].split("Runnable startNow = () -> {",1)[0]
        self.assertIn("ctx.startActivity(intent);", block)
        self.assertNotIn("premiumLaunchCue.play", block)
        self.assertNotIn("frameCueHandoff.afterCueFrame", block)
        self.assertNotIn("ActivityOptions.makeCustomAnimation", block)
        self.assertNotIn("overridePendingTransition", block)
        self.assertIn("lastSmartTubeClickAtMs = -1L;", block)

    def test_press_feedback_and_focus_stay_unchanged(self):
        adapter = src("AppAdapter.java")
        self.assertIn("satinMotion.playClick();", adapter)
        self.assertIn("SatinMotionPolicy.isRapid(interval)", adapter)
        self.assertIn("satinBridge.travel(previousDockCard, h.card, rapid)", adapter)
        self.assertIn("SatinLaunchPolicy.acceptNewPress", adapter)
        self.assertIn('inDock && "org.smarttube.stable".equals(e.pkg)', adapter)

    def test_original_artwork_and_live_widgets_untouched(self):
        main = src("MainActivity.java")
        self.assertIn("V78AssetInstaller.prepare(this)", main)
        self.assertIn('"floating_prism_prayer.webp"', main)
        self.assertIn('"floating_prism_weather.webp"', main)
        self.assertIn('new File(dir, "windy_clouds.mp4")', src("WindyCloudView.java"))
        self.assertIn('PRAYER_LOG = "GlassTV.Prayer"', src("ShahAlamWidgets.java"))

if __name__ == "__main__":
    unittest.main()
