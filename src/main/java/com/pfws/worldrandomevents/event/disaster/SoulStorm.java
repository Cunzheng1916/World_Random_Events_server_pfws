package com.pfws.worldrandomevents.event.disaster;

import com.pfws.worldrandomevents.network.NetworkHandler;
import com.pfws.worldrandomevents.event.BaseEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.Blocks;

import java.util.*;

public class SoulStorm extends BaseEvent {
    private final Map<String, SoulCage> soulCages = new HashMap<>();
    private int tickCounter = 0;

    public SoulStorm() {
        super("soul_storm", "灵魂风暴", EventType.DISASTER);
    }

    @Override public int getBaseTriggerInterval() { return 8; }
    @Override public double getBaseTriggerChance() { return 0.25; }
    @Override public int getBaseDurationTicks() { return daysToTicks(3); }
    @Override public int getBaseCooldownTicks() { return daysToTicks(8); }
    @Override public int getBaseForceTriggerDays() { return 40; }

    @Override protected String getStartTitle() { return "§2§l灵魂风暴降临..."; }
    @Override protected String getStartSubtitle() { return "§8亡灵在黑暗中苏醒"; }

    @Override
    protected void onStart(List<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, -1, 0, false, true, true));
            NetworkHandler.sendSkyEffect(player, 0xFF1A3A1A, 0xFF0A2A0A);
        }
    }

    @Override
    protected void onTick(List<ServerPlayer> players) {
        tickCounter++;

        if (tickCounter % 100 == 0) {
            level.getAllEntities().forEach(entity -> {
                if (entity instanceof Monster && entity instanceof LivingEntity le && !le.hasEffect(MobEffects.SPEED)) {
                    le.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 0, false, true, true));
                    le.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, 0, false, true, true));
                }
            });
        }

        if (tickCounter % 1200 == 0) {
            for (ServerPlayer player : players) {
                if (player.getHealth() < 10f) {
                    player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 100, 0, false, true, true));
                    player.hurtServer(level, level.damageSources().magic(), 2f);
                }
            }
        }
    }

    @Override
    protected void onEnd(List<ServerPlayer> players) {
        for (SoulCage cage : soulCages.values()) {
            cage.destroy(level);
        }
        soulCages.clear();
        for (ServerPlayer player : players) {
            player.removeEffect(MobEffects.WEAKNESS);
            NetworkHandler.sendSkyEffect(player, 0, 0);
        }
    }

    public void createSoulCage(BlockPos pos, ServerPlayer deadPlayer) {
        SoulCage cage = new SoulCage(pos, deadPlayer);
        cage.build(level);
        soulCages.put(deadPlayer.getName().getString(), cage);
    }

    public void releaseSoul(String playerName, ServerPlayer rescuer) {
        SoulCage cage = soulCages.remove(playerName);
        if (cage != null) {
            cage.destroy(level);
            if (rescuer != null) {
                rescuer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 1200, 1, false, true, true));
                rescuer.giveExperiencePoints(10);
            }
        }
    }

    public Map<String, SoulCage> getSoulCages() { return soulCages; }

    @Override
    public BlockPos getEventCenter() {
        if (soulCages.isEmpty()) return null;
        return soulCages.values().iterator().next().pos;
    }

    public static class SoulCage {
        public final BlockPos pos;
        public final ServerPlayer deadPlayer;
        public final long createdAt;
        private ArmorStand marker;

        public SoulCage(BlockPos pos, ServerPlayer deadPlayer) {
            this.pos = pos;
            this.deadPlayer = deadPlayer;
            this.createdAt = System.currentTimeMillis();
        }

        public void build(net.minecraft.server.level.ServerLevel level) {
            level.setBlock(pos, Blocks.SOUL_SAND.defaultBlockState(), 3);
            level.setBlock(pos.above(), Blocks.IRON_BARS.defaultBlockState(), 3);
            level.setBlock(pos.above(2), Blocks.IRON_BARS.defaultBlockState(), 3);
            level.setBlock(pos.north(), Blocks.IRON_BARS.defaultBlockState(), 3);
            level.setBlock(pos.south(), Blocks.IRON_BARS.defaultBlockState(), 3);
            level.setBlock(pos.east(), Blocks.IRON_BARS.defaultBlockState(), 3);
            level.setBlock(pos.west(), Blocks.IRON_BARS.defaultBlockState(), 3);
            marker = EntityType.ARMOR_STAND.create(level, null, pos.above(2), EntitySpawnReason.EVENT, false, false);
            if (marker != null) {
                marker.setInvisible(true);
                marker.setNoGravity(true);
                marker.setInvulnerable(true);
                marker.setGlowingTag(true);
                level.addFreshEntity(marker);
            }
        }

        public void destroy(net.minecraft.server.level.ServerLevel level) {
            level.destroyBlock(pos, false, null);
            level.destroyBlock(pos.above(), false, null);
            level.destroyBlock(pos.above(2), false, null);
            level.destroyBlock(pos.north(), false, null);
            level.destroyBlock(pos.south(), false, null);
            level.destroyBlock(pos.east(), false, null);
            level.destroyBlock(pos.west(), false, null);
            if (marker != null && marker.isAlive()) {
                marker.discard();
                marker = null;
            }
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - createdAt > 120000;
        }
    }
}