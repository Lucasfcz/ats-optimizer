package com.atsoptimizer.modules.resume.domain;

import java.io.Serializable;

public record CertificationItem(
        String name,
        String issuer,
        String year
) implements Serializable {
}
