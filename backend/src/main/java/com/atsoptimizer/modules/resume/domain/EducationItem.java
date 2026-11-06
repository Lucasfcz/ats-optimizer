package com.atsoptimizer.modules.resume.domain;

import java.io.Serializable;

public record EducationItem(
        String institution,
        String degree,
        String fieldOfStudy,
        String period,
        String status
) implements Serializable {
}
