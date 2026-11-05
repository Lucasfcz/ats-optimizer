package com.atsoptimizer.modules.resume.controller;

import com.atsoptimizer.modules.resume.dto.ResumeDetailsDto;
import com.atsoptimizer.modules.resume.dto.ResumeResponseDto;
import com.atsoptimizer.modules.resume.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@Tag(name = "Resumes", description = "Endpoints para upload, consulta e processamento de currículos")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Realiza upload de currículo para análise ATS", description = "Recebe o arquivo (PDF, DOCX, TXT), extrai o texto de forma síncrona com Apache Tika e enfileira para auditoria de IA")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Currículo enviado e enfileirado com sucesso", content = @Content(schema = @Schema(implementation = ResumeResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Arquivo inválido, vazio ou não suportado", content = @Content),
            @ApiResponse(responseCode = "413", description = "Arquivo excede o limite máximo permitido de 15MB", content = @Content)
    })
    public ResponseEntity<ResumeResponseDto> uploadResume(
            @Parameter(description = "Arquivo do currículo (PDF, DOCX, etc.)", required = true)
            @RequestParam("file") MultipartFile file
    ) {
        ResumeResponseDto response = resumeService.uploadAndProcess(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtém detalhes e diagnóstico ATS completo de um currículo", description = "Retorna metadados, status do processamento, texto bruto extraído e análise detalhada da IA")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Currículo localizado com sucesso", content = @Content(schema = @Schema(implementation = ResumeDetailsDto.class))),
            @ApiResponse(responseCode = "404", description = "Currículo não encontrado para o ID informado", content = @Content)
    })
    public ResponseEntity<ResumeDetailsDto> getResumeById(
            @Parameter(description = "ID do currículo", required = true)
            @PathVariable Long id
    ) {
        ResumeDetailsDto details = resumeService.getResumeById(id);
        return ResponseEntity.ok(details);
    }

    @GetMapping
    @Operation(summary = "Lista todos os currículos cadastrados", description = "Retorna um resumo de todos os currículos processados ou em fila")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    public ResponseEntity<List<ResumeResponseDto>> getAllResumes() {
        return ResponseEntity.ok(resumeService.getAllResumes());
    }
}
