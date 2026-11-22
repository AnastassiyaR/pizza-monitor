package com.pizza.monitor.monitor;

import com.pizza.monitor.common.kafka.KafkaTopicsConfig;
import com.pizza.monitor.common.model.Thresholds;
import com.pizza.monitor.common.redis.PizzaRedisRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EnableConfigurationProperties({Thresholds.class, MonitorProperties.class})
@Import({KafkaTopicsConfig.class, PizzaRedisRepository.class})
public class MonitorApplication {
    public static void main(String[] args) {
        SpringApplication.run(MonitorApplication.class, args);
    }
}
