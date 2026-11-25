#!/usr/bin/env python3
"""Prepare a combined offline map boundary for El Bayadh (32) and El Abiodh Sidi Cheikh (69)."""
import argparse
import json
import urllib.request
from pathlib import Path

BOUNDARY_URL = (
    "https://raw.githubusercontent.com/yasserstudio/geoalgeria/main/"
    "packages/dataset/data/geojson/wilaya-boundaries-detailed.geojson"
)
REGIONS = {
    32: {"name": "El Bayadh", "name_ar": "البيض"},
    69: {"name": "El Abiodh Sidi Cheikh", "name_ar": "الأبيض سيدي الشيخ"},
}

def polygons_for_geometry(geometry):
    kind, coords = geometry["type"], geometry["coordinates"]
    if kind == "Polygon":
        return [coords]
    if kind == "MultiPolygon":
        return coords
    raise ValueError(f"Unsupported boundary geometry: {kind}")

def write_poly(features, output):
    polygons = []
    for feature in features:
        polygons.extend(polygons_for_geometry(feature["geometry"]))
    with output.open("w", encoding="utf-8") as stream:
        stream.write("El_Bayadh_and_El_Abiodh_Sidi_Cheikh\n")
        for polygon_index, polygon in enumerate(polygons):
            for ring_index, ring in enumerate(polygon):
                name = f"!ring_{polygon_index}_{ring_index}" if ring_index > 0 else f"ring_{polygon_index}_{ring_index}"
                stream.write(name + "\n")
                for lon, lat in ring:
                    stream.write(f"  {lon:.7f}  {lat:.7f}\n")
                stream.write("END\n")
        stream.write("END\n")

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--boundary", type=Path, required=True)
    parser.add_argument("--poly", type=Path, required=True)
    args = parser.parse_args()
    args.boundary.parent.mkdir(parents=True, exist_ok=True)
    if not args.boundary.exists():
        with urllib.request.urlopen(BOUNDARY_URL, timeout=60) as response:
            args.boundary.write_bytes(response.read())
    dataset = json.loads(args.boundary.read_text(encoding="utf-8"))
    features = dataset.get("features", [])
    selected = {}
    for code in REGIONS:
        matches = [f for f in features if int(f.get("properties", {}).get("code", -1)) == code]
        if len(matches) != 1:
            raise SystemExit(f"Expected exactly one boundary feature for wilaya code {code}, found {len(matches)}")
        selected[code] = matches[0]
    ordered = [selected[32], selected[69]]
    write_poly(ordered, args.poly)

    coordinates = []
    for feature in ordered:
        for polygon in polygons_for_geometry(feature["geometry"]):
            coordinates.extend(point for point in polygon[0])
    lons = [point[0] for point in coordinates]
    lats = [point[1] for point in coordinates]
    print(json.dumps({
        "regions": [{"code": code, **REGIONS[code]} for code in REGIONS],
        "wilaya_codes": [32, 69],
        "osm_relation_ids": {
            str(code): feature.get("properties", {}).get("osm_id")
            for code, feature in selected.items()
        },
        "boundary_source": BOUNDARY_URL,
        "boundary_license": "ODbL-1.0 (OpenStreetMap-derived; see GeoAlgeria LICENSE/NOTICE)",
        "bbox": [min(lats), min(lons), max(lats), max(lons)],
        "outer_rings": sum(len(polygons_for_geometry(feature["geometry"])) for feature in ordered),
        "poly": str(args.poly)
    }, ensure_ascii=False, indent=2))

if __name__ == "__main__":
    main()
