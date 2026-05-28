package com.internhunt.internhunt.service;

import com.internhunt.internhunt.entity.Skill;
import com.internhunt.internhunt.repository.SkillRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class SkillService
{
    @Autowired
    private SkillRepository skillRepository;

    public Skill createSkill(Skill skill)
    {
        return skillRepository.save(skill);
    }

    public Optional<Skill> getSkillById(Integer id)
    {
        return skillRepository.findById(id);
    }

    public List<Skill> getAllSkills()
    {
        return skillRepository.findAll();
    }

    public void deleteSkill(Integer id)
    {
        skillRepository.deleteById(id);
    }
}