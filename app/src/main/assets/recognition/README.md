# VISTA recognition assets: offline guarantee

The Android application must never request landmark photographs from Wikimedia or any other remote image host while a visitor is using it.

## Two separate layers

- `dataset/recognition/source-manifest.json` is a build/development-only provenance file. It stores original source URLs, license terms, and attribution metadata.
- `app/src/main/assets/recognition/index.json` is the runtime image-reference index. It contains only local asset paths and landmark identifiers; it contains no network URLs.
- `app/src/main/assets/recognition/references/` contains the image files bundled into the APK.
- The offline catalogue points image-bearing landmark cards at these local asset paths. Missing images are simply not shown; the UI does not fall back to remote URLs.

The CI script `scripts/fetch_recognition_seed_images.py` downloads source images during APK packaging, validates the downloaded file signatures, and writes the local runtime index. CI caches the downloaded reference directory between builds. The app reads bundled files from Android assets. Once installed, viewing the included images does not require internet access.

## Important limitations

This is a small seed collection, not a trained recognition dataset. The scanner must not claim recognition until a real on-device model and a validated reference index are bundled and tested. No runtime network fallback is permitted for core landmark content.

If CI cannot retrieve a required source image, packaging fails rather than silently producing an incomplete offline package.
