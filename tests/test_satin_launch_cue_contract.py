"""Small static checks for the experimental Glass TV V8.4.3 launch cue.

The true app-to-app 180ms crossfade is owned by Android/OEM; these tests only
guard that launcher feedback begins at remote key-down without delaying launch.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
ADAPTER = ROOT / "launcher/app/src/main/java/com/gathin/tvhome/AppAdapter.java"
MOTION = ROOT / "launcher/app/src/main/java/com/gathin/tvhome/SatinPrismMotion.java"


class SatinLaunchCueContractTest(unittest.TestCase):
    def test_press_cue_starts_on_key_down_without_consuming_click(self):
        code = ADAPTER.read_text(encoding="utf-8")
        self.assertIn("keyCode == KeyEvent.KEYCODE_DPAD_CENTER", code)
        self.assertIn("satinMotion.playClick();", code)
        self.assertIn("return false;", code)
        self.assertIn("if (event.getRepeatCount() == 0", code)

    def test_other_apps_immediate_and_smarttube_wait_bounded(self):
        code = ADAPTER.read_text(encoding="utf-8")
        handoff = (ROOT / "launcher/app/src/main/java/com/gathin/tvhome/FrameCueHandoff.java").read_text(encoding="utf-8")
        self.assertIn("ctx.startActivity(intent, options.toBundle());", code)
        self.assertIn("else startNow.run();", code)
        self.assertIn("MAX_FRAME_WAIT_MS = 64L", handoff)
        self.assertNotIn("postDelayed(() -> startWithSilk", code)

    def test_sweep_keeps_55_percent_preset(self):
        code = MOTION.read_text(encoding="utf-8")
        self.assertIn("REFLECTION_INTENSITY = .55f", code)
        self.assertIn("phase < .68f ? 1f", code)


if __name__ == "__main__":
    unittest.main()
