package com.internhunt.internhunt.scraper.companies;

import com.internhunt.internhunt.entity.JobListing;
import com.internhunt.internhunt.entity.Source;
import com.internhunt.internhunt.scraper.base.JobScraper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scrapes Indeed India for internship listings.
 *
 * Indeed is a React SPA — Jsoup cannot parse the rendered job cards since they
 * require JavaScript to build. However, Indeed server-renders a complete JSON
 * blob into every page as window._initialData, which contains the full GraphQL
 * response including job titles, companies, locations, descriptions, and
 * salaries. This scraper reads that blob directly instead of trying to parse
 * HTML card elements.
 *
 * Specifically reads: hostQueryExecutionResult.data.jobData.results[]
 * Each result has job.title, job.sourceEmployerName, job.location.formatted.long,
 * job.description.text, job.url, job.benefits[], job.compensation
 */
@Component
public class IndeedScraper implements JobScraper
{
    private static final String BASE_URL =
            "https://in.indeed.com/jobs?q=internship+software&l=India&sort=date&start=";

    private static final int MAX_PAGES = 5;
    private static final int PAGE_SIZE  = 15;

    private Source source;

    @Override
    public void setSource(Source source) { this.source = source; }

    @Override
    public String getSourceName() { return "indeed"; }

    @Override
    public List<JobListing> scrape()
    {
        List<JobListing> jobs = new ArrayList<>();

        for (int page = 0; page < MAX_PAGES; page++)
        {
            try
            {
                int start = page * PAGE_SIZE;
                String url = BASE_URL + start;
                System.out.println("[indeed] Fetching page " + (page + 1) + ": " + url);

                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                                 + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .header("Accept", "text/html,application/xhtml+xml")
                        .header("Accept-Language", "en-IN,en-US;q=0.9")
                        .header("Referer", "https://in.indeed.com/")
                        .timeout(20000)
                        .get();

                // Extract window._initialData JSON blob from the page source.
                // The blob is server-rendered and contains the full job list.
                String html = doc.html();
                String initialData = extractInitialData(html);

                if (initialData == null)
                {
                    System.out.println("[indeed] Page " + (page + 1)
                            + ": window._initialData not found — page structure may have changed");
                    break;
                }

                List<JobListing> pageJobs = parseJobResults(initialData);
                System.out.println("[indeed] Page " + (page + 1) + ": " + pageJobs.size() + " jobs");

                if (pageJobs.isEmpty()) break;
                jobs.addAll(pageJobs);

                Thread.sleep(1500);
            }
            catch (Exception e)
            {
                System.err.println("[indeed] Error on page " + (page + 1) + ": " + e.getMessage());
                break;
            }
        }

        System.out.println("[indeed] Total scraped: " + jobs.size());
        return jobs;
    }

    // ─── JSON extraction ─────────────────────────────────────────────────────

    /**
     * Extracts the raw JSON string from window._initialData={...};
     * Uses balanced-brace counting to find the end of the object reliably.
     */
    private String extractInitialData(String html)
    {
        String marker = "window._initialData=";
        int start = html.indexOf(marker);
        if (start == -1) return null;
        start += marker.length();
        if (start >= html.length() || html.charAt(start) != '{') return null;

        int depth = 0, end = start;
        boolean inString = false;
        boolean escaped = false;

        for (int i = start; i < html.length(); i++)
        {
            char c = html.charAt(i);
            if (escaped) { escaped = false; continue; }
            if (c == '\\' && inString) { escaped = true; continue; }
            if (c == '"') { inString = !inString; continue; }
            if (inString) continue;
            if (c == '{') depth++;
            else if (c == '}') { depth--; if (depth == 0) { end = i; break; } }
        }

        if (end <= start) return null;
        return html.substring(start, end + 1);
    }

    /**
     * Parses job results from the extracted _initialData JSON.
     * Navigates: hostQueryExecutionResult -> data -> jobData -> results
     */
    private List<JobListing> parseJobResults(String json)
    {
        List<JobListing> jobs = new ArrayList<>();

        // Find the results array inside jobData
        String resultsKey = "\"results\":[";
        int resultsStart = json.indexOf(resultsKey);
        if (resultsStart == -1) return jobs;
        resultsStart += resultsKey.length();

        // Walk each result object
        int i = resultsStart;
        while (i < json.length())
        {
            if (json.charAt(i) == ']') break;
            if (json.charAt(i) != '{') { i++; continue; }

            // Find matching closing brace
            int objStart = i;
            int depth = 0, objEnd = i;
            boolean inStr = false, esc = false;

            for (int j = i; j < json.length(); j++)
            {
                char c = json.charAt(j);
                if (esc) { esc = false; continue; }
                if (c == '\\' && inStr) { esc = true; continue; }
                if (c == '"') { inStr = !inStr; continue; }
                if (inStr) continue;
                if (c == '{') depth++;
                else if (c == '}') { depth--; if (depth == 0) { objEnd = j; break; } }
            }

            String result = json.substring(objStart, objEnd + 1);
            JobListing job = parseResult(result);
            if (job != null) jobs.add(job);

            i = objEnd + 1;
            // skip comma
            while (i < json.length() && json.charAt(i) == ',') i++;
        }

        return jobs;
    }

