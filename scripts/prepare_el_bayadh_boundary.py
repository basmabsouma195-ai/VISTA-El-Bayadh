#!/usr/bin/env python3
"""Create the Osmium polygon and metadata for El Bayadh Wilaya 32."""
import json
import sys
import urllib.request
from pathlib import Path

BOUNDARY_URL = "https://raw.githubusercontent.com/yasserstudio/geoalgeria/7adb2844c4243cf1ca5ece0cdf02b38e8232da61/packages/dataset/data/geojson/wilaya-boundaries-detailed.geojson"
OUT = Path(sys.argv[1] if len(sys.argv) > 1 else "build/maps")
OUT.mkdir(parents=True, exist_ok=True)
geojson_path = OUT / "wilaya-boundaries.geojson"
if not geojson_path.exists():
    request = urllib.request.Request(BOUNDARY_URL, headers={"User-Agent": "VISTA-El-Bayadh-map-builder"})
    with urllib.request.urlopen(request, timeout=90) as response:
        geojson_path.write_bytes(response.read())

dataset = json.loads(geojson_path.read_text(encoding="utf-8"))
matches = [
    feature for feature in dataset.get("features", [])
    if str(feature.get("properties", {}).get("code", "")).zfill(2) == "32"
]
if len(matches) != 1:
    raise SystemExit(f"Expected one Wilaya 32 boundary, found {len(matches)}")
feature = matches[0]
geometry = feature["geometry"]
if geometry["type"] == "Polygon":
    polygons = [geometry["coordinates"]]
elif geometry["type"] == "MultiPolygon":
    polygons = geometry["coordinates"]
else:
    raise SystemExit(f"Unsupported boundary type: {geometry['type']}")

poly_path = OUT / "el-bayadh.poly"
all_points = []
with poly_path.open("w", encoding="utf-8") as stream:
    stream.write("El_Bayadh_Wilaya_32\n")
    for polygon_index, polygon in enumerate(polygons):
        for ring_index, ring in enumerate(polygon):
            stream.write(f"ring_{polygon_index}_{ring_index}\n")
            for longitude, latitude in ring:
                stream.write(f"  {longitude:.8f}  {latitude:.8f}\n")
                all_points.append((longitude, latitude))
            stream.write("END\n")
    stream.write("END\n")

metadata = {
    "wilaya_code": 32,
    "name": "El Bayadh",
    "name_ar": "البيض",
    "country": "Algeria",
    "source": BOUNDARY_URL,
    "source_commit": "7adb2844c4243cf1ca5ece0cdf02b38e8232da61",
    "osm_relation_id": feature.get("properties", {}).get("osm_id", 1258649),
    "license": "ODbL-1.0",
    "bbox_wsen": [
        min(p[0] for p in all_points), min(p[1] for p in all_points),
        max(p[0] for p in all_points), max(p[1] for p in all_points)
    ],
    "polygon_count": len(polygons),
    "ring_count": sum(len(polygon) for polygon in polygons)
}
(OUT / "boundary.json").write_text(json.dumps(metadata, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(json.dumps(metadata, ensure_ascii=False))

# Keep this boundary-preparation source in the map build trigger set.
