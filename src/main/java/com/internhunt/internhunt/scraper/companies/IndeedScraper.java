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
 * Scrapes Indeed India for internship/entry-level listings.
 * Indeed renders basic HTML for non-JS agents — Jsoup sufficient.
 */
@Component
public class IndeedScraper implements JobScraper
{
    private static final String BASE_URL =
            "https://in.indeed.com/jobs?q=internship+software&l=India&sort=date&start=";

    private static final int MAX_PAGES = 5;
    private static final int PAGE_SIZE  = 15; // Indeed typically shows 15 per page

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
                        .cookie("CTK", "1")           // basic cookie to avoid redirect loops
                        .timeout(20000)
                        .get();

                // Indeed job cards (classic + mosaic layouts)
                Elements cards = doc.select("div.job_seen_beacon");
                if (cards.isEmpty()) cards = doc.select(".resultContent");
                if (cards.isEmpty()) cards = doc.select("[class*=jobsearch-SerpJobCard]");

                System.out.println("[indeed] Page " + (page + 1) + ": " + cards.size() + " cards");

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
                System.err.println("[indeed] Error on page " + (page + 1) + ": " + e.getMessage());
                break;
            }
        }

        System.out.println("[indeed] Total scraped: " + jobs.size());
        return jobs;
    }

    private JobListing parseCard(Element card)
    {
        try
        {
            // Title
            Element titleEl = card.selectFirst("h2.jobTitle span[title]");
            if (titleEl == null) titleEl = card.selectFirst("h2.jobTitle a");
            if (titleEl == null) titleEl = card.selectFirst("[class*=jobTitle]");
            if (titleEl == null) return null;

            String title = titleEl.attr("title");
            if (title.isEmpty()) title = titleEl.text().trim();
            if (title.isEmpty()) return null;

            // Job link
            Element linkEl = card.selectFirst("h2.jobTitle a");
            if (linkEl == null) linkEl = card.selectFirst("a[id^=job_]");
            if (linkEl == null) linkEl = card.selectFirst("a[href*='/rc/clk']");
            String relUrl = linkEl != null ? linkEl.attr("href") : null;
            if (relUrl == null || relUrl.isEmpty()) return null;
            String sourceUrl = relUrl.startsWith("http")
                    ? relUrl
                    : "https://in.indeed.com" + relUrl;

            // Company
            Element compEl = card.selectFirst("[data-testid='company-name']");
            if (compEl == null) compEl = card.selectFirst(".companyName");
            String company = compEl != null ? compEl.text().trim() : "Unknown";

            // Location
            Element locEl = card.selectFirst("[data-testid='text-location']");
            if (locEl == null) locEl = card.selectFirst(".companyLocation");
            String location = locEl != null ? locEl.text().trim() : "India";

            // Salary/stipend snippet
            Element salEl = card.selectFirst("[data-testid='attribute_snippet_testid']");
            if (salEl == null) salEl = card.selectFirst(".salary-snippet");
            String stipend = salEl != null ? salEl.text().trim() : null;

            boolean isRemote = location.toLowerCase().contains("remote")
                             || location.toLowerCase().contains("work from home");

            String titleLower = title.toLowerCase();
            JobListing.ListingType type = titleLower.contains("intern")
                    ? JobListing.ListingType.internship
                    : JobListing.ListingType.full_time;

            // Strip tracking junk from URL
            int trackIdx = sourceUrl.indexOf("&tk=");
            if (trackIdx != -1) sourceUrl = sourceUrl.substring(0, trackIdx);

            JobListing job = new JobListing();
            job.setJobTitle(title.length() > 200 ? title.substring(0, 200) : title);
            job.setCompanyName(company.length() > 200 ? company.substring(0, 200) : company);
            job.setSourceUrl(sourceUrl.length() > 500 ? sourceUrl.substring(0, 500) : sourceUrl);
            job.setSource(source);
            job.setLocation(location.length() > 200 ? location.substring(0, 200) : location);
            job.setIsRemote(isRemote);
            job.setStipend(stipend != null && stipend.length() > 100 ? null : stipend);
            job.setListingType(type);
            job.setStatus(JobListing.Status.ACTIVE);

            return job;
        }
        catch (Exception e)
        {
            return null;
        }
    }
}
