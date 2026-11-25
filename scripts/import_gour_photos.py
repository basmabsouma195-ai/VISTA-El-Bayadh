#!/usr/bin/env python3
"""Import user-uploaded Gour Bent El Khass photos without renaming originals.

Drop JPG/JPEG/PNG/WEBP files into:
dataset/field-photos/gour-bent-el-khass/

The importer copies them byte-for-byte into APK assets and registers them as
gallery photos plus pending recognition references. It never auto-verifies
or enables them for inference.
"""
import hashlib
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
INBOX = ROOT / "dataset/field-photos/gour-bent-el-khass"
ASSET_DIR = ROOT / "app/src/main/assets/field-photos/gour-bent-el-khass/landscape"
GALLERY = ROOT / "app/src/main/assets/content/field-photo-gallery.json"
DATASET = ROOT / "dataset/recognition/field_dataset_manifest.json"
ALLOWED = {".jpg", ".jpeg", ".png", ".webp"}
LANDMARK_ID = "gour-bent-el-khass"
SESSION_ID = "gour-bent-el-khass-upload-batch"


def load_json(path):
    return json.loads(path.read_text(encoding="utf-8"))


def save_json(path, data):
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main():
    if not INBOX.is_dir():
        print("No Gour upload folder found; nothing to import.")
        return
    files = sorted(p for p in INBOX.iterdir() if p.is_file() and p.suffix.lower() in ALLOWED)
    if not files:
        print("No JPG/JPEG/PNG/WEBP files found in the Gour upload folder.")
        return

    gallery = load_json(GALLERY)
    dataset = load_json(DATASET)
    gallery_images = gallery.setdefault("images", [])
    dataset_images = dataset.setdefault("images", [])
    known_paths = {item.get("assetPath") for item in gallery_images}
    known_dataset_paths = {item.get("path") for item in dataset_images}
    ASSET_DIR.mkdir(parents=True, exist_ok=True)
    imported = 0

    for source in files:
        digest = hashlib.sha256(source.read_bytes()).hexdigest()
        destination = ASSET_DIR / source.name
        relative_asset = destination.relative_to(ROOT / "app/src/main/assets").as_posix()
        repo_path = source.relative_to(ROOT).as_posix()
        if destination.exists() and hashlib.sha256(destination.read_bytes()).hexdigest() != digest:
            raise SystemExit(f"Filename collision with different image bytes: {source.name}")
        if not destination.exists():
            shutil.copy2(source, destination)

        image_id = "gour-bent-el-khass-" + digest[:16]
        if relative_asset not in known_paths:
            gallery_images.append({
                "id": "field-gallery-" + image_id,
                "landmarkId": LANDMARK_ID,
                "sourceFileName": source.name,
                "assetPath": relative_asset,
                "imageType": "landscape",
                "locationStatus": "user_confirmed_location",
                "galleryEnabled": True,
                "recognitionApproved": False
            })
            known_paths.add(relative_asset)

        if repo_path not in known_dataset_paths:
            dataset_images.append({
                "id": image_id,
                "path": repo_path,
                "role": "reference",
                "landmarkId": LANDMARK_ID,
                "captureSessionId": SESSION_ID,
                "source": "user-uploaded Gour Bent El Khass photo; pending review",
                "sha256": digest,
                "verified": False,
                "reviewStatus": "pending"
            })
            known_dataset_paths.add(repo_path)
        imported += 1

    save_json(GALLERY, gallery)
    save_json(DATASET, dataset)
    print(f"Imported {imported} Gour photos with original filenames.")
    print("Gallery: enabled. Recognition: pending review, not auto-approved.")


if __name__ == "__main__":
    main()
