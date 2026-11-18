package com.atsoptimizer.modules.jobmatch.repository;

import com.atsoptimizer.modules.jobmatch.domain.JobMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobMatchRepository extends JpaRepository<JobMatch, Long> {

    @Query("SELECT jm FROM JobMatch jm JOIN FETCH jm.resume WHERE jm.id = :id")
    Optional<JobMatch> findWithResumeById(@Param("id") Long id);

    @Query("SELECT jm FROM JobMatch jm JOIN FETCH jm.resume r WHERE r.id = :resumeId ORDER BY jm.createdAt DESC")
    List<JobMatch> findAllByResumeIdWithResume(@Param("resumeId") Long resumeId);
}
