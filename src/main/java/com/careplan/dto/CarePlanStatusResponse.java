package com.careplan.dto;

public record CarePlanStatusResponse(
        Long carePlanId,
        String status,
        String content  // null 表示未完成，COMPLETED 时才有值
) {}
