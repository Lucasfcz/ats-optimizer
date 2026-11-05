package com.atsoptimizer.queue;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${ats.rabbitmq.exchange:ats.exchange}")
    private String exchangeName;

    @Value("${ats.rabbitmq.queue:ats.resume.analysis.queue}")
    private String queueName;

    @Value("${ats.rabbitmq.routing-key:resume.analyze}")
    private String routingKey;

    @Bean
    public TopicExchange atsTopicExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue atsResumeAnalysisQueue() {
        return new Queue(queueName, true);
    }

    @Bean
    public Binding atsResumeAnalysisBinding(Queue atsResumeAnalysisQueue, TopicExchange atsTopicExchange) {
        return BindingBuilder
                .bind(atsResumeAnalysisQueue)
                .to(atsTopicExchange)
                .with(routingKey);
    }

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
}
