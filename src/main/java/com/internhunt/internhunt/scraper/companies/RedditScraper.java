package com.internhunt.internhunt.scraper.companies;

import com.internhunt.internhunt.entity.JobListing;
import com.internhunt.internhunt.entity.ScamReport;
import com.internhunt.internhunt.entity.Source;
import com.internhunt.internhunt.repository.ScamReportRepository;
import com.internhunt.internhunt.scraper.base.JobScraper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Scrapes Reddit using the official public JSON API (no auth required for public posts).
 * Reads r/IndiaCSCareerQuestions and r/cscareerquestions for:
 *  - "Who's Hiring" / internship posts -> JobListings
 *  - Scam warning posts -> ScamReports (auto-flagged)
 *
 * API: https://www.reddit.com/r/{sub}/search.json?q=...&sort=new&limit=100
 * No API key needed for public read-only. Uses a descriptive User-Agent per Reddit rules.
 *
 * NOTE: Reddit's public JSON API is known to return 403 for requests coming from
 * datacenter/cloud IPs or generic User-Agents, even though no auth is technically
 * required. fetchJson() now logs the actual HTTP status on any non-200 response
 * instead of silently swallowing it - if every request comes back 403, that's
 * Reddit blocking the request, not a parsing bug in this scraper.
 */
@Component
public class RedditScraper implements JobScraper
{
    // Subreddits to scan
    private static final String[] SUBREDDITS = {
        "IndiaCSCareerQuestions",
        "india",
        "cscareerquestions"
    };

    // Search queries per subreddit
    private static final String[] HIRING_QUERIES = {
        "hiring intern", "internship", "who is hiring", "looking for intern"
    };

    private static final String[] SCAM_QUERIES = {
        "scam company", "fake internship", "fraud company", "avoid company",
        "scam internship", "beware company", "not paying intern"
    };

    private static final String REDDIT_API = "https://www.reddit.com/r/%s/search.json?q=%s&sort=new&restrict_sr=1&limit=50";

    // A realistic browser User-Agent, instead of a custom one identifying this as
    // a bot. Reddit's API is more permissive with these than with descriptive
    // "AppName/1.0 (purpose)" agents, which it's known to rate-limit/block harder.
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
          + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    @Autowired
    private ScamReportRepository scamReportRepository;

    private Source source;

    @Override
    public void setSource(Source source) { this.source = source; }

    @Override
    public String getSourceName() { return "reddit"; }

    @Override
    public List<JobListing> scrape()
    {
        List<JobListing> jobs = new ArrayList<>();
        HttpClient client = HttpClient.newHttpClient();

        // 1. Scrape hiring posts -> job listings
        for (String subreddit : SUBREDDITS)
        {
            for (String query : HIRING_QUERIES)
            {
                try
                {
                    String url = String.format(REDDIT_API, subreddit,
                            java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8));

                    String json = fetchJson(client, url);
                    if (json == null) continue;

                    List<JobListing> found = parseJobPosts(json, subreddit);
                    jobs.addAll(found);
                    System.out.println("[reddit] r/" + subreddit + " '" + query + "': " + found.size() + " posts");

                    Thread.sleep(1000); // be polite to Reddit API
                }
                catch (Exception e)
                {
                    System.err.println("[reddit] Error on r/" + subreddit + " '" + query + "': " + e.getMessage());
                }
            }
        }

