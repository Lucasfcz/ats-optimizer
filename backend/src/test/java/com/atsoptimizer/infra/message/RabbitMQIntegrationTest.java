package com.atsoptimizer.infra.message;

import com.atsoptimizer.infra.message.dto.ResumeAnalysisMessage;
import com.atsoptimizer.modules.resume.domain.Resume;
import com.atsoptimizer.modules.resume.domain.ResumeStatus;
import com.atsoptimizer.modules.resume.repository.ResumeRepository;
import com.atsoptimizer.modules.resume.service.ResumeAnalysisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class RabbitMQIntegrationTest {

    @Autowired
    private TopicExchange atsTopicExchange;

    @Autowired
    private Queue atsResumeAnalysisQueue;

    @Autowired
    private Binding atsResumeAnalysisBinding;

    @Autowired
    private TopicExchange atsDeadLetterExchange;

    @Autowired
    private Queue atsDeadLetterQueue;

    @Autowired
    private Binding atsDeadLetterBinding;

    @Autowired
    private MessageConverter jsonMessageConverter;

    @Autowired
    private MessageRecoverer messageRecoverer;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private ResumeAnalysisService resumeAnalysisService;

    @MockitoBean
    private ResumeRepository resumeRepository;

    @Autowired
    private ResumeEventPublisher resumeEventPublisher;

    @Autowired
    private ResumeAnalysisListener resumeAnalysisListener;

    @Autowired
    private ResumeAnalysisDlqListener resumeAnalysisDlqListener;

    @Test
    @DisplayName("Should verify Main RabbitMQ Topic Exchange, Queue and Binding topology configuration")
    void shouldVerifyRabbitMQTopologyConfiguration() {
        assertThat(atsTopicExchange).isNotNull();
        assertThat(atsTopicExchange.getName()).isEqualTo("ats.exchange.test");
        assertThat(atsTopicExchange.getType()).isEqualTo("topic");
        assertThat(atsTopicExchange.isDurable()).isTrue();

        assertThat(atsResumeAnalysisQueue).isNotNull();
        assertThat(atsResumeAnalysisQueue.getName()).isEqualTo("resume.ai.analysis.test");
        assertThat(atsResumeAnalysisQueue.isDurable()).isTrue();

        assertThat(atsResumeAnalysisBinding).isNotNull();
        assertThat(atsResumeAnalysisBinding.getExchange()).isEqualTo("ats.exchange.test");
        assertThat(atsResumeAnalysisBinding.getDestination()).isEqualTo("resume.ai.analysis.test");
        assertThat(atsResumeAnalysisBinding.getRoutingKey()).isEqualTo("resume.ai.analysis.test");
    }

    @Test
    @DisplayName("Should verify Dead Letter Exchange (DLX) and Queue (DLQ) topology configuration")
    void shouldVerifyDeadLetterTopologyConfiguration() {
        assertThat(atsDeadLetterExchange).isNotNull();
        assertThat(atsDeadLetterExchange.getName()).isEqualTo("ats.exchange.dlx.test");
        assertThat(atsDeadLetterExchange.getType()).isEqualTo("topic");
        assertThat(atsDeadLetterExchange.isDurable()).isTrue();

        assertThat(atsDeadLetterQueue).isNotNull();
        assertThat(atsDeadLetterQueue.getName()).isEqualTo("resume.ai.analysis.dlq.test");
        assertThat(atsDeadLetterQueue.isDurable()).isTrue();

        assertThat(atsDeadLetterBinding).isNotNull();
        assertThat(atsDeadLetterBinding.getExchange()).isEqualTo("ats.exchange.dlx.test");
        assertThat(atsDeadLetterBinding.getDestination()).isEqualTo("resume.ai.analysis.dlq.test");
        assertThat(atsDeadLetterBinding.getRoutingKey()).isEqualTo("resume.ai.analysis.dlq.test");

        assertThat(messageRecoverer).isInstanceOf(RepublishMessageRecoverer.class);
    }

    @Test
    @DisplayName("Should verify JSON serialization and deserialization of ResumeAnalysisMessage")
    void shouldSerializeAndDeserializeResumeAnalysisMessage() {
        Long resumeId = 42L;
        String fileName = "curriculo_senior.pdf";
        String extractedText = "Desenvolvedor Backend com vasta experiência em Java, Spring Boot e RabbitMQ.";

        ResumeAnalysisMessage originalMessage = new ResumeAnalysisMessage(resumeId, fileName, extractedText);

        // Convert domain object into AMQP wire message (JSON serialization)
        Message amqpMessage = jsonMessageConverter.toMessage(originalMessage, null);
        assertThat(amqpMessage).isNotNull();
        assertThat(amqpMessage.getMessageProperties().getContentType()).contains("application/json");

        // Convert back from AMQP wire message (JSON deserialization)
        Object deserialized = jsonMessageConverter.fromMessage(amqpMessage);
        assertThat(deserialized).isInstanceOf(ResumeAnalysisMessage.class);

        ResumeAnalysisMessage recoveredMessage = (ResumeAnalysisMessage) deserialized;
        assertThat(recoveredMessage.resumeId()).isEqualTo(resumeId);
        assertThat(recoveredMessage.fileName()).isEqualTo(fileName);
        assertThat(recoveredMessage.extractedText()).isEqualTo(extractedText);
    }

    @Test
    @DisplayName("Should verify publishing message with RabbitTemplate through ResumeEventPublisher")
    void shouldPublishMessageThroughEventPublisher() {
        ResumeAnalysisMessage message = new ResumeAnalysisMessage(
                100L,
                "curriculo_test.pdf",
                "Texto de teste para publicação"
        );

        resumeEventPublisher.publishResumeForAnalysis(message);

        ArgumentCaptor<ResumeAnalysisMessage> captor = ArgumentCaptor.forClass(ResumeAnalysisMessage.class);
        verify(rabbitTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("ats.exchange.test"),
                org.mockito.ArgumentMatchers.eq("resume.ai.analysis.test"),
                captor.capture()
        );

        ResumeAnalysisMessage sent = captor.getValue();
        assertThat(sent.resumeId()).isEqualTo(100L);
        assertThat(sent.fileName()).isEqualTo("curriculo_test.pdf");
        assertThat(sent.extractedText()).isEqualTo("Texto de teste para publicação");
    }

    @Test
    @DisplayName("Should verify message consumption and delegation in ResumeAnalysisListener")
    void shouldConsumeMessageAndDelegateToAnalysisService() {
        ResumeAnalysisMessage incomingMessage = new ResumeAnalysisMessage(
                200L,
                "curriculo_consumo.pdf",
                "Texto consumido da fila RabbitMQ"
        );

        resumeAnalysisListener.handleResumeAnalysis(incomingMessage);

        verify(resumeAnalysisService).processAnalysis(200L, "Texto consumido da fila RabbitMQ");
    }

    @Test
    @DisplayName("Should update resume status to FAILED in database when message reaches Dead Letter Queue")
    void shouldMarkResumeAsFailedWhenDlqMessageIsReceived() {
        Long resumeId = 300L;
        Resume resume = new Resume(resumeId, "curriculo_falha.pdf", "application/pdf", 2048L, "Texto com erro");
        when(resumeRepository.findById(resumeId)).thenReturn(Optional.of(resume));

        ResumeAnalysisMessage dlqMessage = new ResumeAnalysisMessage(
                resumeId,
                "curriculo_falha.pdf",
                "Texto com erro"
        );

        resumeAnalysisDlqListener.handleDeadLetterMessage(dlqMessage, "OpenAI Rate Limit Exceeded");

        assertThat(resume.getStatus()).isEqualTo(ResumeStatus.FAILED);
        assertThat(resume.getErrorMessage()).isEqualTo("OpenAI Rate Limit Exceeded");
        verify(resumeRepository).save(resume);
    }
}
