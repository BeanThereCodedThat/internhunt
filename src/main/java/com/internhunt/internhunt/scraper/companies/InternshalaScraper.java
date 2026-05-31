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
public class InternshalaScraper implements JobScraper
{
    private static final String BASE_URL = "https://internshala.com/internships/";
    private static final int MAX_PAGES = 5;

    private Source source;

    public void setSource(Source source)
    {
        this.source = source;
    }

    @Override
    public String getSourceName()
    {
        return "internshala";
    }

    @Override
    public List<JobListing> scrape()
    {
        List<JobListing> jobs = new ArrayList<>();

        for (int page = 1; page <= MAX_PAGES; page++)
        {
            try
            {
                String url = page == 1 ? BASE_URL : BASE_URL + "page-" + page + "/";
                System.out.println("Fetching Internshala page " + page + ": " + url);

                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .timeout(15000)
                        .get();

                Elements listings = doc.select(".individual_internship");
                System.out.println("Found " + listings.size() + " listings on page " + page);

                for (Element listing : listings)
                {
                    JobListing job = parseListing(listing);
                    if (job != null)
                    {
                        jobs.add(job);
                    }
                }

                Thread.sleep(1000);
            }
            catch (Exception e)
            {
                System.err.println("Error on page " + page + ": " + e.getMessage());
            }
        }

        System.out.println("Total Internshala jobs scraped: " + jobs.size());
        return jobs;
    }

    private JobListing parseListing(Element el)
    {
        try
        {
            // skip ads and non-internship cards
            if (el.hasClass("pgc-card")) return null;

            String titleEl = el.select("a.job-title-href").text();
            if (titleEl == null || titleEl.isEmpty()) return null;

            String detailHref = el.select("a.job-title-href").attr("href");
            if (detailHref == null || detailHref.isEmpty()) return null;
            String sourceUrl = "https://internshala.com" + detailHref;

            String company = el.select("p.company-name").text().trim();

            // location — check for WFH icon first
            String location = "India";
            boolean isRemote = false;
            Element locationEl = el.select(".row-1-item.locations").first();
            if (locationEl != null)
            {
                if (locationEl.select("i.ic-16-home").size() > 0)
                {
                    isRemote = true;
                    location = "Work from Home";
                }
                else
                {
                    String locText = locationEl.select("a").text().trim();
                    if (!locText.isEmpty()) location = locText;
                }
            }

            // stipend
            String stipend = el.select("span.stipend").text().trim();
            if (stipend.isEmpty()) stipend = null;

            // short description
            String description = el.select(".about_job .text").text().trim();

            // skills
            List<String> skills = new ArrayList<>();
            el.select(".job_skill").forEach(s -> skills.add(s.text().trim()));

            JobListing job = new JobListing();
            job.setJobTitle(titleEl);
            job.setCompanyName(company.isEmpty() ? "Unknown" : company);
            job.setSourceUrl(sourceUrl);
            job.setSource(source);
            job.setDescription(description);
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
}