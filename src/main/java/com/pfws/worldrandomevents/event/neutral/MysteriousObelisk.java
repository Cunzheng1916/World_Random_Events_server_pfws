package com.pfws.worldrandomevents.event.neutral;

import com.pfws.worldrandomevents.event.BaseEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.*;

public class MysteriousObelisk extends BaseEvent {
    private Obelisk obelisk;
    private boolean worldHasObelisk = false;

    public enum ObeliskState {
        DORMANT,
        KNOWLEDGE,
        SILENCE,
        COMBAT,
        LIFE
    }

    public MysteriousObelisk() {
        super("mysterious_obelisk", "Mysterious Obelisk", EventType.NEUTRAL);
    }

    @Override public int getBaseTriggerInterval() { return 15; }
    @Override public double getBaseTriggerChance() { return 0.25; }
    @Override public int getBaseDurationTicks() { return -1; }
    @Override public int getBaseCooldownTicks() { return daysToTicks(15); }
    @Override public int getBaseForceTriggerDays() { return 0; }

    @Override protected String getStartTitle() { return "§5§l神秘方尖碑!"; }
    @Override protected String getStartSubtitle() { return "§d一座古老的方尖碑出现在附近"; }

    @Override
    public boolean canTrigger() {
        if (worldHasObelisk) return false;
        return super.canTrigger();
    }

    @Override
    protected void onStart(List<ServerPlayer> players) {
        worldHasObelisk = true;
        spawnObelisk(players);
    }

    @Override
    protected void onTick(List<ServerPlayer> players) {
        if (obelisk != null && obelisk.isActive()) {
            obelisk.tick(level, players);
        }
    }

    @Override
    protected void onEnd(List<ServerPlayer> players) {
        if (obelisk != null) {
            obelisk.destroy(level);
            obelisk = null;
        }
        worldHasObelisk = false;
    }

    private void spawnObelisk(List<ServerPlayer> players) {
        if (players.isEmpty()) return;
        Random random = new Random();
        ServerPlayer target = players.get(random.nextInt(players.size()));
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = 20 + random.nextDouble() * 30;
        int x = (int) (target.getX() + Math.cos(angle) * dist);
        int z = (int) (target.getZ() + Math.sin(angle) * dist);
        BlockPos ground = findSurface(x, z);
        if (ground == null) return;

        obelisk = new Obelisk(ground);
        obelisk.build(level);
    }

    public boolean activateObelisk(BlockPos pos, ServerPlayer player, ItemStack offering) {
        if (obelisk == null || !obelisk.isAt(pos)) return false;
        return obelisk.activate(level, player, offering);
    }

    public Obelisk getObelisk() { return obelisk; }

    private BlockPos findSurface(int x, int z) {
        for (int y = level.getMaxY() - 1; y > level.getMinY(); y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above())) {
                return pos.above();
            }
        }
        return null;
    }

    public static class Obelisk {
        private final BlockPos base;
        private ObeliskState state = ObeliskState.DORMANT;
        private boolean active = true;

        public Obelisk(BlockPos base) { this.base = base; }

        public BlockPos getBase() { return base; }
        public ObeliskState getState() { return state; }
        public boolean isActive() { return active; }
        public boolean isAt(BlockPos pos) { return pos.equals(base); }

        void build(net.minecraft.server.level.ServerLevel level) {
            level.setBlock(base, Blocks.BLACKSTONE.defaultBlockState(), 3);
            level.setBlock(base.above(), Blocks.PURPUR_PILLAR.defaultBlockState(), 3);
            level.setBlock(base.above(2), Blocks.PURPUR_PILLAR.defaultBlockState(), 3);
            level.setBlock(base.above(3), Blocks.END_ROD.defaultBlockState(), 3);
            level.setBlock(base.north(), Blocks.BLACKSTONE.defaultBlockState(), 3);
            level.setBlock(base.south(), Blocks.BLACKSTONE.defaultBlockState(), 3);
            level.setBlock(base.east(), Blocks.BLACKSTONE.defaultBlockState(), 3);
            level.setBlock(base.west(), Blocks.BLACKSTONE.defaultBlockState(), 3);
        }

        boolean activate(net.minecraft.server.level.ServerLevel level, ServerPlayer player, ItemStack offering) {
            if (offering.getItem() == Items.NETHER_STAR) {
                state = ObeliskState.KNOWLEDGE;
            } else if (offering.getItem() == Items.SCULK_CATALYST) {
                state = ObeliskState.SILENCE;
            } else if (offering.getItem() == Items.NETHERITE_INGOT) {
                state = ObeliskState.COMBAT;
            } else if (offering.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
                state = ObeliskState.LIFE;
            } else {
                return false;
            }
            offering.shrink(1);
            return true;
        }

        void tick(net.minecraft.server.level.ServerLevel level, List<ServerPlayer> players) {
            for (ServerPlayer player : players) {
                if (player.distanceToSqr(base.getX() + 0.5, base.getY(), base.getZ() + 0.5) > 4096) continue;

                switch (state) {
                    case KNOWLEDGE:
                        player.addEffect(new MobEffectInstance(MobEffects.LUCK, 200, 0, false, true, true));
                        break;
                    case SILENCE:
                        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, true, true));
                        break;
                    case COMBAT:
                        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, 1, false, true, true));
                        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 0, false, true, true));
                        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 0, false, true, true));
                        break;
                    case LIFE:
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1, false, true, true));
                        player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 200, 0, false, true, true));
                        break;
                    default:
                        break;
                }
            }
        }

        void destroy(net.minecraft.server.level.ServerLevel level) {
            for (int dy = 0; dy <= 3; dy++) {
                level.destroyBlock(base.above(dy), true, null);
            }
            level.destroyBlock(base.north(), true, null);
            level.destroyBlock(base.south(), true, null);
            level.destroyBlock(base.east(), true, null);
            level.destroyBlock(base.west(), true, null);
            active = false;
        }
    }
}