package com.internhunt.internhunt.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 15)
    private String phone;

    @Column(length = 500)
    private String resumeUrl;

    @Column(length = 500)
    private String githubUrl;

    @Column(length = 500)
    private String linkedinUrl;

    @Column(length = 200)
    private String college;

    private Integer graduationYear;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate()
    {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Integer getId()
    {
        return id;
    }

    public void setId(Integer id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getPhone()
    {
        return phone;
    }

    public void setPhone(String phone)
    {
        this.phone = phone;
    }

    public String getResumeUrl()
    {
        return resumeUrl;
    }

    public void setResumeUrl(String resumeUrl)
    {
        this.resumeUrl = resumeUrl;
    }

    public String getGithubUrl()
    {
        return githubUrl;
    }

    public void setGithubUrl(String githubUrl)
    {
        this.githubUrl = githubUrl;
    }

    public String getLinkedinUrl()
    {
        return linkedinUrl;
    }

    public void setLinkedinUrl(String linkedinUrl)
    {
        this.linkedinUrl = linkedinUrl;
    }

    public String getCollege()
    {
        return college;
    }

    public void setCollege(String college)
    {
        this.college = college;
    }

    public Integer getGraduationYear()
    {
        return graduationYear;
    }

    public void setGraduationYear(Integer graduationYear)
    {
        this.graduationYear = graduationYear;
    }

    public LocalDateTime getCreatedAt()
    {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt)
    {
        this.createdAt = createdAt;
    }
}