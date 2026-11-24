import contextlib
import importlib.util
import io
import json
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).resolve().parents[1] / "scripts/validate_recognition_dataset.py"
SPEC = importlib.util.spec_from_file_location("dataset_validator", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class DatasetValidatorContractTests(unittest.TestCase):
    def test_thresholds_are_conservative(self):
        self.assertEqual(MODULE.MIN_REFERENCES, 20)
        self.assertEqual(MODULE.MIN_EVALUATION, 10)
        self.assertEqual(MODULE.MIN_UNKNOWN, 100)

    def test_empty_manifest_keeps_gate_closed_without_failing_ci(self):
        with tempfile.TemporaryDirectory() as directory:
            manifest = Path(directory) / "manifest.json"
            manifest.write_text(json.dumps({"schemaVersion": 1, "images": []}), encoding="utf-8")
            original = MODULE.MANIFEST
            MODULE.MANIFEST = manifest
            output = io.StringIO()
            try:
                with contextlib.redirect_stdout(output):
                    self.assertEqual(MODULE.main(), 0)
            finally:
                MODULE.MANIFEST = original
            self.assertIn("RECOGNITION GATE: NOT READY", output.getvalue())

    def test_unverified_entries_do_not_count_toward_readiness(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            image = root / "sample.jpg"
            image.write_bytes(b"test-image")
            manifest = root / "manifest.json"
            manifest.write_text(json.dumps({
                "schemaVersion": 1,
                "images": [{
                    "id": "pending-1", "path": "sample.jpg", "role": "reference",
                    "landmarkId": "test-site", "captureSessionId": "visit-1",
                    "verified": False
                }]
            }), encoding="utf-8")
            old_root, old_manifest = MODULE.ROOT, MODULE.MANIFEST
            MODULE.ROOT, MODULE.MANIFEST = root, manifest
            output = io.StringIO()
            try:
                with contextlib.redirect_stdout(output):
                    self.assertEqual(MODULE.main(), 0)
            finally:
                MODULE.ROOT, MODULE.MANIFEST = old_root, old_manifest
            self.assertIn("Verified references: 0", output.getvalue())
            self.assertIn("RECOGNITION GATE: NOT READY", output.getvalue())


if __name__ == "__main__":
    unittest.main()
