package com.internhunt.internhunt.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig
{
    // Enables @Async on ScraperService methods so HTTP requests
    // return immediately instead of blocking while scraping runs.
}
