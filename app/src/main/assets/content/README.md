# VISTA offline content package

This directory is the bundled content root for the El Bayadh offline edition.

Content is stored locally in the installed application. The initial release must not
depend on a network request to display its core encyclopedia. Future content updates
are versioned separately from application updates and are copied into the local store
only after validation.

## Record requirements

Every published landmark record must include:
- Stable identifier and category identifier.
- Arabic, French and English names and descriptions.
- Editorial status: documented, oral account, or under investigation.
- Verified geographic coordinates before map publication.
- Locally bundled image references.
- Optional locally bundled audio references for each language.

Never fabricate historical claims, coordinates, opening hours, visitor guidance,
or safety information. Empty content is preferable to invented content.


## Visual-recognition reference images

Camera recognition is a separate local index, not the visitor-facing catalogue.
Do not treat a landmark's single display photograph as a reliable recognition set.

Before enabling a landmark in the recognition index, collect a reviewed set of
real photographs for that exact landmark:
- Multiple viewpoints, distances and framing, including partial views.
- Daylight, shade and realistic lighting variation.
- Both close architectural details and wider environmental context.
- Negative examples: visually similar buildings, rocks, terrain and unrelated scenes.
- A stable `landmarkId` that exactly matches a catalogue record.

Keep separate training/reference and held-out evaluation images. Avoid near-duplicate
frames across those splits. Record provenance, capture conditions and permission
for every image. Do not use generated images as real-world reference photographs.

The on-device engine must return an explicit unknown result when the model is
missing, the best score is below its calibrated threshold, or two candidates
are too close. Confidence thresholds must be measured on held-out local images;
they must not be guessed. A model is not production-ready until false matches,
unknown-scene rejection, lighting changes and performance on supported phones
have been evaluated.

Suggested future layout (only populate with reviewed assets):

```
recognition/
  manifest.json
  images/
    ksar-mechria-sughra/
      view-001.jpg
      view-002.jpg
    <landmark-id>/
      ...
```

The current Android recognition package defines the engine/result contract and
a conservative decision policy only. No trained model or landmark image index
is bundled yet, so visual identification is not active.
