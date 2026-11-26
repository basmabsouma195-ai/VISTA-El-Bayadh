#!/usr/bin/env python3
"""Offline visual smoke test for the VISTA El Bayadh MapLibre screen."""

from __future__ import annotations

import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

from PIL import Image


def adb(*args: str, timeout: int = 30) -> str:
    result = subprocess.run(
        ["adb", *args],
        check=True,
        capture_output=True,
        text=True,
        timeout=timeout,
    )
    return result.stdout


def find_text_bounds(label: str) -> tuple[int, int] | None:
    adb("shell", "uiautomator", "dump", "/sdcard/vista-window.xml", timeout=20)
    xml = adb("shell", "cat", "/sdcard/vista-window.xml")
    root = ET.fromstring(xml)
    for node in root.iter("node"):
        if node.attrib.get("text") == label or node.attrib.get("content-desc") == label:
            match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.attrib.get("bounds", ""))
            if match:
                left, top, right, bottom = map(int, match.groups())
                return (left + right) // 2, (top + bottom) // 2
    return None


def tap_text(label: str, timeout_seconds: int = 40) -> None:
    deadline = time.time() + timeout_seconds
    while time.time() < deadline:
        point = find_text_bounds(label)
        if point:
            adb("shell", "input", "tap", str(point[0]), str(point[1]))
            return
        time.sleep(1)
    raise RuntimeError(f"Could not find accessible UI text: {label!r}")


def main() -> None:
    if len(sys.argv) != 3:
        raise SystemExit("Usage: test_vista_offline_map_emulator.py APK_PATH SCREENSHOT_PATH")

    apk_path = Path(sys.argv[1])
    screenshot_path = Path(sys.argv[2])
    screenshot_path.parent.mkdir(parents=True, exist_ok=True)
    if not apk_path.is_file():
        raise FileNotFoundError(apk_path)

    adb("wait-for-device", timeout=120)
    adb("install", "-r", str(apk_path), timeout=180)

    # Deliberately remove connectivity before opening VISTA.
    adb("shell", "svc", "wifi", "disable")
    adb("shell", "svc", "data", "disable")
    adb("shell", "am", "start", "-n", "dz.vista.elbayadh/.MainActivity")
    time.sleep(5)

    tap_text("ابدأ الرحلة")
    time.sleep(2)
    tap_text("الخريطة")
    time.sleep(18)

    device_path = "/sdcard/vista-el-bayadh-map.png"
    adb("shell", "screencap", "-p", device_path)
    adb("pull", device_path, str(screenshot_path), timeout=30)

    image = Image.open(screenshot_path).convert("RGB")
    width, height = image.size
    # Exclude the status and bottom navigation bars; inspect only the map canvas.
    map_canvas = image.crop((0, int(height * 0.06), width, int(height * 0.87)))
    colors = map_canvas.getcolors(maxcolors=1_000_000) or []
    unique_colors = len(colors)
    background = (233, 227, 215)  # Local style's map background.
    non_background_pixels = sum(
        count
        for color, count in colors
        if sum((color[i] - background[i]) ** 2 for i in range(3)) > 18 ** 2
    )

    if unique_colors < 30 or non_background_pixels < 500:
        raise AssertionError(
            "Offline map did not render visible vector features: "
            f"{unique_colors} colors, {non_background_pixels} non-background pixels. "
            f"Screenshot: {screenshot_path}"
        )

    print(
        "OFFLINE MAP VISUAL TEST PASSED: "
        f"{width}x{height}, {unique_colors} colors, "
        f"{non_background_pixels} non-background map pixels."
    )


if __name__ == "__main__":
    main()
