package com.internhunt.internhunt.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "job_required_skills")
public class JobRequiredSkill
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "job_id", nullable = false)
    private JobListing job;

    @ManyToOne
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(nullable = false)
    private Integer minimumProficiency;

    @Column(nullable = false)
    private Boolean isMandatory = true;

    // Getters and Setters
    public Integer getId()
    {
        return id;
    }

    public void setId(Integer id)
    {
        this.id = id;
    }

    public JobListing getJob()
    {
        return job;
    }

    public void setJob(JobListing job)
    {
        this.job = job;
    }

    public Skill getSkill()
    {
        return skill;
    }

    public void setSkill(Skill skill)
    {
        this.skill = skill;
    }

    public Integer getMinimumProficiency()
    {
        return minimumProficiency;
    }

    public void setMinimumProficiency(Integer minimumProficiency)
    {
        this.minimumProficiency = minimumProficiency;
    }

    public Boolean getIsMandatory()
    {
        return isMandatory;
    }

    public void setIsMandatory(Boolean isMandatory)
    {
        this.isMandatory = isMandatory;
    }
}