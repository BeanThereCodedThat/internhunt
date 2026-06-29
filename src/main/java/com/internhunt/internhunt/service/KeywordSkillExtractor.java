package com.internhunt.internhunt.service;

import com.internhunt.internhunt.entity.JobListing;
import com.internhunt.internhunt.entity.JobRequiredSkill;
import com.internhunt.internhunt.entity.Skill;
import com.internhunt.internhunt.repository.JobListingRepository;
import com.internhunt.internhunt.repository.JobRequiredSkillRepository;
import com.internhunt.internhunt.repository.SkillRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * KeywordSkillExtractor — offline, keyword-based skill extraction from job
 * descriptions. No AI, no external API calls, no Ollama. Matches against a
 * static ~116-skill catalog.
 *
 * Algorithm per job:
 *   1. Combine title + description into one lowercase blob.
 *   2. Split at the first "nice to have / preferred / bonus" style marker —
 *      skills found before the marker are treated as mandatory, after as
 *      optional. No marker found = everything mandatory.
 *   3. Match catalog skill names via word-boundary regex for plain names,
 *      or substring containment for symbol-containing names (C++, .NET)
 *      where \b doesn't anchor correctly.
 *   4. Get-or-create each matched Skill row, then write a JobRequiredSkill.
 *
 * Triggered:
 *   - @Async after every successful scrape (called from ScraperService)
 *   - Manually via POST /api/skills/extract/{jobId}
 *   - In bulk via POST /api/skills/backfill
 */
@Service
public class KeywordSkillExtractor
{
    @Autowired private SkillRepository            skillRepository;
    @Autowired private JobRequiredSkillRepository jobRequiredSkillRepository;
    @Autowired private JobListingRepository       jobListingRepository;

    private static final int MANDATORY_PROFICIENCY = 2; // intermediate
    private static final int OPTIONAL_PROFICIENCY  = 1; // beginner

    private static final Pattern OPTIONAL_MARKER = Pattern.compile(
            "(?i)(nice[- ]to[- ]have|good[- ]to[- ]have|preferred\\s+skills?|"
            + "bonus\\s+points?|added advantage|brownie points?)");

    /** Canonical display name -> category. Order doesn't matter functionally. */
    private static final Map<String, Skill.Category> CATALOG = buildCatalog();

    /** Names that contain regex-boundary-unsafe characters (matched via substring instead of \b). */
    private static final Set<String> SYMBOL_NAMES = Set.of(
            "c++", "c#", ".net", "node.js", "express.js", "next.js", "vue.js", "asp.net");

    /** lowercase name -> canonical-cased name, built once for symbol-name lookups. */
    private static final Map<String, String> LOWER_TO_CANONICAL = buildLowerToCanonical();

    /** Precompiled word-boundary patterns for all non-symbol catalog names. */
    private static final Map<String, Pattern> WORD_PATTERNS = buildWordPatterns();

    // ── Public API ───────────────────────────────────────────────────────────

    @Async
    public void extractSkillsForJob(JobListing job)
    {
        doExtract(job);
    }

    @Async
    public void backfillAll()
    {
        List<JobListing> jobs = jobListingRepository.findAll();
        System.out.println("[skill-extractor] backfill starting — " + jobs.size() + " jobs");

        for (JobListing job : jobs)
        {
            try
            {
                doExtract(job);
            }
            catch (Exception e)
            {
                System.err.println("[skill-extractor] failed for job " + job.getId() + ": " + e.getMessage());
            }
        }

        System.out.println("[skill-extractor] backfill complete");
    }

    // ── Core extraction ──────────────────────────────────────────────────────

    @Transactional
    protected void doExtract(JobListing job)
    {
        String title       = job.getJobTitle()    == null ? "" : job.getJobTitle();
        String description = job.getDescription() == null ? "" : job.getDescription();
        String fullText    = (title + "\n" + description).toLowerCase();

        if (fullText.isBlank())
        {
            return;
        }

        Matcher markerMatch  = OPTIONAL_MARKER.matcher(fullText);
        String  mandatoryText = fullText;
        String  optionalText  = "";

        if (markerMatch.find())
        {
            mandatoryText = fullText.substring(0, markerMatch.start());
            optionalText  = fullText.substring(markerMatch.start());
        }

        Set<String> mandatoryMatches = matchCatalog(mandatoryText);
        Set<String> optionalMatches  = matchCatalog(optionalText);
        optionalMatches.removeAll(mandatoryMatches); // mandatory wins if a skill appears on both sides

        if (mandatoryMatches.isEmpty() && optionalMatches.isEmpty())
        {
            return; // nothing recognized — MatchingService.noSkillData() handles this gracefully
        }

        // Idempotent: wipe any previous extraction for this job before writing fresh rows.
        jobRequiredSkillRepository.deleteByJobId(job.getId());

        List<JobRequiredSkill> toSave = new ArrayList<>();
        for (String name : mandatoryMatches)
        {
            toSave.add(buildRequiredSkill(job, name, true, MANDATORY_PROFICIENCY));
        }
        for (String name : optionalMatches)
        {
            toSave.add(buildRequiredSkill(job, name, false, OPTIONAL_PROFICIENCY));
        }

        jobRequiredSkillRepository.saveAll(toSave);

        System.out.println("[skill-extractor] job " + job.getId() + " — "
                + mandatoryMatches.size() + " mandatory, " + optionalMatches.size() + " optional");
    }

