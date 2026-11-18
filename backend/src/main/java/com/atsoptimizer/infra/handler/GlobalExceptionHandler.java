package com.atsoptimizer.infra.handler;

import com.atsoptimizer.infra.ai.exception.AiProcessingException;
import com.atsoptimizer.infra.extraction.exception.DocumentExtractionException;
import com.atsoptimizer.infra.handler.dto.ErrorResponse;
import com.atsoptimizer.infra.message.exception.MessagePublishingException;
import com.atsoptimizer.modules.jobmatch.exception.EmptyJobDescriptionException;
import com.atsoptimizer.modules.jobmatch.exception.GeneratedResumeNotFoundException;
import com.atsoptimizer.modules.jobmatch.exception.JobMatchDomainException;
import com.atsoptimizer.modules.jobmatch.exception.JobMatchNotFoundException;
import com.atsoptimizer.modules.jobmatch.exception.JobMatchNotReadyForGenerationException;
import com.atsoptimizer.modules.jobmatch.exception.PdfGenerationException;
import com.atsoptimizer.modules.jobmatch.exception.ResumeNotReadyForMatchException;
import com.atsoptimizer.modules.resume.exception.EmptyResumeFileException;
import com.atsoptimizer.modules.resume.exception.ResumeDomainException;
import com.atsoptimizer.modules.resume.exception.ResumeFileReadingException;
import com.atsoptimizer.modules.resume.exception.ResumeNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResumeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResumeNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(EmptyResumeFileException.class)
    public ResponseEntity<ErrorResponse> handleEmptyFile(EmptyResumeFileException ex, HttpServletRequest request) {
        log.warn("Empty file upload rejected: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
    }

    @ExceptionHandler(ResumeFileReadingException.class)
    public ResponseEntity<ErrorResponse> handleFileReading(ResumeFileReadingException ex, HttpServletRequest request) {
        log.warn("File reading failure: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
    }

    @ExceptionHandler(DocumentExtractionException.class)
    public ResponseEntity<ErrorResponse> handleExtractionException(DocumentExtractionException ex, HttpServletRequest request) {
        log.warn("Document extraction error: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Document Extraction Error", ex.getMessage(), request);
    }

    @ExceptionHandler(ResumeDomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(ResumeDomainException ex, HttpServletRequest request) {
        log.warn("Resume domain rule violation: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable Entity", ex.getMessage(), request);
    }

    @ExceptionHandler(AiProcessingException.class)
    public ResponseEntity<ErrorResponse> handleAiProcessingException(AiProcessingException ex, HttpServletRequest request) {
        log.error("AI service failure: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.BAD_GATEWAY, "AI Service Error", ex.getMessage(), request);
    }

    @ExceptionHandler({JobMatchNotFoundException.class, GeneratedResumeNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleJobMatchNotFound(RuntimeException ex, HttpServletRequest request) {
        log.warn("Job match resource not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(EmptyJobDescriptionException.class)
    public ResponseEntity<ErrorResponse> handleEmptyJobDescription(EmptyJobDescriptionException ex, HttpServletRequest request) {
        log.warn("Empty job description rejected: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
    }

    @ExceptionHandler({
            ResumeNotReadyForMatchException.class,
            JobMatchNotReadyForGenerationException.class,
            JobMatchDomainException.class
    })
    public ResponseEntity<ErrorResponse> handleJobMatchDomainExceptions(RuntimeException ex, HttpServletRequest request) {
        log.warn("Job match domain rule violation: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable Entity", ex.getMessage(), request);
    }

    @ExceptionHandler(PdfGenerationException.class)
    public ResponseEntity<ErrorResponse> handlePdfGenerationException(PdfGenerationException ex, HttpServletRequest request) {
        log.error("PDF generation failure: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "PDF Generation Error", ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Request validation failed: {}", details);
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", details, request);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleBadRequestPayload(Exception ex, HttpServletRequest request) {
        log.warn("Malformed request payload or parameter: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", "Invalid request format or enum value", request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxSize(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.warn("File upload limit exceeded: {}", ex.getMessage());
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE, "Payload Too Large", "Uploaded file exceeds the maximum allowed limit (15MB)", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Validation error: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
    }

    @ExceptionHandler(MessagePublishingException.class)
    public ResponseEntity<ErrorResponse> handleMessagePublishingException(MessagePublishingException ex, HttpServletRequest request) {
        log.error("Failed to enqueue message to RabbitMQ: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Message Queue Error", "Unable to queue resume for analysis. Please try again later.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled internal server error", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", ex.getMessage(), request);
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String error, String message, HttpServletRequest request) {
        String path = request != null ? request.getRequestURI() : "";
        ErrorResponse response = new ErrorResponse(status.value(), error, message, path);
        return ResponseEntity.status(status).body(response);
    }
}
