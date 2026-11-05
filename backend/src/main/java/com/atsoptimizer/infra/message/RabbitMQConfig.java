package com.atsoptimizer.infra.message;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${ats.rabbitmq.exchange:ats.exchange}")
    private String exchange;

    @Value("${ats.rabbitmq.queue:resume.ai.analysis}")
    private String queue;

    @Value("${ats.rabbitmq.routing-key:resume.ai.analysis}")
    private String routingKey;

    @Value("${ats.rabbitmq.dlx-exchange:ats.exchange.dlx}")
    private String dlxExchange;

    @Value("${ats.rabbitmq.dlq-queue:resume.ai.analysis.dlq}")
    private String dlqQueue;

    @Value("${ats.rabbitmq.dlq-routing-key:resume.ai.analysis.dlq}")
    private String dlqRoutingKey;

    // --- Topologia da Fila Principal ---

    @Bean
    public TopicExchange atsTopicExchange() {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public Queue atsResumeAnalysisQueue() {
        return QueueBuilder.durable(queue).build();
    }

    @Bean
    public Binding atsResumeAnalysisBinding(Queue atsResumeAnalysisQueue, TopicExchange atsTopicExchange) {
        return BindingBuilder
                .bind(atsResumeAnalysisQueue)
                .to(atsTopicExchange)
                .with(routingKey);
    }

    // --- Topologia da Dead Letter Queue (DLQ) ---

    @Bean
    public TopicExchange atsDeadLetterExchange() {
        return new TopicExchange(dlxExchange, true, false);
    }

    @Bean
    public Queue atsDeadLetterQueue() {
        return QueueBuilder.durable(dlqQueue).build();
    }

    @Bean
    public Binding atsDeadLetterBinding(Queue atsDeadLetterQueue, TopicExchange atsDeadLetterExchange) {
        return BindingBuilder
                .bind(atsDeadLetterQueue)
                .to(atsDeadLetterExchange)
                .with(dlqRoutingKey);
    }

    // --- Configurações de Serialização e Retry/Recoverer ---

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    @Bean
    public MessageRecoverer messageRecoverer(RabbitTemplate rabbitTemplate) {
        return new RepublishMessageRecoverer(rabbitTemplate, dlxExchange, dlqRoutingKey);
    }
}
