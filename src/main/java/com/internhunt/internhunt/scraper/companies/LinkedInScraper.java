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

/**
 * Scrapes LinkedIn's public (no-auth) job search for India internships.
 * Uses the guest search endpoint — no login required.
 */
@Component
public class LinkedInScraper implements JobScraper
{
    // LinkedIn public job search — filtered to India, Internship type
    private static final String BASE_URL =
            "https://www.linkedin.com/jobs-guest/jobs/api/seeMoreJobPostings/search" +
            "?keywords=internship&location=India&f_JT=I&f_TPR=r2592000&start=";

    private static final int MAX_PAGES = 5;
    private static final int PAGE_SIZE  = 25;

    private Source source;

    @Override
    public void setSource(Source source) { this.source = source; }

    @Override
    public String getSourceName() { return "linkedin"; }

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
                System.out.println("Fetching LinkedIn page " + (page + 1) + ": " + url);

                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                                 + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .header("Accept", "text/html,application/xhtml+xml")
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .timeout(20000)
                        .get();

                Elements cards = doc.select("li");
                System.out.println("Found " + cards.size() + " cards on page " + (page + 1));

                if (cards.isEmpty()) break;

                for (Element card : cards)
                {
                    JobListing job = parseCard(card);
                    if (job != null) jobs.add(job);
                }

                Thread.sleep(1500);
            }
            catch (Exception e)
            {
                System.err.println("[linkedin] Error on page " + (page + 1) + ": " + e.getMessage());
                break;
            }
        }

        System.out.println("[linkedin] Total jobs scraped: " + jobs.size());
        return jobs;
    }

    private JobListing parseCard(Element card)
    {
        try
        {
            // Title
            Element titleEl = card.selectFirst("h3.base-search-card__title");
            if (titleEl == null) titleEl = card.selectFirst(".job-search-card__title");
            if (titleEl == null) return null;
            String title = titleEl.text().trim();
            if (title.isEmpty()) return null;

            // Company
            Element compEl = card.selectFirst("h4.base-search-card__subtitle");
            if (compEl == null) compEl = card.selectFirst(".job-search-card__company-name");
            String company = compEl != null ? compEl.text().trim() : "Unknown";

            // Location
            Element locEl = card.selectFirst("span.job-search-card__location");
            if (locEl == null) locEl = card.selectFirst(".base-search-card__metadata span");
            String location = locEl != null ? locEl.text().trim() : "India";

            // URL
            Element linkEl = card.selectFirst("a.base-card__full-link");
            if (linkEl == null) linkEl = card.selectFirst("a[href*='linkedin.com/jobs/view']");
            if (linkEl == null) return null;
            String url = linkEl.attr("href").split("\\?")[0]; // strip tracking params

            boolean isRemote = location.toLowerCase().contains("remote")
                             || location.toLowerCase().contains("work from home");

            JobListing job = new JobListing();
            job.setJobTitle(title.length() > 200 ? title.substring(0, 200) : title);
            job.setCompanyName(company.length() > 200 ? company.substring(0, 200) : company);
            job.setSourceUrl(url.length() > 500 ? url.substring(0, 500) : url);
            job.setSource(source);
            job.setLocation(location.length() > 200 ? location.substring(0, 200) : location);
            job.setIsRemote(isRemote);
            job.setStipend(null);
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
