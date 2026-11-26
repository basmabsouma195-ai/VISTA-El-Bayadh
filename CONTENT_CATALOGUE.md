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

Content version **19** contains **31 landmark records** for El Bayadh Province:
**8 verified** and **23 under review**. All 31 records have Arabic, French and
English names and descriptions, plus at least one cited source.

The most important remaining gap is geographic coverage: only **1 of 31**
runtime records currently has latitude and longitude. The other 30 must not be
placed on the map using guessed coordinates. The offline base map is now bundled
and has passed an Android emulator test with Wi-Fi and mobile data disabled;
landmark pins will appear as their records receive confirmed coordinates.

Research is tracked separately in:
- `dataset/photo-research/landmark-research-notes.json`: 12 enriched research notes.
- `dataset/photo-research/additional-landmarks-discovery.json`: 13 discovery entries.
- Eight IDs occur in both the research queue and the runtime catalogue; enrich
  those existing records rather than creating duplicates.

The catalogue covers ksour and historic buildings, natural formations and caves,
rock-art heritage, dinosaur traces, religious heritage, memorials and defensive
structures. The current record count is not a claim that the province-wide
inventory is complete or that every place is ready for visitors.

Do not silently promote `under_review` entries to verified, infer precise
locations from approximate distances, or present oral accounts as established
dates. Review every source against the exact place and retain visitor-access
cautions where the source describes difficult or unsafe access.

## Recognition and field-photo status

The field-photo gallery currently contains **19 records**: 18 are enabled for
the visitor gallery and one remains unconfirmed and disabled. These gallery
photos are not automatically approved for recognition.

The offline recognition index currently contains **3 approved reference images**:
Ksar Bent Khass, Gour Bent El Khass and the Boussemghoun Zaouia. Three references
are not enough for reliable visual recognition; each target still needs several
distinct real-world viewpoints and a separate unknown-scene evaluation set.
Keep gallery, documentary/signage and recognition-training roles separate.

The Android scanner currently supports camera preview and optional QR reading;
visual landmark matching is not yet implemented.

See `dataset/landmarks/collection-status.md` for the live collection priorities
and the rules for coordinates, source review, photographs and publication.
