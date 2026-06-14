package com.pfws.worldrandomevents.event.disaster;

import com.pfws.worldrandomevents.event.BaseEvent;
import com.pfws.worldrandomevents.network.NetworkHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;

import java.util.List;

public class BloodMoon extends BaseEvent {

    public BloodMoon() {
        super("blood_moon", "血月", EventType.DISASTER);
    }

    @Override public int getBaseTriggerInterval() { return 31; }
    @Override public double getBaseTriggerChance() { return 1.0; }
    @Override public int getBaseDurationTicks() { return 13000; }
    @Override public int getBaseCooldownTicks() { return daysToTicks(31); }
    @Override public int getBaseForceTriggerDays() { return 31; }

    @Override
    protected String getStartTitle() { return "§4§l血月升起..."; }
    @Override
    protected String getStartSubtitle() { return "§c夜晚将充满危险与机遇"; }

    @Override
    protected void onStart(List<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            NetworkHandler.sendSkyEffect(player, 0xFF330000, 0xFF220000);
        }
    }

    @Override
    protected void onTick(List<ServerPlayer> players) {
        if (ticksSinceStart % 100 == 0) {
            level.getAllEntities().forEach(entity -> {
                if (entity instanceof Monster monster && !monster.hasEffect(MobEffects.STRENGTH)) {
                    monster.addEffect(new MobEffectInstance(MobEffects.STRENGTH, -1, 2, false, true, true));
                    monster.addEffect(new MobEffectInstance(MobEffects.SPEED, -1, 1, false, true, true));
                    monster.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, -1, 0, false, true, true));
                }
            });
        }
    }

    @Override
    protected void onEnd(List<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            NetworkHandler.sendSkyEffect(player, 0, 0);
        }
    }
}