"""V8.4.5 regression gates: original topbar, verified prayer API and quiet bridge.

Source-level tests guard functionality across release builds without needing
network access or the user's Xiaomi TV. Physical TV validation remains required.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "launcher/app/src/main/java/com/gathin/tvhome"
GRADLE = ROOT / "launcher/app/build.gradle"


class SatinV845ContractTest(unittest.TestCase):
    def test_short_bridge_not_growing_white_line(self):
        code = (JAVA / "SatinLightBridge.java").read_text(encoding="utf-8")
        self.assertIn("new PathInterpolator(.42f, 0f, .58f, 1f)", code)
        self.assertIn("Math.min(gap * .30f", code)
        self.assertIn("float center = startX + (endX - startX) * progress", code)
        self.assertNotIn("startX+(endX-startX)*progress, y, paint", code)

    def test_prayer_fetch_failure_is_reported_and_retry_faster(self):
        code = (JAVA / "ShahAlamWidgets.java").read_text(encoding="utf-8")
        self.assertIn('PRAYER_LOG = "GlassTV.Prayer"', code)
        self.assertIn('PRAYER_RETRY_MS = 2L * 60_000L', code)
        self.assertIn('Prayer fetch failed:', code)
        self.assertIn('saveVerifiedPrayers(json, month, "HTTPS")', code)

    def test_external_prayer_backup_never_fabricates_times(self):
        code = (JAVA / "ShahAlamWidgets.java").read_text(encoding="utf-8")
        self.assertIn('"prayer_sgr01.json"', code)
        self.assertIn('month.equals(format("yyyy-MM", new Date()))', code)
        self.assertIn('shortTime(match.optString(key)) == null', code)
        self.assertIn('verifiedPrayerFile.length() > 100_000', code)
        self.assertNotIn('05:49:00', code)

    def test_launch_cue_remains_bounded_and_oem_fade_retained(self):
        code = (JAVA / "AppAdapter.java").read_text(encoding="utf-8")
        handoff = (JAVA / "FrameCueHandoff.java").read_text(encoding="utf-8")
        self.assertIn('ctx.startActivity(intent, options.toBundle())', code)
        self.assertIn('overridePendingTransition(', code)
        self.assertIn('MAX_FRAME_WAIT_MS = 64L', handoff)
        self.assertNotIn('postDelayed(() -> startWithSilk', code)

    def test_installed_topbar_skins_unmodified(self):
        code = (JAVA / "MainActivity.java").read_text(encoding="utf-8")
        self.assertIn('"floating_prism_prayer.webp"', code)
        self.assertIn('"floating_prism_weather.webp"', code)
        gradle = GRADLE.read_text(encoding="utf-8")
        self.assertIn("applicationIdSuffix '.referencev781v846'", gradle)


if __name__ == "__main__":
    unittest.main()
