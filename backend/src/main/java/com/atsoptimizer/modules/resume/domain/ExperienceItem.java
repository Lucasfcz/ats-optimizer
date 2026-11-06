package com.atsoptimizer.modules.resume.domain;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public record ExperienceItem(
        String title,
        String organization,
        String period,
        String location,
        String description,
        List<String> highlights
) implements Serializable {

    public ExperienceItem {
        highlights = highlights != null ? List.copyOf(highlights) : Collections.emptyList();
    }
}
