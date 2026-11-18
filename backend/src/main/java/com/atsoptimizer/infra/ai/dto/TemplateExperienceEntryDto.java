package com.atsoptimizer.infra.ai.dto;

import java.util.List;

public record TemplateExperienceEntryDto(
        String title,
        String period,
        String subtitleLabel,
        Boolean isStack,
        List<String> bullets
) {
}
