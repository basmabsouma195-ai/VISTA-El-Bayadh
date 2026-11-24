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
- Keep unverified stories, approximate locations, and preliminary research clearly labelled; do not turn a source claim into a confirmed fact without corroboration.
- Keep IDs stable so favourites and later content updates can refer to the same place.
- Do not mark an entry verified until its supporting material has been reviewed.

## Current catalogue snapshot

Content version **12** contains **23 landmark records** focused on El Bayadh Province. The editorial status breakdown is **8 verified** and **15 under review**. Every record currently has Arabic, French and English names and descriptions, and at least one source record. This completeness check does not mean every record has been field-validated or that every coordinate, access route, opening time or visitor-safety detail is known.

The catalogue covers ksour and historic buildings, natural formations and caves, rock-art heritage, dinosaur traces and religious heritage. Examples include Ksar Mechria Sughra, Ksar Ghassoul, Ksar Stitten, Ksar Chellala Dhahrania, Ksar Arbaouat, Aïn Hachifa Ksar, the Sidi Cheikh mausoleum, Gour Bent El Khass, Brézina Gours, Kef El Melh and Boussemghoun heritage sites.

The 15 `under_review` entries remain provisional. Do not silently promote them to verified, infer precise locations from approximate research coordinates, or present oral accounts as established dates. Before publication, review the cited source against the exact landmark and add confirmed field observations where needed.
