package com.careplan;

import com.careplan.model.*;
import com.careplan.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
public class CarePlanController {

    @Value("${anthropic.api.key}")
    private String apiKey;

    private final PatientRepository patientRepository;
    private final ProviderRepository providerRepository;
    private final OrderRepository orderRepository;
    private final CarePlanRepository carePlanRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public CarePlanController(PatientRepository patientRepository,
                              ProviderRepository providerRepository,
                              OrderRepository orderRepository,
                              CarePlanRepository carePlanRepository) {
        this.patientRepository = patientRepository;
        this.providerRepository = providerRepository;
        this.orderRepository = orderRepository;
        this.carePlanRepository = carePlanRepository;
    }

    @PostMapping("/api/orders")
    public Map<String, Object> createOrder(@RequestBody Map<String, String> input) {

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

        // 4. 创建 CarePlan，状态: PENDING
        CarePlan carePlan = new CarePlan();
        carePlan.setOrder(savedOrder);
        carePlan.setStatus("PENDING");
        carePlan = carePlanRepository.save(carePlan);

        // 5. 调用 LLM 前，状态: PROCESSING
        carePlan.setStatus("PROCESSING");
        carePlan = carePlanRepository.save(carePlan);

        // 6. 调用 Claude
        try {
            String content = callClaude(buildPrompt(input));
            carePlan.setContent(content);
            carePlan.setStatus("COMPLETED");
        } catch (Exception e) {
            carePlan.setStatus("FAILED");
            carePlanRepository.save(carePlan);
            return Map.of("error", "Failed to generate care plan");
        }

        carePlanRepository.save(carePlan);

        return Map.of(
                "orderId", savedOrder.getId(),
                "carePlanId", carePlan.getId(),
                "status", carePlan.getStatus(),
                "carePlan", carePlan.getContent()
        );
    }

    @GetMapping("/api/orders/{orderId}")
    public Map<String, Object> getOrder(@PathVariable Long orderId) {
        return carePlanRepository.findByOrderId(orderId)
                .map(cp -> Map.<String, Object>of(
                        "orderId", orderId,
                        "carePlanId", cp.getId(),
                        "status", cp.getStatus(),
                        "carePlan", cp.getContent() != null ? cp.getContent() : ""
                ))
                .orElse(Map.of("error", "Order not found: " + orderId));
    }

    private String buildPrompt(Map<String, String> input) {
        return """
                Generate a pharmacy care plan for the following patient.
                Use exactly these 4 sections with these headers:

                1. Problem List / Drug Therapy Problems
                2. Goals (SMART format)
                3. Pharmacist Interventions / Plan
                4. Monitoring Plan & Lab Schedule

                Patient: %s %s
                MRN: %s
                Medication: %s
                Primary Diagnosis (ICD-10): %s
                Medication History: %s
                """.formatted(
                input.getOrDefault("firstName", ""),
                input.getOrDefault("lastName", ""),
                input.getOrDefault("mrn", ""),
                input.getOrDefault("medicationName", ""),
                input.getOrDefault("primaryDiagnosis", ""),
                input.getOrDefault("medicationHistory", "N/A")
        );
    }

    private String callClaude(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        Map<String, Object> body = Map.of(
                "model", "claude-sonnet-4-6",
                "max_tokens", 2048,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "https://api.anthropic.com/v1/messages",
                new HttpEntity<>(body, headers),
                JsonNode.class
        );

        return response.getBody().get("content").get(0).get("text").asText();
    }
}
