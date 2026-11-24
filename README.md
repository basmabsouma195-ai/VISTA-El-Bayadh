# VISTA El Bayadh

Android-first tourism guide for El Bayadh Province, Algeria.

## Product direction
- Arabic, French and English interface.
- Offline-first landmark catalogue and locally bundled media.
- Landmark camera recognition without requiring QR codes; QR remains an optional fallback.
- Audio guides, offline maps, saved places and itineraries are planned capabilities and must not be described as complete until implemented and tested.
- Private editorial tools for maintaining verified tourism content.

## Visual identity
The supplied VISTA reference boards are the visual authority. Preserve the deep-green, warm-gold and ivory palette, heritage photography direction, destination cards and bottom navigation. Do not replace the reference identity with a new concept.

## Android project
The Android application is in `app/`. Open the repository in Android Studio and sync Gradle.

## Current implementation status
The Android/Compose app, offline catalogue reader, SQLite-backed catalogue storage, CameraX preview, QR fallback, favourites and local search are present.

**Visual landmark recognition is not implemented yet.** The app currently has a recognition-engine contract and a conservative unavailable fallback. Bundled reference photographs and their local index are inputs for the future engine; their presence alone does not mean the camera can identify a landmark. Do not claim recognition is working until a real on-device matcher is integrated, tested against held-out images, and field-validated at the actual sites.

The current catalogue contains 23 records (content version 12): 8 marked `verified` and 15 marked `under_review`. These are editorial states, not a claim that all visitor information, access details or coordinates have been field-validated.

## Content integrity
Do not invent landmark history, coordinates, visitor ratings, opening hours or safety advice. Keep documented facts, oral accounts and unresolved research distinct. Retain source records and licensing/attribution details for every external image. User-supplied field photographs must not be assigned to a site until the location is confirmed.

## Offline recognition image assets
- `app/src/main/assets/recognition/index.json` describes the runtime references.
- `app/src/main/assets/recognition/references/` contains committed local references.
- `dataset/recognition/source-manifest.json` records licensed seed-image sources.
- `scripts/fetch_recognition_seed_images.py` downloads permitted seed images at build time and bundles them into the APK. The installed app does not need a network connection to read these bundled assets.
- `dataset/field-photos/manifest.json` tracks user field photographs; provisional associations are not automatically included in the runtime index.

Seed images are single reference views, not a complete recognition dataset. Expand them with confirmed field photographs from multiple angles, distances and lighting conditions. Keep image attributions available offline.

## Continuous Android build
Every push to `main` runs the Android debug assembly workflow. A successful run publishes the APK as the `vista-el-bayadh-debug` workflow artifact. A successful build confirms compilation and packaging only, not runtime or recognition accuracy.
