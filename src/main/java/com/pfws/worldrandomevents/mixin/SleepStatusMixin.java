package com.pfws.worldrandomevents.mixin;

import com.pfws.worldrandomevents.WorldRandomEvents;
import com.pfws.worldrandomevents.event.BaseEvent;
import com.pfws.worldrandomevents.event.disaster.BloodMoon;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class SleepStatusMixin {

    @Inject(method = "isSleepingLongEnough", at = @At("HEAD"), cancellable = true)
    private void onIsSleepingLongEnough(CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;
        if (self.level().isClientSide()) return;

        BaseEvent current = WorldRandomEvents.EVENT_MANAGER.getCurrentEvent();
        if (current instanceof BloodMoon && current.isActive()) {
            cir.setReturnValue(false);
            if (self instanceof ServerPlayer sp) {
                sp.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal("§c血月之夜，你无法安然入睡"), true);
            }
        }
    }
}