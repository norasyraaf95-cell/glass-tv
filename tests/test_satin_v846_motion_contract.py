"""V8.4.6 read-only source contracts for the experimental motion/handoff branch.

No Android device, APK build, or repository mutation is required.
Real perceived transitions and frame pacing still need physical-TV tests.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "launcher/app/src/main/java/com/gathin/tvhome"


class SatinV846MotionContractTest(unittest.TestCase):
    def text(self, name):
        return (JAVA / name).read_text(encoding="utf-8")

    def test_focus_is_faster_than_synchronized_decoration(self):
        timing = self.text("SatinTiming.java")
        self.assertIn("FOCUS_ILLUMINATION_MS = 218L", timing)
        self.assertIn("OPTICAL_MOTION_MS = 270L", timing)
        self.assertIn("FAST_DPAD_MS = 145L", timing)
        self.assertIn("SatinTiming.FOCUS_ILLUMINATION_MS", self.text("AppAdapter.java"))
        self.assertIn("SatinTiming.OPTICAL_MOTION_MS", self.text("SatinLightBridge.java"))
        self.assertIn("SatinTiming.OPTICAL_MOTION_MS", self.text("SatinPrismMotion.java"))

    def test_rapid_navigation_cancels_decoration_not_focus(self):
        adapter = self.text("AppAdapter.java")
        self.assertIn("SatinMotionPolicy.shouldSweep(", adapter)
        self.assertIn("h.satinMotion.stop();", adapter)
        self.assertIn("animateLight(focused);", adapter)
        self.assertIn("lightAnimator.setFloatValues(lightIntensity, target)", adapter)

    def test_frame_aware_launch_is_bounded_and_smarttube_only(self):
        handoff = self.text("FrameCueHandoff.java")
        adapter = self.text("AppAdapter.java")
        self.assertIn("MAX_FRAME_WAIT_MS = 64L", handoff)
        self.assertIn("Build.VERSION.SDK_INT < Build.VERSION_CODES.Q", handoff)
        self.assertIn("registerFrameCommitCallback(frameCallback)", handoff)
        self.assertIn('finish("bounded-fallback")', handoff)
        self.assertIn("if (!isPending()) return;", handoff)
        self.assertIn("frameCueHandoff.isPending()", adapter)
        self.assertIn("frameCueHandoff.cancel()", adapter)
        self.assertIn('inDock && "org.smarttube.stable".equals(e.pkg)', adapter)
        self.assertIn('"frame_cue_handoff", true', adapter)
        gradle = (ROOT / "launcher/app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("applicationIdSuffix '.referencev781v847'", gradle)
        self.assertNotIn("applicationIdSuffix '.referencev781v845'", gradle)
        self.assertNotIn("applicationIdSuffix '.referencev781v846'", gradle)
        self.assertIn("ctx.startActivity(intent, options.toBundle());", adapter)

    def test_smarttube_repeat_guard_prevents_second_launch_during_handoff(self):
        adapter = self.text("AppAdapter.java")
        policy = self.text("SatinLaunchPolicy.java")
        self.assertIn("SatinLaunchPolicy.acceptNewPress(", adapter)
        self.assertIn("lastSmartTubeClickAtMs = now", adapter)
        self.assertIn("lastSmartTubeClickAtMs = -1L;", adapter)
        self.assertIn("REPEAT_GUARD_MS = 450L", policy)
        self.assertIn("nowMs - previousAtMs >= REPEAT_GUARD_MS", policy)

    def test_previous_prayer_and_weather_remain_present(self):
        widget = self.text("ShahAlamWidgets.java")
        weather = self.text("WindyCloudView.java")
        self.assertIn("PRAYER_LOG = \"GlassTV.Prayer\"", widget)
        self.assertIn('"https://api.waktusolat.online/api/solat/SGR01?month="', widget)
        self.assertIn('new File(dir, "windy_clouds.mp4")', weather)


if __name__ == "__main__":
    unittest.main()
