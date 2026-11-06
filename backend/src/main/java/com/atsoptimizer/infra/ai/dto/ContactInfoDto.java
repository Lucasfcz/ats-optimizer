package com.atsoptimizer.infra.ai.dto;

import java.util.List;

public record ContactInfoDto(
        String phone,
        String email,
        String location,
        String linkedin,
        List<String> otherLinks
) {
}
