package com.evolyn.evolyn_data_service.Activities;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Checks the generated catalogue (CSV + V4 seed) against the Compendium's rules:
 * every code once, heading = first two digits, sensible MET values, and the
 * entries Evolyn relies on directly.
 */
class CompendiumCatalogueTest {

    private record Row(String code, String heading, BigDecimal met, String description) {}

    private static final List<Row> ROWS = new ArrayList<>();
    private static String seedSql;

    @BeforeAll
    static void load() throws IOException {
        String csv = read("/compendium/adult_compendium_2024.csv");
        String[] lines = csv.split("\\r?\\n");
        assertThat(lines[0]).isEqualTo("activity_code,heading_code,heading,met_value,description");
        for (int i = 1; i < lines.length; i++) {
            List<String> f = parseCsvLine(lines[i]);
            ROWS.add(new Row(f.get(0), f.get(1), new BigDecimal(f.get(3)), f.get(4)));
        }
        seedSql = read("/db/migration/V4__seed_compendium_2024.sql");
    }

    @Test
    void hasEveryActivityOfThe2024AdultCompendium() {
        assertThat(ROWS).hasSize(1111);
        assertThat(ROWS.stream().map(Row::heading).distinct()).hasSize(22);
    }

    @Test
    void codesAreUniqueAndMatchTheirHeading() {
        Set<String> seen = new HashSet<>();
        for (Row r : ROWS) {
            assertThat(r.code()).matches("\\d{5}");
            assertThat(seen.add(r.code())).as("duplicate code %s", r.code()).isTrue();
            assertThat(r.code()).startsWith(r.heading());
        }
    }

    @Test
    void metValuesAreSensible() {
        for (Row r : ROWS) {
            assertThat(r.met()).as("MET of %s", r.code())
                    .isBetween(new BigDecimal("0.5"), new BigDecimal("25"));
            assertThat(r.description()).isNotBlank();
        }
    }

    @Test
    void handballEntriesAreExact() {
        Map<String, Row> byCode = new HashMap<>();
        ROWS.forEach(r -> byCode.put(r.code(), r));
        assertThat(byCode.get("15330").met()).isEqualByComparingTo("8.0");
        assertThat(byCode.get("15330").description()).isEqualTo("Handball, team");
        assertThat(byCode.get("15320").met()).isEqualByComparingTo("12.0");
    }

    @Test
    void seedMatchesTheCsvRowForRow() {
        Pattern row = Pattern.compile("\\('2024_ADULT', '(\\d{5})', '(\\d{2})', ([0-9.]+), ");
        Matcher m = row.matcher(seedSql);
        Map<String, BigDecimal> seeded = new HashMap<>();
        while (m.find()) seeded.put(m.group(1), new BigDecimal(m.group(3)));
        assertThat(seeded).hasSize(ROWS.size());
        for (Row r : ROWS) {
            assertThat(seeded.get(r.code())).as("seed MET of %s", r.code()).isEqualByComparingTo(r.met());
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────
    private static String read(String path) throws IOException {
        try (InputStream in = CompendiumCatalogueTest.class.getResourceAsStream(path)) {
            assertThat(in).as(path).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // Minimal CSV: commas inside "quoted" fields, "" for a literal quote
    private static List<String> parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (quoted) {
                if (ch == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                else if (ch == '"') quoted = false;
                else cur.append(ch);
            } else if (ch == '"') quoted = true;
            else if (ch == ',') { out.add(cur.toString()); cur.setLength(0); }
            else cur.append(ch);
        }
        out.add(cur.toString());
        return out;
    }
}
