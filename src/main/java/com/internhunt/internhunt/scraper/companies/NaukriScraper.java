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
 * Scrapes Naukri.com for freshers / entry-level / internship listings in India.
 * Naukri serves server-side HTML for its search pages — Jsoup works directly.
 */
@Component
public class NaukriScraper implements JobScraper
{
    // Freshers + Internship results, ordered by recency
    private static final String BASE_URL =
            "https://www.naukri.com/internship-jobs-in-india?pageNo=";

    private static final int MAX_PAGES = 5;

    private Source source;

    @Override
    public void setSource(Source source) { this.source = source; }

    @Override
    public String getSourceName() { return "naukri"; }

    @Override
    public List<JobListing> scrape()
    {
        List<JobListing> jobs = new ArrayList<>();

        for (int page = 1; page <= MAX_PAGES; page++)
        {
            try
            {
                String url = BASE_URL + page;
                System.out.println("[naukri] Fetching page " + page + ": " + url);

                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                                 + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .header("Accept-Language", "en-IN,en-US;q=0.9,en;q=0.8")
                        .header("Referer", "https://www.naukri.com/")
                        .timeout(20000)
                        .get();

                // Naukri job cards use article.jobTuple or .job-container
                Elements cards = doc.select("article.jobTuple");
                if (cards.isEmpty()) cards = doc.select(".jobTupleHeader");
                if (cards.isEmpty()) cards = doc.select("[class*=tuple]");

                System.out.println("[naukri] Page " + page + ": " + cards.size() + " listings found");

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
                System.err.println("[naukri] Error on page " + page + ": " + e.getMessage());
                break;
            }
        }

        System.out.println("[naukri] Total scraped: " + jobs.size());
        return jobs;
    }

    private JobListing parseCard(Element card)
    {
        try
        {
            // Title — multiple possible selectors across Naukri redesigns
            Element titleEl = card.selectFirst("a.title");
            if (titleEl == null) titleEl = card.selectFirst(".jobTitle a");
            if (titleEl == null) titleEl = card.selectFirst("a[title]");
            if (titleEl == null) return null;

            String title = titleEl.text().trim();
            if (title.isEmpty()) title = titleEl.attr("title").trim();
            if (title.isEmpty()) return null;

            String sourceUrl = titleEl.attr("href");
            if (sourceUrl == null || sourceUrl.isEmpty()) return null;
            if (!sourceUrl.startsWith("http")) sourceUrl = "https://www.naukri.com" + sourceUrl;

            // Company
            Element compEl = card.selectFirst(".companyInfo a");
            if (compEl == null) compEl = card.selectFirst(".comp-name");
            String company = compEl != null ? compEl.text().trim() : "Unknown";

            // Location
            Element locEl = card.selectFirst(".locWdth");
            if (locEl == null) locEl = card.selectFirst(".location span");
            if (locEl == null) locEl = card.selectFirst("[class*=location]");
            String location = locEl != null ? locEl.text().trim() : "India";

            // Experience / stipend hint
            Element expEl = card.selectFirst(".expwdth");
            String stipend = null;
            if (expEl != null)
            {
                String expText = expEl.text().trim();
                if (expText.contains("0") && expText.contains("Yrs")) stipend = "Freshers";
            }

            boolean isRemote = location.toLowerCase().contains("remote")
                             || location.toLowerCase().contains("work from home");

            // Determine listing type
            String titleLower = title.toLowerCase();
            JobListing.ListingType type = titleLower.contains("intern")
                    ? JobListing.ListingType.internship
                    : JobListing.ListingType.full_time;

            JobListing job = new JobListing();
            job.setJobTitle(title.length() > 200 ? title.substring(0, 200) : title);
            job.setCompanyName(company.length() > 200 ? company.substring(0, 200) : company);
            job.setSourceUrl(sourceUrl.length() > 500 ? sourceUrl.substring(0, 500) : sourceUrl);
            job.setSource(source);
            job.setLocation(location.length() > 200 ? location.substring(0, 200) : location);
            job.setIsRemote(isRemote);
            job.setStipend(stipend);
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
