package com.atsoptimizer.modules.resume.service;

import com.atsoptimizer.infra.ai.ResumeAnalysisAI;
import com.atsoptimizer.infra.ai.dto.CertificationDto;
import com.atsoptimizer.infra.ai.dto.ContactInfoDto;
import com.atsoptimizer.infra.ai.dto.EducationDto;
import com.atsoptimizer.infra.ai.dto.ExperienceDto;
import com.atsoptimizer.infra.ai.dto.LanguageDto;
import com.atsoptimizer.infra.ai.dto.ResumeAnalysisResult;
import com.atsoptimizer.modules.resume.domain.CertificationItem;
import com.atsoptimizer.modules.resume.domain.ContactInfo;
import com.atsoptimizer.modules.resume.domain.EducationItem;
import com.atsoptimizer.modules.resume.domain.ExperienceItem;
import com.atsoptimizer.modules.resume.domain.LanguageItem;
import com.atsoptimizer.modules.resume.domain.Resume;
import com.atsoptimizer.modules.resume.domain.ResumeAnalysis;
import com.atsoptimizer.modules.resume.exception.ResumeNotFoundException;
import com.atsoptimizer.modules.resume.repository.ResumeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class ResumeAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ResumeAnalysisService.class);

    private final ResumeRepository resumeRepository;
    private final ResumeAnalysisAI resumeAnalysisAi;

    public ResumeAnalysisService(
            ResumeRepository resumeRepository,
            ResumeAnalysisAI resumeAnalysisAi
    ) {
        this.resumeRepository = resumeRepository;
        this.resumeAnalysisAi = resumeAnalysisAi;
    }

    @Transactional
    public void processAnalysis(Long resumeId, String extractedText) {
        log.info("Starting AI analysis pipeline for resume id: {}", resumeId);

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResumeNotFoundException(resumeId));

        try {
            resume.markAsProcessing();
            resumeRepository.save(resume);

            ResumeAnalysisResult dto = resumeAnalysisAi.sendToAiAnalisysResume(extractedText);

            ResumeAnalysis analysis = new ResumeAnalysis(
                    resumeId,
                    dto.score(),
                    dto.candidateName(),
                    dto.headline(),
                    mapContactInfo(dto.contactInfo()),
                    dto.about(),
                    mapExperiences(dto.experiences()),
                    mapEducation(dto.education()),
                    mapCertifications(dto.certifications()),
                    mapLanguages(dto.languages()),
                    dto.inferredObjective(),
                    dto.overallProfile(),
                    dto.structureEvaluation(),
                    dto.atsCompatibilitySummary(),
                    dto.keywords(),
                    dto.missingKeywords(),
                    dto.inconsistencies(),
                    dto.strengths(),
                    dto.weaknesses(),
                    dto.improvementSuggestions(),
                    LocalDateTime.now()
            );

            resume.completeAnalysis(analysis);
            resumeRepository.save(resume);

            log.info("Successfully completed AI analysis for resume [{}]. Candidate: '{}', ATS Score: {}%",
                    resumeId, dto.candidateName(), dto.score());
        } catch (Exception e) {
            log.error("Error during AI analysis for resume [{}]: {}", resumeId, e.getMessage(), e);
            throw e;
        }
    }

    private ContactInfo mapContactInfo(ContactInfoDto dto) {
        if (dto == null) {
            return null;
        }
        return new ContactInfo(
                dto.phone(),
                dto.email(),
                dto.location(),
                dto.linkedin(),
                dto.otherLinks()
        );
    }

    private List<ExperienceItem> mapExperiences(List<ExperienceDto> dtoList) {
        if (dtoList == null) {
            return Collections.emptyList();
        }
        return dtoList.stream()
                .map(e -> new ExperienceItem(
                        e.title(),
                        e.organization(),
                        e.period(),
                        e.location(),
                        e.description(),
                        e.highlights()
                ))
                .toList();
    }

    private List<EducationItem> mapEducation(List<EducationDto> dtoList) {
        if (dtoList == null) {
            return Collections.emptyList();
        }
        return dtoList.stream()
                .map(e -> new EducationItem(
                        e.institution(),
                        e.degree(),
                        e.fieldOfStudy(),
                        e.period(),
                        e.status()
                ))
                .toList();
    }

    private List<CertificationItem> mapCertifications(List<CertificationDto> dtoList) {
        if (dtoList == null) {
            return Collections.emptyList();
        }
        return dtoList.stream()
                .map(c -> new CertificationItem(
                        c.name(),
                        c.issuer(),
                        c.year()
                ))
                .toList();
    }

    private List<LanguageItem> mapLanguages(List<LanguageDto> dtoList) {
        if (dtoList == null) {
            return Collections.emptyList();
        }
        return dtoList.stream()
                .map(l -> new LanguageItem(
                        l.language(),
                        l.proficiency()
                ))
                .toList();
    }
}
