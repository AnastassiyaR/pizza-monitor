package com.pizza.monitor.monitor;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "pizza.monitor")
public class MonitorProperties {
    private Duration alertCooldown = Duration.ofSeconds(30);
}
