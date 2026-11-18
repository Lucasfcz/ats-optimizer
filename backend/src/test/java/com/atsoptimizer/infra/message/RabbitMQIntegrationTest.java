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
    private Queue jobMatchParseQueue;

    @Autowired
    private Binding jobMatchParseBinding;

    @Autowired
    private Queue jobMatchAuditQueue;

    @Autowired
    private Binding jobMatchAuditBinding;

    @Autowired
    private Queue jobMatchGenerateQueue;

    @Autowired
    private Binding jobMatchGenerateBinding;

    @Autowired
    private Queue jobMatchParseDlqQueue;

    @Autowired
    private Queue jobMatchAuditDlqQueue;

    @Autowired
    private Queue jobMatchGenerateDlqQueue;

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
    @DisplayName("Should verify Main RabbitMQ Topic Exchange, Queues and exact Bindings topology configuration")
    void shouldVerifyRabbitMQTopologyConfiguration() {
        assertThat(atsTopicExchange).isNotNull();
        assertThat(atsTopicExchange.getName()).isEqualTo(RabbitMQConfig.EXCHANGE);
        assertThat(atsTopicExchange.getType()).isEqualTo("topic");
        assertThat(atsTopicExchange.isDurable()).isTrue();

        assertThat(atsResumeAnalysisQueue.getName()).isEqualTo(RabbitMQConfig.RESUME_ANALYSIS_QUEUE);
        assertThat(atsResumeAnalysisQueue.isDurable()).isTrue();
        assertThat(atsResumeAnalysisBinding.getExchange()).isEqualTo(RabbitMQConfig.EXCHANGE);
        assertThat(atsResumeAnalysisBinding.getDestination()).isEqualTo(RabbitMQConfig.RESUME_ANALYSIS_QUEUE);
        assertThat(atsResumeAnalysisBinding.getRoutingKey()).isEqualTo(RabbitMQConfig.RESUME_ANALYSIS_QUEUE);

        assertThat(jobMatchParseQueue.getName()).isEqualTo(RabbitMQConfig.JOB_MATCH_PARSE_QUEUE);
        assertThat(jobMatchParseBinding.getRoutingKey()).isEqualTo(RabbitMQConfig.JOB_MATCH_PARSE_QUEUE);

        assertThat(jobMatchAuditQueue.getName()).isEqualTo(RabbitMQConfig.JOB_MATCH_AUDIT_QUEUE);
        assertThat(jobMatchAuditBinding.getRoutingKey()).isEqualTo(RabbitMQConfig.JOB_MATCH_AUDIT_QUEUE);

        assertThat(jobMatchGenerateQueue.getName()).isEqualTo(RabbitMQConfig.JOB_MATCH_GENERATE_QUEUE);
        assertThat(jobMatchGenerateBinding.getRoutingKey()).isEqualTo(RabbitMQConfig.JOB_MATCH_GENERATE_QUEUE);
    }

    @Test
    @DisplayName("Should verify Dead Letter Exchange (DLX), DLQs and dynamic MessageRecoverer topology")
    void shouldVerifyDeadLetterTopologyConfiguration() {
        assertThat(atsDeadLetterExchange).isNotNull();
        assertThat(atsDeadLetterExchange.getName()).isEqualTo(RabbitMQConfig.DLX_EXCHANGE);
        assertThat(atsDeadLetterExchange.getType()).isEqualTo("topic");
        assertThat(atsDeadLetterExchange.isDurable()).isTrue();

        assertThat(atsDeadLetterQueue.getName()).isEqualTo(RabbitMQConfig.RESUME_ANALYSIS_DLQ);
        assertThat(atsDeadLetterQueue.isDurable()).isTrue();
        assertThat(atsDeadLetterBinding.getExchange()).isEqualTo(RabbitMQConfig.DLX_EXCHANGE);
        assertThat(atsDeadLetterBinding.getDestination()).isEqualTo(RabbitMQConfig.RESUME_ANALYSIS_DLQ);
        assertThat(atsDeadLetterBinding.getRoutingKey()).isEqualTo(RabbitMQConfig.RESUME_ANALYSIS_DLQ);

        assertThat(jobMatchParseDlqQueue.getName()).isEqualTo(RabbitMQConfig.JOB_MATCH_PARSE_DLQ);
        assertThat(jobMatchAuditDlqQueue.getName()).isEqualTo(RabbitMQConfig.JOB_MATCH_AUDIT_DLQ);
        assertThat(jobMatchGenerateDlqQueue.getName()).isEqualTo(RabbitMQConfig.JOB_MATCH_GENERATE_DLQ);

        assertThat(messageRecoverer).isInstanceOf(RepublishMessageRecoverer.class);

        org.springframework.amqp.core.MessageProperties props = new org.springframework.amqp.core.MessageProperties();
        props.setReceivedRoutingKey(RabbitMQConfig.JOB_MATCH_AUDIT_QUEUE);
        Message failedMessage = new Message("{}".getBytes(), props);

        messageRecoverer.recover(failedMessage, new RuntimeException("AI failure"));

        verify(rabbitTemplate).send(
                org.mockito.ArgumentMatchers.eq(RabbitMQConfig.DLX_EXCHANGE),
                org.mockito.ArgumentMatchers.eq(RabbitMQConfig.JOB_MATCH_AUDIT_DLQ),
                org.mockito.ArgumentMatchers.any(Message.class)
        );
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
                org.mockito.ArgumentMatchers.eq(RabbitMQConfig.EXCHANGE),
                org.mockito.ArgumentMatchers.eq(RabbitMQConfig.RESUME_ANALYSIS_QUEUE),
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
