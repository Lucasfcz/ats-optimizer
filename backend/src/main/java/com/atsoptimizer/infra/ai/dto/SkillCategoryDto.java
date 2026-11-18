package com.atsoptimizer.infra.ai.dto;

import java.util.List;

public record SkillCategoryDto(
        String categoryName,
        List<String> skills
) {
}
