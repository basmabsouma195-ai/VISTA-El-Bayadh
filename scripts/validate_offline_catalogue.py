#!/usr/bin/env python3
"""Validate VISTA's bundled offline catalogue and recognition asset index."""
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
CATALOGUE = ASSETS / "content/landmarks.json"
INDEX = ASSETS / "recognition/index.json"
errors = []

def load(path):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:
        errors.append(f"{path.relative_to(ROOT)}: cannot parse JSON: {exc}")
        return {}

data = load(CATALOGUE)
categories = {x.get("id") for x in data.get("categories", [])}
landmarks = data.get("landmarks", [])
ids = [x.get("id") for x in landmarks]
if len(ids) != len(set(ids)):
    errors.append("landmarks.json contains duplicate landmark IDs")
if not landmarks:
    errors.append("catalogue contains no landmarks")

for item in landmarks:
    lid = item.get("id", "<missing-id>")
    if not lid or item.get("categoryId") not in categories:
        errors.append(f"{lid}: missing ID or unknown categoryId")
    for key in ("nameAr", "nameFr", "nameEn", "descriptionAr", "descriptionFr", "descriptionEn"):
        if not isinstance(item.get(key), str) or not item[key].strip():
            errors.append(f"{lid}: missing {key}")
    if item.get("verification") not in ("verified", "oral_history", "under_review"):
        errors.append(f"{lid}: invalid verification status")
    lat, lon = item.get("latitude"), item.get("longitude")
    if (lat is None) != (lon is None):
        errors.append(f"{lid}: latitude/longitude must both be present or both be null")
    if lat is not None and not (-90 <= lat <= 90 and -180 <= lon <= 180):
        errors.append(f"{lid}: coordinates out of range")
    sources = item.get("sources", [])
    if not isinstance(sources, list) or not sources:
        errors.append(f"{lid}: no source record")
    else:
        for source in sources:
            if not str(source.get("url", "")).startswith(("https://", "http://")):
                errors.append(f"{lid}: source URL is missing or invalid")

index = load(INDEX)
seen = set()
for image in index.get("images", []):
    iid = image.get("id", "<missing-image-id>")
    if iid in seen:
        errors.append(f"recognition index contains duplicate image ID: {iid}")
    seen.add(iid)
    if image.get("landmarkId") not in set(ids):
        errors.append(f"{iid}: references unknown landmarkId")
    rel = pathlib.PurePosixPath(image.get("assetPath", ""))
    if not rel.parts or rel.is_absolute() or ".." in rel.parts or "://" in str(rel):
        errors.append(f"{iid}: unsafe or missing local assetPath")
        continue
    path = ASSETS.joinpath(*rel.parts)
    if not path.is_file() or path.stat().st_size < 1024:
        errors.append(f"{iid}: local image asset missing or too small: {rel}")

if errors:
    print("VISTA offline catalogue validation FAILED:")
    for error in errors:
        print(f" - {error}")
    sys.exit(1)

print(f"VISTA offline catalogue OK: {len(landmarks)} landmarks, "
      f"{len(categories)} categories, {len(seen)} local recognition photos.")
