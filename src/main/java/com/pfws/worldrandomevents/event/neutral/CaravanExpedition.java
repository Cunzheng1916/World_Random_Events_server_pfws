package com.pfws.worldrandomevents.event.neutral;

import com.pfws.worldrandomevents.event.BaseEvent;
import com.pfws.worldrandomevents.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class CaravanExpedition extends BaseEvent {
    private final List<TraderGroup> groups = new ArrayList<>();

    public CaravanExpedition() {
        super("caravan_expedition", "Caravan Expedition", EventType.NEUTRAL);
    }

    @Override public int getBaseTriggerInterval() { return 6; }
    @Override public double getBaseTriggerChance() { return 0.40; }
    @Override public int getBaseDurationTicks() { return daysToTicks(2 + new Random().nextInt(4)); }
    @Override public int getBaseCooldownTicks() { return daysToTicks(6); }
    @Override public int getBaseForceTriggerDays() { return 60; }

    @Override protected String getStartTitle() { return "§e§l远征商队到来!"; }
    @Override protected String getStartSubtitle() { return "§6远方商人带来了珍稀货物"; }

    @Override
    protected void onStart(List<ServerPlayer> players) {
        spawnGroups(players);
    }

    @Override
    protected void onTick(List<ServerPlayer> players) {
        for (TraderGroup group : groups) {
            group.tick(level);
        }
    }

    @Override
    protected void onEnd(List<ServerPlayer> players) {
        for (TraderGroup group : groups) {
            group.cleanup();
        }
        groups.clear();
    }

    private void spawnGroups(List<ServerPlayer> players) {
        if (players.isEmpty()) return;
        Random random = new Random();
        ServerPlayer target = players.get(random.nextInt(players.size()));

        double angle = random.nextDouble() * Math.PI * 2;
        double dist = 30 + random.nextDouble() * 50;
        int x = (int) (target.getX() + Math.cos(angle) * dist);
        int z = (int) (target.getZ() + Math.sin(angle) * dist);
        BlockPos ground = findSurface(x, z);
        if (ground == null) return;

        TraderGroup group = new TraderGroup(ground);
        group.spawn(level, random);
        groups.add(group);
    }

    private BlockPos findSurface(int x, int z) {
        for (int y = level.getMaxY() - 1; y > level.getMinY(); y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above())) {
                return pos.above();
            }
        }
        return null;
    }

    private static class TraderGroup {
        private final BlockPos center;
        private final List<WanderingTrader> traders = new ArrayList<>();
        private final List<IronGolem> meleeGuards = new ArrayList<>();
        private final List<SnowGolem> rangedGuards = new ArrayList<>();

        TraderGroup(BlockPos center) { this.center = center; }

        void spawn(net.minecraft.server.level.ServerLevel level, Random random) {
            int traderCount = 5 + random.nextInt(3);
            int meleeCount = 2 + random.nextInt(3);
            int rangedCount = 4 + random.nextInt(3);

            for (int i = 0; i < traderCount; i++) {
                BlockPos pos = randomOffset(center, random);
                WanderingTrader trader = EntityType.WANDERING_TRADER.create(level, null, pos, EntitySpawnReason.EVENT, false, false);
                if (trader != null) {
                    trader.setCustomName(Component.literal("远征商人"));
                    trader.setCustomNameVisible(true);
                    trader.setGlowingTag(true);
                    trader.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, -1, 1, false, true, true));
                    trader.addEffect(new MobEffectInstance(MobEffects.REGENERATION, -1, 1, false, true, true));
                    level.addFreshEntity(trader);
                    traders.add(trader);
                }
            }

            for (int i = 0; i < meleeCount; i++) {
                BlockPos pos = randomOffset(center, random);
                IronGolem golem = EntityType.IRON_GOLEM.create(level, null, pos, EntitySpawnReason.EVENT, false, false);
                if (golem != null) {
                    golem.setCustomName(Component.literal("远征守卫-近战"));
                    golem.setCustomNameVisible(true);
                    golem.addEffect(new MobEffectInstance(MobEffects.SPEED, -1, 2, false, true, true));
                    golem.addEffect(new MobEffectInstance(MobEffects.STRENGTH, -1, 4, false, true, true));
                    golem.addEffect(new MobEffectInstance(MobEffects.REGENERATION, -1, 0, false, true, true));
                    if (golem.getMaxHealth() < 400) {
                        golem.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(400);
                        golem.setHealth(400);
                    }
                    level.addFreshEntity(golem);
                    meleeGuards.add(golem);
                }
            }

            for (int i = 0; i < rangedCount; i++) {
                BlockPos pos = randomOffset(center, random);
                SnowGolem snowman = EntityType.SNOW_GOLEM.create(level, null, pos, EntitySpawnReason.EVENT, false, false);
                if (snowman != null) {
                    snowman.setCustomName(Component.literal("远征守卫-远程"));
                    snowman.setCustomNameVisible(true);
                    snowman.addEffect(new MobEffectInstance(MobEffects.SPEED, -1, 0, false, true, true));
                    snowman.addEffect(new MobEffectInstance(MobEffects.STRENGTH, -1, 5, false, true, true));
                    level.addFreshEntity(snowman);
                    rangedGuards.add(snowman);
                }
            }
        }

        void tick(net.minecraft.server.level.ServerLevel level) {
            Vec3 centerVec = Vec3.atCenterOf(center);
            for (WanderingTrader trader : traders) {
                if (trader.isAlive() && trader.distanceToSqr(centerVec) > 400) {
                    trader.teleportTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
                }
            }
            for (IronGolem golem : meleeGuards) {
                if (golem.isAlive() && golem.distanceToSqr(centerVec) > 2500) {
                    golem.teleportTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
                }
            }
            for (SnowGolem snowman : rangedGuards) {
                if (snowman.isAlive() && snowman.distanceToSqr(centerVec) > 2500) {
                    snowman.teleportTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
                }
            }
        }

        void cleanup() {
            for (WanderingTrader t : traders) { if (t.isAlive()) t.discard(); }
            for (IronGolem g : meleeGuards) { if (g.isAlive()) g.discard(); }
            for (SnowGolem s : rangedGuards) { if (s.isAlive()) s.discard(); }
        }

        private static BlockPos randomOffset(BlockPos center, Random random) {
            return center.offset(
                random.nextInt(11) - 5,
                0,
                random.nextInt(11) - 5
            );
        }
    }
}