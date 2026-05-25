#!/usr/bin/env python3
"""Lociant WebUI build script.

Concatenates source modules from web-src/ into src/main/assets/web/.
Android reads assets from src/main/assets/, so this is the correct output directory.

Usage:
    python apps/android/app/src/main/web-src/build.py
    cd apps/android/app/src/main && python web-src/build.py
    cd apps/android/app/src/main/web-src && python build.py
"""

from pathlib import Path
import shutil


def script_dir() -> Path:
    return Path(__file__).resolve().parent


def src_main_dir() -> Path:
    """src/main/ directory (parent of web-src/)."""
    return script_dir().parent


def assets_dir() -> Path:
    """Output directory: src/main/assets/web/ (Android's default assets path)."""
    return src_main_dir() / "assets" / "web"


def src_dir() -> Path:
    return script_dir()


def build() -> None:
    src = src_dir()
    out = assets_dir()
    out.mkdir(parents=True, exist_ok=True)

    # 1. Concatenate JS modules in order
    js_sources = sorted((src / "js").glob("*.js"))
    js_output = []
    for js_file in js_sources:
        content = js_file.read_text(encoding="utf-8")
        js_output.append(f"/* === {js_file.name} === */\n{content}")

    (out / "app.js").write_text(
        "\n\n".join(js_output),
        encoding="utf-8",
    )
    print(f"[build] wrote app.js ({len(js_output)} modules, {len(js_output[0]) + sum(len(c) for c in js_output[1:]):,} chars)")

    # 2. Copy CSS
    css_src = src / "css" / "styles.css"
    if css_src.is_file():
        shutil.copy(css_src, out / "styles.css")
        size = css_src.stat().st_size
        print(f"[build] copied styles.css ({size:,} bytes)")
    else:
        print(f"[build] WARNING: {css_src} not found, styles.css unchanged")

    # 3. Copy HTML
    html_src = src / "html" / "index.html"
    if html_src.is_file():
        html = html_src.read_text(encoding="utf-8").rstrip() + "\n"
        (out / "index.html").write_text(html, encoding="utf-8")
        size = html_src.stat().st_size
        print(f"[build] copied index.html ({size:,} bytes)")
    else:
        print(f"[build] WARNING: {html_src} not found, index.html unchanged")

    # 4. Copy vendored browser libraries
    vendor_src = src / "vendor"
    if vendor_src.is_dir():
        vendor_out = out / "vendor"
        if vendor_out.exists():
            shutil.rmtree(vendor_out)
        shutil.copytree(vendor_src, vendor_out)
        count = len(list(vendor_out.glob("*")))
        print(f"[build] copied vendor ({count} files)")

    print(f"[build] done. Output: {out.resolve()}")


if __name__ == "__main__":
    build()
