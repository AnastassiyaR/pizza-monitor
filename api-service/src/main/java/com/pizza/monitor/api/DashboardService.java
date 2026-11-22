package com.pizza.monitor.api;

import com.pizza.monitor.common.model.Alert;
import com.pizza.monitor.common.model.PizzaMetric;
import com.pizza.monitor.common.model.Pizzeria;
import com.pizza.monitor.common.model.Pizzerias;
import com.pizza.monitor.common.model.Thresholds;
import com.pizza.monitor.common.redis.PizzaRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PizzaRedisRepository repository;
    private final Thresholds thresholds;

    public List<PizzeriaView> pizzerias() {
        List<String> ids = Pizzerias.ALL.stream().map(Pizzeria::getId).toList();
        Map<String, PizzaMetric> latest = repository.findLatest(ids);

        return Pizzerias.ALL.stream()
                .map(p -> PizzeriaView.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .metrics(latest.get(p.getId()))
                        .build())
                .toList();
    }

    public List<Alert> recentAlerts(int limit) {
        return repository.findRecentAlerts(limit);
    }

    public Snapshot snapshot() {
        return Snapshot.builder()
                .type("snapshot")
                .pizzerias(pizzerias())
                .alerts(recentAlerts(20))
                .thresholds(thresholds)
                .build();
    }
}
