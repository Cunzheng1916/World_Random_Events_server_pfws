package com.pfws.worldrandomevents.mixin;

import com.pfws.worldrandomevents.WorldRandomEvents;
import com.pfws.worldrandomevents.event.BaseEvent;
import com.pfws.worldrandomevents.event.neutral.CaravanExpedition;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public class CaravanGuardAiMixin {

    /** HEAD: 设目标，确保 AI 处理时有攻击对象 */
    @Inject(method = "serverAiStep", at = @At("HEAD"))
    private void beforeServerAiStep(CallbackInfo ci) {
        forceTarget();
    }

    /** TAIL: AI 处理完后再次恢复目标，防止被 vanilla goals 清空 */
    @Inject(method = "serverAiStep", at = @At("TAIL"))
    private void afterServerAiStep(CallbackInfo ci) {
        forceTarget();
    }

    /** 阻止 vanilla goals 将商队守卫的目标设为 null */
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void onSetTarget(LivingEntity target, CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        if (self.level().isClientSide()) return;
        if (target != null) return;

        if (!(self instanceof IronGolem) && !(self instanceof SnowGolem)) return;

        BaseEvent current = WorldRandomEvents.EVENT_MANAGER.getCurrentEvent();
        if (current instanceof CaravanExpedition caravan && caravan.isActive()) {
            if (caravan.isCaravanGuard(self)) {
                Player aggroTarget = caravan.getAggroTarget();
                if (aggroTarget != null && aggroTarget.isAlive()) {
                    ci.cancel();
                }
            }
        }
    }

    private void forceTarget() {
        Mob self = (Mob) (Object) this;
        if (self.level().isClientSide()) return;

        BaseEvent current = WorldRandomEvents.EVENT_MANAGER.getCurrentEvent();
        if (current instanceof CaravanExpedition caravan && caravan.isActive()) {
            if ((self instanceof IronGolem || self instanceof SnowGolem)
                && caravan.isCaravanGuard(self)) {
                Player target = caravan.getAggroTarget();
                if (target != null && target.isAlive()) {
                    self.setTarget(target);
                }
            }
        }
    }
}