#!/usr/bin/env python3
"""Fetch the small set of MapLibre glyph ranges needed for Arabic and Latin labels."""
from __future__ import annotations

import sys
from pathlib import Path
from urllib.parse import quote
from urllib.request import Request, urlopen

OUTPUT = Path(sys.argv[1] if len(sys.argv) > 1 else "build/maps/glyphs")
FONTSTACK = "Noto Sans Regular"
RANGES = ("0-255", "256-511", "1536-1791")

font_dir = OUTPUT / FONTSTACK
font_dir.mkdir(parents=True, exist_ok=True)
for glyph_range in RANGES:
    destination = font_dir / f"{glyph_range}.pbf"
    url = (
        "https://demotiles.maplibre.org/font/"
        + quote(FONTSTACK, safe="")
        + f"/{glyph_range}.pbf"
    )
    request = Request(url, headers={"User-Agent": "VISTA-El-Bayadh-map-builder"})
    with urlopen(request, timeout=60) as response:
        data = response.read()
    if not data:
        raise SystemExit(f"Empty glyph response: {url}")
    destination.write_bytes(data)
    print(f"{destination}: {len(data)} bytes")
