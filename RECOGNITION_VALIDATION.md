# VISTA — offline landmark recognition validation plan

## Current status

The Android app currently has a CameraX preview and optional QR fallback. It does **not** yet contain a visual landmark model or a visual matching engine. The recognition engine contract deliberately returns `ModelUnavailable` until a real implementation is supplied.

The packaged image index is a collection of local references, not a trained model. At present the committed field set contains three confirmed sign/reference photographs; the build-time licensed seed manifest contains five candidate photographs, but a CI build may bundle fewer when Wikimedia throttles downloads. A seed photograph is not evidence of multi-view coverage.

## Dataset rules

1. Every image must have a confirmed landmark ID. Unidentified landscape photographs remain unassigned.
2. Keep field photographs, licensed web seed photographs and evaluation photographs distinguishable in the manifest.
3. Record photographer/source, license and source page for every external image. Keep this metadata in the APK.
4. Capture each supported landmark from different viewpoints, distances, orientations and lighting conditions.
5. Do not split near-identical frames from one camera burst between reference and evaluation sets. Separate by capture session or visit to prevent leakage.
6. Include negative examples: ordinary streets, buildings, mountains, ksour not in the catalogue, close-up details and scenes with no supported landmark.
7. Do not treat a signboard as proof that the system recognizes the physical landmark. Sign recognition and structure recognition are separate test cases.

## Minimum field collection before model activation

For each landmark intended to be recognized:
- At least 20 reference photographs from multiple viewpoints.
- At least 10 held-out photographs from a separate capture session.
- A mixture of near, medium and far distances and daylight/shade conditions.
- At least 100 held-out unknown-scene photographs across the province before a public release.

If a landmark does not meet this minimum, keep it searchable in the catalogue but mark visual recognition as unsupported.

## Release gates

The thresholds below are acceptance targets, not measured results:
- At least 90% top-1 correct recognition on held-out supported-landmark images.
- No more than 1% false acceptance on held-out unknown scenes.
- Ambiguous or low-confidence scenes must return “لم يتم التعرف على المعلم” / equivalent, never a guessed name.
- Measure latency and memory use on the Samsung Galaxy A06 (4 GB RAM) and ensure the camera preview remains responsive.
- Run fully offline with airplane mode enabled.
- Repeat the evaluation after any change to model, preprocessing, reference set or thresholds.

Report a confusion matrix per landmark, unknown-scene false-accept rate, correct-match rate, ambiguous rate, median inference time and peak memory. Do not publish a single overall “accuracy” figure without these details.

## Camera integration safeguards

- Analyze only the latest frame; do not queue camera frames.
- Throttle inference and use a bounded image size.
- Close every CameraX `ImageProxy` on all success, failure and cancellation paths.
- Do not perform inference on the UI thread.
- Keep the QR scanner as an optional fallback.
- If model loading fails, show the unavailable state; never silently switch to guessed or remote recognition.

## Current implementation checklist

- [x] Offline image index and asset integrity validation.
- [x] Preserve licensed image provenance in the build-generated index.
- [x] Show photographer/license information beside seed references in the scanner screen.
- [x] Unit tests for high-confidence, low-confidence, ambiguous, invalid and empty decisions.
- [ ] Collect confirmed multi-view field references.
- [ ] Select and benchmark a compact on-device visual embedding model.
- [ ] Implement offline nearest-reference matching.
- [ ] Calibrate confidence and unknown-scene rejection on held-out field data.
- [ ] Validate latency, memory and offline behavior on the target phone.
