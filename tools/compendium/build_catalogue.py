#!/usr/bin/env python3
"""
Turns the text of the 2024 Adult Compendium of Physical Activities PDF into
  1. src/main/resources/compendium/adult_compendium_2024.csv  (readable copy, one row per activity)
  2. src/main/resources/db/migration/V4__seed_compendium_2024.sql  (the Flyway seed)

Source: https://pacompendium.com/adult-compendium/ (PDF "1_2024 Adult Compendium").
Herrmann SD, Willis EA, Ainsworth BE, et al. 2024 Adult Compendium of Physical
Activities: A third update of the energy costs of human activities.
J Sport Health Sci 2024;13(1):6-12. https://doi.org/10.1016/j.jshs.2024.101974

The Compendium asks that MET values are never changed or combined, so this
script copies them exactly and stops on anything it can't read with certainty.

Usage (from the Evolyn-Data-Service folder):
  osascript -l JavaScript tools/compendium/extract_pdf_text.js <compendium.pdf> > /tmp/compendium.txt
  python3 -I tools/compendium/build_catalogue.py /tmp/compendium.txt
"""
import csv
import re
import sys
from pathlib import Path

EDITION = "2024_ADULT"

# The 22 major headings; the first two digits of every activity code
HEADINGS = {
    "01": "Bicycling", "02": "Conditioning Exercise", "03": "Dancing",
    "04": "Fishing & Hunting", "05": "Home Activities", "06": "Home Repair",
    "07": "Inactivity", "08": "Lawn & Garden", "09": "Miscellaneous",
    "10": "Music Playing", "11": "Occupation", "12": "Running",
    "13": "Self Care", "14": "Sexual Activity", "15": "Sports",
    "16": "Transportation", "17": "Walking", "18": "Water Activities",
    "19": "Winter Activities", "20": "Religious Activities",
    "21": "Volunteer Activities", "22": "Video Games",
}

ROW = re.compile(r"^(?P<heading>.+?) (?P<code>\d{5}) (?P<met>\d{1,2}\.\d{1,2}) (?P<desc>.+)$")
CODES = re.compile(r"\b\d{5}\b")
# Several codes in a row: the PDF flattened a block of table rows into one line
FLATTENED = re.compile(r"^(?P<heading>.+?) (?P<codes>\d{5}(?: \d{5})+) (?P<met>\d{1,2}\.\d{1,2}) (?P<desc>.+)$")
MET_DESC = re.compile(r"^(?P<met>\d{1,2}\.\d{1,2}) (?P<desc>.+)$")
HEADER = "Major Heading Activity Code MET Value Activity Description"


def parse(text: str):
    rows = []          # [code, met, description]
    pending = []       # codes waiting for "MET description" lines (a table the PDF flattened)
    for raw in text.splitlines():
        line = " ".join(raw.split())
        if not line or line.startswith("=====PAGE") or HEADER in line:
            continue

        flat = FLATTENED.match(line)
        if flat:
            # e.g. "Video Games Video Games 22200 22240 3.0 Active video game ..."
            # The codes come first, then one "MET description" per code, one per line.
            pending = CODES.findall(flat["codes"])
            rows.append([pending.pop(0), flat["met"], flat["desc"]])
            continue

        if pending:
            m = MET_DESC.match(line)
            if not m:
                raise SystemExit(f"Expected 'MET description' for code {pending[0]}: {line!r}")
            rows.append([pending.pop(0), m["met"], m["desc"]])
            continue

        m = ROW.match(line)
        if m:
            # The heading printed on the row must match the code's first two digits
            if m["heading"] != HEADINGS.get(m["code"][:2]):
                raise SystemExit(f"Heading {m['heading']!r} doesn't match code {m['code']}: {line!r}")
            rows.append([m["code"], m["met"], m["desc"]])
        elif rows:
            # A description that wrapped onto the next line
            rows[-1][2] = f"{rows[-1][2]} {line}"
        else:
            raise SystemExit(f"Unexpected line before the first activity: {line!r}")

    if pending:
        raise SystemExit(f"Codes without a MET value: {pending}")
    return rows


def check(rows):
    seen = set()
    for code, met, desc in rows:
        if code in seen:
            raise SystemExit(f"Duplicate activity code {code}")
        seen.add(code)
        if code[:2] not in HEADINGS:
            raise SystemExit(f"Unknown heading for code {code}")
        value = float(met)
        if not 0.5 <= value <= 25:
            raise SystemExit(f"MET {met} for {code} looks wrong")
        if not desc.strip():
            raise SystemExit(f"Empty description for {code}")
    # Values Evolyn relies on directly
    lookup = {c: m for c, m, _ in rows}
    for code, expected in {"15330": 8.0, "15320": 12.0}.items():
        if float(lookup.get(code, "nan")) != expected:
            raise SystemExit(f"Spot check failed: {code} should be MET {expected}")


def sql_text(s: str) -> str:
    return "'" + s.replace("'", "''") + "'"


def main():
    if len(sys.argv) != 2:
        raise SystemExit(__doc__)
    rows = parse(Path(sys.argv[1]).read_text(encoding="utf-8"))
    # "8.00" and "8.0" are the same value; write one decimal unless a second one is needed
    rows = [[c, m[:-1] if re.fullmatch(r"\d+\.\d0", m) else m, d.strip()] for c, m, d in rows]
    check(rows)

    root = Path(__file__).resolve().parents[2]
    csv_path = root / "src/main/resources/compendium/adult_compendium_2024.csv"
    sql_path = root / "src/main/resources/db/migration/V4__seed_compendium_2024.sql"
    csv_path.parent.mkdir(parents=True, exist_ok=True)

    with csv_path.open("w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["activity_code", "heading_code", "heading", "met_value", "description"])
        for code, met, desc in rows:
            w.writerow([code, code[:2], HEADINGS[code[:2]], met, desc])

    by_heading = {}
    for code, met, desc in rows:
        by_heading.setdefault(code[:2], []).append((code, met, desc))

    lines = [
        "-- V4: every activity in the 2024 Adult Compendium of Physical Activities.",
        "-- GENERATED by tools/compendium/build_catalogue.py - do not edit by hand.",
        "-- Source: https://pacompendium.com/adult-compendium/",
        "-- Herrmann SD, et al. 2024 Adult Compendium of Physical Activities. J Sport Health Sci 2024;13(1):6-12.",
        "-- MET values are copied exactly, as the Compendium asks.",
        f"-- {len(rows)} activities in {len(by_heading)} headings.",
        "",
        "INSERT INTO compendium_headings (heading_code, name) VALUES",
        ",\n".join(f"    ('{h}', {sql_text(HEADINGS[h])})" for h in sorted(HEADINGS)) + ";",
        "",
    ]
    for h in sorted(by_heading):
        lines.append(f"-- {h} {HEADINGS[h]}")
        lines.append("INSERT INTO activity_definitions (edition, activity_code, heading_code, met_value, description) VALUES")
        lines.append(",\n".join(
            f"    ('{EDITION}', '{code}', '{h}', {met}, {sql_text(desc)})" for code, met, desc in by_heading[h]
        ) + ";")
        lines.append("")
    sql_path.write_text("\n".join(lines), encoding="utf-8")

    print(f"{len(rows)} activities in {len(by_heading)} headings")
    for h in sorted(by_heading):
        print(f"  {h} {HEADINGS[h]:<22} {len(by_heading[h]):>4}")
    print(f"wrote {csv_path.relative_to(root)}")
    print(f"wrote {sql_path.relative_to(root)}")


if __name__ == "__main__":
    main()
