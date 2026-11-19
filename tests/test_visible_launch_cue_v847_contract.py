"""V8.4.7 source contracts: launcher-owned SmartTube cue and stable widgets."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "launcher/app/src/main/java/com/gathin/tvhome"


class VisibleLaunchCueContractTest(unittest.TestCase):
    def content(self, file):
        return (JAVA / file).read_text(encoding="utf-8")

    def test_visible_cue_draws_and_restores_card(self):
        cue = self.content("VisibleLaunchCue.java")
        self.assertIn("Choreographer.getInstance()", cue)
        self.assertIn("choreographer.postFrameCallback(frame)", cue)
        self.assertIn("WATCHDOG_MARGIN_MS = 72L", cue)
        self.assertIn("handler.postDelayed(watchdog, durationMs + WATCHDOG_MARGIN_MS)", cue)
        self.assertIn("Math.sin(Math.PI * phase)", cue)
        self.assertIn("pressedCard.setScaleX(originalScaleX)", cue)
        self.assertIn("pressedCard.setScaleY(originalScaleY)", cue)
        self.assertIn("canvas.drawRoundRect(selected, radius, radius, rim)", cue)

    def test_launch_strategy_is_smarttube_only_and_bounded(self):
        adapter = self.content("AppAdapter.java")
        policy = self.content("LaunchCuePolicy.java")
        self.assertIn('getString("launch_cue_mode", LaunchCuePolicy.CINEMATIC_120)', adapter)
        self.assertIn('int duration = LaunchCuePolicy.durationForMode(mode)', adapter)
        self.assertIn("visibleLaunchCue.play(sourceCard, duration, startNow)", adapter)
        self.assertIn("frameCueHandoff.afterCueFrame(sourceCard, startNow)", adapter)
        self.assertIn('inDock && "org.smarttube.stable".equals(e.pkg)', adapter)
        self.assertIn("if (visibleLaunchCue != null) visibleLaunchCue.cancel();", adapter)
        self.assertIn('CINEMATIC_120 = "cinematic120"', policy)
        self.assertIn('CINEMATIC_180 = "cinematic180"', policy)
        self.assertIn("SHORT_MS = 120", policy)
        self.assertIn("LONG_MS = 180", policy)

    def test_release_id_is_isolated(self):
        gradle = (ROOT / "launcher/app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("applicationIdSuffix '.referencev781v847'", gradle)
        self.assertIn("versionCode 31", gradle)
        self.assertNotIn("applicationIdSuffix '.referencev781v846'", gradle)

    def test_prayer_weather_visual_modules_unchanged_in_scope(self):
        main = self.content("MainActivity.java")
        prayer = self.content("ShahAlamWidgets.java")
        windy = self.content("WindyCloudView.java")
        self.assertIn('"floating_prism_prayer.webp"', main)
        self.assertIn('"floating_prism_weather.webp"', main)
        self.assertIn('"prayer_sgr01.json"', prayer)
        self.assertIn('"windy_clouds.mp4"', windy)


if __name__ == "__main__":
    unittest.main()
