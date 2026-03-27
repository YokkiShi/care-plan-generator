package com.careplan;

import com.careplan.model.*;
import com.careplan.repository.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

@Service
public class CarePlanService {

    private static final String QUEUE_KEY = "careplan:queue";

    private final PatientRepository patientRepository;
    private final ProviderRepository providerRepository;
    private final OrderRepository orderRepository;
    private final CarePlanRepository carePlanRepository;
    private final StringRedisTemplate redisTemplate;

    public CarePlanService(PatientRepository patientRepository,
                           ProviderRepository providerRepository,
                           OrderRepository orderRepository,
                           CarePlanRepository carePlanRepository,
                           StringRedisTemplate redisTemplate) {
        this.patientRepository = patientRepository;
        this.providerRepository = providerRepository;
        this.orderRepository = orderRepository;
        this.carePlanRepository = carePlanRepository;
        this.redisTemplate = redisTemplate;
    }

    public Map<String, Object> createOrder(Map<String, String> input) {

        // 1. 查找或创建 Patient
        Patient patient = patientRepository.findById(input.get("mrn"))
                .orElseGet(() -> {
                    Patient p = new Patient();
                    p.setMrn(input.get("mrn"));
                    p.setFirstName(input.get("firstName"));
                    p.setLastName(input.get("lastName"));
                    p.setDob(LocalDate.parse(input.get("dob")));
                    return patientRepository.save(p);
                });

        // 2. 查找或创建 Provider
        Provider provider = providerRepository.findById(input.get("npi"))
                .orElseGet(() -> {
                    Provider pr = new Provider();
                    pr.setNpi(input.get("npi"));
                    pr.setName(input.get("providerName"));
                    return providerRepository.save(pr);
                });

        // 3. 创建 Order
        Order order = new Order();
        order.setPatient(patient);
        order.setProvider(provider);
        order.setMedicationName(input.get("medicationName"));
        order.setPrimaryDiagnosis(input.get("primaryDiagnosis"));
        order.setMedicationHistory(input.get("medicationHistory"));
        Order savedOrder = orderRepository.save(order);

        // 4. 创建 CarePlan，status = PENDING
        CarePlan carePlan = new CarePlan();
        carePlan.setOrder(savedOrder);
        carePlan.setStatus("PENDING");
        carePlan = carePlanRepository.save(carePlan);

        // 5. 把 carePlanId 放进 Redis 队列
        redisTemplate.opsForList().leftPush(QUEUE_KEY, carePlan.getId().toString());

        // 6. 返回响应数据
        return Map.of(
                "message", "Order received",
                "orderId", savedOrder.getId(),
                "carePlanId", carePlan.getId(),
                "status", "PENDING"
        );
    }

    public Map<String, Object> getOrder(Long orderId) {
        return carePlanRepository.findByOrderId(orderId)
                .map(cp -> Map.<String, Object>of(
                        "orderId", orderId,
                        "carePlanId", cp.getId(),
                        "status", cp.getStatus(),
                        "carePlan", cp.getContent() != null ? cp.getContent() : ""
                ))
                .orElse(Map.of("error", "Order not found: " + orderId));
    }

    public Map<String, Object> getCarePlanStatus(Long carePlanId) {
        return carePlanRepository.findById(carePlanId)
                .map(cp -> {
                    if ("COMPLETED".equals(cp.getStatus())) {
                        return Map.<String, Object>of(
                                "carePlanId", carePlanId,
                                "status", cp.getStatus(),
                                "content", cp.getContent()
                        );
                    }
                    return Map.<String, Object>of(
                            "carePlanId", carePlanId,
                            "status", cp.getStatus()
                    );
                })
                .orElse(Map.of("error", "CarePlan not found: " + carePlanId));
    }
}
