package com.internhunt.internhunt.entity;
import jakarta.persistence.*;

@Entity
@Table(name="skills")
public class Skill
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    public enum Category
    {
        language,
        framework,
        database,
        cloud,
        devops,
        tool,
        ai_ml,
        security,
        soft_skill
    }

    // Getters and setters
    public Integer getId()
    {
        return id;
    }
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }
    public void setId(Integer id)
    {
        this.id = id;
    }
    public Category getCategory()
    {
        return category;
    }
    public void setCategory(Category category)
    {
        this.category = category;
    }
}



