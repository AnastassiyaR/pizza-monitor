package com.pizza.monitor.api;

import com.pizza.monitor.common.model.Alert;
import com.pizza.monitor.common.model.PizzaMetric;
import com.pizza.monitor.common.model.Pizzerias;
import com.pizza.monitor.common.redis.PizzaRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PizzaController {

    private final DashboardService dashboard;
    private final PizzaRedisRepository repository;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        boolean ok = repository.isAlive();
        return ResponseEntity
                .status(ok ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("status", ok ? "ok" : "redis unavailable"));
    }

    @GetMapping("/pizzerias")
    public List<PizzeriaView> pizzerias() {
        return dashboard.pizzerias();
    }

    @GetMapping("/pizzerias/{id}/history")
    public List<PizzaMetric> history(@PathVariable String id,
                                     @RequestParam(defaultValue = "50") int limit) {
        if (!Pizzerias.exists(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pizzeria not found");
        }
        int safeLimit = Math.clamp(limit, 1, 100);
        return repository.findHistory(id, safeLimit);
    }

    @GetMapping("/alerts")
    public List<Alert> alerts() {
        return dashboard.recentAlerts(20);
    }
}
