package com.careplan.dto;

public record OrderResponse(
        String message,
        Long orderId,
        Long carePlanId,
        String status
) {}
