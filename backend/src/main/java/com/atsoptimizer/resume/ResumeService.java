package com.atsoptimizer.resume;

import com.atsoptimizer.parser.DocumentTextExtractor;
import com.atsoptimizer.queue.ResumeEventPublisher;
import com.atsoptimizer.queue.dto.ResumeAnalysisMessage;
import com.atsoptimizer.resume.domain.Resume;
import com.atsoptimizer.resume.dto.ResumeDetailsDto;
import com.atsoptimizer.resume.dto.ResumeResponseDto;
import com.atsoptimizer.resume.repository.ResumeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ResumeService {

    private static final Logger log = LoggerFactory.getLogger(ResumeService.class);

    private final ResumeRepository resumeRepository;
    private final DocumentTextExtractor documentTextExtractor;
    private final ResumeEventPublisher resumeEventPublisher;

    public ResumeService(
            ResumeRepository resumeRepository,
            DocumentTextExtractor documentTextExtractor,
            ResumeEventPublisher resumeEventPublisher
    ) {
        this.resumeRepository = resumeRepository;
        this.documentTextExtractor = documentTextExtractor;
        this.resumeEventPublisher = resumeEventPublisher;
    }

    @Transactional
    public ResumeResponseDto uploadAndProcess(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded resume file cannot be empty");
        }

        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "curriculum";
        log.info("Receiving upload for resume file: {} ({} bytes)", fileName, file.getSize());

        // 1. Synchronously extract text using Apache Tika
        String extractedText;
        try {
            extractedText = documentTextExtractor.extractText(file.getInputStream(), fileName);
        } catch (IOException e) {
            log.error("Failed to read input stream from file: {}", fileName, e);
            throw new IllegalArgumentException("Could not read uploaded file content: " + fileName, e);
        }

        // 2. Persist to database (constructor already initializes status = UPLOADED)
        Resume resume = new Resume(
                fileName,
                file.getContentType(),
                file.getSize(),
                extractedText
        );
        Resume savedResume = resumeRepository.save(resume);
        log.info("Resume saved in database with id: {}", savedResume.getId());

        // 3. Publish event to RabbitMQ topic exchange
        ResumeAnalysisMessage message = new ResumeAnalysisMessage(
                savedResume.getId(),
                savedResume.getFileName(),
                savedResume.getExtractedText()
        );
        resumeEventPublisher.publishResumeForAnalysis(message);

        return ResumeResponseDto.fromEntity(savedResume);
    }

    @Transactional(readOnly = true)
    public ResumeDetailsDto getResumeById(Long id) {
        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Resume not found with id: " + id));
        return ResumeDetailsDto.fromEntity(resume);
    }

    @Transactional(readOnly = true)
    public List<ResumeResponseDto> getAllResumes() {
        return resumeRepository.findAll()
                .stream()
                .map(ResumeResponseDto::fromEntity)
                .toList();
    }
}
