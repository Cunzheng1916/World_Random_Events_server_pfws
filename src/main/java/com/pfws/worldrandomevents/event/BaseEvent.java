package com.pfws.worldrandomevents.event;

import com.pfws.worldrandomevents.WorldRandomEvents;
import com.pfws.worldrandomevents.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

import java.util.List;

public abstract class BaseEvent {
    protected final String id;
    protected final String displayName;
    protected final EventType eventType;
    protected ServerLevel level;
    protected boolean isActive = false;
    protected int ticksSinceStart = 0;
    protected int remainingTicks = 0;
    protected int totalDurationTicks = 0;
    protected int cooldownTicks = 0;
    protected ServerBossEvent bossBar = null;

    public enum EventType {
        DISASTER(3),
        NEUTRAL(2),
        BLESSING(1);

        public final int priority;
        EventType(int priority) { this.priority = priority; }
    }

    public BaseEvent(String id, String displayName, EventType eventType) {
        this.id = id;
        this.displayName = displayName;
        this.eventType = eventType;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public EventType getEventType() { return eventType; }
    public boolean isActive() { return isActive; }
    public int getCooldownTicks() { return cooldownTicks; }
    public ServerBossEvent getBossBar() { return bossBar; }

    public void setLevel(ServerLevel level) { this.level = level; }

    public boolean canTrigger() {
        if (!WorldRandomEvents.CONFIG.isEventEnabled(id)) return false;
        if (isActive) return false;
        if (cooldownTicks > 0) return false;
        return true;
    }

    public abstract int getBaseTriggerInterval();
    public abstract double getBaseTriggerChance();
    public abstract int getBaseDurationTicks();
    public abstract int getBaseCooldownTicks();
    public abstract int getBaseForceTriggerDays();

    public BlockPos getEventCenter() { return null; }

    public void start(ServerLevel serverLevel, List<ServerPlayer> players) {
        this.level = serverLevel;
        this.isActive = true;
        this.ticksSinceStart = 0;
        this.totalDurationTicks = getBaseDurationTicks();
        this.remainingTicks = totalDurationTicks;

        WorldRandomEvents.LOGGER.info("[WorldRandomEvents] Event started: {} ({})", displayName, id);

        BossEvent.BossBarColor color = switch (eventType) {
            case DISASTER -> BossEvent.BossBarColor.RED;
            case NEUTRAL  -> BossEvent.BossBarColor.BLUE;
            case BLESSING -> BossEvent.BossBarColor.GREEN;
        };
        bossBar = new ServerBossEvent(java.util.UUID.randomUUID(), Component.literal(displayName), color, BossEvent.BossBarOverlay.PROGRESS);
        bossBar.setVisible(true);

        for (ServerPlayer player : players) {
            bossBar.addPlayer(player);
            NetworkHandler.sendEventStart(player, id, remainingTicks);
            NetworkHandler.sendTitleMessage(player,
                getStartTitle(),
                getStartSubtitle(),
                20, 100, 20);
        }

        onStart(players);

        String coordMsg = "";
        BlockPos center = getEventCenter();
        if (center != null) {
            coordMsg = " §7@(" + center.getX() + ", " + center.getY() + ", " + center.getZ() + ")";
        }
        serverLevel.getServer().getPlayerList().broadcastSystemMessage(
            Component.literal("§6[世界事件] §e" + displayName + " §a已开始!" + coordMsg), false);
    }

    public void tick(ServerLevel serverLevel, List<ServerPlayer> players) {
        if (!isActive) return;
        this.level = serverLevel;
        ticksSinceStart++;
        remainingTicks--;

        if (bossBar != null && remainingTicks > 0 && totalDurationTicks > 0) {
            bossBar.setProgress((float) remainingTicks / (float) totalDurationTicks);
        }

        onTick(players);

        if (totalDurationTicks > 0 && remainingTicks <= 0) {
            end(players);
        }
    }

    public void end(List<ServerPlayer> players) {
        WorldRandomEvents.LOGGER.info("[WorldRandomEvents] Event ended: {} ({})", displayName, id);

        String coordMsg = "";
        BlockPos center = getEventCenter();
        if (center != null) {
            coordMsg = " §7@(" + center.getX() + ", " + center.getY() + ", " + center.getZ() + ")";
        }
        level.getServer().getPlayerList().broadcastSystemMessage(
            Component.literal("§6[世界事件] §e" + displayName + " §c已结束!" + coordMsg), false);

        for (ServerPlayer player : players) {
            if (bossBar != null) bossBar.removePlayer(player);
            NetworkHandler.sendEventEnd(player, id);
            NetworkHandler.sendSkyEffect(player, 0, 0);
        }

        if (bossBar != null) {
            bossBar.removeAllPlayers();
            bossBar.setVisible(false);
            bossBar = null;
        }

        onEnd(players);

        this.isActive = false;
        this.cooldownTicks = getBaseCooldownTicks();
        this.ticksSinceStart = 0;
        this.remainingTicks = 0;
    }

    public void tickCooldown() {
        if (cooldownTicks > 0) {
            cooldownTicks--;
        }
    }

    public void forceEnd() {
        if (level != null && isActive) {
            end(level.players());
        }
        isActive = false;
        cooldownTicks = 0;
    }

    public void forceStart(ServerLevel serverLevel) {
        cooldownTicks = 0;
        start(serverLevel, serverLevel.players());
    }

    public void resetCooldown() {
        cooldownTicks = 0;
    }

    protected abstract void onStart(List<ServerPlayer> players);
    protected abstract void onTick(List<ServerPlayer> players);
    protected abstract void onEnd(List<ServerPlayer> players);

    protected abstract String getStartTitle();
    protected abstract String getStartSubtitle();

    protected int minutesToTicks(int minutes) { return minutes * 60 * 20; }
    protected int hoursToTicks(int hours) { return hours * 60 * 60 * 20; }
    protected int daysToTicks(int days) { return days * 24000; }
}