#!/usr/bin/env python3
"""Pack Offipe drawable assets from `artifacts/`.

Sources expected (missing files are skipped with a note):
  artifacts/icon.png  -> launcher icon family (adaptive foreground + legacy
                         mipmap buckets) + drawable/offipe_logo.png

Run from the repo root:
    python3 scripts/pack_assets.py
"""
from __future__ import annotations

from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ARTIFACTS = ROOT / "artifacts"
RES = ROOT / "app" / "src" / "main" / "res"
DRAWABLE = RES / "drawable"
DRAWABLE.mkdir(parents=True, exist_ok=True)

ADAPTIVE_SAFE_FRACTION = 0.78
LEGACY_SAFE_FRACTION = 0.96
LEGACY_BUCKETS = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}


def pad_to_canvas(src: Image.Image, canvas_size: int, content_fraction: float) -> Image.Image:
    """Square RGBA canvas at [canvas_size] containing [src] resized so its
    longest edge is canvas_size * content_fraction, centered, transparent
    margins."""
    canvas = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    target_edge = int(canvas_size * content_fraction)
    resized = src.copy()
    resized.thumbnail((target_edge, target_edge), Image.LANCZOS)
    x = (canvas_size - resized.width) // 2
    y = (canvas_size - resized.height) // 2
    canvas.paste(resized, (x, y), resized)
    return canvas


def pack_icon() -> None:
    src_path = ARTIFACTS / "icon.png"
    if not src_path.exists():
        print(f"[skip] icon source missing: {src_path}")
        return
    src = Image.open(src_path).convert("RGBA")

    foreground = pad_to_canvas(src, 432, ADAPTIVE_SAFE_FRACTION)
    fg_path = DRAWABLE / "ic_launcher_foreground.png"
    foreground.save(fg_path, "PNG", optimize=True)
    print(f"[ok] {fg_path.relative_to(ROOT)}")

    for bucket, dim in LEGACY_BUCKETS.items():
        padded = pad_to_canvas(src, dim, LEGACY_SAFE_FRACTION)
        out_dir = RES / bucket
        out_dir.mkdir(parents=True, exist_ok=True)
        for name in ("ic_launcher.png", "ic_launcher_round.png"):
            padded.save(out_dir / name, "PNG", optimize=True)
        print(f"[ok] {bucket}/ic_launcher{{,_round}}.png ({dim}x{dim})")

    in_app = pad_to_canvas(src, 512, 0.92)
    logo_path = DRAWABLE / "offipe_logo.png"
    in_app.save(logo_path, "PNG", optimize=True)
    print(f"[ok] {logo_path.relative_to(ROOT)}")


def main() -> int:
    pack_icon()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
