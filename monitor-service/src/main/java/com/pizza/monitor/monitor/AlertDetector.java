package com.pizza.monitor.monitor;

import com.pizza.monitor.common.model.Alert;
import com.pizza.monitor.common.model.AlertType;
import com.pizza.monitor.common.model.PizzaMetric;
import com.pizza.monitor.common.model.Severity;
import com.pizza.monitor.common.model.Thresholds;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.pizza.monitor.common.model.AlertType.HIGH_TEMP;
import static com.pizza.monitor.common.model.AlertType.LOW_STOCK;
import static com.pizza.monitor.common.model.Severity.CRITICAL;

@Component
@RequiredArgsConstructor
public class AlertDetector {

    private final Thresholds thresholds;

    public List<Alert> detect(PizzaMetric metric) {
        List<Alert> alerts = new ArrayList<>();

        if (metric.getTemperature() > thresholds.getTempCritical()) {
            alerts.add(alert(metric, HIGH_TEMP,
                    "Oven overheating: " + Math.round(metric.getTemperature()) + "°C"));
        }
        if (metric.getDoughStock() < thresholds.getDoughCritical()) {
            alerts.add(alert(metric, LOW_STOCK,
                    "Low dough stock: " + metric.getDoughStock() + " kg"));
        }
        return alerts;
    }

    private static Alert alert(PizzaMetric metric, AlertType type, String message) {
        return Alert.builder()
                .pizzeriaId(metric.getPizzeriaId())
                .pizzeriaName(metric.getPizzeriaName())
                .alertType(type)
                .message(message)
                .severity(CRITICAL)
                .timestamp(Instant.now())
                .build();
    }
}
