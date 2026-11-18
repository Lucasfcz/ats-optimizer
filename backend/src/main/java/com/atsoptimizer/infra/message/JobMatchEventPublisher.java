package com.atsoptimizer.infra.message;

import com.atsoptimizer.infra.message.dto.JobMatchGenerationMessage;
import com.atsoptimizer.infra.message.dto.JobMatchMessage;
import com.atsoptimizer.infra.message.exception.MessagePublishingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class JobMatchEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(JobMatchEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public JobMatchEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishForJobParsing(JobMatchMessage message) {
        sendToQueue(RabbitMQConfig.JOB_MATCH_PARSE_QUEUE, message.jobMatchId(), message);
    }

    public void publishForFitAudit(JobMatchMessage message) {
        sendToQueue(RabbitMQConfig.JOB_MATCH_AUDIT_QUEUE, message.jobMatchId(), message);
    }

    public void publishForResumeGeneration(JobMatchGenerationMessage message) {
        sendToQueue(RabbitMQConfig.JOB_MATCH_GENERATE_QUEUE, message.jobMatchId(), message);
    }

    private void sendToQueue(String routingKey, Long jobMatchId, Object payload) {
        log.info("Publishing jobMatch [{}] to exchange [{}] with routing key [{}]",
                jobMatchId, RabbitMQConfig.EXCHANGE, routingKey);
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, routingKey, payload);
        } catch (Exception e) {
            log.error("Failed to publish jobMatch [{}] to routingKey [{}]: {}",
                    jobMatchId, routingKey, e.getMessage(), e);
            throw new MessagePublishingException("Could not publish job match message to queue: " + routingKey, e);
        }
    }
}
