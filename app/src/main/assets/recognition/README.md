# VISTA recognition assets: offline guarantee

The Android application must never request landmark photographs from Wikimedia or any other remote image host while a visitor is using it.

## Two separate layers

- `dataset/recognition/source-manifest.json` is a development-only provenance file. It stores source URLs, license terms, and attribution metadata.
- `app/src/main/assets/recognition/index.json` is the runtime image-reference index. It contains only local asset paths and landmark identifiers; it contains no network URLs.
- `app/src/main/assets/recognition/references/` contains image files bundled into the APK.
- The offline catalogue points image-bearing landmark cards at local asset paths. Missing files are not displayed; the UI never falls back to remote URLs.

## Build behaviour

The normal Android workflow is local-first and does not contact image hosts. It packages only verified image files already present in `app/src/main/assets/recognition/references/`, then generates the local runtime index. CI caches that directory between builds.

The optional downloader in `scripts/fetch_recognition_seed_images.py` runs only when `VISTA_FETCH_REMOTE_SEED_IMAGES=true` is explicitly supplied. It is not enabled in the normal workflow because Wikimedia rate-limits repeated CI requests. When enabled, it validates downloaded image signatures and fails packaging if any declared seed image remains unavailable.

Once a photo is included in the APK, viewing it requires no internet connection.

## Important limitations

The current seed list is not a trained recognition dataset. The scanner must not claim recognition until a real on-device model and a validated reference index are bundled and tested. No runtime network fallback is permitted for core landmark content.

The user-provided field photographs still need to be added as local files and mapped to their confirmed landmarks before they can appear in the installed app.
