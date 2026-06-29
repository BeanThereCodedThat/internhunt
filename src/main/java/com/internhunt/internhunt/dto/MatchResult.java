package com.internhunt.internhunt.dto;

import java.util.List;

/**
 * Match result between a user and a job listing.
 * Returned by GET /api/match/{userId}/{jobId}
 * and as scores in GET /api/match/{userId}/scores
 */
public class MatchResult
{
    private int                  score;           // 0–100
    private String               summary;         // "You match 8/10 required skills. Missing: Docker, Kubernetes"
    private List<SkillMatch>     matched;         // skills user has that job needs
    private List<String>         missing;         // skills job needs that user doesn't have
    private List<String>         bonus;           // extra skills user has beyond requirements
    private boolean              meetsRequirements; // true if all mandatory skills are met

    // ── Inner DTO ─────────────────────────────────────────────────────────────

    public static class SkillMatch
    {
        private String  skillName;
        private int     userProficiency;
        private int     requiredProficiency;
        private boolean mandatory;

        public SkillMatch() {}
        public SkillMatch(String skillName, int userProficiency, int requiredProficiency, boolean mandatory)
        {
            this.skillName           = skillName;
            this.userProficiency     = userProficiency;
            this.requiredProficiency = requiredProficiency;
            this.mandatory           = mandatory;
        }

        public String  getSkillName()              { return skillName; }
        public int     getUserProficiency()         { return userProficiency; }
        public int     getRequiredProficiency()     { return requiredProficiency; }
        public boolean isMandatory()               { return mandatory; }
        public void    setSkillName(String v)      { skillName = v; }
        public void    setUserProficiency(int v)   { userProficiency = v; }
        public void    setRequiredProficiency(int v){ requiredProficiency = v; }
        public void    setMandatory(boolean v)     { mandatory = v; }
    }

    // ── Static factories ──────────────────────────────────────────────────────

    public static MatchResult noProfile()
    {
        MatchResult r = new MatchResult();
        r.score             = 0;
        r.summary           = "Add skills to your profile to see your match score.";
        r.matched           = List.of();
        r.missing           = List.of();
        r.bonus             = List.of();
        r.meetsRequirements = false;
        return r;
    }

    public static MatchResult noSkillData(String jobTitle)
    {
        MatchResult r = new MatchResult();
        r.score             = 0;
        r.summary           = "No skill data extracted for this listing yet. Try again in a moment.";
        r.matched           = List.of();
        r.missing           = List.of();
        r.bonus             = List.of();
        r.meetsRequirements = false;
        return r;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public int              getScore()                   { return score; }
    public void             setScore(int v)              { score = v; }
    public String           getSummary()                 { return summary; }
    public void             setSummary(String v)         { summary = v; }
    public List<SkillMatch> getMatched()                 { return matched; }
    public void             setMatched(List<SkillMatch> v) { matched = v; }
    public List<String>     getMissing()                 { return missing; }
    public void             setMissing(List<String> v)   { missing = v; }
    public List<String>     getBonus()                   { return bonus; }
    public void             setBonus(List<String> v)     { bonus = v; }
    public boolean          isMeetsRequirements()        { return meetsRequirements; }
    public void             setMeetsRequirements(boolean v) { meetsRequirements = v; }
}
