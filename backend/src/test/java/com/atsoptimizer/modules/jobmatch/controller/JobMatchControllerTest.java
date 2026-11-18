package com.atsoptimizer.modules.jobmatch.controller;

import com.atsoptimizer.infra.handler.GlobalExceptionHandler;
import com.atsoptimizer.modules.jobmatch.domain.JobMatchStatus;
import com.atsoptimizer.modules.jobmatch.domain.OptimizationMode;
import com.atsoptimizer.modules.jobmatch.dto.CreateJobMatchRequest;
import com.atsoptimizer.modules.jobmatch.dto.GenerateResumeRequest;
import com.atsoptimizer.modules.jobmatch.dto.JobMatchDetailsDto;
import com.atsoptimizer.modules.jobmatch.dto.JobMatchResponseDto;
import com.atsoptimizer.modules.jobmatch.exception.GeneratedResumeNotFoundException;
import com.atsoptimizer.modules.jobmatch.exception.JobMatchNotFoundException;
import com.atsoptimizer.modules.jobmatch.exception.JobMatchNotReadyForGenerationException;
import com.atsoptimizer.modules.jobmatch.exception.ResumeNotReadyForMatchException;
import com.atsoptimizer.modules.jobmatch.service.JobMatchService;
import com.atsoptimizer.modules.resume.domain.ResumeStatus;
import com.atsoptimizer.modules.resume.exception.ResumeNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobMatchController.class)
@Import(GlobalExceptionHandler.class)
class JobMatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JobMatchService jobMatchService;

    @Test
    @DisplayName("POST /api/v1/job-matches - Should return 201 Created when initiating Moment 1 analysis")
    void shouldReturnCreatedOnValidCreateRequest() throws Exception {
        JobMatchResponseDto response = new JobMatchResponseDto(
                10L, 1L, JobMatchStatus.PENDING_ANALYSIS, LocalDateTime.now(), LocalDateTime.now()
        );
        when(jobMatchService.createJobMatch(any(CreateJobMatchRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/job-matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateJobMatchRequest(1L, "Vaga Java Spring Boot"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.resumeId").value(1))
                .andExpect(jsonPath("$.status").value("PENDING_ANALYSIS"));
    }

    @Test
    @DisplayName("POST /api/v1/job-matches - Should return 400 Bad Request when job description is blank")
    void shouldReturnBadRequestWhenJobDescriptionIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/job-matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resumeId\":1,\"jobDescription\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/v1/job-matches - Should return 422 Unprocessable Entity when resume analysis is not COMPLETED")
    void shouldReturnUnprocessableEntityWhenResumeNotCompleted() throws Exception {
        when(jobMatchService.createJobMatch(any(CreateJobMatchRequest.class)))
                .thenThrow(new ResumeNotReadyForMatchException(1L, ResumeStatus.PROCESSING));

        mockMvc.perform(post("/api/v1/job-matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateJobMatchRequest(1L, "Vaga Java"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value(containsString("PROCESSING")));
    }

    @Test
    @DisplayName("POST /api/v1/job-matches - Should return 404 Not Found when resume does not exist")
    void shouldReturnNotFoundWhenResumeMissing() throws Exception {
        when(jobMatchService.createJobMatch(any(CreateJobMatchRequest.class)))
                .thenThrow(new ResumeNotFoundException(999L));

        mockMvc.perform(post("/api/v1/job-matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateJobMatchRequest(999L, "Vaga Java"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /api/v1/job-matches/{id} - Should return 200 OK with JobMatchDetailsDto")
    void shouldReturnJobMatchDetails() throws Exception {
        JobMatchDetailsDto details = new JobMatchDetailsDto(
                10L, 1L, JobMatchStatus.ANALYZED, null, "Vaga Java",
                null, null, null, null, null, LocalDateTime.now(), LocalDateTime.now()
        );
        when(jobMatchService.getJobMatchById(10L)).thenReturn(details);

        mockMvc.perform(get("/api/v1/job-matches/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("ANALYZED"));
    }

    @Test
    @DisplayName("GET /api/v1/job-matches/{id} - Should return 404 Not Found when job match does not exist")
    void shouldReturnNotFoundWhenJobMatchMissing() throws Exception {
        when(jobMatchService.getJobMatchById(888L)).thenThrow(new JobMatchNotFoundException(888L));

        mockMvc.perform(get("/api/v1/job-matches/888"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /api/v1/job-matches/resume/{resumeId} - Should return 200 OK with match list")
    void shouldListMatchesByResume() throws Exception {
        JobMatchResponseDto item = new JobMatchResponseDto(
                10L, 1L, JobMatchStatus.ANALYZED, LocalDateTime.now(), LocalDateTime.now()
        );
        when(jobMatchService.getJobMatchesByResumeId(1L)).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/job-matches/resume/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    @DisplayName("POST /api/v1/job-matches/{id}/generate-resume - Should return 202 Accepted for Moment 2 generation")
    void shouldReturnAcceptedOnGenerateResume() throws Exception {
        JobMatchResponseDto response = new JobMatchResponseDto(
                10L, 1L, JobMatchStatus.GENERATING_RESUME, LocalDateTime.now(), LocalDateTime.now()
        );
        when(jobMatchService.requestResumeGeneration(10L, OptimizationMode.TRUTHFUL)).thenReturn(response);

        mockMvc.perform(post("/api/v1/job-matches/10/generate-resume")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GenerateResumeRequest(OptimizationMode.TRUTHFUL))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("GENERATING_RESUME"));
    }

    @Test
    @DisplayName("POST /api/v1/job-matches/{id}/generate-resume - Should return 422 when match is not yet analyzed")
    void shouldReturnUnprocessableWhenMatchNotAnalyzedYet() throws Exception {
        when(jobMatchService.requestResumeGeneration(10L, OptimizationMode.IDEAL))
                .thenThrow(new JobMatchNotReadyForGenerationException(10L, JobMatchStatus.PARSING_JOB));

        mockMvc.perform(post("/api/v1/job-matches/10/generate-resume")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GenerateResumeRequest(OptimizationMode.IDEAL))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    @DisplayName("GET /api/v1/job-matches/{id}/resume-pdf - Should return 200 OK with application/pdf and Content-Disposition")
    void shouldDownloadResumePdf() throws Exception {
        byte[] pdfBytes = "%PDF-1.4 fake".getBytes();
        when(jobMatchService.downloadGeneratedResumePdf(10L, OptimizationMode.TRUTHFUL))
                .thenReturn(new JobMatchService.GeneratedPdfFile("Lucas_Backend_Java.pdf", pdfBytes));

        mockMvc.perform(get("/api/v1/job-matches/10/resume-pdf").param("mode", "TRUTHFUL"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("Lucas_Backend_Java.pdf")))
                .andExpect(content().bytes(pdfBytes));
    }

    @Test
    @DisplayName("GET /api/v1/job-matches/{id}/resume-pdf - Should return 404 when mode was not generated yet")
    void shouldReturnNotFoundWhenGeneratedResumeMissing() throws Exception {
        when(jobMatchService.downloadGeneratedResumePdf(10L, OptimizationMode.IDEAL))
                .thenThrow(new GeneratedResumeNotFoundException(10L, OptimizationMode.IDEAL));

        mockMvc.perform(get("/api/v1/job-matches/10/resume-pdf").param("mode", "IDEAL"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
