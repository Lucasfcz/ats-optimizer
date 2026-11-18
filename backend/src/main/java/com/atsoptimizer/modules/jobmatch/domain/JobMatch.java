package com.atsoptimizer.modules.jobmatch.domain;

import com.atsoptimizer.modules.jobmatch.exception.GeneratedResumeNotFoundException;
import com.atsoptimizer.modules.jobmatch.exception.JobMatchDomainException;
import com.atsoptimizer.modules.jobmatch.exception.JobMatchNotReadyForGenerationException;
import com.atsoptimizer.modules.resume.domain.Resume;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_matches")
public class JobMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Column(name = "job_description", columnDefinition = "TEXT", nullable = false)
    private String jobDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private JobMatchStatus status;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "job_requirements", columnDefinition = "jsonb")
    private JobRequirements jobRequirements;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "keyword_match_report", columnDefinition = "jsonb")
    private KeywordMatchReport keywordMatchReport;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fit_audit", columnDefinition = "jsonb")
    private JobFitAudit fitAudit;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "truthful_resume_data", columnDefinition = "jsonb")
    private StructuredResumeTemplateData truthfulResumeData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ideal_resume_data", columnDefinition = "jsonb")
    private StructuredResumeTemplateData idealResumeData;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected JobMatch() {
    }

    public JobMatch(Resume resume, String jobDescription) {
        if (resume == null) {
            throw new JobMatchDomainException("Resume cannot be null when creating a job match");
        }
        if (jobDescription == null || jobDescription.isBlank()) {
            throw new JobMatchDomainException("Job description cannot be empty");
        }
        this.resume = resume;
        this.jobDescription = jobDescription;
        this.status = JobMatchStatus.PENDING_ANALYSIS;
    }

    public JobMatch(Long id, Resume resume, String jobDescription) {
        this(resume, jobDescription);
        this.id = id;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // --- Semantic Business Transitions ---

    public void markAsParsingJob() {
        this.status = JobMatchStatus.PARSING_JOB;
        this.errorMessage = null;
    }

    public void completeJobParsing(JobRequirements jobRequirements, KeywordMatchReport keywordMatchReport) {
        if (jobRequirements == null || keywordMatchReport == null) {
            throw new JobMatchDomainException("Job requirements and keyword match report cannot be null");
        }
        this.jobRequirements = jobRequirements;
        this.keywordMatchReport = keywordMatchReport;
        this.status = JobMatchStatus.AUDITING_FIT;
        this.errorMessage = null;
    }

    public void completeFitAudit(JobFitAudit fitAudit) {
        if (fitAudit == null) {
            throw new JobMatchDomainException("Fit audit cannot be null");
        }
        this.fitAudit = fitAudit;
        this.status = JobMatchStatus.ANALYZED;
        this.errorMessage = null;
    }

    public void ensureReadyForResumeGeneration() {
        if (this.status != JobMatchStatus.ANALYZED
                && this.status != JobMatchStatus.COMPLETED
                && this.status != JobMatchStatus.GENERATING_RESUME) {
            throw new JobMatchNotReadyForGenerationException(this.id, this.status);
        }
    }

    public void markAsGeneratingResume() {
        ensureReadyForResumeGeneration();
        this.status = JobMatchStatus.GENERATING_RESUME;
        this.errorMessage = null;
    }

    public void completeResumeGeneration(OptimizationMode mode, StructuredResumeTemplateData templateData) {
        if (mode == null || templateData == null) {
            throw new JobMatchDomainException("Optimization mode and structured resume data cannot be null");
        }
        if (mode == OptimizationMode.TRUTHFUL) {
            this.truthfulResumeData = templateData;
        } else {
            this.idealResumeData = templateData;
        }
        this.status = JobMatchStatus.COMPLETED;
        this.errorMessage = null;
    }

    public void failMatch(String errorMessage) {
        this.status = JobMatchStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public StructuredResumeTemplateData requireGeneratedResumeData(OptimizationMode mode) {
        StructuredResumeTemplateData data = mode == OptimizationMode.TRUTHFUL
                ? this.truthfulResumeData
                : this.idealResumeData;
        if (data == null) {
            throw new GeneratedResumeNotFoundException(this.id, mode);
        }
        return data;
    }

    public Long getId() {
        return id;
    }

    public Resume getResume() {
        return resume;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public JobMatchStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public JobRequirements getJobRequirements() {
        return jobRequirements;
    }

    public KeywordMatchReport getKeywordMatchReport() {
        return keywordMatchReport;
    }

    public JobFitAudit getFitAudit() {
        return fitAudit;
    }

    public StructuredResumeTemplateData getTruthfulResumeData() {
        return truthfulResumeData;
    }

    public StructuredResumeTemplateData getIdealResumeData() {
        return idealResumeData;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
