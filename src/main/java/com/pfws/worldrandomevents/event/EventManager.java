package com.pfws.worldrandomevents.event;

import com.pfws.worldrandomevents.WorldRandomEvents;
import com.pfws.worldrandomevents.event.blessing.HarvestFestival;
import com.pfws.worldrandomevents.event.blessing.MeteorShower;
import com.pfws.worldrandomevents.event.disaster.BloodMoon;
import com.pfws.worldrandomevents.event.disaster.FishRain;
import com.pfws.worldrandomevents.event.disaster.PiglinInvasion;
import com.pfws.worldrandomevents.event.neutral.CaravanExpedition;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.*;

public class EventManager {
    private final List<BaseEvent> allEvents = new ArrayList<>();
    private final Map<String, BaseEvent> eventMap = new LinkedHashMap<>();
    private final Map<String, Integer> daysSinceLastTrigger = new HashMap<>();
    private final Map<String, Integer> daysSinceLastCheck = new HashMap<>();
    private long lastDayCheck = -1;
    private BaseEvent currentEvent = null;
    private final LinkedList<BaseEvent> eventQueue = new LinkedList<>();
    private MinecraftServer server = null;
    private final Random random = new Random();

    public void registerAllEvents() {
        register(new BloodMoon());
        register(new PiglinInvasion());
        register(new FishRain());
        register(new MeteorShower());
        register(new HarvestFestival());
        register(new CaravanExpedition());

        ServerLifecycleEvents.SERVER_STARTING.register(s -> server = s);
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
    }

    private void register(BaseEvent event) {
        allEvents.add(event);
        eventMap.put(event.getId(), event);
        daysSinceLastTrigger.put(event.getId(), 0);
        daysSinceLastCheck.put(event.getId(), 0);
    }

    public int getEventCount() {
        return allEvents.size();
    }

    public BaseEvent getCurrentEvent() {
        return currentEvent;
    }

    public BaseEvent getEvent(String id) {
        return eventMap.get(id);
    }

    public Collection<BaseEvent> getAllEvents() {
        return Collections.unmodifiableCollection(allEvents);
    }

    public List<BaseEvent> getQueuedEvents() {
        return Collections.unmodifiableList(eventQueue);
    }

    private void onServerTick(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        List<ServerPlayer> players = overworld.players();

        for (BaseEvent event : allEvents) {
            event.tickCooldown();
        }

        if (currentEvent != null && currentEvent.isActive()) {
            currentEvent.setLevel(overworld);
            currentEvent.tick(overworld, players);
            if (!currentEvent.isActive()) {
                currentEvent = null;
                startNextQueuedEvent(overworld, players);
            }
            return;
        }

        if (currentEvent == null) {
            startNextQueuedEvent(overworld, players);
        }

        if (currentEvent != null && currentEvent.isActive()) return;

        long currentDay = overworld.getOverworldClockTime() / 24000L;
        if (lastDayCheck < 0) {
            lastDayCheck = currentDay;
            return;
        }

        if (currentDay <= lastDayCheck) return;
        lastDayCheck = currentDay;

        for (String id : daysSinceLastTrigger.keySet()) {
            daysSinceLastTrigger.put(id, daysSinceLastTrigger.get(id) + 1);
            daysSinceLastCheck.put(id, daysSinceLastCheck.get(id) + 1);
        }

        if (players.isEmpty()) return;

        if (!checkTriggerConditions(overworld, players)) return;

        tryTriggerEvent(overworld, players);
    }

    private boolean checkTriggerConditions(ServerLevel overworld, List<ServerPlayer> players) {
        boolean anyValid = false;
        for (ServerPlayer player : players) {
            if (player.getY() <= -32) continue;

            AABB box = new AABB(
                player.getX() - 32, player.getY() - 32, player.getZ() - 32,
                player.getX() + 32, player.getY() + 32, player.getZ() + 32
            );
            List<Warden> wardens = overworld.getEntitiesOfClass(Warden.class, box);
            if (!wardens.isEmpty()) continue;

            anyValid = true;
            break;
        }
        return anyValid;
    }

    private void tryTriggerEvent(ServerLevel overworld, List<ServerPlayer> players) {
        for (BaseEvent.EventType type : new BaseEvent.EventType[]{
            BaseEvent.EventType.DISASTER,
            BaseEvent.EventType.NEUTRAL,
            BaseEvent.EventType.BLESSING
        }) {
            List<BaseEvent> candidates = new ArrayList<>();
            for (BaseEvent event : allEvents) {
                if (event.getEventType() != type) continue;
                if (!event.canTrigger()) continue;

                int daysSince = daysSinceLastTrigger.getOrDefault(event.getId(), 0);
                int forceDays = event.getBaseForceTriggerDays();
                boolean forceTrigger = (forceDays > 0 && daysSince >= forceDays);

                if (!forceTrigger) {
                    int daysChecked = daysSinceLastCheck.getOrDefault(event.getId(), 0);
                    if (daysChecked < event.getBaseTriggerInterval()) continue;
                    if (random.nextDouble() > event.getBaseTriggerChance()) continue;
                }
                candidates.add(event);
            }

            if (!candidates.isEmpty()) {
                BaseEvent selected = candidates.get(random.nextInt(candidates.size()));
                daysSinceLastTrigger.put(selected.getId(), 0);

                for (String id : daysSinceLastCheck.keySet()) {
                    daysSinceLastCheck.put(id, 0);
                }

                if (currentEvent != null && currentEvent.isActive()) {
                    eventQueue.add(selected);
                    overworld.getServer().getPlayerList().broadcastSystemMessage(
                        net.minecraft.network.chat.Component.literal(
                            "§6[世界事件] §e" + selected.getDisplayName() + " §7已加入排队，将在当前事件结束后触发"), false);
                } else {
                    selected.start(overworld, players);
                    currentEvent = selected;
                }
                return;
            }
        }
    }

    private void startNextQueuedEvent(ServerLevel overworld, List<ServerPlayer> players) {
        if (!eventQueue.isEmpty()) {
            BaseEvent next = eventQueue.poll();
            if (next.canTrigger()) {
                next.start(overworld, players);
                currentEvent = next;
                overworld.getServer().getPlayerList().broadcastSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                        "§6[世界事件] §e" + next.getDisplayName() + " §a从排队中开始触发!"), false);
            } else {
                startNextQueuedEvent(overworld, players);
            }
        }
    }

    private void onServerStopping(MinecraftServer server) {
        if (currentEvent != null) {
            currentEvent.forceEnd();
            currentEvent = null;
        }
        eventQueue.clear();
    }

    public void forceStartEvent(String eventId) {
        BaseEvent event = eventMap.get(eventId);
        if (event == null) return;
        if (event.isActive()) return;
        if (server == null) return;
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        if (currentEvent != null && currentEvent.isActive()) {
            eventQueue.add(event);
            overworld.getServer().getPlayerList().broadcastSystemMessage(
                net.minecraft.network.chat.Component.literal(
                    "§6[世界事件] §e" + event.getDisplayName() + " §7已加入排队，将在当前事件结束后触发"), false);
        } else {
            event.forceStart(overworld);
            currentEvent = event;
        }
        daysSinceLastTrigger.put(eventId, 0);
    }

    public void forceEndCurrentEvent() {
        if (currentEvent != null) {
            currentEvent.forceEnd();
            currentEvent = null;
        }
    }

    public void resetEventCooldown(String eventId) {
        BaseEvent event = eventMap.get(eventId);
        if (event != null) {
            event.resetCooldown();
        }
    }
}