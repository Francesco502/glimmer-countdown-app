#!/usr/bin/env python3
"""Reproducible subsetting of Glimmer's bundled fonts.

Pinned toolchain: Python 3.12, fonttools==4.54.1 (with brotli installed).
Other fontTools versions may produce byte-different output.

Usage (from the repository root):

    python -I scripts/fonts/subset-fonts.py <source-font-dir> [--serif-mode static|variable]

--serif-mode variable (default) keeps a single subsetted variable font
(glimmer_serif.ttf, wght 200-900); Compose selects weights through FontVariation
settings. About 5.8MB, versus about 13.7MB for four static instances.
--serif-mode static writes four static instances (wght 300/400/500/600).

Glimmer Xiaowei is display-only and uses a smaller charset: GB2312 level-1 hanzi
(3,755) instead of all of GB2312, plus the same non-hanzi, string and lunar sets.

<source-font-dir> must contain the unmodified upstream files:

    NotoSerifSC (variable, wght 200-900)   -> noto_serif_sc.ttf
    ZCOOL XiaoWei Regular                  -> zcool_xiaowei_regular.ttf

These are the files previously shipped in app/src/main/res/font/ and remain
recoverable from git history (e.g. `git show f57d729:app/src/main/res/font/noto_serif_sc.ttf`).

Outputs (written to app/src/main/res/font/):

    glimmer_serif.ttf  (variable, default) or
    glimmer_serif_light.ttf / _regular / _medium / _semibold  (--serif-mode static)
    glimmer_xiaowei_regular.ttf

Both upstream fonts are licensed under SIL OFL 1.1. Modified versions are renamed
("Glimmer Serif", "Glimmer Xiaowei") so that no original font name is reused,
see docs/licenses/FONT-MODIFICATIONS.md.

The resolved character set is written to scripts/fonts/glimmer-charset.txt and
SHA-256 sums of inputs/outputs to scripts/fonts/fonts.sha256.
"""

from __future__ import annotations

import hashlib
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

import fontTools
from fontTools import subset
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

EXPECTED_FONTTOOLS = "4.54.1"

REPO = Path(__file__).resolve().parents[2]
SCRIPT_DIR = Path(__file__).resolve().parent
RES_DIR = REPO / "app" / "src" / "main" / "res"
FONT_OUT = RES_DIR / "font"
JAVA_DIR = REPO / "app" / "src" / "main" / "java"
EXTRA_CHARS_FILE = SCRIPT_DIR / "charset-extra.txt"
CHARSET_OUT = SCRIPT_DIR / "glimmer-charset.txt"
XIAOWEI_CHARSET_OUT = SCRIPT_DIR / "glimmer-xiaowei-charset.txt"
SHA_OUT = SCRIPT_DIR / "fonts.sha256"

SERIF_FAMILY = "Glimmer Serif"
SERIF_PS = "GlimmerSerif"
XIAOWEI_FAMILY = "Glimmer Xiaowei"
XIAOWEI_PS = "GlimmerXiaowei"

SERIF_WEIGHTS = [
    # (wght, style name, output file)
    (300, "Light", "glimmer_serif_light.ttf"),
    (400, "Regular", "glimmer_serif_regular.ttf"),
    (500, "Medium", "glimmer_serif_medium.ttf"),
    (600, "SemiBold", "glimmer_serif_semibold.ttf"),
]

UNICODE_RANGES = [
    (0x0020, 0x007E),  # ASCII printable
    (0x00A0, 0x00FF),  # Latin-1 supplement (punctuation, symbols, letters)
    (0x2010, 0x2027),  # dashes, quotes, ellipsis, middle dots
    (0x2030, 0x203B),  # per mille, primes, reference mark
    (0x2160, 0x216B),  # Roman numerals
    (0x2190, 0x2193),  # basic arrows
    (0x2460, 0x2473),  # circled numbers
    (0x3000, 0x303F),  # CJK symbols and punctuation
    (0xFF00, 0xFFEF),  # halfwidth and fullwidth forms
]


def gb2312_hanzi(level1_only: bool = False) -> set[str]:
    """GB2312 hanzi: level 1 is rows 16-55 (3,755 chars), level 2 rows 56-87."""
    chars: set[str] = set()
    for hi in range(0xB0, 0xD8 if level1_only else 0xF8):
        for lo in range(0xA1, 0xFF):
            try:
                chars.add(bytes([hi, lo]).decode("gb2312"))
            except UnicodeDecodeError:
                pass
    return chars


