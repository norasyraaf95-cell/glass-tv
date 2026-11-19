"""Guard against accidentally dropping the installed V7.8.1 TOPBAR widget look.

This experiment must reuse native prayer/weather content and the original
static WebP skins. The test checks source structure without Android or a TV.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "launcher/app/src/main/java/com/gathin/tvhome"
LAYOUT = ROOT / "launcher/app/src/main/res/layout/activity_main.xml"


class FloatingPrismTopBarContractTest(unittest.TestCase):
    def test_prayer_and_weather_read_original_webp_skins(self):
        code = (JAVA / "MainActivity.java").read_text(encoding="utf-8")
        self.assertIn('"floating_prism_prayer.webp"', code)
        self.assertIn('"floating_prism_weather.webp"', code)
        self.assertIn("FloatingPrismTopBarDrawable.fromFiles(", code)
        self.assertIn("skin != null ? skin : new PremiumGlassDrawable(card, false)", code)

    def test_original_skin_assets_remain_allowed(self):
        code = (JAVA / "V78AssetInstaller.java").read_text(encoding="utf-8")
        self.assertIn('"floating_prism_prayer.webp"', code)
        self.assertIn('"floating_prism_weather.webp"', code)

    def test_date_layout_matches_installed_topbar_variant(self):
        code = LAYOUT.read_text(encoding="utf-8")
        self.assertIn('android:layout_marginTop="54dp"', code)
        self.assertIn('android:id="@+id/dateBlock"\n                    android:visibility="visible"', code)
        self.assertIn('android:text="Thursday  |  19 Nov"', code)
        java = (JAVA / "MainActivity.java").read_text(encoding="utf-8")
        self.assertIn('"EEEE  |  dd MMM", Locale.ENGLISH', java)

    def test_live_weather_prayer_controller_preserved(self):
        code = (JAVA / "MainActivity.java").read_text(encoding="utf-8")
        self.assertIn("shahAlamWidgets = new ShahAlamWidgets(this);", code)
        self.assertIn("shahAlamWidgets.resume();", code)
        self.assertIn("shahAlamWidgets.pause();", code)
        self.assertIn("cloudMotion.setCloudsEnabled(", code)


if __name__ == "__main__":
    unittest.main()
