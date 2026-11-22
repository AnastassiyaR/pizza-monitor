package com.pizza.monitor.api;

import com.pizza.monitor.common.model.Alert;
import com.pizza.monitor.common.model.Thresholds;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class Snapshot {
    String type;
    List<PizzeriaView> pizzerias;
    List<Alert> alerts;
    Thresholds thresholds;
}
