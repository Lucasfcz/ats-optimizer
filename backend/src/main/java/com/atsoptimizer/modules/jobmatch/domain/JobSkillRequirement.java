package com.atsoptimizer.modules.jobmatch.domain;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Represents a job requirement skill together with known aliases/synonyms
 * used by the deterministic keyword matching algorithm.
 */
public record JobSkillRequirement(
        String canonicalName,
        List<String> aliases
) implements Serializable {

    public JobSkillRequirement {
        aliases = aliases != null ? List.copyOf(aliases) : Collections.emptyList();
    }
}
