# Thesis Highlights — notable technical work

Running list of the most impressive / non-obvious engineering decisions, for the write-up.
Brief by design — expand later. Newest at the bottom of each section.

## Architecture & data integrity
- Immutable workout session snapshots — editing/deleting source template/exercise/routine/gym/equipment never alters recorded history.
- Two-level snapshotting — template-day captures exercise name/type/muscles; session snapshots again at start.
- Soft-delete everywhere it matters (templates, exercises, routines, gyms, equipment); sessions never physically deleted.
- Package-by-feature with strict domain/application/infrastructure/web layering.
- Marketplace copy = snapshot-preserving deep copy: official exercise refs kept, foreign/custom refs + routine refs nulled, snapshots retained → copied template fully usable & independent (only link is copiedFromTemplateId).
- Entire Phase 7 (publish, vote, save, copy, stats) shipped with zero schema changes — V1 already modelled the marketplace.
- Deterministic, rule-based evidence-informed template analyzer (weighted set volume, coverage, frequency, push/pull & posterior/quad balance, rest, rep extremes, difficulty), explainable "Template Structure Score" with a fixed deduction table — no ML, honest disclaimer, never claims optimality.
- Analyzer kept architecturally clean: a template application-level read model (not web DTOs); missing planned-set counts are excluded (never fabricated) and flagged as incomplete.

## Concurrency & DB constraints
- One-active-workout-per-user enforced by a **partial unique index** (`WHERE status='IN_PROGRESS'`), not just a service check.
- DB constraint violations mapped to clean 409s (active workout, duplicate set number) instead of 500s.
- Soft-delete-aware uniqueness via partial unique indexes (`WHERE deleted_at IS NULL`) — names reusable after delete.
- Vote/save counters updated via atomic clamped SQL (`GREATEST(0, count ± delta)`) — no read-modify-write race, no negative counts; counters move only on a real row change.

## Security
- IDOR-safe by design — owner-scoped queries return 404 (not 403) so existence isn't leaked; ownership chains for nested resources.
- Refresh-token rotation — opaque tokens stored as SHA-256 hashes, rotated transactionally; reuse of a stolen token is detected.
- Access token in memory only; HttpOnly + SameSite=Strict refresh cookie; profile-based Secure flag; CORS with credentials.

## Performance / clever solutions
- Template aggregates recomputed Postgres-side in one native `array_agg(DISTINCT ... ORDER BY ...)` query — no Java array binding, sidesteps JPA array/`ddl-auto:validate` fragility.
- Deterministic ORDER BY inside aggregates to keep tests non-flaky.
- Batch child loads + grouping when assembling template/session detail trees (avoids N+1).
- Flush-between-clear-and-insert when re-snapshotting muscle groups (avoids composite-PK collision in one tx).
- Analytics fully Postgres-side: `date_trunc` weekly buckets, `DISTINCT ON` best-set, estimated-1RM (Epley) ranking, primary-muscle set distribution.
- Per-exercise estimated-1RM progression series (best 1RM per day per exercise) → multi-line strength-progress chart.
- History list paginated with one batch summary query (set count / volume per page) instead of N+1.
- Custom page-envelope DTO to avoid Spring `PageImpl`'s unstable JSON.

## Testing & correctness
- Real PostgreSQL in every integration test (Testcontainers, no H2); full schema exercised via Flyway.
- Tests assert DB-level guarantees, not just service behavior (e.g. raw insert rejected by the active-workout index; aggregates read back via JdbcTemplate).
- Root-caused a flaky JWT-tamper test to base64url unused bits in the HMAC signature; made the tamper deterministic.
- Handled Spring Data merge-vs-persist subtlety (pre-assigned UUID ids) by building responses from the returned managed entity so generated timestamps appear.
