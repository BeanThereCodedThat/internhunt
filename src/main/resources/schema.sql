-- ============================================================
-- InternHunt — database schema
-- FIX: This file previously contained a SELECT query against
--      INFORMATION_SCHEMA instead of DDL.  Spring Boot runs
--      this file on startup and MySQL uses it as the init
--      script in Docker, so it must contain CREATE statements.
-- ============================================================

CREATE TABLE IF NOT EXISTS sources (
                                       id               INT            NOT NULL AUTO_INCREMENT,
                                       name             VARCHAR(100)   NOT NULL,
    url              VARCHAR(500)   NOT NULL,
    scrape_frequency INT            NOT NULL DEFAULT 24,
    last_scraped_at  DATETIME,
    is_active        TINYINT(1)     NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uq_sources_name (name)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS job_listings (
                                            id            INT            NOT NULL AUTO_INCREMENT,
                                            company_name  VARCHAR(200)   NOT NULL,
    job_title     VARCHAR(200)   NOT NULL,
    stipend       VARCHAR(100),
    location      VARCHAR(200),
    is_remote     TINYINT(1)     NOT NULL DEFAULT 0,
    description   TEXT,
    listing_type  ENUM('internship','full_time','contract') NOT NULL DEFAULT 'internship',
    source_id     INT            NOT NULL,
    source_url    VARCHAR(500)   NOT NULL,
    posted_at     DATETIME,
    deadline      DATETIME,
    scraped_at    DATETIME,
    status        ENUM('ACTIVE','CLOSED','EXPIRED','INVALID') NOT NULL DEFAULT 'ACTIVE',
    last_seen_at  DATETIME,
    last_checked_at DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_job_listings_source_url (source_url),
    CONSTRAINT fk_job_listings_source FOREIGN KEY (source_id) REFERENCES sources(id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS job_sources (
                                           id         INT          NOT NULL AUTO_INCREMENT,
                                           job_id     INT          NOT NULL,
                                           source_id  INT          NOT NULL,
                                           source_url VARCHAR(500) NOT NULL,
    found_at   DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_job_sources_url (source_url),
    CONSTRAINT fk_job_sources_job    FOREIGN KEY (job_id)    REFERENCES job_listings(id),
    CONSTRAINT fk_job_sources_source FOREIGN KEY (source_id) REFERENCES sources(id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS scraper_logs (
                                            id            INT          NOT NULL AUTO_INCREMENT,
                                            source_id     INT          NOT NULL,
                                            run_at        DATETIME,
                                            jobs_found    INT          NOT NULL DEFAULT 0,
                                            jobs_saved    INT          NOT NULL DEFAULT 0,
                                            jobs_skipped  INT          NOT NULL DEFAULT 0,
                                            status        ENUM('SUCCESS','PARTIAL','FAILED') NOT NULL,
    error_message TEXT,
    PRIMARY KEY (id),
    CONSTRAINT fk_scraper_logs_source FOREIGN KEY (source_id) REFERENCES sources(id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS users (
                                     id               INT          NOT NULL AUTO_INCREMENT,
                                     name             VARCHAR(100) NOT NULL,
    email            VARCHAR(150) NOT NULL,
    phone            VARCHAR(15),
    resume_url       VARCHAR(500),
    github_url       VARCHAR(500),
    linkedin_url     VARCHAR(500),
    college          VARCHAR(200),
    graduation_year  INT,
    created_at       DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS skills (
                                      id       INT          NOT NULL AUTO_INCREMENT,
                                      name     VARCHAR(100) NOT NULL,
    category ENUM('language','framework','database','cloud','devops','tool','ai_ml','security','soft_skill') NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_skills_name (name)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_skills (
                                           id          INT NOT NULL AUTO_INCREMENT,
                                           user_id     INT NOT NULL,
                                           skill_id    INT NOT NULL,
                                           proficiency INT NOT NULL,
                                           PRIMARY KEY (id),
    CONSTRAINT fk_user_skills_user  FOREIGN KEY (user_id)  REFERENCES users(id),
    CONSTRAINT fk_user_skills_skill FOREIGN KEY (skill_id) REFERENCES skills(id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS job_required_skills (
                                                   id                  INT      NOT NULL AUTO_INCREMENT,
                                                   job_id              INT      NOT NULL,
                                                   skill_id            INT      NOT NULL,
                                                   minimum_proficiency INT      NOT NULL,
                                                   is_mandatory        TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT fk_job_req_skills_job   FOREIGN KEY (job_id)   REFERENCES job_listings(id),
    CONSTRAINT fk_job_req_skills_skill FOREIGN KEY (skill_id) REFERENCES skills(id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS applications (
                                            id           INT  NOT NULL AUTO_INCREMENT,
                                            user_id      INT  NOT NULL,
                                            job_id       INT  NOT NULL,
                                            status       ENUM('pending','applied','rejected','selected') NOT NULL DEFAULT 'pending',
    cover_letter TEXT,
    applied_at   DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT fk_applications_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_applications_job  FOREIGN KEY (job_id)  REFERENCES job_listings(id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS notifications (
                                             id         INT          NOT NULL AUTO_INCREMENT,
                                             user_id    INT          NOT NULL,
                                             job_id     INT,
                                             message    VARCHAR(500) NOT NULL,
    is_read    TINYINT(1)   NOT NULL DEFAULT 0,
    created_at DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_notifications_job  FOREIGN KEY (job_id)  REFERENCES job_listings(id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS scam_reports (
                                            id           INT          NOT NULL AUTO_INCREMENT,
                                            company_name VARCHAR(200) NOT NULL,
    source_url   VARCHAR(500) NOT NULL,
    reported_at  DATETIME,
    severity     ENUM('warning','confirmed_scam') NOT NULL,
    PRIMARY KEY (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;