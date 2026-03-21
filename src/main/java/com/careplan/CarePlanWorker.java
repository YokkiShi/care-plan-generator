package com.careplan;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CarePlanWorker implements ApplicationRunner {

    private static final String QUEUE_KEY = "careplan:queue";

    private final StringRedisTemplate redisTemplate;
    private final CarePlanProcessor processor;

    public CarePlanWorker(StringRedisTemplate redisTemplate, CarePlanProcessor processor) {
        this.redisTemplate = redisTemplate;
        this.processor = processor;
    }

    @Override
    public void run(ApplicationArguments args) {
        Thread worker = new Thread(() -> {
            System.out.println("[Worker] Started, waiting for tasks...");

            while (true) {
                // BRPOP: 阻塞等待，最多等 5 秒，有任务立刻返回
                String carePlanId = redisTemplate.opsForList()
                        .rightPop(QUEUE_KEY, Duration.ofSeconds(5));

                if (carePlanId != null) {
                    System.out.println("[Worker] Picked up carePlanId: " + carePlanId);
                    try {
                        processor.process(Long.parseLong(carePlanId));
                        System.out.println("[Worker] Completed carePlanId: " + carePlanId);
                    } catch (Exception e) {
                        System.out.println("[Worker] Failed carePlanId: " + carePlanId + " — " + e.getMessage());
                    }
                }
            }
        });

        worker.setDaemon(true); // 主程序退出时，这个线程也跟着退出
        worker.start();
    }
}
