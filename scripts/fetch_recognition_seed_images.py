#!/usr/bin/env python3
"""Build-time local image packer. Remote downloads are opt-in only."""
import json
import pathlib
import os
import sys
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[1]
SOURCE_MANIFEST = ROOT / "dataset/recognition/source-manifest.json"
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
    return head.startswith(b"\xff\xd8\xff") or head.startswith(b"\x89PNG\r\n\x1a\n") or (head[:4] == b"RIFF" and head[8:12] == b"WEBP")

def main():
    source = json.loads(SOURCE_MANIFEST.read_text(encoding="utf-8"))
    OUT.mkdir(parents=True, exist_ok=True)
    allow_remote = os.environ.get("VISTA_FETCH_REMOTE_SEED_IMAGES", "").lower() == "true"
    local_entries = []
    pending = []
    for item in source["images"]:
        destination = OUT / item["fileName"]
        if not is_image(destination) and allow_remote:
            try:
                destination.write_bytes(fetch(item["sourceFileUrl"]))
            except Exception as exc:
                print(f"Remote seed unavailable for {item['id']}: {exc}", file=sys.stderr)
        if not is_image(destination):
            pending.append(item["id"])
            continue
        local_entries.append({
            "id": item["id"],
            "landmarkId": item["landmarkId"],
            "fileName": item["fileName"],
            "assetPath": "recognition/references/" + item["fileName"],
            "labelAr": item["labelAr"],
            "labelFr": item["labelFr"],
            "labelEn": item["labelEn"],
        })
        print(f"Bundled local image {item['id']}: {destination.stat().st_size} bytes")
    runtime_index = {
        "schemaVersion": 1,
        "mode": "offline_local_assets_only",
        "images": local_entries,
    }
    RUNTIME_INDEX.write_text(
        json.dumps(runtime_index, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"Bundled {len(local_entries)} verified local images.")
    if pending:
        print("Images awaiting approved local files: " + ", ".join(pending))
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
