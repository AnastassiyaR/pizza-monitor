package com.pizza.monitor.common.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

@Value
@Builder
@Jacksonized
public class PizzaMetric {
    String pizzeriaId;
    String pizzeriaName;
    double temperature;
    int ordersPerHour;
    int doughStock;
    int employeesOnDuty;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Instant timestamp;
}
