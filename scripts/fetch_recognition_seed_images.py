#!/usr/bin/env python3
"""Build-time packer for licensed seed photos and staged user field photos."""
import json
import pathlib
import os
import sys
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[1]
SOURCE_MANIFEST = ROOT / "dataset/recognition/source-manifest.json"
FIELD_MANIFEST = ROOT / "dataset/field-photos/manifest.json"
FIELD_INCOMING = ROOT / "dataset/field-photos/incoming"
ASSET_ROOT = ROOT / "app/src/main/assets/recognition"
OUT = ASSET_ROOT / "references"
RUNTIME_INDEX = ASSET_ROOT / "index.json"
USER_AGENT = "VISTA-El-Bayadh-OpenImageDataset/1.0"

def fetch(url):
    request = urllib.request.Request(
        url, headers={"User-Agent": USER_AGENT, "Accept": "image/*,*/*;q=0.8"}
    )
    with urllib.request.urlopen(request, timeout=60) as response:
        payload = response.read()
        content_type = response.headers.get("Content-Type", "")
    if not content_type.lower().startswith("image/") or len(payload) < 1024:
        raise RuntimeError(f"invalid image response ({content_type}, {len(payload)} bytes)")
    return payload

def is_image(path):
    if not path.is_file() or path.stat().st_size < 1024:
        return False
    with path.open("rb") as stream:
        head = stream.read(12)
    return (head.startswith(b"\xff\xd8\xff") or
            head.startswith(b"\x89PNG\r\n\x1a\n") or
            (head[:4] == b"RIFF" and head[8:12] == b"WEBP"))

def add_entry(entries, item, file_path, landmark_id=None):
    entries.append({
        "id": item["id"],
        "landmarkId": landmark_id or item["landmarkId"],
        "fileName": item["fileName"],
        "assetPath": "recognition/references/" + item["fileName"],
        "labelAr": item.get("labelAr", ""),
        "labelFr": item.get("labelFr", ""),
        "labelEn": item.get("labelEn", ""),
    })
    print(f"Bundled {item['id']}: {file_path.stat().st_size} bytes")

def main():
    OUT.mkdir(parents=True, exist_ok=True)
    local_entries = []
    allow_remote = os.environ.get("VISTA_FETCH_REMOTE_SEED_IMAGES", "").lower() == "true"

    # Preserve references committed directly into the APK assets. The previous
    # packer regenerated index.json from downloaded seed manifests only, which
    # silently discarded reviewed local field references during CI builds.
    if RUNTIME_INDEX.exists():
        try:
            checked_in = json.loads(RUNTIME_INDEX.read_text(encoding="utf-8"))
            for item in checked_in.get("images", []):
                relative = pathlib.PurePosixPath(item.get("assetPath", ""))
                if (not relative.parts or relative.is_absolute() or
                        ".." in relative.parts or "://" in str(relative)):
                    continue
                asset_file = ROOT / "app/src/main/assets" / pathlib.Path(*relative.parts)
                if is_image(asset_file):
                    local_entries.append(item)
                    print(f"Preserved committed local reference {item.get('id')}")
        except (OSError, ValueError, TypeError) as exc:
            print(f"Could not read checked-in runtime index: {exc}", file=sys.stderr)

    if SOURCE_MANIFEST.exists():
        source = json.loads(SOURCE_MANIFEST.read_text(encoding="utf-8"))
        for item in source.get("images", []):
            destination = OUT / item["fileName"]
            if not is_image(destination) and allow_remote:
                try:
                    destination.write_bytes(fetch(item["sourceFileUrl"]))
                except Exception as exc:
                    print(f"Remote seed unavailable for {item['id']}: {exc}", file=sys.stderr)
            if is_image(destination):
                add_entry(local_entries, item, destination)

    if FIELD_MANIFEST.exists():
        field_data = json.loads(FIELD_MANIFEST.read_text(encoding="utf-8"))
        for item in field_data.get("images", []):
            landmark_id = item.get("landmarkId")
            # A provisional label is not enough for a runtime photo association.
            if not landmark_id or not item.get("runtimeApproved", False):
                continue
            staged = FIELD_INCOMING / item["sourceFileName"]
            if not is_image(staged):
                print(f"Field photo not staged yet: {item['sourceFileName']}")
                continue
            destination = OUT / item["fileName"]
            if not is_image(destination):
                # Keep original JPEG bytes under a .jpg extension if conversion is unavailable.
                destination = OUT / item["sourceFileName"]
                if not is_image(destination):
                    destination.write_bytes(staged.read_bytes())
                runtime_item = dict(item, fileName=item["sourceFileName"])
            else:
                runtime_item = item
            add_entry(local_entries, runtime_item, destination)

    # Avoid duplicate IDs or duplicate asset paths in the runtime index.
    deduped = {entry["id"]: entry for entry in local_entries}
    runtime_index = {
        "schemaVersion": 1,
        "mode": "offline_local_assets_only",
        "images": list(deduped.values()),
    }
    RUNTIME_INDEX.write_text(
        json.dumps(runtime_index, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"Bundled {len(deduped)} local reference images.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
