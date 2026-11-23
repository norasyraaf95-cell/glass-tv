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
        self.assertIn("applicationIdSuffix '.crystalclearmagneticb'", build)
        self.assertIn("versionCode 39", build)
        self.assertNotIn('android.intent.category.HOME', manifest)
        self.assertIn('android.intent.category.LEANBACK_LAUNCHER', manifest)

    def test_weather_updates_but_never_toggles_cloud_video(self):
        main = (JAVA / "MainActivity.java").read_text()
        self.assertIn("cloudMotion.setCloudsEnabled(false);", main)
        self.assertIn("weatherCard.setOnClickListener(null);", main)
        self.assertIn("weatherCard.setClickable(false);", main)
        self.assertNotIn("cloudMotion.setCloudsEnabled(next);", main)
        widgets = (JAVA / "ShahAlamWidgets.java").read_text()
        self.assertIn("fetchWeather()", widgets)
        self.assertIn("fetchPrayers(", widgets)

    def test_original_satin_swipe_suppressed_for_magnetic_b(self):
        adapter = (JAVA / "AppAdapter.java").read_text()
        self.assertIn("h.card.setForeground(satinEnabled && !magneticFocusOnly ? h.satinMotion : null);", adapter)
        self.assertIn("magneticFocusOnly", adapter)
        self.assertIn("dockGlass.animateFocus(focused);", adapter)
        self.assertIn("if (!satinEnabled || magneticFocusOnly || position < 0", adapter)

    def test_locked_470ms_smootherstep_magnetic_spotlight(self):
        view = (JAVA / "MagneticSpotlightView.java").read_text()
        dock = (JAVA / "LiquidCrystalDockDrawable.java").read_text()
        main = (JAVA / "MainActivity.java").read_text()
        layout = (SRC / "res/layout/activity_main.xml").read_text()
        self.assertIn("TRANSITION_MS = 470L;", view)
        self.assertIn("t * t * t * (t * (t * 6f - 15f) + 10f)", view)
        self.assertIn("fromX = positionX;", view)
        self.assertIn("return;", view)
        self.assertIn("static final long HANDOFF_MS = MagneticSpotlightView.TRANSITION_MS;", dock)
        self.assertIn("magneticSpotlight.focus(r, resuming);", main)
        self.assertIn("magneticSpotlight.stop();", main)
        self.assertIn("<com.gathin.tvhome.MagneticSpotlightView", layout)

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
