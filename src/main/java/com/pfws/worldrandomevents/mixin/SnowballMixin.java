package com.pfws.worldrandomevents.mixin;

import com.pfws.worldrandomevents.WorldRandomEvents;
import com.pfws.worldrandomevents.event.BaseEvent;
import com.pfws.worldrandomevents.event.neutral.CaravanExpedition;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Snowball.class)
public abstract class SnowballMixin {

    @Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
    private void onHitEntity(EntityHitResult hitResult, CallbackInfo ci) {
        Snowball self = (Snowball) (Object) this;
        if (self.level().isClientSide()) return;

        BaseEvent current = WorldRandomEvents.EVENT_MANAGER.getCurrentEvent();
        if (current instanceof CaravanExpedition caravan && caravan.isActive()) {
            Entity owner = self.getOwner();
            if (owner instanceof SnowGolem && caravan.isCaravanGuard(owner)) {
                Entity target = hitResult.getEntity();
                if (target instanceof LivingEntity livingTarget) {
                    livingTarget.hurtServer((ServerLevel) self.level(),
                        self.damageSources().thrown(self, owner), 4.0f);
                    ci.cancel();
                }
            }
        }
    }
}
