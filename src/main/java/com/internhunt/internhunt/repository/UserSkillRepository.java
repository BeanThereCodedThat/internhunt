package com.internhunt.internhunt.repository;

import com.internhunt.internhunt.entity.UserSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserSkillRepository extends JpaRepository<UserSkill, Integer>
{
    List<UserSkill> findByUserId(Integer userId);

    List<UserSkill> findBySkillId(Integer skillId);
}