package com.atsoptimizer.modules.jobmatch.domain;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Represents a single work experience or project entry formatted for the 1-page ATS PDF template.
 */
public record TemplateExperienceEntry(
        String title,
        String period,
        String subtitleLabel,
        boolean isStack,
        List<String> bullets
) implements Serializable {

    public TemplateExperienceEntry {
        bullets = bullets != null ? List.copyOf(bullets) : Collections.emptyList();
    }

    public TemplateExperienceEntry withBullets(List<String> updatedBullets) {
        return new TemplateExperienceEntry(title, period, subtitleLabel, isStack, updatedBullets);
    }
}
