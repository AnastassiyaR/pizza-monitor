package com.pizza.monitor.common.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Data
@ConfigurationProperties(prefix = "pizza.thresholds")
public class Thresholds {
    private double tempWarning = 300;
    private double tempCritical = 310;
    private double doughWarning = 40;
    private double doughCritical = 20;
}
