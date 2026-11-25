#!/usr/bin/env python3
"""Create a compact local-search JSON index from an OSM GeoJSON export."""
import json
import sys
from pathlib import Path

source, destination = map(Path, sys.argv[1:3])
data = json.loads(source.read_text(encoding="utf-8"))
items = []
seen = set()

def representative_point(geometry):
    if not geometry:
        return None
    kind, coords = geometry.get("type"), geometry.get("coordinates")
    if kind == "Point":
        return coords
    points = []
    def walk(value):
        if isinstance(value, list) and len(value) >= 2 and all(isinstance(x, (int, float)) for x in value[:2]):
            points.append(value)
        elif isinstance(value, list):
            for child in value:
                walk(child)
    walk(coords)
    if not points:
        return None
    return [sum(p[0] for p in points) / len(points), sum(p[1] for p in points) / len(points)]

for feature in data.get("features", []):
    props = feature.get("properties") or {}
    tags = props.get("all_tags") or {}
    if not isinstance(tags, dict):
        tags = {}
    name = props.get("name") or tags.get("name")
    if not name:
        continue
    point = representative_point(feature.get("geometry"))
    if not point or len(point) < 2:
        continue
    lon, lat = point[0], point[1]
    key = (str(name).strip().casefold(), round(lat, 5), round(lon, 5))
    if key in seen:
        continue
    seen.add(key)
    items.append({
        "name": str(name).strip(),
        "name_ar": tags.get("name:ar"),
        "name_fr": tags.get("name:fr"),
        "name_en": tags.get("name:en"),
        "category": tags.get("amenity") or tags.get("tourism") or tags.get("historic") or tags.get("shop") or tags.get("leisure") or tags.get("natural") or tags.get("place") or "place",
        "latitude": lat,
        "longitude": lon,
        "osm_type": props.get("@type"),
        "osm_id": props.get("@id")
    })

items.sort(key=lambda item: (item["name"].casefold(), item["latitude"], item["longitude"]))
destination.write_text(json.dumps({
    "schema_version": 1,
    "region": "El Bayadh",
    "wilaya_code": 32,
    "source": "OpenStreetMap contributors",
    "licence": "ODbL-1.0",
    "count": len(items),
    "items": items
}, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
print(f"Indexed {len(items)} named local features.")
if not items:
    raise SystemExit("The offline POI index is empty; refusing to publish it.")
