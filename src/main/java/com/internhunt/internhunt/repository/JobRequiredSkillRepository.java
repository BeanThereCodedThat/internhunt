package com.internhunt.internhunt.repository;

import com.internhunt.internhunt.entity.JobRequiredSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface JobRequiredSkillRepository extends JpaRepository<JobRequiredSkill, Integer>
{
    List<JobRequiredSkill> findByJobId(Integer jobId);

    List<JobRequiredSkill> findByJobIdAndIsMandatoryTrue(Integer jobId);
}