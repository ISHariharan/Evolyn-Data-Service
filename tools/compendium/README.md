# Compendium import

Evolyn estimates calories with MET values from the
**2024 Adult Compendium of Physical Activities** (https://pacompendium.com/adult-compendium/).

> Herrmann SD, Willis EA, Ainsworth BE, et al. 2024 Adult Compendium of Physical Activities:
> A third update of the energy costs of human activities. *J Sport Health Sci* 2024;13(1):6-12.
> https://doi.org/10.1016/j.jshs.2024.101974

The Compendium is free for commercial use, must be cited, and its MET values must not be
changed or combined. The scripts here copy them exactly.

## What's generated

| File | What |
|---|---|
| `src/main/resources/compendium/adult_compendium_2024.csv` | Readable copy: code, heading, MET, description |
| `src/main/resources/db/migration/V4__seed_compendium_2024.sql` | Flyway seed for `compendium_headings` and `activity_definitions` |

## Re-running

The PDF itself is not kept in the repo. Download "1_2024 Adult Compendium" from the link above, then:

```bash
osascript -l JavaScript tools/compendium/extract_pdf_text.js ~/Downloads/1_2024-adult-compendium_1_2024.pdf > /tmp/compendium.txt
python3 -I tools/compendium/build_catalogue.py /tmp/compendium.txt
```

The script stops if anything looks wrong (duplicate code, heading that doesn't match the code,
MET outside 0.5-25, the handball spot checks). Note that V4 has already run on existing
databases: a corrected list must go into a NEW migration, never an edited V4.
