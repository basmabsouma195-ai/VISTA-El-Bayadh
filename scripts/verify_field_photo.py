#!/usr/bin/env python3
"""Approve one registered field photo after a human has checked identity and provenance."""
import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "dataset/recognition/field_dataset_manifest.json"


def fail(message):
    print(f"ERROR: {message}", file=sys.stderr)
    raise SystemExit(2)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--id", required=True, help="Registered image ID")
    parser.add_argument("--reviewer", required=True, help="Name or role of human reviewer")
    parser.add_argument("--note", required=True, help="Evidence supporting the landmark identity and source")
    args = parser.parse_args()
    reviewer, note = args.reviewer.strip(), args.note.strip()
    if not reviewer or not note:
        fail("Reviewer and verification note must not be empty")
    if len(note) < 12:
        fail("Verification note must contain at least 12 characters; record actual evidence")

    try:
        manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    except Exception as exc:
        fail(f"Cannot read manifest: {exc}")
    matches = [item for item in manifest.get("images", []) if item.get("id") == args.id]
    if len(matches) != 1:
        fail(f"Expected exactly one registered image with ID {args.id!r}")
    item = matches[0]
    if item.get("verified") is True or item.get("reviewStatus") == "approved":
        fail("Image is already approved; do not silently overwrite its review record")
    relative = Path(str(item.get("path", "")))
    if relative.is_absolute() or ".." in relative.parts:
        fail("Unsafe image path in manifest")
    path = (ROOT / relative).resolve()
    if ROOT.resolve() not in path.parents or not path.is_file():
        fail("Registered image file is missing or outside repository")
    actual_hash = hashlib.sha256(path.read_bytes()).hexdigest()
    if actual_hash != item.get("sha256"):
        fail("Image content changed since registration; register and review the current file again")

    item["verified"] = True
    item["reviewStatus"] = "approved"
    item["verifiedBy"] = reviewer
    item["verificationNote"] = note
    item["verifiedAt"] = datetime.now(timezone.utc).isoformat(timespec="seconds")
    MANIFEST.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Approved {args.id}.")
    print("The dataset validator will now count it only if all metadata and session rules pass.")
    print("This action records a human review; it does not itself prove model accuracy.")


if __name__ == "__main__":
    main()
