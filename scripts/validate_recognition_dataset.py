#!/usr/bin/env python3
"""Validate VISTA field photos and report whether recognition dataset gates are met."""
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
IMAGE_SUFFIXES = {".jpg", ".jpeg", ".png", ".webp"}
ROLES = {"reference", "evaluation", "unknown"}


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

    ids, paths, hashes = set(), set(), {}
    references, evaluations = defaultdict(list), defaultdict(list)
    unknowns = []
    for item in data["images"]:
        if not isinstance(item, dict):
            fail("Every image entry must be an object")
        image_id = str(item.get("id", "")).strip()
        if not image_id or image_id in ids:
            fail(f"Missing or duplicate image id: {image_id!r}")
        ids.add(image_id)

        role = item.get("role")
        if role not in ROLES:
            fail(f"{image_id}: role must be reference, evaluation, or unknown")
        path_text = str(item.get("path", "")).strip()
        if not path_text:
            fail(f"{image_id}: missing path")
        relative = Path(path_text)
        if relative.is_absolute() or ".." in relative.parts:
            fail(f"{image_id}: path must be repository-relative and cannot traverse parents")
        if relative.suffix.lower() not in IMAGE_SUFFIXES:
            fail(f"{image_id}: unsupported image extension")
        if relative.as_posix() in paths:
            fail(f"{image_id}: duplicate image path")
        paths.add(relative.as_posix())
        path = (ROOT / relative).resolve()
        if ROOT.resolve() not in path.parents:
            fail(f"{image_id}: path escapes repository root")
        if not path.is_file():
            fail(f"{image_id}: file does not exist: {path_text}")

        session = str(item.get("captureSessionId", "")).strip()
        source = str(item.get("source", "")).strip()
        landmark = str(item.get("landmarkId", "")).strip()
        if not session:
            fail(f"{image_id}: captureSessionId is required")
        if not source:
            fail(f"{image_id}: source provenance is required")
        if role in {"reference", "evaluation"} and not landmark:
            fail(f"{image_id}: {role} requires landmarkId")
        if role == "unknown" and landmark:
            fail(f"{image_id}: unknown scenes must not have landmarkId")

        expected_hash = str(item.get("sha256", "")).strip().lower()
        if len(expected_hash) != 64 or any(ch not in "0123456789abcdef" for ch in expected_hash):
            fail(f"{image_id}: a valid SHA-256 is required, including for pending review")
        actual_hash = hashlib.sha256(path.read_bytes()).hexdigest()
        if actual_hash != expected_hash:
            fail(f"{image_id}: SHA-256 mismatch; file changed after registration")
        if actual_hash in hashes:
            fail(f"{image_id}: duplicate image content also registered as {hashes[actual_hash]}")
        hashes[actual_hash] = image_id

        if item.get("verified") is not True:
            if item.get("reviewStatus") not in (None, "pending", "rejected"):
                fail(f"{image_id}: unverified image must be pending or rejected")
            continue
        if item.get("reviewStatus") != "approved":
            fail(f"{image_id}: verified image requires reviewStatus=approved")
        if not str(item.get("verifiedBy", "")).strip():
            fail(f"{image_id}: verified image requires verifiedBy")
        if not str(item.get("verificationNote", "")).strip():
            fail(f"{image_id}: verified image requires a verificationNote")

        if role == "reference":
            references[landmark].append((session, image_id))
        elif role == "evaluation":
            evaluations[landmark].append((session, image_id))
        else:
            unknowns.append((session, image_id))

    blockers = []
    supported = sorted(set(references) | set(evaluations))
    if not supported:
        blockers.append("No verified reference or evaluation images yet.")
    all_reference_sessions = {s for entries in references.values() for s, _ in entries}
    all_evaluation_sessions = {s for entries in evaluations.values() for s, _ in entries}
    for landmark in supported:
        refs, tests = references[landmark], evaluations[landmark]
        ref_sessions = {session for session, _ in refs}
        eval_sessions = {session for session, _ in tests}
        if len(refs) < MIN_REFERENCES:
            blockers.append(f"{landmark}: {len(refs)}/{MIN_REFERENCES} reference images.")
        if len(tests) < MIN_EVALUATION:
            blockers.append(f"{landmark}: {len(tests)}/{MIN_EVALUATION} held-out evaluation images.")
        if ref_sessions & eval_sessions:
            blockers.append(f"{landmark}: reference/evaluation capture sessions overlap.")
    unknown_sessions = {session for session, _ in unknowns}
    if unknown_sessions & (all_reference_sessions | all_evaluation_sessions):
        blockers.append("Unknown-scene sessions must be separate from all landmark reference/evaluation sessions.")
    if len(unknowns) < MIN_UNKNOWN:
        blockers.append(f"Unknown scenes: {len(unknowns)}/{MIN_UNKNOWN} verified held-out images.")

    pending = sum(1 for item in data["images"] if item.get("verified") is not True and item.get("reviewStatus") != "rejected")
    print(f"Registered images: {len(data['images'])}")
    print(f"Pending/unverified images: {pending}")
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
