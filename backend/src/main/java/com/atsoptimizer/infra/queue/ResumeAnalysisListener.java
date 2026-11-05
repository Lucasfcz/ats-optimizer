package com.atsoptimizer.infra.queue;

import com.atsoptimizer.infra.queue.dto.ResumeAnalysisMessage;
import com.atsoptimizer.modules.resume.service.ResumeAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ResumeAnalysisListener {

    private static final Logger log = LoggerFactory.getLogger(ResumeAnalysisListener.class);

    private final ResumeAnalysisService resumeAnalysisService;

    public ResumeAnalysisListener(ResumeAnalysisService resumeAnalysisService) {
        this.resumeAnalysisService = resumeAnalysisService;
    }

    @RabbitListener(queues = "${ats.rabbitmq.queue:ats.resume.analysis.queue}")
    public void handleResumeAnalysis(ResumeAnalysisMessage message) {
        log.info("Received message from RabbitMQ queue for resume [{}] ({})", message.resumeId(), message.fileName());
        resumeAnalysisService.processAnalysis(message.resumeId(), message.extractedText());
    }
}
