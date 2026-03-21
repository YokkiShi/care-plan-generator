package com.careplan;

import com.careplan.model.CarePlan;
import com.careplan.repository.CarePlanRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class CarePlanProcessor {

    private static final int MAX_RETRIES = 3;

    private final CarePlanRepository carePlanRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${anthropic.api.key}")
    private String apiKey;

    @Value("${llm.mock:false}")
    private boolean mockLlm;

    public CarePlanProcessor(CarePlanRepository carePlanRepository) {
        this.carePlanRepository = carePlanRepository;
    }

    @Transactional
    public void process(Long carePlanId) {
        // 1. 加载完整数据，status → PROCESSING
        CarePlan carePlan = carePlanRepository.findByIdWithDetails(carePlanId)
                .orElseThrow(() -> new RuntimeException("CarePlan not found: " + carePlanId));

        carePlan.setStatus("PROCESSING");
        carePlanRepository.save(carePlan);

        // 2. 调用 LLM，失败重试（指数退避）
        String content = null;
        String prompt = buildPrompt(carePlan);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                System.out.printf("[Processor] carePlanId=%d attempt=%d/%d%n", carePlanId, attempt, MAX_RETRIES);
                content = mockLlm ? mockCarePlan(carePlan) : callClaude(prompt);
                System.out.printf("[Processor] carePlanId=%d attempt=%d succeeded%n", carePlanId, attempt);
                break;
            } catch (Exception e) {
                System.out.printf("[Processor] carePlanId=%d attempt=%d failed: %s%n", carePlanId, attempt, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    long waitSeconds = (long) Math.pow(2, attempt); // 2s → 4s
                    System.out.printf("[Processor] carePlanId=%d retrying in %ds...%n", carePlanId, waitSeconds);
                    try {
                        Thread.sleep(waitSeconds * 1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        // 3. 写回数据库
        if (content != null) {
            carePlan.setContent(content);
            carePlan.setStatus("COMPLETED");
        } else {
            carePlan.setStatus("FAILED");
            System.out.printf("[Processor] carePlanId=%d all %d attempts failed, status=FAILED%n", carePlanId, MAX_RETRIES);
        }

        carePlanRepository.save(carePlan);
    }

    private String buildPrompt(CarePlan carePlan) {
        var order    = carePlan.getOrder();
        var patient  = order.getPatient();
        var provider = order.getProvider();

        return """
                Generate a pharmacy care plan for the following patient.
                Use exactly these 4 sections with these headers:

                1. Problem List / Drug Therapy Problems
                2. Goals (SMART format)
                3. Pharmacist Interventions / Plan
                4. Monitoring Plan & Lab Schedule

                Patient: %s %s
                MRN: %s
                Provider: %s
                Medication: %s
                Primary Diagnosis (ICD-10): %s
                Medication History: %s
                """.formatted(
                patient.getFirstName(),
                patient.getLastName(),
                patient.getMrn(),
                provider.getName(),
                order.getMedicationName(),
                order.getPrimaryDiagnosis(),
                order.getMedicationHistory() != null ? order.getMedicationHistory() : "N/A"
        );
    }

    private String mockCarePlan(CarePlan carePlan) {
        String medication = carePlan.getOrder().getMedicationName();
        String diagnosis  = carePlan.getOrder().getPrimaryDiagnosis();
        return """
                [MOCK] Care Plan

                1. Problem List / Drug Therapy Problems
                - Diagnosis: %s
                - Medication: %s indicated for treatment

                2. Goals (SMART Format)
                - Patient will show improvement within 4 weeks of starting %s

                3. Pharmacist Interventions / Plan
                - Verify dose and administration schedule
                - Counsel patient on side effects and adherence

                4. Monitoring Plan & Lab Schedule
                - CBC and CMP at baseline, then every 3 months
                - Follow-up with prescriber in 4 weeks
                """.formatted(diagnosis, medication, medication);
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
