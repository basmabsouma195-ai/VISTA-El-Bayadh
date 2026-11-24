#!/usr/bin/env python3
"""Validate the VISTA field dataset and report whether recognition gates are met."""
from collections import defaultdict
from pathlib import Path
import hashlib
import json
import sys

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "dataset/recognition/field_dataset_manifest.json"
MIN_REFERENCES = 20
MIN_EVALUATION = 10
MIN_UNKNOWN = 100


def fail(message):
    print(f"ERROR: {message}", file=sys.stderr)
    raise SystemExit(2)


def main():
    if not MANIFEST.is_file():
        fail(f"Missing manifest: {MANIFEST.relative_to(ROOT)}")
    try:
        data = json.loads(MANIFEST.read_text(encoding="utf-8"))
    except Exception as exc:
        fail(f"Invalid JSON: {exc}")
    if data.get("schemaVersion") != 1 or not isinstance(data.get("images"), list):
        fail("Manifest must use schemaVersion=1 and an images array")

    ids = set()
    references = defaultdict(list)
    evaluations = defaultdict(list)
    unknowns = []
    for item in data["images"]:
        if not isinstance(item, dict):
            fail("Every image entry must be an object")
        image_id = str(item.get("id", "")).strip()
        if not image_id or image_id in ids:
            fail(f"Missing or duplicate image id: {image_id!r}")
        ids.add(image_id)
        role = item.get("role")
        path_text = str(item.get("path", "")).strip()
        if not path_text:
            fail(f"{image_id}: missing path")
        path = (ROOT / path_text).resolve()
        if ROOT.resolve() not in path.parents:
            fail(f"{image_id}: path escapes repository root")
        if not path.is_file():
            fail(f"{image_id}: file does not exist: {path_text}")
        if item.get("verified") is not True:
            # Unverified entries are retained in the manifest but never counted.
            continue
        session = str(item.get("captureSessionId", "")).strip()
        if not session:
            fail(f"{image_id}: verified image requires captureSessionId")
        source = str(item.get("source", "")).strip()
        if not source:
            fail(f"{image_id}: verified image requires source provenance")
        expected_hash = str(item.get("sha256", "")).strip().lower()
        if len(expected_hash) != 64 or any(ch not in "0123456789abcdef" for ch in expected_hash):
            fail(f"{image_id}: verified image requires a valid SHA-256")
        actual = hashlib.sha256(path.read_bytes()).hexdigest()
        if actual != expected_hash:
            fail(f"{image_id}: SHA-256 mismatch")
        if role == "reference":
            landmark = str(item.get("landmarkId", "")).strip()
            if not landmark:
                fail(f"{image_id}: reference requires landmarkId")
            references[landmark].append((session, image_id))
        elif role == "evaluation":
            landmark = str(item.get("landmarkId", "")).strip()
            if not landmark:
                fail(f"{image_id}: evaluation requires landmarkId")
            evaluations[landmark].append((session, image_id))
        elif role == "unknown":
            unknowns.append((session, image_id))
        else:
            fail(f"{image_id}: role must be reference, evaluation, or unknown")

    blockers = []
    supported = sorted(set(references) | set(evaluations))
    if not supported:
        blockers.append("No verified reference or evaluation images yet.")
    for landmark in supported:
        refs = references[landmark]
        tests = evaluations[landmark]
        ref_sessions = {session for session, _ in refs}
        eval_sessions = {session for session, _ in tests}
        if len(refs) < MIN_REFERENCES:
            blockers.append(f"{landmark}: {len(refs)}/{MIN_REFERENCES} reference images.")
        if len(tests) < MIN_EVALUATION:
            blockers.append(f"{landmark}: {len(tests)}/{MIN_EVALUATION} held-out evaluation images.")
        if ref_sessions & eval_sessions:
            blockers.append(f"{landmark}: reference/evaluation capture sessions overlap.")
    if len(unknowns) < MIN_UNKNOWN:
        blockers.append(f"Unknown scenes: {len(unknowns)}/{MIN_UNKNOWN} verified held-out images.")

    print(f"Verified landmarks: {len(supported)}")
    print(f"Verified references: {sum(map(len, references.values()))}")
    print(f"Verified held-out landmark images: {sum(map(len, evaluations.values()))}")
    print(f"Verified held-out unknown scenes: {len(unknowns)}")
    if blockers:
        print("RECOGNITION GATE: NOT READY")
        for blocker in blockers:
            print(f"- {blocker}")
    else:
        print("RECOGNITION GATE: DATASET COUNTS AND SESSION SEPARATION PASS")
        print("Note: accuracy, false-accept rate, calibration, and target-device performance still require evaluation.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
