package com.atsoptimizer.queue;

import com.atsoptimizer.queue.dto.ResumeAnalysisMessage;
import com.atsoptimizer.resume.ResumeAnalysisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

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
    private MessageConverter jsonMessageConverter;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private ResumeAnalysisService resumeAnalysisService;

    @Autowired
    private ResumeEventPublisher resumeEventPublisher;

    @Autowired
    private ResumeAnalysisListener resumeAnalysisListener;

    @Test
    @DisplayName("Should verify RabbitMQ Topic Exchange, Queue and Binding topology configuration")
    void shouldVerifyRabbitMQTopologyConfiguration() {
        assertThat(atsTopicExchange).isNotNull();
        assertThat(atsTopicExchange.getName()).isEqualTo("ats.exchange.test");
        assertThat(atsTopicExchange.getType()).isEqualTo("topic");
        assertThat(atsTopicExchange.isDurable()).isTrue();

        assertThat(atsResumeAnalysisQueue).isNotNull();
        assertThat(atsResumeAnalysisQueue.getName()).isEqualTo("ats.resume.analysis.queue.test");
        assertThat(atsResumeAnalysisQueue.isDurable()).isTrue();

        assertThat(atsResumeAnalysisBinding).isNotNull();
        assertThat(atsResumeAnalysisBinding.getExchange()).isEqualTo("ats.exchange.test");
        assertThat(atsResumeAnalysisBinding.getDestination()).isEqualTo("ats.resume.analysis.queue.test");
        assertThat(atsResumeAnalysisBinding.getRoutingKey()).isEqualTo("resume.analyze.test");
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
                org.mockito.ArgumentMatchers.eq("resume.analyze.test"),
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
}
