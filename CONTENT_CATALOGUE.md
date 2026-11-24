# VISTA El Bayadh — offline catalogue

The bundled catalogue lives at `app/src/main/assets/content/landmarks.json`.
It is loaded from application assets, so reading the catalogue does not require
internet access.

## Top-level structure

- `schemaVersion`: structure version used by the app.
- `contentVersion`: version of the tourism content package.
- `province`: stable province identifier and localized names.
- `categories`: catalogue category records.
- `landmarks`: destination records.

Each landmark record uses this structure:

```json
{
  "id": "stable-unique-id",
  "categoryId": "historic-sites",
  "nameAr": "الاسم بالعربية",
  "nameFr": "Nom en français",
  "nameEn": "English name",
  "descriptionAr": "الوصف العربي",
  "descriptionFr": "Description française",
  "descriptionEn": "English description",
  "latitude": 0.0,
  "longitude": 0.0,
  "verification": "verified",
  "image": "images/example.webp",
  "audioAr": "audio/example-ar.mp3",
  "audioFr": "audio/example-fr.mp3",
  "audioEn": "audio/example-en.mp3"
}
```

## Editorial rules

- `verification` is one of `verified`, `oral_history`, or `under_review`.
- Coordinates are optional; never add guessed coordinates.
- Image and audio paths are optional and must point to bundled files.
- Keep all three language fields together for each destination.
- Keep IDs stable so favourites and later content updates can refer to the same place.
- Do not mark an entry verified until its supporting material has been reviewed.

The current catalogue structure is valid but has no landmark records yet. This is
intentional: no unverified or invented destinations are shipped as facts.
