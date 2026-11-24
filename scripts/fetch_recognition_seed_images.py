#!/usr/bin/env python3
"""Build-time image packer; the Android app reads only bundled local assets."""
import json
import os
import pathlib
import sys
import time
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[1]
SOURCE_MANIFEST = ROOT / "dataset/recognition/source-manifest.json"
ASSET_ROOT = ROOT / "app/src/main/assets/recognition"
OUT = ASSET_ROOT / "references"
RUNTIME_INDEX = ASSET_ROOT / "index.json"
USER_AGENT = "VISTA-El-Bayadh-OpenImageDataset/1.0"

def fetch(url):
    last_error = None
    for attempt in range(4):
        request = urllib.request.Request(
            url,
            headers={"User-Agent": USER_AGENT, "Accept": "image/avif,image/webp,image/apng,image/*,*/*;q=0.8"},
        )
        try:
            with urllib.request.urlopen(request, timeout=20) as response:
                payload = response.read()
                content_type = response.headers.get("Content-Type", "")
            if not content_type.lower().startswith("image/") or len(payload) < 1024:
                raise RuntimeError(f"invalid image response ({content_type}, {len(payload)} bytes)")
            return payload
        except Exception as exc:
            last_error = exc
            if attempt < 3:
                delay = min(30, 2 ** attempt)
                print(f"Download attempt {attempt + 1} failed; retrying in {delay}s: {exc}", file=sys.stderr)
                time.sleep(delay)
    raise last_error

def is_image(path):
    if not path.is_file() or path.stat().st_size < 1024:
        return False
    with path.open("rb") as stream:
        head = stream.read(12)
    return (
        head.startswith(b"\xff\xd8\xff")
        or head.startswith(b"\x89PNG\r\n\x1a\n")
        or (head[:4] == b"RIFF" and head[8:12] == b"WEBP")
    )

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
                payload = fetch(item["sourceFileUrl"])
                temporary = destination.with_suffix(destination.suffix + ".tmp")
                temporary.write_bytes(payload)
                if not is_image(temporary):
                    temporary.unlink(missing_ok=True)
                    raise RuntimeError("downloaded file is not a supported JPEG, PNG or WebP image")
                temporary.replace(destination)
            except Exception as exc:
                print(f"Could not bundle {item['id']}: {exc}", file=sys.stderr)
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
    print(f"Bundled {len(local_entries)} of {len(source['images'])} declared seed images.")
    if pending:
        print("Required images missing: " + ", ".join(pending), file=sys.stderr)
        if allow_remote:
            return 1
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
