package com.internhunt.internhunt.scraper.companies;

import com.internhunt.internhunt.entity.JobListing;
import com.internhunt.internhunt.entity.Source;
import com.internhunt.internhunt.scraper.base.JobScraper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class HackerNewsScraper implements JobScraper
{
    // FIX: was a hardcoded static final — goes stale every month.
    //      Now configurable via application.properties:
    //        scraper.hackernews.thread-id=47975571
    @Value("${scraper.hackernews.thread-id:47975571}")
    private String threadId;

    private static final int MAX_PAGES = 5;

    private Source source;

    @Override
    public void setSource(Source source) { this.source = source; }

    @Override
    public String getSourceName() { return "hackernews"; }

    @Override
    public List<JobListing> scrape()
    {
        List<JobListing> jobs = new ArrayList<>();
        String baseUrl = "https://news.ycombinator.com/item?id=" + threadId;

        for (int page = 1; page <= MAX_PAGES; page++)
        {
            try
            {
                String url = page == 1 ? baseUrl : baseUrl + "&p=" + page;
                System.out.println("Fetching HN page " + page + ": " + url);

                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                        .timeout(15000)
                        .get();

                Elements allComments = doc.select("tr.athing.comtr");
                int pageJobs = 0;

                for (Element comment : allComments)
                {
                    Element indentTd = comment.selectFirst("td.ind");
                    if (indentTd == null) continue;
                    if (!"0".equals(indentTd.attr("indent"))) continue;

                    String commentId = comment.attr("id");
                    Element textEl   = comment.selectFirst(".commtext");
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

            String[] lines    = text.split("\\n");
            String firstLine  = lines[0].trim();

            String  company   = "Unknown";
            String  jobTitle  = firstLine;
            String  location  = "Unknown";
            boolean isRemote  = false;

            if (firstLine.contains("|"))
            {
                String[] parts = firstLine.split("\\|");
                company = parts[0].trim()
                        .replaceAll("(?i)\\s*\\(hiring\\).*", "")
                        .replaceAll("(?i)\\s*is hiring.*", "")
                        .trim();

                jobTitle = parts.length > 1 ? parts[1].trim() : "Software Engineer";

                for (int i = 1; i < parts.length; i++)
                {
                    String part = parts[i].toLowerCase();
                    if (part.contains("remote"))                                isRemote = true;
                    if (part.contains("onsite") || part.contains("hybrid"))    isRemote = false;
                }

                location = parts.length > 2 ? parts[2].trim()
                        : parts.length > 1 ? parts[1].trim()
                        : "Unknown";
            }
            else
            {
                company  = firstLine.length() > 60 ? firstLine.substring(0, 60) + "..." : firstLine;
                jobTitle = "Software Engineer";
            }

            String lowerText = text.toLowerCase();
            if (lowerText.contains("remote") || lowerText.contains("work from home")) isRemote = true;

            String description = text.length() > 5000 ? text.substring(0, 5000) + "..." : text;

            if (company.isEmpty() || company.length() > 200) return null;

            JobListing job = new JobListing();
            job.setJobTitle(jobTitle.length()  > 200 ? jobTitle.substring(0, 200)   : jobTitle);
            job.setCompanyName(company);
            job.setSourceUrl(sourceUrl);
            job.setSource(source);
            job.setDescription(description);
            job.setLocation(location.length() > 200 ? location.substring(0, 200) : location);
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