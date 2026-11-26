# VISTA El Bayadh — landmark data collection register

**Snapshot:** 26 November 2026 — research pass 2
**Runtime catalogue:** `app/src/main/assets/content/landmarks.json`  
**Research queues:** `dataset/photo-research/`

## Current baseline

| Measure | Current count |
|---|---:|
| Runtime landmark records | 32 |
| Verified records | 8 |
| Under review | 24 |
| Records with coordinates | 2 |
| Records still missing coordinates | 30 |
| Cited source records | 37 |
| Research notes | 13 |
| Discovery queue entries | 13 |
| Research/discovery IDs already in runtime | 9 |
| Field-photo gallery records | 19 |
| Gallery-enabled field photos | 18 |
| Unconfirmed field photos (disabled) | 1 |
| Approved offline recognition references | 3 |
| Catalogue categories | 7 |

The catalogue is not yet map-complete: the base map works offline, but only
records with confirmed latitude/longitude can become landmark pins. Do not
manufacture coordinates from a village name, a distance, or a broad map region.

## Collection order

1. **Resolve coordinates for the 30 records without them.** Prefer an official
   heritage-map point, an authoritative map record, or a field-confirmed GPS
   position. Store a point only when it identifies the actual site, not merely
   the municipality or a nearby road. If the source gives only a distance or a
   broad location, keep the coordinates null and record the location text in
   research notes.
2. **Reconcile the research queues.** The eight overlapping IDs must enrich
   existing runtime records. Do not create duplicate landmark IDs or duplicate
   entries for spelling variants of the same site.
3. **Complete administrative and access information.** For each place, collect
   municipality, daïra, nearest settlement, access description, and any
   restrictions or safety cautions. Keep difficult routes clearly identified;
   do not imply that a site is an easy or currently safe visitor destination.
4. **Review historical and architectural descriptions.** Attribute dates,
   founder narratives, dimensions and oral accounts to the source that reports
   them. Distinguish a source-reported claim from an independently verified
   archaeological fact.
5. **Organize photographs.** Keep visitor-gallery photos separate from
   recognition-approved images. Preserve original filenames and provenance.
   A sign or plaque is documentary evidence, not automatically a recognition
   training image.
6. **Complete the three languages.** Keep Arabic, French and English names and
   descriptions together. Translation must preserve uncertainty and source
   attribution rather than strengthening a tentative claim.

## Record acceptance checklist

- [ ] Stable, unique `id` and existing `categoryId`
- [ ] Arabic, French and English names and descriptions
- [ ] Municipality and daïra recorded in research metadata
- [ ] Exact coordinates, or coordinates deliberately left null
- [ ] Coordinate source/method and location confidence recorded
- [ ] At least one source with publisher and a note stating what it supports
- [ ] Verification status justified by the evidence
- [ ] Visitor access and safety caveats retained where relevant
- [ ] Image paths point to bundled files and have provenance
- [ ] Gallery and recognition roles are not conflated

## Newly enriched record

**قصر بوسمغون** is now a separate runtime record (`ksar-boussemghoun`), distinct from the existing mosque and Tijani zawiya entries. Its map coordinate is transcribed from the UNESCO Tentative List entry (32°51′53.27″N, 0°01′11.8″E; converted to 32.8647972, 0.0199444). The coordinate is source-published but still requires field confirmation before being treated as a verified visitor-navigation point. The Algerian Cultural Heritage Map supplies the descriptive material on the seven quarters, oasis, mosque and zawiya.

## First source-backed records to enrich

The official Algerian Cultural Heritage Map already has a detailed record for
**قصر المشرية الصغرى**. It reports the village location and architectural and
social details, including the 1564 foundation date; its public page does not
provide an exact coordinate in the page text. Keep the current record's
coordinates null until an exact point is established.

Other high-value entries already present in the research queue include:
- قصر عين حشيفة بالشلالة القبلية
- قصر بوسمغون
- قصر ملك سليمان
- محطات النقش الصخري: قلموز لبيوض، قارة التبن، قارة السموطة، قارة الطالب،
  خلوة سيدي الشيخ، الكريمة، الضلعة الميتة، شبكة الديغم، والخشيم

The research notes and discovery queue are research material, not automatic
publication approval. Review each entry before transferring it into the
runtime catalogue.

## Map integration rule

The offline PMTiles base map is a separate layer from the tourism catalogue.
The map provides the geographic background; each runtime landmark pin is
created from that landmark's own confirmed coordinates. OSM settlement dots
must not be mistaken for verified heritage-site locations.

## Research pass 2 — official rock-art location enrichment

Added **قويرات بن ثالول** to both research queues (not runtime) using its official heritage-map record. The record describes six rocky elevations over about 5 km, six engraved faces and 19 figures, with the drawings concentrated on the first two elevations. It gives access references to RN47 / kilometre point 110 and a dirt track, but no numeric coordinates; GPS remains null.

Also refined the source-derived location and access notes for **قارة التبن**, **الخشيم**, and **الكريمة**. Their municipality/daïra and route descriptions are recorded as source text, not converted into invented coordinates. Difficult or degraded sites remain subject to field access and conservation checks.

Primary references:
- https://cartes.patrimoineculturelalgerien.org/ar/lieu/32-el-bayadh/1263
- https://cartes.patrimoineculturelalgerien.org/ar/lieu/32-el-bayadh/1265
- https://cartes.patrimoineculturelalgerien.org/ar/lieu/32-el-bayadh/1522
- https://cartes.patrimoineculturelalgerien.org/ar/lieu/32-el-bayadh/1513
