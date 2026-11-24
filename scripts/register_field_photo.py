#!/usr/bin/env python3
"""Register a field photo as pending review; never marks it verified."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "dataset/recognition/field_dataset_manifest.json"
IMAGE_SUFFIXES = {".jpg", ".jpeg", ".png", ".webp"}
ROLES = {"reference", "evaluation", "unknown"}


def fail(message):
    print(f"ERROR: {message}", file=sys.stderr)
    raise SystemExit(2)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--path", required=True, help="Image path relative to repository root")
    parser.add_argument("--id", required=True, help="Unique stable image ID")
    parser.add_argument("--role", required=True, choices=sorted(ROLES))
    parser.add_argument("--session", required=True, help="Capture visit/session ID")
    parser.add_argument("--source", required=True, help="Provenance, e.g. user field capture")
    parser.add_argument("--landmark-id", help="Required for reference/evaluation; omit for unknown")
    args = parser.parse_args()

    image_id = args.id.strip()
    session = args.session.strip()
    source = args.source.strip()
    if not re.fullmatch(r"[a-zA-Z0-9][a-zA-Z0-9._-]{1,95}", image_id):
        fail("ID must be 2-96 characters: letters, digits, dot, underscore, hyphen")
    if not session or not source:
        fail("Session and source must not be empty")
    if args.role in {"reference", "evaluation"} and not args.landmark_id:
        fail("reference/evaluation roles require --landmark-id")
    if args.role == "unknown" and args.landmark_id:
        fail("unknown role must not have --landmark-id")

    relative = Path(args.path)
    if relative.is_absolute() or ".." in relative.parts:
        fail("--path must be a safe repository-relative path")
    image_path = (ROOT / relative).resolve()
    if ROOT.resolve() not in image_path.parents:
        fail("Image path escapes repository root")
    if image_path.suffix.lower() not in IMAGE_SUFFIXES or not image_path.is_file():
        fail("Image must be an existing JPG, JPEG, PNG, or WEBP file")
    if not MANIFEST.is_file():
        fail("Recognition manifest is missing")
    try:
        manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    except Exception as exc:
        fail(f"Cannot parse manifest: {exc}")
    if manifest.get("schemaVersion") != 1 or not isinstance(manifest.get("images"), list):
        fail("Manifest must use schemaVersion=1 and an images array")
    if any(item.get("id") == image_id for item in manifest["images"]):
        fail(f"Image ID already exists: {image_id}")
    if any(item.get("path") == relative.as_posix() for item in manifest["images"]):
        fail(f"Image path already registered: {relative.as_posix()}")

    entry = {
        "id": image_id,
        "path": relative.as_posix(),
        "role": args.role,
        "captureSessionId": session,
        "source": source,
        "sha256": hashlib.sha256(image_path.read_bytes()).hexdigest(),
        "verified": False,
        "reviewStatus": "pending",
    }
    if args.landmark_id:
        entry["landmarkId"] = args.landmark_id.strip()
    manifest["images"].append(entry)
    MANIFEST.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Registered {image_id} as pending review.")
    print("verified=false; this image is not counted by the validator and is not added to the APK.")
    print(f"SHA-256: {entry['sha256']}")


if __name__ == "__main__":
    main()
