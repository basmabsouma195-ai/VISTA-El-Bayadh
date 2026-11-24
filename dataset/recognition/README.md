# Field photo collection for VISTA

This folder is the controlled source of field photographs used to validate landmark recognition.

## Capture procedure

For each supported landmark, capture at least 20 reference photos across multiple visits:
- front, side, rear, and distinctive structural details;
- near, medium, and far distances;
- daylight, shade, and different weather where possible;
- avoid burst duplicates and avoid framing only the signboard.

Capture at least 10 separate-session evaluation photos for the same landmark. Do not reuse a capture session between reference and evaluation roles. Capture at least 100 unknown scenes (ordinary streets, unrelated ksour, mountains, buildings, and ambiguous views) to test rejection.

## Add an image

1. Put the original image under `dataset/recognition/images/`.
2. Calculate its SHA-256: `sha256sum path/to/image.jpg`.
3. Add one entry to `field_dataset_manifest.json` with a unique ID, relative path, role, landmark ID where applicable, capture session ID, source/provenance, SHA-256, and `verified: false`.
4. Have a human reviewer verify the site identity and provenance. Only then change `verified` to `true`.
5. Never use an evaluation image as a reference image. Keep separate visit/session IDs.
6. Run `python3 scripts/validate_recognition_dataset.py`.

An image marked verified is eligible for dataset counts only. It is **not automatically copied into the APK**. Runtime inclusion is a separate explicit review step in the curated runtime index and must not be automated from this manifest.

## Recognition activation

The validator reports counts and session overlap. Passing those checks does not prove recognition accuracy. The model thresholds still require held-out calibration, unknown-scene false-accept testing, and performance testing on the target Android phone.
