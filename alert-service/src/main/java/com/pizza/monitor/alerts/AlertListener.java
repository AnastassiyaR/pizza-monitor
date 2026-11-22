package com.pizza.monitor.alerts;

import com.pizza.monitor.common.kafka.Topics;
import com.pizza.monitor.common.model.Alert;
import com.pizza.monitor.common.redis.PizzaRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class AlertListener {

    private final PizzaRedisRepository repository;

    @KafkaListener(topics = Topics.ALERTS, groupId = "alert-group")
    public void onAlert(ConsumerRecord<String, Alert> record) {
        Alert alert = record.value();

        log.warn("[p{}@{}] {} {}: {}", record.partition(), record.offset(),
                alert.getSeverity(), alert.getPizzeriaName(), alert.getMessage());

        repository.saveAlert(alert);
    }
}
