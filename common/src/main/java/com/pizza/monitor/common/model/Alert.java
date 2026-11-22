package com.pizza.monitor.common.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

@Value
@Builder
@Jacksonized
public class Alert {
    String pizzeriaId;
    String pizzeriaName;
    AlertType alertType;
    String message;
    Severity severity;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Instant timestamp;
}
