package com.pizza.monitor.monitor;

import com.pizza.monitor.common.kafka.Topics;
import com.pizza.monitor.common.model.Alert;
import com.pizza.monitor.common.model.PizzaMetric;
import com.pizza.monitor.common.redis.PizzaRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetricsListener {

    private final PizzaRedisRepository repository;
    private final AlertDetector detector;
    private final MonitorProperties properties;
    private final KafkaTemplate<String, Alert> alertTemplate;

    @KafkaListener(topics = Topics.METRICS, groupId = "monitor-group", concurrency = "3")
    public void onMetric(ConsumerRecord<String, PizzaMetric> record) {
        PizzaMetric metric = record.value();

        repository.saveMetric(metric);

        log.info("[p{}@{}] {}: {}°C, orders/hour {}, dough {} kg, staff {}",
                record.partition(), record.offset(),
                metric.getPizzeriaName(), metric.getTemperature(),
                metric.getOrdersPerHour(), metric.getDoughStock(), metric.getEmployeesOnDuty());

        for (Alert alert : detector.detect(metric)) {
            boolean firstInCooldown = repository.tryAcquireAlertSlot(
                    alert.getPizzeriaId(), alert.getAlertType(), properties.getAlertCooldown());
            if (firstInCooldown) {
                alertTemplate.send(Topics.ALERTS, alert.getPizzeriaId(), alert);
                log.warn("Alert published: {} — {}", alert.getPizzeriaName(), alert.getMessage());
            }
        }
    }
}
