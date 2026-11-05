package com.atsoptimizer.modules.resume.repository;

import com.atsoptimizer.modules.resume.domain.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {
}
