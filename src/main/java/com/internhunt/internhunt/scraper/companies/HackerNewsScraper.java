package com.internhunt.internhunt.scraper.companies;

import com.internhunt.internhunt.entity.JobListing;
import com.internhunt.internhunt.entity.Source;
import com.internhunt.internhunt.scraper.base.JobScraper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class HackerNewsScraper implements JobScraper
{
    // Update this ID when a new monthly thread is posted
    private static final String THREAD_ID = "47975571";
    private static final String BASE_URL =
            "https://news.ycombinator.com/item?id=" + THREAD_ID;
    private static final int MAX_PAGES = 5;

    private Source source;

    public void setSource(Source source)
    {
        this.source = source;
    }

    @Override
    public String getSourceName()
    {
        return "hackernews";
    }

    @Override
    public List<JobListing> scrape()
    {
        List<JobListing> jobs = new ArrayList<>();

        for (int page = 1; page <= MAX_PAGES; page++)
        {
            try
            {
                String url = page == 1 ? BASE_URL : BASE_URL + "&p=" + page;
                System.out.println("Fetching HN page " + page + ": " + url);

                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                        .timeout(15000)
                        .get();

                // Top-level comments have indent=0
                Elements allComments = doc.select("tr.athing.comtr");
                int pageJobs = 0;

                for (Element comment : allComments)
                {
                    Element indentTd = comment.selectFirst("td.ind");
                    if (indentTd == null) continue;

                    String indentAttr = indentTd.attr("indent");
                    if (!"0".equals(indentAttr)) continue;

                    // This is a top-level comment = one job post
                    String commentId = comment.attr("id");
                    Element textEl = comment.selectFirst(".commtext");
                    if (textEl == null) continue;

                    String fullText = textEl.text().trim();
                    if (fullText.isEmpty()) continue;

                    JobListing job = parseComment(fullText, commentId);
                    if (job != null)
                    {
                        jobs.add(job);
                        pageJobs++;
                    }
                }

                System.out.println("Page " + page + ": parsed " + pageJobs + " jobs");

                // Check if there's a next page
                Element moreLink = doc.selectFirst("a.morelink");
                if (moreLink == null) break;

                Thread.sleep(1000);
            }
            catch (Exception e)
            {
                System.err.println("Error on HN page " + page + ": " + e.getMessage());
                break;
            }
        }

        System.out.println("Total HN jobs scraped: " + jobs.size());
        return jobs;
    }

    private JobListing parseComment(String text, String commentId)
    {
        try
        {
            String sourceUrl = "https://news.ycombinator.com/item?id=" + commentId;

            // First line usually has: Company | Location | Type | ...
            String[] lines = text.split("\\n");
            String firstLine = lines[0].trim();

            String company = "Unknown";
            String jobTitle = firstLine;
            String location = "Unknown";
            boolean isRemote = false;

            // Try to parse pipe-separated first line
            if (firstLine.contains("|"))
            {
                String[] parts = firstLine.split("\\|");
                company = parts[0].trim();

                // Clean up company name — remove common suffixes
                company = company.replaceAll("(?i)\\s*\\(hiring\\).*", "").trim();
                company = company.replaceAll("(?i)\\s*is hiring.*", "").trim();

                jobTitle = parts.length > 1
                        ? parts[1].trim()
                        : "Software Engineer";

                // Check remaining parts for location/remote
                for (int i = 1; i < parts.length; i++)
                {
                    String part = parts[i].toLowerCase();
                    if (part.contains("remote"))
                    {
                        isRemote = true;
                    }
                    if (part.contains("onsite") || part.contains("hybrid")
                            || part.contains("office"))
                    {
                        isRemote = false;
                    }
                }

                // Location from second or third part
                if (parts.length > 2)
                {
                    location = parts[2].trim();
                }
                else if (parts.length > 1)
                {
                    location = parts[1].trim();
                }
            }
            else
            {
                // No pipes — use first word(s) before common keywords
                company = firstLine.length() > 60
                        ? firstLine.substring(0, 60) + "..."
                        : firstLine;
                jobTitle = "Software Engineer";
            }

            // Check full text for remote
            String lowerText = text.toLowerCase();
            if (lowerText.contains("remote") || lowerText.contains("work from home"))
            {
                isRemote = true;
            }

            // Truncate description to avoid DB issues
            String description = text.length() > 5000
                    ? text.substring(0, 5000) + "..."
                    : text;

            if (company.isEmpty() || company.length() > 200) return null;

            JobListing job = new JobListing();
            job.setJobTitle(jobTitle.length() > 255
                    ? jobTitle.substring(0, 255) : jobTitle);
            job.setCompanyName(company);
            job.setSourceUrl(sourceUrl);
            job.setSource(source);
            job.setDescription(description);
            job.setLocation(location.length() > 255
                    ? location.substring(0, 255) : location);
            job.setIsRemote(isRemote);
            job.setStipend(null);
            job.setListingType(JobListing.ListingType.full_time);
            job.setStatus(JobListing.Status.ACTIVE);

            return job;
        }
        catch (Exception e)
        {
            return null;
        }
    }
}