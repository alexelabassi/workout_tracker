package com.thesis.workout.benchmark;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * A <em>fair</em> PostgreSQL search baseline for the benchmark — "PostgreSQL done properly", not a
 * strawman {@code ILIKE '%x%'}. It builds a dedicated denormalised search table (the PG analogue of
 * the OpenSearch index) with:
 * <ul>
 *   <li>a weighted {@code tsvector} (name=A, exercises=B, muscles=C, description=D) + GIN index for
 *       relevance-ranked full-text ({@code ts_rank_cd});</li>
 *   <li>a {@code pg_trgm} trigram column + GIN index for typo-tolerant fuzzy search
 *       ({@code word_similarity} / {@code <%});</li>
 *   <li>btree/GIN indexes on the structured filter columns for filtered + faceted queries.</li>
 * </ul>
 * Created at runtime (no Flyway migration) so it only exists under the {@code benchmark} profile.
 */
@Component
@Profile("benchmark")
@ConditionalOnProperty(name = "app.search.enabled", havingValue = "true")
class PostgresSearchBaseline {

    private static final String TABLE = "bench_template_doc";
    private static final int BATCH = 2000;

    private final JdbcTemplate jdbc;

    PostgresSearchBaseline(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String INSERT_SQL = """
            INSERT INTO bench_template_doc
              (id, owner_user_id, visibility, name, description, exercise_names, muscle_groups,
               muscle_groups_text, split_type, difficulty, days_per_week, saves_count, uses_count,
               structure_score, analysis_category, fulltext, trigram_src)
            VALUES (?::uuid, ?::uuid, ?, ?, ?, ?, ?::text[], ?, ?, ?, ?, ?, ?, ?, ?,
                 setweight(to_tsvector('english', ?), 'A')
              || setweight(to_tsvector('english', ?), 'B')
              || setweight(to_tsvector('english', ?), 'C')
              || setweight(to_tsvector('english', coalesce(?, '')), 'D'),
               ?)""";

    /** Idempotent: extension + table only. Indexes are (re)built around bulk load. pg_trgm is trusted (PG13+). */
    void ensureSchema() {
        jdbc.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS bench_template_doc (
                  id uuid PRIMARY KEY,
                  owner_user_id uuid NOT NULL,
                  visibility varchar(20) NOT NULL,
                  name text NOT NULL,
                  description text,
                  exercise_names text NOT NULL,
                  muscle_groups text[] NOT NULL,
                  muscle_groups_text text NOT NULL,
                  split_type varchar(40),
                  difficulty varchar(40),
                  days_per_week int,
                  saves_count int NOT NULL,
                  uses_count int NOT NULL,
                  structure_score int NOT NULL,
                  analysis_category varchar(40),
                  fulltext tsvector NOT NULL,
                  trigram_src text NOT NULL
                )""");
    }

    /** Start a load: empty the table and drop indexes so bulk insert is fast (build-then-index). */
    void prepareLoad() {
        jdbc.execute("TRUNCATE TABLE bench_template_doc");
        for (String idx : List.of("bench_doc_fulltext_idx", "bench_doc_trgm_idx", "bench_doc_vis_idx",
                "bench_doc_diff_idx", "bench_doc_split_idx", "bench_doc_muscles_idx")) {
            jdbc.execute("DROP INDEX IF EXISTS " + idx);
        }
    }

    void insertBatch(List<SearchDoc> docs) {
        List<Object[]> rows = new ArrayList<>(docs.size());
        for (SearchDoc doc : docs) {
            String muscleArray = "{" + String.join(",", doc.muscleGroups()) + "}";
            String trigramSrc = doc.name() + " " + doc.exerciseNamesText();
            rows.add(new Object[] {
                    doc.id(), doc.ownerUserId(), doc.visibility(), doc.name(), doc.description(),
                    doc.exerciseNamesText(), muscleArray, doc.muscleGroupsText(), doc.splitType(),
                    doc.difficulty(), doc.daysPerWeek(), doc.savesCount(), doc.usesCount(),
                    doc.structureScore(), doc.analysisCategory(),
                    doc.name(), doc.exerciseNamesText(), doc.muscleGroupsText(), doc.description(),
                    trigramSrc });
        }
        // Insert in BATCH-sized chunks regardless of incoming batch size.
        for (int i = 0; i < rows.size(); i += BATCH) {
            jdbc.batchUpdate(INSERT_SQL, rows.subList(i, Math.min(i + BATCH, rows.size())));
        }
    }

    /** Finish a load: build all indexes once over the full table, then ANALYZE for fair query plans. */
    void finishLoad() {
        jdbc.execute("CREATE INDEX bench_doc_fulltext_idx ON bench_template_doc USING GIN (fulltext)");
        jdbc.execute("CREATE INDEX bench_doc_trgm_idx ON bench_template_doc USING GIN (trigram_src gin_trgm_ops)");
        jdbc.execute("CREATE INDEX bench_doc_vis_idx ON bench_template_doc (visibility)");
        jdbc.execute("CREATE INDEX bench_doc_diff_idx ON bench_template_doc (difficulty)");
        jdbc.execute("CREATE INDEX bench_doc_split_idx ON bench_template_doc (split_type)");
        jdbc.execute("CREATE INDEX bench_doc_muscles_idx ON bench_template_doc USING GIN (muscle_groups)");
        jdbc.execute("ANALYZE bench_template_doc");
    }

    // --- queries (each returns the matched ids; latency is measured by the harness) ---

    List<String> fullText(String query, int topK) {
        return jdbc.query(
                "SELECT id FROM bench_template_doc "
                        + "WHERE visibility = 'PUBLIC' AND fulltext @@ plainto_tsquery('english', ?) "
                        + "ORDER BY ts_rank_cd(fulltext, plainto_tsquery('english', ?)) DESC, id LIMIT ?",
                (rs, n) -> rs.getString(1), query, query, topK);
    }

    List<String> filtered(String query, String difficulty, String splitType, int topK) {
        return jdbc.query(
                "SELECT id FROM bench_template_doc "
                        + "WHERE visibility = 'PUBLIC' AND difficulty = ? AND split_type = ? "
                        + "AND fulltext @@ plainto_tsquery('english', ?) "
                        + "ORDER BY ts_rank_cd(fulltext, plainto_tsquery('english', ?)) DESC, id LIMIT ?",
                (rs, n) -> rs.getString(1), difficulty, splitType, query, query, topK);
    }

    /** Typo-tolerant fuzzy via pg_trgm word similarity, with the threshold set on the same connection. */
    List<String> fuzzy(String query, int topK) {
        return jdbc.execute((java.sql.Connection con) -> {
            try (Statement st = con.createStatement()) {
                st.execute("SET pg_trgm.word_similarity_threshold = 0.3");
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT id FROM bench_template_doc "
                            + "WHERE visibility = 'PUBLIC' AND ? <% trigram_src "
                            + "ORDER BY word_similarity(?, trigram_src) DESC, id LIMIT ?")) {
                ps.setString(1, query);
                ps.setString(2, query);
                ps.setInt(3, topK);
                List<String> ids = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ids.add(rs.getString(1));
                    }
                }
                return ids;
            }
        });
    }

    /** Facet (terms aggregation analogue): difficulty counts over the full-text match set. */
    int facetByDifficulty(String query) {
        List<?> rows = jdbc.query(
                "SELECT difficulty, count(*) FROM bench_template_doc "
                        + "WHERE visibility = 'PUBLIC' AND fulltext @@ plainto_tsquery('english', ?) "
                        + "GROUP BY difficulty",
                (rs, n) -> rs.getString(1), query);
        return rows.size();
    }

    long storageBytes() {
        Long bytes = jdbc.queryForObject("SELECT pg_total_relation_size('bench_template_doc')", Long.class);
        return bytes != null ? bytes : 0L;
    }
}
