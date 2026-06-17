package com.pfws.worldrandomevents.event.disaster;

import com.pfws.worldrandomevents.WorldRandomEvents;
import com.pfws.worldrandomevents.event.BaseEvent;
import com.pfws.worldrandomevents.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class PiglinInvasion extends BaseEvent {
    private final List<PiglinGroup> groups = new ArrayList<>();
    private int tickCounter = 0;
    public static PiglinInvasion ACTIVE_INSTANCE = null;

    public PiglinInvasion() {
        super("piglin_invasion", "猪灵入侵", EventType.DISASTER);
    }

    @Override public int getBaseTriggerInterval() { return 3; }
    @Override public double getBaseTriggerChance() { return 0.30; }
    @Override public int getBaseDurationTicks() { return daysToTicks(12); }
    @Override public int getBaseCooldownTicks() { return daysToTicks(3); }
    @Override public int getBaseForceTriggerDays() { return 45; }

    @Override protected String getStartTitle() { return "§c§l猪灵入侵!"; }
    @Override protected String getStartSubtitle() { return "§6下界大军已抵达主世界"; }

    @Override
    protected void onStart(List<ServerPlayer> players) {
        ACTIVE_INSTANCE = this;
        spawnGroups(players);
        for (ServerPlayer player : players) {
            NetworkHandler.sendSkyEffect(player, 0xFF4A0000, 0xFF3A0000);
        }
    }

    @Override
    protected void onTick(List<ServerPlayer> players) {
        tickCounter++;

        // 清理死亡引用并检查是否所有生物已死
        cleanupDeadRefs();
        if (areAllGroupsDead()) {
            // 所有入侵生物被消灭，强制剩余时间为0让BaseEvent触发end()
            remainingTicks = 0;
            return;
        }

        for (ServerPlayer player : players) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, -1, 0, false, true, true));
        }

        if (tickCounter % 2400 == 0) {
            for (ServerPlayer player : players) {
                player.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 0, false, true, true));
            }
        }

        for (PiglinGroup group : groups) {
            group.tick(level);
        }
    }

    @Override
    protected void onEnd(List<ServerPlayer> players) {
        ACTIVE_INSTANCE = null;
        for (PiglinGroup group : groups) {
            group.cleanup(level);
        }
        groups.clear();

        for (ServerPlayer player : players) {
            player.removeEffect(MobEffects.WEAKNESS);
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, daysToTicks(2), 0, false, true, true));
            NetworkHandler.sendSkyEffect(player, 0, 0);
        }
    }

    public boolean isInvasionMob(Entity entity) {
        for (PiglinGroup group : groups) {
            if (group.containsEntity(entity)) return true;
        }
        return false;
    }

    private void cleanupDeadRefs() {
        for (PiglinGroup group : groups) {
            group.removeDead();
        }
    }

    private boolean areAllGroupsDead() {
        if (groups.isEmpty()) return false;
        for (PiglinGroup group : groups) {
            if (!group.isAllDead()) return false;
        }
        return true;
    }

    @Override
    public BlockPos getEventCenter() {
        if (groups.isEmpty()) return null;
        return groups.get(0).center;
    }

    private void spawnGroups(List<ServerPlayer> players) {
        if (players.isEmpty()) return;
        Random random = new Random();

        // 仿照远征商队：选取一个随机玩家，在其2400格范围内寻找安全地表
        ServerPlayer target = players.get(random.nextInt(players.size()));
        BlockPos spawnPos = findRandomSafeSurface(level, target.getX(), target.getZ(), 2400, random);
        if (spawnPos == null) {
            spawnPos = findSurface(level, (int) target.getX(), (int) target.getZ());
        }
        if (spawnPos == null) return;

        PiglinGroup group = new PiglinGroup(spawnPos);
        group.spawn(level, random);
        groups.add(group);
    }

    private static BlockPos findRandomSafeSurface(ServerLevel level, double cx, double cz,
                                                   int radius, Random random) {
        for (int attempt = 0; attempt < 30; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = random.nextDouble() * radius;
            int x = (int) (cx + Math.cos(angle) * dist);
            int z = (int) (cz + Math.sin(angle) * dist);
            BlockPos pos = findSurface(level, x, z);
            if (pos != null && isSpawnSafe(level, pos)) return pos;
        }
        return null;
    }

    private static BlockPos findSurface(ServerLevel level, int x, int z) {
        for (int y = level.getMaxY() - 1; y > level.getMinY(); y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState ground = level.getBlockState(pos);
            if (ground.isFaceSturdy(level, pos, Direction.UP)
                && ground.getFluidState().isEmpty()
                && !ground.is(net.minecraft.tags.BlockTags.LOGS)
                && !ground.is(net.minecraft.tags.BlockTags.LEAVES)
                && level.isEmptyBlock(pos.above())
                && level.canSeeSky(pos.above())) {
                return pos.above();
            }
        }
        return null;
    }

    private static boolean isSpawnSafe(ServerLevel level, BlockPos pos) {
        return level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above())
            && level.canSeeSky(pos);
    }

    public static class PiglinGroup {
        private final BlockPos center;
        private final List<ZombifiedPiglin> piglins = new ArrayList<>();
        private final List<Zoglin> zoglins = new ArrayList<>();
        private final List<Blaze> blazes = new ArrayList<>();
        private boolean hasSpawned = false;

        PiglinGroup(BlockPos center) { this.center = center; }

        void spawn(ServerLevel level, Random random) {
            int piglinCount = 9 + random.nextInt(12);
            int zoglinCount = 3 + random.nextInt(5);

            for (int i = 0; i < piglinCount; i++) {
                BlockPos pos = randomOffset(center, random);
                ZombifiedPiglin piglin = EntityType.ZOMBIFIED_PIGLIN.create(
                    level, null, pos, EntitySpawnReason.EVENT, false, false);
                if (piglin != null) {
                    piglin.setCustomName(Component.literal("猪灵前沿部队"));
                    piglin.setCustomNameVisible(true);
                    piglin.setGlowingTag(true);
                    piglin.setPersistenceRequired();
                    applyRandomArmor(piglin, random, level);
                    piglin.addEffect(new MobEffectInstance(MobEffects.STRENGTH, -1, 1, false, true, true));
                    piglin.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, -1, 0, false, true, true));
                    level.addFreshEntity(piglin);
                    piglins.add(piglin);
                }
            }

            for (int i = 0; i < zoglinCount; i++) {
                BlockPos pos = randomOffset(center, random);
                Zoglin zoglin = EntityType.ZOGLIN.create(
                    level, null, pos, EntitySpawnReason.EVENT, false, false);
                if (zoglin != null) {
                    zoglin.setCustomName(Component.literal("猪灵前沿部队"));
                    zoglin.setCustomNameVisible(true);
                    zoglin.setGlowingTag(true);
                    zoglin.setPersistenceRequired();
                    zoglin.addEffect(new MobEffectInstance(MobEffects.STRENGTH, -1, 0, false, true, true));
                    zoglin.addEffect(new MobEffectInstance(MobEffects.SPEED, -1, 0, false, true, true));
                    level.addFreshEntity(zoglin);
                    zoglins.add(zoglin);

                    if (random.nextFloat() < 0.50f) {
                        Blaze blaze = EntityType.BLAZE.create(
                            level, null, pos, EntitySpawnReason.EVENT, false, false);
                        if (blaze != null) {
                            blaze.setPersistenceRequired();
                            blaze.setGlowingTag(true);
                            level.addFreshEntity(blaze);
                            blaze.startRiding(zoglin);
                            blazes.add(blaze);
                        }
                    }
                }
            }
            hasSpawned = true;
        }

        private void applyRandomArmor(ZombifiedPiglin piglin, Random random, ServerLevel level) {
            var enchantLookup = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT);
            for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                if (random.nextFloat() < 0.30f) {
                    ItemStack armor = generateArmorPiece(slot, random, level);
                    piglin.setItemSlot(slot, armor);
                    piglin.setDropChance(slot, 0.5f);
                }
            }
        }

        private ItemStack generateArmorPiece(EquipmentSlot slot, Random random, ServerLevel level) {
            float roll = random.nextFloat();
            ItemStack armor;
            if (roll < 0.20f) {
                armor = switch (slot) {
                    case HEAD -> new ItemStack(Items.IRON_HELMET);
                    case CHEST -> new ItemStack(Items.IRON_CHESTPLATE);
                    case LEGS -> new ItemStack(Items.IRON_LEGGINGS);
                    case FEET -> new ItemStack(Items.IRON_BOOTS);
                    default -> ItemStack.EMPTY;
                };
            } else if (roll < 0.70f) {
                armor = switch (slot) {
                    case HEAD -> new ItemStack(Items.GOLDEN_HELMET);
                    case CHEST -> new ItemStack(Items.GOLDEN_CHESTPLATE);
                    case LEGS -> new ItemStack(Items.GOLDEN_LEGGINGS);
                    case FEET -> new ItemStack(Items.GOLDEN_BOOTS);
                    default -> ItemStack.EMPTY;
                };
            } else if (roll < 0.80f) {
                armor = switch (slot) {
                    case HEAD -> new ItemStack(Items.DIAMOND_HELMET);
                    case CHEST -> new ItemStack(Items.DIAMOND_CHESTPLATE);
                    case LEGS -> new ItemStack(Items.DIAMOND_LEGGINGS);
                    case FEET -> new ItemStack(Items.DIAMOND_BOOTS);
                    default -> ItemStack.EMPTY;
                };
            } else if (roll < 0.90f) {
                armor = switch (slot) {
                    case HEAD -> new ItemStack(Items.DIAMOND_HELMET);
                    case CHEST -> new ItemStack(Items.DIAMOND_CHESTPLATE);
                    case LEGS -> new ItemStack(Items.DIAMOND_LEGGINGS);
                    case FEET -> new ItemStack(Items.DIAMOND_BOOTS);
                    default -> ItemStack.EMPTY;
                };
                var enchHolder = level.registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.PROTECTION);
                armor.enchant(enchHolder, 1 + random.nextInt(3));
            } else {
                armor = switch (slot) {
                    case HEAD -> new ItemStack(Items.NETHERITE_HELMET);
                    case CHEST -> new ItemStack(Items.NETHERITE_CHESTPLATE);
                    case LEGS -> new ItemStack(Items.NETHERITE_LEGGINGS);
                    case FEET -> new ItemStack(Items.NETHERITE_BOOTS);
                    default -> ItemStack.EMPTY;
                };
            }
            return armor;
        }

        void tick(ServerLevel level) {
            net.minecraft.world.phys.Vec3 centerVec = net.minecraft.world.phys.Vec3.atCenterOf(center);
            for (ZombifiedPiglin p : piglins) {
                if (p.isAlive() && p.distanceToSqr(centerVec) > 2500) {
                    p.teleportTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
                }
            }
            for (Zoglin z : zoglins) {
                if (z.isAlive() && z.distanceToSqr(centerVec) > 2500) {
                    z.teleportTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
                }
            }
            for (Blaze b : blazes) {
                if (b.isAlive() && b.distanceToSqr(centerVec) > 2500) {
                    b.teleportTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
                }
            }
        }

        boolean containsEntity(Entity entity) {
            return piglins.contains(entity) || zoglins.contains(entity) || blazes.contains(entity);
        }

        void removeDead() {
            piglins.removeIf(p -> !p.isAlive());
            zoglins.removeIf(z -> !z.isAlive());
            blazes.removeIf(b -> !b.isAlive());
        }

        boolean isAllDead() {
            if (!hasSpawned) return false;
            int aliveCount = (int) piglins.stream().filter(ZombifiedPiglin::isAlive).count()
                + (int) zoglins.stream().filter(Zoglin::isAlive).count()
                + (int) blazes.stream().filter(Blaze::isAlive).count();
            return aliveCount == 0;
        }

        void cleanup(ServerLevel level) {
            for (ZombifiedPiglin p : piglins) { if (p.isAlive()) p.discard(); }
            for (Zoglin z : zoglins) { if (z.isAlive()) z.discard(); }
            for (Blaze b : blazes) { if (b.isAlive()) b.discard(); }
        }

        private static BlockPos randomOffset(BlockPos center, Random random) {
            return center.offset(random.nextInt(17) - 8, 0, random.nextInt(17) - 8);
        }
    }
}