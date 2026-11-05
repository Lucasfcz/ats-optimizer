package com.atsoptimizer.resume;

import com.atsoptimizer.parser.DocumentTextExtractor;
import com.atsoptimizer.queue.ResumeEventPublisher;
import com.atsoptimizer.queue.dto.ResumeAnalysisMessage;
import com.atsoptimizer.resume.domain.Resume;
import com.atsoptimizer.resume.domain.ResumeStatus;
import com.atsoptimizer.resume.dto.ResumeResponseDto;
import com.atsoptimizer.resume.repository.ResumeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeServiceTest {

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private DocumentTextExtractor documentTextExtractor;

    @Mock
    private ResumeEventPublisher resumeEventPublisher;

    @InjectMocks
    private ResumeService resumeService;

    @Test
    @DisplayName("Should successfully upload file, extract text, persist resume and publish RabbitMQ event")
    void shouldUploadAndProcessSuccessfully() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "curriculo.pdf",
                "application/pdf",
                "Conteudo em bytes".getBytes()
        );

        String extractedText = "Texto completo extraído do currículo por Tika";
        when(documentTextExtractor.extractText(any(InputStream.class), eq("curriculo.pdf")))
                .thenReturn(extractedText);

        Long expectedId = 1L;
        Resume savedResume = new Resume(expectedId, "curriculo.pdf", "application/pdf", file.getSize(), extractedText);
        when(resumeRepository.save(any(Resume.class))).thenReturn(savedResume);

        ResumeResponseDto response = resumeService.uploadAndProcess(file);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(expectedId);
        assertThat(response.fileName()).isEqualTo("curriculo.pdf");
        assertThat(response.status()).isEqualTo(ResumeStatus.UPLOADED);

        // Verify message was published to RabbitMQ
        ArgumentCaptor<ResumeAnalysisMessage> messageCaptor = ArgumentCaptor.forClass(ResumeAnalysisMessage.class);
        verify(resumeEventPublisher).publishResumeForAnalysis(messageCaptor.capture());

        ResumeAnalysisMessage publishedMessage = messageCaptor.getValue();
        assertThat(publishedMessage.resumeId()).isEqualTo(expectedId);
        assertThat(publishedMessage.extractedText()).isEqualTo(extractedText);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when uploaded file is empty or null")
    void shouldThrowExceptionWhenFileIsEmpty() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "vazio.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> resumeService.uploadAndProcess(emptyFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Uploaded resume file cannot be empty");
    }
}
