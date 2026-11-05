package com.atsoptimizer.modules.resume.domain;

import com.atsoptimizer.modules.resume.exception.ResumeDomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "resumes")
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ResumeStatus status;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "analysis", columnDefinition = "jsonb")
    private ResumeAnalysis analysis;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Resume() {
    }

    public Resume(String fileName, String contentType, Long fileSize, String extractedText) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.extractedText = extractedText;
        this.status = ResumeStatus.UPLOADED;
    }

    public Resume(Long id, String fileName, String contentType, Long fileSize, String extractedText) {
        this(fileName, contentType, fileSize, extractedText);
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

    // Métodos semânticos de negócio (Rich Domain Model)
    public void markAsProcessing() {
        if (this.status == ResumeStatus.COMPLETED) {
            throw new ResumeDomainException("Cannot reprocess an already completed resume");
        }
        this.status = ResumeStatus.PROCESSING;
        this.errorMessage = null;
    }

    public void completeAnalysis(ResumeAnalysis analysis) {
        if (analysis == null) {
            throw new ResumeDomainException("Analysis cannot be null");
        }
        this.analysis = analysis;
        this.status = ResumeStatus.COMPLETED;
        this.errorMessage = null;
    }

    public void failAnalysis(String errorMessage) {
        this.status = ResumeStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public Long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public ResumeStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public ResumeAnalysis getAnalysis() {
        return analysis;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
