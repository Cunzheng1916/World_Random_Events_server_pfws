package com.pfws.worldrandomevents.event;

import com.pfws.worldrandomevents.WorldRandomEvents;
import com.pfws.worldrandomevents.event.disaster.LeylineDisturbance;
import com.pfws.worldrandomevents.event.disaster.SoulStorm;
import com.pfws.worldrandomevents.event.neutral.MysteriousObelisk;
import com.pfws.worldrandomevents.network.NetworkHandler;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;

public class EventInteractionHandler {

    public static void register() {
        UseBlockCallback.EVENT.register(EventInteractionHandler::onUseBlock);
    }

    private static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand,
                                                  BlockHitResult hitResult) {
        if (level.isClientSide()) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

        BlockPos pos = hitResult.getBlockPos();

        LeylineDisturbance leyline = getActiveLeyline();
        if (leyline != null) {
            ItemStack mainHand = serverPlayer.getMainHandItem();
            if (mainHand.getItem() == Items.AMETHYST_SHARD && mainHand.getCount() >= 8) {
                LeylineDisturbance.LeylineNode node = leyline.getNodes().stream()
                    .filter(n -> n.x == pos.getX() && n.y == pos.getY() && n.z == pos.getZ())
                    .findFirst().orElse(null);
                if (node != null && !node.isStabilized()) {
                    mainHand.shrink(8);
                    leyline.stabilizeNode(pos);
                    serverPlayer.sendSystemMessage(
                        Component.literal("§5地脉节点已稳定!"), true);
                    return InteractionResult.SUCCESS;
                }
            }
        }

        MysteriousObelisk obeliskEvent = getActiveObelisk();
        if (obeliskEvent != null && obeliskEvent.getObelisk() != null) {
            BlockPos obeliskBase = obeliskEvent.getObelisk().getBase();
            if (pos.getX() >= obeliskBase.getX() - 1 && pos.getX() <= obeliskBase.getX() + 1
                && pos.getY() >= obeliskBase.getY() && pos.getY() <= obeliskBase.getY() + 3
                && pos.getZ() >= obeliskBase.getZ() - 1 && pos.getZ() <= obeliskBase.getZ() + 1) {

                ItemStack offering = serverPlayer.getMainHandItem();
                if (!offering.isEmpty()) {
                    boolean activated = obeliskEvent.activateObelisk(obeliskBase, serverPlayer, offering.copy());
                    if (activated) {
                        offering.shrink(1);
                        serverPlayer.sendSystemMessage(
                            Component.literal("§5方尖碑已被激活! 状态: "
                                + obeliskEvent.getObelisk().getState().name()), true);
                        return InteractionResult.SUCCESS;
                    }
                }
            }
        }

        SoulStorm soulStorm = getActiveSoulStorm();
        if (soulStorm != null) {
            SoulStorm.SoulCage cage = soulStorm.getSoulCages().values().stream()
                .filter(c -> c.pos.getX() == pos.getX() && c.pos.getY() + 1 == pos.getY()
                    && c.pos.getZ() == pos.getZ())
                .findFirst().orElse(null);
            if (cage != null) {
                BlockPos above = cage.pos.above();
                if (level.getBlockState(above).is(Blocks.IRON_BARS)) {
                    ItemStack mainHand = serverPlayer.getMainHandItem();
                    if (mainHand.getItem() == Items.TORCH) {
                        soulStorm.releaseSoul(cage.deadPlayer.getName().getString(), serverPlayer);
                        serverPlayer.sendSystemMessage(
                            Component.literal("§a灵魂已释放! 获得生命恢复 II 和 10 点经验"), true);
                        return InteractionResult.SUCCESS;
                    }
                }
            }
        }

        return InteractionResult.PASS;
    }

    private static LeylineDisturbance getActiveLeyline() {
        BaseEvent current = WorldRandomEvents.EVENT_MANAGER.getCurrentEvent();
        if (current instanceof LeylineDisturbance l && current.isActive()) return l;
        return null;
    }

    private static MysteriousObelisk getActiveObelisk() {
        BaseEvent current = WorldRandomEvents.EVENT_MANAGER.getCurrentEvent();
        if (current instanceof MysteriousObelisk o && current.isActive()) return o;
        return null;
    }

    private static SoulStorm getActiveSoulStorm() {
        BaseEvent current = WorldRandomEvents.EVENT_MANAGER.getCurrentEvent();
        if (current instanceof SoulStorm s && current.isActive()) return s;
        return null;
    }
}