    private JobRequiredSkill buildRequiredSkill(JobListing job, String canonicalName, boolean mandatory, int proficiency)
    {
        Skill skill = getOrCreateSkill(canonicalName);

        JobRequiredSkill jrs = new JobRequiredSkill();
        jrs.setJob(job);
        jrs.setSkill(skill);
        jrs.setIsMandatory(mandatory);
        jrs.setMinimumProficiency(proficiency);
        return jrs;
    }

    private Skill getOrCreateSkill(String canonicalName)
    {
        return skillRepository.findByNameIgnoreCase(canonicalName)
                .orElseGet(() ->
                {
                    Skill s = new Skill();
                    s.setName(canonicalName);
                    s.setCategory(CATALOG.getOrDefault(canonicalName, Skill.Category.tool));
                    return skillRepository.save(s);
                });
    }

    /** Returns canonical-cased catalog names found in the given lowercase text. */
    private Set<String> matchCatalog(String lowerText)
    {
        Set<String> found = new LinkedHashSet<>();
        if (lowerText.isBlank())
        {
            return found;
        }

        for (Map.Entry<String, Pattern> entry : WORD_PATTERNS.entrySet())
        {
            if (entry.getValue().matcher(lowerText).find())
            {
                found.add(entry.getKey());
            }
        }

        for (String symbolName : SYMBOL_NAMES)
        {
            if (lowerText.contains(symbolName))
            {
                String canonical = LOWER_TO_CANONICAL.get(symbolName);
                if (canonical != null)
                {
                    found.add(canonical);
                }
            }
        }

        return found;
    }

    // ── Catalog construction (runs once at class load) ──────────────────────

    private static Map<String, Skill.Category> buildCatalog()
    {
        Map<String, Skill.Category> m = new LinkedHashMap<>();

        putAll(m, Skill.Category.language, new String[]{
                "Java", "Python", "JavaScript", "TypeScript", "C++", "C#", "Golang",
                "Rust", "Kotlin", "Swift", "PHP", "Ruby", "Scala", "SQL", "HTML",
                "CSS", "Bash", "Shell Scripting", "Dart", "MATLAB"});

        putAll(m, Skill.Category.framework, new String[]{
                "Spring Boot", "Spring Security", "Spring MVC", "Spring Cloud", "React",
                "Angular", "Vue.js", "Next.js", "Node.js", "Express.js", "Django",
                "Flask", "FastAPI", "Hibernate", "Laravel", ".NET", "ASP.NET",
                "Tailwind CSS", "Bootstrap", "jQuery", "Redux", "GraphQL"});

        putAll(m, Skill.Category.database, new String[]{
                "MySQL", "PostgreSQL", "MongoDB", "Redis", "Oracle DB", "SQLite",
                "Cassandra", "DynamoDB", "Elasticsearch", "Firebase", "MariaDB"});

        putAll(m, Skill.Category.cloud, new String[]{
                "AWS", "Azure", "Google Cloud Platform", "GCP", "Heroku", "Vercel",
                "Netlify", "DigitalOcean"});

        putAll(m, Skill.Category.devops, new String[]{
                "Docker", "Kubernetes", "Jenkins", "GitHub Actions", "CI/CD",
                "Terraform", "Ansible", "Nginx", "Apache Kafka", "RabbitMQ",
                "Grafana", "Prometheus", "Linux", "Git"});

        putAll(m, Skill.Category.tool, new String[]{
                "Postman", "JIRA", "Figma", "Maven", "Gradle", "Webpack", "Vite",
                "IntelliJ", "VS Code", "Selenium", "JUnit", "Mockito", "Swagger",
                "npm", "Playwright", "JSoup"});

        putAll(m, Skill.Category.ai_ml, new String[]{
                "Machine Learning", "Deep Learning", "TensorFlow", "PyTorch", "NLP",
                "Computer Vision", "Pandas", "NumPy", "Scikit-learn", "OpenCV",
                "LLM", "Generative AI"});

        putAll(m, Skill.Category.security, new String[]{
                "OAuth", "JWT", "OWASP", "Penetration Testing", "Cryptography", "SSL/TLS"});

        putAll(m, Skill.Category.soft_skill, new String[]{
                "Communication", "Teamwork", "Problem Solving", "Time Management",
                "Leadership", "Agile", "Scrum"});

        return Collections.unmodifiableMap(m);
    }

    private static void putAll(Map<String, Skill.Category> m, Skill.Category category, String[] names)
    {
        for (String name : names)
        {
            m.put(name, category);
        }
    }

    private static Map<String, String> buildLowerToCanonical()
    {
        Map<String, String> m = new HashMap<>();
        for (String name : CATALOG.keySet())
        {
            m.put(name.toLowerCase(), name);
        }
        return Collections.unmodifiableMap(m);
    }

    private static Map<String, Pattern> buildWordPatterns()
    {
        Map<String, Pattern> patterns = new LinkedHashMap<>();
        for (String name : CATALOG.keySet())
        {
            if (SYMBOL_NAMES.contains(name.toLowerCase()))
            {
                continue; // handled via substring match instead — see SYMBOL_NAMES
            }
            patterns.put(name, Pattern.compile("\\b" + Pattern.quote(name.toLowerCase()) + "\\b"));
        }
        return patterns;
    }
}
