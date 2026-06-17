package com.pfws.worldrandomevents.event.disaster;

import com.pfws.worldrandomevents.event.BaseEvent;
import com.pfws.worldrandomevents.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class KunKunInvasion extends BaseEvent {
    private final List<Mob> chickens = new ArrayList<>();
    private int tickCounter = 0;

    public KunKunInvasion() {
        super("kunkun_invasion", "坤坤入侵", EventType.DISASTER);
    }

    @Override public int getBaseTriggerInterval() { return 7; }
    @Override public double getBaseTriggerChance() { return 0.20; }
    @Override public int getBaseDurationTicks() { return daysToTicks(2); }
    @Override public int getBaseCooldownTicks() { return daysToTicks(7); }
    @Override public int getBaseForceTriggerDays() { return 40; }

    @Override protected String getStartTitle() { return "§e§l坤坤入侵!"; }
    @Override protected String getStartSubtitle() { return "§6鸡你太美~ 一大波坤坤正在接近"; }

    @Override
    protected void onStart(List<ServerPlayer> players) {
        spawnKunKuns(players);
        for (ServerPlayer player : players) {
            NetworkHandler.sendSkyEffect(player, 0xFFFFCC00, 0xFFEEBB00);
        }
    }

    @Override
    protected void onTick(List<ServerPlayer> players) {
        tickCounter++;

        // 每5秒播放一次坤坤叫声给附近玩家
        if (tickCounter % 100 == 0) {
            for (ServerPlayer player : players) {
                // 找到距离玩家最近的坤坤
                LivingEntity nearest = findNearestChicken(player);
                if (nearest != null && player.distanceToSqr(nearest) < 10000) {
                    NetworkHandler.sendSoundEffect(player,
                        "world-random-events:mob.chicken.kunkun",
                        nearest.getX(), nearest.getY(), nearest.getZ(),
                        1.0f, 1.0f);
                }
            }
        }

        // 清理死亡引用
        chickens.removeIf(c -> !c.isAlive());
    }

    @Override
    protected void onEnd(List<ServerPlayer> players) {
        for (Mob c : chickens) {
            if (c.isAlive()) c.discard();
        }
        chickens.clear();
        for (ServerPlayer player : players) {
            NetworkHandler.sendSkyEffect(player, 0, 0);
        }
    }

    private LivingEntity findNearestChicken(ServerPlayer player) {
        LivingEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (Mob c : chickens) {
            if (!c.isAlive()) continue;
            double dist = player.distanceToSqr(c);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = c;
            }
        }
        return nearest;
    }

    private void spawnKunKuns(List<ServerPlayer> players) {
        if (players.isEmpty()) return;
        Random random = new Random();

        for (ServerPlayer player : players) {
            int count = 3 + random.nextInt(6); // 每个玩家3-8只
            for (int i = 0; i < count; i++) {
                for (int attempt = 0; attempt < 15; attempt++) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    double dist = 5 + random.nextDouble() * 95; // 5-100格
                    int x = (int) (player.getX() + Math.cos(angle) * dist);
                    int z = (int) (player.getZ() + Math.sin(angle) * dist);
                    BlockPos spawnPos = findSurface(level, x, z);
                    if (spawnPos == null) continue;

                    net.minecraft.world.entity.Entity chicken = EntityType.CHICKEN.create(
                        level, null, spawnPos, EntitySpawnReason.EVENT, false, false);
                    if (chicken instanceof Mob mob) {
                        mob.setCustomName(Component.literal("坤坤"));
                        mob.setCustomNameVisible(true);
                        mob.setGlowingTag(true);
                        mob.setPersistenceRequired();
                        level.addFreshEntity(mob);
                        chickens.add(mob);
                    }
                    break;
                }
            }
        }
    }

    private static BlockPos findSurface(ServerLevel level, int x, int z) {
        for (int y = level.getMaxY() - 1; y > level.getMinY(); y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState ground = level.getBlockState(pos);
            if (ground.isFaceSturdy(level, pos, Direction.UP)
                && ground.getFluidState().isEmpty()
                && level.isEmptyBlock(pos.above())
                && level.canSeeSky(pos.above())) {
                return pos.above();
            }
        }
        return null;
    }
}