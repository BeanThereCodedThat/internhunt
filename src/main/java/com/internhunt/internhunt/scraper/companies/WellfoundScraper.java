package com.internhunt.internhunt.scraper.companies;

import com.internhunt.internhunt.entity.JobListing;
import com.internhunt.internhunt.entity.Source;
import com.internhunt.internhunt.scraper.base.JobScraper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Scrapes Wellfound (formerly AngelList Talent) for startup internships in India.
 * Uses Wellfound's undocumented JSON API that powers their public listings page.
 *
 * API endpoint: /api/v1/listings/search (returns JSON, no auth for basic use)
 */
@Component
public class WellfoundScraper implements JobScraper
{
    // Wellfound search — internships in India, sorted by recent
    private static final String API_URL =
            "https://wellfound.com/api/v1/listings/search" +
            "?role_types[]=internship&locations[]=india&page=";

    private static final int MAX_PAGES = 5;

    private Source source;

    @Override
    public void setSource(Source source) { this.source = source; }

    @Override
    public String getSourceName() { return "wellfound"; }

    @Override
    public List<JobListing> scrape()
    {
        List<JobListing> jobs = new ArrayList<>();
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                .build();

        for (int page = 1; page <= MAX_PAGES; page++)
        {
            try
            {
                String url = API_URL + page;
                System.out.println("[wellfound] Fetching page " + page);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                        .header("Accept", "application/json, text/javascript, */*")
                        .header("X-Requested-With", "XMLHttpRequest")
                        .header("Referer", "https://wellfound.com/jobs")
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(
                        request, HttpResponse.BodyHandlers.ofString()
                );

                String body = response.body();

                if (response.statusCode() != 200)
                {
                    System.err.println("[wellfound] HTTP " + response.statusCode()
                            + " on page " + page);
                    break;
                }

                List<JobListing> pageJobs = parseResponse(body);
                jobs.addAll(pageJobs);
                System.out.println("[wellfound] Page " + page + ": " + pageJobs.size() + " jobs");

                // If we got fewer than expected, we've hit the last page
                if (pageJobs.isEmpty()) break;

                Thread.sleep(800);
            }
            catch (Exception e)
            {
                System.err.println("[wellfound] Error on page " + page + ": " + e.getMessage());
                break;
            }
        }

        System.out.println("[wellfound] Total scraped: " + jobs.size());
        return jobs;
    }

    // ------------------------------------------------------------------ //
    //  JSON parsing (manual — no external JSON library required)          //
    // ------------------------------------------------------------------ //

    private List<JobListing> parseResponse(String json)
    {
        List<JobListing> jobs = new ArrayList<>();

        // Navigate into the listings array: {"jobs": [...]}
        int arrStart = json.indexOf("\"jobs\":[");
        if (arrStart == -1) arrStart = json.indexOf("\"listings\":[");
        if (arrStart == -1) arrStart = json.indexOf("\"data\":[");
        if (arrStart == -1) return jobs;

        arrStart = json.indexOf("[", arrStart) + 1;

        int depth = 0;
        int objectStart = -1;

        for (int i = arrStart; i < json.length(); i++)
        {
            char c = json.charAt(i);
            if (c == '{')
            {
                if (depth == 0) objectStart = i;
                depth++;
            }
            else if (c == '}')
            {
                depth--;
                if (depth == 0 && objectStart != -1)
                {
                    String obj = json.substring(objectStart, i + 1);
                    JobListing job = parseJobObject(obj);
                    if (job != null) jobs.add(job);
                    objectStart = -1;
                }
            }
            else if (c == ']' && depth == 0)
            {
                break;
            }
        }

        return jobs;
    }

    private JobListing parseJobObject(String obj)
    {
        try
        {
            String title = extractString(obj, "\"title\":\"");
            if (title == null || title.isEmpty())
                title = extractString(obj, "\"role\":\"");
            if (title == null || title.isEmpty()) return null;

            String slug = extractString(obj, "\"slug\":\"");
            String id    = extractRaw(obj, "\"id\":");
            String sourceUrl;
            if (slug != null && !slug.isEmpty())
                sourceUrl = "https://wellfound.com/jobs/" + slug;
            else if (id != null && !id.trim().isEmpty())
                sourceUrl = "https://wellfound.com/jobs?listing=" + id.trim();
            else
                return null;

            // Company — may be nested inside "startup" or "company" object
            String company = extractNestedString(obj, "\"startup\"", "\"name\":\"");
            if (company == null || company.isEmpty())
                company = extractNestedString(obj, "\"company\"", "\"name\":\"");
            if (company == null || company.isEmpty())
                company = extractString(obj, "\"company_name\":\"");
            if (company == null) company = "Unknown";

            // Location
            String location = extractString(obj, "\"location_names\":\"");
            if (location == null || location.isEmpty())
                location = extractString(obj, "\"primary_location\":\"");
            if (location == null || location.isEmpty()) location = "India";

            boolean isRemote = location.toLowerCase().contains("remote");

            // Compensation
            String minComp = extractRaw(obj, "\"min_salary\":");
            String maxComp = extractRaw(obj, "\"max_salary\":");
            String stipend = null;
            if (minComp != null && !minComp.trim().equals("null") && !minComp.trim().isEmpty())
            {
                stipend = maxComp != null && !maxComp.trim().equals("null")
                        ? "₹" + minComp.trim() + " – ₹" + maxComp.trim() + "/yr"
                        : "₹" + minComp.trim() + "/yr";
            }

            // Description
            String description = extractString(obj, "\"description\":\"");
            if (description != null)
            {
                description = description.replace("\\n", " ")
                                         .replace("\\\"", "\"")
                                         .replaceAll("<[^>]+>", "")
                                         .trim();
                if (description.length() > 5000) description = description.substring(0, 5000);
            }

            JobListing job = new JobListing();
            job.setJobTitle(title.length() > 200 ? title.substring(0, 200) : title);
            job.setCompanyName(company.length() > 200 ? company.substring(0, 200) : company);
            job.setSourceUrl(sourceUrl.length() > 500 ? sourceUrl.substring(0, 500) : sourceUrl);
            job.setSource(source);
            job.setDescription(description);
            job.setLocation(location.length() > 200 ? location.substring(0, 200) : location);
            job.setIsRemote(isRemote);
            job.setStipend(stipend);
            job.setListingType(JobListing.ListingType.internship);
            job.setStatus(JobListing.Status.ACTIVE);

            return job;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    // ---- minimal JSON string extraction helpers ----

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

    private String extractRaw(String json, String key)
    {
        int start = json.indexOf(key);
        if (start == -1) return null;
        start += key.length();
        int end = start;
        while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}')
            end++;
        return json.substring(start, end).trim();
    }

    private String extractNestedString(String json, String section, String key)
    {
        int s = json.indexOf(section);
        if (s == -1) return null;
        int b = json.indexOf("{", s);
        if (b == -1) return null;
        int e = b + 1;
        int d = 1;
        while (e < json.length() && d > 0)
        {
            char c = json.charAt(e);
            if (c == '{') d++;
            else if (c == '}') d--;
            e++;
        }
        return extractString(json.substring(b, e), key);
    }
}
