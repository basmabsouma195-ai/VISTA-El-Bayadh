# VISTA Offline Recognition Engine Decision

Status: engineering decision for a prototype; **not a production recognition claim**.

## Selected evaluation path

Evaluate the MediaPipe Image Embedder MobileNetV3 Small model through the LiteRT Android runtime, with CPU inference as the baseline. The model produces a 1,024-dimensional image embedding from a 224 x 224 image and is approximately 4.12 MB (FP32). The model is ImageNet-pretrained, not trained on El Bayadh landmarks. Its model card cautions that fine-grained domains may be outside its training distribution; similarity scores must therefore be treated as retrieval signals, not calibrated probabilities.

Model reference:
- Model: https://huggingface.co/litert-community/MediaPipe-ImageEmbedder-MobileNet-V3-Small
- Model file: mobilenet_v3_small.tflite
- Model licence: Apache-2.0 (verify the exact model card and packaged licence before shipping)
- SHA-256 published by the model repository: bbbb4c51a55a53905af1daec995ca1aae355046f8839bb8c9f5ce9271394bc40

## Why LiteRT instead of the MediaPipe Tasks Android wrapper

The app currently declares minSdk 26. The MediaPipe Tasks Android setup guide states minSdk 28, which would force a compatibility change or exclude API 26-27 devices. LiteRT's Android compatibility table lists the Interpreter API at minSdk 21 for the 1.4.x line and the newer LiteRT 2.x runtime at minSdk 24. The app can therefore evaluate the model without raising its minSdk.

References:
- https://developers.google.com/edge/mediapipe/solutions/setup_android
- https://developers.google.com/edge/litert/android

## Proposed runtime flow

1. Decode a camera frame to a bounded RGB bitmap on a worker thread.
2. Resize/crop to 224 x 224 RGB and divide each channel by 255. The published tensor specification is float32 `[1, 224, 224, 3]`, values 0..1 (mean 0, std 255); verify this against the pinned binary during integration.
3. Run CPU inference; keep one interpreter instance and serialize access.
4. L2-normalize embeddings and compare only against precomputed embeddings for locally bundled, verified field photographs.
5. Aggregate multiple reference views per landmark. Do not allow one generic seed image to identify a landmark.
6. Convert similarity into a candidate ranking only. Do not display similarity as a probability or confidence percentage.
7. Accept a result only after thresholds and the lead over the second candidate have been calibrated on held-out El Bayadh photos. Otherwise return Unknown or Ambiguous.
8. Keep QR as an optional fallback. All recognition and reference lookup must work offline.

## Data and safety gates

- Only use field photographs whose landmark identity has been explicitly confirmed.
- Keep unconfirmed photographs out of the runtime matching index.
- Separate training/reference captures from held-out test captures and record capture session, device, angle, lighting, and landmark ID.
- Include negative scenes: ordinary buildings, roads, sky, rocks, and landmarks outside the supported catalogue.
- Never infer a landmark identity from location proximity or an unlabelled photo.
- Do not ship an enabled visual-recognition button until the model, image index, and real-device tests pass.

## Release gates

Use the quantitative gates in [RECOGNITION_VALIDATION.md](../RECOGNITION_VALIDATION.md). In addition, measure model initialization time, per-frame latency, peak memory, APK size, and thermal behaviour on the Samsung Galaxy A06 target device. Pixel 6 benchmark figures from upstream documentation are not evidence of performance on the A06.

## Current implementation status

- LiteRT Interpreter 1.4.2 is declared and the pinned 4.12 MB FP32 MobileNetV3 Small model is fetched during CI, checked by exact byte size and SHA-256, then packaged in the APK. The model is never fetched at app runtime.
- `MobileNetV3RecognitionEngine` loads the model, validates tensor shapes, preprocesses RGB frames to 224 x 224 float32 values in 0..1, creates L2-normalized 1,024-dimensional embeddings, and ranks local reference vectors by cosine similarity.
- The scanner loads the model offline and exposes a visual-identification action. QR remains available as an optional fallback.
- The visual-identification action remains disabled by design: the current catalogue has one approved photo per supported landmark, not the required three or more, and `calibration.json` explicitly records `validatedOnHeldOutElBayadhSet: false`.
- Unit tests cover similarity math and conservative recognition policy. CI assembly is not a substitute for a camera/device test.

This is an integrated embedding and retrieval pipeline, **not yet a validated landmark recognizer**. Enablement requires multiple verified views per site, held-out local calibration, negative-scene testing, and real-device performance/accuracy validation on the Galaxy A06.
