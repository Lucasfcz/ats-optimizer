package com.atsoptimizer.infra.queue;

import com.atsoptimizer.infra.queue.dto.ResumeAnalysisMessage;
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

    @Value("${ats.rabbitmq.routing-key:resume.analyze}")
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
            throw new RuntimeException("Could not publish resume analysis message to queue", e);
        }
    }
}
