package com.atsoptimizer.infra.ai.dto;

import java.util.List;

public record JobSkillRequirementDto(
        String canonicalName,
        List<String> aliases
) {
}