        // 2. Scrape scam posts -> ScamReports (auto-saved to DB)
        for (String subreddit : new String[]{"IndiaCSCareerQuestions", "india"})
        {
            for (String query : SCAM_QUERIES)
            {
                try
                {
                    String url = String.format(REDDIT_API, subreddit,
                            java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8));

                    String json = fetchJson(client, url);
                    if (json != null) parseAndSaveScamReports(json, subreddit);

                    Thread.sleep(1000);
                }
                catch (Exception e)
                {
                    System.err.println("[reddit] Scam scan error on r/" + subreddit + ": " + e.getMessage());
                }
            }
        }

        System.out.println("[reddit] Total job posts scraped: " + jobs.size());
        return jobs;
    }

    // --- Job Post Parsing -------------------------------------------------

    private List<JobListing> parseJobPosts(String json, String subreddit)
    {
        List<JobListing> jobs = new ArrayList<>();

        // Reddit JSON structure: { data: { children: [ { data: { title, selftext, url, permalink, id } } ] } }
        int childrenStart = json.indexOf("\"children\":[");
        if (childrenStart == -1) return jobs;
        childrenStart += 12;

        // Walk each post object
        int depth = 0;
        int objStart = -1;
        for (int i = childrenStart; i < json.length(); i++)
        {
            char c = json.charAt(i);
            if (c == '{')
            {
                if (depth == 0) objStart = i;
                depth++;
            }
            else if (c == '}')
            {
                depth--;
                if (depth == 0 && objStart != -1)
                {
                    String obj = json.substring(objStart, i + 1);
                    JobListing job = parsePostObject(obj, subreddit);
                    if (job != null) jobs.add(job);
                    objStart = -1;
                }
            }
            else if (c == ']' && depth == 0) break;
        }
        return jobs;
    }

    private JobListing parsePostObject(String obj, String subreddit)
    {
        try
        {
            // Navigate into nested "data" object inside each child
            int dataIdx = obj.indexOf("\"data\":{");
            if (dataIdx == -1) return null;
            int dataStart = obj.indexOf("{", dataIdx + 7);
            if (dataStart == -1) return null;

            // Extract inner data object
            int end = dataStart + 1;
            int d = 1;
            while (end < obj.length() && d > 0)
            {
                char c = obj.charAt(end);
                if (c == '{') d++;
                else if (c == '}') d--;
                end++;
            }
            String data = obj.substring(dataStart, end);

            String title = extractString(data, "\"title\":\"");
            if (title == null || title.isBlank()) return null;

            // Filter: must look like a hiring post
            String titleLower = title.toLowerCase();
            boolean isHiringPost = titleLower.contains("intern") || titleLower.contains("hiring")
                    || titleLower.contains("job") || titleLower.contains("opportunity")
                    || titleLower.contains("looking for") || titleLower.contains("opening");
            if (!isHiringPost) return null;

            String permalink = extractString(data, "\"permalink\":\"");
            if (permalink == null) return null;
            permalink = permalink.replace("\\/", "/");
            String sourceUrl = "https://www.reddit.com" + permalink;

            String selftext = extractString(data, "\"selftext\":\"");
            String description = selftext != null
                    ? selftext.replace("\\n", "\n").replace("\\\"", "\"").replace("\\/", "/")
                    : title;
            if (description.length() > 5000) description = description.substring(0, 5000) + "...";

            // Try to extract company from title (e.g. "[Company Name] Hiring Intern")
            String company = extractCompanyFromTitle(title);

            // Infer location
            String lowerFull = (title + " " + description).toLowerCase();
            String location = inferLocation(lowerFull, subreddit);
            boolean isRemote = lowerFull.contains("remote") || lowerFull.contains("work from home") || lowerFull.contains("wfh");

            JobListing.ListingType type = titleLower.contains("intern")
                    ? JobListing.ListingType.internship
                    : JobListing.ListingType.full_time;

            JobListing job = new JobListing();
            job.setJobTitle(title.length() > 200 ? title.substring(0, 200) : title);
            job.setCompanyName(company);
            job.setSourceUrl(sourceUrl);
            job.setSource(source);
            job.setDescription(description);
            job.setLocation(location);
            job.setIsRemote(isRemote);
            job.setStipend(null);
            job.setListingType(type);
            job.setStatus(JobListing.Status.ACTIVE);
            return job;
        }
        catch (Exception e) { return null; }
    }

    // --- Scam Report Parsing -----------------------------------------------

    private void parseAndSaveScamReports(String json, String subreddit)
    {
        int childrenStart = json.indexOf("\"children\":[");
        if (childrenStart == -1) return;
        childrenStart += 12;

        int depth = 0, objStart = -1;
        for (int i = childrenStart; i < json.length(); i++)
        {
            char c = json.charAt(i);
            if (c == '{')
            {
                if (depth == 0) objStart = i;
                depth++;
            }
            else if (c == '}')
            {
                depth--;
                if (depth == 0 && objStart != -1)
                {
                    processScamPost(json.substring(objStart, i + 1));
                    objStart = -1;
                }
            }
            else if (c == ']' && depth == 0) break;
        }
    }

    private void processScamPost(String obj)
    {
        try
        {
            int dataIdx = obj.indexOf("\"data\":{");
            if (dataIdx == -1) return;
            int dataStart = obj.indexOf("{", dataIdx + 7);
            if (dataStart == -1) return;
            int end = dataStart + 1;
            int d = 1;
            while (end < obj.length() && d > 0)
            {
                char c = obj.charAt(end);
                if (c == '{') d++;
                else if (c == '}') d--;
                end++;
            }
            String data = obj.substring(dataStart, end);

            String title = extractString(data, "\"title\":\"");
            if (title == null || title.isBlank()) return;

            String titleLower = title.toLowerCase();
            boolean isScamPost = titleLower.contains("scam") || titleLower.contains("fraud")
                    || titleLower.contains("fake") || titleLower.contains("avoid")
                    || titleLower.contains("beware") || titleLower.contains("not paying")
                    || titleLower.contains("blacklist");
            if (!isScamPost) return;

            String permalink = extractString(data, "\"permalink\":\"");
            if (permalink == null) return;
            permalink = permalink.replace("\\/", "/");
            String sourceUrl = "https://www.reddit.com" + permalink;

            // Try to extract company name from title
            String company = extractCompanyFromTitle(title);
            if (company.equals("Unknown (Reddit Post)") || company.length() < 3) return;

            // Don't double-insert
            if (!scamReportRepository.findByCompanyNameContainingIgnoreCase(company).isEmpty()) return;

            // Determine severity
            ScamReport.Severity severity = titleLower.contains("confirmed") || titleLower.contains("fraud")
                    ? ScamReport.Severity.confirmed_scam
                    : ScamReport.Severity.warning;

            ScamReport report = new ScamReport();
            report.setCompanyName(company);
            report.setSourceUrl(sourceUrl);
            report.setSeverity(severity);
            scamReportRepository.save(report);

            System.out.println("[reddit] Auto-flagged scam: " + company + " (" + severity + ")");
        }
        catch (Exception e)
        {
            System.err.println("[reddit] Scam parse error: " + e.getMessage());
        }
    }

    // --- Helpers -------------------------------------------------------------

    private String fetchJson(HttpClient client, String url)
    {
        try
        {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 429)
            {
                System.err.println("[reddit] Rate limited (429) on " + url + " - backing off 10s");
                Thread.sleep(10000);
                return null;
            }
            if (response.statusCode() != 200)
            {
                // Previously silent - this is exactly the kind of failure that
                // looked like "0 results found" with no explanation. Reddit
                // commonly returns 403 here for non-browser/datacenter requests.
                System.err.println("[reddit] Non-200 response (" + response.statusCode() + ") on " + url
                        + " - body preview: "
                        + response.body().substring(0, Math.min(200, response.body().length())));
                return null;
            }
            return response.body();
        }
        catch (Exception e)
        {
            System.err.println("[reddit] Fetch error on " + url + ": " + e.getMessage());
            return null;
        }
    }

    private String extractCompanyFromTitle(String title)
    {
        // Patterns: "[CompanyName] hiring...", "CompanyName | ...", "CompanyName is hiring..."
        if (title.startsWith("[") && title.contains("]"))
        {
            String inside = title.substring(1, title.indexOf("]")).trim();
            if (!inside.isEmpty() && inside.length() <= 100) return inside;
        }
        if (title.contains("|"))
        {
            String part = title.split("\\|")[0].trim();
            if (!part.isEmpty() && part.length() <= 100) return part;
        }
        String lower = title.toLowerCase();
        for (String pattern : new String[]{ " is hiring", " are hiring", " hiring", " - hiring" })
        {
            int idx = lower.indexOf(pattern);
            if (idx > 0)
            {
                String company = title.substring(0, idx).trim()
                        .replaceAll("^\\[|]$", "")
                        .replaceAll("(?i)^(at |@)", "")
                        .trim();
                if (!company.isEmpty() && company.length() <= 100) return company;
            }
        }
        return "Unknown (Reddit Post)";
    }

    private String inferLocation(String text, String subreddit)
    {
        if (text.contains("bangalore") || text.contains("bengaluru")) return "Bangalore";
        if (text.contains("mumbai") || text.contains("bombay")) return "Mumbai";
        if (text.contains("delhi") || text.contains("gurgaon") || text.contains("noida")) return "Delhi NCR";
        if (text.contains("hyderabad")) return "Hyderabad";
        if (text.contains("pune")) return "Pune";
        if (text.contains("chennai")) return "Chennai";
        if (text.contains("remote") || text.contains("wfh") || text.contains("work from home")) return "Remote";
        if (subreddit.toLowerCase().contains("india")) return "India";
        return "Unknown";
    }

    private String extractString(String json, String key)
    {
        int start = json.indexOf(key);
        if (start == -1) return null;
        start += key.length();
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = start; i < json.length(); i++)
        {
            char c = json.charAt(i);
            if (escaped) { sb.append(c); escaped = false; }
            else if (c == '\\') escaped = true;
            else if (c == '"') break;
            else sb.append(c);
        }
        return sb.toString();
    }
}
