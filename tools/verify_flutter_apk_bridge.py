#!/usr/bin/env python3
"""Verify that a Flutter APK packages the getter native bridge."""

from __future__ import annotations

import sys
import zipfile
from pathlib import Path

EXPECTED_ABIS = ("arm64-v8a", "armeabi-v7a", "x86_64")
DEX_MARKERS = (
    b"net/xzos/upgradeall/getter/NativeLib",
    b"net/xzos/upgradeall/getter/platform/InstalledInventoryProvider",
)


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: verify_flutter_apk_bridge.py <apk>", file=sys.stderr)
        return 2

    apk = Path(sys.argv[1])
    missing: list[str] = []
    with zipfile.ZipFile(apk) as archive:
        names = set(archive.namelist())
        missing.extend(
            f"lib/{abi}/libapi_proxy.so"
            for abi in EXPECTED_ABIS
            if f"lib/{abi}/libapi_proxy.so" not in names
        )
        dex = b"".join(
            archive.read(name)
            for name in names
            if name.startswith("classes") and name.endswith(".dex")
        )

    missing.extend(marker.decode() for marker in DEX_MARKERS if marker not in dex)
    if missing:
        print(f"missing from {apk}: {', '.join(missing)}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
