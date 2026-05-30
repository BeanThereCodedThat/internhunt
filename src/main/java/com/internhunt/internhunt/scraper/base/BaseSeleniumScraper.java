package com.internhunt.internhunt.scraper.base;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public abstract class BaseSeleniumScraper implements JobScraper
{
    protected WebDriver driver;

    protected void initDriver()
    {
        WebDriverManager.chromedriver().setup();
        System.out.println("Initializing Chrome driver...");
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        driver = new ChromeDriver(options);
        System.out.println("Chrome driver initialized successfully");
    }

    protected void closeDriver()
    {
        if (driver != null)
        {
            driver.quit();
        }
    }

    protected void waitForPage(int seconds)
    {
        try
        {
            Thread.sleep(seconds * 1000L);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }
}