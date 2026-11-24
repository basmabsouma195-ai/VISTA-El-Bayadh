# VISTA — offline landmark recognition validation

## Implemented

- CameraX live preview and optional QR fallback.
- Bundled MobileNetV3 Small image-embedding model, loaded by LiteRT on device without runtime network access.
- RGB preprocessing to 224×224 float input, 1,024-dimensional L2-normalized embeddings, and cosine-similarity ranking against local reference images.
- Scanner action wired to a captured camera preview frame.
- Conservative unknown/ambiguous decisions. Raw cosine similarity is not represented as a probability.
- CI verifies the model size and SHA-256 before packaging it in the APK.

This is an integrated visual embedding and retrieval pipeline. It is **not yet a validated landmark recognition service**: the current curated index contains one reference photograph for each of three landmarks, and the calibration asset explicitly remains unvalidated. The runtime enforces at least 20 reference images per landmark, at least 10 held-out images per landmark, at least 100 held-out unknown-scene images, and a calibration file explicitly recording those held-out counts. The scanner must keep visual identification disabled until these gates pass.

## Dataset contract

The authoritative field dataset manifest is `dataset/recognition/field_dataset_manifest.json`. Each verified image entry must include:

- Unique `id`, repository-relative `path`, `role` (`reference`, `evaluation`, or `unknown`), and `verified: true`.
- `captureSessionId` so reference and held-out sessions can be separated.
- `source` and `sha256` provenance/integrity fields.
- `landmarkId` for reference and evaluation images. Unknown scenes must not be assigned to a supported landmark.

Unverified images can be retained for review but do not count toward readiness. Never infer a landmark identity from scenery alone. A nearby sign may identify the sign or its immediate site, but does not automatically prove that every photograph in the same batch depicts that landmark.

## Minimum collection before activation

For every landmark that will be visually recognized:

- At least 20 verified reference images from varied viewpoints, distances, orientations, and lighting.
- At least 10 verified held-out evaluation images from a different capture session.
- No overlap between reference and evaluation capture sessions.
- At least 100 verified held-out unknown-scene images across the province.

Run `python3 scripts/validate_recognition_dataset.py` to see the current counts and blockers. The script deliberately exits successfully while reporting **NOT READY** for an incomplete dataset, so CI can continue building the app without pretending the recognizer is validated.

## Release gates

The following are acceptance targets, not measured results:

- At least 90% top-1 correct recognition on held-out supported-landmark images.
- No more than 1% false acceptance on held-out unknown scenes.
- Ambiguous and low-similarity scenes must return an unknown/ambiguous result, never a guessed name.
- Measure median and peak inference latency, memory, and camera responsiveness on Samsung Galaxy A06 (4 GB RAM).
- Verify offline operation with airplane mode enabled.
- Re-evaluate after any model, preprocessing, reference-set, or threshold change.

Publish per-landmark confusion matrices, correct-match rate, ambiguous rate, unknown-scene false-accept rate, median inference time, and peak memory. Do not publish a single overall accuracy figure without these details.

## Safety and performance requirements

- Use only curated local assets; never fetch images during recognition.
- Run inference away from the UI thread and throttle camera inference.
- Keep the latest-frame-only CameraX strategy.
- Close every `ImageProxy` on success, failure, and cancellation.
- Do not enable the camera recognition action until verified multi-view references and held-out calibration are packaged.
- Keep QR as an optional fallback, not as the only recognition method.
