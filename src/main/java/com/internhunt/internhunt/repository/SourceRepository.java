package com.internhunt.internhunt.repository;

import com.internhunt.internhunt.entity.Source;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SourceRepository extends JpaRepository<Source, Integer>
{
    List<Source> findByIsActiveTrue();

    Optional<Source> findByName(String name);
}