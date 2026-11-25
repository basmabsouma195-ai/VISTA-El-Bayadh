#!/usr/bin/env python3
"""Prepare the verified El Bayadh boundary and a compact offline OSM place index."""
import argparse
import json
import math
import urllib.request
from pathlib import Path

BOUNDARY_URL = (
    "https://raw.githubusercontent.com/yasserstudio/geoalgeria/main/"
    "packages/dataset/data/geojson/wilaya-boundaries-detailed.geojson"
)

def rings_for_geometry(geometry):
    kind = geometry["type"]
    coords = geometry["coordinates"]
    if kind == "Polygon":
        return [coords]
    if kind == "MultiPolygon":
        return coords
    raise ValueError(f"Unsupported boundary geometry: {kind}")

def write_poly(feature, output):
    rings = rings_for_geometry(feature["geometry"])
    with output.open("w", encoding="utf-8") as stream:
        stream.write("El_Bayadh\n")
        for polygon_index, polygon in enumerate(rings):
            for ring_index, ring in enumerate(polygon):
                is_hole = ring_index > 0
                name = f"!ring_{polygon_index}_{ring_index}" if is_hole else f"ring_{polygon_index}_{ring_index}"
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
    if not args.boundary.exists():
        with urllib.request.urlopen(BOUNDARY_URL, timeout=60) as response:
            args.boundary.write_bytes(response.read())
    dataset = json.loads(args.boundary.read_text(encoding="utf-8"))
    features = dataset.get("features", [])
    matches = [f for f in features if f.get("properties", {}).get("code") == 32]
    if len(matches) != 1:
        raise SystemExit(f"Expected exactly one GeoAlgeria feature for wilaya code 32, found {len(matches)}")
    feature = matches[0]
    write_poly(feature, args.poly)
    coords = []
    for polygon in rings_for_geometry(feature["geometry"]):
        coords.extend(polygon[0])
    lons = [p[0] for p in coords]
    lats = [p[1] for p in coords]
    print(json.dumps({
        "wilaya": "El Bayadh",
        "code": 32,
        "osm_relation_id": 1258649,
        "boundary_source": BOUNDARY_URL,
        "boundary_license": "ODbL-1.0 (OpenStreetMap-derived; see GeoAlgeria LICENSE/NOTICE)",
        "bbox": [min(lats), min(lons), max(lats), max(lons)],
        "outer_rings": len(rings_for_geometry(feature["geometry"])),
        "poly": str(args.poly)
    }, ensure_ascii=False, indent=2))

if __name__ == "__main__":
    main()
