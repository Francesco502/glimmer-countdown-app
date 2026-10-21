import importlib.util
import json
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location(
    "android_runtime_smoke", Path(__file__).resolve().parents[1] / "android-runtime-smoke.py"
)
smoke_module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(smoke_module)


class InputMethodRecoveryTest(unittest.TestCase):
    def probe(self, responses):
        smoke = object.__new__(smoke_module.Smoke)
        calls = []
        remaining = iter(responses)

        def text(*args):
            calls.append(args)
            response = next(remaining)
            if isinstance(response, Exception):
                raise response
            return response

        smoke.text = text
        smoke.run = lambda *args, **kwargs: calls.append((args, kwargs))
        return smoke, calls

    def test_reconnect_repeats_only_the_read_only_probe(self):
        smoke, calls = self.probe([
            smoke_module.SmokeFailure("offline"),
            smoke_module.SmokeFailure("transport interrupted"),
            "mInputShown=false",
        ])
        self.assertEqual("mInputShown=false", smoke.input_method_state())
        self.assertEqual([
            ("shell", "dumpsys", "input_method"),
            (("wait-for-device",), {"timeout": 15}),
            ("shell", "dumpsys", "input_method"),
            (("wait-for-device",), {"timeout": 15}),
            ("shell", "dumpsys", "input_method"),
        ], calls)

    def test_persistent_failure_still_fails_after_three_reads(self):
        smoke, calls = self.probe([smoke_module.SmokeFailure("offline")] * 3)
        with self.assertRaisesRegex(smoke_module.SmokeFailure, "offline"):
            smoke.input_method_state()
        self.assertEqual(5, len(calls))


class ShareMediaCompatibilityTest(unittest.TestCase):
    def test_modern_android_requires_published_media_and_expected_location(self):
        row = {"mime_type": "image/png", "relative_path": "Pictures/TimeAPK/", "is_pending": "1"}
        self.assertFalse(smoke_module.share_media_published(row))
        row["is_pending"] = "0"
        self.assertTrue(smoke_module.share_media_published(row))
        row["relative_path"] = "Pictures/Other"
        with self.assertRaises(smoke_module.SmokeFailure):
            smoke_module.share_media_published(row)


class UpgradeVersionTest(unittest.TestCase):
    def test_upgrade_requires_422_baseline_and_423_candidate(self):
        self.assertEqual(("4.2.2", "27"), smoke_module.UPGRADE_BASELINE)
        self.assertEqual(("4.2.3", "28"), smoke_module.UPGRADE_CANDIDATE)
        for expected in (smoke_module.UPGRADE_BASELINE, smoke_module.UPGRADE_CANDIDATE):
            smoke_module.require_version(dict(versionName=expected[0], versionCode=expected[1]), expected)
        for name, code in (("4.2", "25"), ("4.2.2", "27"), ("4.2.3", "27"), ("4.2.2", "28")):
            with self.subTest(name=name, code=code), self.assertRaises(smoke_module.SmokeFailure):
                smoke_module.require_version(dict(versionName=name, versionCode=code), smoke_module.UPGRADE_CANDIDATE)

    def test_workflow_pins_the_published_422_baseline(self):
        root = Path(__file__).resolve().parents[2]
        workflow = (root / ".github/workflows/android-apk-qa.yml").read_text(encoding="utf-8")
        verification = json.loads((root / "docs/releases/v4.2.2-verification.json").read_text(encoding="utf-8"))
        self.assertIn("verify_apk baseline 4.2.2 27", workflow)
        self.assertIn("verify_apk candidate 4.2.3 28", workflow)
        self.assertIn("releases/download/v4.2.2/glimmer-countdown-4-2-2.apk", workflow)
        self.assertIn(verification["signedApk"]["sha256"], workflow)
        self.assertIn(str(verification["signedApk"]["size"]), workflow)


if __name__ == "__main__":
    unittest.main()
