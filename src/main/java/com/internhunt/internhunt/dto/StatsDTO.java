package com.internhunt.internhunt.dto;

import java.util.List;

public class StatsDTO
{
    public long               totalJobs;
    public long               totalInternships;
    public long               totalRemote;
    public List<SourceCount>  bySource;
    public long               savedToday;

    public static class SourceCount
    {
        public String name;
        public long   count;

        public SourceCount(String name, long count)
        {
            this.name  = name;
            this.count = count;
        }
    }
}
