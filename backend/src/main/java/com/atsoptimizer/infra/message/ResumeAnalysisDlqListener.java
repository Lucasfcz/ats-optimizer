package com.atsoptimizer.infra.message;

import com.atsoptimizer.infra.message.dto.ResumeAnalysisMessage;
import com.atsoptimizer.modules.resume.domain.Resume;
import com.atsoptimizer.modules.resume.repository.ResumeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ResumeAnalysisDlqListener {

    private static final Logger log = LoggerFactory.getLogger(ResumeAnalysisDlqListener.class);

    private final ResumeRepository resumeRepository;

    public ResumeAnalysisDlqListener(ResumeRepository resumeRepository) {
        this.resumeRepository = resumeRepository;
    }

    @RabbitListener(queues = "${ats.rabbitmq.dlq-queue:resume.ai.analysis.dlq}")
    @Transactional
    public void handleDeadLetterMessage(
            ResumeAnalysisMessage message,
            @Header(name = "x-exception-message", required = false) String exceptionMessage
    ) {
        String reason = exceptionMessage != null ? exceptionMessage : "Exceeded maximum retry attempts during AI analysis";
        log.error("CRITICAL: Resume [{}] routed to Dead Letter Queue (DLQ). Reason: {}",
                message.resumeId(), reason);

        Resume resume = resumeRepository.findById(message.resumeId()).orElse(null);
        if (resume != null) {
            resume.failAnalysis(reason);
            resumeRepository.save(resume);
            log.info("Resume [{}] status updated to FAILED in database after DLQ routing", message.resumeId());
        } else {
            log.error("Failed to update status for resume [{}]: entity not found in database", message.resumeId());
        }
    }
}
