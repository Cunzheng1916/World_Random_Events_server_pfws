package com.pfws.worldrandomevents.event.disaster;

import com.pfws.worldrandomevents.event.BaseEvent;
import com.pfws.worldrandomevents.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class FishRain extends BaseEvent {
    private int tickCounter = 0;
    private static List<ItemStack> FISH_POOL = null;

    private static List<ItemStack> getFishPool() {
        if (FISH_POOL == null) {
            FISH_POOL = List.of(
                new ItemStack(Items.COD),
                new ItemStack(Items.SALMON),
                new ItemStack(Items.TROPICAL_FISH),
                new ItemStack(Items.PUFFERFISH),
                new ItemStack(Items.COOKED_COD),
                new ItemStack(Items.COOKED_SALMON)
            );
        }
        return FISH_POOL;
    }

    public FishRain() {
        super("fish_rain", "天降咸鱼", EventType.DISASTER);
    }

    @Override public int getBaseTriggerInterval() { return 5; }
    @Override public double getBaseTriggerChance() { return 0.25; }
    @Override public int getBaseDurationTicks() { return minutesToTicks(2); }
    @Override public int getBaseCooldownTicks() { return daysToTicks(5); }
    @Override public int getBaseForceTriggerDays() { return 30; }

    @Override protected String getStartTitle() { return "§b§l天降咸鱼!"; }
    @Override protected String getStartSubtitle() { return "§3天空中开始下起各种鱼类"; }

    @Override
    protected void onStart(List<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            NetworkHandler.sendSkyEffect(player, 0xFF0033AA, 0xFF0022AA);
        }
    }

    @Override
    protected void onTick(List<ServerPlayer> players) {
        tickCounter++;

        // 每秒下3~5条鱼 (20 ticks = 1秒)
        if (tickCounter % 20 == 0) {
            Random random = new Random();
            for (ServerPlayer player : players) {
                int fishCount = 3 + random.nextInt(3); // 3~5条
                for (int i = 0; i < fishCount; i++) {
                    spawnFishNearPlayer(player, random);
                }
            }
        }
    }

    @Override
    protected void onEnd(List<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            NetworkHandler.sendSkyEffect(player, 0, 0);
        }
    }

    private void spawnFishNearPlayer(ServerPlayer player, Random random) {
        // 在玩家50格范围内随机位置
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = random.nextDouble() * 50;
        double x = player.getX() + Math.cos(angle) * dist;
        double z = player.getZ() + Math.sin(angle) * dist;

        // 找到地表位置，从上方生成鱼
        BlockPos surface = findSurface(level, (int) x, (int) z);
        if (surface == null) {
            surface = new BlockPos((int) x, (int) player.getY() + 10, (int) z);
        }

        // 在天空中生成（地面上方10~20格）
        double spawnY = surface.getY() + 10 + random.nextDouble() * 10;
        BlockPos spawnPos = new BlockPos((int) x, (int) spawnY, (int) z);

        ItemStack fish = getFishPool().get(random.nextInt(getFishPool().size())).copy();
        ItemEntity itemEntity = new ItemEntity(level, x, spawnY, z, fish);
        itemEntity.setDefaultPickUpDelay();
        level.addFreshEntity(itemEntity);
    }

    private static BlockPos findSurface(ServerLevel level, int x, int z) {
        for (int y = level.getMaxY() - 1; y > level.getMinY(); y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState ground = level.getBlockState(pos);
            if (ground.isFaceSturdy(level, pos, Direction.UP)
                && ground.getFluidState().isEmpty()
                && level.isEmptyBlock(pos.above())) {
                return pos.above();
            }
        }
        return null;
    }
}