package com.internhunt.internhunt.scraper.companies;

import com.internhunt.internhunt.entity.JobListing;
import com.internhunt.internhunt.entity.Source;
import com.internhunt.internhunt.scraper.base.JobScraper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class UnstopScraper implements JobScraper
{
    private static final String API_URL =
            "https://unstop.com/api/public/opportunity/search-result" +
                    "?opportunity=internships&per_page=18&oppstatus=open&page=";

    private Source source;

    public void setSource(Source source)
    {
        this.source = source;
    }

    @Override
    public String getSourceName()
    {
        return "unstop";
    }

    @Override
    public List<JobListing> scrape()
    {
        List<JobListing> jobs = new ArrayList<>();
        HttpClient client = HttpClient.newHttpClient();

        int page = 1;
        int totalPages = 1;

        do
        {
            try
            {
                String url = API_URL + page;
                System.out.println("Fetching Unstop page " + page + " of " + totalPages);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "Mozilla/5.0")
                        .header("Accept", "application/json")
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(
                        request, HttpResponse.BodyHandlers.ofString()
                );

                String body = response.body();

                if (page == 1)
                {
                    totalPages = extractInt(body, "\"last_page\":");
                    System.out.println("Total pages: " + totalPages);
                }

                List<JobListing> pageJobs = parseListings(body);
                jobs.addAll(pageJobs);
                System.out.println("Page " + page + ": parsed " + pageJobs.size() + " jobs");

                page++;
                Thread.sleep(500);
            }
            catch (Exception e)
            {
                System.err.println("Error fetching page " + page + ": " + e.getMessage());
                break;
            }
        }
        while (page <= totalPages && page <= 5);

        System.out.println("Total jobs scraped from Unstop: " + jobs.size());
        return jobs;
    }

    private List<JobListing> parseListings(String json)
    {
        List<JobListing> jobs = new ArrayList<>();

        int dataStart = json.indexOf("\"data\":[");
        if (dataStart == -1) return jobs;
        dataStart += 8;

        int depth = 0;
        int objectStart = -1;

        for (int i = dataStart; i < json.length(); i++)
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
            if (title == null || title.isEmpty()) return null;

            String seoUrl = extractString(obj, "\"seo_url\":\"");
            if (seoUrl == null || seoUrl.isEmpty()) return null;
            seoUrl = seoUrl.replace("\\/", "/");

            String companyName = extractNestedString(obj, "\"organisation\"", "\"name\":\"");
            if (companyName == null) companyName = "Unknown";

            String description = extractString(obj, "\"details\":\"");
            if (description != null)
            {
                description = description
                        .replace("\\n", " ")
                        .replace("\\\"", "\"")
                        .replace("\\/", "/")
                        .replaceAll("<[^>]+>", "")
                        .trim();
            }

            String deadlineStr = extractString(obj, "\"end_date\":\"");
            LocalDateTime deadline = null;
            if (deadlineStr != null && deadlineStr.length() >= 19)
            {
                try
                {
                    deadline = LocalDateTime.parse(
                            deadlineStr.substring(0, 19),
                            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                    );
                }
                catch (Exception e)
                {
                    // ignore parse errors
                }
            }

            String jobDetailSection = extractSection(obj, "\"jobDetail\":");
            String location = "India";
            boolean isRemote = false;
            String stipend = null;

            if (jobDetailSection != null)
            {
                String type = extractString(jobDetailSection, "\"type\":\"");
                if ("wfh".equals(type)) isRemote = true;

                String locArray = extractString(jobDetailSection, "\"locations\":[");
                if (locArray != null && !locArray.isEmpty())
                {
                    String firstLoc = extractString("[" + locArray, "\"");
                    if (firstLoc != null) location = firstLoc;
                }

                String minSal = extractRawValue(jobDetailSection, "\"min_salary\":");
                String maxSal = extractRawValue(jobDetailSection, "\"max_salary\":");
                String paidUnpaid = extractString(jobDetailSection, "\"paid_unpaid\":\"");

                if ("unpaid".equals(paidUnpaid))
                {
                    stipend = "Unpaid";
                }
                else if (minSal != null && !minSal.equals("null"))
                {
                    stipend = maxSal != null && !maxSal.equals("null")
                            ? minSal + " - " + maxSal + "/month"
                            : minSal + "/month";
                }
            }

            JobListing job = new JobListing();
            job.setJobTitle(title);
            job.setCompanyName(companyName);
            job.setSourceUrl(seoUrl);
            job.setSource(source);
            job.setDescription(description);
            job.setDeadline(deadline);
            job.setLocation(location);
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
            if (escaped)
            {
                sb.append(c);
                escaped = false;
            }
            else if (c == '\\')
            {
                escaped = true;
            }
            else if (c == '"')
            {
                break;
            }
            else
            {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String extractRawValue(String json, String key)
    {
        int start = json.indexOf(key);
        if (start == -1) return null;
        start += key.length();
        int end = start;
        while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}')
        {
            end++;
        }
        return json.substring(start, end).trim();
    }

    private String extractNestedString(String json, String section, String key)
    {
        int sectionStart = json.indexOf(section);
        if (sectionStart == -1) return null;
        int braceStart = json.indexOf("{", sectionStart);
        if (braceStart == -1) return null;
        int braceEnd = braceStart + 1;
        int depth = 1;
        while (braceEnd < json.length() && depth > 0)
        {
            char c = json.charAt(braceEnd);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            braceEnd++;
        }
        return extractString(json.substring(braceStart, braceEnd), key);
    }

    private String extractSection(String json, String key)
    {
        int start = json.indexOf(key);
        if (start == -1) return null;
        start = json.indexOf("{", start + key.length());
        if (start == -1) return null;
        int end = start + 1;
        int depth = 1;
        while (end < json.length() && depth > 0)
        {
            char c = json.charAt(end);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            end++;
        }
        return json.substring(start, end);
    }

    private int extractInt(String json, String key)
    {
        String val = extractRawValue(json, key);
        if (val == null) return 1;
        try { return Integer.parseInt(val.trim()); }
        catch (Exception e) { return 1; }
    }
}