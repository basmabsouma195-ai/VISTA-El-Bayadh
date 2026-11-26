# VISTA El Bayadh — Map System Architecture

## Decision

Replace the retired Mapsforge implementation with **MapLibre Native Android + a locally packaged PMTiles vector-tile archive**. This is a new implementation, not a patch to the former renderer. The map screen stays intentionally empty until the first complete MapLibre slice passes runtime acceptance tests.

## Geographic scope

- Country: Algeria
- Wilaya: El Bayadh
- Wilaya code: 32
- Coverage: the verified wilaya boundary and all 22 communes across the two current wilayas
- The map must not silently expand to all Algeria or confuse El Bayadh with another wilaya.

The boundary source and its ODbL attribution are tracked in dataset/maps/el-bayadh-map-source.json.

## Runtime architecture

1. **Renderer:** MapLibre Native Android SDK, embedded in the existing Jetpack Compose destination through a lifecycle-safe Android view.
2. **Basemap:** one PMTiles vector archive stored locally in app assets. The style and all fonts/glyphs required for labels must also be local.
3. **Initial camera:** computed from the verified wilaya boundary, with a useful province-wide starting zoom. No arbitrary camera constants.
4. **Places:** a separately versioned local POI catalogue, joined to the existing landmark catalogue by stable IDs. Map taps open the matching VISTA landmark profile; unknown map features do not create fake records.
5. **Location:** optional user-location layer, requested only when the user activates it. The map must render without location permission.
6. **Saved places:** persist user-created pins locally, independently of map tile storage.
7. **Attribution:** visible OpenStreetMap attribution and the applicable data licence in the map UI.

## Data build pipeline

- Fetch a pinned Geofabrik Algeria OSM PBF snapshot.
- Obtain and validate the source-tracked Wilayas 32 and 69 boundary.
- Extract the boundary polygon and clip the PBF to it, preserving required relation context.
- Generate vector tiles with a pinned Planetiler/OpenMapTiles profile and write a PMTiles archive.
- Generate and package the matching local style, glyph/font assets, boundary metadata and POI index.
- Validate archive metadata, tile coverage at the province centre and representative communes, expected source layers, POI coordinates, and checksums before publishing an artifact.
- Keep the raw source, boundary, style and build versions recorded so the map is reproducible.

## Runtime acceptance gates

A build is not considered a working map merely because the APK contains a PMTiles file.

1. Unit/integration checks open the PMTiles header and validate bounds, zoom range and tile metadata.
2. Android emulator instrumentation launches the real map screen and waits for MapLibre style and first rendered frame.
3. The test captures a screenshot and checks that the map is not blank/solid grey.
4. The test verifies at least one road, water/land feature and place label in the El Bayadh viewport.
5. The test repeats with network access disabled.
6. The test checks lifecycle recreation and confirms the renderer remains visible.
7. A human visual review checks the province boundary, styling, labels and attribution before release.

## Delivery phases

- **Phase A — clean baseline:** remove the old Mapsforge renderer and pipeline. Completed on the feature branch.
- **Phase B — renderer proof:** add MapLibre Native and a tiny deterministic PMTiles fixture; render it on an emulator and prove offline rendering before using province data.
- **Phase C — Wilayas 32 and 69 data:** generate the real province PMTiles archive and verify coverage across all communes.
- **Phase D — VISTA integration:** landmark pins, search, saved places, location control, attribution and final visual styling.
- **Phase E — release validation:** clean build, emulator screenshot, offline test, APK signature/alignment and release artifact.

## Explicit non-goals for the first renderer slice

- No online tile provider or API key.
- No network-dependent style, glyph or sprite resources.
- Online road-route preview uses a routing service when connectivity is available; no offline turn-by-turn guidance is claimed until a separate offline routing engine and routable graph have been implemented and tested.
- No changes to the app's existing visual identity or other tabs.
- Do not merge this feature branch into main without approval.
