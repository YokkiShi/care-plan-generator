package com.careplan.dto;

public record OrderStatusResponse(
        Long orderId,
        Long carePlanId,
        String status,
        String carePlan
) {}
