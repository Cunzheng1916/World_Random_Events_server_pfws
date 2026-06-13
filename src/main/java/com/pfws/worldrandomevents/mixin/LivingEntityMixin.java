package com.pfws.worldrandomevents.mixin;

import com.pfws.worldrandomevents.WorldRandomEvents;
import com.pfws.worldrandomevents.event.BaseEvent;
import com.pfws.worldrandomevents.event.disaster.BloodMoon;
import com.pfws.worldrandomevents.event.disaster.SoulStorm;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"))
    private void onDropAllDeathLoot(ServerLevel level, DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (level.isClientSide()) return;

        BaseEvent current = WorldRandomEvents.EVENT_MANAGER.getCurrentEvent();
        if (current instanceof BloodMoon && current.isActive()) {
            if (self instanceof net.minecraft.world.entity.monster.Monster) {
                Random random = new Random();
                if (random.nextFloat() < 0.05) {
                    ItemEntity goldDrop = new ItemEntity(level,
                        self.getX(), self.getY(), self.getZ(),
                        new ItemStack(Items.GOLD_INGOT));
                    level.addFreshEntity(goldDrop);
                }
            }
        }
    }

    @Inject(method = "dropExperience", at = @At("HEAD"), cancellable = true)
    private void onDropExperience(ServerLevel level, net.minecraft.world.entity.Entity attacker, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (level.isClientSide()) return;

        BaseEvent current = WorldRandomEvents.EVENT_MANAGER.getCurrentEvent();
        if (current instanceof BloodMoon && current.isActive()) {
            int baseExp = self.getExperienceReward(level, attacker);
            self.skipDropExperience();
            if (attacker instanceof ServerPlayer player) {
                player.giveExperiencePoints(baseExp * 2);
            }
            ci.cancel();
        }
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void onDie(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return;
        if (!(self instanceof ServerPlayer player)) return;

        ServerLevel level = (ServerLevel) self.level();
        BaseEvent current = WorldRandomEvents.EVENT_MANAGER.getCurrentEvent();

        if (current instanceof SoulStorm storm && storm.isActive()) {
            storm.createSoulCage(player.blockPosition(), player);
        }
    }
}