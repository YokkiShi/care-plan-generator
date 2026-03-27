package com.careplan;

import com.careplan.dto.*;
import com.careplan.model.*;
import com.careplan.repository.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

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

    public OrderResponse createOrder(OrderRequest request) {

        // 1. 查找或创建 Patient
        Patient patient = patientRepository.findById(request.mrn())
                .orElseGet(() -> {
                    Patient p = new Patient();
                    p.setMrn(request.mrn());
                    p.setFirstName(request.firstName());
                    p.setLastName(request.lastName());
                    p.setDob(LocalDate.parse(request.dob()));
                    return patientRepository.save(p);
                });

        // 2. 查找或创建 Provider
        Provider provider = providerRepository.findById(request.npi())
                .orElseGet(() -> {
                    Provider pr = new Provider();
                    pr.setNpi(request.npi());
                    pr.setName(request.providerName());
                    return providerRepository.save(pr);
                });

        // 3. 创建 Order
        Order order = new Order();
        order.setPatient(patient);
        order.setProvider(provider);
        order.setMedicationName(request.medicationName());
        order.setPrimaryDiagnosis(request.primaryDiagnosis());
        order.setMedicationHistory(request.medicationHistory());
        Order savedOrder = orderRepository.save(order);

        // 4. 创建 CarePlan，status = PENDING
        CarePlan carePlan = new CarePlan();
        carePlan.setOrder(savedOrder);
        carePlan.setStatus("PENDING");
        carePlan = carePlanRepository.save(carePlan);

        // 5. 把 carePlanId 放进 Redis 队列
        redisTemplate.opsForList().leftPush(QUEUE_KEY, carePlan.getId().toString());

        return new OrderResponse("Order received", savedOrder.getId(), carePlan.getId(), "PENDING");
    }

    public Optional<OrderStatusResponse> getOrder(Long orderId) {
        return carePlanRepository.findByOrderId(orderId)
                .map(cp -> new OrderStatusResponse(
                        orderId,
                        cp.getId(),
                        cp.getStatus(),
                        cp.getContent() != null ? cp.getContent() : ""
                ));
    }

    public Optional<CarePlanStatusResponse> getCarePlanStatus(Long carePlanId) {
        return carePlanRepository.findById(carePlanId)
                .map(cp -> new CarePlanStatusResponse(
                        carePlanId,
                        cp.getStatus(),
                        "COMPLETED".equals(cp.getStatus()) ? cp.getContent() : null
                ));
    }
}
