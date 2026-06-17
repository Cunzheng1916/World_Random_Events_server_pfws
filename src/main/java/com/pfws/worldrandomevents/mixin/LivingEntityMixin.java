package com.pfws.worldrandomevents.mixin;

import com.pfws.worldrandomevents.WorldRandomEvents;
import com.pfws.worldrandomevents.event.BaseEvent;
import com.pfws.worldrandomevents.event.disaster.BloodMoon;
import com.pfws.worldrandomevents.event.disaster.PiglinInvasion;
import com.pfws.worldrandomevents.event.neutral.CaravanExpedition;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
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

        // 使用 ACTIVE_INSTANCE 静态引用，确保在事件结束清理期间也能正确掉落
        CaravanExpedition caravan = CaravanExpedition.ACTIVE_INSTANCE;
        if (caravan != null && caravan.isActive()) {
            if (caravan.isCaravanGuard(self)) {
                Random random = new Random();
                int diamonds = 7 + random.nextInt(10);
                int ironBlocks = 19 + random.nextInt(7);
                int expBottles = 30 + random.nextInt(21);
                int netheriteScraps = 1 + random.nextInt(2);

                spawnDrop(level, self, new ItemStack(Items.DIAMOND, diamonds));
                spawnDrop(level, self, new ItemStack(Items.IRON_BLOCK, ironBlocks));
                spawnDrop(level, self, new ItemStack(Items.EXPERIENCE_BOTTLE, expBottles));
                spawnDrop(level, self, new ItemStack(Items.NETHERITE_SCRAP, netheriteScraps));
            }
        }

        if (current instanceof PiglinInvasion pi && pi.isActive() && pi.isInvasionMob(self)) {
            Random random = new Random();
            int goldBlocks = 3 + random.nextInt(6);
            int diamonds = 2 + random.nextInt(4);
            int expBottles = 10 + random.nextInt(11);

            spawnDrop(level, self, new ItemStack(Items.GOLD_BLOCK, goldBlocks));
            spawnDrop(level, self, new ItemStack(Items.DIAMOND, diamonds));
            spawnDrop(level, self, new ItemStack(Items.EXPERIENCE_BOTTLE, expBottles));

            if (random.nextFloat() < 0.08f) {
                spawnDrop(level, self, new ItemStack(Items.NETHERITE_SCRAP, 1));
            }
        }
    }

    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
    private float modifyHurtAmount(float amount, ServerLevel level, DamageSource source, float originalAmount) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (level.isClientSide()) return amount;

        BaseEvent current = WorldRandomEvents.EVENT_MANAGER.getCurrentEvent();
        if (current instanceof PiglinInvasion pi && pi.isActive() && pi.isInvasionMob(self)) {
            ItemStack chest = self.getItemBySlot(EquipmentSlot.CHEST);
            if (chest.is(Items.NETHERITE_CHESTPLATE) || chest.is(Items.DIAMOND_CHESTPLATE)) {
                return amount * 0.9f;
            } else {
                return amount * 1.4f;
            }
        }

        return amount;
    }

    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void afterHurt(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (level.isClientSide()) return;
        if (!cir.getReturnValue()) return;

        BaseEvent current = WorldRandomEvents.EVENT_MANAGER.getCurrentEvent();
        if (current instanceof CaravanExpedition caravan && caravan.isActive()) {
            Entity attacker = source.getEntity();
            if (attacker instanceof Player player) {
                if (self instanceof WanderingTrader && caravan.isCaravanTrader(self)) {
                    caravan.aggroGuardsOnPlayer(player);
                } else if (caravan.isCaravanGuard(self)) {
                    caravan.aggroGuardsOnPlayer(player);
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

        }

    private static void spawnDrop(ServerLevel level, LivingEntity entity, ItemStack stack) {
        ItemEntity drop = new ItemEntity(level,
            entity.getX(), entity.getY(), entity.getZ(), stack);
        level.addFreshEntity(drop);
    }
}