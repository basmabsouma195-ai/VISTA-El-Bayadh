# VISTA El Bayadh

Android-first tourism guide for El Bayadh Province, Algeria.

## Product direction
- Arabic, French and English interface.
- Offline-first landmark encyclopedia and locally stored media.
- Interactive offline map backed by bundled regional map data.
- Landmark camera recognition that does not depend on QR codes.
- Audio guides in the three supported languages.
- Saved places and custom itineraries.
- Private administration tools for managing verified content and later publishing content updates.

## Visual identity
The supplied VISTA reference boards are the visual authority. The implementation follows their deep-green, warm-gold and ivory palette, heritage photography direction, rounded destination cards and bottom navigation. Do not replace the reference identity with a new concept.

## Android project
The Android application is in `app/`. Open the repository in Android Studio and sync Gradle.

## Current implementation status
This first repository foundation establishes the Android/Compose project and the initial navigable interface. It does **not** claim that offline maps, camera recognition, audio downloads, accounts, moderation or the content-update service are already implemented. Those capabilities require their own tested modules and real province data.

## Content integrity
Do not invent landmark history, coordinates, visitor ratings, opening hours or safety advice. Each published record must carry an internal editorial status: documented, oral account, or under investigation. Source records remain in the private administration workflow rather than being displayed as dated source links to visitors.

## Offline catalogue foundation

The app now has a typed landmark model and an asset-backed catalogue reader:
- `app/src/main/java/dz/vista/elbayadh/data/Landmark.kt`
- `app/src/main/java/dz/vista/elbayadh/data/OfflineCatalogue.kt`
- `app/src/main/assets/content/landmarks.json`

The catalogue envelope includes schema/content versions, the province identity,
categories and landmark records. The current list is deliberately empty until
the province's records, coordinates, images and audio are sourced and reviewed.
See `CONTENT_CATALOGUE.md` before adding records.

## Continuous Android build

Every push to `main` runs the Android debug assembly workflow. A successful run
publishes the APK as the `vista-el-bayadh-debug` workflow artifact.
