package com.pizza.monitor.monitor;

import com.pizza.monitor.common.model.Alert;
import com.pizza.monitor.common.model.AlertType;
import com.pizza.monitor.common.model.PizzaMetric;
import com.pizza.monitor.common.model.Thresholds;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AlertDetectorTest {

    private final AlertDetector detector = new AlertDetector(new Thresholds());

    private static PizzaMetric metric(double temperature, int doughStock) {
        return PizzaMetric.builder()
                .pizzeriaId("pizza-1")
                .pizzeriaName("Margherita")
                .temperature(temperature)
                .doughStock(doughStock)
                .ordersPerHour(10)
                .employeesOnDuty(4)
                .timestamp(Instant.now())
                .build();
    }

    @Test
    void normalMetricProducesNoAlerts() {
        assertThat(detector.detect(metric(295, 60))).isEmpty();
    }

    @Test
    void overheatingProducesHighTempAlert() {
        List<Alert> alerts = detector.detect(metric(315, 60));

        assertThat(alerts).extracting(Alert::getAlertType).containsExactly(AlertType.HIGH_TEMP);
    }

    @Test
    void lowDoughProducesLowStockAlert() {
        List<Alert> alerts = detector.detect(metric(295, 10));

        assertThat(alerts).extracting(Alert::getAlertType).containsExactly(AlertType.LOW_STOCK);
    }

    @Test
    void bothProblemsProduceTwoAlerts() {
        assertThat(detector.detect(metric(320, 5))).hasSize(2);
    }
}
