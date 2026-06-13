package com.pfws.worldrandomevents.config;

import com.google.gson.annotations.SerializedName;

public class EventConfig {
    public enum EventType {
        DISASTER,
        BLESSING,
        NEUTRAL
    }

    @SerializedName("enabled")
    public boolean enabled;

    @SerializedName("type")
    public EventType type;

    @SerializedName("trigger_chance")
    public double triggerChance = 1.0;

    @SerializedName("cooldown_days")
    public int cooldownDays;

    @SerializedName("duration_days")
    public int durationDays;

    @SerializedName("force_trigger_days")
    public int forceTriggerDays;

    public EventConfig(boolean enabled, EventType type) {
        this.enabled = enabled;
        this.type = type;
    }
}