package com.pfws.worldrandomevents.mixin;

import com.pfws.worldrandomevents.WorldRandomEvents;
import com.pfws.worldrandomevents.event.BaseEvent;
import com.pfws.worldrandomevents.event.disaster.BloodMoon;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {

    @Inject(method = "addFreshEntity", at = @At("HEAD"))
    private void onAddEntity(net.minecraft.world.entity.Entity entity, CallbackInfoReturnable<Boolean> cir) {
        ServerLevel self = (ServerLevel) (Object) this;
        if (self.isClientSide()) return;

        BaseEvent current = WorldRandomEvents.EVENT_MANAGER.getCurrentEvent();

        if (current instanceof BloodMoon && current.isActive() && entity instanceof Mob mob) {
            mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.STRENGTH, -1, 2, false, true, true));
            mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.SPEED, -1, 1, false, true, true));
            mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.RESISTANCE, -1, 0, false, true, true));
        }

    }
}