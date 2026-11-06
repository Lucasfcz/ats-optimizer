package com.atsoptimizer.modules.resume.domain;

import java.io.Serializable;

public record LanguageItem(
        String language,
        String proficiency
) implements Serializable {
}
