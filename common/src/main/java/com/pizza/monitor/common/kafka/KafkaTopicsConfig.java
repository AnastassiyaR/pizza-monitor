package com.pizza.monitor.common.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Topic declarations. Spring Boot creates a {@code KafkaAdmin} which, at application startup,
 * finds all {@link NewTopic} beans and creates the missing topics on the broker
 * (if a topic already exists, nothing happens). Equivalent of {@code ensureTopics()} in the Node version.
 *
 * <p>Imported by services via {@code @Import(KafkaTopicsConfig.class)}.
 */
@Configuration
public class KafkaTopicsConfig {

    /**
     * 3 partitions: messages with the same key (pizzeriaId) always land in the same partition,
     * while different pizzerias can be processed in parallel by different consumer threads.
     * replicas(1) because docker-compose runs a single broker.
     */
    @Bean
    NewTopic metricsTopic() {
        return TopicBuilder.name(Topics.METRICS).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic alertsTopic() {
        return TopicBuilder.name(Topics.ALERTS).partitions(3).replicas(1).build();
    }
}
