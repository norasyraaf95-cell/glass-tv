"""Source-level regression guard for the V8.4.2 reduced-motion bug.

The original V7.8.1-derived Config.load parsed reduced_motion correctly,
then accidentally passed a literal 'true' to its final Config constructor.
This test runs without Android and does not modify the launcher.
"""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
CONFIG = ROOT / "launcher/app/src/main/java/com/gathin/tvhome/Config.java"


class SatinConfigContractTest(unittest.TestCase):
    def test_json_boolean_is_respected(self):
        code = CONFIG.read_text(encoding="utf-8")
        self.assertIn(
            'reducedMotion = bool(json, "reduced_motion", reducedMotion);',
            code,
        )

    def test_effective_setting_is_passed_to_constructor(self):
        code = CONFIG.read_text(encoding="utf-8")
        self.assertRegex(
            code,
            re.compile(r"return new Config\(dock,.*?5,\s*0,\s*shamsi,\s*false,\s*reducedMotion\s*,",
                       re.DOTALL),
        )
        self.assertNotRegex(
            code,
            re.compile(r"return new Config\(dock,.*?5,\s*0,\s*shamsi,\s*false,\s*true\s*,",
                       re.DOTALL),
        )


if __name__ == "__main__":
    unittest.main()
