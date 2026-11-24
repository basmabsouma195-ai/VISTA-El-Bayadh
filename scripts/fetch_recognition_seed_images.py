#!/usr/bin/env python3
"""Build-time downloader. The Android app uses only bundled local assets."""
import json
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
            headers={"User-Agent": USER_AGENT, "Accept": "image/*,*/*;q=0.8"},
        )
        try:
            with urllib.request.urlopen(request, timeout=60) as response:
                payload = response.read()
                content_type = response.headers.get("Content-Type", "")
            if not content_type.lower().startswith("image/"):
                raise RuntimeError(f"unexpected content type: {content_type}")
            if len(payload) < 1024:
                raise RuntimeError(f"download too small: {len(payload)} bytes")
            return payload
        except Exception as exc:
            last_error = exc
            if attempt < 3:
                time.sleep(2 ** attempt)
    raise last_error

def main():
    source = json.loads(SOURCE_MANIFEST.read_text(encoding="utf-8"))
    OUT.mkdir(parents=True, exist_ok=True)
    failures = []
    local_entries = []
    for item in source["images"]:
        destination = OUT / item["fileName"]
        try:
            payload = fetch(item["sourceFileUrl"])
            destination.write_bytes(payload)
            local_entries.append({
                "id": item["id"],
                "landmarkId": item["landmarkId"],
                "fileName": item["fileName"],
                "assetPath": "recognition/references/" + item["fileName"],
                "labelAr": item["labelAr"],
                "labelFr": item["labelFr"],
                "labelEn": item["labelEn"],
            })
            print(f"Bundled {item['id']}: {len(payload)} bytes")
        except Exception as exc:
            failures.append(f"{item['id']}: {exc}")
    if failures:
        print("\n".join(failures), file=sys.stderr)
        return 1
    runtime_index = {
        "schemaVersion": 1,
        "mode": "offline_local_assets_only",
        "images": local_entries,
    }
    RUNTIME_INDEX.write_text(
        json.dumps(runtime_index, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"Bundled {len(local_entries)} images into app assets for offline use.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
