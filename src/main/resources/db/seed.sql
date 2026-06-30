-- ============================================================================
-- Seeds the `sources` table with every source ScraperService actually wires
-- up (see scraperMap() in ScraperService.java). Without these rows,
-- runScraperInternal() logs "Source not found in DB" and does nothing —
-- sourceRepository.findByName(...) comes back empty.
--
-- Mounted by docker-compose.yml as 02_seed.sql, runs once on first container
-- start (after 01_schema.sql), against a fresh MySQL data volume.
--
-- is_active matches SchedulerConfig.runCoreScrapeSchedule(), which only ever
-- calls unstop/internshala/hackernews/reddit/company_careers automatically.
-- LinkedIn/Naukri/Indeed/Wellfound are deliberately excluded from that cron
-- job (per its own comment) — they're newer and still need testing, so they
-- stay inactive here too. Flip is_active to 1 once you've verified one works.
-- ============================================================================

INSERT IGNORE INTO sources (name, url, scrape_frequency, is_active) VALUES
    ('unstop',           'https://unstop.com',                24, 1),
    ('internshala',      'https://internshala.com',           24, 1),
    ('hackernews',       'https://news.ycombinator.com',      24, 1),
    ('reddit',           'https://www.reddit.com',             24, 1),
    ('company_careers',  'https://various-company-pages',     24, 1),
    ('linkedin',         'https://www.linkedin.com/jobs',      24, 0),
    ('naukri',           'https://www.naukri.com',             24, 0),
    ('indeed',           'https://in.indeed.com',              24, 0),
    ('wellfound',        'https://wellfound.com/jobs',         24, 0);
