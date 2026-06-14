package com.pfws.worldrandomevents.mixin;

import com.pfws.worldrandomevents.WorldRandomEvents;
import com.pfws.worldrandomevents.event.BaseEvent;
import com.pfws.worldrandomevents.event.disaster.BloodMoon;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.DropChances;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public class MobMixin {

    @Inject(method = "getDropChances", at = @At("RETURN"), cancellable = true)
    private void onGetDropChances(CallbackInfoReturnable<DropChances> cir) {
        Mob self = (Mob) (Object) this;
        if (self.level().isClientSide()) return;

        BaseEvent current = WorldRandomEvents.EVENT_MANAGER.getCurrentEvent();
        if (current instanceof BloodMoon && current.isActive()) {
            DropChances dropChances = cir.getReturnValue();
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.isArmor() || slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) {
                    dropChances = dropChances.withEquipmentChance(slot, 0.4f);
                }
            }
            cir.setReturnValue(dropChances);
        }
    }
}
