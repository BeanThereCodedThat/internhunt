# InternHunt

A full-stack internship/job aggregation and tracking platform. Scrapes
listings from 7 actively working sources, tracks applications through a
pipeline, flags scam postings, sends deadline reminders, and scores how well
your skills match a job - all without calling a single paid or external AI API.

Built solo, 1st year B.Tech CSE.

**Stack:** Java 21 - Spring Boot 3.3 - MySQL - React 18 + Vite - Docker

---

## What's actually in this repo

### Job listings
- CRUD + paginated search with filters (source, listing type, remote) - `GET /api/jobs`
- React dashboard: cards with match-score badges, modal detail view, filtering UI

### Scrapers

10 scrapers are wired up. 7 are confirmed working; 3 are blocked by the
target site and kept in the codebase for future fixing.

| Source | Method | Status | Notes |
|---|---|---|---|
| Unstop | Internal API | Active | |
| Internshala | Jsoup | Active | |
| HackerNews | Jsoup | Active | Thread ID configurable via `scraper.hackernews.thread-id` |
| Reddit | OAuth2 API | Active | Requires `scraper.reddit.client-id` + `client-secret` in application.properties. Scans r/IndiaCSCareerQuestions, r/india, r/cscareerquestions for hiring posts + auto-flags scam reports |
| 20 company career pages | Jsoup | Active | Google, Microsoft, Amazon, Flipkart, Swiggy, Zomato, Razorpay, CRED, Meesho, PhonePe, Paytm, Ola, Zepto, BrowserStack, Freshworks, Zoho, Infosys, TCS, Wipro, Accenture |
| Engineering blogs (RSS) | ROME | Active | Netflix, Uber, Airbnb, Spotify, Stripe, GitHub, Razorpay, Swiggy. Filters every post for hiring-related keywords - a post titled "How we rewrote our caching layer" gets skipped |
| LinkedIn | Jsoup | Active | Confirmed working - 50 jobs on first run. Descriptions are null (LinkedIn guest pages don't expose full text without login) |
| Naukri | Jsoup | Inactive - blocked | Naukri migrated to Next.js - Jsoup fetches an empty JS shell, not job cards. Would need their internal API or a headless browser to fix |
| Indeed | `window._initialData` parser | Inactive - blocked | Indeed returns 403 for non-browser requests before the page even loads. Code is correct for when/if they relax bot detection |
| Wellfound | Internal API | Inactive - broken | The `/api/v1/listings/search` endpoint returned 404 - Wellfound deprecated it after their Next.js migration. No public replacement found |

`POST /api/scraper/run/{source}` to trigger one manually, `POST /api/scraper/run-all` for every active one. Dedup on `source_url` before insert.

> **Note on Reddit:** Reddit killed unauthenticated API access in 2023.
> The scraper uses the app-only OAuth2 flow. Register a free app at
> https://www.reddit.com/prefs/apps (type: script), then add
> `scraper.reddit.client-id` and `scraper.reddit.client-secret` to your
> `application.properties`. Without these, the scraper logs a clear warning
> and skips gracefully - it doesn't crash the scheduler.

### Scheduled automation (`SchedulerConfig`)
- **6 AM & 6 PM daily** - runs unstop/internshala/hackernews/reddit/company_careers/rss_blogs automatically. LinkedIn runs on the same schedule. Naukri/Indeed/Wellfound excluded.
- **8 AM daily** - scans for jobs closing within 3 days, creates a notification per affected user, deduped so the same (user, job) pair doesn't get notified again within 20 hours
- **Midnight daily** - marks `ACTIVE` jobs past their deadline as `EXPIRED`

### Scam reporting
- Reddit scraper auto-flags companies mentioned in scam/fraud posts, saved with severity `warning` or `confirmed_scam`
- Manual reporting supported - `POST /api/scam-reports`
- `JobModal` checks `GET /api/scam-reports/check?company=...` and shows a warning banner if the company has been flagged

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

### CI/CD
- GitHub Actions workflow on every push/PR to `master`
- Backend job: spins up MySQL 8.0, runs `schema.sql` + `seed.sql`, compiles and runs `contextLoads()` against a real DB
- Frontend job: `npm ci` + `npm run build`

### Deployment
- `Dockerfile` (backend) + `internhunt-frontend/Dockerfile` + `docker-compose.yml`
- MySQL container auto-initializes from `src/main/resources/schema.sql` (real DDL) then `src/main/resources/db/seed.sql` (registers all 10 sources)

---

## Running locally

```bash
# Backend (set DB credentials as env vars - never hardcode them)
export DB_PASSWORD=your_password
./mvnw spring-boot:run

# Frontend
cd internhunt-frontend
npm install
npm run dev
```

Or `docker compose up --build` with `DB_PASSWORD` set - the DB container
self-initializes from `schema.sql` + `db/seed.sql` on first run.

`application.properties` is gitignored. Copy the template below into
`src/main/resources/application.properties` and fill in your values:

```properties
spring.application.name=internhunt
spring.task.scheduling.pool.size=2

spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/internhunt}
spring.datasource.username=${DB_USER:root}
spring.datasource.password=${DB_PASSWORD:}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
spring.jpa.open-in-view=false

scraper.hackernews.thread-id=47975571
scraper.reddit.client-id=
scraper.reddit.client-secret=
```

After the backend is up, run `POST /api/skills/backfill` once to extract
skills for any jobs already in the DB.

---

## Honest gaps / not yet built

- **No multi-user auth** - `ACTIVE_USER_ID = 1` is hardcoded in `App.jsx`. `ProfilePage` lets you select between multiple `User` rows in the DB, but there is no login - "active user" is a hardcoded constant, not a session.
- **Reddit scraper needs API credentials** - the OAuth2 flow is fully implemented but requires a free Reddit app registration to activate. Without credentials it skips gracefully with a console warning.
- **Naukri, Indeed, Wellfound are blocked** - all three migrated to SPAs/added bot detection since these scrapers were written. The code stays in the repo; fixing any of them properly needs either official API access or a headless browser.
- **Docker production hardening** - current setup is fine for local dev, not hardened for a real production server.

## Roadmap

- Reddit OAuth credentials (needs a Reddit account that can register an app)
- Fix or replace Naukri/Indeed/Wellfound with official APIs or alternative sources
- Docker production hardening
- Multi-user auth + (much later) SaaS/payments

## Decisions made on purpose

- **No cover-letter generation** - not worth the complexity
- **No AI narrative for match results** - the skill breakdown list communicates the same thing without an LLM
- **Free and offline over paid APIs** - every working scraper uses Jsoup/HttpClient/RSS/public-JSON-API, nothing needs a subscription
- **RSS blog posts are filtered, not all ingested** - saving every blog post as a "job" would be wrong data; keyword filtering keeps the signal meaningful
- **Naukri/Indeed/Wellfound kept in codebase despite being blocked** - the code is correct, the sites changed; removing it would erase the work without replacing it with anything better
