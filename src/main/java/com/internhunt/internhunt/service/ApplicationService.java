package com.internhunt.internhunt.service;

import com.internhunt.internhunt.entity.Application;
import com.internhunt.internhunt.repository.ApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ApplicationService
{
    @Autowired
    private ApplicationRepository applicationRepository;

    public Application createApplication(Application application)
    {
        return applicationRepository.save(application);
    }

    public Optional<Application> getApplicationById(Integer id)
    {
        return applicationRepository.findById(id);
    }

    public List<Application> getApplicationsByUser(Integer userId)
    {
        return applicationRepository.findByUserId(userId);
    }

    public List<Application> getApplicationsByUserAndStatus(Integer userId, Application.Status status)
    {
        return applicationRepository.findByUserIdAndStatus(userId, status);
    }

    public boolean hasUserApplied(Integer userId, Integer jobId)
    {
        return applicationRepository.findByUserIdAndJobId(userId, jobId).isPresent();
    }

    public Application updateApplication(Application application)
    {
        return applicationRepository.save(application);
    }

    public void deleteApplication(Integer id)
    {
        applicationRepository.deleteById(id);
    }
}