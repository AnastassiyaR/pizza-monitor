package com.pizza.monitor.api;

import com.pizza.monitor.common.model.PizzaMetric;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PizzeriaView {
    String id;
    String name;
    PizzaMetric metrics;
}
