package com.atsoptimizer.resume;

import com.atsoptimizer.resume.dto.ResumeDetailsDto;
import com.atsoptimizer.resume.dto.ResumeResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/resumes")
@Tag(name = "Resumes", description = "Endpoints para upload, extração e análise de currículos com IA para sistemas ATS")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload de currículo",
            description = "Recebe o arquivo (PDF, DOCX, TXT), extrai o texto de forma síncrona com Apache Tika, salva no banco e enfileira para análise de ATS via RabbitMQ."
    )
    @ApiResponse(responseCode = "201", description = "Currículo enviado e enfileirado com sucesso",
            content = @Content(schema = @Schema(implementation = ResumeResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Arquivo inválido ou erro de extração de texto")
    public ResponseEntity<ResumeResponseDto> uploadResume(
            @Parameter(description = "Arquivo do currículo em formato PDF, DOCX, DOC ou TXT", required = true)
            @RequestParam("file") MultipartFile file
    ) {
        ResumeResponseDto response = resumeService.uploadAndProcess(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Consultar status e diagnóstico ATS do currículo",
            description = "Retorna os detalhes completos do currículo, incluindo o texto extraído e a avaliação detalhada da IA caso concluída."
    )
    @ApiResponse(responseCode = "200", description = "Detalhes do currículo encontrados",
            content = @Content(schema = @Schema(implementation = ResumeDetailsDto.class)))
    @ApiResponse(responseCode = "404", description = "Currículo não encontrado")
    public ResponseEntity<ResumeDetailsDto> getResumeById(
            @Parameter(description = "Identificador único do currículo (ID numérico Long)", required = true)
            @PathVariable Long id
    ) {
        ResumeDetailsDto details = resumeService.getResumeById(id);
        return ResponseEntity.ok(details);
    }

    @GetMapping
    @Operation(
            summary = "Listar todos os currículos",
            description = "Retorna a listagem simplificada de todos os currículos enviados e seus respectivos status."
    )
    public ResponseEntity<List<ResumeResponseDto>> getAllResumes() {
        return ResponseEntity.ok(resumeService.getAllResumes());
    }
}
