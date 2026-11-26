#!/usr/bin/env python3
"""Create the combined offline map boundary for Wilayas 32 and 69."""
import json
import sys
import urllib.request
from pathlib import Path

SOURCE_COMMIT = "b9fccb53f60d82cf98bc720163edbfcba2c63881"
BOUNDARY_URL = (
    "https://raw.githubusercontent.com/yasserstudio/geoalgeria/"
    f"{SOURCE_COMMIT}/packages/dataset/data/geojson/wilaya-boundaries-detailed.geojson"
)
WILAYA_CODES = (32, 69)
WILAYA_NAMES = {
    32: ("El Bayadh", "البيض"),
    69: ("El Abiodh Sidi Cheikh", "الأبيض سيدي الشيخ"),
}
OUT = Path(sys.argv[1] if len(sys.argv) > 1 else "build/maps")
OUT.mkdir(parents=True, exist_ok=True)
geojson_path = OUT / "wilaya-boundaries.geojson"
if not geojson_path.exists():
    request = urllib.request.Request(BOUNDARY_URL, headers={"User-Agent": "VISTA-El-Bayadh-map-builder"})
    with urllib.request.urlopen(request, timeout=90) as response:
        geojson_path.write_bytes(response.read())

dataset = json.loads(geojson_path.read_text(encoding="utf-8"))
matches = {
    int(feature.get("properties", {}).get("code", -1)): feature
    for feature in dataset.get("features", [])
    if int(feature.get("properties", {}).get("code", -1)) in WILAYA_CODES
}
missing = sorted(set(WILAYA_CODES) - set(matches))
if missing:
    raise SystemExit(f"Missing required Wilaya boundaries: {missing}")

all_polygons = []
all_points = []
for code in WILAYA_CODES:
    geometry = matches[code]["geometry"]
    if geometry["type"] == "Polygon":
        polygons = [geometry["coordinates"]]
    elif geometry["type"] == "MultiPolygon":
        polygons = geometry["coordinates"]
    else:
        raise SystemExit(f"Unsupported boundary type for Wilaya {code}: {geometry['type']}")
    for polygon in polygons:
        all_polygons.append(polygon)
        for ring in polygon:
            all_points.extend((float(lon), float(lat)) for lon, lat in ring)

combined_geometry = {"type": "MultiPolygon", "coordinates": all_polygons}
poly_path = OUT / "el-bayadh-el-abiodh.poly"
with poly_path.open("w", encoding="utf-8") as stream:
    stream.write("El_Bayadh_32_El_Abiodh_Sidi_Cheikh_69\n")
    for polygon_index, polygon in enumerate(all_polygons):
        for ring_index, ring in enumerate(polygon):
            stream.write(f"ring_{polygon_index}_{ring_index}\n")
            for longitude, latitude in ring:
                stream.write(f"  {longitude:.8f}  {latitude:.8f}\n")
            stream.write("END\n")
    stream.write("END\n")

metadata = {
    "wilaya_codes": list(WILAYA_CODES),
    "wilayas": [
        {"code": code, "name": WILAYA_NAMES[code][0], "name_ar": WILAYA_NAMES[code][1]}
        for code in WILAYA_CODES
    ],
    "name": "El Bayadh and El Abiodh Sidi Cheikh",
    "name_ar": "البيض والأبيض سيدي الشيخ",
    "country": "Algeria",
    "communes_expected": 22,
    "source": BOUNDARY_URL,
    "source_commit": SOURCE_COMMIT,
    "license": "ODbL-1.0",
    "geometry": combined_geometry,
    "bbox_wsen": [
        min(point[0] for point in all_points), min(point[1] for point in all_points),
        max(point[0] for point in all_points), max(point[1] for point in all_points)
    ],
    "polygon_count": len(all_polygons),
    "ring_count": sum(len(polygon) for polygon in all_polygons)
}
(OUT / "boundary.json").write_text(json.dumps(metadata, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(json.dumps(metadata, ensure_ascii=False))
