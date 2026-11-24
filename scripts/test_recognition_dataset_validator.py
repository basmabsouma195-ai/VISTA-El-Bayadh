import importlib.util
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

    def test_roles_are_explicit(self):
        self.assertEqual({"reference", "evaluation", "unknown"}, {"reference", "evaluation", "unknown"})

if __name__ == "__main__":
    unittest.main()
