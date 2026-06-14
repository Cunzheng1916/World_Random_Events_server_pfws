package com.pfws.worldrandomevents.event.neutral;

import com.pfws.worldrandomevents.WorldRandomEvents;
import com.pfws.worldrandomevents.event.BaseEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class CaravanExpedition extends BaseEvent {
    public static CaravanExpedition ACTIVE_INSTANCE = null;

    private final List<CaravanGroup> groups = new ArrayList<>();
    private BlockPos eventCenter = null;

    public CaravanExpedition() {
        super("caravan_expedition", "远征商队", EventType.NEUTRAL);
    }

    @Override public int getBaseTriggerInterval() { return 6; }
    @Override public double getBaseTriggerChance() { return 0.40; }
    @Override public int getBaseDurationTicks() { return daysToTicks(2 + new Random().nextInt(4)); }
    @Override public int getBaseCooldownTicks() { return daysToTicks(6); }
    @Override public int getBaseForceTriggerDays() { return 60; }

    @Override
    public BlockPos getEventCenter() {
        return eventCenter;
    }

    @Override
    protected String getStartTitle() { return "§e§l远征商队到来!"; }
    @Override
    protected String getStartSubtitle() { return "§6远方商人带来了珍稀货物"; }

    @Override
    protected void onStart(List<ServerPlayer> players) {
        ACTIVE_INSTANCE = this;
        groups.clear();
        if (players.isEmpty()) return;
        Random random = new Random();

        ServerPlayer p1 = players.get(random.nextInt(players.size()));
        ServerPlayer p2 = players.size() > 1 ? players.get(random.nextInt(players.size())) : p1;

        double cx = (p1.getX() + p2.getX()) / 2.0;
        double cz = (p1.getZ() + p2.getZ()) / 2.0;

        BlockPos spawnPos = findRandomSafeSurface(level, cx, cz, 2400, random);
        if (spawnPos == null) {
            spawnPos = findSurface(level, (int) cx, (int) cz);
        }
        if (spawnPos == null) return;

        eventCenter = spawnPos;

        CaravanGroup group = new CaravanGroup(spawnPos);
        group.spawn(level, random);
        groups.add(group);
    }

    @Override
    protected void onTick(List<ServerPlayer> players) {
        Iterator<CaravanGroup> iter = groups.iterator();
        while (iter.hasNext()) {
            CaravanGroup group = iter.next();
            group.tick(level);
            if (group.allTradersDead()) {
                group.cleanup(level);
                iter.remove();
            }
        }
        if (groups.isEmpty()) {
            remainingTicks = 0;
        }
    }

    @Override
    protected void onEnd(List<ServerPlayer> players) {
        for (CaravanGroup group : groups) {
            group.cleanup(level);
        }
        groups.clear();

        // 二次清理：扫描残留的商队实体并强制移除
        forceCleanupRemainingEntities();

        eventCenter = null;
        ACTIVE_INSTANCE = null;
    }

    /** 事件结束后扫描并清理所有残留的商队实体 */
    private void forceCleanupRemainingEntities() {
        if (eventCenter == null) return;
        AABB scanArea = new AABB(
            eventCenter.getX() - 80, level.getMinY(), eventCenter.getZ() - 80,
            eventCenter.getX() + 80, level.getMaxY(), eventCenter.getZ() + 80);

        for (Entity entity : level.getEntitiesOfClass(Entity.class, scanArea)) {
            String name = entity.getCustomName() != null ? entity.getCustomName().getString() : "";
            if (name.contains("远征商人") || name.contains("远征守卫")) {
                entity.discard();
                WorldRandomEvents.LOGGER.info("[CaravanExpedition] Force-cleaned residual entity: {}", name);
            }
        }
    }

    public boolean isCaravanGuard(Entity entity) {
        for (CaravanGroup group : groups) {
            if (group.meleeGuards.contains(entity) || group.rangedGuards.contains(entity)) {
                return true;
            }
        }
        return false;
    }

    public boolean isCaravanTrader(Entity entity) {
        for (CaravanGroup group : groups) {
            if (group.traders.contains(entity)) {
                return true;
            }
        }
        return false;
    }

    public void aggroGuardsOnPlayer(Player player) {
        for (CaravanGroup group : groups) {
            group.setPermanentAggro(player);
        }
    }

    public Player getAggroTarget() {
        for (CaravanGroup group : groups) {
            if (group.permanentAggroTarget != null && group.permanentAggroTarget.isAlive()) {
                return group.permanentAggroTarget;
            }
        }
        return null;
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
            // 地面必须是固体方块，不能是液体、树叶、原木，且上方必须能看见天空
            if (ground.isFaceSturdy(level, pos, Direction.UP)
                && ground.getFluidState().isEmpty()
                && !isTreeBlock(ground)
                && level.isEmptyBlock(pos.above())
                && level.canSeeSky(pos.above())) {
                return pos.above();
            }
        }
        return null;
    }

    /** 检查生成位置是否安全：脚部+头部都是空气且上方是天空 */
    private static boolean isSpawnSafe(ServerLevel level, BlockPos pos) {
        return level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above())
            && level.canSeeSky(pos);
    }

    /** 排除树干和树叶 */
    private static boolean isTreeBlock(BlockState state) {
        return state.is(net.minecraft.tags.BlockTags.LOGS)
            || state.is(net.minecraft.tags.BlockTags.LEAVES);
    }

    public static class CaravanGroup {
        final BlockPos center;
        final List<WanderingTrader> traders = new ArrayList<>();
        final List<IronGolem> meleeGuards = new ArrayList<>();
        final List<SnowGolem> rangedGuards = new ArrayList<>();
        Player permanentAggroTarget = null;
        final Map<WanderingTrader, Float> lastTraderHealth = new HashMap<>();

        CaravanGroup(BlockPos center) { this.center = center; }

        void spawn(ServerLevel level, Random random) {
            int traderCount = 5 + random.nextInt(3);
            int meleeCount = 2 + random.nextInt(3);
            int rangedCount = 4 + random.nextInt(3);

            for (int i = 0; i < traderCount; i++) {
                BlockPos pos = randomOffset(center, random);
                WanderingTrader trader = EntityType.WANDERING_TRADER.create(
                    level, null, pos, EntitySpawnReason.EVENT, false, false);
                if (trader != null) {
                    trader.setCustomName(Component.literal("远征商人"));
                    trader.setCustomNameVisible(true);
                    trader.setPersistenceRequired();
                    addCustomTrades(trader, level, random);
                    level.addFreshEntity(trader);
                    trader.setGlowingTag(true);
                    traders.add(trader);
                }
            }

            for (int i = 0; i < meleeCount; i++) {
                BlockPos pos = randomOffset(center, random);
                IronGolem golem = EntityType.IRON_GOLEM.create(
                    level, null, pos, EntitySpawnReason.EVENT, false, false);
                if (golem != null) {
                    golem.setCustomName(Component.literal("远征守卫-近战"));
                    golem.setCustomNameVisible(true);
                    golem.setPersistenceRequired();
                    golem.addEffect(new MobEffectInstance(MobEffects.SPEED, -1, 2,
                        false, true, true));
                    golem.addEffect(new MobEffectInstance(MobEffects.STRENGTH, -1, 4,
                        false, true, true));
                    golem.addEffect(new MobEffectInstance(MobEffects.REGENERATION, -1, 0,
                        false, true, true));
                    Objects.requireNonNull(golem.getAttribute(Attributes.MAX_HEALTH))
                        .setBaseValue(400);
                    golem.setHealth(400);
                    level.addFreshEntity(golem);
                    golem.setGlowingTag(true);
                    meleeGuards.add(golem);
                }
            }

            for (int i = 0; i < rangedCount; i++) {
                BlockPos pos = randomOffset(center, random);
                SnowGolem snowman = EntityType.SNOW_GOLEM.create(
                    level, null, pos, EntitySpawnReason.EVENT, false, false);
                if (snowman != null) {
                    snowman.setCustomName(Component.literal("远征守卫-远程"));
                    snowman.setCustomNameVisible(true);
                    snowman.setPersistenceRequired();
                    snowman.addEffect(new MobEffectInstance(MobEffects.SPEED, -1, 0,
                        false, true, true));
                    snowman.addEffect(new MobEffectInstance(MobEffects.STRENGTH, -1, 5,
                        false, true, true));
                    Objects.requireNonNull(snowman.getAttribute(Attributes.MAX_HEALTH))
                        .setBaseValue(180);
                    snowman.setHealth(180);
                    level.addFreshEntity(snowman);
                    snowman.setGlowingTag(true);
                    rangedGuards.add(snowman);
                }
            }
        }

        void tick(ServerLevel level) {
            List<WanderingTrader> aliveTraders = traders.stream()
                .filter(LivingEntity::isAlive).toList();
            List<IronGolem> aliveMelee = meleeGuards.stream()
                .filter(LivingEntity::isAlive).toList();
            List<SnowGolem> aliveRanged = rangedGuards.stream()
                .filter(LivingEntity::isAlive).toList();

            Vec3 centerVec = Vec3.atCenterOf(center);

            // 血量变化检测：商人受伤时，找到最近玩家建立永久仇恨
            if (permanentAggroTarget == null) {
                for (WanderingTrader trader : aliveTraders) {
                    float currentHealth = trader.getHealth();
                    Float lastHealth = lastTraderHealth.get(trader);
                    if (lastHealth != null && currentHealth < lastHealth) {
                        Player nearest = findNearestPlayer(level, trader, 60);
                        if (nearest != null) {
                            setPermanentAggro(nearest);
                            WorldRandomEvents.LOGGER.info(
                                "[CaravanExpedition] Trader damaged! Nearest player: {}",
                                nearest.getName().getString());
                            break;
                        }
                    }
                    lastTraderHealth.put(trader, currentHealth);
                }
            }

            for (WanderingTrader trader : aliveTraders) {
                if (trader.distanceToSqr(centerVec) > 400) {
                    trader.teleportTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
                    trader.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1,
                        false, true, true));
                    trader.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 1,
                        false, true, true));
                }
            }

            if (permanentAggroTarget != null && permanentAggroTarget.isAlive()) {
                for (IronGolem golem : aliveMelee) {
                    golem.setTarget(permanentAggroTarget);
                }
                for (SnowGolem snowman : aliveRanged) {
                    snowman.setTarget(permanentAggroTarget);
                }
            }

            for (IronGolem golem : aliveMelee) {
                if (golem.distanceToSqr(centerVec) > 2500) {
                    golem.teleportTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
                }
            }
            for (SnowGolem snowman : aliveRanged) {
                if (snowman.distanceToSqr(centerVec) > 2500) {
                    snowman.teleportTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
                }
            }
        }

        void setPermanentAggro(Player player) {
            permanentAggroTarget = player;
            for (IronGolem golem : meleeGuards) {
                if (golem.isAlive()) golem.setTarget(player);
            }
            for (SnowGolem snowman : rangedGuards) {
                if (snowman.isAlive()) snowman.setTarget(player);
            }
        }

        boolean allTradersDead() {
            return traders.stream().noneMatch(LivingEntity::isAlive);
        }

        /** 找到距离给定实体最近的玩家（在指定格数内） */
        private static Player findNearestPlayer(ServerLevel level, Entity entity, int rangeBlocks) {
            double rangeSq = rangeBlocks * rangeBlocks;
            Player nearest = null;
            double nearestDistSq = rangeSq;
            for (Player player : level.players()) {
                double distSq = entity.distanceToSqr(player);
                if (distSq < nearestDistSq) {
                    nearestDistSq = distSq;
                    nearest = player;
                }
            }
            return nearest;
        }

        void cleanup(ServerLevel level) {
            for (WanderingTrader t : traders) { if (t.isAlive()) t.discard(); }
            for (IronGolem g : meleeGuards) { if (g.isAlive()) g.discard(); }
            for (SnowGolem s : rangedGuards) { if (s.isAlive()) s.discard(); }

            AABB area = new AABB(
                center.getX() - 60, center.getY() - 10, center.getZ() - 60,
                center.getX() + 60, center.getY() + 10, center.getZ() + 60);
            List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, area);
            for (ItemEntity item : items) {
                item.discard();
            }
        }

        private static BlockPos randomOffset(BlockPos center, Random random) {
            int dx = random.nextInt(11) - 5;
            int dz = random.nextInt(11) - 5;
            return center.offset(dx, 0, dz);
        }

        private static void addCustomTrades(WanderingTrader trader, ServerLevel level,
                                             Random random) {
            trader.getOffers().clear();

            int tradeCount = 8 + random.nextInt(6);

            for (int i = 0; i < tradeCount; i++) {
                int tradeType = random.nextInt(100);
                Item costItem;
                int costCount;
                ItemStack result;

                if (tradeType < 18) {
                    result = createEnchantedArmor(Items.DIAMOND_HELMET, level, random);
                    costItem = Items.EMERALD_BLOCK;
                    costCount = 2 + random.nextInt(5);
                } else if (tradeType < 36) {
                    result = createEnchantedArmor(Items.DIAMOND_CHESTPLATE, level, random);
                    costItem = Items.EMERALD_BLOCK;
                    costCount = 3 + random.nextInt(7);
                } else if (tradeType < 50) {
                    result = createEnchantedArmor(Items.DIAMOND_LEGGINGS, level, random);
                    costItem = Items.EMERALD_BLOCK;
                    costCount = 2 + random.nextInt(6);
                } else if (tradeType < 64) {
                    result = createEnchantedArmor(Items.DIAMOND_BOOTS, level, random);
                    costItem = Items.EMERALD_BLOCK;
                    costCount = 2 + random.nextInt(4);
                } else if (tradeType < 72) {
                    result = createEnchantedArmor(Items.IRON_HELMET, level, random);
                    costItem = Items.EMERALD;
                    costCount = 4 + random.nextInt(12);
                } else if (tradeType < 80) {
                    result = createEnchantedArmor(Items.IRON_CHESTPLATE, level, random);
                    costItem = Items.EMERALD;
                    costCount = 6 + random.nextInt(16);
                } else if (tradeType < 83) {
                    result = createEnchantedArmor(Items.NETHERITE_CHESTPLATE, level, random);
                    costItem = Items.NETHER_STAR;
                    costCount = 1 + random.nextInt(3);
                } else if (tradeType < 85) {
                    result = createEnchantedSword(Items.NETHERITE_SWORD, level, random);
                    costItem = Items.NETHER_STAR;
                    costCount = 1 + random.nextInt(2);
                } else if (tradeType < 90) {
                    result = new ItemStack(Items.SLIME_BLOCK, 2 + random.nextInt(5));
                    costItem = Items.EMERALD;
                    costCount = 2 + random.nextInt(6);
                } else if (tradeType < 95) {
                    result = new ItemStack(Items.HONEY_BLOCK, 2 + random.nextInt(5));
                    costItem = Items.EMERALD;
                    costCount = 2 + random.nextInt(6);
                } else if (tradeType < 97) {
                    result = new ItemStack(Items.DIAMOND_BLOCK, 1 + random.nextInt(3));
                    costItem = Items.NETHERITE_SCRAP;
                    costCount = 2 + random.nextInt(4);
                } else {
                    result = new ItemStack(Items.AMETHYST_SHARD, 8 + random.nextInt(17));
                    costItem = Items.EMERALD;
                    costCount = 1 + random.nextInt(4);
                }

                ItemCost cost = new ItemCost(costItem, costCount);
                int maxUses = costItem == Items.NETHER_STAR ? 1 + random.nextInt(2)
                    : 2 + random.nextInt(7);
                MerchantOffer offer = new MerchantOffer(cost, result, maxUses,
                    5 + random.nextInt(15), 0.05f);
                trader.getOffers().add(offer);
            }

            if (random.nextFloat() < 0.4f) {
                ItemStack seedResult = switch (random.nextInt(4)) {
                    case 0 -> new ItemStack(Items.WHEAT_SEEDS, 4 + random.nextInt(9));
                    case 1 -> new ItemStack(Items.BEETROOT_SEEDS, 4 + random.nextInt(9));
                    case 2 -> new ItemStack(Items.PUMPKIN_SEEDS, 2 + random.nextInt(5));
                    default -> new ItemStack(Items.MELON_SEEDS, 2 + random.nextInt(5));
                };
                trader.getOffers().add(new MerchantOffer(
                    new ItemCost(Items.EMERALD, 1 + random.nextInt(3)),
                    seedResult, 8, 2, 0.05f));
            }

            if (random.nextFloat() < 0.4f) {
                Item saplingItem = switch (random.nextInt(6)) {
                    case 0 -> Items.OAK_SAPLING;
                    case 1 -> Items.SPRUCE_SAPLING;
                    case 2 -> Items.BIRCH_SAPLING;
                    case 3 -> Items.JUNGLE_SAPLING;
                    case 4 -> Items.ACACIA_SAPLING;
                    default -> Items.DARK_OAK_SAPLING;
                };
                trader.getOffers().add(new MerchantOffer(
                    new ItemCost(Items.EMERALD, 1 + random.nextInt(2)),
                    new ItemStack(saplingItem, 2 + random.nextInt(5)),
                    6, 2, 0.05f));
            }
        }

        private static ItemStack createEnchantedArmor(Item item, ServerLevel level,
                                                       Random random) {
            ItemStack stack = new ItemStack(item);
            var enchantRegistry = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT);

            var builder = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            int enchantCount = 1 + random.nextInt(3);

            for (int i = 0; i < enchantCount; i++) {
                List<Enchantment> pool = new ArrayList<>();
                for (var entry : enchantRegistry.entrySet()) {
                    Enchantment ench = entry.getValue();
                    if (ench.isPrimaryItem(stack)) {
                        pool.add(ench);
                    }
                }
                if (pool.isEmpty()) break;
                Enchantment chosen = pool.get(random.nextInt(pool.size()));
                int maxLvl = chosen.getMaxLevel();
                int lvl = 1 + random.nextInt(maxLvl);
                builder.set(enchantRegistry.wrapAsHolder(chosen), lvl);
            }

            stack.set(DataComponents.ENCHANTMENTS, builder.toImmutable());
            return stack;
        }

        private static ItemStack createEnchantedSword(Item item, ServerLevel level,
                                                       Random random) {
            ItemStack stack = new ItemStack(item);
            var enchantRegistry = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT);

            var builder = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            int enchantCount = 1 + random.nextInt(2);

            for (int i = 0; i < enchantCount; i++) {
                List<Enchantment> pool = new ArrayList<>();
                for (var entry : enchantRegistry.entrySet()) {
                    Enchantment ench = entry.getValue();
                    if (ench.isPrimaryItem(stack)) {
                        pool.add(ench);
                    }
                }
                if (pool.isEmpty()) break;
                Enchantment chosen = pool.get(random.nextInt(pool.size()));
                int maxLvl = chosen.getMaxLevel();
                int lvl = 1 + random.nextInt(maxLvl);
                builder.set(enchantRegistry.wrapAsHolder(chosen), lvl);
            }

            stack.set(DataComponents.ENCHANTMENTS, builder.toImmutable());
            return stack;
        }
    }
}