package com.atsoptimizer.infra.message;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "ats.exchange";
    public static final String DLX_EXCHANGE = "ats.exchange.dlx";

    public static final String RESUME_ANALYSIS_QUEUE = "resume.ai.analysis";
    public static final String RESUME_ANALYSIS_DLQ = "resume.ai.analysis.dlq";

    public static final String JOB_MATCH_PARSE_QUEUE = "resume.ai.description.match.parse";
    public static final String JOB_MATCH_PARSE_DLQ = "resume.ai.description.match.parse.dlq";

    public static final String JOB_MATCH_AUDIT_QUEUE = "resume.ai.description.match.audit";
    public static final String JOB_MATCH_AUDIT_DLQ = "resume.ai.description.match.audit.dlq";

    public static final String JOB_MATCH_GENERATE_QUEUE = "resume.ai.description.match.generate";
    public static final String JOB_MATCH_GENERATE_DLQ = "resume.ai.description.match.generate.dlq";

    // --- Exchanges ---

    @Bean
    public TopicExchange atsTopicExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange atsDeadLetterExchange() {
        return new TopicExchange(DLX_EXCHANGE, true, false);
    }

    // --- Topologia: Análise Inicial de Currículo ---

    @Bean
    public Queue atsResumeAnalysisQueue() {
        return QueueBuilder.durable(RESUME_ANALYSIS_QUEUE).build();
    }

    @Bean
    public Binding atsResumeAnalysisBinding(
            @Qualifier("atsResumeAnalysisQueue") Queue queue,
            @Qualifier("atsTopicExchange") TopicExchange exchange
    ) {
        return bindExact(queue, exchange, RESUME_ANALYSIS_QUEUE);
    }

    @Bean
    public Queue atsDeadLetterQueue() {
        return QueueBuilder.durable(RESUME_ANALYSIS_DLQ).build();
    }

    @Bean
    public Binding atsDeadLetterBinding(
            @Qualifier("atsDeadLetterQueue") Queue queue,
            @Qualifier("atsDeadLetterExchange") TopicExchange dlxExchange
    ) {
        return bindExact(queue, dlxExchange, RESUME_ANALYSIS_DLQ);
    }

    // --- Topologia: Match de Vaga (Parse -> Audit -> Generate) ---

    @Bean
    public Queue jobMatchParseQueue() {
        return QueueBuilder.durable(JOB_MATCH_PARSE_QUEUE).build();
    }

    @Bean
    public Binding jobMatchParseBinding(
            @Qualifier("jobMatchParseQueue") Queue queue,
            @Qualifier("atsTopicExchange") TopicExchange exchange
    ) {
        return bindExact(queue, exchange, JOB_MATCH_PARSE_QUEUE);
    }

    @Bean
    public Queue jobMatchParseDlqQueue() {
        return QueueBuilder.durable(JOB_MATCH_PARSE_DLQ).build();
    }

    @Bean
    public Binding jobMatchParseDlqBinding(
            @Qualifier("jobMatchParseDlqQueue") Queue queue,
            @Qualifier("atsDeadLetterExchange") TopicExchange dlxExchange
    ) {
        return bindExact(queue, dlxExchange, JOB_MATCH_PARSE_DLQ);
    }

    @Bean
    public Queue jobMatchAuditQueue() {
        return QueueBuilder.durable(JOB_MATCH_AUDIT_QUEUE).build();
    }

    @Bean
    public Binding jobMatchAuditBinding(
            @Qualifier("jobMatchAuditQueue") Queue queue,
            @Qualifier("atsTopicExchange") TopicExchange exchange
    ) {
        return bindExact(queue, exchange, JOB_MATCH_AUDIT_QUEUE);
    }

    @Bean
    public Queue jobMatchAuditDlqQueue() {
        return QueueBuilder.durable(JOB_MATCH_AUDIT_DLQ).build();
    }

    @Bean
    public Binding jobMatchAuditDlqBinding(
            @Qualifier("jobMatchAuditDlqQueue") Queue queue,
            @Qualifier("atsDeadLetterExchange") TopicExchange dlxExchange
    ) {
        return bindExact(queue, dlxExchange, JOB_MATCH_AUDIT_DLQ);
    }

    @Bean
    public Queue jobMatchGenerateQueue() {
        return QueueBuilder.durable(JOB_MATCH_GENERATE_QUEUE).build();
    }

    @Bean
    public Binding jobMatchGenerateBinding(
            @Qualifier("jobMatchGenerateQueue") Queue queue,
            @Qualifier("atsTopicExchange") TopicExchange exchange
    ) {
        return bindExact(queue, exchange, JOB_MATCH_GENERATE_QUEUE);
    }

    @Bean
    public Queue jobMatchGenerateDlqQueue() {
        return QueueBuilder.durable(JOB_MATCH_GENERATE_DLQ).build();
    }

    @Bean
    public Binding jobMatchGenerateDlqBinding(
            @Qualifier("jobMatchGenerateDlqQueue") Queue queue,
            @Qualifier("atsDeadLetterExchange") TopicExchange dlxExchange
    ) {
        return bindExact(queue, dlxExchange, JOB_MATCH_GENERATE_DLQ);
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
        return new RepublishMessageRecoverer(rabbitTemplate, DLX_EXCHANGE) {
            @Override
            protected String prefixedOriginalRoutingKey(Message message) {
                String originalRoutingKey = message.getMessageProperties().getReceivedRoutingKey();
                return originalRoutingKey != null ? originalRoutingKey + ".dlq" : RESUME_ANALYSIS_DLQ;
            }
        };
    }

    private Binding bindExact(Queue queue, TopicExchange exchange, String routingKey) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(routingKey);
    }
}
