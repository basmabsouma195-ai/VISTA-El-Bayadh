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

The catalogue currently includes Ksar Mechria Sughra, Gour Brizina, Kef El Melh, Ksar Bent Khass, Gour Bent El Khass, Ksar Boussemghoun – Zaouia Sidi Ahmed Tidjani, and initial El Bayadh/Boussemghoun seed records. Most descriptions, coordinates, and media are intentionally absent pending source review. Gour Brizina and Kef El Melh remain `under_review`; do not present them as historically verified or use unverified coordinates for navigation.

Runtime landmark images are not taken from the optional `image` field in this JSON. `OfflineCatalogue` resolves a photo only when `recognition/index.json` points to an existing packaged local asset for the same `landmarkId`. This prevents broken paths and remote image dependencies.
