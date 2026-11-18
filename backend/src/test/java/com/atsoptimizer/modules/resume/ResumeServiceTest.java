package com.atsoptimizer.modules.resume;

import com.atsoptimizer.infra.extraction.DocumentTextExtractor;
import com.atsoptimizer.infra.extraction.exception.DocumentExtractionException;
import com.atsoptimizer.infra.message.ResumeEventPublisher;
import com.atsoptimizer.infra.message.dto.ResumeAnalysisMessage;
import com.atsoptimizer.modules.jobmatch.repository.JobMatchRepository;
import com.atsoptimizer.modules.resume.domain.Resume;
import com.atsoptimizer.modules.resume.domain.ResumeStatus;
import com.atsoptimizer.modules.resume.dto.ResumeResponseDto;
import com.atsoptimizer.modules.resume.exception.EmptyResumeFileException;
import com.atsoptimizer.modules.resume.exception.ResumeNotFoundException;
import com.atsoptimizer.modules.resume.repository.ResumeRepository;
import com.atsoptimizer.modules.resume.service.ResumeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeServiceTest {

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private JobMatchRepository jobMatchRepository;

    @Mock
    private DocumentTextExtractor documentTextExtractor;

    @Mock
    private ResumeEventPublisher resumeEventPublisher;

    @InjectMocks
    private ResumeService resumeService;

    @Test
    @DisplayName("Should delete previous job matches and resumes, persist new resume, and publish RabbitMQ message")
    void shouldUploadAndProcessResumeSuccessfully() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "curriculo.pdf",
                "application/pdf",
                "Conteudo do curriculo".getBytes()
        );

        when(documentTextExtractor.extractText(any(InputStream.class), eq("curriculo.pdf")))
                .thenReturn("Texto limpo extraido do curriculo");

        Resume savedResume = new Resume(
                1L,
                "curriculo.pdf",
                "application/pdf",
                (long) "Conteudo do curriculo".length(),
                "Texto limpo extraido do curriculo"
        );

        when(resumeRepository.save(any(Resume.class))).thenReturn(savedResume);

        ResumeResponseDto response = resumeService.uploadAndProcess(file);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.fileName()).isEqualTo("curriculo.pdf");
        assertThat(response.status()).isEqualTo(ResumeStatus.UPLOADED);

        InOrder order = inOrder(documentTextExtractor, jobMatchRepository, resumeRepository, resumeEventPublisher);
        order.verify(documentTextExtractor).extractText(any(InputStream.class), eq("curriculo.pdf"));
        order.verify(jobMatchRepository).deleteAllInBatch();
        order.verify(resumeRepository).deleteAllInBatch();
        order.verify(resumeRepository).save(any(Resume.class));

        ArgumentCaptor<ResumeAnalysisMessage> messageCaptor = ArgumentCaptor.forClass(ResumeAnalysisMessage.class);
        order.verify(resumeEventPublisher).publishResumeForAnalysis(messageCaptor.capture());
        ResumeAnalysisMessage publishedMessage = messageCaptor.getValue();
        assertThat(publishedMessage.resumeId()).isEqualTo(1L);
        assertThat(publishedMessage.fileName()).isEqualTo("curriculo.pdf");
        assertThat(publishedMessage.extractedText()).isEqualTo("Texto limpo extraido do curriculo");
    }

    @Test
    @DisplayName("Should preserve existing resumes when text extraction fails on corrupted upload")
    void shouldNotDeletePreviousResumesWhenExtractionFails() {
        MockMultipartFile corruptedFile = new MockMultipartFile(
                "file",
                "corrompido.pdf",
                "application/pdf",
                "dados corrompidos".getBytes()
        );

        when(documentTextExtractor.extractText(any(InputStream.class), eq("corrompido.pdf")))
                .thenThrow(new DocumentExtractionException("Corrupted PDF"));

        assertThatThrownBy(() -> resumeService.uploadAndProcess(corruptedFile))
                .isInstanceOf(DocumentExtractionException.class);

        verifyNoInteractions(jobMatchRepository);
        verify(resumeRepository, never()).deleteAllInBatch();
        verify(resumeRepository, never()).save(any());
        verifyNoInteractions(resumeEventPublisher);
    }

    @Test
    @DisplayName("Should throw EmptyResumeFileException when uploading an empty file")
    void shouldThrowExceptionWhenFileIsEmpty() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "vazio.pdf",
                "application/pdf",
                new byte[0]
        );

        assertThatThrownBy(() -> resumeService.uploadAndProcess(emptyFile))
                .isInstanceOf(EmptyResumeFileException.class)
                .hasMessage("Uploaded resume file cannot be empty");

        verifyNoInteractions(jobMatchRepository);
        verifyNoInteractions(resumeRepository);
    }

    @Test
    @DisplayName("Should throw ResumeNotFoundException when resume ID does not exist")
    void shouldThrowExceptionWhenResumeNotFound() {
        when(resumeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resumeService.getResumeById(999L))
                .isInstanceOf(ResumeNotFoundException.class)
                .hasMessage("Resume not found with id: 999");
    }
}
