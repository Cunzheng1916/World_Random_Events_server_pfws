package com.pfws.worldrandomevents.event.blessing;

import com.pfws.worldrandomevents.event.BaseEvent;
import com.pfws.worldrandomevents.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.*;

public class MeteorShower extends BaseEvent {
    private int tickCounter = 0;

    public MeteorShower() {
        super("meteor_shower", "Meteor Shower", EventType.BLESSING);
    }

    @Override public int getBaseTriggerInterval() { return 5; }
    @Override public double getBaseTriggerChance() { return 0.20; }
    @Override public int getBaseDurationTicks() { return 4200; }
    @Override public int getBaseCooldownTicks() { return daysToTicks(5); }
    @Override public int getBaseForceTriggerDays() { return 0; }

    @Override protected String getStartTitle() { return "§e§l流星雨!"; }
    @Override protected String getStartSubtitle() { return "§6天空划过璀璨的流星"; }

    @Override
    protected void onStart(List<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, getBaseDurationTicks(), 0, false, true, true));
        }
        spawnMeteorites(players);
    }

    @Override
    protected void onTick(List<ServerPlayer> players) {
        tickCounter++;
    }

    @Override
    protected void onEnd(List<ServerPlayer> players) {
    }

    private void spawnMeteorites(List<ServerPlayer> players) {
        if (players.isEmpty()) return;
        Random random = new Random();
        for (ServerPlayer player : players) {
            int count = 10 + random.nextInt(11);
            for (int i = 0; i < count; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double dist = 10 + random.nextDouble() * 54;
                int x = (int) (player.getX() + Math.cos(angle) * dist);
                int z = (int) (player.getZ() + Math.sin(angle) * dist);
                BlockPos ground = findSurface(level, x, z);
                if (ground == null) continue;

                BlockPos center = ground.below();
                boolean nearImportant = false;
                for (int dx = -2; dx <= 2 && !nearImportant; dx++) {
                    for (int dz = -2; dz <= 2 && !nearImportant; dz++) {
                        var state = level.getBlockState(center.offset(dx, 0, dz));
                        if (state.is(Blocks.CHEST) || state.is(Blocks.CRAFTING_TABLE)
                            || state.is(BlockTags.BEDS) || state.is(Blocks.ENCHANTING_TABLE)) {
                            nearImportant = true;
                        }
                    }
                }
                if (nearImportant) continue;

                createMeteoriteCrater(ground, random);
            }
        }
    }

    private void createMeteoriteCrater(BlockPos ground, Random random) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = ground.offset(dx, 0, dz);
                if (!level.isEmptyBlock(pos)) {
                    level.destroyBlock(pos, false, null);
                }
            }
        }
        level.setBlock(ground.below(), Blocks.MAGMA_BLOCK.defaultBlockState(), 3);

        double roll = random.nextDouble();
        ItemStack drop;
        if (roll < 0.01) {
            drop = random.nextBoolean()
                ? new ItemStack(Items.DIAMOND)
                : new ItemStack(Items.NETHERITE_SCRAP);
        } else if (roll < 0.10) {
            drop = new ItemStack(Items.GOLD_INGOT, 1 + random.nextInt(2));
        } else {
            drop = new ItemStack(Items.IRON_INGOT, 1 + random.nextInt(2));
        }

        ItemEntity entity = new ItemEntity(level, ground.getX() + 0.5, ground.getY() + 0.5,
            ground.getZ() + 0.5, drop);
        level.addFreshEntity(entity);
    }

    private BlockPos findSurface(net.minecraft.server.level.ServerLevel level, int x, int z) {
        for (int y = level.getMaxY() - 1; y > level.getMinY(); y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above())) {
                return pos.above();
            }
        }
        return null;
    }
}