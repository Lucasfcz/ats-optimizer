package com.atsoptimizer.modules.resume;

import com.atsoptimizer.infra.extraction.exception.DocumentExtractionException;
import com.atsoptimizer.infra.handler.GlobalExceptionHandler;
import com.atsoptimizer.modules.resume.controller.ResumeController;
import com.atsoptimizer.modules.resume.domain.ResumeStatus;
import com.atsoptimizer.modules.resume.dto.ResumeDetailsDto;
import com.atsoptimizer.modules.resume.dto.ResumeResponseDto;
import com.atsoptimizer.modules.resume.exception.EmptyResumeFileException;
import com.atsoptimizer.modules.resume.exception.ResumeDomainException;
import com.atsoptimizer.modules.resume.exception.ResumeNotFoundException;
import com.atsoptimizer.modules.resume.service.ResumeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResumeController.class)
@Import(GlobalExceptionHandler.class)
class ResumeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResumeService resumeService;

    @Test
    @DisplayName("POST /api/v1/resumes/upload - Should return 201 Created with ResumeResponseDto on valid upload")
    void shouldReturnCreatedWhenUploadingValidFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "curriculo.pdf",
                "application/pdf",
                "conteudo do curriculo".getBytes()
        );

        ResumeResponseDto responseDto = new ResumeResponseDto(
                1L,
                "curriculo.pdf",
                "application/pdf",
                21L,
                ResumeStatus.UPLOADED,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(resumeService.uploadAndProcess(any(MultipartFile.class))).thenReturn(responseDto);

        mockMvc.perform(multipart("/api/v1/resumes/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fileName").value("curriculo.pdf"))
                .andExpect(jsonPath("$.status").value("UPLOADED"));
    }

    @Test
    @DisplayName("POST /api/v1/resumes/upload - Should return 400 Bad Request with ErrorResponse when file is empty")
    void shouldReturnBadRequestWhenFileIsEmpty() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "vazio.pdf",
                "application/pdf",
                new byte[0]
        );

        when(resumeService.uploadAndProcess(any(MultipartFile.class)))
                .thenThrow(new EmptyResumeFileException());

        mockMvc.perform(multipart("/api/v1/resumes/upload").file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Uploaded resume file cannot be empty"))
                .andExpect(jsonPath("$.path").value("/api/v1/resumes/upload"));
    }

    @Test
    @DisplayName("POST /api/v1/resumes/upload - Should return 400 Bad Request with ErrorResponse when Apache Tika extraction fails")
    void shouldReturnBadRequestWhenTikaExtractionFails() throws Exception {
        MockMultipartFile corruptedFile = new MockMultipartFile(
                "file",
                "corrompido.pdf",
                "application/pdf",
                "dados corrompidos".getBytes()
        );

        when(resumeService.uploadAndProcess(any(MultipartFile.class)))
                .thenThrow(new DocumentExtractionException("Failed to extract text from document: corrompido.pdf"));

        mockMvc.perform(multipart("/api/v1/resumes/upload").file(corruptedFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Document Extraction Error"))
                .andExpect(jsonPath("$.message").value(containsString("corrompido.pdf")))
                .andExpect(jsonPath("$.path").value("/api/v1/resumes/upload"));
    }

    @Test
    @DisplayName("GET /api/v1/resumes/{id} - Should return 200 OK with ResumeDetailsDto when resume exists")
    void shouldReturnOkWhenGettingExistingResumeById() throws Exception {
        ResumeDetailsDto detailsDto = new ResumeDetailsDto(
                1L,
                "curriculo.pdf",
                "application/pdf",
                1024L,
                ResumeStatus.COMPLETED,
                null,
                "Texto do curriculo",
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(resumeService.getResumeById(1L)).thenReturn(detailsDto);

        mockMvc.perform(get("/api/v1/resumes/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fileName").value("curriculo.pdf"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.extractedText").value("Texto do curriculo"));
    }

    @Test
    @DisplayName("GET /api/v1/resumes/{id} - Should return 404 Not Found with ErrorResponse when resume does not exist")
    void shouldReturnNotFoundWhenResumeDoesNotExist() throws Exception {
        when(resumeService.getResumeById(999L))
                .thenThrow(new ResumeNotFoundException(999L));

        mockMvc.perform(get("/api/v1/resumes/999").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Resume not found with id: 999"))
                .andExpect(jsonPath("$.path").value("/api/v1/resumes/999"));
    }

    @Test
    @DisplayName("GET /api/v1/resumes - Should return 200 OK with list of resumes")
    void shouldReturnOkWhenListingAllResumes() throws Exception {
        ResumeResponseDto r1 = new ResumeResponseDto(1L, "cv1.pdf", "application/pdf", 100L, ResumeStatus.COMPLETED, LocalDateTime.now(), LocalDateTime.now());
        ResumeResponseDto r2 = new ResumeResponseDto(2L, "cv2.pdf", "application/pdf", 200L, ResumeStatus.UPLOADED, LocalDateTime.now(), LocalDateTime.now());

        when(resumeService.getAllResumes()).thenReturn(List.of(r1, r2));

        mockMvc.perform(get("/api/v1/resumes").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
    }
}
