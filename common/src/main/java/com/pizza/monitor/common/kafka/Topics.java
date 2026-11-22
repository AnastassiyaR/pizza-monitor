package com.pizza.monitor.common.kafka;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Kafka topic names. Constants (not scattered strings) so they can be used in @KafkaListener. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Topics {
    /** sensor -> monitor */
    public static final String METRICS = "pizza-metrics";
    /** monitor -> alerts */
    public static final String ALERTS = "pizza-alerts";
}
