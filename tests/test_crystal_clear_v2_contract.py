"""Crystal Clear V2 behavioural regression contracts. No device or network required."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "launcher/app/src/main"
JAVA = SRC / "java/com/gathin/tvhome"


class CrystalClearV2Contracts(unittest.TestCase):
    def test_separate_preview_not_home(self):
        build = (ROOT / "launcher/app/build.gradle").read_text()
        manifest = (SRC / "AndroidManifest.xml").read_text()
        self.assertIn("applicationIdSuffix '.crystalclearhome'", build)
        self.assertIn("versionCode 38", build)
        self.assertIn('android.intent.category.HOME', manifest)
        self.assertIn('android.intent.category.DEFAULT', manifest)

    def test_weather_updates_but_never_toggles_cloud_video(self):
        main = (JAVA / "MainActivity.java").read_text()
        self.assertIn("cloudMotion.setCloudsEnabled(false);", main)
        self.assertIn("weatherCard.setOnClickListener(null);", main)
        self.assertIn("weatherCard.setClickable(false);", main)
        self.assertNotIn("cloudMotion.setCloudsEnabled(next);", main)
        widgets = (JAVA / "ShahAlamWidgets.java").read_text()
        self.assertIn("fetchWeather()", widgets)
        self.assertIn("fetchPrayers(", widgets)

    def test_original_satin_motion_restored_on_native_tiles(self):
        adapter = (JAVA / "AppAdapter.java").read_text()
        self.assertIn("h.card.setForeground(satinEnabled ? h.satinMotion : null);", adapter)
        self.assertIn("dockGlass.animateFocus(focused);\n                        if (focused) owner.onReferenceDockFocused(this, cardIndex);", adapter)
        self.assertIn("satinBridge.travel(previousDockCard, h.card, rapid);", adapter)

    def test_all_native_dock_labels_white(self):
        adapter = (JAVA / "AppAdapter.java").read_text()
        self.assertIn("h.label.setTextColor(Color.WHITE);", adapter)
        self.assertIn('h.label.setText("prime video");', adapter)
        self.assertIn('h.label.setText(position == 0 ? "YouTube"', adapter)

    def test_old_purple_atlas_will_not_override_new_glass(self):
        dock = (JAVA / "DockArtwork.java").read_text()
        self.assertIn('startsWith("com.gathin.tvhome.crystalclear")', dock)
        self.assertIn("valid = false;", dock)
        tray = (JAVA / "DockDecoration.java").read_text()
        self.assertIn("blur.draw(c, parent, rect, radius, 1f);", tray)
        self.assertIn("c.drawRoundRect(rect, radius, radius, crystalRim);", tray)


if __name__ == "__main__":
    unittest.main()
