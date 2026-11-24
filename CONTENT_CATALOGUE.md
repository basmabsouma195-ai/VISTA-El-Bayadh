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
  "audioEn": "audio/example-en.mp3",
  "sources": [{ "title": "Source title", "url": "https://...", "publisher": "Publisher", "note": "Editorial note" }]
}
```

## Editorial rules

- `verification` is one of `verified`, `oral_history`, or `under_review`.
- Coordinates are optional; never add guessed coordinates.
- Image and audio paths are optional and must point to bundled files.
- Keep all three language fields together for each destination.
- Add one or more `sources` records whenever a description uses external historical, cultural, geographic, or institutional information. Keep the source URL, publisher, and a short note describing exactly what it supports.
- Source records are stored in SQLite and shown in the landmark detail card. They are bundled with the app and available offline as attribution text; opening source websites requires connectivity.
- Keep unverified stories, approximate locations, and preliminary research clearly labelled; do not turn a source claim into a confirmed fact without corroboration.
- Keep IDs stable so favourites and later content updates can refer to the same place.
- Do not mark an entry verified until its supporting material has been reviewed.

The catalogue is focused on El Bayadh Province and currently includes 19 records covering ksour, natural formations, caves, rock-art stations, dinosaur footprints, and religious heritage. The populated entries include Ksar Mechria Sughra, Ksar Ghassoul, Ksar Stitten, Boussemghoun Old Mosque, Gour Bent El Khass, Brézina Gours, Brézina Cave, Khashim Rock Art Station, Merdoufa, Bélier de Bou Alem, Bastion Bent El Khas, and dinosaur footprints near the Reggassa road. Entries without sufficiently reliable descriptions remain intentionally incomplete and marked `under_review`; no coordinates are invented. The source register distinguishes official heritage inventory records, cultural-heritage map descriptions, and local reporting.
