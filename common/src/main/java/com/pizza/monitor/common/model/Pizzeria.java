package com.pizza.monitor.common.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;


@Value
@Builder
@Jacksonized
public class Pizzeria {
    String id;
    String name;
}