    private JobListing parseResult(String result)
    {
        try
        {
            // Navigate into the nested "job" object
            String jobKey = "\"job\":{";
            int jobStart = result.indexOf(jobKey);
            if (jobStart == -1) return null;
            jobStart += jobKey.length() - 1;

            int depth = 0, jobEnd = jobStart;
            boolean inStr = false, esc = false;

            for (int i = jobStart; i < result.length(); i++)
            {
                char c = result.charAt(i);
                if (esc) { esc = false; continue; }
                if (c == '\\' && inStr) { esc = true; continue; }
                if (c == '"') { inStr = !inStr; continue; }
                if (inStr) continue;
                if (c == '{') depth++;
                else if (c == '}') { depth--; if (depth == 0) { jobEnd = i; break; } }
            }

            String job = result.substring(jobStart, jobEnd + 1);

            String title   = extractString(job, "\"title\":\"");
            String company = extractString(job, "\"sourceEmployerName\":\"");
            String url     = extractString(job, "\"url\":\"");
            String descText = extractString(job, "\"text\":\"");
            String key     = extractString(job, "\"key\":\"");

            if (title == null || title.isBlank()) return null;
            if (key  == null || key.isBlank())   return null;

            // URL: use canonical job URL if url field is empty/relative
            String sourceUrl = (url != null && url.startsWith("http"))
                    ? url
                    : "https://in.indeed.com/job/" + sanitize(title) + "-" + key;
            sourceUrl = sourceUrl.replace("\\u002F", "/").replace("\\/", "/");

            // Location — navigate formatted.long inside location object
            String location = extractNestedString(job, "\"formatted\":", "\"long\":\"");
            if (location == null) location = extractString(job, "\"fullAddress\":\"");
            if (location == null) location = "India";
            location = unescape(location);

            String description = descText != null
                    ? unescape(descText).replace("\\n", "\n")
                    : title;
            if (description.length() > 5000) description = description.substring(0, 5000) + "...";

            boolean isRemote = location.toLowerCase().contains("remote")
                            || (description.toLowerCase().contains("work from home"));

            String titleLower = title.toLowerCase();
            JobListing.ListingType type = titleLower.contains("intern")
                    ? JobListing.ListingType.internship
                    : JobListing.ListingType.full_time;

            JobListing listing = new JobListing();
            listing.setJobTitle(title.length() > 200 ? title.substring(0, 200) : title);
            listing.setCompanyName(company != null && !company.isBlank()
                    ? (company.length() > 200 ? company.substring(0, 200) : company)
                    : "Unknown");
            listing.setSourceUrl(sourceUrl.length() > 500 ? sourceUrl.substring(0, 500) : sourceUrl);
            listing.setSource(source);
            listing.setLocation(location.length() > 200 ? location.substring(0, 200) : location);
            listing.setIsRemote(isRemote);
            listing.setListingType(type);
            listing.setDescription(description);
            listing.setStatus(JobListing.Status.ACTIVE);

            return listing;
        }
        catch (Exception e) { return null; }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /** Extracts a string value for a given key from a flat JSON fragment. */
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
        return sb.length() > 0 ? sb.toString() : null;
    }

    /** Extracts a nested string: find outerKey, then find innerKey within that block. */
    private String extractNestedString(String json, String outerKey, String innerKey)
    {
        int outer = json.indexOf(outerKey);
        if (outer == -1) return null;
        // find opening brace of the outer object
        int braceStart = json.indexOf("{", outer + outerKey.length());
        if (braceStart == -1) return null;
        int depth = 0, braceEnd = braceStart;
        boolean inStr = false, esc = false;
        for (int i = braceStart; i < json.length(); i++)
        {
            char c = json.charAt(i);
            if (esc) { esc = false; continue; }
            if (c == '\\' && inStr) { esc = true; continue; }
            if (c == '"') { inStr = !inStr; continue; }
            if (inStr) continue;
            if (c == '{') depth++;
            else if (c == '}') { depth--; if (depth == 0) { braceEnd = i; break; } }
        }
        return extractString(json.substring(braceStart, braceEnd + 1), innerKey);
    }

    private String unescape(String s)
    {
        if (s == null) return null;
        return s.replace("\\u002F", "/").replace("\\/", "/")
                .replace("\\u0026", "&").replace("\\u003C", "<")
                .replace("\\u003E", ">").replace("\\u0027", "'")
                .replace("\\u0022", "\"");
    }

    private String sanitize(String title)
    {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}
