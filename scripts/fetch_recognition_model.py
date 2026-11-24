#!/usr/bin/env python3
"""Fetch the pinned offline MobileNetV3 image-embedding model for Android builds."""
from pathlib import Path
from urllib.request import Request, urlopen
import hashlib
import os
import sys
import time

URL = (
    "https://huggingface.co/litert-community/"
    "MediaPipe-ImageEmbedder-MobileNet-V3-Small/resolve/main/mobilenet_v3_small.tflite"
)
EXPECTED_SHA256 = "bbbb4c51a55a53905af1daec995ca1aae355046f8839bb8c9f5ce9271394bc40"
EXPECTED_SIZE = 4_117_670
DESTINATION = Path("app/src/main/assets/recognition/mobilenet_v3_small.tflite")


def valid(path: Path) -> bool:
    if not path.is_file() or path.stat().st_size != EXPECTED_SIZE:
        return False
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    return digest == EXPECTED_SHA256


def main() -> int:
    DESTINATION.parent.mkdir(parents=True, exist_ok=True)
    if valid(DESTINATION):
        print(f"Verified cached model: {DESTINATION}")
        return 0
    DESTINATION.unlink(missing_ok=True)
    if os.environ.get("VISTA_FETCH_RECOGNITION_MODEL", "true").lower() == "false":
        print("Model fetch disabled and no verified cached model exists.", file=sys.stderr)
        return 1

    last_error = None
    for attempt in range(1, 4):
        temporary = DESTINATION.with_suffix(".tflite.part")
        try:
            request = Request(URL, headers={"User-Agent": "VISTA-El-Bayadh-Android-Build"})
            with urlopen(request, timeout=90) as response, temporary.open("wb") as output:
                while True:
                    chunk = response.read(1024 * 1024)
                    if not chunk:
                        break
                    output.write(chunk)
            if not valid(temporary):
                raise ValueError("Downloaded model failed size or SHA-256 verification")
            temporary.replace(DESTINATION)
            print(f"Downloaded and verified model: {DESTINATION}")
            return 0
        except Exception as error:
            last_error = error
            temporary.unlink(missing_ok=True)
            if attempt < 3:
                time.sleep(attempt * 3)
    print(f"Could not obtain verified recognition model: {last_error}", file=sys.stderr)
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
