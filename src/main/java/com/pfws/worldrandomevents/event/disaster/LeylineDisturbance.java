package com.pfws.worldrandomevents.event.disaster;

import com.pfws.worldrandomevents.event.BaseEvent;
import com.pfws.worldrandomevents.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.Collections;

public class LeylineDisturbance extends BaseEvent {
    private final List<LeylineNode> nodes = new ArrayList<>();
    private int tickCounter = 0;
    private int stabilizedCount = 0;

    public LeylineDisturbance() {
        super("leyline_disturbance", "地脉紊乱", EventType.DISASTER);
    }

    @Override public int getBaseTriggerInterval() { return 7; }
    @Override public double getBaseTriggerChance() { return 0.20; }
    @Override public int getBaseDurationTicks() { return daysToTicks(2); }
    @Override public int getBaseCooldownTicks() { return daysToTicks(7); }
    @Override public int getBaseForceTriggerDays() { return 35; }

    @Override protected String getStartTitle() { return "§5§l地脉紊乱!"; }
    @Override protected String getStartSubtitle() { return "§d大地在震动...净化节点以恢复平衡"; }

    @Override
    protected void onStart(List<ServerPlayer> players) {
        spawnNodes(players);
        for (ServerPlayer player : players) {
            NetworkHandler.sendSkyEffect(player, 0xFF2A1A3A, 0xFF1A0A2A);
        }
    }

    @Override
    protected void onTick(List<ServerPlayer> players) {
        tickCounter++;

        if (tickCounter % 600 == 0 || tickCounter % 1200 == 0) {
            for (ServerPlayer player : players) {
                NetworkHandler.sendScreenShake(player, 0.5f + (float) Math.random() * 0.5f, 60);
            }
        }

        if (tickCounter % 200 == 0) {
            for (ServerPlayer player : players) {
                for (LeylineNode node : nodes) {
                    if (node.isActive() && player.distanceToSqr(node.x + 0.5, node.y, node.z + 0.5) < 100) {
                        applyNodeEffect(player, node);
                    }
                }
            }
        }
    }

    @Override
    protected void onEnd(List<ServerPlayer> players) {
        for (LeylineNode node : nodes) {
            node.destroy(level);
        }
        nodes.clear();
        for (ServerPlayer player : players) {
            NetworkHandler.sendSkyEffect(player, 0, 0);
        }
    }

    private void spawnNodes(List<ServerPlayer> players) {
        if (players.isEmpty()) return;
        Random random = new Random();
        ServerPlayer target = players.get(random.nextInt(players.size()));
        int nodeCount = 3 + random.nextInt(3);

        for (int i = 0; i < nodeCount; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = 20 + random.nextDouble() * 40;
            int x = (int) (target.getX() + Math.cos(angle) * dist);
            int z = (int) (target.getZ() + Math.sin(angle) * dist);
            BlockPos pos = findGroundPos(x, z);
            if (pos == null) continue;

            LeylineNode node = new LeylineNode(pos);
            node.build(level);
            nodes.add(node);
        }
    }

    private BlockPos findGroundPos(int x, int z) {
        for (int y = level.getMaxY() - 1; y > level.getMinY(); y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState ground = level.getBlockState(pos);
            if (ground.isFaceSturdy(level, pos, Direction.UP) && ground.getFluidState().isEmpty() && level.isEmptyBlock(pos.above())) {
                return pos.above();
            }
        }
        return null;
    }

    private void applyNodeEffect(ServerPlayer player, LeylineNode node) {
        if (node.isStabilized()) {
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, 12000, 0, false, true, true));
        } else {
            List<Holder<MobEffect>> debuffs = List.of(MobEffects.WEAKNESS, MobEffects.MINING_FATIGUE,
                MobEffects.HUNGER, MobEffects.POISON);
            player.addEffect(new MobEffectInstance(debuffs.get(new Random().nextInt(debuffs.size())), 300, 0, false, true, true));
        }
    }

    public List<LeylineNode> getNodes() { return Collections.unmodifiableList(nodes); }

    @Override
    public BlockPos getEventCenter() {
        if (nodes.isEmpty()) return null;
        return new BlockPos(nodes.get(0).x, nodes.get(0).y, nodes.get(0).z);
    }

    public void stabilizeNode(BlockPos pos) {
        for (LeylineNode node : nodes) {
            if (node.x == pos.getX() && node.y == pos.getY() && node.z == pos.getZ()) {
                node.stabilize(level);
                stabilizedCount++;
                if (stabilizedCount >= nodes.size()) {
                    for (ServerPlayer player : level.players()) {
                        player.addEffect(new MobEffectInstance(MobEffects.LUCK, 36000, 0, false, true, true));
                    }
                    end(level.players());
                }
                return;
            }
        }
    }

    public static class LeylineNode {
        public final int x, y, z;
        private boolean active = true;
        private boolean stabilized = false;
        private ArmorStand marker;

        LeylineNode(BlockPos pos) { this.x = pos.getX(); this.y = pos.getY(); this.z = pos.getZ(); }

        public boolean isActive() { return active; }
        public boolean isStabilized() { return stabilized; }

        void build(net.minecraft.server.level.ServerLevel level) {
            BlockPos pos = new BlockPos(x, y, z);
            level.setBlock(pos, Blocks.AMETHYST_BLOCK.defaultBlockState(), 3);
            marker = EntityType.ARMOR_STAND.create(level, null, pos, EntitySpawnReason.EVENT, false, false);
            if (marker != null) {
                marker.setInvisible(true);
                marker.setNoGravity(true);
                marker.setInvulnerable(true);
                marker.setGlowingTag(true);
                level.addFreshEntity(marker);
            }
        }

        void stabilize(net.minecraft.server.level.ServerLevel level) {
            stabilized = true;
            BlockPos pos = new BlockPos(x, y, z);
            level.setBlock(pos, Blocks.BUDDING_AMETHYST.defaultBlockState(), 3);
        }

        void destroy(net.minecraft.server.level.ServerLevel level) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.isEmptyBlock(pos)) {
                level.destroyBlock(pos, true, null);
            }
            if (marker != null && marker.isAlive()) {
                marker.discard();
                marker = null;
            }
        }
    }
}