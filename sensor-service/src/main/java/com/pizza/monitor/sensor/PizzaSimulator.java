package com.pizza.monitor.sensor;

import com.pizza.monitor.common.model.PizzaMetric;
import com.pizza.monitor.common.model.Pizzeria;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class PizzaSimulator {

    private static final double NORMAL_TEMP = 295;

    @Data
    @AllArgsConstructor
    private static class State {
        private double temperature;
        private double doughStock;
    }

    private final Map<String, State> states = new ConcurrentHashMap<>();

    public PizzaMetric next(Pizzeria pizzeria) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        State state = states.computeIfAbsent(pizzeria.getId(),
                id -> new State(NORMAL_TEMP, rnd.nextDouble(60, 100)));

        double spike = rnd.nextDouble() < 0.01 ? rnd.nextDouble(10, 25) : 0;
        double temp = state.getTemperature()
                + (NORMAL_TEMP - state.getTemperature()) * 0.15
                + rnd.nextDouble(-4, 4)
                + spike;
        state.setTemperature(clamp(temp));

        double dough = state.getDoughStock() - rnd.nextDouble(0, 1.2);
        if (dough < 18 && rnd.nextDouble() < 0.35) {
            dough = rnd.nextDouble(80, 100);
        }
        state.setDoughStock(Math.max(0, dough));

        return PizzaMetric.builder()
                .pizzeriaId(pizzeria.getId())
                .pizzeriaName(pizzeria.getName())
                .temperature(Math.round(state.getTemperature() * 10) / 10.0)
                .ordersPerHour(rnd.nextInt(5, 30))
                .doughStock((int) Math.round(state.getDoughStock()))
                .employeesOnDuty(rnd.nextInt(2, 8))
                .timestamp(Instant.now())
                .build();
    }

    private static double clamp(double value) {
        return Math.clamp(270, value, 330);
    }
}
