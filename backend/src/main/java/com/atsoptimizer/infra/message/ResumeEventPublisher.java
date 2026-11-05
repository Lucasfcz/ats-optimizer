package com.atsoptimizer.infra.message;

import com.atsoptimizer.infra.message.dto.ResumeAnalysisMessage;
import com.atsoptimizer.infra.message.exception.MessagePublishingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ResumeEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ResumeEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${ats.rabbitmq.exchange:ats.exchange}")
    private String exchange;

    @Value("${ats.rabbitmq.routing-key:resume.ai.analysis}")
    private String routingKey;

    public ResumeEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishResumeForAnalysis(ResumeAnalysisMessage message) {
        log.info("Publishing resume [{}] to topic exchange [{}] with routing key [{}]",
                message.resumeId(), exchange, routingKey);
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, message);
            log.info("Successfully published resume [{}] to RabbitMQ", message.resumeId());
        } catch (Exception e) {
            log.error("Failed to publish resume [{}] to RabbitMQ: {}", message.resumeId(), e.getMessage(), e);
            throw new MessagePublishingException("Could not publish resume analysis message to queue", e);
        }
    }
}
