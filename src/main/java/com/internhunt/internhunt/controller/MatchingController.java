package com.internhunt.internhunt.controller;

import com.internhunt.internhunt.dto.MatchResult;
import com.internhunt.internhunt.service.KeywordSkillExtractor;
import com.internhunt.internhunt.service.MatchingService;
import com.internhunt.internhunt.repository.JobListingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST endpoints for skill matching and extraction.
 *
 * GET  /api/match/{userId}/{jobId}          Full match breakdown
 * GET  /api/match/{userId}/scores?jobs=1,2  Batch quick scores for job cards
 * POST /api/skills/extract/{jobId}          Manually trigger for one job
 * POST /api/skills/backfill                 Backfill all existing jobs (run once)
 */
@RestController
public class MatchingController
{
    @Autowired private MatchingService       matchingService;
    @Autowired private KeywordSkillExtractor skillExtractor;
    @Autowired private JobListingRepository  jobListingRepository;

    // ── Matching ──────────────────────────────────────────────────────────────

    @GetMapping("/api/match/{userId}/{jobId}")
    public ResponseEntity<MatchResult> getMatch(
            @PathVariable Integer userId, @PathVariable Integer jobId)
    {
        try { return ResponseEntity.ok(matchingService.fullMatch(userId, jobId)); }
        catch (Exception e) { return ResponseEntity.internalServerError().build(); }
    }

    @GetMapping("/api/match/{userId}/scores")
    public ResponseEntity<Map<Integer, Integer>> batchScores(
            @PathVariable Integer userId, @RequestParam List<Integer> jobs)
    {
        return ResponseEntity.ok(matchingService.batchQuickScores(userId, jobs));
    }

    // ── Skill Extraction ──────────────────────────────────────────────────────

    @PostMapping("/api/skills/extract/{jobId}")
    public ResponseEntity<?> extractSkills(@PathVariable Integer jobId)
    {
        return jobListingRepository.findById(jobId)
                .map(job ->
                {
                    skillExtractor.extractSkillsForJob(job);
                    return ResponseEntity.accepted()
                            .body(Map.of("message", "Extraction started for job " + jobId));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/skills/backfill")
    public ResponseEntity<?> backfillSkills()
    {
        skillExtractor.backfillAll();
        return ResponseEntity.accepted()
                .body(Map.of("message", "Backfill started — check logs for progress."));
    }
}
