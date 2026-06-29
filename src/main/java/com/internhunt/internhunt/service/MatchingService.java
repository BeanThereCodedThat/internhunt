package com.internhunt.internhunt.service;

import com.internhunt.internhunt.dto.MatchResult;
import com.internhunt.internhunt.entity.JobListing;
import com.internhunt.internhunt.entity.JobRequiredSkill;
import com.internhunt.internhunt.entity.UserSkill;
import com.internhunt.internhunt.repository.JobListingRepository;
import com.internhunt.internhunt.repository.JobRequiredSkillRepository;
import com.internhunt.internhunt.repository.UserRepository;
import com.internhunt.internhunt.repository.UserSkillRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MatchingService — pure deterministic skill matching. No AI, no external calls.
 *
 * Scoring:
 *   Mandatory skill match  = 10 pts
 *   Optional skill match   =  3 pts
 *   Score = (earned / total) × 100
 *
 * Used by:
 *   GET /api/match/{userId}/{jobId}         — full breakdown
 *   GET /api/match/{userId}/scores?jobs=... — batch scores for job cards
 */
@Service
public class MatchingService
{
    @Autowired private UserRepository             userRepository;
    @Autowired private JobListingRepository       jobListingRepository;
    @Autowired private UserSkillRepository        userSkillRepository;
    @Autowired private JobRequiredSkillRepository jobRequiredSkillRepository;

    // ── Public API ────────────────────────────────────────────────────────────

    /** Full match result with skill breakdown. */
    public MatchResult fullMatch(Integer userId, Integer jobId)
    {
        if (userRepository.findById(userId).isEmpty()
                || jobListingRepository.findById(jobId).isEmpty())
            return MatchResult.noProfile();

        List<UserSkill>        userSkills = userSkillRepository.findByUserId(userId);
        List<JobRequiredSkill> required   = jobRequiredSkillRepository.findByJobId(jobId);

        if (userSkills.isEmpty()) return MatchResult.noProfile();
        if (required.isEmpty())
            return MatchResult.noSkillData(
                    jobListingRepository.findById(jobId).get().getJobTitle());

        return computeMatch(userSkills, required);
    }

    /** Quick score only — no breakdown. Returns -1 if no data. */
    public int quickScore(Integer userId, Integer jobId)
    {
        List<UserSkill>        us = userSkillRepository.findByUserId(userId);
        List<JobRequiredSkill> rq = jobRequiredSkillRepository.findByJobId(jobId);
        if (us.isEmpty() || rq.isEmpty()) return -1;
        return computeMatch(us, rq).getScore();
    }

    /** Batch scores for a page of job cards — one DB call per job, no AI. */
    public Map<Integer, Integer> batchQuickScores(Integer userId, List<Integer> jobIds)
    {
        List<UserSkill> userSkills = userSkillRepository.findByUserId(userId);
        Map<Integer, Integer> scores = new HashMap<>();

        if (userSkills.isEmpty())
        {
            jobIds.forEach(id -> scores.put(id, -1));
            return scores;
        }

        for (Integer jobId : jobIds)
        {
            List<JobRequiredSkill> required = jobRequiredSkillRepository.findByJobId(jobId);
            scores.put(jobId, required.isEmpty() ? -1 : computeMatch(userSkills, required).getScore());
        }
        return scores;
    }

    // ── Core computation ──────────────────────────────────────────────────────

    private MatchResult computeMatch(List<UserSkill> userSkills, List<JobRequiredSkill> required)
    {
        // Build lookups
        Map<Integer, Integer> byId   = userSkills.stream().collect(
                Collectors.toMap(us -> us.getSkill().getId(), UserSkill::getProficiency, (a, b) -> a));
        Map<String, Integer>  byName = userSkills.stream().collect(
                Collectors.toMap(us -> us.getSkill().getName().toLowerCase(), UserSkill::getProficiency, (a, b) -> a));

        List<MatchResult.SkillMatch> matched  = new ArrayList<>();
        List<String>                 missing  = new ArrayList<>();
        int total = 0, earned = 0;
        boolean meetsAll = true;

        for (JobRequiredSkill jrs : required)
        {
            int weight = jrs.getIsMandatory() ? 10 : 3;
            total += weight;

            Integer userP = byId.get(jrs.getSkill().getId());
            if (userP == null) userP = byName.get(jrs.getSkill().getName().toLowerCase());

            if (userP != null && userP >= jrs.getMinimumProficiency())
            {
                earned += weight;
                matched.add(new MatchResult.SkillMatch(
                        jrs.getSkill().getName(), userP,
                        jrs.getMinimumProficiency(), jrs.getIsMandatory()));
            }
            else
            {
                missing.add(jrs.getSkill().getName());
                if (jrs.getIsMandatory()) meetsAll = false;
            }
        }

        // Bonus: skills the user has that the job doesn't require
        Set<String> requiredNames = required.stream()
                .map(jrs -> jrs.getSkill().getName().toLowerCase())
                .collect(Collectors.toSet());
        List<String> bonus = userSkills.stream()
                .map(us -> us.getSkill().getName())
                .filter(n -> !requiredNames.contains(n.toLowerCase()))
                .limit(5)
                .collect(Collectors.toList());

        int score = total == 0 ? 0 : (int) Math.round((double) earned / total * 100);

        MatchResult r = new MatchResult();
        r.setScore(score);
        r.setSummary(buildSummary(matched.size(), required.size(), missing));
        r.setMatched(matched);
        r.setMissing(missing);
        r.setBonus(bonus);
        r.setMeetsRequirements(meetsAll);
        return r;
    }

    private String buildSummary(int matched, int total, List<String> missing)
    {
        String s = "You match " + matched + "/" + total + " required skills";
        if (missing.isEmpty()) return s + " — great fit!";
        int show = Math.min(missing.size(), 3);
        s += ". Missing: " + String.join(", ", missing.subList(0, show));
        if (missing.size() > 3) s += " +" + (missing.size() - 3) + " more";
        return s;
    }
}
