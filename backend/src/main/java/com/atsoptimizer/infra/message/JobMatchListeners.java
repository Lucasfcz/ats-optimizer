package com.atsoptimizer.infra.message;

import com.atsoptimizer.infra.message.dto.JobMatchGenerationMessage;
import com.atsoptimizer.infra.message.dto.JobMatchMessage;
import com.atsoptimizer.modules.jobmatch.service.JobMatchProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class JobMatchListeners {

    private static final Logger log = LoggerFactory.getLogger(JobMatchListeners.class);

    private final JobMatchProcessingService jobMatchProcessingService;

    public JobMatchListeners(JobMatchProcessingService jobMatchProcessingService) {
        this.jobMatchProcessingService = jobMatchProcessingService;
    }

    @RabbitListener(queues = RabbitMQConfig.JOB_MATCH_PARSE_QUEUE)
    public void handleJobParsing(JobMatchMessage message) {
        log.info("Received PARSE message for jobMatch [{}]", message.jobMatchId());
        jobMatchProcessingService.processJobParsing(message.jobMatchId());
    }

    @RabbitListener(queues = RabbitMQConfig.JOB_MATCH_AUDIT_QUEUE)
    public void handleFitAudit(JobMatchMessage message) {
        log.info("Received AUDIT message for jobMatch [{}]", message.jobMatchId());
        jobMatchProcessingService.processFitAudit(message.jobMatchId());
    }

    @RabbitListener(queues = RabbitMQConfig.JOB_MATCH_GENERATE_QUEUE)
    public void handleResumeGeneration(JobMatchGenerationMessage message) {
        log.info("Received GENERATE message for jobMatch [{}] with mode [{}]",
                message.jobMatchId(), message.mode());
        jobMatchProcessingService.processResumeGeneration(message.jobMatchId(), message.mode());
    }
}
