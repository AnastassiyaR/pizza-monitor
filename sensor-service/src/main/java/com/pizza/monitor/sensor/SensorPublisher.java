package com.pizza.monitor.sensor;

import com.pizza.monitor.common.model.PizzaMetric;
import com.pizza.monitor.common.model.Pizzeria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static com.pizza.monitor.common.kafka.Topics.METRICS;
import static com.pizza.monitor.common.model.Pizzerias.ALL;

@Slf4j
@Component
@RequiredArgsConstructor
public class SensorPublisher {

    private final KafkaTemplate<String, PizzaMetric> kafkaTemplate;
    private final PizzaSimulator simulator;

    @Scheduled(fixedDelayString = "${pizza.sensor.interval-ms:3000}")
    public void publishMetrics() {
        for (Pizzeria pizzeria : ALL) {
            PizzaMetric metric = simulator.next(pizzeria);

            kafkaTemplate.send(METRICS, pizzeria.getId(), metric)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            log.error("Failed to send metric of {}", pizzeria.getName(), error);
                        } else {
                            log.debug("Sent {} -> partition {}, offset {}",
                                    pizzeria.getName(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        }
        log.info("Published {} metrics to '{}'", ALL.size(), METRICS);
    }
}
