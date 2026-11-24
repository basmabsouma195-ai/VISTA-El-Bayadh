#!/usr/bin/env python3
"""Fetch only reviewed, openly licensed VISTA landmark seed images."""
import json
import pathlib
import sys
import time
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "app/src/main/assets/recognition/dataset-manifest.json"
OUT = ROOT / "app/src/main/assets/recognition/references"
USER_AGENT = "VISTA-El-Bayadh-OpenImageDataset/1.0"

def fetch(url):
    last_error = None
    for attempt in range(4):
        request = urllib.request.Request(
            url,
            headers={
                "User-Agent": USER_AGENT,
                "Accept": "image/avif,image/webp,image/apng,image/*,*/*;q=0.8",
            },
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
    data = json.loads(MANIFEST.read_text(encoding="utf-8"))
    OUT.mkdir(parents=True, exist_ok=True)
    failures = []
    for item in data["images"]:
        destination = OUT / item["fileName"]
        try:
            payload = fetch(item["sourceFileUrl"])
            destination.write_bytes(payload)
            print(f"Fetched {item['id']}: {len(payload)} bytes [{item['license']}]")
        except Exception as exc:
            failures.append(f"{item['id']}: {exc}")
    if failures:
        print("\n".join(failures), file=sys.stderr)
        return 1
    print(f"Fetched {len(data['images'])} seed images. Dataset remains seed-only.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
