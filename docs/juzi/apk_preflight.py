#!/usr/bin/env python3
"""
Quick APK integrity preflight before BrowserStack upload.
"""

from __future__ import annotations

import argparse
import hashlib
import sys
import zipfile
from pathlib import Path


def sha256sum(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def main() -> int:
    parser = argparse.ArgumentParser(description="APK preflight checks")
    parser.add_argument("apk", type=Path, help="Path to APK file")
    args = parser.parse_args()

    apk = args.apk
    if not apk.exists():
        print(f"[FAIL] file not found: {apk}")
        return 2
    if not apk.is_file():
        print(f"[FAIL] not a file: {apk}")
        return 2

    size = apk.stat().st_size
    print(f"[INFO] file={apk}")
    print(f"[INFO] size_bytes={size}")
    print(f"[INFO] sha256={sha256sum(apk)}")

    if size <= 0:
        print("[FAIL] APK is zero-byte.")
        return 3
    if size < 2 * 1024 * 1024:
        print("[WARN] APK is very small (<2MB); verify this is expected.")

    try:
        with zipfile.ZipFile(apk, "r") as zf:
            names = set(zf.namelist())
    except zipfile.BadZipFile:
        print("[FAIL] APK is not a valid ZIP container.")
        return 4

    required = {"AndroidManifest.xml", "classes.dex"}
    missing = sorted(required - names)
    if missing:
        print(f"[FAIL] missing required entries: {', '.join(missing)}")
        return 5

    print("[OK] APK preflight passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())

