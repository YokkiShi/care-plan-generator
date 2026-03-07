package com.careplan;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class CarePlanController {

    @Value("${anthropic.api.key}")
    private String apiKey;

    // In-memory store: "mrn-medication" -> care plan text
    private final Map<String, String> store = new HashMap<>();

    private final RestTemplate restTemplate = new RestTemplate();


    @GetMapping("/api/orders/{id}")
    public Map<String, String> getOrder(@PathVariable String id) {
        String carePlan = store.get(id);
        if (carePlan == null) {
            return Map.of("error", "Care plan not found for ID: " + id);
        }
        return Map.of("id", id, "carePlan", carePlan);
    }
    @PostMapping("/api/orders")
    public Map<String, String> createOrder(@RequestBody Map<String, String> input) {
        String key = input.get("mrn") + "-" + input.get("medicationName");

        String prompt = """
                Generate a pharmacy care plan for the following patient.
                Use exactly these 4 sections with these headers:

                1. Problem List / Drug Therapy Problems
                2. Goals (SMART format)
                3. Pharmacist Interventions / Plan
                4. Monitoring Plan & Lab Schedule

                Patient: %s
                MRN: %s
                Medication: %s
                Primary Diagnosis (ICD-10): %s
                Medication History: %s
                """.formatted(
                input.getOrDefault("patientName", ""),
                input.getOrDefault("mrn", ""),
                input.getOrDefault("medicationName", ""),
                input.getOrDefault("primaryDiagnosis", ""),
                input.getOrDefault("medicationHistory", "N/A")
        );

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

        String carePlan = response.getBody().get("content").get(0).get("text").asText();
        store.put(key, carePlan);

        return Map.of("id", key, "carePlan", carePlan);
    }
}
