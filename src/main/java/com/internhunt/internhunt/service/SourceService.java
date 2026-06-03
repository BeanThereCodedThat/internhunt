package com.internhunt.internhunt.service;

import com.internhunt.internhunt.entity.Source;
import com.internhunt.internhunt.repository.SourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class SourceService
{
    @Autowired
    private SourceRepository sourceRepository;

    public Source createSource(Source source)
    {
        return sourceRepository.save(source);
    }

    public Optional<Source> getSourceById(Integer id)
    {
        return sourceRepository.findById(id);
    }

    public List<Source> getAllSources()
    {
        return sourceRepository.findAll();
    }

    public List<Source> getActiveSources()
    {
        return sourceRepository.findByIsActiveTrue();
    }

    public Source updateSource(Source source)
    {
        return sourceRepository.save(source);
    }

    public void deleteSource(Integer id)
    {
        sourceRepository.deleteById(id);
    }
}