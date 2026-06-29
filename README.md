# InternHunt

A full-stack internship/job aggregation and tracking platform. Built solo while
in 1st year — scrapes listings from multiple sources, tracks applications
through a pipeline, and scores how well your skills match a job, all without
calling a single paid or external AI API.

**Stack:** Java 21 · Spring Boot 4.0 · MySQL · React 18 + Vite · Docker

---

## What's actually in this repo

### Job listings
- CRUD + paginated search with filters (source, listing type, remote) — `JobListingController` / `GET /api/jobs`
- React dashboard with cards, modal detail view, filtering UI

### Scrapers (manual trigger, no cron yet)
- **Unstop**, **Internshala**, **HackerNews** — `POST /api/scraper/run/{source}` or the dedicated `/api/scraper/{source}` endpoints
- Built on a shared `JobScraper` interface (`scraper/base`) so adding a new source means implementing one interface, not rewriting the pipeline
- Dedup on `source_url` before insert

### Skill matching (Phase 2 — no AI, fully offline)
- `KeywordSkillExtractor` — matches job descriptions against a static ~116-skill
  catalog (languages, frameworks, databases, cloud, devops, tools, AI/ML,
  security, soft skills). Splits mandatory vs. optional requirements by
  detecting "nice to have / preferred / bonus" sections in the text.
  Runs `@Async` after every scrape.
- `MatchingService` — deterministic scoring: mandatory skill = 10pts, optional
  = 3pts, score = earned/total × 100. No ML model, no embeddings, no LLM calls.
- `GET /api/match/{userId}/{jobId}` — full breakdown (matched / missing / bonus skills)
- `GET /api/match/{userId}/scores?jobs=1,2,3` — batch scores for card badges
- `POST /api/skills/extract/{jobId}` / `POST /api/skills/backfill` — manual re-run
- Frontend: color-coded SVG ring badge on every job card, "AI Match" tab in the job modal with the full skill breakdown

### User profile & skills
- User CRUD, skill catalog CRUD, per-user skill tracking with a 1–3 proficiency level

### Application tracker
- Pipeline: `pending → applied → rejected → selected` — `ApplicationController`

### Scam reporting
- Manual company flagging with severity (`warning` / `confirmed_scam`),
  search by company name, and a `check` endpoint other features can call
  before showing a listing

### Notifications
- Basic CRUD + unread tracking per user (`NotificationController`).
  No deadline-detection or dedup logic wired in yet — that's still on the roadmap, not built.

### Deployment
- `Dockerfile` (backend) + `internhunt-frontend/Dockerfile` + root `docker-compose.yml` for local multi-container runs

---

## Running locally

```bash
# Backend
./mvnw spring-boot:run

# Frontend
cd internhunt-frontend
npm install
npm run dev
```

You'll need a local MySQL instance matching `src/main/resources/application.properties`
(`spring.datasource.url`, set `DB_PASSWORD` as an env var). Note: `schema.sql`
in this repo is currently a leftover debug query, not real DDL — it won't
create your tables for you. Run your schema setup manually for now (fixing
this properly is on the list below).

After the backend is up, hit `POST /api/skills/backfill` once to extract
skills for any jobs already in the DB, then add some skills for user id `1`
(single-user mode for now — `ACTIVE_USER_ID = 1` is hardcoded in the frontend).

---

## Honest gaps / not yet built

These are real, not hidden:

- **`schema.sql` doesn't create tables** — it's a stray `INFORMATION_SCHEMA`
  query. Needs to be replaced with actual DDL before CI or a fresh DB setup
  will work.
- **No scheduled scraping** — every scraper run is manual (`POST` trigger).
  No `@Scheduled` cron job exists yet.
- **Only 3 scrapers are live** — Unstop, Internshala, HackerNews. Reddit and
  individual company career-page scrapers are a planned next step, not built.
- **No deadline-alert / notification-dedup logic** — notifications are
  basic CRUD only.
- **Single-user** — `ACTIVE_USER_ID = 1` is hardcoded on the frontend; no auth yet.
- **No CI/CD pipeline yet** — being set up next.

## Roadmap

- Fix `schema.sql`, then add a GitHub Actions CI pipeline (build + test on push/PR)
- RSS feed scrapers for engineering blogs
- Scheduled auto-scraping
- Docker production hardening
- Multi-user auth + (much later) a SaaS/payments layer

## Decisions made on purpose

- **No cover-letter generation** — decided the complexity wasn't worth it
- **No AI narrative for match results** — the skill breakdown list communicates the same thing without the cost/complexity of an LLM call
- **Free and offline over paid APIs**, every time there was a choice
