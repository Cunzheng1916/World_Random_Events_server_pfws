package com.pfws.worldrandomevents.event.disaster;

import com.pfws.worldrandomevents.WorldRandomEvents;
import com.pfws.worldrandomevents.event.BaseEvent;
import com.pfws.worldrandomevents.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class PiglinInvasion extends BaseEvent {
    private final List<InvasionGroup> invasionGroups = new ArrayList<>();
    private int tickCounter = 0;

    public PiglinInvasion() {
        super("piglin_invasion", "Piglin Invasion", EventType.DISASTER);
    }

    @Override public int getBaseTriggerInterval() { return 3; }
    @Override public double getBaseTriggerChance() { return 0.30; }
    @Override public int getBaseDurationTicks() { return daysToTicks(12); }
    @Override public int getBaseCooldownTicks() { return daysToTicks(3); }
    @Override public int getBaseForceTriggerDays() { return 45; }

    @Override
    protected String getStartTitle() { return "§c§l猪灵入侵!"; }
    @Override
    protected String getStartSubtitle() { return "§6下界大军已抵达主世界"; }

    @Override
    protected void onStart(List<ServerPlayer> players) {
        spawnInvasionGroups(players);
        for (ServerPlayer player : players) {
            NetworkHandler.sendSkyEffect(player, 0xFF4A0000, 0xFF3A0000);
        }
    }

    @Override
    protected void onTick(List<ServerPlayer> players) {
        tickCounter++;

        if (tickCounter % 2400 == 0) {
            for (ServerPlayer player : players) {
                player.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 0, false, true, true));
            }
        }

        if (tickCounter % 200 == 0) {
            for (ServerPlayer player : players) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, -1, 0, false, true, true));
            }
        }

        for (InvasionGroup group : invasionGroups) {
            group.tick(level);
        }
    }

    @Override
    protected void onEnd(List<ServerPlayer> players) {
        for (InvasionGroup group : invasionGroups) {
            group.cleanupMobs();
        }
        invasionGroups.clear();

        for (ServerPlayer player : players) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, daysToTicks(2), 0, false, true, true));
            NetworkHandler.sendSkyEffect(player, 0, 0);
        }
    }

    private void spawnInvasionGroups(List<ServerPlayer> players) {
        if (players.isEmpty()) return;
        Random random = new Random();

        ServerPlayer target = players.get(random.nextInt(players.size()));
        for (int g = 0; g < 2; g++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = 30 + random.nextDouble() * 50;
            double x = target.getX() + Math.cos(angle) * dist;
            double z = target.getZ() + Math.sin(angle) * dist;
            BlockPos spawnPos = findSurfacePos(level, (int) x, (int) z);
            if (spawnPos == null) continue;

            InvasionGroup group = new InvasionGroup(spawnPos);
            group.spawn(level, random);
            invasionGroups.add(group);
        }
    }

    private BlockPos findSurfacePos(ServerLevel level, int x, int z) {
        for (int y = level.getMaxY() - 1; y > level.getMinY(); y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above()) && level.isEmptyBlock(pos.above(2))) {
                return pos.above();
            }
        }
        return null;
    }

    private static class InvasionGroup {
        private final BlockPos center;
        private final List<ZombifiedPiglin> piglins = new ArrayList<>();
        private final List<Zoglin> zoglins = new ArrayList<>();
        private final List<Blaze> blazes = new ArrayList<>();

        InvasionGroup(BlockPos center) { this.center = center; }

        void spawn(ServerLevel level, Random random) {
            int piglinCount = 9 + random.nextInt(12);
            int zoglinCount = 3 + random.nextInt(5);

            for (int i = 0; i < piglinCount; i++) {
                BlockPos pos = randomOffset(center, random);
                ZombifiedPiglin piglin = EntityType.ZOMBIFIED_PIGLIN.create(level, null, pos, EntitySpawnReason.EVENT, false, false);
                if (piglin != null) {
                    piglin.setCustomName(Component.literal("猪灵前沿部队"));
                    piglin.setCustomNameVisible(true);
                    piglin.setGlowingTag(true);
                    piglin.addEffect(new MobEffectInstance(MobEffects.STRENGTH, -1, 1, false, true, true));
                    piglin.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, -1, 0, false, true, true));
                    equipRandomArmor(piglin, random);
                    level.addFreshEntity(piglin);
                    piglins.add(piglin);
                }
            }

            for (int i = 0; i < zoglinCount; i++) {
                BlockPos pos = randomOffset(center, random);
                Zoglin zoglin = EntityType.ZOGLIN.create(level, null, pos, EntitySpawnReason.EVENT, false, false);
                if (zoglin != null) {
                    zoglin.addEffect(new MobEffectInstance(MobEffects.STRENGTH, -1, 0, false, true, true));
                    zoglin.addEffect(new MobEffectInstance(MobEffects.SPEED, -1, 0, false, true, true));
                    level.addFreshEntity(zoglin);
                    zoglins.add(zoglin);

                    if (random.nextBoolean()) {
                        Blaze blaze = EntityType.BLAZE.create(level, null, pos, EntitySpawnReason.EVENT, false, false);
                        if (blaze != null) {
                            blaze.startRiding(zoglin);
                            level.addFreshEntity(blaze);
                            blazes.add(blaze);
                        }
                    }
                }
            }
        }

        private void equipRandomArmor(ZombifiedPiglin piglin, Random random) {
            if (random.nextFloat() < 0.3) {
                double roll = random.nextDouble();
                if (roll < 0.2) {
                    piglin.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                } else if (roll < 0.7) {
                    piglin.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.GOLDEN_CHESTPLATE));
                } else if (roll < 0.8) {
                    piglin.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                } else if (roll < 0.9) {
                    piglin.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                } else {
                    piglin.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
                }
            }
        }

        void tick(ServerLevel level) {
            for (ZombifiedPiglin piglin : piglins) {
                if (piglin.isAlive() && piglin.distanceToSqr(Vec3.atCenterOf(center)) > 2500) {
                    piglin.teleportTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
                }
            }
            for (Zoglin zoglin : zoglins) {
                if (zoglin.isAlive() && zoglin.distanceToSqr(Vec3.atCenterOf(center)) > 2500) {
                    zoglin.teleportTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
                }
            }
        }

        void cleanupMobs() {
            for (ZombifiedPiglin p : piglins) { if (p.isAlive()) p.discard(); }
            for (Zoglin z : zoglins) { if (z.isAlive()) z.discard(); }
            for (Blaze b : blazes) { if (b.isAlive()) b.discard(); }
        }

        private static BlockPos randomOffset(BlockPos center, Random random) {
            return center.offset(
                random.nextInt(21) - 10,
                0,
                random.nextInt(21) - 10
            );
        }
    }
}