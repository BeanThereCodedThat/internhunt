# InternHunt

A full-stack internship/job aggregation and tracking platform. Scrapes
listings from 10 sources (including 20 individual company career pages and
8 engineering blogs), tracks applications through a pipeline, flags scam
postings, sends deadline reminders, and scores how well your skills match a
job - all without calling a single paid or external AI API.

Built solo, 1st year B.Tech CSE.

**Stack:** Java 21 - Spring Boot 3.3 - MySQL - React 18 + Vite - Docker

---

## What's actually in this repo

### Job listings
- CRUD + paginated search with filters (source, listing type, remote) - `GET /api/jobs`
- React dashboard: cards with match-score badges, modal detail view, filtering UI

### Scrapers - 10 sources, no Selenium
All scraping is done with `Jsoup` (HTML parsing), Java's built-in `HttpClient`,
or ROME (RSS parsing) - nothing here drives a headless browser. Selenium was
tried early for Unstop and dropped (see git history) in favor of hitting
Unstop's internal API directly; the unused base class and the
Selenium/WebDriverManager dependencies it needed have since been removed
from the project entirely.

| Source | Method | Notes |
|---|---|---|
| Unstop | Internal API | |
| Internshala | Jsoup | |
| HackerNews | Jsoup | "Who's Hiring" thread ID is a config property, not hardcoded |
| Reddit | Public JSON API | No auth needed. Also scans for scam reports - see below |
| 20 company career pages | Jsoup | Google, Microsoft, Amazon, Flipkart, Swiggy, Zomato, Razorpay, CRED, Meesho, PhonePe, Paytm, Ola, Zepto, BrowserStack, Freshworks, Zoho, Infosys, TCS, Wipro, Accenture - all in `CompanyCareersScrapers.java` |
| Engineering blogs (RSS) | ROME | Netflix, Uber, Airbnb, Spotify, Stripe, GitHub, Razorpay, Swiggy. Not job boards - filters every post for hiring-related keywords ("we're hiring", "join our team", "internship", etc.) and only saves matches. A post titled "How we rewrote our caching layer" gets skipped; "We're hiring backend engineers" gets saved |
| LinkedIn, Naukri, Indeed, Wellfound | Jsoup / API | Built, but **inactive by default** (`is_active=0` in the seed) - not yet verified reliable, and `SchedulerConfig` deliberately excludes them from the automatic run. See "Testing the inactive scrapers" below |

`POST /api/scraper/run/{source}` to trigger one manually, `POST /api/scraper/run-all` for every active one. Dedup on `source_url` before insert.

### Scheduled automation (`SchedulerConfig`)
- **6 AM & 6 PM daily** - runs unstop/internshala/hackernews/reddit/company_careers/rss_blogs automatically
- **8 AM daily** - scans for jobs closing within 3 days, creates a notification per affected user, deduped so the same (user, job) pair doesn't get notified again within 20 hours
- **Midnight daily** - marks `ACTIVE` jobs past their deadline as `EXPIRED`

### Scam reporting
- Reddit scraper actively scans hiring threads for scam reports and saves them with a severity (`warning` / `confirmed_scam`)
- Manual reporting also supported - `POST /api/scam-reports`
- `JobModal` checks `GET /api/scam-reports/check?company=...` and shows a warning banner if the company's been flagged

### Skill matching (Phase 2 - no AI, fully offline)
- `KeywordSkillExtractor` - matches job descriptions against a static ~116-skill
  catalog (languages, frameworks, databases, cloud, devops, tools, AI/ML,
  security, soft skills). Splits mandatory vs. optional by detecting
  "nice to have / preferred / bonus" sections in the text. Runs `@Async`
  after every scrape.
- `MatchingService` - deterministic scoring: mandatory skill = 10pts, optional
  = 3pts, score = earned/total x 100. No ML model, no embeddings, no LLM calls.
- `GET /api/match/{userId}/{jobId}` - full breakdown (matched / missing / bonus skills)
- `GET /api/match/{userId}/scores?jobs=1,2,3` - batch scores for card badges
- `POST /api/skills/extract/{jobId}` / `POST /api/skills/backfill` - manual re-run
- Frontend: color-coded SVG ring badge on every job card, "AI Match" tab in the job modal with the full skill breakdown

