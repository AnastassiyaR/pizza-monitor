package com.pizza.monitor.common.model;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Pizzerias {

    public static final List<Pizzeria> ALL = List.of(
            Pizzeria.builder().id("pizza-1").name("Margherita").build(),
            Pizzeria.builder().id("pizza-2").name("Celentano").build(),
            Pizzeria.builder().id("pizza-3").name("Papa John's").build(),
            Pizzeria.builder().id("pizza-4").name("Dodo Pizza").build()
    );

    public static boolean exists(String id) {
        return ALL.stream().anyMatch(p -> p.getId().equals(id));
    }
}
