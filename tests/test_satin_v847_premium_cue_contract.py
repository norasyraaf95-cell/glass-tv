"""Source-level acceptance contract for V8.4.7 launcher-controlled SmartTube cue.

This suite verifies wiring/guardrails, NOT perceived smoothness or panel FPS.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "launcher/app/src/main/java/com/gathin/tvhome"


def read(name):
    return (JAVA / name).read_text(encoding="utf-8")


class V847PremiumCueContractTest(unittest.TestCase):
    def test_isolated_package_and_version(self):
        gradle = (ROOT / "launcher/app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("versionCode 32", gradle)
        self.assertIn("applicationIdSuffix '.referencev781v848'", gradle)
        self.assertNotIn("applicationIdSuffix '.referencev781v847'", gradle)
        self.assertNotIn("applicationIdSuffix '.referencev781v846'", gradle)
        self.assertNotIn("applicationIdSuffix '.referencev781v845'", gradle)

    def test_three_explicit_ab_modes(self):
        policy = read("LaunchCuePolicy.java")
        activity = read("MainActivity.java")
        self.assertIn("BASELINE_MS = 0", policy)
        self.assertIn("QUICK_MS = 120", policy)
        self.assertIn("SILK_MS = 180", policy)
        self.assertIn('intent.hasExtra("glass_tv_launch_cue_ms")', activity)
        self.assertIn('getSharedPreferences("satin_v847", MODE_PRIVATE)', activity)
        self.assertIn("LaunchCuePolicy.isSupported(requested)", activity)

    def test_card_only_prelauch_and_bounded(self):
        cue = read("PremiumLaunchCue.java")
        adapter = read("AppAdapter.java")
        self.assertIn("PRESSED_SCALE = .95f", cue)
        self.assertIn("DIM_ALPHA = .28f", cue)
        self.assertIn("host.getOverlay().add(scrim)", cue)
        self.assertIn("canvas.clipOutPath(exclusion)", cue)
        self.assertIn("handler.postDelayed(deadline, durationMs + STALL_GRACE_MS)", cue)
        self.assertIn("finishCue(\"animation-complete\")", cue)
        self.assertIn("finishCue(\"bounded-fallback\")", cue)
        self.assertIn("Choreographer.getInstance()", cue)
        self.assertIn("SystemClock.uptimeMillis()", cue)
        self.assertIn("choreographer.postFrameCallback(frameCallback)", cue)
        self.assertIn("choreographer.removeFrameCallback(frameCallback)", cue)
        self.assertNotIn("import android.animation.ValueAnimator;", cue)
        self.assertNotIn("ValueAnimator.ofFloat(", cue)
        self.assertIn("if (premiumLaunchCue != null) premiumLaunchCue.cancel();", adapter)
        self.assertIn("premiumLaunchCue.play(sourceCard, cueMs, startNow)", adapter)
        self.assertIn('inDock && "org.smarttube.stable".equals(e.pkg)', adapter)
        self.assertIn("frameCueHandoff.afterCueFrame(sourceCard, startNow)", adapter)

    def test_home_and_navigation_abort_stale_prelaunch(self):
        activity = read("MainActivity.java")
        adapter = read("AppAdapter.java")
        self.assertIn("adapter.cancelPendingLaunchCue()", activity)
        self.assertIn("owner.premiumLaunchCue.cancel()", adapter)
        self.assertIn("owner.lastSmartTubeClickAtMs = -1L", adapter)
        self.assertIn("if (frameCueHandoff.isPending()", adapter)
        self.assertIn("premiumLaunchCue.isPending()", adapter)

    def test_prayer_weather_and_focus_not_rewritten(self):
        activity = read("MainActivity.java")
        widgets = read("ShahAlamWidgets.java")
        clouds = read("WindyCloudView.java")
        self.assertIn('"floating_prism_prayer.webp"', activity)
        self.assertIn('"floating_prism_weather.webp"', activity)
        self.assertIn('PRAYER_LOG = "GlassTV.Prayer"', widgets)
        self.assertIn('new File(dir, "windy_clouds.mp4")', clouds)
        self.assertIn("SatinMotionPolicy.shouldSweep(", read("AppAdapter.java"))
        self.assertIn("SatinTiming.OPTICAL_MOTION_MS", read("SatinLightBridge.java"))


if __name__ == "__main__":
    unittest.main()
