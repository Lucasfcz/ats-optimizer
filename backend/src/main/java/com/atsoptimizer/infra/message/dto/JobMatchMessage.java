package com.atsoptimizer.infra.message.dto;

import java.io.Serializable;

public record JobMatchMessage(
        Long jobMatchId,
        Long resumeId
) implements Serializable {
}
