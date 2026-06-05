-- Run this after adding the new scrapers.
-- Adds the 4 new sources to the `sources` table.

INSERT INTO sources (name, base_url, is_active, created_at)
VALUES
    ('linkedin',   'https://www.linkedin.com/jobs',     TRUE, NOW()),
    ('naukri',     'https://www.naukri.com',            TRUE, NOW()),
    ('indeed',     'https://in.indeed.com',             TRUE, NOW()),
    ('wellfound',  'https://wellfound.com/jobs',        TRUE, NOW())
ON CONFLICT (name) DO NOTHING;
