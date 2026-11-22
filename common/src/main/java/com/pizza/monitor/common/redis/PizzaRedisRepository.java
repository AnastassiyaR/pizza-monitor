package com.pizza.monitor.common.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pizza.monitor.common.model.Alert;
import com.pizza.monitor.common.model.AlertType;
import com.pizza.monitor.common.model.PizzaMetric;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class PizzaRedisRepository {

    public static final Duration LATEST_TTL = Duration.ofSeconds(60);
    public static final int HISTORY_LIMIT = 100;
    public static final int RECENT_ALERTS_LIMIT = 50;

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public void saveMetric(PizzaMetric metric) {
        String json = toJson(metric);
        String id = metric.getPizzeriaId();

        inTransaction(ops -> {
            ops.opsForValue().set(RedisKeys.latest(id), json, LATEST_TTL);
            ops.opsForList().leftPush(RedisKeys.history(id), json);
            ops.opsForList().trim(RedisKeys.history(id), 0, HISTORY_LIMIT - 1);
        });
    }

    public Map<String, PizzaMetric> findLatest(Collection<String> pizzeriaIds) {
        List<String> ids = List.copyOf(pizzeriaIds);
        List<String> keys = ids.stream().map(RedisKeys::latest).toList();
        List<String> raw = redis.opsForValue().multiGet(keys);

        Map<String, PizzaMetric> result = new LinkedHashMap<>();
        if (raw == null) {
            return result;
        }
        for (int i = 0; i < ids.size(); i++) {
            PizzaMetric metric = fromJson(raw.get(i), PizzaMetric.class);
            if (metric != null) {
                result.put(ids.get(i), metric);
            }
        }
        return result;
    }

    public List<PizzaMetric> findHistory(String pizzeriaId, int limit) {
        List<String> raw = redis.opsForList().range(RedisKeys.history(pizzeriaId), 0, Math.max(limit, 1) - 1);
        return parseAll(raw, PizzaMetric.class);
    }

   public void saveAlert(Alert alert) {
        String json = toJson(alert);

        inTransaction(ops -> {
            ops.opsForList().leftPush(RedisKeys.RECENT_ALERTS, json);
            ops.opsForList().trim(RedisKeys.RECENT_ALERTS, 0, RECENT_ALERTS_LIMIT - 1);
            ops.opsForValue().increment(RedisKeys.alertCount(alert.getPizzeriaId()));
        });
    }

    public List<Alert> findRecentAlerts(int limit) {
        List<String> raw = redis.opsForList().range(RedisKeys.RECENT_ALERTS, 0, Math.max(limit, 1) - 1);
        return parseAll(raw, Alert.class);
    }

    public boolean tryAcquireAlertSlot(String pizzeriaId, AlertType type, Duration cooldown) {
        Boolean acquired = redis.opsForValue()
                .setIfAbsent(RedisKeys.alertCooldown(pizzeriaId, type.name()), "1", cooldown);
        return Boolean.TRUE.equals(acquired);
    }

    public boolean isAlive() {
        try {
            String pong = redis.execute((RedisCallback<String>) connection -> connection.ping());
            return "PONG".equalsIgnoreCase(pong);
        } catch (RuntimeException e) {
            log.warn("Redis PING failed: {}", e.getMessage());
            return false;
        }
    }

    private void inTransaction(Consumer<RedisOperations<String, String>> commands) {
        redis.execute(new SessionCallback<List<Object>>() {
            @Override
            @SuppressWarnings("unchecked")
            public <K, V> List<Object> execute(@Nonnull RedisOperations<K, V> operations) throws DataAccessException {
                RedisOperations<String, String> ops = (RedisOperations<String, String>) operations;
                ops.multi();
                commands.accept(ops);
                return ops.exec();
            }
        });
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize " + value.getClass().getSimpleName(), e);
        }
    }

    private <T> T fromJson(String json, Class<T> type) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            log.warn("Skipping invalid JSON in Redis for {}: {}", type.getSimpleName(), e.getOriginalMessage());
            return null;
        }
    }

    private <T> List<T> parseAll(List<String> raw, Class<T> type) {
        List<T> result = new ArrayList<>();
        if (raw == null) {
            return result;
        }
        for (String json : raw) {
            T value = fromJson(json, type);
            if (Objects.nonNull(value)) {
                result.add(value);
            }
        }
        return result;
    }
}
