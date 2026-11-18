package com.atsoptimizer.modules.resume.domain;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Immutable value object representing a grouped category of technical skills
 * (e.g., "Linguagens", "Back-end e Arquitetura").
 */
public record SkillCategory(
        String categoryName,
        List<String> skills
) implements Serializable {

    public SkillCategory {
        skills = skills != null ? List.copyOf(skills) : Collections.emptyList();
    }
}
