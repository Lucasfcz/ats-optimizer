package com.atsoptimizer.modules.jobmatch.controller;

import com.atsoptimizer.modules.jobmatch.domain.OptimizationMode;
import com.atsoptimizer.modules.jobmatch.dto.CreateJobMatchRequest;
import com.atsoptimizer.modules.jobmatch.dto.GenerateResumeRequest;
import com.atsoptimizer.modules.jobmatch.dto.JobMatchDetailsDto;
import com.atsoptimizer.modules.jobmatch.dto.JobMatchResponseDto;
import com.atsoptimizer.modules.jobmatch.service.JobMatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/job-matches")
@Tag(name = "Job Matches", description = "Endpoints para análise de compatibilidade de currículos com vagas e geração de PDF otimizado (1 página)")
public class JobMatchController {

    private final JobMatchService jobMatchService;

    public JobMatchController(JobMatchService jobMatchService) {
        this.jobMatchService = jobMatchService;
    }

    @PostMapping
    @Operation(summary = "Inicia análise ATS de compatibilidade entre um currículo e uma vaga (Momento 1)",
            description = "Extrai requisitos da vaga, executa matching determinístico de palavras-chave e realiza auditoria semântica sem gerar o arquivo PDF ainda.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Análise de vaga enfileirada com sucesso", content = @Content(schema = @Schema(implementation = JobMatchResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou descrição vazia", content = @Content),
            @ApiResponse(responseCode = "404", description = "Currículo não encontrado", content = @Content),
            @ApiResponse(responseCode = "422", description = "Currículo ainda não concluiu a análise inicial", content = @Content)
    })
    public ResponseEntity<JobMatchResponseDto> createJobMatch(@Valid @RequestBody CreateJobMatchRequest request) {
        JobMatchResponseDto response = jobMatchService.createJobMatch(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta o status e relatório completo do match com a vaga")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Match localizado com sucesso", content = @Content(schema = @Schema(implementation = JobMatchDetailsDto.class))),
            @ApiResponse(responseCode = "404", description = "Job match não encontrado", content = @Content)
    })
    public ResponseEntity<JobMatchDetailsDto> getJobMatchById(
            @Parameter(description = "ID da análise de vaga", required = true)
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(jobMatchService.getJobMatchById(id));
    }

    @GetMapping("/resume/{resumeId}")
    @Operation(summary = "Lista todas as análises de vagas vinculadas a um currículo")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    public ResponseEntity<List<JobMatchResponseDto>> getJobMatchesByResumeId(
            @Parameter(description = "ID do currículo", required = true)
            @PathVariable Long resumeId
    ) {
        return ResponseEntity.ok(jobMatchService.getJobMatchesByResumeId(resumeId));
    }

    @PostMapping("/{id}/generate-resume")
    @Operation(summary = "Solicita geração dos textos estruturados do currículo para a vaga (Momento 2)",
            description = "Aciona a IA no modo selecionado (TRUTHFUL ou IDEAL) para entregar os dados estruturados otimizados para 1 página.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Geração do currículo enfileirada com sucesso", content = @Content(schema = @Schema(implementation = JobMatchResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Modo de otimização inválido ou ausente", content = @Content),
            @ApiResponse(responseCode = "404", description = "Job match não encontrado", content = @Content),
            @ApiResponse(responseCode = "422", description = "Análise inicial da vaga ainda não foi concluída", content = @Content)
    })
    public ResponseEntity<JobMatchResponseDto> generateResume(
            @PathVariable Long id,
            @Valid @RequestBody GenerateResumeRequest request
    ) {
        JobMatchResponseDto response = jobMatchService.requestResumeGeneration(id, request.mode());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{id}/resume-pdf")
    @Operation(summary = "Gera e baixa o currículo em PDF (1 página) seguindo o template ATS")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "PDF gerado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Job match ou versão gerada não encontrada para o modo informado", content = @Content)
    })
    public ResponseEntity<byte[]> downloadResumePdf(
            @PathVariable Long id,
            @Parameter(description = "Modo de otimização desejado (TRUTHFUL ou IDEAL)", required = true)
            @RequestParam OptimizationMode mode
    ) {
        JobMatchService.GeneratedPdfFile pdfFile = jobMatchService.downloadGeneratedResumePdf(id, mode);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(pdfFile.fileName()).build());

        return new ResponseEntity<>(pdfFile.content(), headers, HttpStatus.OK);
    }
}
