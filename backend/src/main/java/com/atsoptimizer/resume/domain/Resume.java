package com.atsoptimizer.resume.domain;

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
import java.util.Objects;

@Entity
@Table(name = "resumes")
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    private String contentType;

    private Long fileSize;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String extractedText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResumeStatus status = ResumeStatus.UPLOADED;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "analysis", columnDefinition = "jsonb")
    private ResumeAnalysis analysis;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    protected Resume() {
        // Construtor vazio para o Hibernate
    }

    public Resume(String fileName, String contentType, Long fileSize, String extractedText) {
        this.fileName = Objects.requireNonNull(fileName, "fileName cannot be null");
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.extractedText = Objects.requireNonNull(extractedText, "extractedText cannot be null");
        this.status = ResumeStatus.UPLOADED;
    }

    // Construtor completo para criação manual ou testes unitários
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
            throw new IllegalStateException("Cannot reprocess an already completed resume");
        }
        this.status = ResumeStatus.PROCESSING;
        this.errorMessage = null;
    }

    public void completeAnalysis(ResumeAnalysis analysis) {
        this.analysis = Objects.requireNonNull(analysis, "Analysis cannot be null");
        this.status = ResumeStatus.COMPLETED;
        this.errorMessage = null;
    }

    public void failAnalysis(String errorMessage) {
        this.status = ResumeStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    // Getters apenas (sem setters públicos)
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

    public String getExtractedText() {
        return extractedText;
    }

    public ResumeStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
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