### Profile, skills, and application tracking
- `ProfilePage` - select/create a user, edit profile fields (resume/GitHub/LinkedIn URLs, college, graduation year), manage skills with a 1-3 proficiency level
- `ApplicationTracker` - pipeline view: `pending -> applied -> rejected -> selected`. "+ Track Application" button right in the job modal
- `NotificationsPanel` - unread tracking, fed by the deadline scheduler above

### Deployment
- `Dockerfile` (backend) + `internhunt-frontend/Dockerfile` + `docker-compose.yml`
- MySQL container auto-initializes from `src/main/resources/schema.sql` (real DDL - see below) then `src/main/resources/db/seed.sql` (registers all 10 sources)

---

## Running locally

```bash
# Backend
export DB_PASSWORD=your_password
./mvnw spring-boot:run

# Frontend
cd internhunt-frontend
npm install
npm run dev
```

Or just `docker compose up --build` with `DB_PASSWORD` set - the DB container
will self-initialize from `schema.sql` + `db/seed.sql` on first run.

If you're setting up a DB manually instead of via Docker, run `schema.sql`
then `db/seed.sql` against it directly, in that order.

After the backend is up, hit `POST /api/skills/backfill` once to extract
skills for any jobs already in the DB.

### Testing the inactive scrapers (LinkedIn / Naukri / Indeed / Wellfound)

These were built but never verified against the live sites, so treat them as
untested code, not working features, until you've actually run each one:

```bash
curl -X POST http://localhost:8080/api/scraper/run/linkedin
curl -X POST http://localhost:8080/api/scraper/run/naukri
curl -X POST http://localhost:8080/api/scraper/run/indeed
curl -X POST http://localhost:8080/api/scraper/run/wellfound
```

Each runs `@Async`, so the HTTP response returns immediately - check the
backend console logs for `[linkedin] Total jobs scraped: N` (or `[naukri]`,
etc). Then confirm real rows landed in the DB:

```bash
curl "http://localhost:8080/api/jobs?source=linkedin&size=5"
```

What to watch for, since all four scrape live HTML/JSON that can change
without notice:
- **LinkedIn** is the most likely to fail or return 0 results - their guest
  search endpoint is known to rate-limit or block scraping attempts
  inconsistently. If it returns nothing or errors, that's expected behavior
  to discover, not a bug in this codebase to chase.
- **Naukri / Indeed** use Jsoup against server-rendered HTML, so they're more
  likely to actually work, but their CSS selectors are based on whatever the
  page structure looked like when they were written - if either site has
  redesigned since, the selectors will silently return 0 jobs rather than
  error.
- **Wellfound** hits an internal JSON API directly, similar to Unstop - most
  likely to keep working if it works at all, but also most likely to break
  outright (instead of just returning fewer results) if the API contract
  changes.

Once you've confirmed one actually returns real jobs, flip it to active in
the DB so the scheduler picks it up automatically:

```sql
UPDATE sources SET is_active = 1 WHERE name = 'linkedin';
```

---

## Honest gaps / not yet built

- **No CI/CD pipeline** - discussed and drafted, never written into the repo.
- **No multi-user auth** - `ACTIVE_USER_ID = 1` is hardcoded in `App.jsx`. `ProfilePage` lets you *select* between multiple `User` rows in the DB, but there's no login, so "active user" is still a single hardcoded constant, not a session.
- **LinkedIn/Naukri/Indeed/Wellfound scrapers are unverified** - built, wired into `ScraperService`, but `is_active=0` and excluded from the scheduler until tested. See the testing section above.
- **Docker production hardening** - current setup is fine for local dev, not yet hardened for a real deploy target.

## Roadmap

- Verify and activate LinkedIn/Naukri/Indeed/Wellfound, or drop them if they don't pan out
- Add a GitHub Actions CI pipeline (build + test on push/PR)
- Docker production hardening
- Multi-user auth + (much later) a SaaS/payments layer

## Decisions made on purpose

- **No cover-letter generation** - decided the complexity wasn't worth it
- **No AI narrative for match results** - the skill breakdown list communicates the same thing without the cost/complexity of an LLM call
- **Free and offline over paid APIs**, every time there was a choice - every scraper here is Jsoup/HttpClient/RSS/public-JSON-API, nothing needs a key or a subscription
- **RSS blog posts are filtered, not all ingested** - a blog scraper that saves every post as a "job" would just be wrong data. Hiring-keyword filtering keeps the signal real even though it means most posts get skipped
