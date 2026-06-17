package com.pfws.worldrandomevents.event.blessing;

import com.pfws.worldrandomevents.event.BaseEvent;
import com.pfws.worldrandomevents.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class HarvestFestival extends BaseEvent {
    private int tickCounter = 0;
    private final List<HarvestAltar> altars = new ArrayList<>();
    private final List<HarvestSpirit> spirits = new ArrayList<>();

    public HarvestFestival() {
        super("harvest_festival", "丰收祭典", EventType.BLESSING);
    }

    @Override public int getBaseTriggerInterval() { return 12; }
    @Override public double getBaseTriggerChance() { return 0.35; }
    @Override public int getBaseDurationTicks() { return daysToTicks(1); }
    @Override public int getBaseCooldownTicks() { return daysToTicks(12); }
    @Override public int getBaseForceTriggerDays() { return 0; }

    @Override protected String getStartTitle() { return "§a§l丰收祭典!"; }
    @Override protected String getStartSubtitle() { return "§e大地赐予丰饶的祝福"; }

    @Override
    protected void onStart(List<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, daysToTicks(1), 1, false, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, daysToTicks(1), 0, false, true, true));
        }
        spawnAltars(players);
        spawnSpirits(players);
    }

    @Override
    protected void onTick(List<ServerPlayer> players) {
        tickCounter++;

        if (tickCounter % 600 == 0) {
            for (ServerPlayer player : players) {
                autoHarvest(player);
            }
        }

        // 每10秒对附近农作物使用1~3次骨粉加速生长
        if (tickCounter % 200 == 0) {
            for (ServerPlayer player : players) {
                bonemealCrops(player);
            }
        }

        if (tickCounter % 200 == 0) {
            for (ServerPlayer player : players) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0, false, true, true));
            }
        }
    }

    @Override
    protected void onEnd(List<ServerPlayer> players) {
        for (HarvestAltar altar : altars) { altar.destroy(level); }
        for (HarvestSpirit spirit : spirits) { spirit.discard(); }
        altars.clear();
        spirits.clear();

        for (ServerPlayer player : players) {
            int crops = countNearbyCrops(player);
            if (crops > 0) {
                player.giveExperiencePoints(crops * 5);
            }
        }
    }

    private void autoHarvest(ServerPlayer player) {
        for (int dx = -32; dx <= 32; dx++) {
            for (int dz = -32; dz <= 32; dz++) {
                BlockPos pos = player.blockPosition().offset(dx, 0, dz);
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
                    level.destroyBlock(pos, true, player);
                    level.setBlock(pos, state.getBlock().defaultBlockState(), 3);
                }
            }
        }
    }

    private void bonemealCrops(ServerPlayer player) {
        Random rand = new Random();
        int radius = 16;
        int maxAttempts = 20;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int dx = rand.nextInt(radius * 2 + 1) - radius;
            int dz = rand.nextInt(radius * 2 + 1) - radius;
            BlockPos pos = player.blockPosition().offset(dx, 0, dz);
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof CropBlock crop && !crop.isMaxAge(state)) {
                int times = 1 + rand.nextInt(3);
                for (int i = 0; i < times; i++) {
                    BlockState current = level.getBlockState(pos);
                    if (current.getBlock() instanceof CropBlock c && !c.isMaxAge(current)) {
                        c.performBonemeal(level, level.getRandom(), pos, current);
                    } else {
                        break;
                    }
                }
            }
        }
    }

    private int countNearbyCrops(ServerPlayer player) {
        int count = 0;
        for (int dx = -32; dx <= 32; dx++) {
            for (int dz = -32; dz <= 32; dz++) {
                BlockPos pos = player.blockPosition().offset(dx, 0, dz);
                if (level.getBlockState(pos).getBlock() instanceof CropBlock) count++;
            }
        }
        return count;
    }

    @Override
    public BlockPos getEventCenter() {
        if (altars.isEmpty()) return null;
        return altars.get(0).pos;
    }

    private void spawnAltars(List<ServerPlayer> players) {
        if (players.isEmpty()) return;
        Random random = new Random();
        for (ServerPlayer player : players) {
            int x = player.blockPosition().getX() + random.nextInt(21) - 10;
            int z = player.blockPosition().getZ() + random.nextInt(21) - 10;
            BlockPos ground = findSurface(x, z);
            if (ground == null) continue;
            HarvestAltar altar = new HarvestAltar(ground);
            altar.build(level);
            altars.add(altar);
        }
    }

    private void spawnSpirits(List<ServerPlayer> players) {
        if (players.isEmpty()) return;
        Random random = new Random();
        for (ServerPlayer player : players) {
            int count = 2 + random.nextInt(2);
            for (int i = 0; i < count; i++) {
                int x = player.blockPosition().getX() + random.nextInt(41) - 20;
                int z = player.blockPosition().getZ() + random.nextInt(41) - 20;
                BlockPos pos = findSurface(x, z);
                if (pos == null) pos = player.blockPosition().offset(random.nextInt(21) - 10, 1, random.nextInt(21) - 10);
                HarvestSpirit spirit = new HarvestSpirit(pos);
                spirit.spawn(level);
                spirits.add(spirit);
            }
        }
    }

    private BlockPos findSurface(int x, int z) {
        for (int y = level.getMaxY() - 1; y > level.getMinY(); y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState ground = level.getBlockState(pos);
            if (ground.isFaceSturdy(level, pos, Direction.UP) && ground.getFluidState().isEmpty() && level.isEmptyBlock(pos.above())) {
                return pos.above();
            }
        }
        return null;
    }

    public static class HarvestAltar {
        public final BlockPos pos;
        HarvestAltar(BlockPos pos) { this.pos = pos; }
        void build(net.minecraft.server.level.ServerLevel level) {
            level.setBlock(pos.below(), Blocks.GOLD_BLOCK.defaultBlockState(), 3);
            level.setBlock(pos.below().east(), Blocks.EMERALD_BLOCK.defaultBlockState(), 3);
            level.setBlock(pos.below().west(), Blocks.GOLD_BLOCK.defaultBlockState(), 3);
        }
        void destroy(net.minecraft.server.level.ServerLevel level) {
            level.destroyBlock(pos.below(), true, null);
            level.destroyBlock(pos.below().east(), true, null);
            level.destroyBlock(pos.below().west(), true, null);
        }
    }

    public static class HarvestSpirit {
        private final BlockPos pos;
        HarvestSpirit(BlockPos pos) { this.pos = pos; }
        void spawn(net.minecraft.server.level.ServerLevel level) {
            ItemStack gift = new Random().nextDouble() < 0.05
                ? new ItemStack(Items.ENCHANTED_GOLDEN_APPLE)
                : new Random().nextBoolean()
                    ? new ItemStack(Items.GOLDEN_APPLE)
                    : new ItemStack(Items.GOLDEN_CARROT, 1 + new Random().nextInt(3));
            ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, gift);
            entity.setGlowingTag(true);
            level.addFreshEntity(entity);
        }
        void discard() {}
    }
}