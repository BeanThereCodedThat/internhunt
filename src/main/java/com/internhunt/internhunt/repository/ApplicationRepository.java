package com.internhunt.internhunt.repository;

import com.internhunt.internhunt.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Integer>
{
    List<Application> findByUserId(Integer userId);

    List<Application> findByUserIdAndStatus(Integer userId, Application.Status status);

    Optional<Application> findByUserIdAndJobId(Integer userId, Integer jobId);
}