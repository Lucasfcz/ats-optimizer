package com.atsoptimizer.modules.resume.domain;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public record ContactInfo(
        String phone,
        String email,
        String location,
        String linkedin,
        List<String> otherLinks
) implements Serializable {

    public ContactInfo {
        otherLinks = otherLinks != null ? List.copyOf(otherLinks) : Collections.emptyList();
    }
}