def gb2312_symbols() -> set[str]:
    """GB2312 rows 1-9 (punctuation, fullwidth, kana, Greek, Cyrillic, box drawing)."""
    chars: set[str] = set()
    for hi in range(0xA1, 0xAA):
        for lo in range(0xA1, 0xFF):
            try:
                chars.add(bytes([hi, lo]).decode("gb2312"))
            except UnicodeDecodeError:
                pass
    return chars


def strings_xml_chars() -> set[str]:
    chars: set[str] = set()
    for path in sorted(RES_DIR.glob("values*/strings.xml")):
        root = ET.parse(path).getroot()
        for element in root.iter():
            if element.text:
                chars.update(element.text)
            if element.tail:
                chars.update(element.tail)
    return chars


def kotlin_cjk_chars() -> set[str]:
    """CJK characters hard-coded in Kotlin sources (lunar calendar tables etc.)."""
    pattern = re.compile(r"[　-〿㐀-鿿＀-￯]")
    chars: set[str] = set()
    for path in sorted(JAVA_DIR.rglob("*.kt")):
        chars.update(pattern.findall(path.read_text(encoding="utf-8")))
    return chars


def extra_chars() -> set[str]:
    chars: set[str] = set()
    for line in EXTRA_CHARS_FILE.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        chars.update(c for c in line if not c.isspace())
    return chars


def build_charset(level1_only: bool = False) -> list[int]:
    chars: set[str] = set()
    chars |= gb2312_hanzi(level1_only)
    chars |= gb2312_symbols()
    chars |= strings_xml_chars()
    chars |= kotlin_cjk_chars()
    chars |= extra_chars()
    for start, end in UNICODE_RANGES:
        chars.update(chr(cp) for cp in range(start, end + 1))
    codepoints = sorted(ord(c) for c in chars if ord(c) >= 0x20 and not (0x7F <= ord(c) < 0xA0))
    return codepoints


def subset_options() -> subset.Options:
    options = subset.Options()
    options.layout_features = ["*"]
    options.name_IDs = ["*"]
    options.name_languages = ["*"]
    options.hinting = False
    options.drop_tables += ["DSIG", "vhea", "vmtx", "VORG"]
    options.glyph_names = False
    options.notdef_outline = True
    options.recalc_timestamp = False
    options.prune_unicode_ranges = True
    return options


def subset_font(font: TTFont, codepoints: list[int]) -> None:
    subsetter = subset.Subsetter(options=subset_options())
    subsetter.populate(unicodes=codepoints)
    subsetter.subset(font)


def rename(font: TTFont, family: str, ps_family: str, style: str, weight_class: int) -> None:
    name = font["name"]
    original_version = name.getDebugName(5) or "Version 1.000"
    for name_id in (1, 2, 3, 4, 6, 16, 17, 18, 21, 22, 25):
        name.removeNames(nameID=name_id)

    ribbi = style == "Regular"
    legacy_family = family if ribbi else f"{family} {style}"
    full_name = family if ribbi else f"{family} {style}"
    ps_name = f"{ps_family}-{style}"
    names = {
        1: legacy_family,
        2: "Regular",
        3: f"{original_version.split(';')[0].replace('Version ', '')};GLIMMER;{ps_name}",
        4: full_name,
        6: ps_name,
    }
    if not ribbi:
        names[16] = family
        names[17] = style
    for name_id, value in names.items():
        name.setName(value, name_id, 3, 1, 0x409)
        name.setName(value, name_id, 1, 0, 0)
    name.setName(
        "Subsetted and renamed for the Glimmer countdown app under SIL OFL 1.1 section 3.",
        10, 3, 1, 0x409,
    )

    os2 = font["OS/2"]
    os2.usWeightClass = weight_class
    # fsSelection: clear ITALIC(0) and BOLD(5); set REGULAR(6) only for the Regular style.
    os2.fsSelection &= ~((1 << 0) | (1 << 5) | (1 << 6))
    if ribbi:
        os2.fsSelection |= 1 << 6
    font["head"].macStyle = 0


