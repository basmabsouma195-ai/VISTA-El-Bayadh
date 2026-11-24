import contextlib
import hashlib
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
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.image = self.root / "sample.jpg"
        self.image.write_bytes(b"unique-test-image")
        self.manifest = self.root / "manifest.json"
        self.old_root, self.old_manifest = MODULE.ROOT, MODULE.MANIFEST
        MODULE.ROOT, MODULE.MANIFEST = self.root, self.manifest

    def tearDown(self):
        MODULE.ROOT, MODULE.MANIFEST = self.old_root, self.old_manifest
        self.temp.cleanup()

    def write_manifest(self, images):
        self.manifest.write_text(json.dumps({"schemaVersion": 1, "images": images}), encoding="utf-8")

    def pending(self, image_id="pending-1", path="sample.jpg", **extra):
        entry = {
            "id": image_id, "path": path, "role": "reference",
            "landmarkId": "test-site", "captureSessionId": "visit-1",
            "source": "documented field capture",
            "sha256": hashlib.sha256(self.image.read_bytes()).hexdigest(),
            "verified": False, "reviewStatus": "pending",
        }
        entry.update(extra)
        return entry

    def run_validator(self):
        output = io.StringIO()
        with contextlib.redirect_stdout(output):
            self.assertEqual(MODULE.main(), 0)
        return output.getvalue()

    def test_thresholds_are_conservative(self):
        self.assertEqual(MODULE.MIN_REFERENCES, 20)
        self.assertEqual(MODULE.MIN_EVALUATION, 10)
        self.assertEqual(MODULE.MIN_UNKNOWN, 100)

    def test_empty_manifest_keeps_gate_closed_without_failing_ci(self):
        self.write_manifest([])
        output = self.run_validator()
        self.assertIn("RECOGNITION GATE: NOT READY", output)

    def test_pending_entry_is_valid_but_never_counted(self):
        self.write_manifest([self.pending()])
        output = self.run_validator()
        self.assertIn("Pending/unverified images: 1", output)
        self.assertIn("Verified references: 0", output)
        self.assertIn("RECOGNITION GATE: NOT READY", output)

    def test_modified_file_fails_hash_integrity(self):
        entry = self.pending()
        self.write_manifest([entry])
        self.image.write_bytes(b"changed-after-registration")
        with self.assertRaises(SystemExit):
            self.run_validator()

    def test_duplicate_content_is_rejected_even_with_different_ids(self):
        duplicate = self.root / "copy.jpg"
        duplicate.write_bytes(self.image.read_bytes())
        second = self.pending("pending-2", "copy.jpg")
        self.write_manifest([self.pending(), second])
        with self.assertRaises(SystemExit):
            self.run_validator()

    def test_invalid_role_is_rejected_even_while_pending(self):
        self.write_manifest([self.pending(role="mystery")])
        with self.assertRaises(SystemExit):
            self.run_validator()

    def test_verified_photo_requires_human_review_audit(self):
        entry = self.pending(verified=True, reviewStatus="approved")
        self.write_manifest([entry])
        with self.assertRaises(SystemExit):
            self.run_validator()


if __name__ == "__main__":
    unittest.main()
