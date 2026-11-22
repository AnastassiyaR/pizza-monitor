package com.pizza.monitor.alerts;

import com.pizza.monitor.common.kafka.KafkaTopicsConfig;
import com.pizza.monitor.common.redis.PizzaRedisRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;


@SpringBootApplication
@Import({KafkaTopicsConfig.class, PizzaRedisRepository.class})
public class AlertApplication {
    public static void main(String[] args) {
        SpringApplication.run(AlertApplication.class, args);
    }
}
