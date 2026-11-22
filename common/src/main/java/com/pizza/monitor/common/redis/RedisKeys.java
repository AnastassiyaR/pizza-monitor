package com.pizza.monitor.common.redis;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RedisKeys {

    public final String RECENT_ALERTS = "alerts:recent";

    public String latest(String pizzeriaId) {
        return "pizzeria:" + pizzeriaId + ":latest";
    }

    public String history(String pizzeriaId) {
        return "pizzeria:" + pizzeriaId + ":history";
    }

    public String alertCount(String pizzeriaId) {
        return "alerts:count:" + pizzeriaId;
    }

    public String alertCooldown(String pizzeriaId, String alertType) {
        return "alerts:cooldown:" + pizzeriaId + ":" + alertType;
    }
}
