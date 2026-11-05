package com.atsoptimizer.queue;

import com.atsoptimizer.queue.dto.ResumeAnalysisMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ResumeEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ResumeEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchangeName;
    private final String routingKey;

    public ResumeEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${ats.rabbitmq.exchange:ats.exchange}") String exchangeName,
            @Value("${ats.rabbitmq.routing-key:resume.analyze}") String routingKey
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchangeName = exchangeName;
        this.routingKey = routingKey;
    }

    public void publishResumeForAnalysis(ResumeAnalysisMessage message) {
        log.info("Publishing resume [{}] to topic exchange [{}] with routing key [{}]",
                message.resumeId(), exchangeName, routingKey);
        rabbitTemplate.convertAndSend(exchangeName, routingKey, message);
        log.info("Successfully published resume [{}] to RabbitMQ", message.resumeId());
    }
}
