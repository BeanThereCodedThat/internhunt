-- ============================================================================
-- Incremental migration: run this against an ALREADY-DEPLOYED database after
-- pulling the commit that adds the new scrapers, instead of resetting the
-- whole DB. (For a fresh install, db/seed.sql already covers everything —
-- you don't need this file there.)
--
-- FIXED from the original version of this file, which had bugs that would
-- have failed outright on MySQL:
--   1. Column names didn't match the real `sources` table — used
--      `base_url`/`created_at`, but the actual columns are `url` with no
--      `created_at` column at all (see Source.java / schema.sql).
--   2. `ON CONFLICT (name) DO NOTHING` is PostgreSQL syntax. This is MySQL —
--      the equivalent is `INSERT IGNORE`, used below.
--   3. Only seeded 4 of the 6 new sources — reddit and company_careers were
--      missing, so those scrapers had no `sources` row to look up.
--
-- is_active: linkedin/naukri/indeed/wellfound start inactive — they're not
-- called by SchedulerConfig's cron job yet and still need testing.
-- ============================================================================

INSERT IGNORE INTO sources (name, url, scrape_frequency, is_active) VALUES
    ('reddit',           'https://www.reddit.com',         24, 1),
    ('company_careers',  'https://various-company-pages',  24, 1),
    ('linkedin',         'https://www.linkedin.com/jobs',  24, 0),
    ('naukri',           'https://www.naukri.com',         24, 0),
    ('indeed',           'https://in.indeed.com',          24, 0),
    ('wellfound',        'https://wellfound.com/jobs',     24, 0);
