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
        self.assertIn("applicationIdSuffix '.crystalcontrolborderfix'", build)
        self.assertIn("versionCode 42", build)
        self.assertNotIn('android.intent.category.HOME', manifest)
        self.assertIn('android.intent.category.LEANBACK_LAUNCHER', manifest)
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
        self.assertIn('magneticSpotlight.focus(r, resuming);', main)
        self.assertIn("magneticSpotlight.stop();", main)
        self.assertIn("<com.gathin.tvhome.MagneticSpotlightView", layout)

    def test_control_center_gear_is_hidden_and_remote_driven(self):
        main = (JAVA / "MainActivity.java").read_text()
        control = (JAVA / "CrystalControlCenter.java").read_text()
        self.assertIn("btnSettings.setOnClickListener(v -> openCrystalControlCenter());",main)
        self.assertIn("open(systemIntent(Settings.ACTION_SETTINGS));",main)
        self.assertIn("new Dialog(activity)",control)
        for item in ("Widgets", "Dock Glass", "Magnetic Spotlight", "Movement",
                     "Adaptive Glass", "Wallpaper", "Themes"):
            self.assertIn('"' + item + '"',control)
        self.assertIn("host.preview(draft.copy());",control)
        self.assertIn("host.cancel();",control)

    def test_theme_and_wallpaper_are_reversible(self):
        state = (JAVA / "CrystalAppearance.java").read_text()
        file = (JAVA / "CrystalWallpaperManager.java").read_text()
        self.assertIn('"appearance.json"',state)
        self.assertIn('"CrystalClear_Theme_Backup.json"',state)
        self.assertIn('"wallpaper_original.jpg"',state)
        self.assertIn("void save(Context c)",state)
        self.assertIn("static void commit(Context ctx)",file)
        self.assertIn("static void cancel(Context ctx)",file)
        self.assertIn("copyFile(active,orig,MAX_BYTES)",file)

    def test_magnetic_light_is_above_glass_below_cards(self):
        dock=(JAVA/"DockDecoration.java").read_text()
        light=(JAVA/"MagneticSpotlightView.java").read_text()
        self.assertLess(dock.index("c.drawRoundRect(rect, radius, radius, band);"),
                        dock.index("spotlight.drawInTray(c, parent);"))
        self.assertIn("setDrawingHost(grid)",dock)
        self.assertIn("drawInTray(Canvas canvas, View grid)",light)
        self.assertIn("fromX = positionX;",light)

    def test_locked_video_matches_additive_light_geometry(self):
        light = (JAVA/"MagneticSpotlightView.java").read_text()
        shelf = (JAVA/"DockDecoration.java").read_text()
        self.assertIn("broadPaint.setBlendMode(android.graphics.BlendMode.PLUS);",light)
        self.assertIn("PorterDuff.Mode.ADD",light)
        self.assertIn("w * .64f",light)
        self.assertIn("h * .35f",light)
        self.assertIn("0xFF3D4536",light)
        self.assertIn("0xFF5D7470",light)
        self.assertNotIn("new LinearGradient(rect.left, rect.bottom",shelf)
        self.assertNotIn("new LinearGradient(0f, top, 0f, bottom",shelf)

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
        self.assertIn("spotlight.drawInTray(c, parent);", tray)
        self.assertIn("c.drawRoundRect(rect, radius, radius, crystalRim);", tray)


if __name__ == "__main__":
    unittest.main()
