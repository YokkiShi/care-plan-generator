package com.careplan;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class CarePlanController {

    private final CarePlanService carePlanService;

    public CarePlanController(CarePlanService carePlanService) {
        this.carePlanService = carePlanService;
    }

    @PostMapping("/api/orders")
    public Map<String, Object> createOrder(@RequestBody Map<String, String> input) {
        return carePlanService.createOrder(input);
    }

    @GetMapping("/api/orders/{orderId}")
    public Map<String, Object> getOrder(@PathVariable Long orderId) {
        return carePlanService.getOrder(orderId);
    }

    @GetMapping("/api/careplan/{carePlanId}/status")
    public Map<String, Object> getCarePlanStatus(@PathVariable Long carePlanId) {
        return carePlanService.getCarePlanStatus(carePlanId);
    }
}
