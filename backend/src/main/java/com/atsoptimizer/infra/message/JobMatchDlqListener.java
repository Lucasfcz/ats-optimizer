package com.atsoptimizer.infra.message;

import com.atsoptimizer.infra.message.dto.JobMatchGenerationMessage;
import com.atsoptimizer.infra.message.dto.JobMatchMessage;
import com.atsoptimizer.modules.jobmatch.domain.JobMatch;
import com.atsoptimizer.modules.jobmatch.repository.JobMatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JobMatchDlqListener {

    private static final Logger log = LoggerFactory.getLogger(JobMatchDlqListener.class);

    private final JobMatchRepository jobMatchRepository;

    public JobMatchDlqListener(JobMatchRepository jobMatchRepository) {
        this.jobMatchRepository = jobMatchRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.JOB_MATCH_PARSE_DLQ)
    @Transactional
    public void handleParseDlq(
            JobMatchMessage message,
            @Header(name = "x-exception-message", required = false) String exceptionMessage
    ) {
        markJobMatchFailed(message.jobMatchId(), RabbitMQConfig.JOB_MATCH_PARSE_DLQ, exceptionMessage);
    }

    @RabbitListener(queues = RabbitMQConfig.JOB_MATCH_AUDIT_DLQ)
    @Transactional
    public void handleAuditDlq(
            JobMatchMessage message,
            @Header(name = "x-exception-message", required = false) String exceptionMessage
    ) {
        markJobMatchFailed(message.jobMatchId(), RabbitMQConfig.JOB_MATCH_AUDIT_DLQ, exceptionMessage);
    }

    @RabbitListener(queues = RabbitMQConfig.JOB_MATCH_GENERATE_DLQ)
    @Transactional
    public void handleGenerateDlq(
            JobMatchGenerationMessage message,
            @Header(name = "x-exception-message", required = false) String exceptionMessage
    ) {
        markJobMatchFailed(message.jobMatchId(), RabbitMQConfig.JOB_MATCH_GENERATE_DLQ, exceptionMessage);
    }

    private void markJobMatchFailed(Long jobMatchId, String dlqQueue, String exceptionMessage) {
        String reason = exceptionMessage != null
                ? exceptionMessage
                : "Exceeded maximum retry attempts during job match processing";
        log.error("CRITICAL: JobMatch [{}] routed to DLQ [{}]. Reason: {}", jobMatchId, dlqQueue, reason);

        JobMatch jobMatch = jobMatchRepository.findById(jobMatchId).orElse(null);
        if (jobMatch != null) {
            jobMatch.failMatch(reason);
            jobMatchRepository.save(jobMatch);
            log.info("JobMatch [{}] status updated to FAILED in database after DLQ routing", jobMatchId);
        } else {
            log.error("Failed to update status for jobMatch [{}]: entity not found in database", jobMatchId);
        }
    }
}
