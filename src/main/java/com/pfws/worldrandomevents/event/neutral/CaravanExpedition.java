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
        // 仅维护守卫 AI，不再因商人死亡而提前结束事件
        // 事件只由自然计时结束或管理员指令结束
        for (CaravanGroup group : groups) {
            group.tick(level);
        }
    }

    @Override
    protected void onEnd(List<ServerPlayer> players) {
        // 先清理 groups 追踪列表中的实体
        for (CaravanGroup group : groups) {
            group.cleanup(level);
        }
        groups.clear();

        // 全维度扫描，强制清空所有远征商人/守卫实体
        // 解决守卫追玩家跑远后局部扫描遗漏的问题
        cleanupAllCaravanEntities();

        eventCenter = null;
        ACTIVE_INSTANCE = null;
    }

    /** 全维度扫描并清空所有远征商队实体（商人+近战守卫+远程守卫） */
    private void cleanupAllCaravanEntities() {
        int removed = 0;
        for (Entity entity : level.getAllEntities()) {
            String name = entity.getCustomName() != null ? entity.getCustomName().getString() : "";
            if (name.contains("远征商人") || name.contains("远征守卫")) {
                entity.discard();
                removed++;
            }
        }
        if (removed > 0) {
            WorldRandomEvents.LOGGER.info(
                "[CaravanExpedition] Full-dimension cleanup: removed {} residual caravan entities", removed);
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
                        .setBaseValue(450);
                    golem.setHealth(450);
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
                        .setBaseValue(240);
                    snowman.setHealth(240);
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

            final int TOTAL_CAP = 11;

            // 1. 附魔钻石装备: 1~3 个交易, 1~2 种装备类型
            int diamondCount = 1 + random.nextInt(3);
            int diamondTypeCount = Math.min(diamondCount, 1 + random.nextInt(2));
            Item[] diamondPool = {Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE,
                Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS};
            shuffleArray(diamondPool, random);

            int added = 0;
            for (int t = 0; t < diamondTypeCount && added < diamondCount; t++) {
                int remaining = diamondCount - added;
                int remainingTypes = diamondTypeCount - t;
                int count = (t == diamondTypeCount - 1) ? remaining
                    : 1 + random.nextInt(Math.max(1, remaining - remainingTypes + 1));
                for (int i = 0; i < count; i++) {
                    ItemStack result = createEnchantedArmor(diamondPool[t], level, random);
                    MerchantOffer offer = new MerchantOffer(
                        new ItemCost(Items.EMERALD_BLOCK, 2 + random.nextInt(8)),
                        result, 2 + random.nextInt(7), 5 + random.nextInt(15), 0.05f);
                    trader.getOffers().add(offer);
                }
                added += count;
            }

            // 2. 附魔铁装备: 4~6 个交易, 1~3 种装备类型
            int ironCount = Math.min(4 + random.nextInt(3), TOTAL_CAP - trader.getOffers().size());
            int ironTypeCount = Math.min(ironCount, 1 + random.nextInt(3));
            Item[] ironPool = {Items.IRON_HELMET, Items.IRON_CHESTPLATE,
                Items.IRON_LEGGINGS, Items.IRON_BOOTS};
            shuffleArray(ironPool, random);

            added = 0;
            for (int t = 0; t < ironTypeCount && added < ironCount
                && trader.getOffers().size() < TOTAL_CAP; t++) {
                int remaining = ironCount - added;
                int remainingTypes = ironTypeCount - t;
                int count = (t == ironTypeCount - 1) ? remaining
                    : 1 + random.nextInt(Math.max(1, remaining - remainingTypes + 1));
                count = Math.min(count, TOTAL_CAP - trader.getOffers().size());
                for (int i = 0; i < count; i++) {
                    ItemStack result = createEnchantedArmor(ironPool[t], level, random);
                    MerchantOffer offer = new MerchantOffer(
                        new ItemCost(Items.EMERALD, 4 + random.nextInt(20)),
                        result, 2 + random.nextInt(7), 5 + random.nextInt(15), 0.05f);
                    trader.getOffers().add(offer);
                }
                added += count;
            }

            // 3. 下界合金装备: 5% 概率，无附魔，需下界之星+钻石块兑换
            int slotsBeforeMisc = TOTAL_CAP - trader.getOffers().size();
            if (slotsBeforeMisc > 0 && random.nextFloat() < 0.05f) {
                Item netheriteItem = random.nextBoolean()
                    ? Items.NETHERITE_CHESTPLATE : Items.NETHERITE_SWORD;
                ItemStack result = new ItemStack(netheriteItem);
                MerchantOffer offer = new MerchantOffer(
                    new ItemCost(Items.NETHER_STAR, 1 + random.nextInt(3)),
                    java.util.Optional.of(new ItemCost(Items.DIAMOND_BLOCK, 5 + random.nextInt(5))),
                    result, 1 + random.nextInt(2), 5 + random.nextInt(15), 0.05f);
                trader.getOffers().add(offer);
            }

            // 4. 杂物: 种子/树苗/史莱姆/蜂蜜/紫水晶/钻石块 等，填充至 11 上限
            int miscSlots = TOTAL_CAP - trader.getOffers().size();
            int miscTarget = 5 + random.nextInt(4); // 5~8
            int miscCount = Math.min(miscTarget, miscSlots);

            Item[] miscPool = {
                Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.PUMPKIN_SEEDS,
                Items.MELON_SEEDS, Items.OAK_SAPLING, Items.SPRUCE_SAPLING,
                Items.BIRCH_SAPLING, Items.JUNGLE_SAPLING, Items.ACACIA_SAPLING,
                Items.DARK_OAK_SAPLING, Items.SLIME_BLOCK, Items.HONEY_BLOCK,
                Items.AMETHYST_SHARD, Items.DIAMOND_BLOCK
            };
            shuffleArray(miscPool, random);

            for (int i = 0; i < miscCount && i < miscPool.length; i++) {
                Item item = miscPool[i];
                ItemStack result;
                ItemCost cost;

                if (item == Items.DIAMOND_BLOCK) {
                    result = new ItemStack(Items.DIAMOND_BLOCK, 1 + random.nextInt(3));
                    cost = new ItemCost(Items.NETHERITE_SCRAP, 2 + random.nextInt(4));
                } else if (item == Items.SLIME_BLOCK || item == Items.HONEY_BLOCK) {
                    result = new ItemStack(item, 2 + random.nextInt(5));
                    cost = new ItemCost(Items.EMERALD, 2 + random.nextInt(6));
                } else if (item == Items.AMETHYST_SHARD) {
                    result = new ItemStack(Items.AMETHYST_SHARD, 8 + random.nextInt(17));
                    cost = new ItemCost(Items.EMERALD, 1 + random.nextInt(4));
                } else if (item.toString().contains("seed")) {
                    // 种子类型
                    int seedQty = (item == Items.PUMPKIN_SEEDS || item == Items.MELON_SEEDS)
                        ? 2 + random.nextInt(5) : 4 + random.nextInt(9);
                    result = new ItemStack(item, seedQty);
                    cost = new ItemCost(Items.EMERALD, 1 + random.nextInt(3));
                } else {
                    // 树苗
                    result = new ItemStack(item, 2 + random.nextInt(5));
                    cost = new ItemCost(Items.EMERALD, 1 + random.nextInt(2));
                }

                MerchantOffer offer = new MerchantOffer(cost, result,
                    2 + random.nextInt(7), 2 + random.nextInt(13), 0.05f);
                trader.getOffers().add(offer);
            }
        }

        private static void shuffleArray(Item[] arr, Random random) {
            for (int i = arr.length - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                Item tmp = arr[i];
                arr[i] = arr[j];
                arr[j] = tmp;
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