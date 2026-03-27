package com.careplan;

import com.careplan.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class CarePlanController {

    private final CarePlanService carePlanService;

    public CarePlanController(CarePlanService carePlanService) {
        this.carePlanService = carePlanService;
    }

    @PostMapping("/api/orders")
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {
        return ResponseEntity.ok(carePlanService.createOrder(request));
    }

    @GetMapping("/api/orders/{orderId}")
    public ResponseEntity<OrderStatusResponse> getOrder(@PathVariable Long orderId) {
        return carePlanService.getOrder(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/careplan/{carePlanId}/status")
    public ResponseEntity<CarePlanStatusResponse> getCarePlanStatus(@PathVariable Long carePlanId) {
        return carePlanService.getCarePlanStatus(carePlanId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
