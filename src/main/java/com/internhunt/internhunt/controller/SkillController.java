package com.internhunt.internhunt.controller;

import com.internhunt.internhunt.entity.Skill;
import com.internhunt.internhunt.service.SkillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/skills")
public class SkillController
{
    @Autowired
    private SkillService skillService;

    @GetMapping
    public List<Skill> getAllSkills()
    {
        return skillService.getAllSkills();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Skill> getSkillById(@PathVariable Integer id)
    {
        return skillService.getSkillById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Skill createSkill(@RequestBody Skill skill)
    {
        return skillService.createSkill(skill);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSkill(@PathVariable Integer id)
    {
        return skillService.getSkillById(id)
                .map(skill ->
                {
                    skillService.deleteSkill(id);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
