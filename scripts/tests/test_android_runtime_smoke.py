import importlib.util
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


if __name__ == "__main__":
    unittest.main()
