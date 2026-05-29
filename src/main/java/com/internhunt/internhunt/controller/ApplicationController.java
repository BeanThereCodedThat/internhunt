package com.internhunt.internhunt.controller;

import com.internhunt.internhunt.entity.Application;
import com.internhunt.internhunt.service.ApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController
{
    @Autowired
    private ApplicationService applicationService;

    @GetMapping("/user/{userId}")
    public List<Application> getApplicationsByUser(@PathVariable Integer userId)
    {
        return applicationService.getApplicationsByUser(userId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Application> getApplicationById(@PathVariable Integer id)
    {
        return applicationService.getApplicationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Application createApplication(@RequestBody Application application)
    {
        return applicationService.createApplication(application);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Application> updateApplication(@PathVariable Integer id, @RequestBody Application application)
    {
        return applicationService.getApplicationById(id)
                .map(existing ->
                {
                    application.setId(id);
                    return ResponseEntity.ok(applicationService.updateApplication(application));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(@PathVariable Integer id)
    {
        return applicationService.getApplicationById(id)
                .map(app ->
                {
                    applicationService.deleteApplication(id);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}