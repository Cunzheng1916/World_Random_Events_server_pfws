package com.pfws.worldrandomevents.event.neutral;

import com.pfws.worldrandomevents.event.BaseEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.*;

public class WanderingBlacksmith extends BaseEvent {
    private BlacksmithCamp camp;

    public WanderingBlacksmith() {
        super("wandering_blacksmith", "Wandering Blacksmith", EventType.NEUTRAL);
    }

    @Override public int getBaseTriggerInterval() { return 10; }
    @Override public double getBaseTriggerChance() { return 0.30; }
    @Override public int getBaseDurationTicks() { return daysToTicks(4); }
    @Override public int getBaseCooldownTicks() { return daysToTicks(10); }
    @Override public int getBaseForceTriggerDays() { return 50; }

    @Override protected String getStartTitle() { return "§6§l流浪铁匠到来!"; }
    @Override protected String getStartSubtitle() { return "§e一位技艺精湛的铁匠在此扎营"; }

    @Override
    protected void onStart(List<ServerPlayer> players) {
        spawnCamp(players);
    }

    @Override
    protected void onTick(List<ServerPlayer> players) {
    }

    @Override
    protected void onEnd(List<ServerPlayer> players) {
        if (camp != null) {
            camp.destroy(level);
            camp = null;
        }
    }

    private void spawnCamp(List<ServerPlayer> players) {
        if (players.isEmpty()) return;
        Random random = new Random();
        ServerPlayer target = players.get(random.nextInt(players.size()));
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = 20 + random.nextDouble() * 30;
        int x = (int) (target.getX() + Math.cos(angle) * dist);
        int z = (int) (target.getZ() + Math.sin(angle) * dist);
        BlockPos ground = findSurface(x, z);
        if (ground == null) return;

        camp = new BlacksmithCamp(ground);
        camp.build(level, random);
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

    private static class BlacksmithCamp {
        private final BlockPos pos;
        private Villager blacksmith;
        private Camel camel;
        private IronGolem golem;

        BlacksmithCamp(BlockPos pos) { this.pos = pos; }

        void build(net.minecraft.server.level.ServerLevel level, Random random) {
            level.setBlock(pos.below(), Blocks.BLAST_FURNACE.defaultBlockState(), 3);
            level.setBlock(pos.below().east(), Blocks.SMITHING_TABLE.defaultBlockState(), 3);
            level.setBlock(pos.below().west(), Blocks.DAMAGED_ANVIL.defaultBlockState(), 3);
            level.setBlock(pos.north(), Blocks.LANTERN.defaultBlockState(), 3);
            level.setBlock(pos.south(), Blocks.LANTERN.defaultBlockState(), 3);

            blacksmith = EntityType.VILLAGER.create(level, null, pos,
                EntitySpawnReason.EVENT, false, false);
            if (blacksmith != null) {
                blacksmith.setCustomName(Component.literal("流浪铁匠"));
                blacksmith.setCustomNameVisible(true);
                level.addFreshEntity(blacksmith);
            }

            camel = EntityType.CAMEL.create(level, null, pos.east(2),
                EntitySpawnReason.EVENT, false, false);
            if (camel != null) {
                camel.setCustomName(Component.literal("驮箱骆驼"));
                camel.setCustomNameVisible(true);
                level.addFreshEntity(camel);
            }

            golem = EntityType.IRON_GOLEM.create(level, null, pos.west(2),
                EntitySpawnReason.EVENT, false, false);
            if (golem != null) {
                golem.setCustomName(Component.literal("铁匠卫士"));
                level.addFreshEntity(golem);
            }
        }

        void destroy(net.minecraft.server.level.ServerLevel level) {
            level.destroyBlock(pos.below(), false, null);
            level.destroyBlock(pos.below().east(), false, null);
            level.destroyBlock(pos.below().west(), false, null);
            level.destroyBlock(pos.north(), false, null);
            level.destroyBlock(pos.south(), false, null);
            if (blacksmith != null && blacksmith.isAlive()) blacksmith.discard();
            if (camel != null && camel.isAlive()) camel.discard();
            if (golem != null && golem.isAlive()) golem.discard();
        }
    }
}