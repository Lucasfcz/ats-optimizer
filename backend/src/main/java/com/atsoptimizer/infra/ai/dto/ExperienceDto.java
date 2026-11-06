package com.atsoptimizer.infra.ai.dto;

import java.util.List;

public record ExperienceDto(
        String title,
        String organization,
        String period,
        String location,
        String description,
        List<String> highlights
) {
}