def save(font: TTFont, path: Path, modified: int) -> None:
    font.recalcTimestamp = False
    font["head"].modified = modified
    font.save(str(path))


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main(argv: list[str]) -> int:
    serif_mode = "variable"
    if "--serif-mode" in argv:
        index = argv.index("--serif-mode")
        serif_mode = argv[index + 1] if index + 1 < len(argv) else ""
        argv = argv[:index] + argv[index + 2:]
    if len(argv) != 2 or serif_mode not in ("static", "variable"):
        print(__doc__)
        return 2
    if fontTools.version != EXPECTED_FONTTOOLS:
        print(f"warning: fonttools {fontTools.version} != pinned {EXPECTED_FONTTOOLS}", file=sys.stderr)

    src_dir = Path(argv[1]).resolve()
    serif_src = src_dir / "noto_serif_sc.ttf"
    xiaowei_src = src_dir / "zcool_xiaowei_regular.ttf"
    for path in (serif_src, xiaowei_src):
        if not path.is_file():
            print(f"missing source font: {path}", file=sys.stderr)
            return 1
        if path.resolve().parent == FONT_OUT.resolve():
            print("source fonts must not live in app/src/main/res/font", file=sys.stderr)
            return 1

    codepoints = build_charset()
    xiaowei_codepoints = build_charset(level1_only=True)
    for path, cps in ((CHARSET_OUT, codepoints), (XIAOWEI_CHARSET_OUT, xiaowei_codepoints)):
        path.write_text(
            "# Generated by scripts/fonts/subset-fonts.py - do not edit by hand.\n"
            + "".join(chr(cp) for cp in cps if cp > 0x20)
            + "\n",
            encoding="utf-8",
        )
    # Remove outputs of the other serif mode so stale resources never linger.
    stale = [name for _, _, name in SERIF_WEIGHTS] if serif_mode == "variable" else ["glimmer_serif.ttf"]
    for name in stale:
        (FONT_OUT / name).unlink(missing_ok=True)
    print(f"charset: serif {len(codepoints)}, xiaowei {len(xiaowei_codepoints)} code points")

    outputs: list[Path] = []

    # Noto Serif SC: subset the variable font first (fast), then pin static instances.
    variable = TTFont(str(serif_src))
    modified = variable["head"].modified
    subset_font(variable, codepoints)
    if serif_mode == "variable":
        default_wght = int(next(a.defaultValue for a in variable["fvar"].axes if a.axisTag == "wght"))
        rename(variable, SERIF_FAMILY, SERIF_PS, "Regular", default_wght)
        # Named-instance PostScript names (fvar, name IDs >= 256) carry the original name.
        for record in variable["name"].names:
            if record.nameID >= 256 and "NotoSerifSC" in str(record):
                record.string = str(record).replace("NotoSerifSC", SERIF_PS)
        # Keep fvar/STAT naming consistent with the new family.
        out = FONT_OUT / "glimmer_serif.ttf"
        save(variable, out, modified)
        outputs.append(out)
        print(f"{out.name}: {out.stat().st_size} bytes")
    for wght, style, filename in (SERIF_WEIGHTS if serif_mode == "static" else []):
        static = instancer.instantiateVariableFont(
            variable, {"wght": wght}, inplace=False, optimize=True
        )
        for tag in ("STAT", "avar", "fvar", "gvar", "HVAR", "MVAR", "cvar"):
            if tag in static:
                del static[tag]
        rename(static, SERIF_FAMILY, SERIF_PS, style, wght)
        out = FONT_OUT / filename
        save(static, out, modified)
        outputs.append(out)
        print(f"{filename}: {out.stat().st_size} bytes")

    xiaowei = TTFont(str(xiaowei_src))
    modified = xiaowei["head"].modified
    subset_font(xiaowei, xiaowei_codepoints)
    rename(xiaowei, XIAOWEI_FAMILY, XIAOWEI_PS, "Regular", 400)
    out = FONT_OUT / "glimmer_xiaowei_regular.ttf"
    save(xiaowei, out, modified)
    outputs.append(out)
    print(f"{out.name}: {out.stat().st_size} bytes")

    lines = [f"# fonttools {fontTools.version}"]
    for path in (serif_src, xiaowei_src):
        lines.append(f"{sha256(path)}  source/{path.name}")
    for path in outputs:
        lines.append(f"{sha256(path)}  {path.relative_to(REPO).as_posix()}")
    SHA_OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"total: {sum(p.stat().st_size for p in outputs)} bytes")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
